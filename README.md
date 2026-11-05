# My notes - index

This branch, `notes`, holds my **personal notes**: what I learned, written with Claude Code,
while following John Thompson's
[Spring Framework 7 - Beginner to Guru](https://www.udemy.com/course/spring-framework-6-beginner-to-guru/) course on Udemy.
The code they talk about is the course application of this repository, a beer and customer REST API.
This is not the documentation of that application.

`notes` is the **single source of truth** of these notes: when a note exists in several places,
the version on `notes` is the right one.

The branch `notes` holds documents only: no code, no CI, never merged.
JT's original README stays on every lesson branch.

## Words used in this index

| Word | Meaning |
|---|---|
| **lesson branch** | One branch per lecture of the course, named after JT's branch for that lecture plus a suffix, e.g. `127-springdoc-security-cfg-jt-sb412`. Never merged into `master`. The number at the start (here 127) is **JT's branch index**, not a chapter number. "Branch 127" is short for that branch. |
| **`-sbNNN` suffix** | The Spring Boot version of the branch: `-sb411` = Boot 4.1.1, `-sb412` = Boot 4.1.2. |
| **my own branches** ("mes branches") | The chain where my own improvements live, each branch started from my previous one. The last one is branch 109, `109-lock-demo-optimistic-sb411` (Sections 13-18). It will be brought up to the current lecture later (#34). |
| **training branch** | A lesson branch with `-jt-` in its name: it **restarted from JT's original code**, and I improve that code by hand, issue by issue. Two chains: branches 110 to 125 (Sections 22-38, restarted from JT's branch 110) and branches 126 to 127 (Section 40, restarted from JT's branch 126). |
| **frozen copy** | Some documents of `notes` also exist, in an older version, on lesson branches: `docs/regles-a-retenir.md` is on branches 116, 121 and 125. Those copies show the state of their branch and are **never updated**: the version on `notes` is the right one. |
| **SHA** | The identifier of a commit, e.g. `a79dc240`. A link with a SHA opens a file exactly as it was at that commit, even if the branch changes later. |

Links to a document of `notes` open its latest version. Links to a document of a lesson branch open it at a fixed commit (SHA).

---

## Start here

| Document | Why |
|---|---|
| [`docs/regles-a-retenir.md`](docs/regles-a-retenir.md) | Every rule learned in the project (R1-R27), with example and proof. |
| [`docs/procedures-tickets-et-ci.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/a79dc24018e91efece595ab2e84857186ea71700/docs/procedures-tickets-et-ci.md) (branch 127) | How issues and the CI work, from an issue to its closing. |
| [`CLAUDE.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/a79dc24018e91efece595ab2e84857186ea71700/CLAUDE.md) (branch 127) | What the code of the current lesson branch does, and the rules Claude follows on it. |
| [Open issues](https://github.com/PierreSQS/spring-7-rest-mvc/issues) | What is left to do. |

---

## On this branch (`notes`)

| Document | What it is for | When to read it |
|---|---|---|
| [`docs/regles-a-retenir.md`](docs/regles-a-retenir.md) | The rules R1-R27 (MapStruct, PUT/PATCH, Hibernate, lambdas, database, Spring Security...), each with the rule, an example and the proof (test or commit). Moved here from branch 125, `125-build-info-jt-sb411` (#38); frozen copies on branches 116, 121 and 125. French. | Before writing similar code; when a rule number (R19, R22...) is quoted in an issue or a commit. |
| [`docs/openapi-annotations-et-json.md`](docs/openapi-annotations-et-json.md) | Which annotation of `OpenApiConfig` produces which part of `/v3/api-docs`, and what Swagger UI does with it (Section 40, #27, #31). French. | When Swagger UI shows, or does not show, something unexpected. |
| [`docs/travailler-avec-claude-code.md`](docs/travailler-avec-claude-code.md) | Working with Claude Code: why a long conversation costs more, `/clear`, `/resume`, `/rename`... French. | When a conversation gets long or expensive, or before starting a new one. |
| [`docs/postman-avec-claude-code.md`](docs/postman-avec-claude-code.md) | Exploring Postman with Claude Code, step by step: how Claude reaches my Postman collection (CLI, plugin, access key, commands), then each step with goal, command, result and what it proves (#39). French. | Before working with Postman from the CLI, or to redo the steps alone. |

---

## On the current lesson branch: branch 127, `127-springdoc-security-cfg-jt-sb412` (`a79dc240`)

Training branch, Section 40 (OpenAPI). These documents describe the code of their branch, so they stay there.
Every lesson branch has its own version of `CLAUDE.md`, describing its own code.

| Document | What it is for | When to read it |
|---|---|---|
| [`CLAUDE.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/a79dc24018e91efece595ab2e84857186ea71700/CLAUDE.md) | Guide of the branch: stack, build and test commands, architecture, security, gotchas of Boot 4.1 / Spring 7 / Jackson 3. | To know how the branch is built, tested and organised. |
| [`.claude/rules/database.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/a79dc24018e91efece595ab2e84857186ea71700/.claude/rules/database.md) | H2, Compose MySQL and `MySqlIT`: profiles, Compose lifecycle, migration rule (#17). | Before touching `compose.yaml`, `src/main/resources` or a Flyway migration. |
| [`.claude/rules/testing.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/a79dc24018e91efece595ab2e84857186ea71700/.claude/rules/testing.md) | Test classes and their setup, seeded data, flush/clear, `MySqlIT` (#17). | Before writing or changing a test. |
| [`docs/procedures-tickets-et-ci.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/a79dc24018e91efece595ab2e84857186ea71700/docs/procedures-tickets-et-ci.md) | Issues (content, labels, closing) and the CI pipeline; traps already met (broken `mvnw`, run stuck in *Queued*). French. | When an issue is created or closed, or when the CI behaves strangely. |

---

## On branch 125, `125-build-info-jt-sb411` (`90a2fee5`): Spring Security, Section 22

Training branch, last branch of Section 38. The three documents below were written in Section 22.
Branch 116 (`116-security-fix-remaining-tests-jt-sb411`) and branch 121 (`121-resource-server-refacto-jwt-jt-sb411`)
hold the same three files, identical to the ones on branch 125.

| Document | What it is for | When to read it |
|---|---|---|
| [`docs/jt-branches-section22-security.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/90a2fee5da52b9403660632f4733adcd007c647b/docs/jt-branches-section22-security.md) | Which of JT's branches belongs to which lesson of Section 22, and the traps (two branches numbered 115...). | When comparing with JT's Section 22 branches. |
| [`docs/spring-security-enable-web-security.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/90a2fee5da52b9403660632f4733adcd007c647b/docs/spring-security-enable-web-security.md) | What `@EnableWebSecurity` does, when it is needed (not with Spring Boot), and `debug = true`. | When writing a security configuration. |
| [`docs/spring-security-test-user-vs-httpbasic.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/90a2fee5da52b9403660632f4733adcd007c647b/docs/spring-security-test-user-vs-httpbasic.md) | `user(...)` (mock user, nothing checked) versus `httpBasic(...)` (real login) in a MockMvc test. | When a secured MockMvc test passes or fails for an unclear reason. |

---

## On branch 109, `109-lock-demo-optimistic-sb411` (`69f0a32b`): Sections 13-18

The last of my own branches ("mes branches"). The documents keep their original language (German or English).

### Topic documents (12)

| Document | What it is for | When to read it |
|---|---|---|
| [`docs/relationship-owner-and-mirror.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/relationship-owner-and-mirror.md) | How to tell the owner and the mirror (`mappedBy`) of a JPA relationship (Section 17, Chap177). German. | When mapping a relationship. |
| [`docs/java-and-database-two-worlds.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/java-and-database-two-worlds.md) | Java objects and database rows are two worlds; why the helper method helps on the Java side (Section 17, Chap180). German. | Together with the document above. |
| [`docs/bidirectional-helper-methods.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/bidirectional-helper-methods.md) | The helper method `setCustomer(...)`: why it sets both sides of a relationship (Section 17, Chap176). German. | When a bidirectional relationship is out of sync. |
| [`docs/lazy-collections-and-flush.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/lazy-collections-and-flush.md) | Why the customer does (not) know the new order: lazy lists, `save` versus `saveAndFlush` (Section 17, Chap175). German. | When a test does not see data that was just saved. |
| [`docs/jt-code-pitfalls.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/jt-code-pitfalls.md) | Errors and weaknesses in JT's code of Section 17, each proven by a test or the SQL log. German. | Before copying JT's code for relationships or `equals`/`hashCode`. |
| [`docs/builder-constructor-parameter-order.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/builder-constructor-parameter-order.md) | `@Builder` with a hand-written constructor: its parameters must follow the field order (Section 17, Chap179). German. | When adding `@Builder` to a class with its own constructor. |
| [`docs/entity-states-and-optimistic-locking.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/entity-states-and-optimistic-locking.md) | Loaded, built and detached objects, and why `setVersion(...)` protects nothing (Section 18, Chap186). German. | Before the real optimistic locking work (409 Conflict). |
| [`docs/service-layer-boundaries.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/service-layer-boundaries.md) | Three rules of the service layer: what leaves (DTOs, not entities), what enters (validated where the request arrives), how it writes (one transaction). English. | When adding a service method. |
| [`docs/uuid-id-generation.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/uuid-id-generation.md) | Who generates the entity IDs: the application (`@UuidGenerator`), never the database. English. | When an ID is missing or `null`. |
| [`docs/testcontainers-test-profile.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/testcontainers-test-profile.md) | The test-only `testcontainers` profile: what a profile is, who switches it on, why `MySqlIT` needs it. English. | When working on `MySqlIT`. |
| [`docs/mockito-agent-and-class-sharing.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/mockito-agent-and-class-sharing.md) | Why the tests start the JVM with `-javaagent` (Mockito) and `-Xshare:off`. English. | When the Surefire/Failsafe `argLine` is touched. |
| [`docs/reading-classpath-files-from-a-jar.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/reading-classpath-files-from-a-jar.md) | Why a classpath file works in the IDE but not from a jar: read it as a stream, never as a `File` (Section 14, `beers.csv`). English. | When reading a resource file. |

### Lesson reviews (5)

Snapshots: each one reviews a lesson as the code stood then and is never updated (#20). English.

| Document | Lesson |
|---|---|
| [`docs/sec13-chap142-dynamic-properties-improvements.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec13-chap142-dynamic-properties-improvements.md) | Section 13, Chap142: Testcontainers with `@DynamicPropertySource`. |
| [`docs/sec13-chap143-service-connection-improvements.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec13-chap143-service-connection-improvements.md) | Section 13, Chap143: `@ServiceConnection`. |
| [`docs/sec13-chap144-maven-failsafe-improvements.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec13-chap144-maven-failsafe-improvements.md) | Section 13, Chap144: the Failsafe plugin, `MySqlTest` renamed `MySqlIT`. |
| [`docs/sec14-csv-improvements.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec14-csv-improvements.md) | Section 14, Chap147-153: CSV import. |
| [`docs/sec16-paging-improvements.md`](https://github.com/PierreSQS/spring-7-rest-mvc/blob/69f0a32bf8d3106c74140b1bc49e9c5c8dc4a7c9/docs/sec16-paging-improvements.md) | Section 16, Chap166-169: paging and sorting. |

No lesson review exists yet for Section 17 and later (#20).

---

## On GitHub

| What | What it is for |
|---|---|
| [Issues](https://github.com/PierreSQS/spring-7-rest-mvc/issues?q=is%3Aissue) | The task list: every bug, optimization and document has one, with its "Done when". A closed issue names the branch and commit that did the work. |
| Commit comments | The *why* of a change, in Markdown, under the commit on GitHub (open a commit and scroll down). |
| [CI runs](https://github.com/PierreSQS/spring-7-rest-mvc/actions) | The proof that a commit builds and its tests pass (`./mvnw -B verify`). |

---

## Where does a new document go?

| The document... | Goes to |
|---|---|
| does not depend on the code of one branch (rules, explanations, lesson reviews, working with Claude Code) | **`notes`**, plus a line in this README |
| describes the code of a branch (`CLAUDE.md`, `.claude/rules/`, the CI procedure) | **that lesson branch**, plus a line in this README |

A lesson branch never gets a copy of a document of `notes`. When it needs one, its `CLAUDE.md` gets a pointer instead:
one line saying "on branch `notes`" (example: the line for `docs/regles-a-retenir.md` in `CLAUDE.md` of branch 127).
