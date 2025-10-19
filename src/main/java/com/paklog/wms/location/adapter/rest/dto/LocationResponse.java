package com.paklog.wms.location.adapter.rest.dto;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;

import java.time.LocalDateTime;
import java.util.Map;

public record LocationResponse(
    String locationId,
    String warehouseId,
    String locationName,
    LocationType type,
    LocationStatus status,
    String parentLocationId,
    Integer hierarchyLevel,
    String zone,
    String aisle,
    String bay,
    String level,
    String position,
    SlottingClass slottingClass,
    Integer distanceFromDock,
    Integer pickPathSequence,
    CapacityDto capacity,
    DimensionsDto dimensions,
    Map<String, String> attributes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static LocationResponse from(LocationMaster location) {
        return new LocationResponse(
            location.getLocationId(),
            location.getWarehouseId(),
            location.getLocationName(),
            location.getType(),
            location.getStatus(),
            location.getParentLocationId(),
            location.getHierarchyLevel(),
            location.getZone(),
            location.getAisle(),
            location.getBay(),
            location.getLevel(),
            location.getPosition(),
            location.getSlottingClass(),
            location.getDistanceFromDock(),
            location.getPickPathSequence(),
            location.getCapacity() != null ?
                CapacityDto.from(location.getCapacity()) : null,
            location.getDimensions() != null ?
                DimensionsDto.from(location.getDimensions()) : null,
            location.getAttributes(),
            location.getCreatedAt(),
            location.getUpdatedAt()
        );
    }
}
