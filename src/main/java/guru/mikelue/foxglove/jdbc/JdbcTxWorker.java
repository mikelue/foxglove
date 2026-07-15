package guru.mikelue.foxglove.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
