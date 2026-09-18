package guru.springframework.spring7restmvc;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base class for tests that need a real MySQL database.
 * <p>
 * The container is a singleton: it is started once per JVM, the first time a subclass is used,
 * and shared by every test class extending this one. It is not annotated with {@code @Container},
 * so JUnit does not stop it after each class; Testcontainers' Ryuk removes it when the JVM exits.
 * <p>
 * Without a running Docker daemon the subclasses are skipped instead of failing.
 *
 * Created by Pierrot on 18-09-2026
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("testcontainers")
public abstract class MySqlContainerBase {

    // 8.4 is MySQL's long-term-support line; 9.x releases are short-lived "innovation" releases
    protected static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer("mysql:8.4");

    static {
        MY_SQL_CONTAINER.start();
    }

    // Point the Spring datasource at the container (random port, generated credentials)
    @DynamicPropertySource
    static void mySqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
    }
}
