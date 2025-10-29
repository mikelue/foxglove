package guru.mikelue.foxglove.jdbc;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.foxglove.TupleAccessor;
import guru.mikelue.foxglove.functional.SupplierDecider;
import guru.mikelue.foxglove.setting.DataSetting;
import guru.mikelue.misc.testlib.AbstractTestBase;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class JdbcTableFacetTest extends AbstractTestBase {
	/**
	 * Tests the column for referencing another table.
	 */
	@Test
	void referencing()
	{
		final int numberOfParents = gen().ints().range(3, 6).get();
		final int numberOfChildrenPerParent = gen().ints().range(2, 4).get();

		var parentTable = JdbcTableFacet.builder("parent_table")
			.keyOfInt("pt_id").limit(10, numberOfParents)
			.build();

		var childTable = JdbcTableFacet.builder("child_table")
			.referencing("ch_pt_id")
				.parent(parentTable, "pt_id")
				.cardinality(numberOfChildrenPerParent)
			.build();

		/*
		 * Simulates the id values in parent table
						currentMeta.properties(),
		 */
		var columnMetaOfParentId = newColumnMeta(
			"pt_id", JDBCType.INTEGER
		);
		var tupleSchema = new TupleAccessorImpl.TupleSchema(
			List.of(columnMetaOfParentId)
		);

		IntStream.range(0, numberOfParents)
			.<TupleAccessor>mapToObj(i -> tupleSchema.createTupleAccessor(
				Map.of(columnMetaOfParentId, 10 + i), i
			))
			.forEach(parentTable.getValueTomb()::preserveProtoData);
		// :~)

		/*
		 * Asserts the expected number of rows in child table
		 */
		assertThat(childTable.getNumberOfRows())
			.isEqualTo(numberOfParents * numberOfChildrenPerParent);
		// :~)

		var testedSupplier = childTable.resolveSupplier(
			newColumnMeta("ch_pt_id", JDBCType.INTEGER)
		)
			.get();

		/*
		 * Asserts teh values of referencing column
		 */
		assertThat(
			Stream.generate(testedSupplier)
				.limit(childTable.getNumberOfRows())
		)
			.containsExactly(
				IntStream.range(0, numberOfParents)
					.map(i -> 10 + i)
					.flatMap(i ->
						IntStream.range(0, numberOfChildrenPerParent)
							.map(j -> i)
					)
					.boxed()
					.toArray()
			);
		// :~)
	}

	/**
	 * Tests the building for number of rows.
	 */
	@ParameterizedTest
	@MethodSource
	void getNumberOfRows(
		Consumer<JdbcTableFacet.Builder> builderSetup,
		int expectedNumberOfRows
	) {
		var builder = JdbcTableFacet.builder("TEST_TABLE");
		builderSetup.accept(builder);

		var tableFacet = builder.build();

		assertThat(tableFacet.tableName())
			.isEqualTo("TEST_TABLE");

		assertThat(tableFacet.getNumberOfRows())
			.isEqualTo(expectedNumberOfRows);

	}

	static Stream<Arguments> getNumberOfRows()
	{
		record TestCase(
			Consumer<JdbcTableFacet.Builder> builderSetup,
			int expectedNumberOfRows
		) {};

		return Stream.of(
			// Builder's numberOfRows take precedence
			new TestCase(
				builder -> builder.numberOfRows(15)
					.withSetting(
						new DataSetting().setDefaultNumberOfRows(20)
					),
				15
			),
			// Setting's defaultNumberOfRows is used
			new TestCase(
				builder -> builder
					.withSetting(
						new DataSetting().setDefaultNumberOfRows(20)
					),
				20
			),
			// Use global setting
			new TestCase(
				builder -> {},
				DataSetting.defaults().getDefaultNumberOfRows()
			),
			// By key column
			new TestCase(
				builder -> builder
					.keyOfInt("tt_id").range(1, 100),
				99
			),
			// By cartesian product
			new TestCase(
				builder -> builder
					.cartesianProduct("tt_weight")
						.domain(10, 20)
					.cartesianProduct("tt_color")
						.domain("GREEN", "RED"),
				4
			)
		)
			.map(c -> arguments(
				c.builderSetup(),
				c.expectedNumberOfRows()
			));
	}

	@Nested
	class BuilderTest {
		/**
		 * Tests the conflicting for number of rows.
		 */
		@ParameterizedTest
		@MethodSource
		void conflictNumberOfRows(
			Consumer<JdbcTableFacet.Builder> builderSetup
		) {
			assertThatThrownBy(() -> {
				var builder = JdbcTableFacet.builder("TEST_TABLE");

				try {
					builderSetup.accept(builder);
				} catch (Throwable e) {
					getLogger().debug("Column conflict: {}", e.getMessage());
					throw e;
				}
			})
				.isInstanceOf(IllegalArgumentException.class);
		}
		static Stream<Consumer<JdbcTableFacet.Builder>> conflictNumberOfRows()
		{
			return Stream.<Consumer<JdbcTableFacet.Builder>>of(
				// Has defined by numberOfRows()
				builder -> builder
					.numberOfRows(10)
					.keyOfInt("key_1").range(1, 20),
				// Has defined by numberOfRows()
				builder -> builder
					.numberOfRows(10)
					.cartesianProduct("cp_1").domain(1, 2, 3),
				// Has defined by key column
				builder -> builder
					.keyOfInt("key_1").range(1, 20)
					.numberOfRows(10),
				// Has defined by Cartesian product
				builder -> builder
					.cartesianProduct("cp_1").domain(1, 2, 3)
					.numberOfRows(10),
				// Has defined by Cartesian product
				builder -> builder
					.cartesianProduct("cp_1").domain(1, 2, 3)
					.keyOfInt("cp_1").range(1, 10)
			);
		}

		/**
		 * Tests the conflicting settings for columns.
		 */
		@ParameterizedTest
		@MethodSource
		void conflictColumns(
			Consumer<JdbcTableFacet.Builder> builderSetup
		) {
			assertThatThrownBy(() -> {
				var builder = JdbcTableFacet.builder("TEST_TABLE");

				try {
					builderSetup.accept(builder);
				} catch (Throwable e) {
					getLogger().debug("Column conflict: {}", e.getMessage());
					throw e;
				}
			})
				.isInstanceOf(IllegalArgumentException.class);
		}
		static Stream<Consumer<JdbcTableFacet.Builder>> conflictColumns()
		{
			return Stream.<Consumer<JdbcTableFacet.Builder>>of(
				// Defines same columns
				builder -> builder
					.column("same_1").fixed("A")
					.column("same_1").fixed("B"),
				// Includes same columns
				builder -> builder
					.includeColumns("in_1").includeColumns("in_1"),
				// Excludes same columns
				builder -> builder
					.excludeColumns("ex_1").excludeColumns("ex_1"),
				// Include/Exclude at same time
				builder -> builder
					.includeColumns("in_ex_1").excludeColumns("in_ex_2"),
				// Includes a column with SupplierDecider
				builder -> builder
					.column("su_1").fixed(10)
					.includeColumns("su_1"),
				// Defines a column with included
				builder -> builder
					.includeColumns("in_1")
					.column("in_1"),
				// Excludes a column with SupplierDecider
				builder -> builder
					.column("su_1").fixed(10)
					.excludeColumns("su_1"),
				// Defines a column with excluded
				builder -> builder
					.excludeColumns("ex_1")
					.column("ex_1").fixed(100),
				// Conflicts with key
				builder -> builder
					.keyOfInt("key_1").range(1, 10)
					.column("key_1").fixed(10),
				// Conflicts with Cartesian product
				builder -> builder
					.cartesianProduct("cp_1").domain(10, 20)
					.column("cp_1").fixed(100)
			);
		}

		/**
		 * Tests the checking for columns with viable {@link SupplierDecider}.
		 */
		@ParameterizedTest
		@CsvSource({
			"in_1,Include",
			"in_2,NotSet",
		})
		void supplierDecider(
			String columnName, ColumnInclusionMode expectedMode
		) {
			var tableFacet = JdbcTableFacet.builder("TEST_TABLE")
				.column("in_1")
					.roundRobin(10, 20, 30)
				.build();

			var columnMeta = newColumnMeta(columnName);

			assertThat(tableFacet.getColumnInclusion(columnMeta))
				.isEqualTo(expectedMode);
		}

		/**
		 * Tests the key column generator.
		 */
		@ParameterizedTest
		@MethodSource
		void keyOfInt(
			Consumer<JdbcTableFacet.Builder> builderSetup,
			long[] expectedValues
		) {
			var builder = JdbcTableFacet.builder("TEST_TABLE");
			builderSetup.accept(builder);

			var tableFacet = builder.build();

			/*
			 * Generates the values to be tested
			 */
			assertThat(tableFacet.getNumberOfRows())
				.isEqualTo(expectedValues.length);

			var testedSupplier = tableFacet.<Long>resolveSupplier(
				newColumnMeta("key_col", JDBCType.INTEGER)
			).get();

			var testedValues = new long[expectedValues.length];
			for (int i = 0; i < expectedValues.length; i++) {
				testedValues[i] = testedSupplier.get();
			}
			// :~)

			assertThat(testedValues)
				.isEqualTo(expectedValues);
		}
		static Stream<Arguments> keyOfInt()
		{
			record TestCase(
				Consumer<JdbcTableFacet.Builder> builderSetup,
				long[] expectedValues
			) {};

			return Stream.<TestCase>of(
				new TestCase(
					builder -> builder
						.keyOfInt("key_col").range(1, 6),
					new long[] {1, 2, 3, 4, 5}
				),
				new TestCase(
					builder -> builder
						.keyOfInt("key_col").limit(10, 5),
					new long[] {10, 11, 12, 13, 14}
				)
			)
				.map(c -> arguments(
					c.builderSetup(),
					c.expectedValues()
				));
		}

		/**
		 * Tests the supplier for Cartesian product.
		 */
		@ParameterizedTest
		@MethodSource
		void cartesianProduct(
			Consumer<JdbcTableFacet.Builder> builderSetup,
			Map<String, List<?>> expectedValues
		) {
			var builder = JdbcTableFacet.builder("TEST_TABLE");
			builderSetup.accept(builder);

			var tableFacet = builder.build();
			tableFacet.getNumberOfRows(); // Triggers the generation

			assertCartesianProduct(
				tableFacet, expectedValues
			);
		}
		static Stream<Arguments> cartesianProduct()
		{
			record TestCase(
				Consumer<JdbcTableFacet.Builder> builderSetup,
				Map<String, List<?>> expectedValues
			) {};

			return Stream.<TestCase>of(
				// Single column
				new TestCase(
					builder -> builder
						.cartesianProduct("cp_1").domain(11, 12, 13),
					Map.of("cp_1", List.of(11, 12, 13))
				),
				// Complex case
				new TestCase(
					builder -> builder
						.cartesianProduct("cp_1")
							.domain(1, 2)
						.cartesianProduct("cp_2")
							.domain("A", "B"),
					Map.of(
						"cp_1", List.of(1, 1, 2, 2),
						"cp_2", List.of("A", "B", "A", "B")
					)
				)
			)
				.map(c -> arguments(
					c.builderSetup(),
					c.expectedValues()
				));
		}

		/**
		 * Tests the Cartesian product by referencing other tables.
		 */
		@Test
		void cartesianProductByReferencing()
		{
			var referentTableA = JdbcTableFacet.builder("rt_a")
				.keyOfInt("ra_id").limit(100, 3)
				.build();
			var referentTableB = JdbcTableFacet.builder("rt_b")
				.keyOfInt("rb_id").limit(70, 3)
				.build();

			var expectedValues = Map.<String, List<?>>of(
				"tt_ra_id", List.of(100, 100, 100, 101, 101, 101, 102, 102, 102),
				"tt_rb_id", List.of(70, 71, 72, 70, 71, 72, 70, 71, 72)
			);

			/*
			 * Prepares data of ids in referent tables
			 */
			var tableFacet = JdbcTableFacet.builder("TEST_TABLE")
				.cartesianProduct("tt_ra_id")
					.referencing(referentTableA, "ra_id")
				.cartesianProduct("tt_rb_id")
					.referencing(referentTableB, "rb_id")
				.build();

			TombTestUtils.setupTomb(
				referentTableA.getValueTomb(), "ra_id",
				100, 101, 102
			);
			TombTestUtils.setupTomb(
				referentTableB.getValueTomb(), "rb_id",
				70, 71, 72
			);

			tableFacet.getNumberOfRows(); // Triggers the generation
			// :~)

			assertCartesianProduct(
				tableFacet, expectedValues
			);
		}

		private static void assertCartesianProduct(
			JdbcTableFacet testedTable,
			Map<String, List<?>> expectedValues
		) {
			/*
			 * Asserts values of every columns expanded by Cartesian product
			 */
			for (var entry: expectedValues.entrySet()) {
				var columnName = entry.getKey();
				var expectedColumnValues = entry.getValue();

				/*
				 * Generates the values to be tested
				 */
				var columnMeta = newColumnMeta(columnName);
				var testedSupplier = testedTable.<Object>resolveSupplier(columnMeta).get();

				var testedColumnValues = new ArrayList<Object>(expectedColumnValues.size());
				for (int i = 0; i < expectedColumnValues.size(); i++) {
					testedColumnValues.add(testedSupplier.get());
				}
				// :~)

				/*
				 * Asserts the values for a column
				 */
				assertThat(testedColumnValues)
					.as("For column: %s", columnName)
					.isEqualTo(expectedColumnValues);
				// :~)
			}
			// :~)
		}
		/**
		 * Tests the checking for inclusion of columns.
		 */
		@ParameterizedTest
		@CsvSource({
			"in_1,Include",
			"in_2,Include",
			"out_1,Exclude",
		})
		void includeColumns(
			String columnName, ColumnInclusionMode expectedMode
		) {
			var tableFacet = JdbcTableFacet.builder("TEST_TABLE")
				.includeColumns("in_1", "in_2")
				.build();

			var columnMeta = newColumnMeta(columnName);

			assertThat(tableFacet.getColumnInclusion(columnMeta))
				.isEqualTo(expectedMode);
		}

		/**
		 * Tests the checking for exclusion of columns.
		 */
		@ParameterizedTest
		@CsvSource({
			"in_1,NotSet", // included by default
			"out_2,Exclude",
			"out_1,Exclude",
		})
		void excludeColumns(
			String columnName, ColumnInclusionMode expectedMode
		) {
			var tableFacet = JdbcTableFacet.builder("TEST_TABLE")
				.excludeColumns("out_1", "out_2")
				.build();

			var columnMeta = newColumnMeta(columnName);

			assertThat(tableFacet.getColumnInclusion(columnMeta))
				.isEqualTo(expectedMode);
		}

		/**
		 * Tests the supplier converting row index to value.
		 */
		@Test
		void forRow()
		{
			final int TIMES = 5;

			var tableFacet = JdbcTableFacet.builder("TEST_TABLE")
				.column("int_value")
					.forRow(i -> i * 2)
				.build();

			var columnMeta = newColumnMeta("int_value");

			var testedValue = new ArrayList<Integer>(TIMES);
			var testedSupplier = tableFacet.<Integer>resolveSupplier(columnMeta).get();

			for (int i = 0; i < TIMES; i++) {
				testedValue.add(testedSupplier.get());
			}

			assertThat(testedValue)
				.isEqualTo(List.of(0, 2, 4, 6, 8));
		}

		/**
		 * Tests the modification after row generating.
		 */
		@Test
		void onTupleGenerated()
		{
			var tableFacet = JdbcTableFacet.builder("TEST_TABLE")
				.column("tb_value")
					.fixed(1)
				.includeColumns("tb_value2")
				.onTupleGenerated(tuple -> {
					var baseValue = tuple.<Integer>getValue("tb_value");
					tuple.setValue("tb_value2", baseValue + 10);
				})
				.build();

			var testedValues = List.of(1, -1);
			var sampleMetaOfColumns = List.of(
				newColumnMeta("tb_value"),
				newColumnMeta("tb_value2")
			);
			var tupleSchema = new TupleAccessorImpl.TupleSchema(
				sampleMetaOfColumns
			);
			var sampleTuple = tupleSchema.createTupleAccessor(
				Map.of(
					sampleMetaOfColumns.get(0), testedValues.get(0),
					sampleMetaOfColumns.get(1), testedValues.get(1)
				), 0
			);

			tableFacet.getHandlerOfTuple()
				.accept(sampleTuple);

			assertThat(sampleTuple.<Integer>getValue("tb_value2"))
				.isEqualTo(11);
		}
	}
}
