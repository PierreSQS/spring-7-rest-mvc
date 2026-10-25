package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 25-10-2026
 */
class BeerCsvServiceImplTest {

    BeerCsvService beerCsvService = new BeerCsvServiceImpl();

    @Test
    void convertCSV() {

        ClassPathResource classPathResource = new ClassPathResource("csvdata/beers.csv");

        List<BeerCSVRecord> recs = beerCsvService.convertCSV(classPathResource);

        System.out.println(recs.size());

        assertThat(recs).hasSizeGreaterThan(0);
    }
}