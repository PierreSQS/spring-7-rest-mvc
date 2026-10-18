package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.entities.Category;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 18-10-2026
 */
@Slf4j
@SpringBootTest
class CategoryRepositoryTest {
    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    EntityManager entityManager;

    Beer testBeer;

    @BeforeEach
    void setUp() {
        testBeer = beerRepository.findAll().getFirst();
    }

    @Transactional
    @Test
    void testAddCategory() {
        Category savedCat = categoryRepository.save(Category.builder()
                        .description("Ales")
                .build());

        testBeer.addCategory(savedCat);
        Beer saveBeer = beerRepository.saveAndFlush(testBeer);
        log.info("Saved beer with category: {}", saveBeer.getBeerName());

        // forget the objects in memory, so the next read really comes from the database
        entityManager.clear();

        // the link is in beer_category, exactly once
        Beer reloadedBeer = beerRepository.findById(testBeer.getId()).orElseThrow();
        assertThat(reloadedBeer.getCategories())
                .extracting(Category::getId)
                .containsExactly(savedCat.getId());
    }
}




