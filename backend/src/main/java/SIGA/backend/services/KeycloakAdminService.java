package SIGA.backend.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.security.SecureRandom;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.text.Normalizer;
import java.util.Locale;
import java.time.Year;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    @Value("${KEYCLOAK_INTERNAL_URL}")
    private String keycloakUrl;

    @Value("${KEYCLOAK_ADMIN_CLIENT_ID}")
    private String clientId;

    @Value("${KEYCLOAK_ADMIN_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${siga.keycloak.local-fictitious-passwords:false}")
    private boolean localFictitiousPasswords;

    private final Environment environment;

    private final RestClient restClient = RestClient.create();
    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);

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

        if (response == null || !(response.get("access_token") instanceof String token) || token.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo obtener el token de Keycloak");
        }

        return token;
    }

    // validate user and realm role in Keycloak
    public void validateUserRole(String keycloakSubject, String expectedRole) {

        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de Keycloak es obligatorio");
        }

        // validate user ID format
        String subject;

        try {
            subject = UUID.fromString(keycloakSubject.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de Keycloak invalido");
        }

        if (!List.of("STUDENT", "TEACHER").contains(expectedRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol de Keycloak invalido");
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
                throw new ResponseStatusException( HttpStatus.CONFLICT, "El usuario no tiene el rol " + expectedRole);
            }

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario de Keycloak no encontrado");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo consultar Keycloak");

        } catch (org.springframework.web.client.RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo conectar con Keycloak");
        }
    }


    // generate temporary password
    private String generateTemporaryPassword() {

        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String symbols = "!@#$%&*";
        String characters = uppercase + lowercase + numbers + symbols;

        SecureRandom random = new SecureRandom();
        List<Character> password = new ArrayList<>();

        password.add(uppercase.charAt(random.nextInt(uppercase.length())));
        password.add(lowercase.charAt(random.nextInt(lowercase.length())));
        password.add(numbers.charAt(random.nextInt(numbers.length())));
        password.add(symbols.charAt(random.nextInt(symbols.length())));

        for (int i = 4; i < 20; i++) {
            password.add(characters.charAt(random.nextInt(characters.length())));
        }

        Collections.shuffle(password, random);

        StringBuilder result = new StringBuilder();

        for (Character character : password) {
            result.append(character);
        }

        return result.toString();
    }



    // Uses predictable passwords only with the explicit local fictitious-data settings.
    private String generateTemporaryPassword(String firstName) {
        String[] profiles = environment.getActiveProfiles();
        if (!localFictitiousPasswords || profiles.length != 1 || !"local-fictitious".equals(profiles[0])) {
            return generateTemporaryPassword();
        }

        String name = normalizeUsername(firstName.trim().split("\\s+")[0]);
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El primer nombre no permite generar una contraseña temporal valida");
        }
        return name + Year.now().getValue();
    }


    // delete newly created user
    public void deleteCreatedUser(CreatedUser createdUser) {

        if (createdUser == null || createdUser.keycloakSubject() == null) {
            return;
        }

        String subject = UUID.fromString(createdUser.keycloakSubject()).toString();
        String token = getAccessToken();

        try {
            verifyCreatedUser(createdUser, token);

            ResponseEntity<Void> response = restClient.delete()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .toBodilessEntity();
            if (response.getStatusCode().value() != 204) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no confirmó la eliminación de la cuenta creada");
            }

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode().value() == 404) {
                return;
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo eliminar el usuario de Keycloak");

        } catch (org.springframework.web.client.RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo conectar con Keycloak");
        }
    }


    // clean up newly created user
    private boolean cleanupCreatedUser(CreatedUser createdUser) {

        if (createdUser.keycloakSubject() == null) {
            return false;
        }

        try {
            deleteCreatedUser(createdUser);
            return true;

        } catch (Exception exception) {
            logger.error("Failed to clean up Keycloak user {}", createdUser.keycloakSubject());
            return false;
        }
    }




    public record CreatedUser(String keycloakSubject, String username, String temporaryPassword, String registrationId) {
        @Override
        public String toString() {
            return "CreatedUser[keycloakSubject=" + keycloakSubject + "]";
        }
    }

    private Map<?, ?> verifyCreatedUser(CreatedUser createdUser, String token) {
        Map<?, ?> user = restClient.get()
                .uri(keycloakUrl + "/admin/realms/SIGA/users/" + createdUser.keycloakSubject())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve().body(Map.class);
        if (user == null || !createdUser.keycloakSubject().equals(user.get("id"))
                || !(user.get("username") instanceof String username) || !createdUser.username().equalsIgnoreCase(username)
                || !(user.get("email") instanceof String email) || email.isBlank()
                || !(user.get("attributes") instanceof Map<?, ?> attributes)
                || !List.of(createdUser.registrationId()).equals(attributes.get("sigaRegistrationId"))) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo verificar la identidad de la cuenta creada; requiere revisión manual");
        }
        return user;
    }

    // Synchronizes only a verified linked identity.
    public void updateUserIdentity(String keycloakSubject, String role, String currentEmail,
            String currentFirstName, String currentLastName, String email, String firstName, String lastName) {

        if (Objects.equals(currentEmail, email) && Objects.equals(currentFirstName, firstName)
                && Objects.equals(currentLastName, lastName)) return;
        if (keycloakSubject == null || keycloakSubject.isBlank()) return;

        String subject;
        try {
            subject = UUID.fromString(keycloakSubject).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El vínculo con Keycloak requiere revisión manual");
        }

        validateUserRole(subject, role);
        try {
            String token = getAccessToken();
            Map<?, ?> user = restClient.get()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve().body(Map.class);
            if (user == null || !subject.equals(user.get("id"))
                    || !Objects.equals(currentEmail, user.get("email"))
                    || !Objects.equals(currentFirstName, user.get("firstName"))
                    || !Objects.equals(currentLastName, user.get("lastName"))
                    || !(user.get("username") instanceof String username) || username.isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La identidad vinculada no coincide con Keycloak; requiere revisión manual");
            }

            Map<String, Object> previous = new java.util.HashMap<>();
            for (String field : List.of("username", "email", "firstName", "lastName", "emailVerified")) {
                previous.put(field, user.get(field));
            }
            Map<String, Object> updated = new java.util.HashMap<>(previous);
            updated.put("email", email);
            updated.put("firstName", firstName);
            updated.put("lastName", lastName);
            if (!Objects.equals(currentEmail, email)) {
                updated.put("emailVerified", false);
                if (currentEmail.equalsIgnoreCase(username)) updated.put("username", email.toLowerCase(java.util.Locale.ROOT));
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                private boolean attempted;

                @Override
                public void beforeCommit(boolean readOnly) {
                    try {
                        validateUserRole(subject, role);
                        String checkToken = getAccessToken();
                        Map<?, ?> actual = restClient.get()
                                .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject)
                                .headers(headers -> headers.setBearerAuth(checkToken))
                                .retrieve().body(Map.class);
                        if (actual == null || !subject.equals(actual.get("id"))
                                || !previous.entrySet().stream().allMatch(entry -> Objects.equals(entry.getValue(), actual.get(entry.getKey())))) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "La identidad de Keycloak cambió durante la edición; vuelve a consultar el registro");
                        }
                        if (!Objects.equals(currentEmail, email)) {
                            List<?> matches = restClient.get()
                                    .uri(keycloakUrl + "/admin/realms/SIGA/users?email={value}&exact=true", email)
                                    .headers(headers -> headers.setBearerAuth(checkToken))
                                    .retrieve().body(List.class);
                            if (matches == null) {
                                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no devolvió la comprobación del correo");
                            }
                            if (matches.stream().anyMatch(match -> !(match instanceof Map<?, ?> account) || !subject.equals(account.get("id")))) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está asociado a otra cuenta de Keycloak");
                            }
                        }
                        attempted = true;
                        writeUserIdentity(subject, updated);
                    } catch (org.springframework.web.client.RestClientException exception) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo verificar la identidad en Keycloak antes de guardar");
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (!attempted || status == STATUS_COMMITTED) return;
                    if (status != STATUS_ROLLED_BACK) {
                        logger.error("Uncertain identity transaction for Keycloak user {}; manual reconciliation required", subject);
                        return;
                    }
                    try {
                        String cleanupToken = getAccessToken();
                        Map<?, ?> actual = restClient.get()
                                .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject)
                                .headers(headers -> headers.setBearerAuth(cleanupToken))
                                .retrieve().body(Map.class);
                        if (actual != null && subject.equals(actual.get("id"))
                                && previous.entrySet().stream().allMatch(entry -> Objects.equals(entry.getValue(), actual.get(entry.getKey())))) return;
                        if (actual == null || !subject.equals(actual.get("id"))
                                || !updated.entrySet().stream().allMatch(entry -> Objects.equals(entry.getValue(), actual.get(entry.getKey())))) {
                            logger.error("Identity changed for Keycloak user {}; manual reconciliation required", subject);
                            return;
                        }
                        writeUserIdentity(subject, previous);
                    } catch (Exception exception) {
                        logger.error("Failed to restore Keycloak identity {}; manual reconciliation required", subject);
                    }
                }
            });
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo verificar la identidad en Keycloak");
        }
    }

    private void writeUserIdentity(String subject, Map<String, Object> identity) {
        try {
            String token = getAccessToken();
            ResponseEntity<Void> response = restClient.put()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject)
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(identity).retrieve().toBodilessEntity();
            if (response.getStatusCode().value() != 204) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no confirmó la actualización de identidad; requiere revisión manual");
            }
            Map<?, ?> actual = restClient.get()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve().body(Map.class);
            if (actual == null || !subject.equals(actual.get("id"))
                    || !identity.entrySet().stream().allMatch(entry -> Objects.equals(entry.getValue(), actual.get(entry.getKey())))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no confirmó los datos de identidad actualizados; requiere revisión manual");
            }
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario o correo ya existe en Keycloak");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo actualizar la identidad en Keycloak; requiere revisión manual");
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falló la comunicación al actualizar Keycloak; requiere revisión manual antes de reintentar");
        }
    }

    // Keeps email usernames when the realm settings cannot be verified.
    private boolean supportsIndependentUsername(String token) {
        try {
            Map<?, ?> realm = restClient.get()
                    .uri(keycloakUrl + "/admin/realms/SIGA")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve().body(Map.class);
            return realm != null && Boolean.FALSE.equals(realm.get("registrationEmailAsUsername"));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 403) {
                logger.warn("Realm settings unavailable; new accounts retain email usernames");
                return false;
            }
            throw exception;
        }
    }

    private String normalizeUsername(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    // create user in Keycloak
    public CreatedUser createUser(String username, String firstName, String lastName, String role) {

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo del usuario es obligatorio");
        }

        if (!List.of("STUDENT", "TEACHER").contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol de Keycloak invalido");
        }

        String email = username.trim();
        String login = email;
        String password = generateTemporaryPassword(firstName);
        String subject = null;
        String registrationId = UUID.randomUUID().toString();
        String stage = "obtener autorización";
        boolean creationRequested = false;

        try {
            String token = getAccessToken();
            boolean independentUsername = supportsIndependentUsername(token);
            String name = normalizeUsername(firstName.trim().split("\\s+")[0]);
            String surname = normalizeUsername(lastName);
            if (independentUsername && name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El primer nombre no permite generar un usuario valido");
            }

            stage = "comprobar los conflictos de identidad";
            for (String field : independentUsername ? List.of("email") : List.of("username", "email")) {
                List<?> matches = restClient.get()
                        .uri(keycloakUrl + "/admin/realms/SIGA/users?" + field + "={value}&exact=true", email)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve().body(List.class);
                if (matches == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no devolvió la comprobación de duplicados");
                }
                if (!matches.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "El usuario o correo ya existe en Keycloak. No se modificó la cuenta existente; si hubo un intento anterior, requiere revisión manual");
                }
            }

            ResponseEntity<Void> response = null;
            // Keycloak remains the uniqueness authority, including concurrent requests.
            for (int attempt = 0; attempt < surname.length() + 101; attempt++) {
                login = independentUsername
                        ? name + (attempt <= surname.length() ? surname.substring(0, attempt)
                                : surname + (attempt - surname.length()))
                        : email;
                stage = "comprobar el nombre de usuario";
                List<?> matches = restClient.get()
                        .uri(keycloakUrl + "/admin/realms/SIGA/users?username={value}&exact=true", login)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve().body(List.class);
                if (matches == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no devolvio la comprobacion del usuario");
                }
                if (!matches.isEmpty()) {
                    if (independentUsername) continue;
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe en Keycloak");
                }

                Map<String, Object> user = Map.of(
                        "username", login,
                        "email", email,
                        "firstName", firstName,
                        "lastName", lastName,
                        "enabled", true,
                        "attributes", Map.of("sigaRegistrationId", List.of(registrationId)),
                        "requiredActions", List.of("UPDATE_PASSWORD"));

                stage = "crear la cuenta";
                creationRequested = true;
                try {
                    response = restClient.post()
                            .uri(keycloakUrl + "/admin/realms/SIGA/users")
                            .headers(headers -> headers.setBearerAuth(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(user)
                            .retrieve()
                            .toBodilessEntity();
                    break;
                } catch (RestClientResponseException exception) {
                    if (exception.getStatusCode().value() != 409) throw exception;
                    creationRequested = false;
                    if (!independentUsername) throw exception;
                    List<?> emails = restClient.get()
                            .uri(keycloakUrl + "/admin/realms/SIGA/users?email={value}&exact=true", email)
                            .headers(headers -> headers.setBearerAuth(token))
                            .retrieve().body(List.class);
                    if (emails == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no devolvio la comprobacion del correo");
                    }
                    if (!emails.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya existe en Keycloak");
                    }
                }
            }

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo reservar un nombre de usuario disponible; vuelve a intentar");
            }

            URI location = response.getHeaders().getLocation();

            if (response.getStatusCode().value() != 201 || location == null || location.getPath() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Keycloak devolvió una respuesta de creación incompleta");
            }

            String path = location.getPath();
            try {
                subject = UUID.fromString(path.substring(path.lastIndexOf("/") + 1)).toString();
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak devolvió un identificador inválido");
            }
            stage = "verificar la cuenta creada";
            Map<?, ?> createdUser = verifyCreatedUser(new CreatedUser(subject, login, password, registrationId), token);
            if (!email.equalsIgnoreCase((String) createdUser.get("email"))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no conservo el correo de contacto");
            }

            // set temporary password
            stage = "configurar la contraseña temporal";
            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", true);

            ResponseEntity<Void> passwordResponse = restClient.put()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject + "/reset-password")
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credential)
                    .retrieve()
                    .toBodilessEntity();
            if (passwordResponse.getStatusCode().value() != 204) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no confirmó la contraseña temporal");
            }

            // get realm role
            stage = "obtener el rol " + role;
            Map<?, ?> realmRole = restClient.get()
                    .uri(keycloakUrl + "/admin/realms/SIGA/roles/" + role)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(Map.class);

            if (realmRole == null || !(realmRole.get("id") instanceof String roleId) || roleId.isBlank()
                    || !role.equals(realmRole.get("name"))) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "No se pudo obtener el rol de Keycloak");
            }

            // assign realm role
            stage = "asignar el rol " + role;
            ResponseEntity<Void> roleResponse = restClient.post()
                    .uri(keycloakUrl + "/admin/realms/SIGA/users/" + subject + "/role-mappings/realm")
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(realmRole))
                    .retrieve()
                    .toBodilessEntity();
            if (roleResponse.getStatusCode().value() != 204) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak no confirmó la asignación del rol");
            }
            stage = "verificar el registro completo";
            validateUserRole(subject, role);
            Map<?, ?> completedUser = verifyCreatedUser(new CreatedUser(subject, login, password, registrationId), token);
            if (!Boolean.TRUE.equals(completedUser.get("enabled"))
                    || !(completedUser.get("requiredActions") instanceof List<?> actions) || !actions.contains("UPDATE_PASSWORD")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La cuenta de Keycloak está incompleta");
            }

            return new CreatedUser(subject, (String) completedUser.get("username"), password, registrationId);


        } catch (RestClientResponseException exception) {

            if (stage.equals("crear la cuenta") && exception.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El usuario o correo ya existe en Keycloak. No se modificó la cuenta existente; si hubo un intento anterior, requiere revisión manual");
            }

            boolean cleaned = !creationRequested || cleanupCreatedUser(new CreatedUser(subject, login, password, registrationId));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo " + stage + " en Keycloak" + (cleaned ? "" : ". El estado de la cuenta requiere revisión manual antes de reintentar"));

        } catch (org.springframework.web.client.RestClientException exception) {

            boolean cleaned = !creationRequested || cleanupCreatedUser(new CreatedUser(subject, login, password, registrationId));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falló la comunicación con Keycloak al " + stage + (cleaned ? "" : ". El estado de la cuenta requiere revisión manual antes de reintentar"));

        } catch (RuntimeException exception) {

            if (!creationRequested && exception instanceof ResponseStatusException) throw exception;
            boolean cleaned = !creationRequested || cleanupCreatedUser(new CreatedUser(subject, login, password, registrationId));
            String reason = exception instanceof ResponseStatusException statusException ? statusException.getReason() : "Respuesta inesperada de Keycloak al " + stage;
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    reason + (cleaned ? "" : ". El estado de la cuenta requiere revisión manual antes de reintentar"));
        }

    }


}
    
