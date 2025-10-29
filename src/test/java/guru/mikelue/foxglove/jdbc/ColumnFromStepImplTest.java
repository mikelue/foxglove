package guru.mikelue.foxglove.jdbc;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.assertThat;

public class ColumnFromStepImplTest extends AbstractTestBase {
	public ColumnFromStepImplTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the supplier by round-robin strategy.
	 */
	@Test
	void roundRobin()
	{
		/*
		 * Prepares tested step
		 */
		var supplierHolder = new MutableObject<Supplier<?>>();
		var parentTable = prepareStep(
			"pt_source", supplierHolder,
			step -> step.roundRobin()
		);
		// :~)

		/*
		 * Prepares data of parent
		 */
		TombTestUtils.setupTomb(
			parentTable.getValueTomb(), "pt_source",
			"Value-1", "Value-2", "Value-3"
		);
		// :~)

		@SuppressWarnings("unchecked")
		var testedSupplier = (Supplier<String>)supplierHolder.get();

		assertThat(
			IntStream.range(0, 4)
				.mapToObj(i -> testedSupplier.get())
		)
			.containsExactly(
				"Value-1", "Value-2", "Value-3", "Value-1"
			);
	}

	/**
	 * Tests the supplier by random strategy.
	 */
	@Test
	void random()
	{
		/*
		 * Prepares tested step
		 */
		var supplierHolder = new MutableObject<Supplier<?>>();
		var parentTable = prepareStep(
			"pt_source", supplierHolder,
			step -> step.random()
		);
		// :~)

		/*
		 * Prepares data of parent
		 */
		var sampleValues = IntStream.range(0, 10)
			.mapToObj(i -> "Value-" + (i + 1))
			.toArray(String[]::new);

		TombTestUtils.setupTomb(
			parentTable.getValueTomb(), "pt_source",
			sampleValues
		);
		// :~)

		@SuppressWarnings("unchecked")
		var testedSupplier = (Supplier<String>)supplierHolder.get();

		assertThat(
			IntStream.range(0, 50)
				.mapToObj(i -> testedSupplier.get())
		)
			.containsAnyOf(sampleValues)
			.doesNotContainSequence(sampleValues);
	}

	/**
	 * Tests the transforming of domain values.
	 */
	@Test
	void transformDomain()
	{
		/*
		 * Prepares tested step
		 */
		var supplierHolder = new MutableObject<Supplier<?>>();
		var parentTable = ColumnFromStepImplTest.<Integer>prepareStep(
			"pt_source", supplierHolder,
			step -> step
				.transformDomain(
					/*
					 * Converts the integer domain to string domain
					 */
					domainStream -> domainStream
						.map(i -> "Transformed-" + (i + 1))
					// :~)
				)
				.roundRobin()
		);
		// :~)

		/*
		 * Prepares data of parent
		 */
		var sampleValues = IntStream.range(0, 3)
			.boxed()
			.toArray(Integer[]::new);

		TombTestUtils.setupTomb(
			parentTable.getValueTomb(), "pt_source",
			sampleValues
		);
		// :~)

		@SuppressWarnings("unchecked")
		var testedSupplier = (Supplier<String>)supplierHolder.get();

		assertThat(
			IntStream.range(0, 3)
				.mapToObj(i -> testedSupplier.get())
		)
			.containsExactly(
				"Transformed-1", "Transformed-2", "Transformed-3"
			);
	}

	private static <T> JdbcTableFacet prepareStep(
		String columnName,
		MutableObject<Supplier<?>> supplierHolder,
		Consumer<ColumnFromStepImpl<T>> stepCustomizer
	) {
		var parentTable = JdbcTableFacet.builder("sample_parent_table")
			.build();
		parentTable.getValueTomb().keepColumn(columnName);

		var step = new ColumnFromStepImpl<T>(
			JdbcTableFacet.builder("sample_child_table"),
			parentTable, columnName,
			supplierHolder::setValue
		);

		stepCustomizer.accept(step);

		return parentTable;
	}
}
