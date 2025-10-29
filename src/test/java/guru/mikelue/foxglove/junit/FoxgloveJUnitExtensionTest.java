package guru.mikelue.foxglove.junit;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.DataGeneratorSource;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

import static guru.mikelue.foxglove.test.SampleSchema.TABLE_CAR;

@ExtendWith(FoxgloveJUnitExtension.class)
public class FoxgloveJUnitExtensionTest extends AbstractJdbcTestBase {
	private final static int RANDOM_ROWS = gen()
		.ints().range(10, 20).get();

	public FoxgloveJUnitExtensionTest() {}

	@BeforeAll
	static void globalSetup(
		@Autowired
		DataSource ds
	) {
		OnTypedProviders.StaticInnerDataGeneratorProvider
			.injectDataSource(ds);
	}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown()
	{
		deleteAll(TABLE_CAR);
	}

	/**
	 * Tests the usage of class-level providers of table facets and data generator.
	 *
	 * The preparing of data should be done only once for all test methods in this class.
	 */
	@Nested
	@GenData
	class OnClassLevel {
		@Test
		void onClassLevel_1()
		{
			assertNumberOfRows(TABLE_CAR, "cr_color = 'red'")
				.isEqualTo(RANDOM_ROWS);
		}

		@Test
		void onClassLevel_2()
		{
			assertNumberOfRows(TABLE_CAR, "cr_color = 'red'")
				.isEqualTo(RANDOM_ROWS);
		}

		@TableFacetsSource
		JdbcTableFacet defaultTableFacets()
		{
			return newTableFacet();
		}

		@DataGeneratorSource
		JdbcDataGenerator defaultDataGenerator()
		{
			return new JdbcDataGenerator(getDataSource());
		}
	}

	/**
	 * Tests the default providers of table facets and data generator.
	 */
	@Nested
	class OnDefaultSources {
		/**
		 * Tests the referencing to default sources.
		 */
		@Test
		@GenData
		void defaultSources()
		{
			assertNumberOfRows(TABLE_CAR, "cr_color = 'red'")
				.isEqualTo(RANDOM_ROWS);
		}

		/**
		 * Tests the no needed of data generation.
		 */
		@Test
		void nothing() {}

		@TableFacetsSource
		JdbcTableFacet defaultTableFacets()
		{
			return newTableFacet();
		}

		@DataGeneratorSource
		JdbcDataGenerator defaultDataGenerator()
		{
			return new JdbcDataGenerator(getDataSource());
		}
	}

	/**
	 * Tests the usage of typed providers of table facets and data generator.
	 */
	@Nested
	class OnTypedProviders {
		class InnerDataGeneratorProvider implements DataGeneratorProvider<TableFacet> {
			@Override
			public DataGenerator<TableFacet> get()
			{
				return newDataGenerator();
			}
		}

		class InnerTableFacetProvider implements TableFacetProvider<TableFacet> {
			@Override
			public TableFacet getOne()
			{
				return newTableFacet();
			}
		}

		static class StaticInnerDataGeneratorProvider implements DataGeneratorProvider<TableFacet> {
			private static DataGenerator<TableFacet> dataGenerator;

			@SuppressWarnings("unchecked")
			static void injectDataSource(DataSource ds)
			{
				if (dataGenerator != null) {
					return;
				}

				dataGenerator = (DataGenerator<TableFacet>)(Object)new JdbcDataGenerator(ds);
			}

			@Override
			public DataGenerator<TableFacet> get()
			{
				return dataGenerator;
			}
		}

		static class StaticInnerTableFacetProvider implements TableFacetProvider<TableFacet> {
			@Override
			public TableFacet getOne()
			{
				return newTableFacet();
			}
		}

		@Test
		@GenData(
			generator = InnerDataGeneratorProvider.class,
			facets = InnerTableFacetProvider.class
		)
		void providersOfNonStaticMember()
		{
			assertNumberOfRows(TABLE_CAR, "cr_color = 'red'")
				.isEqualTo(RANDOM_ROWS);
		}

		@Test
		@GenData(
			generator = StaticInnerDataGeneratorProvider.class,
			facets = StaticInnerTableFacetProvider.class
		)
		void providersOfStaticMember()
		{
			assertNumberOfRows(TABLE_CAR, "cr_color = 'red'")
				.isEqualTo(RANDOM_ROWS);
		}
	}

	/**
	 * Tests the usage of named providers of table facets and data generator.
	 */
	@Nested
	class OnNamedProviders {
		@Test
		@GenData(
			generatorName = "namedDataGenerator",
			facetsNames = "namedTableFacets"
		)
		void namedProviders()
		{
			assertNumberOfRows(TABLE_CAR, "cr_color = 'red'")
				.isEqualTo(RANDOM_ROWS);
		}

		@TableFacetsSource
		JdbcTableFacet namedTableFacets()
		{
			return newTableFacet();
		}

		@DataGeneratorSource
		JdbcDataGenerator namedDataGenerator()
		{
			return new JdbcDataGenerator(getDataSource());
		}
	}

	/**
	 * Tests the referencing generating sources from outer class.
	 */
	@Nested
	class OuterConfig {
		@Nested
		class InnerTest {
			/**
			 * Tests the referencing generating sources from outer class.
			 */
			@Test
			@GenData(
				value = { OuterTableFacetProvider.class },
				facetsNames = "outerTableFacets",
				generatorName = "outerDataGenerator"
			)
			void useOuterConfig()
			{
				assertNumberOfRows(TABLE_CAR, "cr_color = 'outer'")
					.isEqualTo(RANDOM_ROWS * 2);
			}
		}

		class OuterTableFacetProvider implements TableFacetProvider<TableFacet> {
			@Override
			public TableFacet getOne()
			{
				return outerTableFacets();
			}
		}

		@DataGeneratorSource
		DataGenerator<TableFacet> outerDataGenerator()
		{
			return FoxgloveJUnitExtensionTest.this.newDataGenerator();
		}

		@TableFacetsSource
		JdbcTableFacet outerTableFacets()
		{
			return JdbcTableFacet.builder("ap_car")
				.numberOfRows(RANDOM_ROWS)
				.column("cr_color").fixed("outer")
				.build();
		}
	}

	@SuppressWarnings("unchecked")
	private DataGenerator<TableFacet> newDataGenerator()
	{
		return (DataGenerator<TableFacet>)(Object)new JdbcDataGenerator(getDataSource());
	}

	private static JdbcTableFacet newTableFacet()
	{
		return JdbcTableFacet.builder("ap_car")
			.numberOfRows(RANDOM_ROWS)
			.column("cr_color").fixed("red")
			.build();
	}
}
