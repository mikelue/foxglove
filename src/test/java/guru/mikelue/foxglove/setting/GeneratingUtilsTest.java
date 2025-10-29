package guru.mikelue.foxglove.setting;

import java.sql.JDBCType;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import guru.mikelue.foxglove.ColumnMeta;

import static guru.mikelue.foxglove.ColumnMeta.Property.*;
import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class GeneratingUtilsTest extends AbstractTestBase {
	public GeneratingUtilsTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the checking for whether or not to
	 * generate value for a column automatically.
	 */
	@ParameterizedTest
	@MethodSource
	void checkAutoGenerating(
		EnumSet<ColumnMeta.Property> columnProperties,
		Set<ColumnMeta.Property> autoGenerateProperties,
		boolean expectedResult
	) {
		var sampleColumnMeta = newColumnMeta(
			"any_column", JDBCType.VARCHAR, columnProperties
		);

		var testedResult = GeneratingUtils.checkAutoGenerating(
			sampleColumnMeta, autoGenerateProperties
		);

		assertThat(testedResult)
			.isEqualTo(expectedResult);
	}
	static Arguments[] checkAutoGenerating()
	{
		return new Arguments[] {
			arguments( // Empty properties of a column
				EnumSet.noneOf(ColumnMeta.Property.class),
				EnumSet.noneOf(ColumnMeta.Property.class),
				true
			),
			arguments( // [TRUE case] generated column
				EnumSet.of(GENERATED), Set.of(GENERATED),
				true
			),
			arguments( // [TRUE case] auto-increment column
				EnumSet.of(AUTO_INCREMENT), Set.of(AUTO_INCREMENT),
				true
			),
			arguments( // [TRUE case] nullable column
				EnumSet.of(NULLABLE), Set.of(NULLABLE),
				true
			),
			arguments( // [TRUE case] column having default value
				EnumSet.of(DEFAULT_VALUE), Set.of(DEFAULT_VALUE),
				true
			),
			arguments( // [FALSE case] generated column
				EnumSet.of(GENERATED, AUTO_INCREMENT), Set.of(AUTO_INCREMENT),
				false
			),
			arguments( // [FALSE case] auto_increment column
				EnumSet.of(AUTO_INCREMENT, NULLABLE), Set.of(NULLABLE),
				false
			),
			arguments( // [FALSE case] nullable  column
				EnumSet.of(NULLABLE, DEFAULT_VALUE), Set.of(DEFAULT_VALUE),
				false
			),
			arguments( // [FALSE case] column having default value
				EnumSet.of(DEFAULT_VALUE), Set.of(NULLABLE),
				false
			),
		};
	}
}
