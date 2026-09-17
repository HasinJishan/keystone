package com.zidio.keystone.controller;

import com.zidio.keystone.dto.PartDto;
import com.zidio.keystone.service.PartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
@Tag(name = "Parts")
public class PartController {

    private final PartService partService;

    @GetMapping
    public List<PartDto> all() {
        return partService.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public PartDto create(@Valid @RequestBody PartDto dto) {
        return partService.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PartDto update(@PathVariable Long id, @Valid @RequestBody PartDto dto) {
        return partService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        partService.delete(id);
    }
}
