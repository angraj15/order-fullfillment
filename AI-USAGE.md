# AI Usage Documentation

## Tools Used

- **Kiro IDE** (with Claude AI) — primary tool for code generation, architecture design, and implementation guidance throughout the entire development lifecycle.

---

## Approach

The entire project was developed using a **spec-driven approach**:
1. Requirements document analyzed and structured into `requirements.md`
2. Full architecture and low-level design produced in `design.md`
3. Implementation tasks defined in `tasks.md` with clear dependencies and phases
4. Each phase implemented iteratively with AI assistance
5. End-to-end testing revealed issues that were debugged and fixed collaboratively

---

## Most Significant Prompts

1. **"Analyze the requirement and design a spec-driven approach"** — Led to creation of requirements.md, design.md, and tasks.md with detailed module structure, BPMN flow, API contracts, and design patterns.

2. **"Include high level design and low level design considering java design patterns as well as spring boot microservices design pattern with performance, resilience and recovery topmost priority"** — Produced the 5-layer resilience stack (Process → Application → Transaction → Thread → Infrastructure), circuit breaker config, exponential backoff strategy, and Template Method worker pattern.

3. **"Proceed with task list"** — Triggered the full implementation from Phase 0 through Phase 8.

4. **"Start with phase 6 angular"** — Generated the complete Angular 22 frontend with standalone components, signals-based state, Angular Material, and lazy-loaded routes.

5. **"I want login form since it has credit officer approves or rejects"** — Led to implementing AuthService, LoginComponent, AuthGuard, RoleGuard, and role-based tab visibility.

6. **Various debugging prompts during E2E testing** — "Test A after creating order it status shows validating", "CORS issue", "503 Service Unavailable", "BPMN parse errors" — all led to iterative fixes of the deployed system.

---

## AI Output — Accepted, Modified, and Rejected

### Accepted As-Is
- **Gradle multi-module structure** (buildSrc convention plugins, libs.versions.toml) — standard best practice.
- **BaseExternalTaskWorker (Template Method)** — clean separation of skeleton from concrete logic.
- **CamundaRestClient with Resilience4j** — Circuit breaker + retry pattern.
- **Angular 22 component structure** — standalone components with signals, lazy-loaded routes.
- **Docker Compose with health checks** — standard multi-service orchestration.
- **GlobalExceptionHandler** — Chain of Responsibility for consistent error responses.
- **Login form with AuthService** — sessionStorage-based auth with role-based routing.

### Modified During Testing

| Issue Found | AI Initial Approach | Fix Applied | Reason |
|-------------|-------------------|-------------|--------|
| Parallel gateway deadlock | Used AND gateway with 2 incoming flows (auto-approved + credit-approved) | Added XOR merge gateway before parallel split | Parallel GW waits for ALL incoming tokens; XOR merge allows single token through |
| BPMN boundary events | Included `<outgoing>` elements inside boundary events | Removed `<outgoing>` from boundary events | Camunda 7 BPMN XSD doesn't allow `<outgoing>` on boundary events; sequence flow `sourceRef` handles routing |
| BPMN no diagram in Cockpit | BPMN file had no `<bpmndi:BPMNDiagram>` section | Added complete BPMNDiagram with coordinates | Camunda Cockpit requires DI elements to render visual diagram |
| historyTimeToLive missing | Process element had no TTL attribute | Added `camunda:historyTimeToLive="180"` | Camunda 7.21 enforces TTL for history cleanup |
| BPMN deployment (reactive-streams) | Used `MultipartBodyBuilder` (requires spring-webflux) | Switched to `RestTemplate` with `LinkedMultiValueMap` | Servlet-stack app; avoided adding webflux dependency |
| Orders stuck at VALIDATING | Only ValidateOrderWorker updated status; other workers didn't advance it | ReserveInventoryWorker sets AUTO_APPROVED; SendEmailWorker/SendSmsWorker set FULFILLED | Each worker must advance DB status at its execution point |
| CORS blocked by Spring Security | WebConfig CORS was defined but Security intercepted preflight | Added `.cors(Customizer.withDefaults())` to SecurityFilterChain | Security must explicitly delegate CORS handling before auth |
| 401 from Angular frontend | No auth headers sent with HTTP calls | Added AuthInterceptor attaching Basic Auth from AuthService credentials | Frontend needs to send credentials with every API call |
| `-parameters` compiler flag missing | Gradle convention plugin didn't include it | Added `compilerArgs.add('-parameters')` to common.gradle | Spring needs parameter names for @PathVariable resolution at runtime |
| `@angular/animations` version mismatch | npm installed v21 instead of v22 | Fixed package.json to `^22.0.0`, regenerated lockfile | All @angular/* packages must be same major version |
| ValidateOrderWorker status for large orders | Set VALIDATING for all valid orders | Set PENDING_OVERRIDE when amount > 1000 | CreditTaskService.completeTask() transitions from PENDING_OVERRIDE → APPROVED; VALIDATING → APPROVED isn't valid in the state machine |
| `-parameters` compiler flag missing | Gradle convention plugin didn't include it | Added `compilerArgs.add('-parameters')` to common.gradle | Spring needs parameter names for @PathVariable/@RequestBody resolution at runtime |
| Message correlation using `correlationKeys` | Used `correlationKeys` in REST API body | Changed to `processVariables` | `correlationKeys` matches execution-local keys; `processVariables` matches process instance variables where `orderId` lives |
| UpdateSlaLogWorker self-transition | Worker called updateStatus(PENDING_OVERRIDE → PENDING_OVERRIDE) | Removed status change; only updates decisionReason field | Self-transitions aren't allowed in the state machine; SLA log is a side-effect, not a state change |
| AUTO_CANCELLED never set | Timer boundary → End Event (no worker in between) | Added AutoCancelWorker (topic: auto-cancel-order) as external task between timer and end event | Without a worker on the timer path, the local DB was never updated when the process auto-cancelled in Camunda |
| Message correlation ambiguity (multiple instances) | Used `processVariables` which matches ALL instances with that orderId | Changed to `processInstanceId` for direct targeting | When multiple large orders are pending simultaneously, `processVariables` finds 2+ matches and fails. `processInstanceId` is unique and unambiguous |

### Rejected
- **NgRx state management** — Not needed for this scope; Angular Signals + RxJS is sufficient.
- **WebSocket/SSE for real-time updates** — Spec says manual refresh is fine; unnecessary complexity.
- **Embedded Camunda engine** — Explicitly prohibited by requirements.
- **SendEmailWorker adding OrderService dependency (first attempt)** — Initially rejected to keep it pure; later re-added when E2E testing showed orders never reaching FULFILLED status for EMAIL-only orders.

---

## Key Debugging Sessions

### 1. Parallel Gateway Deadlock
**Symptom:** Orders stuck at `gw_parallel_split` in Camunda; never advancing to inventory/invoice workers.
**Root Cause:** Parallel gateway had 2 incoming flows (auto-approved + credit-approved). When only one path fired (auto-approved), the gateway waited forever for the second token.
**Fix:** Inserted an exclusive (XOR) merge gateway between the credit decision paths and the parallel split.
**Learning:** Parallel gateways are join points by default — they wait for ALL incoming sequence flows.

### 2. BPMN Schema Validation Failures
**Symptom:** `ENGINE-09005 Could not parse BPMN process` on deployment.
**Root Cause:** Two issues: (a) `<outgoing>` elements inside `<boundaryEvent>` which isn't allowed by the XSD, (b) missing `camunda:historyTimeToLive` on the process element.
**Fix:** Removed outgoing elements from boundary events; added historyTimeToLive="180".
**Learning:** Camunda 7.21+ enforces TTL strictly. Boundary events route via sequenceFlow sourceRef, not internal outgoing elements.

### 3. Worker Status Update Strategy
**Symptom:** Camunda process completed successfully (all 3 instances showed COMPLETED state), but local DB showed VALIDATING for all orders.
**Root Cause:** After ValidateOrderWorker set VALIDATING, no subsequent worker updated the status further. The Camunda process and local DB status were out of sync.
**Fix:** Assigned clear status update responsibility to each worker in the chain: ReserveInventory → AUTO_APPROVED, SendEmail/SendSms → FULFILLED.
**Learning:** When using dual state (DB + process engine), each worker must advance the local state at its execution point.

### 4. Credit Override Status Transition Error
**Symptom:** Officer clicking Approve returned `409: Invalid order status transition: VALIDATING -> APPROVED`.
**Root Cause:** `ValidateOrderWorker` set `VALIDATING` for ALL valid orders regardless of amount. When a large order (>1000) went to the credit override user task, the DB status stayed at `VALIDATING`. When the officer approved, `CreditTaskService` tried `VALIDATING → APPROVED` which wasn't in the valid transitions map (only `PENDING_OVERRIDE → APPROVED` is valid).
**Fix:** Updated `ValidateOrderWorker` to set `PENDING_OVERRIDE` directly when amount > 1000, so the state machine transition `PENDING_OVERRIDE → APPROVED` is valid when the officer completes the task.
**Learning:** The worker that determines the routing path must also set the corresponding DB status, not just the Camunda process variable.

### 5. Message Correlation Failure (CustomerUpdatedPriority)
**Symptom:** Clicking "Update Priority" returned `400: Cannot correlate message 'CustomerUpdatedPriority': No process definition or execution matches the parameters`.
**Root Cause:** The Camunda REST API `/message` endpoint was called with `correlationKeys` — which matches against execution-local correlation keys (business key). But `orderId` is a **process variable** set at process start, not a correlation key. Camunda couldn't find any execution with a matching correlation key.
**Fix:** Changed `correlationKeys` to `processVariables` in the REST body. The `processVariables` field instructs Camunda to find a process instance where the variable `orderId` matches the given value.
**Learning:** Camunda message correlation has three matching strategies: `businessKey`, `correlationKeys` (execution-local), and `processVariables` (instance-level). For variables set at process start, use `processVariables`.

### 6. UpdateSlaLogWorker Self-Transition Error
**Symptom:** After clicking "Update Priority", the SLA worker executed successfully but then failed with `Invalid order status transition: PENDING_OVERRIDE -> PENDING_OVERRIDE`.
**Root Cause:** The worker called `orderService.updateStatus(orderId, PENDING_OVERRIDE, slaNote)` — attempting to transition to the same status the order was already in. The state machine correctly rejects self-transitions (they're not in the valid transitions map).
**Fix:** Changed the worker to only update the `decisionReason` field on the Order entity without calling `updateStatus()`. The SLA log is a side-effect — it records that a priority update happened, but doesn't change the order's lifecycle state.
**Learning:** Non-interrupting boundary events produce side-effects, not state transitions. Workers triggered by non-interrupting events should avoid calling the state machine.

### 7. AUTO_CANCELLED Status Never Set
**Symptom:** After waiting 2+ minutes without approving a large order, the status stayed at PENDING_OVERRIDE instead of changing to AUTO_CANCELLED.
**Root Cause:** The BPMN had `Timer Boundary → End Event (Auto-Cancel Order)` with no service task in between. When the timer fired, Camunda ended the process instance correctly, but no external task worker existed to update the local database.
**Fix:** Inserted an `AutoCancelWorker` (external task, topic: `auto-cancel-order`) between the timer boundary event and the end event. This worker sets the order status to AUTO_CANCELLED in the local DB before the process ends.
**Learning:** Every BPMN path that affects local database state needs a corresponding external task worker. End events alone don't trigger any application code — they just terminate the process instance in the engine.

### 8. Message Correlation Ambiguity (Multiple Pending Orders)
**Symptom:** Clicking "Update Priority" when multiple large orders are pending returned `400: Cannot correlate a message with name 'CustomerUpdatedPriority' to a single execution. 2 executions match the correlation keys`.
**Root Cause:** The `processVariables` correlation strategy searches ALL running process instances for a matching variable. When multiple orders are at the User Task (both have the message boundary event active), Camunda finds multiple matches and rejects the correlation as ambiguous.
**Fix:** Changed from `processVariables` to `processInstanceId` in the correlation REST body. The `processInstanceId` (stored in the local Order entity) uniquely identifies the exact process instance, eliminating ambiguity regardless of how many orders are pending.
**Learning:** For message correlation in multi-instance scenarios, always use `processInstanceId` (most specific) over `processVariables` (searches all instances). The `processInstanceId` approach is O(1) lookup vs O(n) scan + ambiguity check.

---

## Angular Signals Usage in This Project

Angular Signals are used as the primary state management approach (replacing NgRx/BehaviorSubject):

### 1. Component State (`signal()`)
```typescript
// OrdersListComponent
orders = signal<OrderResponse[]>([]);   // reactive list
loading = signal(false);                 // loading indicator

// Template reads signals as function calls:
// @if (loading()) { ... }
// [dataSource]="orders()"
```

### 2. Computed Signals (`computed()`)
```typescript
// AuthService — derived state auto-updates when source changes
private currentUser = signal<AuthUser | null>(null);
isLoggedIn = computed(() => this.currentUser() !== null);
role = computed(() => this.currentUser()?.role ?? null);

// StatusBadgeComponent — colour derives from status input
color = computed(() => STATUS_COLORS[this.status()] ?? '#757575');
```

### 3. Signal-Based Inputs (`input()`)
```typescript
// StatusBadgeComponent — Angular 22 replacement for @Input() decorator
status = input.required<OrderStatus>();
// Read as: this.status() — reactive by default
```

### 4. Signals as Lightweight Reactive Store
```typescript
// AuthService acts as a mini-store without NgRx
login() → this.currentUser.set(user);   // triggers all computed signals
logout() → this.currentUser.set(null);   // isLoggedIn/role/credentials all update
```

**Why Signals over RxJS BehaviorSubject:** Simpler syntax, no manual unsubscribe, automatic dependency tracking via `computed()`, and Angular 22's idiomatic approach for synchronous state. RxJS is still used for async HTTP calls — results are stored in signals for template consumption.

---

## Confidence Level

I can explain and defend every architectural decision, BPMN model choice, gateway condition, and code pattern in this solution. The AI accelerated implementation significantly, but all design choices were reviewed, tested end-to-end, and issues were debugged collaboratively. The iterative fix-and-test cycle (BPMN schema fixes, gateway deadlock, status update strategy, CORS, auth) demonstrates active ownership of the AI-generated code.

---

## Time Breakdown (Approximate)

| Phase | Estimated Time | AI Contribution |
|-------|---------------|-----------------|
| Spec creation (requirements, design, tasks) | 1 hour | High — generated structure, I reviewed and directed |
| Backend implementation (Phases 0-4) | 2 hours | High — generated all code, I reviewed patterns |
| Angular frontend (Phase 6) | 1 hour | High — scaffolded and implemented all components |
| Docker + documentation (Phases 7-8) | 30 min | High — generated configs and docs |
| Unit tests (Phase 5) | 30 min | High — generated tests, fixed Mockito issues |
| E2E testing + debugging (Phase 9) | 2 hours | Collaborative — AI diagnosed issues from logs, I validated fixes |
| Login form + role-based UI | 30 min | High — generated complete auth flow |
| **Total** | **~7.5 hours** | — |
