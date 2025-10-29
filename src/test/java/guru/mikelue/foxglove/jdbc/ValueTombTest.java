package guru.mikelue.foxglove.jdbc;

import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.TupleAccessor;
import guru.mikelue.misc.testlib.AbstractTestBase;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;
import static org.assertj.core.api.Assertions.assertThat;

public class ValueTombTest extends AbstractTestBase {
	public ValueTombTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the preserving of values by proto-data.
	 */
	@Test
	void preserveProtoData()
	{
		var testedTomb = new ValueTomb("sample_table");

		testedTomb.keepColumn("col1");

		testedTomb.preserveProtoData(
			newTuple("col1", 20)
		);
		testedTomb.preserveProtoData(
			newTuple("col1", 30)
		);

		assertThat(testedTomb.getValues("col1"))
			.containsExactly(20, 30);
	}

	/**
	 * Tests the preserving of values by proto-data.
	 */
	@Test
	void preserveAfterData()
	{
		var testedTomb = new ValueTomb("sample_table");

		testedTomb.keepColumn("col1");
		testedTomb.keepColumn("col2");

		testedTomb.preserveProtoData(
			newTuple("col2", 99)
		);

		testedTomb.preserveAfterData(List.of(
			newTuple("col1", 37),
			newTuple("col1", 38),
			/*
			 * Doesn't get preserved since 'col2' is preserved in proto-data
			 */
			newTuple("col2", 57),
			newTuple("col2", 58)
			// :~)
		));

		assertThat(testedTomb.getValues("col1"))
			.containsExactly(37, 38);
		assertThat(testedTomb.getValues("col2"))
			.containsExactly(99);
	}

	private static TupleAccessor newTuple(String columnName, Object value)
	{
		var column = newColumnMeta(columnName, JDBCType.INTEGER);
		var tupleSchema = new TupleAccessorImpl.TupleSchema(
			List.of(column)
		);

		return tupleSchema.createTupleAccessor(
			Map.of(column, value), 0
		);
	}
}
