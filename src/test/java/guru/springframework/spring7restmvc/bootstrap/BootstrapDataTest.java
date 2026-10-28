package guru.springframework.spring7restmvc.bootstrap;

import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.repositories.BeerRepository;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 29-10-2026
 */
@DataJpaTest(showSql = false)
@Import({BeerCsvServiceImpl.class, BootstrapData.class})
class BootstrapDataTest {

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Test
    void testRun() {

        assertThat(beerRepository.count()).isEqualTo(2413);
        assertThat(customerRepository.count()).isEqualTo(3);
    }

    @Test
    void testOnlyRealPilsnersArePilsner() {
        // 91 CSV beers have "Pils" in their style; the old switch defaulted 1160 beers to PILSNER
        assertThat(beerRepository.findAllByBeerStyle(BeerStyle.PILSNER, Pageable.unpaged()).getTotalElements())
                .isEqualTo(91);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            American Pale Wheat Ale       | WHEAT
            Hefeweizen                    | WHEAT
            American Double / Imperial IPA | IPA
            American India Pale Lager     | LAGER
            Russian Imperial Stout        | STOUT
            Baltic Porter                 | PORTER
            Gose                          | GOSE
            Saison / Farmhouse Ale        | SAISON
            Czech Pilsener                | PILSNER
            Märzen / Oktoberfest          | LAGER
            Doppelbock                    | LAGER
            American Pale Ale (APA)       | PALE_ALE
            American Amber / Red Ale      | ALE
            Kölsch                        | ALE
            Cider                         | ALE
            """)
    void testBeerStyleOf(String csvStyle, BeerStyle expected) {
        assertThat(BootstrapData.beerStyleOf(csvStyle)).isEqualTo(expected);
    }

    @Test
    void testBeerStyleOfNull() {
        assertThat(BootstrapData.beerStyleOf(null)).isEqualTo(BeerStyle.ALE);
    }
}
