Based on the README already in your files:

## Manual OCPP Integration Test — Feed Charging Points to CSMS

### Prerequisites
```bash
# Install websocat (macOS)
brew install websocat

# Or via cargo
cargo install websocat
```

### Step 1 — Start infrastructure
```bash
docker compose up -d postgres mosquitto
./mvnw quarkus:dev
```

### Step 2 — Register a charging box (BootNotification)
```bash
echo '[2,"boot-1","BootNotification",{"chargePointVendor":"Demo","chargePointModel":"WS-Sim-1","firmwareVersion":"1.0"}]' \
  | websocat -n1 ws://localhost:8080/ocpp/CP-DEMO-001
```

### Step 3 — Set connector status to Available (StatusNotification)
```bash
echo '[2,"stat-1","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available"}]' \
  | websocat -n1 ws://localhost:8080/ocpp/CP-DEMO-001
```

### Step 4 — Register more free charging points
Repeat for as many stations as you need, incrementing the `chargePointId` and message IDs:

```bash
for ID in CP-DEMO-002 CP-DEMO-003 CP-DEMO-004; do
  echo "[2,\"boot-$ID\",\"BootNotification\",{\"chargePointVendor\":\"Demo\",\"chargePointModel\":\"WS-Sim-1\",\"firmwareVersion\":\"1.0\"}]" \
    | websocat -n1 ws://localhost:8080/ocpp/$ID
  echo "[2,\"stat-$ID\",\"StatusNotification\",{\"connectorId\":1,\"errorCode\":\"NoError\",\"status\":\"Available\"}]" \
    | websocat -n1 ws://localhost:8080/ocpp/$ID
done
```

### Step 5 — Verify via MQTT (trigger a BMS location request)
```bash
# Install mosquitto clients if needed
brew install mosquitto

# Subscribe to the response topic first (in a separate terminal)
mosquitto_sub -h localhost -p 1883 -t "stations/available/vehicle-001"

# Then publish a location request as the BMS Monitor would
mosquitto_pub -h localhost -p 1883 \
  -t "vehicles/vehicle-001/request/location" \
  -m '{"vehicleId":"vehicle-001","latitude":52.52,"longitude":13.405,"radiusMeters":10000}'
```

The `mosquitto_sub` terminal should receive a JSON array of the available stations you registered in Steps 2–4.

---

**What each OCPP message does in the CSMS:**

| Message | CSMS Effect |
|---|---|
| `BootNotification` | Registers the charge point in the `station` context |
| `StatusNotification` `Available` | Marks the connector as free; `location` context makes it queryable |
| `StatusNotification` `Charging` | Marks it occupied — excluded from nearby results |
| `StatusNotification` `Unavailable` | Marks it offline — excluded from nearby results |
