package com.sales.resource;

import com.sales.dto.TransactionDTO;
import com.sales.service.TransactionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @GET
    public Response findAll() {
        return Response.ok(transactionService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") UUID id) {
        return Response.ok(transactionService.findById(id)).build();
    }

    @GET
    @Path("/user/{userId}")
    public Response findByUserId(@PathParam("userId") UUID userId) {
        return Response.ok(transactionService.findByUserId(userId)).build();
    }

    @POST
    public Response create(@Valid TransactionDTO transactionDTO) {
        TransactionDTO created = transactionService.create(transactionDTO);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        transactionService.delete(id);
        return Response.noContent().build();
    }
}
