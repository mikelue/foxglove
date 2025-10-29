package guru.mikelue.foxglove.examples;

import java.sql.JDBCType;

import org.junit.jupiter.api.*;

import guru.mikelue.foxglove.ColumnMeta.Property;
import guru.mikelue.foxglove.functional.ColumnMatcher;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.setting.DataSetting;
import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static guru.mikelue.foxglove.test.SampleSchema.TABLE_CAR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SettingTest extends AbstractJdbcTestBase {
	public SettingTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown()
	{
		deleteAll(TABLE_CAR);
	}

	/**
	 * Example for versatile setting.
	 */
	@Test
	void versatileSetting()
	{
		// tag::versatileSetting[]
		new DataSetting()
			// Generates 20 rows if no other row number setting on the table
			.setDefaultNumberOfRows(20)
			// Excludes columns with name starting with "audit_"
			.excludeWhen(columnMeta -> columnMeta.name().startsWith("audit_"))
			// Use column matcher
			.columnMatcher(columnMeta -> columnMeta.name().endsWith("_status"))
				// Choose one of the fixed domain values randomly
				.useSupplier(gen().oneOf("ACTIVE", "INACTIVE", "PENDING"))
			// For type name
			.givenType("enum_status")
				// Choose one of the fixed domain values randomly
				.useSupplier(gen().oneOf("ACTIVE", "INACTIVE", "UNKNOWN"))
			// For JDBCType
			.givenType(JDBCType.VARCHAR)
				// Fixed value for all VARCHAR columns
				.useSupplier(() -> "fixed-text")
			.givenType(JDBCType.INTEGER)
				// Fixed range for all INTEGER columns
				.useSpec(() -> gen().ints().range(1000, 1000000))
			// Generates 128 characters for types of TEXT, LONGVARCHAR, etc.
			.largeTextLength(128)
			// Generates null values with 5% odds for all nullable columns
			.generateNull(20);
		// end::versatileSetting[]
	}

	/**
	 * Example for setting the configuration globally.
	 */
	@Test @Disabled
	void globalSetting()
	{
		// tag::globalSetting[]
		DataSetting.defaults()
			// Generates text with 128 characters for types of TEXT, LONGVARCHAR, etc.
			.largeTextLength(128)
			// Generates null values with 10% odds for all nullable columns
			.generateNull(10);
		// end::globalSetting[]

		var testedSupplier = DataSetting.defaults()
			.resolveSupplier(
				newColumnMeta("nt_addition", JDBCType.LONGVARCHAR, Property.NULLABLE)
			)
			.get();

		await()
			.atMost(5, SECONDS)
			.untilAsserted(() -> {
				var testedText = (String)testedSupplier.get();

				if (testedText != null) {
					assertThat(testedText)
						.hasSize(128);
				}

				// null value is generated
				assertThat(testedText)
					.isNull();
			});
	}

	/**
	 * Example for setting on {@link JdbcDataGenerator}.
	 */
	@Test
	void onDataGenerator()
	{
		// tag::onDataGenerator[]
		var dataSetting = new DataSetting()
			// Wont' generate value for NULLABLE column automatically
			.notAutoGenerateFor(Property.NULLABLE);

		var dataGenerator = new JdbcDataGenerator(getDataSource());
		dataGenerator
			.withSetting(dataSetting);
		// end::onDataGenerator[]

		var sampleTable = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(10)
			.build();

		dataGenerator.generate(sampleTable);

		assertNumberOfRows(TABLE_CAR, "cr_daily_rate IS NULL")
			.isEqualTo(10);
	}

	@Nested
	class SettingFeatureTest {
		/**
		 * Example for setting default supplier by {@link JDBCType} of column.
		 */
		@Test
		void byJdbcType()
		{
			// tag::byJdbcType[]
			var dataSetting = new DataSetting()
				// Sets up fixed text for all VARCHAR columns
				.givenType(JDBCType.VARCHAR)
					.useSupplier(() -> "fixed-text");
			// end::byJdbcType[]

			var testedSupplier = dataSetting
				.resolveSupplier(
					newColumnMeta("cr_name", JDBCType.VARCHAR)
				)
				.get();

			assertThat(testedSupplier.get())
				.isEqualTo("fixed-text");
		}

		/**
		 * Example for setting default supplier by type name of column.
		 */
		@Test
		void byTypeName()
		{
			// tag::byTypeName[]
			var dataSetting = new DataSetting()
				// Sets up fixed text for columns with type name of "VARYING CHARACTERS"
				.givenType("VARYING CHARACTERS")
					.useSupplier(() -> "varying-text");
			// end::byTypeName[]

			var testedSupplier = dataSetting
				.resolveSupplier(
					newColumnMeta("cr_name", "VARYING CHARACTERS", JDBCType.VARCHAR)
				)
				.get();

			assertThat(testedSupplier.get())
				.isEqualTo("varying-text");
		}

		/**
		 * Example for setting default supplier by {@link ColumnMatcher}.
		 */
		@Test
		void byColumnMatcher()
		{
			var addressGen = gen().oneOf(
				"123 Main St", "456 Oak Ave", "789 Pine Rd"
			);

			// tag::byColumnMatcher[]
			var dataSetting = new DataSetting()
				// Sets up supplier if the name of column contains "address"
				.columnMatcher(meta -> meta.name().contains("address"))
					.useSupplier(addressGen);
			// end::byColumnMatcher[]

			var testedSupplier = dataSetting
				.resolveSupplier(
					newColumnMeta("cr_address")
				)
				.get();

			assertThat(testedSupplier.get())
				.isIn("123 Main St", "456 Oak Ave", "789 Pine Rd");
		}

		/**
		 * Example for setting exclusion by {@link ColumnMatcher}.
		 */
		@Test
		void excludeWhen()
		{
			// tag::excludeWhen[]
			var dataSetting = new DataSetting()
				// Excludes the column with name ending with "_computed"
				.excludeWhen(meta -> meta.name().endsWith("_computed"));
			// end::excludeWhen[]

			assertThat(dataSetting.isAutoGenerating(
				newColumnMeta("cr_score_computed")
			))
				.isFalse();
		}

		/**
		 * Example for setting auto-generating by properties of a column.
		 */
		@Test
		void autoGenerating()
		{
			// tag::autoGenerating[]
			var dataSetting = new DataSetting()
				// Generates value even if the column is AUTO_INCREMENT
				.autoGenerateFor(Property.AUTO_INCREMENT)
				// Won't generate value for NULLABLE columns
				.notAutoGenerateFor(Property.NULLABLE);
			// end::autoGenerating[]

			assertThat(dataSetting.isAutoGenerating(
				newColumnMeta("cr_address", Property.NULLABLE)
			))
				.isFalse();

			assertThat(dataSetting.isAutoGenerating(
				newColumnMeta("cr_address", Property.AUTO_INCREMENT)
			))
				.isTrue();
		}

		/**
		 * Example for setting generating of null values on nullable columns.
		 */
		@Test
		void nullable()
		{
			// tag::nullValue[]
			var dataSetting = new DataSetting()
				.givenType(JDBCType.VARCHAR)
					.useSupplier(() -> "not-null-value")
				// Generates null value(1/6 odds) for any nullable column
				.generateNull(true);
			// end::nullValue[]

			var testedSupplier = dataSetting
				.resolveSupplier(
					newColumnMeta("cr_address", JDBCType.VARCHAR, Property.NULLABLE)
				)
				.get();

			await()
				.atMost(5, SECONDS)
				.untilAsserted(() -> {
					assertThat(testedSupplier.get())
						.isNull();
				});
		}

		/**
		 * Example for setting generating of null values on nullable columns.
		 */
		@Test
		void customizeOddsForNullValue()
		{
			// tag::customizedOddsForNullValue[]
			var dataSetting = new DataSetting()
				.givenType(JDBCType.VARCHAR)
					.useSupplier(() -> "not-null-value")
				// Generates null value(10% odds) for any nullable column
				.generateNull(10);
			// end::customizedOddsForNullValue[]

			var testedSupplier = dataSetting
				.resolveSupplier(
					newColumnMeta("cr_address", JDBCType.VARCHAR, Property.NULLABLE)
				)
				.get();

			await()
				.atMost(5, SECONDS)
				.untilAsserted(() -> {
					assertThat(testedSupplier.get())
						.isNull();
				});
		}

		/**
		 * Example for length of large text settings.
		 */
		@Test
		void largeText()
		{
			// tag::largeText[]
			var dataSetting = new DataSetting()
				// Generates 256 characters for types of TEXT, LONGVARCHAR, etc.
				.largeTextLength(256);
			// end::largeText[]

			var testedSupplier = dataSetting
				.<String>resolveSupplier(
					newColumnMeta("cr_address", JDBCType.LONGVARCHAR)
				)
				.get();

			assertThat(testedSupplier.get())
				.hasSize(256);
		}
	}
}
