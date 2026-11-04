package SIGA.backend.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class DeploymentConfigurationTests {
  @Test
  void corsAcceptsConfiguredOriginsAndRejectsOtherOrigins() {
    var config = new SecurityConfig();
    ReflectionTestUtils.setField(
        config, "allowedOrigins", "https://siga.example.com, http://localhost:5173");
    var request = new MockHttpServletRequest("OPTIONS", "/api/students");
    var cors = config.corsConfigurationSource().getCorsConfiguration(request);
    assertNotNull(cors);
    assertEquals("https://siga.example.com", cors.checkOrigin("https://siga.example.com"));
    assertEquals("http://localhost:5173", cors.checkOrigin("http://localhost:5173"));
    assertNull(cors.checkOrigin("https://other.example.com"));
  }

  @Test
  void swaggerUsesThePublicKeycloakIssuer() {
    var config = new OpenApiConfig();
    ReflectionTestUtils.setField(config, "issuerUri", "https://siga.example.com/auth/realms/SIGA/");
    var flow =
        config
            .customOpenAPI()
            .getComponents()
            .getSecuritySchemes()
            .get("keycloak")
            .getFlows()
            .getAuthorizationCode();
    assertEquals(
        "https://siga.example.com/auth/realms/SIGA/protocol/openid-connect/auth",
        flow.getAuthorizationUrl());
    assertEquals(
        "https://siga.example.com/auth/realms/SIGA/protocol/openid-connect/token",
        flow.getTokenUrl());
  }
}
