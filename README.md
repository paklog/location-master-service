# Location Master Service (WMS)

Warehouse location configuration and slotting management service.

## Responsibilities

- Location hierarchy definition (Warehouse → Zone → Aisle → Bay → Bin)
- Location attributes and configuration
- Capacity management
- Slotting rules and optimization
- ABC velocity classification
- Zone assignment rules

## Architecture

```
domain/
├── aggregate/      # LocationMaster, SlottingConfiguration
├── entity/         # Capacity, Dimensions, Restrictions
├── valueobject/    # LocationId, LocationType, SlottingClass
├── service/        # LocationService, SlottingOptimizationService
├── repository/     # LocationMasterRepository
└── event/          # LocationCreatedEvent, LocationConfiguredEvent

application/
├── command/        # CreateLocationCommand, UpdateCapacityCommand
├── query/          # SearchLocationsQuery, GetHierarchyQuery
└── handler/        # Event handlers

adapter/
├── rest/           # REST controllers
└── persistence/    # JPA repositories

infrastructure/
├── config/         # Spring configurations
├── messaging/      # Kafka publishers/consumers
└── events/         # Event publishing infrastructure
```

## Tech Stack

- Java 21
- Spring Boot 3.2.0
- PostgreSQL (location hierarchy data)
- Apache Kafka (event-driven integration)
- CloudEvents
- OpenAPI/Swagger

## Running the Service

```bash
mvn spring-boot:run
```

## API Documentation

Available at: http://localhost:8081/swagger-ui.html

## Events Published

- `LocationCreatedEvent` - When a new location is created
- `LocationCapacityChangedEvent` - When location capacity is updated
- `LocationSlottingChangedEvent` - When slotting class changes
- `LocationDeactivatedEvent` - When a location is deactivated

## Events Consumed

- `LocationCapacityExceededEvent` - From Physical Tracking Service
- `LocationBlockedEvent` - From Physical Tracking Service
