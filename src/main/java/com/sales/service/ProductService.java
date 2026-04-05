package com.sales.service;

import com.sales.dto.ProductDTO;
import com.sales.entity.ProductEntity;
import com.sales.exception.ResourceNotFoundException;
import com.sales.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductService {

    @Inject
    ProductRepository productRepository;

    public List<ProductDTO> findAll() {
        return productRepository.listAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO findById(UUID id) {
        return productRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public ProductDTO create(ProductDTO productDTO, UUID userId) {
        ProductEntity entity = new ProductEntity();
        entity.setName(productDTO.getName());
        entity.setPrice(productDTO.getPrice());
        entity.setStock(productDTO.getStock());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        productRepository.persist(entity);
        return toDTO(entity);
    }

    @Transactional
    public ProductDTO update(UUID id, ProductDTO productDTO, UUID userId) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        entity.setName(productDTO.getName());
        entity.setPrice(productDTO.getPrice());
        entity.setStock(productDTO.getStock());
        entity.setUpdatedBy(userId);
        return toDTO(entity);
    }

    @Transactional
    public void delete(UUID id) {
        productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.deleteById(id);
    }

    private ProductDTO toDTO(ProductEntity entity) {
        return new ProductDTO(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getStock(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy()
        );
    }
}
