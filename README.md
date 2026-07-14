# Order Fulfilment Workflow

Full-stack order fulfilment application processing customer orders through a **Camunda 7 standalone** BPMN workflow engine.

**Tech Stack:** Spring Boot 3.3 (Java 21) · Camunda 7 (standalone) · Angular 22 · H2 · Gradle 8

---

## Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Node.js 24.15+** (for Angular 22)
- **Docker & Docker Compose** (for running Camunda engine)
- **Gradle 8.10** (via included wrapper)

---

## Quick Start (Docker Compose)

```bash
docker compose up --build
```

This starts all three services:

| Service | URL | Description |
|---------|-----|-------------|
| Camunda Engine | http://localhost:8081 | Standalone BPM engine (Cockpit: admin/admin) |
| Backend | http://localhost:8080 | Spring Boot REST API |
| Frontend | http://localhost:4200 | Angular 22 UI |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |

---

## One-Time BPMN Deployment

After the first `docker compose up`, deploy the BPMN process to Camunda:

```bash
curl -X POST http://localhost:8081/engine-rest/deployment/create \
  -F "deployment-name=Order Fulfilment Process" \
  -F "deploy-changed-only=true" \
  -F "data=@backend/app/src/main/resources/processes/order-fulfilment.bpmn"
```

Verify deployment:
```bash
curl -s http://localhost:8081/engine-rest/process-definition | python3 -m json.tool
```

---

## Manual Start (3 terminals)

### Terminal 1: Camunda Engine
```bash
docker run -d --name camunda -p 8081:8080 camunda/camunda-bpm-platform:run-7.21.0
```

### Terminal 2: Backend
```bash
cd backend
./setup-wrapper.sh          # one-time: downloads gradle-wrapper.jar
./gradlew :app:bootRun
```

### Terminal 3: Frontend
```bash
cd frontend
npm install
npx ng serve
```

---

## Login Credentials

| Username | Password | Role | Access |
|----------|----------|------|--------|
| customer | customer123 | CUSTOMER | Submit orders, view orders, send priority message |
| officer | officer123 | CREDIT_OFFICER | Review and approve/reject credit overrides |
| admin | admin123 | Both roles | All endpoints (for API testing) |

---

## Step-by-Step Testing Guide

### Step 1: Verify Services Are Healthy

```bash
# Backend health
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Camunda engine
curl -s http://localhost:8081/engine-rest/engine | python3 -m json.tool

# Frontend (returns HTML)
curl -s http://localhost:4200 | head -5
```

---

### Step 2: Test via Angular UI

1. Open **http://localhost:4200** in your browser
2. You'll see the **Login** screen with demo credentials displayed

#### As Customer:
1. Log in as `customer` / `customer123`
2. You see two tabs: **Orders** and **New Order**
3. Go to **New Order** tab
4. Fill in: Customer ID = `CUST-001`, Name = `Alice Smith`, Amount = `500`, Preference = `EMAIL`
5. Click **Submit Order**
6. Go to **Orders** tab — you should see the order with status progressing to `FULFILLED`

#### As Credit Officer:
1. Log out (click logout icon in toolbar)
2. Log in as `officer` / `officer123`
3. You see one tab: **Credit Review**
4. Any orders with amount > 1000 will appear here as pending overrides
5. Enter a comment and click **Approve** or **Reject**

---

### Step 3: Happy Path — Small Order (auto-approved → fulfilled)

```bash
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","customerName":"Alice Smith","amount":500,"notificationPreference":"EMAIL"}'
```

Wait 5 seconds, then check status:

```bash
curl -s -u customer:customer123 http://localhost:8080/api/orders | python3 -m json.tool
```

**Expected:** Order status = `FULFILLED` (path: RECEIVED → VALIDATING → auto-approved → Reserve Inventory + Generate Invoice → Send Email → FULFILLED)

---

### Step 4: Credit Override Path — Large Order (amount > 1000)

```bash
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-002","customerName":"Bob Jones","amount":5000,"notificationPreference":"BOTH"}'
```

Wait 3 seconds, verify it's at `PENDING_OVERRIDE`:

```bash
curl -s -u customer:customer123 http://localhost:8080/api/orders | python3 -m json.tool
```

Get pending credit tasks:

```bash
curl -s -u officer:officer123 http://localhost:8080/api/tasks/credit-override | python3 -m json.tool
```

Approve (replace `TASK_ID` with actual taskId from above):

```bash
curl -u officer:officer123 -X POST http://localhost:8080/api/tasks/credit-override/TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{"approved":true,"comment":"Credit verified by officer"}'
```

Wait 5 seconds, verify it reached `FULFILLED`:

```bash
curl -s -u customer:customer123 http://localhost:8080/api/orders | python3 -m json.tool
```

---

### Step 5: Priority Message (non-interrupting boundary event)

```bash
# Submit a large order
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-003","customerName":"Carol White","amount":3000,"notificationPreference":"SMS"}'
```

Wait 3 seconds, get the order ID:

```bash
curl -s -u customer:customer123 http://localhost:8080/api/orders | python3 -m json.tool
```

Send priority update (replace `ORDER_ID`):

```bash
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/ORDER_ID/priority
```

**Expected:** Returns `204 No Content`. Check backend logs:

```bash
docker compose logs backend | grep "UpdateSlaLog"
```

You should see: `[UpdateSlaLog] SLA update received`. The credit override task remains pending (non-interrupting event).

In the UI: log in as `customer`, go to **Orders** tab, click **Update Priority** button on the pending order.

---

### Step 6: Timer Expiry (auto-cancel)

```bash
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-004","customerName":"Dave Timer","amount":2000,"notificationPreference":"EMAIL"}'
```

**Do NOT approve it.** Wait 2+ minutes (timer is `PT2M` in dev profile), then check:

```bash
curl -s -u customer:customer123 http://localhost:8080/api/orders | python3 -m json.tool
```

**Expected:** Order status = `AUTO_CANCELLED`

---

### Step 7: Credit Rejection

```bash
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-005","customerName":"Eve Reject","amount":8000,"notificationPreference":"SMS"}'
```

Wait 3 seconds, get the task:

```bash
curl -s -u officer:officer123 http://localhost:8080/api/tasks/credit-override | python3 -m json.tool
```

Reject (replace `TASK_ID`):

```bash
curl -u officer:officer123 -X POST http://localhost:8080/api/tasks/credit-override/TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{"approved":false,"comment":"Insufficient credit history"}'
```

**Expected:** Order status = `REJECTED`

---

### Step 8: Input Validation (bad request)

```bash
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"","customerName":"","amount":-10,"notificationPreference":"EMAIL"}'
```

**Expected:** Returns `400 Bad Request` with validation error messages.

---

### Step 9: Verify Camunda Cockpit

Open: **http://localhost:8081/camunda/app/cockpit**

Login: `admin` / `admin`

- View deployed "Order Fulfilment" process definition
- See running and completed process instances
- Inspect process variables and history

---

### Step 10: Notification Routing Test

Test all three notification preferences:

```bash
# EMAIL only
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-E","customerName":"Email Only","amount":200,"notificationPreference":"EMAIL"}'

# SMS only
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-S","customerName":"SMS Only","amount":300,"notificationPreference":"SMS"}'

# BOTH
curl -u customer:customer123 -X POST http://localhost:8080/api/orders/webhook \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-B","customerName":"Both Channels","amount":400,"notificationPreference":"BOTH"}'
```

Check backend logs for notification workers:

```bash
docker compose logs backend | grep -E "(SendEmail|SendSms)"
```

**Expected:**
- EMAIL preference → only `[SendEmail]` log
- SMS preference → only `[SendSms]` log
- BOTH preference → both `[SendEmail]` and `[SendSms]` logs

---

## API Endpoints Summary

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/orders/webhook | CUSTOMER | Submit new order |
| GET | /api/orders | CUSTOMER | List all orders |
| GET | /api/orders/{id} | CUSTOMER | Get order details |
| POST | /api/orders/{id}/priority | CUSTOMER | Send priority update message |
| GET | /api/tasks/credit-override | CREDIT_OFFICER | List pending overrides |
| POST | /api/tasks/credit-override/{taskId}/complete | CREDIT_OFFICER | Approve/reject override |

---

## Project Structure

```
order-fulfilment/
├── backend/
│   ├── buildSrc/          # Gradle convention plugins
│   ├── gradle/            # Version catalog (libs.versions.toml)
│   ├── service/           # Domain layer: entities, repos, workers, services
│   ├── api/               # REST layer: controllers, DTOs, exception handler
│   ├── app/               # Spring Boot app: config, main class, BPMN
│   └── Dockerfile
├── frontend/
│   ├── src/app/
│   │   ├── core/          # Services, models, interceptors, guards
│   │   ├── features/      # Login, NewOrder, OrdersList, CreditReview
│   │   └── shared/        # StatusBadge component
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml
├── README.md
└── AI-USAGE.md
```

---

## Order Status Flow

```
RECEIVED  ──→  VALIDATING  ──→  AUTO_APPROVED  ──→  FULFILLED
                    │                                  (amount ≤ 1000)
                    │
                    ├──→  PENDING_OVERRIDE  ──→  APPROVED  ──→  FULFILLED
                    │                                           (officer approves)
                    │
                    ├──→  PENDING_OVERRIDE  ──→  REJECTED
                    │                           (officer rejects)
                    │
                    ├──→  PENDING_OVERRIDE  ──→  AUTO_CANCELLED
                    │                           (timer expires, 2min in dev)
                    │
                    └──→  REJECTED
                         (validation failed)
```

**Worker responsibility for status updates:**
| Worker | Status Set |
|--------|-----------|
| ValidateOrderWorker | VALIDATING (valid) or REJECTED (invalid) |
| ReserveInventoryWorker | AUTO_APPROVED (if currently VALIDATING) |
| CreditTaskService.completeTask | APPROVED or REJECTED |
| SendEmailWorker | FULFILLED |
| SendSmsWorker | FULFILLED |

---

## Assumptions

1. "Known customer" = customerId is non-blank (no external lookup).
2. Inventory reservation and invoice generation are log-based simulations.
3. Email/SMS are logged, not actually sent.
4. Timer boundary default is PT10M (PT2M in dev profile for demo).
5. Single Camunda deployment version assumed.
6. Both SendEmailWorker and SendSmsWorker attempt to set FULFILLED status (idempotent — the second one silently skips if already fulfilled).

---

## BPMN Process Design Notes

The BPMN uses the following key patterns:
- **XOR (Exclusive) Gateway** for credit decision routing (auto-approve / override / reject)
- **XOR Merge Gateway** to join auto-approved and officer-approved paths into a single token before the parallel split (avoids parallel gateway waiting for multiple incoming tokens)
- **AND (Parallel) Gateway** for inventory + invoice parallel execution
- **OR (Inclusive) Gateway** for notification routing (EMAIL / SMS / BOTH)
- **Timer Boundary Event (interrupting)** on the credit override user task — auto-cancels after configurable duration
- **Message Boundary Event (non-interrupting)** for priority updates — triggers SLA log without cancelling the review

The BPMN file includes `bpmndi:BPMNDiagram` interchange data for Camunda Cockpit rendering.

---

## Known Limitations

- No real email/SMS/payment integration.
- H2 in-memory database (data lost on restart).
- Basic Auth only (no JWT/OAuth2).
- No WebSocket/SSE for real-time order status updates (manual refresh).
- Angular CLI 22 requires Node.js 24.15+ or 22.22.3+.
- Gradle wrapper JAR must be downloaded on first setup (run `setup-wrapper.sh`).
- BPMN auto-deployment on startup uses RestTemplate multipart upload; if it fails, deploy manually using the curl command above.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `503 Service Unavailable` on order submit | BPMN not deployed. Run the manual deployment curl command above. |
| `401 Unauthorized` from frontend | Not logged in. Go to http://localhost:4200/login |
| CORS error | Rebuild backend with `docker compose down && docker compose up --build` |
| Angular CLI version error | Requires Node.js 24.15+. Use `node -v` to check. |
| Gradle build fails | Ensure Java 21 is installed: `java -version` |
| Timer not expiring | Timer is PT2M in dev. Wait 2+ minutes without approving. |
| `409 Invalid status transition` on approve | Ensure you're running latest build. ValidateOrderWorker now sets PENDING_OVERRIDE for large orders. Rebuild with `docker compose up --build`. |
| `Cannot correlate message` on Update Priority | Order must be in PENDING_OVERRIDE state with an active User Task. If multiple large orders are pending, ensure you rebuilt with the latest code (uses `processInstanceId` for precise targeting). |
| `2 executions match` on Update Priority | You have multiple pending orders. Rebuild required — latest code uses `processInstanceId` instead of `processVariables`. Run `docker compose down && docker compose up --build`. |
| `Name for argument not specified` error | Rebuild required — `-parameters` compiler flag was added. Run `docker compose down && docker compose up --build`. |
