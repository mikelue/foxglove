package guru.mikelue.foxglove.examples;

import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet;
import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

import static guru.mikelue.foxglove.test.SampleSchema.*;

public class ManyToManyTest extends AbstractJdbcTestBase {
	public ManyToManyTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown()
	{
		deleteAll(TABLE_RENT, TABLE_CAR, TABLE_MEMBER);
	}

	/**
	 * Example for fixed domain of a many-to-many relationship.
	 */
	@Test
	void fixedDomain()
	{
		// tag::fixedDomain[]
		// The domain of ids of cars
		var idsOfCars = LongStream.range(11, 21).boxed()
			.toList();
		// The domain of ids of members
		var idsOfMembers = LongStream.range(101, 111).boxed()
			.toList();

		/*
		 * Sets up the facets for car and member tables by their domain of ids.
		 */
		var carFacet = JdbcTableFacet.builder(TABLE_CAR)
			.keyOfInt("cr_id").domain(idsOfCars)
			.build();
		var memberFacet = JdbcTableFacet.builder(TABLE_MEMBER)
			.keyOfInt("mb_id").domain(idsOfMembers)
			.build();
		// :~)

		/*
		 * Uses Cartesian product to set up the many-to-many relationship
		 */
		var rentFacet = JdbcTableFacet.builder(TABLE_RENT)
			.cartesianProduct("rt_cr_id").domain(idsOfCars)
			.cartesianProduct("rt_mb_id").domain(idsOfMembers)
			.build();
		// :~)

		dataGenerator()
			.generate(carFacet, memberFacet, rentFacet);
		// end::fixedDomain[]

		assertNumberOfRows(
			TABLE_RENT,
			" rt_cr_id BETWEEN 11 AND 20 AND" +
			" rt_mb_id BETWEEN 101 AND 110"
		)
			.isEqualTo(idsOfCars.size() * idsOfMembers.size());
	}

	/**
	 * Example for referencing column of one side in a many-to-many relationship.
	 */
	@Test
	void oneSideReferencing()
	{
		// tag::oneSideReferencing[]
		final int numberOfCars = 10;

		// The domain of ids of members
		var idsOfMembers = LongStream.range(101, 111).boxed()
			.toList();

		/*
		 * Can be any way to generate the ids of cars
		 */
		var carFacet = JdbcTableFacet.builder(TABLE_CAR)
			.keyOfInt("cr_id").limit(1020, numberOfCars)
			.build();
		// :~)
		/*
		 * Fixed domain of ids of members
		 */
		var memberFacet = JdbcTableFacet.builder(TABLE_MEMBER)
			.keyOfInt("mb_id").domain(idsOfMembers)
			.build();
		// :~)

		/*
		 * Uses Cartesian product to set up the many-to-many relationship
		 */
		var rentFacet = JdbcTableFacet.builder(TABLE_RENT)
			// References to the ids of cars
			.referencing("rt_cr_id").parent(carFacet, "cr_id")
				// The cardinality is the number of members
				.cardinality(idsOfMembers.size())
			// The domain comes from the ids of members
			.column("rt_mb_id").from(memberFacet, "mb_id")
				// Uses round-robin to cover all members
				.roundRobin()
			.build();
		// :~)

		dataGenerator()
			.generate(carFacet, memberFacet, rentFacet);
		// end::oneSideReferencing[]

		assertNumberOfRows(
			TABLE_RENT,
			" rt_cr_id >= 1020 AND" +
			" rt_mb_id BETWEEN 101 AND 110"
		)
			.isEqualTo(10 * idsOfMembers.size());
	}

	/**
	 * Example for Cartesian product with referencing column.
	 */
	@Test
	void cartesianProductByReferencing()
	{
		// tag::cartesianProductByReferencing[]
		// Any way to generate data for these two tables
		var carFacet = JdbcTableFacet.builder(TABLE_CAR)
			.keyOfInt("cr_id").limit(1000, 3)
			.build();
		var memberFacet = JdbcTableFacet.builder(TABLE_MEMBER)
			.keyOfInt("mb_id").limit(2000, 5)
			.build();
		// :~)

		var rentFacet = JdbcTableFacet.builder(TABLE_RENT)
			// References to ids of cars
			.cartesianProduct("rt_cr_id")
				.referencing(carFacet, "cr_id")
			// References to ids of members
			.cartesianProduct("rt_mb_id")
				.referencing(memberFacet, "mb_id")
			.build();
		// end::cartesianProductByReferencing[]

		dataGenerator()
			.generate(carFacet, memberFacet, rentFacet);

		assertNumberOfRows(
			TABLE_RENT,
			String.format(
				"""
				rt_cr_id BETWEEN 1000 AND %d AND
				rt_mb_id BETWEEN 2000 AND %d
				""",
				1002, 2004
			)
		)
			.isEqualTo(15);
	}

	private JdbcDataGenerator dataGenerator()
	{
		return new JdbcDataGenerator(getDataSource());
	}
}
