package guru.mikelue.foxglove.jdbc;

import java.util.stream.IntStream;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class ReferenceSettingStepImplTest extends AbstractTestBase {
	public ReferenceSettingStepImplTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the generator by cardinality setting.
	 */
	@ParameterizedTest
	@CsvSource({
		"4,1,1",
		"5,1,3",
		"4,3,3",
		"5,3,10",
	})
	void cardinality(
		int numberOfParentRows,
		int minCardinality, int maxCardinality
	) {
		/*
		 * Prepares tested step
		 */
		var cardinalityInfoHolder = new MutableObject<CardinalityInfo<?>>();

		var testedStep = new ReferenceSettingStepImpl<String>(
			JdbcTableFacet.builder("sample_child_table"),
			cardinalityInfoHolder::setValue
		);
		// :~)

		var parent = JdbcTableFacet.builder("sample_parent_table")
			.build();
		testedStep.parent(
			parent, "pt_source"
		)
			.cardinality(minCardinality, maxCardinality);

		/*
		 * Prepares data of parent
		 */
		TombTestUtils.setupTomb(
			parent.getValueTomb(), "pt_source",
			IntStream.rangeClosed(1001, 1001 + numberOfParentRows - 1)
				.boxed()
				.toArray()
		);
		// :~)

		var testedInfo = cardinalityInfoHolder.get();

		/*
		 * Asserts the expected rows of children
		 */
		if (minCardinality == maxCardinality) {
			assertThat(testedInfo.getNumberOfRows())
				.isEqualTo(numberOfParentRows * minCardinality);
		} else {
			assertThat(testedInfo.getNumberOfRows())
				.isGreaterThanOrEqualTo(numberOfParentRows * minCardinality)
				.isLessThanOrEqualTo(numberOfParentRows * maxCardinality);
		}
		// :~)
	}
}
