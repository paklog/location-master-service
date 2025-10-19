package com.paklog.wms.location.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlottingClassTest {

    @Test
    void pickPathPriorityAndDistanceRecommendations() {
        assertTrue(SlottingClass.FAST_MOVER.getPickPathPriority() <
                   SlottingClass.C.getPickPathPriority());

        assertEquals(1, SlottingClass.A.getRecommendedDistanceFromDock());
        assertEquals(4, SlottingClass.OVERSIZED.getRecommendedDistanceFromDock());
    }

    @Test
    void specialHandlingFlag() {
        assertTrue(SlottingClass.HAZMAT.requiresSpecialHandling());
        assertFalse(SlottingClass.A.requiresSpecialHandling());
    }
}
