package com.sales.repository;

import com.sales.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepository<UserEntity> {

    public Optional<UserEntity> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public Optional<UserEntity> findByKeycloakId(String keycloakId) {
        return find("keycloakId", keycloakId).firstResultOptional();
    }

    public Optional<UserEntity> findById(UUID id) {
        return find("id", id).firstResultOptional();
    }

    public boolean deleteById(UUID id) {
        return delete("id", id) > 0;
    }
}
