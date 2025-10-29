package guru.mikelue.foxglove.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.setting.DataSettingInfo;

import static guru.mikelue.foxglove.ColumnMeta.Property.*;
import static java.util.Collections.unmodifiableList;

/**
 * Utility class for metadata organizing.
 */
final class MetaUtils {
	private final static Logger logger = LoggerFactory.getLogger(MetaUtils.class);

	private MetaUtils() {}

	static List<ColumnMeta> filterColumns(
		List<ColumnMeta> columnMetaList,
		DataSettingInfo dataSetting, JdbcTableFacet tableFacet
	) {
		var result = columnMetaList.stream()
			.filter(
				columnMeta -> {
					var inclusionMode = tableFacet.getColumnInclusion(columnMeta);

					switch (inclusionMode) {
						case Include:
							return true;
						case Exclude:
							return false;
						default:
							break;
					}

					return dataSetting.isAutoGenerating(columnMeta);
				}
			)
			.toList();

		return unmodifiableList(result);
	}

	static List<ColumnMeta> getColumnMetaList(ResultSetMetaData rsMeta)
	{
		try {
			return fetchColumnMetaListImpl(rsMeta);
		} catch (SQLException e) {
			throw new RuntimeJdbcException(e);
		}
	}

	static List<ColumnMeta> getColumnMetaList(DatabaseMetaData dbMeta, String tableName)
	{
		try {
			return fetchColumnMetaListImpl(dbMeta, tableName);
		} catch (SQLException e) {
			throw new RuntimeJdbcException(e);
		}
	}

	static String buildInsertSql(DatabaseMetaData dbMetaData, String tableName, List<ColumnMeta> columnMetaList)
	{
		String quote;

		try {
			quote = dbMetaData.getIdentifierQuoteString();
		} catch (SQLException e) {
			throw new RuntimeJdbcException(e);
		}

		var columnNames = columnMetaList.stream()
			.map(columnMeta -> quoteIdentifier(columnMeta.name(), quote))
			.toList();

		return String.format(
			"INSERT INTO %s (%s)\nVALUES (%s)",
			quoteIdentifier(tableName, quote),
			String.join(", ", columnNames),
			String.join(", ", Collections.nCopies(columnNames.size(), "?"))
		);
	}

	private static List<ColumnMeta> fetchColumnMetaListImpl(ResultSetMetaData rsMeta)
		throws SQLException
	{
		var columnCount = rsMeta.getColumnCount();
		var metaOfColumns = new ArrayList<ColumnMeta>(columnCount);

		for (int index = 1; index <= columnCount; ++index) {
			var columName = rsMeta.getColumnName(index);
			var jdbcType = resolveJdbcType(rsMeta.getColumnType(index));
			var typeName = rsMeta.getColumnTypeName(index);
			var size = rsMeta.getPrecision(index);
			var decimalDigits = rsMeta.getScale(index);

			var properties = EnumSet.noneOf(ColumnMeta.Property.class);
			if (rsMeta.isNullable(index) != DatabaseMetaData.columnNoNulls) {
				properties.add(NULLABLE);
			}
			if (rsMeta.isAutoIncrement(index)) {
				properties.add(AUTO_INCREMENT);
			}

			var newColumnMeta = new ColumnMeta(
				columName, properties,
				typeName, jdbcType,
				size, decimalDigits
			);

			logger.trace("Fetched column meta(ResultSet): {}", newColumnMeta);

			metaOfColumns.add(newColumnMeta);
		}

		return unmodifiableList(metaOfColumns);
	}

	private static List<ColumnMeta> fetchColumnMetaListImpl(DatabaseMetaData dbMeta, String tableName)
		throws SQLException
	{
		if (dbMeta.storesUpperCaseIdentifiers()) {
			tableName = tableName.toUpperCase();
		} else if (dbMeta.storesLowerCaseIdentifiers()) {
			tableName = tableName.toLowerCase();
		}

		var rsOfColumnMeta = dbMeta.getColumns(null, null, tableName, null);

		var metaOfColumns = new ArrayList<ColumnMeta>(rsOfColumnMeta.getFetchSize());

		while (rsOfColumnMeta.next()) {
			var columName = rsOfColumnMeta.getString("COLUMN_NAME");
			var jdbcType = resolveJdbcType(rsOfColumnMeta.getInt("DATA_TYPE"));
			var typeName = rsOfColumnMeta.getString("TYPE_NAME");
			var size = rsOfColumnMeta.getInt("COLUMN_SIZE");
			var decimalDigits = rsOfColumnMeta.getInt("DECIMAL_DIGITS");

			/*
			 * TRUE/FALSE properties of a column
			 */
			var nullable = rsOfColumnMeta.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
			var hasDefaultValue = rsOfColumnMeta.getString("COLUMN_DEF") != null;
			var isAutoIncrement = "YES".equals(rsOfColumnMeta.getString("IS_AUTOINCREMENT"));
			var isGeneratedColumn = "YES".equals(rsOfColumnMeta.getString("IS_GENERATEDCOLUMN"));
			var properties = EnumSet.noneOf(ColumnMeta.Property.class);

			if (nullable) {
				properties.add(NULLABLE);
			}
			if (hasDefaultValue) {
				properties.add(DEFAULT_VALUE);
			}
			if (isAutoIncrement) {
				properties.add(AUTO_INCREMENT);
			}
			if (isGeneratedColumn) {
				properties.add(GENERATED);
			}
			// :~)

			var newColumnMeta = new ColumnMeta(
				columName, properties,
				typeName, jdbcType,
				size, decimalDigits
			);

			logger.debug("Fetched column meta: {}", newColumnMeta);

			metaOfColumns.add(newColumnMeta);
		}

		if (metaOfColumns.isEmpty()) {
			logger.warn("No column metadata fetched for table: {}", tableName);
		}

		return unmodifiableList(metaOfColumns);
	}

	/*
	 * Converts the integral value of java.sql.Types to JDBCType
	 */
	private static JDBCType resolveJdbcType(int sqlType)
	{
		try {
			return JDBCType.valueOf(sqlType);
		} catch (IllegalArgumentException e) {
			logger.warn("Unknown SQL type value: {}. Use OTHER as JDBCType.", sqlType);
			return JDBCType.OTHER;
		}
	}

	private final static Set<String> RESERVED_KEYWORDS = Set.of(
		"SELECT","FROM","WHERE","ORDER","GROUP","USER","TABLE","INDEX","KEY",
		"DATE","TIME","YEAR","NAME","TYPE","VALUE","STATUS","DESC","ASC",
		"COUNT","SUM","AVG","MIN","MAX","LEVEL","TOP","IDENTITY","WITH"
	);
	private final static Pattern IDENTIFIER_PATTERN = Pattern.compile(".*[-\\s].*");

	/**
	 * Only quote the identifier when it is a reserved keyword or contains special characters.
	 */
	private static String quoteIdentifier(String identifier, String quote)
	{
		if (
			!RESERVED_KEYWORDS.contains(identifier.toUpperCase()) &&
			!IDENTIFIER_PATTERN.matcher(identifier).matches() &&
			!identifier.contains(quote)
		) {
			return identifier;
		}

		return quote + identifier.replace(quote, quote + quote) + quote;
	}
}
