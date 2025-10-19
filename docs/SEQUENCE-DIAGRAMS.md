# Location Master Service - Sequence Diagrams

## Overview

This document contains sequence diagrams for key operational flows in the Location Master Service.

## 1. Create Location Hierarchy

```mermaid
sequenceDiagram
    participant Admin
    participant Controller as LocationController
    participant Service as LocationConfigurationService
    participant Repo as LocationMasterRepository
    participant EventPub as LocationEventPublisher

    Note over Admin,EventPub: Create Warehouse (Root)

    Admin->>Controller: POST /locations<br/>{type: WAREHOUSE, name: "WH-001"}
    Controller->>Service: createLocation(params)

    Service->>Service: LocationMaster.create()<br/>hierarchyLevel = 0<br/>parentLocationId = null
    Service->>Service: location.validateHierarchy()

    Service->>Repo: save(location)
    Repo-->>Service: LocationMaster

    Service->>EventPub: publishLocationCreated(event)
    Service-->>Controller: LocationMaster
    Controller-->>Admin: 201 Created

    Note over Admin,EventPub: Create Zone (Child of Warehouse)

    Admin->>Controller: POST /locations<br/>{type: ZONE, name: "PICK",<br/>parentLocationId: "WH-001"}
    Controller->>Service: createLocation(params)

    Service->>Repo: findById(parentLocationId)
    Repo-->>Service: Parent LocationMaster

    Service->>Service: parent.canHaveChildren()?
    alt Cannot Have Children
        Service-->>Controller: IllegalArgumentException
        Controller-->>Admin: 400 Bad Request
    else Can Have Children
        Service->>Service: LocationMaster.create()<br/>hierarchyLevel = 1
        Service->>Repo: save(location)
        Service->>EventPub: publishLocationCreated(event)
        Service-->>Controller: LocationMaster
        Controller-->>Admin: 201 Created
    end
```

## 2. Configure Location Capacity and Slotting

```mermaid
sequenceDiagram
    participant Admin
    participant Controller as LocationController
    participant ConfigService as LocationConfigurationService
    participant SlottingService
    participant Repo as LocationMasterRepository
    participant EventPub as LocationEventPublisher

    Note over Admin,EventPub: Configure Capacity

    Admin->>Controller: PUT /locations/{id}/capacity<br/>{maxQuantity: 1000, maxWeight: 2000}
    Controller->>ConfigService: configureCapacity(locationId, capacity, admin)

    ConfigService->>Repo: findById(locationId)
    Repo-->>ConfigService: LocationMaster

    ConfigService->>ConfigService: location.configureCapacity(capacity)
    Note over ConfigService: Previous: 500<br/>New: 1000

    ConfigService->>Repo: save(location)
    Repo-->>ConfigService: LocationMaster

    ConfigService->>EventPub: publishLocationCapacityChanged(event)
    ConfigService-->>Controller: LocationMaster
    Controller-->>Admin: 200 OK

    Note over Admin,EventPub: Update Slotting Classification

    Admin->>Controller: PUT /locations/{id}/slotting<br/>{slottingClass: "A", reason: "High velocity"}
    Controller->>ConfigService: updateSlottingClass(locationId, A, admin, reason)

    ConfigService->>Repo: findById(locationId)
    Repo-->>ConfigService: LocationMaster

    ConfigService->>ConfigService: location.setSlottingClass(A)
    Note over ConfigService: Calculates pickPathSequence<br/>based on slotting priority

    ConfigService->>Repo: save(location)
    ConfigService->>EventPub: publishLocationSlottingChanged(event)
    ConfigService-->>Controller: LocationMaster
    Controller-->>Admin: 200 OK
```

## 3. Optimize Zone Slotting

```mermaid
sequenceDiagram
    participant Admin
    participant Controller as LocationController
    participant SlottingService
    participant ConfigService as LocationConfigurationService
    participant Repo as LocationMasterRepository

    Admin->>Controller: POST /locations/warehouse/{id}/slotting/optimize<br/>{zone: "PICK"}
    Controller->>SlottingService: optimizeZoneSlotting(warehouseId, "PICK", admin)

    SlottingService->>Repo: findByWarehouseIdAndZoneAndStatus(warehouseId, "PICK", ACTIVE)
    Repo-->>SlottingService: List<LocationMaster>

    loop For Each Location
        SlottingService->>SlottingService: distanceFromDock = location.distanceFromDock
        SlottingService->>SlottingService: optimalClass = determineOptimalClass(distance)

        alt Class Changed
            SlottingService->>ConfigService: updateSlottingClass(locationId, optimalClass, admin, "Auto-optimization")
            ConfigService->>Repo: save(location)
            Note over SlottingService: Updated count++
        end
    end

    SlottingService-->>Controller: {updated: 25}
    Controller-->>Admin: 200 OK<br/>{message: "Updated 25 locations"}
```

## 4. Find Optimal Location for Product

```mermaid
sequenceDiagram
    participant WMS as Warehouse System
    participant Controller as LocationController
    participant SlottingService
    participant Repo as LocationMasterRepository

    WMS->>Controller: GET /locations/optimal?<br/>zone=PICK&class=A&requiredQty=500
    Controller->>SlottingService: findOptimalLocation(warehouseId, "PICK", A, 500)

    SlottingService->>Repo: findOptimalSlottingLocations(warehouseId, "PICK", A)
    Repo-->>SlottingService: List<LocationMaster><br/>(sorted by distance, pick path)

    SlottingService->>SlottingService: Filter by capacity

    loop For Each Candidate
        SlottingService->>SlottingService: availableQty = capacity.getAvailableQuantity()
        alt availableQty >= 500
            SlottingService-->>Controller: LocationMaster (first match)
            Controller-->>WMS: 200 OK<br/>{locationId, availableCapacity}
        end
    end

    alt No Suitable Location
        SlottingService-->>Controller: Optional.empty()
        Controller-->>WMS: 404 Not Found<br/>{message: "No location available"}
    end
```

## 5. Block and Unblock Location

```mermaid
sequenceDiagram
    participant Supervisor
    participant Controller as LocationController
    participant ConfigService as LocationConfigurationService
    participant Repo as LocationMasterRepository
    participant EventPub as LocationEventPublisher
    participant PTS as Physical Tracking Service

    Note over Supervisor,PTS: Block Location

    Supervisor->>Controller: POST /locations/{id}/block<br/>{reason: "Damaged rack - safety hazard"}
    Controller->>ConfigService: blockLocation(locationId, reason, supervisor)

    ConfigService->>Repo: findById(locationId)
    Repo-->>ConfigService: LocationMaster

    ConfigService->>ConfigService: location.block(reason)
    Note over ConfigService: status = BLOCKED<br/>attributes["block_reason"] = reason

    ConfigService->>Repo: save(location)
    ConfigService->>EventPub: publishLocationStatusChanged(event)
    EventPub->>PTS: LocationBlockedEvent
    PTS->>PTS: Block in location state

    ConfigService-->>Controller: LocationMaster
    Controller-->>Supervisor: 200 OK

    Note over Supervisor,PTS: Unblock After Repair

    Supervisor->>Controller: POST /locations/{id}/unblock
    Controller->>ConfigService: unblockLocation(locationId, supervisor)

    ConfigService->>Repo: findById(locationId)
    Repo-->>ConfigService: LocationMaster {status: BLOCKED}

    ConfigService->>ConfigService: location.unblock()
    Note over ConfigService: status = ACTIVE<br/>blockReason removed

    ConfigService->>Repo: save(location)
    ConfigService->>EventPub: publishLocationStatusChanged(event)
    EventPub->>PTS: LocationUnblockedEvent

    ConfigService-->>Controller: LocationMaster
    Controller-->>Supervisor: 200 OK
```

## 6. Get Optimized Pick Path

```mermaid
sequenceDiagram
    participant WPS as Wave Planning Service
    participant Controller as LocationController
    participant SlottingService
    participant Repo as LocationMasterRepository

    WPS->>Controller: GET /locations/warehouse/{id}/pick-path?zone=PICK
    Controller->>SlottingService: getOptimizedPickPath(warehouseId, "PICK")

    SlottingService->>Repo: findPickPathLocations(warehouseId, "PICK")
    Repo-->>SlottingService: List<LocationMaster>

    SlottingService->>SlottingService: Sort by:<br/>1. Slotting priority (FAST_MOVER first)<br/>2. Distance from dock<br/>3. Aisle<br/>4. Bay<br/>5. Level

    Note over SlottingService: Serpentine (S-shape) pattern:<br/>Aisle A: forward<br/>Aisle B: backward<br/>Aisle C: forward

    SlottingService-->>Controller: Ordered List<LocationMaster>
    Controller-->>WPS: 200 OK<br/>[<br/>  {locationId: "A-01-1", sequence: 1},<br/>  {locationId: "A-02-1", sequence: 2},<br/>  {locationId: "B-03-1", sequence: 3},<br/>  {locationId: "B-02-1", sequence: 4}<br/>]
```

## 7. Identify Golden Zone

```mermaid
sequenceDiagram
    participant Analyst
    participant Controller as LocationController
    participant SlottingService
    participant Repo as LocationMasterRepository

    Analyst->>Controller: GET /locations/warehouse/{id}/golden-zone?zone=PICK
    Controller->>SlottingService: identifyGoldenZone(warehouseId, "PICK")

    SlottingService->>Repo: findByWarehouseIdAndZoneAndStatus(warehouseId, "PICK", ACTIVE)
    Repo-->>SlottingService: List<LocationMaster> (150 locations)

    SlottingService->>SlottingService: Filter storage locations only<br/>(BIN, SHELF)

    SlottingService->>SlottingService: Sort by:<br/>1. Distance from dock (ASC)<br/>2. Pick path sequence

    SlottingService->>SlottingService: Calculate golden zone size:<br/>size = 150 * 0.20 = 30 locations

    SlottingService->>SlottingService: Take top 30 locations

    SlottingService-->>Controller: List<LocationMaster> (30 locations)
    Controller-->>Analyst: 200 OK<br/>{<br/>  goldenZoneLocations: [...],<br/>  size: 30,<br/>  percentOfTotal: 20.0<br/>}
```

## 8. Slotting Recommendations

```mermaid
sequenceDiagram
    participant Admin
    participant Controller as LocationController
    participant SlottingService
    participant Repo as LocationMasterRepository

    Admin->>Controller: GET /locations/warehouse/{id}/slotting/recommendations
    Controller->>SlottingService: getSlottingRecommendations(warehouseId)

    SlottingService->>Repo: findActiveStorageLocations(warehouseId)
    Repo-->>SlottingService: List<LocationMaster>

    loop For Each Location
        SlottingService->>SlottingService: recommendedClass = determineOptimalClass(distanceFromDock)
        SlottingService->>SlottingService: confidenceScore = calculateConfidence(location, recommendedClass)

        alt Current Class != Recommended
            SlottingService->>SlottingService: Add to recommendations<br/>{<br/>  locationId,<br/>  currentClass,<br/>  recommendedClass,<br/>  confidence,<br/>  reasoning<br/>}
        end
    end

    SlottingService-->>Controller: Map<String, SlottingRecommendation>
    Controller-->>Admin: 200 OK<br/>[<br/>  {<br/>    locationId: "A-01-1",<br/>    current: "B",<br/>    recommended: "A",<br/>    confidence: 85,<br/>    reasoning: "20 units from dock..."<br/>  },<br/>  ...<br/>]
```

## 9. Balance Slotting Across Zones

```mermaid
sequenceDiagram
    participant System
    participant SlottingService
    participant ConfigService as LocationConfigurationService
    participant Repo as LocationMasterRepository

    Note over System,Repo: Scheduled Daily at 2 AM

    System->>SlottingService: balanceSlotting(warehouseId, "SYSTEM")

    SlottingService->>Repo: findZones(warehouseId)
    Repo-->>SlottingService: List<LocationMaster> (zones)

    loop For Each Zone
        SlottingService->>Repo: findByWarehouseIdAndZone(warehouseId, zone)
        Repo-->>SlottingService: List<LocationMaster>

        SlottingService->>SlottingService: Calculate distribution:<br/>- Fast movers: 5%<br/>- A-class: 15%<br/>- B-class: 30%<br/>- C-class: 40%<br/>- Slow movers: 10%

        SlottingService->>SlottingService: Check if follows 80-20 rule

        alt Needs Rebalancing
            SlottingService->>SlottingService: optimizeZoneSlotting(warehouseId, zone, "SYSTEM")
            Note over SlottingService: Rebalanced++
        end
    end

    SlottingService-->>System: SlottingBalanceReport<br/>{<br/>  warehouseId,<br/>  distributionByZone,<br/>  locationsRebalanced: 45<br/>}
```

## 10. Query Location Hierarchy

```mermaid
sequenceDiagram
    participant Client
    participant Controller as LocationController
    participant ConfigService as LocationConfigurationService
    participant Repo as LocationMasterRepository

    Note over Client,Repo: Get Location with Children

    Client->>Controller: GET /locations/{locationId}
    Controller->>ConfigService: getLocation(locationId)

    ConfigService->>Repo: findById(locationId)
    Repo-->>ConfigService: LocationMaster

    ConfigService-->>Controller: LocationMaster
    Controller-->>Client: 200 OK

    Note over Client,Repo: Get All Children

    Client->>Controller: GET /locations/{locationId}/children
    Controller->>ConfigService: getChildLocations(locationId)

    ConfigService->>Repo: findByParentLocationId(locationId)
    Repo-->>ConfigService: List<LocationMaster>

    ConfigService-->>Controller: List<LocationMaster>
    Controller-->>Client: 200 OK<br/>[<br/>  {child1},<br/>  {child2},<br/>  ...<br/>]

    Note over Client,Repo: Navigate Full Tree

    loop For Each Child
        Client->>Controller: GET /locations/{childId}/children
        Controller->>ConfigService: getChildLocations(childId)
        ConfigService->>Repo: findByParentLocationId(childId)
        ConfigService-->>Controller: List<LocationMaster>
        Controller-->>Client: 200 OK
    end
```

## Error Scenarios

### Invalid Hierarchy
```mermaid
sequenceDiagram
    participant Admin
    participant Service as LocationConfigurationService
    participant Repo as LocationMasterRepository

    Admin->>Service: createLocation(type: BIN, parentId: null)
    Service->>Service: location.validateHierarchy()
    Note over Service: hierarchyLevel = 4 (BIN)<br/>parentLocationId = null<br/>ERROR: Non-root must have parent
    Service-->>Admin: IllegalStateException<br/>"Non-root location must have a parent"
```

### Cannot Deactivate with Inventory
```mermaid
sequenceDiagram
    participant Admin
    participant Service as LocationConfigurationService
    participant PTS as Physical Tracking Service

    Admin->>Service: deactivateLocation(locationId)
    Service->>PTS: getLocationState(locationId)
    PTS-->>Service: LocationState {currentQuantity: 500}

    alt Has Inventory
        Service-->>Admin: IllegalStateException<br/>"Cannot deactivate location with inventory"
    end
```
