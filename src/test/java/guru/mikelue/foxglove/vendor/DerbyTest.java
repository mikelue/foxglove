package guru.mikelue.foxglove.vendor;

import java.io.IOException;
import java.sql.JDBCType;

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
	TableFacet defaultData = JdbcTableFacet.builder("ap_types")
		.withSetting(
			derbyTyping
		)
		.numberOfRows(RANDOM_ROWS)
		.column("tp_color").fixed("green")
		.build();

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
			.isEqualTo(RANDOM_ROWS);
	}
}
