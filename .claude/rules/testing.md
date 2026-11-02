---
paths:
  - "src/test/**/*"
---

# Testing conventions

Loaded with `CLAUDE.md` when Claude reads or edits a file under `src/test/`. The AssertJ rule stays in `CLAUDE.md` (section 5), so it is always loaded.

## Test selection

| Test pattern | Setup | Runner | Docker |
|---|---|---|---|
| `*ControllerTest` | `@WebMvcTest(XController.class)` + `@Import(SpringSecConfig.class)`, `@MockitoBean` services, `@ExtendWith(MockitoExtension.class)` for the `@Captor` fields, `.with(jwtRequestPostProcessor)` | Surefire (`test`) | No |
| `*RepositoryTest`, `BootstrapDataTest` | `@DataJpaTest` | Surefire | No |
| `BeerCsvServiceImplTest` | plain unit test | Surefire | No |
| `*ControllerIT` | `@SpringBootTest` + `@AutoConfigureMockMvc`, H2. Data-changing tests are `@Transactional`. **18 tests still call the controller methods directly** (issue #11): such a call skips security, `@Validated`, `CustomErrorController` and the JSON conversion. New tests go through `MockMvc` with `.with(jwtRequestPostProcessor)`; a missing entity expects `404`, not an exception. | Failsafe (`verify`) | No |
| `Spring7RestMvcApplicationTests` | `@SpringBootTest` context check | Surefire | No |
| `MySqlIT` | `@SpringBootTest` + `@Testcontainers` + `@ActiveProfiles("localmysql")` | Failsafe | Yes |

Surefire selects `*Test` / `*Tests`; Failsafe selects `*IT`.

**Seeded data is part of the tests.** `BootstrapDataTest` expects 2413 beers; the search counts are fixed
constants (336 beers named `IPA`, 572 of style `IPA`, 324 both). A change to `beers.csv` or
`BootstrapData` makes these tests fail on purpose.

**Read back from the database, not from Hibernate's cache:** in a `@Transactional` test, call
`entityManager.flush()` and `clear()` before re-reading what was written (rules R6, R9).
**No `@Rollback`:** Spring rolls back `@Transactional` tests by default.

## MySQL integration test

- `MySqlIT` declares its own `static` container with `@Container` and `@ServiceConnection`; it runs Flyway and `BootstrapData`.
- **Missing Docker must fail this test, not skip it.** Without Docker, use `test` or `-DskipITs`.
- Compose is not involved in tests (`spring.docker.compose.skip.in-tests=true` by default).
