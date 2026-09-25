package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.entities.Customer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
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
        log.info("Beer Orders Count: {}", beerOrderRepo.findAll().size());
        log.info("Customer Count: {}", customerRepo.findAll().size());
        log.info("Beer Count: {}", beerRepo.findAll().size());
        log.info("Test Customer Name: {}", testCustomer.getName());
        log.info("Test Beer Name: {}", testBeer.getBeerName());

    }
}