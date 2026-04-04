package com.sales.service;

import com.sales.dto.UserDTO;
import com.sales.entity.UserEntity;
import com.sales.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public List<UserDTO> findAll() {
        return userRepository.listAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO findById(UUID id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    public UserDTO findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDTO)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
    }

    @Transactional
    public UserDTO create(UserDTO userDTO) {
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new WebApplicationException("Username already exists", Response.Status.CONFLICT);
        }
        UserEntity entity = new UserEntity();
        entity.setUsername(userDTO.getUsername());
        entity.setRole(userDTO.getRole());
        userRepository.persist(entity);
        return toDTO(entity);
    }

    @Transactional
    public UserDTO update(UUID id, UserDTO userDTO) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        entity.setUsername(userDTO.getUsername());
        entity.setRole(userDTO.getRole());
        return toDTO(entity);
    }

    @Transactional
    public void delete(UUID id) {
        userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        userRepository.deleteById(id);
    }

    private UserDTO toDTO(UserEntity entity) {
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setRole(entity.getRole());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
