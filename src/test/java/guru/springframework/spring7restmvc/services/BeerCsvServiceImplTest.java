package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BeerCsvServiceImplTest {

    BeerCsvService beerCsvService = new BeerCsvServiceImpl();

    @Test
    void convertsTheWholeDataset() {
        List<BeerCSVRecord> beerCSVRecords = beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv"));

        assertThat(beerCSVRecords).hasSize(2410);
    }

    /**
     * Pins the charset: read with the platform default of a German Windows (cp1252) the name would
     * come out as "Garce SelÃƒÂ©".
     */
    @Test
    void decodesTheFileAsUtf8() {
        List<BeerCSVRecord> beerCSVRecords = beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv"));

        assertThat(beerCSVRecords)
                .filteredOn(beerCSVRecord -> beerCSVRecord.getRow() == 13)
                .singleElement()
                .extracting(BeerCSVRecord::getBeer)
                .isEqualTo("Garce SelÃ©");
    }

    @Test
    void failsOnAMissingFile() {
        Resource missing = new ClassPathResource("csvdata/no-such-file.csv");

        assertThatExceptionOfType(java.io.UncheckedIOException.class)
                .isThrownBy(() -> beerCsvService.convertCSV(missing))
                .withMessageContaining("no-such-file.csv");
    }
}
