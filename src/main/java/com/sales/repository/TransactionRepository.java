package com.sales.repository;

import com.sales.entity.TransactionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<TransactionEntity> {

    public Optional<TransactionEntity> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public List<TransactionEntity> findByUserId(UUID userId) {
        return list("user.id", userId);
    }
}
