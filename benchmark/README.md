# Load / Pressure Benchmark

A [k6](https://k6.io/) load test for the reporting system's ClientService API.
It establishes a repeatable performance baseline and asserts SLO thresholds so
regressions surface in CI or local runs.

## Prerequisites

- `k6` installed (`brew install k6`)
- The full stack running and reachable at `http://localhost:8080`
  (ClientService + Excel + PDF services, LocalStack, per the repo README).
  The benchmark logs in with the demo `admin` / `password` account.

## Run

```bash
# default: realistic "mixed" dashboard traffic (read-heavy + some writes)
k6 run benchmark/report_benchmark.js

# individual scenarios
k6 run -e SCENARIO=read  -e VUS=25 -e DURATION=30s benchmark/report_benchmark.js
k6 run -e SCENARIO=async -e VUS=20 -e DURATION=30s benchmark/report_benchmark.js
k6 run -e SCENARIO=sync  -e VUS=5  -e DURATION=30s benchmark/report_benchmark.js
```

Env knobs: `BASE_URL`, `USERNAME`, `PASSWORD`, `SCENARIO`
(`mixed` | `read` | `async` | `sync`), `VUS`, `DURATION`. Each scenario ramps
up over 5s, holds `VUS` for `DURATION`, then ramps down.

## What each scenario measures

| Scenario | Endpoint | What it stresses |
| --- | --- | --- |
| `read` | `GET /report` | List/read throughput (DB read path) |
| `async` | `POST /report/async` | API ingestion — publishes to SNS and returns; the heavy generation happens off the request thread |
| `sync` | `POST /report/sync` | End-to-end generation — Excel + PDF built inline (parallelized), so this is the slow, resource-bound path. Concurrency is capped at 5 VUs. |
| `mixed` | `GET` + `POST /report/async` | Realistic traffic: ~80% reads, ~20% async writes |

Per-operation latency is tracked in custom trends
(`op_list_latency`, `op_async_submit_latency`, `op_sync_generate_latency`) so
each endpoint is measured independently of the others.

## Thresholds (SLOs)

The run **fails** (non-zero exit) if any of these are breached:

- `http_req_failed`: error rate `< 1%`
- `op_list_latency` p95 `< 500ms`
- `op_async_submit_latency` p95 `< 1000ms`
- `op_sync_generate_latency` p95 `< 4000ms`

## Baseline results

Captured on a laptop against the **LocalStack** dev stack (single-node, in-process
AWS emulation — so these reflect the dev environment, not production capacity).
Treat them as a relative baseline for catching regressions, not an absolute ceiling.

| Scenario | Config | Throughput | Latency p95 | Errors |
| --- | --- | --- | --- | --- |
| read | 25 VUs / 20s | ~68 req/s | list **10 ms** | 0% |
| async | 20 VUs / 20s | ~42 req/s (1,274 reports) | submit **231 ms** | 0% |
| sync | 5 VUs / 20s | ~6.7 req/s (206 reports) | generate **819 ms** | 0% |
| mixed | 20 VUs / 15s | ~48 req/s | list 49 ms · submit 27 ms | 0% |

Takeaways: the read and async-ingestion paths are cheap and scale well; the
sync path is the bottleneck because every request builds a real Excel workbook
and Jasper PDF and round-trips S3/DynamoDB — which is exactly why the system
offers the async API for volume.

## Notes

- Each `async`/`sync` iteration creates a real report (Excel on disk, PDF in
  S3 + DynamoDB, a row in H2). A long run accumulates data; clean up via the
  `DELETE /report/{id}` API or by resetting the LocalStack containers.
- Tune `VUS`/`DURATION` up to find the saturation point; the `sync` scenario
  will show rising p95 first.
