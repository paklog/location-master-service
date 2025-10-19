package com.paklog.wms.location.domain.event;

import com.paklog.wms.location.domain.valueobject.LocationType;
import java.time.LocalDateTime;

/**
 * Domain event published when a new location is created
 */
public record LocationCreatedEvent(
    String eventId,
    String locationId,
    String warehouseId,
    String locationName,
    LocationType type,
    String parentLocationId,
    Integer hierarchyLevel,
    String zone,
    LocalDateTime occurredAt,
    String createdBy
) {
    public static LocationCreatedEvent of(
            String locationId,
            String warehouseId,
            String locationName,
            LocationType type,
            String parentLocationId,
            Integer hierarchyLevel,
            String zone,
            String createdBy
    ) {
        return new LocationCreatedEvent(
            java.util.UUID.randomUUID().toString(),
            locationId,
            warehouseId,
            locationName,
            type,
            parentLocationId,
            hierarchyLevel,
            zone,
            LocalDateTime.now(),
            createdBy
        );
    }
}
