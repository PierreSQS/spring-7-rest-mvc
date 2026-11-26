---
paths:
  - "src/test/**/*"
---

# Testing conventions

Loaded with `CLAUDE.md` when Claude reads or edits a file under `src/test/`. The AssertJ rule stays in `CLAUDE.md` (section 5), so it is always loaded.

## Test selection

| Test pattern | Setup | Runner | Docker |
|---|---|---|---|
| `*ControllerTest` | `@WebMvcTest(XController.class)` + `@Import(SpringSecConfig.class)`, `@MockitoBean` services, `@ExtendWith(MockitoExtension.class)` for the `@Captor` fields, `.with(jwtRequestPostProcessor)`. **A `POST` or `PUT` body carries no `id`, `version`, `createdDate` or `updateDate`** (the database and Hibernate own them, issue #53): a private `beerRequest(name)` / `customerRequest(name)` builds it, while the `@BeforeEach` DTOs keep those fields and play the answer of the mocked service. | Surefire (`test`) | No |
| `*RepositoryTest`, `BootstrapDataTest` | `@DataJpaTest` | Surefire | No |
| `BeerCsvServiceImplTest` | plain unit test | Surefire | No |
| `*ControllerIT` | `@SpringBootTest` + `@AutoConfigureMockMvc`, H2. **Every test goes through `MockMvc`** with `.with(BeerControllerTest.jwtRequestPostProcessor)` (the 18 direct controller calls were rewritten, issue #11): a missing entity expects `404`, not an exception, and a `PUT`/`POST` body must be valid or the answer is `400`. Data-changing tests are `@Transactional` and call `entityManager.flush()` + `clear()` before reading the row back (R6). | Failsafe (`verify`) | No |
| `Spring7RestMvcApplicationTests` | `@SpringBootTest` context check | Surefire | No |
| `MySqlIT` | `@SpringBootTest` + `@Testcontainers` + `@ActiveProfiles("localmysql")` | Failsafe | Yes |

Surefire selects `*Test` / `*Tests`; Failsafe selects `*IT`.

**Seeded data is part of the tests.** `BootstrapDataTest` expects 2413 beers; the search counts are fixed
constants (336 beers named `IPA`, 572 of style `IPA`, 324 both). A change to `beers.csv` or
`BootstrapData` makes these tests fail on purpose.

**Read back from the database, not from Hibernate's cache:** in a `@Transactional` test, call
`entityManager.flush()` and `clear()` before re-reading what was written (rules R6, R9).
**No `@Rollback`:** Spring rolls back `@Transactional` tests by default.

## Lessons learned in this project

Each one comes from a real mistake or review on an earlier branch.

| Topic | Rule |
|---|---|
| Validation tests | A test expecting `400` from `@Validated` on the `@RequestBody` needs **no `given(...)`**: the body is rejected before the handler runs, so the mocked service is never called. Mockito's strict stubs do not catch it (they only police `@Mock`, not Spring's `@MockitoBean`). Leave a short comment saying why there is no stubbing. |
| `JsonMapper` import | Import `tools.jackson.databind.json.JsonMapper`. IntelliJ may offer Testcontainers' shaded copy `org.testcontainers.shaded.com.fasterxml.jackson.databind.json.JsonMapper`: no Spring bean has that type. |
| Changing a response | Before predicting green after changing a status or a body (error handler, ...), search the tests that assert the **old** response (`content().string("")`, the old status), not only the new case (issue #9: predicted green, CI red). |
| Helper names | Test helpers take the `get` prefix (`getFirstBeer()`, not `firstBeer()`). |
| Status constants | `HttpStatus.NO_CONTENT` / `HttpStatus.CREATED`, not `HttpStatusCode.valueOf(204)`. |
| Lambdas | Extract a local before the lambda: `UUID beerId = UUID.randomUUID(); assertThatThrownBy(() -> beerService.deleteById(beerId))`. |
| `BeerControllerIT.testPatchBeerBadName` | `patchBeerById` has no `@Validated`, so the too-long name only fails at the JPA commit (`TransactionSystemException` -> `400`). The test cannot move to `@WebMvcTest`, and `BeerControllerIT` must not get a class-level `@Transactional` (the commit would never happen). |
| `id` / `version` in an update DTO | The update and patch mappers ignore them (rule R1), so setting them to `null` in a test is dead code. |

## MySQL integration test

- `MySqlIT` declares its own `static` container with `@Container` and `@ServiceConnection`; it runs Flyway and `BootstrapData`.
- **Missing Docker must fail this test, not skip it.** Without Docker, use `test` or `-DskipITs`.
- Compose is not involved in tests (`spring.docker.compose.skip.in-tests=true` by default).
