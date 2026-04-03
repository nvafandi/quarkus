package com.sales.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "Sales System API",
        version = "1.0.0",
        description = "REST API for managing users, products, and sales transactions with UUID v7 identifiers",
        contact = @Contact(name = "Sales System Team"),
        license = @License(name = "Apache 2.0")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development server")
    }
)
public class OpenAPIConfig {
}
