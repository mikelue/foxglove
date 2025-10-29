package guru.mikelue.foxglove.setting;

import java.sql.JDBCType;
import java.util.Optional;
import java.util.function.Supplier;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.functional.ColumnMatcher;

/**
 * This interface defines the methods used by engine of data generating.
 */
public interface DataSettingInfo {
	/**
	 * Gets the default number of rows for generated data.
	 *
	 * <p>
	 *
	 * This number of rows is used when:
	 *
	 * <ul>
	 *   <li>No specific number of rows is defined by {@link guru.mikelue.foxglove.TableFacet}</li>
	 * </ul>
	 *
	 * The default value is {@value DefaultSetting#DEFAULT_NUMBER_OF_ROWS}.
	 *
	 * @return The default number of rows for generated data
	 */
	int getDefaultNumberOfRows();

	/**
	 * Resolves the {@link Supplier} for the given column metadata.
	 *
	 * The priority of resolution is:
	 *
	 * <ol>
	 *  <li>Column matcher configured by {@link DataSetting#columnMatcher(ColumnMatcher)}</li>
	 *  <li>Type name configured by {@link DataSetting#givenType(String)}</li>
	 *  <li>JDBC type configured by {@link DataSetting#givenType(JDBCType)}</li>
	 * </ol>
	 *
	 * @param <T> The type of value supplied by the resolved {@link Supplier}
	 * @param columnMeta The metadata of column used to resolve {@link Supplier}
	 *
	 * @return The resolved {@link Supplier} or empty
	 */
	<T> Optional<Supplier<T>> resolveSupplier(ColumnMeta columnMeta);

	/**
	 * Gets whether or not to generate value automatically
	 * by the given properties of a column.
	 *
	 * Additionally, the {@link DataSetting#excludeWhen(ColumnMatcher)} also affects this behavior.
	 *
	 * <p>
	 * The default properties to generate value automatically:
	 *
	 * <ul>
	 *   <li>{@link ColumnMeta.Property#NULLABLE}</li>
	 *   <li>{@link ColumnMeta.Property#DEFAULT_VALUE}</li>
	 * </ul>
	 *
	 * <p>
	 * The conditions used by this method will gives {@code true} for a column:
	 * <ol>
	 *   <li>Not-excluded by {@link DataSetting#excludeWhen(ColumnMatcher)}</li>
	 *   <li>Matches any customized matching by {@link DataSetting#columnMatcher(ColumnMatcher)}</li>
	 *   <li>Not-excluded by {@link ColumnMeta#properties()}</li>
	 *   <li>Matches type by {@link DataSetting#givenType(String)}</li>
	 *   <li>Matches type by {@link DataSetting#givenType(JDBCType)}</li>
	 *   <li>Not-excluded by not supported {@link JDBCType}s</li>
	 * </ol>
	 *
	 * @param column The metadata of a column
	 *
	 * @return Whether or not to generate value automatically
	 */
	boolean isAutoGenerating(ColumnMeta column);
}
