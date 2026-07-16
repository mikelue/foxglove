package guru.mikelue.foxglove.vendor;

import java.io.IOException;
import java.time.*;

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
	TableFacet[] defaultData = new JdbcTableFacet[] {
		// Uses java.time.Instant as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_int_array").fixed(new Integer[] { 1, 2, 3 })
			.column("tp_date").fixed(Instant.now())
			.column("tp_time").fixed(Instant.now())
			.column("tp_time_with_time_zone").fixed(Instant.now())
			.column("tp_timestamp").fixed(Instant.now())
			.column("tp_timestamp_with_time_zone").fixed(Instant.now())
			.build(),
		// Uses java.time.OffsetXXX as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_int_array").fixed(new Integer[] { 1, 2, 3 })
			.column("tp_date").fixed(OffsetDateTime.now())
			.column("tp_time").fixed(OffsetTime.now())
			.column("tp_time_with_time_zone").fixed(OffsetTime.now())
			.column("tp_timestamp").fixed(OffsetDateTime.now())
			.column("tp_timestamp_with_time_zone").fixed(OffsetDateTime.now())
			.build(),
		// Uses java.time.ZonedDateTime as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_int_array").fixed(new Integer[] { 1, 2, 3 })
			.column("tp_date").fixed(ZonedDateTime.now())
			.column("tp_time").fixed(ZonedDateTime.now())
			.column("tp_time_with_time_zone").fixed(ZonedDateTime.now())
			.column("tp_timestamp").fixed(ZonedDateTime.now())
			.column("tp_timestamp_with_time_zone").fixed(ZonedDateTime.now())
			.build(),
		// Uses time types in java.sql as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_int_array").fixed(new Integer[] { 1, 2, 3 })
			.column("tp_date").fixed(java.sql.Date.from(Instant.now()))
			.column("tp_time").fixed(java.sql.Time.from(Instant.now()))
			.column("tp_time_with_time_zone").fixed(java.sql.Time.from(Instant.now()))
			.column("tp_timestamp").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_timestamp_with_time_zone").fixed(java.sql.Timestamp.from(Instant.now()))
			.build(),
		// Uses java.time.LocalTime as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_int_array").fixed(new Integer[] { 1, 2, 3 })
			.column("tp_date").fixed(LocalDate.now())
			.column("tp_time").fixed(LocalTime.now())
			.column("tp_time_with_time_zone").fixed(LocalTime.now())
			.column("tp_timestamp").fixed(LocalDateTime.now())
			.column("tp_timestamp_with_time_zone").fixed(LocalDateTime.now())
			.build(),
		// Uses strings as values for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_int_array").fixed(new Integer[] { 1, 2, 3 })
			.column("tp_date").fixed("2026-07-16")
			.column("tp_time").fixed("12:34:56")
			.column("tp_time_with_time_zone").fixed("12:34:56+00:00")
			.column("tp_timestamp").fixed("2026-07-16 12:34:56")
			.column("tp_timestamp_with_time_zone").fixed("2026-07-16 12:34:56+00:00")
			.build(),
	};

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
			.isEqualTo(RANDOM_ROWS * defaultData.length);
	}
}
