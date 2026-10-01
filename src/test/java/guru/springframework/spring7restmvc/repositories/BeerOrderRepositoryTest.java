package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.entities.BeerOrder;
import guru.springframework.spring7restmvc.entities.BeerOrderShipment;
import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

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
        // a new order for the test customer; the builder calls setCustomer(...), which also adds
        // the order to the customer's order list in Java
        BeerOrder order = BeerOrder.builder()
                .customerRef(testCustomer.getName())
                .customer(testCustomer)
                .beerOrderShipment(BeerOrderShipment.builder()
                        .trackingNumber("1234k")
                        .build())
                .build();

        // save() only notes the order for later: no insert is needed for the check below
        BeerOrder savedOrder = beerOrderRepo.save(order);

        // the customer already knows the order because setCustomer(...) put it into the list
        // (see docs/lazy-collections-and-flush.md)
        assertThat(savedOrder.getCustomer().getBeerOrders()).contains(savedOrder);

        // the shipment was saved too: only a saved shipment gets an id
        assertThat(savedOrder.getBeerOrderShipment().getId()).isNotNull();

        // the shipment knows its order (the other side of the one-to-one)
        assertThat(savedOrder.getBeerOrderShipment().getBeerOrder()).isNotNull();
    }
}