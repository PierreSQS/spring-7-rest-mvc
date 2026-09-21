# Section 14 (CSV) - possible improvements

Written at the end of the CSV lesson, **Section 14, chapters 147-153** (JT's branches `79-beer-csv-data`
to `85-beer-csv-fix-integration-tests`). Chapter 152 and JT's branch `84-hibernate-create-update-timestamp`
were skipped: `@CreationTimestamp` and `@UpdateTimestamp` were already on `Beer` from an earlier lesson.

All numbers below were measured against `src/main/resources/csvdata/beers.csv`: **2410 records**,
15 columns, 100 distinct style names.

---

## 1. Deviations already applied during the lesson

These are differences from JT's code that were part of the lesson commits, not suggestions.

| Deviation | Reason |
|---|---|
| `BeerCsvService.convertCSV(Resource)` instead of `File` | JT's `ResourceUtils.getFile("classpath:...")` throws `FileNotFoundException` once the app runs from the packaged jar, because the classpath entry is no longer a file. |
| `InputStreamReader(..., UTF_8)` instead of `new FileReader(file)` | `FileReader` uses the platform default charset - cp1252 on this machine - which corrupts every accented name. |
| `UncheckedIOException` with the resource description instead of `RuntimeException(e)` | Keeps the failing resource in the message; the missing-file case has a test. |
| Own `truncateBeerName` instead of `StringUtils.abbreviate` | `StringUtils` is commons-lang3, which the project only gets **transitively** through OpenCSV. |
| `@Import({BootstrapData.class, BeerCsvServiceImpl.class})` instead of `new BootstrapData(...)` | Convention of this repository; a `@DataJpaTest` slice loads no `@Service`, so `MySqlIT` and `BootstrapDataTest` must import it. |
| Keyword-based `BeerStyle` classification | JT's `switch` names 16 of the 100 styles, so **1160 of 2410 rows** fell through to `PILSNER`. The keyword rules leave 91 there. |
| Seed-independent test assertions | `BootstrapDataTest` derives the expected count from the parsed CSV, `BeerControllerIT` compares the listing with `beerRepository.count()`; neither needs editing when the dataset changes. |
| `brewery` column bound in `BeerCSVRecord` | JT binds 14 of the 15 columns and silently drops the brewery name. |
| `count_y` renamed to `countY` | Lombok generated `getCount_y()`. |
| `required = true` on seven columns | No column is ever empty in the 2410 records, so a malformed file should fail loudly rather than produce nulls. |

---

## 2. Improvements applied after the lesson

Branch `improvements/sec14-csv`, commits prefixed `Sec14_Chap153-X`.

### 2.1 Repaired the double encoding of the dataset

The file stored UTF-8 text that had been misread as **cp1252** once before being saved again, so
115 of the 2411 lines carried mangled names:

| in the file | correct |
|---|---|
| `KÃ¶lsch` | `Kölsch` |
| `MÃ¤rzen / Oktoberfest` | `Märzen / Oktoberfest` |
| `Garce SelÃ©` | `Garce Selé` |
| `Avery Joeâ€™s Premium American Pilsner` | `Avery Joe's Premium American Pilsner` |

Those names went into the database unchanged, so the damage was visible in every API response.

The repair is a byte-level round trip - encode the text as cp1252, decode it as UTF-8 - with strict
decoding, so an unrepairable sequence aborts instead of producing `U+FFFD`. `â€™` is the giveaway
that the misreading was cp1252 and not Latin-1: byte `0x80` becomes `€` and `0x99` becomes `™`,
which Latin-1 does not do.

The tests that pinned the damaged strings (`BeerCsvServiceImplTest`, `BeerStyleMappingTest`) now pin
the correct ones. The `RZEN` keyword in `BootstrapData.beerStyleOf` stays ASCII on purpose: it
matches `MÄRZEN` as well as the old mangled spelling.

### 2.2 `MySqlIT` asserts the seeded row count

It only checked `isNotEmpty()` while seeding 2413 rows into a real MySQL container. It now asserts
`HANDWRITTEN_BEERS + csvRecords`, so a row lost to a MySQL column limit - `varchar(36)` ids, the
`SMALLINT` style, the 50-character name - fails the test instead of passing unnoticed.

### 2.3 The truncated beer names are logged

Two of the 2410 names are longer than the 50-character column and were shortened silently.
`BootstrapData` now warns with both the stored and the full name:

```text
### Beer name longer than 50 characters, stored as 'Lee Hill Series Vol. 3 - Barrel Aged Imperial S...':
    Lee Hill Series Vol. 3 - Barrel Aged Imperial Stout
### Beer name longer than 50 characters, stored as 'Lee Hill Series Vol. 5 - Belgian Style Quadrupe...':
    Lee Hill Series Vol. 5 - Belgian Style Quadrupel Ale
```

---

## 3. Deliberately not done

| Suggestion | Decision |
|---|---|
| Seed the beers with a Flyway migration (`V3__insert-beers.sql`) instead of the `CommandLineRunner` | **Declined.** The lesson is about loading data from a CSV; moving the data into SQL would remove its subject. |

---

## 4. Still open

| # | Improvement | Why it would help |
|---|---|---|
| 1 | `abv` and `ibu` as numbers, through a small `@CsvCustomBindByName` converter mapping `NA` to `null` | They are `String` today (`NA` appears in 62 and 1005 records), so they cannot be filtered or sorted in the paging chapters ahead. |
| 2 | Batch the inserts: `saveAll` in chunks plus `spring.jpa.properties.hibernate.jdbc.batch_size` | 2410 individual `save()` calls; the bootstrap costs about 7-9 seconds in every `@DataJpaTest` and on every fresh `localmysql` start. |
| 3 | A real `upc` instead of the row number (`upc(record.getRow().toString())`) | A UPC is not a line number. The CSV `id` column or a generated 12-digit code would give the field a meaning. |
| 4 | A varying `price` instead of `BigDecimal.TEN` for all 2413 beers | Flat data makes sorting and paging demonstrations meaningless. |
| 5 | Keep `brewery`, `city` and `state` - a `Brewery` entity | They are parsed and then dropped; the model is heading there anyway. |
| 6 | Avoid parsing the CSV twice in `BootstrapDataTest` | The price of the seed-independent assertion: once in the test, once in the bootstrap. |

---

## 5. Verification

`mvnw verify` with Docker running, after every commit on this branch:

```text
Unit:        48 tests, 0 failures
Integration: 20 tests, 0 failures   (BeerControllerIT 10, CustomerControllerIT 9, MySqlIT 1)
BUILD SUCCESS
```
