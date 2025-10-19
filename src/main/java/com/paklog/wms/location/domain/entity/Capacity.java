package com.paklog.wms.location.domain.entity;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Capacity - Location capacity management
 */
@Embeddable
public class Capacity {
    private Integer maxQuantity;
    private BigDecimal maxWeight;
    private BigDecimal maxVolume;
    private String weightUnit; // KG, LB
    private String volumeUnit; // M3, FT3

    private Integer currentQuantity;
    private BigDecimal currentWeight;
    private BigDecimal currentVolume;

    protected Capacity() {
        // JPA constructor
    }

    public Capacity(Integer maxQuantity, BigDecimal maxWeight, BigDecimal maxVolume,
                   String weightUnit, String volumeUnit) {
        if (maxQuantity == null || maxQuantity <= 0) {
            throw new IllegalArgumentException("Max quantity must be positive");
        }
        if (maxWeight == null || maxWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max weight must be positive");
        }
        if (maxVolume == null || maxVolume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max volume must be positive");
        }

        this.maxQuantity = maxQuantity;
        this.maxWeight = maxWeight;
        this.maxVolume = maxVolume;
        this.weightUnit = weightUnit != null ? weightUnit : "KG";
        this.volumeUnit = volumeUnit != null ? volumeUnit : "M3";

        this.currentQuantity = 0;
        this.currentWeight = BigDecimal.ZERO;
        this.currentVolume = BigDecimal.ZERO;
    }

    /**
     * Check if can accept additional quantity
     */
    public boolean canAccept(int quantity, BigDecimal weight, BigDecimal volume) {
        if (currentQuantity + quantity > maxQuantity) {
            return false;
        }
        if (currentWeight.add(weight).compareTo(maxWeight) > 0) {
            return false;
        }
        if (currentVolume.add(volume).compareTo(maxVolume) > 0) {
            return false;
        }
        return true;
    }

    /**
     * Add inventory to location
     */
    public void addInventory(int quantity, BigDecimal weight, BigDecimal volume) {
        if (!canAccept(quantity, weight, volume)) {
            throw new IllegalStateException("Location does not have sufficient capacity");
        }

        this.currentQuantity += quantity;
        this.currentWeight = this.currentWeight.add(weight);
        this.currentVolume = this.currentVolume.add(volume);
    }

    /**
     * Remove inventory from location
     */
    public void removeInventory(int quantity, BigDecimal weight, BigDecimal volume) {
        if (currentQuantity < quantity) {
            throw new IllegalStateException("Cannot remove more quantity than available");
        }

        this.currentQuantity -= quantity;
        this.currentWeight = this.currentWeight.subtract(weight);
        this.currentVolume = this.currentVolume.subtract(volume);

        // Ensure no negative values
        if (this.currentWeight.compareTo(BigDecimal.ZERO) < 0) {
            this.currentWeight = BigDecimal.ZERO;
        }
        if (this.currentVolume.compareTo(BigDecimal.ZERO) < 0) {
            this.currentVolume = BigDecimal.ZERO;
        }
    }

    /**
     * Calculate utilization percentage
     */
    public BigDecimal getUtilizationPercentage() {
        if (maxQuantity == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal quantityPct = new BigDecimal(currentQuantity)
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal(maxQuantity), 2, RoundingMode.HALF_UP);

        BigDecimal weightPct = currentWeight
            .multiply(new BigDecimal("100"))
            .divide(maxWeight, 2, RoundingMode.HALF_UP);

        BigDecimal volumePct = currentVolume
            .multiply(new BigDecimal("100"))
            .divide(maxVolume, 2, RoundingMode.HALF_UP);

        // Return maximum of the three percentages
        return quantityPct.max(weightPct).max(volumePct);
    }

    /**
     * Check if location is full (>95% utilization)
     */
    public boolean isFull() {
        return getUtilizationPercentage().compareTo(new BigDecimal("95")) >= 0;
    }

    /**
     * Check if location is near capacity (>80% utilization)
     */
    public boolean isNearCapacity() {
        return getUtilizationPercentage().compareTo(new BigDecimal("80")) >= 0;
    }

    /**
     * Get available quantity
     */
    public int getAvailableQuantity() {
        return maxQuantity - currentQuantity;
    }

    /**
     * Get available weight
     */
    public BigDecimal getAvailableWeight() {
        return maxWeight.subtract(currentWeight);
    }

    /**
     * Get available volume
     */
    public BigDecimal getAvailableVolume() {
        return maxVolume.subtract(currentVolume);
    }

    // Getters
    public Integer getMaxQuantity() {
        return maxQuantity;
    }

    public BigDecimal getMaxWeight() {
        return maxWeight;
    }

    public BigDecimal getMaxVolume() {
        return maxVolume;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public String getVolumeUnit() {
        return volumeUnit;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getCurrentWeight() {
        return currentWeight;
    }

    public BigDecimal getCurrentVolume() {
        return currentVolume;
    }

    @Override
    public String toString() {
        return String.format("Capacity[qty=%d/%d, weight=%s/%s %s, volume=%s/%s %s, util=%s%%]",
            currentQuantity, maxQuantity,
            currentWeight, maxWeight, weightUnit,
            currentVolume, maxVolume, volumeUnit,
            getUtilizationPercentage());
    }
}
