# Repository guide

Guidance for Claude Code when working on this repository.

---

## 1. Project at a glance

A beer and customer REST API built alongside John Thompson's **Spring Framework 6: Beginner to Guru** course on Udemy.

| Area | Reference |
|---|---|
| Runtime | Java 25, Spring Boot 4.1.2, Spring Framework 7.0, Spring Security 7.1 |
| Persistence / JSON | Hibernate 7.4, Jackson 3.1 |
| Libraries | Spring Data JPA, MapStruct 1.6.3, Lombok, Flyway, OAuth2 resource server, springdoc-openapi 3.2.0 (JT: 2.0.2; issues #26, #13) |
| Root package | `guru.springframework.spring7restmvc` |
| Entry point | `Spring7RestMvcApplication` |
| Branches | One per lesson, named after JT's branch plus a suffix: this one is `127-springdoc-security-cfg-jt-sb412` (Chap375), created from `126-springdoc-maven-jt-sb412` (Chap374, itself from JT's `126-springdoc-maven`). Branches are **never merged into `master`**: each documents an improved variant of JT's lecture (`-sbNNN` = Boot version). 126 was upgraded to Boot 4.1.2 and merged into this branch (issue #13). |
| Commits | Prefix with section and chapter: `Sec40_Chap375: ...`; `-X` for own additions, `-NOK` when tests are knowingly left red. `#N` in a message refers to GitHub issue N. |
| Open work | GitHub issues of `PierreSQS/spring-7-rest-mvc` (label `sec40`); every `gh` command takes `-R PierreSQS/spring-7-rest-mvc`. |

### Documentation

| File | Content |
|---|---|
| `docs/procedures-tickets-et-ci.md` | How issues and the CI are handled on this branch (in French). |
| `docs/openapi-annotations-et-json.md` | Which OpenAPI annotation of `OpenApiConfig` produces which part of `/v3/api-docs`, and what Swagger UI does with it (in French). On branch **`notes`** (not here). |
| `docs/regles-a-retenir.md` | The project's rules (R1, R2, ...), in French. On branch **`notes`** (not here), moved from 125 (#38). |
| Lesson reviews | One snapshot per section, `docs/secNN[-chapNNN]-<topic>-improvements.md`, on branch **`notes`** (not here). Each names the lesson branch + commit it describes and is never updated afterwards (issue #20). |

**When a change contradicts something this guide states, correct it in the same commit.**
Older lesson branches carry older copies of this file; they describe their own code and are not back-ported.

---

## 2. Build, test and run

Use **JDK 25** (`JAVA_HOME`) and the Maven wrapper, which pins **Maven 3.9.16**. Commands below use Git Bash; in PowerShell replace `./mvnw` with `.\mvnw.cmd`.

| Task | Command | Docker |
|---|---|---|
| Unit tests | `./mvnw test` | No |
| One test class | `./mvnw test -Dtest=BeerControllerTest` | No |
| One test method | `./mvnw test -Dtest=BeerControllerTest#testPatchBeer` | No |
| Full verification | `./mvnw clean verify` | Yes |
| One integration test class, plus unit tests | `./mvnw verify -Dit.test=MySqlIT` | Yes |
| Build without integration tests | `./mvnw verify -DskipITs` | No |
| Run with Compose MySQL | `./mvnw spring-boot:run` (profile `localmysql` set in `pom.xml`) | Yes |
| Run with H2 | `./mvnw spring-boot:run -Dspring-boot.run.profiles=default` | No |

No linter or formatter is configured. Calling the running API needs a JWT from an authorization server on `http://localhost:9000` (see section 4).

**Before a commit, Pierrot runs the full suite in IntelliJ**; Claude may run single tests or classes.

**CI:** `.github/workflows/build.yml` runs `./mvnw -B verify` on every push, except pushes that change only `docs/**` or `*.md`. A green run on the pushed commit is the proof an issue is fixed. The manual trigger (`workflow_dispatch`) is commented out: GitHub only accepts it when the workflow is on the default branch, and `master` has none. To rerun, use "Re-run all jobs" on a run.

**Build info:** `spring-boot-maven-plugin` runs its `build-info` goal (`META-INF/build-info.properties`); `OpenApiConfig` reads it through `BuildProperties` (optional: `ObjectProvider`) and shows the build version as `info.x-build-version` of `/v3/api-docs` (#28) and, because Swagger UI ignores `x-` extensions, in `info.description` under the title (#42).

---

## 3. Architecture and implementation rules

Beer and Customer follow the same structure:

```text
controller -> services -> repositories -> entities
                  |
               mappers <-> model (DTOs)
```

This branch is a **training branch**: most of the code is still JT's. Pierrot improves it by hand, issue by issue.
Do not fix JT's code on the side: name the problem, propose an issue, let him decide.

| Package | Responsibility | Rules to preserve |
|---|---|---|
| `config` | `SpringSecConfig`: the security filter chain; `OpenApiConfig`: OpenAPI documentation (Bearer JWT scheme, #27; build version, #28) | See section 4. `OpenApiConfig` only documents: it opens or protects nothing. `info.version` = `v1`, the version of the API contract, hand-written on purpose; the build version goes into `info.x-build-version` and `info.description` (shown by Swagger UI, #42). |
| `controller` | REST endpoints, `CustomErrorController`, `NotFoundException` | Reuse the path constants (`BEER_PATH` = `/api/v1/beer`, `BEER_PATH_ID`, `CUSTOMER_PATH`, `CUSTOMER_PATH_ID`); tests reference them. `@Validated` on POST and PUT, **not** on PATCH: a PATCH body may be partial. Writes return `ResponseEntity<Void>`. |
| `model` | `BeerDTO`, `CustomerDTO`, `BeerStyle`, `BeerCSVRecord` | Keep the Bean Validation annotations (`BeerDTO`: `beerName`, `upc` `@NotBlank`; `beerStyle`, `price` `@NotNull`. `CustomerDTO`: `name` `@NotBlank`, `email` optional, `@Size(1..255)` when present) and the Jackson annotations (section 6). |
| `entities` | `Beer`, `Customer`, `Category`, `BeerOrder`, `BeerOrderLine`, `BeerOrderShipment` | `@UuidGenerator` + `@JdbcTypeCode(SqlTypes.CHAR)` for UUIDs stored as `varchar(36)`. `BeerStyle` as `SMALLINT`. A field initializer used with `@Builder` needs `@Builder.Default`. |
| `mappers` | MapStruct entity/DTO conversion | Spring beans through `-Amapstruct.defaultComponentModel=spring`. DTO -> entity ignores what belongs to the database (`id`, `version`, dates, collections; rule R1). Update and patch methods write into the loaded entity (`@MappingTarget`); patch skips `null` fields. |
| `repositories` | Spring Data JPA repositories | Plain `JpaRepository` interfaces. The three beer finders take a `Pageable` and return `Page<Beer>`. |
| `services` | Resource operations, CSV parsing | `BeerServiceJPA` and `CustomerServiceJPA` are the real implementations (`@Primary`). JT's map-based `BeerServiceImpl` / `CustomerServiceImpl` are still `@Service` beans (issue #8); `BeerControllerTest` builds its sample data with `new BeerServiceImpl()`. `BeerCsvServiceImpl` parses `csvdata/beers.csv`. |
| `bootstrap` | `BootstrapData` (`CommandLineRunner`, `@Transactional`) | Seeds 3 beers when the table is empty, then every row of `csvdata/beers.csv` when it holds fewer than 10 (**2413 beers**), and 3 customers when that table is empty. |

**Paging and sorting** (`BeerServiceJPA.buildPageRequest`): the API counts pages from **1**, Spring Data from 0. Without parameters: page 1 with **25** beers; `pageSize` is capped at **1000**; a missing or `< 1` `pageNumber` gives page 1. Every page is sorted by `beerName` ascending.

**Persistence:** keep `@Version` (`Integer`). `@CreationTimestamp` / `@UpdateTimestamp` fill `createdDate` / `updateDate`; never set them by hand. All collections are lazy (JPA default). `Beer.categories` owns the join table `beer_category`; `Category.beers` is its `mappedBy` mirror. Every table uses `created_date`, `update_date` and `version integer` (`V6`, `V7`). `spring.jpa.open-in-view` is not configured, so Boot's default (`true`) applies (issue #10).

**Annotation processing:** Lombok, lombok-mapstruct-binding and mapstruct-processor are registered on `maven-compiler-plugin`. MapStruct's version (1.6.3) is pinned because Boot does not manage it.

### HTTP behavior

| Outcome | Response |
|---|---|
| No or invalid Bearer token | `401` for `GET`; `403` for writes (CSRF only ignored with a Bearer token) |
| springdoc URLs (`/v3/api-docs`, `/swagger-ui/index.html`) | `200` without a token |
| `GET` on the beer list | a serialized Spring Data `Page` |
| `GET` on the customer list | a plain JSON array |
| Successful POST | `201` with a `Location` header |
| Successful PUT / PATCH / DELETE | `204` |
| Missing entity | `NotFoundException` (`@ResponseStatus(404)`) |
| Invalid request DTO (POST, PUT) | `MethodArgumentNotValidException` -> `400` with a list of `{field: message}` |
| Validation failure at commit (PATCH) | `TransactionSystemException` whose root cause is a `ConstraintViolationException` -> same `400` list; any other cause -> plain `400` |

---

## 4. Security

The API is an **OAuth2 resource server** (`spring-boot-starter-security-oauth2-resource-server`). `SpringSecConfig` (`@EnableWebSecurity`) leaves the springdoc URLs open (Chap375) and requires authentication on every other request, validating JWTs:

```java
http.authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
        .anyRequest().authenticated())
    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

Rules are read top to bottom, first match wins: `anyRequest()` stays last.

| Detail | Behavior |
|---|---|
| `.jwt(...)` is mandatory | `oauth2ResourceServer(withDefaults())` alone fails at startup: *Jwt and Opaque Token are the only supported formats*. |
| Issuer | `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000`. The `JwtDecoder` is created lazily, so the application and the tests start even when the authorization server is down. |
| CSRF | Ignored by the resource-server configurer for requests with a Bearer token; no `csrf(...)` configuration. |
| No security user | No `spring.security.user.*` properties. |
| Tests | Every `MockMvc` request carries `.with(BeerControllerTest.jwtRequestPostProcessor)`, a shared bare `jwt()` (`SecurityMockMvcRequestPostProcessors`). No claims or scopes are needed: the chain only checks `authenticated()`. `spring-security-test` comes through `spring-boot-starter-security-test` (test scope). |

---

## 5. Topic rules in `.claude/rules/`

Two topics live in their own file and load only when Claude works on matching files. **Read them before answering about their topic**, even when no such file is open:

| File | Topic | Loaded for |
|---|---|---|
| `.claude/rules/database.md` | H2 / Compose MySQL / `MySqlIT`, Compose lifecycle, migration rule | `compose.yaml`, `.env.example`, `src/main/resources/**`, `MySqlIT.java` |
| `.claude/rules/testing.md` | test classes and their setup, seeded data, flush/clear, `MySqlIT` | `src/test/**` |

### Assertions (always loaded)

**Always use AssertJ `assertThat`. Never use JUnit's `assertEquals` / `assertTrue` / `assertThrows`.**
Exceptions are asserted with `assertThatThrownBy(...)` (or `assertThatExceptionOfType(...)`),
**including the message**, not only the type:

```java
assertThatThrownBy(() -> beerService.deleteById(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("...");
```

Inside MockMvc, `jsonPath(..., is(...))` (Hamcrest) is MockMvc's own matcher API and stays:
MockMvc is the standard of this project. Issue #25 evaluates `MockMvcTester` on one example;
Pierrot then decides to stay with MockMvc or to replace it.
The remaining `assertThrows` in `BeerControllerIT` / `CustomerControllerIT` belong to the
direct controller calls of issue #11 and disappear with it.

---

## 6. Gotchas

Traps of this stack (Boot 4.1 / Spring 7 / Hibernate 7.4 / Jackson 3 / Java 25). Check here before assuming an API behaves as in earlier versions.

| Topic | Rule |
|---|---|
| JSON | **Use `JsonMapper` (`tools.jackson.databind.json.JsonMapper`), never `ObjectMapper`.** The tests moved off it; do not reintroduce it. Jackson 3 lives in `tools.jackson.*`, but its annotations (`@JsonProperty`, ...) keep the package `com.fasterxml.jackson.annotation`. |
| Jackson and Lombok | `BeerDTO` and `CustomerDTO` are deserialized **through the Lombok builder**: `@JsonDeserialize(builder = XDTO.XDTOBuilder.class)` plus `@JsonProperty` on every field (Lombok copies it onto the builder methods). A new DTO field needs its `@JsonProperty` too. |
| Mockito `@Captor` | Boot 4 no longer initializes `@Captor` fields: the `@WebMvcTest` classes using captors carry `@ExtendWith(MockitoExtension.class)`. |
| `@Rollback` | Redundant: Spring rolls back `@Transactional` tests by default. Do not add it. |
| Dirty checking | A managed entity that is modified is written at flush, without any `save()` (rule R22). Change DTOs, not entities, when the change must not reach the database (issue #22). |
| `@WebMvcTest` placeholders | The slice has no `PropertyPlaceholderAutoConfiguration`: an unresolvable `@Value("${...}")` silently keeps the raw text, where `@SpringBootTest` fails. A green `@WebMvcTest` proves nothing about properties (rule R11). |
| `StringUtils.hasText` | Not the same as `s != null && !s.isEmpty()`: `hasText` also refuses `"   "`. IDE assistants have silently swapped one for the other - check the diff. |
| Test support modules | Boot 4 splits them (`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-validation-test`), so `@WebMvcTest` and `@DataJpaTest` live under `org.springframework.boot.<module>.test.autoconfigure` - let the IDE resolve them. |
| Plugin versions | Managed by the Boot parent (no version in `pom.xml`). Surefire and Failsafe start the JVM with Mockito as a Java agent (`argLine`, `-Xshare:off`): no "Mockito is currently self-attaching" warning (issue #13). |
| devtools and H2 | On stop, devtools finds the in-memory H2 already closed (issue #18); the `localmysql` driver line exists for the opposite trap (#16, `.claude/rules/database.md`). |
| Container credentials | `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE` only apply to an **empty** volume (`.claude/rules/database.md`). |
