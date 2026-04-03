package com.sales.resource;

import com.sales.dto.UserDTO;
import com.sales.service.UserService;
import io.quarkus.security.Authenticated;
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

import java.util.List;
import java.util.UUID;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User management operations")
public class UserResource {

    @Inject
    UserService userService;

    @GET
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @APIResponse(responseCode = "200", description = "List of users retrieved successfully")
    public Response findAll() {
        return Response.ok(userService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a user by their UUID")
    @APIResponse(responseCode = "200", description = "User found")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response findById(
            @Parameter(description = "User UUID", example = "019d54c7-d83c-7c28-8000-0682ed879d04")
            @PathParam("id") UUID id) {
        return Response.ok(userService.findById(id)).build();
    }

    @POST
    @Operation(summary = "Create user", description = "Create a new user account")
    @APIResponse(responseCode = "201", description = "User created successfully")
    @APIResponse(responseCode = "409", description = "Username already exists")
    public Response create(
            @RequestBody(description = "User data", required = true,
                    content = @Content(schema = @Schema(implementation = UserDTO.class)))
            @Valid UserDTO userDTO) {
        UserDTO created = userService.create(userDTO);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    @APIResponse(responseCode = "200", description = "User updated successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response update(
            @Parameter(description = "User UUID") @PathParam("id") UUID id,
            @Valid UserDTO userDTO) {
        return Response.ok(userService.update(id, userDTO)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by UUID")
    @APIResponse(responseCode = "204", description = "User deleted successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response delete(
            @Parameter(description = "User UUID") @PathParam("id") UUID id) {
        userService.delete(id);
        return Response.noContent().build();
    }
}
