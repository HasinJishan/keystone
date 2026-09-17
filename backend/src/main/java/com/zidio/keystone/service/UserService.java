package com.zidio.keystone.service;

import com.zidio.keystone.domain.Permission;
import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.dto.CreateUserRequest;
import com.zidio.keystone.dto.UpdateUserRequest;
import com.zidio.keystone.dto.UserDto;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Per the mentor's Admin permission list: Admin creates the login credentials for every
 * employee (dispatcher, technician, manager) - there is no public self-registration.
 * Manager can additionally create/update users but not delete them; only Admin deletes.
 * Every write here is additionally checked against RolePermissions, on top of the
 * @PreAuthorize role gate on the controller, so the permission matrix is enforced in
 * exactly one place rather than duplicated per method.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public List<UserDto> byRole(Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role && u.isActive())
                .map(this::toDto)
                .toList();
    }

    public List<UserDto> all() {
        currentUser.requirePermission(Permission.VIEW_USER);
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        currentUser.requirePermission(Permission.CREATE_USER);
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new IllegalArgumentException("A user with this email already exists: " + req.email());
        }
        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .customerId(req.role() == Role.CUSTOMER ? req.customerId() : null)
                .active(true)
                .build();
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest req) {
        currentUser.requirePermission(Permission.UPDATE_USER);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setName(req.name());
        user.setActive(req.active());
        return toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        currentUser.requirePermission(Permission.DELETE_USER);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }
}
