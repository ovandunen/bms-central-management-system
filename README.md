# Solar Station CSMS

Central System Management System (CSMS) for solar-powered EV charging stations. Built with **Quarkus 3.x** and **Java 21**, following Domain-Driven Design with five bounded contexts that communicate only via CDI events.

## Architecture

| Context | Responsibility |
|---------|----------------|
| `station` | Charging station lifecycle and OCPP-derived status |
| `location` | PostGIS geospatial storage and nearby-station queries |
| `driver` | MQTT driver requests → `NearbyStationQuery` events |
| `swap` | Optimum battery swap moment calculation and MQTT push |
| `notification` | OCPP 1.6 WebSocket ACL (anti-corruption layer) |

## Swap MQTT topics

The `swap` bounded context publishes two **intentionally distinct** MQTT signals. They serve different consumers and urgency levels — neither replaces or gates the other.

| Purpose | Topic | When published |
|---------|-------|----------------|
| **Proactive / predictive** — optimal swap timing from monitoring, prediction, and calculation | `swap/optimization/{vehicleId}` | On every swap decision emitted by `OptimizeSwapMomentUseCase`, **independent of SoC** |
| **Reactive / necessity** — driver must swap before reaching a station with a charged pack | `vehicles/{vehicleId}/swap/recommendation` | Only when `isLowBattery()` is true: SoC **< 20%** and station inventory **> 0** |

Both payloads include station coordinates (`latitude`, `longitude`) from `station_location` when available.

## Prerequisites

- Java 21+
- Docker & Docker Compose (for local infra)

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
- MQTT inbound: `drivers/{driverId}/request/location`
- MQTT outbound: `stations/available/{driverId}` (dynamic, via `MqttClient`)

### 3. Run tests

```bash
./mvnw test
```

PostGIS spatial tests are tagged `postgis` and excluded from the default test run. Execute them with Docker available:

```bash
./mvnw test -Ppostgis
# equivalent:
./mvnw test -DexcludedGroups= -Dgroups=postgis
```

### E2E black-box tests (Docker Compose)

Requires Docker. Host ports: Postgres **5433**, MQTT **1884**, CSMS **8081**, WireMock KI **8090**.

```bash
make e2e-build    # ./mvnw package + build solar-csms-app image
make e2e-up       # start postgres-e2e, mosquitto-e2e, wiremock-ki-e2e, solar-csms-app
make e2e-test     # pytest smoke test: verifies Quarkus app boots and KI WireMock stub is reachable
make e2e-down
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

## Example MQTT driver request

Topic: `drivers/alice/request/location`

Payload:

```json
{
  "driverId": "alice",
  "latitude": 52.52,
  "longitude": 13.405,
  "radiusMeters": 10000
}
```

Response is published to `stations/available/alice` as a JSON array of station locations.

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
