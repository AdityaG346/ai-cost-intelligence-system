# AI Cost Intelligence System — Copilot Instructions

## Architecture Overview

**Multi-agent system** that detects enterprise financial inefficiencies and takes automated corrective actions. Core pipeline: **DETECT → ANALYZE → ACT → APPROVE → SAVE**.

### Component Structure
- **4 autonomous agents** (`src/main/java/com/costintel/agents/`): SpendIntelligenceAgent, SLAMonitoringAgent, ResourceOptimizationAgent, FinancialOperationsAgent
- **WorkflowEngine** (`service/`): Orchestrates agents sequentially through the detection pipeline
- **Spring Boot backend** (`ApiController`): REST endpoints expose pipeline results
- **Vanilla JS dashboard** (`frontend/app.js`): Consumes `/api/analyze` and `/api/*` endpoints
- **Services**: DataService (in-memory storage), ApprovalService (approval queue), AuditLogger (comprehensive event logging)

### Key Design Decisions
1. **Interface-based agent design**: All agents implement `Agent` interface (4 methods: `getName()`, `detect()`, `analyze()`, `act()`, optional `generatePlaybooks()`)
2. **Risk-based approval gating**: Actions with savings > ₹5,000 auto-escalate to `PENDING_APPROVAL` status; WorkflowEngine creates ApprovalRequest records
3. **Quantified financial impact**: Every issue type has specific savings formulas (e.g., duplicate payment = 100% recovery, SLA breach = 10% of contract value)
4. **No external AI libraries**: Agents use rule-based detection (thresholds, statistical averages, heuristics), not ML models

## Code Patterns

### Agent Implementation
Each agent works the same way:
```java
List<Issue> issues = agent.detect(transactions);           // Pattern matching on Transaction fields
List<Issue> analyzed = agent.analyze(issues);              // Filter/re-prioritize by severity
List<ActionItem> actions = agent.act(analyzed);            // Generate ActionItems (status: EXECUTED or PENDING_APPROVAL)
List<Playbook> playbooks = agent.generatePlaybooks(issues); // 3-7 step remediation guides with costMath
```

### Issue & Action Lifecycle
- **Issue** = detected problem (DUPLICATE_PAYMENT, SPENDING_ANOMALY, SLA_BREACH, etc. — by agent-specific types)
- **ActionItem** = corrective action (statuses: EXECUTED, PENDING_APPROVAL, PENDING_REVIEW, IN_PROGRESS)
- **ApprovalRequest** = queued high-risk action (auto-created by WorkflowEngine for status==PENDING_APPROVAL)
- **Playbook** = step-by-step guide with cost calculations (triggerIssueId links to Issue)

### Field Naming & Conventions
- Monetary values: `double` type, stored in Indian Rupees (₹), no currency symbol in code
- IDs: Format like `SI-1` (agent prefix + counter) for issues, `APR-1` for approvals, `PB-SI-1` for playbooks
- Dates: String format `yyyy-MM-dd`
- Timestamps: String format `yyyy-MM-dd HH:mm:ss` (DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
- Collections: Use `LinkedHashMap` for maintaining insertion order; `Collections.synchronizedList` for thread-safe audit logs

## API & Frontend Integration

### Frontend Workflow
1. User clicks "Run Analysis" → `POST /api/analyze`
2. WorkflowEngine runs full pipeline, returns JSON with: `totalIssues`, `totalActions`, `totalPlaybooks`, `pendingApprovals`, `impact{totalSavings, yearlySavings, wasteBreakdown}`, `agentActivity`, `auditLog`
3. Dashboard renders KPI cards, charts, approval queue (with Approve/Reject buttons → `/api/approvals/{id}/approve|reject`)

### Data Flow
- Frontend stateless; all state in Spring Boot services
- Dashboard auto-updates on `/api/analyze` response
- Approval actions update via POST, dashboard re-fetches from `/api/approvals`

## Build & Run

### Commands
```bash
# Build & run backend (Java 17+ required)
./mvnw spring-boot:run           # Unix/Mac
mvnw.cmd spring-boot:run          # Windows
# Server on http://localhost:8080

# Frontend: Open frontend/index.html in browser (or serve via any static server)
```

### Configuration
- `application.properties`: `server.port=8080` (configurable via `PORT` env var)
- CORS enabled for all origins (`*`) in WebConfig
- Static assets in `src/main/resources/static/` served automatically

## Agent-Specific Detection Logic

| Agent | Key Fields Scanned | Detection Examples | Status Assignment |
|---|---|---|---|
| **Spend Intelligence** | vendor, amount, date, category | Duplicate (same vendor+amount+date), Anomaly (>2× vendor avg), Recurring (≥3 payments), Rate (max>1.5× min) | Depends on type (DUPLICATE→EXECUTED, RATE→PENDING_APPROVAL) |
| **SLA Monitoring** | deadline, resourceUsage, amount | Breach (past deadline), At-Risk (≤3 days), Resource Risk (≤7 days AND <50% usage) | ESCALATE→EXECUTED, SHIFT→IN_PROGRESS, ALLOCATE→PENDING_APPROVAL |
| **Resource Optimization** | resourceUsage, amount, category | Underutilized (<40% usage), Consolidation (multiple underutilized) | <₹5K→EXECUTED, ≥₹5K→PENDING_APPROVAL |
| **Financial Operations** | expectedAmount, actualAmount, vendor | Mismatch (expected≠actual), Systemic Overbilling (>3% variance across ≥2 txns) | <₹3K→EXECUTED, ≥₹3K→PENDING_APPROVAL |

## Critical Workflows

### Adding a New Detection Algorithm
1. Open agent file (e.g., `SpendIntelligenceAgent.java`)
2. In `detect()` method: Add loop/analysis logic → create Issue objects with `id`, `type`, `severity`, `description`, `agent`, `potentialSavings`
3. In `analyze()` method: Filter/re-sort by severity; return filtered list
4. In `act()` method: Loop issues → create ActionItems with `type`, `status` (auto-escalation logic), `savings`
5. Override `generatePlaybooks()` if applicable: Create Playbook with `steps` (List<String>) and `costMath` string
6. Test: Run `./mvnw spring-boot:run`, hit `/api/analyze`, validate issue appears

### Modifying Thresholds
Edit agent detect logic directly (e.g., `> 2 * avg` for spending anomaly, `<= 3` days for SLA at-risk).

### Dashboard Customization
Profile `frontend/app.js`:
- Fetch data: Line 1-50 (API calls to `/api/*` endpoints)
- Rendering: Lines 50-300 (DOM updates with Chart.js charts)
- Approval flow: Lines 300-400 (POST requests to `/api/approvals/{id}/approve|reject`)
- **Currency formatting**: Uses Indian Lakh system (`₹X.XXL` and `₹X,XXX` conversions)

## Sample Data & Testing

**25 transactions** in SampleDataLoader designed to trigger all detection types:
- TXN-001–003: Baseline (reference for averages)
- TXN-004–007: Duplicates (Spend agent)
- TXN-008–011: SLA issues (SLA agent)
- TXN-012–014, TXN-025: Underutilized (Resource agent)
- TXN-015–017: Financial mismatches (Financial agent)
- TXN-019–024: Anomalies, recurring, rate optimization (Spend agent)

No external data loading; system uses in-memory SampleDataLoader on startup.

## Approval & Audit Patterns

### Approval Logic
WorkflowEngine auto-creates ApprovalRequest for actions with `status==PENDING_APPROVAL`. Risk level determined by savings: `HIGH` if >₹5K, else `MEDIUM`. Link to playbook via `findPlaybookForIssue()`. Only `PENDING` requests can be approved/rejected (idempotent).

### Audit Trail
AuditLogger is thread-safe; logs every step (data load, detect, analyze, act, playbook gen, approval decision). Access via `/api/audit` endpoint.
