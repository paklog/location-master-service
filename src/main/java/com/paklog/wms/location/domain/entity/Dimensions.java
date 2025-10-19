package com.paklog.wms.location.domain.entity;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Dimensions - Physical dimensions of a location
 */
@Embeddable
public class Dimensions {
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private String unit; // CM, INCH, etc.

    protected Dimensions() {
        // JPA constructor
    }

    public Dimensions(BigDecimal length, BigDecimal width, BigDecimal height, String unit) {
        if (length == null || length.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        if (width == null || width.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Width must be positive");
        }
        if (height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Unit is required");
        }

        this.length = length;
        this.width = width;
        this.height = height;
        this.unit = unit;
    }

    /**
     * Calculate volume
     */
    public BigDecimal calculateVolume() {
        return length.multiply(width).multiply(height);
    }

    /**
     * Calculate floor area
     */
    public BigDecimal calculateFloorArea() {
        return length.multiply(width);
    }

    /**
     * Check if can fit item with given dimensions
     */
    public boolean canFit(Dimensions itemDimensions) {
        if (!this.unit.equals(itemDimensions.unit)) {
            throw new IllegalArgumentException("Cannot compare dimensions with different units");
        }

        return this.length.compareTo(itemDimensions.length) >= 0 &&
               this.width.compareTo(itemDimensions.width) >= 0 &&
               this.height.compareTo(itemDimensions.height) >= 0;
    }

    /**
     * Get dimensions as string
     */
    public String asString() {
        return String.format("%s x %s x %s %s", length, width, height, unit);
    }

    // Getters
    public BigDecimal getLength() {
        return length;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return asString();
    }
}
