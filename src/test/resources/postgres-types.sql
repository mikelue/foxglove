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
    tp_time_created  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tp_time_updated  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
