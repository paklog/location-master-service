package com.paklog.wms.location.application.service;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.repository.LocationMasterRepository;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Slotting Optimization Service
 * Provides algorithms for optimal product-to-location assignment
 * based on velocity, dimensions, and warehouse layout
 */
@Service
@Transactional(readOnly = true)
public class SlottingService {

    private static final Logger logger = LoggerFactory.getLogger(SlottingService.class);

    private final LocationMasterRepository locationRepository;
    private final LocationConfigurationService configService;

    public SlottingService(
            LocationMasterRepository locationRepository,
            LocationConfigurationService configService
    ) {
        this.locationRepository = locationRepository;
        this.configService = configService;
    }

    /**
     * Find optimal location for a product based on slotting class
     *
     * @param warehouseId Warehouse ID
     * @param zone Zone to search
     * @param targetClass Slotting class (A, B, C, etc.)
     * @param requiredQuantity Required capacity
     * @return Optimal location or empty if none found
     */
    public Optional<LocationMaster> findOptimalLocation(
            String warehouseId,
            String zone,
            SlottingClass targetClass,
            int requiredQuantity
    ) {
        logger.debug("Finding optimal location for class {} in warehouse {} zone {}",
            targetClass, warehouseId, zone);

        List<LocationMaster> candidates = locationRepository
            .findOptimalSlottingLocations(warehouseId, zone, targetClass);

        return candidates.stream()
            .filter(loc -> loc.getCapacity() != null &&
                          loc.getCapacity().getAvailableQuantity() >= requiredQuantity)
            .findFirst();
    }

    /**
     * Optimize slotting for entire warehouse zone
     * Reassigns slotting classes based on distance from dock
     *
     * @param warehouseId Warehouse ID
     * @param zone Zone to optimize
     * @param updatedBy User performing optimization
     * @return Number of locations updated
     */
    @Transactional
    public int optimizeZoneSlotting(String warehouseId, String zone, String updatedBy) {
        logger.info("Optimizing slotting for warehouse {} zone {}", warehouseId, zone);

        List<LocationMaster> locations = locationRepository
            .findByWarehouseIdAndZoneAndStatus(warehouseId, zone, LocationStatus.ACTIVE);

        int updated = 0;

        for (LocationMaster location : locations) {
            if (location.getType().canStoreInventory() &&
                location.getDistanceFromDock() != null) {

                SlottingClass optimalClass = determineOptimalSlottingClass(
                    location.getDistanceFromDock()
                );

                if (optimalClass != location.getSlottingClass()) {
                    configService.updateSlottingClass(
                        location.getLocationId(),
                        optimalClass,
                        updatedBy,
                        "Automatic slotting optimization"
                    );
                    updated++;
                }
            }
        }

        logger.info("Updated {} locations in zone {}", updated, zone);
        return updated;
    }

    /**
     * Get slotting recommendations for a warehouse
     *
     * @param warehouseId Warehouse ID
     * @return Map of location ID to recommended slotting class
     */
    public Map<String, SlottingRecommendation> getSlottingRecommendations(String warehouseId) {
        logger.info("Generating slotting recommendations for warehouse {}", warehouseId);

        List<LocationMaster> locations = locationRepository.findActiveStorageLocations(warehouseId);

        return locations.stream()
            .filter(loc -> loc.getDistanceFromDock() != null)
            .collect(Collectors.toMap(
                LocationMaster::getLocationId,
                this::generateRecommendation
            ));
    }

    /**
     * Calculate golden zone (optimal picking area) locations
     * Golden zone = locations closest to dock with best accessibility
     *
     * @param warehouseId Warehouse ID
     * @param zone Zone to analyze
     * @return List of golden zone locations
     */
    public List<LocationMaster> identifyGoldenZone(String warehouseId, String zone) {
        logger.debug("Identifying golden zone for warehouse {} zone {}", warehouseId, zone);

        List<LocationMaster> candidates = locationRepository
            .findByWarehouseIdAndZoneAndStatus(warehouseId, zone, LocationStatus.ACTIVE);

        // Sort by distance from dock (ascending) and pick path sequence
        return candidates.stream()
            .filter(loc -> loc.getType().canStoreInventory())
            .filter(loc -> loc.getDistanceFromDock() != null)
            .sorted(Comparator.comparing(LocationMaster::getDistanceFromDock)
                .thenComparing(loc -> loc.getPickPathSequence() != null ?
                    loc.getPickPathSequence() : Integer.MAX_VALUE))
            .limit(calculateGoldenZoneSize(candidates.size()))
            .collect(Collectors.toList());
    }

    /**
     * Balance slotting across zones
     * Ensures even distribution of velocity classes
     *
     * @param warehouseId Warehouse ID
     * @return Rebalancing report
     */
    @Transactional
    public SlottingBalanceReport balanceSlotting(String warehouseId, String updatedBy) {
        logger.info("Balancing slotting for warehouse {}", warehouseId);

        List<LocationMaster> zones = locationRepository.findZones(warehouseId);
        Map<String, SlottingDistribution> distributionByZone = new HashMap<>();

        for (LocationMaster zone : zones) {
            List<LocationMaster> zoneLocations = locationRepository
                .findByWarehouseIdAndZone(warehouseId, zone.getZone());

            SlottingDistribution distribution = calculateSlottingDistribution(zoneLocations);
            distributionByZone.put(zone.getZone(), distribution);
        }

        // Identify zones that need rebalancing
        int rebalanced = 0;
        for (Map.Entry<String, SlottingDistribution> entry : distributionByZone.entrySet()) {
            if (entry.getValue().needsRebalancing()) {
                int updated = optimizeZoneSlotting(warehouseId, entry.getKey(), updatedBy);
                rebalanced += updated;
            }
        }

        return new SlottingBalanceReport(warehouseId, distributionByZone, rebalanced);
    }

    /**
     * Get pick path optimization for zone
     * Calculates optimal sequence based on slotting class and location
     *
     * @param warehouseId Warehouse ID
     * @param zone Zone to optimize
     * @return Ordered list of locations in optimal pick path
     */
    public List<LocationMaster> getOptimizedPickPath(String warehouseId, String zone) {
        List<LocationMaster> locations = locationRepository.findPickPathLocations(warehouseId, zone);

        // Sort by slotting class priority, then distance, then physical location
        return locations.stream()
            .sorted(Comparator
                .comparing((LocationMaster loc) -> loc.getSlottingClass().getPickPathPriority())
                .thenComparing(loc -> loc.getDistanceFromDock() != null ?
                    loc.getDistanceFromDock() : Integer.MAX_VALUE)
                .thenComparing(loc -> loc.getAisle() != null ? loc.getAisle() : "")
                .thenComparing(loc -> loc.getBay() != null ? loc.getBay() : "")
                .thenComparing(loc -> loc.getLevel() != null ? loc.getLevel() : ""))
            .collect(Collectors.toList());
    }

    /**
     * Determine optimal slotting class based on distance from dock
     */
    private SlottingClass determineOptimalSlottingClass(int distanceFromDock) {
        if (distanceFromDock <= 20) {
            return SlottingClass.FAST_MOVER;
        } else if (distanceFromDock <= 50) {
            return SlottingClass.A;
        } else if (distanceFromDock <= 100) {
            return SlottingClass.B;
        } else if (distanceFromDock <= 200) {
            return SlottingClass.C;
        } else {
            return SlottingClass.SLOW_MOVER;
        }
    }

    /**
     * Generate slotting recommendation for a location
     */
    private SlottingRecommendation generateRecommendation(LocationMaster location) {
        if (location.getDistanceFromDock() == null) {
            return new SlottingRecommendation(
                location.getLocationId(),
                location.getSlottingClass(),
                location.getSlottingClass(),
                0,
                "Distance from dock not set"
            );
        }

        SlottingClass recommendedClass = determineOptimalSlottingClass(
            location.getDistanceFromDock()
        );

        int confidenceScore = calculateConfidenceScore(location, recommendedClass);

        String reasoning = String.format(
            "Location is %d units from dock. Recommended %s (current: %s)",
            location.getDistanceFromDock(),
            recommendedClass,
            location.getSlottingClass()
        );

        return new SlottingRecommendation(
            location.getLocationId(),
            location.getSlottingClass(),
            recommendedClass,
            confidenceScore,
            reasoning
        );
    }

    /**
     * Calculate confidence score for recommendation (0-100)
     */
    private int calculateConfidenceScore(LocationMaster location, SlottingClass recommendedClass) {
        int score = 50; // Base score

        // Higher confidence if distance is optimal for class
        if (location.getDistanceFromDock() != null) {
            int idealDistance = recommendedClass.getRecommendedDistanceFromDock() * 20;
            int distanceDiff = Math.abs(location.getDistanceFromDock() - idealDistance);
            score += Math.max(0, 30 - distanceDiff); // Up to +30
        }

        // Higher confidence if location has capacity info
        if (location.getCapacity() != null) {
            score += 10;
        }

        // Higher confidence if dimensions are set
        if (location.getDimensions() != null) {
            score += 10;
        }

        return Math.min(100, score);
    }

    /**
     * Calculate slotting distribution for locations
     */
    private SlottingDistribution calculateSlottingDistribution(List<LocationMaster> locations) {
        Map<SlottingClass, Long> distribution = locations.stream()
            .filter(loc -> loc.getSlottingClass() != null)
            .collect(Collectors.groupingBy(
                LocationMaster::getSlottingClass,
                Collectors.counting()
            ));

        long total = locations.size();
        long fastMovers = distribution.getOrDefault(SlottingClass.FAST_MOVER, 0L);
        long aClass = distribution.getOrDefault(SlottingClass.A, 0L);
        long bClass = distribution.getOrDefault(SlottingClass.B, 0L);
        long cClass = distribution.getOrDefault(SlottingClass.C, 0L);

        // Check if distribution follows 80-20 rule (approximately)
        boolean needsRebalancing = total > 0 &&
            (fastMovers + aClass) * 100 / total > 30; // Should be ~20%

        return new SlottingDistribution(distribution, total, needsRebalancing);
    }

    /**
     * Calculate golden zone size (top 20% of locations)
     */
    private long calculateGoldenZoneSize(int totalLocations) {
        return Math.max(1, (long) (totalLocations * 0.2));
    }

    // DTOs
    public record SlottingRecommendation(
        String locationId,
        SlottingClass currentClass,
        SlottingClass recommendedClass,
        int confidenceScore,
        String reasoning
    ) {}

    public record SlottingDistribution(
        Map<SlottingClass, Long> distribution,
        long totalLocations,
        boolean needsRebalancing
    ) {}

    public record SlottingBalanceReport(
        String warehouseId,
        Map<String, SlottingDistribution> distributionByZone,
        int locationsRebalanced
    ) {}
}
