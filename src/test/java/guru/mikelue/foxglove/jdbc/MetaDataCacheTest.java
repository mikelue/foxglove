package guru.mikelue.foxglove.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import mockit.Expectations;
import mockit.Mocked;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetaDataCacheTest extends AbstractTestBase {
	private final static String TEST_TABLE = "ap_table";

	@Mocked
	private Connection mockConn;
	@Mocked
	private MetaUtils mockMetaUtils;

	public MetaDataCacheTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the checking of existence of column defined by {@link JdbcTableFacet} on database.
	 */
	@ParameterizedTest
	@CsvSource({
		"col_existing,col_existing,true",
		"col_existing,COL_EXISTING,true",
		"col_not_existing,col_existing,false"
	})
	void existingOnDatabase(
		String columnName, String metaColumName,
		boolean expectedExistence
	) throws SQLException
	{
		new Expectations() {{
			MetaUtils.getColumnMetaList((DatabaseMetaData)any, TEST_TABLE);
			result = List.of(newColumnMeta(
				metaColumName, JDBCType.VARCHAR
			));
		}};

		var testedMeta = new MetaDataCache(mockConn);
		var sampleTable = JdbcTableFacet.builder(TEST_TABLE)
			.includeColumns(columnName)
			.build();

		if (expectedExistence) {
			testedMeta.loadMetadata(List.of(sampleTable), mockConn);
			assertThatNoException();
		} else {
			assertThatThrownBy(() ->
				testedMeta.loadMetadata(List.of(sampleTable), mockConn)
			)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(columnName);
		}
	}
}
