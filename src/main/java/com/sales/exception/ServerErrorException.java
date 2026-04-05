package com.sales.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ServerErrorException extends WebApplicationException {

    public ServerErrorException(String message) {
        super(message, Response.Status.INTERNAL_SERVER_ERROR);
    }
}
