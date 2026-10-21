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
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 21-10-2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {
    private final BeerRepository beerRepository;
    private final CustomerRepository customerRepository;
    private final BeerCsvService beerCsvService;

    @Transactional
    @Override
    public void run(String @NonNull ... args) {
        loadBeerData();
        loadCsvData();
        loadCustomerData();
    }

    private void loadCsvData() {
        if (beerRepository.count() < 10){
            ClassPathResource resource = new ClassPathResource("csvdata/beers.csv");

            List<BeerCSVRecord> recs = beerCsvService.convertCSV(resource);

            recs.forEach(beerCSVRecord -> beerRepository.save(Beer.builder()
                            .beerName(StringUtils.abbreviate(beerCSVRecord.getBeer(), 50))
                            .beerStyle(beerStyleOf(beerCSVRecord.getStyle()))
                            .price(BigDecimal.TEN)
                            .upc(beerCSVRecord.getRow().toString())
                            .quantityOnHand(beerCSVRecord.getCount())
                    .build()));

            log.info("### loaded {} Beers from CSV-File ###", recs.size());
            log.info("### total Beers loaded: {} ###", beerRepository.count());
        } else {
            log.info("### More than 10 Beers present. CSV-Data loading skipped!! ###");
        }
    }

    /**
     * Maps the free-text style of the CSV (e.g. "American IPA") to a {@link BeerStyle} by keyword.
     * The order matters: "American Pale Wheat Ale" must become WHEAT and
     * "English India Pale Ale (IPA)" IPA before the "PALE ALE" rule can catch them.
     * Styles without a known keyword are almost all ales, so ALE is the default.
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

            log.info("### loaded {} Beers manually ###", beerRepository.count());
        } else {
            log.info("### Beers are present in the DB. Manual Data loading skipped");
        }

    }

    private void loadCustomerData() {

        if (customerRepository.count() == 0) {
            Customer customer1 = Customer.builder()
                    .name("Customer 1")
                    .build();

            Customer customer2 = Customer.builder()
                    .name("Customer 2")
                    .build();

            Customer customer3 = Customer.builder()
                    .name("Customer 3")
                    .build();

            customerRepository.saveAll(List.of(customer1, customer2, customer3));

            log.info("### loaded {} customers manually ###", customerRepository.count());
        } else {
            log.info("### Customers are present in the DB. Data loading skipped");
        }

    }


}
