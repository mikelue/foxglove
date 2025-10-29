package guru.mikelue.foxglove.functional;

import org.junit.jupiter.api.*;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.*;

public class RowIndexToValueSupplierTest extends AbstractTestBase {
	public RowIndexToValueSupplierTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the conversion from row index to value.
	 */
	@Test
	void get()
	{
		var testedFunction = RowIndexToValueSupplier.of(
			rowIndex -> "Value-" + rowIndex
		);

		assertThat(testedFunction.get())
			.isEqualTo("Value-0");
		assertThat(testedFunction.get())
			.isEqualTo("Value-1");
		assertThat(testedFunction.get())
			.isEqualTo("Value-2");
	}
}
