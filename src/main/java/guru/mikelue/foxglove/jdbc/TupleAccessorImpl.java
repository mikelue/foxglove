package guru.mikelue.foxglove.jdbc;

import java.util.*;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.TupleAccessor;

import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

class TupleAccessorImpl implements TupleAccessor {
	/**
	 * Cached mapping between column name and its metadata.
	 */
	static class TupleSchema {
		private final Map<String, ColumnMeta> nameMappingOfColumns;

		TupleSchema(Collection<ColumnMeta> columns)
		{
			var newMap = new HashMap<String, ColumnMeta>(columns.size());

			for (var columnMeta: columns) {
				newMap.put(columnMeta.name().toLowerCase(), columnMeta);
			}

			nameMappingOfColumns = unmodifiableMap(newMap);
		}

		TupleAccessorImpl createTupleAccessor(
			Map<ColumnMeta, Object> tuple,
			int tupleIndex
		) {
			return new TupleAccessorImpl(this, tuple, tupleIndex);
		}
	}

	private final Map<String, Object> nameMappingOfValues;
	private final Map<String, ColumnMeta> nameMappingOfColumns;
	private final int tupleIndex;

	private TupleAccessorImpl(
		TupleSchema tupleSchema, Map<ColumnMeta, Object> tuple,
		int tupleIndex
	) {
		this.tupleIndex = tupleIndex;

		nameMappingOfValues = new LinkedHashMap<String, Object>(tuple.size());
		nameMappingOfColumns = tupleSchema.nameMappingOfColumns;

		for (var entry: tuple.entrySet()) {
			nameMappingOfValues.put(
				entry.getKey().name().toLowerCase(),
				entry.getValue()
			);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(String name)
	{
		var safeName = trimToEmpty(name).toLowerCase();

		Validate.notEmpty(safeName, "Column name cannot be null or empty");
		Validate.isTrue(
			nameMappingOfValues.containsKey(safeName),
			"Column name [%s] is not existing", safeName
		);

		return (T)nameMappingOfValues.get(safeName);
	}

	@Override
	public <T> void setValue(String name, T value)
	{
		var safeName = trimToEmpty(name).toLowerCase();

		Validate.notEmpty(safeName, "Column name cannot be null or empty");
		Validate.isTrue(
			nameMappingOfValues.containsKey(safeName),
			"Column name [%s] is not existing", safeName
		);

		nameMappingOfValues.put(safeName, value);
	}

	@Override
	public List<ColumnMeta> getMetaOfColumns()
	{
		return Collections.unmodifiableList(
			new ArrayList<>(nameMappingOfColumns.values())
		);
	}

	@Override
	public Map<ColumnMeta, Object> asMap()
	{
		var newMap = new LinkedHashMap<ColumnMeta, Object>(nameMappingOfValues.size());
		for (var columnMeta: nameMappingOfValues.keySet()) {
			newMap.put(
				nameMappingOfColumns.get(columnMeta),
				nameMappingOfValues.get(columnMeta)
			);
		}

		return unmodifiableMap(newMap);
	}

	@Override
	public int index()
	{
		return tupleIndex;
	}

	@Override
	public boolean hasColumn(String name)
	{
		var safeName = trimToEmpty(name).toLowerCase();

		Validate.notEmpty(safeName, "Column name cannot be null or empty");
		return nameMappingOfColumns.containsKey(safeName);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, NO_CLASS_NAME_STYLE)
			.append("columns",
				nameMappingOfValues.keySet().stream()
					.map(columnName -> {
						var value = nameMappingOfValues.get(columnName);
						var meta = nameMappingOfColumns.get(columnName);
						return "\"" + meta.name() + "\": " + (value != null ? value.toString() : "null");
					})
					.toList()
			)
			.append("index", tupleIndex)
			.toString();
	}
}
