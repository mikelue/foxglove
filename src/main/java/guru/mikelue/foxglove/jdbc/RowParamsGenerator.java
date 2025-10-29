package guru.mikelue.foxglove.jdbc;

import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.setting.DataSettingInfo;

/**
 * With provided metadata of columns and data setting,
 * this object generates the row parameters used by {@link PreparedStatement}.
 */
class RowParamsGenerator {
	private Logger logger = LoggerFactory.getLogger(RowParamsGenerator.class);

	private final Map<ColumnMeta, Supplier<Object>> supplierOfColumns;

	RowParamsGenerator(
		JdbcTableFacet table,
		List<ColumnMeta> metaOfColumns,
		DataSettingInfo setting
	) {
		this.supplierOfColumns = new LinkedHashMap<>(metaOfColumns.size());

		for (ColumnMeta columnMeta : metaOfColumns) {
			var supplierOpt = table.resolveSupplier(columnMeta)
				.or(() -> setting.resolveSupplier(columnMeta));

			Validate.isTrue(
				supplierOpt.isPresent(),
				"No supplier found for column: %s", columnMeta
			);

			var supplier = supplierOpt.get();

			if (logger.isTraceEnabled()) {
				logger.trace("Found supplier[{}] for column: {}",
					supplier.getClass().getSimpleName(),
					columnMeta
				);
			}

			this.supplierOfColumns.put(columnMeta, supplier);
		}
	}

	Map<ColumnMeta, Object> generateRowParams()
	{
		var resultRow = new LinkedHashMap<ColumnMeta, Object>(supplierOfColumns.size());

		for (var columnMeta: supplierOfColumns.keySet()) {
			var value = supplierOfColumns.get(columnMeta)
				.get();

			resultRow.put(columnMeta, value);
		}

		return resultRow;
	}
}
