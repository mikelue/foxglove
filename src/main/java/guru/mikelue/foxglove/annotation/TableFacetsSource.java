package guru.mikelue.foxglove.annotation;

import java.lang.annotation.*;
import java.util.List;
import java.util.stream.Stream;

import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.TableFacetsProvider;

/**
 * Marks a method or a filed as instant/provider of {@link TableFacet}.
 *
 * <p>
 * For annotated field, its type should be:
 *
 * <ul>
 *   <li>{@link TableFacet},</li>
 *   <li>{@link List}, {@link Stream} or {@code array([])} of {@link TableFacet}s</li>
 *   <li>{@link TableFacetsProvider}</li>
 * </ul>
 *
 * <p>
 * For annotated method, its return type of method should be:
 * <ul>
 *   <li>{@link TableFacet},</li>
 *   <li>{@link List}, {@link Stream} or {@code array([])} of {@link TableFacet}s</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
public @interface TableFacetsSource {
	/**
	 * Defines the name of table facet.
	 *
	 * Without this attribute, the name of method/field is used as the name.
	 *
	 * @return The name of table facet
	 *
	 * @see GenData#facetsNames()
	 */
	String value() default "";
}
