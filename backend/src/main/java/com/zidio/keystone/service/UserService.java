package com.zidio.keystone.service;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.dto.UserDto;
import com.zidio.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto> byRole(Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role && u.isActive())
                .map(u -> new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole().name()))
                .toList();
    }
}
