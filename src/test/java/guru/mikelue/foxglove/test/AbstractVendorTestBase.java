package guru.mikelue.foxglove.test;

import org.junit.jupiter.api.Tag;

import guru.mikelue.foxglove.springframework.EnableFoxglove;

@Tag("vendor")
@EnableFoxglove
public class AbstractVendorTestBase extends AbstractJdbcTestBase {
	protected AbstractVendorTestBase() {}
}
