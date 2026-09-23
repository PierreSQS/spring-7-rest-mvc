package guru.springframework.spring7restmvc.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Pierrot on 23-09-2026.
 * <p>
 * {@code buildPageRequest} is pure translation and touches neither the repository nor the mapper, so
 * the service is built by hand with both dependencies null - no Spring context is needed.
 */
class BeerServiceJPATest {

    private final BeerServiceJPA beerServiceJPA = new BeerServiceJPA(null, null);

    @ParameterizedTest(name = "pageNumber {0} and pageSize {1} become page {2} of size {3}")
    @CsvSource({
            // the API counts pages from 1, Spring Data from 0
            "1,  50, 0,  50",
            "2,  50, 1,  50",
            "7,  10, 6,  10",
            // a page number below 1 has no meaning, so the first page is served
            "0,  50, 0,  50",
            "-3, 50, 0,  50",
            // a size below 1 has no meaning either, so the default size is served
            "2,   0, 1,  25",
            "2,  -5, 1,  25",
            // and nobody pulls the whole table in one request
            "2, 5000, 1, 1000"
    })
    void testBuildPageRequest(Integer pageNumber, Integer pageSize,
                              int expectedPageNumber, int expectedPageSize) {

        PageRequest pageRequest = beerServiceJPA.buildPageRequest(pageNumber, pageSize);

        assertThat(pageRequest.getPageNumber()).isEqualTo(expectedPageNumber);
        assertThat(pageRequest.getPageSize()).isEqualTo(expectedPageSize);
    }

    @Test
    void testBuildPageRequestWithoutParameters() {
        PageRequest pageRequest = beerServiceJPA.buildPageRequest(null, null);

        assertThat(pageRequest.getPageNumber()).isZero();
        assertThat(pageRequest.getPageSize()).isEqualTo(25);
    }
}
