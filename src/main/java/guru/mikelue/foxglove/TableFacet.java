package guru.mikelue.foxglove;

/**
 * Defines the rules to generate rows of a table.
 */
public interface TableFacet {
	/**
	 * Returns the name of table to generate data.
	 *
	 * @return The name of table
	 */
	String tableName();

	/**
	 * Returns the number of rows to be generated for the table.
	 *
	 * @return The number of rows to be generated
	 */
	int getNumberOfRows();
}
