package com.sales.exception;

import com.sales.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        int status;
        String message;

        if (exception instanceof jakarta.ws.rs.NotAuthorizedException) {
            status = Response.Status.UNAUTHORIZED.getStatusCode();
            message = "Unauthorized - valid authentication required";
        } else if (exception instanceof WebApplicationException) {
            WebApplicationException webEx = (WebApplicationException) exception;
            status = webEx.getResponse().getStatus();
            message = webEx.getMessage() != null ? webEx.getMessage() : "Web application error";
        } else if (exception instanceof ConstraintViolationException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            ConstraintViolationException cve = (ConstraintViolationException) exception;
            message = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            message = "Internal server error";
        }

        return Response.status(status)
                .entity(ApiResponse.error(status, message))
                .build();
    }
}
