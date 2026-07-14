# Order Fulfilment Workflow — Project Conventions

## Project Overview

This is a full-stack order fulfilment application with:
- **Backend**: Spring Boot 3.3, Java 21, Gradle 8 multi-module (service / api / app)
- **Frontend**: Angular 22, standalone components, Angular Material, signals
- **Process Engine**: Camunda 7 (standalone Docker, NOT embedded)
- **Database**: H2 in-memory (MODE=PostgreSQL)
- **Auth**: Spring Security Basic Auth (CUSTOMER / CREDIT_OFFICER roles)

## Architecture Rules

- Backend module dependency: `app → api → service` (no cycles, no reverse dependencies)
- `service` module: entities, repositories, domain services, external task workers, Camunda client
- `api` module: REST controllers, DTOs, exception handler (depends on :service)
- `app` module: Spring Boot main class, config, security, application.yml (depends on :api and :service)
- Camunda integration is exclusively via REST API and External Task Client — NEVER embed the engine

## Code Conventions

### Java (Backend)
- Java 21 with virtual threads enabled (`spring.threads.virtual.enabled=true`)
- Records for DTOs (immutable, no boilerplate)
- Lombok for entities (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`)
- Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Positive`) on request DTOs
- `@ControllerAdvice` for centralised exception handling (GlobalExceptionHandler)
- Compile with `-parameters` flag (required for Spring @PathVariable resolution)
- All external task workers extend `BaseExternalTaskWorker` (Template Method pattern)
- Resilience4j `@CircuitBreaker` + `@Retry` on all CamundaRestClient methods
- Optimistic locking (`@Version`) on Order entity
- State machine validation in `OrderService.updateStatus()` — invalid transitions throw `InvalidStatusTransitionException`

### Angular (Frontend)
- Angular 22 with standalone components (no NgModules)
- State managed with Angular Signals (`signal()`, `computed()`, `input()`)
- HTTP calls via functional `HttpClient` with `inject()`
- Auth: `AuthService` stores credentials in sessionStorage; `AuthInterceptor` attaches Basic Auth
- Routes guarded by `authGuard` and `roleGuard`
- Angular Material for all UI components
- Material Icons font required in index.html

### BPMN (Camunda 7)
- Process key: `order-fulfilment`
- All service tasks use `camunda:type="external"` with topic names
- Gateway conditions use JUEL expressions (e.g. `${validationResult == 'VALID' && amount <= 1000}`)
- Timer boundary events use process variable `${timerDuration}` (configurable, default PT10M)
- Message correlation uses `processInstanceId` (NOT `processVariables` — avoids ambiguity with multiple instances)
- Boundary events do NOT use `<outgoing>` child elements — sequence flow `sourceRef` handles routing
- Every BPMN path that affects local DB state MUST have an external task worker (end events alone don't trigger app code)
- Process element requires `camunda:historyTimeToLive="180"`
- BPMN file MUST include `<bpmndi:BPMNDiagram>` section for Camunda Cockpit rendering

## External Task Workers

| Worker | Topic | Responsibility |
|--------|-------|---------------|
| ValidateOrderWorker | validate-order | Validates payload; sets VALIDATING (≤1000) or PENDING_OVERRIDE (>1000) or REJECTED |
| ReserveInventoryWorker | reserve-inventory | Simulates reservation; sets AUTO_APPROVED if currently VALIDATING |
| GenerateInvoiceWorker | generate-invoice | Generates invoice number; no status change |
| SendEmailWorker | send-email | Logs email; sets FULFILLED |
| SendSmsWorker | send-sms | Logs SMS; sets FULFILLED (idempotent) |
| UpdateSlaLogWorker | update-sla-log | Logs SLA note; updates decisionReason only (NO status change) |
| AutoCancelWorker | auto-cancel-order | Sets AUTO_CANCELLED when timer boundary fires |

## API Endpoints

| Method | Path | Role | Purpose |
|--------|------|------|---------|
| POST | /api/orders/webhook | CUSTOMER | Submit order, start process |
| GET | /api/orders | CUSTOMER | List all orders |
| GET | /api/orders/{id} | CUSTOMER | Get order detail |
| POST | /api/orders/{id}/priority | CUSTOMER | Correlate priority message |
| GET | /api/tasks/credit-override | CREDIT_OFFICER | List pending tasks |
| POST | /api/tasks/credit-override/{taskId}/complete | CREDIT_OFFICER | Approve/reject |

## Build & Run

- `./gradlew build` must pass from clean checkout
- `./gradlew :app:bootJar` produces runnable JAR
- `docker compose up --build` starts all 3 services
- BPMN auto-deploys on startup via `BpmnDeploymentService`; if it fails, deploy manually with curl
- Frontend build: `npx ng build` (requires Node.js 24.15+)

## Testing Conventions

- JUnit 5 + Mockito for unit tests
- Use `@MockitoSettings(strictness = Strictness.LENIENT)` when @BeforeEach stubs aren't used by all tests
- External task workers tested by mocking `ExternalTask` and `ExternalTaskService`
- Assert with AssertJ (`assertThat(...)`)

## Key Lessons Learned

1. Parallel gateways wait for ALL incoming tokens — use XOR merge gateway to join alternative paths before a parallel split
2. Camunda message correlation with `processVariables` fails when multiple instances match — use `processInstanceId` instead
3. Non-interrupting boundary events are side-effects — workers they trigger should NOT change order lifecycle status
4. Every BPMN path to an end event that should update local state needs an external task worker in between
5. Spring Security intercepts CORS preflight — add `.cors(Customizer.withDefaults())` to SecurityFilterChain
6. Gradle buildSrc convention plugins need Spring Boot Gradle plugin as a dependency to resolve `springBoot{}` DSL
