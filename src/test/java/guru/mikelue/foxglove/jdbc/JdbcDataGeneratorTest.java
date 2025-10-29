package guru.mikelue.foxglove.jdbc;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

import static guru.mikelue.foxglove.test.SampleSchema.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class JdbcDataGeneratorTest extends AbstractJdbcTestBase {
	public JdbcDataGeneratorTest() {}

	@BeforeEach
	void setup()
	{
		deleteAll(TABLE_DATA_TYPES, TABLE_RENT, TABLE_CAR_FEATURE, TABLE_CAR, TABLE_CAR_ARCHIVED, TABLE_MEMBER);
	}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the generating of data for referencing columns.
	 */
	@ParameterizedTest
	@MethodSource
	void referencing(
		int parentRows, int childRowsPerParent,
		int expectedNumberOfChildRows
	) {
		var parentTable = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(parentRows)
			.build();
		var childTable = JdbcTableFacet.builder(TABLE_CAR_FEATURE)
			.referencing("cf_cr_id")
				.parent(parentTable, "cr_id")
				.cardinality(childRowsPerParent)
			.column("cf_feature_name")
				.forRow(rowIndex -> "Feature-" + (rowIndex + 1))
			.build();

		int testedNumber = getDataGenerator()
			.generate(parentTable, childTable);

		assertThat(testedNumber)
			.isEqualTo(expectedNumberOfChildRows + parentRows);

		assertNumberOfRows(
			TABLE_CAR_FEATURE, "cf_feature_name LIKE 'Feature-%'"
		)
			.isEqualTo(expectedNumberOfChildRows);
	}

	static Arguments[] referencing()
	{
		return new Arguments[] {
			// 3 parents, 2 children per parent
			Arguments.arguments(3, 2, 6),
			// 5 parents, 1 children per parent
			Arguments.arguments(5, 1, 5),
		};
	}

	/**
	 * Tests the value domain from another table.
	 */
	@ParameterizedTest
	@MethodSource
	void from(
		int parentRows, List<String> sampleColors
	) {
		final int numberOfRows = gen().ints().range(10, 30).get();

		/*
		 * Sets up the table facets.
		 */
		var parentTable = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(parentRows)
			.column("cr_color")
				.roundRobin(sampleColors)
			.column("cr_model")
				.fixed("GM-001")
			.build();

		var childTable = JdbcTableFacet.builder(TABLE_CAR_ARCHIVED)
			.numberOfRows(numberOfRows)
			.column("ca_color")
				.from(parentTable, "cr_color")
				.roundRobin()
			.column("ca_model")
				.from(parentTable, "cr_model")
				.random()
			.build();

		var colorNamesForQuery = sampleColors.stream()
			.map(color -> "'" + color + "'")
			.collect(Collectors.joining(", "));

		getDataGenerator()
			.generate(parentTable, childTable);

		/*
		 * Asserts the number of rows in referencing table,
		 * which has expected colors.
		 */
		assertNumberOfRows(
			TABLE_CAR_ARCHIVED,
			" ca_color IN (" + colorNamesForQuery + ") AND" +
			" ca_model = 'GM-001'"
		)
			.isEqualTo(numberOfRows);
		// :~)
	}
	static Arguments[] from()
	{
		return new Arguments[] {
			arguments(10, List.of("Purple", "Grass Green")),
			arguments(10, List.of("Red", "Green", "Blue")),
			arguments(20, List.of("Yellow", "Black", "White", "Silver")),
		};
	}

	/**
	 * Tests the generating of data.
	 */
	@ParameterizedTest
	@MethodSource
	void generate(
		Consumer<JdbcTableFacet.Builder> facetCustomizer,
		String whereCondition, int expectedNumberOfRows
	) {
		var builder = JdbcTableFacet.builder(TABLE_DATA_TYPES);
		facetCustomizer.accept(builder);
		var facet = builder.build();

		int testedNumber = getDataGenerator()
			.generate(facet);

		/*
		 * Asserts the:
		 * 1) returned value from generate
		 * 2) actual number of rows in the table
		 */
		assertThat(testedNumber)
			.isEqualTo(expectedNumberOfRows);
		assertNumberOfRows(TABLE_DATA_TYPES, whereCondition)
			.isEqualTo(expectedNumberOfRows);
		// :~)
	}

	enum SampleEnum {
		VAL_A, VAL_B, VAL_C
	}

	static Stream<Arguments> generate()
	{
		record TestCase(
			Consumer<JdbcTableFacet.Builder> facetCustomizer,
			String whereClause, int expectedNumberOfRows
		) {}

		var enumSupplier = gen().enumOf(SampleEnum.class);
		Supplier<String> enumAsTextSupplier = () -> enumSupplier.get().name();

		return Stream.<TestCase>of(
			// Default case
			new TestCase(
				builder -> builder
					.numberOfRows(10)
					.column("st_big_int")
						.fixed(null)
					.column("st_enum")
						.useSupplier(enumAsTextSupplier)
					.excludeColumns("st_double"),
				"st_big_int IS NULL AND st_double IS NULL", 10
			),
			// By key column
			new TestCase(
				builder -> builder
					.keyOfInt("st_id").limit(1000, 7),
				"st_id BETWEEN 1000 AND 1006", 7
			),
			// By Cartesian product
			new TestCase(
				builder -> builder
					.cartesianProduct("st_int")
						.domain(1, 2, 3)
					.cartesianProduct("st_varchar")
						.domain("A", "B", "C")
					.includeColumns("st_float", "st_large_text"),
				"st_int IN (1, 2, 3) AND st_varchar IN ('A', 'B', 'C')", 9
			)
		)
			.map(tc -> Arguments.arguments(
				tc.facetCustomizer(),
				tc.whereClause(),
				tc.expectedNumberOfRows()
			));
	}

	/**
	 * Tests the generating of data by Cartesian product,
	 * which the domains come from other tables.
	 */
	@ParameterizedTest
	@CsvSource({
		"1,1", "1,2", "3,2", "5,5",
	})
	void cartesianProductByReferencing(
		int numberOfRowsForCar, int numberOfRowsForMember
	) {
		var dataOfCars = JdbcTableFacet.builder(TABLE_CAR)
			.keyOfInt("cr_id")
				.limit(1000, numberOfRowsForCar)
			.build();
		var dataOfMembers = JdbcTableFacet.builder(TABLE_MEMBER)
			.keyOfInt("mb_id")
				.limit(2000, numberOfRowsForMember)
			.build();

		var rentData = JdbcTableFacet.builder(TABLE_RENT)
			.cartesianProduct("rt_cr_id")
				.referencing(dataOfCars, "cr_id")
			.cartesianProduct("rt_mb_id")
				.referencing(dataOfMembers, "mb_id")
			.build();

		getDataGenerator()
			.generate(dataOfCars, dataOfMembers, rentData);

		assertNumberOfRows(
			TABLE_RENT,
			String.format(
				"""
				rt_cr_id BETWEEN 1000 AND %d AND
				rt_mb_id BETWEEN 2000 AND %d
				""",
				1000 + numberOfRowsForCar - 1,
				2000 + numberOfRowsForMember - 1
			)
		)
			.isEqualTo(numberOfRowsForCar * numberOfRowsForMember);
	}

	private JdbcDataGenerator getDataGenerator()
	{
		return new JdbcDataGenerator(getDataSource());
	}
}
