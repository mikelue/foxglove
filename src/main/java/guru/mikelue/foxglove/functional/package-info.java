/**
 * Provides utilities for {@link java.util.function functional interfaces}.
 *
 * <h2>Interfaces for {@link DataSetting}</h2>
 *
 * <ul>
 *   <li>{@link ColumnMatcher} - Used to match a column by its metadata.</li>
 *   <li>{@link SupplierDecider} - A {@link Function} turns a {@link ColumnMeta} to a {@link Supplier}.</li>
 * </ul>
 *
 * <h2>Featured Suppliers</h2>
 *
 * <ul>
 *   <li>{@link Int4SequenceSupplier}, {@link Int8SequenceSupplier} - Used to generate sequence numbers for integer types.</li>
 *   <li>{@link RoundRobinValueSupplier} provides round robin behavior over a domain of values</li>
 *   <li>{@link RowIndexToValue} defines the {@link Function} which converts an index of row(generated for a table facet) to certain value</li>
 * </ul>
 *
 * <h2>Supplier wrapper</h2>
 *
 * {@link Suppliers} provides utilities to wrap a {@link Supplier} with extra behavior.
 *
 * <ul>
 *   <li>{@link Suppliers#rollingSupplier(Supplier)} makes {@link Supplier#get()} emitting {@code null} value with customizable odds</li>
 *   <li>{@link Suppliers#lazySupplier(Supplier)} makes the instantiation of a {@link Supplier} to be delayed until the first time calling of {@link Supplier#get()}.</li>
 * </ul>
 */
package guru.mikelue.foxglove.functional;

import java.util.function.Function;
import java.util.function.Supplier;

import guru.mikelue.foxglove.ColumnMeta;
import guru.mikelue.foxglove.setting.DataSetting;
