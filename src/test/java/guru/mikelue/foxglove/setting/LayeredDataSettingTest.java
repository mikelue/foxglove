package guru.mikelue.foxglove.setting;

import java.sql.JDBCType;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import guru.mikelue.foxglove.ColumnMeta;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThat;

public class LayeredDataSettingTest extends AbstractTestBase {
	private final static DataSettingInfo firstSetting = new DataSetting()
		.setDefaultNumberOfRows(34)
		.<String>givenType(JDBCType.VARCHAR)
			.useSupplier(gen().string().length(16));
	private final static DataSettingInfo secondSetting = new DataSetting()
		.<Integer>givenType(JDBCType.INTEGER)
			.useSupplier(gen().ints().range(1, 100))
		.<String>givenType(JDBCType.VARCHAR) // Should get overridden
			.useSupplier(gen().string().length(4));

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
}
