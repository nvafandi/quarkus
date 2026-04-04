package com.sales.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product data transfer object")
public class ProductDTO {

    @Schema(description = "Product unique identifier (UUID v7)")
    private UUID id;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    @Schema(description = "Product name", example = "Laptop Pro 15")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(description = "Product price", example = "1500000.00")
    private BigDecimal price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be non-negative")
    @Schema(description = "Available stock quantity", example = "50")
    private Integer stock;

    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "User who created this product")
    private UUID createdBy;

    @Schema(description = "User who last updated this product")
    private UUID updatedBy;
}
