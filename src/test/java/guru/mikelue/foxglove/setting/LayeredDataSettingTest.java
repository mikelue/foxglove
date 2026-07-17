package guru.mikelue.foxglove.setting;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.jdbc.CustomStatementSetter;
import guru.mikelue.misc.testlib.AbstractTestBase;
import mockit.Mocked;
import mockit.Verifications;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThat;

public class LayeredDataSettingTest extends AbstractTestBase {
	private final static DataSettingInfo firstSetting = new DataSetting()
		.setDefaultNumberOfRows(34)
		.<String>givenType(JDBCType.VARCHAR)
			.useSupplier(gen().string().length(16))
		.addStatementSetter(
			meta -> meta.name().equals("col_1"),
			(stmt, index, meta, value) -> stmt.setInt(index, 30)
		);
	private final static DataSettingInfo secondSetting = new DataSetting()
		.<Integer>givenType(JDBCType.INTEGER)
			.useSupplier(gen().ints().range(1, 100))
		.<String>givenType(JDBCType.VARCHAR) // Should get overridden
			.useSupplier(gen().string().length(4))
		.addStatementSetter(
			meta -> meta.name().equals("col_1"),
			(stmt, index, meta, value) -> stmt.setInt(index, 37)
		)
		.addStatementSetter(
			meta -> meta.name().equals("col_2"),
			(stmt, index, meta, value) -> stmt.setInt(index, 42)
		);

	private DataSettingInfo testedSetting = null;

	public LayeredDataSettingTest() {}

	@BeforeEach
	void setup()
	{
		testedSetting = new LayeredDataSetting(
			firstSetting, secondSetting
		);
	}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the overriding behavior for {@link LayeredDataSetting#resolveSupplier(ColumnMeta)}.
	 */
	@Test
	void resolveSupplier()
	{
		var varcharColumnMeta = newColumnMeta(
			"cl_varchar", JDBCType.VARCHAR, 12
		);
		var intColumnMeta = newColumnMeta(
			"cl_int", JDBCType.INTEGER
		);

		/*
		 * Get overridden by 1st setting
		 */
		var testedSupplier = testedSetting
			.<String>resolveSupplier(varcharColumnMeta);

		assertThat(testedSupplier)
			.isPresent()
			.get().extracting(Supplier::get, InstanceOfAssertFactories.STRING)
			.hasSize(16); // From first setting
		// :~)

		/*
		 * Get fallback value spec from 2nd setting
		 */
		var testedFallbackSupplier = testedSetting
			.<Integer>resolveSupplier(intColumnMeta);

		assertThat(testedFallbackSupplier)
			.isPresent();
		// :~)

		/*
		 * Use the fallback setting for 2nd one
		 */
		assertThat(testedSetting.getDefaultNumberOfRows())
			.isEqualTo(34);
		// :~)
	}

	/**
	 * Tests the getting of CustomStatementSetter.
	 */
	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@CsvSource({
		"col_1, 30", // Use 1st DataSetting(overriding 2nd's statement setter)
		"col_2, 42", // Use 2nd DataSetting
		"col_3,", // Null(doesn't have statement setter of custom)
	})
	void getStatementSetter(
		String columnName,
		Integer expectedValueOfSetInt,
		@Mocked PreparedStatement mockStat
	) throws SQLException {
		var columnMeta = newColumnMeta(columnName);

		var setterAssertion = assertThat(testedSetting.getStatementSetter(columnMeta));

		if (expectedValueOfSetInt == null) {
			setterAssertion.isEmpty();
			return;
		}

		var testedSetter = (CustomStatementSetter<Integer>)setterAssertion
			.isPresent()
			.get().actual();

		testedSetter.setParameter(
			mockStat, 1, columnMeta, Integer.valueOf(-1)
		);

		/*
		 * Verifies the retrieved statement setter
		 */
		new Verifications() {{
			mockStat.setInt(1, expectedValueOfSetInt);
			times = 1;
		}};
		// :~)
	}
}
