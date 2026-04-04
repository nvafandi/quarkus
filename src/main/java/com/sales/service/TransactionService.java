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

    public List<TransactionDTO> findAll() {
        return transactionRepository.listAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public TransactionDTO findById(UUID id) {
        return transactionRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + transactionDTO.getUserId()));

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
    }

    @Transactional
    public void delete(UUID id) {
        transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        transactionRepository.deleteById(id);
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
