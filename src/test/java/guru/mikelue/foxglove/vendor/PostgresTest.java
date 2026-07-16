package guru.mikelue.foxglove.vendor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.*;

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
	TableFacet[] defaultData = new JdbcTableFacet[] {
		// Uses java.time.Instant as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_dimension").fixed(new Integer[] {1, 2, 3, 4})
			.column("tp_range").fixed("[1,10]")
			.column("tp_json_data").fixed("[2, 4, 6, 8, 10]")
			.column("tp_date").fixed(Instant.now())
			.column("tp_time").fixed(Instant.now())
			.column("tp_timetz").fixed(Instant.now())
			.column("tp_timestamp").fixed(Instant.now())
			.column("tp_timestamptz").fixed(Instant.now())
			.column("tp_interval").fixed("10 MINUTES")
			.build(),
		// Uses java.time.OffsetXXX as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_dimension").fixed(new Integer[] {1, 2, 3, 4})
			.column("tp_range").fixed("[1,10]")
			.column("tp_json_data").fixed("[2, 4, 6, 8, 10]")
			.column("tp_date").fixed(OffsetDateTime.now())
			.column("tp_time").fixed(OffsetTime.now())
			.column("tp_timetz").fixed(OffsetTime.now())
			.column("tp_timestamp").fixed(OffsetDateTime.now())
			.column("tp_timestamptz").fixed(OffsetDateTime.now())
			.column("tp_interval").fixed("10 MINUTES")
			.build(),
		// Uses java.time.ZonedDateTime as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_dimension").fixed(new Integer[] {1, 2, 3, 4})
			.column("tp_range").fixed("[1,10]")
			.column("tp_json_data").fixed("[2, 4, 6, 8, 10]")
			.column("tp_date").fixed(ZonedDateTime.now())
			.column("tp_time").fixed(ZonedDateTime.now())
			.column("tp_timetz").fixed(ZonedDateTime.now())
			.column("tp_timestamp").fixed(ZonedDateTime.now())
			.column("tp_timestamptz").fixed(ZonedDateTime.now())
			.column("tp_interval").fixed("10 MINUTES")
			.build(),
		// Uses time types in java.sql as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_dimension").fixed(new Integer[] {1, 2, 3, 4})
			.column("tp_range").fixed("[1,10]")
			.column("tp_json_data").fixed("[2, 4, 6, 8, 10]")
			.column("tp_date").fixed(java.sql.Date.from(Instant.now()))
			.column("tp_time").fixed(java.sql.Time.from(Instant.now()))
			.column("tp_timetz").fixed(java.sql.Time.from(Instant.now()))
			.column("tp_timestamp").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_timestamptz").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_interval").fixed("10 MINUTES")
			.build(),
		// Uses java.time.LocalTime as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_dimension").fixed(new Integer[] {1, 2, 3, 4})
			.column("tp_range").fixed("[1,10]")
			.column("tp_json_data").fixed("[2, 4, 6, 8, 10]")
			.column("tp_date").fixed(LocalDate.now())
			.column("tp_time").fixed(LocalTime.now())
			.column("tp_timetz").fixed(LocalTime.now())
			.column("tp_timestamp").fixed(LocalDateTime.now())
			.column("tp_timestamptz").fixed(LocalDateTime.now())
			.column("tp_interval").fixed("10 MINUTES")
			.build(),
		// Uses strings as values for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_dimension").fixed(new Integer[] {1, 2, 3, 4})
			.column("tp_range").fixed("[1,10]")
			.column("tp_json_data").fixed("[2, 4, 6, 8, 10]")
			.column("tp_date").fixed("2026-07-16")
			.column("tp_time").fixed("12:34:56")
			.column("tp_timetz").fixed("12:34:56+00")
			.column("tp_timestamp").fixed("2026-07-16 12:34:56")
			.column("tp_timestamptz").fixed("2026-07-16 12:34:56+00")
			.column("tp_interval").fixed("10 MINUTES")
			.build(),
	};

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
			.isEqualTo(RANDOM_ROWS * defaultData.length);
	}
}
