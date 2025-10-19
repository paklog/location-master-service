package com.paklog.wms.location.adapter.rest.dto;

public record SlottingOptimizationResponse(
    String warehouseId,
    String zone,
    int locationsUpdated,
    String message
) {}
