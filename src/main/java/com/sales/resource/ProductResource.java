package com.sales.resource;

import com.sales.dto.ProductDTO;
import com.sales.dto.UserDTO;
import com.sales.service.ProductService;
import com.sales.service.UserService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
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

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Products", description = "Product management operations (requires authentication)")
public class ProductResource {

    @Inject
    ProductService productService;

    @Inject
    UserService userService;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Operation(summary = "Get all products", description = "Retrieve a list of all products")
    @APIResponse(responseCode = "200", description = "List of products retrieved successfully")
    public Response findAll() {
        return Response.ok(productService.findAll()).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a product by its UUID")
    @APIResponse(responseCode = "200", description = "Product found")
    @APIResponse(responseCode = "404", description = "Product not found")
    public Response findById(
            @Parameter(description = "Product UUID", example = "019d54c8-1ad8-70c8-8000-0686e5e2d186")
            @PathParam("id") UUID id) {
        return Response.ok(productService.findById(id)).build();
    }

    @POST
    @Operation(summary = "Create product", description = "Create a new product")
    @APIResponse(responseCode = "201", description = "Product created successfully")
    public Response create(
            @RequestBody(description = "Product data", required = true,
                    content = @Content(schema = @Schema(implementation = ProductDTO.class)))
            @Valid ProductDTO productDTO) {
        UUID userId = getCurrentUserId();
        ProductDTO created = productService.create(productDTO, userId);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update product", description = "Update an existing product")
    @APIResponse(responseCode = "200", description = "Product updated successfully")
    @APIResponse(responseCode = "404", description = "Product not found")
    public Response update(
            @Parameter(description = "Product UUID") @PathParam("id") UUID id,
            @Valid ProductDTO productDTO) {
        UUID userId = getCurrentUserId();
        return Response.ok(productService.update(id, productDTO, userId)).build();
    }

    private UUID getCurrentUserId() {
        if (securityIdentity != null && !securityIdentity.isAnonymous()) {
            return syncUserIdFromSecurity();
        }
        return null;
    }

    private UUID syncUserIdFromSecurity() {
        try {
            String username = securityIdentity.getPrincipal().getName();
            UserDTO user = userService.findByKeycloakId(username);
            
            if (user != null) {
                return user.getId();
            }
            
            // Auto-create local user record if not exists
            user = userService.createOrUpdateFromKeycloak(username, username, "USER");
            return user.getId();
        } catch (Exception e) {
            // Return null if unable to get user
            return null;
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete product", description = "Delete a product by UUID")
    @APIResponse(responseCode = "204", description = "Product deleted successfully")
    @APIResponse(responseCode = "404", description = "Product not found")
    public Response delete(
            @Parameter(description = "Product UUID") @PathParam("id") UUID id) {
        productService.delete(id);
        return Response.noContent().build();
    }
}
