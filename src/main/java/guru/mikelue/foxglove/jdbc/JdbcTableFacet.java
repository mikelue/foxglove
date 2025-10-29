package guru.mikelue.foxglove.jdbc;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.TupleAccessor;
import guru.mikelue.foxglove.functional.Int4SequenceSupplier;
import guru.mikelue.foxglove.functional.SequenceSupplier;
import guru.mikelue.foxglove.functional.SupplierDecider;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.CartesianProductSettingStep;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.ColumnSimpleStep;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.KeyOfIntSettingStep;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.ReferenceSettingStep;
import guru.mikelue.foxglove.setting.DataSetting;
import guru.mikelue.foxglove.setting.DataSettingInfo;
import guru.mikelue.foxglove.setting.SettingAware;
import guru.mikelue.foxglove.setting.SettingProvider;

/**
 * JDBC implementation of {@link TableFacet}.
 *
 * <p>
 * <strong style="color:red">Important: this object is not reusable across tests, you should build an new one for each test.</strong>
 *
 * <p>
 * Use {@link #builder(String)} to build this object.
 *
 * <pre><code class="language-java">
 * JdbcTableFacet tableFacet = JdbcTableFacet.builder("test_table")
 *     .build();
 * </code></pre>
 *
 * See more examples in {@link Builder}.
 *
 * @see #builder(String)
 * @see JdbcDataGenerator
 */
public class JdbcTableFacet implements TableFacet, SettingProvider {
	/**
	 * Starts to build a {@link JdbcTableFacet} with table name.
	 *
	 * @param tableName The name of table
	 *
	 * @return The builder to build {@link JdbcTableFacet}
	 */
	public static Builder builder(String tableName)
	{
		return new Builder(tableName);
	}

	/**
	 * Builds a {@link JdbcTableFacet} with table name.
	 *
	 * <h2>Number of rows</h2>
	 *
	 * You can only use <strong>one of</strong> following ways to decide the number of rows to be generated:
	 *
	 * <ul>
	 *   <li>Using {@link #numberOfRows(int)} in the builder.</li>
	 *   <li>Using {@link #keyOfInt(String)} by limit the amount of generated keys.</li>
	 *   <li>Using {@link #cartesianProduct(String)} on at least one column.</li>
	 *   <li>Using {@link #referencing(String)} with cardinality(self side) to parent rows.</li>
	 * </ul>
	 *
	 * <hr>
	 *
	 * <h2>Basic example</h2>
	 *
	 * <ul>
	 *   <li>Use {@link #column(String)} to configure value generator for a column.</li>
	 *   <li>Use {@link #numberOfRows(int)} to configure fixed number of rows to be generated.</li>
	 *   <li>Use {@link #includeColumns(String...)} to include those columns only(others are excluded).</li>
	 *   <li>Use {@link #excludeColumns(String...)} to exclude some columns.</li>
	 * </ul>
	 *
	 * Example(<em>builds {@code 10} rows</em>):
	 * <pre><code class="language-java">
	 * var facet = JdbcTableFacet.builder(TABLE_CAR)
	 *     .numberOfRows(10)
	 *     // Round robin among several brands
	 *     .column("cr_brand")
	 *         .roundRobin("Toyota", "Honda", "Ford", "BMW", "Audi")
	 *     // Generates year between 2015 and 2025
	 *     .column("cr_year")
	 *         .useSpec(() -&gt; gen().shorts().range((short)2015, (short)2025))
	 *     // Random choice for number of seats
	 *     .column("cr_seats")
	 *         .useSpec(() -&gt; gen().oneOf(2, 4, 5, 7))
	 *     // Color with nullable property get nullable values
	 *     .&lt;String&gt;column("cr_color")
	 *         .decideSupplier(columnMeta -&gt; {
	 *             var colorGenerator = gen().oneOf("Red", "Blue", "Green");
	 *
	 *             if (columnMeta.properties().contains(Property.NULLABLE)) {
	 *                 return colorGenerator.nullable();
	 *             }
	 *
	 *             return colorGenerator;
	 *         })
	 *     // Fixed value for status column
	 *     .column("cr_status")
	 *         .fixed(null)
	 *     // Includes these two columns by auto-generating their values
	 *     .includeColumns("cr_license_plate", "cr_daily_rate", "cr_created_at", "cr_model")
	 *     .build();
	 * </code></pre>
	 *
	 * <h2>Example for deciding number of rows</h2>
	 *
	 * <h3>Key column</h3>
	 * Use {@link #keyOfInt(String)} to configure a key column as integral value,
	 *
	 * <p>Example(<em>builds {@code 10} rows by number of key values</em>):
	 * <pre><code class="language-java">
	 * var facet = JdbcTableFacet.builder(TABLE_CAR)
	 *     .keyOfInt("cr_id").limit(1000, 10)
	 *     .build();
	 * </code></pre>
	 *
	 * <h3>Cartesian product</h3>
	 *
	 * Use {@link #cartesianProduct(String)} to configure columns for Cartesian product.
	 *
	 * <p>Example(<em>builds <code>3 * 3 = 9</code> rows</em>):
	 * <pre><code class="language-java">
	 * var facet = JdbcTableFacet.builder(TABLE_CAR)
	 *     .cartesianProduct("cr_brand")
	 *         .domain("Toyota", "Honda", "Ford")
	 *     .cartesianProduct("cr_year")
	 *         .domain(2020, 2021, 2022)
	 *     .build();
	 * </code></pre>
     *
	 * <h3>Referencing</h3>
	 *
	 * Use {@link #referencing(String)} to configure a column referencing to another table,
	 *
	 * <p>Example(<em>builds <code>10 * 2 = 20</code> rows by number of key values</em>):
	 *
	 * <pre><code class="language-java">
	 * // Prepares parent facet
	 * var carFacet = JdbcTableFacet.builder(TABLE_CAR)
	 *     .numberOfRows(10)
	 *     .build();
	 *
	 * // Generates rows referencing to the parent facet.
	 * var featureFacet = JdbcTableFacet.builder(TABLE_CAR_FEATURE)
	 *     .referencing("cf_cr_id").parent(carFacet, "cr_id")
	 *         // Every car is referenced by one feature.
	 *         .cardinality(2)
	 *     .column("cf_feature_name")
	 *         .fixed("Sunroof")
	 *     .build();
	 * </code></pre>
	 *
	 * <h2>Other features</h2>
	 *
	 * <h3>Change row values before insertion</h3>
	 *
	 * Use {@link #onTupleGenerated(Consumer)} to change generated values before insertion.
	 *
	 * <pre><code class="language-java">
	 * var facet = JdbcTableFacet.builder(TABLE_CAR)
	 *     .numberOfRows(RANDOM_SIZE)
	 *     .column("cr_brand")
	 *         .roundRobin("Toyota", "Honda", "Ford", "BMW", "Audi")
	 *     // Sets the value of "cr_license_plate" by combination of "cr_brand" and a random number
	 *     .onTupleGenerated(tuple -> tuple.setValue(
	 *         "cr_license_plate",
	 *         tuple.getValue("cr_brand") + "-" + randomNumber.get()
	 *     ))
	 *     .build();
	 * </code></pre>
	 *
	 * <h3>Applies {@link DataSetting}</h3>
	 *
	 * {@link #withSetting(DataSettingInfo)} can be used to apply {@link DataSettingInfo} locally.
	 *
	 * <pre><code class="language-java">
	 * var setting = new DataSetting()
	 *     // For any column with type of "VARCHAR", using the supplier
	 *     .givenType(JDBCType.VARCHAR)
	 *         .useSupplier(() -> sampleText + suffixSupplier.get())
	 *
	 * var facet = JdbcTableFacet.builder(TABLE_CAR)
	 *     // Uses the setting on the whole table
	 *     .withSetting(setting)
	 *     .numberOfRows(RANDOM_SIZE)
	 *     .build();
	 * </code></pre>
	 *
	 * @see DataSetting
	 */
	public static class Builder implements SettingAware<Builder> {
		private JdbcTableFacet newTableFacet = new JdbcTableFacet();
		private CartesianProductBuilder cartesianProductBuilder = null;
		private NumberSource sourceOfRowNumber = null;

		/**
		 * Constructs the builder with table name.
		 *
		 * @param newTableName The name of table
		 */
		private Builder(String newTableName)
		{
			newTableName = StringUtils.trimToNull(newTableName);

			Validate.notNull(newTableName, "Table name must not be blank");

			newTableFacet.tableName = newTableName;
			newTableFacet.valueTomb = new ValueTomb(newTableName);
		}

		/**
		 * Sets the number of rows to be generated for the table.
		 *
		 * @param numberOfRows The number of rows to be generated
		 *
		 * @return This builder
		 */
		public Builder numberOfRows(int numberOfRows)
		{
			validateRowNumberIntegrity(null);
			Validate.isTrue(numberOfRows > 0, "Number of numberOfRows must be greater than zero");
			sourceOfRowNumber = NumberSource.Plain;

			newTableFacet.supplierForNumberOfRows = () -> numberOfRows;
			return this;
		}

		/**
		 * Starts to configure key column as integral value,
		 * which would limit for number of generated rows for this table.
		 *
		 * If you don't have need to limit the number of generated values,
		 * you can use {@link #column(String)} with {@link Int4SequenceSupplier}, etc.
		 *
		 * @param columnName The name of column
		 *
		 * @return The next step to configure value generator for the key column
		 *
		 * @see SequenceSupplier
		 */
		public KeyOfIntSettingStep keyOfInt(String columnName)
		{
			validateRowNumberIntegrity(columnName);
			final String safeColumnName = safeColumnName(columnName);
			sourceOfRowNumber = NumberSource.KeyColumn;

			return new KeySettingStepImpl(
				this,
				keySupplier -> {
					newTableFacet.supplierForNumberOfRows = keySupplier::getNumberOfRows;

					@SuppressWarnings("unchecked")
					var objectSupplier = (Supplier<Object>)(Object)keySupplier;
					newTableFacet.columnSuppliers.put(
						safeColumnName,
						columnMeta -> objectSupplier
					);
				}
			);
		}

		/**
		 * Starts to configure table reference for a column.
		 *
		 * <p>
		 * This is differ from {@link ColumnSimpleStep#from(JdbcTableFacet, String)}
		 * that this method would decide the number of rows to be generated.
		 *
		 * @param <T> The type of value generated for the column
		 * @param columnName The name of column referencing to another table
		 *
		 * @return The next step to configure table reference for the column
		 */
		@SuppressWarnings("unchecked")
		public <T> ReferenceSettingStep<T> referencing(String columnName)
		{
			validateRowNumberIntegrity(columnName);

			final String safeColumnName = safeColumnName(columnName);
			sourceOfRowNumber = NumberSource.Reference;

			return new ReferenceSettingStepImpl<>(
				this,
				cardinalityInfo -> {
					newTableFacet.supplierForNumberOfRows = cardinalityInfo::getNumberOfRows;
					newTableFacet.columnSuppliers.put(
						safeColumnName,
						columnMeta -> (Supplier<Object>)cardinalityInfo.buildLazySupplier()
					);
				}
			);
		}

		/**
		 * Starts to configure cartesian product for a column.
		 *
		 * @param <T> The type of value generated for the column
		 * @param columnName The name of column
		 *
		 * @return The next step to configure cartesian product for the column
		 */
		public <T> CartesianProductSettingStep<T> cartesianProduct(String columnName)
		{
			Validate.isTrue(
				sourceOfRowNumber == null ||
				sourceOfRowNumber == NumberSource.CartesianProduct,
				"Number of rows conflict[%s]: %s", columnName, sourceOfRowNumber
			);
			sourceOfRowNumber = NumberSource.CartesianProduct;

			if (cartesianProductBuilder == null) {
				cartesianProductBuilder = new CartesianProductBuilder();
			}

			final String safeColumnName = safeColumnName(columnName);

			return new CartesianProductStepImpl<T>(
				this,
				values -> cartesianProductBuilder.putDomain(safeColumnName, values)
			);
		}

		/**
		 * Starts to configure value generator by column name.
		 *
		 * @param <T> The type of value generated for the column
		 * @param columnName The name of column
		 *
		 * @return The next step to configure value generator for the column
		 */
		public <T> ColumnSimpleStep<T> column(String columnName)
		{
			final String safeColumnName = safeColumnName(columnName);

			return new ColumnSettingStepImpl<T>(this,
				decider -> {
					newTableFacet.columnSuppliers.put(safeColumnName, decider);
				}
			);
		}

		/**
		 * These columns would be generated automatically in spite of their properties.
		 *
		 * <p>
		 * This setting would take precedence over {@link DataSettingInfo#isAutoGenerating(ColumnMeta)}.
		 *
		 * @param columns The column names
		 *
		 * @return This builder
		 */
		public Builder includeColumns(String... columns)
		{
			Validate.isTrue(
				newTableFacet.exclusionForColumns.isEmpty(),
				"Cannot include columns when exclusion for columns is set already"
			);
			Validate.notEmpty(
				columns, "At least one column must be specified to include"
			);

			for (var includingColumn : columns) {
				newTableFacet.inclusionForColumns.add(safeColumnName(includingColumn));
			}

			return this;
		}

		/**
		 * Excludes the columns by their name.
		 *
		 * <p>
		 * This method is differ from {@link #includeColumns(String...)},
		 * the the value generation for un-set columns is decided by {@link DataSetting}.
		 *
		 * @param columns The column names
		 *
		 * @return This builder
		 */
		public Builder excludeColumns(String... columns)
		{
			Validate.isTrue(
				newTableFacet.inclusionForColumns.isEmpty(),
				"Cannot exclude columns when inclusion for columns is set already"
			);
			Validate.notEmpty(
				columns, "At least one column must be specified to exclude"
			);

			for (var excludingColumn : columns) {
				newTableFacet.exclusionForColumns.add(safeColumnName(excludingColumn));
			}

			return this;
		}

		/**
		 * Sets the consumer to handle generated tuple.
		 *
		 * You can use this callback to change generated values before insertion,
		 *
		 * @param tupleGeneratedConsumer The consumer to handle generated tuple
		 *
		 * @return This builder
		 */
		public Builder onTupleGenerated(Consumer<TupleAccessor> tupleGeneratedConsumer)
		{
			Validate.notNull(tupleGeneratedConsumer, "Tuple generated consumer must not be null");

			newTableFacet.tupleHandler = tupleGeneratedConsumer;
			return this;
		}

		/**
		 * This object has highest priority than {@link JdbcDataGenerator#withSetting(DataSettingInfo)} and
		 * {@link DataSetting#defaults()}.
		 *
		 * @param setting The setting to use
		 *
		 * @return This instance
		 */
		@Override
		public Builder withSetting(DataSettingInfo setting)
		{
			Validate.notNull(setting, "Data setting must not be null");

			newTableFacet.dataSetting = Optional.of(setting);
			return this;
		}

		/**
		 * Builds the {@link JdbcTableFacet}.
		 *
		 * @return The built {@link JdbcTableFacet}
		 */
		public JdbcTableFacet build()
		{
			/*
			 * Puts built values of columns following Cartesian product configuration
			 */
			if (cartesianProductBuilder != null) {
				newTableFacet.supplierForNumberOfRows = cartesianProductBuilder::getNumberOfRows;

				for (var columnName : cartesianProductBuilder.getColumnNames()) {
					newTableFacet.columnSuppliers.put(
						columnName,
						columnMeta -> cartesianProductBuilder.buildLazySupplier(columnName)
					);
				}
			}
			// :~)

			return newTableFacet;
		}

		private void validateRowNumberIntegrity(String columnName)
		{
			Validate.isTrue(
				sourceOfRowNumber == null,
				"Number of rows conflict[%s]: %s",
				columnName != null ? columnName : "<UNKNOWN>", sourceOfRowNumber
			);
		}

		private String safeColumnName(String columnName)
		{
			final String safeColumnName = StringUtils.trimToEmpty(columnName).toLowerCase();
			Validate.notBlank(safeColumnName, "Column name must not be blank");

			/*
			 * Ensures the integrity of column configuration
			 */
			Validate.isTrue(!newTableFacet.inclusionForColumns.contains(safeColumnName),
				"Column '%s' is set to be included(auto-generating) already", safeColumnName
			);
			Validate.isTrue(!newTableFacet.exclusionForColumns.contains(safeColumnName),
				"Column '%s' is set to be excluded(auto-generating) already", safeColumnName
			);
			Validate.isTrue(!newTableFacet.columnSuppliers.containsKey(safeColumnName),
				"Column '%s' has been configured already", safeColumnName
			);

			if (cartesianProductBuilder != null) {
				Validate.isTrue(!cartesianProductBuilder.isExisting(safeColumnName),
					"Column '%s' has been configured by Cartesian product already", safeColumnName
				);
			}
			// :~)

			return safeColumnName;
		}
	}

	private Supplier<Integer> supplierForNumberOfRows = null;

	private String tableName = null;
	private Optional<DataSettingInfo> dataSetting = Optional.empty();

	private Consumer<TupleAccessor> tupleHandler = null;

	/*
	 * Any kind of supplier is put to this:
	 *
	 * 1. General suppliers
	 * 2. Key column
	 * 3. Columns defined by Cartesian product
	 */
	private Map<String, SupplierDecider<?>> columnSuppliers = new HashMap<>(8);

	private Set<String> inclusionForColumns = new HashSet<>();
	private Set<String> exclusionForColumns = new HashSet<>();

	private ValueTomb valueTomb;

	private JdbcTableFacet() {}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String tableName()
	{
		return tableName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfRows()
	{
		if (supplierForNumberOfRows != null) {
			return supplierForNumberOfRows.get();
		}

		if (dataSetting.isPresent()) {
			return dataSetting.get().getDefaultNumberOfRows();
		}

		return DataSetting.defaults().getDefaultNumberOfRows();
	}

	/**
	 * Retrieves the configured {@link Supplier} for given column name.
	 *
	 * @param columnName The name of column
	 *
	 * @return The configured {@link Supplier} or empty
	 */
	@SuppressWarnings("unchecked")
	<T> Optional<Supplier<T>> resolveSupplier(ColumnMeta columnMeta)
	{
		var lowerCaseName = columnMeta.name().toLowerCase();

		if (columnSuppliers.containsKey(lowerCaseName)) {
			SupplierDecider<T> foundDecider = (SupplierDecider<T>)columnSuppliers.get(lowerCaseName);

			return Optional.of(foundDecider.apply(columnMeta));
		}

		return Optional.empty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<DataSettingInfo> getSetting()
	{
		return this.dataSetting;
	}

	ValueTomb getValueTomb()
	{
		return valueTomb;
	}

	List<String> getConfiguredNamesOfColumn()
	{
		var result = new ArrayList<String>(
			columnSuppliers.size() +
			inclusionForColumns.size() + exclusionForColumns.size()
		);

		result.addAll(columnSuppliers.keySet());
		result.addAll(inclusionForColumns);
		result.addAll(exclusionForColumns);

		return result;
	}

	/**
	 * Since there is automatic generation of columns,
	 * this method give three states:
	 *
	 * 1. Include - the column is must to be generated
	 * 2. Exclude - the column is must not to be generated
	 * 3. NotSet - let DataSetting to decide
	 */
	ColumnInclusionMode getColumnInclusion(ColumnMeta columnMeta)
	{
		var lowerCaseName = columnMeta.name().toLowerCase();

		if (columnSuppliers.containsKey(lowerCaseName)) {
			return ColumnInclusionMode.Include;
		}

		if (!inclusionForColumns.isEmpty()) {
			return inclusionForColumns.contains(lowerCaseName) ?
				ColumnInclusionMode.Include : ColumnInclusionMode.Exclude;
		}

		if (exclusionForColumns.contains(lowerCaseName)) {
			return ColumnInclusionMode.Exclude;
		}

		return ColumnInclusionMode.NotSet;
	}

	Consumer<TupleAccessor> getHandlerOfTuple()
	{
		return tupleHandler;
	}
}
