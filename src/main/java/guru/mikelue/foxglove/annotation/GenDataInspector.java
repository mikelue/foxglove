package guru.mikelue.foxglove.annotation;

import org.apache.commons.lang3.Validate;

import guru.mikelue.foxglove.annotation.GenData.FallbackDataGeneratorProvider;

/**
 * A convenient interface to process objects defined by {@link GenData}.
 */
public interface GenDataInspector {
	/**
	 * Creates an instance of {@link GenDataInspector}.
	 *
	 * @param genData The annotation instance of {@link GenData}
	 *
	 * @return The instance of {@link GenDataInspector}
	 */
	public static GenDataInspector of(GenData genData)
	{
		Validate.isTrue(
			genData.generatorName().isEmpty() ||
			genData.generator() == FallbackDataGeneratorProvider.class,
			"Only one of 'generatorName()' and 'generator()' should be used"
		);

		return new GenDataInspectorImpl(genData);
	}

	/**
	 * Returns the annotation instance of {@link GenData}.
	 *
	 * @return The annotation instance of {@link GenData}
	 */
	GenData genData();

	/**
	 * Returns the number of table facets defined in {@link GenData}.
	 *
	 * @return The number of table facets
	 */
	int numberOfTableFacets();

	/**
	 * Indicates whether the default data generator is used.
	 *
	 * <p>
	 * The default data generator is used when both of the following conditions are met:
	 * <ul>
	 *   <li>The {@link GenData#generatorName()} is not set (empty string),</li>
	 *   <li>The {@link GenData#generator()} is set to {@link FallbackDataGeneratorProvider}.</li>
	 * </ul>
	 *
	 * @return true if the default data generator is used; false otherwise
	 */
	boolean useDefaultDataGenerator();
	/**
	 * Indicates whether the default table facets are used.
	 *
	 * <p>
	 * The default table facets are used when all of the following conditions are met:
	 *
	 * <ul>
	 *   <li>The {@link GenData#facetsNames()} is not set (empty array),</li>
	 *   <li>The {@link GenData#value()} is not set (empty array),</li>
	 *   <li>The {@link GenData#facets()} is not set (empty array).</li>
	 * </ul>
	 *
	 * @return true if the default table facets are used; false otherwise
	 */
	boolean useDefaultTableFacets();
}

class GenDataInspectorImpl implements GenDataInspector {
	private final GenData genData;
	private final boolean useDefaultDataGenerator;
	private final boolean useDefaultTableFacets;
	private final int numberOfTableFacets;

	GenDataInspectorImpl(GenData genData)
	{
		this.genData = genData;

		this.useDefaultDataGenerator = genData.generatorName().isEmpty() &&
			genData.generator() == FallbackDataGeneratorProvider.class;

		this.numberOfTableFacets = genData.value().length +
			genData.facets().length +
			genData.facetsNames().length;

		this.useDefaultTableFacets = numberOfTableFacets == 0;
	}

	@Override
	public GenData genData()
	{
		return this.genData;
	}

	@Override
	public boolean useDefaultDataGenerator()
	{
		return this.useDefaultDataGenerator;
	}

	@Override
	public boolean useDefaultTableFacets()
	{
		return this.useDefaultTableFacets;
	}

	@Override
	public int numberOfTableFacets()
	{
		return this.numberOfTableFacets;
	}
}
