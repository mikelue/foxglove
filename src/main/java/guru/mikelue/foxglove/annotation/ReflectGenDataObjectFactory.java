package guru.mikelue.foxglove.annotation;

import java.util.ArrayList;
import java.util.List;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;

/**
 * Uses reflection to create data generation objects defined by {@link GenData}.
 */
public class ReflectGenDataObjectFactory implements GenDataObjectFactory {
	private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReflectGenDataObjectFactory.class);

	private final Object testingInstance;
	private final DataGenContext<TableFacet> dataGenContext;
	private final GenDataInspector genDataInspector;
	private final String nameOfTestedObject;

	/**
	 * Creates an instance of {@link ReflectGenDataObjectFactory} on a testing instance.
	 *
	 * @param testingInstance The instance of tested class
	 * @param genData The information of {@link GenData}
	 * @param dataGenContext The data generation context
	 * @param name The name of tested method
	 */
	public ReflectGenDataObjectFactory(
		Object testingInstance,
		GenData genData, DataGenContext<TableFacet> dataGenContext,
		String name
	) {
		this.genDataInspector = GenDataInspector.of(genData);
		this.testingInstance = testingInstance;
		this.dataGenContext = dataGenContext;
		this.nameOfTestedObject = name;
	}

	@Override
	public DataGenerator<TableFacet> getDataGenerator() throws Exception
	{
		var targetClass = testingInstance.getClass();

		if (genDataInspector.useDefaultDataGenerator()) {
			logger.debug("Using default DataGenerator for: {}", nameOfTestedObject);

			return dataGenContext.getDefaultDataGenerator(testingInstance)
				.orElseThrow(() -> new IllegalStateException(
					"Default DataGenerator is not configured in class: " + targetClass.getSimpleName()
				));
		}

		var genData = genDataInspector.genData();

		if (genData.generatorName().isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using specified DataGenerator provider class[{}] for: {}",
					genData.generator(), nameOfTestedObject);
			}

			@SuppressWarnings("unchecked")
			var typeOfProvider = (Class<DataGeneratorProvider<TableFacet>>)genData.generator();
			return dataGenContext.getTypedDataGenerator(
				typeOfProvider, testingInstance
			);
		}

		return dataGenContext.getNamedDataGenerator(
			genData.generatorName(), testingInstance
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<TableFacet> getTableFacets() throws Exception
	{
		var targetClass = testingInstance.getClass();

		if (genDataInspector.useDefaultTableFacets()) {
			logger.debug("Using default table facets for: {}", nameOfTestedObject);

			return dataGenContext.getDefaultTableFacets(testingInstance)
				.orElseThrow(() -> new IllegalStateException(
					"Default TableFacets is not configured in class: " + targetClass.getSimpleName()
				));
		}

		var tableFacets = new ArrayList<TableFacet>(genDataInspector.numberOfTableFacets());
		var genData = genDataInspector.genData();

		/*
		 * Adds table facets from value() and facets()
		 */

		if (logger.isDebugEnabled()) {
			logger.debug("TableFacets config: [value() #{}]. [facets() #{}]. [facetsNames() #{}]. for: {}",
				genData.value().length, genData.facets().length,
				genData.facetsNames().length,
				nameOfTestedObject
			);
		}

		for (var facetProviderClass: genData.value()) {
			var typedProviderClass = (Class<TableFacetsProvider<TableFacet>>)facetProviderClass;
			tableFacets.addAll(
				dataGenContext.getTypedTableFacets(typedProviderClass, testingInstance)
			);
		}
		for (var facetProviderClass: genData.facets()) {
			var typedProviderClass = (Class<TableFacetsProvider<TableFacet>>)facetProviderClass;
			tableFacets.addAll(
				dataGenContext.getTypedTableFacets(typedProviderClass, testingInstance)
			);
		}
		// :~)

		/*
		 * Adds table facets from facetsNames()
		 */
		for (var facetName: genDataInspector.genData().facetsNames()) {
			tableFacets.addAll(
				dataGenContext.getNamedTableFacets(facetName, testingInstance)
			);
		}
		// :~)

		return tableFacets;
	}
}
