package guru.mikelue.foxglove.functional;

import java.util.function.Supplier;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;

/**
 * Provides {@link DataGenerator} instances,
 * this interface is usually used by <a href="https://docs.junit.org/current/user-guide/#extensions">JUnit extension</a> or
 * <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/TestExecutionListener.html">TestExecutionListener</a> of SpringFramework.
 *
 * @param <T> The type of table facet
 */
public interface DataGeneratorProvider<T extends TableFacet> extends Supplier<DataGenerator<T>> {}
