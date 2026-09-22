# Why a classpath file works in the IDE but not from a jar

**Read it as a stream, never as a `File`.** In the IDE a classpath file is a real file on disk, inside a jar it is only an entry in a ZIP archive. Everything that asks for a `File` breaks at that moment; everything that asks for an `InputStream` keeps working.

This project hit it with `csvdata/beers.csv` in Section 14.

## What actually changes

Running from the IDE or from `mvn test`, the classpath is a directory tree:

```
target/classes/csvdata/beers.csv          <- a real file, has a path on disk
```

Running from the packaged jar, the same file is one entry in one archive:

```
spring-7-rest-mvc-0.0.1-SNAPSHOT.jar
└── BOOT-INF/classes/csvdata/beers.csv    <- no path on disk, cannot be opened as a File
```

Check it yourself after a build:

```bash
unzip -l target/spring-7-rest-mvc-0.0.1-SNAPSHOT.jar | grep beers.csv
#  427708  2026-09-22 23:54   BOOT-INF/classes/csvdata/beers.csv
```

The file is present, so "not found" errors are misleading: the file is there, it simply is not a file.

## What works and what does not

| How the file is read | IDE / `mvn test` | From the jar |
|---|---|---|
| `resource.getInputStream()` | works | **works** |
| `getClass().getResourceAsStream("/csvdata/beers.csv")` | works | **works** |
| `resource.getFile()` | works | `FileNotFoundException` |
| `ResourceUtils.getFile("classpath:csvdata/beers.csv")` | works | `FileNotFoundException` |
| `new File("src/main/resources/csvdata/beers.csv")` | works while the working directory is the project | fails - `src` is not shipped |
| `Paths.get(url.toURI())` | works | `FileSystemNotFoundException` |
| Listing a classpath *directory* | works | returns nothing - use `PathMatchingResourcePatternResolver` with `classpath*:` |

The failure message names the cause exactly (verified in the spring-core 7.0.9 sources):

```
class path resource [csvdata/beers.csv] cannot be resolved to absolute file path
because it does not reside in the file system: jar:file:/.../spring-7-rest-mvc-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes/csvdata/beers.csv
```

"does not reside in the file system" is the sentence to look for.

## How this project does it

**The service takes a `Resource` and opens a stream** (`BeerCsvServiceImpl`):

```java
public List<BeerCSVRecord> convertCSV(Resource csvResource) {
    try (Reader reader = new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8)) {
        ...
    }
}
```

Two decisions in one line:

1. `getInputStream()` instead of `getFile()` - survives packaging.
2. An explicit `StandardCharsets.UTF_8` instead of `new FileReader(...)`, which uses the platform charset. On this Windows machine that is cp1252, which mangles every accented beer name.

**The caller names the file, the service stays unaware of where it lives** (`BootstrapData`, `BeerControllerIT`, `BootstrapDataTest`):

```java
beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv"));
```

JT's original was `ResourceUtils.getFile("classpath:csvdata/beers.csv")`, which is in the third row of the table above: fine in the IDE, broken in production.

## Path spelling

| API | Leading slash | Root |
|---|---|---|
| `new ClassPathResource("csvdata/beers.csv")` | **no** | classpath root |
| `getClass().getResourceAsStream("/csvdata/beers.csv")` | **yes**, otherwise it resolves relative to the class's package | classpath root |
| `ApplicationContext.getResource("classpath:csvdata/beers.csv")` | no | classpath root |

## Why the tests never catch this

Maven does not package tests, and a test never runs from the application jar: it runs from `target/test-classes` against `target/classes`, which are plain directories. **A green build therefore proves nothing about jar behaviour.** The only honest check is to run the jar:

```bash
./mvnw.cmd clean package -DskipITs
java -jar target/spring-7-rest-mvc-0.0.1-SNAPSHOT.jar --server.port=0 --spring.main.banner-mode=off
```

Run on 2026-09-23 against the jar this project builds:

```
INFO ... g.s.s.Spring7RestMvcApplication : Started Spring7RestMvcApplication in 6.821 seconds
INFO ... g.s.s.bootstrap.BootstrapData   : ### 2413 beers in the DB after loading csvdata/beers.csv
```

That second line comes from `BootstrapData`, so seeing it means the CSV really was read from inside the archive. `--server.port=0` picks a free port, so the run cannot collide with an application already using 8080.

## Rule of thumb

- Write against `Resource` / `InputStream`, always.
- Treat `getFile()`, `ResourceUtils.getFile()`, `new File(...)` and `Path` on a classpath entry as bugs waiting for the first deployment.
- State the charset when turning a stream into text.
- If a library insists on a `File`, copy the stream to a temporary file first - this project never needed to.
