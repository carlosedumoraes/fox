package com.fox.service;

import com.fox.dto.UserAdminResponse;
import com.fox.dto.UpdateUserRequest;
import com.fox.dto.UserResponse;
import com.fox.entity.Role;
import com.fox.entity.User;
import com.fox.exception.BadRequestApiException;
import com.fox.exception.EmailAlreadyExistsException;
import com.fox.exception.ResourceNotFoundException;
import com.fox.mapper.UserMapper;
import com.fox.repository.RoleRepository;
import com.fox.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public List<UserAdminResponse> findActiveUsers() {
        return userRepository.findByDeletedAtIsNull().stream()
                .map(UserMapper::toAdminResponse)
                .toList();
    }

    @Transactional
    public UserResponse disable(UUID id) {
        User user = findById(id);
        user.setActive(false);
        User savedUser = userRepository.save(user);
        refreshTokenService.revokeActiveTokens(savedUser);
        return UserMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse enable(UUID id) {
        User user = findById(id);
        user.setActive(true);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findById(id);
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        user.setName(request.getName().trim());
        user.setEmail(email);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void softDelete(UUID id) {
        User user = findById(id);
        user.setDeletedAt(LocalDateTime.now());
        user.setActive(false);
        User savedUser = userRepository.save(user);
        refreshTokenService.revokeActiveTokens(savedUser);
    }

    @Transactional
    public UserResponse updateRoles(UUID id, List<String> requestedRoles) {
        User user = findById(id);
        Set<String> roleNames = requestedRoles.stream()
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (roleNames.isEmpty()) {
            throw new BadRequestApiException("At least one valid role is required");
        }

        List<Role> roles = roleRepository.findByNameIn(roleNames);
        Set<String> foundRoleNames = roles.stream().map(Role::getName).collect(Collectors.toSet());

        if (foundRoleNames.size() != roleNames.size()) {
            Set<String> missingRoles = new LinkedHashSet<>(roleNames);
            missingRoles.removeAll(foundRoleNames);
            throw new ResourceNotFoundException("Roles not found: " + String.join(", ", missingRoles));
        }

        user.getRoles().clear();
        user.getRoles().addAll(roles);
        return UserMapper.toResponse(userRepository.save(user));
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
