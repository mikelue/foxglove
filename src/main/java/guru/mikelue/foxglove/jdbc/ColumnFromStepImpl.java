package guru.mikelue.foxglove.jdbc;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.instancio.Instancio;

import guru.mikelue.foxglove.functional.RoundRobinValueSupplier;
import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.ColumnFromStep;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet.Builder;

import static guru.mikelue.foxglove.functional.Suppliers.lazySupplier;

class ColumnFromStepImpl<T> implements ColumnFromStep<T> {
	private final Builder baseBuilder;
	private final Supplier<List<T>> domainSupplier;
	private final Consumer<Supplier<?>> supplierSetter;

	ColumnFromStepImpl(
		Builder builder,
		JdbcTableFacet table, String columnName,
		Consumer<Supplier<?>> supplierSetter
	) {
		this(
			builder,
			() -> table.getValueTomb().getValues(columnName),
			supplierSetter
		);
	}

	private ColumnFromStepImpl(
		Builder builder,
		Supplier<List<T>> domainSupplier,
		Consumer<Supplier<?>> supplierSetter
	) {
		this.baseBuilder = builder;
		this.domainSupplier = domainSupplier;
		this.supplierSetter = supplierSetter;
	}

	@Override
	public Builder roundRobin()
	{
		supplierSetter.accept(
			lazySupplier(() ->
				RoundRobinValueSupplier.of(domainSupplier.get())
			)
		);

		return baseBuilder;
	}

	@Override
	public Builder random()
	{
		var supplier = lazySupplier(
			() -> {
				var values = domainSupplier.get();
				var gen = Instancio.gen().ints().range(0, values.size() - 1);

				return () -> values.get(gen.get());
			}
		);
		supplierSetter.accept(supplier);

		return baseBuilder;
	}

	@Override
	public <V> ColumnFromStep<V> transformDomain(Function<? super Stream<T>, ? extends Stream<V>> domainConverter)
	{
		Supplier<List<V>> convertedSupplier = () ->
			domainConverter.apply(
				domainSupplier.get().stream()
			)
			.toList();

		return new ColumnFromStepImpl<V>(
			baseBuilder, convertedSupplier,
			supplierSetter
		);
	}
}
