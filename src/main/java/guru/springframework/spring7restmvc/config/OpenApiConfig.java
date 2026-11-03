package guru.springframework.spring7restmvc.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Pierrot on 02-11-2026
 * <p>
 * Tells the OpenAPI documentation that every operation needs a Bearer JWT,
 * so Swagger UI shows an "Authorize" button and sends the token (#27).
 * Documentation only: the real security is in {@link SpringSecConfig}.
 * {@code info} is mandatory in OpenAPI: once this annotation exists, springdoc no longer fills its default title.
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@OpenAPIDefinition(
        info = @Info(title = "Beer and Customer API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
public class OpenApiConfig {
}
