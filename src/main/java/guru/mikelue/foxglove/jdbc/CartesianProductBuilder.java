package guru.mikelue.foxglove.jdbc;

import java.util.*;
import java.util.function.Supplier;

import guru.mikelue.foxglove.functional.RoundRobinValueSupplier;

import static guru.mikelue.foxglove.functional.Suppliers.lazySupplier;

class CartesianProductBuilder {
	private Map<String, Supplier<List<?>>> domainProviders = new LinkedHashMap<>(2);
	// As the suppliers for expanded values
	private Map<String, List<?>> finalValues;
	private int numberOfRows = -1;

	CartesianProductBuilder() {}

	<T> void putDomain(String columnName, Supplier<List<T>> supplierOfValues)
	{
		@SuppressWarnings("unchecked")
		var newSupplier = (Supplier<List<?>>)(Supplier<?>)supplierOfValues;

		domainProviders.put(columnName, newSupplier);
	}

	int getNumberOfRows()
	{
		init();
		return numberOfRows;
	}

	boolean isExisting(String columnName)
	{
		return domainProviders.containsKey(columnName);
	}

	List<String> getColumnNames()
	{
		return new ArrayList<>(domainProviders.keySet());
	}

	@SuppressWarnings("unchecked")
	<T> Supplier<T> buildLazySupplier(String columnName)
	{
		return lazySupplier(() -> {
			init();

			var values = (List<T>)finalValues.get(columnName);
			return RoundRobinValueSupplier.of(values);
		});
	}

	private void init()
	{
		if (finalValues != null) {
			return;
		}

		finalValues = new HashMap<>(domainProviders.size());

		/*
		 * Builds the source of domain values and calculate repeating times for each column.
		 */
		Map<String, List<?>> sourceValues = new HashMap<>(domainProviders.size());
		domainProviders.entrySet().stream()
			.forEach(entry ->
				sourceValues.put(
					entry.getKey(),
					entry.getValue().get()
				)
			);

		var expandedSizesOfRestColumns = new HashMap<String, Integer>(domainProviders.size());
		numberOfRows = buildProductNumberOfRestColumns(sourceValues, expandedSizesOfRestColumns);
		// :~)

		for (var columnName: domainProviders.keySet()) {
			var sourceValuesOfCurrentColumn = sourceValues.get(columnName);
			int repeatedTimes = expandedSizesOfRestColumns.get(columnName);;

			var expandedValuesOfCurrentColumn = new ArrayList<Object>(numberOfRows);

			/*
			 * Adds all of the values for current column
			 * by pre-calculating the number of repetitions.
			 */
			for (int i = 0; i < numberOfRows;) {
				for (var v: sourceValuesOfCurrentColumn) {
					/*
					 * Adds the same number of supplierOfValues corresponding to
					 * Cartesian product of rest columns.
					 */
					for (int t = 0; t < repeatedTimes; t++) {
						expandedValuesOfCurrentColumn.add(v);
					}
					// :~)

					i += repeatedTimes;
				}
			}
			// :~)

			finalValues.put(
				columnName, expandedValuesOfCurrentColumn
			);
		}
	}

	private int buildProductNumberOfRestColumns(
		Map<String, List<?>> sourceValues, Map<String, Integer> expandedSizes
	) {
		var productNumberOfRows = 1;
		var columnNames = new ArrayList<>(sourceValues.keySet());

		/*
		 * Caches the number of rows for Cartesian product for
		 * rest of columns on every column.
		 */
		for (var i = sourceValues.size() - 1; i >= 0; i--) {
			var currentColumnName = columnNames.get(i);
			var sizeOfCurrentColumn = sourceValues.get(currentColumnName).size();
			productNumberOfRows *= sizeOfCurrentColumn;

			if (i == sourceValues.size() - 1) {
				expandedSizes.put(currentColumnName, 1);
				continue;
			}

			var nextColumnName = columnNames.get(i + 1);

			/*
			 * Calculates the number of rows for rest of columns.
			 *
			 * This is used to optimize the building of expanded supplierOfValues.
			 */
			var productSizeOfCurrentColumn = expandedSizes.get(nextColumnName) * sourceValues.get(nextColumnName).size();
			expandedSizes.put(currentColumnName, productSizeOfCurrentColumn);
			// :~)
		}

		return productNumberOfRows;
	}
}
