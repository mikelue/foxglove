package guru.mikelue.foxglove.functional;

import java.util.List;
import java.util.stream.Stream;

import guru.mikelue.foxglove.TableFacet;

/**
 * {@link Stream} alternative of {@link TableFacetsProvider},
 *
 * @param <T> The type of table facet
 */
public interface StreamOfTableFacetsProvider<T extends TableFacet> extends TableFacetsProvider<T> {
	/**
	 * Converts the stream of table facets to a list.
	 *
	 * @return A list of table facets
	 */
	@Override
	default List<T> get()
	{
		return streamOf().toList();
	}

	/**
	 * Provides a stream of {@link TableFacet} instances.
	 *
	 * @return A stream of table facets
	 */
	Stream<T> streamOf();
}
