package guru.mikelue.foxglove.setting;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.time.*;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.instancio.Instancio;
import org.instancio.generator.specs.BooleanSpec;
import org.instancio.generator.specs.FloatSpec;
import org.instancio.generator.specs.StringSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.functional.SupplierDecider;
import guru.mikelue.foxglove.instancio.ByteArraySpec;

import static java.sql.JDBCType.*;
import static java.util.Collections.unmodifiableSet;

class DefaultSetting {
	/**
	 * The default number of rows to be generated for a table(if not set by it).
	 */
	final static int DEFAULT_NUMBER_OF_ROWS = 1024;

	/**
	 * The min length for generating value of large text types.
	 */
	final static int LARGE_TEXT_MIN_LENGTH = 1024;
	/**
	 * The max length for generating value of large text types.
	 */
	final static int LARGE_TEXT_MAX_LENGTH = 2048;

	/**
	 * The default number of sides for dice roll.
	 */
	final static int DEFAULT_DICE_SIDES = 6;

	/**
	 * Whether or not to generate possible null value by for nullable columns.
	 */
	final static boolean DEFAULT_GENERATE_NULL = false;

	/**
	 * The default column properties used for auto-generating data.
	 */
	final static Set<ColumnMeta.Property> DEFAULT_COLUMN_PROPERTIES_FOR_AUTO_GENERATING =
		unmodifiableSet(EnumSet.of(
			ColumnMeta.Property.NULLABLE,
			ColumnMeta.Property.DEFAULT_VALUE
		));

	private final static Logger logger = LoggerFactory.getLogger(DefaultSetting.class);

	private final static FloatSpec DEFAULT_FLOAT_SPEC =
		Instancio.gen().floats().min(0.0f).max(Float.MAX_VALUE);
	private final static BooleanSpec DEFAULT_BOOLEAN_SPEC =
		Instancio.gen().booleans();
	private final static StringSpec LARGE_TEXT_SPEC =
		Instancio.gen().string().alphaNumeric()
			.length(LARGE_TEXT_MIN_LENGTH, LARGE_TEXT_MAX_LENGTH); // minus prefix length

	private final static SupplierDecider<String> CHAR_SUPPLIER_DECIDER = columnMeta -> {
		logger.trace("Deciding [{}][{}] spec for column: {}",
			columnMeta.jdbcType(), columnMeta.size(), columnMeta.name());

		int columnSize = columnMeta.size();
		if (columnSize <= 8) {
			return Instancio.gen().string().alphaNumeric().length(columnSize);
		} else if (columnSize <= 32) {
			return Instancio.gen().string().alphaNumeric()
				.length(8, columnSize);
		}

		return Instancio.gen().string().alphaNumeric()
			.length(32, Math.min(128, columnSize));
	};

	private final static SupplierDecider<byte[]> BYTE_ARRAY_SUPPLIER_DECIDER = columnMeta -> {
		logger.trace("Deciding [{}][{}] spec for column: {}",
			columnMeta.jdbcType(), columnMeta.size(), columnMeta.name());

		int columnSize = columnMeta.size();
		if (columnSize <= 16) {
			return new ByteArraySpec().length(columnSize);
		} else if (columnSize <= 64) {
			return new ByteArraySpec().minLength(16).maxLength(columnSize);
		}

		return new ByteArraySpec().minLength(64).maxLength(Math.min(256, columnSize));
	};

	private final static ByteArraySpec LARGE_BINARY_SPEC = new ByteArraySpec()
		.minLength(1024).maxLength(2048);

	private final static SupplierDecider<BigDecimal> BIG_DECIMAL_SUPPLIER_DECIDER = columnMeta -> {
		logger.trace("Deciding [{}] <{}.{}> spec for column: {}",
			columnMeta.jdbcType(),
			columnMeta.size(), columnMeta.decimalDigits(),
			columnMeta.name());

		return Instancio.gen().math().bigDecimal()
			.precision(columnMeta.size())
			.scale(columnMeta.decimalDigits());
	};

	private static final Set<JDBCType> NOT_SUPPORTED_JDBC_TYPES = EnumSet.of(
		 ARRAY, STRUCT,
		 ROWID, NULL, JAVA_OBJECT, DISTINCT, OTHER,
		 SQLXML,
		 REF, REF_CURSOR, DATALINK
	);

	private final static DataSetting DEFAULT_SETTING = new DataSetting()
		/*
		 * Integral types
		 */
		.<Boolean>givenType(BIT).useSupplier(DEFAULT_BOOLEAN_SPEC)
		.<Boolean>givenType(BOOLEAN).useSupplier(DEFAULT_BOOLEAN_SPEC)
		.<Byte>givenType(TINYINT).useSupplier(Instancio.gen().bytes().min((byte) 0).max(Byte.MAX_VALUE))
		.<Short>givenType(SMALLINT).useSupplier(Instancio.gen().shorts().min((short) 0).max(Short.MAX_VALUE))
		.<Integer>givenType(INTEGER).useSupplier(Instancio.gen().ints().min(0).max(Integer.MAX_VALUE))
		.<Long>givenType(BIGINT).useSupplier(Instancio.gen().longs().min(0L).max(Long.MAX_VALUE))
		// :~)
		/*
		 * Binary types
		 */
		.<byte[]>givenType(BINARY)
			.decideSupplier(BYTE_ARRAY_SUPPLIER_DECIDER)
		.<byte[]>givenType(VARBINARY)
			.decideSupplier(BYTE_ARRAY_SUPPLIER_DECIDER)
		.<byte[]>givenType(LONGVARBINARY)
			.useSupplier(LARGE_BINARY_SPEC)
		.<byte[]>givenType(BLOB)
			.useSupplier(LARGE_BINARY_SPEC)
		// :~)
		/*
		 * Floating point types
		 */
		.<Float>givenType(FLOAT).useSupplier(DEFAULT_FLOAT_SPEC)
		.<Float>givenType(REAL).useSupplier(DEFAULT_FLOAT_SPEC)
		.<Double>givenType(DOUBLE).useSupplier(Instancio.gen().doubles().min(0.0).max(Double.MAX_VALUE))
		.<BigDecimal>givenType(DECIMAL).decideSupplier(BIG_DECIMAL_SUPPLIER_DECIDER)
		.<BigDecimal>givenType(NUMERIC).decideSupplier(BIG_DECIMAL_SUPPLIER_DECIDER)
		// :~)
		/*
		 * Character types
		 */
		.<String>givenType(CHAR).decideSupplier(CHAR_SUPPLIER_DECIDER)
		.<String>givenType(VARCHAR).decideSupplier(CHAR_SUPPLIER_DECIDER)
		.<String>givenType(NCHAR).decideSupplier(CHAR_SUPPLIER_DECIDER)
		.<String>givenType(NVARCHAR).decideSupplier(CHAR_SUPPLIER_DECIDER)
		.<String>givenType(LONGVARCHAR).useSupplier(LARGE_TEXT_SPEC)
		.<String>givenType(CLOB).useSupplier(LARGE_TEXT_SPEC)
		.<String>givenType(LONGNVARCHAR).useSupplier(LARGE_TEXT_SPEC)
		.<String>givenType(NCLOB).useSupplier(LARGE_TEXT_SPEC)
		// :~)
		/*
		 * Temporal types
		 */
		.<LocalDate>givenType(DATE)
			.useSupplier(Instancio.gen().temporal().localDate())
		.<LocalTime>givenType(TIME)
			.useSupplier(Instancio.gen().temporal().localTime())
		.<LocalDateTime>givenType(TIMESTAMP)
			.useSupplier(Instancio.gen().temporal().localDateTime())
		.<OffsetTime>givenType(TIME_WITH_TIMEZONE)
			.useSupplier(Instancio.gen().temporal().offsetTime())
		.<ZonedDateTime>givenType(TIMESTAMP_WITH_TIMEZONE)
			.useSupplier(Instancio.gen().temporal().zonedDateTime())
		// :~)
		/*
		 * Specific types
		 */
		.<UUID>givenType("UUID")
			.useSupplier(Instancio.gen().uuid())
		.notSupportedJdbcTypes(NOT_SUPPORTED_JDBC_TYPES);
		// :~)

	/**
	 * Internal implementation of default {@link DataSetting}.
	 *
	 * @return The default data setting
	 */
	static DataSetting instance()
	{
		return DEFAULT_SETTING;
	}
}
