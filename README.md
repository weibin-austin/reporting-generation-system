# Reporting Generation System

A microservice-based reporting platform. A client submits tabular data and the
system generates the same report in **three formats — Excel, PDF, and PNG image —**
either synchronously or asynchronously, tracks each report's status, and lets the
user download or manage it through a REST API and a React UI.

## Architecture

Four Spring Boot services coordinate over AWS SNS/SQS/S3 and DynamoDB:

| Service | Port | Responsibility | Storage |
| --- | --- | --- | --- |
| **ClientService** | 8080 | Public API + web UI, orchestration, report tracking, email notifications | PostgreSQL (report metadata) |
| **ExcelService** | 8888 | Generates `.xlsx` (Apache POI) | PostgreSQL metadata + local file |
| **PDFService** | 9999 | Generates PDF (JasperReports) | S3 + DynamoDB (`PDFFile`) |
| **ImageService** | 7777 | Renders a PNG table image (Java2D) | S3 + DynamoDB (`ImageFile`) |

**Two generation paths:**

- **Sync** (`POST /report/sync`) — ClientService calls the three worker services
  concurrently over REST and returns the combined result. Latency is `max(excel, pdf, image)`,
  not their sum.
- **Async** (`POST /report/async`) — ClientService publishes to an SNS topic that
  fans out to three request queues (`Excel_Request_Queue`, `PDF_Request_Queue`,
  `Image_Request_Queue`). Each worker generates its file and replies on its response
  queue; ClientService consumes the replies and updates the report's status.

Every request queue has a **dead-letter queue** (`maxReceiveCount=3`): transient
failures are retried and self-heal, persistent ones dead-letter and the report is
marked FAILED rather than hanging. A single email notification is sent once all
three legs reach a terminal state (via an SQS `email_queue` consumed by the
[email Lambda](./lambda/sendEmailCode.py)).

See [ReportingSystemArchitecture.pdf](./ReportingSystemArchitecture.pdf) for the
original high-level diagram.

## Tech stack

- Java 11, Spring Boot 2.7.18, Maven (multi-module)
- Spring Cloud AWS (`io.awspring.cloud` 2.4) for SNS/SQS/S3
- AWS SDK v1 (S3, DynamoDB via `DynamoDBMapper`)
- Apache POI (Excel), JasperReports (PDF), Java2D/ImageIO (PNG)
- Spring Security + JWT (`jjwt`) on the ClientService API
- PostgreSQL (ClientService + ExcelService; in-memory H2 for the test suite), DynamoDB (PDF + Image metadata)
- React 18 + Vite web UI
- JUnit 5 + Mockito; k6 for load testing; GitHub Actions CI

## Running locally

Everything runs against **LocalStack** (AWS) and a local **PostgreSQL** — no real AWS
account required. All services use the `local` Spring profile.

### 1. Start LocalStack and provision resources

```bash
# LocalStack (community image) — provides SQS, SNS, S3, DynamoDB
docker run -d --name reporting-localstack -p 4566:4566 localstack/localstack:4.0

# Create the topic, queues (+ DLQs), S3 bucket, subscriptions, and DynamoDB tables.
# Requires the AWS CLI pointed at LocalStack (or run the equivalent awslocal commands).
AWS_ENDPOINT=http://localhost:4566 ./scripts/setup_localstack.sh
```

### 2. Start PostgreSQL

ClientService and ExcelService store metadata in PostgreSQL. Connection settings are
overridable via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`; the defaults expect a server
on `localhost:5432` with the `report` and `excel_service` databases:

```bash
docker run -d --name reporting-postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=report -p 5432:5432 postgres:16
docker exec reporting-postgres psql -U postgres -c "CREATE DATABASE excel_service;"
```

Schemas are created automatically on startup (Hibernate `ddl-auto=update`). The test
suite uses in-memory H2, so `mvn test` and CI need no database server.

### 3. Build

```bash
mvn clean package
```

### 4. Start the services (each with the local profile)

```bash
java -jar ExcelService/target/reporting.jar  --spring.profiles.active=local   # :8888
java -jar PDFService/target/reporting.jar    --spring.profiles.active=local   # :9999
java -jar ImageService/target/reporting.jar  --spring.profiles.active=local   # :7777
java -jar ClientService/target/main.jar      --spring.profiles.active=local   # :8080
```

> On JDK 17+, prepend the JVM flags Boot 2.7 needs:
> `--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED`.
> The DynamoDB tables are auto-created on startup if missing.

### 5. Web UI

```bash
cd web
npm install
npm run dev        # http://localhost:5173  (proxies the API to :8080)
```

Sign in with the demo account **`admin` / `password`**.

The email step is optional. In a real AWS deployment the [Lambda](./lambda/sendEmailCode.py)
drains `email_queue`; locally, [`scripts/local_email_worker.py`](./scripts/local_email_worker.py)
delivers queued messages through macOS Mail.app.

## API

The report API requires a JWT. Get one from `POST /auth/login`, then send
`Authorization: Bearer <token>`.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/auth/login` | Authenticate, returns a JWT (public) |
| `GET`  | `/report` | List all reports with per-format status |
| `POST` | `/report/sync` | Generate synchronously, return the completed report |
| `POST` | `/report/async` | Submit for async generation (SNS fan-out) |
| `GET`  | `/report/content/{reqId}/{type}` | Download a file — `type` = `PDF` \| `EXCEL` \| `IMAGE` |
| `PUT`  | `/report/{reqId}` | Update a report's description |
| `DELETE` | `/report/{reqId}` | Delete a report and all its generated files |

Request body for generation:

```json
{
  "description": "Student Math Course Report",
  "headers": ["Student #", "Name", "Class", "Score"],
  "data": [["s-008", "Sarah", "Class-A", "B"], ["s-009", "Thomas", "Class-A", "B-"]],
  "submitter": "Mrs. York"
}
```

The Excel and PDF services also expose Swagger UI (`/swagger-ui/index.html`).

## Testing, benchmarking, CI

- **Unit/integration tests:** `mvn test` (LocalStack must be running for the
  context-loading tests). ~76 tests across the four modules.
- **Load/pressure test:** [k6](https://k6.io/) scripts in [`benchmark/`](./benchmark).
  See [benchmark/README.md](./benchmark/README.md) — `read`, `async`, `sync`, and
  `mixed` scenarios with SLO thresholds.
- **CI:** [GitHub Actions](./.github/workflows/ci.yml) runs the full build + tests
  on every push/PR, with LocalStack and MongoDB as service containers.

## Improvements implemented

This started as an evaluation project with a list of suggested improvements. All of
them have been implemented:

- [x] **0.** Update / delete report APIs (`PUT` / `DELETE /report/{reqId}`), with the UI wired up
- [x] **1.** Concurrent sync API — the three workers are called in parallel (`CompletableFuture`)
- [x] **2.** Database instead of the in-memory hashmap in `ExcelRepositoryImpl` (Spring Data JPA + PostgreSQL)
- [x] **3.** Expanded test coverage across all modules
- [x] **5.** Pressure/benchmark tests (k6)
- [x] **6.** MongoDB → DynamoDB for PDF (and Image) metadata
- [x] **7.** Bug fixes (build failures, packaged-jar resource loading, download links, failure emails, …)
- [x] **8.** Fault tolerance — dead-letter queues, retry, non-retryable-vs-transient failure handling
- [x] **9.** Security + JWT on the ClientService API
- [x] **10.** Modern React UI ([`web/`](./web))
- [x] **11.** CI/CD pipeline (GitHub Actions)
- [x] **12.** New worker service — `ImageService` (PNG generation)

> Item **4** (Eureka/Ribbon service discovery) was intentionally skipped: both are in
> maintenance mode. The system uses configurable service URLs
> (`app.service.{excel,pdf,image}.url`); Spring Cloud LoadBalancer or Kubernetes-native
> discovery would be the modern equivalent if needed.
