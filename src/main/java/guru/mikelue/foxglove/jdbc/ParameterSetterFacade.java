package guru.mikelue.foxglove.jdbc;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.*;
import java.time.temporal.Temporal;

import guru.mikelue.foxglove.ColumnMeta;

/**
 * In order to support different behavior of JDBC for setting parameter,
 * this utility class implements different cases between Java types and SQL types.
 */
final class ParameterSetterFacade {
	private ParameterSetterFacade() {}

	static void smartSetParameter(
		PreparedStatement stmt, int paramIndex,
		ColumnMeta columnMeta, Object value
	) throws SQLException {
		var jdbcType = columnMeta.jdbcType();

		if (value == null) {
			stmt.setNull(paramIndex, jdbcType.getVendorTypeNumber());
			paramIndex++;
			return;
		}

		if (value instanceof Temporal) {
			if (value instanceof Instant instant) {
				var utcLocalTime = instant.atOffset(ZoneOffset.UTC);
				setGeneralOffsetDateTime(stmt, paramIndex, jdbcType, utcLocalTime);
			} else if (value instanceof LocalDate localDate) {
				stmt.setDate(paramIndex, java.sql.Date.valueOf(localDate));
			} else if (value instanceof LocalTime localTime) {
				stmt.setTime(paramIndex, java.sql.Time.valueOf(localTime));
			} else if (value instanceof LocalDateTime localDateTime) {
				stmt.setTimestamp(paramIndex, java.sql.Timestamp.valueOf(localDateTime));
			} else if (value instanceof ZonedDateTime zonedDateTime) {
				var offsetDateTime = zonedDateTime.toOffsetDateTime();
				setGeneralOffsetDateTime(stmt, paramIndex, jdbcType, offsetDateTime);
			} else if (value instanceof OffsetTime localTime) {
				stmt.setTime(paramIndex, java.sql.Time.valueOf(localTime.toLocalTime()));
			} else if (value instanceof OffsetDateTime offsetDateTime) {
				setGeneralOffsetDateTime(stmt, paramIndex, jdbcType, offsetDateTime);
			} else {
				stmt.setObject(paramIndex, value, jdbcType.getVendorTypeNumber());
			}

			return;
		}

		if (value instanceof java.util.Date utilDate) {
			if (value instanceof java.sql.Time sqlTime)  {
				stmt.setTime(paramIndex, sqlTime);
			} else if (value instanceof java.sql.Date sqlDate)  {
				stmt.setDate(paramIndex, sqlDate);
			} else if (value instanceof java.sql.Timestamp sqlTimestamp)  {
				stmt.setTimestamp(paramIndex, sqlTimestamp);
			} else {
				stmt.setTimestamp(paramIndex, new java.sql.Timestamp(utilDate.getTime()));
			}

			return;
		}

		if (value instanceof java.lang.Number) {
			if (value instanceof Byte) {
				stmt.setByte(paramIndex, (Byte) value);
			} else if (value instanceof Short) {
				stmt.setShort(paramIndex, (Short) value);
			} else if (value instanceof Integer) {
				stmt.setInt(paramIndex, (Integer) value);
			} else if (value instanceof Long) {
				stmt.setLong(paramIndex, (Long) value);
			} else if (value instanceof Float) {
				stmt.setFloat(paramIndex, (Float) value);
			} else if (value instanceof Double) {
				stmt.setDouble(paramIndex, (Double) value);
			} else if (value instanceof BigDecimal) {
				stmt.setBigDecimal(paramIndex, (BigDecimal) value);
			} else {
				stmt.setObject(paramIndex, value, jdbcType.getVendorTypeNumber());
			}

			return;
		}

		/*
		For oracle INTERVALDS and INTERVALYM
		this would cause error:
		Caused by: java.sql.SQLException: ORA-17004: Invalid column type
		https://docs.oracle.com/error-help/db/ora-17004/
		*/
		if (jdbcType.equals(JDBCType.OTHER)) {
			var typeName = columnMeta.typeName();

			if (typeName.contains("timestamp")) {
				if (typeName.contains("time zone")) {
					stmt.setObject(paramIndex, value, Types.TIMESTAMP_WITH_TIMEZONE);
				} else {
					stmt.setObject(paramIndex, value, Types.TIMESTAMP);
				}
			} else if (typeName.contains("datetime")) {
				stmt.setObject(paramIndex, value, Types.TIMESTAMP);
			} else {
				stmt.setObject(paramIndex, value, jdbcType.getVendorTypeNumber());
			}

			return;
		}

		stmt.setObject(paramIndex, value, jdbcType.getVendorTypeNumber());
	}

	private static void setGeneralOffsetDateTime(
		PreparedStatement stmt, int paramIndex,
		JDBCType jdbcType, OffsetDateTime offsetDateTime
	) throws SQLException {
		switch (jdbcType) {
			case DATE:
				stmt.setDate(paramIndex, java.sql.Date.valueOf(offsetDateTime.toLocalDate()));
				break;
			case TIME:
				stmt.setTime(paramIndex, java.sql.Time.valueOf(offsetDateTime.toLocalTime()));
				break;
			case TIMESTAMP:
				stmt.setTimestamp(paramIndex, java.sql.Timestamp.valueOf(offsetDateTime.toLocalDateTime()));
				break;
			default:
				stmt.setObject(paramIndex, offsetDateTime);
				break;
		}
	}
}
