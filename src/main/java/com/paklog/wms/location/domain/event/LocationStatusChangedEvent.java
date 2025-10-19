package com.paklog.wms.location.domain.event;

import com.paklog.wms.location.domain.valueobject.LocationStatus;
import java.time.LocalDateTime;

/**
 * Domain event published when location status changes
 */
public record LocationStatusChangedEvent(
    String eventId,
    String locationId,
    String warehouseId,
    LocationStatus previousStatus,
    LocationStatus newStatus,
    String reason,
    LocalDateTime occurredAt,
    String updatedBy
) {
    public static LocationStatusChangedEvent of(
            String locationId,
            String warehouseId,
            LocationStatus previousStatus,
            LocationStatus newStatus,
            String reason,
            String updatedBy
    ) {
        return new LocationStatusChangedEvent(
            java.util.UUID.randomUUID().toString(),
            locationId,
            warehouseId,
            previousStatus,
            newStatus,
            reason,
            LocalDateTime.now(),
            updatedBy
        );
    }
}
