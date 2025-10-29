package guru.mikelue.foxglove.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;

import static org.assertj.core.api.Assertions.assertThat;

public class GenDataInspectorTest extends AbstractTestBase {
	public GenDataInspectorTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests whether or not using default {@DataGeneratorSource} from {@link GenData}.
	 */
	@ParameterizedTest
	@CsvSource({
		"byDefault,true",
		"byNamed,false",
	})
	void useDefaultDataGenerator(
		String sampleMethodName, boolean expectedUseDefault
	) throws NoSuchMethodException {
		var method = SampleTestInstance.class.getDeclaredMethod(sampleMethodName);
		var testedAnnotation = method.getAnnotation(GenData.class);
		var testedInspector = GenDataInspector.of(testedAnnotation);

		assertThat(testedInspector.useDefaultDataGenerator())
			.isEqualTo(expectedUseDefault);
	}

	/**
	 * Tests whether or not using default {@TableFacetsSource} from {@link GenData}.
	 */
	@ParameterizedTest
	@CsvSource({
		"byDefault,true,0",
		"byNamed,false,1",
		"multipleTableFacets,false,2",
	})
	void useDefaultTableFacets(
		String sampleMethodName,
		boolean expectedUseDefault, int expectedNumberOfFacets
	) throws NoSuchMethodException {
		var method = SampleTestInstance.class.getDeclaredMethod(sampleMethodName);
		var testedAnnotation = method.getAnnotation(GenData.class);
		var testedInspector = GenDataInspector.of(testedAnnotation);

		assertThat(testedInspector.useDefaultTableFacets())
			.isEqualTo(expectedUseDefault);
		assertThat(testedInspector.numberOfTableFacets())
			.isEqualTo(expectedNumberOfFacets);
	}
}

class SampleTestInstance {
	@GenData
	void byDefault() {}

	@GenData(generatorName = "customGenerator", facetsNames = {"customFacet"})
	void byNamed() {}

	@GenData(value = { FacetProvider1.class }, facets = { FacetProvider2.class })
	void multipleTableFacets() {}

	class FacetProvider1 implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return JdbcTableFacet.builder("table_1").build();
		}
	}

	class FacetProvider2 implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return JdbcTableFacet.builder("table_2").build();
		}
	}
}
