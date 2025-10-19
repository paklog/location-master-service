package com.paklog.wms.location.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ReserveLocationRequest(
    @NotBlank(message = "Purpose is required")
    String purpose
) {}
