package guru.mikelue.foxglove.jdbc;

import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

import guru.mikelue.foxglove.TupleAccessor;

import static guru.mikelue.foxglove.ColumnMetaTestUtils.newColumnMeta;

interface TombTestUtils {
	@SuppressWarnings("unchecked")
	static <T> void setupTomb(
		ValueTomb tomb, String columnName,
		T... values
	) {
		for (var value : values) {
			tomb.preserveProtoData(
				newTuple(columnName, value)
			);
		}
	}

	static <T> TupleAccessor newTuple(
		String columName, T Value
	) {
		var sampleColumn = newColumnMeta(
			columName, JDBCType.INTEGER
		);

		var tupleSchema = new TupleAccessorImpl.TupleSchema(
			List.of(sampleColumn)
		);
		return tupleSchema.createTupleAccessor(
			Map.of(sampleColumn, Value), 0
		);
	}
}
