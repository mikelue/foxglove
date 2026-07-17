package guru.mikelue.foxglove.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.setting.DataSetting;
import mockit.Mocked;
import mockit.Verifications;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;

public class WorkerAssistanceMockTest {
	@Mocked
	private PreparedStatement mockStat;

	private WorkerAssistance testedAssistance;

	@BeforeEach
	void setup()
	{
		var dataSetting = new DataSetting()
			.addStatementSetter(
				meta -> meta.name().equals("col_3"),
				(stmt, index, meta, value) -> stmt.setInt(index, (Integer)value)
			);

		testedAssistance = new WorkerAssistance(
			mockStat,
			new GeneratedValueLoader(new String[0]),
			values -> {}, dataSetting
		);
	}

	public WorkerAssistanceMockTest() {}

	/**
	 * Tests the setting of params with usage of custom parameter setter.
	 */
	@Test
	void setParams() throws SQLException
	{
		var sampleParams = new LinkedHashMap<ColumnMeta, Object>();
		sampleParams.put(
			newColumnMeta("col_1", "varchar", JDBCType.VARCHAR), "str_v1"
		);
		sampleParams.put(
			newColumnMeta("col_2", "bigint", JDBCType.BIGINT), null
		);
		sampleParams.put(
			newColumnMeta("col_3", "int", JDBCType.INTEGER), 1900
		);

		testedAssistance.setParams(sampleParams);

		new Verifications() {{
			/*
			 * By smart setter
			 */
			mockStat.setObject(1, "str_v1", JDBCType.VARCHAR.getVendorTypeNumber());
			times = 1;
			// :~)

			/*
			 * Null value
			 */
			mockStat.setNull(2, JDBCType.BIGINT.getVendorTypeNumber());
			times = 1;
			// :~)

			/*
			 * Custom setter
			 */
			mockStat.setInt(3, 1900);
			times = 1;
			// :~)
		}};
	}
}
