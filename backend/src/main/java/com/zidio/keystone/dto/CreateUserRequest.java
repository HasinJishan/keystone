package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull Role role,
        Long customerId // only meaningful when role == CUSTOMER
) {}
