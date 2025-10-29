/**
 * Foxglove is a lightweight Java library for easing the generating data for testing database-related code.
 *
 * <h2>Core Concepts</h2>
 * <ul>
 *   <li>{@link TableFacet} represents the facet of a database table to be generated.</li>
 *   <li>{@link DataGenerator} is the working interface which is responsible.</li>
 *   <li>{@link ColumnMeta} is the read-only metadata for a column.</li>
 * </ul>
 *
 * <h2>Settings</h2>
 * {@link DataSetting} is used to configure the behavior for data generating.
 *
 * <h2>JDBC</h2>
 * <ul>
 *   <li>{@link JdbcTableFacet#builder(String)} is the entry method used to construct and configure a table facet.</li>
 *   <li>{@link JdbcDataGenerator} is the connection implementation for {@link javax.sql.DataSource}.</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <ul>
 *   <li>{@link JdbcTableFacet.Builder} - code snippets to configure table facets.</li>
 *   <li>{@link DataSetting} - code snippets to use settings.</li>
 * </ul>
 *
 * @see <a href="https://www.instancio.org/user-guide/">Instancio</a><em>(backbone of random generator)</em>
 */
package guru.mikelue.foxglove;

import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.setting.DataSetting;
