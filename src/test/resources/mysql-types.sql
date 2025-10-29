CREATE TABLE ap_types (
    tp_id            INT AUTO_INCREMENT PRIMARY KEY,
    tp_name          VARCHAR(32) NOT NULL,
    tp_size          INT NOT NULL DEFAULT 0,
    tp_key           VARBINARY(255),
    tp_color         VARCHAR(32),
    tp_json_data     JSON,
	tp_computed_size INT AS (tp_size - 1),
    tp_time_created  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    tp_time_updated  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                     ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
