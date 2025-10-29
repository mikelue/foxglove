package guru.mikelue.foxglove.jdbc;

import java.sql.*;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.setting.DataSetting;
import guru.mikelue.misc.testlib.AbstractTestBase;

import mockit.Expectations;
import mockit.Mocked;

import static guru.mikelue.foxglove.ColumnMeta.Property.*;
import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThat;

public class MetaUtilsTest extends AbstractTestBase {
	@Mocked
	private DatabaseMetaData mockDbMeta;
	@Mocked
	private ResultSet mockRs;

	private final static String TEST_TABLE = "ap_table";
	private final static String SAMPLE_COLUMN_NAME = "rb_any_column";

	public MetaUtilsTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the filtering of columns with {@link JdbcTableFacet.ColumnInclusionMode#Include}
	 * and {@link DataSetInfo}.
	 */
	@ParameterizedTest
	@MethodSource
	void filterColumns(
		Consumer<JdbcTableFacet.Builder> setupBuilder,
		List<String> expectedInclusions
	) {
		var builder = JdbcTableFacet.builder("kc_person");
		setupBuilder.accept(builder);
		var sampleTable = builder.build();

		var sampleSetting = new DataSetting()
			.excludeWhen(columnMeta -> columnMeta.name().equals("kc_excluded_by_setting"));

		var testedResult = MetaUtils.filterColumns(
			List.of(
				newColumnMeta("kc_included_1"),
				newColumnMeta("kc_included_2"),
				newColumnMeta("kc_excluded_1"),
				newColumnMeta("kc_excluded_2"),
				newColumnMeta("kc_excluded_by_setting")
			),
			sampleSetting, sampleTable
		);

		/*
		 * Asserts the columns that should be included
		 */
		assertThat(testedResult)
			.extracting(ColumnMeta::name)
			.containsExactlyInAnyOrderElementsOf(expectedInclusions);
		// :~)
	}
	static Stream<Arguments> filterColumns()
	{
		record TestCase(
			Consumer<JdbcTableFacet.Builder> builder,
			List<String> expectedInclusions
		) {}

		return Stream.of(
			new TestCase( // Only includes columns defined by JdbcTableFacet
				builder -> builder
					.includeColumns("kc_included_1", "kc_included_2"),
				List.of(
					"kc_included_1", "kc_included_2"
				)
			),
			new TestCase( // Only excludes columns defined by JdbcTableFacet
				builder -> builder
					.excludeColumns("kc_excluded_1", "kc_excluded_2"),
				List.of("kc_included_1", "kc_included_2")
			),
			new TestCase( // Applies setting's auto-generating
				builder -> builder
					.column("kc_included_1").fixed(20),
				List.of("kc_included_1", "kc_included_2", "kc_excluded_1", "kc_excluded_2")
			)
		)
			.map(testCase -> Arguments.of(
				testCase.builder,
				testCase.expectedInclusions
			));
	}

	/**
	 * Tests the mapping for: typeName, jdbcType, size, and decimalDigits.
	 */
	@Test
	void descriptiveMeta() throws SQLException
	{
		final String sampleTypeName = "VARCHAR";
		final int sampleColumnSize = 32;
		final int sampleDecimalDigits = 10;

		mockOneRow();

		new Expectations() {{
			mockRs.getString("TYPE_NAME");
			result = sampleTypeName;

			mockRs.getInt("DATA_TYPE");
			result = Types.VARCHAR;

			mockRs.getInt("COLUMN_SIZE");
			result = sampleColumnSize;

			mockRs.getInt("DECIMAL_DIGITS");
			result = sampleDecimalDigits;
		}};

		var testedMeta = MetaUtils.getColumnMetaList(mockDbMeta, TEST_TABLE);

		assertThat(testedMeta.get(0))
			.hasFieldOrPropertyWithValue("name", SAMPLE_COLUMN_NAME)
			.hasFieldOrPropertyWithValue("typeName", sampleTypeName)
			.hasFieldOrPropertyWithValue("jdbcType", JDBCType.VARCHAR)
			.hasFieldOrPropertyWithValue("size", sampleColumnSize)
			.hasFieldOrPropertyWithValue("decimalDigits", sampleDecimalDigits);
	}

	/**
	 * Tests the mapping for nullable property.
	 */
	@ParameterizedTest
	@CsvSource({
		"0, false", // columnNoNulls
		"1, true", // columnNullable
		"2, true" // columnNullableUnknown
	})
	void isNullable(int nullableValue, boolean expectedNullable) throws SQLException
	{
		mockOneRow();

		new Expectations() {{
			mockRs.getInt("NULLABLE");
			result = nullableValue;
		}};

		var testedMeta = MetaUtils.getColumnMetaList(mockDbMeta, TEST_TABLE);
		assertContains(testedMeta.get(0).properties(),
			NULLABLE, expectedNullable
		);
	}

	/**
	 * Tests the mapping for default value property.
	 */
	@ParameterizedTest
	@CsvSource(
		value={
			"NULL, false",
			"10, true"
		},
		nullValues={"NULL"}
	)
	void hasDefaultValue(Integer defaultValue, boolean expectedHasDefault) throws SQLException
	{
		mockOneRow();

		new Expectations() {{
			mockRs.getString("COLUMN_DEF");
			result = defaultValue;
		}};

		var testedMeta = MetaUtils.getColumnMetaList(mockDbMeta, TEST_TABLE);
		assertContains(testedMeta.get(0).properties(), DEFAULT_VALUE, expectedHasDefault);
	}

	/**
	 * Tests the mapping for auto-increment property.
	 */
	@ParameterizedTest
	@CsvSource({
		"YES, true",
		"NO, false",
		", false"
	})
	void isAutoIncrement(String isAutoIncrementValue, boolean expectedIsAutoIncrement) throws SQLException
	{
		mockOneRow();

		new Expectations() {{
			mockRs.getString("IS_AUTOINCREMENT");
			result = isAutoIncrementValue;

		}};

		var testedMeta = MetaUtils.getColumnMetaList(mockDbMeta, TEST_TABLE);
		assertContains(testedMeta.get(0).properties(),
			AUTO_INCREMENT, expectedIsAutoIncrement
		);
	}

	/**
	 * Tests the mapping for generated column property.
	 */
	@ParameterizedTest
	@CsvSource({
		"YES, true",
		"NO, false",
		", false"
	})
	void isGeneratedColumn(String isGeneratedColumnValue, boolean expectedIsGeneratedColumn) throws SQLException
	{
		mockOneRow();

		new Expectations() {{
			mockRs.getString("IS_GENERATEDCOLUMN");
			result = isGeneratedColumnValue;
		}};

		var testedMeta = MetaUtils.getColumnMetaList(mockDbMeta, TEST_TABLE);
		assertContains(testedMeta.get(0).properties(),
			GENERATED, expectedIsGeneratedColumn
		);
	}

	/**
	 * Tests the building SQL by list of columns' meta.
	 */
	@Test
	void buildInsertSql() throws SQLException
	{
		var sampleColumns = List.of(
			newColumnMeta("kc_name", JDBCType.VARCHAR),
			newColumnMeta("kc_age", JDBCType.INTEGER),
			newColumnMeta("kc_address", JDBCType.VARCHAR)
		);

		new Expectations() {{
			mockDbMeta.getIdentifierQuoteString();
			result = "`";
		}};

		var testedSql = MetaUtils.buildInsertSql(mockDbMeta, "kc_person", sampleColumns);

		getLogger().debug("Generated SQL:\n{}", testedSql);

		assertThat(testedSql)
			.contains("INSERT INTO kc_person (kc_name, kc_age, kc_address)")
			.contains("?, ?, ?");
	}

	private void mockOneRow() throws SQLException
	{
		new Expectations() {{
			mockDbMeta.getColumns(null, null, TEST_TABLE, null);
			result = mockRs;

			mockRs.next();
			returns(true, false);

			mockRs.getString("COLUMN_NAME");
			result = SAMPLE_COLUMN_NAME;
		}};
	}

	private static void assertContains(
		EnumSet<ColumnMeta.Property> testedProperties, ColumnMeta.Property checkedProperty,
		boolean expectedContains
	) {
		if (expectedContains) {
			assertThat(testedProperties)
				.contains(checkedProperty);
		} else {
			assertThat(testedProperties)
				.doesNotContain(checkedProperty);
		}
	}
}
