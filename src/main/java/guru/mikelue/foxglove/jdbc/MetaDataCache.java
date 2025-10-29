package guru.mikelue.foxglove.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Validate;

import guru.mikelue.foxglove.ColumnMeta;

import static java.util.stream.Collectors.toSet;

class MetaDataCache {
	private final static Map<String, Map<String, List<ColumnMeta>>> globalCache =
		new ConcurrentHashMap<>();

	private final Map<String, List<ColumnMeta>> cacheByInstance;
	private final String connUrl;

	MetaDataCache(Connection conn)
	{
		try {
			connUrl = conn.getMetaData().getURL();
		} catch (SQLException e) {
			throw new RuntimeJdbcException(e);
		}

		if (connUrl == null) {
			cacheByInstance = new ConcurrentHashMap<>();
		} else {
			cacheByInstance = globalCache.computeIfAbsent(
				connUrl,
				key -> new ConcurrentHashMap<>()
			);
		}
	}

	void loadMetadata(List<JdbcTableFacet> facets, Connection conn)
	{
		for (var facet: facets) {
			var metaOfColumnsOfDb = cacheByInstance.computeIfAbsent(
				facet.tableName(),
				name -> {
					try {
						return MetaUtils.getColumnMetaList(conn.getMetaData(), name);
					} catch (SQLException e) {
						throw new RuntimeJdbcException(e);
					}
				}
			);

			/*
			 * Checks if all configured columns exist in database
			 */
			var setOfColumnNamesOfDb = metaOfColumnsOfDb.stream()
				.map(meta -> meta.name().toLowerCase())
				.collect(toSet());

			for (var checkedName: facet.getConfiguredNamesOfColumn()) {
				Validate.isTrue(
					setOfColumnNamesOfDb.contains(checkedName),
					"Configured column not found in table[%s]: \"%s\"",
					facet.tableName(), checkedName
				);
			}
			// :~)
		}
	}

	List<ColumnMeta> getMetaOfColumns(String tableName)
	{
		return cacheByInstance.get(tableName);
	}
}
