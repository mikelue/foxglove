package guru.mikelue.foxglove.annotation;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;

/**
 * Context class for layered data generations.
 *
 * <p>
 * This class is intended to support layered testing structure like
 * <a href="https://docs.junit.org/current/user-guide/#writing-tests-nested">Nested Tests</a> in JUnit.
 *
 * <p>
 * The methods accepting the testing instance as parameter are ignored in this class.
 *
 * @param <T> The type of table facet
 */
public class LayeredDataGenContext<T extends TableFacet> implements DataGenContext<T> {
	private final List<Pair<Object, DataGenContextImpl<T>>> pairedContexts;

	/**
	 * Creates an instance with multiple {@link DataGenContext}s.
	 *
	 * <p>
	 * The first item in the list has the highest priority when searching for.
	 *
	 * @param testingInstances The list of testing instances for each context
	 * @param layeredContexts The list of layered data generation contexts
	 */
	public LayeredDataGenContext(
		List<Object> testingInstances,
		List<DataGenContext<T>> layeredContexts
	) {
		pairedContexts = new ArrayList<>(testingInstances.size());

		for (int i = 0; i < testingInstances.size(); i++) {
			pairedContexts.add(
				Pair.of(
					testingInstances.get(i),
					(DataGenContextImpl<T>)layeredContexts.get(i)
				)
			);
		}
	}

	@Override
	public List<T> getTypedTableFacets(Class<? extends TableFacetsProvider<T>> clazz, Object testingInstance)
	{
		var pair = getDataGenContext(clazz, testingInstance);
		return pair.getRight().getTypedTableFacets(clazz, pair.getLeft());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<T> getNamedTableFacets(String name, Object testingInstance)
	{
		return pairedContexts.stream()
			.filter(pair -> pair.getRight().hasNamedTableFacets(name))
			.map(pair -> pair.getRight().getNamedTableFacets(name, pair.getLeft()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				String.format(
					"No @DataFacetsSource found for name: [%s]", name
				)
			));
	}

	@Override
	public DataGenerator<T> getTypedDataGenerator(Class<? extends DataGeneratorProvider<T>> clazz,
		Object testingInstance)
	{
		var pair = getDataGenContext(clazz, testingInstance);
		return pair.getRight().getTypedDataGenerator(clazz, pair.getLeft());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataGenerator<T> getNamedDataGenerator(String name, Object testingInstance)
	{
		return pairedContexts.stream()
			.filter(pair -> pair.getRight().hasNamedDataGenerator(name))
			.map(pair -> pair.getRight().getNamedDataGenerator(name, pair.getLeft()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				String.format(
					"No @DataGeneratorSource found for name: [%s]", name
				)
			));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<List<T>> getDefaultTableFacets(Object testingInstance)
	{
		return pairedContexts.stream()
			.map(pair -> pair.getRight().getDefaultTableFacets(pair.getLeft()))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<DataGenerator<T>> getDefaultDataGenerator(Object testingInstance)
	{
		return pairedContexts.stream()
			.map(pair -> pair.getRight().getDefaultDataGenerator(pair.getLeft()))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
	}

	private Pair<Object, DataGenContextImpl<T>> getDataGenContext(Class<?> clazz, Object testingInstance)
	{
		if (Modifier.isStatic(clazz.getModifiers()) || !clazz.isMemberClass()) {
			return pairedContexts.get(0);
		}

		return pairedContexts.stream()
			.filter(pair -> {
				var typeOfInstance = pair.getLeft().getClass();
				if (typeOfInstance.equals(clazz.getEnclosingClass())) {
					return true;
				}

				return false;
			})
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				String.format(
					"No enclosing class found for provider class: [%s]", clazz.getSimpleName()
				)
			));
	}
}
