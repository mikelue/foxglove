package guru.mikelue.foxglove.setting;

import java.sql.JDBCType;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.instancio.Instancio;
import org.instancio.generator.ValueSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.ColumnMeta.Property;
import guru.mikelue.foxglove.functional.ColumnMatcher;
import guru.mikelue.foxglove.functional.SupplierDecider;
import guru.mikelue.foxglove.functional.Suppliers;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;

import static java.sql.JDBCType.*;

/**
 * Defines the data generation setting.
 *
 * <h2>Overview</h2>
 *
 * This object can used by multi-layered mechanism(sorted by priority):
 *
 * <ul>
 *   <li>{@link JdbcTableFacet}</li>
 *   <li>{@link guru.mikelue.foxglove.DataGenerator}</li>
 *   <li><strong>globally</strong></li>
 * </ul>
 *
 * <h2>Global setting</h2>
 *
 * {@link #defaults()} could be used to configure the global default setting.
 *
 * <pre><code class="language-java">
 * DataSetting.defaults()
 *     .givenType(JDBCType.VARCHAR)
 *         .useSpec(Instancio.gen().string().alphaNumeric().length(16))
 * </code></pre>
 *
 * <hr>
 * <h2>Use setting locally</h2>
 *
 * Some types implement {@link SettingAware#withSetting(DataSettingInfo)},
 * so you could provide a customized {@link DataSetting} to them.
 *
 * Example of {@link guru.mikelue.foxglove.jdbc.JdbcTableFacet.Builder}
 * <pre><code class="language-java">
 * var settingOfATable = new DataSetting();
 *     .givenType(JDBCType.VARCHAR)
 *         .useSpec(Instancio.gen().string().alphaNumeric().length(16))
 *
 * tableFacetBuilder.withDataSetting(settingOfATable);
 * </code></pre>
 *
 * Example of {@link guru.mikelue.foxglove.DataGenerator}
 * <pre><code class="language-java">
 * var settingForGenerator = new DataSetting();
 *     .givenType(JDBCType.VARCHAR)
 *         .useSpec(Instancio.gen().string().alphaNumeric().length(16))
 *
 * dataGenerator.withDataSetting(settingForGenerator);
 * </code></pre>
 *
 * <hr>
 * <h2>Features</h2>
 *
 * <h3>Column spec</h3>
 *
 * There are ways to define the supplier for matched columns:
 *
 * <ul>
 *   <li>Using a {@link Supplier} by {@link ColumnConfig#useSupplier(Supplier)}</li>
 *   <li>Using a {@link Supplier} of {@link ValueSpec} by {@link ColumnConfig#useSpec(Supplier)}</li>
 *   <li>Using a {@link SupplierDecider} by {@link ColumnConfig#decideSupplier(SupplierDecider)}</li>
 * </ul>
 *
 * <h3>Auto-generating by properties</h3>
 *
 * <ul>
 *   <li>Use {@link #autoGenerateFor(Property...)} to set properties for auto-generating</li>
 *   <li>Use {@link #notAutoGenerateFor(Property...)} to unset properties for not to auto-generating</li>
 * </ul>
 *
 * <h3>Miscellaneous</h3>
 *
 * <ul>
 *   <li>Use {@link #generateNull(boolean)}, {@link #generateNull(int)} to supply possible {@code null} value for nullable columns</li>
 *   <li>Use {@link #largeTextLength(int, int)} to alter length for large text. e.g.: {@link JDBCType#CLOB}</li>
 * </ul>
 *
 * @see JdbcTableFacet.Builder
 * @see JdbcDataGenerator
 */
public class DataSetting implements DataSettingInfo {
	private final static Optional<Supplier<?>> NULL_SUPPLIER_OPT = Optional.of(() -> null);

	/**
	 * Gives the data setting can be changed for applying any {@link SettingAware} globally.
	 *
	 * @return The default data setting.
	 *
	 * @see <a href="https://foxglove.mikelue.guru/docs/default-generators/">Default Generators</a>
	 */
	public final static DataSetting defaults()
	{
		 return DefaultSetting.instance();
	}

	private Logger logger = LoggerFactory.getLogger(DataSetting.class);

	private final Map<JDBCType, SupplierDecider<?>> jdbcTypeConfigMap = new HashMap<>(32);
	private final Map<String, SupplierDecider<?>> typeNameConfigMap = new HashMap<>(4);
	private final Map<ColumnMatcher, SupplierDecider<?>> matcherConfigMap = new HashMap<>(4);

	private int minLengthOfLargeText = DefaultSetting.LARGE_TEXT_MIN_LENGTH;
	private int maxLengthOfLargeText = DefaultSetting.LARGE_TEXT_MAX_LENGTH;

	private int defaultNumberOfRows = DefaultSetting.DEFAULT_NUMBER_OF_ROWS;
	private Set<ColumnMeta.Property> autoGeneratingByProperties = EnumSet.copyOf(
		DefaultSetting.DEFAULT_COLUMN_PROPERTIES_FOR_AUTO_GENERATING
	);

	private int diceSides = DefaultSetting.DEFAULT_DICE_SIDES;
	private boolean generateNull = DefaultSetting.DEFAULT_GENERATE_NULL;

	private Set<JDBCType> notSupportedJdbcTypes = EnumSet.noneOf(JDBCType.class);

	private ColumnMatcher exclusion = c -> false;

	/**
	 * Constructs an empty data setting.
	 *
	 * <p>following settings are copied from {@link #defaults()}:
	 *
	 * <ul>
	 *   <li>Default number of rows</li>
	 *   <li>Auto-generating properties</li>
	 *   <li>Null generating setting</li>
	 *   <li>Large text length setting</li>
	 * </ul>
	 */
	public DataSetting()
	{
		var defaultSetting = defaults();

		/*
		 * The global setting may be not initialized yet
		 */
		if (defaultSetting != null) {
			setDefaultNumberOfRows(defaultSetting.getDefaultNumberOfRows());
			autoGeneratingByProperties = EnumSet.copyOf(defaultSetting.autoGeneratingByProperties);

			diceSides = defaultSetting.diceSides;
			generateNull = defaultSetting.generateNull;

			minLengthOfLargeText = defaultSetting.minLengthOfLargeText;
			maxLengthOfLargeText = defaultSetting.maxLengthOfLargeText;
		}
		// :~)
	}

	/**
	 * Starts to configure {@link Supplier} for columns matched by given {@link JDBCType}.
	 *
	 * @param <T> The type of values supplied
	 * @param jdbcType The JDBC type to match columns
	 *
	 * @return The next step to configure value generator for matched {@link JDBCType}
	 */
	public <T> ColumnConfig<T, DataSetting> givenType(JDBCType jdbcType)
	{
		Validate.notNull(jdbcType, "JDBC type must not be null");

		var newColumnConfig = new ColumnConfigImpl<T>(
			this,
			decider -> jdbcTypeConfigMap.put(jdbcType, decider)
		);

		return newColumnConfig;
	}

	/**
	 * Starts to configure {@link Supplier} for columns matched by given type name.
	 *
	 * @param <T> The type of values supplied
	 * @param typeName The type name to match columns
	 *
	 * @return The next step to configure value generator for matched type name
	 */
	public <T> ColumnConfig<T, DataSetting> givenType(String typeName)
	{
		final String safeTypeName = StringUtils.trimToNull(typeName);
		Validate.notBlank(safeTypeName, "Type name must not be blank");

		var newColumnConfig = new ColumnConfigImpl<T>(
			this,
			decider -> typeNameConfigMap.put(safeTypeName.toUpperCase(), decider)
		);

		return newColumnConfig;
	}

	/**
	 * Starts to configure {@link Supplier} for columns matched by given {@link ColumnMatcher}.
	 *
	 * <p>
	 * <em>The priority of multiple matchers <span style="color: Crimson;">is not determined</span></em>
	 *
	 * <strong>You have to provide a valid {@link Supplier} in your decider for matched column.</strong>
	 *
	 * @param <T> The type of values supplied
	 * @param matcher The matcher to match columns
	 *
	 * @return The next step to configure value generator for matched columns
	 */
	public <T> ColumnConfig<T, DataSetting> columnMatcher(ColumnMatcher matcher)
	{
		Validate.notNull(matcher, "Column matcher must not be null");

		var newColumnConfig = new ColumnConfigImpl<T>(
			this,
			decider -> matcherConfigMap.put(matcher, decider)
		);

		return newColumnConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getDefaultNumberOfRows()
	{
		return this.defaultNumberOfRows;
	}

	/**
	 * Sets the default number of rows for generated data.
	 *
	 * <p> This number of rows is used when:
	 *
	 * <ul>
	 *   <li>No specific number of rows is defined by {@link guru.mikelue.foxglove.TableFacet}</li>
	 * </ul>
	 *
	 * @param numberOfRows of rows The default number of rows for generated data
	 *
	 * @return The data setting itself
	 *
	 * @see #getDefaultNumberOfRows()
	 */
	public DataSetting setDefaultNumberOfRows(int numberOfRows)
	{
		Validate.isTrue(numberOfRows > 0, "Default number of rows must be greater than zero");

		this.defaultNumberOfRows = numberOfRows;
		return this;
	}

	/**
	 * Sets whether or not to generate value automatically by the given properties of a column.
	 *
	 * <p>
	 * Other properties not given will be set to not generate value automatically
	 * for first time call this method.
	 *
	 * @param properties The properties of columns
	 *
	 * @return The data setting itself
	 *
	 * @see notAutoGenerateFor(Property...)
	 */
	public DataSetting autoGenerateFor(ColumnMeta.Property... properties)
	{
		Validate.notEmpty(properties, "At least one property is required to set auto-generating");

		for (var property: properties) {
			autoGeneratingByProperties.add(property);
		}

		return this;
	}

	/**
	 * Sets to not to generate value automatically by the given properties of a column.
	 *
	 * Other properties not given will be set to generate value automatically
	 * for first time call this method.
	 *
	 * @param properties The properties of columns
	 *
	 * @return The data setting itself
	 *
	 * @see #autoGenerateFor(Property...)
	 */
	public DataSetting notAutoGenerateFor(ColumnMeta.Property... properties)
	{
		Validate.notEmpty(properties, "At least one property is required to set auto-generating");

		for (var property: properties) {
			autoGeneratingByProperties.remove(property);
		}

		return this;
	}

	/**
	 * Sets to generate possible {@code null} for nullable columns.
	 *
	 * <p>
	 *
	 * Default is not to generate {@code null}.
	 *
	 * @param enabled Whether or not to generate possible {@code null}
	 *
	 * @return The data setting itself
	 *
	 * @see #generateNull(int)
	 */
	public DataSetting generateNull(boolean enabled)
	{
		this.generateNull = enabled;
		return this;
	}

	/**
	 * Enables null value generating and sets the dice sides to generate possible {@code null}
	 * for nullable columns, by ratio of <em>{@code 1/diceSides}</em>.
	 *
	 * <p>
	 *
	 * Default sides of dice is {@value DefaultSetting#DEFAULT_DICE_SIDES}.
	 *
	 * @param diceSides The sides of dice to generate possible {@code null}
	 *
	 * @return The data setting itself
	 *
	 * @see #generateNull(boolean)
	 */
	public DataSetting generateNull(int diceSides)
	{
		Validate.isTrue(diceSides >= 2, "Sides of dice must be greater than or equal to 2");

		this.diceSides = diceSides;
		this.generateNull = true;

		return this;
	}

	/**
	 * Excludes columns matched by given {@link ColumnMatcher} from auto-generating.
	 *
	 * This exclusion has highest priority than auto-generating by other settings.
	 *
	 * @param matcher The matcher to match columns
	 *
	 * @return The data setting itself
	 *
	 * @see #givenType(JDBCType)
	 * @see #columnMatcher(ColumnMatcher)
	 */
	public DataSetting excludeWhen(ColumnMatcher matcher)
	{
		this.exclusion = matcher;
		return this;
	}

	/**
	 * Sets the length for types of {@code CLOB}, {@code LONGVARCHAR}, etc.
	 *
	 * @param length The fixed length of large text
	 *
	 * @return The data setting itself
	 *
	 * @see #largeTextLength(int, int)
	 */
	public DataSetting largeTextLength(int length)
	{
		return largeTextLength(length, length);
	}

	/**
	 * Sets the length range for types of {@code CLOB}, {@code LONGVARCHAR}, etc.
	 *
	 * @param minLength The minimum length of large text
	 * @param maxLength The maximum length of large text
	 *
	 * @return The data setting itself
	 *
	 * @see #largeTextLength(int)
	 */
	public DataSetting largeTextLength(int minLength, int maxLength)
	{
		Validate.isTrue(minLength >= 0, "Minimum length of large text must not be negative");
		Validate.isTrue(maxLength >= minLength,
			"Maximum length of large text[%d] must be greater than or equal to minimum length[%d]",
			maxLength, minLength
		);

		minLengthOfLargeText = minLength;
		maxLengthOfLargeText = maxLength;

		var newTextSpec = Instancio.gen().string().alphaNumeric()
			.length(minLengthOfLargeText, maxLengthOfLargeText);

		this.<String>givenType(LONGVARCHAR).useSupplier(newTextSpec);
		this.<String>givenType(CLOB).useSupplier(newTextSpec);
		this.<String>givenType(LONGNVARCHAR).useSupplier(newTextSpec);
		this.<String>givenType(NCLOB).useSupplier(newTextSpec);

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<Supplier<T>> resolveSupplier(ColumnMeta columnMeta)
	{
		Validate.notNull(columnMeta, "Column metadata must not be null");

		/*
		 * By column matcher
		 */
		for (var entry: matcherConfigMap.entrySet()) {
			if (entry.getKey().test(columnMeta)) {
				SupplierDecider<T> supplierDecider = (SupplierDecider<T>)entry.getValue();
				var matchedSupplier = supplierDecider.apply(columnMeta);
				Validate.notNull(matchedSupplier,
					"Value supplier resolved by column matcher[%s] must not be null",
					entry.getKey()
				);

				logger.debug("Found supplier for column(matcher): {}", columnMeta);

				return buildSupplier(columnMeta, matchedSupplier);
			}
		}
		// :~)

		/*
		 * By type name
		 */
		var typeName = columnMeta.typeName().toUpperCase();
		if (typeNameConfigMap.containsKey(typeName)) {
			SupplierDecider<T> supplierDecider = (SupplierDecider<T>)typeNameConfigMap.get(typeName);
			logger.debug("Found supplier for column(type name): {}", columnMeta);

			return buildSupplier(columnMeta, supplierDecider.apply(columnMeta));
		}
		// :~)

		/*
		 * By JDBC type
		 */
		var jdbcType = columnMeta.jdbcType();
		if (jdbcTypeConfigMap.containsKey(jdbcType)) {
			SupplierDecider<T> supplierDecider = (SupplierDecider<T>)jdbcTypeConfigMap.get(jdbcType);
			logger.debug("Found supplier for column(JDBCType): {}", columnMeta);

			return buildSupplier(columnMeta, supplierDecider.apply(columnMeta));
		}
		// :~)

		if (notSupportedJdbcTypes.contains(columnMeta.jdbcType())) {
			logger.debug("No supplier for column (not supported JDBCType): {}", columnMeta);
			return (Optional<Supplier<T>>)(Optional<?>)NULL_SUPPLIER_OPT;
		}

		return Optional.empty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoGenerating(ColumnMeta column)
	{
		if (exclusion.test(column)) {
			return false;
		}

		for (var matcher: matcherConfigMap.keySet()) {
			if (matcher.test(column)) {
				return true;
			}
		}

		if (!GeneratingUtils.checkAutoGenerating(column, autoGeneratingByProperties)) {
			return false;
		}

		if (typeNameConfigMap.containsKey(column.typeName())) {
			return true;
		}

		if (jdbcTypeConfigMap.containsKey(column.jdbcType())) {
			return true;
		}

		return !notSupportedJdbcTypes.contains(column.jdbcType());
	}

	DataSetting notSupportedJdbcTypes(Set<JDBCType> jdbcTypes)
	{
		notSupportedJdbcTypes = jdbcTypes;
		return this;
	}

	/**
	 * Builds a supplier which may generate {@code null} value based on current setting.
	 */
	private <T> Optional<Supplier<T>> buildSupplier(ColumnMeta column, Supplier<T> baseSupplier)
	{
		if (generateNull && isNullableColumn(column)) {
			logger.debug("[GENERATE NULL][1/{}] For column: {}", diceSides, column);
			return Optional.of(Suppliers.rollingSupplier(baseSupplier, diceSides));
		} else {
			return Optional.of(baseSupplier);
		}
	}

	private static boolean isNullableColumn(ColumnMeta columnMeta)
	{
		return columnMeta.properties().contains(ColumnMeta.Property.NULLABLE);
	}
}

@SuppressWarnings("unchecked")
class ColumnConfigImpl<T> implements ColumnConfig<T, DataSetting> {
	SupplierDecider<?> supplierDecider;

	private final DataSetting dataSetting;
	private final Consumer<SupplierDecider<T>> finalStageSetter;

	ColumnConfigImpl(DataSetting setting, Consumer<SupplierDecider<T>> finalStageSetter)
	{
		this.dataSetting = setting;
		this.finalStageSetter = finalStageSetter;
	}

	@Override
	public DataSetting useSpec(Supplier<ValueSpec<? extends T>> valueSpecSupplier)
	{
		var baseSpecSupplier = (Supplier<ValueSpec<T>>)(Object)valueSpecSupplier;
		SupplierDecider<T> decider = columnMeta -> baseSpecSupplier.get();
		finalStageSetter.accept(decider);

		return dataSetting;
	}

	@Override
	public DataSetting useSupplier(Supplier<? extends T> valueSupplier)
	{
		var baseSupplier = (Supplier<T>)valueSupplier;
		SupplierDecider<T> decider = columnMeta -> baseSupplier;
		finalStageSetter.accept(decider);

		return dataSetting;
	}

	@Override
	public DataSetting decideSupplier(SupplierDecider<? extends T> supplierDecider)
	{
		var baseDecider = (SupplierDecider<T>)supplierDecider;
		finalStageSetter.accept(columnMeta -> baseDecider.apply(columnMeta));

		return dataSetting;
	}
}
