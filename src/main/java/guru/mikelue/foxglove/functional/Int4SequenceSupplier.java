package guru.mikelue.foxglove.functional;

import java.util.function.IntSupplier;

/**
 * Provides consequent number with customizable start and steps.
 *
 * The type is {@link Integer}(4 bytes) based.
 *
 * @see Int8SequenceSupplier
 */
public class Int4SequenceSupplier implements IntSupplier, SequenceSupplier<Integer> {
	private int currentValue;
	private final int step;

	/**
	 * Constructs the supplier with start value as 1 and step as 1.
	 *
	 * @see Int4SequenceSupplier(int, int)
	 */
	public Int4SequenceSupplier()
	{
		this(1, 1);
	}

	/**
	 * Constructs the supplier with start value as 0 and step as 1.
	 *
	 * @param startValue The start value
	 *
	 * @see Int4SequenceSupplier(int, int)
	 */
	public Int4SequenceSupplier(int startValue)
	{
		this(startValue, 1);
	}

	/**
	 * Constructs the supplier with customizable start value and step.
	 *
	 * @param startValue The start value
	 * @param stepValue The step value
	 *
	 * @see Int4SequenceSupplier(int)
	 */
	public Int4SequenceSupplier(int startValue, int stepValue)
	{
		this.currentValue = startValue;
		this.step = stepValue;
	}

	/**
	 * Gets the next value in sequence.
	 *
	 * @return The next value
	 */
	@Override
	public int getAsInt()
	{
		return nextValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer nextValue()
	{
		var nextValue = currentValue;
		currentValue += step;
		return nextValue;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer lastValue()
	{
		return currentValue;
	}
}
