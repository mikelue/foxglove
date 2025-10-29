package guru.mikelue.foxglove.examples;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.annotation.DataGeneratorSource;
import guru.mikelue.foxglove.annotation.GenData;
import guru.mikelue.foxglove.annotation.TableFacetsSource;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.junit.FoxgloveJUnitExtension;
import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

import static guru.mikelue.foxglove.test.SampleSchema.TABLE_CAR;
import static guru.mikelue.foxglove.test.SampleSchema.TABLE_CAR_FEATURE;
import static org.assertj.core.api.Assertions.assertThat;

// tag::foxgloveExtension[]
@ExtendWith(FoxgloveJUnitExtension.class)
public class JUnit5Test extends AbstractJdbcTestBase {
	private final static int RANDOM_ROWS = gen().ints().range(5, 10).get();

	public JUnit5Test() {}

	/**
	 * For @GenData, the name will be same as the method name.
	 */
	@Test
	@GenData(facetsNames = { "carsWithFeature" })
	void junit5Method()
	{
		int testedCount = getJdbcTemplate().query(
			"""
			SELECT COUNT(DISTINCT cr_id)
			FROM ap_car
				INNER JOIN
				ap_car_feature
				ON cr_id = cf_cr_id
					AND cf_feature_name = 'Sunroof'
			""",
			rs -> {
				rs.next();
				return rs.getInt(1);
			}
		);

		assertThat(testedCount)
			.isEqualTo(RANDOM_ROWS);
	}

	@TableFacetsSource
	TableFacet[] carsWithFeature()
	{
		var carFacet = JdbcTableFacet.builder(TABLE_CAR)
			.numberOfRows(RANDOM_ROWS)
			.build();

		return new TableFacet[] {
			carFacet,
			JdbcTableFacet.builder(TABLE_CAR_FEATURE)
				.referencing("cf_cr_id")
					.parent(carFacet, "cr_id")
					.cardinality(2)
				.column("cf_feature_name")
					.roundRobin("Sunroof", "Leather Seats")
				.build()
		};
	}

	/**
	 * Default data generator for JDBC.
	 */
	@DataGeneratorSource
	DataGenerator<?> defaultDataGenerator()
	{
		return new JdbcDataGenerator(getDataSource());
	}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown()
	{
		deleteAll(TABLE_CAR);
	}
}
// end::foxgloveExtension[]
