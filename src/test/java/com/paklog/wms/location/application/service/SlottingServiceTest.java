package com.paklog.wms.location.application.service;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.repository.LocationMasterRepository;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import com.paklog.wms.location.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlottingServiceTest {

    @Mock
    private LocationMasterRepository locationRepository;

    @Mock
    private LocationConfigurationService configService;

    private SlottingService service;

    @BeforeEach
    void setUp() {
        service = new SlottingService(locationRepository, configService);
    }

    @Test
    @DisplayName("findOptimalLocation returns first candidate with sufficient capacity")
    void findOptimalLocation() {
        LocationMaster candidate = TestDataFactory.sampleBinLocation("BIN-1");
        LocationMaster insufficient = TestDataFactory.sampleBinLocation("BIN-2");
        insufficient.configureCapacity(new Capacity(
            5, new BigDecimal("50"), new BigDecimal("5"), "KG", "M3"
        ));

        when(locationRepository.findOptimalSlottingLocations("WH-001", "PICK", SlottingClass.A))
            .thenReturn(List.of(candidate, insufficient));

        Optional<LocationMaster> result = service.findOptimalLocation(
            "WH-001", "PICK", SlottingClass.A, 10
        );

        assertTrue(result.isPresent());
        assertEquals("BIN-1", result.get().getLocationId());

        when(locationRepository.findOptimalSlottingLocations("WH-001", "PICK", SlottingClass.B))
            .thenReturn(List.of(insufficient));
        assertTrue(service.findOptimalLocation("WH-001", "PICK", SlottingClass.B, 10).isEmpty());
    }

    @Test
    @DisplayName("optimizeZoneSlotting updates slotting class for qualifying locations")
    void optimizeZoneSlotting() {
        LocationMaster nearDock = TestDataFactory.sampleBinLocation("BIN-3");
        nearDock.setSlottingClass(SlottingClass.C);
        nearDock.setDistanceFromDock(10);

        LocationMaster midZone = TestDataFactory.sampleBinLocation("BIN-4");
        midZone.setSlottingClass(SlottingClass.SLOW_MOVER);
        midZone.setDistanceFromDock(60);

        when(locationRepository.findByWarehouseIdAndZoneAndStatus(
            "WH-001", "PICK", LocationStatus.ACTIVE))
            .thenReturn(List.of(nearDock, midZone));

        when(configService.updateSlottingClass(eq("BIN-3"), eq(SlottingClass.FAST_MOVER), any(), any()))
            .thenReturn(nearDock);
        when(configService.updateSlottingClass(eq("BIN-4"), eq(SlottingClass.B), any(), any()))
            .thenReturn(midZone);

        int updated = service.optimizeZoneSlotting("WH-001", "PICK", "optimizer");

        assertEquals(2, updated);
        verify(configService, times(2)).updateSlottingClass(anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("optimizeZoneSlotting ignores non-storage locations and missing distances")
    void optimizeZoneSlottingSkipsNonInventoryLocations() {
        LocationMaster aisle = LocationMaster.create(
            "AISLE-1",
            "WH-001",
            "Aisle 1",
            LocationType.AISLE,
            "ZONE-1",
            LocationType.AISLE.getHierarchyDepth(),
            "PICK"
        );
        aisle.setDistanceFromDock(10);

        when(locationRepository.findByWarehouseIdAndZoneAndStatus(
            "WH-001", "PICK", LocationStatus.ACTIVE))
            .thenReturn(List.of(aisle));

        int updated = service.optimizeZoneSlotting("WH-001", "PICK", "optimizer");
        assertEquals(0, updated);
        verifyNoInteractions(configService);
    }

    @Test
    @DisplayName("Slotting recommendations include reasoning and confidence scores")
    void getSlottingRecommendations() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-5");
        location.setDistanceFromDock(25);
        when(locationRepository.findActiveStorageLocations("WH-001"))
            .thenReturn(List.of(location));

        Map<String, SlottingService.SlottingRecommendation> recommendations =
            service.getSlottingRecommendations("WH-001");

        assertEquals(1, recommendations.size());
        SlottingService.SlottingRecommendation recommendation = recommendations.get("BIN-5");
        assertNotNull(recommendation);
        assertEquals("BIN-5", recommendation.locationId());
        assertTrue(recommendation.confidenceScore() > 0);
    }

    @Test
    @DisplayName("Golden zone identifies closest storage locations")
    void identifyGoldenZone() {
        LocationMaster fast = TestDataFactory.sampleBinLocation("BIN-6");
        fast.setDistanceFromDock(5);
        fast.setSlottingClass(SlottingClass.FAST_MOVER);

        LocationMaster slow = TestDataFactory.sampleBinLocation("BIN-7");
        slow.setDistanceFromDock(100);
        slow.setSlottingClass(SlottingClass.SLOW_MOVER);

        when(locationRepository.findByWarehouseIdAndZoneAndStatus(
            "WH-001", "PICK", LocationStatus.ACTIVE))
            .thenReturn(List.of(slow, fast));

        List<LocationMaster> goldenZone = service.identifyGoldenZone("WH-001", "PICK");
        assertEquals(1, goldenZone.size());
        assertEquals("BIN-6", goldenZone.getFirst().getLocationId());
    }

    @Test
    @DisplayName("balanceSlotting triggers optimization when distribution skewed")
    void balanceSlotting() {
        LocationMaster zone = TestDataFactory.sampleZone("ZONE-1", "PICK");
        LocationMaster fast = TestDataFactory.sampleBinLocation("BIN-8");
        fast.setSlottingClass(SlottingClass.SLOW_MOVER);
        fast.setDistanceFromDock(10);

        LocationMaster aClass = TestDataFactory.sampleBinLocation("BIN-9");
        aClass.setSlottingClass(SlottingClass.A);
        aClass.setDistanceFromDock(30);

        LocationMaster bClass = TestDataFactory.sampleBinLocation("BIN-10");
        bClass.setSlottingClass(SlottingClass.B);
        bClass.setDistanceFromDock(80);

        when(locationRepository.findZones("WH-001")).thenReturn(List.of(zone));
        when(locationRepository.findByWarehouseIdAndZone("WH-001", "PICK"))
            .thenReturn(List.of(fast, aClass, bClass));
        when(locationRepository.findByWarehouseIdAndZoneAndStatus("WH-001", "PICK", LocationStatus.ACTIVE))
            .thenReturn(List.of(fast, aClass, bClass));

        when(configService.updateSlottingClass(anyString(), any(), any(), any()))
            .thenReturn(fast);

        SlottingService.SlottingBalanceReport report =
            service.balanceSlotting("WH-001", "optimizer");

        assertTrue(report.locationsRebalanced() > 0);
        assertTrue(report.distributionByZone().containsKey("PICK"));
    }

    @Test
    @DisplayName("Optimized pick path sorts by slotting priority and distance")
    void optimizedPickPath() {
        LocationMaster fast = TestDataFactory.sampleBinLocation("BIN-11");
        fast.setSlottingClass(SlottingClass.FAST_MOVER);
        fast.setDistanceFromDock(40);
        fast.setPickPathSequence(50);

        LocationMaster slow = TestDataFactory.sampleBinLocation("BIN-12");
        slow.setSlottingClass(SlottingClass.SLOW_MOVER);
        slow.setDistanceFromDock(10);
        slow.setPickPathSequence(10);

        LocationMaster medium = TestDataFactory.sampleBinLocation("BIN-13");
        medium.setSlottingClass(SlottingClass.B);
        medium.setDistanceFromDock(20);
        medium.setPickPathSequence(5);

        when(locationRepository.findPickPathLocations("WH-001", "PICK"))
            .thenReturn(List.of(slow, medium, fast));

        List<LocationMaster> pickPath = service.getOptimizedPickPath("WH-001", "PICK");
        assertEquals(List.of(fast, medium, slow), pickPath);
    }
}
