# E2E coverage matrix (Solar CSMS)

Black-box tests run via `make e2e-test` inside Docker Compose (`docker-compose.e2e.yml`).

## Automated coverage

| Spec case | Test file | Status |
|-----------|-----------|--------|
| **E2E-1** Pull: eligible swap stations | `test_location_pull.py` | Covered |
| **E2E-1** Pull: empty + message | `test_location_pull.py` | Covered |
| **E2E-1** Stale station telemetry excluded | `test_location_pull.py` (Station-E seed) | Covered |
| **E2E-1** `correlationId` in response | `test_location_pull.py` | Covered |
| **E2E-2** Push happy path | `test_swap_flow.py` | Covered |
| **E2E-2** Cooldown suppression | `test_swap_negatives.py` | Covered |
| **E2E-2** KI unavailable → rule fallback | `test_swap_negatives.py` | Covered |
| **E2E-2** KI low confidence suppressed | `test_swap_negatives.py` | Covered |
| **E2E-3** Feedback accepted/completed | `test_feedback_flow.py` | Covered |
| **E2E-3** Batch export to KI | `test_feedback_flow.py` | Covered |
| **Cross-cutting** Malformed MQTT | `test_mqtt_resilience.py` | Covered |
| Stack smoke | `test_smoke.py` | Covered |

## MQTT swap topics (implemented)

| Requirement | Status | Notes |
|-------------|--------|-------|
| `swap/optimization/{vehicleId}` topic | Implemented | Proactive/predictive signal — optimal swap timing from monitoring/prediction/calculation. Emitted on every swap decision from `OptimizeSwapMomentUseCase`, independent of SoC. |
| `vehicles/{vehicleId}/swap/recommendation` topic | Implemented | Reactive/necessity signal — SoC too low to reach a station with a fully charged battery available. Gated by `isLowBattery()` (SoC < 20% and station inventory > 0). |

## Not yet automated (out of CSMS black-box scope or pending)

| Spec item | Reason |
|-----------|--------|
| **BMS → AIDL → EcoCar dialog** (full Stufe 1 push) | CSMS E2E exercises MQTT in/out only; `test_swap_flow.py` validates telemetry → recommendation on the broker, not `OptimalSwapDialog` |
| VCU → BMS low-SoC detection | BMS app / car-gui responsibility |
| BMS map UI rendering | EcoCar GUI APK |
| OCPP DataTransfer swap ingest | UC-01 not implemented |
| Scheduled aggregation job (1 min) | Telemetry-triggered path used instead |
| 7-day historical training data | No seed pipeline |
| Policy REST admin API (UC-10) | Hardcoded thresholds in use case |
| Grafana dashboards (UC-4.2) | Ops tooling |
| Stale *vehicle* position rejection | Not implemented |
| `dismissed` / auto-`expired` feedback | Partial (states accepted in domain) |
| Parallel pull + push interference test | Implicit via suite order |

## Documentation notes

Clarify and document topic semantics — `swap/optimization/{vehicleId}` (predictive, SoC-independent) and `vehicles/{vehicleId}/swap/recommendation` (reactive, low-SoC-gated) are intentionally distinct business signals with different consumers, not variants of the same event. No code changes required.

## Environment variables

Configured in `docker-compose.e2e.yml` under `solar-csms-app` and `e2e-tester`. See root `README.md` E2E section.
