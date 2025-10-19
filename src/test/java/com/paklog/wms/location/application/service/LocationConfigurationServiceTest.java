package com.paklog.wms.location.application.service;

import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.entity.Restrictions;
import com.paklog.wms.location.domain.repository.LocationMasterRepository;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import com.paklog.wms.location.infrastructure.events.LocationEventPublisher;
import com.paklog.wms.location.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class LocationConfigurationServiceTest {

    @Mock
    private LocationMasterRepository locationRepository;

    @Mock
    private LocationEventPublisher eventPublisher;

    private LocationConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new LocationConfigurationService(locationRepository, eventPublisher);
    }

    @Test
    @DisplayName("createLocation validates parent and publishes event")
    void createLocation() {
        LocationMaster parent = TestDataFactory.sampleWarehouse("WAREHOUSE-ROOT");
        when(locationRepository.findById("WAREHOUSE-ROOT"))
            .thenReturn(Optional.of(parent));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocationMaster created = service.createLocation(
            "ZONE-001",
            "WH-001",
            "Zone 1",
            LocationType.ZONE,
            "WAREHOUSE-ROOT",
            null,
            "ZONE-1",
            "tester"
        );

        assertEquals("ZONE-001", created.getLocationId());
        verify(locationRepository).save(any(LocationMaster.class));
        verify(eventPublisher).publishLocationCreated(any());
    }

    @Test
    @DisplayName("createLocation rejects parents that cannot have children")
    void createLocationRejectsInvalidParent() {
        LocationMaster parent = TestDataFactory.sampleBinLocation("BIN-001");
        when(locationRepository.findById("BIN-001"))
            .thenReturn(Optional.of(parent));

        assertThrows(IllegalArgumentException.class, () -> service.createLocation(
            "BIN-CHILD",
            "WH-001",
            "Child",
            LocationType.BIN,
            "BIN-001",
            null,
            "PICK",
            "tester"
        ));
        verify(locationRepository, never()).save(any());
    }

    @Test
    @DisplayName("configureCapacity publishes event when capacity changes")
    void configureCapacityPublishesEvent() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-002");
        when(locationRepository.findById("BIN-002"))
            .thenReturn(Optional.of(location));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Capacity newCapacity = new Capacity(
            150,
            new BigDecimal("1500"),
            new BigDecimal("30"),
            "KG",
            "M3"
        );

        service.configureCapacity("BIN-002", newCapacity, "updater", "reason");

        verify(locationRepository).save(location);
        verify(eventPublisher).publishLocationCapacityChanged(any());
    }

    @Test
    @DisplayName("configureCapacity sets capacity without event when first configured")
    void configureCapacityWithoutPrevious() {
        LocationMaster location = LocationMaster.create(
            "BIN-003",
            "WH-001",
            "Bin 3",
            LocationType.BIN,
            "LEVEL-001",
            null,
            "PICK"
        );
        when(locationRepository.findById("BIN-003"))
            .thenReturn(Optional.of(location));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.configureCapacity("BIN-003", TestDataFactory.sampleCapacity(), "user", "setup");

        verify(eventPublisher, never()).publishLocationCapacityChanged(any());
    }

    @Test
    @DisplayName("updateSlottingClass persists change and emits event")
    void updateSlottingClass() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-004");
        when(locationRepository.findById("BIN-004"))
            .thenReturn(Optional.of(location));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateSlottingClass("BIN-004", SlottingClass.FAST_MOVER, "optimiser", "automation");

        assertEquals(SlottingClass.FAST_MOVER, location.getSlottingClass());
        verify(eventPublisher).publishLocationSlottingChanged(any());
    }

    @Test
    @DisplayName("Lifecycle transitions publish status changed events")
    void statusTransitions() {
        LocationMaster inactive = TestDataFactory.sampleBinLocation("BIN-005");
        inactive.deactivate();
        when(locationRepository.findById("BIN-005")).thenReturn(Optional.of(inactive));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.activateLocation("BIN-005", "user");
        verify(eventPublisher).publishLocationStatusChanged(argThat(event ->
            event.locationId().equals("BIN-005") && event.newStatus() == LocationStatus.ACTIVE));

        LocationMaster active = TestDataFactory.sampleBinLocation("BIN-006");
        when(locationRepository.findById("BIN-006")).thenReturn(Optional.of(active));

        service.blockLocation("BIN-006", "Damage", "user");
        verify(eventPublisher, atLeast(1)).publishLocationStatusChanged(argThat(event ->
            event.locationId().equals("BIN-006") && event.newStatus() == LocationStatus.BLOCKED));

        reset(eventPublisher);
        when(locationRepository.findById("BIN-006")).thenReturn(Optional.of(active));
        service.reserveLocation("BIN-006", "Wave", "user");
        verify(eventPublisher).publishLocationStatusChanged(argThat(event ->
            event.newStatus() == LocationStatus.RESERVED));
    }

    @Test
    @DisplayName("Decommission publishes status changed event")
    void decommissionLocation() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-007");
        when(locationRepository.findById("BIN-007")).thenReturn(Optional.of(location));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.decommissionLocation("BIN-007", "Relocate", "user");

        verify(eventPublisher).publishLocationStatusChanged(argThat(event ->
            event.newStatus() == LocationStatus.DECOMMISSIONED));
    }

    @Test
    @DisplayName("setPickPathSequence delegates to repository")
    void setPickPathSequence() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-008");
        when(locationRepository.findById("BIN-008")).thenReturn(Optional.of(location));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.setPickPathSequence("BIN-008", 500, "user");

        assertEquals(500, location.getPickPathSequence());
        verify(locationRepository).save(location);
    }

    @Test
    @DisplayName("Getting locations delegates to repository")
    void queryMethodsDelegate() {
        LocationMaster location = TestDataFactory.sampleBinLocation("BIN-009");
        when(locationRepository.findById("BIN-009")).thenReturn(Optional.of(location));
        when(locationRepository.findByWarehouseId("WH-001")).thenReturn(List.of(location));
        when(locationRepository.findActiveStorageLocations("WH-001")).thenReturn(List.of(location));
        when(locationRepository.findPickPathLocations("WH-001", "PICK")).thenReturn(List.of(location));
        when(locationRepository.findLocationsRequiringAttention("WH-001")).thenReturn(List.of(location));

        assertTrue(service.getLocation("BIN-009").isPresent());
        assertEquals(1, service.getLocationsByWarehouse("WH-001").size());
        assertEquals(1, service.getActiveStorageLocations("WH-001").size());
        assertEquals(1, service.getPickPathLocations("WH-001", "PICK").size());
        assertEquals(1, service.getLocationsRequiringAttention("WH-001").size());
    }

    @Test
    @DisplayName("Missing locations throw informative exception")
    void missingLocationThrows() {
        when(locationRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.configureRestrictions("MISSING", Restrictions.createDefault(), "user"));
    }
}
