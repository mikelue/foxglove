package guru.mikelue.foxglove.vendor;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.test.AbstractVendorTestBase;

@Tag("vendor-postgres")
public class PostgresTest extends AbstractVendorTestBase {
	private final static int RANDOM_ROWS = gen().ints().range(10, 20).get();

	public PostgresTest() {}

	@BeforeAll
	static void beforeAllSetup(
		@Autowired ApplicationContext appContext,
		@Autowired JdbcTemplate jdbcTemplate
	) throws IOException {
		dropTables(jdbcTemplate, "ap_types");

		build("classpath:postgres-types.sql", appContext);
	}

	@TableFacetsSource
	TableFacet defaultData = JdbcTableFacet.builder("ap_types")
		.numberOfRows(RANDOM_ROWS)
		.column("tp_color").fixed("green")
		.column("tp_dimension").fixed(new Integer[] {1, 2, 3, 4})
		.column("tp_range").fixed("[1,10]")
		.column("tp_json_data").fixed("[2, 4, 6, 8, 10]")
		.build();

	/**
	 * Tests basic functionality of Postgres database.
	 */
	@Test
	@GenData @Transactional
	void basic()
	{
		assertNumberOfRows(
			"ap_types", "tp_color = 'green'"
		)
			.isEqualTo(RANDOM_ROWS);
	}
}
