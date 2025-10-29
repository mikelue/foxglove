package guru.mikelue.foxglove.examples;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.springframework.EnableFoxglove;

import static guru.mikelue.foxglove.test.SampleSchema.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

// tag::springframworkTest[]
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = SampleTestConfig.class)
@EnableFoxglove
public class SpringFrameworkTest {
	public SpringFrameworkTest() {}

	@TableFacetsSource
	TableFacet redCars = JdbcTableFacet.builder("ap_car")
		.numberOfRows(RANDOM_ROWS)
		.column("cr_color").fixed("red")
		.build();

	/**
	 * The default transaction behavior of @JdbcTest is to rollback at the end of each test.
	 */
	@Test
	@GenData(
		facetsNames = { "redCars" }
	)
	void someTest(
		@Autowired
		JdbcTemplate jdbcTemplate
	) {
		assertThat(
			JdbcTestUtils.countRowsInTableWhere(
				jdbcTemplate,
				"ap_car", "cr_color = 'red'"
			)
		)
			.isEqualTo(RANDOM_ROWS);
	}
	// end::springframworkTest[]

	final static int RANDOM_ROWS = Instancio.gen()
		.ints().range(5, 10).get();

	// tag::useBeanOfSpringFramework[]
	@Test
	@GenData(
		// Uses the bean name of Spring Framework context
		facetsNames = { "blueCars", "purpleCars", "yellowCars" }
	)
	void blueCarsTest(
		@Autowired
		JdbcTemplate jdbcTemplate
	) {
		assertThat(
			JdbcTestUtils.countRowsInTableWhere(
				jdbcTemplate,
				"ap_car", "cr_color IN ('blue', 'purple', 'yellow')"
			)
		)
			.isEqualTo(RANDOM_ROWS * 3);
	}
	// end::useBeanOfSpringFramework[]

	// tag::noRollback[]
	@AfterEach
	void tearDownForYellowCars(
		@Autowired
		JdbcTemplate jdbcTemplate
	) {
		// You have to remove the data by yourself
		JdbcTestUtils.deleteFromTableWhere(
			jdbcTemplate,
			"ap_car", "cr_color = 'yellow'"
		);
	}

	@Test
	@GenData(
		facetsNames = { "yellowCars" }
	)
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void noRollback(
		@Autowired
		JdbcTemplate jdbcTemplate
	) {
		assertThat(
			JdbcTestUtils.countRowsInTableWhere(
				jdbcTemplate,
				"ap_car", "cr_color = 'yellow'"
			)
		)
			.isEqualTo(RANDOM_ROWS);
	}
	// end::noRollback[]

	// tag::byProvider[]
	@Test
	@GenData(ToyotaCarsProvider.class)
	void byProvider(
		@Autowired
		JdbcTemplate jdbcTemplate
	) {
		assertThat(
			JdbcTestUtils.countRowsInTableWhere(
				jdbcTemplate,
				"ap_car", "cr_brand = 'Toyota'"
			)
		)
			.isEqualTo(RANDOM_ROWS);
	}
	// end::byProvider[]

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	@BeforeAll
	static void globalSetup(ApplicationContext context) throws IOException
	{
		var jdbcTemplate = context.getBean(JdbcTemplate.class);

		dropTables(
			jdbcTemplate,
			TABLE_DATA_TYPES, TABLE_RENT, TABLE_CAR_FEATURE, TABLE_CAR, TABLE_MEMBER,
			TABLE_CAR_ARCHIVED
		);

		build("classpath:data-types.sql", context);
		build("classpath:car-renting.sql", context);
	}

	private static void dropTables(JdbcTemplate jdbcTemplate, String... tableNames)
	{
		for (var tableName: tableNames) {
			jdbcTemplate.execute(
				String.format("DROP TABLE IF EXISTS %s", tableName)
			);
		}
	}

	private static void build(String resourcePath, ApplicationContext appContext) throws IOException
	{
		var ddl = StreamUtils.copyToString(
			appContext.getResource(resourcePath)
				.getInputStream(),
			UTF_8
		);

		var jdbcTemplate = appContext.getBean(JdbcTemplate.class);

		for (var stmt: ddl.split(";")) {
			jdbcTemplate.execute(stmt);
		}
	}
}

// tag::providerClass[]
class ToyotaCarsProvider implements TableFacetProvider<TableFacet> {
	@Override
	public TableFacet getOne()
	{
		return JdbcTableFacet.builder("ap_car")
			.numberOfRows(SpringFrameworkTest.RANDOM_ROWS)
			.column("cr_brand").fixed("Toyota")
			.build();
	}
}
// end::providerClass[]

// tag::contextConfiguration[]
@Configuration
class SampleTestConfig {
	@Bean
	ToyotaCarsProvider toyotaCars()
	{
		return new ToyotaCarsProvider();
	}

	@Bean @Scope("prototype")
	TableFacet blueCars()
	{
		return JdbcTableFacet.builder("ap_car")
			.numberOfRows(SpringFrameworkTest.RANDOM_ROWS)
			.column("cr_color").fixed("blue")
			.build();
	}

	@Bean
	TableFacetProvider<TableFacet> yellowCars()
	{
		return () -> JdbcTableFacet.builder("ap_car")
			.numberOfRows(SpringFrameworkTest.RANDOM_ROWS)
			.column("cr_color").fixed("yellow")
			.build();
	}

	@Bean @Scope("prototype")
	TableFacet[] purpleCars()
	{
		return new TableFacet[] {
			JdbcTableFacet.builder("ap_car")
				.numberOfRows(SpringFrameworkTest.RANDOM_ROWS)
				.column("cr_color").fixed("purple")
				.build()
		};
	}
}
// end::contextConfiguration[]
