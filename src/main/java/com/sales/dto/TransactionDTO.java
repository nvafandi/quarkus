package com.sales.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TransactionDTO {

    private UUID id;

    @NotNull(message = "User ID is required")
    private UUID userId;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    @NotEmpty(message = "Transaction must have at least one item")
    @Valid
    private List<TransactionItemDTO> items;

    public TransactionDTO() {}

    public TransactionDTO(UUID userId, List<TransactionItemDTO> items) {
        this.userId = userId;
        this.items = items;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<TransactionItemDTO> getItems() {
        return items;
    }

    public void setItems(List<TransactionItemDTO> items) {
        this.items = items;
    }
}
