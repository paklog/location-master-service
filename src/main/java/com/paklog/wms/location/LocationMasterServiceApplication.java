package com.paklog.wms.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Location Master Service - WMS
 * Handles warehouse location hierarchy, configuration, and slotting optimization
 */
@SpringBootApplication
public class LocationMasterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocationMasterServiceApplication.class, args);
    }
}
