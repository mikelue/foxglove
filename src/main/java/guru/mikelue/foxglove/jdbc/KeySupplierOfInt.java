package guru.mikelue.foxglove.jdbc;

import java.util.function.LongSupplier;

import guru.mikelue.foxglove.functional.StatefulSupplier;

/**
 * Suppliers integral values with limiting number of generated values;
 */
final class KeySupplierOfInt implements StatefulSupplier<Long> {
	private int generatedCounter = 0;

	private long currentValue;
	private final long step;
	private final int limit;

	private long[] domain = null;
	private final LongSupplier valueGetter;
	private final int numberOfRows;

	/**
	 * Creates the supplier by specifying the range of values.
	 *
	 * @param currentValue The start value(inclusive)
	 * @param end The end value(exclusive)
	 * @param step The step value
	 *
	 * @return The created supplier
	 */
	static KeySupplierOfInt byRange(long currentValue, long end, int step)
	{
		return new KeySupplierOfInt(currentValue, (int)((end - currentValue) / step), step);
	}

	/**
	 * Creates the supplier by specifying the maximum amount of values.
	 *
	 * @param currentValue The start value(inclusive)
	 * @param limit The maximum amount of values
	 * @param step The step value
	 *
	 * @return The created supplier
	 */
	static KeySupplierOfInt byLimit(long currentValue, int limit, int step)
	{
		return new KeySupplierOfInt(currentValue, limit, step);
	}

	/**
	 * Creates the supplier by specifying the domain of values.
	 *
	 * @param domain The domain of values
	 *
	 * @return The created supplier
	 */
	static KeySupplierOfInt of(long[] domain)
	{
		return new KeySupplierOfInt(domain);
	}

	private KeySupplierOfInt(long currentValue, int limit, int step)
	{
		this.currentValue = currentValue;
		this.limit = limit;
		this.step = step;

		this.valueGetter = this::nextValueByRange;
		this.numberOfRows = limit;
	}

	private KeySupplierOfInt(long[] domain)
	{
		this.currentValue = 0;
		this.limit = -1;
		this.step = 0;

		this.valueGetter = this::nextValueByDomain;
		this.domain = domain;
		this.numberOfRows = domain.length;
	}

	int getNumberOfRows()
	{
		return numberOfRows;
	}

	long nextValue()
	{
		return valueGetter.getAsLong();
	}

	long lastValue()
	{
		return currentValue;
	}

	@Override
	public Long get()
	{
		return valueGetter.getAsLong();
	}

	private long nextValueByDomain()
	{
		if (generatedCounter >= domain.length) {
			throw new IllegalStateException("Exceeds maximum amount[" + domain.length + "] of keys");
		}

		long nextValue = domain[generatedCounter];
		generatedCounter++;

		return nextValue;
	}

	private long nextValueByRange()
	{
		if (generatedCounter >= limit) {
			throw new IllegalStateException("Exceeds maximum amount[" + limit + "] of keys");
		}

		long nextValue = currentValue;

		currentValue += step;
		generatedCounter++;

		return nextValue;
	}
}
