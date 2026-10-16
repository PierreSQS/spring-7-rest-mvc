package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 16-10-2026
 */
@Slf4j
class BeerCsvServiceImplTest {

    BeerCsvService beerCsvService = new BeerCsvServiceImpl();

    @Test
    void convertCSV() {

        ClassPathResource resource = new ClassPathResource("csvdata/beers.csv");

        List<BeerCSVRecord> recs = beerCsvService.convertCSV(resource);

        log.info("CSV records read: {}", recs.size());

        assertThat(recs.size()).isGreaterThan(0);
    }
}