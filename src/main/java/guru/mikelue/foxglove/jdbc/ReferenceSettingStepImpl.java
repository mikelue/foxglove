package guru.mikelue.foxglove.jdbc;

import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;

import guru.mikelue.foxglove.jdbc.ColumnSettingSteps.ReferenceSettingStep;
import guru.mikelue.foxglove.jdbc.JdbcTableFacet.Builder;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

class ReferenceSettingStepImpl<T> implements ReferenceSettingStep<T> {
	private class CardinalityStepImpl implements CardinalityStep {
		private final JdbcTableFacet parentTable;
		private final String columnName;

		private CardinalityStepImpl(JdbcTableFacet parentTable, String columnName)
		{
			this.parentTable = parentTable;
			this.columnName = columnName;
		}

		@Override
		public JdbcTableFacet.Builder cardinality(int numberOfReferences)
		{
			Validate.isTrue(numberOfReferences > 0, "Number of references must be greater than zero");

			return cardinality(numberOfReferences, numberOfReferences);
		}

		@Override
		public Builder cardinality(int min, int max)
		{
			Validate.isTrue(min >= 0, "Number of references must be greater than or equal to zero");
			Validate.isTrue(max >= min, "Max number of references must be greater than or equal to min");

			finalStageSetter.accept(
				new CardinalityInfo<T>(
					min, max,
					parentTable, columnName
				)
			);

			return baseBuilder;
		}
	}

	private final JdbcTableFacet.Builder baseBuilder;
	private final Consumer<CardinalityInfo<T>> finalStageSetter;

	ReferenceSettingStepImpl(
		JdbcTableFacet.Builder baseBuilder,
		Consumer<CardinalityInfo<T>> finalStageSetter
	) {
		this.baseBuilder = baseBuilder;
		this.finalStageSetter = finalStageSetter;
	}

	@Override
	public CardinalityStep parent(JdbcTableFacet parentTable, String referencedColumn)
	{
		var safeReferencedColumn = trimToEmpty(referencedColumn).toLowerCase();

		Validate.notNull(parentTable, "Parent table must not be null");
		Validate.notEmpty(safeReferencedColumn, "Referenced column must not be empty");

		parentTable.getValueTomb().keepColumn(safeReferencedColumn);
		return new CardinalityStepImpl(parentTable, safeReferencedColumn);
	}
}
