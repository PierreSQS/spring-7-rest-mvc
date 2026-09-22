# Repository guide

Guidance for Claude Code when working on this repository.

---

## 1. Project at a glance

A beer and customer REST API built alongside John Thompson's **Spring Framework 6: Beginner to Guru** course on Udemy.

| Area | Reference |
|---|---|
| Runtime | Java 25, Spring Boot 4.1, Spring Framework 7 |
| Persistence / JSON | Hibernate 7, Jackson 3 |
| Libraries | Spring Data JPA, MapStruct, Lombok, Flyway |
| Root package | `guru.springframework.spring7restmvc` |
| Entry point | `Spring7RestMvcApplication` |
| Branches | One per lesson (e.g. `78-fly-add-column`), merged forward into the next |
| Commits | Include the course section: `Sec11_Chap124-XX: ...` |

---

## 2. Build, test and run

Use **JDK 25** (`JAVA_HOME`) and the Maven wrapper, which pins **Maven 3.9.16**. Commands below use PowerShell; replace `.\mvnw.cmd` with `./mvnw` in a POSIX shell.

| Task | Command | Docker |
|---|---|---|
| Unit tests | `.\mvnw.cmd test` | No |
| One test class | `.\mvnw.cmd test "-Dtest=BeerControllerTest"` | No |
| One test method | `.\mvnw.cmd test "-Dtest=BeerControllerTest#testPatchBeer"` | No |
| Full verification | `.\mvnw.cmd clean verify` | Yes |
| One integration test class, plus unit tests | `.\mvnw.cmd verify "-Dit.test=MySqlIT"` | Yes |
| Build without integration tests | `.\mvnw.cmd verify -DskipITs` | No |
| Run with H2 | `.\mvnw.cmd spring-boot:run` | No |
| Run with Compose MySQL | `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=localmysql"` | Yes |

No linter or formatter is configured.

---

## 3. Architecture and implementation rules

Beer and Customer follow the same structure:

```text
controller -> services -> repositories -> entities
                  |
               mappers <-> model (DTOs)
```

| Package | Responsibility | Rules to preserve |
|---|---|---|
| `controller` | REST endpoints returning `ResponseEntity` | Reuse path constants such as `BEER_PATH` (`/api/v1/beer`) and `BEER_PATH_ID`; tests reference them. Validate DTOs with `@Validated`. |
| `model` | API contracts: `BeerDTO`, `CustomerDTO` | Keep the Bean Validation annotations, and the constructor annotations Jackson 3 needs (see section 6). |
| `entities` | JPA models with persistence constraints | Use `@UuidGenerator` and `@JdbcTypeCode(SqlTypes.VARCHAR)` for UUIDs stored as `varchar(36)`. Store `BeerStyle` as `SMALLINT`, matching migrations. |
| `mappers` | MapStruct entity/DTO conversion | Generated mappers are Spring beans through `-Amapstruct.defaultComponentModel=spring`. |
| `repositories` | Spring Data JPA repositories | `BeerRepository`, `CustomerRepository`; plain `JpaRepository` interfaces, no custom implementations. |
| `services` | Resource operations | `*ServiceJPA` is `@Primary`. Keep the earlier map-based `*ServiceImpl` beans: controller tests also instantiate them for sample DTOs. |
| `bootstrap` | Initial data via `BootstrapData` | The `CommandLineRunner` seeds 3 beers and 3 customers when their tables are empty. |

**Persistence:** keep `@Version` for optimistic locking. Let `@CreationTimestamp` and `@UpdateTimestamp` populate `createdDate` and `updateDate`; do not assign them manually. Open-in-view is disabled; the model has no lazy associations.

**Annotation processing:** `pom.xml` registers Lombok, lombok-mapstruct-binding and mapstruct-processor for `default-compile`. MapStruct's version is pinned explicitly because Boot does not manage it.

### HTTP behavior

| Outcome | Response |
|---|---|
| Successful POST | `201` with a `Location` header |
| Successful PUT / PATCH / DELETE | `204` |
| Missing entity | `NotFoundException`, annotated with `@ResponseStatus(404)` |
| Invalid request DTO | `CustomErrorController` (`@ControllerAdvice`) handles `MethodArgumentNotValidException`: `400` with a list of `{field: message}` maps |
| JPA transaction validation failure | `CustomErrorController` handles `TransactionSystemException`: plain `400` |

---

## 4. Database environments

| Environment | Database / connection | Schema management | Configuration |
|---|---|---|---|
| Default application and H2 tests | In-memory H2; Compose disabled via `spring.docker.compose.enabled=false` (it is on by default once `spring-boot-docker-compose` is on the classpath, and this line applies to every profile that does not override it) | Hibernate generates schema; Flyway off | `src/main/resources/application.properties` |
| `localmysql` with Compose | `mysql:9.5` (pinned, not `latest`), host port **3308**, container `mysql-spring7-rest-sec13-chap145`, database `jt_spring7_rest_sec13_chap145_db` | Flyway on; `ddl-auto=validate` | `application-localmysql.properties` (`spring.docker.compose.enabled=true`) and `compose.yaml` |
| `testcontainers` (tests only) | Shared `mysql:9.5` container; connection supplied by `@ServiceConnection` | Flyway on; `ddl-auto=validate` | `src/test/resources/application-testcontainers.properties` |

The `localmysql` profile declares **no** `spring.datasource.*` of its own: url, user and password all come from Compose, so the profile only works with Compose enabled. It does configure a Hikari pool named `RestDB-Pool` (max 5 connections) and logs formatted SQL with its bind values.

### Compose setup and lifecycle

1. Copy `.env.example` to `.env` beside `compose.yaml`.
2. Fill in `MYSQL_USER`, `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD`. The file is Git-ignored; Compose rejects missing values.
3. Start with the `localmysql` command above. Boot waits for the container to become healthy.

| Detail | Behavior |
|---|---|
| Project isolation | Keep the Compose project name `spring-7-rest-mvc-sb411` and the fixed `container_name` `mysql-spring7-rest-sec13-chap145`: both name this lecture, so Docker cannot mix the container up with one from another lesson. |
| Graceful shutdown | `spring.docker.compose.stop.command=down` with `stop.arguments=-v` makes Boot run `docker compose down -v`, removing the container, network and the named data volume. The next start migrates and seeds a fresh database. |
| Forced termination | No shutdown hook runs; the container and its data remain. The data directory is the **named** volume `mysql-spring7-rest-sec13-chap145-data` (declared in `compose.yaml` with an explicit `name:`, so Compose adds no project prefix), so a leftover can be inspected or removed by name instead of being an anonymous hash. |
| Changed credentials or database name | MySQL initialization variables only affect an empty volume. To reinitialize, remove the old container and volume with `docker compose down -v`; this deletes its data. |

### Standalone MySQL (legacy, no longer wired up)

`src/scripts/mysql-init.sql` creates a `restdb` database and a `restadmin` user on a MySQL installed on the machine (port 3307). Earlier lessons connected the `localmysql` profile to it; that datasource configuration is gone, so using it again means adding `spring.datasource.*` back and setting `spring.docker.compose.enabled=false`. Its `mysql_native_password` authentication works on MySQL 8.0 but is disabled or removed in 8.4+/9.x. Flyway error `1045` (`Access denied`) usually means a missing user or a wrong password.

### Migration rule

**Add a new migration whenever an entity column changes; never edit an applied migration.**

Store migrations in `src/main/resources/db/migration` using `V<n>__description.sql`. H2 can still start successfully when the MySQL schema is out of sync; `localmysql` validates it and fails.

---

## 5. Testing conventions

### Test selection

| Test pattern | Setup | Runner | Docker |
|---|---|---|---|
| `*ControllerTest` | `@WebMvcTest` with `@MockitoBean` services | Surefire (`test`) | No |
| `*RepositoryTest`, `BootstrapDataTest` | `@DataJpaTest` | Surefire (`test`) | No |
| `*ControllerIT` | `@SpringBootTest` with H2, controllers called directly; `BeerControllerIT` adds `@AutoConfigureMockMvc`. Data-changing tests are `@Transactional` | Failsafe (`verify`) | No |
| `Spring7RestMvcApplicationTests` | `@SpringBootTest` context check | Surefire (`test`) | No |
| `MySqlIT` | `@DataJpaTest`, extends `MySqlContainerBase` | Failsafe (`verify`) | Yes |

Surefire selects `*Test` / `*Tests`; Failsafe selects `*IT`. `verify` runs unit tests first, then `BeerControllerIT`, `CustomerControllerIT` and `MySqlIT`. Controller integration tests depend on seed data: `testListBeers` expects exactly **3 beers**.

### MySQL integration tests

- Extend `MySqlContainerBase` in the test root package. It starts one `mysql:9.5` container per JVM, shared by subclasses and removed by Testcontainers' Ryuk at JVM exit.
- The base class activates `testcontainers` and supplies connection details through `@ServiceConnection`; no manual datasource wiring is needed.
- `repositories/MySqlIT` imports `BootstrapData` with `@Import` so the JPA slice seeds its data; it logs `### ... loaded` or `### ... Bootstrap skipped`. It needs no `@AutoConfigureTestDatabase`: Boot 4.1's `NON_TEST` replacement policy retains the test-supplied datasource.
- **Missing Docker must fail these tests, not skip them.** Use `test` for checks without Docker, or explicitly opt out of integration tests with `-DskipITs`.
- Docker Compose is not involved in tests (`spring.docker.compose.skip.in-tests=true` by default).

---

## 6. Gotchas

Traps of this stack (Boot 4.1 / Spring 7 / Hibernate 7 / Jackson 3 / Java 25). Check here before assuming an API behaves as in earlier versions.

| Topic | Rule |
|---|---|
| JSON | **Use `JsonMapper` (`tools.jackson.databind.json.JsonMapper`), never the old `ObjectMapper`.** Jackson 3 keeps `ObjectMapper` alive, but this codebase moved off it everywhere - do not reintroduce it, not even in a new test. |
| Jackson and Lombok | DTOs need `@NoArgsConstructor` and `@AllArgsConstructor` next to `@Builder`: Jackson 3 ignores Lombok's package-private builder constructor when deserializing. |
| Mockito as an agent | Both runners load Mockito through `-javaagent` (JAR path from the `dependency:properties` goal), since self-attaching is being phased out in newer JDKs. Each forks its own JVM, so **change JVM arguments only in the shared `test.jvm.argLine` property in `pom.xml`**. |
| Mockito `@Captor` | Boot 4 no longer initializes `@Captor` fields, so a `@WebMvcTest` class using captors also needs `@ExtendWith(MockitoExtension.class)`. |
| `@Rollback` | Redundant: Spring rolls back `@Transactional` tests by default. Do not add it. |
| Failsafe configuration | Version and execution goals come from the Spring Boot parent's `pluginManagement`; the pom only adds the plugin and its `argLine`. |
| Test support modules | Boot 4 splits them (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, ...), so `@WebMvcTest` and `@DataJpaTest` live under `org.springframework.boot.<module>.test.autoconfigure` - let the IDE resolve them rather than guessing. |
| Container credentials | `MYSQL_USER`, `MYSQL_PASSWORD` and `MYSQL_DATABASE` are only applied to an **empty** data volume. Renaming any of them without `docker compose down -v` produces `Access denied`. |
| Port 3308 | Shared with containers of other lessons; only one can run at a time. |
| Flyway warning on MySQL 9.5 | `Using MySQL 9.5 which is newer than the version Flyway has been verified with` - expected, the migrations apply normally. Both the Compose service and the Testcontainers image are pinned to `mysql:9.5`; keep them equal so local runs and tests validate on the same server. |
| Classpath files in a jar | Read a classpath resource through `Resource.getInputStream()`, never `getFile()`, `ResourceUtils.getFile()` or `new File(...)`: inside a jar it is an archive entry, not a file. **Tests never run from the jar, so a green build proves nothing** - see `docs/reading-classpath-files-from-a-jar.md`. |
| Version-specific claims | The stack is bleeding edge. Verify against the JARs in `~/.m2` or current docs instead of recalling an API, and prove behaviour by running the tests. |
