package guru.springframework.spring7restmvc.bootstrap;

import guru.springframework.spring7restmvc.model.BeerStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link BootstrapData#beerStyleOf(String)}: one case per keyword rule, the order of the rules
 * and the double-encoded names the dataset contains.
 */
class BeerStyleMappingTest {

    @DisplayName("maps a CSV style onto a beer style")
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // one case per rule
            "American IPA,                    IPA",
            "Oatmeal Stout,                   STOUT",
            "American Porter,                 PORTER",
            "Gose,                            GOSE",
            "Saison / Farmhouse Ale,          SAISON",
            "Witbier,                         WHEAT",
            "English Pale Ale,                PALE_ALE",
            "German Pilsener,                 PILSNER",
            "American Pale Lager,             LAGER",

            // the fallback
            "Cider,                           ALE",
            "Cream Ale,                       ALE",
            "American Amber / Red Ale,        ALE",
            "Pumpkin Ale,                     ALE",

            // rule order: both keywords match, the earlier rule wins
            "American Double / Imperial IPA,  IPA",
            "American Pale Wheat Ale,         WHEAT",

            // accented names of the dataset. "Märzen" stands alone here on purpose: the real value
            // also contains OKTOBERFEST, which would match even if the RZEN keyword were broken.
            "Märzen,                          LAGER",
            "Märzen / Oktoberfest,            LAGER",
            "Kölsch,                          ALE",
    })
    void csvKeyBeerStyleMatchesEnumBeerStyle(String csvStyle, BeerStyle expected) {
        assertThat(BootstrapData.beerStyleOf(csvStyle)).isEqualTo(expected);
    }

    @DisplayName("matches case-insensitively")
    @ParameterizedTest(name = "{0} -> IPA")
    @CsvSource({"american ipa", "AMERICAN IPA", "American Ipa"})
    void csvKeyBeerStyleMatchesEnumBeerStyleIgnoringCase(String csvStyle) {
        assertThat(BootstrapData.beerStyleOf(csvStyle)).isEqualTo(BeerStyle.IPA);
    }
}
