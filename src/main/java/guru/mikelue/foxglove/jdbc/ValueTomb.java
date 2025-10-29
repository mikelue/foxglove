package guru.mikelue.foxglove.jdbc;

import java.util.*;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.TupleAccessor;

import static java.util.Collections.unmodifiableList;

/**
 * Keeps the generated values defined by {@link JdbcTableFacet}.
 */
class ValueTomb {
	private final Logger logger = LoggerFactory.getLogger(ValueTomb.class);
	private final Map<String, List<Object>> keptColumnValues = new HashMap<>();
	private final Set<String> havePreserved = new HashSet<>();
	private final String tableName;

	ValueTomb(String tableName)
	{
		this.tableName = tableName;
	}

	void keepColumn(String columnName)
	{
		logger.debug("Keep column: \"{}\"", columnName);

		keptColumnValues.putIfAbsent(columnName, new ArrayList<>());
	}

	/**
	 * Preserves the proto-data from the given tuple.
	 *
	 * @param tuple The tuple to be preserved
	 *
	 * @return The column names being preserved
	 */
	Set<String> preserveProtoData(TupleAccessor tuple)
	{
		var preservedColumnNames = new HashSet<String>();

		for (var columnName : keptColumnValues.keySet()) {
			if (!tuple.hasColumn(columnName)) {
				continue;
			}

			preservedColumnNames.add(columnName);
			havePreserved.add(columnName);

			keptColumnValues.get(columnName).add(tuple.getValue(columnName));
		}

		return preservedColumnNames;
	}

	/**
	 * Preserves the auto-data from the given tuple.
	 *
	 * @param tuple The tuple to be preserved
	 *
	 * @return The column names being preserved
	 */
	Set<String> preserveAfterData(List<TupleAccessor> tuples)
	{
		var preservedColumnNames = new HashSet<String>();

		for (var columnName : keptColumnValues.keySet()) {
			for (var tuple : tuples) {
				if (havePreserved.contains(columnName) ||
					!tuple.hasColumn(columnName)) {
					continue;
				}

				preservedColumnNames.add(columnName);
				keptColumnValues.get(columnName).add(tuple.getValue(columnName));
			}
		}

		return preservedColumnNames;
	}

	@SuppressWarnings("unchecked")
	<T> List<T> getValues(String columnName)
	{
		Validate.isTrue(
			keptColumnValues.containsKey(columnName),
			"Column name [%s.%s] is not being kept", tableName, columnName
		);

		var values = keptColumnValues.get(columnName);
		Validate.isTrue(
			values.size() > 0,
			"Empty for column [%s]. Do you generate referencing table before referent table[%s]?",
			columnName, tableName
		);

		return unmodifiableList(
			(List<T>)keptColumnValues.get(columnName)
		);
	}

	@Override
	public String toString()
	{
		var builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

		keptColumnValues.forEach((columnName, values) -> {
			builder.append(columnName, values.size());
		});

		return builder.toString();
	}
}
