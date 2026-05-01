package com.sales.resource;

import com.sales.dto.KeycloakUserDTO;
import com.sales.dto.UserDTO;
import com.sales.exception.BadRequestException;
import com.sales.service.KeycloakAdminClient;
import com.sales.service.UserSyncService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Management", description = "Manage users in Keycloak and local database")
public class UserResource {

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    UserSyncService userSyncService;

    @GET
    @Operation(summary = "List all Keycloak users", description = "Returns all users from Keycloak sales-realm")
    @APIResponse(responseCode = "200", description = "List of Keycloak users")
    public Response listUsers() {
        List<Map<String, Object>> users = keycloakAdminClient.getAllUsers();
        return Response.ok(users).build();
    }

    @GET
    @Path("/roles")
    @Operation(summary = "List available roles", description = "Returns all available client roles for sales-client")
    @APIResponse(responseCode = "200", description = "List of available roles")
    public Response listAvailableRoles() {
        List<String> roles = keycloakAdminClient.getAvailableRoles();
        return Response.ok(Map.of("roles", roles)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Keycloak user by ID", description = "Returns a single user from Keycloak by ID")
    @APIResponse(responseCode = "200", description = "User found")
    @APIResponse(responseCode = "404", description = "User not found")
    public Uni<Response> getUserById(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            Map<String, Object> user = keycloakAdminClient.getUserById(id);
            List<String> roles = keycloakAdminClient.getUserRoles(id);
            user.put("roles", roles);
            return Response.ok(user).build();
        });
    }

    @GET
    @Path("/username/{username}")
    @Operation(summary = "Get Keycloak user by username", description = "Returns a single user from Keycloak by username")
    @APIResponse(responseCode = "200", description = "User found")
    @APIResponse(responseCode = "404", description = "User not found")
    public Uni<Response> getUserByUsername(
            @Parameter(description = "Username", required = true)
            @PathParam("username") String username) {
        return Uni.createFrom().item(() -> {
            Map<String, Object> user = keycloakAdminClient.getUserByUsername(username);
            String userId = (String) user.get("id");
            List<String> roles = keycloakAdminClient.getUserRoles(userId);
            user.put("roles", roles);
            return Response.ok(user).build();
        });
    }

    @POST
    @Operation(summary = "Create Keycloak user", description = "Creates a new user in Keycloak with password and roles, and syncs to local database")
    @APIResponse(responseCode = "201", description = "User created and synced to local database",
            content = @Content(schema = @Schema(implementation = KeycloakUserDTO.class)))
    @APIResponse(responseCode = "400", description = "Invalid input")
    @APIResponse(responseCode = "409", description = "Username already exists")
    public Uni<Response> createUser(@Valid KeycloakUserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            return Uni.createFrom().failure(new BadRequestException("Password is required"));
        }

        return Uni.createFrom().item(() ->
            keycloakAdminClient.createUser(
                    dto.getUsername(),
                    dto.getPassword(),
                    dto.getEmail(),
                    true,
                    dto.getRoles()
            )
        ).chain(created -> userSyncService.syncUserFromKeycloak(created))
         .map(syncedUser -> Response.status(Response.Status.CREATED).entity(syncedUser).build());
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update Keycloak user", description = "Updates user details in Keycloak (username, email, enabled status)")
    @APIResponse(responseCode = "200", description = "User updated")
    @APIResponse(responseCode = "404", description = "User not found")
    public Uni<Response> updateUser(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id,
            @Valid KeycloakUserDTO dto) {
        return Uni.createFrom().item(() -> {
            Map<String, Object> updated = keycloakAdminClient.updateUser(
                    id,
                    dto.getUsername(),
                    dto.getEmail(),
                    true,
                    true
            );
            return Response.ok(updated).build();
        });
    }

    @PUT
    @Path("/{id}/password")
    @Operation(summary = "Reset Keycloak user password", description = "Resets the password for a Keycloak user")
    @APIResponse(responseCode = "204", description = "Password reset successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    @APIResponse(responseCode = "400", description = "Invalid input")
    public Uni<Response> resetPassword(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id,
            @Valid KeycloakUserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            return Uni.createFrom().failure(new BadRequestException("Password is required"));
        }

        return Uni.createFrom().item(() -> {
            keycloakAdminClient.resetPassword(id, dto.getPassword());
            return Response.noContent().build();
        });
    }

    @PUT
    @Path("/{id}/roles")
    @Operation(summary = "Assign roles to Keycloak user", description = "Assigns client roles to a Keycloak user")
    @APIResponse(responseCode = "204", description = "Roles assigned")
    @APIResponse(responseCode = "404", description = "User not found")
    public Uni<Response> assignRoles(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id,
            Map<String, List<String>> body) {
        List<String> roles = body.get("roles");
        if (roles == null) {
            return Uni.createFrom().failure(new BadRequestException("Roles list is required"));
        }

        return Uni.createFrom().item(() -> {
            keycloakAdminClient.assignRoles(id, roles);
            return Response.noContent().build();
        });
    }

    @GET
    @Path("/{id}/roles")
    @Operation(summary = "Get Keycloak user roles", description = "Returns client roles assigned to a Keycloak user")
    @APIResponse(responseCode = "200", description = "List of roles")
    @APIResponse(responseCode = "404", description = "User not found")
    public Uni<Response> getUserRoles(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id) {
        return Uni.createFrom().item(() -> {
            List<String> roles = keycloakAdminClient.getUserRoles(id);
            return Response.ok(Map.of("userId", id, "roles", roles)).build();
        });
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Keycloak user", description = "Permanently deletes a user from Keycloak and local database")
    @APIResponse(responseCode = "204", description = "User deleted from both Keycloak and local database")
    @APIResponse(responseCode = "404", description = "User not found")
    public Uni<Response> deleteUser(@PathParam("id") String id) {
        return Uni.createFrom().voidItem()
                .call(() -> {
                    keycloakAdminClient.deleteUser(id);
                    return Uni.createFrom().voidItem();
                })
                .chain(() -> userSyncService.deleteUserByKeycloakId(id))
                .map(v -> Response.noContent().build());
    }

}
