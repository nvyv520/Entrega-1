
package SIGA.backend.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
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

                        // Sections
                        .requestMatchers(HttpMethod.GET, "/api/sections", "/api/sections/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        .requestMatchers("/api/sections", "/api/sections/**")
                                .hasRole("ADMIN")

                        // Period matriculations
                        .requestMatchers("/api/period-matriculations", "/api/period-matriculations/**")
                                .hasRole("ADMIN")

                        // Enrollments - temporary protection
                        .requestMatchers("/api/enrollments", "/api/enrollments/**")
                                .hasRole("ADMIN")

                        // Grades - temporary protection
                        .requestMatchers("/api/grades", "/api/grades/**")
                                .hasRole("ADMIN")

                        // Deny other API endpoints
                        .requestMatchers("/api/**").denyAll()

                        .anyRequest().authenticated()
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
