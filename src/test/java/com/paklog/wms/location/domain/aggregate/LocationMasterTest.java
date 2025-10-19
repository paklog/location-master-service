package com.paklog.wms.location.domain.aggregate;

import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.entity.Dimensions;
import com.paklog.wms.location.domain.entity.Restrictions;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import com.paklog.wms.location.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LocationMasterTest {

    @Test
    @DisplayName("Factory initializes sensible defaults")
    void createInitializesDefaults() {
        LocationMaster location = LocationMaster.create(
            "ZONE-001",
            "WH-001",
            "Zone 1",
            LocationType.ZONE,
            "WAREHOUSE-ROOT",
            null,
            "ZONE-1"
        );

        assertEquals(LocationStatus.ACTIVE, location.getStatus());
        assertEquals(LocationType.ZONE, location.getType());
        assertNotNull(location.getRestrictions());
        assertTrue(location.getAttributes().isEmpty());
    }

    @Test
    @DisplayName("Configuration methods update embedded value objects")
    void configureComponents() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-001");

        Dimensions newDimensions = new Dimensions(
            new BigDecimal("150"),
            new BigDecimal("90"),
            new BigDecimal("70"),
            "CM"
        );
        Capacity newCapacity = new Capacity(
            200,
            new BigDecimal("2000"),
            new BigDecimal("40"),
            "KG",
            "M3"
        );
        Restrictions restrictions = Restrictions.createHazmat();
        restrictions.addAllowedCategory("CHEMICAL");

        location.configureDimensions(newDimensions);
        location.configureCapacity(newCapacity);
        location.configureRestrictions(restrictions);
        location.setSlottingClass(SlottingClass.FAST_MOVER);
        location.setPickPathSequence(42);
        location.setDistanceFromDock(10);
        location.setCoordinates(1.0, 2.0, 3.0);
        location.setPhysicalAddress("A", "B", "C", "D");

        assertEquals(newDimensions, location.getDimensions());
        assertEquals(newCapacity, location.getCapacity());
        assertEquals(restrictions, location.getRestrictions());
        assertEquals(SlottingClass.FAST_MOVER, location.getSlottingClass());
        assertEquals(42, location.getPickPathSequence());
        assertEquals(10, location.getDistanceFromDock());
        assertEquals("A", location.getAisle());
        assertEquals(1.0, location.getXCoordinate());
    }

    @Test
    @DisplayName("Setters reject invalid inputs")
    void setterValidation() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-002");

        assertThrows(IllegalArgumentException.class, () -> location.configureDimensions(null));
        assertThrows(IllegalArgumentException.class, () -> location.configureCapacity(null));
        assertThrows(IllegalArgumentException.class, () -> location.configureRestrictions(null));
        assertThrows(IllegalArgumentException.class, () -> location.setSlottingClass(null));
        assertThrows(IllegalArgumentException.class, () -> location.setPickPathSequence(-1));
        assertThrows(IllegalArgumentException.class, () -> location.setDistanceFromDock(-5));
        assertThrows(IllegalArgumentException.class, () -> location.setAttribute("  ", "value"));
    }

    @Test
    @DisplayName("Block and unblock update status and attributes")
    void blockAndUnblockFlow() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-003");

        location.block("Damage");
        assertEquals(LocationStatus.BLOCKED, location.getStatus());
        assertEquals("Damage", location.getAttribute("block_reason"));

        location.unblock();
        assertEquals(LocationStatus.ACTIVE, location.getStatus());
        assertNull(location.getAttribute("block_reason"));
    }

    @Test
    @DisplayName("Reserve and release reservation enforce lifecycle rules")
    void reserveLifecycle() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-004");

        location.reserve("Cycle Count");
        assertEquals(LocationStatus.RESERVED, location.getStatus());
        assertEquals("Cycle Count", location.getAttribute("reservation_purpose"));

        location.releaseReservation();
        assertEquals(LocationStatus.ACTIVE, location.getStatus());
        assertNull(location.getAttribute("reservation_purpose"));

        assertThrows(IllegalStateException.class, location::releaseReservation);
    }

    @Test
    @DisplayName("Marking full and available toggles status accordingly")
    void markFullAndAvailable() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-005");

        location.markAsFull();
        assertEquals(LocationStatus.FULL, location.getStatus());

        location.markAsAvailable();
        assertEquals(LocationStatus.ACTIVE, location.getStatus());
    }

    @Test
    @DisplayName("Activate rejects decommissioned locations")
    void activateAfterDecommission() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-006");
        location.decommission("Obsolete");

        assertThrows(IllegalStateException.class, location::activate);
    }

    @Test
    @DisplayName("Validate hierarchy checks expected parent and depth settings")
    void validateHierarchy() {
        LocationMaster mismatchedLevel = LocationMaster.create(
            "ZONE-002",
            "WH-001",
            "Zone 2",
            LocationType.ZONE,
            "WAREHOUSE-ROOT",
            0,
            "ZONE-2"
        );
        assertThrows(IllegalStateException.class, mismatchedLevel::validateHierarchy);

        LocationMaster missingParent = LocationMaster.create(
            "AISLE-001",
            "WH-001",
            "Aisle 1",
            LocationType.AISLE,
            null,
            LocationType.AISLE.getHierarchyDepth(),
            "ZONE-1"
        );
        assertThrows(IllegalStateException.class, missingParent::validateHierarchy);

        LocationMaster rootWithParent = LocationMaster.create(
            "WARE-1",
            "WH-001",
            "Warehouse 1",
            LocationType.WAREHOUSE,
            "SHOULD-NOT-BE-SET",
            0,
            null
        );
        assertThrows(IllegalStateException.class, rootWithParent::validateHierarchy);
    }

    @Test
    @DisplayName("Full path concatenates hierarchical address")
    void fullPath() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-007");
        assertTrue(location.getFullPath().contains("/"));

        location.setAttribute("custom", "value");
        assertEquals("value", location.getAttribute("custom"));
        location.removeAttribute("custom");
        assertNull(location.getAttribute("custom"));
    }
}
