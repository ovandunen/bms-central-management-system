#!/usr/bin/env bash
# Build Senegal+Gambia GraphHopper graph for fleet tablet offline routing.
# Requires: wget, java 21+, graphhopper-web-10.2.jar (or local Maven build).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="${ROOT}/build/routing-graph"
PBF_URL="https://download.geofabrik.de/africa/senegal-and-gambia-latest.osm.pbf"
PBF_FILE="${OUT_DIR}/senegal-and-gambia-latest.osm.pbf"
GRAPH_DIR="${OUT_DIR}/senegal-and-gambia-gh"
VERSION_FILE="${OUT_DIR}/senegal-and-gambia-graph-version.txt"
ZIP_FILE="${OUT_DIR}/senegal-and-gambia-gh.zip"

mkdir -p "${OUT_DIR}"
if [[ ! -f "${PBF_FILE}" ]]; then
  wget -O "${PBF_FILE}" "${PBF_URL}"
fi

# TODO: wire GraphHopper 10.2 import CLI when CI image is pinned.
# java -Xmx4g -jar graphhopper-web-10.2.jar import config-routing.yml

echo "$(date -u +%Y-%m-%d)" > "${VERSION_FILE}"
echo "Graph build placeholder — run GraphHopper import before publishing ${ZIP_FILE}"
