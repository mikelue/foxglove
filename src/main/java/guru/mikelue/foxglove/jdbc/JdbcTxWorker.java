package guru.mikelue.foxglove.jdbc;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.TupleAccessor;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

/**
 * This worker is responsible for performing JDBC transaction for insertion of generated rows.
 */
class JdbcTxWorker implements AutoCloseable {
	/**
	 * The context make JdbcTxWorker easier to be tested.
	 */
	record InsertionContext (
		String sql, int numberOfRows,
		String[] namesOfGeneratedColumns,
		Supplier<Map<ColumnMeta, Object>> rowParamsGenerator
	) {}

	private Logger logger = LoggerFactory.getLogger(JdbcTxWorker.class);

	private final Connection conn;
	private final TransactionGear txGear;
	private final boolean oldAutoCommit;

	JdbcTxWorker(
		TransactionGear txGear
	) throws SQLException {
		this.txGear = txGear;
		this.conn = txGear.connection();

		oldAutoCommit = conn.getAutoCommit();

		if (!txGear.joinConnection()) {
			conn.setAutoCommit(false);
		}
	}

	private int unCommittedNumberOfRows = 0;

	@Override
	public void close() throws SQLException
	{
		logger.debug("Closing JDBC transaction worker");
		commitIfNeeded();

		if (!txGear.joinConnection()) {
			conn.setAutoCommit(oldAutoCommit);
		}
	}

	/**
	 * This is stateful method, which would keep track of uncommitted rows.
	 *
	 * The {@link #close()} method would commit remaining uncommitted rows
	 * if joinConnection is false.
	 */
	int performInsert(
		InsertionContext context,
		Consumer<List<TupleAccessor>> generatedValuesConsumer
	) throws SQLException
	{
		var insertSql = context.sql();
		var namesOfGeneratedColumns = context.namesOfGeneratedColumns();

		logger.debug(
			"Going to insert [{}] rows by SQL:\n\t{}",
			context.numberOfRows(), insertSql
		);

		try (var stmt = namesOfGeneratedColumns.length > 0 ?
			conn.prepareStatement(insertSql, namesOfGeneratedColumns) :
			conn.prepareStatement(insertSql, RETURN_GENERATED_KEYS)
		) {
			var batchWorker = BatchWorker.newInstance(
				conn.getMetaData().getDriverName(),
				stmt, namesOfGeneratedColumns, generatedValuesConsumer,
				txGear.batchSize()
			);
			var rowParamsGenerator = context.rowParamsGenerator();

			for (int rowIndex = 0; rowIndex < context.numberOfRows(); rowIndex++) {
				var valuesOfRow = rowParamsGenerator.get();

				if (logger.isDebugEnabled()) {
					logger.trace("Preparing row[{}] for insertion: {}",
						rowIndex, valuesOfRow.values());
				}

				batchWorker.addBatch(valuesOfRow);

				unCommittedNumberOfRows++;
				if (unCommittedNumberOfRows >= txGear.batchSize()) {
					commitIfNeeded();
				}
			}

			/*
			 * If there are existing statements not executed, execute them here.
			 */
			batchWorker.executeBatch();
			// :~)
		}

		return context.numberOfRows();
	}

	private void commitIfNeeded() throws SQLException
	{
		if (txGear.joinConnection()) {
			logger.debug("Skip committing [{}] remaining statements because of joining existing transaction.", unCommittedNumberOfRows);
			unCommittedNumberOfRows = 0;
			return;
		}

		if (unCommittedNumberOfRows > 0) {
			logger.debug("Committing [{}] remaining statements of batch[{}].", unCommittedNumberOfRows, txGear.batchSize());
			conn.commit();
			unCommittedNumberOfRows = 0;
		}
	}
}

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
		int paramIndex = 1;
		for (ColumnMeta columnMeta: paramSet.keySet()) {
			Object value = paramSet.get(columnMeta);
			JDBCType jdbcType = columnMeta.jdbcType();

			if (value == null) {
				stmt.setNull(paramIndex, jdbcType.getVendorTypeNumber());
				paramIndex++;
				continue;
			}

			stmt.setObject(paramIndex, value, jdbcType.getVendorTypeNumber());
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
