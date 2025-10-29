package guru.mikelue.foxglove.functional;

import java.util.function.Supplier;

import org.instancio.Instancio;
import org.instancio.generator.ValueSpec;

/**
 * Provides utility methods for {@link Supplier}.
 */
public interface Suppliers {
	/**
	 * Builds a {@link Supplier} which returns the default value based on dice
	 * rolling.
	 *
	 * <p>
	 *
	 * The returned {@link Supplier} will return the {@code defaultValue} when the dice rolls to 1;
	 * otherwise, it will return the value from the {@code baseSupplier}.
	 *
	 * @param <T> The type of supplied value
	 *
	 * @param baseSupplier The base {@link Supplier} to get value when the dice
	 *        roll is not 1
	 * @param diceSides The number of sides of dice
	 * @param defaultValue The default value to return when the dice roll is 1
	 *
	 * @return The built {@link Supplier}
	 *
	 * @see #rollingSupplier(Supplier)
	 * @see #rollingSupplier(Supplier, int)
	 */
	static <T> Supplier<T> rollingSupplier(Supplier<? extends T> baseSupplier, int diceSides, T defaultValue) {
		final ValueSpec<Integer> diceRollSpec = Instancio.gen().ints()
			.range(1, diceSides);

		return () -> {
			if (diceRollSpec.get() == 1) {
				return defaultValue;
			}

			return baseSupplier.get();
		};
	}

	/**
	 * Builds a {@link Supplier} which returns {@code null}  based on dice rolling.
	 *
	 * <p>
	 *
	 * The returned {@link Supplier} will return the {@code null} when the dice rolls to 1;
	 * otherwise, it will return the value from the {@code baseSupplier}.
	 *
	 * @param <T> The type of supplied value
	 *
	 * @param baseSupplier The base {@link Supplier} to get value when the dice
	 *        roll is not 1
	 * @param diceSides The number of sides of dice
	 *
	 * @return The built {@link Supplier}
	 *
	 * @see #rollingSupplier(Supplier, int, Object)
	 */
	static <T> Supplier<T> rollingSupplier(Supplier<? extends T> baseSupplier, int diceSides)
	{
		return rollingSupplier(baseSupplier, diceSides, null);
	}

	/**
	 * Builds a {@link Supplier} which returns {@code null}  based on dice rolling.
	 *
	 * The number of sides of dice is 6 by default.
	 *
	 * <p>
	 *
	 * The returned {@link Supplier} will return the {@code null} when the dice rolls to 1;
	 * otherwise, it will return the value from the {@code baseSupplier}.
	 *
	 * @param <T> The type of supplied value
	 *
	 * @param baseSupplier The base {@link Supplier} to get value when the dice
	 *        roll is not 1
	 *
	 * @return The built {@link Supplier}
	 *
	 * @see #rollingSupplier(Supplier, int, Object)
	 */
	static <T> Supplier<T> rollingSupplier(Supplier<? extends T> baseSupplier)
	{
		return rollingSupplier(baseSupplier, 6, null);
	}

	/**
	 * Builds a {@link Supplier} which initializes the target {@link Supplier} lazily.
	 *
	 * <p>
	 * The returned {@link Supplier} will invoke the {@code lazySupplier} only once
	 * to get the target {@link Supplier}. Subsequent invocations will use the
	 * target {@link Supplier} directly.
	 *
	 * @param <T> The type of supplied value
	 * @param lazySupplier The supplier of target {@link Supplier}
	 *
	 * @return The built {@link Supplier}
	 */
	static <T> Supplier<T> lazySupplier(Supplier<? extends Supplier<? extends T>> lazySupplier) {
		return new Supplier<>() {
			private Supplier<T> targetSupplier = null;

			@Override
			@SuppressWarnings("unchecked")
			public T get()
			{
				if (targetSupplier == null) {
					targetSupplier = (Supplier<T>)lazySupplier.get();
				}

				return targetSupplier.get();
			}
		};
	}
}
