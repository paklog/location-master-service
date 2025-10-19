package com.paklog.wms.location.adapter.rest;

import com.paklog.wms.location.adapter.rest.dto.*;
import com.paklog.wms.location.application.service.LocationConfigurationService;
import com.paklog.wms.location.application.service.SlottingService;
import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for location master configuration and management
 */
@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Location Management", description = "Location configuration and hierarchy management")
public class LocationController {

    private final LocationConfigurationService configService;
    private final SlottingService slottingService;

    public LocationController(
            LocationConfigurationService configService,
            SlottingService slottingService
    ) {
        this.configService = configService;
        this.slottingService = slottingService;
    }

    /**
     * Create a new location
     */
    @PostMapping
    @Operation(summary = "Create location", description = "Create a new location in the warehouse hierarchy")
    public ResponseEntity<LocationResponse> createLocation(
            @Valid @RequestBody CreateLocationRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.createLocation(
            request.locationId(),
            request.warehouseId(),
            request.locationName(),
            request.type(),
            request.parentLocationId(),
            request.hierarchyLevel(),
            request.zone(),
            userId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(LocationResponse.from(location));
    }

    /**
     * Get location by ID
     */
    @GetMapping("/{locationId}")
    @Operation(summary = "Get location", description = "Get location details by ID")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable String locationId) {
        return configService.getLocation(locationId)
            .map(LocationResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get locations by warehouse
     */
    @GetMapping
    @Operation(summary = "List locations", description = "List all locations for a warehouse")
    public ResponseEntity<List<LocationResponse>> getLocations(
            @RequestParam String warehouseId,
            @RequestParam(required = false) LocationStatus status,
            @RequestParam(required = false) String zone
    ) {
        List<LocationMaster> locations;

        if (status != null && zone != null) {
            locations = configService.getLocationsByWarehouse(warehouseId).stream()
                .filter(loc -> loc.getStatus() == status && zone.equals(loc.getZone()))
                .collect(Collectors.toList());
        } else if (status != null) {
            locations = configService.getLocationsByWarehouse(warehouseId).stream()
                .filter(loc -> loc.getStatus() == status)
                .collect(Collectors.toList());
        } else if (zone != null) {
            locations = configService.getLocationsByWarehouse(warehouseId).stream()
                .filter(loc -> zone.equals(loc.getZone()))
                .collect(Collectors.toList());
        } else {
            locations = configService.getLocationsByWarehouse(warehouseId);
        }

        return ResponseEntity.ok(
            locations.stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Get active storage locations
     */
    @GetMapping("/storage")
    @Operation(summary = "Get storage locations", description = "Get active storage locations")
    public ResponseEntity<List<LocationResponse>> getStorageLocations(
            @RequestParam String warehouseId
    ) {
        List<LocationMaster> locations = configService.getActiveStorageLocations(warehouseId);
        return ResponseEntity.ok(
            locations.stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Get pick path locations
     */
    @GetMapping("/pick-path")
    @Operation(summary = "Get pick path", description = "Get locations in pick path order")
    public ResponseEntity<List<LocationResponse>> getPickPathLocations(
            @RequestParam String warehouseId,
            @RequestParam String zone
    ) {
        List<LocationMaster> locations = configService.getPickPathLocations(warehouseId, zone);
        return ResponseEntity.ok(
            locations.stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Get child locations
     */
    @GetMapping("/{locationId}/children")
    @Operation(summary = "Get child locations", description = "Get all child locations of a parent")
    public ResponseEntity<List<LocationResponse>> getChildLocations(
            @PathVariable String locationId
    ) {
        List<LocationMaster> children = configService.getChildLocations(locationId);
        return ResponseEntity.ok(
            children.stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Configure location capacity
     */
    @PutMapping("/{locationId}/capacity")
    @Operation(summary = "Configure capacity", description = "Configure location capacity limits")
    public ResponseEntity<LocationResponse> configureCapacity(
            @PathVariable String locationId,
            @Valid @RequestBody ConfigureCapacityRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.configureCapacity(
            locationId,
            request.toCapacity(),
            userId,
            request.reason()
        );

        return ResponseEntity.ok(LocationResponse.from(location));
    }

    /**
     * Update slotting class
     */
    @PutMapping("/{locationId}/slotting")
    @Operation(summary = "Update slotting", description = "Update location slotting classification")
    public ResponseEntity<LocationResponse> updateSlotting(
            @PathVariable String locationId,
            @Valid @RequestBody UpdateSlottingRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.updateSlottingClass(
            locationId,
            request.slottingClass(),
            userId,
            request.reason()
        );

        return ResponseEntity.ok(LocationResponse.from(location));
    }

    /**
     * Activate location
     */
    @PostMapping("/{locationId}/activate")
    @Operation(summary = "Activate location", description = "Activate a location")
    public ResponseEntity<LocationResponse> activateLocation(
            @PathVariable String locationId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.activateLocation(locationId, userId);
        return ResponseEntity.ok(LocationResponse.from(location));
    }

    /**
     * Deactivate location
     */
    @PostMapping("/{locationId}/deactivate")
    @Operation(summary = "Deactivate location", description = "Deactivate a location")
    public ResponseEntity<LocationResponse> deactivateLocation(
            @PathVariable String locationId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.deactivateLocation(locationId, userId);
        return ResponseEntity.ok(LocationResponse.from(location));
    }

    /**
     * Block location
     */
    @PostMapping("/{locationId}/block")
    @Operation(summary = "Block location", description = "Block a location")
    public ResponseEntity<LocationResponse> blockLocation(
            @PathVariable String locationId,
            @Valid @RequestBody BlockLocationRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.blockLocation(
            locationId,
            request.reason(),
            userId
        );
        return ResponseEntity.ok(LocationResponse.from(location));
    }

    /**
     * Unblock location
     */
    @PostMapping("/{locationId}/unblock")
    @Operation(summary = "Unblock location", description = "Unblock a location")
    public ResponseEntity<LocationResponse> unblockLocation(
            @PathVariable String locationId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.unblockLocation(locationId, userId);
        return ResponseEntity.ok(LocationResponse.from(location));
    }

    /**
     * Reserve location
     */
    @PostMapping("/{locationId}/reserve")
    @Operation(summary = "Reserve location", description = "Reserve a location")
    public ResponseEntity<LocationResponse> reserveLocation(
            @PathVariable String locationId,
            @Valid @RequestBody ReserveLocationRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        LocationMaster location = configService.reserveLocation(
            locationId,
            request.purpose(),
            userId
        );
        return ResponseEntity.ok(LocationResponse.from(location));
    }

    /**
     * Get slotting recommendations
     */
    @GetMapping("/slotting/recommendations")
    @Operation(summary = "Get slotting recommendations", description = "Get AI-powered slotting recommendations")
    public ResponseEntity<List<SlottingRecommendationResponse>> getSlottingRecommendations(
            @RequestParam String warehouseId
    ) {
        var recommendations = slottingService.getSlottingRecommendations(warehouseId);

        List<SlottingRecommendationResponse> response = recommendations.values().stream()
            .map(rec -> new SlottingRecommendationResponse(
                rec.locationId(),
                rec.currentClass(),
                rec.recommendedClass(),
                rec.confidenceScore(),
                rec.reasoning()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Optimize zone slotting
     */
    @PostMapping("/slotting/optimize")
    @Operation(summary = "Optimize slotting", description = "Optimize slotting for a warehouse zone")
    public ResponseEntity<SlottingOptimizationResponse> optimizeSlotting(
            @RequestParam String warehouseId,
            @RequestParam String zone,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId
    ) {
        int updated = slottingService.optimizeZoneSlotting(warehouseId, zone, userId);
        return ResponseEntity.ok(new SlottingOptimizationResponse(
            warehouseId,
            zone,
            updated,
            "Slotting optimization completed successfully"
        ));
    }

    /**
     * Get golden zone locations
     */
    @GetMapping("/golden-zone")
    @Operation(summary = "Get golden zone", description = "Get optimal golden zone locations")
    public ResponseEntity<List<LocationResponse>> getGoldenZone(
            @RequestParam String warehouseId,
            @RequestParam String zone
    ) {
        List<LocationMaster> goldenZone = slottingService.identifyGoldenZone(warehouseId, zone);
        return ResponseEntity.ok(
            goldenZone.stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList())
        );
    }

    /**
     * Get locations requiring attention
     */
    @GetMapping("/attention-required")
    @Operation(summary = "Get locations requiring attention", description = "Get blocked or full locations")
    public ResponseEntity<List<LocationResponse>> getLocationsRequiringAttention(
            @RequestParam String warehouseId
    ) {
        List<LocationMaster> locations = configService.getLocationsRequiringAttention(warehouseId);
        return ResponseEntity.ok(
            locations.stream()
                .map(LocationResponse::from)
                .collect(Collectors.toList())
        );
    }
}
