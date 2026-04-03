package com.sales.repository;

import com.sales.entity.ProductEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<ProductEntity> {

    public Optional<ProductEntity> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public boolean deleteById(UUID id) {
        return delete("id", id) > 0;
    }
}
