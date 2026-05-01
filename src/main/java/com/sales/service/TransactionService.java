package com.sales.service;

import com.sales.dto.TransactionDTO;
import com.sales.dto.TransactionItemDTO;
import com.sales.entity.ProductEntity;
import com.sales.entity.TransactionEntity;
import com.sales.entity.TransactionItemEntity;
import com.sales.entity.UserEntity;
import com.sales.exception.BadRequestException;
import com.sales.exception.ResourceNotFoundException;
import com.sales.repository.ProductRepository;
import com.sales.repository.TransactionRepository;
import com.sales.repository.UserRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TransactionService {

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ProductRepository productRepository;

    public Uni<List<TransactionDTO>> findAll() {
        return Uni.createFrom().item(() ->
            transactionRepository.listAll()
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList())
        );
    }

    public Uni<TransactionDTO> findById(UUID id) {
        return Uni.createFrom().item(() ->
            transactionRepository.findById(id)
                    .map(this::toDTO)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id))
        );
    }

    public Uni<List<TransactionDTO>> findByUserId(UUID userId) {
        return Uni.createFrom().item(() ->
            transactionRepository.findByUserId(userId)
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList())
        );
    }

    @Transactional
    public Uni<TransactionDTO> create(TransactionDTO transactionDTO, UUID userId) {
        return Uni.createFrom().item(() -> {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            TransactionEntity transaction = new TransactionEntity();
            transaction.setUser(user);
            transaction.setTotalAmount(BigDecimal.ZERO);
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (TransactionItemDTO itemDTO : transactionDTO.getItems()) {
                ProductEntity product = productRepository.findById(itemDTO.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemDTO.getProductId()));

                if (product.getStock() < itemDTO.getQuantity()) {
                    throw new BadRequestException("Insufficient stock for product: " + product.getName());
                }

                product.setStock(product.getStock() - itemDTO.getQuantity());

                TransactionItemEntity item = new TransactionItemEntity();
                item.setTransaction(null);
                item.setProduct(product);
                item.setQuantity(itemDTO.getQuantity());
                item.setPrice(product.getPrice());
                transaction.addItem(item);

                totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            }

            transaction.setTotalAmount(totalAmount);
            transactionRepository.persist(transaction);

            return toDTO(transaction);
        });
    }

    @Transactional
    public Uni<Void> delete(UUID id) {
        return Uni.createFrom().item(() -> {
            TransactionEntity entity = transactionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
            transactionRepository.delete(entity);
            return null;
        });
    }

    private TransactionDTO toDTO(TransactionEntity entity) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        List<TransactionItemDTO> itemDTOs = entity.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    private TransactionItemDTO toItemDTO(TransactionItemEntity entity) {
        return new TransactionItemDTO(entity.getProduct().getId(), entity.getQuantity(), entity.getPrice());
    }
}
