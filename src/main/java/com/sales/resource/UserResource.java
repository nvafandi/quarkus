package com.sales.resource;

import com.sales.dto.UserDTO;
import com.sales.service.UserService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @GET
    public Response findAll() {
        return Response.ok(userService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") UUID id) {
        return Response.ok(userService.findById(id)).build();
    }

    @POST
    public Response create(@Valid UserDTO userDTO) {
        UserDTO created = userService.create(userDTO);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, @Valid UserDTO userDTO) {
        return Response.ok(userService.update(id, userDTO)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        userService.delete(id);
        return Response.noContent().build();
    }
}
