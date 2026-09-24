# Section 13, Chapter 143 (`Sec13_Chap143`): possible improvements

> **Snapshot.** This page reviews one lesson and describes the code as it stood then. Later sections
> changed some of it. For how the project behaves **today**, see `CLAUDE.md` and the topic pages such as
> `service-layer-boundaries.md`.

This lecture replaces the hand-written `@DynamicPropertySource` wiring with `@ServiceConnection`: Spring Boot recognizes the MySQL container and derives the datasource url, username and password from it by itself. JT's code for it is on his branch `78.4-Using-Service-Connection`.

Versions compared: JT's upstream branch uses **Spring Boot 4.0.6**, this project **Spring Boot 4.1.1**. Both resolve **Testcontainers 2.0.5**.

Status legend: ✅ done · ⏭ covered by a later lecture · 💡 suggestion, not applied

## 1. Differences caused by versions

| # | JT's code | On our versions | Status |
|---|---|---|---|
| 1 | `@ServiceConnection` on the deprecated `org.testcontainers.containers.MySQLContainer<?>` | The 2.x `org.testcontainers.mysql.MySQLContainer` works the same way with `@ServiceConnection` | ✅ done (since Sec13_Chap142) |
| 2 | `@ServiceConnection` on a `@Container` field in the test class | Also works on a **superclass singleton** that is not a `@Container` - verified: `MySqlTest` connects to the container's MySQL 8.4 with no other datasource settings | ✅ done |
| 3 | `@SpringBootTest`, so the H2 question never comes up | With `@DataJpaTest`, no `@AutoConfigureTestDatabase` is needed: in Boot 4.1.1 its default is `replace = NON_TEST` (read from the jar), which keeps a datasource supplied by the test | ✅ done - and the claim in the Sec13_Chap142 doc corrected |

## 2. General improvements

| # | Improvement | Why | Status |
|---|---|---|---|
| 4 | Don't combine `@ServiceConnection` with the `localmysql` profile | JT still activates `localmysql`, whose url, user and password are now **silently ignored** - even more misleading than before | ✅ done - profile `testcontainers` |
| 5 | Log whether the seed data was loaded | The server log shows whether seeding happened: `### 3 beers loaded into the DB` or `### Beers are present in the DB. Bootstrap skipped` | ✅ done |
| 6 | Remove the redundant `@BeforeEach` seeding in `MySqlTest` | The imported `BootstrapData` already runs as a `CommandLineRunner` at context start - the new log showed it running twice | ✅ done |
| 7 | Move the container into a `@TestConfiguration` with `@Bean @ServiceConnection` | Reusable by tests *and* by a `TestSpring7RestMvcApplication` that starts the app against a container, so no local MySQL is needed during development | 💡 worth trying |
| 8 | Be aware of shared data in the singleton container | See the explanation below | 💡 keep in mind |
| 9 | Don't set `.version(1)` on new customers in `BootstrapData` | See the explanation below | 💡 small fix (JT's code) |
| 10 | Verify the Docker-less skip (#8 of Sec13_Chap142) | Still unproven: needs Docker Desktop actually stopped; `mvnw test -Dtest=MySqlTest` should then report `Skipped: 1` | 💡 open |
| 11 | A test that proves something only real MySQL can (#11 of Sec13_Chap142) | e.g. the 50-character `beerName` column limit, or UUIDs stored as `varchar(36)` | 💡 open |
| 12 | Rename to `MySqlIT` and run it with Failsafe | So that `mvn test` no longer needs Docker | ⏭ later lecture (JT's branch `78.5`) |

**Top 3 for now:** #9 (tiny fix, real trap), #7 (the most useful next step with `@ServiceConnection`), #8 (worth knowing before writing more container tests).

## Improvement 7 in plain English: a container you can also run the app with

Today the container lives in `MySqlContainerBase`, so only tests that extend it can use it. Boot offers a second place for it: a small `@TestConfiguration` class with a `@Bean` method that returns the container, marked `@ServiceConnection`.

- **Tests** use it with `@Import(TestcontainersConfiguration.class)` instead of extending a base class.
- **The app itself** can use it too, through a test-only main class:

  ```java
  public class TestSpring7RestMvcApplication {
      public static void main(String[] args) {
          SpringApplication.from(Spring7RestMvcApplication::main)
                  .with(TestcontainersConfiguration.class)
                  .run(args);
      }
  }
  ```

  Starting that class runs the real application against a fresh MySQL container, with Flyway and `BootstrapData` - no local MySQL on port 3307 and no `mysql-init.sql` needed.

This is what Spring Initializr generates for Boot 4.1 when Testcontainers is selected.

## Improvement 8 in plain English: data that survives between tests

`@DataJpaTest` rolls back what a test does, so each test normally starts clean. But `BootstrapData` runs **when the test context starts**, before and outside any test transaction, so its 3 beers and 3 customers are **committed** to the container.

Because the container is a singleton (one for the whole test run), those rows stay there for every later test class that uses it. Today that is fine - `MySqlTest` even relies on it. But a future test that expects empty tables, or exactly one beer, would fail for a reason that has nothing to do with its own code.

What to do when that day comes: make such tests independent of the seed data (e.g. assert on what they create themselves), or clean up the tables they need before they start.

## Improvement 9 in plain English: `version(1)` on a new customer

`BootstrapData` builds new customers like this:

```java
Customer.builder().name("Customer 1").version(1).build();
```

`version` is the entity's `@Version` field, used for optimistic locking. Spring Data decides whether `save()` must **insert** a new row or **update** an existing one by looking at that field: `null` means new, anything else means "already in the database". A brand-new customer with `version = 1` therefore looks like an existing one, and `save()` uses `merge()` instead of `persist()`.

It still works - Hibernate notices the missing id and inserts anyway - but only by accident. Removing `.version(1)` lets Hibernate set the version itself, as it already does for beers.
