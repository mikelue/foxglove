package guru.mikelue.foxglove.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import guru.mikelue.foxglove.ColumnMeta;

/**
 * Defines the custom extension for setting parameter value.
 *
 * @param <T> The type of parameter value
 */
@FunctionalInterface
public interface CustomStatementSetter<T> {
	/**
	 * Sets the parameter value to the statement.
	 *
	 * @param stmt The statement to set parameter value
	 * @param paramIndex The index of parameter to set
	 * @param columnMeta The column metadata of the parameter
	 * @param value The parameter value to set
	 *
	 * @throws SQLException If any SQL error occurs
	 */
	void setParameter(
		PreparedStatement stmt, int paramIndex,
		ColumnMeta columnMeta, T value
	) throws SQLException;
}
