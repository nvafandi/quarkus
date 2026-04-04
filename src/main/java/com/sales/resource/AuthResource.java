package com.sales.resource;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Authentication and user info")
public class AuthResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/userinfo")
    @Operation(summary = "Get current user info", description = "Returns information about the currently authenticated user")
    public Response getUserInfo() {
        if (identity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Not authenticated"))
                    .build();
        }

        String username = identity.getPrincipal().getName();
        List<String> roles = identity.getRoles().stream().toList();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("roles", roles);
        userInfo.put("authenticated", true);

        return Response.ok(userInfo).build();
    }

    @GET
    @Path("/roles")
    @Operation(summary = "Get current user roles", description = "Returns roles assigned to the currently authenticated user")
    public Response getUserRoles() {
        if (identity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<String> roles = identity.getRoles().stream().toList();
        return Response.ok(Map.of("roles", roles)).build();
    }

    @GET
    @Path("/check")
    @Operation(summary = "Check authentication", description = "Returns whether the user is authenticated")
    public Response checkAuth() {
        boolean authenticated = !identity.isAnonymous();
        return Response.ok(Map.of("authenticated", authenticated)).build();
    }
}
