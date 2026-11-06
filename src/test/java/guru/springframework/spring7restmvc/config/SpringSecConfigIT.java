package guru.springframework.spring7restmvc.config;

import guru.springframework.spring7restmvc.controller.BeerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by Pierrot on 02-11-2026
 * Modified by Pierrot on 06-11-2026
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpringSecConfigIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void testApiDocsOpenWithoutToken() throws Exception {
        // GET /v3/api-docs without a token -> 200 with the OpenAPI JSON (info is mandatory in OpenAPI)
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.info.title").value("Beer and Customer API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void testApiDocsShowBuildVersion(@Autowired BuildProperties buildProperties) throws Exception {
        // GET /v3/api-docs -> info.version stays the API version "v1", the build version is in info.x-build-version (#28)
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.info['x-build-version']").value(buildProperties.getVersion()));
    }

    @Test
    void testApiDocsDeclareBearerJwt() throws Exception {
        // GET /v3/api-docs -> a Bearer JWT security scheme, required by every operation (Authorize button, #27)
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.description",
                        containsString("http://localhost:9000/oauth2/token")))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }

    @Test
    void testBeerListNeedsToken() throws Exception {
        // GET /api/v1/beer without a token -> 401 with WWW-Authenticate: Bearer
        mockMvc.perform(get(BeerController.BEER_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        containsString("Bearer")));
    }

    @Test
    void testSwaggerUiOpenWithoutToken() throws Exception {
        // GET /swagger-ui/index.html without a token -> 200
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testSwaggerUiShortUrlRedirects() throws Exception {
        // GET /swagger-ui.html without a token -> 302 to /swagger-ui/index.html
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
    }
}
