package com.paklog.wms.location.domain.valueobject;

/**
 * Location Status - Operational status of a location
 */
public enum LocationStatus {
    ACTIVE("Location is active and available"),
    INACTIVE("Location is temporarily inactive"),
    BLOCKED("Location is blocked (damage, maintenance)"),
    RESERVED("Location is reserved for specific purpose"),
    FULL("Location is at capacity"),
    DECOMMISSIONED("Location is permanently decommissioned");

    private final String description;

    LocationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if location can accept inventory
     */
    public boolean canAcceptInventory() {
        return this == ACTIVE || this == RESERVED;
    }

    /**
     * Check if location can release inventory
     */
    public boolean canReleaseInventory() {
        return switch (this) {
            case ACTIVE, FULL, RESERVED -> true;
            case INACTIVE, BLOCKED, DECOMMISSIONED -> false;
        };
    }

    /**
     * Check if location is operational
     */
    public boolean isOperational() {
        return this == ACTIVE || this == FULL || this == RESERVED;
    }

    /**
     * Check if location requires attention
     */
    public boolean requiresAttention() {
        return this == BLOCKED || this == FULL;
    }
}
