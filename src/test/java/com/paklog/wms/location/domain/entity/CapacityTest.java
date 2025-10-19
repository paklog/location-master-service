package com.paklog.wms.location.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CapacityTest {

    private static Capacity newCapacity() {
        return new Capacity(
            100,
            new BigDecimal("1000"),
            new BigDecimal("20"),
            "KG",
            "M3"
        );
    }

    @Test
    @DisplayName("Constructor enforces positive max values")
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class,
            () -> new Capacity(0, BigDecimal.ONE, BigDecimal.ONE, "KG", "M3"));
        assertThrows(IllegalArgumentException.class,
            () -> new Capacity(1, BigDecimal.ZERO, BigDecimal.ONE, "KG", "M3"));
        assertThrows(IllegalArgumentException.class,
            () -> new Capacity(1, BigDecimal.ONE, BigDecimal.ZERO, "KG", "M3"));
    }

    @Test
    @DisplayName("canAccept and addInventory update totals and enforce limits")
    void canAcceptAndAddInventory() {
        Capacity capacity = newCapacity();

        assertTrue(capacity.canAccept(10, new BigDecimal("100"), new BigDecimal("5")));
        capacity.addInventory(10, new BigDecimal("100"), new BigDecimal("5"));

        assertEquals(90, capacity.getAvailableQuantity());
        assertEquals(new BigDecimal("900"), capacity.getAvailableWeight());
        assertEquals(new BigDecimal("15"), capacity.getAvailableVolume());

        assertThrows(IllegalStateException.class,
            () -> capacity.addInventory(200, new BigDecimal("100"), new BigDecimal("1")));
    }

    @Test
    @DisplayName("removeInventory decreases usage and prevents overdraft")
    void removeInventory() {
        Capacity capacity = newCapacity();
        capacity.addInventory(50, new BigDecimal("600"), new BigDecimal("10"));

        capacity.removeInventory(20, new BigDecimal("300"), new BigDecimal("9"));

        assertEquals(30, capacity.getCurrentQuantity());
        assertEquals(new BigDecimal("300"), capacity.getCurrentWeight());
        assertEquals(new BigDecimal("1"), capacity.getCurrentVolume());

        assertThrows(IllegalStateException.class,
            () -> capacity.removeInventory(50, new BigDecimal("100"), new BigDecimal("1")));
    }

    @Test
    @DisplayName("Utilization drives near-capacity and full thresholds")
    void utilizationFlags() {
        Capacity capacity = newCapacity();

        capacity.addInventory(90, new BigDecimal("900"), new BigDecimal("18"));
        assertTrue(capacity.isNearCapacity());
        assertFalse(capacity.isFull());

        capacity.addInventory(5, new BigDecimal("50"), new BigDecimal("1"));
        assertTrue(capacity.isFull());
        assertTrue(capacity.getUtilizationPercentage().compareTo(new BigDecimal("95")) >= 0);
    }
}
