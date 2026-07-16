package guru.mikelue.foxglove.vendor;

import java.io.IOException;
import java.sql.JDBCType;
import java.time.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.setting.DataSetting;
import guru.mikelue.foxglove.test.AbstractVendorTestBase;

@Tag("vendor-derby")
public class DerbyTest extends AbstractVendorTestBase {
	private final static int RANDOM_ROWS = gen().ints().range(5, 10).get();

	public DerbyTest() {}

	@BeforeAll
	static void beforeAllSetup(
		@Autowired ApplicationContext appContext,
		@Autowired JdbcTemplate jdbcTemplate
	) throws IOException {
		build("classpath:derby-types.sql", appContext);
	}

	@AfterAll
	static void afterAllTearDown(
		@Autowired ApplicationContext appContext,
		@Autowired JdbcTemplate jdbcTemplate
	) throws IOException {
		JdbcTestUtils.dropTables(jdbcTemplate, "ap_types");
	}

	private DataSetting derbyTyping = new DataSetting()
		.givenType(JDBCType.TIMESTAMP)
			.useSupplier(() -> gen().temporal().timestamp().get());

	@TableFacetsSource
	TableFacet[] defaultData = new JdbcTableFacet[] {
		// Uses java.time.Instant as values for Derby temporal types
		JdbcTableFacet.builder("ap_types")
			.withSetting(derbyTyping)
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(Instant.now())
			.column("tp_time").fixed(Instant.now())
			.column("tp_timestamp").fixed(Instant.now())
			.build(),
		// Uses java.time.OffsetXXX as values for Derby temporal types
		JdbcTableFacet.builder("ap_types")
			.withSetting(derbyTyping)
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(OffsetDateTime.now())
			.column("tp_time").fixed(OffsetTime.now())
			.column("tp_timestamp").fixed(OffsetDateTime.now())
			.build(),
		// Uses java.time.ZonedDateTime as values for Derby temporal types
		JdbcTableFacet.builder("ap_types")
			.withSetting(derbyTyping)
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(ZonedDateTime.now())
			.column("tp_time").fixed(ZonedDateTime.now())
			.column("tp_timestamp").fixed(ZonedDateTime.now())
			.build(),
		// Uses java.sql temporal types as values for Derby temporal types
		JdbcTableFacet.builder("ap_types")
			.withSetting(derbyTyping)
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(java.sql.Date.from(Instant.now()))
			.column("tp_time").fixed(java.sql.Time.from(Instant.now()))
			.column("tp_timestamp").fixed(java.sql.Timestamp.from(Instant.now()))
			.build(),
		// Uses java.time.LocalXXX values for Derby temporal types
		JdbcTableFacet.builder("ap_types")
			.withSetting(derbyTyping)
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed(LocalDate.now())
			.column("tp_time").fixed(LocalTime.now())
			.column("tp_timestamp").fixed(LocalDateTime.now())
			.build(),
		// Uses string values for Derby temporal types
		JdbcTableFacet.builder("ap_types")
			.withSetting(derbyTyping)
			.numberOfRows(RANDOM_ROWS)
			.column("tp_color").fixed("green")
			.column("tp_date").fixed("2026-07-16")
			.column("tp_time").fixed("12:34:56")
			.column("tp_timestamp").fixed("2026-07-16 12:34:56")
			.build(),
	};

	/**
	 * Tests basic functionality of Derby database.
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
