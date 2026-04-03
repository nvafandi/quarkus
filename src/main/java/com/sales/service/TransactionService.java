package com.sales.service;

import com.sales.dto.TransactionDTO;
import com.sales.dto.TransactionItemDTO;
import com.sales.entity.ProductEntity;
import com.sales.entity.TransactionEntity;
import com.sales.entity.TransactionItemEntity;
import com.sales.entity.UserEntity;
import com.sales.repository.ProductRepository;
import com.sales.repository.TransactionRepository;
import com.sales.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

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

    public List<TransactionDTO> findAll() {
        return transactionRepository.listAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TransactionDTO findById(UUID id) {
        return transactionRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new NotFoundException("Transaction not found with id: " + id));
    }

    public List<TransactionDTO> findByUserId(UUID userId) {
        return transactionRepository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionDTO create(TransactionDTO transactionDTO) {
        UserEntity user = userRepository.findById(transactionDTO.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + transactionDTO.getUserId()));

        TransactionEntity transaction = new TransactionEntity(null, user, BigDecimal.ZERO, null, null);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (TransactionItemDTO itemDTO : transactionDTO.getItems()) {
            ProductEntity product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + itemDTO.getProductId()));

            if (product.getStock() < itemDTO.getQuantity()) {
                throw new WebApplicationException(
                        "Insufficient stock for product: " + product.getName(),
                        Response.Status.BAD_REQUEST
                );
            }

            product.setStock(product.getStock() - itemDTO.getQuantity());

            TransactionItemEntity item = new TransactionItemEntity(null, null, product, itemDTO.getQuantity(), product.getPrice());
            transaction.addItem(item);

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        transaction.setTotalAmount(totalAmount);
        transactionRepository.persist(transaction);

        return toDTO(transaction);
    }

    @Transactional
    public void delete(UUID id) {
        transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found with id: " + id));
        transactionRepository.deleteById(id);
    }

    private TransactionDTO toDTO(TransactionEntity entity) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setCreatedAt(entity.getCreatedAt());

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
