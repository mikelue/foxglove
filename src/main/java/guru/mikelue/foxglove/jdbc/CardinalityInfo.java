package guru.mikelue.foxglove.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.instancio.Instancio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.functional.RoundRobinValueSupplier;

import static guru.mikelue.foxglove.functional.Suppliers.lazySupplier;

class CardinalityInfo<T> {
	private final Logger logger = LoggerFactory.getLogger(ReferenceSettingStepImpl.class);

	private final String columnName;
	private final Supplier<Integer> numberPerParent;
	private final ValueTomb valueTomb;

	private List<T> values = null;
	private int numberOfRows = -1;

	CardinalityInfo(
		int min, int max,
		JdbcTableFacet table, String columnName
	) {
		this.valueTomb = table.getValueTomb();
		this.columnName = columnName;

		if (min == max) {
			this.numberPerParent = () -> min;
		} else {
			this.numberPerParent = () -> Instancio.gen().ints().range(min, max).get();
		}
	}

	int getNumberOfRows()
	{
		init();
		return numberOfRows;
	}

	private void init()
	{
		if (this.values != null) {
			return;
		}

		var sourceValues = valueTomb.<T>getValues(columnName);
		this.values = new ArrayList<>(sourceValues.size());

		logger.debug(
			"Setting up reference values for column[{}]. Parent size[{}].",
			columnName, sourceValues.size()
		);

		/*
		 * Builds the list of values by cardinality number
		 */
		int newNumberOfRows = 0;
		for (var sourceValue: sourceValues) {
			int numberOfChildren = numberPerParent.get();

			logger.trace(
				"Put value[{}]. Size per parent[{}].",
				columnName, numberOfChildren
			);

			for (int i = 0; i < numberOfChildren; i++) {
				this.values.add(sourceValue);
			}

			newNumberOfRows += numberOfChildren;
		}

		this.numberOfRows = newNumberOfRows;
		// :~)
	}

	Supplier<T> buildLazySupplier()
	{
		return lazySupplier(
			() -> {
				init();
				return RoundRobinValueSupplier.of(values);
			}
		);
	}
}
