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

## MySQL integration test

- `MySqlIT` declares its own `static` container with `@Container` and `@ServiceConnection`; it runs Flyway and `BootstrapData`.
- **Missing Docker must fail this test, not skip it.** Without Docker, use `test` or `-DskipITs`.
- Compose is not involved in tests (`spring.docker.compose.skip.in-tests=true` by default).
