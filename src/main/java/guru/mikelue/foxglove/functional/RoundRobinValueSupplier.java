package guru.mikelue.foxglove.functional;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;

/**
 * Supplies values in round-robin manner from the given list.
 *
 * @param <T> The type of value supplied
 */
public class RoundRobinValueSupplier<T> implements StatefulSupplier<T> {
	/**
	 * Constructs the supplier with at least two values.
	 *
	 * @param <T> The type of value supplied
	 * @param values The values to be supplied in round-robin manner
	 *
	 * @return The constructed supplier
	 *
	 * @see #of(List)
	 */
	@SuppressWarnings("unchecked")
	public static <T> RoundRobinValueSupplier<T> of(T... values)
	{
		Validate.notEmpty(values, "Values for round-robin supplier cannot be null or empty");

		var newArray = (T[])new Object[values.length];
		System.arraycopy(values, 0, newArray, 0, values.length);

		return new RoundRobinValueSupplier<>(newArray);
	}

	/**
	 * Constructs the supplier with at least two values from the given stream.
	 *
	 * @param <T> The type of value supplied
	 * @param values The stream of values
	 *
	 * @return The constructed supplier
	 *
	 * @see #of(List)
	 */
	@SuppressWarnings("unchecked")
	public static <T> RoundRobinValueSupplier<T> of(Stream<? extends T> values)
	{
		Validate.notNull(values, "Values for round-robin supplier cannot be null");

		return new RoundRobinValueSupplier<>(
			(T[])values.toArray()
		);
	}

	/**
	 * Constructs the supplier with at least two values from the given list.
	 *
	 * @param <T> The type of value supplied
	 * @param values The list of values
	 *
	 * @return The constructed supplier
	 *
	 * @see #of(Object...)
	 */
	@SuppressWarnings("unchecked")
	public static <T> RoundRobinValueSupplier<T> of(List<? extends T> values)
	{
		Validate.notNull(values, "Values for round-robin supplier cannot be null");

		return new RoundRobinValueSupplier<>(
			(T[])values.toArray()
		);
	}

	private final T[] values;
	private int currentIndex = 0;

	private RoundRobinValueSupplier(T[] values)
	{
		Validate.notNull(values, "Values for round-robin supplier cannot be null");
		Validate.notEmpty(values, "At least one value are required for round-robin supplier");

		this.values = values;
	}

	@Override
	public T get()
	{
		T value = values[currentIndex];
		currentIndex = (currentIndex + 1) % values.length;
		return value;
	}
}
