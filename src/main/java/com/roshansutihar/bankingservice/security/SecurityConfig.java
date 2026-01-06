package com.roshansutihar.bankingservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // We need sessions for oauth2Login (browser flow)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // This enables the browser-based login redirect to Keycloak
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak") // Triggers Keycloak login when needed
                        .defaultSuccessUrl("/dashboard", true)       // After login, go to dashboard
                )

                // Keep JWT validation for API calls (mobile app, Postman, etc.)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                .authorizeHttpRequests(authz -> authz
                        // Public pages - no login needed
                        .requestMatchers(
                                "/",
                                "/create-account",
                                "/accounts/create",
                                "/accounts/create-business",
                                "/teller-deposit",
                                "/teller/deposit",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Allow POST for teller creation (staff use)
                        .requestMatchers(HttpMethod.POST, "/accounts/create", "/accounts/create-business").permitAll()

                        // These API endpoints require valid JWT (for mobile/frontend)
                        .requestMatchers("/dashboard/api/validate-qr", "/dashboard/api/process-payment")
                        .authenticated()

                        // Dashboard pages require login (will redirect to Keycloak if not logged in)
                        .requestMatchers("/dashboard/**").authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .csrf(csrf -> csrf.disable())

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, ex1) ->
                                res.sendRedirect("/oauth2/authorization/keycloak")) // Nice redirect instead of 401
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}