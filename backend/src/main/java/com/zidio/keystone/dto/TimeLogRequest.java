package com.zidio.keystone.dto;

import jakarta.validation.constraints.Positive;

public record TimeLogRequest(@Positive int minutes, String note) {}
