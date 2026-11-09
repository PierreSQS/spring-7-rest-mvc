package guru.springframework.spring7restmvc.controller;

import guru.springframework.spring7restmvc.config.SpringSecConfig;
import guru.springframework.spring7restmvc.model.BeerDTO;
import guru.springframework.spring7restmvc.model.BeerStyle;
import guru.springframework.spring7restmvc.services.BeerService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modified by Pierrot on 09-11-2026
 */
@Slf4j
@WebMvcTest(BeerController.class)
@Import(SpringSecConfig.class)
@ExtendWith(MockitoExtension.class)
class BeerControllerTest {

    @Autowired
    MockMvc mockMvc;

    // #25: the MockMvcTester bean comes from MockMvcTesterConfiguration of spring-boot-webmvc-test
    @Autowired
    MockMvcTester mockMvcTester;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    BeerService beerService;

    // #43: a real "Authorization: Bearer ..." header needs a decoder that accepts it
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Captor
    ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    ArgumentCaptor<BeerDTO> beerArgumentCaptor;

    BeerDTO beerDTO1, beerDTO2, beerDTO3;

    @BeforeEach
    void setUp() {
        beerDTO1 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Galaxy Cat")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356")
                .price(new BigDecimal("12.99"))
                .quantityOnHand(122)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        beerDTO2 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Crank")
                .beerStyle(BeerStyle.PALE_ALE)
                .upc("12356222")
                .price(new BigDecimal("11.99"))
                .quantityOnHand(392)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        beerDTO3 = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Sunshine City")
                .beerStyle(BeerStyle.IPA)
                .upc("12356")
                .price(new BigDecimal("13.99"))
                .quantityOnHand(144)
                .createdDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
    }

    // SpringSecConfig only checks that the request is authenticated: no scope, subject or date is read.
    // jwt() also skips the CSRF filter, like a real Bearer token. Without it: PATCH -> 403 (CSRF), GET -> 401.
    public static final SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtRequestPostProcessor = jwt();

    @Test
    void testPatchBeer() throws Exception {
        Map<String, Object> beerMap = new HashMap<>();
        beerMap.put("beerName", "New Name");

        given(beerService.patchBeerById(any(UUID.class), any(BeerDTO.class)))
                .willReturn(Optional.of(beerDTO1));

        mockMvc.perform(patch(BeerController.BEER_PATH_ID, beerDTO1.getId())
                        .with(jwtRequestPostProcessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerMap)))
                .andExpect(status().isNoContent());

        verify(beerService).patchBeerById(uuidArgumentCaptor.capture(), beerArgumentCaptor.capture());

        assertThat(uuidArgumentCaptor.getValue()).isEqualTo(beerDTO1.getId());
        assertThat(beerArgumentCaptor.getValue().getBeerName())
                .isEqualTo(beerMap.get("beerName"));
    }

    @Test
    void testDeleteBeer() throws Exception {
        given(beerService.deleteById(any())).willReturn(true);

        mockMvc.perform(delete(BeerController.BEER_PATH_ID, beerDTO1.getId())
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(beerService).deleteById(uuidArgumentCaptor.capture());

        assertThat(uuidArgumentCaptor.getValue()).isEqualTo(beerDTO1.getId());
    }

    @Test
    void testUpdateBeer() throws Exception {
        given(beerService.updateBeerById(any(), any())).willReturn(Optional.of(beerDTO1));

        mockMvc.perform(put(BeerController.BEER_PATH_ID, beerDTO1.getId())
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO1)))
                .andExpect(status().isNoContent());

        verify(beerService).updateBeerById(any(UUID.class), any(BeerDTO.class));
    }

    @Test
    void testUpdateBeerBlankName() throws Exception {
        beerDTO1.setBeerName("");
        given(beerService.updateBeerById(any(), any())).willReturn(Optional.of(beerDTO1));

        mockMvc.perform(put(BeerController.BEER_PATH_ID, beerDTO1.getId())
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(1)));

    }

    @Test
    void testCreateNewBeer() throws Exception {
        beerDTO1.setVersion(null);
        beerDTO1.setId(null);

        given(beerService.saveNewBeer(any(BeerDTO.class))).willReturn(beerDTO2);

        mockMvc.perform(post(BeerController.BEER_PATH)
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO1)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    // ----- #43: first look at RestTestClient, next to the MockMvc version above. -----
    @Test
    void testCreateNewBeerWithRestTestClient() {
        beerDTO1.setVersion(null);
        beerDTO1.setId(null);

        given(beerService.saveNewBeer(any(BeerDTO.class))).willReturn(beerDTO2);

        given(jwtDecoder.decode("fake-token")).willReturn(
                Jwt.withTokenValue("fake-token").header("alg", "none").claim("sub", "pierrot").build());

        RestTestClient.bindTo(mockMvc).build()
                .post().uri(BeerController.BEER_PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(beerDTO1)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location");
    }

    @Test
    void testCreateBeerNullBeerName() throws Exception {

        BeerDTO beerDTO = BeerDTO.builder().build();

        given(beerService.saveNewBeer(any(BeerDTO.class))).willReturn(beerDTO1);

        MvcResult mvcResult = mockMvc.perform(post(BeerController.BEER_PATH)
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(beerDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", is(4)))
                .andExpect(jsonPath("$..beerName").exists())
                .andExpect(jsonPath("$..beerStyle").exists())
                .andExpect(jsonPath("$..upc").exists())
                .andExpect(jsonPath("$..price").exists())
                .andReturn();

        log.info("the error JSON: {}", mvcResult.getResponse().getContentAsString());
    }

    @Test
    void testListBeers() throws Exception {
        given(beerService.listBeers(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(beerDTO1, beerDTO2, beerDTO3)));

        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()", is(3)));
    }

    // ----- #25: evaluation of MockMvcTester. Reference only: MockMvc stays the standard of this project. -----
    // The two tests below print request and response on purpose, as documentation of the two writings:
    // .andDo(print()) with MockMvc, .debug() with MockMvcTester (same output, no static import needed).

    @Test
    void testListBeersWithMockMvc() throws Exception {
        given(beerService.listBeers(any(), any(), any(), any(), any())).willReturn(twoBeersPage());

        mockMvc.perform(get(BeerController.BEER_PATH)
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].beerName", is("Mango Bobs")))
                .andExpect(jsonPath("$.content[1].beerName", is("Pinball Porter")))
                .andDo(print());
    }

    @Test
    void testListBeersWithMockMvcTester() {
        given(beerService.listBeers(any(), any(), any(), any(), any())).willReturn(twoBeersPage());

        assertThat(mockMvcTester.get().uri(BeerController.BEER_PATH)
                .with(jwtRequestPostProcessor)
                .accept(MediaType.APPLICATION_JSON))
                // prints request and response, like .andDo(print()) with MockMvc
                .debug()
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson()
                .hasPathSatisfying("$.content.length()", value -> assertThat(value).isEqualTo(2))
                .hasPathSatisfying("$.content[0].beerName", value -> assertThat(value).isEqualTo("Mango Bobs"))
                .hasPathSatisfying("$.content[1].beerName", value -> assertThat(value).isEqualTo("Pinball Porter"));
    }

    /**
     * The page of exactly 2 beers both versions of the test work on. Built here, with 2 beers of
     * its own, so the test can assert the body content and not only its length.
     */
    private Page<BeerDTO> twoBeersPage() {
        BeerDTO mangoBobs = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.ALE)
                .upc("0631234200036")
                .quantityOnHand(12)
                .price(new BigDecimal("12.99"))
                .build();

        BeerDTO pinballPorter = BeerDTO.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("Pinball Porter")
                .beerStyle(BeerStyle.PORTER)
                .upc("0083783375213")
                .quantityOnHand(24)
                .price(new BigDecimal("17.99"))
                .build();

        return new PageImpl<>(List.of(mangoBobs, pinballPorter));
    }

    @Test
    void getBeerByIdNotFound() throws Exception {

        given(beerService.getBeerById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(get(BeerController.BEER_PATH_ID, UUID.randomUUID())
                        .with(jwtRequestPostProcessor))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBeerById() throws Exception {
        given(beerService.getBeerById(beerDTO1.getId())).willReturn(Optional.of(beerDTO1));

        mockMvc.perform(get(BeerController.BEER_PATH_ID, beerDTO1.getId())
                        .with(jwtRequestPostProcessor)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(beerDTO1.getId().toString())))
                .andExpect(jsonPath("$.beerName", is(beerDTO1.getBeerName())));
    }
}