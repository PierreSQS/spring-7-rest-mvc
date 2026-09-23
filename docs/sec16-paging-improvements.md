# Section 16 (Paging) - improvements

Written during the paging lesson, **Section 16, chapters 166-168** (JT's branches `94-page-add-parameters`
to `96-refactor-spring-data-methods`). The lesson was worked in the TDD order JT uses: the failing test
first, then the parameters, then the page request, then the paged queries.

All numbers below come from the seeded data: **2413 beers**, of which **336** carry `IPA` in their name,
**572** are of style `IPA`, and **324** are both.

Status legend: ✅ done · 💡 suggestion, not applied · ⏭ later lecture

---

## 1. Applied in `BeerServiceJPA`

| # | Improvement | Why | Status |
|---|---|---|---|
| 1 | `MAX_PAGE_SIZE` constant instead of the literal `1000`, used twice | The number said nothing about itself, and the cap exists for a reason worth writing down: a client must not pull the whole table in one request | ✅ |
| 2 | `Math.min` and two conditional expressions instead of nested `if/else` | 22 lines became 8, with the same behaviour | ✅ |
| 3 | A page size below 1 falls back to the default | **Bug fix.** `AbstractPageRequest` throws `Page size must not be less than one`, so `pageSize=0` or `pageSize=-5` answered **500** instead of serving a page. Verified in the spring-data-commons sources | ✅ |
| 4 | `buildPageRequest` package-private instead of `public` | Only the class and its test use it | ✅ |
| 5 | Constants above the injected fields | Convention | ✅ |

`BeerServiceJPATest` (plain JUnit, no Spring context) pins the nine cases that matter: page 1 becomes
page 0, page 2 becomes page 1, a page number or size below 1 falls back to the default, 5000 is capped
at 1000, and no parameters at all means page 0 with 25 rows.

## 2. Applied in the tests

| # | Improvement | Why | Status |
|---|---|---|---|
| 6 | The list tests assert the **content**, not only the size | After paging, every list test expected `25`, which is simply the default page size. **All five would have passed with a completely broken search.** They now check that every beer on the page matches the search: `$..beerName` contains `IPA`, `$..beerStyle` equals `IPA` | ✅ |
| 7 | `DEFAULT_PAGE_SIZE` constant in `BeerControllerIT` | `25` appeared seven times as a bare number. It is not a number the tests chose - it is `BeerServiceJPA.DEFAULT_PAGE_SIZE`, and the constant says so. `PAGE_SIZE = 50` stays for the test that pages on purpose | ✅ |
| 8 | `BeerRepositoryTest` asserts `firstPage.getPageSize()` | The test compared a paged result with an unpaged count: the query filters then takes 25, the count took 25 then filtered - different questions, no reason to be equal. A variable `long expected = 25;` said nothing either | ✅ |
| 9 | Javadoc of the TDD test rewritten | It still described a feature that did not exist yet and quoted a count that had become wrong | ✅ |

### The trap behind #6

`showInventory=false` does not remove `quantityOnHand` from the JSON, it sets it to `null`. So
`jsonPath("$..quantityOnHand").doesNotExist()` fails with `found: [null, null, ...]`; the correct
assertion is `everyItem(nullValue())`. Found by running the tests, not by reading the code.

---

## 3. Applied - the return type

| # | Improvement | Why | Status |
|---|---|---|---|
| 10 | `Page<Beer>` / `Page<BeerDTO>` instead of `List` along the whole chain | A `List` carries the rows of one page and nothing else. The client could not learn that there are 336 matches over 14 pages, so it could neither render "page 2 of 14" nor decide whether a next page exists. A `Page` carries `content`, `totalElements`, `totalPages`, `first`, `last` - Spring Data runs one extra counting query for it | ✅ |

This is also where JT's lecture ends (`96-refactor-spring-data-methods`): repository, service and
controller all return `Page`.

What it brought:

- **`BeerServiceJPA` maps in one call** - `beerPage.map(beerMapper::beerToBeerDto)` instead of a stream
  and `toList()`, and the totals survive the mapping.
- **The totals are asserted again.** `BEERS_NAMED_IPA` (336), `BEERS_OF_STYLE_IPA` (572) and
  `BEERS_NAMED_IPA_OF_STYLE_IPA` (324) came back as `$.totalElements` assertions, so every search test
  proves its whole result set and not just the size of one page.
- **`BeerRepositoryTest` proves both** at once: `getContent()` holds 25 rows, `getTotalElements()` is 336.
- **The three finder helpers of the service are `private`** now; two of them used to be `public`.

The price is a **breaking change of the API**: the answer is no longer a bare array but an object with
`content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`. Every JSON path in the tests
moved under `content`, and `BeerServiceImpl`, the map-based service kept for the controller tests, now
wraps its sample beers in a `PageImpl`.

## 4. Open - other

| # | Improvement | Why | Status |
|---|---|---|---|
| 11 | Assert that page 2 holds **different** beers than page 1 | The paging test only counts, so an implementation that ignores `pageNumber` and always serves the first 50 would pass it. Comparing the first id of both pages catches the off-by-one between the API (pages from 1) and Spring Data (pages from 0) | 💡 agreed to revisit once paging is complete |
| 12 | No sort order anywhere | Without an `ORDER BY`, "the first 25 rows" is whatever the database returns. It can differ between H2 and MySQL, and a beer can in principle appear on two pages. JT's next lecture (`97-paging-add-sort`) adds sorting | ⏭ next lecture |
| 13 | `listBeers` still nulls `quantityOnHand` on the **entities** | The DTOs are what the client sees; nulling the entity works only because the entities are detached here. Inside a transaction the change could be written to the database | 💡 carried over from the Section 13 review |
| 14 | `listBeersByStyle` and `listBeersByName` are `public`, `listBeersByNameAndStyle` is `private` | All three are internal helpers of the service | 💡 |
