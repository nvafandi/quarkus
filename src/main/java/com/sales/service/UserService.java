package com.sales.service;

import com.sales.dto.UserDTO;
import com.sales.entity.UserEntity;
import com.sales.exception.ResourceNotFoundException;
import com.sales.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public UserDTO findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public UserDTO createOrUpdateFromKeycloak(String keycloakId, String username, String role) {
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
    }

    public UserDTO findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional
    public void delete(UUID id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.deleteById(id);
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
