# Section 16 (Paging) - improvements

> **Snapshot.** This page reviews one lesson and describes the code as it stood then. Later sections
> changed some of it. For how the project behaves **today**, see `CLAUDE.md` and the topic pages such as
> `service-layer-boundaries.md`.

Written during the paging lesson, **Section 16, chapters 166-169** (JT's branches `94-page-add-parameters`
to `97-paging-add-sort`). The lesson was worked in the TDD order JT uses: the failing test first, then the
request parameters, then the page request, then the paged queries, then the sort order.

All numbers below come from the seeded data: **2413 beers**, of which **336** carry `IPA` in their name,
**572** are of style `IPA`, and **324** are both.

Every item below was applied in the end. The numbering is kept because the commit comments refer to
it, and the status column records that nothing is left open.

---

## 1. Applied in `BeerServiceJPA`

| # | Improvement | Why | Status |
|---|---|---|---|
| 1 | `MAX_PAGE_SIZE` constant instead of the literal `1000`, used twice | The number said nothing about itself, and the cap exists for a reason worth writing down: a client must not pull the whole table in one request | ✅ |
| 2 | `Math.min` and two conditional expressions instead of nested `if/else` | 22 lines became 8, with the same behaviour | ✅ |
| 3 | A page size below 1 falls back to the default | **Bug fix.** `AbstractPageRequest` throws `Page size must not be less than one`, so `pageSize=0` or `pageSize=-5` answered **500** instead of serving a page. Verified in the spring-data-commons sources | ✅ |
| 4 | `buildPageRequest` package-private instead of `public` | Only the class and its test use it | ✅ |
| 5 | Constants above the injected fields | Convention | ✅ |

`BeerServiceJPATest` (plain JUnit, no Spring context) pins eleven cases: eight translations - page 1
becomes page 0, page 2 becomes page 1, a page number or size below 1 falls back to the default, 5000 is
capped at 1000 - plus no parameters at all meaning page 0 with 25 rows, and the two sort checks added in
chapter 169 (see item 11).

## 2. Applied in the tests

| # | Improvement | Why | Status |
|---|---|---|---|
| 6 | The list tests assert the **content**, not only the size | After paging, every list test expected `25`, which is simply the default page size. **All five would have passed with a completely broken search.** They now check that every beer on the page matches the search: `$.content..beerName` contains `IPA`, `$.content..beerStyle` equals `IPA` | ✅ |
| 7 | `DEFAULT_PAGE_SIZE` constant in `BeerControllerIT` | `25` appeared seven times as a bare number. It is not a number the tests chose - it is `BeerServiceJPA.DEFAULT_PAGE_SIZE`, and the constant says so. `PAGE_SIZE = 50` stays for the test that pages on purpose | ✅ |
| 8 | `BeerRepositoryTest` asserts the page, not an unpaged count | It compared a paged result with a count over the whole table: the query filters then takes 25, the count took 25 then filtered - different questions, no reason to be equal. A variable `long expected = 25;` said nothing either. Since item 10 the test proves both facets: `getContent()` holds 25 IPAs and `getTotalElements()` is 336 | ✅ |
| 9 | Javadoc of the TDD test rewritten | It still described a feature that did not exist yet and quoted a count that had become wrong | ✅ |

### The trap behind #6

`showInventory=false` does not remove `quantityOnHand` from the JSON, it sets it to `null`. So
`jsonPath("$.content..quantityOnHand").doesNotExist()` fails with `found: [null, null, ...]`; the correct
assertion is `everyItem(nullValue())`. Found by running the tests, not by reading the code.

The paths in these tests start with `$.content` because the answer became a page object, see items 10
and 15.

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
  `BEERS_NAMED_IPA_OF_STYLE_IPA` (324) came back as assertions on the reported total, so every search test
  proves its whole result set and not just the size of one page. The path is `$.page.totalElements` since
  item 15.
- **`BeerRepositoryTest` proves both** at once: `getContent()` holds 25 rows, `getTotalElements()` is 336.
- **The three finder helpers of the service are `private`** now; two of them used to be `public`.

The price is a **breaking change of the API**: the answer is no longer a bare array but an object, so
every JSON path in the tests moved under `content`, and `BeerServiceImpl`, the map-based service kept for
the controller tests, now wraps its sample beers in a `PageImpl`. The exact shape of that object changed
once more straight afterwards - see item 15.

| # | Improvement | Why | Status |
|---|---|---|---|
| 15 | `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` | Sending a `Page` as it is produced twelve fields, most of them Spring's own bookkeeping (`pageable`, `sort`, `empty`, `numberOfElements`), and Spring Data warned on every start that it promises nothing about that JSON. `VIA_DTO` answers a documented `PagedModel` instead: `content` plus `size`, `number`, `totalElements`, `totalPages` under `page`. The warning is gone, and the six total assertions moved from `$.totalElements` to `$.page.totalElements` | ✅ |

The annotation takes over the web support Boot would configure by itself, so the `spring.data.web.pageable.*`
properties no longer apply - this project does not use them, its defaults live in `BeerServiceJPA`.

## 4. Applied - found during the section

| # | Improvement | Why | Status |
|---|---|---|---|
| 11 | Assert that page 2 holds **different** beers than page 1 | Counting the beers on a page proved nothing: an implementation ignoring `pageNumber` and always serving the first 50 passed that check. `BeerControllerIT.testSecondPageHoldsOtherBeersThanTheFirst` compares the ids of both pages, and `BeerServiceJPATest` pins the sort - including that the sorted property is really a field of `Beer` | ✅ |
| 12 | No sort order anywhere | Without an `ORDER BY`, "the first 25 rows" was whatever the database returned: a beer could appear on two pages, and H2 and MySQL could disagree. Chapter 169 adds `Sort.by("beerName").ascending()` to every page request | ✅ |
| 13 | `listBeers` nulls `quantityOnHand` on the **DTOs**, no longer on the entities | Inside a transaction Hibernate took the emptied field for a change and would have written the nulls to the database. See `docs/service-layer-boundaries.md` | ✅ |
| 14 | `listBeersByStyle` and `listBeersByName` are `public`, `listBeersByNameAndStyle` is `private` | All three are internal helpers of the service | ✅ all three are `private` since the `Page` change |

---

## 5. Where the API stands at the end of Section 16

```
GET /api/v1/beer?beerName=IPA&beerStyle=IPA&showInventory=false&pageNumber=2&pageSize=50
```

```json
{
  "content": [ ... 50 beers, ordered by name, without their stock ... ],
  "page": { "size": 50, "number": 1, "totalElements": 324, "totalPages": 7 }
}
```

| Behaviour | Rule |
|---|---|
| Page numbering | the API counts from 1, Spring Data from 0; `buildPageRequest` shifts it |
| No parameters given | page 1 with 25 beers (`DEFAULT_PAGE_SIZE`) |
| `pageSize` above 1000 | capped at `MAX_PAGE_SIZE` |
| `pageNumber` or `pageSize` below 1 | treated as missing, so the default applies - `PageRequest.of` can never throw |
| Order | always `beerName` ascending, so no beer lands on two pages and H2 and MySQL agree |
| `showInventory=false` | `quantityOnHand` is emptied **on the DTOs**, never on the entities - see `service-layer-boundaries.md` |
| Response shape | `PagedModel` through `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`: `content` plus four documented values under `page` |

Every item in this document is applied. The whole section was worked on two branches,
`94-page-add-parameters-sb411` (chapter 166) and `97-paging-add-sort-sb411` (chapters 167-169, after the
short-lived `95-...` and `96-...` names), and ends with **68 unit + 33 integration tests** green.

Chapters 167 and 168 each left the suite red for a while, on purpose: the TDD test came first and the
commits that could not fix it yet carry the `-NOK` suffix.
