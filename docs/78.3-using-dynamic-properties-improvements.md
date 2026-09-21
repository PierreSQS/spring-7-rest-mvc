# Section 13, Chapter 142 (`Sec13_Chap142`): possible improvements

This lecture adds `repositories/MySqlTest`: a `@SpringBootTest` that starts a MySQL container with Testcontainers and points the Spring datasource at it with `@DynamicPropertySource`. JT's code for it is on his branch `78.3-using-dynamic-properties`.

Versions compared: JT's upstream branch uses **Spring Boot 4.0.6**, this project **Spring Boot 4.1.1**. Both resolve **Testcontainers 2.0.5**, so most differences come from Testcontainers 2.x itself, not from the Boot versions.

Status legend: ✅ done · ⏭ covered by a later lecture · 💡 suggestion, not applied

## 1. Differences caused by versions

| # | JT's code | Better on our versions | Status |
|---|---|---|---|
| 1 | `org.testcontainers.containers.MySQLContainer` | `org.testcontainers.mysql.MySQLContainer` - the 2.x home of the class; the old one is deprecated | ✅ done |
| 2 | `MySQLContainer<?>` (generic) | `MySQLContainer` - the 2.x class is not generic | ✅ done |
| 3 | `@DynamicPropertySource`, wired by hand | `@ServiceConnection` on the container field (Boot 3.1+) | ⏭ later lecture (JT's branch `78.4`) |
| 4 | Container declared inside the test class | A reusable `@TestConfiguration` with a `@Bean @ServiceConnection` container, imported where needed - what Initializr generates for Boot 4.1 | 💡 optional, after the `@ServiceConnection` lecture (JT's branch `78.4`) |

## 2. General improvements

| # | Improvement | Why | Status |
|---|---|---|---|
| 5 | Package-private class, no unused `DataSource` | Less noise; JUnit 5 does not need `public` | ✅ done |
| 6 | `assertThat(beers).isNotEmpty()` | Reads more directly than `hasSizeGreaterThan(0)` | ✅ done |
| 7 | Name it `MySqlIT` and run it with Failsafe | Today every `mvn test` needs Docker and starts a container | ⏭ later lecture (JT's branch `78.5`) |
| 8 | `@Testcontainers(disabledWithoutDocker = true)` | Skips the test instead of failing when Docker is missing - but see the caveat below | ✅ done - not yet verified without Docker |
| 9 | Pin the image to MySQL 8.4 LTS, or to what production uses | `9.2` is on MySQL's short-lived "innovation" track, while the local server is `8.0.27`, so tests do not run against the version actually used | ✅ done - `mysql:8.4` |
| 10 | A dedicated test profile instead of `localmysql` | See the explanation below | ✅ done - profile `testcontainers` |
| 11 | Assert something only real MySQL can prove | "More than 0 beers" only shows that the context started and `BootstrapData` ran. Better: check that a 51-character `beerName` is rejected by the column limit, or that UUIDs round-trip as `varchar(36)` | 💡 makes the test meaningful |
| 12 | `@DataJpaTest` instead of `@SpringBootTest` | Loads only the JPA slice, so it is faster. (Originally stated that it needs `@AutoConfigureTestDatabase(replace = NONE)` to avoid H2 - that was wrong, see the correction below.) | ✅ done |
| 13 | Container reuse (`withReuse(true)`) or a shared singleton container | Each test class starts its own MySQL, which gets slow as container tests multiply | ✅ done - singleton in `MySqlContainerBase` |

**Applied:** #6, #8, #9, #10, #12 and #13 (see "How they were applied" below). **Still open:** #11; #3 and #7 arrive with the next lectures anyway.

## Improvement 10 in plain English: a dedicated test profile

A **profile** is a named bundle of settings that can be switched on with one word. `localmysql` is the bundle for running the app on this PC against the local MySQL. It says:

- connect to MySQL on **port 3307** as **restadmin / restadmin**;
- run **Flyway** and **check that the tables match the entities**;
- **print every SQL statement and every value sent with it** - handy when debugging by hand.

`MySqlTest` switches the whole bundle on with `@ActiveProfiles("localmysql")`, but it only needs *"run Flyway and check the tables"*. It gets everything else too:

1. **Connection details that are thrown away.** The container runs on a random port with its own user, so `@DynamicPropertySource` overwrites them immediately. A reader could easily think the test talks to the local database.
2. **Noise.** In one run, 45 of 227 log lines were just values sent to the database (`binding parameter (1:VARCHAR) <- ...`), and this grows with every new test.
3. **Accidental coupling.** Changing `localmysql` for local work - turning Flyway off, changing the port - silently changes the test too.

*Analogy:* you need a stapler, so you borrow your colleague's entire desk.

**Fix:** give the test its own small profile with only what it needs, e.g. `src/test/resources/application-testcontainers.properties`:

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

and use `@ActiveProfiles("testcontainers")`. The container supplies the connection details.

## Improvement 8 - why skipping is *not* automatically better than failing

Without Docker, `MySqlTest` currently errors and the whole build fails - even for a change unrelated to MySQL. With `disabledWithoutDocker = true` the test is reported as **skipped** (`Skipped: 1`), not passed, and the build stays green.

| Situation | Failing | Skipping |
|---|---|---|
| Laptop without Docker, editing a controller | ❌ blocked by an unrelated test | ✅ can keep working |
| A colleague clones the repo and runs `mvn test` | ❌ confusing failure | ✅ works, with 1 skipped |
| **The CI server lost Docker** | ✅ loud, someone notices | ❌ **quiet: green build, MySQL never tested** |
| A Flyway migration changed while Docker was off | ✅ you find out | ❌ **looks passed, never ran** |

The last two rows are the risk: a skipped test looks fine at a glance, so it gives false confidence.

**The better answer is #7:** split the tests so `mvn test` runs only what needs no Docker, and `mvn verify` also runs the container tests and **fails loudly** if Docker is missing. That is exactly what JT does in a later lecture (his branch `78.5`: Failsafe, `MySqlIT`). So #8 is only worth it when working without Docker regularly *and* consciously checking the skip count.

## How they were applied

- **`MySqlContainerBase`** (test root package) is the shared base for MySQL tests:
  - **#13** - one `static final` container, started in a static block and *not* annotated with `@Container`, so JUnit does not stop it after each class. Every subclass shares it; Ryuk removes it when the JVM exits. With a single container test class the gain only shows once a second one exists.
  - **#9** - `mysql:8.4` (LTS) instead of `mysql:9.2`.
  - **#8** - `@Testcontainers(disabledWithoutDocker = true)`.
  - **#10** - `@ActiveProfiles("testcontainers")`, backed by `src/test/resources/application-testcontainers.properties` with only `spring.flyway.enabled=true` and `spring.jpa.hibernate.ddl-auto=validate`. The bind-value TRACE lines in the test output dropped from 45 to 0.
  - The `@DynamicPropertySource` wiring moved here from `MySqlTest`.
- **`MySqlTest`** extends the base class:
  - **#12** - `@DataJpaTest`. The JPA slice only loads JPA components, so the test `@Import`s `BootstrapData`, autowires it and calls `run()` in `@BeforeEach` (safe to call repeatedly: it only inserts into empty tables). Flyway still runs in the slice.
  - **#6** - `assertThat(beers).isNotEmpty()`.

**#8 is not verified yet.** Pointing `DOCKER_HOST` at a closed port did not simulate a missing Docker: Testcontainers fell back to Docker Desktop's named pipe and ran the test. Verifying it needs Docker Desktop actually stopped; `mvnw test -Dtest=MySqlTest` should then report `Skipped: 1`.

**Correction (Sec13_Chap143):** `@AutoConfigureTestDatabase(replace = NONE)` was never needed on Boot 4.1.1. Its default there is `replace = NON_TEST`, which keeps a datasource the test supplies itself (`@DynamicPropertySource` or `@ServiceConnection`) instead of swapping in H2. A control run with the old `@DynamicPropertySource` base class and no annotation still used MySQL 8.4. The annotation was removed in the next lecture.
