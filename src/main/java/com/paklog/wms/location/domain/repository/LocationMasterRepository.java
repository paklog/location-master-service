package com.paklog.wms.location.domain.repository;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LocationMaster aggregate
 */
@Repository
public interface LocationMasterRepository extends JpaRepository<LocationMaster, String> {

    /**
     * Find locations by warehouse
     */
    List<LocationMaster> findByWarehouseId(String warehouseId);

    /**
     * Find locations by warehouse and status
     */
    List<LocationMaster> findByWarehouseIdAndStatus(String warehouseId, LocationStatus status);

    /**
     * Find locations by warehouse and type
     */
    List<LocationMaster> findByWarehouseIdAndType(String warehouseId, LocationType type);

    /**
     * Find locations by warehouse, type, and status
     */
    List<LocationMaster> findByWarehouseIdAndTypeAndStatus(
        String warehouseId,
        LocationType type,
        LocationStatus status
    );

    /**
     * Find locations by parent
     */
    List<LocationMaster> findByParentLocationId(String parentLocationId);

    /**
     * Find locations by zone
     */
    List<LocationMaster> findByWarehouseIdAndZone(String warehouseId, String zone);

    /**
     * Find locations by zone and status
     */
    List<LocationMaster> findByWarehouseIdAndZoneAndStatus(
        String warehouseId,
        String zone,
        LocationStatus status
    );

    /**
     * Find locations by slotting class
     */
    List<LocationMaster> findByWarehouseIdAndSlottingClass(
        String warehouseId,
        SlottingClass slottingClass
    );

    /**
     * Find active storage locations
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.type = 'BIN' AND l.status = 'ACTIVE'")
    List<LocationMaster> findActiveStorageLocations(@Param("warehouseId") String warehouseId);

    /**
     * Find locations by hierarchy level
     */
    List<LocationMaster> findByWarehouseIdAndHierarchyLevel(
        String warehouseId,
        Integer hierarchyLevel
    );

    /**
     * Find locations in pick path order
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.zone = :zone AND l.type = 'BIN' AND l.status = 'ACTIVE' " +
           "ORDER BY l.pickPathSequence, l.aisle, l.bay, l.level, l.position")
    List<LocationMaster> findPickPathLocations(
        @Param("warehouseId") String warehouseId,
        @Param("zone") String zone
    );

    /**
     * Find available locations with capacity
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.zone = :zone AND l.type = 'BIN' " +
           "AND l.status IN ('ACTIVE', 'RESERVED') " +
           "AND l.capacity.currentQuantity < l.capacity.maxQuantity")
    List<LocationMaster> findAvailableLocationsWithCapacity(
        @Param("warehouseId") String warehouseId,
        @Param("zone") String zone
    );

    /**
     * Find full locations
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.status = 'FULL'")
    List<LocationMaster> findFullLocations(@Param("warehouseId") String warehouseId);

    /**
     * Find blocked locations
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.status = 'BLOCKED'")
    List<LocationMaster> findBlockedLocations(@Param("warehouseId") String warehouseId);

    /**
     * Find locations requiring attention (blocked or full)
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.status IN ('BLOCKED', 'FULL')")
    List<LocationMaster> findLocationsRequiringAttention(@Param("warehouseId") String warehouseId);

    /**
     * Find locations by slotting class and status
     */
    List<LocationMaster> findByWarehouseIdAndSlottingClassAndStatus(
        String warehouseId,
        SlottingClass slottingClass,
        LocationStatus status
    );

    /**
     * Find locations by aisle
     */
    List<LocationMaster> findByWarehouseIdAndAisle(String warehouseId, String aisle);

    /**
     * Find nearby locations (same aisle and bay)
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.aisle = :aisle AND l.bay = :bay " +
           "AND l.type = 'BIN' AND l.status = 'ACTIVE' " +
           "ORDER BY l.level, l.position")
    List<LocationMaster> findNearbyLocations(
        @Param("warehouseId") String warehouseId,
        @Param("aisle") String aisle,
        @Param("bay") String bay
    );

    /**
     * Count locations by warehouse and type
     */
    long countByWarehouseIdAndType(String warehouseId, LocationType type);

    /**
     * Count locations by warehouse and status
     */
    long countByWarehouseIdAndStatus(String warehouseId, LocationStatus status);

    /**
     * Count active storage locations
     */
    @Query("SELECT COUNT(l) FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.type = 'BIN' AND l.status = 'ACTIVE'")
    long countActiveStorageLocations(@Param("warehouseId") String warehouseId);

    /**
     * Get warehouse hierarchy (root locations)
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.hierarchyLevel = 0")
    Optional<LocationMaster> findWarehouseRoot(@Param("warehouseId") String warehouseId);

    /**
     * Find zones for warehouse
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.type = 'ZONE' ORDER BY l.locationName")
    List<LocationMaster> findZones(@Param("warehouseId") String warehouseId);

    /**
     * Find locations by coordinates range (for mapping/routing)
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.xCoordinate BETWEEN :minX AND :maxX " +
           "AND l.yCoordinate BETWEEN :minY AND :maxY")
    List<LocationMaster> findLocationsInArea(
        @Param("warehouseId") String warehouseId,
        @Param("minX") Double minX,
        @Param("maxX") Double maxX,
        @Param("minY") Double minY,
        @Param("maxY") Double maxY
    );

    /**
     * Find locations suitable for slotting (by velocity class)
     */
    @Query("SELECT l FROM LocationMaster l WHERE l.warehouseId = :warehouseId " +
           "AND l.zone = :zone AND l.type = 'BIN' " +
           "AND l.status IN ('ACTIVE', 'RESERVED') " +
           "AND l.slottingClass = :targetClass " +
           "ORDER BY l.distanceFromDock, l.pickPathSequence")
    List<LocationMaster> findOptimalSlottingLocations(
        @Param("warehouseId") String warehouseId,
        @Param("zone") String zone,
        @Param("targetClass") SlottingClass targetClass
    );
}
