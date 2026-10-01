package com.zidio.keystone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration request. Unlike CreateUserRequest (Admin/Manager only,
 * any role), this always results in a CUSTOMER account - there is deliberately no
 * role field here, so nobody can register themselves in as ADMIN/TECHNICIAN/etc.
 * companyName is optional: if left blank we fall back to "<name>'s Account" so every
 * CUSTOMER user still has a Customer org to belong to (see User.customerId).
 */
public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String companyName
) {}
