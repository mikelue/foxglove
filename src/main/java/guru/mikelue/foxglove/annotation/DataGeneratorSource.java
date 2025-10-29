package guru.mikelue.foxglove.annotation;

import java.lang.annotation.*;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;

/**
 * Marks a method or a field as instant/provider of {@link DataGenerator}.
 *
 * <p>
 * For annotated field, its type should be {@link DataGenerator} or {@link DataGeneratorProvider}.
 *
 * <p>
 * For annotated method, its return type of method should be {@link DataGenerator}.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented @Inherited
public @interface DataGeneratorSource {
	/**
	 * Defines the name of data generator.
	 *
	 * Without this attribute, the name of method/field is used as the name.
	 *
	 * @return The name of data generator
	 *
	 * @see GenData#generatorName()
	 */
	String value() default "";
}
