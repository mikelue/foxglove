package guru.mikelue.foxglove.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.function.Supplier;

import guru.mikelue.foxglove.TableFacet;

/**
 * Worker class for test framework integration.
 */
public class GenDataProcessor {
	private final boolean hasGenData;

	private final Object testingInstance;
	private final AnnotatedElement annotatedElement;
	private final Supplier<DataGenContext<TableFacet>> supplierOfDataGenContext;
	private final String name;

	/**
	 * Creates an instance of {@link GenDataProcessor} on a tested method.
	 *
	 * @param testingInstance The instance of tested class
	 * @param annotatedElement The {@link GenData} annotated element (class or method)
	 * @param supplierOfDataGenContext The supplier of data generation context
	 * @param name The name of tested method or class
	 */
	public GenDataProcessor(
		Object testingInstance, AnnotatedElement annotatedElement,
		Supplier<DataGenContext<TableFacet>> supplierOfDataGenContext,
		String name
	) {
		var genData = annotatedElement.getAnnotation(GenData.class);

		this.hasGenData = (genData != null);
		this.testingInstance = testingInstance;
		this.annotatedElement = annotatedElement;
		this.supplierOfDataGenContext = supplierOfDataGenContext;
		this.name = name;
	}

	/**
	 * Indicates whether {@link GenData} is present on tested method of current.
	 *
	 * @return true if data generation is required; false otherwise
	 */
	public boolean hasDataGenerating()
	{
		return hasGenData;
	}

	/**
	 * Performs data generating action.
	 *
	 * @return The number of generated rows; -1 if no generation is performed
	 *
	 * @throws Exception if any error is encountered during data generation
	 */
	public int performGenerating() throws Exception
	{
		if (!hasDataGenerating()) {
			return -1;
		}

		var objectFactory = buildObjectFactory(
			testingInstance,
			annotatedElement.getAnnotation(GenData.class),
			supplierOfDataGenContext.get(),
			name
		);

		/*
		 * Prepares data generator and table facets
		 */
		var dataGenerator = objectFactory.getDataGenerator();
		var tableFacets = objectFactory.getTableFacets();
		// :~)

		return dataGenerator.generate(tableFacets);
	}

	/**
	 * Builds the instance of {@link GenDataObjectFactory}.
	 *
	 * <p>
	 * This method may be overridden by subclasses to provide customized
	 * factory of object.
	 *
	 * @param testingInstance The instance of tested class
	 * @param genData The information of {@link GenData}
	 * @param dataGenContext The context for data generating
	 * @param name The name of tested method or class
	 *
	 * @return The instance of {@link GenDataObjectFactory}
	 */
	protected GenDataObjectFactory buildObjectFactory(
		Object testingInstance, GenData genData,
		DataGenContext<TableFacet> dataGenContext,
		String name
	) {
		return new ReflectGenDataObjectFactory(
			testingInstance, genData,
			dataGenContext,
			name
		);
	}
}
