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

@Tag("vendor-mssql")
public class MssqlTest extends AbstractVendorTestBase {
	private final static int RANDOM_ROWS = gen().ints().range(5, 10).get();

	public MssqlTest() {}

	@BeforeAll
	static void beforeAllSetup(
		@Autowired ApplicationContext appContext,
		@Autowired JdbcTemplate jdbcTemplate
	) throws IOException {
		dropTables(jdbcTemplate, "ap_types");

		build("classpath:mssql-types.sql", appContext);
	}

	@TableFacetsSource
	TableFacet[] defaultData = new JdbcTableFacet[] {
		// Uses java.time.Instant as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(Instant.now())
			.column("tp_time").fixed(Instant.now())
			.column("tp_datetime").fixed(Instant.now())
			.column("tp_datetime2").fixed(Instant.now())
			.column("tp_datetimeoffset").fixed(Instant.now())
			.column("tp_smalldatetime").fixed(Instant.now())
			.build(),
		// Uses java.time.OffsetXXX as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(OffsetDateTime.now())
			.column("tp_time").fixed(OffsetTime.now())
			.column("tp_datetime").fixed(OffsetDateTime.now())
			.column("tp_datetime2").fixed(OffsetDateTime.now())
			.column("tp_datetimeoffset").fixed(OffsetDateTime.now())
			.column("tp_smalldatetime").fixed(OffsetDateTime.now())
			.build(),
		// Uses java.time.ZonedDateTime as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(ZonedDateTime.now())
			.column("tp_time").fixed(ZonedDateTime.now())
			.column("tp_datetime").fixed(ZonedDateTime.now())
			.column("tp_datetime2").fixed(ZonedDateTime.now())
			.column("tp_datetimeoffset").fixed(ZonedDateTime.now())
			.column("tp_smalldatetime").fixed(ZonedDateTime.now())
			.build(),
		// Uses time types in java.sql as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(java.sql.Date.from(Instant.now()))
			.column("tp_time").fixed(java.sql.Time.from(Instant.now()))
			.column("tp_datetime").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_datetime2").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_datetimeoffset").fixed(java.sql.Timestamp.from(Instant.now()))
			.column("tp_smalldatetime").fixed(java.sql.Timestamp.from(Instant.now()))
			.build(),
		// Uses local java.time types as value for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(LocalDate.now())
			.column("tp_time").fixed(LocalTime.now())
			.column("tp_datetime").fixed(LocalDateTime.now())
			.column("tp_datetime2").fixed(LocalDateTime.now())
			.column("tp_datetimeoffset").fixed(LocalDateTime.now())
			.column("tp_smalldatetime").fixed(LocalDateTime.now())
			.build(),
		// Uses strings as values for temporal types of database
		JdbcTableFacet.builder("ap_types")
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed("2026-07-16")
			.column("tp_time").fixed("12:34:56")
			.column("tp_datetime").fixed("2026-07-16 12:34:56")
			.column("tp_datetime2").fixed("2026-07-16 12:34:56")
			.column("tp_datetimeoffset").fixed("2026-07-16 12:34:56 +00:00")
			.column("tp_smalldatetime").fixed("2026-07-16 12:34:56")
			.build(),
	};

	/**
	 * Tests basic functionality of Mysql database.
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
