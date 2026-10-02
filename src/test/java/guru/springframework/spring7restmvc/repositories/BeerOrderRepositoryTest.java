package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.BeerOrder;
import guru.springframework.spring7restmvc.entities.BeerOrderShipment;
import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.UUID;

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

    // für Sendungen gibt es kein Repository; der TestEntityManager liest sie direkt
    @Autowired
    TestEntityManager entityManager;

    Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = customerRepo.findAll().getFirst();
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

        // saveAndFlush writes the order and its shipment to the database now, so a mapping error
        // fails the test instead of hiding behind the rollback
        BeerOrder savedOrder = beerOrderRepo.saveAndFlush(order);

        // the customer already knows the order because setCustomer(...) put it into the list
        // (see docs/lazy-collections-and-flush.md)
        assertThat(savedOrder.getCustomer().getBeerOrders()).contains(savedOrder);

        // the shipment was saved too: only a saved shipment gets an id
        assertThat(savedOrder.getBeerOrderShipment().getId()).isNotNull();

        // the shipment knows its order (the other side of the one-to-one)
        assertThat(savedOrder.getBeerOrderShipment().getBeerOrder()).isNotNull();
    }

    // Issue 3: Eine Bestellung mit Sendung lässt sich löschen, und die Sendung geht mit
    @Test
    void testDeleteOrderDeletesShipment() {
        BeerOrder savedOrder = beerOrderRepo.saveAndFlush(getOrderWithShipment("to-be-deleted"));

        // die id VOR dem Löschen merken, danach gibt es nichts mehr zum Fragen
        UUID shipmentId = savedOrder.getBeerOrderShipment().getId();

        beerOrderRepo.delete(savedOrder);
        // flush: das delete muss wirklich in die Datenbank, sonst prüfen wir nur Java
        beerOrderRepo.flush();

        assertThat(beerOrderRepo.findById(savedOrder.getId())).isEmpty();
        // ohne cascade = ALL bliebe die Sendung übrig
        assertThat(entityManager.find(BeerOrderShipment.class, shipmentId)).isNull();
    }

    // Issue 4: Eine gespeicherte Bestellung bekommt eine neue Sendung, und die alte wird getrennt
    @Test
    void testReplaceShipment() {
        BeerOrder savedOrder = beerOrderRepo.saveAndFlush(getOrderWithShipment("old-shipment"));

        // die alte Sendung VOR dem Austauschen merken, sonst kann man sie hinterher nicht prüfen
        BeerOrderShipment oldShipment = savedOrder.getBeerOrderShipment();

        savedOrder.setBeerOrderShipment(BeerOrderShipment.builder()
                .trackingNumber("new-shipment")
                .build());
        beerOrderRepo.saveAndFlush(savedOrder);

        // Java-Seite: der Helper hat die alte Sendung getrennt und die neue verbunden
        assertThat(oldShipment.getBeerOrder()).isNull();
        assertThat(savedOrder.getBeerOrderShipment().getBeerOrder()).isEqualTo(savedOrder);

        // Datenbank-Seite: Cache leeren und neu laden, damit die Antwort aus der Datenbank kommt
        entityManager.clear();
        BeerOrder reloadedOrder = beerOrderRepo.findById(savedOrder.getId()).orElseThrow();
        assertThat(reloadedOrder.getBeerOrderShipment().getTrackingNumber()).isEqualTo("new-shipment");
    }

    /** Eine neue Bestellung des Testkunden mit einer neuen Sendung, beide noch nicht gespeichert. */
    private BeerOrder getOrderWithShipment(String trackingNumber) {
        return BeerOrder.builder()
                .customerRef(testCustomer.getName())
                .customer(testCustomer)
                .beerOrderShipment(BeerOrderShipment.builder()
                        .trackingNumber(trackingNumber)
                        .build())
                .build();
    }
}