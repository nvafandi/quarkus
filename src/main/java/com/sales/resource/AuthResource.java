package com.sales.resource;

import com.sales.dto.ApiResponse;
import com.sales.dto.LoginRequestDTO;
import com.sales.dto.TokenDTO;
import com.sales.exception.BadRequestException;
import com.sales.service.KeycloakAdminClient;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Authentication, token management, and user info")
public class AuthResource {

    @Inject
    SecurityIdentity identity;

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    // ==================== Token Operations ====================

    @POST
    @Path("/login")
    @Operation(summary = "Login and get tokens", description = "Authenticate user with username/password and return access token + refresh token")
    @APIResponse(responseCode = "200", description = "Login successful, tokens returned",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @APIResponse(responseCode = "401", description = "Invalid username or password")
    public Uni<Response> login(
            @RequestBody(description = "Login credentials", required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDTO.class)))
            @Valid LoginRequestDTO loginRequest) {

        return Uni.createFrom().item(() -> {
            Map<String, String> tokenResponse = keycloakAdminClient.getTokenForUser(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            );

            TokenDTO response = buildTokenDTO(tokenResponse);
            return Response.ok(ApiResponse.ok(response)).build();
        });
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh access token", description = "Use a valid refresh token to get a new access token")
    @APIResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @APIResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public Uni<Response> refreshToken(
            @RequestBody(description = "Refresh token", required = true,
                    content = @Content(schema = @Schema(implementation = TokenDTO.class)))
            @Valid TokenDTO refreshRequest) {

        if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isBlank()) {
            return Uni.createFrom().failure(new BadRequestException("Refresh token is required"));
        }

        return Uni.createFrom().item(() -> {
            Map<String, String> tokenResponse = keycloakAdminClient.refreshToken(refreshRequest.getRefreshToken());
            TokenDTO response = buildTokenDTO(tokenResponse);
            return Response.ok(ApiResponse.ok(response)).build();
        });
    }

    @POST
    @Path("/revoke")
    @Operation(summary = "Revoke refresh token", description = "Revoke a refresh token to invalidate the user session")
    @APIResponse(responseCode = "204", description = "Token revoked successfully")
    @APIResponse(responseCode = "400", description = "Invalid token or failed to revoke")
    public Uni<Response> revokeToken(
            @RequestBody(description = "Refresh token to revoke", required = true,
                    content = @Content(schema = @Schema(implementation = TokenDTO.class)))
            @Valid TokenDTO revokeRequest) {

        if (revokeRequest.getRefreshToken() == null || revokeRequest.getRefreshToken().isBlank()) {
            return Uni.createFrom().failure(new BadRequestException("Refresh token is required"));
        }

        return Uni.createFrom().item(() -> {
            keycloakAdminClient.revokeToken(revokeRequest.getRefreshToken());
            return Response.noContent().build();
        });
    }

    // ==================== User Info Operations ====================

    @GET
    @Path("/userinfo")
    @Operation(summary = "Get current user info", description = "Returns information about the currently authenticated user")
    public Uni<Response> getUserInfo() {
        return Uni.createFrom().item(() -> {
            if (identity.isAnonymous()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(ApiResponse.error(Response.Status.UNAUTHORIZED.getStatusCode(), "Not authenticated"))
                        .build();
            }

            String username = identity.getPrincipal().getName();
            List<String> roles = identity.getRoles().stream().toList();

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("roles", roles);
            userInfo.put("authenticated", true);

            return Response.ok(ApiResponse.ok(userInfo)).build();
        });
    }

    @GET
    @Path("/roles")
    @Operation(summary = "Get current user roles", description = "Returns roles assigned to the currently authenticated user")
    public Uni<Response> getUserRoles() {
        return Uni.createFrom().item(() -> {
            if (identity.isAnonymous()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(ApiResponse.error(Response.Status.UNAUTHORIZED.getStatusCode(), "Not authenticated"))
                        .build();
            }

            List<String> roles = identity.getRoles().stream().toList();
            return Response.ok(ApiResponse.ok(Map.of("roles", roles))).build();
        });
    }

    @GET
    @Path("/check")
    @Operation(summary = "Check authentication", description = "Returns whether the user is authenticated")
    public Uni<Response> checkAuth() {
        return Uni.createFrom().item(() -> {
            boolean authenticated = !identity.isAnonymous();
            return Response.ok(ApiResponse.ok(Map.of("authenticated", authenticated))).build();
        });
    }

    private TokenDTO buildTokenDTO(Map<String, String> tokenResponse) {
        TokenDTO dto = new TokenDTO();
        dto.setAccessToken(tokenResponse.get("accessToken"));
        dto.setRefreshToken(tokenResponse.get("refreshToken"));
        dto.setExpiresIn(tokenResponse.get("expiresIn"));
        dto.setRefreshExpiresIn(tokenResponse.get("refreshExpiresIn"));
        dto.setTokenType(tokenResponse.get("tokenType"));
        dto.setScope(tokenResponse.get("scope"));
        return dto;
    }
}
