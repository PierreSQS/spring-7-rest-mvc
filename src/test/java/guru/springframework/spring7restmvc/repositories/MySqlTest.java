package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.MySqlContainerBase;
import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.Beer;
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
@Import(BootstrapData.class)
class MySqlTest extends MySqlContainerBase {

    @Autowired
    BeerRepository beerRepository;

    @Test
    void testListBeers() {
        List<Beer> beers = beerRepository.findAll();

        assertThat(beers).isNotEmpty();
    }
}
