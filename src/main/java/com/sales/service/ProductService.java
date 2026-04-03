package com.sales.service;

import com.sales.dto.ProductDTO;
import com.sales.entity.ProductEntity;
import com.sales.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

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
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public ProductDTO create(ProductDTO productDTO) {
        ProductEntity entity = new ProductEntity();
        entity.setName(productDTO.getName());
        entity.setPrice(productDTO.getPrice());
        entity.setStock(productDTO.getStock());
        productRepository.persist(entity);
        return toDTO(entity);
    }

    @Transactional
    public ProductDTO update(UUID id, ProductDTO productDTO) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
        entity.setName(productDTO.getName());
        entity.setPrice(productDTO.getPrice());
        entity.setStock(productDTO.getStock());
        return toDTO(entity);
    }

    @Transactional
    public void delete(UUID id) {
        productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
        productRepository.deleteById(id);
    }

    @Transactional
    public void decreaseStock(UUID productId, int quantity) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));
        if (entity.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        entity.setStock(entity.getStock() - quantity);
    }

    private ProductDTO toDTO(ProductEntity entity) {
        return new ProductDTO(entity.getId(), entity.getName(), entity.getPrice(), entity.getStock(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
