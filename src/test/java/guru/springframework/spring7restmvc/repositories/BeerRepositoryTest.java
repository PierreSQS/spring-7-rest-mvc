package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@Import({BootstrapData.class, BeerCsvServiceImpl.class})
class BeerRepositoryTest {

    /** Beers carrying IPA in their name, in the seeded data. */
    private static final int BEERS_NAMED_IPA = 336;

    @Autowired
    BeerRepository beerRepository;

    @Test
    void testGetBeerListByName() {
        PageRequest firstPage = PageRequest.of(0, 25);

        Page<Beer> page = beerRepository.findAllByBeerNameIsLikeIgnoreCase("%IPA%", firstPage);

        // the page comes back full, and it also reports how many beers match in total
        assertThat(page.getContent())
                .hasSize(firstPage.getPageSize())
                .allSatisfy(beer -> assertThat(beer.getBeerName()).containsIgnoringCase("IPA"));
        assertThat(page.getTotalElements()).isEqualTo(BEERS_NAMED_IPA);
    }

    @Test
    void testSaveBeerNameTooLong() {
        Beer tooLongName = Beer.builder()
                .beerName("a".repeat(51))
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234234234234")
                .price(new BigDecimal("11.99"))
                .build();

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> beerRepository.saveAndFlush(tooLongName))
                .withMessageContaining("Beer name must not exceed 50 characters");
    }

    @Test
    void testSaveBeer() {
        Beer savedBeer = beerRepository.saveAndFlush(Beer.builder()
                .beerName("My Beer")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("234234234234")
                .price(new BigDecimal("11.99"))
                .build());

        assertThat(savedBeer.getId()).isNotNull();
        assertThat(savedBeer.getCreatedDate()).isNotNull();
    }
}
