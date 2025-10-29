package guru.mikelue.foxglove;

import java.util.List;

import guru.mikelue.foxglove.setting.SettingAware;

/**
 * Defines the common operators for data generating.
 *
 * @param <T> The type of table facet
 */
public interface DataGenerator<T extends TableFacet> extends SettingAware<DataGenerator<T>> {
	/**
	 * Performs the data generating for the given table facets.
	 *
	 * @param tables The table facets
	 *
	 * @return The total number of rows generated
	 *
	 * @see #generate(List)
	 */
	@SuppressWarnings("unchecked")
	default int generate(T... tables)
	{
		return generate(java.util.Arrays.asList(tables));
	}

	/**
	 * Performs the data generating for the given table facets.
	 *
	 * @param tables The table facets
	 *
	 * @return The total number of rows generated
	 *
	 * @see #generate(T...)
	 */
	int generate(List<T> tables);
}
