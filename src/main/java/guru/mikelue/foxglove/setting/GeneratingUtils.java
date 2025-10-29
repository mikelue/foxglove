package guru.mikelue.foxglove.setting;

import java.util.Set;

import guru.mikelue.foxglove.ColumnMeta;

import static guru.mikelue.foxglove.ColumnMeta.Property.*;

interface GeneratingUtils {
	static boolean checkAutoGenerating(
		ColumnMeta columnMeta,
		Set<ColumnMeta.Property> autoGenerateProperties
	) {
		var columnProperties = columnMeta.properties();

		if (columnProperties.isEmpty()) {
			return true;
		}

		if (columnProperties.contains(GENERATED)) {
			return autoGenerateProperties.contains(GENERATED);
		}

		if (columnProperties.contains(AUTO_INCREMENT)) {
			return autoGenerateProperties.contains(AUTO_INCREMENT);
		}

		if (columnProperties.contains(NULLABLE)) {
			return autoGenerateProperties.contains(NULLABLE);
		}

		if (columnProperties.contains(DEFAULT_VALUE)) {
			return autoGenerateProperties.contains(DEFAULT_VALUE);
		}

		return true;
	}
}
