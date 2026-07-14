# Implementation Tasks — Order Fulfilment Workflow

> Tasks are ordered by dependency. Complete each phase before starting the next.
> Each task references the relevant design section and requirement IDs.

---

## Phase 0 — Project Scaffold

- [ ] **T-00.1** Initialise Gradle multi-module project
  - Create root `settings.gradle` including `service`, `api`, `app` modules
  - Create `buildSrc/` with `common.gradle`, `library.gradle`, `application.gradle` convention plugins
  - Create `gradle/libs.versions.toml` with all dependency versions (Spring Boot BOM, Camunda, H2, Resilience4j, JUnit5, Jacoco, springdoc)
  - Verify `./gradlew build` passes on empty modules
  - _Ref: FR-B01, FR-B02_

- [ ] **T-00.2** Create `app` module skeleton
  - `OrderFulfilmentApplication.java` with `main()`
  - `application.yml` with all environment-externalised config (Camunda URL, timer duration, virtual threads)
  - `application-dev.yml` with H2 console, show-sql, short timer duration
  - _Ref: FR-B02, NFR-01_

- [ ] **T-00.3** Create BPMN file placeholder
  - Add `app/src/main/resources/processes/order-fulfilment.bpmn` with a valid (minimal) BPMN skeleton
  - _Ref: FR-W02_

---

## Phase 1 — BPMN Process Model

- [ ] **T-01.1** Model complete BPMN 2.0 process
  - Start Event → Validate Order (external task) → XOR Gateway (3 paths)
  - Path A: auto-approved → AND split → Reserve Inventory + Generate Invoice → AND join
  - Path B: User Task (Approve Credit Override) with Timer boundary (interrupting, configurable) and Message boundary (non-interrupting, CustomerUpdatedPriority → Update SLA Log)
  - Path C: Order Rejected End Event
  - AND join → OR inclusive gateway → Send Email / Send SMS → OR join → Order Fulfilled End Event
  - _Ref: FR-W02, FR-W03, FR-W04_

- [ ] **T-01.2** Configure all external task topics in BPMN
  - Set `camunda:topic` on each Service Task: `validate-order`, `update-sla-log`, `reserve-inventory`, `generate-invoice`, `send-email`, `send-sms`
  - Set `camunda:type="external"` on all service tasks
  - _Ref: FR-B04_

- [ ] **T-01.3** Configure gateway conditions
  - XOR gateway JUEL expressions for amount thresholds and validation result
  - OR gateway JUEL expressions for notification preference
  - _Ref: FR-W02 §3.3_

- [ ] **T-01.4** Configure timer and message boundary events
  - Timer: `${timerDuration}` process variable (ISO-8601), default `PT10M`
  - Message: name = `CustomerUpdatedPriority`
  - _Ref: FR-W03, FR-W04_

---

## Phase 2 — Service Module (Domain Layer)

- [ ] **T-02.1** Create `Order` JPA entity and `OrderStatus` enum
  - All fields per FR-B06, `@Version` for optimistic locking, `@PrePersist`/`@PreUpdate` timestamps
  - _Ref: FR-B06, §9.4_

- [ ] **T-02.2** Create `OrderRepository` (Spring Data JPA)
  - `findByStatus(OrderStatus)`, `findByProcessInstanceId(String)` query methods
  - _Ref: FR-B06_

- [ ] **T-02.3** Create `CamundaRestClient`
  - Spring 6 `RestClient` wrapper with `@CircuitBreaker` + `@Retry` (Resilience4j)
  - Methods: `startProcess()`, `correlateMessage()`, `getPendingTasks()`, `completeTask()`, `isProcessDeployed()`, `deployProcess()`
  - _Ref: §2.1, §9.3_

- [ ] **T-02.4** Create `BpmnDeploymentService` (`ApplicationRunner`)
  - On startup: check if `order-fulfilment` process is deployed; deploy if not
  - Idempotent — safe to run on every startup
  - _Ref: §11.3_

- [ ] **T-02.5** Create `OrderService`
  - `createOrder(OrderRequest)`: persist order, start Camunda process, return response
  - `updateStatus(UUID, OrderStatus, String reason)`: transactional status update with optimistic lock retry
  - `getOrders()`, `getOrderById(UUID)`
  - Publish `OrderFulfilledEvent` when status → `FULFILLED`
  - _Ref: FR-B03, FR-B05, §4.3_

- [ ] **T-02.6** Create `CreditTaskService`
  - `getPendingTasks()`: fetch user tasks from Camunda, enrich with local order data
  - `completeTask(taskId, approved, comment)`: complete Camunda task, update order status
  - _Ref: FR-B05_

- [ ] **T-02.7** Create `BaseExternalTaskWorker` abstract class
  - Template Method pattern: `execute()` → `executeTask()` → `complete()`/`handleFailure()`
  - Exponential backoff on failure, logging, BPMN error propagation
  - _Ref: §4.2.2, §9.2_

- [ ] **T-02.8** Implement `ValidateOrderWorker`
  - Validates: customerId non-blank, customerName non-blank, amount > 0
  - Sets `validationResult` (VALID/INVALID), `validationReason` process variables
  - Updates order status to `VALIDATING` or `REJECTED`
  - _Ref: FR-B04, FR-W02 step 2_

- [ ] **T-02.9** Implement `UpdateSlaLogWorker`
  - Logs SLA update with orderId and timestamp
  - Updates order `decisionReason` with SLA note
  - _Ref: FR-B04_

- [ ] **T-02.10** Implement `ReserveInventoryWorker`
  - Simulates inventory reservation with log statement
  - Updates order status to `FULFILLING` (if not already)
  - _Ref: FR-B04_

- [ ] **T-02.11** Implement `GenerateInvoiceWorker`
  - Simulates invoice generation with log statement
  - _Ref: FR-B04_

- [ ] **T-02.12** Implement `SendEmailWorker`
  - Logs email notification sent for orderId
  - _Ref: FR-B04_

- [ ] **T-02.13** Implement `SendSmsWorker`
  - Logs SMS notification sent for orderId
  - Updates order status to `FULFILLED` after notification
  - _Ref: FR-B04_

- [ ] **T-02.14** Create `ExternalTaskClientConfig`
  - Register all workers with the `ExternalTaskClient` bean
  - Configure lock duration, async timeout, max tasks, worker ID
  - `@PreDestroy` to cleanly close client on shutdown
  - _Ref: NFR-02, §9.7_

---

## Phase 3 — API Module (REST Layer)

- [ ] **T-03.1** Create DTOs
  - `OrderRequest` record with `@Valid` annotations
  - `OrderResponse` record (Builder)
  - `TaskResponse` record
  - `CompleteTaskRequest` record
  - `ErrorResponse` record
  - _Ref: FR-B09, §7.2_

- [ ] **T-03.2** Create `OrderWebhookController`
  - `POST /api/orders/webhook` → delegates to `OrderService.createOrder()`
  - Returns `201 Created` with `OrderResponse`
  - _Ref: FR-B03_

- [ ] **T-03.3** Create `OrderQueryController`
  - `GET /api/orders` → list all orders
  - `GET /api/orders/{id}` → single order or 404
  - _Ref: FR-B05_

- [ ] **T-03.4** Create `TaskController`
  - `GET /api/tasks/credit-override` → pending tasks
  - `POST /api/tasks/credit-override/{taskId}/complete` → approve/reject
  - _Ref: FR-B05_

- [ ] **T-03.5** Create `PriorityController`
  - `POST /api/orders/{id}/priority` → correlate `CustomerUpdatedPriority` message
  - Returns `204 No Content`
  - _Ref: FR-B05, FR-W04_

- [ ] **T-03.6** Create `GlobalExceptionHandler`
  - Handle: `OrderNotFoundException` (404), `MethodArgumentNotValidException` (400), `OptimisticLockException` (409), generic `Exception` (500)
  - Consistent `ErrorResponse` structure with timestamp, status, message, path
  - _Ref: FR-B09_

---

## Phase 4 — App Module (Config & Security)

- [ ] **T-04.1** Create `SecurityConfig`
  - Two in-memory users: `customer` (role CUSTOMER), `officer` (role CREDIT_OFFICER)
  - URL-based authorization per §10.1
  - CSRF disabled (REST API)
  - _Ref: FR-S02_

- [ ] **T-04.2** Create `WebConfig` (CORS)
  - Allow `http://localhost:4200` for all `/api/**` endpoints
  - _Ref: NFR-05_

- [ ] **T-04.3** Create `OpenApiConfig`
  - API title, version, description
  - Basic auth security scheme in OpenAPI spec
  - _Ref: FR-B08_

- [ ] **T-04.4** Verify `./gradlew build` and `./gradlew :app:bootJar`
  - Clean build must pass
  - JAR must start and connect to Camunda
  - _Ref: FR-B02_

---

## Phase 5 — Unit Tests

- [ ] **T-05.1** `ValidateOrderWorkerTest`
  - Test: null customerId → INVALID
  - Test: blank customerName → INVALID
  - Test: negative amount → INVALID
  - Test: valid payload → VALID with correct variables
  - _Ref: FR-Q01_

- [ ] **T-05.2** `OrderServiceTest`
  - Test: credit decision routing (amount ≤ 1000 → AUTO_APPROVED)
  - Test: credit decision routing (amount > 1000 → PENDING_OVERRIDE)
  - Test: status update persisted correctly
  - Mock `OrderRepository` and `CamundaProcessService`
  - _Ref: FR-Q01_

- [ ] **T-05.3** `ReserveInventoryWorkerTest`
  - Test: successful execution sets expected variables
  - Test: exception triggers `handleFailure` with retry decrement
  - _Ref: FR-Q01_

- [ ] **T-05.4** `GenerateInvoiceWorkerTest`
  - Test: successful execution logs and completes
  - Test: BPMN error propagated correctly
  - _Ref: FR-Q01_

---

## Phase 6 — Angular Frontend

- [ ] **T-06.1** Scaffold Angular 22 app
  - `ng new frontend --standalone --routing --style=scss`
  - Install Angular Material (`ng add @angular/material`)
  - Configure `environment.ts` with `apiUrl`
  - Set up `app.config.ts` with `provideRouter`, `provideHttpClient(withFetch())`, `provideAnimationsAsync()`
  - _Ref: FR-F04, FR-F05_

- [ ] **T-06.2** Create core services and models
  - `OrderService` with all HTTP methods
  - `TaskService` with credit task HTTP methods
  - `ErrorInterceptor` (functional interceptor) for global error toasts
  - TypeScript models: `OrderResponse`, `OrderRequest`, `TaskResponse`, `CompleteTaskRequest`
  - _Ref: §5.3, §5.4_

- [ ] **T-06.3** Implement `NewOrderComponent`
  - Reactive form: customerId, customerName, amount, notificationPreference
  - Submit → `POST /api/orders/webhook`
  - Success/error feedback via `MatSnackBar`
  - _Ref: FR-F01_

- [ ] **T-06.4** Implement `OrdersListComponent`
  - Display table with `MatTable`: id (truncated), customerName, amount, status badge, createdAt
  - Status badge: colour-coded chip (FULFILLED=green, REJECTED=red, PENDING_OVERRIDE=orange, etc.)
  - "Refresh" button → reload orders
  - "Update Priority" button per row (shown only for `PENDING_OVERRIDE`) → `POST /api/orders/{id}/priority`
  - _Ref: FR-F02_

- [ ] **T-06.5** Implement `CreditReviewComponent`
  - Fetch and display pending credit tasks
  - Each row: customer name, amount, task created date
  - Inline `MatFormField` comment input per row
  - Approve / Reject `MatButton` → `POST /api/tasks/credit-override/{taskId}/complete`
  - Refresh list after action
  - _Ref: FR-F03_

- [ ] **T-06.6** Implement `AppComponent` shell
  - `MatToolbar` with app title
  - `MatTabGroup` or `MatSideNav` linking to routes: Orders, New Order, Credit Review
  - `RouterOutlet`
  - _Ref: FR-F04_

- [ ] **T-06.7** Implement `StatusBadgeComponent`
  - Standalone component: `@Input() status: OrderStatus`
  - Renders `MatChip` with status-dependent colour
  - _Ref: FR-F02_

---

## Phase 7 — Docker Compose & Integration

- [ ] **T-07.1** Create `docker-compose.yml`
  - Services: `camunda` (camunda-bpm-platform:run-7.21.0), `backend`, `frontend`
  - Health checks, `depends_on` with `condition: service_healthy`
  - Environment variable overrides (short timer for demo)
  - _Ref: FR-S01_

- [ ] **T-07.2** Create `Dockerfile` for backend
  - Multi-stage: `gradle:8-jdk21` build → `eclipse-temurin:21-jre` runtime
  - `ENTRYPOINT ["java", "-jar", "/app/app.jar"]`

- [ ] **T-07.3** Create `Dockerfile` for frontend
  - Multi-stage: `node:22` build → `nginx:alpine` serve
  - `nginx.conf` with `try_files` for Angular routing + API proxy

---

## Phase 8 — Documentation

- [ ] **T-08.1** Write `README.md`
  - Prerequisites (Java 21, Node 22, Docker, Gradle 8)
  - How to run: docker-compose (single command) and manual (3 separate terminals)
  - Sample cURL requests for each endpoint
  - How to demo: full happy path + timer expiry + message correlation
  - Assumptions and known limitations
  - _Ref: FR-Q02_

- [ ] **T-08.2** Write `AI-USAGE.md`
  - Tools used (Kiro / Claude)
  - Most significant prompts
  - Where AI output was accepted as-is, modified, or rejected and why
  - _Ref: FR-A (AI usage section)_

---

## Phase 9 — Final Verification

- [ ] **T-09.1** End-to-end happy path test
  - Submit order ≤ 1000 → auto-approved → fulfilled
  - Submit order > 1000 → pending override → approve → fulfilled
  - Verify EMAIL/SMS/BOTH notification routing

- [ ] **T-09.2** Boundary event tests
  - Let timer expire → verify AUTO_CANCELLED status
  - Send priority update → verify SLA log worker executes without interrupting user task

- [ ] **T-09.3** Rejection path test
  - Submit invalid order → verify REJECTED status
  - Submit order > 1000 → reject override → verify REJECTED status

- [ ] **T-09.4** Run `./gradlew build` from clean checkout — must pass
- [ ] **T-09.5** Run `docker-compose up` — all three services healthy
