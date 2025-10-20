# Location Master Service

Warehouse location configuration, hierarchical management, and intelligent slotting optimization for maximizing storage efficiency and pick productivity.

## Overview

The Location Master Service provides comprehensive warehouse location master data management and slotting optimization. This bounded context defines the complete location hierarchy from warehouse to bin level, configures location attributes including dimensions and capacity, manages location restrictions and equipment requirements, implements ABC velocity-based slotting, provides intelligent location recommendations based on product characteristics, and maintains the golden source of location configuration data. The service enables data-driven slotting decisions that optimize both storage density and pick efficiency.

## Domain-Driven Design

### Bounded Context
**Location Master Data & Slotting Optimization** - Manages warehouse location configuration, capacity planning, and intelligent slotting recommendations.

### Core Domain Model

#### Aggregates
- **LocationMaster** - Root aggregate representing a warehouse location

#### Entities
- **Capacity** - Location capacity configuration
- **Dimensions** - Physical dimensions of location
- **Restrictions** - Location usage restrictions

#### Value Objects
- **LocationType** - Location hierarchy type (WAREHOUSE, ZONE, AISLE, BAY, LEVEL, BIN)
- **LocationStatus** - Location availability status (ACTIVE, INACTIVE, BLOCKED, FULL, RESERVED, DECOMMISSIONED)
- **SlottingClass** - Velocity-based classification (FAST_PICK, MEDIUM_PICK, SLOW_PICK, RESERVE, BULK, MIXED)
- **LocationId** - Unique hierarchical identifier

#### Domain Events
- **LocationCreatedEvent** - New location created
- **LocationConfiguredEvent** - Location configuration updated
- **LocationCapacityChangedEvent** - Capacity modified
- **LocationSlottingChangedEvent** - Slotting class reassigned
- **LocationActivatedEvent** - Location activated
- **LocationDeactivatedEvent** - Location deactivated
- **LocationBlockedEvent** - Location blocked from use

### Ubiquitous Language
- **Location Master**: Configuration and attributes of warehouse location
- **Location Hierarchy**: Tree structure from warehouse to bin level
- **Slotting**: Assignment of products to optimal storage locations
- **ABC Classification**: Velocity-based product grouping (Activity-Based Classification)
- **Pick Face**: Forward pick location for high-velocity items
- **Reserve Storage**: Bulk storage for inventory replenishment
- **Slotting Optimization**: Algorithmic assignment to minimize travel and maximize density
- **Pick Path Sequence**: Optimal order for visiting locations
- **Distance from Dock**: Travel distance consideration for slotting

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/wms/location/
├── domain/                           # Core business logic
│   ├── aggregate/                   # Aggregates
│   │   └── LocationMaster.java      # Location aggregate root
│   ├── entity/                      # Entities
│   │   ├── Capacity.java           # Capacity configuration
│   │   ├── Dimensions.java         # Physical dimensions
│   │   └── Restrictions.java        # Usage restrictions
│   ├── valueobject/                 # Value objects
│   │   ├── LocationType.java
│   │   ├── LocationStatus.java
│   │   ├── SlottingClass.java
│   │   └── LocationId.java
│   ├── repository/                  # Repository interfaces
│   │   └── LocationMasterRepository.java
│   ├── service/                     # Domain services
│   │   ├── SlottingService.java
│   │   └── LocationOptimizer.java
│   └── event/                       # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   │   ├── LocationConfigurationService.java
│   │   └── SlottingService.java
│   ├── command/                     # Commands
│   │   ├── CreateLocationCommand.java
│   │   ├── ConfigureCapacityCommand.java
│   │   └── UpdateSlottingCommand.java
│   └── query/                       # Queries
└── adapter/                          # External adapters
    ├── rest/                        # REST controllers
    │   └── LocationController.java
    ├── persistence/                 # JPA repositories
    └── events/                      # Event publishers/consumers
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain models for locations
- **Composite Pattern** - Hierarchical location structure
- **Strategy Pattern** - Pluggable slotting algorithms
- **Template Method Pattern** - Location validation workflow
- **Event-Driven Architecture** - Location change notifications
- **Repository Pattern** - Data access abstraction
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **PostgreSQL** - Relational database for location hierarchy
- **Spring Data JPA** - ORM and data access layer
- **Flyway** - Database schema migrations

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents 2.5.0** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Micrometer Tracing** - Distributed tracing
- **Loki Logback Appender** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Master data management patterns

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Event-driven configuration changes
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event handling

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/location-master-service.git
   cd location-master-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d postgresql kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f location-master-service

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/v3/api-docs

### Key Endpoints

#### Location Management
- `POST /locations` - Create new location
- `GET /locations/{locationId}` - Get location details
- `PUT /locations/{locationId}/capacity` - Configure capacity
- `PUT /locations/{locationId}/dimensions` - Set dimensions
- `PUT /locations/{locationId}/restrictions` - Update restrictions
- `POST /locations/{locationId}/activate` - Activate location
- `POST /locations/{locationId}/deactivate` - Deactivate location
- `POST /locations/{locationId}/block` - Block location
- `POST /locations/{locationId}/reserve` - Reserve location

#### Slotting Management
- `PUT /locations/{locationId}/slotting` - Update slotting class
- `POST /slotting/optimize` - Run slotting optimization
- `POST /slotting/recommendations` - Get slotting recommendations
- `GET /locations/slotting-class/{class}` - Find by slotting class

#### Hierarchy Queries
- `GET /locations/hierarchy/{warehouseId}` - Get location hierarchy
- `GET /locations/children/{parentId}` - Get child locations
- `GET /locations/zone/{zone}` - Query by zone
- `GET /locations/available` - Find available locations

## Slotting Optimization Features

### ABC Velocity Classification

Automatic classification based on activity:
- **A Items (Fast Movers)**: Top 20% by volume, 80% of picks
- **B Items (Medium Movers)**: Middle 30% by volume, 15% of picks
- **C Items (Slow Movers)**: Bottom 50% by volume, 5% of picks

### Slotting Classes

- **FAST_PICK**: High-velocity items in prime pick positions
- **MEDIUM_PICK**: Moderate velocity in standard pick locations
- **SLOW_PICK**: Low velocity in less accessible positions
- **RESERVE**: Bulk storage for replenishment
- **BULK**: Large quantity pallet storage
- **MIXED**: Multi-SKU shared locations

### Slotting Optimization Algorithm

```
1. Analyze historical pick velocity data
2. Classify items by ABC methodology
3. Calculate optimal location assignments
4. Minimize: (velocity × distance from dock)
5. Maximize: space utilization
6. Apply constraints: dimensions, weight, restrictions
7. Generate slotting recommendations
```

### Location Selection Criteria

- **Distance from shipping**: Minimize travel for high-velocity items
- **Pick path optimization**: Sequential location numbering
- **Ergonomic placement**: Appropriate height for item weight
- **Dimensional fit**: Item size vs location capacity
- **Equipment requirements**: Specialized handling needs

## Location Hierarchy

```
WAREHOUSE (Level 0)
└── ZONE (Level 1) - Functional areas (RECEIVING, PICKING, PACKING, SHIPPING, RESERVE)
    └── AISLE (Level 2) - Physical aisles
        └── BAY (Level 3) - Sections within aisle
            └── LEVEL (Level 4) - Vertical height
                └── BIN (Level 5) - Individual storage position
```

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/location_master
    username: paklog
    password: paklog
  jpa:
    hibernate:
      ddl-auto: validate
  kafka:
    bootstrap-servers: localhost:9092

location-master:
  slotting:
    abc-thresholds:
      a-percentage: 20
      b-percentage: 30
      c-percentage: 50
    optimization:
      enable-auto-slotting: true
      reoptimize-interval-days: 30
```

## Event Integration

### Published Events
- `com.paklog.wms.location.created.v1`
- `com.paklog.wms.location.configured.v1`
- `com.paklog.wms.location.capacity.changed.v1`
- `com.paklog.wms.location.slotting.changed.v1`
- `com.paklog.wms.location.activated.v1`
- `com.paklog.wms.location.deactivated.v1`
- `com.paklog.wms.location.blocked.v1`

### Consumed Events
- `com.paklog.inventory.velocity.updated.v1` - Update slotting recommendations
- `com.paklog.wes.tracking.location.full.v1` - Mark location as full
- `com.paklog.wes.tracking.location.empty.v1` - Mark location as available

### Event Format
All events follow the CloudEvents specification v1.0 and are published asynchronously via Kafka.

## Monitoring

- **Health**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/metrics
- **Prometheus**: http://localhost:8081/actuator/prometheus
- **Info**: http://localhost:8081/actuator/info

### Key Metrics
- `locations.total` - Total locations configured
- `locations.active.count` - Currently active locations
- `locations.utilization.percentage` - Overall location usage
- `slotting.optimization.duration` - Time to complete slotting
- `slotting.recommendations.count` - Pending slotting changes

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Maintain location hierarchy integrity
4. Optimize slotting algorithms for efficiency
5. Validate location configurations
6. Write comprehensive tests including hierarchy tests
7. Document domain concepts using ubiquitous language
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
