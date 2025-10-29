package guru.mikelue.foxglove.setting;

import java.util.Optional;

/**
 * A type implements this interface is meant to provide {@link DataSettingInfo} possibly.
 */
public interface SettingProvider {
	/**
	 * Retrieves the data setting applied, if set by {@link SettingAware#withSetting}.
	 *
	 * @return The data setting applied
	 */
	Optional<DataSettingInfo> getSetting();
}
