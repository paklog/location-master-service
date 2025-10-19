package com.paklog.wms.location.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RestrictionsTest {

    @Test
    @DisplayName("Allowed and blocked categories are mutually exclusive")
    void allowedAndBlockedCategories() {
        Restrictions restrictions = Restrictions.createDefault();

        assertTrue(restrictions.isProductCategoryAllowed("ANY"));

        restrictions.addAllowedCategory("FOOD");
        assertTrue(restrictions.isProductCategoryAllowed("FOOD"));

        restrictions.blockCategory("FOOD");
        assertFalse(restrictions.isProductCategoryAllowed("FOOD"));
    }

    @Test
    @DisplayName("Hazmat configuration disables mixed SKUs")
    void hazmatConfiguration() {
        Restrictions restrictions = Restrictions.createDefault();
        assertTrue(restrictions.getMixedSkuAllowed());
        assertFalse(restrictions.getHazmatAllowed());

        restrictions.configureHazmat(true);
        assertTrue(restrictions.getHazmatAllowed());
        assertFalse(restrictions.getMixedSkuAllowed());
    }

    @Test
    @DisplayName("Temperature control and aging policies validate inputs")
    void temperatureAndAgingPolicies() {
        Restrictions restrictions = Restrictions.createDefault();

        restrictions.configureTemperatureControl(true, "2-8C");
        assertTrue(restrictions.getTemperatureControlled());
        assertEquals("2-8C", restrictions.getTemperatureRange());

        restrictions.setMaxDaysInLocation(10);
        assertEquals(10, restrictions.getMaxDaysInLocation());

        assertThrows(IllegalArgumentException.class, () -> restrictions.setMaxDaysInLocation(0));
    }

    @Test
    @DisplayName("FIFO and LIFO enforcement are mutually exclusive")
    void fifoLifoEnforcement() {
        Restrictions restrictions = Restrictions.createDefault();

        restrictions.enforceFifo(true);
        assertTrue(restrictions.getFifoEnforced());
        assertFalse(restrictions.getLifoEnforced());

        restrictions.enforceLifo(true);
        assertTrue(restrictions.getLifoEnforced());
        assertFalse(restrictions.getFifoEnforced());
    }

    @Test
    @DisplayName("Active restrictions detected when any rule applied")
    void activeRestrictionsDetection() {
        Restrictions restrictions = Restrictions.createDefault();
        assertFalse(restrictions.hasActiveRestrictions());

        restrictions.addAllowedCategory("FOOD");
        assertTrue(restrictions.hasActiveRestrictions());
    }
}
