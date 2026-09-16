package guru.springframework.spring7restmvc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modified by Pierrot on 17-09-2026
 */
@SpringBootTest
class Spring7RestMvcApplicationTests {

    @Test
    void contextLoads(ApplicationContext appCtx) {
        assertThat(appCtx).isNotNull();
    }

}
