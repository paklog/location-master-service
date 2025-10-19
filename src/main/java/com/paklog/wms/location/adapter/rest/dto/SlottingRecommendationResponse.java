package com.paklog.wms.location.adapter.rest.dto;

import com.paklog.wms.location.domain.valueobject.SlottingClass;

public record SlottingRecommendationResponse(
    String locationId,
    SlottingClass currentClass,
    SlottingClass recommendedClass,
    int confidenceScore,
    String reasoning
) {}
