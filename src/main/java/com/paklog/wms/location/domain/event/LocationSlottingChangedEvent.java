package com.paklog.wms.location.domain.event;

import com.paklog.wms.location.domain.valueobject.SlottingClass;
import java.time.LocalDateTime;

/**
 * Domain event published when location slotting classification changes
 */
public record LocationSlottingChangedEvent(
    String eventId,
    String locationId,
    String warehouseId,
    String zone,
    SlottingClass previousClass,
    SlottingClass newClass,
    Integer newPickPathSequence,
    LocalDateTime occurredAt,
    String updatedBy,
    String reason
) {
    public static LocationSlottingChangedEvent of(
            String locationId,
            String warehouseId,
            String zone,
            SlottingClass previousClass,
            SlottingClass newClass,
            Integer newPickPathSequence,
            String updatedBy,
            String reason
    ) {
        return new LocationSlottingChangedEvent(
            java.util.UUID.randomUUID().toString(),
            locationId,
            warehouseId,
            zone,
            previousClass,
            newClass,
            newPickPathSequence,
            LocalDateTime.now(),
            updatedBy,
            reason
        );
    }
}
