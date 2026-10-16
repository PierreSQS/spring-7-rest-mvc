package guru.springframework.spring7restmvc.services;

import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 16-10-2026
 */
@Slf4j
class BeerCsvServiceImplTest {

    BeerCsvService beerCsvService = new BeerCsvServiceImpl();

    @Test
    void convertCSV() {

        ClassPathResource resource = new ClassPathResource("csvdata/beers.csv");

        List<BeerCSVRecord> recs = beerCsvService.convertCSV(resource);

        log.info("CSV records read: {}", recs.size());

        assertThat(recs.size()).isGreaterThan(0);
    }

    /*
     * Protège la réparation de beers.csv contre les caractères cassés ("mojibake").
     * Le fichier contenait "KÃ¶lsch" au lieu de "Kölsch" : le texte UTF-8 avait été lu un jour
     * comme du cp1252 (Windows), puis réenregistré en UTF-8 ; chaque lettre accentuée était ainsi
     * devenue deux caractères. Les autres tests ne peuvent pas le voir : ils ne font que compter
     * des bières et chercher des mots-clés ASCII (IPA, RZEN...), qui ne changent pas.
     * Ce test échoue si le fichier cassé revient (par ex. copié depuis le dépôt de JT) ou si le CSV
     * est de nouveau lu sans UTF-8 (par ex. le new FileReader(file) d'origine de JT, qui utilise
     * l'encodage du système - cp1252 sous Windows).
     */
    @Test
    void convertCSVKeepsAccentedCharacters() {
        ClassPathResource resource = new ClassPathResource("csvdata/beers.csv");

        List<BeerCSVRecord> recs = beerCsvService.convertCSV(resource);

        assertThat(recs).extracting(BeerCSVRecord::getStyle)
                .contains("Kölsch", "Märzen / Oktoberfest");
        assertThat(recs).extracting(BeerCSVRecord::getBeer)
                .contains("Garce Selé");
        assertThat(recs).allSatisfy(beerCSVRecord ->
                assertThat(beerCSVRecord.getBeer() + " / " + beerCSVRecord.getStyle())
                        .doesNotContain("Ã", "Â", "â€"));
    }
}