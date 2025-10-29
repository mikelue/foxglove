package guru.mikelue.foxglove.annotation;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;
import guru.mikelue.foxglove.setting.DataSettingInfo;
import mockit.Expectations;
import mockit.Mocked;

import static java.util.Collections.emptyList;

public class ReflectGenDataObjectFactoryTest extends AbstractTestBase {
	@Mocked
	private GenData mockGenData;

	@Mocked
	private DataGenContext<TableFacet> mockDataGenContext;

	private static DataGenerator<TableFacet> fakeDataGenerator = new FakeDataGenerator();

	public ReflectGenDataObjectFactoryTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	@ParameterizedTest
	@MethodSource
	void getDataGenerator(
		String generatorName, Class<? extends DataGeneratorProvider<TableFacet>> providerClass,
		BiConsumer<DataGenContext<TableFacet>, Object> verifyFunction
	) throws Exception {
		var currentObject = this;

		new Expectations() {{
			mockGenData.generatorName();
			result = generatorName;

			mockGenData.generator();
			result = providerClass;
		}};

		verifyFunction.accept(mockDataGenContext, currentObject);

		if (!generatorName.isEmpty()) {
			new Expectations() {{
				mockDataGenContext.getNamedDataGenerator(
					generatorName, currentObject
				);
				result = fakeDataGenerator;
				times = 1;
			}};
		}

		if (!GenData.FallbackDataGeneratorProvider.class.equals(providerClass)) {
			new Expectations() {{
				mockDataGenContext.getTypedDataGenerator(
					providerClass, currentObject
				);
				result = fakeDataGenerator;
				times = 1;
			}};
		}

		newTestedFactory().getDataGenerator();
	}

	class FakeDataGeneratorProvider implements DataGeneratorProvider<TableFacet> {
		@Override
		public DataGenerator<TableFacet> get()
		{
			return fakeDataGenerator;
		}
	}

	static Stream<Arguments> getDataGenerator()
	{
		record TestCase(
			String generatorName,
			Class<? extends DataGeneratorProvider<TableFacet>> providerClass,
			BiConsumer<DataGenContext<TableFacet>, Object> verifyFunction
		) {}

		return Stream.<TestCase>of(
			// Default
			new TestCase(
				"", GenData.FallbackDataGeneratorProvider.class,
				(context, obj) -> {
					new Expectations() {{
						context.getDefaultDataGenerator(obj);
						result = Optional.of(fakeDataGenerator);
						times = 1;
					}};
				}
			),
			// Uses class of provider
			new TestCase(
				"", FakeDataGeneratorProvider.class,
				(context, obj) -> {}
			),
			// Uses named data generator
			new TestCase(
				"fakeName", GenData.FallbackDataGeneratorProvider.class,
				(context, obj) -> {}
			)
		)
			.map(testCase -> Arguments.of(
				testCase.generatorName,
				testCase.providerClass,
				testCase.verifyFunction
			));
	}

	/**
	 * Tests the getting of {@link TableFacet}s.
	 */
	@ParameterizedTest
	@MethodSource
	void getTableFacets(
		Class<? extends TableFacetsProvider<TableFacet>>[] providers,
		String[] names,
		BiConsumer<DataGenContext<TableFacet>, Object> verifyFunction
	) throws Exception {
		var currentObject = this;

		/*
		 * Mocks the GenData info
		 */
		new Expectations() {{
			mockGenData.generatorName();
			result = "";

			mockGenData.value();
			result = providers;

			mockGenData.facets();
			result = providers;

			mockGenData.facetsNames();
			result = names;
		}};

		for (var providerClass: providers) {
			new Expectations() {{
				mockDataGenContext.getTypedTableFacets(
					providerClass, currentObject
				);
				result = emptyList();
				times = 2;
			}};
		}

		for (var name: names) {
			new Expectations() {{
				mockDataGenContext.getNamedTableFacets(
					name, currentObject
				);
				result = emptyList();
				times = 1;
			}};
		}
		// :~)

		verifyFunction.accept(mockDataGenContext, currentObject);

		newTestedFactory().getTableFacets();
	}

	class FakeTableFacetsProvider_1 implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return null;
		}
	}
	class FakeTableFacetsProvider_2 implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return null;
		}
	}

	static Stream<Arguments> getTableFacets()
	{
		record TestCase(
			Class<?>[] providers,
			String[] names,
			BiConsumer<DataGenContext<TableFacet>, Object> verifyFunction
		) {}

		return Stream.<TestCase>of(
			// By default resolving
			new TestCase(
				new Class<?>[0], new String[0],
				(context, obj) -> {
					new Expectations() {{
						context.getDefaultTableFacets(obj);
						result = Optional.of(emptyList());
						times = 1;
					}};
				}
			),
			// By class of providers
			new TestCase(
				new Class<?>[] { FakeTableFacetsProvider_1.class, FakeTableFacetsProvider_2.class },
				new String[0],
				(context, obj) -> {}
			),
			// By names of facets
			new TestCase(
				new Class<?>[0],
				new String[] { "fakeName1", "fakeName2" },
				(context, obj) -> {}
			)
		)
			.map(testCase -> Arguments.of(
				testCase.providers,
				testCase.names,
				testCase.verifyFunction
			));
	}

	private ReflectGenDataObjectFactory newTestedFactory()
	{
		return new ReflectGenDataObjectFactory(
			this,
			mockGenData, mockDataGenContext,
			"Test-Main"
		);
	}
}

class FakeDataGenerator implements DataGenerator<TableFacet> {
	@Override
	public DataGenerator<TableFacet> withSetting(DataSettingInfo setting)
	{
		throw new UnsupportedOperationException("Unimplemented method 'withSetting'");
	}

	@Override
	public int generate(List<TableFacet> tables)
	{
		throw new UnsupportedOperationException("Unimplemented method 'generate'");
	}
}
