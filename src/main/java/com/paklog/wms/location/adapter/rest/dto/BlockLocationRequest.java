package com.paklog.wms.location.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockLocationRequest(
    @NotBlank(message = "Reason is required")
    String reason
) {}
