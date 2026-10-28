
package SIGA.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        String securitySchemeName = "keycloak";

        return new OpenAPI()
                .addSecurityItem(
                        new SecurityRequirement().addList(securitySchemeName)
                )
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(
                                                                "http://localhost:8081/realms/SIGA/protocol/openid-connect/auth"
                                                        )
                                                        .tokenUrl(
                                                                "http://localhost:8081/realms/SIGA/protocol/openid-connect/token"
                                                        )
                                                        .scopes(new Scopes()
                                                                .addString("openid", "Iniciar sesión con Keycloak")
                                                        )
                                                )
                                        )
                        )
                );
    }
}
