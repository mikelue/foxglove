package guru.mikelue.foxglove;

import java.sql.JDBCType;
import java.util.EnumSet;

import guru.mikelue.foxglove.ColumnMeta.Property;

public interface ColumnMetaTestUtils {
	static EnumSet<ColumnMeta.Property> newEmptyProperties()
	{
		return EnumSet.noneOf(ColumnMeta.Property.class);
	}

	static ColumnMeta newColumnMeta(
		String name, String typeName, JDBCType jdbcType
	) {
		return new ColumnMeta(
			name, newEmptyProperties(),
			typeName, jdbcType, 8, 0
		);
	}

	static ColumnMeta newColumnMeta(
		String name, JDBCType jdbcType,
		EnumSet<Property> properties
	) {
		return new ColumnMeta(
			name, properties,
			jdbcType.getName(), jdbcType,
			32, 0
		);
	}

	static ColumnMeta newColumnMeta(
		String name, JDBCType jdbcType,
		Property... properties
	) {
		var propertySet = newEmptyProperties();

		for (var property : properties) {
			propertySet.add(property);
		}

		return newColumnMeta(name, jdbcType, propertySet);
	}

	static ColumnMeta newColumnMeta(
		String name, Property... properties
	) {
		return newColumnMeta(
			name, JDBCType.VARCHAR, properties
		);
	}

	static ColumnMeta newColumnMeta(
		String columnName
	) {
		return newColumnMeta(
			columnName, JDBCType.VARCHAR
		);
	}

	static ColumnMeta newColumnMeta(
		String columnName, JDBCType jdbcType
	) {
		return newColumnMeta(columnName, jdbcType, 16);
	}

	static ColumnMeta newColumnMeta(
		String columnName, JDBCType jdbcType, int size
	) {
		return newColumnMeta(
			columnName, jdbcType, size, 0
		);
	}

	static ColumnMeta newColumnMeta(
		String columnName, JDBCType jdbcType,
		int size, int scale
	) {
		return new ColumnMeta(
			columnName, newEmptyProperties(),
			jdbcType.getName(), jdbcType, size, scale
		);
	}
}
