package guru.mikelue.foxglove.functional;

import java.util.function.Function;

/**
 * This interface defines the action to convert a row index to a value.
 *
 * @param <T> Type of value to be provided
 */
@FunctionalInterface
public interface RowIndexToValue<T> extends Function<Integer, T> {}
