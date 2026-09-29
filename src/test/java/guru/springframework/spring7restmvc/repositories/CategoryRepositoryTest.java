package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.bootstrap.BootstrapData;
import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.entities.Category;
import guru.springframework.spring7restmvc.services.BeerCsvServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({BootstrapData.class, BeerCsvServiceImpl.class})
class CategoryRepositoryTest {
    
    @Autowired
    CategoryRepository categoryRepository;
    
    @Autowired
    BeerRepository beerRepository;

    Beer beer;

    @BeforeEach
    void setUp() {
        beer = beerRepository.findAll().getFirst();
    }

    @Test
    void testAddCategory() {
        Category category = Category.builder()
                .description("Test Category")
                .build();

        Category savedCategory = categoryRepository.save(category);

        beer.addCategory(savedCategory);

        Beer savedBeer = beerRepository.save(beer);

        assertThat(savedBeer.getCategories()).contains(savedCategory);
        assertThat(savedCategory.getBeers()).contains(savedBeer);
    }
}