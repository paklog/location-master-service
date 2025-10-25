package com.paklog.wms.location.application.service;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.entity.Dimensions;
import com.paklog.wms.location.domain.entity.Restrictions;
import com.paklog.wms.location.domain.event.*;
import com.paklog.wms.location.domain.repository.LocationMasterRepository;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import com.paklog.wms.location.infrastructure.events.LocationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for location configuration management
 * Orchestrates location creation, update, and lifecycle operations
 */
@Service
@Transactional
public class LocationConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationConfigurationService.class);

    private final LocationMasterRepository locationRepository;
    private final LocationEventPublisher eventPublisher;

    public LocationConfigurationService(
            LocationMasterRepository locationRepository,
            LocationEventPublisher eventPublisher
    ) {
        this.locationRepository = locationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new location
     */
    public LocationMaster createLocation(
            String locationId,
            String warehouseId,
            String locationName,
            LocationType type,
            String parentLocationId,
            Integer hierarchyLevel,
            String zone,
            String createdBy
    ) {
        logger.info("Creating location: {} ({}) in warehouse {}", locationId, locationName, warehouseId);

        // Validate parent exists if specified
        if (parentLocationId != null) {
            LocationMaster parent = locationRepository.findById(parentLocationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Parent location not found: " + parentLocationId));

            if (!parent.canHaveChildren()) {
                throw new IllegalArgumentException(
                    "Parent location type " + parent.getType() + " cannot have children");
            }
        }

        // Create location
        LocationMaster location = LocationMaster.create(
            locationId, warehouseId, locationName, type,
            parentLocationId, hierarchyLevel, zone
        );
        location.setCreatedBy(createdBy);
        location.validateHierarchy();

        // Save
        location = locationRepository.save(location);

        // Publish event
        LocationCreatedEvent event = LocationCreatedEvent.of(
            locationId, warehouseId, locationName, type,
            parentLocationId, hierarchyLevel, zone, createdBy
        );
        eventPublisher.publishLocationCreated(event);

        logger.info("Location created: {}", locationId);
        return location;
    }

    /**
     * Configure location dimensions
     */
    public LocationMaster configureDimensions(
            String locationId,
            Dimensions dimensions,
            String updatedBy
    ) {
        logger.info("Configuring dimensions for location: {}", locationId);

        LocationMaster location = getLocationOrThrow(locationId);
        location.configureDimensions(dimensions);
        location.setUpdatedBy(updatedBy);

        return locationRepository.save(location);
    }

    /**
     * Configure location capacity
     */
    public LocationMaster configureCapacity(
            String locationId,
            Capacity capacity,
            String updatedBy,
            String reason
    ) {
        logger.info("Configuring capacity for location: {}", locationId);

        LocationMaster location = getLocationOrThrow(locationId);

        // Store previous values for event
        Integer previousMaxQty = location.getCapacity() != null ?
            location.getCapacity().getMaxQuantity() : null;

        location.configureCapacity(capacity);
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        // Publish event if capacity changed
        if (previousMaxQty != null && !previousMaxQty.equals(capacity.getMaxQuantity())) {
            LocationCapacityChangedEvent event = LocationCapacityChangedEvent.of(
                locationId,
                location.getWarehouseId(),
                previousMaxQty,
                capacity.getMaxQuantity(),
                null, capacity.getMaxWeight(),
                null, capacity.getMaxVolume(),
                updatedBy,
                reason
            );
            eventPublisher.publishLocationCapacityChanged(event);
        }

        return location;
    }

    /**
     * Configure location restrictions
     */
    public LocationMaster configureRestrictions(
            String locationId,
            Restrictions restrictions,
            String updatedBy
    ) {
        logger.info("Configuring restrictions for location: {}", locationId);

        LocationMaster location = getLocationOrThrow(locationId);
        location.configureRestrictions(restrictions);
        location.setUpdatedBy(updatedBy);

        return locationRepository.save(location);
    }

    /**
     * Update slotting classification
     */
    public LocationMaster updateSlottingClass(
            String locationId,
            SlottingClass slottingClass,
            String updatedBy,
            String reason
    ) {
        logger.info("Updating slotting class for location {} to {}", locationId, slottingClass);

        LocationMaster location = getLocationOrThrow(locationId);
        SlottingClass previousClass = location.getSlottingClass();

        location.setSlottingClass(slottingClass);
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        // Publish event
        LocationSlottingChangedEvent event = LocationSlottingChangedEvent.of(
            locationId,
            location.getWarehouseId(),
            location.getZone(),
            previousClass,
            slottingClass,
            location.getPickPathSequence(),
            updatedBy,
            reason
        );
        eventPublisher.publishLocationSlottingChanged(event);

        return location;
    }

    /**
     * Activate location
     */
    public LocationMaster activateLocation(String locationId, String updatedBy) {
        logger.info("Activating location: {}", locationId);

        LocationMaster location = getLocationOrThrow(locationId);
        LocationStatus previousStatus = location.getStatus();

        location.activate();
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        // Publish event
        publishStatusChangeEvent(location, previousStatus, "Location activated", updatedBy);

        return location;
    }

    /**
     * Deactivate location
     */
    public LocationMaster deactivateLocation(String locationId, String updatedBy) {
        logger.info("Deactivating location: {}", locationId);

        LocationMaster location = getLocationOrThrow(locationId);
        LocationStatus previousStatus = location.getStatus();

        location.deactivate();
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        publishStatusChangeEvent(location, previousStatus, "Location deactivated", updatedBy);

        return location;
    }

    /**
     * Block location
     */
    public LocationMaster blockLocation(String locationId, String reason, String updatedBy) {
        logger.info("Blocking location {}: {}", locationId, reason);

        LocationMaster location = getLocationOrThrow(locationId);
        LocationStatus previousStatus = location.getStatus();

        location.block(reason);
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        publishStatusChangeEvent(location, previousStatus, "Location blocked: " + reason, updatedBy);

        return location;
    }

    /**
     * Unblock location
     */
    public LocationMaster unblockLocation(String locationId, String updatedBy) {
        logger.info("Unblocking location: {}", locationId);

        LocationMaster location = getLocationOrThrow(locationId);
        LocationStatus previousStatus = location.getStatus();

        location.unblock();
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        publishStatusChangeEvent(location, previousStatus, "Location unblocked", updatedBy);

        return location;
    }

    /**
     * Reserve location
     */
    public LocationMaster reserveLocation(String locationId, String purpose, String updatedBy) {
        logger.info("Reserving location {}: {}", locationId, purpose);

        LocationMaster location = getLocationOrThrow(locationId);
        LocationStatus previousStatus = location.getStatus();

        location.reserve(purpose);
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        publishStatusChangeEvent(location, previousStatus, "Location reserved: " + purpose, updatedBy);

        return location;
    }

    /**
     * Release reservation
     */
    public LocationMaster releaseReservation(String locationId, String updatedBy) {
        logger.info("Releasing reservation for location: {}", locationId);

        LocationMaster location = getLocationOrThrow(locationId);
        LocationStatus previousStatus = location.getStatus();

        location.releaseReservation();
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        publishStatusChangeEvent(location, previousStatus, "Reservation released", updatedBy);

        return location;
    }

    /**
     * Decommission location
     */
    public LocationMaster decommissionLocation(String locationId, String reason, String updatedBy) {
        logger.info("Decommissioning location {}: {}", locationId, reason);

        LocationMaster location = getLocationOrThrow(locationId);
        LocationStatus previousStatus = location.getStatus();

        location.decommission(reason);
        location.setUpdatedBy(updatedBy);

        location = locationRepository.save(location);

        publishStatusChangeEvent(location, previousStatus, "Location decommissioned: " + reason, updatedBy);

        return location;
    }

    /**
     * Set pick path sequence
     */
    public LocationMaster setPickPathSequence(String locationId, Integer sequence, String updatedBy) {
        logger.info("Setting pick path sequence for location {} to {}", locationId, sequence);

        LocationMaster location = getLocationOrThrow(locationId);
        location.setPickPathSequence(sequence);
        location.setUpdatedBy(updatedBy);

        return locationRepository.save(location);
    }

    /**
     * Set location coordinates
     */
    public LocationMaster setCoordinates(
            String locationId,
            Double x, Double y, Double z,
            String updatedBy
    ) {
        LocationMaster location = getLocationOrThrow(locationId);
        location.setCoordinates(x, y, z);
        location.setUpdatedBy(updatedBy);

        return locationRepository.save(location);
    }

    /**
     * Get location by ID
     */
    @Transactional(readOnly = true)
    public Optional<LocationMaster> getLocation(String locationId) {
        return locationRepository.findById(locationId);
    }

    /**
     * Get locations by warehouse
     */
    @Transactional(readOnly = true)
    public List<LocationMaster> getLocationsByWarehouse(String warehouseId) {
        return locationRepository.findByWarehouseId(warehouseId);
    }

    /**
     * Get active storage locations
     */
    @Transactional(readOnly = true)
    public List<LocationMaster> getActiveStorageLocations(String warehouseId) {
        return locationRepository.findActiveStorageLocations(warehouseId);
    }

    /**
     * Get locations in pick path order
     */
    @Transactional(readOnly = true)
    public List<LocationMaster> getPickPathLocations(String warehouseId, String zone) {
        return locationRepository.findPickPathLocations(warehouseId, zone);
    }

    /**
     * Get locations requiring attention
     */
    @Transactional(readOnly = true)
    public List<LocationMaster> getLocationsRequiringAttention(String warehouseId) {
        return locationRepository.findLocationsRequiringAttention(warehouseId);
    }

    /**
     * Get child locations
     */
    @Transactional(readOnly = true)
    public List<LocationMaster> getChildLocations(String parentLocationId) {
        return locationRepository.findByParentLocationId(parentLocationId);
    }

    private LocationMaster getLocationOrThrow(String locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));
    }

    private void publishStatusChangeEvent(
            LocationMaster location,
            LocationStatus previousStatus,
            String reason,
            String updatedBy
    ) {
        if (previousStatus != location.getStatus()) {
            LocationStatusChangedEvent event = LocationStatusChangedEvent.of(
                location.getLocationId(),
                location.getWarehouseId(),
                previousStatus,
                location.getStatus(),
                reason,
                updatedBy
            );
            eventPublisher.publishLocationStatusChanged(event);
        

}
}
}
