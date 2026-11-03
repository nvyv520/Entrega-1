
package SIGA.backend.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()

                        // Careers
                        .requestMatchers(HttpMethod.GET, "/api/careers", "/api/careers/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        .requestMatchers("/api/careers", "/api/careers/**")
                                .hasRole("ADMIN")

                        // Study plans
                        .requestMatchers(HttpMethod.GET, "/api/study-plans", "/api/study-plans/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        .requestMatchers("/api/study-plans", "/api/study-plans/**")
                                .hasRole("ADMIN")

                        // Students - own information
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/students/me",
                                "/api/students/me/academic-history"
                        )
                                .hasRole("STUDENT")

                        // Students - administration
                        .requestMatchers("/api/students", "/api/students/**")
                                .hasRole("ADMIN")

                        // Teachers - own information
                        .requestMatchers(HttpMethod.GET, "/api/teachers/me")
                                .hasRole("TEACHER")

                        // Teachers - administration
                        .requestMatchers("/api/teachers", "/api/teachers/**")
                                .hasRole("ADMIN")

                        // Subjects
                        .requestMatchers(HttpMethod.GET, "/api/subjects", "/api/subjects/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        .requestMatchers("/api/subjects", "/api/subjects/**")
                                .hasRole("ADMIN")

                        // Academic periods
                        .requestMatchers(HttpMethod.GET, "/api/academic-periods", "/api/academic-periods/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        .requestMatchers("/api/academic-periods", "/api/academic-periods/**")
                                .hasRole("ADMIN")

                        // Sections - authenticated teacher
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/sections/me",
                                "/api/sections/me/*/students"
                        )
                                .hasRole("TEACHER")

                        // Sections - academic catalog
                        .requestMatchers(HttpMethod.GET,
                                "/api/sections",
                                "/api/sections/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        // Sections - administration
                        .requestMatchers("/api/sections", "/api/sections/**")
                                .hasRole("ADMIN")


                        // Period matriculations
                        .requestMatchers("/api/period-matriculations", "/api/period-matriculations/**")
                                .hasRole("ADMIN")

                        // Enrollments - authenticated student
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/me")
                                .hasRole("STUDENT")

                        .requestMatchers(HttpMethod.POST, "/api/enrollments/me")
                                .hasRole("STUDENT")

                        .requestMatchers(HttpMethod.PATCH, "/api/enrollments/me/*/cancel")
                                .hasRole("STUDENT")

                        
                        // Verify student enrollment selection
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/enrollments/me/academic-period/*/verify",
                                "/api/enrollments/me/academic-period/*/discard"
                        )
                                .hasRole("STUDENT")

                        // Student selection status and academically eligible catalogue
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/enrollments/me/academic-period/*/verification-status",
                                "/api/enrollments/me/academic-period/*/eligible-sections"
                        )
                                .hasRole("STUDENT")


                        // Enrollments - administration
                        .requestMatchers(HttpMethod.GET, "/api/enrollments", "/api/enrollments/**")
                                .hasRole("ADMIN")

                        .requestMatchers("/api/enrollments", "/api/enrollments/**")
                                .denyAll()

                        // Grades - authenticated student
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/grades/me"
                        )
                                .hasRole("STUDENT")

                        // Grades - authenticated teacher section
                        .requestMatchers(HttpMethod.GET, "/api/grades/me/sections/*")
                                .hasRole("TEACHER")

                        // Grades - create
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/grades"
                        )
                                .hasRole("TEACHER")

                        // Grades - update
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/grades/*"
                        )
                                .hasRole("TEACHER")

                        // Grades - administration
                        .requestMatchers(
                                "/api/grades",
                                "/api/grades/**"
                        )
                                .hasRole("ADMIN")


                        // Deny other API endpoints
                        .requestMatchers("/api/**").denyAll()

                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }

    // Use internal JWK access while still validating the browser-facing issuer.
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${siga.security.jwt.audience}") String audience,
            @Value("${siga.security.jwt.validate-audience}") boolean validateAudience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        var issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("The SIGA API audience must not be empty");
        }
        if (validateAudience) {
            var audienceValidator = new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD, audiences -> audiences != null && audiences.contains(audience));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        } else {
            decoder.setJwtValidator(issuerValidator);
            LoggerFactory.getLogger(SecurityConfig.class).warn(
                    "JWT audience validation is disabled pending Keycloak audience mapper setup");
        }
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(this::extractRoles);

        return converter;
    }

    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess != null) {

            Object roles = realmAccess.get("roles");

            if (roles instanceof Collection<?> roleList) {

                for (Object role : roleList) {

                    if (role instanceof String roleName) {
                        authorities.add(
                                new SimpleGrantedAuthority("ROLE_" + roleName)
                        );
                    }
                }
            }
        }

        return authorities;
    }

    // CORS configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
