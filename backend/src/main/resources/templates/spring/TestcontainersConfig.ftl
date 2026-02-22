package ${packageName};

<#if request.databaseType?? && request.databaseType == "postgresql">
import org.testcontainers.containers.PostgreSQLContainer;
<#elseif request.databaseType?? && request.databaseType == "mysql">
import org.testcontainers.containers.MySQLContainer;
<#elseif request.databaseType?? && request.databaseType == "mariadb">
import org.testcontainers.containers.MariaDBContainer;
</#if>
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Testcontainers configuration for integration tests.
 * Provides a reusable database container with {@link ServiceConnection}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

<#if request.databaseType?? && request.databaseType == "postgresql">
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        postgres.start();
    }

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return postgres;
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
<#elseif request.databaseType?? && request.databaseType == "mysql">
    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        mysql.start();
    }

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return mysql;
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
<#elseif request.databaseType?? && request.databaseType == "mariadb">
    private static final MariaDBContainer<?> mariadb =
            new MariaDBContainer<>("mariadb:10.11")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        mariadb.start();
    }

    @Bean
    @ServiceConnection
    public MariaDBContainer<?> mariadbContainer() {
        return mariadb;
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
    }
</#if>
}
