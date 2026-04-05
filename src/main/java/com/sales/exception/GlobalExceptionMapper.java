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

        if (exception instanceof UnAuthorizedException) {
            UnAuthorizedException ex = (UnAuthorizedException) exception;
            status = Response.Status.UNAUTHORIZED.getStatusCode();
            message = ex.getMessage() != null ? ex.getMessage() : "Unauthorized";
        } else if (exception instanceof BadRequestException) {
            BadRequestException ex = (BadRequestException) exception;
            status = Response.Status.BAD_REQUEST.getStatusCode();
            message = ex.getMessage() != null ? ex.getMessage() : "Bad request";
        } else if (exception instanceof ConflictException) {
            ConflictException ex = (ConflictException) exception;
            status = Response.Status.CONFLICT.getStatusCode();
            message = ex.getMessage() != null ? ex.getMessage() : "Conflict";
        } else if (exception instanceof ResourceNotFoundException) {
            ResourceNotFoundException ex = (ResourceNotFoundException) exception;
            status = Response.Status.NOT_FOUND.getStatusCode();
            message = ex.getMessage() != null ? ex.getMessage() : "Resource not found";
        } else if (exception instanceof ServerErrorException) {
            ServerErrorException ex = (ServerErrorException) exception;
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            message = ex.getMessage() != null ? ex.getMessage() : "Internal server error";
        } else if (exception instanceof jakarta.ws.rs.NotAuthorizedException) {
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
