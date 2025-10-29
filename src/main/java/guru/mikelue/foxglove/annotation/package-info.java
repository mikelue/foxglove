/**
 * This package contains annotations for Foxglove library.
 *
 * <p>
 * A processing engine,
 * like <a href="https://docs.junit.org/current/user-guide/#extensions">Extension Model(JUnit)</a> or <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/TestExecutionListener.html">TestExecutionListener(SpringFramework Test)</a>,<br>
 * is responsible to implement the semantics described below:
 *
 * <hr>
 *
 * <h2>The identifying of {@link TableFacet}</h2>
 *
 * The priority to identify {@link TableFacet}(s) is listed as:
 *
 * <ol>
 *   <li>The test instance has at least one field or method annotated by {@link TableFacetsSource}</li>
 *   <li>The member annotated by {@link TableFacetsSource} should be one of
 *   	<ul>
 *   		<li>Field of type {@link TableFacet}</li>
 *   		<li>Field of type {@link List} of {@link TableFacet}</li>
 *   		<li>Field of type {@link Stream} of {@link TableFacet}</li>
 *   		<li>Field of type array of {@link TableFacet}</li>
 *   		<li>Field of type {@link TableFacetsProvider}</li>
 *   		<li>Method(no arguments) returns {@link TableFacet}</li>
 *   		<li>Method(no arguments) returns {@link List} of {@link TableFacet}</li>
 *   		<li>Method(no arguments) returns {@link Stream} of {@link TableFacet}</li>
 *   		<li>Method(no arguments) returns array of {@link TableFacet}</li>
 *   	</ul>
 *   </li>
 *   <li>Keeps looking for parent class if nothing found in current class</li>
 * </ol>
 *
 * <h2>The identifying of {@link DataGenerator}</h2>
 *
 * The priority to identify {@link DataGenerator} is listed as:
 *
 * <ol>
 *   <li>The test instance has at least one of field or method annotated by {@link DataGeneratorSource}</li>
 *   <li>The member annotated by {@link DataGeneratorSource} should be one of
 *   	<ul>
 *   		<li>Field of type {@link DataGenerator}</li>
 *   		<li>Field of type {@link DataGeneratorProvider}</li>
 *   		<li>Method(no arguments) returns {@link DataGenerator}</li>
 *   	</ul>
 *   </li>
 *   <li>Keeps looking for parent class if nothing found in current class</li>
 * </ol>
 *
 * @see TableFacetsProvider
 * @see DataGeneratorProvider
 * @see <a href="https://docs.junit.org/current/user-guide/#extensions">Extension Model(JUnit)</a>
 * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/TestExecutionListener.html">TestExecutionListener(SpringFramework Test)</a>
 */
package guru.mikelue.foxglove.annotation;

import java.util.List;
import java.util.stream.Stream;

import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.DataGenerator;
