package guru.mikelue.foxglove.examples;

import java.sql.JDBCType;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.ColumnMeta.Property;
import guru.mikelue.foxglove.functional.Int4SequenceSupplier;
import guru.mikelue.foxglove.functional.Suppliers;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.setting.DataSetting;
import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

import static guru.mikelue.foxglove.test.SampleSchema.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TableFacetTest extends AbstractJdbcTestBase {
	private final static int RANDOM_SIZE = gen().ints().range(5, 15).get();

	public TableFacetTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown()
	{
		deleteAll(TABLE_CAR_FEATURE, TABLE_CAR, TABLE_CAR_ARCHIVED);
	}

	/**
	 * Example of basic usage of Foxglove.
	 */
	@Test
	void basicUsage()
	{
		// tag::basicUsage[]
		// Generates 4 rows with "cr_brand "fixed to "Toyota" and
		// 4 different values on "cr_model"
		var facet = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(4)
			.column("cr_brand")
				.fixed("Toyota")
			.column("cr_model")
				.roundRobin("Corolla", "Camry", "RAV4", "Prius")
			.build();

		new JdbcDataGenerator(getDataSource())
			.generate(facet);
		// end::basicUsage[]

		assertNumberOfRows(TABLE_CAR, "cr_brand = 'Toyota'")
			.isEqualTo(4);
	}

	/**
	 * Example of versatile configuration of a table.
	 */
	@Test
	void versatileConfiguration()
	{
		// tag::versatileConfiguration[]
		var randomNumber = gen().ints().range(1000, 9999);

		var setting = new DataSetting()
			// Generates past date-time or JDBCType of "TIMESTAMP_WITH_TIMEZONE
			.givenType(JDBCType.TIMESTAMP_WITH_TIMEZONE)
				.useSupplier(
					gen().temporal().zonedDateTime()
						.past()
				)
			// Customizes text pattern for any column with "model" in its name
			.columnMatcher(
				columnMeta -> columnMeta.name().contains("model")
			)
				.useSupplier(gen().text().pattern("Model-#C#C#d#d"));

		var facet = JdbcTableFacet.builder(TABLE_CAR)
			// Uses the setting on this table
			.withSetting(setting)
			.numberOfRows(RANDOM_SIZE)
			// Round robin among several brands
			.column("cr_brand")
				.roundRobin("Toyota", "Honda", "Ford", "BMW", "Audi")
			// Generates year between 2015 and 2025
			.column("cr_year")
				.useSpec(() -> gen().shorts().range((short)2015, (short)2025))
			// Random choice for number of seats
			.column("cr_seats")
				.useSpec(() -> gen().oneOf(2, 4, 5, 7))
			// Color with nullable property get nullable values
			.<String>column("cr_color")
				.decideSupplier(columnMeta -> {
					var colorGenerator = gen().oneOf("Red", "Blue", "Green");

					if (columnMeta.properties().contains(Property.NULLABLE)) {
						return colorGenerator.nullable();
					}

					return colorGenerator;
				})
			// Fixed value for status column
			.column("cr_status")
				.fixed(null)
			// Includes these two columns by auto-generating their values
			.includeColumns("cr_license_plate", "cr_daily_rate", "cr_created_at", "cr_model")
			// Alter the values for "cr_license_plate" and "cr_daily_rate"
			.onTupleGenerated(tuple -> {
				tuple.setValue(
					"cr_license_plate",
					tuple.getValue("cr_brand") + "-" + randomNumber.get()
				);

				var dailyRate = switch (tuple.<String>getValue("cr_brand")) {
					case "Toyota", "Honda" -> 0.01;
					case "Ford" -> 0.02;
					default -> 0.03;
				};
				tuple.setValue("cr_daily_rate", dailyRate);
			})
			.build();
		// end::versatileConfiguration[]

		new JdbcDataGenerator(getDataSource())
			.generate(facet);
	}

	/**
	 * Example of modify column values after data get generated.
	 */
	@Test
	void onTupleGenerated()
	{
		var randomNumber = gen().ints().range(1000, 9999);

		// tag::onTupleGenerated[]
		var facet = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(RANDOM_SIZE)
			.column("cr_brand")
				.roundRobin("Toyota", "Honda", "Ford", "BMW", "Audi")
			// Sets the value of "cr_license_plate" by combination of "cr_brand" and a random number
			.onTupleGenerated(tuple -> tuple.setValue(
				"cr_license_plate",
				tuple.getValue("cr_brand") + "-" + randomNumber.get()
			))
			.build();
		// end::onTupleGenerated[]

		new JdbcDataGenerator(getDataSource())
			.generate(facet);

		assertNumberOfRows(
			TABLE_CAR, " cr_license_plate LIKE CONCAT(cr_brand, '%')"
		)
			.isEqualTo(RANDOM_SIZE);
	}

	/**
	 * Example for setting on {@link JdbcTableFacet}.
	 */
	@Test
	void setting()
	{
		final String sampleText = gen().text().word().get();
		var suffixSupplier = gen().text().pattern("#C#C#d#d");

		// tag::settingOnTable[]
		var setting = new DataSetting()
			// Excludes any column with JDBCType of OTHER
			.excludeWhen(columnMeta -> columnMeta.jdbcType() == JDBCType.OTHER)
			// For any column with type of "VARCHAR", using the supplier
			.givenType(JDBCType.VARCHAR)
				.useSupplier(() -> sampleText + suffixSupplier.get())
			// For not-nullable SMALLINT columns, generating values between 2010 and 2020
			.columnMatcher(
				columnMeta -> columnMeta.jdbcType() == JDBCType.SMALLINT &&
					!columnMeta.properties().contains(Property.NULLABLE)
			)
				.useSupplier(gen().shorts().range((short)2010, (short)2020)::get);

		var facet = JdbcTableFacet.builder(TABLE_CAR)
			// Uses the setting on the whole table
			.withSetting(setting)
			.numberOfRows(RANDOM_SIZE)
			.build();
		// end::settingOnTable[]

		new JdbcDataGenerator(getDataSource())
			.generate(facet);

		assertNumberOfRows(
			TABLE_CAR,
			" cr_brand LIKE '" + sampleText + "%' AND" +
			" cr_year BETWEEN 2010 AND 2020"
		)
			.isEqualTo(RANDOM_SIZE);
	}

	/**
	 * Example for inclusion of a table.
	 */
	@Test
	void inclusion()
	{
		// tag::includeColumns[]
		var tableFacet = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(RANDOM_SIZE)
			// Must put all of the columns which are not-nullable, no-default value.
			.includeColumns("cr_license_plate", "cr_brand", "cr_model", "cr_year")
			.build();
		// end::includeColumns[]

		new JdbcDataGenerator(getDataSource())
			.generate(tableFacet);
	}

	/**
	 * Example for exclusion of a table.
	 */
	@Test
	void exclusion()
	{
		// tag::excludeColumns[]
		var tableFacet = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(RANDOM_SIZE)
			// These columns would not be generated by Foxglove
			.excludeColumns("cr_seats", "cr_color", "cr_status", "cr_created_at", "cr_updated_at")
			.build();
		// end::excludeColumns[]

		new JdbcDataGenerator(getDataSource())
			.generate(tableFacet);
	}

	@Nested
	class NumberOfRowsTest {
		/**
		 * Example for fixed number of rows to be generated.
		 */
		@Test
		void numberOfRows()
		{
			// tag::numberOfRows[]
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(RANDOM_SIZE)
				.build();
			// end::numberOfRows[]

			var generatedNumber = dataGenerator()
				.generate(facet);

			assertThat(generatedNumber)
				.isEqualTo(RANDOM_SIZE);
		}

		/**
		 * Example for bound range of a key column.
		 */
		@Test
		void keyColumn()
		{
			// tag::keyColumn[]
			// The range of cr_id is between 1000 and 1000 + RANDOM_SIZE - 1
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.keyOfInt("cr_id").limit(1000, RANDOM_SIZE)
				.build();
			// end::keyColumn[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				String.format("cr_id BETWEEN %d AND %d",
					1000,
					1000 + RANDOM_SIZE - 1
				)
			)
				.isEqualTo(RANDOM_SIZE);
		}

		/**
		 * Example for Cartesian product of multiple columns.
		 */
		@Test
		void cartesianProduct()
		{
			// tag::cartesianProduct[]
			// Generates 3 (brands) * 3 (years) = 9 rows
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.cartesianProduct("cr_brand")
					.domain("Toyota", "Honda", "Ford")
				.cartesianProduct("cr_year")
					.domain(2020, 2021, 2022)
				.build();
			// end::cartesianProduct[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				" cr_brand IN ('Toyota', 'Honda', 'Ford') AND" +
				" cr_year IN (2020, 2021, 2022)"
			)
				.isEqualTo(9);
		}

		/**
		 * Example for referencing column of another table.
		 */
		@Test
		void referencing()
		{
			// tag::referencing[]
			// Prepares parent facet
			var carFacet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(RANDOM_SIZE)
				.build();

			// Generates rows referencing to the parent facet.
			// The number of features is equal to "RANDOM_SIZE".
			var featureFacet = JdbcTableFacet.builder(TABLE_CAR_FEATURE)
				.referencing("cf_cr_id").parent(carFacet, "cr_id")
					// Every car is referenced by one feature.
					.cardinality(1)
				.column("cf_feature_name")
					.fixed("Sunroof")
				.build();
			// end::referencing[]

			dataGenerator()
				.generate(carFacet, featureFacet);

			assertNumberOfRows(
				TABLE_CAR_FEATURE,
				"cf_feature_name = 'Sunroof'"
			)
				.isEqualTo(RANDOM_SIZE);
		}

		/**
		 * Example for fixed domain.
		 */
		@Test
		void fixedDomain()
		{
			// tag::fixedDomain[]
			// Prepares list of colors
			var colors = gen().oneOf("Red", "Blue", "Green")
				.list(RANDOM_SIZE);

			var facet = JdbcTableFacet.builder(TABLE_CAR)
				// The number of rows is equal to the size of colors list
				.numberOfRows(colors.size())
				// Use round robin for these colors
				.column("cr_color").roundRobin(colors)
				.build();
			// end::fixedDomain[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				"cr_color IN ('Red', 'Blue', 'Green')"
			)
				.isEqualTo(colors.size());
		}

		/**
		 * Example for fixed domain.
		 */
		@Test
		void cartesianProductOnOneColumn()
		{
			// tag::cartesianProductOnOneColumn[]
			// Prepares list of colors
			var colors = gen().oneOf("Red", "Blue", "Green")
				.list(RANDOM_SIZE);

			var facet = JdbcTableFacet.builder(TABLE_CAR)
				// Use round robin for these colors
				.cartesianProduct("cr_color").domain(colors)
				.build();
			// end::cartesianProductOnOneColumn[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				"cr_color IN ('Red', 'Blue', 'Green')"
			)
				.isEqualTo(colors.size());
		}
	}

	@Nested
	class ColumnValueTest {
		/**
		 * Example for fixed value of a column.
		 */
		@Test
		void fixed()
		{
			var sampleColor = "red";

			// tag::fixed[]
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(RANDOM_SIZE)
				.column("cr_color").fixed(sampleColor)
				.build();
			// end::fixed[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				String.format("cr_color = '%s'", sampleColor)
			)
				.isEqualTo(RANDOM_SIZE);
		}

		/**
		 * Example for customizing random value of a column.
		 */
		@Test
		void randomValue()
		{
			// tag::randomValue[]
			// Generates random year between 2015 and 2025
			var yearSupplier = gen().ints().range(2015, 2025);
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(RANDOM_SIZE)
				.column("cr_year").useSupplier(yearSupplier)
				.build();
			// end::randomValue[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				"cr_year BETWEEN 2015 AND 2025"
			)
				.isEqualTo(RANDOM_SIZE);
		}

		/**
		 * Example for value generator by round robin.
		 */
		@Test
		void roundRobin()
		{
			// tag::roundRobin[]
			// Round robin among BMW, Audi, and Mercedes
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(RANDOM_SIZE)
				.column("cr_brand")
					.roundRobin("BMW", "Audi", "Mercedes")
				.build();
			// end::roundRobin[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				"cr_brand IN ('BMW', 'Audi', 'Mercedes')"
			)
				.isEqualTo(RANDOM_SIZE);
		}

		/**
		 * Example for value generator by sequence number.
		 */
		@Test
		void sequenceNumber()
		{
			// tag::sequenceNumber[]
			// Generates license plate like CAR-01, CAR-02, ...
			var sequence = new Int4SequenceSupplier(2, 2);
			Supplier<String> plateSupplier = () -> String.format(
				"CAR-%02d",
				sequence.getAsInt()
			);
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(RANDOM_SIZE)
				.column("cr_license_plate").useSupplier(plateSupplier)
				.build();
			// end::sequenceNumber[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				String.format(
					"cr_license_plate LIKE 'CAR-%%'",
					RANDOM_SIZE
				)
			)
				.isEqualTo(RANDOM_SIZE);
		}

		/**
		 * Example for nullable value by Instancio generator.
		 */
		@Test
		void nullByInstancio()
		{
			final int ROWS = gen().ints().range(60, 300).get();

			// tag::nullByInstancio[]
			// Generates colors(red, blue, green, or NULL) and 1/6 odds to be null
			var colorSupplier = gen().oneOf("red", "blue", "green")
				.nullable();
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(ROWS)
				.column("cr_color").useSupplier(colorSupplier)
				.build();
			// end::nullByInstancio[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				"cr_color IS NULL"
			)
				.isGreaterThan(0);
		}

		/**
		 * Example for nullable value by customizing odds.
		 */
		@Test
		void nullByCustomizedOdds()
		{
			final int ROWS = gen().ints().range(20, 100).get();

			// tag::nullByCustomizedOdds[]
			// Generates colors(red, blue, green, or NULL) and 50% chance to be null
			var colorSupplier = Suppliers.rollingSupplier(
				gen().oneOf("red", "blue", "green"), 2
			);
			var facet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(ROWS)
				.column("cr_color").useSupplier(colorSupplier)
				.build();
			// end::nullByCustomizedOdds[]

			dataGenerator().generate(facet);

			assertNumberOfRows(
				TABLE_CAR,
				"cr_color IS NULL"
			)
				.isGreaterThan(0);
		}

		/**
		 * Example for value domain from another table.
		 */
		@Test
		void from()
		{
			// tag::from[]
			// Prepares parent facet
			var carFacet = JdbcTableFacet.builder(TABLE_CAR)
				.numberOfRows(RANDOM_SIZE)
				.column("cr_color")
					.roundRobin("Red", "Blue", "Green")
				.build();

			var carArchivedFacet = JdbcTableFacet.builder(TABLE_CAR_ARCHIVED)
				.numberOfRows(10)
				// Has no effect on the number of rows
				.column("ca_color")
					.from(carFacet, "cr_color")
					// Chooses the values of cars' colors randomly
					.random()
				.build();
			// end::from[]

			dataGenerator()
				.generate(carFacet, carArchivedFacet);

			assertNumberOfRows(
				TABLE_CAR_ARCHIVED,
				"ca_color IN ('Red', 'Blue', 'Green')"
			)
				.isEqualTo(10);
		}
	}

	private JdbcDataGenerator dataGenerator()
	{
		return new JdbcDataGenerator(getDataSource());
	}
}
