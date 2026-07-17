package guru.mikelue.foxglove.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.TupleAccessor;
import guru.mikelue.foxglove.setting.DataSettingInfo;

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
		int batchSize,
		DataSettingInfo dataSettingInfo
	) {
		var WorkerAssistance = new WorkerAssistance(
			stmt, new GeneratedValueLoader(askedGeneratedColumns),
			generatedValuesConsumer, dataSettingInfo
		);

		if (DRIVER_FOR_SINGLE_WORKER.matcher(dbDriverName).matches()) {
			return new SingleBatchWorker(stmt, WorkerAssistance);
		}

		return new PluralBatchWorker(stmt, batchSize, WorkerAssistance);
	}

	void addBatch(Map<ColumnMeta, Object> paramSet) throws SQLException;
	void executeBatch() throws SQLException;
}

class WorkerAssistance {
	private final PreparedStatement stmt;
	private final Consumer<List<TupleAccessor>> generatedValuesConsumer;
	private final GeneratedValueLoader generatedValueLoader;
	private final Map<SetParameterIndex, CustomStatementSetter<?>> paramSetterCache = new HashMap<>(32);
	private final CustomStatementSetterProvider paramSetterProvider;

	WorkerAssistance(
		PreparedStatement stmt,
		GeneratedValueLoader generatedValueLoader,
		Consumer<List<TupleAccessor>> generatedValuesConsumer,
		DataSettingInfo dataSettingInfo
	) {
		this.stmt = stmt;
		this.generatedValuesConsumer = generatedValuesConsumer;
		this.generatedValueLoader = generatedValueLoader;
		this.paramSetterProvider = dataSettingInfo::getStatementSetter;
	}

	final protected void setParams(Map<ColumnMeta, Object> paramSet) throws SQLException
	{
		setParams(stmt, paramSet, paramSetterCache);
	}

	final protected void consumeValues(ResultSet rs) throws SQLException
	{
		generatedValuesConsumer.accept(
			generatedValueLoader.toTuples(rs)
		);
	}

	@SuppressWarnings("unchecked")
	private void setParams(
		PreparedStatement stmt, Map<ColumnMeta, Object> paramSet,
		Map<SetParameterIndex, CustomStatementSetter<?>> setterCache
	) throws SQLException {
		var paramIndex = 1;

		for (ColumnMeta columnMeta: paramSet.keySet()) {
			var jdbcType = columnMeta.jdbcType();
			var value = paramSet.get(columnMeta);

			CustomStatementSetter<Object> setParamFunc;

			if (value == null) {
				setParamFunc = (localStmt, localIndex, meta, localValue) ->
					stmt.setNull(localIndex, jdbcType.getVendorTypeNumber());
			} else {
				setParamFunc = (CustomStatementSetter<Object>)setterCache.computeIfAbsent(
					new SetParameterIndex(columnMeta, value.getClass()),
					index -> paramSetterProvider.apply(columnMeta)
						.orElseGet(
							() -> {
								return ParameterSetterFactory.smartSetterImpl(index);
							}
						)
				);
			}

			setParamFunc.setParameter(stmt, paramIndex, columnMeta, value);
			paramIndex++;
		}
	}
}

class PluralBatchWorker implements BatchWorker {
	private Logger logger = LoggerFactory.getLogger(PluralBatchWorker.class);

	private final WorkerAssistance assistance;
	private final PreparedStatement stmt;
	private final int batchSize;
	private int unExecutedNumberOfRows = 0;

	PluralBatchWorker(
		PreparedStatement stmt, int batchSize,
		WorkerAssistance assistance
	) {
		this.stmt = stmt;
		this.batchSize = batchSize;
		this.assistance = assistance;
	}

	@Override
	public void addBatch(Map<ColumnMeta, Object> paramSet) throws SQLException
	{
		assistance.setParams(paramSet);

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
			assistance.consumeValues(rs);
		}
	}
}

class SingleBatchWorker implements BatchWorker {
	private Logger logger = LoggerFactory.getLogger(SingleBatchWorker.class);

	private final WorkerAssistance assistance;
	private final PreparedStatement stmt;
	private int counter = 0;

	SingleBatchWorker(
		PreparedStatement stmt, WorkerAssistance assistance
	) {
		this.stmt = stmt;
		this.assistance = assistance;
	}

	@Override
	public void addBatch(Map<ColumnMeta, Object> paramSet) throws SQLException
	{
		assistance.setParams(paramSet);

		stmt.executeUpdate();
		counter++;

		try (var rs = stmt.getGeneratedKeys()) {
			assistance.consumeValues(rs);
		}
	}

	@Override
	public void executeBatch() throws SQLException
	{
		logger.debug("Have executed [{}] statements individually", counter);
		counter = 0;
	}
}

@FunctionalInterface
interface CustomStatementSetterProvider extends Function<ColumnMeta, Optional<CustomStatementSetter<?>>> {}
