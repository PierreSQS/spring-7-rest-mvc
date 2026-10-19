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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modified by Pierrot on 19-10-2026
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class BeerControllerIT {
    @Autowired
    BeerController beerController;

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

    @Test
    void tesListBeersByStyleAndNameShowInventoryTruePage2() throws Exception {
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

    @Test
    void tesListBeersByStyleAndNameShowInventoryTrue() throws Exception {
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
    void tesListBeersByStyleAndNameShowInventoryFalse() throws Exception {
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
    void tesListBeersByStyleAndName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(324)));
    }

    @Test
    void tesListBeersByStyle() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(572)));
    }

    @Test
    void tesListBeersByName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                .queryParam("beerName", "IPA")
                .queryParam("pageSize", "800")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()", is(336)));
    }

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

    @Test
    void testDeleteByIDNotFound() {
        UUID beerId = UUID.randomUUID();
        assertThatThrownBy(() -> beerController.deleteById(beerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Transactional
    @Test
    void deleteByIdFound() {
        Beer beer = beerRepository.findAll().getFirst();

        ResponseEntity<Void> responseEntity = beerController.deleteById(beer.getId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        assertThat(beerRepository.findById(beer.getId())).isEmpty();
    }

    @Test
    void testUpdateNotFound() {
        UUID beerId = UUID.randomUUID();
        BeerDTO beerDTO = BeerDTO.builder().build();

        assertThatThrownBy(() -> beerController.updateById(beerId, beerDTO))
                .isInstanceOf(NotFoundException.class);
    }

    @Transactional
    @Test
    void updateExistingBeer() {
        Beer beer = beerRepository.findAll().getFirst();
        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);
        beerDTO.setId(null);
        beerDTO.setVersion(null);
        final String beerName = "UPDATED";
        beerDTO.setBeerName(beerName);

        ResponseEntity<Void> responseEntity = beerController.updateById(beer.getId(), beerDTO);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        Beer updatedBeer = beerRepository.findById(beer.getId()).get();
        assertThat(updatedBeer.getBeerName()).isEqualTo(beerName);
    }

    // a PUT replaces the whole beer: a field missing from the body must become null,
    // unlike a PATCH, which keeps it (see BeerMapper.updateBeerFromDto / patchBeerFromDto)
    @Transactional
    @Test
    void updateExistingBeerWithoutQuantityClearsIt() {
        // take a beer that HAS a quantity - otherwise "becomes null" would prove nothing
        Beer beer = beerRepository.findAll().stream()
                .filter(foundBeer -> foundBeer.getQuantityOnHand() != null)
                .findFirst().orElseThrow();
        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);

        // set quantityOnHand to null to simulate a PUT request that doesn't include it
        beerDTO.setQuantityOnHand(null);

        // the PUT itself succeeds: 204 No Content
        ResponseEntity<Void> responseEntity = beerController.updateById(beer.getId(), beerDTO);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(204));

        // reload the beer: the quantity must now be null, because a PUT replaces the whole beer;
        // with the PATCH mapper (IGNORE) it would still hold its old value - that is the bug this test catches
        assertThat(beerRepository.findById(beer.getId()))
                .get()
                .extracting(Beer::getQuantityOnHand)
                .isNull();
    }

    @Transactional
    @Test
    void saveNewBeerTest() {
        BeerDTO beerDTO = BeerDTO.builder()
                .beerName("New Beer")
                .build();

        ResponseEntity<Void> responseEntity = beerController.handlePost(beerDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
        assertThat(responseEntity.getHeaders().getLocation()).isNotNull();

        String[] locationUUID = responseEntity.getHeaders().getLocation().getPath().split("/");
        UUID savedUUID = UUID.fromString(locationUUID[4]);

        Beer beer = beerRepository.findById(savedUUID).get();
        assertThat(beer).isNotNull();
    }

    @Test
    void testBeerIdNotFound() {
        UUID beerId = UUID.randomUUID();

        assertThatThrownBy(() -> beerController.getBeerById(beerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetById() {
        Beer beer = beerRepository.findAll().getFirst();

        BeerDTO dto = beerController.getBeerById(beer.getId());

        assertThat(dto).isNotNull();
    }

    @Test
    void testListBeers() {
        Page<BeerDTO> dtos = beerController.listBeers(null, null, false, 1, 2413);

        assertThat(dtos.getContent()).hasSize(1000);
    }

    // listBeers must only READ: hiding the stock must not change the database,
    // even when the call runs inside a transaction (dirty checking)
    @Transactional
    @Test
    void listBeersWithoutInventoryKeepsTheStockInTheDatabase() {
        Page<BeerDTO> dtos = beerController.listBeers(null, null, false, 1, 25);

        // write now whatever Hibernate noted as changed, then forget the cached entities
        entityManager.flush();
        entityManager.clear();

        BeerDTO first = dtos.getContent().getFirst();
        assertThat(beerRepository.findById(first.getId()))
                .get()
                .extracting(Beer::getQuantityOnHand)
                .isNotNull();
    }

    // pageSize 0 used to reach PageRequest.of(..., 0) and fail with a 500; it now falls back to the default size
    @Test
    void testListBeersPageSizeZeroFallsBackToDefault() {
        Page<BeerDTO> dtos = beerController.listBeers(null, null, false, 1, 0);

        assertThat(dtos.getContent()).hasSize(25);
    }

    @Transactional
    @Test
    void testEmptyList() {
        beerRepository.deleteAll();
        Page<BeerDTO> dtos = beerController.listBeers(null, null, false, 1, 25);

        assertThat(dtos.getContent()).isEmpty();
    }
}







