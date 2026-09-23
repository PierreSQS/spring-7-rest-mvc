package guru.springframework.spring7restmvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * {@code VIA_DTO} answers a {@code Page} as Spring Data's {@code PagedModel}: the beers under
 * {@code content} and four documented values under {@code page}. Serializing the {@code PageImpl}
 * itself works too, but its JSON mirrors the class internals and Spring Data promises nothing about
 * it - which is what the warning on every start was about.
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class Spring7RestMvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(Spring7RestMvcApplication.class, args);
    }

}
