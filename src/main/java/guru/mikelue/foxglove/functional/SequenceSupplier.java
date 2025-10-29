package guru.mikelue.foxglove.functional;

/**
 * Supplies consequent number with customizable start and steps.
 *
 * @param <T> The type of number
 */
public interface SequenceSupplier<T extends Number> extends StatefulSupplier<T> {
	/**
	 * Gets the next value in sequence.
	 *
	 * @return The next value
	 *
	 * @see #nextValue()
	 */
	default T get()
	{
		return nextValue();
	}

	/**
	 * Gets the next value in sequence.
	 *
	 * @return The next value
	 */
	T nextValue();

	/**
	 * Gets the next value in sequence(without stepping).
	 *
	 * @return The current value
	 */
	T lastValue();
}
