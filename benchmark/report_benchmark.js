import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend, Counter } from 'k6/metrics'

// Load / pressure test for the reporting system's ClientService API.
//
// Run one of the scenarios with:
//   k6 run benchmark/report_benchmark.js                       # mixed (default)
//   k6 run -e SCENARIO=read  -e VUS=25 -e DURATION=30s ...     # read throughput
//   k6 run -e SCENARIO=async -e VUS=25 -e DURATION=30s ...     # async ingestion
//   k6 run -e SCENARIO=sync  -e VUS=5  -e DURATION=30s ...     # end-to-end generation
//
// Env knobs: BASE_URL, USERNAME, PASSWORD, SCENARIO, VUS, DURATION.

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const USERNAME = __ENV.USERNAME || 'admin'
const PASSWORD = __ENV.PASSWORD || 'password'
const SCENARIO = __ENV.SCENARIO || 'mixed'
const VUS = Number(__ENV.VUS || 10)
const DURATION = __ENV.DURATION || '30s'

// Per-operation latency so each endpoint is measured independently.
const listLatency = new Trend('op_list_latency', true)
const asyncLatency = new Trend('op_async_submit_latency', true)
const syncLatency = new Trend('op_sync_generate_latency', true)
const reportsCreated = new Counter('reports_created')

function rampingScenario(exec, peak) {
  return {
    executor: 'ramping-vus',
    startVUs: 0,
    exec,
    gracefulStop: '5s',
    stages: [
      { duration: '5s', target: peak },
      { duration: DURATION, target: peak },
      { duration: '5s', target: 0 },
    ],
  }
}

function buildScenarios() {
  switch (SCENARIO) {
    case 'read':
      return { read: rampingScenario('browseReports', VUS) }
    case 'async':
      return { async: rampingScenario('submitAsync', VUS) }
    case 'sync':
      // sync does real Excel + PDF generation, so keep concurrency modest
      return { sync: rampingScenario('generateSync', Math.max(1, Math.min(VUS, 5))) }
    default:
      return { mixed: rampingScenario('mixedTraffic', VUS) }
  }
}

export const options = {
  scenarios: buildScenarios(),
  thresholds: {
    http_req_failed: ['rate<0.01'],
    op_list_latency: ['p(95)<500'],
    op_async_submit_latency: ['p(95)<1000'],
    op_sync_generate_latency: ['p(95)<4000'],
  },
}

export function setup() {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  )
  check(res, { 'login succeeded': (r) => r.status === 200 })
  return { token: res.json('token') }
}

function authHeaders(token) {
  return {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
  }
}

function reportBody(kind) {
  return JSON.stringify({
    description: `bench-${kind}-${__VU}-${__ITER}`,
    submitter: 'k6',
    headers: ['Id', 'Name', 'Score'],
    data: [
      ['1', 'Alice', '90'],
      ['2', 'Bob', '85'],
      ['3', 'Carol', '88'],
    ],
  })
}

export function browseReports(data) {
  const res = http.get(`${BASE_URL}/report`, authHeaders(data.token))
  listLatency.add(res.timings.duration)
  check(res, { 'list 200': (r) => r.status === 200 })
  sleep(0.3)
}

export function submitAsync(data) {
  const res = http.post(`${BASE_URL}/report/async`, reportBody('async'), authHeaders(data.token))
  asyncLatency.add(res.timings.duration)
  if (check(res, { 'async 200': (r) => r.status === 200 })) reportsCreated.add(1)
  sleep(0.3)
}

export function generateSync(data) {
  const res = http.post(`${BASE_URL}/report/sync`, reportBody('sync'), authHeaders(data.token))
  syncLatency.add(res.timings.duration)
  if (check(res, { 'sync 200': (r) => r.status === 200 })) reportsCreated.add(1)
  sleep(0.5)
}

// Realistic dashboard traffic: read-heavy with occasional async writes.
export function mixedTraffic(data) {
  if (Math.random() < 0.2) {
    submitAsync(data)
  } else {
    browseReports(data)
  }
}
