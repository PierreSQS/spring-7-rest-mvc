package guru.springframework.spring7restmvc.controller;

import com.jayway.jsonpath.JsonPath;
import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.mappers.BeerMapper;
import guru.springframework.spring7restmvc.model.BeerDTO;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.repositories.BeerRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every test sends a real HTTP request through MockMvc, so it also goes through the security
 * filter chain, @Validated, the CustomErrorController and the JSON conversion - a direct call to
 * the controller method would skip all of them.
 * Tests that change data are @Transactional (rolled back); before reading back what a request
 * wrote, they flush and clear, so the check reads the database and not Hibernate's cache.
 * <p>
 * Modified by Pierrot on 19-10-2026
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class BeerControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    BeerMapper beerMapper;

    @Autowired
    EntityManager entityManager;

    @Test
    void testRequestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET list ----------

    @Test
    void testListBeers() throws Exception {
        // asks for every beer; the page size is capped at 1000
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("pageSize", "2413")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(1000)));
    }

    // pageSize 0 used to reach PageRequest.of(..., 0) and fail with a 500; it now falls back to the default size
    @Test
    void testListBeersPageSizeZeroFallsBackToDefault() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("pageSize", "0")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(25)));
    }

    @Transactional
    @Test
    void testListBeersEmpty() throws Exception {
        beerRepository.deleteAll();

        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(0)));
    }

    @Test
    void testListBeersByName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(336)));
    }

    @Test
    void testListBeersByStyle() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(572)));
    }

    @Test
    void testListBeersByStyleAndName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(324)));
    }

    @Test
    void testListBeersByStyleAndNameShowInventoryTrue() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "true")
                        .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(324)))
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.notNullValue()));
    }

    @Test
    void testListBeersByStyleAndNameShowInventoryFalse() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "false")
                        .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(324)))
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.nullValue()));
    }

    @Test
    void testListBeersByStyleAndNameShowInventoryTruePage2() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "true")
                        .queryParam("pageNumber", "2")
                        .queryParam("pageSize", "50")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(50)))
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.notNullValue()));
    }

    // listBeers must only READ: hiding the stock must not change the database,
    // even when the request runs inside a transaction (dirty checking)
    @Transactional
    @Test
    void testListBeersWithoutInventoryKeepsTheStockInTheDatabase() throws Exception {
        MvcResult result = mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("showInventory", "false")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.nullValue()))
                .andReturn();

        // write now whatever Hibernate noted as changed, then forget the cached entities
        entityManager.flush();
        entityManager.clear();

        String firstId = JsonPath.read(result.getResponse().getContentAsString(), "$.content[0].id");
        assertThat(beerRepository.findById(UUID.fromString(firstId)))
                .get()
                .extracting(Beer::getQuantityOnHand)
                .isNotNull();
    }

    // ---------- GET by id ----------

    @Test
    void testGetById() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        mockMvc.perform(get(BeerController.BEER_PATH_ID, beer.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(beer.getId().toString())))
                .andExpect(jsonPath("$.beerName", is(beer.getBeerName())));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH_ID, UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    // ---------- POST ----------

    @Transactional
    @Test
    void testCreate() throws Exception {
        BeerDTO beerDTO = BeerDTO.builder()
                .beerName("New Beer")
                .beerStyle(BeerStyle.IPA)
                .upc("123456")
                .price(new BigDecimal("12.99"))
                .build();

        MvcResult result = mockMvc.perform(post(BeerController.BEER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO))
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        entityManager.flush();
        entityManager.clear();

        assertThat(beerRepository.findById(idFromLocation(result)))
                .get()
                .extracting(Beer::getBeerName)
                .isEqualTo("New Beer");
    }

    // the old direct call controller.handlePost(...) accepted a beer with only a name: it skipped
    // @Validated, and the rolled-back test never flushed, so the entity constraints never ran either
    @Test
    void testCreateBeerWithOnlyANameIsRejected() throws Exception {
        BeerDTO beerDTO = BeerDTO.builder()
                .beerName("New Beer")
                .build();

        mockMvc.perform(post(BeerController.BEER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO))
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                // beerStyle, price and upc are missing (upc breaks both @NotNull and @NotBlank)
                .andExpect(jsonPath("$.length()", is(4)));
    }

    // ---------- PUT ----------

    @Transactional
    @Test
    void testUpdateById() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();
        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);
        beerDTO.setId(null);
        beerDTO.setVersion(null);
        beerDTO.setBeerName("UPDATED");

        mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO))
                        .with(jwt()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(beerRepository.findById(beer.getId()))
                .get()
                .extracting(Beer::getBeerName)
                .isEqualTo("UPDATED");
    }

    // a PUT replaces the whole beer: a field missing from the body must become null,
    // unlike a PATCH, which keeps it (see BeerMapper.updateBeerFromDto / patchBeerFromDto)
    @Transactional
    @Test
    void testUpdateByIdWithoutQuantityClearsIt() throws Exception {
        // take a beer that HAS a quantity - otherwise "becomes null" would prove nothing
        Beer beer = beerRepository.findAll().stream()
                .filter(foundBeer -> foundBeer.getQuantityOnHand() != null)
                .findFirst().orElseThrow();
        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);

        // a PUT request that doesn't include the quantity
        beerDTO.setQuantityOnHand(null);

        mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO))
                        .with(jwt()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        // the quantity must now be null, because a PUT replaces the whole beer;
        // with the PATCH mapper (IGNORE) it would still hold its old value - that is the bug this test catches
        assertThat(beerRepository.findById(beer.getId()))
                .get()
                .extracting(Beer::getQuantityOnHand)
                .isNull();
    }

    @Test
    void testUpdateByIdNotFound() throws Exception {
        // a valid body, so the request passes @Validated and really reaches the "not found" check
        BeerDTO beerDTO = beerMapper.beerToBeerDto(beerRepository.findAll().getFirst());
        beerDTO.setId(null);
        beerDTO.setVersion(null);

        mockMvc.perform(put(BeerController.BEER_PATH_ID, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO))
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Disabled("just for demo purposes")
    @Test
    void testUpdateBeerBadVersion() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);

        beerDTO.setBeerName("Updated Name");

        MvcResult result = mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO))
                        .with(jwt()))
                .andExpect(status().isNoContent())
                .andReturn();

        log.info("First PUT response: {}", result.getResponse().getContentAsString());

        beerDTO.setBeerName("Updated Name 2");

        MvcResult result2 = mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO))
                        .with(jwt()))
                .andExpect(status().isNoContent())
                .andReturn();

        log.info("Second PUT status: {}", result2.getResponse().getStatus());
    }

    // ---------- PATCH ----------

    @Test
    void testPatchBeerBadName() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "New Name 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");

        mockMvc.perform(patch(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerMap))
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                // PATCH has no @Validated: the entity rejects the name at commit, and the
                // client must still learn which field is wrong and why
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].beerName", is("Beer name must be at most 50 characters")));
    }

    // JT's if-chain skipped an empty name and answered 204; the MapStruct patch copies it,
    // @NotBlank rejects it on save, so the client gets 400 and the beer keeps its name
    @Test
    void testPatchBeerBlankName() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "");

        mockMvc.perform(patch(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerMap))
                        .with(jwt()))
                .andExpect(status().isBadRequest());

        assertThat(beerRepository.findById(beer.getId()))
                .get()
                .extracting(Beer::getBeerName)
                .isEqualTo(beer.getBeerName());
    }

    // ---------- DELETE ----------

    @Transactional
    @Test
    void testDeleteById() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        mockMvc.perform(delete(BeerController.BEER_PATH_ID, beer.getId())
                        .with(jwt()))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(beerRepository.findById(beer.getId())).isEmpty();
    }

    @Test
    void testDeleteByIdNotFound() throws Exception {
        mockMvc.perform(delete(BeerController.BEER_PATH_ID, UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    /**
     * Reads the id of the created beer from the end of the Location header,
     * e.g. {@code /api/v1/beer/<uuid>}.
     */
    private UUID idFromLocation(MvcResult result) {
        String location = Objects.requireNonNull(result.getResponse().getHeader("Location"),
                "the response has no Location header");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
