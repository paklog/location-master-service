---
layout: default
title: Home
---

# Location Master Service Documentation

Warehouse location and zone management service with hierarchical structure, capacity management, and intelligent slotting optimization.

## Overview

The Location Master Service manages the warehouse physical infrastructure including locations, zones, aisles, bins, and their hierarchical relationships. It provides sophisticated slotting optimization to place products in optimal locations based on velocity, size, and pick path efficiency. The service enforces capacity constraints, maintains location restrictions, and supports dynamic location configuration.

## Quick Links

### Getting Started
- [README](README.md) - Quick start guide and overview
- [Architecture Overview](architecture.md) - System architecture description

### Architecture & Design
- [Domain Model](DOMAIN-MODEL.md) - Complete domain model with class diagrams
- [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) - Process flows and interactions
- [OpenAPI Specification](openapi.yaml) - REST API documentation
- [AsyncAPI Specification](asyncapi.yaml) - Event documentation

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **MongoDB** - Document database for location data
- **Apache Kafka** - Event streaming platform
- **CloudEvents 2.5.0** - Event standard
- **Maven** - Build tool

## Key Features

- **Hierarchical Location Structure** - Warehouse → Zone → Aisle → Bay → Bin
- **Capacity Management** - Quantity, weight, volume, and container limits
- **Intelligent Slotting** - ABC classification and golden zone optimization
- **Location Restrictions** - Hazmat, temperature, SKU mixing controls
- **Pick Path Sequencing** - Optimized pick path ordering
- **3D Coordinates** - Precise location positioning (x, y, z)
- **Location Lifecycle** - Active, blocked, reserved, decommissioned states
- **Custom Attributes** - Extensible metadata support

## Domain Model

### Aggregates
- **LocationMaster** - Complete location configuration and state

### Entities
- **Dimensions** - Physical size specifications
- **Capacity** - Capacity constraints and limits
- **Restrictions** - Access and product restrictions

### Value Objects
- **LocationType** - Location hierarchy classification
- **LocationStatus** - Operational status
- **SlottingClass** - Velocity-based classification (A, B, C, Fast Mover, Slow Mover)

### Location Hierarchy

```
WAREHOUSE
└── ZONE
    └── AISLE
        └── BAY
            └── BIN/SHELF/RACK
```

### Location Lifecycle

```
ACTIVE <-> BLOCKED
  ↓         ↓
INACTIVE  RESERVED
  ↓
UNDER_MAINTENANCE
  ↓
DECOMMISSIONED
```

## Domain Events

### Published Events
- **LocationCreated** - New location created
- **LocationActivated** - Location activated
- **LocationDeactivated** - Location deactivated
- **LocationBlocked** - Location blocked
- **LocationUnblocked** - Location unblocked
- **LocationReserved** - Location reserved
- **LocationDecommissioned** - Location decommissioned
- **LocationCapacityChanged** - Capacity limits updated
- **LocationRestrictionsChanged** - Restrictions modified
- **LocationSlottingClassChanged** - Slotting classification updated
- **PickPathSequenceUpdated** - Pick path order changed

### Consumed Events
- **InventoryMovement** - Update location utilization
- **LocationStateChanged** - Sync with physical tracking
- **SlottingRecommendation** - Apply slotting suggestions

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters for clean separation
- **Domain-Driven Design** - Rich domain model with business logic
- **Event-Driven Architecture** - Location change notifications
- **Composite Pattern** - Hierarchical location structure
- **Strategy Pattern** - Multiple slotting strategies

## API Endpoints

### Location Management
- `POST /locations` - Create location
- `GET /locations/{locationId}` - Get location details
- `PUT /locations/{locationId}` - Update location
- `DELETE /locations/{locationId}` - Decommission location
- `GET /locations` - List locations with filtering

### Location Configuration
- `PUT /locations/{locationId}/dimensions` - Configure dimensions
- `PUT /locations/{locationId}/capacity` - Configure capacity
- `PUT /locations/{locationId}/restrictions` - Configure restrictions
- `PUT /locations/{locationId}/slotting-class` - Set slotting class
- `PUT /locations/{locationId}/coordinates` - Set 3D coordinates

### Location Operations
- `POST /locations/{locationId}/activate` - Activate location
- `POST /locations/{locationId}/deactivate` - Deactivate location
- `POST /locations/{locationId}/block` - Block location
- `POST /locations/{locationId}/unblock` - Unblock location
- `POST /locations/{locationId}/reserve` - Reserve location

### Hierarchy Navigation
- `GET /locations/{locationId}/children` - Get child locations
- `GET /locations/{locationId}/parent` - Get parent location
- `GET /locations/warehouse/{warehouseId}` - Get all warehouse locations
- `GET /locations/zone/{zone}` - Get locations by zone
- `GET /locations/aisle/{aisle}` - Get locations by aisle

### Slotting Operations
- `POST /slotting/optimize` - Run slotting optimization
- `GET /slotting/recommendations` - Get slotting suggestions
- `GET /slotting/golden-zone` - Identify golden zone locations
- `POST /slotting/balance` - Balance slotting distribution
- `GET /slotting/pick-path` - Get optimized pick path

## Location Types

### WAREHOUSE
Top-level container representing entire warehouse facility.
- Hierarchy depth: 0
- Cannot store inventory directly
- Can have child zones

### ZONE
Major warehouse area (receiving, storage, picking, shipping).
- Hierarchy depth: 1
- Cannot store inventory directly
- Can have child aisles or locations

### AISLE
Row of storage locations.
- Hierarchy depth: 2
- Cannot store inventory directly
- Can have child bays

### BAY
Section within aisle.
- Hierarchy depth: 3
- Cannot store inventory directly
- Can have child bins/shelves/racks

### BIN / SHELF / RACK
Actual storage location for inventory.
- Hierarchy depth: 4
- Can store inventory
- Cannot have children

### STAGING / DOCK / FLOOR
Special purpose locations for temporary storage or operations.
- Variable hierarchy depth
- Can store inventory
- Used for specific workflows

## Location Status

### ACTIVE
Location is operational and can accept inventory.

### INACTIVE
Location is temporarily unavailable but not blocked.

### BLOCKED
Location is blocked due to damage, maintenance, or restrictions.

### RESERVED
Location is reserved for specific use or product.

### FULL
Location is at capacity and cannot accept more inventory.

### UNDER_MAINTENANCE
Location is being repaired or modified.

### DECOMMISSIONED
Location is permanently removed from service.

## Slotting Classification

### ABC Analysis

Locations are classified based on product velocity and pick frequency:

#### FAST_MOVER (A Class)
- Top 20% of products by volume
- Located nearest to dock/packing
- Golden zone placement (waist to shoulder height)
- Distance from dock: 0-50 feet
- Pick frequency: >100 picks/week

#### A Class
- Next 15% of products by volume
- Near golden zone
- Distance from dock: 50-100 feet
- Pick frequency: 50-100 picks/week

#### B Class
- Middle 30% of products by volume
- Standard pick locations
- Distance from dock: 100-200 feet
- Pick frequency: 20-50 picks/week

#### C Class
- Next 25% of products by volume
- Remote storage areas
- Distance from dock: 200-300 feet
- Pick frequency: 5-20 picks/week

#### SLOW_MOVER
- Bottom 10% of products by volume
- Furthest from dock
- Upper or lower shelves acceptable
- Distance from dock: >300 feet
- Pick frequency: <5 picks/week

### Golden Zone
- Optimal pick height: 30-60 inches from floor
- Nearest to dock and packing areas
- Reserved for fast-moving products
- Maximum ergonomic efficiency

## Capacity Management

### Capacity Types

#### Quantity Capacity
Maximum number of individual items or units.

#### Weight Capacity
Maximum total weight in pounds or kilograms.

#### Volume Capacity
Maximum total volume in cubic feet or cubic meters.

#### Pallet Capacity
Maximum number of pallets.

#### License Plate Capacity
Maximum number of containers (license plates).

### Capacity Enforcement
- Prevent putaway to over-capacity locations
- Alert on approaching capacity (configurable threshold)
- Automatic status change when full
- Support for temporary capacity overrides

## Location Restrictions

### Product Restrictions

#### Hazmat Allowed
Whether location can store hazardous materials.

#### Temperature Controlled
Whether location requires temperature control with min/max settings.

#### Mixed SKU Allowed
Whether multiple SKUs can coexist in same location.

#### Mixed Lot Allowed
Whether multiple lot numbers can coexist in same location.

#### Product Categories
Whitelist or blacklist of allowed product categories.

#### Max Items Per SKU
Maximum quantity of single SKU in location.

### Restriction Validation
- Enforced during putaway planning
- Checked before inventory moves
- Validated for slotting recommendations
- Override capability for authorized users

## Pick Path Optimization

### Pick Path Sequence
Locations assigned sequential numbers for optimal pick path:
- Lower numbers = earlier in pick path
- Optimized to minimize travel distance
- Considers aisle serpentine pattern
- Updated during slotting optimization

### Pick Path Strategies

#### Serpentine Pattern
S-shaped path through aisles - forward down one aisle, backward up next.

#### Zone-Based Path
Complete one zone before moving to next.

#### Proximity-Based Path
Shortest distance from starting point.

#### Velocity-Based Path
High-velocity items first for early release to packing.

## Slotting Optimization

### Slotting Service Features

#### Find Optimal Location
Recommends best location for product based on:
- Product velocity and pick frequency
- Product dimensions and weight
- Current location utilization
- Pick path efficiency
- Proximity to dock/packing

#### Optimize Zone Slotting
Re-slots entire zone for optimal efficiency:
- Place fast movers in golden zone
- Balance capacity utilization
- Minimize pick path distance
- Consider product compatibility

#### Identify Golden Zone
Identifies prime picking locations:
- Near dock and packing
- Optimal height (30-60 inches)
- Easy access
- High throughput capacity

#### Balance Slotting
Distributes products across zones:
- Prevent zone overload
- Balance picker workload
- Optimize space utilization
- Maintain velocity stratification

## Integration Points

### Consumes Events From
- Physical Tracking (location utilization)
- Inventory (product dimensions, velocity)
- Pick Execution (pick frequency data)

### Publishes Events To
- Physical Tracking (location configuration changes)
- Task Execution (location availability)
- Slotting Analytics (optimization results)

## Performance Considerations

### Database Optimization
- MongoDB indexes on locationId, warehouseId, zone, type, status
- Compound index on warehouseId + type + status
- Geospatial index on coordinates
- Index on parentLocationId for hierarchy queries

### Caching Strategy
- Location details cached for 15 minutes
- Hierarchy relationships cached
- Slotting classifications cached for 1 hour
- Capacity limits cached

### Slotting Performance
- Batch slotting optimization runs off-peak
- Incremental updates for single locations
- Pre-calculated golden zones
- Cached pick path sequences

## Business Rules

1. **Location Creation Rules**
   - Location ID must be unique
   - Parent location must exist (except warehouse)
   - Hierarchy level must match type
   - Cannot create children for leaf nodes

2. **Capacity Rules**
   - All capacity values must be positive
   - Cannot reduce capacity below current usage
   - Weight and volume must have valid units
   - Capacity overrides require authorization

3. **Slotting Rules**
   - Fast movers must be in golden zone when possible
   - Hazmat products require hazmat-approved locations
   - Temperature-sensitive products need temp-controlled locations
   - Oversized products assigned to appropriate bin sizes

4. **Status Transition Rules**
   - Cannot activate location with inventory if restrictions violated
   - Cannot decommission location with inventory
   - Blocked locations cannot accept new inventory
   - Reserved locations require matching reservation ID

## Getting Started

1. Review the [README](README.md) for quick start instructions
2. Understand the [Architecture](architecture.md) and design patterns
3. Explore the [Domain Model](DOMAIN-MODEL.md) to understand business concepts
4. Study the [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) for process flows
5. Reference the [OpenAPI](openapi.yaml) and [AsyncAPI](asyncapi.yaml) specifications

## Configuration

Key configuration properties:
- `location.cache.details-ttl` - Location cache TTL (default: 15m)
- `location.cache.hierarchy-ttl` - Hierarchy cache TTL (default: 1h)
- `location.capacity.alert-threshold` - Capacity alert % (default: 90)
- `slotting.golden-zone.height-min` - Golden zone min height (default: 30 inches)
- `slotting.golden-zone.height-max` - Golden zone max height (default: 60 inches)
- `slotting.golden-zone.distance-max` - Golden zone max distance (default: 50 feet)
- `slotting.fast-mover.threshold` - Fast mover pick frequency (default: 100/week)
- `slotting.optimization.batch-size` - Optimization batch size (default: 1000)

## Contributing

For contribution guidelines, please refer to the main README in the project root.

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Browse the guides in the navigation menu
- **Service Owner**: WMS Team
- **Slack**: #wms-location-master
