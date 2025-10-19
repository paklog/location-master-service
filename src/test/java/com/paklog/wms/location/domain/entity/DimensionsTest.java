package com.paklog.wms.location.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DimensionsTest {

    @Test
    @DisplayName("Constructor rejects non-positive values and blank unit")
    void constructorValidation() {
        assertThrows(IllegalArgumentException.class,
            () -> new Dimensions(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, "CM"));
        assertThrows(IllegalArgumentException.class,
            () -> new Dimensions(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, "CM"));
        assertThrows(IllegalArgumentException.class,
            () -> new Dimensions(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, "CM"));
        assertThrows(IllegalArgumentException.class,
            () -> new Dimensions(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, " "));
    }

    @Test
    @DisplayName("Volume and floor area calculations use simple multiplication")
    void calculatesVolumeAndArea() {
        Dimensions dimensions = new Dimensions(
            new BigDecimal("2"),
            new BigDecimal("3"),
            new BigDecimal("4"),
            "M"
        );

        assertEquals(new BigDecimal("24"), dimensions.calculateVolume());
        assertEquals(new BigDecimal("6"), dimensions.calculateFloorArea());
        assertEquals("2 x 3 x 4 M", dimensions.asString());
    }

    @Test
    @DisplayName("canFit requires same unit and each side larger or equal")
    void canFitValidation() {
        Dimensions container = new Dimensions(
            new BigDecimal("10"),
            new BigDecimal("10"),
            new BigDecimal("10"),
            "CM"
        );
        Dimensions item = new Dimensions(
            new BigDecimal("9"),
            new BigDecimal("9"),
            new BigDecimal("9"),
            "CM"
        );

        assertTrue(container.canFit(item));

        Dimensions largeItem = new Dimensions(
            new BigDecimal("11"),
            new BigDecimal("10"),
            new BigDecimal("10"),
            "CM"
        );
        assertFalse(container.canFit(largeItem));

        Dimensions differentUnit = new Dimensions(
            new BigDecimal("1"),
            new BigDecimal("1"),
            new BigDecimal("1"),
            "INCH"
        );
        assertThrows(IllegalArgumentException.class, () -> container.canFit(differentUnit));
    }
}
