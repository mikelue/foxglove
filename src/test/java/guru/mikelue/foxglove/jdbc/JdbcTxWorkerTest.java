package guru.mikelue.foxglove.jdbc;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.misc.testlib.AbstractTestBase;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.assertj.core.api.Assertions.assertThat;

public class JdbcTxWorkerTest extends AbstractTestBase {
	@Mocked
	private Connection mockConn;

	@Mocked
	private DatabaseMetaData mockDbMeta;

	@Mocked
	private PreparedStatement mockStmt;

	@Mocked
	private RowParamsGenerator mockRowGenerator;

	public JdbcTxWorkerTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the transaction controlled by batch size.
	 */
	@ParameterizedTest
	@CsvSource({
		"3,10,1", // No remaining batch
		"3,10,2", // No remaining batch
		"10,5,1", // No remaining batch
		"10,5,2", // No remaining batch
		"20,7,1", // Has remaining batch
		"20,7,3", // Has remaining batch
	})
	void byBatchSize(
		int numberOfRows, int batchSize,
		int tableCount
	) throws SQLException {
		final int remainBatch = (numberOfRows % batchSize == 0) ? 0 : 1;

		var txGear = new TransactionGear(
			mockConn, batchSize, false
		);

		mockAndExerciseInsertion(
			numberOfRows, batchSize, tableCount,
			txGear
		);

		/*
		 * Asserts:
		 *
		 * 1. Auto-commit is disabled at the beginning
		 * 2. Auto-commit is enabled at the end
		 */
		new Verifications() {{
			mockConn.setAutoCommit(false);
			times = 1;

			mockStmt.executeBatch();
			times = (numberOfRows / batchSize + remainBatch) * tableCount;

			mockConn.commit();
			times = (numberOfRows * tableCount) / batchSize + remainBatch;

			mockConn.setAutoCommit(true);
			times = 1;
		}};
		// :~)
	}

	/**
	 * Tests the transaction is controlled by outside.
	 */
	@ParameterizedTest
	@CsvSource({
		"3,10,1", // No remaining batch
		"3,10,2", // No remaining batch
		"10,5,1", // No remaining batch
		"10,5,2", // No remaining batch
		"20,7,1", // Has remaining batch
		"20,7,3", // Has remaining batch
	})
	void byOutside(
		int numberOfRows, int batchSize,
		int tableCount
	) throws SQLException {
		final int remainBatch = (numberOfRows % batchSize == 0) ? 0 : 1;

		var txGear = new TransactionGear(
			mockConn, batchSize, true
		);

		mockAndExerciseInsertion(
			numberOfRows, batchSize, tableCount,
			txGear
		);

		/*
		 * Asserts:
		 *
		 * 1. Auto-commit is disabled at the beginning
		 * 2. Auto-commit is enabled at the end
		 */
		new Verifications() {{
			mockConn.setAutoCommit(anyBoolean);
			times = 0;

			mockStmt.executeBatch();
			times = (numberOfRows / batchSize + remainBatch) * tableCount;

			mockConn.commit();
			times = 0;
		}};
		// :~)
	}

	private void mockAndExerciseInsertion(
		int numberOfRows, int batchSize,
		int tableCount,
		TransactionGear txGear
	) throws SQLException {
		var sampleTable = JdbcTableFacet.builder("any_table")
			.numberOfRows(numberOfRows)
			.build();
		var sampleColumns = List.of(
			newColumnMeta("col1", JDBCType.VARCHAR),
			newColumnMeta("col2", JDBCType.INTEGER)
		);

		var sampleResultOfGeneratedRow = new LinkedHashMap<ColumnMeta, Object>();
		sampleResultOfGeneratedRow.put(sampleColumns.get(0), "sample-string");
		sampleResultOfGeneratedRow.put(sampleColumns.get(1), 12345);

		/*
		 * Mocks fake values of rows
		 */
		new Expectations() {{
			mockDbMeta.getIdentifierQuoteString();
			result = "";

			mockConn.getAutoCommit();
			result = true;

			mockConn.prepareStatement(anyString, RETURN_GENERATED_KEYS);
			result = mockStmt;

			mockConn.getMetaData().getDriverName();
			result = "HSQL Database Engine Driver";

			mockRowGenerator.generateRowParams();
			result = sampleResultOfGeneratedRow;
			times = numberOfRows * tableCount;
		}};
		// :~)

		/*
		 * Performs insert operations
		 */
		try (var testedWorker = new JdbcTxWorker(txGear)) {
			int numberOfGeneratedRows = 0;

			var insertSql = MetaUtils.buildInsertSql(
				mockDbMeta,
				sampleTable.tableName(), sampleColumns
			);
			for (int i = 0; i < tableCount; i++) {
				numberOfGeneratedRows += testedWorker.performInsert(
					new JdbcTxWorker.InsertionContext(
						insertSql, numberOfRows, new String[0],
						mockRowGenerator::generateRowParams
					),
					v -> {}
				);
			}

			assertThat(numberOfGeneratedRows)
				.isEqualTo(numberOfRows * tableCount);
		}
		// :~)
	}
}
