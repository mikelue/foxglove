package guru.mikelue.foxglove.setting;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.time.temporal.Temporal;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSettingTest extends AbstractTestBase {
	public DefaultSettingTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	private final static int timesForSpecValue = 64;

	/**
	 * Tests the generators with min value of zero.
	 */
	@ParameterizedTest
	@EnumSource(
		value=JDBCType.class,
		names={ "TINYINT", "SMALLINT", "INTEGER", "BIGINT", "FLOAT", "REAL", "DOUBLE" }
	)
	void atLeastZero(JDBCType jdbcType)
	{
		var sampleColumnMeta = newColumnMeta(
			"cl_any", jdbcType
		);

		var testedSupplierOpt = DefaultSetting.instance()
			.<Number>resolveSupplier(sampleColumnMeta);

		Consumer<Number> validator = value -> {
			assertThat(value.longValue())
				.isGreaterThanOrEqualTo(0L);
		};

		for (int i = 0; i < timesForSpecValue; ++i) {
			assertThat(testedSupplierOpt)
				.isPresent()
				.get().extracting(Supplier::get)
				.satisfies(validator);
		}
	}

	/**
	 * Tests the generators for types of characters.
	 */
	@ParameterizedTest
	@CsvSource({
		"CHAR,3,3,3",
		"CHAR,8,8,8",
		"CHAR,10,8,10",
		"CHAR,32,8,32",
		"CHAR,100,32,128",
		"CHAR,256,32,128",
		"VARCHAR,4,4,4",
		"VARCHAR,8,8,8",
		"VARCHAR,20,8,20",
		"VARCHAR,32,8,32",
		"VARCHAR,100,32,128",
		"VARCHAR,256,32,128",
		"NCHAR,5,5,5",
		"NCHAR,8,8,8",
		"NCHAR,25,8,25",
		"NCHAR,32,8,32",
		"NCHAR,100,32,128",
		"NCHAR,256,32,128",
		"NVARCHAR,6,6,6",
		"NVARCHAR,8,8,8",
		"NVARCHAR,30,8,30",
		"NVARCHAR,32,8,32",
		"NVARCHAR,100,32,128",
		"NVARCHAR,256,32,128",
	})
	void chars(
		JDBCType jdbcType, int size,
		int expectedMinSize, int expectedMaxSize
	) {
		var sampleColumnMeta = newColumnMeta(
			"cl_any", jdbcType, size
		);

		var testedSupplierOpt = DefaultSetting.instance()
			.<String>resolveSupplier(sampleColumnMeta);

		for (int i = 0; i < timesForSpecValue; ++i) {
			assertThat(testedSupplierOpt)
				.isPresent()
				.get().extracting(Supplier::get, InstanceOfAssertFactories.STRING)
				.hasSizeBetween(expectedMinSize, expectedMaxSize);
		}
	}

	/**
	 * Tests the generators for types of large text.
	 */
	@ParameterizedTest
	@EnumSource(
		value=JDBCType.class,
		names={ "LONGVARCHAR", "CLOB", "LONGNVARCHAR", "NCLOB" }
	)
	void largeText(JDBCType jdbcType)
	{
		var sampleColumnMeta = newColumnMeta(
			"cl_any", jdbcType
		);

		var testedSupplierOpt = DefaultSetting.instance()
			.<String>resolveSupplier(sampleColumnMeta);

		for (int i = 0; i < timesForSpecValue; ++i) {
			assertThat(testedSupplierOpt)
				.isPresent()
				.get().extracting(Supplier::get, InstanceOfAssertFactories.STRING)
				.hasSizeBetween(1024, 2048);
		}
	}

	/**
	 * Tests the binary types.
	 */
	@ParameterizedTest
	@CsvSource({
		"BINARY,12,12,12",
		"BINARY,16,16,16",
		"BINARY,64,16,64",
		"BINARY,512,16,256",
		"VARBINARY,12,12,12",
		"VARBINARY,16,16,16",
		"VARBINARY,64,16,64",
		"VARBINARY,512,16,256",
	})
	void binary(
		JDBCType jdbcType, int size,
		int expectedMinSize, int expectedMaxSize
	) {
		var sampleColumnMeta = newColumnMeta(
			"cl_any", jdbcType, size
		);

		var testedSupplierOpt = DefaultSetting.instance()
			.<byte[]>resolveSupplier(sampleColumnMeta);

		for (int i = 0; i < timesForSpecValue; ++i) {
			assertThat(testedSupplierOpt)
				.isPresent()
				.get().extracting(Supplier::get, InstanceOfAssertFactories.BYTE_ARRAY)
				.hasSizeBetween(expectedMinSize, expectedMaxSize);
		}
	}

	/**
	 * Tests the generators for types of large binary.
	 */
	@ParameterizedTest
	@EnumSource(
		value=JDBCType.class,
		names={ "LONGVARBINARY", "BLOB" }
	)
	void largeBinary(
		JDBCType jdbcType
	) {
		var sampleColumnMeta = newColumnMeta(
			"cl_any", jdbcType
		);

		var testedSupplierOpt = DefaultSetting.instance()
			.<byte[]>resolveSupplier(sampleColumnMeta);

		for (int i = 0; i < timesForSpecValue; ++i) {
			assertThat(testedSupplierOpt)
				.isPresent()
				.get().extracting(Supplier::get, InstanceOfAssertFactories.BYTE_ARRAY)
				.hasSizeBetween(1024, 2048);
		}
	}

	/**
	 * Tests the {@link BigDecimal} generator.
	 */
	@ParameterizedTest
	@CsvSource(
		value={
			"DECIMAL,4,2,-99.99,99.99",
			"NUMERIC,4,2,-99.99,99.99",
			"DECIMAL,4,0,-9999,9999",
			"NUMERIC,4,0,-9999,9999",
		}
	)
	void bigDecimal(
		JDBCType jdbcType,
		int size, int decimalDigits,
		BigDecimal expectedMin, BigDecimal expectedMax
	) {
		var sampleColumnMeta = newColumnMeta(
			"cl_any", jdbcType, size, decimalDigits
		);

		var testedSupplierOpt = DefaultSetting.instance()
			.<BigDecimal>resolveSupplier(sampleColumnMeta);

		for (int i = 0; i < timesForSpecValue; ++i) {
			assertThat(testedSupplierOpt)
				.isPresent()
				.get().extracting(Supplier::get, InstanceOfAssertFactories.BIG_DECIMAL)
				.isBetween(expectedMin, expectedMax);
		}
	}

	/**
	 * Tests the temporal generator.
	 */
	@ParameterizedTest
	@EnumSource(
		value=JDBCType.class,
		names={
			"DATE", "TIMESTAMP", "TIMESTAMP_WITH_TIMEZONE",
			"TIME", "TIME_WITH_TIMEZONE",
		}
	)
	void temporalTypes(JDBCType jdbcType)
	{
		var sampleColumnMeta = newColumnMeta(
			"cl_any", jdbcType
		);

		var testedSupplierOpt = DefaultSetting.instance()
			.<Temporal>resolveSupplier(sampleColumnMeta);

		for (int i = 0; i < timesForSpecValue; ++i) {
			assertThat(testedSupplierOpt)
				.isPresent()
				.get().extracting(Supplier::get, InstanceOfAssertFactories.TEMPORAL)
				.isInstanceOf(Temporal.class)
				.isNotNull();
		}
	}
}
