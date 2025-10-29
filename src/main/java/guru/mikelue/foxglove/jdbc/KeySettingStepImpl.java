package guru.mikelue.foxglove.jdbc;

import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;

import guru.mikelue.foxglove.jdbc.JdbcTableFacet.Builder;

class KeySettingStepImpl implements ColumnSettingSteps.KeyOfIntSettingStep {
	private final JdbcTableFacet.Builder parentBuilder;
	private final Consumer<KeySupplierOfInt> finalStageSetter;

	KeySettingStepImpl(JdbcTableFacet.Builder parentBuilder, Consumer<KeySupplierOfInt> finalStageSetter)
	{
		this.parentBuilder = parentBuilder;
		this.finalStageSetter = finalStageSetter;
	}

	@Override
	public Builder range(long start, long end, int step)
	{
		Validate.isTrue(step != 0, "Step must not be zero");

		if (step > 0) {
			Validate.isTrue(start < end, "For positive step, start must be less than end");
		} else {
			Validate.isTrue(start > end, "For negative step, start must be greater than end");
		}

		finalStageSetter.accept(KeySupplierOfInt.byRange(start, end, step));

		return parentBuilder;
	}

	@Override
	public Builder limit(long start, int number, int step)
	{
		Validate.isTrue(step != 0, "Step must not be zero");
		Validate.isTrue(number > 0, "Number of values must be greater than zero");

		finalStageSetter.accept(KeySupplierOfInt.byLimit(start, number, step));

		return parentBuilder;
	}

	@Override
	public Builder domain(long[] domain)
	{
		finalStageSetter.accept(KeySupplierOfInt.of(domain));

		return parentBuilder;
	}
}
