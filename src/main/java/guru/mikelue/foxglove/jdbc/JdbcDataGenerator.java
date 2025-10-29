package guru.mikelue.foxglove.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TupleAccessor;
import guru.mikelue.foxglove.setting.DataSetting;
import guru.mikelue.foxglove.setting.DataSettingInfo;
import guru.mikelue.foxglove.setting.LayeredDataSetting;

import static guru.mikelue.foxglove.ColumnMeta.Property.AUTO_INCREMENT;

/**
 * JDBC implementation of {@link DataGenerator}.
 */
public class JdbcDataGenerator implements DataGenerator<JdbcTableFacet> {
	/**
	 * The default batch size for insertion of rows.
	 */
	public final static int DEFAULT_BATCH_SIZE = 1024;

	private final DataSource dataSource;
	private final Connection connection;
	private int batchSize = DEFAULT_BATCH_SIZE;

	private Optional<DataSettingInfo> dataSetting = Optional.empty();
	private final MetaDataCache metaDataCache;

	/**
	 * Uses the given data source as the target database for generated data.
	 *
	 * @param dataSource The data source to use
	 */
	public JdbcDataGenerator(DataSource dataSource)
	{
		this.dataSource = dataSource;
		this.connection = null;

		try (var conn = dataSource.getConnection()) {
			this.metaDataCache = new MetaDataCache(conn);
		} catch (SQLException e) {
			throw new RuntimeJdbcException(e);
		}
	}

	/**
	 * Uses the database and joins the transaction of the given connection.
	 *
	 * @param connection The connection to use
	 */
	public JdbcDataGenerator(Connection connection)
	{
		this.dataSource = null;
		this.connection = connection;

		this.metaDataCache = new MetaDataCache(connection);
	}

	/**
	 * Sets the batch size for insertion of rows.
	 *
	 * @param batchSize The batch size for insertion of rows
	 *
	 * @return This instance
	 */
	public JdbcDataGenerator setBatchSize(int batchSize)
	{
		Validate.isTrue(batchSize > 0, "Batch size must be greater than zero");

		this.batchSize = batchSize;

		return this;
	}

	@Override
	public int generate(List<JdbcTableFacet> tableFacets)
		throws RuntimeJdbcException
	{
		Validate.notEmpty(tableFacets, "At least one table facet must be given");

		final var joinConn = this.connection != null;

		if (joinConn) {
			try {
				metaDataCache.loadMetadata(tableFacets, this.connection);

				return new DataGeneratorWorker(
					tableFacets, metaDataCache, dataSetting,
					new TransactionGear(
						this.connection, batchSize, true
					)
				)
					.generate();
			} catch (Exception e) {
				try {
					this.connection.rollback();
				} catch (SQLException rollbackEx) {
					throw new RuntimeJdbcException(e);
				}

				throw new RuntimeJdbcException(e);
			}
		}

		try (var currentConn = this.dataSource.getConnection()) {
			metaDataCache.loadMetadata(tableFacets, currentConn);

			try {
				return new DataGeneratorWorker(
					tableFacets, metaDataCache, dataSetting,
					new TransactionGear(
						currentConn, batchSize, false
					)
				).generate();
			} catch (Exception e) {
				if (!currentConn.getAutoCommit()) {
					currentConn.rollback();
				}
				throw e;
			}
		} catch (Exception e) {
			throw new RuntimeJdbcException(e);
		}
	}

	/**
	 * This object has lower priority than table facet's own setting, yet
	 * has higher priority than {@link DataSetting#defaults()}.
	 *
	 * @param setting The setting to use
	 *
	 * @return This instance
	 */
	@Override
	public DataGenerator<JdbcTableFacet> withSetting(DataSettingInfo setting)
	{
		dataSetting = Optional.of(setting);
		return this;
	}
}

class DataGeneratorWorker {
	private Logger logger = LoggerFactory.getLogger(DataGeneratorWorker.class);

	private final List<JdbcTableFacet> facetOfTables;
	private final DataSettingInfo dataSetting;
	private final TransactionGear transactionGear;
	private final MetaDataCache metaDataCache;

	DataGeneratorWorker(
		List<JdbcTableFacet> facetOfTables,
		MetaDataCache metaDataCache,
		Optional<DataSettingInfo> dataSetting,
		TransactionGear transactionGear
	) {
		this.facetOfTables = facetOfTables;
		this.metaDataCache = metaDataCache;
		this.dataSetting = dataSetting.orElse(null);
		this.transactionGear = transactionGear;
	}

	int generate() throws SQLException
	{
		int totalRowsGenerated = 0;

		try (var txWorker = new JdbcTxWorker(transactionGear)) {
			for (var table: facetOfTables) {
				var layeredDataSetting = new LayeredDataSetting(
					table.getSetting().orElse(null),
					dataSetting
				);

				totalRowsGenerated += doInsertForTable(
					txWorker, table, layeredDataSetting
				);
			}
		}

		return totalRowsGenerated;
	}

	/**
	 * Responsible for:
	 *
	 * 1. Determining the columns to be generated
	 * 2. Building the SQL statement
	 * 3. Building the row parameter generator
	 */
	private int doInsertForTable(
		JdbcTxWorker txWorker, JdbcTableFacet table,
		DataSettingInfo setting
	) throws SQLException
	{
		var metaOfColumns = metaDataCache.getMetaOfColumns(table.tableName());
		if (metaOfColumns.isEmpty()) {
			throw new RuntimeJdbcException("No column meta data found: " + table.tableName());
		}

		/*
		 * Determines the target columns for data generation
		 */
		var targetColumns = MetaUtils.filterColumns(
			metaOfColumns, setting, table
		);
		// :~)

		var targetColumnsSet = new HashSet<>(targetColumns);
		var namesOfGeneratedColumns = metaOfColumns.stream()
			.filter(meta -> {
				/*
				 * Excludes the columns should be generated by Foxglove.
				 */
				if (targetColumnsSet.contains(meta)) {
					return false;
				}
				// :~)

				var properties = meta.properties();
				return properties.contains(AUTO_INCREMENT);
			})
			.map(ColumnMeta::name)
			.toArray(String[]::new);

		// Builds the insertion SQL
		var sql = MetaUtils.buildInsertSql(
			transactionGear.connection().getMetaData(),
			table.tableName(), targetColumns
		);

		/*
		 * Builds the row parameter generator
		 */
		var rowGenerator = new RowParamsGenerator(table, targetColumns, setting);
		if (logger.isDebugEnabled()) {
			logger.debug(
				"Generating data for table: {}({})",
				table.tableName(),
				targetColumns.stream()
					.map(c -> String.format("%s(%s)", c.name(), c.typeName()))
					.toList()
			);
		}
		// :~)

		Consumer<TupleAccessor> tupleHandler = table.getHandlerOfTuple() != null ?
			table.getHandlerOfTuple() : (t -> {});
		var numberOfGeneratedRow = new MutableInt(0);
		var valueTomb = table.getValueTomb();
		var tupleSchema = new TupleAccessorImpl.TupleSchema(targetColumns);

		return txWorker.performInsert(
			new JdbcTxWorker.InsertionContext(
				sql, table.getNumberOfRows(), namesOfGeneratedColumns,
				() -> {
					var newRow = rowGenerator.generateRowParams();
					var newTuple = tupleSchema.createTupleAccessor(
						newRow, numberOfGeneratedRow.intValue()
					);

					tupleHandler.accept(newTuple);

					numberOfGeneratedRow.increment();
					valueTomb.preserveProtoData(newTuple);

					return newTuple.asMap();
				}
			),
			valueTomb::preserveAfterData
		);
	}
}
