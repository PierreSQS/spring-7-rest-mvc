package guru.springframework.spring7restmvc.bootstrap;

import guru.springframework.spring7restmvc.repositories.BeerRepository;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import guru.springframework.spring7restmvc.services.BeerCsvService;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 17-09-2026
 */
@DataJpaTest
@Import({BootstrapData.class, BeerCsvServiceImpl.class})
class BootstrapDataTest {

    /** The beers and the customers BootstrapData writes by hand, before it loads the CSV. */
    private static final int HANDWRITTEN_ROWS = 3;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    BeerCsvService beerCsvService;

    @Autowired
    BootstrapData bootstrapData;

    @Test
    void testRun() {
        int csvRecords = beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv")).size();

        bootstrapData.run();

        // every CSV record becomes a beer, on top of the three written by hand
        assertThat(beerRepository.count()).isEqualTo(HANDWRITTEN_ROWS + csvRecords);
        assertThat(customerRepository.count()).isEqualTo(HANDWRITTEN_ROWS);
    }
}
