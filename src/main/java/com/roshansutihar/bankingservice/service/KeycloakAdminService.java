package com.roshansutihar.bankingservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class KeycloakAdminService {

    @Value("${keycloak.base-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-client.id}")
    private String adminClientId;  // e.g., banking-admin-client

    @Value("${keycloak.admin-client.secret}")
    private String adminClientSecret;

    private final RestTemplate restTemplate;

    private String adminToken;
    private Instant tokenExpiry = Instant.now(); // Simple caching

    public KeycloakAdminService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private synchronized void ensureValidToken() {
        if (adminToken == null || Instant.now().isAfter(tokenExpiry.minusSeconds(30))) {
            adminToken = fetchAdminAccessToken();
            // Tokens usually expire in 60-300 seconds; assume 5 min safe
            tokenExpiry = Instant.now().plusSeconds(300);
        }
    }

    private String fetchAdminAccessToken() {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", adminClientId);
        body.add("client_secret", adminClientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            Map<String, Object> respBody = response.getBody();
            if (respBody != null && respBody.containsKey("access_token")) {
                return (String) respBody.get("access_token");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Keycloak admin token", e);
        }

        throw new RuntimeException("Invalid response from Keycloak token endpoint");
    }

    public String createUserInKeycloak(String username, String email, String firstName, String lastName, String tempPassword) {
        ensureValidToken(); // Get fresh token if needed

        String url = keycloakUrl + "/admin/realms/" + realm + "/users";

        Map<String, Object> userRepresentation = new HashMap<>();
        userRepresentation.put("username", username);
        userRepresentation.put("email", email);
        userRepresentation.put("firstName", firstName);
        userRepresentation.put("lastName", lastName);
        userRepresentation.put("enabled", true);
        userRepresentation.put("emailVerified", false);

        // Temporary password - forces change on first login
        List<Map<String, Object>> credentials = new ArrayList<>();
        Map<String, Object> cred = new HashMap<>();
        cred.put("type", "password");
        cred.put("value", tempPassword);
        cred.put("temporary", true);
        credentials.add(cred);
        userRepresentation.put("credentials", credentials);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userRepresentation, headers);

        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            String location = Objects.requireNonNull(response.getHeaders().getLocation()).toString();
            return location.substring(location.lastIndexOf("/") + 1); // Returns Keycloak user ID (sub)
        }

        throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusCode());
    }

    // Add more methods as needed (e.g., update user, reset password, etc.)
}