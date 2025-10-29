CREATE TABLE IF NOT EXISTS dt_main (
	st_id SERIAL PRIMARY KEY,
	st_uuid UUID UNIQUE,

	st_tiny_int SMALLINT,
	st_int INTEGER,
	st_small_int SMALLINT,
	st_big_int BIGINT,

	st_decimal DECIMAL(8,2),
	st_float REAL,
	st_double DOUBLE PRECISION,

	st_char CHAR(8),
	st_varchar VARCHAR(32),
	st_large_text TEXT,

	st_date DATE,
	st_time TIME,
	st_time_zone TIME WITH TIME ZONE,
	st_timestamp TIMESTAMP,
	st_timestamp_zone TIMESTAMP WITH TIME ZONE,

	st_binary BINARY(16),
	st_var_binary VARBINARY(16),
	st_enum ENUM('VAL_A', 'VAL_B', 'VAL_C'),

	st_boolean BOOLEAN
)
