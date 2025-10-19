package com.paklog.wms.location.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationStatusTest {

    @Test
    void inventoryCapabilitiesFollowStatus() {
        assertTrue(LocationStatus.ACTIVE.canAcceptInventory());
        assertTrue(LocationStatus.RESERVED.canAcceptInventory());
        assertFalse(LocationStatus.BLOCKED.canAcceptInventory());

        assertTrue(LocationStatus.FULL.canReleaseInventory());
        assertFalse(LocationStatus.BLOCKED.canReleaseInventory());
    }

    @Test
    void attentionAndOperationalFlags() {
        assertTrue(LocationStatus.ACTIVE.isOperational());
        assertFalse(LocationStatus.INACTIVE.isOperational());

        assertTrue(LocationStatus.FULL.requiresAttention());
        assertFalse(LocationStatus.ACTIVE.requiresAttention());
    }
}
