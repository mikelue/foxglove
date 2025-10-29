package guru.mikelue.foxglove.examples;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;

import static guru.mikelue.foxglove.test.SampleSchema.TABLE_CAR;
import static guru.mikelue.foxglove.test.SampleSchema.TABLE_MEMBER;;

public class JUnit5SimpleTest extends JUnit5ExtensionTestBase {
	private final static int RANDOM_ROWS = gen().ints().range(5, 10).get();

	public JUnit5SimpleTest() {}

	// tag::tableFacetFields[]
	// Use direct object of TableFacet
	@TableFacetsSource
	TableFacet carsOfRed = JdbcTableFacet.builder(TABLE_CAR)
		.column("cr_color").fixed("red")
		.numberOfRows(RANDOM_ROWS).build();

	// Use provider of TableFacet
	@TableFacetsSource
	TableFacetProvider<TableFacet> membersInTaipei = () ->
		JdbcTableFacet.builder(TABLE_MEMBER)
			.column("mb_address").fixed("Taipei, Taiwan 1234 Main St")
			.numberOfRows(RANDOM_ROWS).build();

	@Test
	@GenData(facetsNames = { "carsOfRed", "membersInTaipei" })
	void byFields()
	{
	// end::tableFacetFields[]
		assertNumberOfRows(
			TABLE_CAR,
			"cr_color = 'red'"
		)
			.isEqualTo(RANDOM_ROWS);

		assertNumberOfRows(
			TABLE_MEMBER,
			"mb_address = 'Taipei, Taiwan 1234 Main St'"
		)
			.isEqualTo(RANDOM_ROWS);
	}

	// tag::tableFacetMethods[]
	@TableFacetsSource
	TableFacet carsOfBlue()
	{
		return JdbcTableFacet.builder(TABLE_CAR)
			.column("cr_color").fixed("blue")
			.numberOfRows(RANDOM_ROWS)
			.build();
	}

	@TableFacetsSource
	TableFacet membersInNewYork()
	{
		return JdbcTableFacet.builder(TABLE_MEMBER)
			.column("mb_address").fixed("New York, NY 1234 Main St")
			.numberOfRows(RANDOM_ROWS)
			.build();
	}

	@Test
	@GenData(facetsNames = { "carsOfBlue", "membersInNewYork" })
	void byMethods()
	{
	// end::tableFacetMethods[]
		assertNumberOfRows(
			TABLE_CAR,
			"cr_color = 'blue'"
		)
			.isEqualTo(RANDOM_ROWS);

		assertNumberOfRows(
			TABLE_MEMBER,
			"mb_address = 'New York, NY 1234 Main St'"
		)
			.isEqualTo(RANDOM_ROWS);
	}

	// tag::tableFacetProviderClass[]
	static class CarsInSilver implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_color").fixed("silver")
				.numberOfRows(RANDOM_ROWS)
				.build();
		}
	}

	@Test
	@GenData({ CarsInSilver.class })
	void byProviderClass()
	{
	// end::tableFacetProviderClass[]
		assertNumberOfRows(
			TABLE_CAR,
			"cr_color = 'silver'"
		)
			.isEqualTo(RANDOM_ROWS);
	}

	// tag::tableFacetsOfArray[]
	@TableFacetsSource
	TableFacet[] carsOfArray()
	{
		return new TableFacet[] {
			JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_color").fixed("coral")
				.numberOfRows(RANDOM_ROWS)
				.build(),
			JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_model").fixed("GK-001")
				.numberOfRows(RANDOM_ROWS)
				.build()
		};
	}
	@Test
	@GenData(facetsNames = { "carsOfArray" })
	void byArrayOfTableFacet()
	{
	// end::tableFacetsOfArray[]
		assertNumberOfRows(
			TABLE_CAR,
			"cr_color = 'coral'"
		)
			.isEqualTo(RANDOM_ROWS);
		assertNumberOfRows(
			TABLE_CAR,
			"cr_model = 'GK-001'"
		)
			.isEqualTo(RANDOM_ROWS);
	}

	// tag::tableFacetsOfList[]
	@TableFacetsSource
	List<TableFacet> carsOfList()
	{
		return List.of(
			JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_color").fixed("Crimson")
				.numberOfRows(RANDOM_ROWS)
				.build(),
			JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_model").fixed("LK-002")
				.numberOfRows(RANDOM_ROWS)
				.build()
		);
	}
	@Test
	@GenData(facetsNames = { "carsOfList" })
	void byListOfTableFacet()
	{
	// end::tableFacetsOfList[]
		assertNumberOfRows(
			TABLE_CAR,
			"cr_color = 'Crimson'"
		)
			.isEqualTo(RANDOM_ROWS);
		assertNumberOfRows(
			TABLE_CAR,
			"cr_model = 'LK-002'"
		)
			.isEqualTo(RANDOM_ROWS);
	}

	// tag::tableFacetsOfStream[]
	@TableFacetsSource
	Stream<TableFacet> carsOfStream()
	{
		return Stream.of(
			JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_color").fixed("BlueViolet")
				.numberOfRows(RANDOM_ROWS)
				.build(),
			JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_model").fixed("GN-003")
				.numberOfRows(RANDOM_ROWS)
				.build()
		);
	}
	@Test
	@GenData(facetsNames = { "carsOfStream" })
	void byStreamOfTableFacet()
	{
	// end::tableFacetsOfStream[]
		assertNumberOfRows(
			TABLE_CAR,
			"cr_color = 'BlueViolet'"
		)
			.isEqualTo(RANDOM_ROWS);
		assertNumberOfRows(
			TABLE_CAR,
			"cr_model = 'GN-003'"
		)
			.isEqualTo(RANDOM_ROWS);
	}

	// tag::useTableFacetOfEnclosingClass[]
	@TableFacetsSource
	TableFacet carsOfGreen = JdbcTableFacet.builder(TABLE_CAR)
		.column("cr_color").fixed("green")
		.numberOfRows(RANDOM_ROWS).build();

	@Nested
	class NestedTest {
		@Test
		@GenData(facetsNames = { "carsOfGreen" })
		void nestedByFields()
		{
			assertNumberOfRows(
				TABLE_CAR,
				"cr_color = 'green'"
			)
				.isEqualTo(RANDOM_ROWS);
		}
	}
	// end::useTableFacetOfEnclosingClass[]

	// tag::tableFacetWithName[]
	@TableFacetsSource("carsOfRainbow")
	TableFacet carsOfSomeColors = JdbcTableFacet.builder(TABLE_CAR)
			.column("cr_color")
				.roundRobin(
					"Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet"
				)
			.numberOfRows(RANDOM_ROWS)
			.build();
	@Test
	@GenData(facetsNames = { "carsOfRainbow" })
	void byTableFacetWithName()
	{
	// end::tableFacetWithName[]
		assertNumberOfRows(
			TABLE_CAR,
			"cr_color IN ('Red', 'Orange', 'Yellow', 'Green', 'Blue', 'Indigo', 'Violet')"
		)
			.isEqualTo(RANDOM_ROWS);
	}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown()
	{

		deleteAll(TABLE_CAR, TABLE_MEMBER);
	}
}
