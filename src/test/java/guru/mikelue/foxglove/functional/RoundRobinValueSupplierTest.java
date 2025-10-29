package guru.mikelue.foxglove.functional;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class RoundRobinValueSupplierTest extends AbstractTestBase {
	public RoundRobinValueSupplierTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the round-robin value supplying.
	 */
	@Test
	void get()
	{
		var testedSupplier = RoundRobinValueSupplier.<String>of(
			"A", "B", "C"
		);

		var expectedValues = Arrays.asList("A", "B", "C", "A");
		var testedResult = new ArrayList<>(expectedValues.size());

		for (int i = 0; i < expectedValues.size(); ++i) {
			testedResult.add(testedSupplier.get());
		}

		assertThat(testedResult)
			.containsExactlyElementsOf(expectedValues);
	}
}
