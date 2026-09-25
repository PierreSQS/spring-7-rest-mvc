package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.MySqlContainerBase;
import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.services.BeerCsvService;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 18-09-2026
 */
@DataJpaTest
// the JPA slice only loads JPA components, so BootstrapData has to be imported explicitly
// once imported, it runs as a CommandLineRunner when the test context starts and seeds the data
// BeerCsvServiceImpl comes with it: BootstrapData needs it to load the CSV, and a @Service is
// not part of the JPA slice either
@Import({BootstrapData.class, BeerCsvServiceImpl.class})
class MySqlIT extends MySqlContainerBase {

    /** The beers BootstrapData writes by hand, before it loads the CSV. */
    private static final int HANDWRITTEN_BEERS = 3;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    BeerCsvService beerCsvService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void testListBeers() {
        int csvRecords = beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv")).size();

        List<Beer> beers = beerRepository.findAll();

        // real MySQL, not H2: every CSV record survived the varchar(36) ids, the SMALLINT style
        // and the 50-character name column
        assertThat(beers).hasSize(HANDWRITTEN_BEERS + csvRecords);
    }

    // Kept for documentation, deliberately disabled: this test can hardly ever fail. A failing
    // migration stops the Spring context before any test runs, and skipped migrations leave the
    // order tables missing, which testOrderTablesUseOurColumnNames already catches.
    // To run it, add the field "@Autowired org.flywaydb.core.Flyway flyway;" and uncomment it.
    //
    // @Test
    // void testAllMigrationsApplied() {
    //     org.flywaydb.core.api.MigrationInfoService migrations = flyway.info();
    //
    //     // every V<n> script on the classpath ran and none failed; a new migration is covered
    //     // without touching this test
    //     assertThat(migrations.pending()).isEmpty();
    //     assertThat(migrations.applied()).isNotEmpty()
    //             .allSatisfy(migration -> assertThat(migration.getState())
    //                     .isEqualTo(org.flywaydb.core.api.MigrationState.SUCCESS));
    // }

    @Test
    void testOrderTablesUseOurColumnNames() {
        // V3 names these columns like beer and customer, not like JT's ERD (last_modified_date,
        // version bigint); a missing table shows up as an empty map
        for (String table : List.of("beer_order", "beer_order_line")) {
            assertThat(getColumnTypes(table)).as(table)
                    .containsEntry("created_date", "datetime")
                    .containsEntry("update_date", "datetime")
                    .containsEntry("version", "int")
                    .doesNotContainKey("last_modified_date");
        }
    }

    /** Column name to MySQL data type, as the server reports them for {@code table}. */
    private Map<String, String> getColumnTypes(String table) {
        Map<String, String> columnTypes = new HashMap<>();
        jdbcTemplate.query("""
                        select column_name, data_type
                        from information_schema.columns
                        where table_schema = database() and table_name = ?""",
                row -> {
                    columnTypes.put(row.getString(1), row.getString(2));
                },
                table);
        return columnTypes;
    }
}
