package guru.mikelue.foxglove.jdbc;

import java.sql.Connection;

/**
 * Keeps the need information for transaction processing.
 */
record TransactionGear(
	Connection connection,
	int batchSize,
	boolean joinConnection
) {}
