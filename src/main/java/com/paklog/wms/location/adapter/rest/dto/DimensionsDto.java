package com.paklog.wms.location.adapter.rest.dto;

import com.paklog.wms.location.domain.entity.Dimensions;
import java.math.BigDecimal;

public record DimensionsDto(
    BigDecimal length,
    BigDecimal width,
    BigDecimal height,
    String unit
) {
    public static DimensionsDto from(Dimensions dimensions) {
        return new DimensionsDto(
            dimensions.getLength(),
            dimensions.getWidth(),
            dimensions.getHeight(),
            dimensions.getUnit()
        );
    }
}
