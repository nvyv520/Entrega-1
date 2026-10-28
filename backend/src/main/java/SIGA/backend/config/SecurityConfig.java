
package SIGA.backend.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/careers", "/api/careers/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        .requestMatchers("/api/careers", "/api/careers/**")
                                .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/study-plans", "/api/study-plans/**")
                                .hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                        .requestMatchers("/api/study-plans", "/api/study-plans/**")
                                .hasRole("ADMIN")

                        // Student
                        .requestMatchers("/api/students", "/api/students/**")
                                .hasRole("ADMIN")

                        // Teachers
                        .requestMatchers("/api/teachers", "/api/teachers/**")
                                .hasRole("ADMIN")
                                
                        .requestMatchers("/api/**").authenticated()
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
}
