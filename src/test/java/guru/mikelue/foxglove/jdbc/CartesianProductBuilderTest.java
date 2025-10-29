package guru.mikelue.foxglove.jdbc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import guru.mikelue.misc.testlib.AbstractTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CartesianProductBuilderTest extends AbstractTestBase {
	public CartesianProductBuilderTest() {}

	@BeforeEach
	void setup() {}

	@AfterEach
	void tearDown() {}

	/**
	 * Tests the building of Cartesian product values.
	 */
	@ParameterizedTest
	@MethodSource
	void buildSupplierFactory(
		Map<String, List<?>> domains,
		Map<String, List<?>> expectedExpandedValues
	) {
		var testedBuilder = new CartesianProductBuilder();
		for (var entry: domains.entrySet()) {
			testedBuilder.putDomain(entry.getKey(), () -> entry.getValue());
		}

		var columnNames = testedBuilder.getColumnNames();
		var testedResult = new HashMap<>(columnNames.size());
		columnNames.stream()
			.forEach(name -> {
				var supplier = testedBuilder.buildLazySupplier(name);

				testedResult.put(
					name,
					Stream.generate(supplier)
						.limit(testedBuilder.getNumberOfRows())
						.toList()
				);
			});

		assertThat(testedResult)
			.isEqualTo(expectedExpandedValues);
	}

	static Arguments[] buildSupplierFactory()
	{
		return new Arguments[] {
			arguments( // Single column
				ofMap(
					"A", List.of(1, 2)
				),
				ofMap(
					"A", List.of(1, 2)
				)
			),
			arguments( // Different size, two columns
				ofMap(
					"A", List.of(1, 2),
					"B", List.of(1, 2, 3)
				),
				ofMap(
					"A", List.of(1, 1, 1, 2, 2, 2),
					"B", List.of(1, 2, 3, 1, 2, 3)
				)
			),
			arguments( // Different size, three columns
				ofMap(
					"A", List.of(1, 2),
					"B", List.of(1, 2, 3),
					"C", List.of(7, 8)
				),
				ofMap(
					"A", List.of(1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2),
					"B", List.of(1, 1, 2, 2, 3, 3, 1, 1, 2, 2, 3, 3),
					"C", List.of(7, 8, 7, 8, 7, 8, 7, 8, 7, 8, 7, 8)
				)
			),
			arguments( // Same size, more columns
				ofMap(
					"A", List.of("A1", "A2"),
					"B", List.of("B1", "B2"),
					"C", List.of("C1", "C2"),
					"D", List.of("D1", "D2")
				),
				ofMap(
					"A", List.of(
						"A1", "A1", "A1", "A1", "A1", "A1", "A1", "A1",
						"A2", "A2", "A2", "A2", "A2", "A2", "A2", "A2"
					),
					"B", List.of(
						"B1", "B1", "B1", "B1", "B2", "B2", "B2", "B2",
						"B1", "B1", "B1", "B1", "B2", "B2", "B2", "B2"
					),
					"C", List.of(
						"C1", "C1", "C2", "C2", "C1", "C1", "C2", "C2",
						"C1", "C1", "C2", "C2", "C1", "C1", "C2", "C2"
					),
					"D", List.of(
						"D1", "D2", "D1", "D2", "D1", "D2", "D1", "D2",
						"D1", "D2", "D1", "D2", "D1", "D2", "D1", "D2"
					)
				)
			),
		};
	}

	private static Map<String, List<?>> ofMap(
		String key, List<?> values,
		Object... keyValues
	) {
		var result = new LinkedHashMap<String, List<?>>();

		result.put(key, values);

		for (int i = 0; i < keyValues.length; i += 2) {
			result.put(
				(String)keyValues[i],
				(List<?>)keyValues[i + 1]
			);
		}

		return result;
	}
}
