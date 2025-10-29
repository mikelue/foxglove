package guru.mikelue.foxglove.jdbc;

import java.sql.SQLException;

/**
 * Converts {@link SQLException} to unchecked exception.
 */
public class RuntimeJdbcException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Message-only exception.
	 *
	 * @param message The message of exception
	 */
	public RuntimeJdbcException(String message)
	{
		super(message);
	}

	/**
	 * Wraps {@link Exception} to unchecked exception.
	 *
	 * @param cause The original {@link Exception}
	 */
	public RuntimeJdbcException(Exception cause)
	{
		super(cause);
	}
}
