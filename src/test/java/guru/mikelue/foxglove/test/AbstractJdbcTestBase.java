package guru.mikelue.foxglove.test;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import org.assertj.core.api.IntegerAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static guru.mikelue.foxglove.test.SampleSchema.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(classes = JdbcTestConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AbstractJdbcTestBase extends AbstractTestBase {
	private final static Logger logger = LoggerFactory.getLogger(AbstractJdbcTestBase.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource ds;

	@BeforeAll
	static void globalSetup(ApplicationContext context) throws IOException
	{
		var jdbcTemplate = context.getBean(JdbcTemplate.class);

		var vendor = System.getProperty("database.vendor");

		switch (vendor) {
			case "org.h2.Driver":
				dropTables(
					jdbcTemplate,
					TABLE_DATA_TYPES, TABLE_RENT, TABLE_CAR_FEATURE, TABLE_CAR, TABLE_MEMBER,
					TABLE_CAR_ARCHIVED
				);

				build("classpath:data-types.sql", context);
				build("classpath:car-renting.sql", context);
				break;
		}
	}

	@AfterAll
	static void globalTeardown(ApplicationContext context) { }

	public DataSource getDataSource()
	{
		return ds;
	}

	public JdbcTemplate getJdbcTemplate()
	{
		return jdbcTemplate;
	}

	public void deleteAll(String... tableNames)
	{
		for (var otherTableName: tableNames) {
			jdbcTemplate.execute(
				String.format("DELETE FROM %s", otherTableName)
			);
		}
	}

	public int getNumberOfRows(String tableName)
	{
		return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
	}

	public int getNumberOfRows(String tableName, String whereClause)
	{
		return JdbcTestUtils.countRowsInTableWhere(
			jdbcTemplate, tableName, whereClause
		);
	}

	public IntegerAssert assertNumberOfRows(String tableName)
	{
		return new IntegerAssert(getNumberOfRows(tableName));
	}

	public IntegerAssert assertNumberOfRows(String tableName, String whereClause)
	{
		return new IntegerAssert(getNumberOfRows(tableName, whereClause));
	}

	protected static void dropTables(JdbcTemplate jdbcTemplate, String... tableNames)
	{
		for (var tableName: tableNames) {
			jdbcTemplate.execute(
				String.format("DROP TABLE IF EXISTS %s", tableName)
			);
		}
	}

	protected static void build(String resourcePath, ApplicationContext appContext) throws IOException
	{
		logger.debug("Loading resource: \"{}\"", resourcePath);

		var ddl = StreamUtils.copyToString(
			appContext.getResource(resourcePath)
				.getInputStream(),
			UTF_8
		);

		var jdbcTemplate = appContext.getBean(JdbcTemplate.class);

		jdbcTemplate.execute(ddl);
	}
}

@Configuration
class JdbcTestConfig {}
