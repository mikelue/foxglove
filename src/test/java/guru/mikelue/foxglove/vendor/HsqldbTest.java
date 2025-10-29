package guru.mikelue.foxglove.vendor;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.*;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.test.AbstractVendorTestBase;

@Tag("vendor-hsqldb")
public class HsqldbTest extends AbstractVendorTestBase {
	private final static int RANDOM_ROWS = gen().ints().range(5, 10).get();

	public HsqldbTest() {}

	@BeforeAll
	static void beforeAllSetup(
		@Autowired ApplicationContext appContext,
		@Autowired JdbcTemplate jdbcTemplate
	) throws IOException {
		dropTables(jdbcTemplate, "ap_types");

		build("classpath:hsqldb-types.sql", appContext);
	}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	@TableFacetsSource
	TableFacet defaultData = JdbcTableFacet.builder("ap_types")
		.numberOfRows(RANDOM_ROWS)
		.column("tp_color").fixed("green")
		.column("tp_int_array").fixed(
			new Integer[] { 1, 2, 3 }
		)
		.build();

	/**
	 * Tests the basic functionality on HSQLDB.
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
