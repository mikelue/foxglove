package guru.mikelue.foxglove.jdbc;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import guru.mikelue.foxglove.setting.DataSetting;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static java.sql.JDBCType.INTEGER;
import static java.sql.JDBCType.VARCHAR;
import static org.assertj.core.api.Assertions.assertThat;

public class RowParamsGeneratorTest extends AbstractTestBase {
	public RowParamsGeneratorTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the cases of resolving suppliers provided by {@link JdbcTableFacet} or {@link DataSetting}.
	 */
	@Test
	void resolveSupplier()
	{
		var settings = new DataSetting()
			.givenType(VARCHAR)
				.useSupplier(() -> "v2")
			.givenType(INTEGER)
				.useSupplier(() -> 40);

		var tableFacet = JdbcTableFacet.builder("any_table")
			.column("st_col2").fixed("v1")
			.build();

		var sampleMetaOfColumns = List.of(
			// Overridden by table facet
			newColumnMeta("st_col2"),
			// Resolved by setting
			newColumnMeta("st_col1", INTEGER),
			// Resolved by setting
			newColumnMeta("st_col3")
		);

		var testedRow= new RowParamsGenerator(
			tableFacet, sampleMetaOfColumns, settings
		)
			.generateRowParams();

		/*
		 * Ensures the sequence of columns in the row(map key is ordered)
		 */
		assertThat(testedRow.values())
			.containsExactly("v1", 40, "v2");
		// :~)
	}
}
