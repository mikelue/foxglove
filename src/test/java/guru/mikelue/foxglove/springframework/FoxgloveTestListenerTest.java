package guru.mikelue.foxglove.springframework;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.DataGeneratorSource;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

import static guru.mikelue.foxglove.test.SampleSchema.TABLE_CAR;
import static org.assertj.core.api.Assertions.assertThat;

@TestExecutionListeners(
	listeners = FoxgloveTestListener.class,
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public class FoxgloveTestListenerTest extends AbstractJdbcTestBase {
	static class CleanupListener extends AbstractTestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) throws Exception
		{
			testContext.getApplicationContext().getBean(
				JdbcTemplate.class
			)
				.execute("DELETE FROM " + TABLE_CAR);
		}

		@Override
		public int getOrder()
		{
			return 4000 - 1;
		}
	}

	private final static int RANDOM_ROWS = gen()
		.ints().range(5, 10).get();

	public FoxgloveTestListenerTest() {}

	@Nested
	@TestExecutionListeners(
		listeners = FoxgloveTestListener.class,
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
	)
	class TransactionalTest {
		@BeforeEach
		void setup() {}

		@AfterEach
		void tearDown() {}

		@TableFacetsSource
		TableFacet defaultFacet()
		{
			return JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_color").fixed("blue")
				.numberOfRows(RANDOM_ROWS)
				.build();
		}

		/**
		 * Tests the transactional data generation.
		 */
		@Test
		@GenData @Transactional(propagation = Propagation.REQUIRED)
		void transactional()
		{
			assertNumberOfRows(
				TABLE_CAR, "cr_color = 'blue'"
			)
				.isEqualTo(RANDOM_ROWS);

			assertThat(TestTransaction.isFlaggedForRollback())
				.isTrue();
		}
	}

	/**
	 * Tests the resolving of {@link TableFacetsSource} and {@link DataGeneratorSource}.
	 */
	@Nested
	@ContextConfiguration(classes = {
		BasicTest.SampleConfig.class
	})
	@TestExecutionListeners(
		listeners = FoxgloveTestListener.class,
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
	)
	class BasicTest {
		@NestedTestConfiguration(EnclosingConfiguration.INHERIT)
		static class SampleConfig {
			@Primary @Bean
			DataGenerator<?> dataGenerator(
				DataSource ds
			) {
				return new JdbcDataGenerator(ds);
			}

			@Primary @Bean
			TableFacetsProvider<TableFacet> defaultTableFacets()
			{
				return (TableFacetProvider<TableFacet>) () ->
					JdbcTableFacet.builder(TABLE_CAR)
						.column("cr_color").fixed("yellow")
						.numberOfRows(RANDOM_ROWS)
						.build();
			}
		}

		/**
		 * Tests the resolving by Java reflection.
		 */
		@Test
		@GenData(
			facetsNames = { "memberDataFacet" },
			generatorName = "memberDataGenerator"
		)
		void byJavaReflection()
		{
			assertNumberOfRows(
				TABLE_CAR, "cr_color = 'red'"
			)
				.isEqualTo(RANDOM_ROWS);

			assertThat(TestTransaction.isActive())
				.isFalse();
		}

		/**
		 * Tests the default resolving by Spring managed beans.
		 */
		@Test
		@GenData
		void bySpringBeans()
		{
			assertNumberOfRows(
				TABLE_CAR, "cr_color = 'yellow'"
			)
				.isEqualTo(RANDOM_ROWS);

			assertThat(TestTransaction.isActive())
				.isFalse();
		}

		@DataGeneratorSource
		DataGenerator<?> memberDataGenerator()
		{
			return new JdbcDataGenerator(getDataSource());
		}

		/**
		 * Should not affect resolving of default {@link TableFacet}s.
		 */
		@TableFacetsSource
		TableFacet memberDataFacet()
		{
			return JdbcTableFacet.builder(TABLE_CAR)
				.column("cr_color").fixed("red")
				.numberOfRows(RANDOM_ROWS)
				.build();
		}
	}
}
