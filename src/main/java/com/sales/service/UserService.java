package com.sales.service;

import com.sales.dto.UserDTO;
import com.sales.entity.UserEntity;
import com.sales.exception.ResourceNotFoundException;
import com.sales.repository.UserRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public Uni<UserDTO> findByUsername(String username) {
        return Uni.createFrom().item(() ->
            userRepository.findByUsername(username)
                    .map(this::toDTO)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username))
        );
    }

    @Transactional
    public Uni<UserDTO> createOrUpdateFromKeycloak(String keycloakId, String username, String role) {
        return Uni.createFrom().item(() -> {
            UserEntity entity = userRepository.findByKeycloakId(keycloakId)
                    .orElseGet(() -> {
                        UserEntity newEntity = new UserEntity();
                        newEntity.setKeycloakId(keycloakId);
                        newEntity.setUsername(username);
                        newEntity.setRole(role != null ? role : "USER");
                        return newEntity;
                    });

            entity.setUsername(username);
            if (role != null) {
                entity.setRole(role);
            }

            if (entity.getId() == null) {
                userRepository.persist(entity);
            }

            return toDTO(entity);
        });
    }

    public Uni<UserDTO> findByKeycloakId(String keycloakId) {
        return Uni.createFrom().item(() ->
            userRepository.findByKeycloakId(keycloakId)
                    .map(this::toDTO)
                    .orElse(null)
        );
    }

    @Transactional
    public Uni<Void> delete(UUID id) {
        return Uni.createFrom().item(() -> {
            userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
            userRepository.deleteById(id);
            return null;
        });
    }

    private UserDTO toDTO(UserEntity entity) {
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setKeycloakId(entity.getKeycloakId());
        dto.setUsername(entity.getUsername());
        dto.setRole(entity.getRole());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
