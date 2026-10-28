
package SIGA.backend.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KeycloakAdminService {

    @Value("${KEYCLOAK_INTERNAL_URL}")
    private String keycloakUrl;

    @Value("${KEYCLOAK_ADMIN_CLIENT_ID}")
    private String clientId;

    @Value("${KEYCLOAK_ADMIN_CLIENT_SECRET}")
    private String clientSecret;

    private final RestClient restClient = RestClient.create();

    // get service account access token
    private String getAccessToken() {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<?, ?> response = restClient.post()
                .uri(keycloakUrl + "/realms/SIGA/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("access_token") instanceof String token)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo obtener el token de Keycloak");
        }

        return token;
    }

    // validate user and realm role in Keycloak
    public void validateUserRole(String keycloakSubject, String expectedRole) {

        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de Keycloak es obligatorio");
        }

        // validate user ID format
        String subject;

        try {
            subject = UUID.fromString(keycloakSubject.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identificador de Keycloak invalido");
        }

        if (!List.of("STUDENT", "TEACHER").contains(expectedRole)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rol de Keycloak invalido");
        }

        try {
            String token = getAccessToken();

            // check that the user exists
            restClient.get()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .toBodilessEntity();

            // get effective realm roles
            List<?> roles = restClient.get()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject
                            + "/role-mappings/realm/composite")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(List.class);

            boolean hasRole = roles != null && roles.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .anyMatch(role -> expectedRole.equals(role.get("name")));

            if (!hasRole) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El usuario no tiene el rol " + expectedRole);
            }

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario de Keycloak no encontrado");
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar Keycloak");

        } catch (org.springframework.web.client.RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con Keycloak");
        }
    }
}
