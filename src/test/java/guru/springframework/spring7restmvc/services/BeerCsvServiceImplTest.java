package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 25-10-2026
 */
class BeerCsvServiceImplTest {

    BeerCsvService beerCsvService = new BeerCsvServiceImpl();

    @Test
    void convertCSV() throws IOException {

        ClassPathResource classPathResource = new ClassPathResource("csvdata/beers.csv");

        List<BeerCSVRecord> recs = beerCsvService.convertCSV(
                new InputStreamReader(classPathResource.getInputStream(), StandardCharsets.UTF_8));

        System.out.println(recs.size());

        assertThat(recs).hasSizeGreaterThan(0);
    }
}