package guru.springframework.spring7restmvc.controller;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

/**
 * Created by jt, Spring Framework Guru.
 * Modified by Pierrot on 27-11-2026
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Both annotations are needed, because of the nested @Configuration class below:
// - the nested configuration class makes Spring Boot stop looking for Spring7RestMvcApplication.
// - @Import makes Spring Boot register this test class itself as configuration, so its annotations count.
// - @ComponentScan (active thanks to @Import) then finds Spring7RestMvcApplication again.
@Import(BeerControllerRestAssuredTest.TestSecurityConfig.class)
@ComponentScan(basePackages = {"guru.springframework.spring7restmvc"})
public class BeerControllerRestAssuredTest {

    OpenApiValidationFilter validationFilter = new OpenApiValidationFilter(OpenApiInteractionValidator
            .createForSpecificationUrl("oa3.yml")
            .build());

    // Add local Configuration class to disable security for RestAssured tests
    @Configuration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) {
            http.authorizeHttpRequests(authorize ->
                    authorize.anyRequest().permitAll());
            return http.build();
        }
    }

    @LocalServerPort
    Integer localPort;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = localPort;
    }

    @Test
    void testListBeers() {
        given().contentType("application/json")
                .when()
                .filter(validationFilter)
                .get("/api/v1/beer")
                .then()
                .statusCode(200);
    }
}
