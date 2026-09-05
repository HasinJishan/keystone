package com.zidio.keystone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PartDto(
        Long id,
        @NotBlank String name,
        @NotBlank String sku,
        @NotNull BigDecimal unitCost,
        @NotNull Integer stockQty
) {}
