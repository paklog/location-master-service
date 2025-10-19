package com.paklog.wms.location.domain.repository;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.entity.Dimensions;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LocationMasterRepositoryIntegrationTest {

    private static PostgreSQLContainer<?> postgres;
    private static final Logger logger = LoggerFactory.getLogger(LocationMasterRepositoryIntegrationTest.class);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        try {
            PostgreSQLContainer<?> container = postgres();
            registry.add("spring.datasource.url", container::getJdbcUrl);
            registry.add("spring.datasource.username", container::getUsername);
            registry.add("spring.datasource.password", container::getPassword);
        } catch (IllegalStateException ex) {
            logger.warn("Docker not available, falling back to in-memory H2 database for integration tests: {}", ex.getMessage());
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
        }
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private LocationMasterRepository repository;

    private LocationMaster warehouse;
    private LocationMaster zone;
    private LocationMaster activeBin;
    private LocationMaster reservedBin;
    private LocationMaster fullBin;
    private LocationMaster blockedBin;
    private LocationMaster secondaryActive;

    @AfterAll
    void tearDown() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        warehouse = LocationMaster.create(
            "WAREHOUSE-ROOT",
            "WH-001",
            "Main Warehouse",
            LocationType.WAREHOUSE,
            null,
            0,
            null
        );
        repository.save(warehouse);

        zone = LocationMaster.create(
            "ZONE-1",
            "WH-001",
            "Pick Zone",
            LocationType.ZONE,
            warehouse.getLocationId(),
            LocationType.ZONE.getHierarchyDepth(),
            "PICK"
        );
        repository.save(zone);

        activeBin = createBin("BIN-ACTIVE", LocationStatus.ACTIVE, SlottingClass.A, 15, 10, 10);
        secondaryActive = createBin("BIN-ACTIVE-2", LocationStatus.ACTIVE, SlottingClass.B, 25, 20, 2);
        reservedBin = createBin("BIN-RESERVED", LocationStatus.RESERVED, SlottingClass.B, 30, 30, 5);
        fullBin = createBin("BIN-FULL", LocationStatus.FULL, SlottingClass.C, 80, 40, 100);
        blockedBin = createBin("BIN-BLOCKED", LocationStatus.BLOCKED, SlottingClass.SLOW_MOVER, 40, 50, 0);

        repository.saveAll(List.of(activeBin, secondaryActive, reservedBin, fullBin, blockedBin));
        repository.flush();
    }

    @Test
    @DisplayName("findByWarehouseId returns all persisted locations")
    void findByWarehouseId() {
        List<LocationMaster> locations = repository.findByWarehouseId("WH-001");
        assertThat(locations).hasSize(7);
    }

    @Test
    @DisplayName("Active storage locations filter by status and type")
    void findActiveStorageLocations() {
        List<LocationMaster> storage = repository.findActiveStorageLocations("WH-001");
        assertThat(storage).extracting(LocationMaster::getLocationId)
            .containsExactlyInAnyOrder("BIN-ACTIVE", "BIN-ACTIVE-2");
    }

    @Test
    @DisplayName("Available locations consider capacity and status")
    void findAvailableLocationsWithCapacity() {
        List<LocationMaster> available = repository.findAvailableLocationsWithCapacity("WH-001", "PICK");
        assertThat(available).extracting(LocationMaster::getLocationId)
            .containsExactlyInAnyOrder("BIN-ACTIVE", "BIN-ACTIVE-2", "BIN-RESERVED");
    }

    @Test
    @DisplayName("Pick path ordering respects configured sequence")
    void findPickPathLocationsOrdersBySequence() {
        List<LocationMaster> ordered = repository.findPickPathLocations("WH-001", "PICK");
        assertThat(ordered).extracting(LocationMaster::getLocationId)
            .containsExactly("BIN-ACTIVE", "BIN-ACTIVE-2");
    }

    @Test
    @DisplayName("Locations requiring attention include blocked and full")
    void findLocationsRequiringAttention() {
        List<LocationMaster> attention = repository.findLocationsRequiringAttention("WH-001");
        assertThat(attention).extracting(LocationMaster::getLocationId)
            .containsExactlyInAnyOrder("BIN-FULL", "BIN-BLOCKED");
    }

    @Test
    @DisplayName("Warehouse root and zone queries succeed")
    void findHierarchyMetadata() {
        assertThat(repository.findWarehouseRoot("WH-001"))
            .isPresent()
            .get()
            .extracting(LocationMaster::getLocationId)
            .isEqualTo("WAREHOUSE-ROOT");

        assertThat(repository.findZones("WH-001"))
            .extracting(LocationMaster::getLocationName)
            .containsExactly("Pick Zone");
    }

    @Test
    @DisplayName("Optimal slotting locates candidates sorted by priority")
    void findOptimalSlottingLocations() {
        List<LocationMaster> optimal = repository.findOptimalSlottingLocations("WH-001", "PICK", SlottingClass.A);
        assertThat(optimal).first().extracting(LocationMaster::getLocationId)
            .isEqualTo("BIN-ACTIVE");
    }

    private LocationMaster createBin(
            String locationId,
            LocationStatus status,
            SlottingClass slottingClass,
            int distanceFromDock,
            int pickSequence,
            int currentQuantity
    ) {
        LocationMaster bin = LocationMaster.create(
            locationId,
            "WH-001",
            locationId,
            LocationType.BIN,
            zone.getLocationId(),
            LocationType.BIN.getHierarchyDepth(),
            "PICK"
        );
        bin.setSlottingClass(slottingClass);
        bin.setDistanceFromDock(distanceFromDock);
        bin.setPickPathSequence(pickSequence);
        bin.setPhysicalAddress("A1", "B" + pickSequence, "L1", "P1");
        bin.configureDimensions(new Dimensions(
            new BigDecimal("120"),
            new BigDecimal("80"),
            new BigDecimal("60"),
            "CM"
        ));
        Capacity capacity = new Capacity(
            100,
            new BigDecimal("1000"),
            new BigDecimal("20"),
            "KG",
            "M3"
        );
        if (currentQuantity > 0) {
            capacity.addInventory(
                currentQuantity,
                new BigDecimal(currentQuantity * 5L),
                BigDecimal.valueOf(currentQuantity).multiply(new BigDecimal("0.1"))
            );
        }
        bin.configureCapacity(capacity);

        switch (status) {
            case ACTIVE -> {
                // default
            }
            case RESERVED -> bin.reserve("Wave");
            case FULL -> {
                bin.markAsFull();
            }
            case BLOCKED -> bin.block("Maintenance");
            case INACTIVE -> bin.deactivate();
            case DECOMMISSIONED -> bin.decommission("Retired");
        }

        return bin;
    }

    private static PostgreSQLContainer<?> postgres() {
        if (postgres == null) {
            try {
                DockerClientFactory.instance().client();
            } catch (IllegalStateException ex) {
                throw new IllegalStateException("Docker unavailable", ex);
            }
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("location_master")
                .withUsername("test")
                .withPassword("test");
            postgres.start();
        }
        return postgres;
    }
}
