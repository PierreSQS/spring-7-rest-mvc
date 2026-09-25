package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@Slf4j
@DataJpaTest
// the JPA slice loads no @Component, so BootstrapData is imported to seed the customers and beers
// BeerCsvServiceImpl comes with it because BootstrapData reads the CSV through it.
// Unlike JT's @SpringBootTest, the slice runs each test in a transaction that is rolled back.
@Import({BootstrapData.class, BeerCsvServiceImpl.class})
class BeerOrderRepositoryTest {

    @Autowired
    BeerOrderRepository beerOrderRepo;

    @Autowired
    CustomerRepository customerRepo;

    @Autowired
    BeerRepository beerRepo;

    Customer testCustomer;
    Beer testBeer;

    @BeforeEach
    void setUp() {
        testCustomer = customerRepo.findAll().getFirst();
        testBeer = beerRepo.findAll().getFirst();
    }

    @Test
    void testBeerOrders() {
        log.info("Beer Orders Count: {}", beerOrderRepo.count());
        log.info("Customer Count: {}", customerRepo.count());
        log.info("Beer Count: {}", beerRepo.count());
        log.info("Test Customer Name: {}", testCustomer.getName());
        log.info("Test Beer Name: {}", testBeer.getBeerName());

    }
}