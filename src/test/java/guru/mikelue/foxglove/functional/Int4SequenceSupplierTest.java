package guru.mikelue.foxglove.functional;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class Int4SequenceSupplierTest extends AbstractTestBase {
	public Int4SequenceSupplierTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the generated values.
	 */
	@Test
	void get()
	{
		var testedSupplier = new Int4SequenceSupplier(10, 5);

		assertThat(List.of(
			testedSupplier.nextValue(), testedSupplier.nextValue(),
			testedSupplier.nextValue(), testedSupplier.nextValue()
		))
			.containsExactly(10, 15, 20, 25);
	}
}
