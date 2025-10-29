package guru.mikelue.foxglove.setting;

/**
 * Interface for classes which support customized {@link DataSetting}.
 *
 * @param <T>  The type of class which implements this interface
 */
public interface SettingAware<T extends SettingAware<T>> {
	/**
	 * Applies the given data setting and the supporting class itself.
	 *
	 * @param setting  The data setting to be applied
	 *
	 * @return The supporting class itself
	 */
	T withSetting(DataSettingInfo setting);
}
