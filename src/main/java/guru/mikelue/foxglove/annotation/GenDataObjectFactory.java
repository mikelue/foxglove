package guru.mikelue.foxglove.annotation;

import java.util.List;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;

/**
 * A factory interface to create data generator and table facets defined
 * by {@link GenData}.
 */
public interface GenDataObjectFactory {
	/**
	 * Creates a data generator defined by {@link GenData}.
	 *
	 * @return The instance of data generator
	 *
	 * @throws Exception If fails to create the data generator
	 */
	DataGenerator<TableFacet> getDataGenerator() throws Exception;

	/**
	 * Creates table facets defined by {@link GenData}.
	 *
	 * @return The list of table facets
	 *
	 * @throws Exception If fails to create the table facets
	 */
	List<TableFacet> getTableFacets() throws Exception;
}
