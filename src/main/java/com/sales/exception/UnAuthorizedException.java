package com.sales.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class UnAuthorizedException extends WebApplicationException {

    public UnAuthorizedException(String message) {
        super(message, Response.Status.UNAUTHORIZED);
    }
}
