package guru.mikelue.foxglove.springframework;

import java.lang.annotation.*;

import org.springframework.test.context.TestExecutionListeners;

/**
 * Composite annotation to enable Foxglove test features.
 *
 * <p>
 * This annotation registers {@link FoxgloveTestListener} to {@link TestExecutionListeners},
 * with merging mode of {@link TestExecutionListeners.MergeMode#MERGE_WITH_DEFAULTS}.
 *
 * @see FoxgloveTestListener
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
@TestExecutionListeners(
	listeners = FoxgloveTestListener.class,
	mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public @interface EnableFoxglove {}
