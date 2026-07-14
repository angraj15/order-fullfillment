# Design — Order Fulfilment Workflow

## Table of Contents

1. [High-Level Design](#1-high-level-design)
2. [System Architecture](#2-system-architecture)
3. [BPMN Process Design](#3-bpmn-process-design)
4. [Backend Low-Level Design](#4-backend-low-level-design)
5. [Frontend Low-Level Design](#5-frontend-low-level-design)
6. [Database Design](#6-database-design)
7. [API Contract Design](#7-api-contract-design)
8. [Design Patterns Applied](#8-design-patterns-applied)
9. [Resilience and Recovery Design](#9-resilience-and-recovery-design)
10. [Security Design](#10-security-design)
11. [Infrastructure and Deployment Design](#11-infrastructure-and-deployment-design)
12. [Key Design Decisions Summary](#12-key-design-decisions-summary)

---

## 1. High-Level Design

### 1.1 System Context

Three runtime processes communicate over HTTP:

- **Angular 22 Frontend** (port 4200) — UI with login form, role-based navigation (Customer: Orders + New Order; Credit Officer: Credit Review)
- **Spring Boot Backend** (port 8080) — Java 21 microservice owning order persistence, REST API, and external task workers
- **Camunda 7 Engine** (port 8081) — standalone BPM engine in Docker. Backend never embeds it; all interaction via Camunda REST API and External Task Client long-poll protocol

```
  Angular 22 Frontend  <──REST + Basic Auth──>  Spring Boot Backend  <──REST──>  Camunda 7 Engine
     (port 4200)                                   (port 8080)                     (port 8081)
     Login Form                                        |
     Role-based tabs                              H2 In-Memory DB
```

### 1.2 Key Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Engine deployment | Standalone Docker | Requirement mandates no embedded engine |
| Engine integration | REST API + External Task Client | Long-polling decouples workers from engine lifecycle |
| Backend structure | Gradle multi-module (service/api/app) | Enforces strict dependency direction; isolates domain from HTTP layer |
| Persistence | H2 in-memory + Spring Data JPA | Zero-setup for demo; Postgres swap is one config change |
| Frontend state | Angular Signals + RxJS | Angular 22 idiomatic; no NgRx overhead needed |
| Frontend auth | Login form + sessionStorage + HTTP interceptor | Role separation visible in UI without OAuth infrastructure |
| Concurrency | Java 21 Virtual Threads | High-throughput I/O polling without reactive complexity |
| Resilience | Resilience4j Circuit Breaker + Retry | Camunda REST calls protected; engine restarts don't crash the backend |
| Auth | Spring Security Basic Auth | Role separation (CUSTOMER/CREDIT_OFFICER) without OAuth infrastructure |
| BPMN merge pattern | XOR merge gateway before parallel split | Avoids parallel gateway waiting for multiple incoming tokens |

---

## 2. System Architecture

### 2.1 Module Dependency Graph

```
app  ──depends──>  api  ──depends──>  service
(Spring Boot)     (REST layer)        (Domain layer)
main class        controllers         entities, repos
application.yml   DTOs                workers, services
security config   exception handler   camunda client
camunda config
```

### 2.2 Frontend Architecture

```
Angular 22 (Standalone Components)
├── Login → AuthService (sessionStorage + Basic Auth credentials)
├── AuthInterceptor → attaches Basic Auth header to /api/ calls
├── AuthGuard / RoleGuard → protects routes based on login state and role
├── AppComponent → shows/hides tabs based on AuthService.role()
├── OrdersListComponent → GET /api/orders (CUSTOMER)
├── NewOrderComponent → POST /api/orders/webhook (CUSTOMER)
└── CreditReviewComponent → GET/POST /api/tasks/credit-override (CREDIT_OFFICER)
```

### 2.3 External Task Worker Status Update Strategy

Each worker is responsible for advancing the local DB status at specific points:

```
Process Flow:                          DB Status Set By:
Start Event                            → RECEIVED (OrderService.createOrder)
  ↓
ValidateOrderWorker (amount ≤ 1000)    → VALIDATING (auto-approved path)
ValidateOrderWorker (amount > 1000)    → PENDING_OVERRIDE (credit override path)
ValidateOrderWorker (invalid)          → REJECTED
  ↓
XOR Gateway → auto-approved path
  ↓
XOR Merge Gateway
  ↓
Parallel Split
  ↓
ReserveInventoryWorker                 → AUTO_APPROVED (if currently VALIDATING)
GenerateInvoiceWorker                  → (no status change)
  ↓
Parallel Join
  ↓
Inclusive Gateway → notifications
  ↓
SendEmailWorker                        → FULFILLED
SendSmsWorker                          → FULFILLED (idempotent)
```

For the credit override path:
```
ValidateOrderWorker (amount > 1000)    → PENDING_OVERRIDE
  ↓
XOR Gateway → override path
  ↓
User Task (Approve Credit Override)    → PENDING_OVERRIDE (set by CreditTaskService on fetch)
  ↓
CreditTaskService.completeTask()       → APPROVED or REJECTED
  ↓
XOR Merge → Parallel Split → ... → FULFILLED
```

---

## 3. BPMN Process Design

### 3.1 Process Flow (Corrected)

```
[Start: Order Received]
        |
        v
[Ext Task: Validate Order]  topic=validate-order
        |
        v
   {XOR: Credit Decision}
    |           |              |
[INVALID]  [amt>1000]     [amt<=1000]
    |           |              |
    v           v              v
[End:       [User Task:    {XOR Merge} <── also receives approved path
 Rejected]   Override]         |
              |    |           v
        [Timer]    [Msg]   {AND Split}
        PT2M       non-int  /        \
              |          [Reserve]  [Invoice]
              v               \        /
         [End:Auto-         {AND Join}
          Cancel]                |
              |                  v
         [rejected]        {OR Split}
              |            /          \
              v       [Email]      [SMS]
         [End:             \          /
          Rejected]       {OR Join}
                               |
                               v
                         [End: Fulfilled]
```

Key design fix: The **XOR Merge Gateway** (`gw_credit_merge`) sits between the credit decision paths and the parallel split. This ensures a single token passes through regardless of whether it came from auto-approval or officer approval. A parallel gateway would wait for ALL incoming tokens (causing deadlock).

### 3.2 Gateway Expressions (JUEL)

```
XOR auto-approved:     ${validationResult == 'VALID' && amount <= 1000}
XOR override required: ${validationResult == 'VALID' && amount > 1000}
XOR invalid:           default flow (no condition needed)
XOR approved:          ${approved == true}
XOR rejected:          ${approved == false}
OR send email:         ${notificationPreference == 'EMAIL' || notificationPreference == 'BOTH'}
OR send sms:           ${notificationPreference == 'SMS'   || notificationPreference == 'BOTH'}
```

### 3.3 Boundary Events

- **Timer (interrupting):** `${timerDuration}` process variable — PT10M default, PT2M in dev profile
- **Message (non-interrupting):** `CustomerUpdatedPriority` — triggers Update SLA Log without cancelling the user task

### 3.4 BPMN DI (Diagram Interchange)

The `.bpmn` file includes a complete `<bpmndi:BPMNDiagram>` section with coordinates for all elements, enabling Camunda Cockpit to render the visual process diagram.

---

## 4. Backend Low-Level Design

### 4.1 Worker Implementations

| Worker | Topic | Status Update | Key Logic |
|--------|-------|--------------|-----------|
| ValidateOrderWorker | validate-order | VALIDATING (≤1000), PENDING_OVERRIDE (>1000), or REJECTED | Checks customerId, customerName, amount > 0; determines credit path based on amount |
| ReserveInventoryWorker | reserve-inventory | AUTO_APPROVED (if VALIDATING) | Generates INV-xxx reference |
| GenerateInvoiceWorker | generate-invoice | (none) | Generates INV-timestamp number |
| SendEmailWorker | send-email | FULFILLED | Log-based email simulation |
| SendSmsWorker | send-sms | FULFILLED (idempotent) | Log-based SMS simulation |
| UpdateSlaLogWorker | update-sla-log | (updates decisionReason only, no status change) | Logs SLA note with timestamp; does NOT transition status |
| AutoCancelWorker | auto-cancel-order | AUTO_CANCELLED | Triggered by timer boundary event when credit officer doesn't act within timeout |

### 4.2 CamundaRestClient

- Uses Spring 6 `RestClient` for all Camunda REST API calls
- `@CircuitBreaker(name="camunda")` on `startProcess()` with fallback returning 503
- `@Retry(name="camunda")` on all mutating methods (3 attempts, exponential backoff)
- BPMN deployment uses `RestTemplate` with `LinkedMultiValueMap` for multipart form-data (RestClient multipart is unreliable)
- Stores `baseUrl` field for RestTemplate usage in deployment

### 4.3 BpmnDeploymentService

- Implements `ApplicationRunner` (runs after Spring context ready)
- Checks if process deployed via `GET /process-definition/key/order-fulfilment`
- Deploys via `POST /deployment/create` with multipart if not found
- Catches exceptions without crashing app — logs error, app continues
- Fallback: manual deployment via curl command documented in README

---

## 5. Frontend Low-Level Design

### 5.1 Authentication Flow

```
LoginComponent
  → AuthService.login(username, password)
  → Validates against known demo users (client-side)
  → Stores AuthUser {username, role, credentials(base64)} in sessionStorage
  → Routes to /orders (CUSTOMER) or /credit-review (CREDIT_OFFICER)

AuthInterceptor (functional HttpInterceptorFn)
  → Reads credentials from AuthService.credentials() signal
  → Attaches "Authorization: Basic {base64}" header to all /api/ requests

AuthGuard (CanActivateFn)
  → Checks AuthService.isLoggedIn()
  → Redirects to /login if not authenticated

RoleGuard (factory function returning CanActivateFn)
  → Checks AuthService.role() matches required role
  → Redirects to /orders if wrong role
```

### 5.2 Component Visibility by Role

| Component | CUSTOMER | CREDIT_OFFICER |
|-----------|----------|----------------|
| Orders tab | Yes | No |
| New Order tab | Yes | No |
| Credit Review tab | No | Yes |
| Update Priority button | Yes (on PENDING_OVERRIDE orders) | N/A |

---

## 8. Design Patterns Applied

| Pattern | Location | Purpose |
|---|---|---|
| Template Method | BaseExternalTaskWorker | Fixed skeleton: execute → executeTask → complete/handleFailure |
| Adapter | CamundaRestClient | Wraps Camunda REST API into typed Java interface |
| Facade | OrderService | Single entry point for order lifecycle + Camunda + events |
| State Machine | OrderService.updateStatus() | Validates transitions; invalid jumps throw exception |
| Observer | OrderFulfilledEvent | Domain event published on FULFILLED; listeners decoupled |
| Factory Method | OrderService.createOrder() | Creates Order entity from parameters |
| Circuit Breaker | CamundaRestClient.startProcess() | Resilience4j protects against engine downtime |
| Chain of Responsibility | GlobalExceptionHandler | Most-specific handler first, generic fallback last |
| Strategy | Send Email / Send SMS workers | OR gateway selects which notification strategies execute |

---

## 12. Key Design Decisions Summary

| Decision | Chosen Approach | Alternative | Reason |
|---|---|---|---|
| Credit path merge | XOR merge gateway before parallel split | Parallel gateway with 2 incoming | Parallel GW waits for ALL tokens — causes deadlock when only one path fires |
| Worker status updates | Each worker updates DB at its execution point | Central status service polling Camunda | Workers already have context; avoids extra Camunda API calls |
| BPMN deployment | RestTemplate multipart in ApplicationRunner | RestClient multipart / embedded deploy | RestClient multipart has classpath issues; RestTemplate is reliable for multipart |
| Frontend auth | Login form + sessionStorage + interceptor | Token-based JWT | Simple for demo; demonstrates role separation without auth server |
| BPMN DI | Inline coordinates in .bpmn file | Separate diagram file | Camunda Cockpit requires DI in same file for rendering |
| Boundary event outgoing | No `<outgoing>` element on boundary events | Include `<outgoing>` | BPMN XSD for boundary events doesn't allow `<outgoing>` children; sequence flow sourceRef handles routing |
| Java compiler `-parameters` | Added to Gradle convention plugin | Rely on Spring Boot default | Spring needs parameter names for `@PathVariable` resolution; Gradle buildSrc convention plugins don't inherit Boot's defaults |
| ValidateOrderWorker status routing | Sets PENDING_OVERRIDE directly for amount > 1000 | Set VALIDATING for all valid orders | Worker knows the amount; setting correct status immediately avoids invalid transition when officer completes task |
| UpdateSlaLogWorker no status change | Only updates decisionReason field | Tried PENDING_OVERRIDE → PENDING_OVERRIDE transition | Self-transitions aren't valid in the state machine; SLA worker is a side-effect — it shouldn't change order lifecycle state |
| Auto-cancel via external task | Added AutoCancelWorker (topic: auto-cancel-order) between timer and end event | Direct timer → end event (no worker) | Without a worker on the timer path, nobody updates the local DB to AUTO_CANCELLED when the timer fires |
| Message correlation strategy | Use `processInstanceId` for direct targeting | `processVariables` matching (ambiguous with multiple instances) | When multiple process instances have the same variable value, `processVariables` fails with "2 executions match". Using `processInstanceId` targets the exact process instance |
