package guru.mikelue.foxglove.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;

import guru.mikelue.foxglove.functional.*;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.ColumnFromStep;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet.Builder;
import guru.mikelue.foxglove.setting.ColumnConfig;

/**
 * Aggregation of steps for column setting steps.
 */
public interface ColumnSettingSteps {
	/**
	 * The step to configure column.
	 *
	 * <p>
	 * This interface supports same methods as {@link ColumnConfig}.
	 *
	 * @param <T> The type of value generated for the column
	 */
	public interface ColumnSimpleStep<T> extends ColumnConfig<T, Builder> {
		/**
		 * Uses a function that converts row index to value.
		 *
		 * @param rowIndexToValue The function that converts row index to value
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 */
		Builder forRow(RowIndexToValue<? extends T> rowIndexToValue);

		/**
		 * Uses a fixed value for the column.
		 *
		 * @param fixedValue The fixed value
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 */
		Builder fixed(T fixedValue);

		/**
		 * Configures the column to get values from another table's column.
		 *
		 * <p>
		 * This is differ from {@link JdbcTableFacet.Builder#referencing(String)} in that
		 * the number of rows is not affected by this configuration.
		 *
		 * @param referencedTable The referenced table facet
		 * @param referencedColumn The column name of referenced in referenced table
		 *
		 * @return The step to configure column from another table
		 */
		ColumnFromStep<T> from(JdbcTableFacet referencedTable, String referencedColumn);

		/**
		 * Generates data by round-robin of values.
		 *
		 * @param values The values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #roundRobin(List)
		 */
		@SuppressWarnings("unchecked")
		Builder roundRobin(T... values);

		/**
		 * Generates data by round-robin of values.
		 *
		 * @param domain The stream of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #roundRobin(List)
		 */
		@SuppressWarnings("unchecked")
		default Builder roundRobin(Stream<? extends T> domain)
		{
			Validate.notNull(domain, "Domain stream must not be null");

			return roundRobin((T[])domain.toArray());
		};

		/**
		 * Generates data by round-robin of values.
		 *
		 * @param values The list of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #roundRobin(Object...)
		 */
		@SuppressWarnings("unchecked")
		default Builder roundRobin(List<? extends T> values)
		{
			Validate.notNull(values, "Domain stream must not be null");

			return roundRobin((T[])values.toArray());
		}
	}

	/**
	 * The step to configure column from another table.
	 *
	 * @param <T> The type of value generated for the column
	 */
	public interface ColumnFromStep<T> {
		/**
		 * Transforms the domain of referenced column to another domain values.
		 *
		 * @param <V> The type to which the value of referencing column is converted
		 * @param domainConverter The function to convert stream of values
		 *
		 * @return The step to configure column from another table
		 */
		<V> ColumnFromStep<V> transformDomain(Function<? super Stream<T>, ? extends Stream<V>> domainConverter);

		/**
		 * Uses round-robin strategy to assign values from referenced column of another table.
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #random()
		 */
		Builder roundRobin();

		/**
		 * Uses random choosing over values from referenced column of another table.
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #roundRobin()
		 */
		Builder random();
	}

	/**
	 * The step to configure reference column.
	 *
	 * @param <T> The type of value generated for the column
	 */
	public interface ReferenceSettingStep<T> {
		/**
		 * The step to configure cardinality of reference.
		 */
		public interface CardinalityStep {
			/**
			 * Sets the number of rows in child table per one row in parent table.
			 *
			 * @param numberOfChildrenPerParent The number of rows in child table. At least 1.
			 *
			 * @return The builder for {@link JdbcTableFacet}
			 */
			Builder cardinality(int numberOfChildrenPerParent);

			/**
			 * Makes the number of rows is generated randomly in child table per one row in parent table.
			 *
			 * @param min The minimum number of rows in child table, At least 0.
			 * @param max The maximum number of rows in child table
			 *
			 * @return The builder for {@link JdbcTableFacet}
			 */
			Builder cardinality(int min, int max);
		}

		/**
		 * Assigns the key column explicitly.
		 *
		 * @param parentTable The parent table facet
		 * @param referencedColumn The column name of referenced in parent table
		 *
		 * @return The next step to configure reference column
		 */
		CardinalityStep parent(JdbcTableFacet parentTable, String referencedColumn);
	}

	/**
	 * The step used to limit the number of rows by
	 * bounding integral value for a column,
	 *
	 * <p>
	 * <em>This interface is not about uniqueness,
	 * is about to assign number of rows.</em>
	 *
	 * If what you need only integral sequence for a column,
	 * just use {@link Builder#column(String)} with {@link Int4SequenceSupplier}, etc.
	 *
	 * @see SequenceSupplier
	 * @see Int4SequenceSupplier
	 * @see Int8SequenceSupplier
	 */
	public interface KeyOfIntSettingStep {
		/**
		 * Sets the range of values for key column(starts with 1, step 1).
		 *
		 * @param end The end value (exclusive)
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #range(long, long)
		 * @see #limit(int)
		 */
		default Builder range(long end)
		{
			return range(1, end, 1);
		}

		/**
		 * Sets the range of values for key column(with step 1).
		 *
		 * @param start The start value (inclusive)
		 * @param end The end value (exclusive)
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #range(long, long, int)
		 * @see #limit(long, int)
		 */
		default Builder range(long start, long end)
		{
			return range(start, end, 1);
		}

		/**
		 * Sets the range of values for key column.
		 *
		 * @param start The start value (inclusive)
		 * @param end The end value (exclusive)
		 * @param step The step value
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #range(long)
		 * @see #range(long, long)
		 * @see #limit(long, int, int)
		 */
		Builder range(long start, long end, int step);

		/**
		 * Sets the limit number of values can be generated for key column(starts with 1, steps with 1).
		 *
		 * @param number The number of values can be generated
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #limit(long, int)
		 * @see #range(long)
		 */
		default Builder limit(int number)
		{
			return limit(1, number, 1);
		}

		/**
		 * Sets the limit number of values can be generated for key column(steps with 1).
		 *
		 * @param start The start value
		 * @param number The number of values can be generated
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #limit(long, int, int)
		 * @see #range(long, long)
		 */
		default Builder limit(long start, int number)
		{
			return limit(start, number, 1);
		}

		/**
		 * Sets the limit number of values can be generated for key column.
		 *
		 * @param start The start value
		 * @param number The number of values can be generated
		 * @param step The step value
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #limit(int)
		 * @see #limit(long, int)
		 * @see #range(long, long, int)
		 */
		Builder limit(long start, int number, int step);

		/**
		 * Sets the domain of numbers to be used for key column.
		 *
		 * @param domain The list of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #range(long, long, int)
		 * @see #limit(long, int, int)
		 */
		default Builder domain(Stream<Long> domain)
		{
			Validate.notNull(domain, "Domain stream must not be null");

			return domain(
				domain.mapToLong(Long::longValue)
					.toArray()
			);
		}

		/**
		 * Sets the domain of numbers to be used for key column.
		 *
		 * @param domain The list of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #range(long, long, int)
		 * @see #limit(long, int, int)
		 */
		default Builder domain(List<Long> domain)
		{
			Validate.noNullElements(domain, "Domain list must not have null element");

			return domain(
				domain.stream().mapToLong(Long::longValue)
					.toArray()
			);
		}

		/**
		 * Sets the set of number of values to be used for key column.
		 *
		 * @param domain The list of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #range(long, long, int)
		 * @see #limit(long, int, int)
		 */
		default Builder domain(LongStream domain)
		{
			Validate.notNull(domain, "Domain stream must not be null");

			return domain(domain.toArray());
		}

		/**
		 * Sets the set of number of values to be used for key column.
		 *
		 * @param domain The list of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 *
		 * @see #range(long, long, int)
		 * @see #limit(long, int, int)
		 */
		Builder domain(long[] domain);
	}

	/**
	 * The step to configure Cartesian product for a column.
	 *
	 * @param <T> The type of value generated for the column
	 */
	public interface CartesianProductSettingStep<T> {
		/**
		 * Sets the domain of values for Cartesian product.
		 *
		 * @param values The values to use
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 */
		@SuppressWarnings("unchecked")
		default Builder domain(T... values)
		{
			Validate.notEmpty(values, "Rest values must not have null element");
			Validate.noNullElements(values, "Rest values must not have null element");

			var allValues = new ArrayList<T>(values.length);
			for (var value : values) {
				allValues.add(value);
			}

			return domain(allValues);
		}

		/**
		 * Sets the domain of values for Cartesian product.
		 *
		 * @param domain The stream of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 */
		default Builder domain(Stream<? extends T> domain)
		{
			Validate.notNull(domain, "Domain stream must not be null");

			return domain(domain.toList());
		}

		/**
		 * Sets the domain of values for Cartesian product.
		 *
		 * @param values The list of values
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 */
		Builder domain(List<? extends T> values);

		/**
		 * References values from column of another table's for Cartesian product.
		 *
		 * @param referencedTable The referenced table facet
		 * @param referencedColumn The column name of referenced in referenced table
		 *
		 * @return The builder for {@link JdbcTableFacet}
		 */
		Builder referencing(JdbcTableFacet referencedTable, String referencedColumn);
	}
}
