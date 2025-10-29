package guru.mikelue.foxglove.functional;

import java.util.function.Supplier;

/**
 * This is a stateful {@link Supplier}, which need client code to implements {@link RowIndexToValue#apply} method.
 *
 * The value of row index is maintained internally which is fed to {@link RowIndexToValue#apply}.
 *
 * @param <T> Type of value to be provided
 */
public class RowIndexToValueSupplier<T> implements StatefulSupplier<T> {
	private final RowIndexToValue<T> toValueFunc;
	private int currentRow = 0;

	/**
	 * Creates the supplier with given function to get value at specified row index.
	 *
	 * @param <T> The type of value supplied
	 * @param toValueFunc The function to get value at specified row index
	 *
	 * @return The created supplier
	 */
	public static <T> RowIndexToValueSupplier<T> of(RowIndexToValue<T> toValueFunc)
	{
		return new RowIndexToValueSupplier<>(toValueFunc);
	}

	private RowIndexToValueSupplier(RowIndexToValue<T> toValueFunc)
	{
		this.toValueFunc = toValueFunc;
	}

	/**
	 * Gets the value at current row index, then advances the row index by one.
	 *
	 * @return The value at current row index
	 */
	@Override
	public T get()
	{
		var value = toValueFunc.apply(currentRow);
		currentRow++;

		return value;
	}

	/**
	 * Gets the the row index to be used for next time calling of {@link #get()} .
	 *
	 * @return The value at specified row index
	 */
	public int getCurrentIndex()
	{
		return currentRow;
	}
}
