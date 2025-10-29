package guru.mikelue.foxglove.annotation;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.setting.DataSettingInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class DataGenContextTest extends AbstractTestBase {
	public DataGenContextTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests various cases for building of named {@link TableFacet} from method.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"byDefaults",
		"byPublic", "byProtected", "byPrivate",
		"byPublicStatic", "byProtectedStatic", "byStatic", "byPrivateStatic",
		"byList", "byStream", "byArray",
		"byPublicOfParent", "byProtectedOfParent",
		"byPublicStaticOfParent", "byProtectedStaticOfParent",
	})
	void namedTableFacetsByMethod(
		String tableFacetName
	) {
		var testedInstance = new TableFacetWarehouseOnMethod();
		var testedContextOnMethod =
			DataGenContext.loadFrom(TableFacetWarehouseOnMethod.class);

		var testedFacets = testedContextOnMethod.getNamedTableFacets(tableFacetName, testedInstance);

		assertThat(testedFacets)
			.isNotNull()
			.hasSize(1)
			.allSatisfy(facet -> {
				assertThat(facet)
					.isInstanceOf(JdbcTableFacet.class);
			});
	}

	/**
	 * Tests various cases for building of named {@link TableFacet} from field.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"byDefaults",
		"byPublic", "byProtected", "byPrivate",
		"byPublicStatic", "byProtectedStatic", "byStatic", "byPrivateStatic",
		"byList", "byStream", "byArray", "byProvider",
		"byPublicOfParent", "byProtectedOfParent",
		"byPublicStaticOfParent", "byProtectedStaticOfParent",
	})
	void namedTableFacetsByField(
		String tableFacetName
	) {
		var testedContextOnField = DataGenContext.loadFrom(TableFacetWarehouseOnField.class);

		var testedInstance = new TableFacetWarehouseOnField();
		var testedFacets = testedContextOnField.getNamedTableFacets(tableFacetName, testedInstance);

		assertThat(testedFacets)
			.isNotNull()
			.hasSize(1)
			.allSatisfy(facet -> {
				assertThat(facet)
					.isInstanceOf(JdbcTableFacet.class);
			});
	}

	/**
	 * Tests various cases for building {@link TableFacet} by providers' class.
	 */
	@ParameterizedTest
	@ValueSource(classes = {
		TableFacetWarehouseOnMethod.StaticFacetProvider.class,
		TableFacetWarehouseOnMethod.FacetProvider.class,
	})
	void typedProviderForTableFacets(
		Class<TableFacetsProvider<TableFacet>> providerClass
	) {
		var testedInstance = new TableFacetWarehouseOnMethod();
		var testedContextOnMethod =
			DataGenContext.loadFrom(TableFacetWarehouseOnMethod.class);

		var testedFacets = testedContextOnMethod.getTypedTableFacets(
			providerClass, testedInstance
		);
		assertThat(testedFacets)
			.hasSize(1)
			.allSatisfy(facet -> {
				assertThat(facet)
					.isInstanceOf(JdbcTableFacet.class);
			});
	}

	/**
	 * Tests the case for methods/fields with named {@link DataGeneratorSource}.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"byField", "byMethod",
		"byProvider",
	})
	void namedDataGeneratorSource(
		String name
	) {
		var testedContextForDataGenerator = DataGenContext.loadFrom(DataGeneratorWarehouse.class);
		var testedInstance = new DataGeneratorWarehouse();

		var testedGenerator = testedContextForDataGenerator.getNamedDataGenerator(
			name, testedInstance
		);
		assertThat(testedGenerator)
			.isNotNull()
			.isInstanceOf(DataGenerator.class);
	}

	/**
	 * Tests various cases for building {@link DataGenerator} by providers' class.
	 */
	@ParameterizedTest
	@ValueSource(classes = {
		DataGeneratorWarehouse.StaticGeneratorProvider.class,
		DataGeneratorWarehouse.GeneratorProvider.class,
	})
	void typedProviderForDataGenerator(
		Class<DataGeneratorProvider<TableFacet>> providerClass
	) {
		var testedInstance = new DataGeneratorWarehouse();
		var testedContextOnMethod =
			DataGenContext.loadFrom(DataGeneratorWarehouse.class);

		var testedGenerator = testedContextOnMethod.getTypedDataGenerator(
			providerClass, testedInstance
		);
		assertThat(testedGenerator)
			.isNotNull()
			.isInstanceOf(DataGenerator.class);
	}
}

class DataGeneratorWarehouse {
	@DataGeneratorSource
	DataGenerator<TableFacet> byField = new FakeDataGenerator();

	@DataGeneratorSource
	DataGeneratorProvider<TableFacet> byProvider = () -> new FakeDataGenerator();

	@DataGeneratorSource
	DataGenerator<TableFacet> byMethod()
	{
		return new FakeDataGenerator();
	}

	static class StaticGeneratorProvider implements DataGeneratorProvider<TableFacet> {
		@Override
		public DataGenerator<TableFacet> get()
		{
			return new FakeDataGenerator();
		}
	}

	class GeneratorProvider implements DataGeneratorProvider<TableFacet> {
		@Override
		public DataGenerator<TableFacet> get()
		{
			return new FakeDataGenerator();
		}
	}

	static class FakeDataGenerator implements DataGenerator<TableFacet> {
		@Override
		public DataGenerator<TableFacet> withSetting(DataSettingInfo setting)
		{
			return this;
		}

		@Override
		public int generate(List<TableFacet> tables)
		{
			return -1;
		}
	}
}

class TableFacetWarehouseParentOnMethod {
	@TableFacetsSource
	TableFacet byDefaults()
	{
		return builder();
	}

	@TableFacetsSource("byPublicOfParent")
	public TableFacet byPublicOfParent()
	{
		return builder();
	}

	@TableFacetsSource("byProtectedOfParent")
	protected TableFacet byProtectedOfParent()
	{
		return builder();
	}

	@TableFacetsSource("byPublicStaticOfParent")
	public static TableFacet byPublicStaticOfParent()
	{
		return builder();
	}

	@TableFacetsSource("byProtectedStaticOfParent")
	protected TableFacet byProtectedStaticOfParent()
	{
		return builder();
	}

	// Should be ignored
	@TableFacetsSource("byPrivateStatic")
	private static TableFacet carsOfPrivateStatic()
	{
		return builder();
	}

	// Should be ignored
	@TableFacetsSource("byPrivate")
	private TableFacet carsOfPrivate()
	{
		return builder();
	}

	static TableFacet builder()
	{
		return JdbcTableFacet.builder("ap_car")
			.build();
	}
}

class TableFacetWarehouseOnMethod extends TableFacetWarehouseParentOnMethod {
	static class StaticFacetProvider implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return builder();
		}
	}

	class FacetProvider implements TableFacetProvider<TableFacet> {
		@Override
		public TableFacet getOne()
		{
			return builder();
		}
	}

	// Should use the name in parent class
	@Override
	TableFacet byDefaults()
	{
		return builder();
	}

	@TableFacetsSource("byPublic")
	public TableFacet byPublic()
	{
		return builder();
	}

	@TableFacetsSource("byProtected")
	public TableFacet byProtected()
	{
		return builder();
	}

	@TableFacetsSource("byPrivate")
	private TableFacet byPrivate()
	{
		return builder();
	}

	@TableFacetsSource("byList")
	List<TableFacet> byList()
	{
		return List.of(builder());
	}

	@TableFacetsSource("byStream")
	Stream<TableFacet> byStream()
	{
		return Stream.of(builder());
	}

	@TableFacetsSource("byArray")
	TableFacet[] byArray()
	{
		return new TableFacet[] { builder() };
	}

	@TableFacetsSource("byPublicStatic")
	public static TableFacet byPublicStatic()
	{
		return builder();
	}

	@TableFacetsSource("byProtectedStatic")
	protected static TableFacet byProtectedStatic()
	{
		return builder();
	}

	@TableFacetsSource("byStatic")
	static TableFacet byStatic()
	{
		return builder();
	}

	@TableFacetsSource("byPrivateStatic")
	private static TableFacet byPrivateStatic()
	{
		return builder();
	}
}

class TableFacetWarehouseParentOnField {
	@TableFacetsSource("byDefaults")
	TableFacet byDefaults = builder();

	@TableFacetsSource("byPublicOfParent")
	public TableFacet byPublicOfParent = builder();

	@TableFacetsSource("byProtectedOfParent")
	protected TableFacet byProtectedOfParent = builder();

	@TableFacetsSource("byPublicStaticOfParent")
	public static TableFacet byPublicStaticOfParent = builder();

	@TableFacetsSource("byProtectedStaticOfParent")
	protected static TableFacet byProtectedStaticOfParent = builder();

	// Should be ignored
	@TableFacetsSource("byPrivate")
	private TableFacet byPrivate = builder();

	// Should be ignored
	@TableFacetsSource("byPrivateStatic")
	private static TableFacet byPrivateStatic = builder();

	static TableFacet builder()
	{
		return JdbcTableFacet.builder("ap_car")
			.build();
	}
}

class TableFacetWarehouseOnField extends TableFacetWarehouseParentOnField {
	// Overriding parent
	TableFacet byDefaults = builder();

	@TableFacetsSource("byPublic")
	public TableFacet byPublic = builder();

	@TableFacetsSource("byProtected")
	protected TableFacet byProtected = builder();

	@TableFacetsSource("byPrivate")
	private TableFacet byPrivate = builder();

	@TableFacetsSource("byList")
	List<TableFacet> byList = List.of(builder());

	@TableFacetsSource("byStream")
	Stream<TableFacet> byStream = Stream.of(builder());

	@TableFacetsSource("byArray")
	TableFacet[] byArray = { builder() };

	@TableFacetsSource("byProvider")
	TableFacetProvider<TableFacet> byProvider = TableFacetWarehouseOnField::builder;

	@TableFacetsSource("byPublicStatic")
	public static TableFacet byPublicStatic = builder();

	@TableFacetsSource("byStatic")
	static TableFacet byStatic = builder();

	@TableFacetsSource("byProtectedStatic")
	protected static TableFacet byProtectedStatic = builder();

	@TableFacetsSource("byPrivateStatic")
	private static TableFacet byPrivateStatic = builder();
}
