package com.paklog.wms.location.adapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wms.location.adapter.rest.dto.*;
import com.paklog.wms.location.application.service.LocationConfigurationService;
import com.paklog.wms.location.application.service.SlottingService;
import com.paklog.wms.location.domain.aggregate.LocationMaster;
import com.paklog.wms.location.domain.entity.Capacity;
import com.paklog.wms.location.domain.valueobject.LocationStatus;
import com.paklog.wms.location.domain.valueobject.LocationType;
import com.paklog.wms.location.domain.valueobject.SlottingClass;
import com.paklog.wms.location.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationConfigurationService configService;

    @MockBean
    private SlottingService slottingService;

    private LocationMaster sampleLocation;

    @BeforeEach
    void setUp() {
        sampleLocation = TestDataFactory.sampleBinLocation("BIN-100");
    }

    @Test
    @DisplayName("Create location returns 201 with response payload")
    void createLocation() throws Exception {
        when(configService.createLocation(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(sampleLocation);

        CreateLocationRequest request = new CreateLocationRequest(
            "BIN-100",
            "WH-001",
            "Primary Bin",
            LocationType.BIN,
            "LEVEL-1",
            LocationType.BIN.getHierarchyDepth(),
            "PICK"
        );

        mockMvc.perform(post("/api/v1/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.locationId").value("BIN-100"))
            .andExpect(jsonPath("$.capacity.maxQuantity").value(sampleLocation.getCapacity().getMaxQuantity()));
    }

    @Test
    @DisplayName("Get location returns 200 when present and 404 when missing")
    void getLocation() throws Exception {
        when(configService.getLocation("BIN-100")).thenReturn(Optional.of(sampleLocation));

        mockMvc.perform(get("/api/v1/locations/BIN-100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId").value("BIN-100"));

        when(configService.getLocation("MISSING")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/locations/MISSING"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("List locations filters by status and zone")
    void listLocations() throws Exception {
        LocationMaster blocked = createLocation("BIN-BLOCK", LocationStatus.BLOCKED, "PACK");
        LocationMaster pick = createLocation("BIN-PICK", LocationStatus.ACTIVE, "PICK");

        when(configService.getLocationsByWarehouse("WH-001"))
            .thenReturn(List.of(sampleLocation, blocked, pick));

        mockMvc.perform(get("/api/v1/locations")
                .param("warehouseId", "WH-001")
                .param("status", "BLOCKED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].locationId").value("BIN-BLOCK"));

        mockMvc.perform(get("/api/v1/locations")
                .param("warehouseId", "WH-001")
                .param("zone", "PACK"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locationId").value("BIN-BLOCK"));

        mockMvc.perform(get("/api/v1/locations")
                .param("warehouseId", "WH-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Storage, pick path and child endpoints delegate to service layer")
    void storageAndHierarchyEndpoints() throws Exception {
        when(configService.getActiveStorageLocations("WH-001"))
            .thenReturn(List.of(sampleLocation));
        when(configService.getPickPathLocations("WH-001", "PICK"))
            .thenReturn(List.of(sampleLocation));
        when(configService.getChildLocations("LEVEL-1"))
            .thenReturn(List.of(sampleLocation));

        mockMvc.perform(get("/api/v1/locations/storage").param("warehouseId", "WH-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locationId").value("BIN-100"));

        mockMvc.perform(get("/api/v1/locations/pick-path")
                .param("warehouseId", "WH-001")
                .param("zone", "PICK"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locationId").value("BIN-100"));

        mockMvc.perform(get("/api/v1/locations/LEVEL-1/children"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locationId").value("BIN-100"));
    }

    @Test
    @DisplayName("Configure capacity converts request payload to domain object")
    void configureCapacity() throws Exception {
        ArgumentCaptor<Capacity> capacityCaptor = ArgumentCaptor.forClass(Capacity.class);
        when(configService.configureCapacity(eq("BIN-100"), any(), eq("tester"), eq("Expansion")))
            .thenReturn(sampleLocation);

        ConfigureCapacityRequest request = new ConfigureCapacityRequest(
            120,
            new BigDecimal("2000"),
            new BigDecimal("40"),
            "KG",
            "M3",
            "Expansion"
        );

        mockMvc.perform(put("/api/v1/locations/BIN-100/capacity")
                .header("X-User-Id", "tester")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId").value("BIN-100"));

        verify(configService).configureCapacity(eq("BIN-100"), capacityCaptor.capture(), eq("tester"), eq("Expansion"));
        Capacity capacity = capacityCaptor.getValue();
        assertEquals(120, capacity.getMaxQuantity());
        assertEquals(new BigDecimal("2000"), capacity.getMaxWeight());
    }

    @Test
    @DisplayName("Slotting update endpoint updates location and returns response")
    void updateSlotting() throws Exception {
        LocationMaster updated = TestDataFactory.sampleBinLocation("BIN-100");
        updated.setSlottingClass(SlottingClass.FAST_MOVER);

        when(configService.updateSlottingClass(eq("BIN-100"), eq(SlottingClass.FAST_MOVER), eq("tester"), eq("Optimization")))
            .thenReturn(updated);

        UpdateSlottingRequest request = new UpdateSlottingRequest(SlottingClass.FAST_MOVER, "Optimization");

        mockMvc.perform(put("/api/v1/locations/BIN-100/slotting")
                .header("X-User-Id", "tester")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slottingClass").value("FAST_MOVER"));
    }

    @Test
    @DisplayName("Lifecycle endpoints propagate to configuration service")
    void lifecycleEndpoints() throws Exception {
        LocationMaster activated = TestDataFactory.sampleBinLocation("BIN-100");
        activated.activate();
        when(configService.activateLocation("BIN-100", "tester")).thenReturn(activated);

        mockMvc.perform(post("/api/v1/locations/BIN-100/activate")
                .header("X-User-Id", "tester"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        LocationMaster deactivated = TestDataFactory.sampleBinLocation("BIN-100");
        deactivated.deactivate();
        when(configService.deactivateLocation("BIN-100", "tester")).thenReturn(deactivated);
        mockMvc.perform(post("/api/v1/locations/BIN-100/deactivate")
                .header("X-User-Id", "tester"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));

        LocationMaster blocked = TestDataFactory.sampleBinLocation("BIN-100");
        blocked.block("Maintenance");
        when(configService.blockLocation(eq("BIN-100"), eq("Maintenance"), eq("tester")))
            .thenReturn(blocked);
        mockMvc.perform(post("/api/v1/locations/BIN-100/block")
                .header("X-User-Id", "tester")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BlockLocationRequest("Maintenance"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("BLOCKED"));

        LocationMaster unblocked = TestDataFactory.sampleBinLocation("BIN-100");
        when(configService.unblockLocation("BIN-100", "tester")).thenReturn(unblocked);
        mockMvc.perform(post("/api/v1/locations/BIN-100/unblock")
                .header("X-User-Id", "tester"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        LocationMaster reserved = TestDataFactory.sampleBinLocation("BIN-100");
        reserved.reserve("Wave");
        when(configService.reserveLocation("BIN-100", "Wave", "tester"))
            .thenReturn(reserved);
        mockMvc.perform(post("/api/v1/locations/BIN-100/reserve")
                .header("X-User-Id", "tester")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReserveLocationRequest("Wave"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    @DisplayName("Slotting recommendations and optimization endpoints operate successfully")
    void slottingEndpoints() throws Exception {
        SlottingService.SlottingRecommendation recommendation = new SlottingService.SlottingRecommendation(
            "BIN-100",
            SlottingClass.A,
            SlottingClass.B,
            80,
            "Reasoned"
        );
        when(slottingService.getSlottingRecommendations("WH-001"))
            .thenReturn(Map.of("BIN-100", recommendation));
        when(slottingService.optimizeZoneSlotting("WH-001", "PICK", "tester"))
            .thenReturn(2);
        when(slottingService.identifyGoldenZone("WH-001", "PICK"))
            .thenReturn(List.of(sampleLocation));

        mockMvc.perform(get("/api/v1/locations/slotting/recommendations")
                .param("warehouseId", "WH-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].confidenceScore").value(80));

        mockMvc.perform(post("/api/v1/locations/slotting/optimize")
                .param("warehouseId", "WH-001")
                .param("zone", "PICK")
                .header("X-User-Id", "tester"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationsUpdated").value(2));

        mockMvc.perform(get("/api/v1/locations/golden-zone")
                .param("warehouseId", "WH-001")
                .param("zone", "PICK"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].locationId").value("BIN-100"));
    }

    @Test
    @DisplayName("Attention required endpoint returns blocked/full locations")
    void attentionRequired() throws Exception {
        when(configService.getLocationsRequiringAttention("WH-001"))
            .thenReturn(List.of(createLocation("BIN-FULL", LocationStatus.FULL, "PICK")));

        mockMvc.perform(get("/api/v1/locations/attention-required")
                .param("warehouseId", "WH-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("FULL"));
    }

    private LocationMaster createLocation(String id, LocationStatus status, String zone) {
        LocationMaster location = LocationMaster.create(
            id,
            "WH-001",
            id,
            LocationType.BIN,
            "LEVEL-1",
            LocationType.BIN.getHierarchyDepth(),
            zone
        );
        location.configureDimensions(TestDataFactory.sampleDimensions());
        location.configureCapacity(TestDataFactory.sampleCapacity());
        switch (status) {
            case BLOCKED -> location.block("Maintenance");
            case RESERVED -> location.reserve("Reserved");
            case FULL -> location.markAsFull();
            case INACTIVE -> location.deactivate();
            case ACTIVE -> location.activate();
            case DECOMMISSIONED -> location.decommission("Removed");
        }
        return location;
    }
}
