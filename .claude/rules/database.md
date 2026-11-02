---
paths:
  - "compose.yaml"
  - ".env.example"
  - "src/main/resources/**/*"
  - "src/test/**/MySqlIT.java"
---

# Database environments and profiles

Loaded with `CLAUDE.md` when Claude reads or edits one of the files above. Moved out of `CLAUDE.md` (issue #17) to keep it under 200 lines.

| Environment | Database / connection | Schema management | Configuration |
|---|---|---|---|
| Default application and H2 tests | In-memory H2; Compose disabled (`spring.docker.compose.enabled=false`, valid for every profile that does not override it) | Hibernate generates the schema; Flyway off | `application.properties` |
| `localmysql` with Compose | `mysql:9.5`, host port **3308**, container `mysql-spring7-rest-sec22-assn25`, database `jt_spring7_rest_sec22_assn25_db` | Flyway on (`V1` to `V7`); `ddl-auto=validate` | `application-localmysql.properties` (port **8081**, Compose on) and `compose.yaml` |
| `MySqlIT` (tests only) | Its own Testcontainers container `mysql:9.2` (JT's image, not the Compose one; issue #24), connection from `@ServiceConnection`; `localmysql` profile | Flyway on; `ddl-auto=validate` | `repositories/MySqlIT.java` |

`localmysql` declares **no** `spring.datasource.url`, user or password: Compose (or `@ServiceConnection`) supplies them. It keeps `spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver` on purpose: without it, devtools takes MySQL for an in-memory H2 and sends it `SHUTDOWN` on stop (#16). It also names the Hikari pool `RestDB-Pool` (max 5).

**Local debug toggles** (SQL logging, security trace, ...) go into the Git-ignored `config/application*.properties`, which Boot reads on top of `src/main/resources`. Never commit them.

## Compose setup and lifecycle

1. Copy `.env.example` to `.env` beside `compose.yaml` (Git-ignored).
2. Fill in `MYSQL_USER`, `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD`; Compose refuses to start without them.
3. Start with the `localmysql` command of section 2 of `CLAUDE.md`. Boot waits for the container to be healthy.

| Detail | Behavior |
|---|---|
| Names | Compose project `spring-7-rest-mvc-jt-sb411-sec22-assn25`, container and named volume `mysql-spring7-rest-sec22-assn25(-data)`: they name the lecture whose migration created the schema (`V7`, Section 22 Assignment 25). Same database as branch 125. Rename project, container, volume and database together, and only with a new Flyway migration. When a container of the same project is already running, Boot connects to it without `docker compose up`. |
| Shutdown | `spring.docker.compose.stop.command=stop`: the container and its volume survive, the next start reuses the database and Flyway only validates (rule R25). A fresh database needs `docker compose down -v` by hand. |
| Forced termination | No shutdown hook; the container keeps running. |
| Changed credentials or database name | MySQL only applies them to an **empty** volume: `docker compose down -v` first (deletes the data), otherwise `Access denied`. |
| Port 3308 | Shared with containers of other lessons; only one can run at a time. |

## Migration rule

**Add a new migration whenever an entity column changes; never edit an applied migration.** Files: `src/main/resources/db/migration/V<n>__description.sql`. H2 can start with a schema out of sync; `localmysql` validates it and fails.
