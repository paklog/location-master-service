package com.paklog.wms.location.domain.entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import java.util.HashSet;
import java.util.Set;

/**
 * Restrictions - Storage restrictions and rules for a location
 */
@Embeddable
public class Restrictions {
    @ElementCollection
    private Set<String> allowedProductCategories;

    @ElementCollection
    private Set<String> blockedProductCategories;

    private Boolean mixedSkuAllowed;
    private Boolean mixedLotAllowed;
    private Boolean hazmatAllowed;
    private Boolean temperatureControlled;
    private String temperatureRange; // e.g., "2-8C"

    private Integer maxDaysInLocation; // Max aging days
    private Boolean fifoEnforced; // First-In-First-Out
    private Boolean lifoEnforced; // Last-In-First-Out

    protected Restrictions() {
        // JPA constructor
        this.allowedProductCategories = new HashSet<>();
        this.blockedProductCategories = new HashSet<>();
        this.mixedSkuAllowed = true;
        this.mixedLotAllowed = true;
        this.hazmatAllowed = false;
        this.temperatureControlled = false;
        this.fifoEnforced = false;
        this.lifoEnforced = false;
    }

    public static Restrictions createDefault() {
        return new Restrictions();
    }

    public static Restrictions createSingleSkuOnly() {
        Restrictions restrictions = new Restrictions();
        restrictions.mixedSkuAllowed = false;
        restrictions.mixedLotAllowed = false;
        return restrictions;
    }

    public static Restrictions createHazmat() {
        Restrictions restrictions = new Restrictions();
        restrictions.hazmatAllowed = true;
        restrictions.mixedSkuAllowed = false;
        return restrictions;
    }

    public static Restrictions createTemperatureControlled(String temperatureRange) {
        Restrictions restrictions = new Restrictions();
        restrictions.temperatureControlled = true;
        restrictions.temperatureRange = temperatureRange;
        restrictions.mixedSkuAllowed = true;
        return restrictions;
    }

    /**
     * Check if product category is allowed
     */
    public boolean isProductCategoryAllowed(String category) {
        if (category == null || category.isBlank()) {
            return false;
        }

        // If blocked list contains category, deny
        if (blockedProductCategories.contains(category)) {
            return false;
        }

        // If allowed list is empty, allow all (except blocked)
        if (allowedProductCategories.isEmpty()) {
            return true;
        }

        // Check if in allowed list
        return allowedProductCategories.contains(category);
    }

    /**
     * Add allowed product category
     */
    public void addAllowedCategory(String category) {
        if (category != null && !category.isBlank()) {
            allowedProductCategories.add(category);
        }
    }

    /**
     * Block product category
     */
    public void blockCategory(String category) {
        if (category != null && !category.isBlank()) {
            blockedProductCategories.add(category);
            allowedProductCategories.remove(category);
        }
    }

    /**
     * Set mixed SKU policy
     */
    public void setMixedSkuAllowed(boolean allowed) {
        this.mixedSkuAllowed = allowed;
    }

    /**
     * Set mixed lot policy
     */
    public void setMixedLotAllowed(boolean allowed) {
        this.mixedLotAllowed = allowed;
    }

    /**
     * Configure hazmat storage
     */
    public void configureHazmat(boolean allowed) {
        this.hazmatAllowed = allowed;
        if (allowed) {
            // Hazmat locations should not mix SKUs for safety
            this.mixedSkuAllowed = false;
        }
    }

    /**
     * Configure temperature control
     */
    public void configureTemperatureControl(boolean controlled, String temperatureRange) {
        this.temperatureControlled = controlled;
        this.temperatureRange = temperatureRange;
    }

    /**
     * Set aging policy
     */
    public void setMaxDaysInLocation(Integer maxDays) {
        if (maxDays != null && maxDays <= 0) {
            throw new IllegalArgumentException("Max days must be positive");
        }
        this.maxDaysInLocation = maxDays;
    }

    /**
     * Configure FIFO enforcement
     */
    public void enforceFifo(boolean enforce) {
        this.fifoEnforced = enforce;
        if (enforce) {
            this.lifoEnforced = false; // Cannot have both FIFO and LIFO
        }
    }

    /**
     * Configure LIFO enforcement
     */
    public void enforceLifo(boolean enforce) {
        this.lifoEnforced = enforce;
        if (enforce) {
            this.fifoEnforced = false; // Cannot have both FIFO and LIFO
        }
    }

    /**
     * Check if location has any active restrictions
     */
    public boolean hasActiveRestrictions() {
        return !allowedProductCategories.isEmpty() ||
               !blockedProductCategories.isEmpty() ||
               !mixedSkuAllowed ||
               !mixedLotAllowed ||
               hazmatAllowed ||
               temperatureControlled ||
               maxDaysInLocation != null ||
               fifoEnforced ||
               lifoEnforced;
    }

    // Getters
    public Set<String> getAllowedProductCategories() {
        return new HashSet<>(allowedProductCategories);
    }

    public Set<String> getBlockedProductCategories() {
        return new HashSet<>(blockedProductCategories);
    }

    public Boolean getMixedSkuAllowed() {
        return mixedSkuAllowed;
    }

    public Boolean getMixedLotAllowed() {
        return mixedLotAllowed;
    }

    public Boolean getHazmatAllowed() {
        return hazmatAllowed;
    }

    public Boolean getTemperatureControlled() {
        return temperatureControlled;
    }

    public String getTemperatureRange() {
        return temperatureRange;
    }

    public Integer getMaxDaysInLocation() {
        return maxDaysInLocation;
    }

    public Boolean getFifoEnforced() {
        return fifoEnforced;
    }

    public Boolean getLifoEnforced() {
        return lifoEnforced;
    }

    @Override
    public String toString() {
        return String.format("Restrictions[mixedSku=%s, hazmat=%s, tempControl=%s, restrictions=%s]",
            mixedSkuAllowed, hazmatAllowed, temperatureControlled,
            hasActiveRestrictions() ? "active" : "none");
    }
}
