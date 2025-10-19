package com.paklog.wms.location.adapter.rest.dto;

import com.paklog.wms.location.domain.valueobject.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLocationRequest(
    @NotBlank(message = "Location ID is required")
    String locationId,

    @NotBlank(message = "Warehouse ID is required")
    String warehouseId,

    @NotBlank(message = "Location name is required")
    String locationName,

    @NotNull(message = "Location type is required")
    LocationType type,

    String parentLocationId,

    Integer hierarchyLevel,

    String zone
) {}
