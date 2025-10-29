package guru.mikelue.foxglove.annotation;

import java.lang.annotation.*;
import java.util.function.Supplier;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;

/**
 * Annotation to define data generation configuration for a test method.
 *
 * {@link guru.mikelue.foxglove.annotation package doc} detail the semantics
 * if none of the {@link #facetsNames()} or {@link #facets()} is defined by client code.
 *
 * <hr>
 *
 * <h2>How should a processing engine resolve names</h2>
 *
 * <ol>
 *   <li>Looking for {@link TableFacetsSource#value()} or {@link DataGeneratorSource#value()}.</li>
 *   <li>Using the name of method/field.</li>
 * </ol>
 *
 * @see TableFacetProvider
 * @see DataGeneratorProvider
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
public @interface GenData {
	/**
	 * Defines the a builder(as a {@link Supplier}) of {@link TableFacet} for generating rules of tables.
	 *
	 * This property has higher priority than {@link #facets()}.
	 *
	 * @return The classes of providers for table facets
	 *
	 * @see #facetsNames()
	 */
	Class<? extends TableFacetsProvider<? extends TableFacet>>[] value() default {};

	/**
	 * Defines the a builder(as a {@link Supplier}) of {@link TableFacet} for generating rules of tables.
	 *
	 * This property has higher priority than {@link #facetsNames()}.
	 *
	 * @return The classes of providers for table facets
	 *
	 * @see #value()
	 */
	Class<? extends TableFacetsProvider<? extends TableFacet>>[] facets() default {};
	/**
	 * Defines the name of {@link TableFacet} for generating rules of tables.
	 *
	 * The resolving of names is dependent on {@link TableFacetsSource} and the implementation of processing engine.
	 *
	 * @return The names of table facets
	 */
	String[] facetsNames() default {};
	/**
	 * Defines the data generator(as a {@link Supplier}).
	 *
	 * <p>
	 * Only one of {@link #generatorName()} or this property should be defined.
	 *
	 * @return The class of provider for data generator
	 */
	Class<? extends DataGeneratorProvider<? extends TableFacet>> generator() default FallbackDataGeneratorProvider.class;
	/**
	 * Defines the name of data generator(as a {@link Supplier}).
	 *
	 * <p>
	 * Only one of {@link #generator()} or this property should be defined.
	 *
	 * The resolving of names is dependent on {@link DataGeneratorSource} and the implementation of processing engine.
	 *
	 * @return The name of data generator
	 */
	String generatorName() default "";

	/**
	 * Fallback provider of data generator,
	 * this provider will throw {@link UnsupportedOperationException} while being invoked.
	 *
	 * <p>
	 * The processing engine of test framework should ignore this provider if a default one is figured out.
	 */
	class FallbackDataGeneratorProvider implements DataGeneratorProvider<TableFacet> {
		FallbackDataGeneratorProvider() {}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataGenerator<TableFacet> get()
		{
			throw new UnsupportedOperationException("No default data generator is provided");
		}
	}
}
