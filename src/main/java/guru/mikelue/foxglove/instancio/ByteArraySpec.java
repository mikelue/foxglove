package guru.mikelue.foxglove.instancio;

import java.util.function.Supplier;

import org.instancio.Instancio;
import org.instancio.generator.ValueSpec;
import org.instancio.generator.specs.ByteSpec;

/**
 * Provides a {@link ValueSpec} for byte array({@code byte[]}).
 */
public class ByteArraySpec implements ValueSpec<byte[]> {
	private boolean nullable = false;
	private ByteSpec byteSpec = Instancio.gen().bytes();
	private int minLength = 16;
	private int maxLength = 128;
	private int length = -1;

	private Supplier<Byte> byteSupplier = byteSpec;

	/**
	 * Constructs the byte array spec.
	 */
	public ByteArraySpec() {}

	/**
	 * Sets the length of the array.
	 *
	 * @param length the exact length of the array
	 *
	 * @return this instance
	 */
	public ByteArraySpec length(int length)
	{
		this.length = length;
		return this;
	}

	/**
	 * Sets the minimum length of the array (inclusive).
	 *
	 * @param min minimum length
	 *
	 * @return this instance
	 */
	public ByteArraySpec minLength(int min)
	{
		this.minLength = min;
		return this;
	}

	/**
	 * Sets the maximum length of the array (inclusive).
	 *
	 * @param max maximum length
	 *
	 * @return this instance
	 */
	public ByteArraySpec maxLength(int max)
	{
		this.maxLength = max;
		return this;
	}

	/**
	 * Sets whether zero elements (0x00) should be generated.
	 *
	 * @return this instance
	 */
	public ByteArraySpec zeroElements()
	{
		this.byteSpec = this.byteSpec.nullable();
		this.byteSupplier = this::nullableByte;

		return this;
	}

	private final static ValueSpec<Integer> diceSpec = Instancio.gen().ints().range(1, 6);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] get()
	{
		if (nullable && diceSpec.get() == 1) {
			return null;
		}

		int arrayLength = length > 0 ? length : Instancio.gen().ints().range(minLength, maxLength).get();
		byte[] result = new byte[arrayLength];
		for (int i = 0; i < arrayLength; i++) {
			result[i] = byteSupplier.get();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValueSpec<byte[]> nullable()
	{
		nullable = true;
		return this;
	}

	private byte nullableByte()
	{
		Byte v = byteSpec.get();
		return v != null ? v : 0x00;
	}
}

