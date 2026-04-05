package com.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keycloak user data transfer object")
public class KeycloakUserDTO {

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must not exceed 100 characters")
    @Schema(description = "User login name", example = "john_doe")
    private String username;

    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "User email", example = "john@example.com")
    private String email;

    @Size(max = 100, message = "Password must not exceed 100 characters")
    @Schema(description = "User password (write-only, used for create and password reset)")
    private String password;

    @Schema(description = "Assigned client roles", example = "[\"ADMIN\", \"MANAGER\"]")
    private List<String> roles;
}
