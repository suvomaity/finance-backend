# Finance Data Processing & Access Control Backend

A backend API for a finance dashboard system with role-based access control, financial records management, audit trails, data reconciliation, cash forecasting, and anomaly detection.

Built with **Java 17**, **Spring Boot 3.2.5**, **PostgreSQL**, and **Spring Security**.

## Architecture

```
Controller (HTTP)  →  Service (Business logic)  →  Repository (Data access)  →  Entity (Data model)
```

- **Controllers** handle HTTP requests/responses only
- **Services** contain all business rules, validation, and audit logging
- **Repositories** use Spring Data JPA with Specifications for dynamic filtering and native queries for dashboard aggregations
- **DTOs** decouple API contracts from internal entity structure
- **Global exception handling** via `@RestControllerAdvice` ensures consistent error responses

## Tech Stack

| Component        | Technology                     | Rationale                                                          |
|------------------|--------------------------------|--------------------------------------------------------------------|
| Runtime          | Java 17                        | LTS, strong type safety, enterprise standard                      |
| Framework        | Spring Boot 3.2.5              | Production-grade, industry standard for backend APIs               |
| Database         | PostgreSQL                     | ACID transactions, relational integrity, native aggregation        |
| Auth             | JWT + Spring Security          | Stateless authentication with declarative `@PreAuthorize` RBAC     |
| Validation       | Bean Validation (Jakarta)      | Compile-time contracts with `@NotNull`, `@DecimalMin`, `@Email`    |
| Money            | BigDecimal (precision 15,2)    | No floating-point errors in financial calculations                 |
| API Docs         | SpringDoc OpenAPI (Swagger UI) | Interactive API testing at `/swagger-ui.html`                      |
| Rate Limiting    | In-memory (ConcurrentHashMap)  | 100 req/min per IP, no external dependencies                       |
| Soft Delete      | `@SQLRestriction`              | Auto-filters deleted records from all queries                      |
| Testing          | JUnit 5 + Mockito              | Unit tests for services with mocked repositories                   |

## Setup Instructions

### Prerequisites
- Java 17+
- PostgreSQL 14+
- Maven 3.8+

### Steps

```bash
git clone <repo-url>
cd finance-backend
```

Create the database:
```sql
CREATE DATABASE finance_db;
```

Configure your PostgreSQL credentials in `src/main/resources/application.yml`. A template is available at `src/main/resources/application.yml.example`.

```bash
./mvnw spring-boot:run
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Run tests:
```bash
./mvnw test
```

## Seed Data

The application seeds 3 users and 20 sample transactions on first startup.

| Email               | Password     | Role    |
|---------------------|-------------|---------|
| admin@zorvyn.com    | admin123    | ADMIN   |
| analyst@zorvyn.com  | analyst123  | ANALYST |
| viewer@zorvyn.com   | viewer123   | VIEWER  |

Login at `POST /api/auth/login`, copy the JWT, use it as `Authorization: Bearer <token>`.

## API Endpoints

### Authentication (Public)
| Method | Endpoint             | Description                                    |
|--------|----------------------|------------------------------------------------|
| POST   | `/api/auth/register` | Register new user (always assigned VIEWER role) |
| POST   | `/api/auth/login`    | Login and receive JWT                          |

### User Management (Admin only)
| Method | Endpoint                  | Description                  |
|--------|---------------------------|------------------------------|
| GET    | `/api/users`              | List all users               |
| GET    | `/api/users/{id}`         | Get user details             |
| PATCH  | `/api/users/{id}/role`    | Update user role             |
| PATCH  | `/api/users/{id}/status`  | Activate / deactivate user   |
| DELETE | `/api/users/{id}`         | Deactivate user (can't self-delete) |

### Transactions
| Method | Endpoint                          | Access    | Description                            |
|--------|-----------------------------------|-----------|----------------------------------------|
| POST   | `/api/transactions`               | Admin     | Create a new transaction               |
| GET    | `/api/transactions`               | All roles | List with filters, search & pagination |
| GET    | `/api/transactions/{id}`          | All roles | Get single transaction                 |
| PUT    | `/api/transactions/{id}`          | Admin     | Update a transaction                   |
| DELETE | `/api/transactions/{id}`          | Admin     | Soft delete a transaction              |
| PATCH  | `/api/transactions/{id}/recover`  | Admin     | Recover a soft-deleted transaction     |
| GET    | `/api/transactions/export`        | Admin     | Export transactions as CSV             |

**Query parameters for `GET /api/transactions`:**
- `type` — INCOME or EXPENSE
- `category` — filter by category name
- `startDate`, `endDate` — date range (ISO: 2026-01-01)
- `keyword` — search in description and category
- `page`, `size` — pagination (defaults: 0, 20)
- `sortBy`, `sortDir` — sorting (defaults: date, desc)

### Dashboard & Analytics
| Method | Endpoint                        | Access          | Description                                       |
|--------|---------------------------------|-----------------|---------------------------------------------------|
| GET    | `/api/dashboard/summary`        | Analyst, Admin  | Total income, expenses, net balance               |
| GET    | `/api/dashboard/categories`     | Analyst, Admin  | Category-wise breakdown                           |
| GET    | `/api/dashboard/trends`         | Analyst, Admin  | Monthly income/expense trends                     |
| GET    | `/api/dashboard/recent`         | All roles       | 10 most recent transactions                       |
| GET    | `/api/dashboard/reconciliation` | Analyst, Admin  | Data integrity check — income - expenses = balance|
| GET    | `/api/dashboard/forecast`       | Analyst, Admin  | 3-month cash forecast based on recent trends      |
| GET    | `/api/dashboard/anomalies`      | Admin           | Expenses exceeding 2x the average                 |
| GET    | `/api/dashboard/audit-logs`     | Admin           | View audit trail (paginated)                      |

## Access Control Matrix

| Action                          | Viewer | Analyst | Admin |
|---------------------------------|--------|---------|-------|
| View transactions               | ✅     | ✅      | ✅    |
| Filter / search / paginate      | ✅     | ✅      | ✅    |
| View recent activity            | ✅     | ✅      | ✅    |
| View summary & trends           | ❌     | ✅      | ✅    |
| View category breakdown         | ❌     | ✅      | ✅    |
| Cash forecast                   | ❌     | ✅      | ✅    |
| Data reconciliation             | ❌     | ✅      | ✅    |
| Create / update / delete records| ❌     | ❌      | ✅    |
| Recover soft-deleted records    | ❌     | ❌      | ✅    |
| Export CSV                      | ❌     | ❌      | ✅    |
| View anomalies                  | ❌     | ❌      | ✅    |
| Manage users & roles            | ❌     | ❌      | ✅    |
| Deactivate users                | ❌     | ❌      | ✅    |
| View audit logs                 | ❌     | ❌      | ✅    |

## Design Decisions

1. **BigDecimal for monetary amounts** — Avoids floating-point precision errors. Using `double` for money is a known source of bugs in fintech systems.

2. **PostgreSQL over NoSQL** — Financial data needs ACID transactions and relational integrity. Native aggregate functions (`SUM`, `DATE_TRUNC`, `GROUP BY`) make dashboard queries efficient without loading data into memory.

3. **Soft delete with recovery** — Financial records are never permanently removed. `@SQLRestriction("is_deleted = false")` auto-filters deleted records while preserving them for compliance. Admins can recover accidentally deleted records.

4. **Audit logging** — Every create, update, delete, and recover operation is recorded with who performed it, what changed, and when. Essential for regulatory traceability in financial systems.

5. **Data reconciliation** — The `/reconciliation` endpoint independently computes income and expenses, then cross-checks against a combined calculation. This verifies data integrity — a core concept in financial systems.

6. **Cash forecasting** — Averages the last 3 months of income/expenses and projects 3 months forward with running balances. Demonstrates financial projection logic beyond basic CRUD.

7. **Anomaly detection** — Flags expense transactions exceeding 2x the average expense amount. Returns the deviation factor for each flagged transaction. Simple but effective for spotting unusual activity.

8. **Secure registration** — Self-registration always assigns VIEWER role regardless of what's sent in the request body. Role elevation requires admin action via `PATCH /api/users/{id}/role`. Prevents privilege escalation.

9. **Admin self-delete protection** — Admins cannot deactivate their own account, preventing accidental lockout.

10. **JPA Specifications for filtering** — Instead of hardcoded if/else query combinations, filters are composed dynamically using `Specification<Transaction>`. Adding new filters requires zero changes to the repository layer.

11. **CSV export** — Simple one-endpoint implementation that returns a downloadable CSV file with proper headers. No external library needed — just StringBuilder.

12. **Consistent API response format** — All endpoints return `ApiResponse<T>` with `success`, `message`, `data`, and `timestamp`. Error responses follow the same structure.

13. **In-memory rate limiting** — 100 requests/minute per IP using `ConcurrentHashMap`. No Redis dependency for an assessment-scope project.

14. **JWT stateless authentication** — Role embedded in token claims. `@PreAuthorize` handles access control declaratively.

## Testing

The test suite includes **15 unit tests** covering:

- Transaction CRUD (create, read, update, soft delete)
- Soft delete recovery (recover deleted, reject recovering active)
- CSV export format validation
- Audit logging on every mutation
- Dashboard summary calculation (correct totals, empty data handling)
- Reconciliation balance verification
- Cash forecast (3-month projection structure)
- Anomaly detection (empty when no expenses)

```bash
./mvnw test
```

## Project Structure

```
src/main/java/com/zorvyn/finance/
├── FinanceApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── RateLimitConfig.java
│   └── OpenApiConfig.java
├── entity/
│   ├── User.java
│   ├── Transaction.java
│   ├── AuditLog.java
│   └── enums/
├── repository/
│   ├── UserRepository.java
│   ├── TransactionRepository.java
│   └── AuditLogRepository.java
├── dto/
│   ├── ApiResponse.java
│   ├── AuthResponse.java
│   ├── DashboardSummary.java
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   └── TransactionRequest.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── TransactionService.java
│   └── DashboardService.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── TransactionController.java
│   └── DashboardController.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
└── exception/
    ├── GlobalExceptionHandler.java
    └── ResourceNotFoundException.java
```
