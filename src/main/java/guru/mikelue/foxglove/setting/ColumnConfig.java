package guru.mikelue.foxglove.setting;

import java.util.function.Supplier;

import org.instancio.generator.ValueSpec;

import guru.mikelue.foxglove.functional.SupplierDecider;

/**
 * General interface used to configure value generator
 * for a matched column.
 *
 * @param <T> The type of value generated for the matched column
 * @param <R> The type of returned object after configuration
 */
public interface ColumnConfig<T, R> {

	/**
	 * Uses a {@link Supplier} of {@link ValueSpec} for matched column.
	 *
	 * @param valueSpecSupplier The supplier of {@link ValueSpec}
	 *
	 * @return The containing object itself
	 */
	R useSpec(Supplier<ValueSpec<? extends T>> valueSpecSupplier);

	/**
	 * Uses a {@link Supplier} for matched column.
	 *
	 * @param valueSupplier The supplier of value
	 *
	 * @return The containing object itself
	 */
	R useSupplier(Supplier<? extends T> valueSupplier);

	/**
	 * Decides a {@link Supplier} for matched column.
	 *
	 * @param supplierDecider The decider of {@link Supplier}
	 *
	 * @return The containing object itself
	 */
	R decideSupplier(SupplierDecider<? extends T> supplierDecider);
}
