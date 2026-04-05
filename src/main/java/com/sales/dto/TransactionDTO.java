package com.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction data transfer object")
public class TransactionDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Transaction unique identifier (UUID v7)")
    private UUID id;

    @NotNull(message = "User ID is required")
    @Schema(description = "User who created this transaction", example = "019d54c7-d83c-7c28-8000-0682ed879d04")
    private UUID userId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Total transaction amount (auto-calculated)")
    private BigDecimal totalAmount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;

    @NotEmpty(message = "Transaction must have at least one item")
    @Valid
    @Schema(description = "List of transaction items")
    private List<TransactionItemDTO> items;
}
