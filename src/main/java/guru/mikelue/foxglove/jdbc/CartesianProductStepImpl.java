package guru.mikelue.foxglove.jdbc;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.CartesianProductSettingStep;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet.Builder;

class CartesianProductStepImpl<T> implements CartesianProductSettingStep<T> {
	private final JdbcTableFacet.Builder parentBuilder;
	private final Consumer<Supplier<List<T>>> finalStageSetter;

	CartesianProductStepImpl(
		JdbcTableFacet.Builder parentBuilder,
		Consumer<Supplier<List<T>>> finalStageSetter
	) {
		this.parentBuilder = parentBuilder;
		this.finalStageSetter = finalStageSetter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Builder domain(List<? extends T> values)
	{
		finalStageSetter.accept(() -> (List<T>)values);
		return parentBuilder;
	}

	@Override
	public Builder referencing(JdbcTableFacet referencedTable, String referencedColumn)
	{
		var valueTomb = referencedTable.getValueTomb();
		valueTomb.keepColumn(referencedColumn);

		finalStageSetter.accept(() -> valueTomb.getValues(referencedColumn));
		return parentBuilder;
	}
}
