package com.paklog.wms.location.support;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.entity.Dimensions;
import com.paklog.wms.location.domain.entity.Restrictions;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;

import java.math.BigDecimal;

/**
 * Provides reusable builders for unit and integration tests.
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Dimensions sampleDimensions() {
        return new Dimensions(
            new BigDecimal("120"),
            new BigDecimal("80"),
            new BigDecimal("60"),
            "CM"
        );
    }

    public static Capacity sampleCapacity() {
        return new Capacity(
            100,
            new BigDecimal("1000"),
            new BigDecimal("20"),
            "KG",
            "M3"
        );
    }

    public static Restrictions sampleRestrictions() {
        Restrictions restrictions = Restrictions.createDefault();
        restrictions.addAllowedCategory("GENERAL");
        return restrictions;
    }

    public static LocationMaster sampleWarehouse(String locationId) {
        return LocationMaster.create(
            locationId,
            "WH-001",
            "Main Warehouse",
            LocationType.WAREHOUSE,
            null,
            0,
            null
        );
    }

    public static LocationMaster sampleZone(String locationId, String zoneCode) {
        return LocationMaster.create(
            locationId,
            "WH-001",
            "Zone " + zoneCode,
            LocationType.ZONE,
            "WAREHOUSE-ROOT",
            1,
            zoneCode
        );
    }

    public static LocationMaster sampleBinLocation(String locationId) {
        LocationMaster location = LocationMaster.create(
            locationId,
            "WH-001",
            "Bin " + locationId,
            LocationType.BIN,
            "LEVEL-001",
            LocationType.BIN.getHierarchyDepth(),
            "PICK"
        );
        location.configureDimensions(sampleDimensions());
        location.configureCapacity(sampleCapacity());
        location.configureRestrictions(sampleRestrictions());
        location.setSlottingClass(SlottingClass.A);
        location.setDistanceFromDock(15);
        location.setPickPathSequence(100);
        location.setCoordinates(1.0, 2.0, 3.0);
        location.setPhysicalAddress("A1", "B2", "L3", "P4");
        return location;
    }
}
