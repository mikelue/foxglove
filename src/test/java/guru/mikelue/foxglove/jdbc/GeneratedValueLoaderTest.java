package guru.mikelue.foxglove.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import mockit.Expectations;
import mockit.Mocked;

import static org.assertj.core.api.Assertions.assertThat;

public class GeneratedValueLoaderTest extends AbstractTestBase {
	@Mocked
	private ResultSet mockRs;

	@Mocked
	private ResultSetMetaData mockMeta;

	public GeneratedValueLoaderTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the conversion to tuples.
	 */
	@Test
	void toTuples() throws SQLException
	{
		new Expectations() {{
			mockRs.getMetaData();
			result = mockMeta;

			/*
			 * Mocks the meta data
			 */
			mockMeta.getColumnCount();
			result = 2;

			mockMeta.getColumnName(anyInt);
			returns("zc_id", "zc_id2");

			mockMeta.getColumnType(anyInt);
			returns(4, 12);

			mockMeta.getColumnTypeName(anyInt);
			returns("INT", "VARCHAR");
			// :~)

			/*
			 * Mocks the data on ResultSet
			 */
			mockRs.next();
			returns(true, true, false);

			mockRs.getObject(1);
			returns(1, 2);

			mockRs.getObject(2);
			returns(4l, 5l);
			// :~)
		}};

		var testedRows = new GeneratedValueLoader(
			new String[] { "zc_id", "zc_id3" }
		).toTuples(mockRs);

		assertThat(testedRows)
			.hasSize(2);

		assertThat(testedRows)
			.extracting(row -> row.getValue("zc_id"))
			.containsExactly(1, 2);

		// Rename the column
		assertThat(testedRows)
			.extracting(row -> row.getValue("zc_id3"))
			.containsExactly(4l, 5l);
	}
}
