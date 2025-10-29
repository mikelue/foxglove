package guru.mikelue.foxglove.functional;

import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SuppliersTest extends AbstractTestBase {
	public SuppliersTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests {@link Suppliers#rollingSupplier(Supplier, int, Object)}.
	 */
	@Test
	void buildRollingSupplier()
	{
		var testedSupplier = Suppliers.rollingSupplier(
			() -> 1, 2, 0
		);

		await()
			.atMost(5, SECONDS)
			.untilAsserted(() -> {
				var suppliedValue = testedSupplier.get();
				assertThat(suppliedValue)
					.isEqualTo(0);
			});
	}
}
