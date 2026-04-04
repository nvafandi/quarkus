package com.sales.dto;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        int status = responseContext.getStatus();
        Object entity = responseContext.getEntity();

        // Skip if entity is null or already an ApiResponse
        if (entity == null || entity instanceof ApiResponse) {
            return;
        }

        // Wrap 2xx success responses (skip 204 No Content)
        if (status >= 200 && status < 300) {
            if (status == Response.Status.NO_CONTENT.getStatusCode()) {
                return; // Skip 204 - no content to wrap
            }
            
            // Use appropriate factory method based on status code
            ApiResponse<?> wrappedResponse;
            if (status == Response.Status.CREATED.getStatusCode()) {
                wrappedResponse = ApiResponse.created(entity);
            } else {
                wrappedResponse = ApiResponse.ok(entity);
            }
            responseContext.setEntity(wrappedResponse);
        }
        // Wrap 4xx and 5xx error responses that aren't already ApiResponse
        else if (status >= 400 && status < 600) {
            String message = "Error occurred";
            
            // Try to extract message from entity if it's a Map with "error" or "message" key
            if (entity instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) entity;
                if (map.containsKey("message")) {
                    message = (String) map.get("message");
                } else if (map.containsKey("error")) {
                    message = (String) map.get("error");
                }
            }
            
            responseContext.setEntity(ApiResponse.error(status, message));
        }
    }
}
