package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.MySqlContainerBase;
import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 18-09-2026
 */
@DataJpaTest
// the JPA slice only loads JPA components, so BootstrapData has to be imported explicitly
// once imported, it runs as a CommandLineRunner when the test context starts and seeds the data
// BeerCsvServiceImpl comes with it: BootstrapData needs it to load the CSV, and a @Service is
// not part of the JPA slice either
@Import({BootstrapData.class, BeerCsvServiceImpl.class})
class MySqlIT extends MySqlContainerBase {

    @Autowired
    BeerRepository beerRepository;

    @Test
    void testListBeers() {
        List<Beer> beers = beerRepository.findAll();

        assertThat(beers).isNotEmpty();
    }
}
