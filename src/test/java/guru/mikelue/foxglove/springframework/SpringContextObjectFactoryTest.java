package guru.mikelue.foxglove.springframework;

import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.DataGenContext;
import guru.mikelue.foxglove.annotation.DataGeneratorSource;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.setting.DataSettingInfo;
import mockit.Mocked;
import mockit.Verifications;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringContextObjectFactoryTest extends AbstractTestBase {
	@Mocked
	private DataGenContext<TableFacet> mockDataGenContext;

	private static DataGenContext<TableFacet> dataGenContext =
		DataGenContext.loadFrom(SpringContextObjectFactoryTest.class);

	@DataGeneratorSource
	private DataGenerator<TableFacet> defaultDataGenerator = new FakeDataGenerator();

	@TableFacetsSource
	private TableFacet defaultFacet = JdbcTableFacet.builder("TABLE_DEFAULT")
		.column("col_default").fixed("default")
		.numberOfRows(3)
		.build();

	public SpringContextObjectFactoryTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the fallback to field-defined {@link DataGenerator}.
	 */
	@Test
	void fallbackToDefaultDataGenerator() throws Exception
	{
		var appContext = new AnnotationConfigApplicationContext(EmptyConfig.class);

		var testedFactory = new SpringContextObjectFactory(
			appContext, this, getByMethod("defaultMethod"),
			dataGenContext, "defaultMethod"
		);

		var testedGenerator = (FakeDataGenerator)testedFactory.getDataGenerator();
		assertThat(testedGenerator.id)
			.isEqualTo(-1);
	}

	/**
	 * Tests the getting of {@link DataGenerator} for various cases.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"defaultMethod",
		"nameSourceByProvider",
		"namedSourcesByObject",
		"typedSources"
	})
	void getDataGeneratorBySpring(
		String methodName
	) throws Exception {
		var appContext = new AnnotationConfigApplicationContext(SpringProvidingConfig.class);
		var sampleTestingObject = new Object();

		var testedFactory = new SpringContextObjectFactory(
			appContext, sampleTestingObject, getByMethod(methodName),
			mockDataGenContext, methodName
		);

		/*
		 * Asserts the generator comes from Spring context
		 */
		var testedGenerator = (FakeDataGenerator)testedFactory.getDataGenerator();
		assertThat(testedGenerator.id)
			.isEqualTo(1);
		// :~)

		verifyNoneOfDataGenContextCalled(sampleTestingObject);
	}

	/**
	 * Tests the auto-constructed of {@link DataGenerator} by {@link DataSource}.
	 */
	@Test
	void autoDataGeneratorByDataSource() throws Exception
	{
		var sampleTestingObject = new Object();

		DataGenerator<TableFacet> testedGenerator;

		try (var appContext = new AnnotationConfigApplicationContext(DataSourceSpringContext.class)) {
			var testedFactory = new SpringContextObjectFactory(
				appContext, sampleTestingObject, getByMethod("defaultMethod"),
				mockDataGenContext, "defaultMethod"
			);

			testedGenerator = testedFactory.getDataGenerator();
		}

		assertThat(testedGenerator)
			.isNotInstanceOf(FakeDataGenerator.class);

		verifyNoneOfDataGenContextCalled(sampleTestingObject);
	}

	@SuppressWarnings({ "unchecked" })
	private void verifyNoneOfDataGenContextCalled(Object sampleTestingObject)
	{
		new Verifications() {{
			mockDataGenContext.getDefaultDataGenerator(sampleTestingObject);
			times = 0;

			mockDataGenContext.getTypedDataGenerator(
				(Class<DataGeneratorProvider<TableFacet>>)any, sampleTestingObject
			);
			times = 0;

			mockDataGenContext.getNamedDataGenerator(
				anyString, sampleTestingObject
			);
			times = 0;
		}};
	}

	/**
	 * Tests the fallback to {@link TableFacet}s defined by testing class.
	 */
	@Test
	void fallbackToDefaultTableFacet() throws Exception
	{
		var appContext = new AnnotationConfigApplicationContext(EmptyConfig.class);

		var testedFactory = new SpringContextObjectFactory(
			appContext, this, getByMethod("defaultMethod"),
			dataGenContext, "defaultMethod"
		);

		var testedGenerator = testedFactory.getTableFacets();
		assertThat(testedGenerator)
			.element(0)
			.extracting(t -> t.getNumberOfRows())
			.isEqualTo(3);
	}

	/**
	 * Tests the getting of {@link TableFacet}s list from Spring's context.
	 */
	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@CsvSource({
		"defaultMethod,1",
		"nameSourceByProvider,4",
		"namedSourcesByObject,4",
		"typedSources,1",
	})
	void getTableFacetsBySpring(
		String methodName, int expectedSize
	) throws Exception {
		var appContext = new AnnotationConfigApplicationContext(SpringProvidingConfig.class);
		var sampleTestingObject = new Object();

		var testedFactory = new SpringContextObjectFactory(
			appContext, sampleTestingObject, getByMethod(methodName),
			mockDataGenContext, methodName
		);

		var testedFacets = testedFactory.getTableFacets();
		assertThat(testedFacets)
			.hasSize(expectedSize)
			.allSatisfy(t -> {
				assertThat(t.getNumberOfRows())
					.isEqualTo(5);
			});

		new Verifications() {{
			mockDataGenContext.getDefaultTableFacets(sampleTestingObject);
			times = 0;

			mockDataGenContext.getTypedTableFacets(
				(Class<? extends TableFacetsProvider<TableFacet>>)any, methodName
			);
			times = 0;

			mockDataGenContext.getNamedTableFacets(anyString, sampleTestingObject);
			times = 0;
		}};
	}

	private static GenData getByMethod(String methodName)
	{
		return MethodUtils.getMatchingMethod(
			GenDataCases.class, methodName
		)
			.getAnnotation(GenData.class);
	}
}

class GenDataCases {
	@GenData
	void defaultMethod() {}

	@GenData(
		generatorName = "redDataGenerator",
		facetsNames = { "redTableFacet", "tableFacetOfList", "tableFacetOfArray", "tableFacetOfStream" }
	)
	void nameSourceByProvider() {}

	@GenData(
		generatorName = "blueDataGenerator",
		facetsNames = { "blueTableFacet", "tableFacetOfList", "tableFacetOfArray", "tableFacetOfStream" }
	)
	void namedSourcesByObject() {}

	@GenData(
		generator = SpringProvidingConfig.RedDataGeneratorProvider.class,
		facets = { SpringProvidingConfig.RedTableFacetProvider.class }
	)
	void typedSources() {}
}

@Configuration
class EmptyConfig {}

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Configuration
class DataSourceSpringContext {}

@Configuration
class SpringProvidingConfig {
	static TableFacet sampleFacet = JdbcTableFacet.builder("TABLE_SAMPLE")
		.column("col_sample").fixed("sample")
		.numberOfRows(5)
		.build();

	static class RedDataGeneratorProvider implements DataGeneratorProvider<TableFacet> {
		@Override
		public DataGenerator<TableFacet> get()
		{
			return new FakeDataGenerator(1);
		}
	}
	static class RedTableFacetProvider implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return sampleFacet;
		}
	}

	@Primary @Bean
	TableFacetProvider<TableFacet> defaultTableFacets()
	{
		return () -> sampleFacet;
	}

	@Primary @Bean
	DataGeneratorProvider<?> dataGeneratorProvider()
	{
		return () -> new FakeDataGenerator(1);
	}

	@Bean("redDataGenerator")
	RedDataGeneratorProvider redDataGeneratorProvider()
	{
		return new RedDataGeneratorProvider();
	}

	@Bean("blueDataGenerator")
	DataGenerator<?> blueDataGenerator()
	{
		return new FakeDataGenerator(1);
	}

	@Bean("redTableFacet")
	RedTableFacetProvider redTableFacet()
	{
		return new RedTableFacetProvider();
	}

	@Bean("blueTableFacet")
	TableFacet blueTableFacet()
	{
		return sampleFacet;
	}

	@Bean("tableFacetOfArray")
	TableFacet[] tableFacetOfArray()
	{
		return new TableFacet[] { sampleFacet };
	}

	@Bean("tableFacetOfList")
	List<TableFacet> tableFacetOfList()
	{
		return List.of(sampleFacet);
	}

	@Bean("tableFacetOfStream")
	Stream<TableFacet> tableFacetOfStream()
	{
		return Stream.of(sampleFacet);
	}
}

class FakeDataGenerator implements DataGenerator<TableFacet> {
	final int id;

	FakeDataGenerator()
	{
		this(-1);
	}

	FakeDataGenerator(int id)
	{
		this.id = id;
	}

	@Override
	public FakeDataGenerator withSetting(DataSettingInfo setting)
	{
		throw new UnsupportedOperationException("Unimplemented method 'withSetting'");
	}

	@Override
	public int generate(List<TableFacet> tables)
	{
		throw new UnsupportedOperationException("Unimplemented method 'generate'");
	}
}
