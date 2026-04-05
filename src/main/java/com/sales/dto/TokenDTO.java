package com.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token response DTO")
public class TokenDTO {

    @Schema(description = "Access token (JWT)")
    private String accessToken;

    @Schema(description = "Refresh token (JWT)")
    private String refreshToken;

    @Schema(description = "Access token expiry in seconds", example = "300")
    private String expiresIn;

    @Schema(description = "Refresh token expiry in seconds", example = "1800")
    private String refreshExpiresIn;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token scopes")
    private String scope;
}
