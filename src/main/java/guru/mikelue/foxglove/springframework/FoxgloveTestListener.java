package guru.mikelue.foxglove.springframework;

import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.TestTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.*;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;

/**
 * This listener resolving {@link TableFacetsSource} and {@link DataGeneratorSource}
 * in Spring's Context framework of test first.
 *
 * <h2>From Spring's context</h2>
 *
 * <ol>
 *    <li>For named sources, using {@link BeanFactory#getBean(String)} to get object</li>
 *    <li>For subtype of {@link TableFacetsProvider} or {@link DataGeneratorProvider}, using {@link BeanFactory#getBean(Class)} to get object of that type.</li>
 *    <li>For {@link DataGenerator}, using {@link BeanFactory#getBean(Class)} with type of
 *    	{@link DataGeneratorProvider TableFacetsProvider.class}, or {@link DataGenerator DataGenerator.class}.<br>
 *    	This method would throw {@link NoUniqueBeanDefinitionException} if there are multiple candidates in the context.
 *    </li>
 *    <li>For {@link TableFacet}, using {@link BeanFactory#getBean(Class)} with type of
 *    	{@link TableFacetsProvider TableFacetsProvider.class}.<br>
 *    	This method would throw {@link NoUniqueBeanDefinitionException} if there are multiple candidates in the context.
 *    </li>
 * </ol>
 *
 * <h3>Auto-constructing default {@link DataGenerator}</h3>
 *
 * If the Spring's context cannot find a viable {@link DataGenerator} bean,
 * this listener will construct with:
 *
 * <ol>
 *    <li>Using {@link TestTransaction#isActive} and {@link DataSourceUtils#getConnection(DataSource)} to get current transaction's connection for data generation.</li>
 *    <li>Otherwise, using {@link DataSource} to construct a new {@link DataGenerator}(prototype scope).</li>
 * </ol>
 *
 * <p>
 * Otherwise, this listener follows the semantics defined by {@link guru.mikelue.foxglove.annotation annotation doc}.
 *
 * <h2>Nested test classes</h2>
 *
 * <p>
 * This listener doesn't support resolving sources from enclosing classes.<br>
 * However, you could use Spring's {@link NestedTestConfiguration} to resolve sources from Spring's context.
 *
 * @see JdbcDataGenerator
 * @see TableFacetsSource
 * @see DataGeneratorSource
 * @see NestedTestConfiguration
 */
public class FoxgloveTestListener extends AbstractTestExecutionListener {
	private final Logger logger = LoggerFactory.getLogger(FoxgloveTestListener.class);

	/**
	 * The default order of this listener is just after {@link SqlScriptsTestExecutionListener}.
	 */
	public final static int ORDER = 5000 + 1;

	private final static String ATTR_DATA_GEN_CONTEXT_PREFIX = "foxglove.data_gen_context.";
	private final static String ATTR_CLASS_LEVEL_PREFIX = "foxglove.class_level.";

	/**
	 * Default constructor.
	 */
	public FoxgloveTestListener() {}

	/**
	 * The order of this listener is just after {@link SqlScriptsTestExecutionListener}.
	 *
	 * @return The order value
	 *
	 * @see #ORDER
	 */
	@Override
	public int getOrder()
	{
		return ORDER;
	}

	@Override
	public void beforeTestClass(TestContext testContext) throws Exception
	{
		testContext.computeAttribute(
			keyForDataGenContext(testContext.getTestClass()),
			clazz -> DataGenContext.loadFrom(
				testContext.getTestClass()
			)
		);
	}

    @SuppressWarnings("unchecked")
	@Override
    public void beforeTestMethod(TestContext testContext) throws Exception
    {
		Supplier<DataGenContext<TableFacet>> contextSupplier = () ->
			(DataGenContext<TableFacet>)testContext.getAttribute(
				keyForDataGenContext(testContext.getTestClass())
			);

		/*
		 * Perform data generation on class level only once.
		 */
		var classLevelAttrName = ATTR_CLASS_LEVEL_PREFIX + testContext.getTestClass().getCanonicalName();
		testContext.computeAttribute(
			classLevelAttrName,
			name -> doOnClassLevel(
				testContext, contextSupplier
			)
		);
		// :~)

		new SpringContextGenDataProcessor(
			testContext.getApplicationContext(),
			testContext.getTestInstance(), testContext.getTestMethod(),
			contextSupplier,
			testContext.getTestMethod().getName()
		)
			.performGenerating();
    }

	private Integer doOnClassLevel(
		TestContext testContext, Supplier<DataGenContext<TableFacet>> dataGenContextSupplier
	) {
		var testingInstance = testContext.getTestInstance();
		var testingClass = testContext.getTestClass();

		var processor = new SpringContextGenDataProcessor(
			testContext.getApplicationContext(),
			testingInstance, testingClass, dataGenContextSupplier,
			testingClass.getSimpleName()
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
			throw new RuntimeException(
				"Failed to generating data for class level: " + testingInstance.getClass().getSimpleName(),
				e
			);
		}
	}

	private static String keyForDataGenContext(Class<?> testedClass)
	{
		return ATTR_DATA_GEN_CONTEXT_PREFIX + testedClass.getCanonicalName();
	}
}

class SpringContextGenDataProcessor extends GenDataProcessor {
	private final ApplicationContext appContext;

	public SpringContextGenDataProcessor(
		ApplicationContext appContext,
		Object testingInstance, java.lang.reflect.AnnotatedElement annotatedElement,
		Supplier<DataGenContext<TableFacet>> dataGenContextSupplier,
		String name
	) {
		super(
			testingInstance, annotatedElement,
			dataGenContextSupplier, name
		);

		this.appContext = appContext;
	}

	@Override
	protected GenDataObjectFactory buildObjectFactory(
		Object testingInstance, GenData genData,
		DataGenContext<TableFacet> dataGenContext,
		String name
	) {
		return new SpringContextObjectFactory(
			appContext,
			testingInstance, genData,
			dataGenContext, name
		);
	}
}
