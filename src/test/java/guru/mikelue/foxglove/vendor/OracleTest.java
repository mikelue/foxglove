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

@Tag("vendor-oracle")
public class OracleTest extends AbstractVendorTestBase {
	private final static int RANDOM_ROWS = gen().ints().range(5, 10).get();

	public OracleTest() {}

	@BeforeAll
	static void beforeAllSetup(
		@Autowired ApplicationContext appContext,
		@Autowired JdbcTemplate jdbcTemplate
	) throws IOException {
		dropTables(jdbcTemplate, "ap_types");

		build("classpath:oracle-types.sql", appContext);
	}

	@TableFacetsSource
	TableFacet[] defaultData = new JdbcTableFacet[] {
		// Uses java.time.Instant as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(Instant.now())
			.column("tp_timestamp").fixed(Instant.now())
			.column("tp_timestamp_tz").fixed(Instant.now())
			.column("tp_timestamp_ltz").fixed(Instant.now())
			.build(),
		// Uses java.time.OffsetXXX as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(OffsetDateTime.now())
			.column("tp_timestamp").fixed(OffsetDateTime.now())
			.column("tp_timestamp_tz").fixed(OffsetDateTime.now())
			.column("tp_timestamp_ltz").fixed(OffsetDateTime.now())
			.build(),
		// Uses java.time.ZonedDateTime as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(ZonedDateTime.now())
			.column("tp_timestamp").fixed(ZonedDateTime.now())
			.column("tp_timestamp_tz").fixed(ZonedDateTime.now())
			.column("tp_timestamp_ltz").fixed(ZonedDateTime.now())
			.build(),
		// Uses time types in java.sql as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(java.sql.Date.from(Instant.now()))
			.column("tp_timestamp").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_timestamp_tz").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_timestamp_ltz").fixed(java.sql.Timestamp.from(Instant.now()))
			.build(),
		// Uses java.time.LocalTime as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(LocalDate.now())
			.column("tp_timestamp").fixed(LocalDateTime.now())
			.column("tp_timestamp_tz").fixed(LocalDateTime.now())
			.column("tp_timestamp_ltz").fixed(LocalDateTime.now())
			.build(),
		// Uses strings as values for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed("2026-07-16 10:20:12")
			.column("tp_timestamp").fixed("2026-07-16 12:34:56")
			.column("tp_timestamp_tz").fixed("2026-07-16 12:34:56 +00:00")
			.column("tp_timestamp_ltz").fixed("2026-07-16 12:34:56 +00:00")
			.build(),
	};

	/**
	 * Tests basic functionality of Oracle database.
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
