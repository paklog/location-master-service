package com.paklog.wms.location.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event published when location capacity is updated
 */
public record LocationCapacityChangedEvent(
    String eventId,
    String locationId,
    String warehouseId,
    Integer previousMaxQuantity,
    Integer newMaxQuantity,
    BigDecimal previousMaxWeight,
    BigDecimal newMaxWeight,
    BigDecimal previousMaxVolume,
    BigDecimal newMaxVolume,
    LocalDateTime occurredAt,
    String updatedBy,
    String reason
) {
    public static LocationCapacityChangedEvent of(
            String locationId,
            String warehouseId,
            Integer previousMaxQuantity,
            Integer newMaxQuantity,
            BigDecimal previousMaxWeight,
            BigDecimal newMaxWeight,
            BigDecimal previousMaxVolume,
            BigDecimal newMaxVolume,
            String updatedBy,
            String reason
    ) {
        return new LocationCapacityChangedEvent(
            java.util.UUID.randomUUID().toString(),
            locationId,
            warehouseId,
            previousMaxQuantity,
            newMaxQuantity,
            previousMaxWeight,
            newMaxWeight,
            previousMaxVolume,
            newMaxVolume,
            LocalDateTime.now(),
            updatedBy,
            reason
        );
    }
}
