# Repository guide

Guidance for Claude Code when working on this repository.

---

## 1. Project at a glance

A beer and customer REST API built alongside John Thompson's **Spring Framework 6: Beginner to Guru** course on Udemy.

| Area | Reference |
|---|---|
| Runtime | Java 25, Spring Boot 4.1.1, Spring Framework 7, Spring Security 7.1 |
| Persistence / JSON | Hibernate 7.4, Jackson 3 |
| Libraries | Spring Data JPA, MapStruct 1.6.3, Lombok, Flyway, OAuth2 resource server |
| Root package | `guru.springframework.spring7restmvc` |
| Entry point | `Spring7RestMvcApplication` |
| Branches | One per lesson, named after JT's branch plus `-jt-sb411` (e.g. `121-resource-server-refacto-jwt-jt-sb411`). Branches are **never merged**: each one documents an improved variant of JT's lecture on the newest Spring Boot (`-sbNNN` = Boot version). |
| Commits | Prefix with the course section: `Sec24_Chap251: ...`; `-X` for own additions, `-NOK` when tests are knowingly left red |

### Documentation

| Kind | Naming | Rule |
|---|---|---|
| Rules | `docs/regles-a-retenir.md` | The rules learned on this project (in French): each with the rule, an example and the proof (test or commit). Extended lesson by lesson. |
| Lesson review | `secNN[-chapNNN]-<topic>-improvements.md` | A snapshot of one lecture: what JT did, what this project changed and why. It is **history**; it is not updated when later sections move on, and it says so at the top. |
| Topic page | `<topic>.md`, e.g. `spring-security-test-user-vs-httpbasic.md` | Explains a mechanism that outlives the lecture. Must describe the code as it is **today**. |

Never name a document after JT's branch index - it is his numbering, not the chapter number.

**When a change contradicts something a topic page or this guide states, correct it in the same commit.**
Both were found stale on 2026-09-24: `CLAUDE.md` still claimed the bootstrap seeded 3 beers, five months
of lessons after it stopped doing so.

**Older lesson branches carry older copies of these files.** They are frozen and describe the code they
contain, so they are deliberately not back-ported; read the newest branch for current truth.

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

No linter or formatter is configured. Calling the running API needs a JWT from an authorization server on `http://localhost:9000` (see section 4).

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
| `config` | `SpringSecConfig`: the security filter chain | See section 4. |
| `controller` | REST endpoints returning `ResponseEntity`, plus `CustomErrorController` and `NotFoundException` | Reuse path constants such as `BEER_PATH` (`/api/v1/beer`) and `BEER_PATH_ID`; tests reference them. Validate request DTOs with `@Validated` on POST and PUT, **not** on PATCH: a PATCH body may be partial. |
| `model` | API contracts: `BeerDTO`, `CustomerDTO`, `BeerStyle`, `BeerCSVRecord` | Keep the Bean Validation annotations (`BeerDTO.beerName` is `@NotBlank` and `@Size(max = 50)` like the column; `CustomerDTO.name` is `@NotBlank`), and the constructor annotations Jackson 3 needs (see section 6). |
| `entities` | `Beer`, `Customer`, `Category`, `BeerOrder`, `BeerOrderLine`, `BeerOrderShipment` | Use `@UuidGenerator` and `@JdbcTypeCode(SqlTypes.CHAR)` for UUIDs stored as `varchar(36)`. Store `BeerStyle` as `SMALLINT`, matching migrations. A field initializer used with `@Builder` needs `@Builder.Default`, or the builder leaves it `null`. |
| `mappers` | MapStruct entity/DTO conversion | Generated mappers are Spring beans through `-Amapstruct.defaultComponentModel=spring`. In the DTO -> entity direction, ignore what belongs to the database (`id`, ...; rule R1). |
| `repositories` | Spring Data JPA repositories | `BeerRepository`, `CustomerRepository`, `CategoryRepository`, `BeerOrderRepository`; plain `JpaRepository` interfaces, no custom implementations. The three beer finders take a `Pageable` and return `Page<Beer>`. |
| `services` | Resource operations, CSV parsing | `BeerServiceJPA` and `CustomerServiceJPA` are the only implementations of `BeerService` / `CustomerService`, so they need no `@Primary`. JT's map-based `BeerServiceImpl` / `CustomerServiceImpl`, written to introduce dependency injection, were removed: the application never used them; the `@WebMvcTest` classes take their sample DTOs from `SampleDtos` in the test `controller` package. `BeerCsvServiceImpl` parses `csvdata/beers.csv`. |
| `bootstrap` | Initial data via `BootstrapData` | The `CommandLineRunner` seeds 3 hand-written beers when the table is empty, then every row of `csvdata/beers.csv` when it holds fewer than 10 (**2413 beers** in total), and 3 customers when that table is empty. |

**Paging and sorting** (`BeerServiceJPA.buildPageRequest`): the API counts pages from **1**, Spring Data from 0. Without parameters a request gets page 1 with **25** beers; `pageSize` is capped at **1000**; a `pageNumber` or `pageSize` below 1 is treated as missing, so `PageRequest.of` can never throw. Every page is sorted by `beerName` ascending, which keeps a beer from landing on two pages. `showInventory=false` empties `quantityOnHand` **on the DTOs**, never on the entities: a managed entity changed in the service is written back by dirty checking (rule R22).

**Persistence:** keep `@Version` (`Integer`) for optimistic locking. Let `@CreationTimestamp` and `@UpdateTimestamp` populate `createdDate` and `updateDate`; do not assign them manually. Open-in-view is disabled, and all collections - the `@OneToMany` sides `Customer.beerOrders`, `BeerOrder.beerOrderLines`, `Beer.beerOrderLines` and the `@ManyToMany` pair `Beer.categories` / `Category.beers` - are lazy (the JPA default): read them inside a transaction, or they throw `LazyInitializationException`. `Beer.categories` owns the join table `beer_category`; `Category.beers` is its `mappedBy` mirror - two owning sides would insert every join row twice. Every table names its columns `created_date`, `update_date` and `version integer` (`V6`, `V7`) - including `beer_order` and `beer_order_line`, where JT uses `last_modified_date` and `version bigint`; map their entities to `updateDate` and `Integer version`, not to his `lastModifiedDate` / `Long`.

**Annotation processing:** `pom.xml` registers Lombok, lombok-mapstruct-binding and mapstruct-processor for `default-compile`. MapStruct's version is pinned explicitly because Boot does not manage it. The javac note `Die folgenden Optionen wurden von keinem Prozessor erkannt: [mapstruct.defaultComponentModel]` on an incremental compile only means no compiled file had a `@Mapper`; it is harmless.

### HTTP behavior

| Outcome | Response |
|---|---|
| No or invalid Bearer token | `401` for `GET`; `403` for writes (CSRF is only ignored for requests carrying a Bearer token) |
| `GET` on the beer list | a serialized Spring Data `Page`: `content` plus the paging fields. `VIA_DTO` serialization is not enabled yet (open work on branch 116) |
| `GET` on the customer list | a plain JSON array |
| Successful POST | `201` with a `Location` header |
| Successful PUT / PATCH / DELETE | `204` |
| Missing entity | `NotFoundException`, annotated with `@ResponseStatus(404)` |
| Invalid request DTO | `CustomErrorController` (`@ControllerAdvice`) handles `MethodArgumentNotValidException`: `400` with a list of `{field: message}` maps |
| JPA transaction validation failure (e.g. a PATCH, which has no `@Validated`) | `CustomErrorController` handles `TransactionSystemException`: its root cause is a `ConstraintViolationException`, answered with the same `400` list of `{field: message}` maps; any other cause gets a plain `400` |

---

## 4. Security

The API is an **OAuth2 resource server** (`spring-boot-starter-security-oauth2-resource-server`). `SpringSecConfig` requires authentication on every request and validates JWTs:

```java
httpSecurity.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
httpSecurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

| Detail | Behavior |
|---|---|
| `.jwt(...)` is mandatory | `oauth2ResourceServer(withDefaults())` alone fails at startup: *Jwt and Opaque Token are the only supported formats*. `oauth2AuthorizationServer` is a different thing (it compiles, but configures the opposite side). |
| Issuer | `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000`. The `JwtDecoder` is created lazily, so the application and the tests start even when the authorization server is down; only a real token check needs it. |
| CSRF | The resource-server configurer already ignores CSRF for requests with a Bearer token; no `csrf(...)` configuration is needed. |
| No security user | The former `spring.security.user.*` properties and their `.env` entries are gone. |
| Tests | Add `.with(jwt())` (`SecurityMockMvcRequestPostProcessors`) to every `MockMvc` request. The bare `jwt()` is enough because the chain only checks `authenticated()`; no claims or scopes are needed. |
| No `-test` starter | `spring-boot-starter-security-oauth2-resource-server-test` is not needed: the main module already registers its auto-configurations for `@WebMvcTest`. `spring-boot-starter-security-test` is `test`-scoped. |

---

## 5. Database environments

| Environment | Database / connection | Schema management | Configuration |
|---|---|---|---|
| Default application and H2 tests | In-memory H2; Compose disabled via `spring.docker.compose.enabled=false` (it is on by default once `spring-boot-docker-compose` is on the classpath, and this line applies to every profile that does not override it) | Hibernate generates schema; Flyway off | `src/main/resources/application.properties` |
| `localmysql` with Compose | `mysql:9.5` (pinned, not `latest`), host port **3308**, container `mysql-spring7-rest-sec22-assn25`, database `jt_spring7_rest_sec22_assn25_db` | Flyway on (`V1` to `V7`); `ddl-auto=validate` | `application-localmysql.properties` (`spring.docker.compose.enabled=true`) and `compose.yaml` |
| `MySqlIT` (tests only) | Its own `mysql:9.5` Testcontainers container, connection supplied by `@ServiceConnection`; runs with the `localmysql` profile | Flyway on; `ddl-auto=validate` | `repositories/MySqlIT.java` |

The `localmysql` profile declares **no** `spring.datasource.*` url, user or password of its own: they all come from Compose (or from `@ServiceConnection` in `MySqlIT`). It does configure a Hikari pool named `RestDB-Pool` (max 5 connections) and logs formatted SQL with its bind values.

### Compose setup and lifecycle

1. Copy `.env.example` to `.env` beside `compose.yaml`.
2. Fill in `MYSQL_USER`, `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD`. The file is Git-ignored; Compose rejects missing values.
3. Start with the `localmysql` command above. Boot waits for the container to become healthy.

| Detail | Behavior |
|---|---|
| Project isolation | Keep the Compose project name `spring-7-rest-mvc-jt-sb411-sec22-assn25` and the fixed `container_name` `mysql-spring7-rest-sec22-assn25`: both name a lecture, so Docker cannot mix the container up with one from another lesson. The project name matters most: when a container of the same project is already running, Boot skips `docker compose up` and connects to it, whatever its database. Rename project, container, volume and database together, and only when a new Flyway migration changes the schema: a lecture without one keeps the previous names, so the names show the chapter in which the current schema was first created (`V7`, Section 22 Assignment 25). |
| Graceful shutdown | `spring.docker.compose.stop.command=down` with `stop.arguments=-v` makes Boot run `docker compose down -v`, removing the container, network and the named data volume. The next start migrates and seeds a fresh database. |
| Forced termination | No shutdown hook runs; the container and its data remain. The data directory is the **named** volume `mysql-spring7-rest-sec22-assn25-data` (declared in `compose.yaml` with an explicit `name:`, so Compose adds no project prefix), so a leftover can be inspected or removed by name instead of being an anonymous hash. |
| Changed credentials or database name | MySQL initialization variables only affect an empty volume. To reinitialize, remove the old container and volume with `docker compose down -v`; this deletes its data. |

### Standalone MySQL (legacy, no longer wired up)

`src/scripts/mysql-init.sql` creates a `restdb` database and a `restadmin` user on a MySQL installed on the machine (port 3307). Earlier lessons connected the `localmysql` profile to it; that datasource configuration is gone, so using it again means adding `spring.datasource.*` back and setting `spring.docker.compose.enabled=false`. Its `mysql_native_password` authentication works on MySQL 8.0 but is disabled or removed in 8.4+/9.x. Flyway error `1045` (`Access denied`) usually means a missing user or a wrong password.

### Migration rule

**Add a new migration whenever an entity column changes; never edit an applied migration.**

Store migrations in `src/main/resources/db/migration` using `V<n>__description.sql`. H2 can still start successfully when the MySQL schema is out of sync; `localmysql` validates it and fails.

---

## 6. Testing conventions

### Test selection

| Test pattern | Setup | Runner | Docker |
|---|---|---|---|
| `*ControllerTest` | `@WebMvcTest(XController.class)` with `@Import(SpringSecConfig.class)`, `@MockitoBean` services and `.with(jwt())` | Surefire (`test`) | No |
| `*RepositoryTest`, `BootstrapDataTest` | `@DataJpaTest` | Surefire (`test`) | No |
| `BeerTest`, `BeerOrderLineTest`, `BeerCsvServiceImplTest` | Plain unit tests | Surefire (`test`) | No |
| `*ControllerIT` | `@SpringBootTest` + `@AutoConfigureMockMvc` with H2. **Every test sends its request through `MockMvc` with `.with(jwt())`**, never by calling the controller method: a direct call skips security, `@Validated`, `CustomErrorController` and the JSON conversion (it once let a beer with only a name be "created"). Not-found cases expect `404`, not an exception. Data-changing tests are `@Transactional` and call `entityManager.flush()` + `clear()` before reading back. | Failsafe (`verify`) | No |
| `Spring7RestMvcApplicationTests` | `@SpringBootTest` context check | Surefire (`test`) | No |
| `MySqlIT` | `@SpringBootTest` + `@Testcontainers` + `@ActiveProfiles("localmysql")` | Failsafe (`verify`) | Yes |

Surefire selects `*Test` / `*Tests`; Failsafe selects `*IT`. `verify` runs unit tests first, then `BeerControllerIT`, `CustomerControllerIT` and `MySqlIT`.

The controller integration tests depend on the seeded data. `BootstrapDataTest` expects exactly 2413 beers and 3 customers; `BeerControllerIT.testListBeers` asks for 2413 beers and gets the 1000 of the page-size cap. The search counts are fixed constants too - 336 beers named `IPA`, 572 of style `IPA`, 324 both - so a change to `beers.csv` or to `BootstrapData.beerStyleOf` makes these tests fail on purpose.

In a `@Transactional` test, call `entityManager.flush()` and `clear()` before reading back what was written: otherwise `findById` returns Hibernate's cached object, not the database row (rules R6 and R9).

### MySQL integration test

- `repositories/MySqlIT` declares its own `static MySQLContainer` (`mysql:9.5`) with `@Container` and `@ServiceConnection`; no datasource wiring is needed.
- As a full `@SpringBootTest` it runs Flyway and `BootstrapData`, then checks that beers were loaded.
- **Missing Docker must fail this test, not skip it.** Use `test` for checks without Docker, or explicitly opt out of integration tests with `-DskipITs`.
- Docker Compose is not involved in tests (`spring.docker.compose.skip.in-tests=true` by default), although the `localmysql` profile enables it.

---

## 7. Gotchas

Traps of this stack (Boot 4.1 / Spring 7 / Hibernate 7 / Jackson 3 / Java 25). Check here before assuming an API behaves as in earlier versions.

| Topic | Rule |
|---|---|
| JSON | **Use `JsonMapper` (`tools.jackson.databind.json.JsonMapper`), never the old `ObjectMapper`.** Jackson 3 keeps `ObjectMapper` alive, but this codebase moved off it everywhere - do not reintroduce it, not even in a new test. |
| Jackson and Lombok | DTOs need `@NoArgsConstructor` and `@AllArgsConstructor` next to `@Builder`: Jackson 3 ignores Lombok's package-private builder constructor when deserializing. |
| Mockito as an agent | Both runners load Mockito through `-javaagent` (`${settings.localRepository}/org/mockito/mockito-core/${mockito.version}/...`, plus `-Xshare:off`), since self-attaching is being phased out in newer JDKs. Each forks its own JVM, and the `argLine` is written twice (Surefire and Failsafe): **change both together**. |
| Mockito `@Captor` | Boot 4 no longer initializes `@Captor` fields, so a `@WebMvcTest` class using captors also needs `@ExtendWith(MockitoExtension.class)`. |
| `@Rollback` | Redundant: Spring rolls back `@Transactional` tests by default. Do not add it. |
| Dirty checking | A managed entity that is modified is written at flush, without any `save()` (rule R22). Change DTOs, not entities, when the change must not reach the database. |
| `@WebMvcTest` placeholders | The slice has no `PropertyPlaceholderAutoConfiguration`: an unresolvable `@Value("${...}")` silently keeps the raw `${...}` text, where `@SpringBootTest` fails. A green `@WebMvcTest` proves nothing about properties (rule R11). |
| Failsafe configuration | Version and execution goals come from the Spring Boot parent's `pluginManagement`; the pom only adds the plugin and its `argLine`. |
| Test support modules | Boot 4 splits them (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, ...), so `@WebMvcTest` and `@DataJpaTest` live under `org.springframework.boot.<module>.test.autoconfigure` - let the IDE resolve them rather than guessing. |
| Container credentials | `MYSQL_USER`, `MYSQL_PASSWORD` and `MYSQL_DATABASE` are only applied to an **empty** data volume. Renaming any of them without `docker compose down -v` produces `Access denied`. |
| Port 3308 | Shared with containers of other lessons; only one can run at a time. |
| Flyway warning on MySQL 9.5 | `Using MySQL 9.5 which is newer than the version Flyway has been verified with` - expected, the migrations apply normally. Both the Compose service and the Testcontainers image are pinned to `mysql:9.5`; keep them equal so local runs and tests validate on the same server. |
| Classpath files in a jar | Read a classpath resource through `Resource.getInputStream()` (in UTF-8), never `getFile()`, `ResourceUtils.getFile()` or `new File(...)`: inside a jar it is an archive entry, not a file. **Tests never run from the jar, so a green build proves nothing** (rule R19). |
| Version-specific claims | The stack is bleeding edge. Verify against the JARs in `~/.m2` or current docs instead of recalling an API, and prove behaviour by running the tests. |
