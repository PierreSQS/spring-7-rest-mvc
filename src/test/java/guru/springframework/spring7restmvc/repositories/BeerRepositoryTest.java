package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.model.BeerStyle;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Modified by Pierrot on 17-09-2026
 */
@DataJpaTest
class BeerRepositoryTest {

    @Autowired
    BeerRepository beerRepository;

    @Test
    void testSaveBeerNameTooLong() {
        Beer beer = Beer.builder()
                .beerName("My Beer 0123345678901233456789012334567890123345678901233456789012334567890123345678901233456789")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234234234234")
                .price(new BigDecimal("11.99"))
                .build();

        assertThatThrownBy(() -> beerRepository.saveAndFlush(beer),
                "saving a beer with a %d-character name (max 50) should fail bean validation",
                beer.getBeerName().length())
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void testSaveBeer() {
        Beer savedBeer = beerRepository.save(Beer.builder()
                        .beerName("My Beer")
                        .beerStyle(BeerStyle.PALE_ALE)
                        .upc("234234234234")
                        .price(new BigDecimal("11.99"))
                .build());

        beerRepository.flush();

        assertThat(savedBeer).isNotNull();
        assertThat(savedBeer.getId()).isNotNull();
    }
}