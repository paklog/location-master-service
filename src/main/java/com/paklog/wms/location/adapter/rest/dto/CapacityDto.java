package com.paklog.wms.location.adapter.rest.dto;

import com.paklog.wms.location.domain.entity.Capacity;
import java.math.BigDecimal;

public record CapacityDto(
    Integer maxQuantity,
    BigDecimal maxWeight,
    BigDecimal maxVolume,
    String weightUnit,
    String volumeUnit,
    Integer currentQuantity,
    BigDecimal currentWeight,
    BigDecimal currentVolume,
    BigDecimal utilizationPercentage
) {
    public static CapacityDto from(Capacity capacity) {
        return new CapacityDto(
            capacity.getMaxQuantity(),
            capacity.getMaxWeight(),
            capacity.getMaxVolume(),
            capacity.getWeightUnit(),
            capacity.getVolumeUnit(),
            capacity.getCurrentQuantity(),
            capacity.getCurrentWeight(),
            capacity.getCurrentVolume(),
            capacity.getUtilizationPercentage()
        );
    }
}
