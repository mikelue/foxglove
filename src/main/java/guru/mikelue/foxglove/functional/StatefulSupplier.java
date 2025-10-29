package guru.mikelue.foxglove.functional;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * In order to prevent state pollution when cloning {@link Supplier}s,
 * this interface marks the {@link Supplier} as stateful.
 *
 * @param <T> The type of results supplied by this supplier
 */
public interface StatefulSupplier<T> extends Supplier<T> {
	/**
	 * Turns a thread-safe version of the given stateful supplier.
	 *
	 * @param <T> The type of results supplied by this supplier
	 * @param sourceSupplier The source stateful supplier
	 *
	 * @return The thread-safe stateful supplier
	 */
	static <T> StatefulSupplier<T> threadSafe(StatefulSupplier<T> sourceSupplier)
	{
		return new ThreadSafeSupplierImpl<>(sourceSupplier);
	}
}

class ThreadSafeSupplierImpl<T> implements StatefulSupplier<T> {
	private final Lock lock = new ReentrantLock();
	private final StatefulSupplier<? extends T> sourceSupplier;

	ThreadSafeSupplierImpl(StatefulSupplier<? extends T> sourceSupplier)
	{
		this.sourceSupplier = sourceSupplier;
	}

	@Override
	public T get()
	{
		lock.lock();

		try {
			T value = (T)sourceSupplier.get();
			return value;
		} finally {
			lock.unlock();
		}
	}
}
