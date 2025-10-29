package guru.mikelue.foxglove;

import java.sql.JDBCType;
import java.util.EnumSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class ColumnMetaTest extends AbstractTestBase {
	public ColumnMetaTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the {@link String} representation.
	 */
	@Test
	void testToString()
	{
		final String columnName = "ap_address";

		ColumnMeta testedInstance = new ColumnMeta(
			columnName,
			EnumSet.allOf(ColumnMeta.Property.class),
			"VARCHAR",
			JDBCType.VARCHAR,
			32, 10
		);

		getLogger().info("ColumnMeta.toString(): {}", testedInstance.toString());

		assertThat(testedInstance.toString())
			.contains(columnName)
			.contains("JDBC<VARCHAR>", "VARCHAR")
			.contains("NULLABLE", "DEFAULT_VALUE", "AUTO_INCREMENT", "GENERATED")
			.contains("32", "10");
	}
}
