package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.entities.Category;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 28-10-2026
 */
@SpringBootTest
class CategoryRepositoryTest {
    public static final String COUNT_ENTRIES_FROM_BEER_CATEGORIES = "select count(*) from beer_category where beer_id = :beerId and category_id = :categoryId";
    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    BeerRepository beerRepository;
    Beer testBeer;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void setUp() {
        testBeer = beerRepository.findAll().getFirst();
    }

    // @Transactional: rolled back at the end, and keeps the persistence context open for the lazy collections
    @Transactional
    @Test
    void testAddCategory() {
        Category savedCat = categoryRepository.save(Category.builder()
                        .description("Ales")
                .build());

        // 1. link both sides in Java, then flush: the INSERT into beer_category really reaches the database
        //    (without the flush, the rollback would discard it unseen)
        testBeer.addCategory(savedCat);
        Beer savedBeer = beerRepository.saveAndFlush(testBeer);

        // 2. the join table has no entity: count its rows in SQL - exactly 1, not 2 (only Beer owns the relation)
        Long beerCategoriesCnt = jdbcClient.sql(COUNT_ENTRIES_FROM_BEER_CATEGORIES)
                        .param("beerId", savedBeer.getId())
                        .param("categoryId", savedCat.getId())
                        .query(Long.class)
                        .single();

        assertThat(beerCategoriesCnt).isEqualTo(1L);

        // 3. empty the persistence context, otherwise findById returns the objects already in memory (R6)
        entityManager.clear();

        // 4. re-read both sides from the database and check that each one finds the other, by id
        //    (Category has no equals: the re-read objects are new Java objects)
        Beer beerFromDB = beerRepository.findById(savedBeer.getId()).orElseThrow();

        Category categoryFromDB = categoryRepository.findById(savedCat.getId()).orElseThrow();

        assertThat(beerFromDB.getCategories())
                .extracting(Category::getId)
                .contains(categoryFromDB.getId());

        assertThat(categoryFromDB.getBeers())
                .extracting(Beer::getId)
                .contains(beerFromDB.getId());

    }
}



