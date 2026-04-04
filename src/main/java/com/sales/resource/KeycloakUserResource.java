package com.sales.resource;

import com.sales.dto.KeycloakUserDTO;
import com.sales.service.KeycloakAdminClient;
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

@Path("/api/keycloak/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Keycloak User Management", description = "Manage users in Keycloak via Admin API")
public class KeycloakUserResource {

    @Inject
    KeycloakAdminClient keycloakAdminClient;

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
    public Response getUserById(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id) {
        Map<String, Object> user = keycloakAdminClient.getUserById(id);
        List<String> roles = keycloakAdminClient.getUserRoles(id);
        user.put("roles", roles);
        return Response.ok(user).build();
    }

    @GET
    @Path("/username/{username}")
    @Operation(summary = "Get Keycloak user by username", description = "Returns a single user from Keycloak by username")
    @APIResponse(responseCode = "200", description = "User found")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response getUserByUsername(
            @Parameter(description = "Username", required = true)
            @PathParam("username") String username) {
        Map<String, Object> user = keycloakAdminClient.getUserByUsername(username);
        String userId = (String) user.get("id");
        List<String> roles = keycloakAdminClient.getUserRoles(userId);
        user.put("roles", roles);
        return Response.ok(user).build();
    }

    @POST
    @Operation(summary = "Create Keycloak user", description = "Creates a new user in Keycloak with password and roles")
    @APIResponse(responseCode = "201", description = "User created",
            content = @Content(schema = @Schema(implementation = KeycloakUserDTO.class)))
    @APIResponse(responseCode = "400", description = "Invalid input")
    @APIResponse(responseCode = "409", description = "Username already exists")
    public Response createUser(@Valid KeycloakUserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new WebApplicationException("Password is required", Response.Status.BAD_REQUEST);
        }

        boolean emailVerified = dto.getEmailVerified() != null && dto.getEmailVerified();
        Map<String, Object> created = keycloakAdminClient.createUser(
                dto.getUsername(),
                dto.getPassword(),
                dto.getEmail(),
                emailVerified,
                dto.getRoles()
        );

        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update Keycloak user", description = "Updates user details in Keycloak (username, email, enabled status)")
    @APIResponse(responseCode = "200", description = "User updated")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response updateUser(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id,
            @Valid KeycloakUserDTO dto) {
        Map<String, Object> updated = keycloakAdminClient.updateUser(
                id,
                dto.getUsername(),
                dto.getEmail(),
                dto.getEmailVerified() != null && dto.getEmailVerified(),
                dto.getEnabled()
        );
        return Response.ok(updated).build();
    }

    @PUT
    @Path("/{id}/password")
    @Operation(summary = "Reset Keycloak user password", description = "Resets the password for a Keycloak user")
    @APIResponse(responseCode = "204", description = "Password reset successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    @APIResponse(responseCode = "400", description = "Invalid input")
    public Response resetPassword(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id,
            @Valid KeycloakUserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new WebApplicationException("Password is required", Response.Status.BAD_REQUEST);
        }

        keycloakAdminClient.resetPassword(id, dto.getPassword());
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/roles")
    @Operation(summary = "Assign roles to Keycloak user", description = "Assigns client roles to a Keycloak user")
    @APIResponse(responseCode = "204", description = "Roles assigned")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response assignRoles(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id,
            Map<String, List<String>> body) {
        List<String> roles = body.get("roles");
        if (roles == null) {
            throw new WebApplicationException("Roles list is required", Response.Status.BAD_REQUEST);
        }

        keycloakAdminClient.assignRoles(id, roles);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/roles")
    @Operation(summary = "Get Keycloak user roles", description = "Returns client roles assigned to a Keycloak user")
    @APIResponse(responseCode = "200", description = "List of roles")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response getUserRoles(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id) {
        List<String> roles = keycloakAdminClient.getUserRoles(id);
        return Response.ok(Map.of("userId", id, "roles", roles)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Keycloak user", description = "Permanently deletes a user from Keycloak")
    @APIResponse(responseCode = "204", description = "User deleted")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response deleteUser(
            @Parameter(description = "Keycloak user ID", required = true)
            @PathParam("id") String id) {
        keycloakAdminClient.deleteUser(id);
        return Response.noContent().build();
    }
}
