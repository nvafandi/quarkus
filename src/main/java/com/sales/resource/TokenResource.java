package com.sales.resource;

import com.sales.dto.ApiResponse;
import com.sales.dto.LoginRequestDTO;
import com.sales.dto.TokenDTO;
import com.sales.dto.UserDTO;
import com.sales.service.KeycloakAdminClient;
import com.sales.service.UserSyncService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Token Management", description = "Token operations: login, refresh, revoke")
public class TokenResource {

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    UserSyncService userSyncService;

    @POST
    @Path("/login")
    @Operation(
        summary = "Login and get tokens",
        description = "Authenticate user with username/password and return access token + refresh token"
    )
    @APIResponse(responseCode = "200", description = "Login successful, tokens returned",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @APIResponse(responseCode = "401", description = "Invalid username or password")
    public Response login(
            @RequestBody(description = "Login credentials", required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDTO.class)))
            @Valid LoginRequestDTO loginRequest) {

        Map<String, String> tokenResponse = keycloakAdminClient.getTokenForUser(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        );

        // Sync user to local database after successful login
        try {
            userSyncService.syncUserByUsername(loginRequest.getUsername());
        } catch (Exception e) {
            // Log warning but don't fail the login - user will be synced on first API access
            System.err.println("[WARN] Failed to sync user during login: " + e.getMessage());
        }

        TokenDTO response = new TokenDTO();
        response.setAccessToken(tokenResponse.get("accessToken"));
        response.setRefreshToken(tokenResponse.get("refreshToken"));
        response.setExpiresIn(tokenResponse.get("expiresIn"));
        response.setRefreshExpiresIn(tokenResponse.get("refreshExpiresIn"));
        response.setTokenType(tokenResponse.get("tokenType"));
        response.setScope(tokenResponse.get("scope"));

        return Response.ok(ApiResponse.ok(response)).build();
    }

    @POST
    @Path("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Use a valid refresh token to get a new access token (and optionally a new refresh token)"
    )
    @APIResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @APIResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public Response refreshToken(
            @RequestBody(description = "Refresh token", required = true,
                    content = @Content(schema = @Schema(implementation = TokenDTO.class)))
            @Valid TokenDTO refreshRequest) {

        if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isBlank()) {
            throw new WebApplicationException("Refresh token is required", Response.Status.BAD_REQUEST);
        }

        Map<String, String> tokenResponse = keycloakAdminClient.refreshToken(
                refreshRequest.getRefreshToken()
        );

        TokenDTO response = new TokenDTO();
        response.setAccessToken(tokenResponse.get("accessToken"));
        response.setRefreshToken(tokenResponse.get("refreshToken"));
        response.setExpiresIn(tokenResponse.get("expiresIn"));
        response.setRefreshExpiresIn(tokenResponse.get("refreshExpiresIn"));
        response.setTokenType(tokenResponse.get("tokenType"));
        response.setScope(tokenResponse.get("scope"));

        return Response.ok(ApiResponse.ok(response)).build();
    }

    @POST
    @Path("/revoke")
    @Operation(
        summary = "Revoke refresh token",
        description = "Revoke a refresh token to invalidate the user session"
    )
    @APIResponse(responseCode = "204", description = "Token revoked successfully")
    @APIResponse(responseCode = "400", description = "Invalid token or failed to revoke")
    public Response revokeToken(
            @RequestBody(description = "Refresh token to revoke", required = true,
                    content = @Content(schema = @Schema(implementation = TokenDTO.class)))
            @Valid TokenDTO revokeRequest) {

        if (revokeRequest.getRefreshToken() == null || revokeRequest.getRefreshToken().isBlank()) {
            throw new WebApplicationException("Refresh token is required", Response.Status.BAD_REQUEST);
        }

        keycloakAdminClient.revokeToken(revokeRequest.getRefreshToken());

        return Response.noContent().build();
    }
}
