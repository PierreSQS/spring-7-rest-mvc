package guru.springframework.spring7restmvc.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Pierrot on 02-11-2026
 * <p>
 * Tells the OpenAPI documentation that every operation needs a Bearer JWT,
 * so Swagger UI shows an "Authorize" button and sends the token (#27).
 * Documentation only: the real security is in {@link SpringSecConfig}.
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@OpenAPIDefinition(security = @SecurityRequirement(name = "bearerAuth"))
public class OpenApiConfig {
}
