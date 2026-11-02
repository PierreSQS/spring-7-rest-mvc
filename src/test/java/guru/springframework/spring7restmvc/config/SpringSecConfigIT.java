package guru.springframework.spring7restmvc.config;

import guru.springframework.spring7restmvc.controller.BeerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpringSecConfigIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void testApiDocsOpenWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"));
    }

    @Test
    void testBeerListNeedsToken() throws Exception {
        mockMvc.perform(get(BeerController.BEER_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        containsString("Bearer")));
    }
}
