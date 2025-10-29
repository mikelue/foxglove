CREATE TABLE ap_types (
    tp_id INTEGER PRIMARY KEY AUTOINCREMENT,
    tp_name TEXT NOT NULL UNIQUE,
    tp_size INTEGER NOT NULL DEFAULT 0,
    tp_key BLOB,
    tp_color TEXT,
    tp_boolean INTEGER, -- SQLite uses 0/1 for boolean
	tp_computed_size INTEGER GENERATED ALWAYS AS (tp_size - 1),
    tp_time_created DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tp_time_updated DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL
);
