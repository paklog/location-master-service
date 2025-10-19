package com.paklog.wms.location.adapter.rest.dto;

import com.paklog.wms.location.domain.valueobject.SlottingClass;
import jakarta.validation.constraints.NotNull;

public record UpdateSlottingRequest(
    @NotNull(message = "Slotting class is required")
    SlottingClass slottingClass,

    String reason
) {}
