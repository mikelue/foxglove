package guru.mikelue.foxglove.vendor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

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
	TableFacet defaultData = JdbcTableFacet.builder("ap_types")
		.numberOfRows(RANDOM_ROWS)
		.column("tp_color").fixed("green")
		.column("tp_date").fixed(LocalDateTime.now())
		// .column("tp_date").fixed(Instant.now())
		.column("tp_timestamp").fixed(LocalDateTime.now())
		// .column("tp_timestamp").fixed(Instant.now())
		.column("tp_timestamp_tz").fixed(ZonedDateTime.now())
		// .column("tp_timestamp_tz").fixed(OffsetDateTime.now())
		.column("tp_timestamp_ltz").fixed(ZonedDateTime.now())
		// .column("tp_timestamp_ltz").fixed(OffsetDateTime.now())
		// Unsupported for now
		// .column("tp_interval_year_month").fixed(new INTERVALYM("+2-3"))
		// .column("tp_interval_day_second").fixed(new INTERVALDS("+10 10:00:00"))
		.build();

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
			.isEqualTo(RANDOM_ROWS);
	}
}
