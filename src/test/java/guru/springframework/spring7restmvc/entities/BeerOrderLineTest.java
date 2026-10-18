package guru.springframework.spring7restmvc.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Pierrot on 18-10-2026
 */
class BeerOrderLineTest {

    // @Builder ignores a field initializer unless the field has @Builder.Default:
    // without it, the builder left both quantities null instead of 0
    @Test
    void builderKeepsTheDefaultQuantities() {
        BeerOrderLine line = BeerOrderLine.builder().build();

        assertThat(line.getOrderQuantity()).isZero();
        assertThat(line.getQuantityAllocated()).isZero();
    }
}
