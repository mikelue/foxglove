package guru.mikelue.foxglove.junit;

import java.util.ArrayList;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.platform.commons.JUnitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.DataGenContext;
import guru.mikelue.foxglove.annotation.GenDataProcessor;
import guru.mikelue.foxglove.annotation.LayeredDataGenContext;

/**
 * JUnit extension for Foxglove.
 *
 * This extension implements semantics defined by {@link guru.mikelue.foxglove.annotation annotation doc}.
 */
public class FoxgloveJUnitExtension implements BeforeAllCallback, BeforeEachCallback {
	private final Logger logger = LoggerFactory.getLogger(FoxgloveJUnitExtension.class);

	private final static String DATA_GEN_CONTEXT = "foxglove.data_gen";
	private final static String DATA_GEN_ON_CLASS_LEVEL = "foxglove.data_gen.on_class_level";

	/**
	 * Default constructor should get called by JUnit engine.
	 */
	public FoxgloveJUnitExtension() {}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception
	{
		var targetClass = context.getTestClass().get();
		var namespace = Namespace.create(targetClass);

		var storeForTestedClass = context.getStore(namespace);

		storeForTestedClass.getOrComputeIfAbsent(
			DATA_GEN_CONTEXT,
			key -> DataGenContext.loadFrom(targetClass)
		);
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception
	{
		var storeForTestedClass = context.getStore(
			Namespace.create(context.getRequiredTestClass())
		);

		var contextSupplier = buildContextSupplier(context);

		/*
		 * Since we like to perform data generation on class level only once,
		 * we store the marker in the store of tested class.
		 *
		 * Because the testing instance may not be ready in {BeforeAllCallback},
		 * we do it here.
		 */
		storeForTestedClass.getOrComputeIfAbsent(
			DATA_GEN_ON_CLASS_LEVEL,
			key -> doOnClassLevel(context.getRequiredTestInstance(), contextSupplier)
		);
		// :~)

		new GenDataProcessor(
			context.getRequiredTestInstance(),
			context.getRequiredTestMethod(),
			contextSupplier,
			context.getRequiredTestMethod().getName()
		)
			.performGenerating();
	}

	private Integer doOnClassLevel(Object testingInstance, Supplier<DataGenContext<TableFacet>> dataGenContextSupplier)
	{
		var processor = new GenDataProcessor(
			testingInstance, testingInstance.getClass(), dataGenContextSupplier,
			testingInstance.getClass().getSimpleName()
		);

		if (!processor.hasDataGenerating()) {
			return -1;
		}

		logger.debug("Performing data generation on class level: \"{}\"",
			testingInstance.getClass().getSimpleName()
		);

		try {
			return processor.performGenerating();
		} catch (Exception e) {
			throw new JUnitException(
				String.format(
					"Preparing @GenData on class level has error: \"%s\"",
					testingInstance.getClass().getSimpleName()
				),
				e
			);
		}
	}

	/**
	 * Builds layered {@link DataGenContext} supplier if the testing class is a member class.
	 */
	private static Supplier<DataGenContext<TableFacet>> buildContextSupplier(ExtensionContext context)
	{
		var testingClass = context.getRequiredTestClass();

		if (testingClass.isMemberClass()) {
			return () -> {
				var testingInstances = context.getRequiredTestInstances().getAllInstances();
				var genDataContexts = new ArrayList<DataGenContext<TableFacet>>(testingInstances.size());

				for (var testingInstance : testingInstances) {
					var namespace = Namespace.create(testingInstance.getClass());
					var store = context.getStore(namespace);

					@SuppressWarnings("unchecked")
					var dataGenContext = (DataGenContext<TableFacet>)
						store.get(DATA_GEN_CONTEXT, DataGenContext.class);

					genDataContexts.add(dataGenContext);
				}

				return new LayeredDataGenContext<>(
					testingInstances, genDataContexts
				);
			};
		}

		return () -> {
			var namespace = Namespace.create(testingClass);
			var store = context.getStore(namespace);

			@SuppressWarnings("unchecked")
			var dataGenContext = (DataGenContext<TableFacet>)
				store.get(DATA_GEN_CONTEXT, DataGenContext.class);

			return dataGenContext;
		};
	}
}
