package guru.springframework.spring7restmvc.bootstrap;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.entities.Customer;
import guru.springframework.spring7restmvc.model.BeerCSVRecord;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.repositories.BeerRepository;
import guru.springframework.spring7restmvc.repositories.CustomerRepository;
import guru.springframework.spring7restmvc.services.BeerCsvService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 18-09-2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    /** The beer name column of the entity, see {@link Beer#getBeerName()}. */
    private static final int MAX_BEER_NAME_LENGTH = 50;
    private static final String BEERS_CSV = "csvdata/beers.csv";

    private final BeerRepository beerRepository;
    private final CustomerRepository customerRepository;
    private final BeerCsvService beerCsvService;

    @Transactional
    @Override
    public void run(String... args) {
        loadBeerData();
        loadCsvData();
        loadCustomerData();
    }

    private void loadCsvData() {
        if (beerRepository.count() < 10) {
            List<BeerCSVRecord> beerCSVRecords = beerCsvService.convertCSV(new ClassPathResource(BEERS_CSV));

            beerCSVRecords.forEach(beerCSVRecord -> beerRepository.save(Beer.builder()
                    .beerName(truncateBeerName(beerCSVRecord.getBeer()))
                    .beerStyle(beerStyleOf(beerCSVRecord.getStyle()))
                    .price(BigDecimal.TEN)
                    .upc(beerCSVRecord.getRow().toString())
                    .quantityOnHand(beerCSVRecord.getCount())
                    .build()));

            log.info("### {} beers in the DB after loading {}", beerRepository.count(), BEERS_CSV);
        } else {
            log.info("### More than 10 beers are present in the DB. CSV bootstrap skipped");
        }
    }

    /**
     * Classifies one of the 100 style names of the CSV as one of the ten {@link BeerStyle}s, by the
     * first keyword that matches. Order matters: "American Double / Imperial IPA" is an IPA, and
     * "American Pale Wheat Ale" a wheat beer rather than a pale ale.
     * <p>
     * The keywords are ASCII on purpose: {@code RZEN} matches "M&auml;rzen" whatever the file's encoding
     * does to the umlaut, which the dataset used to mangle into {@code MÃ¤rzen}.
     */
    static BeerStyle beerStyleOf(String csvStyle) {
        String style = csvStyle.toUpperCase();

        if (style.contains("IPA")) return BeerStyle.IPA;
        if (style.contains("STOUT")) return BeerStyle.STOUT;
        if (style.contains("PORTER")) return BeerStyle.PORTER;
        if (style.contains("GOSE")) return BeerStyle.GOSE;
        if (style.contains("SAISON") || style.contains("FARMHOUSE")) return BeerStyle.SAISON;
        if (style.contains("WHEAT") || style.contains("WITBIER") || style.contains("WEISS")
                || style.contains("WEIZEN") || style.contains("HEFE")) return BeerStyle.WHEAT;
        if (style.contains("PALE ALE")) return BeerStyle.PALE_ALE;
        if (style.contains("PILSNER") || style.contains("PILSENER") || style.contains("PILS")) return BeerStyle.PILSNER;
        if (style.contains("LAGER") || style.contains("BOCK") || style.contains("RZEN")
                || style.contains("OKTOBERFEST") || style.contains("HELLES") || style.contains("DUNKEL")
                || style.contains("SCHWARZ") || style.contains("VIENNA")) return BeerStyle.LAGER;

        return BeerStyle.ALE;
    }

    /** Two of the 2410 names are longer than the column, which would fail the @Size validation. */
    private static String truncateBeerName(String beerName) {
        return beerName.length() <= MAX_BEER_NAME_LENGTH
                ? beerName
                : beerName.substring(0, MAX_BEER_NAME_LENGTH - 3) + "...";
    }

    private void loadBeerData() {
        if (beerRepository.count() == 0){
            Beer beer1 = Beer.builder()
                    .beerName("Galaxy Cat")
                    .beerStyle(BeerStyle.PALE_ALE)
                    .upc("12356")
                    .price(new BigDecimal("12.99"))
                    .quantityOnHand(122)
                    .build();

            Beer beer2 = Beer.builder()
                    .beerName("Crank")
                    .beerStyle(BeerStyle.PALE_ALE)
                    .upc("12356222")
                    .price(new BigDecimal("11.99"))
                    .quantityOnHand(392)
                    .build();

            Beer beer3 = Beer.builder()
                    .beerName("Sunshine City")
                    .beerStyle(BeerStyle.IPA)
                    .upc("12356")
                    .price(new BigDecimal("13.99"))
                    .quantityOnHand(144)
                    .build();

            beerRepository.save(beer1);
            beerRepository.save(beer2);
            beerRepository.save(beer3);

            log.info("### {} beers loaded into the DB", beerRepository.count());
        } else {
            log.info("### Beers are present in the DB. Bootstrap skipped");
        }

    }

    private void loadCustomerData() {

        if (customerRepository.count() == 0) {
            Customer customer1 = Customer.builder()
                    .name("Customer 1")
                    .version(1)
                    .build();

            Customer customer2 = Customer.builder()
                    .name("Customer 2")
                    .version(1)
                    .build();

            Customer customer3 = Customer.builder()
                    .name("Customer 3")
                    .version(1)
                    .build();

            customerRepository.saveAll(List.of(customer1, customer2, customer3));

            log.info("### {} customers loaded into the DB", customerRepository.count());
        } else {
            log.info("### Customers are present in the DB. Bootstrap skipped");
        }

    }


}
