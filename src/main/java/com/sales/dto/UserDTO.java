package com.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data transfer object")
public class UserDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "User unique identifier (UUID v7)", example = "019d54c7-d83c-7c28-8000-0682ed879d04")
    private UUID id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Keycloak user ID (external auth identifier)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String keycloakId;

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must not exceed 100 characters")
    @Schema(description = "User login name", example = "john_doe")
    private String username;

    @Size(max = 50, message = "Role must not exceed 50 characters")
    @Schema(description = "User role", example = "CASHIER")
    private String role;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;
}
