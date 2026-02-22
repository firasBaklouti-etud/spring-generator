package ${packageName};

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Abstract base test class for integration tests using Testcontainers.
 * Extend this class to automatically configure a database container.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {
}
