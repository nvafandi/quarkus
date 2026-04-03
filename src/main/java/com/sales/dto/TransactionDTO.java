package com.sales.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private UUID id;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    @NotEmpty(message = "Transaction must have at least one item")
    @Valid
    private List<TransactionItemDTO> items;
}
