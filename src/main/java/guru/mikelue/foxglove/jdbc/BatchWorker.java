package guru.mikelue.foxglove.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.TupleAccessor;

/**
 * Since {@link PreparedStatement#getGeneratedKeys()} behaves differently
 * across different databases, this interface is used to implement different strategy.
 *
 * <ul>
 *   <li>Derby, SQLite - Only get generated value of latest inserted row</li>
 *   <li>MSSQL - Only able to get value by {@link PreparedStatement#executeUpdate()}</li>
 *   <li>Otherwise, gets the generated values by {@link PreparedStatement#executeBatch}</li>
 * </ul>
 *
 * Microsoft JDBC Driver 11.2 for SQL Server
 * Apache Derby Embedded JDBC Driver
 * SQLite JDBC
 *
 * HSQL Database Engine Driver
 * PostgreSQL JDBC Driver
 * MySQL Connector/J
 * Oracle JDBC driver
 */
interface BatchWorker {
	static Pattern DRIVER_FOR_SINGLE_WORKER = Pattern.compile(
		"(?i).*(derby|sqlite|microsoft).*"
	);

	static BatchWorker newInstance(
		String dbDriverName,
		PreparedStatement stmt, String[] askedGeneratedColumns,
		Consumer<List<TupleAccessor>> generatedValuesConsumer,
		int batchSize
	) {
		if (DRIVER_FOR_SINGLE_WORKER.matcher(dbDriverName).matches()) {
			return new SingleBatchWorker(
				stmt, askedGeneratedColumns, generatedValuesConsumer
			);
		}

		return new PluralBatchWorker(
			stmt, askedGeneratedColumns, generatedValuesConsumer, batchSize
		);
	}

	void addBatch(Map<ColumnMeta, Object> paramSet) throws SQLException;
	void executeBatch() throws SQLException;

	static void setParams(
		PreparedStatement stmt, Map<ColumnMeta, Object> paramSet
	) throws SQLException {
		var logger = org.slf4j.LoggerFactory.getLogger(BatchWorker.class);

		int paramIndex = 1;
		for (ColumnMeta columnMeta: paramSet.keySet()) {
			Object value = paramSet.get(columnMeta);
			JDBCType jdbcType = columnMeta.jdbcType();

			if (value == null) {
				stmt.setNull(paramIndex, jdbcType.getVendorTypeNumber());
				paramIndex++;
				continue;
			}

			if (value instanceof Temporal) {
				if (value instanceof Instant instant) {
					stmt.setObject(paramIndex, instant.atOffset(ZoneOffset.UTC));
				} else if (value instanceof ZonedDateTime zonedDateTime) {
					stmt.setObject(paramIndex, zonedDateTime.toOffsetDateTime());
				} else {
					stmt.setObject(paramIndex, value);
				}
			} else if (value instanceof java.util.Date utilDate) {
				if (value instanceof java.sql.Time sqlTime)  {
					stmt.setTime(paramIndex, sqlTime);
				} else if (value instanceof java.sql.Date sqlDate)  {
					stmt.setDate(paramIndex, sqlDate);
				} else if (value instanceof java.sql.Timestamp sqlTimestamp)  {
					stmt.setTimestamp(paramIndex, sqlTimestamp);
				} else {
					stmt.setTimestamp(paramIndex, new java.sql.Timestamp(utilDate.getTime()));
				}
			} else if (value instanceof java.lang.Number) {
				if (value instanceof Byte) {
					stmt.setByte(paramIndex, (Byte) value);
				} else if (value instanceof Short) {
					stmt.setShort(paramIndex, (Short) value);
				} else if (value instanceof Integer) {
					stmt.setInt(paramIndex, (Integer) value);
				} else if (value instanceof Long) {
					stmt.setLong(paramIndex, (Long) value);
				} else if (value instanceof Float) {
					stmt.setFloat(paramIndex, (Float) value);
				} else if (value instanceof Double) {
					stmt.setDouble(paramIndex, (Double) value);
				} else if (value instanceof BigDecimal) {
					stmt.setBigDecimal(paramIndex, (BigDecimal) value);
				}
			} else {
				/*
				For oracle INTERVALDS and INTERVALYM
				this would cause error:
				Caused by: java.sql.SQLException: ORA-17004: Invalid column type
				https://docs.oracle.com/error-help/db/ora-17004/
			  	*/
				stmt.setObject(paramIndex, value, jdbcType.getVendorTypeNumber());
			}

			paramIndex++;
		}
	}
}

class PluralBatchWorker implements BatchWorker {
	private Logger logger = LoggerFactory.getLogger(PluralBatchWorker.class);

	private final PreparedStatement stmt;
	private final Consumer<List<TupleAccessor>> generatedValuesConsumer;
	private final GeneratedValueLoader generatedValueLoader;
	private final int batchSize;
	private int unExecutedNumberOfRows = 0;

	PluralBatchWorker(
		PreparedStatement stmt, String[] askedGeneratedColumns,
		Consumer<List<TupleAccessor>> generatedValuesConsumer,
		int batchSize
	) {
		this.stmt = stmt;
		this.generatedValueLoader = new GeneratedValueLoader(askedGeneratedColumns);
		this.generatedValuesConsumer = generatedValuesConsumer;
		this.batchSize = batchSize;
	}

	@Override
	public void addBatch(Map<ColumnMeta, Object> paramSet) throws SQLException
	{
		BatchWorker.setParams(stmt, paramSet);

		stmt.addBatch();
		unExecutedNumberOfRows++;

		if (unExecutedNumberOfRows >= batchSize) {
			executeBatch();
		}
	}

	@Override
	public void executeBatch() throws SQLException
	{
		if (unExecutedNumberOfRows == 0) {
			return;
		}

		logger.debug("Executing for [{}] statements of batch[{}]", unExecutedNumberOfRows, batchSize);

		stmt.executeBatch();
		unExecutedNumberOfRows = 0;

		try (var rs = stmt.getGeneratedKeys()) {
			generatedValuesConsumer.accept(
				generatedValueLoader.toTuples(rs)
			);
		}
	}
}

class SingleBatchWorker implements BatchWorker {
	private Logger logger = LoggerFactory.getLogger(SingleBatchWorker.class);

	private final PreparedStatement stmt;
	private final Consumer<List<TupleAccessor>> generatedValuesConsumer;
	private final GeneratedValueLoader generatedValueLoader;
	private int counter = 0;

	SingleBatchWorker(
		PreparedStatement stmt, String[] askedGeneratedColumns,
		Consumer<List<TupleAccessor>> generatedValuesConsumer
	) {
		this.stmt = stmt;
		this.generatedValueLoader = new GeneratedValueLoader(askedGeneratedColumns);
		this.generatedValuesConsumer = generatedValuesConsumer;
	}

	@Override
	public void addBatch(Map<ColumnMeta, Object> paramSet) throws SQLException
	{
		BatchWorker.setParams(stmt, paramSet);

		stmt.executeUpdate();
		counter++;

		try (var rs = stmt.getGeneratedKeys()) {
			generatedValuesConsumer.accept(
				generatedValueLoader.toTuples(rs)
			);
		}
	}

	@Override
	public void executeBatch() throws SQLException
	{
		logger.debug("Have executed [{}] statements individually", counter);
		counter = 0;
	}
}
