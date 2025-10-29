package guru.mikelue.foxglove;

import java.util.List;
import java.util.Map;

/**
 * Easy to be used for auto-casting on values of a tuple.
 */
public interface TupleAccessor {
	/**
	 * Gets the value of a column with auto-casting.
	 *
	 * @param <T> Expected type of the column
	 * @param name Name of the column
	 *
	 * @return Value of the column
	 *
	 * @throws ClassCastException When the value cannot be casted to the expected type
	 * @throws IllegalArgumentException When the column name is not existing
	 *
	 * @see #setValue(String, Object)
	 */
	<T> T getValue(String name);

	/**
	 * Sets the value of a column.
	 *
	 * @param <T> Type of the value
	 * @param name Name of the column
	 * @param value Value to be set
	 *
	 * @throws IllegalArgumentException When the column name is not existing
	 *
	 * @see #getValue(String)
	 */
	<T> void setValue(String name, T value);

	/**
	 * Checks whether or not the generated row has the given column.
	 *
	 * @param name Name of the column
	 *
	 * @return true if the generated row has the given column; false otherwise
	 */
	boolean hasColumn(String name);

	/**
	 * Gets metadata of all columns of the generated row.
	 *
	 * @return List of ColumnMeta of all columns
	 */
	List<ColumnMeta> getMetaOfColumns();

	/**
	 * Gets all columns as a map.
	 *
	 * @return read-only Map with duplicated column names and their values
	 */
	Map<ColumnMeta, Object> asMap();

	/**
	 * Gets the index(starts with {@code 0}) for the {@link TableFacet}.
	 *
	 * @return Index for of generated rows on the TableFacet
	 */
	int index();
}
