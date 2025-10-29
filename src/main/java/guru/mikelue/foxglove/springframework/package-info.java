/**
 * Spring Framework integration for Foxglove.
 *
 * <p>
 *
 * You can use one of the following ways to enable Foxglove test features in Spring-based tests.
 *
 * <h2>Use {@link EnableFoxglove}</h2>
 *
 * <pre><code class="language-java">
 * {@literal @}EnableFoxglove
 * {@literal @}ContextConfiguration(classes = {AppConfig.class})
 * public class SomeTest {}
 * </code></pre>
 *
 * <h2>Add {@link FoxgloveTestListener} to {@link TestExecutionListeners}</h2>
 *
 * <pre><code class="language-java">
 * {@literal @}TestExecutionListeners(
 *     listeners = FoxgloveTestListener.class,
 *     mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
 * )
 * {@literal @}ContextConfiguration(classes = {AppConfig.class})
 * public class SomeTest {}
 * </code></pre>
 *
 * @see EnableFoxglove
 * @see FoxgloveTestListener
 */
package guru.mikelue.foxglove.springframework;

import org.springframework.test.context.TestExecutionListeners;
