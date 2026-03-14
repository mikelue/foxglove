package guru.mikelue.foxglove.functional;

import java.sql.JDBCType;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.setting.DataSetting;

/**
 * Built-in column matchers.
 *
 * @see DataSetting#columnMatcher(ColumnMatcher)
 */
public interface ColumnMatchers {
	/**
	 * Builds a matcher for matching name(case incensitive) of a column.
	 *
	 * @param columnName The column name to be checked
	 *
	 * @return A matcher for that column name
	 */
	static ColumnMatcher forName(String columnName)
	{
		return c -> c.name().equalsIgnoreCase(columnName);
	};

	/**
	 * Builds a matcher for a column that has a specific property.
	 *
	 * @param checkedProperty The property to be checked
	 *
	 * @return A matcher for a column has that property
	 */
	static ColumnMatcher hasProperty(ColumnMeta.Property checkedProperty)
	{
		return c -> c.properties().contains(checkedProperty);
	};

	/**
	 * Builds a matcher for matching a type of JDBC.
	 *
	 * @param targetType The target type of JDBC
	 *
	 * @return A matcher for that JDBC type
	 */
	static ColumnMatcher forJdbcType(JDBCType targetType)
	{
		return c -> c.jdbcType().equals(targetType);
	};

	/**
	 * Builds a matcher for matching type name(case incensitive) of {@link ColumnMeta}.
	 *
	 * @param typeName The type to be checked
	 *
	 * @return A matcher for that type name
	 */
	static ColumnMatcher forTypeName(String typeName)
	{
		return c -> c.typeName().equalsIgnoreCase(typeName);
	};
}
