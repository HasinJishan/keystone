package com.zidio.keystone.controller;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.dto.CreateUserRequest;
import com.zidio.keystone.dto.UpdateUserRequest;
import com.zidio.keystone.dto.UserDto;
import com.zidio.keystone.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User management, per the Admin/Manager permission split covered in training:
 * Admin can create, update, view, AND delete users. Manager can create, update, and view,
 * but not delete - matching "they don't have delete... only admin having the delete feature."
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','MANAGER')")
    public List<UserDto> byRole(@RequestParam(required = false) Role role) {
        return role != null ? userService.byRole(role) : userService.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return userService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return userService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
