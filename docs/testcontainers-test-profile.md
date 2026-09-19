# The `testcontainers` test profile

Tests that run against a real MySQL container (today: `MySqlIT`) use a small, test-only Spring profile called `testcontainers`. This page explains what it is, who switches it on, and why it is needed.

## 1. What is a profile?

A profile is a **named set of extra settings**. Spring Boot always reads `application.properties`. If a profile called `xyz` is active, it **also** reads `application-xyz.properties`, and where both files set the same property, **the profile's value wins**.

So `testcontainers` is just a name, and `application-testcontainers.properties` is the file that belongs to it.

## 2. Where is it, and who switches it on?

- **Where:** `src/test/resources/application-testcontainers.properties`. It is test-only, so it is never part of the real application.
- **Who:** `MySqlContainerBase` carries `@ActiveProfiles("testcontainers")`. Every test that extends it (today `MySqlIT`) runs with the profile on; all other tests don't.

## 3. What is inside? Only two lines

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

## 4. Why these two lines are needed

The normal `application.properties` says:

```properties
spring.flyway.enabled=false
```

Flyway is **off** by default, because the default setup uses H2, where Hibernate creates the tables itself.

**Without the profile**, a container test goes like this:

1. The MySQL container starts - completely **empty**, no tables.
2. Flyway is off, so **no migrations run**.
3. No tables are created, and the test fails.

**With the profile:**

1. The container starts, still empty.
2. `spring.flyway.enabled=true` **overrides** the `false`, so Flyway runs `V1` and `V2` and **creates the tables**.
3. `ddl-auto=validate` makes Hibernate **check** that those tables match the entities - for example, that `id` really is `varchar(36)`.
4. The test runs against a real, correctly built MySQL database.

### Proof (2026-09-19)

`MySqlIT` was run once with `@ActiveProfiles("testcontainers")` temporarily commented out:

- Spring reported `No active profile set, falling back to 1 default profile: "default"`.
- Flyway did **not** run.
- `BootstrapData` failed on its first query with `Table 'test.beer' doesn't exist`, and the build ended in **BUILD FAILURE**.

With the profile active, the same test passes and the log shows `Successfully applied 2 migrations`.

## 5. What is NOT inside - and why

There is no `spring.datasource.url`, `username` or `password`. The container starts on a **random port** with its **own** user, so nobody can write those values in advance. `@ServiceConnection` on the container in `MySqlContainerBase` hands them to Spring automatically.

## 6. Why a separate profile and not `localmysql`?

`localmysql` is for running the application on **this PC's own MySQL**: it holds port `3307`, the `restadmin` user and very noisy SQL/bind-value logging. A container test needs none of that - only "Flyway on, check the tables". Borrowing `localmysql` also meant that changing it for local work would silently change the tests.

JT's course code uses `@ActiveProfiles("localmysql")` in the test; the switch to a dedicated profile was improvement #10 in Sec13_Chap142 - see `docs/78.3-using-dynamic-properties-improvements.md` for that discussion.

## In one sentence

The `testcontainers` profile tells the MySQL container tests: *"build the tables with Flyway and check that they match the entities"* - while the container itself tells Spring where the database is and how to log in.
