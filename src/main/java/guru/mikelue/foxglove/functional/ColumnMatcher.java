package guru.mikelue.foxglove.functional;

import java.util.function.Predicate;

import guru.mikelue.foxglove.ColumnMeta;

/**
 * Defines the matcher for column metadata.
 */
@FunctionalInterface
public interface ColumnMatcher extends Predicate<ColumnMeta> {}
