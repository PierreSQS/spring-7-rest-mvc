package guru.springframework.spring7restmvc.controller;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modified by Pierrot on 08-11-2026
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class BeerControllerIT {
    @Autowired
    BeerRepository beerRepository;

    @Autowired
    BeerMapper beerMapper;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManager entityManager;

    @Disabled("just for demo purposes")
    @Test
    void testUpdateBeerBadVersion() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);

        beerDTO.setBeerName("Updated Name");

        MvcResult result = mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isNoContent())
                .andReturn();

        log.info(result.getResponse().getContentAsString());

        beerDTO.setBeerName("Updated Name 2");

        MvcResult result2 = mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isNoContent())
                .andReturn();

        log.info(String.valueOf(result2.getResponse().getStatus()));
    }

    @Transactional
    @Test
    void testPostBeerNameTooLong() throws Exception {
        BeerDTO beerDTO = BeerDTO.builder()
                .beerName("A".repeat(51))
                .upc("123456789012")
                .beerStyle(BeerStyle.PALE_ALE)
                .price(new BigDecimal("9.99"))
                .build();

        MvcResult result = mockMvc.perform(post(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].beerName").value("The Beer Name must be maximum 50 characters"))
                .andReturn();

        log.info("status: {}, body: {}", result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    @Test
    void tesListBeersByStyleAndNameShowInventoryTruePage2() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "true")
                        .queryParam("pageNumber", "2")
                        .queryParam("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(50)))
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.notNullValue()));
    }

    @Test
    void tesListBeersByStyleAndNameShowInventoryTrue() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "true")
                        .queryParam("pageSize", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(324)))
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.notNullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5"})
    void testListBeersPageSizeBelowOne(String pageSize) throws Exception {
        // a pageSize below 1 is treated like a missing pageSize: the default page of 25 beers (#21)
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("pageSize", pageSize))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(25)));
    }

    @Test
    void tesListBeersByStyleAndNameShowInventoryFalse() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "false")
                        .queryParam("pageSize", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(324)))
                .andExpect(jsonPath("$.content[0].quantityOnHand").value(IsNull.nullValue()));
    }

    @Transactional
    @Test
    void testListBeersShowInventoryFalseKeepsStockInDatabase() throws Exception {
        // showInventory=false must hide the stock in the response only, never in the database (#22).
        // The test transaction surrounds the request, so a changed entity would be written at flush.
        MvcResult result = mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("showInventory", "false"))
                .andExpect(status().isOk())
                .andReturn();

        // the first beer of the page the client received
        UUID firstBeerId = UUID.fromString(jsonMapper.readTree(result.getResponse().getContentAsString())
                .get("content").get(0).get("id").asString());

        // write pending changes, then re-read from the database instead of Hibernate's cache
        entityManager.flush();
        entityManager.clear();

        Beer firstBeer = beerRepository.findById(firstBeerId).orElseThrow();

        assertThat(firstBeer.getQuantityOnHand()).isNotNull();
    }

    @Test
    void tesListBeersByStyleAndName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("pageSize", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(324)));
    }

    @Test
    void testNoAuth() throws Exception {
        //Test No Auth
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("pageSize", "800"))
                .andExpect(status().isUnauthorized());

    }

    @Test
    void tesListBeersByStyle() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("pageSize", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(572)));
    }

    @Test
    void tesListBeersByName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("beerName", "IPA")
                        .queryParam("pageSize", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(336)));
    }

    @Test
    void testPatchBeerNotFound() throws Exception {
        UUID beerId = UUID.randomUUID();
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "Updated Name");

        mockMvc.perform(patch(BeerController.BEER_PATH_ID, beerId)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerMap)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testPatchBeerBadName() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "New Name 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");

        MvcResult result = mockMvc.perform(patch(BeerController.BEER_PATH_ID, beer.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].beerName").value("Beer Name must be maximum 50 characters"))
                .andReturn();

        log.info(result.getResponse().getContentAsString());
    }

    @Test
    void testDeleteByIDNotFound() throws Exception {
        mockMvc.perform(delete(BeerController.BEER_PATH_ID, UUID.randomUUID())
                        .with(BeerControllerTest.jwtRequestPostProcessor))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void deleteByIdFound() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        mockMvc.perform(delete(BeerController.BEER_PATH_ID, beer.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor))
                .andExpect(status().isNoContent());

        // write the delete, then forget Hibernate's memory, so findById reads the real table (R6)
        entityManager.flush();
        entityManager.clear();

        assertThat(beerRepository.findById(beer.getId())).isEmpty();
    }

    @Test
    void testUpdateNotFound() throws Exception {
        // the PUT is @Validated: an empty DTO would answer 400, and the test would prove nothing
        BeerDTO beerDTO = BeerDTO.builder()
                .beerName("Unknown Beer")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("123456789012")
                .price(new BigDecimal("9.99"))
                .build();

        mockMvc.perform(put(BeerController.BEER_PATH_ID, UUID.randomUUID())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isNotFound());
    }

    @Transactional
    @Test
    void updateExistingBeer() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();
        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);
        beerDTO.setId(null);
        beerDTO.setVersion(null);
        final String beerName = "UPDATED";
        beerDTO.setBeerName(beerName);

        mockMvc.perform(put(BeerController.BEER_PATH_ID, beer.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        Beer updatedBeer = beerRepository.findById(beer.getId()).orElseThrow();
        assertThat(updatedBeer.getBeerName()).isEqualTo(beerName);
    }

    // the POST request carries its own id and version: the API must ignore both (issue #15, R1)
    @Transactional
    @Test
    void saveNewBeerTest() throws Exception {
        // 1. a valid DTO (the POST is @Validated), carrying a chosen id and version
        BeerDTO beerDTO = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(42)
                .beerName("New Beer")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("123456789012")
                .price(new BigDecimal("9.99"))
                .build();

        // 2. POST it as JSON, expect 201 Created and a Location header
        MvcResult result = mockMvc.perform(post(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        entityManager.flush();
        entityManager.clear();

        // 3. read the id of the new beer from the Location header
        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        UUID savedUUID = UUID.fromString(location.split("/")[4]);

        // 4. the new id is not the one sent, and the beer read back has the first version (0)
        Beer savedBeer = beerRepository.findById(savedUUID).orElseThrow();
        assertThat(savedBeer.getId()).isNotEqualTo(beerDTO.getId());
        assertThat(savedBeer.getVersion()).isZero();
    }

    @Test
    void testBeerIdNotFound() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH_ID, UUID.randomUUID())
                        .with(BeerControllerTest.jwtRequestPostProcessor))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetById() throws Exception {
        Beer beer = beerRepository.findAll().getFirst();

        mockMvc.perform(get(BeerController.BEER_PATH_ID, beer.getId())
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(beer.getId().toString())))
                .andExpect(jsonPath("$.beerName", is(beer.getBeerName())));
    }

    @Test
    void testListBeers() throws Exception {
        // the service caps pageSize at 1000, whatever the client asks
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor)
                        .queryParam("pageSize", "2413"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1000)));
    }

    @Transactional
    @Test
    void testEmptyList() throws Exception {
        beerRepository.deleteAll();

        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(BeerControllerTest.jwtRequestPostProcessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(0)));
    }
}






