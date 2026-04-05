package com.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction item data transfer object")
public class TransactionItemDTO {

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product UUID", example = "019d54c8-1ad8-70c8-8000-0686e5e2d186")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Item quantity", example = "2")
    private Integer quantity;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Item price at time of purchase (auto-set from product)")
    private BigDecimal price;
}
