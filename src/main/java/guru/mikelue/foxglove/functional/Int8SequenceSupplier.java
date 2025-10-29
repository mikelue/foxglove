package guru.mikelue.foxglove.functional;

import java.util.function.LongSupplier;

/**
 * Provides consequent number with customizable start and steps.
 *
 * The type is {@link Long}(8 bytes) based.
 *
 * @see Int4SequenceSupplier
 */
public class Int8SequenceSupplier implements LongSupplier, SequenceSupplier<Long> {
	private long currentValue;
	private final int step;

	/**
	 * Constructs the supplier with start value as 1 and step as 1.
	 *
	 * @see Int8SequenceSupplier(long, int)
	 */
	public Int8SequenceSupplier()
	{
		this(1, 1);
	}

	/**
	 * Constructs the supplier with start value as 0 and step as 1.
	 *
	 * @param startValue The start value
	 *
	 * @see Int8SequenceSupplier(long, int)
	 */
	public Int8SequenceSupplier(long startValue)
	{
		this(startValue, 1);
	}

	/**
	 * Constructs the supplier with customizable start value and step.
	 *
	 * @param startValue The start value
	 * @param stepValue The step value
	 *
	 * @see Int8SequenceSupplier(long)
	 */
	public Int8SequenceSupplier(long startValue, int stepValue)
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
	public long getAsLong()
	{
		return nextValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long nextValue()
	{
		var nextValue = currentValue;
		currentValue += step;
		return nextValue;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long lastValue()
	{
		return currentValue;
	}
}
