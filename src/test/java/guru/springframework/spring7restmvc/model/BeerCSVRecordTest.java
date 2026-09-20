package guru.springframework.spring7restmvc.model;

import com.opencsv.bean.CsvToBeanBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that every column of csvdata/beers.csv is bound, including the ones whose header is not a
 * legal Java identifier.
 */
class BeerCSVRecordTest {

    private List<BeerCSVRecord> parseDataset() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/csvdata/beers.csv");
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return new CsvToBeanBuilder<BeerCSVRecord>(reader)
                    .withType(BeerCSVRecord.class)
                    .build()
                    .parse();
        }
    }

    @Test
    void parsesEveryRecordOfTheDataset() throws Exception {
        assertThat(parseDataset()).hasSize(2410);
    }

    @Test
    void bindsEveryColumnOfTheFirstRecord() throws Exception {
        BeerCSVRecord first = parseDataset().getFirst();

        assertThat(first.getRow()).isEqualTo(1);
        assertThat(first.getCount()).isEqualTo(1);
        assertThat(first.getAbv()).isEqualTo("0.05");
        assertThat(first.getIbu()).isEqualTo("NA");
        assertThat(first.getId()).isEqualTo(1436);
        assertThat(first.getBeer()).isEqualTo("Pub Beer");
        assertThat(first.getStyle()).isEqualTo("American Pale Lager");
        assertThat(first.getBreweryId()).isEqualTo(408);
        assertThat(first.getOunces()).isEqualTo(12.0f);
        assertThat(first.getStyle2()).isEqualTo("NA");
        assertThat(first.getCountY()).isEqualTo("409");
        assertThat(first.getBrewery()).isEqualTo("10 Barrel Brewing Company");
        assertThat(first.getCity()).isEqualTo("Bend");
        assertThat(first.getState()).isEqualTo("OR");
        assertThat(first.getLabel()).isEqualTo("Pub Beer (10 Barrel Brewing Company)");
    }

    @Test
    void bindsTheColumnsTheImportDependsOnForEveryRecord() throws Exception {
        assertThat(parseDataset())
                .allSatisfy(record -> assertThat(record)
                        .extracting(BeerCSVRecord::getRow, BeerCSVRecord::getCount,
                                BeerCSVRecord::getBeer, BeerCSVRecord::getStyle,
                                BeerCSVRecord::getBrewery)
                        .doesNotContainNull());
    }
}
