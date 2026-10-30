package SIGA.backend.config;

import java.time.Instant;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.jwt.JwtException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidationTests {
    private final SecurityConfig config = new SecurityConfig();
    private static final String ISSUER = "https://issuer.test/realms/SIGA";

    @SuppressWarnings("unchecked")
    private OAuth2TokenValidator<Jwt> validator(boolean enabled) {
        return (OAuth2TokenValidator<Jwt>) ReflectionTestUtils.getField(
                config.jwtDecoder(ISSUER, "https://internal.test/certs", "siga-backend", enabled), "jwtValidator");
    }

    private Jwt token(String issuer, List<String> audiences, Instant expires) {
        var builder = Jwt.withTokenValue("test-value").header("alg", "RS256").subject("account")
                .issuer(issuer).issuedAt(Instant.now().minusSeconds(600)).expiresAt(expires);
        if (audiences != null) builder.audience(audiences);
        return builder.build();
    }

    @Test
    void requiresApiAudienceButAllowsAdditionalAudiences() {
        var validator = validator(true);
        assertFalse(validator.validate(token(ISSUER, List.of("account", "siga-backend"), Instant.now().plusSeconds(300))).hasErrors());
        assertTrue(validator.validate(token(ISSUER, List.of("account"), Instant.now().plusSeconds(300))).hasErrors());
        assertTrue(validator.validate(token(ISSUER, null, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void issuerAndExpiryRemainRequiredInBothRolloutModes() {
        for (boolean enabled : new boolean[]{true, false}) {
            var validator = validator(enabled);
            assertTrue(validator.validate(token("https://wrong.test", List.of("siga-backend"), Instant.now().plusSeconds(300))).hasErrors());
            assertTrue(validator.validate(token(ISSUER, List.of("siga-backend"), Instant.now().minusSeconds(120))).hasErrors());
        }
    }

    @Test
    void migrationModePreservesExistingAccessAndRealmRoles() {
        assertFalse(validator(false).validate(token(ISSUER, List.of("account"), Instant.now().plusSeconds(300))).hasErrors());
        var jwt = Jwt.withTokenValue("test-value").header("alg", "RS256").subject("account")
                .claim("realm_access", Map.of("roles", List.of("TEACHER", "STUDENT"))).build();
        var authentication = config.jwtAuthenticationConverter().convert(jwt);
        assertTrue(authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_TEACHER")));
        assertTrue(authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_STUDENT")));
    }
    @Test
    @SuppressWarnings("unchecked")
    void decoderRejectsInvalidSignaturesAndWrongAudiences() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var trusted = generator.generateKeyPair();
        var untrusted = generator.generateKeyPair();
        var decoder = config.jwtDecoder(ISSUER, "https://internal.test/certs", "siga-backend", true);
        var processor = (ConfigurableJWTProcessor<SecurityContext>) ReflectionTestUtils.getField(decoder, "jwtProcessor");
        // Supply local verification keys to exercise the real decoder without a Keycloak network connection.
        processor.setJWSKeySelector((header, context) -> List.of((RSAPublicKey) trusted.getPublic()));
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject("account")
                .audience("siga-backend").expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var valid = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        valid.sign(new RSASSASigner((RSAPrivateKey) trusted.getPrivate()));
        assertEquals("account", decoder.decode(valid.serialize()).getSubject());
        var wrongSignature = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        wrongSignature.sign(new RSASSASigner((RSAPrivateKey) untrusted.getPrivate()));
        assertThrows(JwtException.class, () -> decoder.decode(wrongSignature.serialize()));
        var wrongAudience = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),
                new JWTClaimsSet.Builder(claims).audience("another-api").build());
        wrongAudience.sign(new RSASSASigner((RSAPrivateKey) trusted.getPrivate()));
        assertThrows(JwtException.class, () -> decoder.decode(wrongAudience.serialize()));
    }
}
