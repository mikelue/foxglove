package guru.mikelue.foxglove.springframework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.*;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;

import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

/**
 * A factory class which uses Spring {@link ApplicationContext} to
 * create data generation objects defined by {@link GenData} or
 * fallback to {@link ReflectGenDataObjectFactory}.
 */
class SpringContextObjectFactory implements GenDataObjectFactory {
	private final Logger logger = LoggerFactory.getLogger(SpringContextObjectFactory.class);

	private final ApplicationContext appContext;

	private final GenDataInspector genDataInspector;
	private final DataGenContext<TableFacet> dataGenContext;
	private final Object testingInstance;
	private final String nameOfTestingObject;

	SpringContextObjectFactory(
		ApplicationContext appContext,
		Object testingInstance, GenData genData,
		DataGenContext<TableFacet> dataGenContext,
		String name
	) {
		this.appContext = appContext;
		this.genDataInspector = GenDataInspector.of(genData);
		this.dataGenContext = dataGenContext;
		this.testingInstance = testingInstance;
		this.nameOfTestingObject = name;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DataGenerator<TableFacet> getDataGenerator() throws Exception
	{
		if (genDataInspector.useDefaultDataGenerator()) {
			/*
			 * Tries to get the default data generator from Spring context first.
			 */
			return getBeanIfUnique(DataGeneratorProvider.class) // Uses provider if available
				.map(providerBean -> (DataGenerator<TableFacet>)providerBean.get())
				.or(() -> (Optional<DataGenerator<TableFacet>>) // Uses direct object if available
					(Optional<?>)getBeanIfUnique(DataGenerator.class)
				)
				.or(() -> buildDataGeneratorByDataSource())
				.or(() -> dataGenContext.getDefaultDataGenerator(testingInstance))
				.orElseThrow(() -> new IllegalStateException(
					String.format(
						"Unable to find default DataGenerator from Spring context or auto-construction: [%s]",
						nameOfTestingObject
					)
				));
			// :~)
		}

		var genData = genDataInspector.genData();

		if (genData.generatorName().isEmpty()) {
			/*
			 * Tries to get data generator from specified provider class
			 */
			var providerClass = (Class<DataGeneratorProvider<TableFacet>>)genData.generator();

			return getBeanIfUnique(providerClass)
				.map(providerBean -> (DataGenerator<TableFacet>)
					((DataGeneratorProvider<?>)providerBean).get()
				)
				.orElseGet(() -> dataGenContext.getTypedDataGenerator(providerClass, testingInstance));
			// :~)
		}

		return getBeanByName(genData.generatorName())
			.map(foundBean -> {
				/*
				 * Type checking for supported bean types
				 */
				if (foundBean instanceof DataGeneratorProvider) {
					return (DataGenerator<TableFacet>)((DataGeneratorProvider<?>)foundBean).get();
				}

				if (foundBean instanceof DataGenerator) {
					return (DataGenerator<TableFacet>)foundBean;
				}
				// :~)

				throw new BeanNotOfRequiredTypeException(
					genData.generatorName(), DataGeneratorProvider.class, foundBean.getClass()
				);
			})
			.orElseGet(() -> dataGenContext.getNamedDataGenerator(
				genData.generatorName(), testingInstance
			));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TableFacet> getTableFacets() throws Exception
	{
		var genData = genDataInspector.genData();

		if (genDataInspector.useDefaultTableFacets()) {
			/*
			 * Tries to get the default data generator from Spring context first.
			 */
			return getBeanIfUnique(
				TableFacetsProvider.class
			)
				.map(providerBean -> (List<TableFacet>)providerBean.get())
				.or(() -> (Optional<List<TableFacet>>)
					(Optional<?>)getBeanIfUnique(List.class)
				)
				.or(() -> {
					return dataGenContext.getDefaultTableFacets(testingInstance);
				})
				.get();
			// :~)
		}

		List<TableFacet> tableFacets = new ArrayList<>(genDataInspector.numberOfTableFacets());

		for (var classOfFacetsProvider: genData.value()) {
			tableFacets.addAll(
				getTableFacetsByProviderClass(
					(Class<? extends TableFacetsProvider<TableFacet>>)classOfFacetsProvider,
					testingInstance
				)
			);
		}
		for (var classOfFacetsProvider: genData.facets()) {
			tableFacets.addAll(
				getTableFacetsByProviderClass(
					(Class<? extends TableFacetsProvider<TableFacet>>)classOfFacetsProvider,
					testingInstance
				)
			);
		}
		for (var facetName: genData.facetsNames()) {
			tableFacets.addAll(
				getTableFacetsByName(
					facetName, testingInstance
				)
			);
		}

		return tableFacets;
	}

	@SuppressWarnings("unchecked")
	private Optional<DataGenerator<TableFacet>> buildDataGeneratorByDataSource()
	{
		var dataSource = appContext.getBeanProvider(DataSource.class)
			.getIfAvailable();

		if (dataSource == null) {
			return Optional.empty();
		}

		Object dataGeneratorObject;

		if (isActualTransactionActive()) {
			/*
			 * Joins current transaction if it is active
			 */
			logger.debug(
				"Auto-constructing JdbcDataGenerator which joins current transaction for: {}",
				nameOfTestingObject
			);
			dataGeneratorObject = (DataGenerator<TableFacet>)(Object)
				new JdbcDataGenerator(DataSourceUtils.getConnection(dataSource));
			// :~)
		} else {
			logger.debug(
				"Auto-constructing JdbcDataGenerator by DataSource for: {}", dataSource
			);
			dataGeneratorObject = new JdbcDataGenerator(dataSource);
		}

		return Optional.of(
			(DataGenerator<TableFacet>)dataGeneratorObject
		);
	}

	@SuppressWarnings("unchecked")
	private List<TableFacet> getTableFacetsByName(String name, Object testingInstance)
	{
		return getBeanByName(name)
			.<List<TableFacet>>map(foundBean -> {
				/*
				 * Tries to use found bean to get table facets
				 */
				if (foundBean instanceof TableFacetsProvider) {
					return (List<TableFacet>)((TableFacetsProvider<?>)foundBean).get();
				}

				if (foundBean instanceof TableFacet) {
					return List.of((TableFacet)foundBean);
				}

				if (foundBean instanceof List) {
					return (List<TableFacet>)foundBean;
				}

				if (foundBean instanceof Stream) {
					return ((Stream<TableFacet>)foundBean).toList();
				}

				var beanClass = foundBean.getClass();
				if (beanClass.isArray() &&
					TableFacet.class.isAssignableFrom(beanClass.getComponentType())
				) {
					return Arrays.asList((TableFacet[])foundBean);
				}
				// :~)

				throw new BeanNotOfRequiredTypeException(
					name, TableFacetsProvider.class, foundBean.getClass()
				);
			})
			.orElseGet(() -> dataGenContext.getNamedTableFacets(
				name, testingInstance
			));
	}

	@SuppressWarnings("unchecked")
	private List<TableFacet> getTableFacetsByProviderClass(
		Class<? extends TableFacetsProvider<TableFacet>> providerClass,
		Object testingInstance
	) {
		return getBeanIfUnique(providerClass)
			.map(providerBean -> (List<TableFacet>)
				((TableFacetsProvider<?>)providerBean).get()
			)
			.orElseGet(() -> dataGenContext.getTypedTableFacets(
				providerClass, testingInstance
			));
	}

	private <T> Optional<T> getBeanIfUnique(Class<T> beanClass)
	{
		try {
			return Optional.of(appContext.getBean(beanClass));
		} catch (NoUniqueBeanDefinitionException e) {
			throw e;
		} catch (NoSuchBeanDefinitionException e) {
			logger.trace("Unable to find bean by type: \"{}\" in Spring context: [{}]",
				beanClass.getName(), nameOfTestingObject);
			return Optional.empty();
		}
	}

	private Optional<Object> getBeanByName(String name)
	{
		try {
			return Optional.of(appContext.getBean(name));
		} catch (NoSuchBeanDefinitionException e) {
			logger.trace("Unable to find bean by name: \"{}\" in Spring context: [{}]",
				name, nameOfTestingObject);
			return Optional.empty();
		}
	}
}
