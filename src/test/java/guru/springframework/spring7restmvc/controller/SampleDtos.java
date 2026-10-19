package guru.springframework.spring7restmvc.controller;

import guru.springframework.spring7restmvc.model.BeerDTO;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.model.CustomerDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Sample DTOs for the {@code @WebMvcTest} controller tests, which mock the service.
 * They replace JT's in-memory BeerServiceImpl / CustomerServiceImpl, which only served to
 * introduce dependency injection: the application never used them, but they stayed in main.
 * <p>
 * Every call builds new objects, so a test may change them freely; the order is always the same.
 * <p>
 * Created by Pierrot on 20-10-2026
 */
final class SampleDtos {

    private SampleDtos() {
    }

    static List<BeerDTO> beers() {
        return List.of(
                beer("Galaxy Cat", BeerStyle.PALE_ALE, "12356", "12.99", 122),
                beer("Crank", BeerStyle.PALE_ALE, "12356222", "11.99", 392),
                beer("Sunshine City", BeerStyle.IPA, "12356", "13.99", 144));
    }

    static List<CustomerDTO> customers() {
        return List.of(customer("Customer 1"), customer("Customer 2"), customer("Customer 3"));
    }

    private static BeerDTO beer(String name, BeerStyle style, String upc, String price, int quantity) {
        return BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName(name)
                .beerStyle(style)
                .upc(upc)
                .price(new BigDecimal(price))
                .quantityOnHand(quantity)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
    }

    private static CustomerDTO customer(String name) {
        return CustomerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .name(name)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
    }
}
