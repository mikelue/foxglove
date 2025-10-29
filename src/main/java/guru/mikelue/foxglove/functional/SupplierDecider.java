package guru.mikelue.foxglove.functional;

import java.util.function.Function;
import java.util.function.Supplier;

import guru.mikelue.foxglove.ColumnMeta;

/**
 * Form a given context, decides the corresponding {@link Supplier}.
 *
 * @param <T> The type of value supplied by the decided {@link Supplier}
 */
@FunctionalInterface
public interface SupplierDecider<T> extends Function<ColumnMeta, Supplier<T>> {}
