package guru.springframework.spring7restmvc.controller;

import guru.springframework.spring7restmvc.entities.Beer;
import guru.springframework.spring7restmvc.mappers.BeerMapper;
import guru.springframework.spring7restmvc.model.BeerDTO;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.repositories.BeerRepository;
import guru.springframework.spring7restmvc.services.BeerCsvService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 22-09-2026
 */
@SpringBootTest
@AutoConfigureMockMvc
class BeerControllerIT {

    /** The beers BootstrapData writes by hand, before it loads the CSV. */
    private static final int HANDWRITTEN_BEERS = 3;

    /**
     * Search hits in the seeded data. They are higher than in JT's lecture because the CSV encoding
     * was repaired and the styles are mapped by keyword, see {@code BootstrapData.beerStyleOf}.
     */
    private static final int BEERS_NAMED_IPA = 336;
    private static final int BEERS_OF_STYLE_IPA = 572;
    private static final int BEERS_NAMED_IPA_OF_STYLE_IPA = 324;

    @Autowired
    BeerController beerController;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    BeerMapper beerMapper;

    @Autowired
    BeerCsvService beerCsvService;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    void testListBeersByStyleAndNameShowInventoryTrue() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(BEERS_NAMED_IPA_OF_STYLE_IPA))
                .andExpect(jsonPath("$.[0].quantityOnHand").isNotEmpty());
    }

    @Test
    void testListBeersByStyleAndNameShowInventoryFalse() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name())
                        .queryParam("showInventory", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(BEERS_NAMED_IPA_OF_STYLE_IPA))
                .andExpect(jsonPath("$.[0].quantityOnHand").isEmpty());
    }

    @Test
    void testListBeersByStyleAndName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA")
                        .queryParam("beerStyle", BeerStyle.IPA.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(BEERS_NAMED_IPA_OF_STYLE_IPA));
    }

    @Test
    void testListBeersByStyle() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerStyle", BeerStyle.IPA.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(BEERS_OF_STYLE_IPA));
    }

    @Test
    void testListBeersByName() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH)
                        .queryParam("beerName", "IPA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(BEERS_NAMED_IPA));
    }

    @Test
    void testPatchBeerNameTooLong() throws Exception {
        Beer beer = getFirstBeer();

        // @Size(max = 50) on the entity and the column
        Map<String, Object> beerMap = Map.of("beerName", "N".repeat(51));

        mockMvc.perform(patch(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerMap)))
                .andExpect(status().isBadRequest());

        assertThat(getFirstBeer().getBeerName()).isEqualTo(beer.getBeerName());
    }

    @Test
    void testPatchBeerBlankName() throws Exception {
        Beer beer = getFirstBeer();

        // @NotBlank on the entity: an empty name is rejected instead of silently ignored
        mockMvc.perform(patch(BeerController.BEER_PATH_ID, beer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("beerName", " "))))
                .andExpect(status().isBadRequest());

        assertThat(getFirstBeer().getBeerName()).isEqualTo(beer.getBeerName());
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
        Beer beer = getFirstBeer();

        ResponseEntity<Void> responseEntity = beerController.deleteById(beer.getId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

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
        Beer beer = getFirstBeer();
        BeerDTO beerDTO = beerMapper.beerToBeerDto(beer);
        beerDTO.setId(null);
        beerDTO.setVersion(null);
        final String beerName = "UPDATED";
        beerDTO.setBeerName(beerName);

        ResponseEntity<Void> responseEntity = beerController.updateById(beer.getId(), beerDTO);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Beer updatedBeer = beerRepository.findById(beer.getId()).orElseThrow();
        assertThat(updatedBeer.getBeerName()).isEqualTo(beerName);
    }

    @Transactional
    @Test
    void saveNewBeerTest() {
        BeerDTO beerDTO = BeerDTO.builder()
                .beerName("New Beer")
                .build();

        ResponseEntity<Void> responseEntity = beerController.handlePost(beerDTO);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = responseEntity.getHeaders().getLocation();
        assertThat(location).isNotNull();

        // the id is whatever follows BEER_PATH in the Location header.
        // Counting the parts of the URL instead would break as soon as the path changes.
        UUID savedUUID = UUID.fromString(
                location.getPath().substring(BeerController.BEER_PATH.length() + 1));

        assertThat(beerRepository.findById(savedUUID)).isPresent();
    }

    @Test
    void testBeerIdNotFound() {
        UUID beerId = UUID.randomUUID();

        assertThatThrownBy(() -> beerController.getBeerById(beerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetById() {
        Beer beer = getFirstBeer();

        BeerDTO dto = beerController.getBeerById(beer.getId());

        assertThat(dto.getId()).isEqualTo(beer.getId());
    }

    @Test
    void testListBeers() {
        int csvRecords = beerCsvService.convertCSV(new ClassPathResource("csvdata/beers.csv")).size();

        List<BeerDTO> dtos = beerController.listBeers(null, null, false);

        // every CSV record becomes a beer, on top of the three written by hand
        assertThat(dtos).hasSize(HANDWRITTEN_BEERS + csvRecords);
    }

    @Transactional
    @Test
    void testEmptyList() {
        beerRepository.deleteAll();
        List<BeerDTO> dtos = beerController.listBeers(null, null, false);

        assertThat(dtos).isEmpty();
    }

    private Beer getFirstBeer() {
        return beerRepository.findAll().getFirst();
    }
}
