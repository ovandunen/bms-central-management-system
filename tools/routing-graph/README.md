# Offline routing graph (Senegal + Gambia)

Fleet tablets download a pre-built GraphHopper graph for on-device turn-by-turn navigation.
This folder builds and publishes that artifact for EcoCar depot sync (Wi-Fi only).

## Source extract

- Geofabrik: `senegal-and-gambia-latest.osm.pbf`
- URL: https://download.geofabrik.de/africa/senegal-and-gambia-latest.osm.pbf

## Build (one-time / CI)

Requires GraphHopper **10.2** CLI (same major version as `graphhopper-core` in car-gui).

```bash
./tools/routing-graph/build-senegal-gambia-graph.sh
```

Outputs:

| Artifact | Purpose |
|----------|---------|
| `senegal-and-gambia-gh.zip` | ~400 MB graph bundle for tablets |
| `senegal-and-gambia-graph-version.txt` | Version manifest for depot sync |

Host both on CSMS static storage (S3/R2/NGINX). Tablets compare local
`files/routing/graph-version.txt` to the manifest; when stale **and on Wi-Fi**,
EcoCar triggers a background download.

## Out of scope here

- Cellular graph updates (explicitly disabled in EcoCar)
- HTTP `/navigate` routing fallback
- In-app graph import (tablet receives pre-built `.zip`)
