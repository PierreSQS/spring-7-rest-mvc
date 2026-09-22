package guru.springframework.spring7restmvc.bootstrap;

import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.repositories.BeerRepository;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import guru.springframework.spring7restmvc.services.BeerCsvService;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 22-09-2026
 */

@Slf4j
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

    @Test
    void testRun() {
        int csvRecords = beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv")).size();

        long beersInDB = beerRepository.count();
        log.info("Data in the DB = {}", beersInDB);
        log.info("Data in the CSV File = {}", csvRecords);

        // every CSV record becomes a beer, on top of the three written by hand
        assertThat(beersInDB).isEqualTo(HANDWRITTEN_ROWS + csvRecords);
        assertThat(customerRepository.count()).isEqualTo(HANDWRITTEN_ROWS);
    }

    @Test
    void testCustomersStartAtFirstVersion() {
        // Hibernate starts @Version at 0 for a persisted entity
        assertThat(customerRepository.findAll())
                .extracting(Customer::getVersion)
                .containsOnly(0);
    }
}
