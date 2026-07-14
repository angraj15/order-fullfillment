# Requirements — Order Fulfilment Workflow

## 1. Overview

Build a production-minded, full-stack order fulfilment application that processes customer orders
through a **Camunda 7 standalone** BPMN workflow engine. The system integrates a **Spring Boot
(Java 21)** microservice backend with an **Angular 22** frontend.

---

## 2. Functional Requirements

### 2.1 Workflow (Camunda 7 — Standalone Engine)

#### FR-W01 — Standalone Engine
- Camunda 7 MUST run as a **separate process** (Docker: `camunda/camunda-bpm-platform`).
- The Spring Boot application MUST NOT embed the Camunda engine.
- Integration happens exclusively via the **Camunda REST API** and the **External Task Client**.

#### FR-W02 — BPMN Process Definition
The process `order-fulfilment` MUST model the following flow in BPMN 2.0:

| Step | Element | Description |
|------|---------|-------------|
| 1 | Start Event | Triggered when an order is received via webhook |
| 2 | Service Task (External) | **Validate Order** — check required fields, positive amount, known customer |
| 3 | XOR Gateway | **Credit Decision** — route based on validation result and amount |
| 3a | Path A | amount ≤ EUR 1,000 → auto-approved → proceed to parallel fulfilment |
| 3b | Path B | amount > EUR 1,000 → route to Credit Override User Task |
| 3c | Path C | validation failed → **Order Rejected** End Event |
| 4 | User Task | **Approve Credit Override** — credit officer approves or rejects |
| 4a | Timer Boundary (interrupting) | No action within configurable duration (default 10 min) → **Auto-Cancel Order** End Event |
| 4b | Message Boundary (non-interrupting) | Receives `CustomerUpdatedPriority` message → triggers parallel **Update SLA Log** Service Task without interrupting the User Task |
| 4c | Rejection path | Credit officer rejects → **Order Rejected** End Event |
| 5 | AND Gateway (split) | Parallel fulfilment: split into two branches |
| 5a | Branch 1 | Service Task (External): **Reserve Inventory** |
| 5b | Branch 2 | Service Task (External): **Generate Invoice** |
| 6 | AND Gateway (join) | Wait for both parallel branches to complete |
| 7 | OR Gateway (inclusive) | Notification routing based on customer preference (EMAIL / SMS / BOTH) |
| 7a | Path 1 | Service Task (External): **Send Email** (log-based) |
| 7b | Path 2 | Service Task (External): **Send SMS** (log-based) |
| 8 | End Event | **Order Fulfilled** |

#### FR-W03 — Timer Configuration
- The timer boundary event duration MUST be configurable via a process variable (default: `PT10M`).
- This allows demo-time adjustment without redeployment.

#### FR-W04 — Message Correlation
- The `CustomerUpdatedPriority` message MUST be correlatable to a running process instance by `orderId`.

---

### 2.2 Backend (Java 21, Spring Boot, Gradle Multi-Module)

#### FR-B01 — Multi-Module Project Structure
```
order-fulfilment/
├── buildSrc/                     ← Gradle convention plugins
│   └── src/main/groovy/
│       ├── common.gradle         ← shared: Java 21, encoding, JUnit5, Jacoco
│       ├── library.gradle        ← applies common, java-library plugin
│       └── application.gradle    ← applies common, Spring Boot plugin
├── gradle/
│   └── libs.versions.toml        ← centralised version catalog
├── service/                      ← java-library: entities, repos, domain services, workers
├── api/                          ← java-library: REST controllers, DTOs (depends on :service)
├── app/                          ← Spring Boot app: main class, application.yml (depends on :api, :service)
└── bpmn/
    └── order-fulfilment.bpmn     ← BPMN 2.0 process definition
```
- Dependency direction: `app → api → service` (no cycles).

#### FR-B02 — Build Conventions
- Gradle 8+ via wrapper (`./gradlew`).
- `./gradlew build` MUST pass from a clean checkout.
- `./gradlew :app:bootJar` MUST produce a runnable JAR.
- All dependency versions in `gradle/libs.versions.toml`.
- Spring versions aligned via the Spring Boot BOM.
- JUnit 5 (`useJUnitPlatform`) and Jacoco applied to ALL modules.

#### FR-B03 — Webhook Endpoint
- `POST /api/orders/webhook` — accepts an incoming order payload and starts a Camunda process instance.
- Returns `orderId` and initial status `RECEIVED`.

#### FR-B04 — External Task Workers
The following topic workers MUST be implemented:

| Topic | Description |
|-------|-------------|
| `validate-order` | Validate required fields, positive amount, known customer; set `validationResult` variable |
| `update-sla-log` | Log SLA update when `CustomerUpdatedPriority` message is received |
| `reserve-inventory` | Simulate inventory reservation (log-based) |
| `generate-invoice` | Simulate invoice generation (log-based) |
| `send-email` | Send email notification (log-based) |
| `send-sms` | Send SMS notification (log-based) |

#### FR-B05 — REST API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/orders` | List all orders with current status |
| `GET` | `/api/orders/{id}` | Get order details |
| `GET` | `/api/tasks/credit-override` | List pending credit-override User Tasks |
| `POST` | `/api/tasks/credit-override/{taskId}/complete` | Complete task with `approved` (boolean) + `comment` |
| `POST` | `/api/orders/{id}/priority` | Correlate `CustomerUpdatedPriority` message to a running instance |

#### FR-B06 — Persistence
- Spring Data JPA with H2 in-memory database.
- `Order` entity with fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `customerId` | String | Customer identifier |
| `customerName` | String | Customer display name |
| `amount` | BigDecimal | Order amount in EUR |
| `notificationPreference` | Enum (EMAIL, SMS, BOTH) | Notification channel |
| `status` | Enum | See FR-B07 |
| `processInstanceId` | String | Camunda process instance ID |
| `decisionReason` | String | Reason for approval/rejection/cancellation |
| `createdAt` | Instant | Order creation timestamp |
| `updatedAt` | Instant | Last status update timestamp |

#### FR-B07 — Order Status Lifecycle

```
RECEIVED → VALIDATING → PENDING_OVERRIDE → FULFILLED
                      ↘ REJECTED
                      ↘ AUTO_CANCELLED
         ↘ REJECTED (failed validation)
```

Statuses: `RECEIVED`, `VALIDATING`, `PENDING_OVERRIDE`, `AUTO_APPROVED`, `APPROVED`, `REJECTED`, `AUTO_CANCELLED`, `FULFILLED`

#### FR-B08 — OpenAPI / Swagger
- Expose API spec and Swagger UI via `springdoc-openapi`.
- Available at `/swagger-ui.html` and `/v3/api-docs`.

#### FR-B09 — Error Handling
- `@ControllerAdvice` with consistent `ErrorResponse` DTO.
- Standard HTTP status codes (400, 404, 409, 500).
- Validation errors via `@Valid` / `@Validated`.

---

### 2.3 Frontend (Angular 22)

#### FR-F01 — Order Form (Simulate Webhook)
- Fields: Customer Name, Customer ID, Amount (EUR), Notification Preference (EMAIL/SMS/BOTH).
- Submits to `POST /api/orders/webhook`.
- Shows success/error feedback.

#### FR-F02 — Orders List
- Displays all orders with: Order ID, Customer Name, Amount, Status (colour-coded), Created At.
- Manual refresh button.
- "Update Priority" button visible on orders with status `PENDING_OVERRIDE` — calls `POST /api/orders/{id}/priority`.

#### FR-F03 — Credit Officer Screen
- Lists all pending credit-override tasks from `GET /api/tasks/credit-override`.
- Each row shows: Order ID, Customer Name, Amount, Created At.
- Inline Approve / Reject buttons with a comment text field.
- On action, calls `POST /api/tasks/credit-override/{taskId}/complete`.

#### FR-F04 — Navigation
- Tabbed or routed navigation between: Orders, New Order, Credit Review.

#### FR-F05 — Styling
- Angular Material components.
- Tidy but plain — no responsive design required.
- No state management library (RxJS/signals only).

---

### 2.4 Quality

#### FR-Q01 — Unit Tests
- JUnit 5 + Mockito.
- Validation logic (`ValidateOrderWorker`).
- Credit decision logic (gateway conditions).
- At least 2 other external task workers.
- Meaningful tests, not full coverage.

#### FR-Q02 — Documentation
- `README.md`: prerequisites, how to run (Camunda engine, backend, frontend), docker-compose instructions, sample cURL requests, assumptions, known limitations.
- `AI-USAGE.md`: tools used, significant prompts, and where AI output was accepted/modified/rejected.

---

### 2.5 Stretch Goals (Optional, Time Permitting)

#### FR-S01 — Docker Compose
- Single `docker-compose.yml` covering: Camunda engine + backend + frontend.

#### FR-S02 — Basic Auth / Role Separation
- Spring Security basic auth.
- Two roles: `CUSTOMER` (can submit orders, view orders) and `CREDIT_OFFICER` (can access credit review screen).

#### FR-S03 — Domain Event on Fulfilment
- Publish a `OrderFulfilledEvent` using Spring Application Events when an order reaches `FULFILLED` status.

---

## 3. Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-01 | Java 21 with virtual threads (`spring.threads.virtual.enabled=true`) |
| NFR-02 | Camunda External Task Client polls with `lockDuration=30000`, `asyncResponseTimeout=10000` |
| NFR-03 | Application starts within 30 seconds |
| NFR-04 | H2 console enabled in dev profile for local inspection |
| NFR-05 | CORS configured for Angular dev server (`http://localhost:4200`) |
| NFR-06 | All timestamps in UTC (ISO-8601) |
| NFR-07 | No circular module dependencies |

---

## 4. Assumptions

1. Customer identity is validated by checking if `customerId` is non-blank (no external customer service).
2. "Known customer" validation is a simple non-empty check — no external lookup.
3. Inventory reservation and invoice generation are simulated via log statements.
4. Email/SMS sending is log-based — no real third-party integration.
5. The credit override timer default is 10 minutes but overridable via `TIMER_DURATION` env var / process variable for demo.
6. A single Camunda deployment (one process definition version) is assumed.
7. Angular 22 uses standalone components (no NgModules), signals for state, and the new `@angular/core` inject pattern.

---

## 5. Out of Scope

- Real payment processing
- External customer/inventory/CRM system integration
- Real email/SMS delivery
- Multi-tenancy
- Production-grade security (JWT, OAuth2)
- Horizontal scaling / clustering
