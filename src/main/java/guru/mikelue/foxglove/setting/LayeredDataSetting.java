package guru.mikelue.foxglove.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import guru.mikelue.foxglove.ColumnMeta;

/**
 * A proxy of {@link DataSettingInfo} for multi-layer settings.
 *
 * The default setting of global is always added as the last priority.
 */
public class LayeredDataSetting implements DataSettingInfo {
	private final List<DataSettingInfo> viableSettings =
		new ArrayList<>(3);

	/**
	 * Constructs the layered data setting with multiple settings.
	 *
	 * The usage priority is from first to last.
	 *
	 * @param settings The rest data setting info, can be null for some
	 */
	public LayeredDataSetting(
		DataSettingInfo... settings
	) {
		for (var setting : settings) {
			if (setting == null) {
				continue;
			}
			viableSettings.add(setting);
		}

		viableSettings.add(DataSetting.defaults());
	}

	/**
	 * Uses the highest priority setting to get the default number of rows.
	 *
	 * @return The number of rows defined in the highest priority setting
	 *
	 * @see DataSetting#defaults()
	 */
	@Override
	public int getDefaultNumberOfRows()
	{
		return viableSettings.get(0).getDefaultNumberOfRows();
	}

	/**
	 * Retrieves the found {@link Supplier} by priority of settings.
	 *
	 * @param columnMeta The metadata of column used to resolve {@link Supplier}
	 *
	 * @return The resolved {@link Supplier} or empty.
	 */
	@Override
	public <T> Optional<Supplier<T>> resolveSupplier(ColumnMeta columnMeta)
	{
		for (var setting : viableSettings) {
			var resolvedSpec = setting.<T>resolveSupplier(columnMeta);
			if (resolvedSpec.isPresent()) {
				return resolvedSpec;
			}
		}

		return Optional.empty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoGenerating(ColumnMeta column)
	{
		return viableSettings.get(0).isAutoGenerating(column);
	}
}
