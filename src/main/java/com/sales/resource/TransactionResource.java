package com.sales.resource;

import com.sales.dto.TransactionDTO;
import com.sales.service.TransactionService;
import com.sales.util.SecurityContextHelper;
import io.quarkus.security.Authenticated;
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
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Transactions", description = "Transaction management operations (requires authentication)")
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    SecurityContextHelper securityContextHelper;

    @GET
    @Operation(summary = "Get all transactions", description = "Retrieve a list of all transactions")
    @APIResponse(responseCode = "200", description = "List of transactions retrieved successfully")
    public Uni<Response> findAll() {
        return transactionService.findAll()
                .map(transactions -> Response.ok(transactions).build());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieve a transaction by its UUID")
    @APIResponse(responseCode = "200", description = "Transaction found")
    @APIResponse(responseCode = "404", description = "Transaction not found")
    public Uni<Response> findById(
            @Parameter(description = "Transaction UUID", example = "019d54c9-84c0-77ae-8000-069c7822b826")
            @PathParam("id") UUID id) {
        return transactionService.findById(id)
                .map(transaction -> Response.ok(transaction).build());
    }

    @GET
    @Path("/user/{userId}")
    @Operation(summary = "Get transactions by user", description = "Retrieve all transactions for a specific user")
    @APIResponse(responseCode = "200", description = "List of user transactions")
    public Uni<Response> findByUserId(
            @Parameter(description = "User UUID") @PathParam("userId") UUID userId) {
        return transactionService.findByUserId(userId)
                .map(transactions -> Response.ok(transactions).build());
    }

    @POST
    @Operation(summary = "Create transaction", description = "Create a new sales transaction with items")
    @APIResponse(responseCode = "201", description = "Transaction created successfully")
    @APIResponse(responseCode = "400", description = "Insufficient stock or invalid data")
    @APIResponse(responseCode = "404", description = "User or product not found")
    public Uni<Response> create(
            @RequestBody(description = "Transaction data with items", required = true,
                    content = @Content(schema = @Schema(implementation = TransactionDTO.class)))
            @Valid TransactionDTO transactionDTO) {
        UUID userId = securityContextHelper.extractUserId(securityIdentity);
        return transactionService.create(transactionDTO, userId)
                .map(created -> Response.status(Response.Status.CREATED).entity(created).build());
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete transaction", description = "Delete a transaction by UUID")
    @APIResponse(responseCode = "204", description = "Transaction deleted successfully")
    @APIResponse(responseCode = "404", description = "Transaction not found")
    public Uni<Response> delete(
            @Parameter(description = "Transaction UUID") @PathParam("id") UUID id) {
        return transactionService.delete(id)
                .map(v -> Response.noContent().build());
    }
}
