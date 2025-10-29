package guru.mikelue.foxglove.examples;

import org.junit.jupiter.api.extension.ExtendWith;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.annotation.DataGeneratorSource;
import guru.mikelue.foxglove.jdbc.JdbcDataGenerator;
import guru.mikelue.foxglove.junit.FoxgloveJUnitExtension;
import guru.mikelue.foxglove.test.AbstractJdbcTestBase;

@ExtendWith(FoxgloveJUnitExtension.class)
public abstract class JUnit5ExtensionTestBase extends AbstractJdbcTestBase {
	public JUnit5ExtensionTestBase() {}

	// tag::dataGeneratorSource[]
	@DataGeneratorSource
	DataGenerator<?> defaultDataGenerator()
	{
		return new JdbcDataGenerator(getDataSource());
	}
	// end::dataGeneratorSource[]
}
