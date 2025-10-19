package com.paklog.wms.location.domain.valueobject;

/**
 * Location Type - Defines the type of location in the warehouse hierarchy
 */
public enum LocationType {
    WAREHOUSE("Warehouse facility"),
    ZONE("Functional zone within warehouse"),
    AISLE("Storage aisle"),
    BAY("Storage bay within aisle"),
    LEVEL("Storage level/shelf"),
    BIN("Individual storage bin/position"),
    DOOR("Shipping/receiving door"),
    STAGING("Staging area"),
    WORK_STATION("Work station/put wall");

    private final String description;

    LocationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this location type can contain other locations
     */
    public boolean canHaveChildren() {
        return switch (this) {
            case WAREHOUSE, ZONE, AISLE, BAY, LEVEL -> true;
            case BIN, DOOR, STAGING, WORK_STATION -> false;
        };
    }

    /**
     * Check if this location type can store inventory
     */
    public boolean canStoreInventory() {
        return switch (this) {
            case BIN, STAGING -> true;
            case WAREHOUSE, ZONE, AISLE, BAY, LEVEL, DOOR, WORK_STATION -> false;
        };
    }

    /**
     * Get typical parent location type
     */
    public LocationType getTypicalParent() {
        return switch (this) {
            case WAREHOUSE -> null;
            case ZONE -> WAREHOUSE;
            case AISLE -> ZONE;
            case BAY -> AISLE;
            case LEVEL -> BAY;
            case BIN -> LEVEL;
            case DOOR -> WAREHOUSE;
            case STAGING -> ZONE;
            case WORK_STATION -> ZONE;
        };
    }

    /**
     * Get hierarchy depth (0 = root)
     */
    public int getHierarchyDepth() {
        return switch (this) {
            case WAREHOUSE -> 0;
            case ZONE, DOOR -> 1;
            case AISLE, STAGING, WORK_STATION -> 2;
            case BAY -> 3;
            case LEVEL -> 4;
            case BIN -> 5;
        };
    }
}
