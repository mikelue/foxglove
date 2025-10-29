CREATE TABLE ap_types (
    tp_id            BIGINT GENERATED ALWAYS AS IDENTITY
                     (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    tp_name          VARCHAR(32) NOT NULL UNIQUE,
    tp_size          INT DEFAULT 0 NOT NULL,
    tp_key           VARCHAR(64) FOR BIT DATA,
    tp_color         VARCHAR(32),
	tp_computed_size INT GENERATED ALWAYS AS (tp_size - 1),
    tp_time_created  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tp_time_updated  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
)
