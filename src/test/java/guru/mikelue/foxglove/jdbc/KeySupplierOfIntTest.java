package guru.mikelue.foxglove.jdbc;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class KeySupplierOfIntTest extends AbstractTestBase {
	public KeySupplierOfIntTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the generation by domain.
	 */
	@Test
	void byDomain()
	{
		var expectedValues = new Long[] {
			10L, 20L, 15L, 25L, 50L
		};

		var testedSupplier = KeySupplierOfInt.of(
			Stream.of(expectedValues).mapToLong(Long::longValue)
				.toArray()
		);

		assertGeneratedValue(testedSupplier, expectedValues);
	}

	/**
	 * Tests the generation of integer keys.
	 */
	@ParameterizedTest
	@MethodSource
	void byLimit(
		long start, int limit, int step,
		Long[] expectedValues
	) {
		var testedSupplier = KeySupplierOfInt.byLimit(start, limit, step);
		assertGeneratedValue(testedSupplier, expectedValues);
	}

	static Arguments[] byLimit()
	{
		return new Arguments[] {
			arguments( // normal case(ascending)
				1, 5, 1,
				new Long[] {1L, 2L, 3L, 4L, 5L}
			),
			arguments( // step case
				1, 5, 3,
				new Long[] {1L, 4L, 7L, 10L, 13L}
			),
			arguments( // descending case
				5, 5, -1,
				new Long[] {5L, 4L, 3L, 2L, 1L}
			),
		};
	}

	/**
	 * Tests the constructor by range.
	 */
	@ParameterizedTest
	@MethodSource
	void byRange(
		long start, long end, int step,
		Long[] expectedValues
	) {
		var testedSupplier = KeySupplierOfInt.byRange(start, end, step);
		assertGeneratedValue(testedSupplier, expectedValues);
	}

	static Arguments[] byRange()
	{
		return new Arguments[] {
			arguments( // normal case(ascending)
				1, 6, 1,
				new Long[] {1L, 2L, 3L, 4L, 5L}
			),
			arguments( // step case
				1, 16, 3,
				new Long[] {1L, 4L, 7L, 10L, 13L}
			),
			arguments( // descending case
				5, 0, -1,
				new Long[] {5L, 4L, 3L, 2L, 1L}
			),
		};
	}

	private static void assertGeneratedValue(
		KeySupplierOfInt testedSupplier,
		Long[] expectedValues
	) {
		var testedValues = new ArrayList<Long>(expectedValues.length);

		for (int i = 0; i < expectedValues.length; i++) {
			testedValues.add(testedSupplier.get());
		}

		assertThat(testedValues)
			.containsExactly(expectedValues);
	}
}
