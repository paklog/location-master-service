package com.paklog.wms.location.domain.aggregate;

import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.entity.Dimensions;
import com.paklog.wms.location.domain.entity.Restrictions;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * LocationMaster - Aggregate root for warehouse location configuration
 *
 * Represents a location in the warehouse hierarchy with its configuration,
 * capacity, dimensions, restrictions, and slotting classification.
 */
@Entity
@Table(name = "location_master", indexes = {
    @Index(name = "idx_warehouse_id", columnList = "warehouseId"),
    @Index(name = "idx_parent_location", columnList = "parentLocationId"),
    @Index(name = "idx_zone", columnList = "zone"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_type_status", columnList = "type,status"),
    @Index(name = "idx_slotting_class", columnList = "slottingClass")
})
public class LocationMaster {

    @Id
    private String locationId; // e.g., "WH-001-A-01-02-03"

    @Column(nullable = false)
    private String warehouseId; // e.g., "WH-001"

    @Column(nullable = false)
    private String locationName; // Human-readable name

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationStatus status;

    // Hierarchy
    private String parentLocationId;

    @Column(nullable = false)
    private Integer hierarchyLevel; // 0 = warehouse, 1 = zone, etc.

    private String zone; // Functional zone (PICK, PACK, STAGING, etc.)

    // Physical location in warehouse
    private String aisle;
    private String bay;
    private String level;
    private String position;

    // Configuration
    @Embedded
    private Dimensions dimensions;

    @Embedded
    private Capacity capacity;

    @Embedded
    private Restrictions restrictions;

    // Slotting
    @Enumerated(EnumType.STRING)
    private SlottingClass slottingClass;

    private Integer distanceFromDock; // In meters or feet

    private Integer pickPathSequence; // Order in pick path

    // Coordinates for mapping/routing
    private Double xCoordinate;
    private Double yCoordinate;
    private Double zCoordinate;

    // Metadata
    @ElementCollection
    @CollectionTable(name = "location_attributes",
                     joinColumns = @JoinColumn(name = "location_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    private Map<String, String> attributes;

    // Audit
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;

    @Version
    private Long version;

    protected LocationMaster() {
        // JPA constructor
    }

    /**
     * Create a new location
     */
    public static LocationMaster create(
            String locationId,
            String warehouseId,
            String locationName,
            LocationType type,
            String parentLocationId,
            Integer hierarchyLevel,
            String zone
    ) {
        LocationMaster location = new LocationMaster();
        location.locationId = locationId;
        location.warehouseId = warehouseId;
        location.locationName = locationName;
        location.type = type;
        location.status = LocationStatus.ACTIVE;
        location.parentLocationId = parentLocationId;
        location.hierarchyLevel = hierarchyLevel != null ? hierarchyLevel : type.getHierarchyDepth();
        location.zone = zone;
        location.attributes = new HashMap<>();
        location.createdAt = LocalDateTime.now();
        location.updatedAt = LocalDateTime.now();

        // Initialize defaults
        location.restrictions = Restrictions.createDefault();
        location.slottingClass = SlottingClass.MIXED;

        return location;
    }

    /**
     * Configure location dimensions
     */
    public void configureDimensions(Dimensions dimensions) {
        if (dimensions == null) {
            throw new IllegalArgumentException("Dimensions cannot be null");
        }
        this.dimensions = dimensions;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Configure location capacity
     */
    public void configureCapacity(Capacity capacity) {
        if (capacity == null) {
            throw new IllegalArgumentException("Capacity cannot be null");
        }
        this.capacity = capacity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Configure location restrictions
     */
    public void configureRestrictions(Restrictions restrictions) {
        if (restrictions == null) {
            throw new IllegalArgumentException("Restrictions cannot be null");
        }
        this.restrictions = restrictions;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set slotting classification
     */
    public void setSlottingClass(SlottingClass slottingClass) {
        if (slottingClass == null) {
            throw new IllegalArgumentException("Slotting class cannot be null");
        }
        this.slottingClass = slottingClass;
        this.pickPathSequence = slottingClass.getPickPathPriority() * 1000; // Base sequence
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set pick path sequence
     */
    public void setPickPathSequence(Integer sequence) {
        if (sequence != null && sequence < 0) {
            throw new IllegalArgumentException("Pick path sequence must be non-negative");
        }
        this.pickPathSequence = sequence;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set physical location coordinates
     */
    public void setCoordinates(Double x, Double y, Double z) {
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.zCoordinate = z;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set aisle/bay/level/position
     */
    public void setPhysicalAddress(String aisle, String bay, String level, String position) {
        this.aisle = aisle;
        this.bay = bay;
        this.level = level;
        this.position = position;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set distance from dock
     */
    public void setDistanceFromDock(Integer distance) {
        if (distance != null && distance < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
        this.distanceFromDock = distance;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Activate location
     */
    public void activate() {
        if (status == LocationStatus.DECOMMISSIONED) {
            throw new IllegalStateException("Cannot activate decommissioned location");
        }
        this.status = LocationStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate location
     */
    public void deactivate() {
        this.status = LocationStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Block location
     */
    public void block(String reason) {
        this.status = LocationStatus.BLOCKED;
        this.setAttribute("block_reason", reason);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Unblock location
     */
    public void unblock() {
        if (status != LocationStatus.BLOCKED) {
            throw new IllegalStateException("Location is not blocked");
        }
        this.status = LocationStatus.ACTIVE;
        this.removeAttribute("block_reason");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark location as full
     */
    public void markAsFull() {
        this.status = LocationStatus.FULL;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark location as available
     */
    public void markAsAvailable() {
        if (status == LocationStatus.FULL) {
            this.status = LocationStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Reserve location
     */
    public void reserve(String purpose) {
        this.status = LocationStatus.RESERVED;
        this.setAttribute("reservation_purpose", purpose);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Release reservation
     */
    public void releaseReservation() {
        if (status != LocationStatus.RESERVED) {
            throw new IllegalStateException("Location is not reserved");
        }
        this.status = LocationStatus.ACTIVE;
        this.removeAttribute("reservation_purpose");
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Decommission location permanently
     */
    public void decommission(String reason) {
        this.status = LocationStatus.DECOMMISSIONED;
        this.setAttribute("decommission_reason", reason);
        this.setAttribute("decommissioned_at", LocalDateTime.now().toString());
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Set custom attribute
     */
    public void setAttribute(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Attribute key cannot be null or blank");
        }
        this.attributes.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Remove attribute
     */
    public void removeAttribute(String key) {
        this.attributes.remove(key);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get attribute value
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Check if location can accept inventory
     */
    public boolean canAcceptInventory() {
        return status.canAcceptInventory() && type.canStoreInventory();
    }

    /**
     * Check if location can have child locations
     */
    public boolean canHaveChildren() {
        return type.canHaveChildren();
    }

    /**
     * Validate location hierarchy
     */
    public void validateHierarchy() {
        if (hierarchyLevel != type.getHierarchyDepth()) {
            throw new IllegalStateException(
                String.format("Hierarchy level %d does not match type %s (expected %d)",
                    hierarchyLevel, type, type.getHierarchyDepth())
            );
        }

        if (hierarchyLevel > 0 && parentLocationId == null) {
            throw new IllegalStateException("Non-root location must have a parent");
        }

        if (hierarchyLevel == 0 && parentLocationId != null) {
            throw new IllegalStateException("Root location cannot have a parent");
        }
    }

    /**
     * Get full location path
     */
    public String getFullPath() {
        StringBuilder path = new StringBuilder(locationName);
        if (aisle != null) path.append("/").append(aisle);
        if (bay != null) path.append("/").append(bay);
        if (level != null) path.append("/").append(level);
        if (position != null) path.append("/").append(position);
        return path.toString();
    }

    // Getters
    public String getLocationId() {
        return locationId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getLocationName() {
        return locationName;
    }

    public LocationType getType() {
        return type;
    }

    public LocationStatus getStatus() {
        return status;
    }

    public String getParentLocationId() {
        return parentLocationId;
    }

    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    public String getZone() {
        return zone;
    }

    public String getAisle() {
        return aisle;
    }

    public String getBay() {
        return bay;
    }

    public String getLevel() {
        return level;
    }

    public String getPosition() {
        return position;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public Capacity getCapacity() {
        return capacity;
    }

    public Restrictions getRestrictions() {
        return restrictions;
    }

    public SlottingClass getSlottingClass() {
        return slottingClass;
    }

    public Integer getDistanceFromDock() {
        return distanceFromDock;
    }

    public Integer getPickPathSequence() {
        return pickPathSequence;
    }

    public Double getXCoordinate() {
        return xCoordinate;
    }

    public Double getYCoordinate() {
        return yCoordinate;
    }

    public Double getZCoordinate() {
        return zCoordinate;
    }

    public Map<String, String> getAttributes() {
        return new HashMap<>(attributes);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("Location[id=%s, name=%s, type=%s, status=%s, zone=%s]",
            locationId, locationName, type, status, zone);
    }
}
