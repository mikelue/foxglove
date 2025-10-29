/**
 * JUnit extensions for Foxglove testing.
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Supports class/method level data generation by annotating the objects with {@link GenData}.</li>
 *   <li>Supports inheritance of classes for {@link TableFacetsSource} and {@link DataGeneratorSource}.</li>
 *   <li>Supports <a href="https://docs.junit.org/current/user-guide/#writing-tests-nested">Nested Tests</a> provided by JUnit 5.</li>
 * </ul>
 *
 * <hr>
 *
 * <h2>Example</h2>
 *
 * <pre><code class="language-java">
 * import guru.mikelue.foxglove.annotation.DataGeneratorSource;
 * import guru.mikelue.foxglove.annotation.GenData;
 * import guru.mikelue.foxglove.annotation.TableFacetsSource;
 * import guru.mikelue.foxglove.junit.FoxgloveJUnitExtension;
 *
 * &#64;ExtendWith(FoxgloveJUnitExtension.class)
 * public class SampleTest {
 *     &#64;Test
 *     &#64;GenData(facetsNames = { "carsWithFeature" })
 *     void junit5Method()
 *     {
 *         // Your test code here
 *     }
 *
 *     &#64;TableFacetsSource
 *     TableFacet[] carsWithFeature()
 *     {
 *         // Prepare your TableFacet(s)
 *         return new TableFacet[] {};
 *     }
 *
 *     &#64;DataGeneratorSource
 *     DataGenerator&lt;?&gt; defaultDataGenerator()
 *     {
 *         return new JdbcDataGenerator(getDataSource());
 *     }
 * }
 * </code></pre>
 *
 * @see <a href="https://docs.junit.org/current/user-guide/#writing-tests-nested">JUnit 5 User Guide</a>
 */
package guru.mikelue.foxglove.junit;

import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.annotation.DataGeneratorSource;
