package guru.mikelue.foxglove.annotation;

import java.util.List;
import java.util.Optional;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;

/**
 * Reflection information for named {@link TableFacetsSource} and {@link DataGeneratorSource} objects.
 *
 * <p>
 * This interface is usually used by processing engine of test framework.
 *
 * <h2>Default methods</h2>
 *
 * For {@link #getDefaultTableFacets(Object)} and {@link #getDefaultDataGenerator(Object)},
 * the tested instance must define only one {@link TableFacetsSource} or {@link DataGeneratorSource}.
 *
 * @param <T> The type of table facet
 */
public interface DataGenContext<T extends TableFacet> {
	/**
	 * Loads data generation context from given test class.
	 *
	 * @param testClass The test class to load from
	 * @param <T> The type of table facet
	 *
	 * @return The loaded data generation context
	 */
	static <T extends TableFacet> DataGenContext<T> loadFrom(Class<?> testClass)
	{
		return new DataGenContextImpl<>(testClass);
	}

	/**
	 * Gets {@link TableFacet}s by provider class.
	 *
	 * @param clazz The type of table facets provider
	 * @param testingInstance The test instance to enclosing the provider class
	 *
	 * @return The initialized table facets
	 */
	List<T> getTypedTableFacets(Class<? extends TableFacetsProvider<T>> clazz, Object testingInstance);

	/**
	 * Gets named table facets from given test instance.
	 *
	 * @param name The name of table facets
	 * @param testingInstance The test instance to grab from
	 *
	 * @return The grabbed table facets
	 */
	List<T> getNamedTableFacets(String name, Object testingInstance);

	/**
	 * Gets {@link DataGenerator}s by provider class.
	 *
	 * @param clazz The type of provider of data generator
	 * @param testingInstance The test instance to enclosing the provider class
	 *
	 * @return The initialized data generator
	 */
	DataGenerator<T> getTypedDataGenerator(Class<? extends DataGeneratorProvider<T>> clazz, Object testingInstance);

	/**
	 * Gets named data generator from given test instance.
	 *
	 * @param name The name of data generator
	 * @param testingInstance The test instance to grab from
	 *
	 * @return The grabbed data generator
	 */
	DataGenerator<T> getNamedDataGenerator(String name, Object testingInstance);

	/**
	 * Gets default table facets from given test instance.
	 *
	 * <p>
	 * The tested instance must define only one {@link TableFacetsSource},
	 * otherwise exception is thrown.
	 *
	 * @param testingInstance The test instance to grab from
	 *
	 * @return The grabbed table facets
	 */
	Optional<List<T>> getDefaultTableFacets(Object testingInstance);

	/**
	 * Gets default data generator from given test instance.
	 *
	 * <p>
	 * The tested instance must define only one {@link DataGeneratorSource},
	 * otherwise exception is thrown.
	 *
	 * @param testingInstance The test instance to grab from
	 *
	 * @return The grabbed data generator
	 */
	Optional<DataGenerator<T>> getDefaultDataGenerator(Object testingInstance);
}
