package guru.mikelue.foxglove.jdbc;

import java.sql.JDBCType;
import java.sql.Types;
import java.time.*;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;

/**
 * In order to support different behavior of JDBC for setting parameter,
 * this utility class implements different cases between Java types and SQL types.
 */
final class ParameterSetterFactory {
	private static final Map<Class<?>, Function<JDBCType, CustomStatementSetter<?>>> JAVA_TIME_SETTERS =
		readonlyMap(
			/*
			 * Converts these types to OffsetDateTime for most of JDBC drivers(by experiment),
			 * which can handle OffsetDateTime for temporal types
			 */
			entry(
				Instant.class, jdbcType -> getSetterForGeneralOffsetDateTime(
					jdbcType,
					value -> ((Instant) value).atOffset(ZoneOffset.UTC)
				)
			),
			entry(
				ZonedDateTime.class, jdbcType -> getSetterForGeneralOffsetDateTime(
					jdbcType,
					value -> ((ZonedDateTime) value).toOffsetDateTime()
				)
			),
			entry(
				OffsetDateTime.class, jdbcType -> getSetterForGeneralOffsetDateTime(
					jdbcType,
					value -> (OffsetDateTime) value
				)
			),
			// :~)

			// Vendor-specific rules for OffsetTime:
			// mysql/posgres needs TIME,
			// derby/mssql needs TIMESTAMP_WITH_TIMEZONE
			entry(
				OffsetTime.class, jdbcType -> {
					switch (jdbcType) {
						case TIME:
							return (stmt, paramIndex, columnMeta, value) ->
								stmt.setTime(paramIndex, java.sql.Time.valueOf(((OffsetTime) value).toLocalTime()));
						case TIME_WITH_TIMEZONE:
							return (stmt, paramIndex, columnMeta, value) ->
								stmt.setObject(paramIndex, ((OffsetTime) value), jdbcType.getVendorTypeNumber());
						default:
							return (stmt, paramIndex, columnMeta, value) ->
								stmt.setTime(paramIndex, java.sql.Time.valueOf(((OffsetTime) value).toLocalTime()));
					}
				}
			)
		);

	private static final Map<Class<?>, JDBCType> TYPE_TO_JDBC_TYPES = readonlyMap(
		/*
		 * For legacy temporal types of Java
		 */
		entry(java.sql.Time.class, JDBCType.TIME),
		entry(java.sql.Date.class, JDBCType.DATE),
		entry(java.sql.Timestamp.class, JDBCType.TIMESTAMP),
		entry(java.util.Date.class, JDBCType.TIMESTAMP),
		// :~)

		entry(java.time.LocalTime.class, JDBCType.TIME),

		// LocalDate
		// Not supported: derby
		entry(java.time.LocalDate.class, JDBCType.DATE),

		entry(java.time.LocalDateTime.class, JDBCType.TIMESTAMP),

		/*
		 * For number types
		 */
		entry(Byte.class, JDBCType.TINYINT),
		entry(byte.class, JDBCType.TINYINT),
		entry(Short.class, JDBCType.SMALLINT),
		entry(short.class, JDBCType.SMALLINT),
		entry(Integer.class, JDBCType.INTEGER),
		entry(int.class, JDBCType.INTEGER),
		entry(Long.class, JDBCType.BIGINT),
		entry(long.class, JDBCType.BIGINT),
		entry(Float.class, JDBCType.REAL),
		entry(float.class, JDBCType.REAL),
		entry(Double.class, JDBCType.DOUBLE),
		entry(double.class, JDBCType.DOUBLE),
		entry(java.math.BigInteger.class, JDBCType.NUMERIC),
		entry(java.math.BigDecimal.class, JDBCType.DECIMAL)
		// :~)
	);

	private ParameterSetterFactory() {}

	static CustomStatementSetter<?> smartSetterImpl(SetParameterIndex index)
	{
		var jdbcType = index.meta().jdbcType();
		var typeOfValue = index.typeOfValue();

		var deducedJdbcType = TYPE_TO_JDBC_TYPES.get(typeOfValue);
		if (deducedJdbcType != null) {
			return (stmt, paramIndex, columnMeta, value) ->
				stmt.setObject(paramIndex, value, deducedJdbcType.getVendorTypeNumber());
		}

		/*
		 * Gets setter by type of value
		 */
		var setterFactory = JAVA_TIME_SETTERS.get(typeOfValue);
		if (setterFactory != null) {
			return setterFactory.apply(jdbcType);
		}
		// :~)

		/*
		 * Special processing for JDBCType.OTHER, which is used for some database vendor specific types
		 *
		 * e.g. oracle: timestamp_tz, timestamp_ltz
		 */
		if (jdbcType.equals(JDBCType.OTHER)) {
			var typeName = index.meta().typeName();

			if (typeName.contains("timestamp") || typeName.contains("datetime")) {
				var targetType = typeName.contains("time zone") || typeName.contains("tz")?
					Types.TIMESTAMP_WITH_TIMEZONE : Types.TIMESTAMP;

				return (stmt, paramIndex, columnMeta, value) ->
					stmt.setObject(paramIndex, value, targetType);
			}
		}
		// :~)

		return (stmt, paramIndex, columnMeta, value) ->
			stmt.setObject(paramIndex, value, jdbcType.getVendorTypeNumber());
	}

	private static final Map<JDBCType, CustomStatementSetter<OffsetDateTime>> JDBC_TEMPORAL_SETTERS =
		readonlyMap(
			entry(
				JDBCType.DATE,
				(stmt, paramIndex, columnMeta, value) ->
					stmt.setDate(paramIndex, java.sql.Date.valueOf(value.toLocalDate()))
			),
			entry(
				JDBCType.TIME,
				(stmt, paramIndex, columnMeta, value) ->
					stmt.setTime(paramIndex, java.sql.Time.valueOf(value.toLocalTime()))
			),
			entry(
				JDBCType.TIMESTAMP,
				(stmt, paramIndex, columnMeta, value) ->
					stmt.setTimestamp(paramIndex, java.sql.Timestamp.valueOf(value.toLocalDateTime()))
			)
		);

	private static CustomStatementSetter<?> getSetterForGeneralOffsetDateTime(
		JDBCType jdbcType, Function<Object, OffsetDateTime> toOffsetDateTime
	) {
		var setter = JDBC_TEMPORAL_SETTERS.get(jdbcType);
		if (setter == null) {
			setter = (stmt, paramIndex, columnMeta, value) -> stmt.setObject(paramIndex, value);
		}

		final var finalSetter = setter;
		return (stmt, paramIndex, columnMeta, value) -> {
			var offsetDateTime = toOffsetDateTime.apply(value);
			finalSetter.setParameter(stmt, paramIndex, columnMeta, offsetDateTime);
		};
	}

    @SafeVarargs
	private static <K, V> Map<K, V> readonlyMap(
		Map.Entry<K, V>... keyValues
	) {
		return Stream.of(keyValues)
			.collect(Collectors.toMap(
				entry -> entry.getKey(),
				entry -> entry.getValue()
			));
	}
}

