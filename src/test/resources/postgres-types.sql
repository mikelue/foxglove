CREATE TABLE ap_types (
    tp_id            SERIAL PRIMARY KEY,
    tp_name          VARCHAR(32) NOT NULL,
    tp_size          INT NOT NULL DEFAULT 0,
    tp_key           BYTEA,
    tp_color         VARCHAR(32),
	tp_dimension     INTEGER ARRAY[4],
	tp_range		 INT4RANGE,
	tp_json_data     JSONB,
	tp_computed_size INT GENERATED ALWAYS AS (tp_size - 1) STORED,
	tp_date          DATE,
    tp_time          TIME,
    tp_timetz        TIME WITH TIME ZONE,
    tp_timestamp     TIMESTAMP,
    tp_timestamptz   TIMESTAMP WITH TIME ZONE,
    tp_interval      INTERVAL,
    tp_time_created  TIMESTAMP(0) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    tp_time_updated  TIMESTAMP(0) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP(0)
);
