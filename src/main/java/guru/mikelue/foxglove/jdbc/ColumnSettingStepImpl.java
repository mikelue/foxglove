package guru.mikelue.foxglove.jdbc;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.instancio.generator.ValueSpec;

import guru.mikelue.foxglove.functional.RoundRobinValueSupplier;
import guru.mikelue.foxglove.functional.RowIndexToValue;
import guru.mikelue.foxglove.functional.RowIndexToValueSupplier;
import guru.mikelue.foxglove.functional.SupplierDecider;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.ColumnFromStep;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.ColumnSimpleStep;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet.Builder;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@SuppressWarnings("unchecked")
class ColumnSettingStepImpl<T> implements ColumnSimpleStep<T> {
	private final JdbcTableFacet.Builder parentBuilder;
	private final Consumer<SupplierDecider<?>> finalStageSetter;

	ColumnSettingStepImpl(Builder parentBuilder, Consumer<SupplierDecider<?>> finalStageSetter)
	{
		this.parentBuilder = parentBuilder;
		this.finalStageSetter = finalStageSetter;
	}

	@Override
	public Builder useSpec(Supplier<ValueSpec<? extends T>> valueSpecSupplier)
	{
		finalStageSetter.accept(columnMeta -> {
			return (Supplier<Object>) valueSpecSupplier.get();
		});
		return parentBuilder;
	}

	@Override
	public Builder useSupplier(Supplier<? extends T> valueSupplier)
	{
		finalStageSetter.accept(columnMeta -> {
			return (Supplier<Object>)valueSupplier;
		});
		return parentBuilder;
	}

	@Override
	public Builder decideSupplier(SupplierDecider<? extends T> supplierDecider)
	{
		finalStageSetter.accept(columnMeta -> {
			return (Supplier<Object>)supplierDecider.apply(columnMeta);
		});
		return parentBuilder;
	}

	@Override
	public Builder fixed(T fixedValue)
	{
		return useSupplier(() -> fixedValue);
	}

	@Override
	public Builder roundRobin(T... values)
	{
		Validate.notEmpty(values, "At least two values are required for round-robin supplier");

		return useSupplier(RoundRobinValueSupplier.of(values));
	}

	@Override
	public Builder forRow(RowIndexToValue<? extends T> rowIndexToValue)
	{
		return useSupplier(
			RowIndexToValueSupplier.of(rowIndexToValue)
		);
	}

	@Override
	public ColumnFromStep<T> from(JdbcTableFacet referencedTable, String referencedColumn)
	{
		var safeReferencedColumn = trimToEmpty(referencedColumn).toLowerCase();

		Validate.notNull(referencedTable, "Parent table must not be null");
		Validate.notEmpty(safeReferencedColumn, "Referenced column must not be empty");

		referencedTable.getValueTomb().keepColumn(safeReferencedColumn);
		return new ColumnFromStepImpl<T>(
			parentBuilder,
			referencedTable, referencedColumn,
			supplier -> {
				finalStageSetter.accept(columnMeta -> (Supplier<Object>)supplier);
			}
		);
	}
}
