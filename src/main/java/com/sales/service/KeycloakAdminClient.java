package com.sales.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Startup
@ApplicationScoped
public class KeycloakAdminClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ConfigProperty(name = "keycloak.admin.url", defaultValue = "http://localhost:8180")
    String keycloakUrl;

    @ConfigProperty(name = "keycloak.admin.realm", defaultValue = "master")
    String adminRealm;

    @ConfigProperty(name = "keycloak.admin.username", defaultValue = "admin")
    String adminUsername;

    @ConfigProperty(name = "keycloak.admin.password", defaultValue = "admin")
    String adminPassword;

    @ConfigProperty(name = "keycloak.admin.target-realm", defaultValue = "sales-realm")
    String targetRealm;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String clientSecret;

    private volatile String adminToken;
    private volatile long tokenExpiry;

    private String getAdminToken() {
        if (adminToken != null && System.currentTimeMillis() < tokenExpiry) {
            return adminToken;
        }

        try {
            String tokenUrl = keycloakUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token";
            String body = "client_id=admin-cli"
                    + "&grant_type=password"
                    + "&username=" + adminUsername
                    + "&password=" + adminPassword;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new WebApplicationException("Failed to get Keycloak admin token: " + response.body(),
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            JsonNode json = MAPPER.readTree(response.body());
            adminToken = json.get("access_token").asText();
            int expiresIn = json.get("expires_in").asInt();
            tokenExpiry = System.currentTimeMillis() + (expiresIn - 10) * 1000L;

            return adminToken;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to authenticate with Keycloak: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpRequest.Builder authenticatedRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAdminToken())
                .header("Content-Type", "application/json");
    }

    public List<Map<String, Object>> getAllUsers() {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users";
            HttpRequest request = authenticatedRequest(url).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return MAPPER.readValue(response.body(), List.class);
            }
            throw new WebApplicationException("Failed to fetch users: " + response.body(),
                    Response.Status.fromStatusCode(response.statusCode()));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to fetch users: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> getUserById(String userId) {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users/" + userId;
            HttpRequest request = authenticatedRequest(url).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new jakarta.ws.rs.NotFoundException("Keycloak user not found: " + userId);
            }
            if (response.statusCode() == 200) {
                return MAPPER.readValue(response.body(), Map.class);
            }
            throw new WebApplicationException("Failed to fetch user: " + response.body(),
                    Response.Status.fromStatusCode(response.statusCode()));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to fetch user: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> getUserByUsername(String username) {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users?username=" + username;
            HttpRequest request = authenticatedRequest(url).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Map<String, Object>> users = MAPPER.readValue(response.body(), List.class);
                if (users.isEmpty()) {
                    throw new jakarta.ws.rs.NotFoundException("Keycloak user not found: " + username);
                }
                return users.get(0);
            }
            throw new WebApplicationException("Failed to fetch user: " + response.body(),
                    Response.Status.fromStatusCode(response.statusCode()));
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to fetch user: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> createUser(String username, String password, String email,
                                          boolean emailVerified, List<String> roles) {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users";

            ObjectNode userJson = MAPPER.createObjectNode();
            userJson.put("username", username);
            userJson.put("enabled", true);
            userJson.put("emailVerified", emailVerified);
            if (email != null && !email.isEmpty()) {
                userJson.put("email", email);
            }

            HttpRequest request = authenticatedRequest(url)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(userJson)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                if (response.statusCode() == 409) {
                    throw new WebApplicationException("Username already exists: " + username,
                            Response.Status.CONFLICT);
                }
                throw new WebApplicationException("Failed to create user: " + response.body(),
                        Response.Status.BAD_REQUEST);
            }

            Map<String, Object> createdUser = getUserByUsername(username);
            String userId = (String) createdUser.get("id");

            // Step 2: Set password separately (Keycloak ignores credentials in user creation payload)
            resetPassword(userId, password);

            if (roles != null && !roles.isEmpty()) {
                assignRoles(userId, roles);
                createdUser.put("roles", roles);
            }

            return createdUser;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to create user: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, Object> updateUser(String userId, String username, String email,
                                          boolean emailVerified, Boolean enabled) {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users/" + userId;

            ObjectNode userJson = MAPPER.createObjectNode();
            if (username != null) userJson.put("username", username);
            if (email != null) userJson.put("email", email);
            userJson.put("emailVerified", emailVerified);
            if (enabled != null) userJson.put("enabled", enabled);

            HttpRequest request = authenticatedRequest(url)
                    .method("PUT", HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(userJson)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204) {
                if (response.statusCode() == 404) {
                    throw new jakarta.ws.rs.NotFoundException("Keycloak user not found: " + userId);
                }
                throw new WebApplicationException("Failed to update user: " + response.body(),
                        Response.Status.BAD_REQUEST);
            }

            return getUserById(userId);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to update user: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public void resetPassword(String userId, String newPassword) {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users/" + userId + "/reset-password";

            ObjectNode passwordJson = MAPPER.createObjectNode();
            passwordJson.put("type", "password");
            passwordJson.put("value", newPassword);
            passwordJson.put("temporary", false);

            HttpRequest request = authenticatedRequest(url)
                    .method("PUT", HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(passwordJson)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204) {
                if (response.statusCode() == 404) {
                    throw new jakarta.ws.rs.NotFoundException("Keycloak user not found: " + userId);
                }
                throw new WebApplicationException("Failed to reset password: " + response.body(),
                        Response.Status.BAD_REQUEST);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to reset password: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteUser(String userId) {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users/" + userId;

            HttpRequest request = authenticatedRequest(url)
                    .DELETE()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204) {
                if (response.statusCode() == 404) {
                    throw new jakarta.ws.rs.NotFoundException("Keycloak user not found: " + userId);
                }
                throw new WebApplicationException("Failed to delete user: " + response.body(),
                        Response.Status.BAD_REQUEST);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to delete user: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public List<String> getAvailableRoles() {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/clients";
            HttpRequest request = authenticatedRequest(url).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Map<String, Object>> clients = MAPPER.readValue(response.body(), List.class);
                String salesClientId = null;
                for (Map<String, Object> client : clients) {
                    if ("sales-client".equals(client.get("clientId"))) {
                        salesClientId = (String) client.get("id");
                        break;
                    }
                }

                if (salesClientId == null) {
                    return List.of();
                }

                String rolesUrl = keycloakUrl + "/admin/realms/" + targetRealm + "/clients/" + salesClientId + "/roles";
                HttpRequest rolesRequest = authenticatedRequest(rolesUrl).GET().build();
                HttpResponse<String> rolesResponse = HTTP_CLIENT.send(rolesRequest, HttpResponse.BodyHandlers.ofString());

                if (rolesResponse.statusCode() == 200) {
                    List<Map<String, Object>> roles = MAPPER.readValue(rolesResponse.body(), List.class);
                    return roles.stream()
                            .map(r -> (String) r.get("name"))
                            .toList();
                }
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public void assignRoles(String userId, List<String> roles) {
        try {
            String salesClientId = getSalesClientId();
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users/" + userId + "/role-mappings/clients/" + salesClientId;

            ArrayNode rolesArray = MAPPER.createArrayNode();
            List<String> skippedRoles = new java.util.ArrayList<>();

            for (String roleName : roles) {
                String roleUrl = keycloakUrl + "/admin/realms/" + targetRealm + "/clients/" + salesClientId + "/roles/" + roleName;
                HttpRequest roleRequest = authenticatedRequest(roleUrl).GET().build();
                HttpResponse<String> roleResponse = HTTP_CLIENT.send(roleRequest, HttpResponse.BodyHandlers.ofString());

                if (roleResponse.statusCode() == 200) {
                    JsonNode roleJson = MAPPER.readTree(roleResponse.body());
                    rolesArray.add(roleJson);
                } else {
                    // Log warning and skip missing roles instead of failing
                    System.err.println("[WARN] Role not found: " + roleName + " - skipping");
                    skippedRoles.add(roleName);
                }
            }

            // Only assign if we found at least one valid role
            if (rolesArray.isEmpty()) {
                System.err.println("[WARN] No valid roles found for user " + userId + ". Skipped: " + skippedRoles);
                return;
            }

            HttpRequest request = authenticatedRequest(url)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(rolesArray)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204) {
                throw new WebApplicationException("Failed to assign roles: " + response.body(),
                        Response.Status.BAD_REQUEST);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to assign roles: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public List<String> getUserRoles(String userId) {
        try {
            String salesClientId = getSalesClientId();
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/users/" + userId + "/role-mappings/clients/" + salesClientId;

            HttpRequest request = authenticatedRequest(url).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Map<String, Object>> roles = MAPPER.readValue(response.body(), List.class);
                return roles.stream()
                        .map(r -> (String) r.get("name"))
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String getSalesClientId() {
        try {
            String url = keycloakUrl + "/admin/realms/" + targetRealm + "/clients?clientId=sales-client";
            HttpRequest request = authenticatedRequest(url).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Map<String, Object>> clients = MAPPER.readValue(response.body(), List.class);
                if (!clients.isEmpty()) {
                    return (String) clients.get(0).get("id");
                }
            }
        } catch (Exception e) {
            // ignore
        }
        throw new WebApplicationException("Sales client not found in Keycloak",
                Response.Status.INTERNAL_SERVER_ERROR);
    }

    public Map<String, String> getTokenForUser(String username, String password) {
        try {
            String tokenUrl = keycloakUrl + "/realms/" + targetRealm + "/protocol/openid-connect/token";

            String body = "client_id=sales-client"
                    + "&client_secret=" + clientSecret
                    + "&grant_type=password"
                    + "&username=" + username
                    + "&password=" + password;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401 || response.statusCode() == 400) {
                throw new WebApplicationException("Invalid username or password",
                        Response.Status.UNAUTHORIZED);
            }

            if (response.statusCode() != 200) {
                throw new WebApplicationException("Failed to get token: " + response.body(),
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            JsonNode json = MAPPER.readTree(response.body());
            Map<String, String> tokenResponse = new HashMap<>();
            tokenResponse.put("accessToken", json.get("access_token").asText());
            tokenResponse.put("refreshToken", json.get("refresh_token").asText());
            tokenResponse.put("expiresIn", String.valueOf(json.get("expires_in").asInt()));
            tokenResponse.put("refreshExpiresIn", String.valueOf(json.get("refresh_expires_in").asInt()));
            tokenResponse.put("tokenType", json.get("token_type").asText());
            tokenResponse.put("scope", json.has("scope") ? json.get("scope").asText() : "");

            return tokenResponse;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to authenticate user: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public Map<String, String> refreshToken(String refreshToken) {
        try {
            String tokenUrl = keycloakUrl + "/realms/" + targetRealm + "/protocol/openid-connect/token";

            String body = "client_id=sales-client"
                    + "&client_secret=" + clientSecret
                    + "&grant_type=refresh_token"
                    + "&refresh_token=" + refreshToken;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 400) {
                throw new WebApplicationException("Invalid or expired refresh token",
                        Response.Status.UNAUTHORIZED);
            }

            if (response.statusCode() != 200) {
                throw new WebApplicationException("Failed to refresh token: " + response.body(),
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            JsonNode json = MAPPER.readTree(response.body());
            Map<String, String> tokenResponse = new HashMap<>();
            tokenResponse.put("accessToken", json.get("access_token").asText());
            tokenResponse.put("refreshToken", json.has("refresh_token") ? json.get("refresh_token").asText() : "");
            tokenResponse.put("expiresIn", String.valueOf(json.get("expires_in").asInt()));
            tokenResponse.put("refreshExpiresIn", json.has("refresh_expires_in") ? String.valueOf(json.get("refresh_expires_in").asInt()) : "");
            tokenResponse.put("tokenType", json.get("token_type").asText());
            tokenResponse.put("scope", json.has("scope") ? json.get("scope").asText() : "");

            return tokenResponse;
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to refresh token: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    public void revokeToken(String refreshToken) {
        try {
            String revokeUrl = keycloakUrl + "/realms/" + targetRealm + "/protocol/openid-connect/revoke";

            String body = "client_id=sales-client"
                    + "&client_secret=" + clientSecret
                    + "&token=" + refreshToken
                    + "&token_type_hint=refresh_token";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(revokeUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204 && response.statusCode() != 200) {
                throw new WebApplicationException("Failed to revoke token: " + response.body(),
                        Response.Status.BAD_REQUEST);
            }
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException("Failed to revoke token: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
