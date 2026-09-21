package guru.springframework.spring7restmvc;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base class for tests that need a real MySQL database.
 * <p>
 * The container is a singleton: it is started once per JVM, the first time a subclass is used,
 * and shared by every test class extending this one. It is not annotated with {@code @Container},
 * so JUnit does not stop it after each class; Testcontainers' Ryuk removes it when the JVM exits.
 * <p>
 * Subclasses are integration tests ({@code *IT}), run by Failsafe in {@code mvn verify}. Without a
 * running Docker daemon they fail on purpose, so a missing Docker cannot hide behind a green build;
 * {@code mvn test} does not run them and needs no Docker.
 * <p>
 * Created by Pierrot on 18-09-2026
 * Modified by Pierrot on 19-09-2026
 */
@ActiveProfiles("testcontainers")
public abstract class MySqlContainerBase {

    // 8.4 is MySQL's long-term-support line; 9.x releases are short-lived "innovation" releases.
    // @ServiceConnection lets Spring Boot derive the datasource connection details (url, username,
    // password) from the container itself, replacing the spring.datasource.* properties.
    @ServiceConnection
    protected static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer("mysql:9.5");

    static {
        MY_SQL_CONTAINER.start();
    }
}
