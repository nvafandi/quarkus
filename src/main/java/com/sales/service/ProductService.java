package com.sales.service;

import com.sales.dto.ProductDTO;
import com.sales.entity.ProductEntity;
import com.sales.exception.ResourceNotFoundException;
import com.sales.repository.ProductRepository;
import io.smallrye.mutiny.Uni;
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

    public Uni<List<ProductDTO>> findAll() {
        return Uni.createFrom().item(() ->
            productRepository.listAll()
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList())
        );
    }

    public Uni<ProductDTO> findById(UUID id) {
        return Uni.createFrom().item(() ->
            productRepository.findById(id)
                    .map(this::toDTO)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id))
        );
    }

    @Transactional
    public Uni<ProductDTO> create(ProductDTO productDTO, UUID userId) {
        return Uni.createFrom().item(() -> {
            ProductEntity entity = new ProductEntity();
            entity.setName(productDTO.getName());
            entity.setPrice(productDTO.getPrice());
            entity.setStock(productDTO.getStock());
            entity.setCreatedBy(userId);
            entity.setUpdatedBy(userId);
            productRepository.persist(entity);
            return toDTO(entity);
        });
    }

    @Transactional
    public Uni<ProductDTO> update(UUID id, ProductDTO productDTO, UUID userId) {
        return Uni.createFrom().item(() -> {
            ProductEntity entity = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
            entity.setName(productDTO.getName());
            entity.setPrice(productDTO.getPrice());
            entity.setStock(productDTO.getStock());
            entity.setUpdatedBy(userId);
            return toDTO(entity);
        });
    }

    @Transactional
    public Uni<Void> delete(UUID id) {
        return Uni.createFrom().item(() -> {
            productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
            productRepository.deleteById(id);
            return null;
        });
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
