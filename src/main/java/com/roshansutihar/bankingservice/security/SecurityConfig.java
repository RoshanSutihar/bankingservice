package com.roshansutihar.bankingservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${LOGOUT_URL}")
    private String logoutUrl;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // We need sessions for oauth2Login (browser flow)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // This enables the browser-based login redirect to Keycloak
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak") // Triggers Keycloak login when needed
                        .successHandler(customAuthenticationSuccessHandler()) // Custom redirect based on roles
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
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()

                        // Login endpoint
                        .requestMatchers("/login").permitAll()

                        // Account creation endpoints - different access rules
                        .requestMatchers("/create-account").permitAll() // Public self-registration

                        // Teller admin only endpoints
                        .requestMatchers("/accounts/create", "/accounts/create-business")
                        .hasRole("TELLERADMIN")

                        // Teller deposit endpoints - adjust based on your needs
                        .requestMatchers("/teller-deposit", "/teller/deposit")
                        .hasRole("TELLERADMIN")

                        // Allow POST for account creation - but only for teller admins
                        .requestMatchers(HttpMethod.POST, "/accounts/create", "/accounts/create-business")
                        .hasRole("TELLERADMIN")

                        // Mobile dashboard - accessible to all authenticated users
                        .requestMatchers("/dashboard").authenticated()

                        // These API endpoints require valid JWT (for mobile/frontend)
                        .requestMatchers("/dashboard/api/validate-qr", "/dashboard/api/process-payment")
                        .authenticated()

                        .requestMatchers("/api/mobile/**").authenticated()

                        // Dashboard access for authenticated users (teller admin only)
                        .requestMatchers("/dashboard/**").hasRole("TELLERADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .csrf(csrf -> csrf.disable())

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler()) // Handle 403 Access Denied
                        .authenticationEntryPoint((req, res, ex1) ->
                                res.sendRedirect("/oauth2/authorization/keycloak")) // Redirect to login
                );

        return http.build();
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri(logoutUrl);
        return handler;
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
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            // Delegate to the default OidcUserService
            OidcUser oidcUser = delegate.loadUser(userRequest);

            // Extract roles from OIDC user (Keycloak specific)
            Map<String, Object> claims = oidcUser.getClaims();
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            List<String> roles = new ArrayList<>();

            if (realmAccess != null) {
                roles = (List<String>) realmAccess.get("roles");
            }

            // Convert roles to authorities with ROLE_ prefix
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            if (roles != null) {
                mappedAuthorities = roles.stream()
                        .map(role -> "ROLE_" + role.toUpperCase()) // Convert to uppercase: "telleradmin" -> "ROLE_TELLERADMIN"
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());
            }

            // Add standard OIDC authorities
            mappedAuthorities.addAll(oidcUser.getAuthorities());

            // Return user with authorities
            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {

                // Check if user has TELLERADMIN role
                boolean isTellerAdmin = false;
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String auth = authority.getAuthority();
                    // Check both "ROLE_TELLERADMIN" and "TELLERADMIN" (case-insensitive)
                    if (auth.equalsIgnoreCase("ROLE_TELLERADMIN") ||
                            auth.equalsIgnoreCase("TELLERADMIN")) {
                        isTellerAdmin = true;
                        break;
                    }
                }

                if (isTellerAdmin) {
                    // Redirect teller admin to account creation page
                    response.sendRedirect("/accounts/create");
                } else {
                    // Redirect regular users to mobile dashboard
                    response.sendRedirect("/dashboard");
                }
            }
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request,
                               HttpServletResponse response,
                               AccessDeniedException accessDeniedException) throws IOException {

                // Check if user is authenticated
                if (request.getUserPrincipal() != null) {
                    // Authenticated user but lacking permissions - redirect to appropriate page
                    response.sendRedirect("/dashboard?error=access_denied");
                } else {
                    // Not authenticated - redirect to login
                    response.sendRedirect("/oauth2/authorization/keycloak");
                }
            }
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}