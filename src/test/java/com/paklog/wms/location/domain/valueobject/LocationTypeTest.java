package com.paklog.wms.location.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationTypeTest {

    @Test
    void childCapabilitiesDependOnType() {
        assertTrue(LocationType.WAREHOUSE.canHaveChildren());
        assertFalse(LocationType.BIN.canHaveChildren());

        assertFalse(LocationType.WAREHOUSE.canStoreInventory());
        assertTrue(LocationType.BIN.canStoreInventory());
    }

    @Test
    void hierarchyDepthMatchesExpectedValues() {
        assertEquals(0, LocationType.WAREHOUSE.getHierarchyDepth());
        assertEquals(5, LocationType.BIN.getHierarchyDepth());
        assertEquals(LocationType.ZONE, LocationType.AISLE.getTypicalParent());
    }
}
