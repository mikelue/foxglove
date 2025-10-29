package guru.mikelue.foxglove.functional;

import java.util.List;

import guru.mikelue.foxglove.TableFacet;

/**
 * Convenient interface to provide a single {@link TableFacet}.
 *
 * @param <T> The type of table facet
 *
 * @see TableFacetsProvider
 */
@FunctionalInterface
public interface TableFacetProvider<T extends TableFacet> extends TableFacetsProvider<T> {
	/**
	 * Provides a list of with only one {@link TableFacet} instance.
	 *
	 * @return A list of table facet
	 */
	@Override
	default List<T> get()
	{
		return List.of(getOne());
	}

	/**
	 * Provides a {@link TableFacet} instance.
	 *
	 * @return A table facet instance
	 */
	T getOne();
}
