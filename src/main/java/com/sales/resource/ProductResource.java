package com.sales.resource;

import com.sales.dto.ProductDTO;
import com.sales.service.ProductService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject
    ProductService productService;

    @GET
    public Response findAll() {
        return Response.ok(productService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") UUID id) {
        return Response.ok(productService.findById(id)).build();
    }

    @POST
    public Response create(@Valid ProductDTO productDTO) {
        ProductDTO created = productService.create(productDTO);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, @Valid ProductDTO productDTO) {
        return Response.ok(productService.update(id, productDTO)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        productService.delete(id);
        return Response.noContent().build();
    }
}
