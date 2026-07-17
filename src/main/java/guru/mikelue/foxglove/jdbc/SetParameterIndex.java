package guru.mikelue.foxglove.jdbc;

import guru.mikelue.foxglove.ColumnMeta;

/**
 * As the index used to matching certain combination of column meta and value type,
 * this record is used to store the cache for parameter set.
 */
record SetParameterIndex(ColumnMeta meta, Class<?> typeOfValue) {}
