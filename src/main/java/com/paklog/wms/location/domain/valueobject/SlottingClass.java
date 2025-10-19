package com.paklog.wms.location.domain.valueobject;

/**
 * Slotting Class - ABC classification for inventory velocity and location assignment
 */
public enum SlottingClass {
    A("High velocity - 80% of picks, 20% of SKUs"),
    B("Medium velocity - 15% of picks, 30% of SKUs"),
    C("Low velocity - 5% of picks, 50% of SKUs"),
    FAST_MOVER("Very high velocity - critical items"),
    SLOW_MOVER("Very low velocity - rare picks"),
    SEASONAL("Seasonal items"),
    HAZMAT("Hazardous materials"),
    OVERSIZED("Oversized items requiring special handling"),
    MIXED("Mixed velocity items");

    private final String description;

    SlottingClass(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get recommended pick path priority (lower = higher priority)
     */
    public int getPickPathPriority() {
        return switch (this) {
            case FAST_MOVER -> 1;
            case A -> 2;
            case B -> 3;
            case C -> 4;
            case SLOW_MOVER -> 5;
            case SEASONAL -> 3;
            case HAZMAT -> 6;
            case OVERSIZED -> 6;
            case MIXED -> 3;
        };
    }

    /**
     * Get recommended distance from dock (zones)
     */
    public int getRecommendedDistanceFromDock() {
        return switch (this) {
            case FAST_MOVER, A -> 1;  // Closest to dock
            case B, SEASONAL -> 2;
            case C -> 3;
            case SLOW_MOVER, OVERSIZED -> 4;  // Farthest from dock
            case HAZMAT -> 5;  // Special zone
            case MIXED -> 2;
        };
    }

    /**
     * Check if requires special handling
     */
    public boolean requiresSpecialHandling() {
        return this == HAZMAT || this == OVERSIZED;
    }
}
