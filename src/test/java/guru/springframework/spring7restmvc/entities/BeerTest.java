package guru.springframework.spring7restmvc.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Pierrot on 12-10-2026
 */
class BeerTest {

    @Test
    void testRemoveCategoryUnlinksBothSides() {
        Beer beer = Beer.builder().beerName("Test Beer").build();
        Category category = Category.builder().description("Ales").build();

        beer.addCategory(category);
        assertThat(beer.getCategories()).containsExactly(category);
        assertThat(category.getBeers()).containsExactly(beer);

        beer.removeCategory(category);

        assertThat(beer.getCategories()).isEmpty();
        assertThat(category.getBeers()).isEmpty();
    }
}
