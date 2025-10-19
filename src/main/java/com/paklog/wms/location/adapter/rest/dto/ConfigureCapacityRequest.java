package com.paklog.wms.location.adapter.rest.dto;

import com.paklog.wms.location.domain.entity.Capacity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ConfigureCapacityRequest(
    @NotNull(message = "Max quantity is required")
    @Positive(message = "Max quantity must be positive")
    Integer maxQuantity,

    @NotNull(message = "Max weight is required")
    @Positive(message = "Max weight must be positive")
    BigDecimal maxWeight,

    @NotNull(message = "Max volume is required")
    @Positive(message = "Max volume must be positive")
    BigDecimal maxVolume,

    String weightUnit,
    String volumeUnit,
    String reason
) {
    public Capacity toCapacity() {
        return new Capacity(
            maxQuantity,
            maxWeight,
            maxVolume,
            weightUnit != null ? weightUnit : "KG",
            volumeUnit != null ? volumeUnit : "M3"
        );
    }
}
