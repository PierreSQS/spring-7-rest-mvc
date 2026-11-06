package guru.springframework.spring7restmvc.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Pierrot on 02-11-2026
 * Modified by Pierrot on 06-11-2026
 * <p>
 * Tells the OpenAPI documentation that every operation needs a Bearer JWT,
 * so Swagger UI shows an "Authorize" button and sends the token (#27).
 * Documentation only: the real security is in {@link SpringSecConfig}.
 * {@code info} is mandatory in OpenAPI: once this annotation exists, springdoc no longer fills its default title.
 * {@code info.version} is the version of the API contract ("v1", as in /api/v1), written by hand on purpose:
 * OpenAPI keeps it distinct from the implementation version. The build version goes into the extension
 * {@code info.x-build-version} ({@link BuildProperties}, #28), when Maven wrote {@code META-INF/build-info.properties};
 * without that file the application still starts, without the extension.
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "Get a token from the authorization server http://localhost:9000/oauth2/token (client credentials), then paste it here.")
@OpenAPIDefinition(
        info = @Info(title = "Beer and Customer API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer openApiCustomizer(ObjectProvider<BuildProperties> buildProperties) {
        return openApi -> buildProperties.ifAvailable(build -> openApi.getInfo()
                .addExtension("x-build-version", build.getVersion()));
    }
}
