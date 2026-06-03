# Solar Station CSMS

Central System Management System (CSMS) for solar-powered EV charging stations. Built with **Quarkus 3.x** and **Java 21**, following Domain-Driven Design with four bounded contexts that communicate only via CDI events.

## Architecture

| Context | Responsibility |
|---------|----------------|
| `station` | Charging station lifecycle and OCPP-derived status |
| `location` | PostGIS geospatial storage and nearby-station queries |
| `vehicle` | MQTT vehicle location requests → `NearbyStationQuery` events |
| `notification` | OCPP 1.6 WebSocket ACL (anti-corruption layer) |

## Prerequisites

- **Docker** & Docker Compose (tests, infra, and full stack)
- Java 21+ (only if you run `./mvnw` on the host instead of in Compose)

The repo includes a **Maven Wrapper** (`./mvnw`); a system Maven install is optional.

On **Java 22+**, the project configures **Woodstox** for StAX parsing (via `.mvn/jvm.config`) so Narayana JTA does not fail with `Provider com.bea.xml.stream.MXParserFactory not found`.

## Quick start

### 1. Start infrastructure

```bash
docker compose up -d postgres mosquitto
```

This starts **PostgreSQL + PostGIS** on port `5432` and **Mosquitto** on `1883` (MQTT) / `9001` (WebSocket).

### 2. Run the application (dev mode)

```bash
./mvnw quarkus:dev
```

Flyway applies `V1__init.sql` on startup. The CSMS listens on:

- HTTP/WebSocket: `ws://localhost:8080/ocpp/{chargePointId}` (OCPP 1.6 JSON)
- MQTT inbound: `vehicles/{vehicleId}/request/location` (BMS / VCU — not the mobile driver app)
- MQTT outbound: `stations/available/{vehicleId}` (via `station-location-response` + `MqttStationResponsePublisher`)

### 3. Run tests (automation / CI)

Uses **PostGIS** and **Mosquitto** (same as production). Recommended — runs Maven inside Compose against `postgres` and `mosquitto`:

```bash
docker compose run --rm test
```

On the host (with infra already up via Compose):

```bash
docker compose up -d postgres mosquitto
./mvnw test
```

### 4. Full stack with Docker

Build the application image, then start all services:

```bash
# Package the fast-jar, then build/run containers (Dockerfile copies target/quarkus-app/)
./mvnw package -DskipTests
docker compose up --build

# Or build the image via Quarkus in one step:
./mvnw package -DskipTests -Dquarkus.container-image.build=true
docker compose up
```

## Example MQTT vehicle location request

Topic: `vehicles/vehicle-001/request/location`

Payload:

```json
{
  "vehicleId": "vehicle-001",
  "latitude": 52.52,
  "longitude": 13.405,
  "radiusMeters": 10000
}
```

Response is published to `stations/available/vehicle-001` as a JSON array of station locations.

## Example OCPP StatusNotification

Send a CALL frame over WebSocket to `ws://localhost:8080/ocpp/CP001`:

```json
[2, "msg-1", "StatusNotification", {"connectorId": 1, "status": "Available"}]
```

## Configuration

See `src/main/resources/application.properties`. Key settings:

- `app.location.default-radius-m` — default search radius (metres)
- `quarkus.datasource.jdbc.url` — PostgreSQL connection
- `mp.messaging.connector.smallrye-mqtt.host` — MQTT broker host

## Out of scope (skeleton)

OCPP 2.x, OCPI roaming, payments, JWT auth, admin REST API, and frontend UI are not included.

## Register Charging Box via websocat
echo '[2,"boot-1","BootNotification",{"chargePointVendor":"Demo","chargePointModel":"WS-Sim-1","firmwareVersion":"1.0"}]' | websocat -n1 ws://localhost:8080/ocpp/CP-DEMO-001
echo '[2,"stat-1","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available"}]' | websocat -n1 ws://localhost:8080/ocpp/CP-DEMO-001
