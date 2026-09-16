# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Course project for John Thompson's "Spring Framework 6: Beginner to Guru" (Udemy). It is a REST API for beers and customers, built on Spring Boot 3.4 with Java 21, Spring Data JPA, MapStruct, Lombok, and Flyway. Work is done branch by branch, one lesson at a time: each branch (for example `78-fly-add-column`) is merged forward into the next, and commit messages refer to course sections (`Sec11_Chap124-XX: ...`).

## Commands

The development machine runs Windows, so use `.\mvnw.cmd` in PowerShell or `./mvnw` in a POSIX shell. The Maven wrapper pins Maven 3.8.6. Build with **JDK 21**: the machine's `JAVA_HOME` is JDK 25, and with the Lombok version managed by Boot 3.4.0 compilation fails with `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`. Set `$env:JAVA_HOME = 'C:\Pierrot\03_Tools\Oracle_Open_JDKs\jdk-21.0.2'` for the shell first.

```powershell
.\mvnw.cmd clean package                          # build and run unit tests
.\mvnw.cmd test                                   # unit tests only (see note on *IT below)
.\mvnw.cmd test -Dtest=BeerControllerTest         # single test class
.\mvnw.cmd test -Dtest=BeerControllerTest#testPatchBeer   # single test method
.\mvnw.cmd test -Dtest=BeerControllerIT           # integration test class (must be named explicitly)
.\mvnw.cmd spring-boot:run                        # run with H2 in memory (default profile)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=localmysql"   # run against local MySQL
```

The project has no linter or formatter configured.

**`*IT` tests are not run by `mvn test`/`package`.** No failsafe plugin is configured, and Surefire's default includes (`*Test`, `*Tests`, `Test*`, `*TestCase`) don't match `*IT`. Run them with `-Dtest=...`. They are `@SpringBootTest` tests against the default H2 context, and they depend on the seed data from `BootstrapData`: for example, `testListBeers` expects exactly 3 beers.

## Profiles and database

- **Default profile** (`application.properties`): H2 in memory, Flyway **disabled**, and Hibernate generates the schema. Tests use this profile.
- **`localmysql` profile** (`application-localmysql.properties`): MySQL on `127.0.0.1:3307`, database `restdb`, user `restadmin`/`restadmin`. Flyway is **enabled** and `ddl-auto=validate`, so the entity mappings must exactly match the schema produced by the migrations. It also sets up a Hikari pool called `RestDB-Pool` and SQL/bind-value logging.
- `src/scripts/mysql-init.sql` creates the database and the `restadmin` user, and must be run as MySQL root first. It uses `IDENTIFIED WITH mysql_native_password`, which works on MySQL 8.0 but is disabled or removed in 8.4+/9.x. If startup fails with `Access denied ... (Error 1045)` from Flyway, the user is usually missing or has the wrong password.
- Flyway migrations are in `src/main/resources/db/migration` (`V<n>__description.sql`). Whenever you change an entity column, add a new migration (don't edit an applied one). Otherwise the `validate` check fails under `localmysql`, even though H2 still starts fine.

## Architecture

Package root: `guru.springframework.spring7restmvc` (main class `Spring7RestMvcApplication`). Each resource (Beer, Customer) is built from the same set of layers:

- `controller`: `@RestController`s with path constants (`BEER_PATH = "/api/v1/beer"`, `BEER_PATH_ID`) that tests reuse. Handlers return `ResponseEntity` (201 with a `Location` header on POST, 204 on PUT/PATCH/DELETE). A missing entity throws `NotFoundException`, which carries `@ResponseStatus(404)`. `CustomErrorController` (`@ControllerAdvice`) turns `MethodArgumentNotValidException` into a 400 with a list of `{field: message}` maps, and JPA `TransactionSystemException` into a plain 400.
- `model`: DTOs (`BeerDTO`, `CustomerDTO`) with Bean Validation annotations, validated in controllers via `@Validated`. This is the API contract.
- `entities`: JPA entities with their own validation constraints. UUID IDs are stored as `varchar(36)` (`@JdbcTypeCode(SqlTypes.CHAR)`) and `BeerStyle` as `SMALLINT`, to match the MySQL migrations. Both use `@Version` for optimistic locking.
- `mappers`: MapStruct interfaces that convert between entities and DTOs. The compiler arg `-Amapstruct.defaultComponentModel=spring` makes them Spring beans, and the annotation processor order in `pom.xml` (mapstruct, lombok, lombok-mapstruct-binding) must be kept.
- `services`: each resource has **two** implementations of its service interface. `*ServiceJPA` is `@Primary` and does the real work; `*ServiceImpl` is the earlier in-memory, map-based version from previous lessons. It stays a bean and is also instantiated directly in `@WebMvcTest` controller tests as a source of sample DTOs.
- `bootstrap/BootstrapData`: a `CommandLineRunner` that seeds 3 beers and 3 customers when the tables are empty.

Test styles: `*ControllerTest` uses `@WebMvcTest` with `@MockitoBean` services (the Spring Framework 6.2 replacement for `@MockBean`). `*IT` tests are full-context tests that call controllers directly, with `@Transactional @Rollback` on tests that change data. `*RepositoryTest` and `BootstrapDataTest` use `@DataJpaTest`.
