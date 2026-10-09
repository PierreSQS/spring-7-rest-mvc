package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 09-10-2026
 */
class BeerCsvServiceImplTest {

    BeerCsvService beerCsvService = new BeerCsvServiceImpl();

    @Test
    void convertCSV() {

        ClassPathResource resource = new ClassPathResource("csvdata/beers.csv");

        List<BeerCSVRecord> recs = beerCsvService.convertCSV(resource);

        System.out.println(recs.size());

        assertThat(recs.size()).isGreaterThan(0);
    }
}