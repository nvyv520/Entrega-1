package SIGA.backend.services;

import static SIGA.backend.services.ServiceFixtures.rejects;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.transaction.support.*;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class KeycloakAdminServiceTests {
  static final String ID = "00000000-0000-0000-0000-000000000001";
  static final String USER = "/admin/realms/SIGA/users/" + ID;
  KeycloakAdminService s;
  MockRestServiceServer server;
  MockEnvironment env;
  JsonMapper json = new JsonMapper();
  Map<String, Object> user;
  boolean independent;
  int deleted;
  int created;
  int puts;
  String roles;
  Function<MockClientHttpRequest, ResponseCreator> override;

  @BeforeEach
  void setup() {
    env = new MockEnvironment();
    s = new KeycloakAdminService(env);
    var builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    ReflectionTestUtils.setField(s, "restClient", builder.build());
    ReflectionTestUtils.setField(s, "keycloakUrl", "http://keycloak");
    ReflectionTestUtils.setField(s, "clientId", "test");
    ReflectionTestUtils.setField(s, "clientSecret", "secret");
    roles = "TEACHER";
    user =
        new HashMap<>(
            Map.of(
                "id",
                ID,
                "username",
                "ana@example.cl",
                "email",
                "ana@example.cl",
                "firstName",
                "Ana",
                "lastName",
                "Pérez",
                "emailVerified",
                true));
    server
        .expect(manyTimes(), requestTo(org.hamcrest.Matchers.startsWith("http://keycloak/")))
        .andRespond(
            request -> {
              var req = (MockClientHttpRequest) request;
              if (override != null) {
                var response = override.apply(req);
                if (response != null) return response.createResponse(request);
              }
              String path = req.getURI().getPath();
              String method = req.getMethod().name();
              if (path.endsWith("/token"))
                return withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON)
                    .createResponse(request);
              if (path.equals("/admin/realms/SIGA"))
                return withSuccess(
                        "{\"registrationEmailAsUsername\":" + !independent + "}",
                        MediaType.APPLICATION_JSON)
                    .createResponse(request);
              if (path.equals("/admin/realms/SIGA/users") && method.equals("GET"))
                return withSuccess("[]", MediaType.APPLICATION_JSON).createResponse(request);
              if (path.equals("/admin/realms/SIGA/users") && method.equals("POST")) {
                user = new HashMap<>(json.readValue(req.getBodyAsString(), Map.class));
                user.put("id", ID);
                created++;
                return withStatus(HttpStatus.CREATED)
                    .location(java.net.URI.create("http://keycloak" + USER))
                    .createResponse(request);
              }
              if (path.endsWith("/composite"))
                return withSuccess("[{\"name\":\"" + roles + "\"}]", MediaType.APPLICATION_JSON)
                    .createResponse(request);
              if (path.startsWith("/admin/realms/SIGA/roles/"))
                return withSuccess(
                        "{\"id\":\"role-id\",\"name\":\"" + roles + "\"}",
                        MediaType.APPLICATION_JSON)
                    .createResponse(request);
              if (path.equals(USER) && method.equals("GET"))
                return withSuccess(json.writeValueAsString(user), MediaType.APPLICATION_JSON)
                    .createResponse(request);
              if (path.equals(USER) && method.equals("PUT")) {
                user.putAll(json.readValue(req.getBodyAsString(), Map.class));
                puts++;
                return withStatus(HttpStatus.NO_CONTENT).createResponse(request);
              }
              if (path.equals(USER) && method.equals("DELETE")) {
                deleted++;
                return withStatus(HttpStatus.NO_CONTENT).createResponse(request);
              }
              if (path.endsWith("/reset-password") || path.endsWith("/role-mappings/realm"))
                return withStatus(HttpStatus.NO_CONTENT).createResponse(request);
              throw new AssertionError("Unexpected request: " + method + " " + path);
            });
  }

  @AfterEach
  void cleanup() {
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.clearSynchronization();
  }

  KeycloakAdminService.CreatedUser create() {
    return s.createUser("ana@example.cl", "Ana", "Pérez", "TEACHER");
  }

  void update() {
    s.updateUserIdentity(
        ID, "TEACHER", "ana@example.cl", "Ana", "Pérez", "new@example.cl", "Anita", "Pérez");
  }

  TransactionSynchronization prepareUpdate() {
    TransactionSynchronizationManager.initSynchronization();
    update();
    return TransactionSynchronizationManager.getSynchronizations().get(0);
  }

  @Test
  void createsVerifiesAndDeletesOwnedAccount() {
    var result = create();
    assertEquals(ID, result.keycloakSubject());
    assertEquals("ana@example.cl", result.username());
    String password = result.temporaryPassword();
    assertEquals(20, password.length());
    assertTrue(password.matches(".*[A-Z].*"));
    assertTrue(password.matches(".*[a-z].*"));
    assertTrue(password.matches(".*[0-9].*"));
    assertTrue(password.matches(".*[!@#$%&*].*"));
    assertFalse(result.toString().contains(password));
    s.deleteCreatedUser(result);
    assertEquals(1, deleted);
    s.deleteCreatedUser(null);
    s.deleteCreatedUser(new KeycloakAdminService.CreatedUser(null, "x", "x", "x"));
    server.verify();
  }

  @Test
  void independentUsernameRetriesOccupiedName() {
    independent = true;
    override =
        req ->
            req.getMethod() == HttpMethod.GET
                    && req.getURI().getQuery() != null
                    && req.getURI().getQuery().contains("username=ana&")
                ? withSuccess("[{\"id\":\"existing\"}]", MediaType.APPLICATION_JSON)
                : null;
    assertEquals("anap", create().username());
  }

  @Test
  void localFictitiousPasswordRequiresExclusiveProfile() {
    env.setActiveProfiles("local-fictitious");
    ReflectionTestUtils.setField(s, "localFictitiousPasswords", true);
    assertEquals(
        "ana123",
        s.createUser("ana@example.cl", "María", "Pérez", "TEACHER").temporaryPassword());
    env.setActiveProfiles("local-fictitious", "test");
    assertEquals(20, create().temporaryPassword().length());
  }

  @Test
  void invalidRegistrationInputsNeverCreateAccount() {
    rejects(400, () -> s.createUser(null, "Ana", "Pérez", "TEACHER"));
    rejects(400, () -> s.createUser(" ", "Ana", "Pérez", "TEACHER"));
    rejects(400, () -> s.createUser("ana@example.cl", "Ana", "Pérez", "ADMIN"));
    env.setActiveProfiles("local-fictitious");
    ReflectionTestUtils.setField(s, "localFictitiousPasswords", true);
    rejects(400, () -> s.createUser("@example.cl", "Ana", "Pérez", "TEACHER"));
    assertEquals(0, created);
  }

  @Test
  void validatesRoleAndRejectsBadIdentity() {
    s.validateUserRole(ID, "TEACHER");
    roles = "STUDENT";
    rejects(409, () -> s.validateUserRole(ID, "TEACHER"));
    rejects(400, () -> s.validateUserRole(null, "TEACHER"));
    rejects(400, () -> s.validateUserRole(" ", "TEACHER"));
    rejects(400, () -> s.validateUserRole("bad", "TEACHER"));
    rejects(400, () -> s.validateUserRole(ID, "ADMIN"));
  }

  @ParameterizedTest
  @ValueSource(ints = {404, 500})
  void roleLookupMapsRemoteErrors(int status) {
    override =
        req -> req.getURI().getPath().equals(USER) ? withStatus(HttpStatus.valueOf(status)) : null;
    rejects(status == 404 ? 404 : 502, () -> s.validateUserRole(ID, "TEACHER"));
  }

  @Test
  void roleLookupMapsNetworkFailure() {
    override =
        req ->
            req.getURI().getPath().equals(USER)
                ? withException(new java.io.IOException("simulated outage"))
                : null;
    rejects(502, () -> s.validateUserRole(ID, "TEACHER"));
  }

  @Test
  void invalidAccessTokenIsRejected() {
    override =
        req ->
            req.getURI().getPath().endsWith("/token")
                ? withSuccess("{}", MediaType.APPLICATION_JSON)
                : null;
    rejects(502, () -> create());
    assertEquals(0, created);
  }

  @Test
  void duplicateIdentityIsNotModified() {
    override =
        req ->
            req.getURI().getQuery() != null
                ? withSuccess("[{\"id\":\"existing\"}]", MediaType.APPLICATION_JSON)
                : null;
    rejects(409, () -> create());
    assertEquals(0, created);
    assertEquals(0, deleted);
  }

  @Test
  void unavailableRealmSettingsFallBackToEmail() {
    override =
        req ->
            req.getURI().getPath().equals("/admin/realms/SIGA")
                ? withStatus(HttpStatus.FORBIDDEN)
                : null;
    assertEquals("ana@example.cl", create().username());
  }

  @Test
  void registrationConflictDoesNotDeleteExistingAccount() {
    override =
        req ->
            req.getMethod() == HttpMethod.POST
                    && req.getURI().getPath().equals("/admin/realms/SIGA/users")
                ? withStatus(HttpStatus.CONFLICT)
                : null;
    rejects(409, () -> create());
    assertEquals(0, deleted);
  }

  @Test
  void failedPasswordSetupCleansOnlyOwnedAccount() {
    override = req -> req.getURI().getPath().endsWith("/reset-password") ? withServerError() : null;
    rejects(502, () -> create());
    assertEquals(1, created);
    assertEquals(1, deleted);
  }

  @Test
  void unexpectedPasswordResponseCleansOwnedAccount() {
    override = req -> req.getURI().getPath().endsWith("/reset-password") ? withSuccess() : null;
    rejects(502, () -> create());
    assertEquals(1, deleted);
  }

  @Test
  void networkFailureAfterCreationCleansOwnedAccount() {
    override =
        req ->
            req.getURI().getPath().endsWith("/reset-password")
                ? withException(new java.io.IOException("simulated outage"))
                : null;
    rejects(502, () -> create());
    assertEquals(1, deleted);
  }

  @Test
  void cleanupFailureReportsUncertainAccount() {
    override =
        req ->
            req.getURI().getPath().endsWith("/reset-password")
                    || req.getMethod() == HttpMethod.DELETE
                ? withServerError()
                : null;
    var error =
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> create());
    assertTrue(error.getReason().contains("revisión manual"));
    assertEquals(0, deleted);
  }

  @Test
  void incompleteCreationResponseCannotDeleteUnidentifiedAccount() {
    override =
        req ->
            req.getMethod() == HttpMethod.POST
                    && req.getURI().getPath().equals("/admin/realms/SIGA/users")
                ? withStatus(HttpStatus.CREATED)
                : null;
    rejects(502, () -> create());
    assertEquals(0, deleted);
  }

  @Test
  void rejectsMalformedLocation() {
    override =
        req ->
            req.getMethod() == HttpMethod.POST
                    && req.getURI().getPath().equals("/admin/realms/SIGA/users")
                ? withStatus(HttpStatus.CREATED)
                    .location(java.net.URI.create("http://keycloak/users/bad"))
                : null;
    rejects(502, () -> create());
  }

  @Test
  void deletionVerifiesOwnership() {
    var result = create();
    user.put("attributes", Map.of("sigaRegistrationId", List.of("another-registration")));
    rejects(502, () -> s.deleteCreatedUser(result));
    assertEquals(0, deleted);
  }

  @ParameterizedTest
  @ValueSource(ints = {404, 500, 200})
  void deletionHandlesRemoteResponses(int status) {
    var result = create();
    override =
        req -> req.getMethod() == HttpMethod.DELETE ? withStatus(HttpStatus.valueOf(status)) : null;
    if (status == 404) assertDoesNotThrow(() -> s.deleteCreatedUser(result));
    else rejects(502, () -> s.deleteCreatedUser(result));
  }

  @Test
  void deletionMapsConnectionFailure() {
    var result = create();
    override =
        req ->
            req.getMethod() == HttpMethod.DELETE
                ? withException(new java.io.IOException("outage"))
                : null;
    rejects(502, () -> s.deleteCreatedUser(result));
  }

  @Test
  void identityChangesCommitAndCanCompensateRollback() {
    var sync = prepareUpdate();
    assertEquals("ana@example.cl", user.get("email"));
    sync.beforeCommit(false);
    assertEquals("new@example.cl", user.get("email"));
    assertEquals("new@example.cl", user.get("username"));
    assertEquals(false, user.get("emailVerified"));
    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertEquals("ana@example.cl", user.get("email"));
    assertEquals(2, puts);
  }

  @Test
  void committedIdentityIsNotReverted() {
    var sync = prepareUpdate();
    sync.beforeCommit(false);
    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertEquals(1, puts);
  }

  @Test
  void unknownTransactionOutcomeLeavesIdentityForReconciliation() {
    var sync = prepareUpdate();
    sync.beforeCommit(false);
    sync.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
    assertEquals(1, puts);
  }

  @Test
  void identityChangedConcurrentlyIsNeverOverwritten() {
    var sync = prepareUpdate();
    user.put("firstName", "Other");
    rejects(409, () -> sync.beforeCommit(false));
    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertEquals(0, puts);
  }

  @Test
  void rollbackDoesNotOverwriteFurtherRemoteEdits() {
    var sync = prepareUpdate();
    sync.beforeCommit(false);
    user.put("firstName", "Other");
    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertEquals(1, puts);
  }

  @Test
  void rollbackSkipsIdentityAlreadyRestored() {
    var sync = prepareUpdate();
    sync.beforeCommit(false);
    user.putAll(
        Map.of(
            "email",
            "ana@example.cl",
            "username",
            "ana@example.cl",
            "firstName",
            "Ana",
            "emailVerified",
            true));
    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertEquals(1, puts);
  }

  @Test
  void unchangedOrUnlinkedIdentityDoesNotContactKeycloak() {
    s.updateUserIdentity(ID, "TEACHER", "same", "Ana", "Pérez", "same", "Ana", "Pérez");
    s.updateUserIdentity(null, "TEACHER", "old", "Ana", "Pérez", "new", "Ana", "Pérez");
    s.updateUserIdentity(" ", "TEACHER", "old", "Ana", "Pérez", "new", "Ana", "Pérez");
    rejects(
        409,
        () -> s.updateUserIdentity("bad", "TEACHER", "old", "Ana", "Pérez", "new", "Ana", "Pérez"));
  }

  @Test
  void mismatchingLinkedIdentityIsRejected() {
    user.put("firstName", "Other");
    rejects(409, () -> update());
  }

  @Test
  void duplicateEmailAtCommitIsRejected() {
    var sync = prepareUpdate();
    override =
        req ->
            req.getURI().getQuery() != null
                ? withSuccess("[{\"id\":\"other\"}]", MediaType.APPLICATION_JSON)
                : null;
    rejects(409, () -> sync.beforeCommit(false));
    assertEquals(0, puts);
  }

  @ParameterizedTest
  @ValueSource(ints = {409, 500, 200})
  void identityWriteFailuresAreMapped(int status) {
    var sync = prepareUpdate();
    override =
        req -> req.getMethod() == HttpMethod.PUT ? withStatus(HttpStatus.valueOf(status)) : null;
    rejects(status == 409 ? 409 : 502, () -> sync.beforeCommit(false));
  }

  @Test
  void identityWriteNetworkFailureIsMapped() {
    var sync = prepareUpdate();
    override =
        req ->
            req.getMethod() == HttpMethod.PUT
                ? withException(new java.io.IOException("outage"))
                : null;
    rejects(502, () -> sync.beforeCommit(false));
    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
  }

  @Test
  void initialIdentityLookupNetworkFailureIsMapped() {
    final int[] gets = {0};
    override =
        req ->
            req.getURI().getPath().equals(USER) && ++gets[0] > 1
                ? withException(new java.io.IOException("outage"))
                : null;
    rejects(502, () -> update());
  }
}
