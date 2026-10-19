package guru.springframework.spring7restmvc.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Pierrot on 19-10-2026
 */
class BeerOrderTest {

    // build() calls the full constructor, which calls setCustomer and setBeerOrderShipment:
    // without a null check, an order with no customer or no shipment threw a NullPointerException
    @Test
    void builderWithoutCustomerAndShipment() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customerRef("Not shipped yet")
                .build();

        assertThat(beerOrder.getCustomer()).isNull();
        assertThat(beerOrder.getBeerOrderShipment()).isNull();
    }

    // the null check must not break the helper setters: both sides are still linked
    @Test
    void builderLinksCustomerAndShipmentToTheOrder() {
        Customer customer = Customer.builder().name("Test Customer").build();
        BeerOrderShipment shipment = BeerOrderShipment.builder().trackingNumber("1235r").build();

        BeerOrder beerOrder = BeerOrder.builder()
                .customer(customer)
                .beerOrderShipment(shipment)
                .build();

        assertThat(customer.getBeerOrders()).containsExactly(beerOrder);
        assertThat(shipment.getBeerOrder()).isSameAs(beerOrder);
    }
}
