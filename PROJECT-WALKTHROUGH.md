# PG Management System — Complete Walkthrough for Interview

> **Use this document like a textbook.** Read it top to bottom once, then re-read sections you stumble on. Every key term has a "why this is here" explanation, not just "what it does", because evaluators ask `why`, not `what`.

---

## TABLE OF CONTENTS

1. [What this project does](#1-what-this-project-does)
2. [Tech stack and why](#2-tech-stack-and-why)
3. [High-level architecture](#3-high-level-architecture)
4. [Project folder structure](#4-project-folder-structure)
5. [Backend — Entity layer (data model)](#5-backend--entity-layer-data-model)
6. [Backend — Repository layer](#6-backend--repository-layer)
7. [Backend — Service layer](#7-backend--service-layer)
8. [Backend — Controller layer](#8-backend--controller-layer)
9. [Backend — Security layer (JWT)](#9-backend--security-layer-jwt)
10. [Backend — Configuration & infrastructure](#10-backend--configuration--infrastructure)
11. [Frontend — Architecture overview](#11-frontend--architecture-overview)
12. [Frontend — Services](#12-frontend--services)
13. [Frontend — Auth interceptor and guards](#13-frontend--auth-interceptor-and-guards)
14. [Frontend — Components](#14-frontend--components)
15. [End-to-end request flow examples](#15-end-to-end-request-flow-examples)
16. [Key concepts you MUST be able to explain](#16-key-concepts-you-must-be-able-to-explain)
17. [Common interview questions + sample answers](#17-common-interview-questions--sample-answers)
18. [Glossary](#18-glossary)

---

## 1. What this project does

**PG Management System** is a web application for managing **PG (Paying Guest) accommodations** / hostels. It has two types of users:

- **Owner (Manager)** — runs one or more PG properties. Manages PGs, rooms, tenants, complaints, rent, announcements.
- **Tenant (Resident)** — lives in a room. Submits complaints, raises maintenance requests, sees rent dues, requests to vacate.

### Core features

| Feature | Owner can | Tenant can |
|---|---|---|
| PGs | Create, edit, list their own PGs | — |
| Rooms | Add (single or bulk), see occupancy as `X/Y beds` | View their assigned room |
| Tenants | Assign to rooms, see active list, force-vacate | Self-register |
| Bookings/Vacate | Approve/reject vacate requests | Submit/cancel vacate request |
| Complaints | View, update status (OPEN/IN_PROGRESS/RESOLVED) | File with photo upload |
| Maintenance | Update status, assign worker | File with photo upload |
| Rent | Mark records as PAID | View own rent history |
| Announcements | Post (for all or one PG) | View |
| Notifications | View own | View own |

---

## 2. Tech stack and why

### Backend
| Tech | Version | Purpose |
|---|---|---|
| **Java** | 21 | Modern long-term-support release, used industry-wide |
| **Spring Boot** | 3.2.5 | Auto-configures everything (web server, JPA, DI). One-line server start. |
| **Spring Web** | (starter) | Builds REST APIs via `@RestController` |
| **Spring Data JPA** | (starter) | Database ORM — write Java methods, Spring writes the SQL |
| **Spring Security** | (starter) | Filters incoming requests, integrates JWT, hashes passwords |
| **Hibernate** | 6.x | The JPA implementation under the hood — talks to the DB |
| **JJWT** | 0.11.5 | Generates and validates JWT tokens |
| **Lombok** | latest | Auto-generates getters/setters/builders via annotations |
| **MySQL Connector / H2** | latest | Database drivers — H2 (default for dev) and MySQL (for prod) |

### Frontend
| Tech | Version | Purpose |
|---|---|---|
| **Angular** | 17 | Single-page application framework |
| **TypeScript** | 5.x | Compiled-to-JS language with types |
| **RxJS** | (bundled) | Reactive streams (`Observable`s) for async data |
| **HttpClient** | (Angular) | Wrapper over `fetch` for HTTP calls |

### Database
- **H2** (file-based) — embedded, no admin needed; default profile for local dev
- **MySQL 8** — for "production"; switch via Spring profile

---

## 3. High-level architecture

```
┌─────────────────────────────────────────────────────────┐
│         Browser  http://localhost:4200                  │
│         Angular SPA (TypeScript + HTML + CSS)           │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP + JSON + JWT in Authorization header
                      ▼
┌─────────────────────────────────────────────────────────┐
│   Spring Boot REST API  http://localhost:8082           │
│                                                         │
│   ┌──────────────┐                                      │
│   │  Controllers │  validate input, call services      │
│   └──────┬───────┘                                      │
│          ▼                                              │
│   ┌──────────────┐                                      │
│   │  Services    │  business logic (assign tenant,     │
│   │              │  approve vacate, etc.)              │
│   └──────┬───────┘                                      │
│          ▼                                              │
│   ┌──────────────┐                                      │
│   │ Repositories │  Spring Data JPA — auto-generated   │
│   │              │  SQL via method names               │
│   └──────┬───────┘                                      │
│          ▼                                              │
│   ┌──────────────┐                                      │
│   │  Entities    │  Java classes mapped to DB tables   │
│   │  (@Entity)   │  via JPA annotations                │
│   └──────┬───────┘                                      │
│          ▼                                              │
└──────────┼──────────────────────────────────────────────┘
           ▼
       ┌────────────────┐
       │  H2 / MySQL    │
       │   database     │
       └────────────────┘
```

**Layered architecture (separation of concerns):**

- **Controller**: receives HTTP → calls service → returns HTTP response. *No business logic here.*
- **Service**: business logic — "to assign a tenant, find the room, check capacity, save the assignment, send notification."
- **Repository**: just data access — "find room by id", "save room", "count active assignments."
- **Entity**: the data shape — fields, relationships, mapped to a DB table.

**Why layered?** If the database changes (MySQL → H2), only the repository/config layer changes. If the business logic changes, only services change. Controllers never know about SQL.

---

## 4. Project folder structure

```
pg/
├── backend/                                  ← Spring Boot project
│   ├── pom.xml                               ← Maven config + dependencies
│   ├── src/main/
│   │   ├── java/com/pgmanagement/
│   │   │   ├── PgManagementApplication.java  ← @SpringBootApplication entry point
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java       ← Spring Security filter chain
│   │   │   │   └── DataSeeder.java           ← Populates demo data on first run
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java       ← /api/auth/login, /register
│   │   │   │   ├── OwnerController.java      ← /api/owner/** (role: OWNER)
│   │   │   │   ├── TenantController.java     ← /api/tenant/** (role: TENANT)
│   │   │   │   └── FileController.java       ← /api/files/upload + serve
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java          ← Standard wrapper {success, message, data}
│   │   │   │   ├── AuthRequest.java          ← Login payload {email, password}
│   │   │   │   ├── AuthResponse.java         ← Login response {token, userId, role, ...}
│   │   │   │   └── RegisterRequest.java
│   │   │   ├── entity/
│   │   │   │   ├── User.java
│   │   │   │   ├── PG.java
│   │   │   │   ├── Room.java
│   │   │   │   ├── RoomAssignment.java
│   │   │   │   ├── RentRecord.java
│   │   │   │   ├── Complaint.java
│   │   │   │   ├── MaintenanceRequest.java
│   │   │   │   ├── Announcement.java
│   │   │   │   └── Notification.java
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java   ← @RestControllerAdvice — catches errors
│   │   │   ├── repository/
│   │   │   │   └── (one Spring Data interface per entity)
│   │   │   ├── security/
│   │   │   │   ├── JwtUtil.java                  ← Generate + validate tokens
│   │   │   │   ├── JwtAuthFilter.java            ← Runs on every request
│   │   │   │   └── CustomUserDetailsService.java ← Loads user from DB for Spring Security
│   │   │   └── service/
│   │   │       ├── AuthService.java
│   │   │       ├── OwnerService.java
│   │   │       └── TenantService.java
│   │   └── resources/
│   │       ├── application.properties             ← Shared + active profile
│   │       ├── application-h2.properties          ← H2 DB config
│   │       └── application-mysql.properties       ← MySQL DB config
│   └── uploads/                                   ← Where complaint photos are saved
│
└── frontend/                                  ← Angular project
    ├── package.json                           ← npm dependencies
    ├── angular.json                           ← Angular CLI config
    └── src/app/
        ├── app.config.ts                      ← provideHttpClient, provideRouter
        ├── app.routes.ts                      ← All route paths + which component
        ├── app.component.ts                   ← Root <router-outlet>
        ├── core/
        │   ├── services/
        │   │   ├── auth.service.ts            ← Login/logout/getRole/getToken
        │   │   └── api.service.ts             ← All HTTP calls
        │   ├── interceptors/
        │   │   └── auth.interceptor.ts        ← Auto-attaches Bearer token
        │   ├── guards/
        │   │   ├── auth.guard.ts              ← Blocks routes if no token
        │   │   └── role.guard.ts              ← Blocks if wrong role
        │   └── models/
        │       └── models.ts                  ← TypeScript interfaces
        ├── auth/
        │   ├── login/
        │   └── register/
        ├── owner/
        │   ├── dashboard/
        │   ├── pgs/
        │   ├── rooms/
        │   ├── tenants/
        │   ├── complaints/
        │   ├── maintenance/
        │   ├── announcements/
        │   └── rent/
        └── tenant/
            ├── dashboard/
            ├── my-room/
            ├── complaints/
            ├── maintenance/
            ├── rent/
            ├── announcements/
            └── notifications/
```

---

## 5. Backend — Entity layer (data model)

An **entity** is a Java class that represents a row in a database table. We mark it with `@Entity` and Hibernate handles the SQL.

### `User.java`

| Field | Type | Notes |
|---|---|---|
| id | Long | `@Id @GeneratedValue` — auto-increment primary key |
| name | String | |
| email | String | `unique=true` — no duplicates |
| password | String | BCrypt-hashed (we never store plain text) |
| role | Role enum | `OWNER` or `TENANT` |
| phone | String | |

**Key annotations:**
- `@Entity` — Hibernate sees this as a DB table
- `@Table(name = "users")` — explicit table name (because `user` is reserved in some DBs)
- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` — auto-increment
- `@Enumerated(EnumType.STRING)` — store enum as text (`"OWNER"`) instead of number, easier to read in DB
- `@JsonIgnore` on password — never include hashed password in JSON responses
- `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` — prevents Hibernate proxy fields from breaking JSON serialization

### `PG.java`

A PG (paying guest house) — owned by one User.

| Field | Type | Notes |
|---|---|---|
| id | Long | |
| owner | User | `@ManyToOne` — many PGs can belong to one owner |
| name, address, rules, amenities, phone, totalRooms | String | |

`@ManyToOne(fetch = FetchType.LAZY)` means: the User (owner) is loaded only when explicitly accessed, not every time you load the PG. This avoids unnecessary SQL queries.

### `Room.java`

A room inside a PG. Has a `capacity` (1 = single, 2 = double, etc.).

| Field | Type | Notes |
|---|---|---|
| id | Long | |
| pg | PG | `@ManyToOne` — many rooms in one PG |
| roomNumber | String | "101", "102", etc. |
| capacity | Integer | how many tenants can fit |
| rentAmount | BigDecimal | per month |
| isOccupied | Boolean | **true only when room is FULL** (active assignments == capacity) |
| type | RoomType enum | SINGLE, DOUBLE, TRIPLE, DORMITORY |
| **currentOccupancy** | Integer | `@Transient` — computed by service, not in DB |

`@Transient` tells Hibernate "don't put this in the DB". The service layer fills it in by counting active assignments.

### `RoomAssignment.java`

The link between a tenant and the room they live in. **One row per "stay"** — when a tenant leaves, `isActive` becomes `false`, the row stays for history.

| Field | Notes |
|---|---|
| tenant | the User (TENANT role) |
| room | the Room |
| joinDate, leaveDate | dates |
| isActive | true while currently living there |
| vacateRequestedAt | when tenant submitted vacate request (null if none) |
| requestedLeaveDate | when they want to leave |
| vacateReason | text |

The vacate-request fields are part of the assignment because each assignment has at most one pending vacate request — keeps it simple.

### `RentRecord.java`

One row per (tenant, month) — represents a rent obligation.

| Field | Notes |
|---|---|
| tenant, room | links |
| month | "2024-04" — string format YYYY-MM |
| amount | BigDecimal |
| status | PENDING, PAID, OVERDUE |
| dueDate, paidDate | dates |

### `Complaint.java`

| Field | Notes |
|---|---|
| tenant | who raised it |
| pg | which PG |
| title, description | |
| imageUrls | `@ElementCollection` — list of URLs in a separate `complaint_images` table |
| status | OPEN, IN_PROGRESS, RESOLVED, REJECTED |
| ownerNote | the owner's response |
| createdAt, resolvedAt | timestamps |

`@ElementCollection` is JPA's way of storing a `List<String>` — it creates a child table automatically.

### `MaintenanceRequest.java`

Like Complaint but for maintenance issues (broken fan, leaking pipe).
Has extra: `priority` (LOW/MEDIUM/HIGH/URGENT), `assignedTo` (which worker).

### `Announcement.java`

Owner posts these to one PG (or all). Has `priority`.

### `Notification.java`

Per-user message delivered in-app.

| Field | Notes |
|---|---|
| user | who receives it |
| message | text |
| type | "COMPLAINT", "RENT", "VACATE", etc. |
| isRead | toggle |
| createdAt | |

---

## 6. Backend — Repository layer

A **repository** is an interface that extends `JpaRepository<Entity, ID>`. Spring Data **auto-implements** it at startup based on the method names.

Example — `RoomRepository.java`:

```java
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByPg(PG pg);
    List<Room> findByPgIn(List<PG> pgs);
    long countByPgAndIsOccupied(PG pg, Boolean isOccupied);
}
```

You write **zero SQL**. Spring parses the method name:
- `findByPg(PG pg)` → `SELECT * FROM rooms WHERE pg_id = ?`
- `countByPgAndIsOccupied(...)` → `SELECT COUNT(*) FROM rooms WHERE pg_id = ? AND is_occupied = ?`

**Inheriting from `JpaRepository` gives you for free:**
- `findAll()`, `findById(id)`, `save(entity)`, `delete(id)`, `count()`, `existsById(id)` — all work out of the box.

### Why interfaces, not classes?

Spring Data **generates the implementation** at startup using runtime proxy classes. You define the contract (interface), Spring writes the SQL.

### How services USE repositories

```java
@Service
public class OwnerService {
    private final RoomRepository roomRepository;  // injected

    public OwnerService(RoomRepository roomRepository) {  // constructor injection
        this.roomRepository = roomRepository;
    }

    public List<Room> getAllOwnerRooms(User owner) {
        List<PG> pgs = pgRepository.findByOwner(owner);
        return roomRepository.findByPgIn(pgs);
    }
}
```

The service receives the repository via **dependency injection** — Spring creates the repository and "injects" it into the service's constructor.

---

## 7. Backend — Service layer

Services contain **business logic** — the rules of the application. They orchestrate one or more repositories.

### `OwnerService.java` — main responsibilities

| Method | What it does |
|---|---|
| `createPG(owner, body)` | Creates a PG, sets the owner, saves to DB |
| `addRoom(...)` | Creates one room |
| `addRoomsBulk(owner, pgId, roomsData)` | Bulk creates rooms in one transaction; skips duplicate room numbers |
| `getAllOwnerRooms(owner)` | Fetches owner's rooms, populates `currentOccupancy` for each |
| `populateOccupancy(rooms)` | Counts active assignments per room and fills the transient field |
| `assignTenant(roomId, tenantId, owner)` | Multi-step: validates capacity, vacates old room if any, creates assignment, generates first rent record, sends notification |
| `getDashboardStats(owner)` | Aggregates metrics for the dashboard |
| `getPendingVacateRequests(owner)` | Finds active assignments where vacate request was submitted |
| `approveVacate(id, owner)` | Marks assignment inactive, recomputes room occupancy, notifies tenant |
| `rejectVacate(id, owner, note)` | Clears the request, notifies tenant |
| `manualVacate(id, owner, note)` | Owner-initiated eviction (no tenant request) |

### Why the service layer is critical

The service is where **invariants are enforced**:
- "A room cannot have more tenants than its capacity" — checked in `assignTenant`.
- "Only the PG's owner can vacate its rooms" — checked at the top of every method.
- "After a vacate, room occupancy must be recomputed" — done in `finalizeVacate`.

If you put this logic in controllers, every controller would have to repeat it. The service is the single source of truth for business rules.

### `@Transactional`

```java
@Transactional
public RoomAssignment assignTenant(Long roomId, Long tenantId, User owner) { ... }
```

This annotation tells Spring: **all DB operations inside this method must succeed together, or none of them apply.** If the notification save throws an exception, the assignment save is rolled back too — the system stays consistent.

### `TenantService.java`

| Method | What it does |
|---|---|
| `getMyRoom(tenant)` | Finds the tenant's active assignment |
| `requestVacate(tenant, leaveDate, reason)` | Sets vacate fields on the active assignment, notifies owner |
| `cancelVacateRequest(tenant)` | Clears vacate fields |
| `raiseComplaint(...)` | Creates Complaint with optional image URLs |
| `raiseMaintenance(...)` | Creates MaintenanceRequest |
| `getMyRentHistory(tenant)` | Lists tenant's rent records |
| `getMyAnnouncements(tenant)` | Lists announcements for the tenant's PG |
| `getMyNotifications(tenant)`, `markNotificationRead(id)` | Self-service notifications |
| `getDashboardStats(tenant)` | Aggregates pending rent, open complaints, unread notifications |

### `AuthService.java`

| Method | What it does |
|---|---|
| `register(request)` | Hashes password with BCrypt, saves user, generates JWT |
| `login(request)` | Validates credentials via `AuthenticationManager`, generates JWT |

The login flow uses Spring Security's `AuthenticationManager` which:
1. Loads the user via `CustomUserDetailsService` (from DB by email)
2. Compares the submitted password against the BCrypt hash
3. Throws `BadCredentialsException` if wrong

---

## 8. Backend — Controller layer

Controllers expose REST endpoints. They are **thin** — receive request, call service, return response.

### Controller pattern

```java
@RestController
@RequestMapping("/api/owner")
public class OwnerController {
    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String,Object>>> dashboard(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", ownerService.getDashboardStats(getUser(ud))));
    }
}
```

Key annotations:
- `@RestController` = `@Controller` + `@ResponseBody` (everything returned is auto-converted to JSON)
- `@RequestMapping("/api/owner")` — base path for the whole class
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` — HTTP verbs
- `@PathVariable` — values from the URL (`/{id}`)
- `@RequestBody` — JSON body parsed into a Java object
- `@RequestParam` — query string (`?key=value`)
- `@AuthenticationPrincipal UserDetails ud` — Spring Security injects the currently logged-in user

### Standard response shape — `ApiResponse<T>`

Every endpoint returns:
```json
{ "success": true, "message": "OK", "data": <actual payload> }
```

This makes the frontend's life easier — it always knows where to find the data and the success flag.

### What controllers DO NOT do

- ❌ No DB queries (that's the repository's job)
- ❌ No multi-step business logic (that's the service's job)
- ✅ Validate input, dispatch to service, return result

---

## 9. Backend — Security layer (JWT)

This is the part interviewers love to probe. **Master this section.**

### What is JWT?

A **JSON Web Token** is a string with three parts separated by dots:
```
eyJhbGciOiJIUzI1NiJ9 . eyJyb2xlIjoiT1dORVIiLCJzdWIiOiJvd25lckBkZW1vLmNvbSJ9 . SIGNATURE
└── header ──┘   └────── payload (claims) ──────┘   └────── signature ──────┘
```

- **Header**: which algorithm signs it (HS256 here)
- **Payload (claims)**: data — `sub` (subject = email), `role`, `iat` (issued at), `exp` (expiry)
- **Signature**: `HMACSHA256(header + payload, secretKey)` — only the server has the secret, so only the server can produce a valid signature

The server can **trust any JWT it receives without a DB lookup** because if the signature is valid, the claims weren't tampered with.

### `JwtUtil.java` — what it does

| Method | Purpose |
|---|---|
| `generateToken(UserDetails, role)` | Build a signed JWT with email + role + 24h expiry |
| `validateToken(token)` | Parse and verify signature; return true/false |
| `extractEmail(token)` | Read the `sub` claim |
| `extractRole(token)` | Read the `role` claim |

### `JwtAuthFilter.java` — the gatekeeper

Extends `OncePerRequestFilter`. **Runs on every incoming request** (registered before Spring Security's username-password filter).

For each request:
1. Read the `Authorization` header
2. Strip the `"Bearer "` prefix → get the raw JWT
3. Validate it via `JwtUtil`
4. If valid:
   - Load the user via `CustomUserDetailsService.loadUserByUsername(email)`
   - Build a `UsernamePasswordAuthenticationToken` with the user's authorities (e.g., `ROLE_OWNER`)
   - Put it on `SecurityContextHolder` — this is how Spring knows "this request is authenticated as user X"
5. `chain.doFilter()` — proceed to the controller

If the token is missing or invalid, the filter just doesn't authenticate. Spring Security then rejects the request because of the rules in `SecurityConfig`.

### `SecurityConfig.java` — the rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**", "/api/files/**", "/h2-console/**").permitAll()
    .requestMatchers("/api/owner/**").hasRole("OWNER")
    .requestMatchers("/api/tenant/**").hasRole("TENANT")
    .anyRequest().authenticated()
)
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**Translation:**
- `/api/auth/**` and `/api/files/**` → no token needed (login, register, image serving)
- `/api/owner/**` → must have role OWNER
- `/api/tenant/**` → must have role TENANT
- Anything else → must at least be authenticated
- `STATELESS` → no server-side session; every request must carry its JWT
- `addFilterBefore(...)` → put our JWT filter before Spring's default username/password filter

### Why JWT over sessions?

| Aspect | Session cookies | JWT |
|---|---|---|
| Storage | Server-side store (memory/Redis) | Client-side (localStorage) |
| Scaling | Need shared session store across servers | Stateless — any server can verify |
| Mobile-friendly | Cookies awkward in native apps | Trivial |
| Logout | Just delete server session | Token remains valid until expiry (drawback) |
| Best for | Traditional web apps | SPAs, microservices, mobile |

### BCrypt password hashing

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

When a user registers: `passwordEncoder.encode("demo123")` → produces something like `$2a$10$abc.../...`. We store **only the hash**, never the plain password.

When they log in: `passwordEncoder.matches("demo123", storedHash)` → returns true if they match.

Bcrypt is **slow on purpose** (~100 ms per check) — this is what makes it resistant to brute force.

---

## 10. Backend — Configuration & infrastructure

### `DataSeeder.java`

Implements `CommandLineRunner` — runs once at startup. If `userRepository.existsByEmail("owner@demo.com")` returns false, seed:
- 1 owner + 3 tenants
- 2 PGs with rooms
- Active assignments
- Sample complaints, announcements, rent records, notifications

Idempotent: won't re-seed on later starts.

### `GlobalExceptionHandler.java` — `@RestControllerAdvice`

Catches exceptions thrown by any controller and converts them to HTTP responses.

| Exception | Returns |
|---|---|
| `IllegalArgumentException` | 400 Bad Request |
| `HttpMessageNotReadableException` | 400 (malformed JSON) |
| `BadCredentialsException` | 401 Unauthorized |
| `AccessDeniedException` | 403 Forbidden |
| `RuntimeException` with "not found" in message | 404 |
| Other `RuntimeException` | 500 Internal Server Error |
| Anything else | 500 |

Without this, all errors would return generic Spring 500 pages. Now the frontend gets clean JSON like `{"success": false, "message": "Room is already full"}`.

### Profiles — `application-h2.properties` vs `application-mysql.properties`

`application.properties` selects which is active:
```properties
spring.profiles.active=h2
```

**H2** is a Java-embedded database — no install needed, runs inside the JVM. Stores data in `./data/pg-h2.mv.db`. Uses `MODE=MySQL;NON_KEYWORDS=MONTH,USER,VALUE,KEY,TYPE` to mimic MySQL syntax.

**MySQL** is the production-ready DB — runs as a separate service on port 3306. Uses `createDatabaseIfNotExist=true` to auto-create the schema.

Switching is one line — the entity code is **identical** for both because JPA abstracts the SQL dialect.

### `ddl-auto=update`

```properties
spring.jpa.hibernate.ddl-auto=update
```

On startup, Hibernate compares the entity classes to the actual DB schema and **adds missing tables/columns**. Never drops anything. Good for dev, dangerous for prod (use Flyway/Liquibase in real prod).

---

## 11. Frontend — Architecture overview

### Angular concepts

- **Component** = a piece of UI. Has a TypeScript class, an HTML template, and (sometimes) CSS.
- **Service** = injectable singleton. Used for shared state, HTTP calls, business logic.
- **Module / standalone component** — Angular 17 uses **standalone components**: each component declares what it imports (no NgModule needed). Old Angular used `@NgModule`.
- **Routing** — `app.routes.ts` maps URLs to components.
- **Dependency injection** — services are auto-injected into components via constructor.

### How a page renders

1. User navigates to `/owner/dashboard`
2. Angular router matches the route, loads `DashboardComponent`
3. Component's constructor receives `ApiService`, `Router`, etc. via DI
4. `ngOnInit()` runs → calls `apiService.ownerDashboard()` → returns `Observable<ApiResponse<any>>`
5. Component subscribes to the observable: when data arrives, set local field
6. Angular's change detection re-renders the template

### `app.config.ts`

```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
```

- `provideRouter(routes)` → enables routing
- `provideHttpClient(withInterceptors([authInterceptor]))` → enables HTTP + registers our auth interceptor

### `app.routes.ts`

Routes are lazy-loaded for code-splitting:

```typescript
{ path: 'login', loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent) }
```

Each owner/tenant route uses guards:
```typescript
{ path: 'owner/dashboard', loadComponent: ..., canActivate: [authGuard, roleGuard], data: { role: 'OWNER' } }
```

`canActivate` runs the guards before navigating — if they return `false`, navigation is cancelled.

---

## 12. Frontend — Services

### `auth.service.ts`

Manages login state. Uses a `BehaviorSubject` so components can subscribe and react to login/logout.

| Method | Purpose |
|---|---|
| `login({email, password})` | POSTs to `/api/auth/login`, stores token + user in localStorage |
| `register(...)` | Same for register |
| `logout()` | Clears localStorage, resets `currentUserSubject` |
| `getToken()` | Returns the JWT for the interceptor |
| `isLoggedIn()` | True if token exists |
| `getRole()` | Reads role from current user |

**Why `BehaviorSubject`?** It remembers the last value and emits it to new subscribers — so any component that subscribes to `currentUser$` immediately gets the current state.

### `api.service.ts`

Wraps every backend endpoint as a method returning an `Observable<ApiResponse<T>>`. The component just calls `api.getOwnerRooms().subscribe(...)` and doesn't worry about URLs/headers.

Sample:
```typescript
getMyRoom(): Observable<ApiResponse<RoomAssignment>> {
  return this.http.get<ApiResponse<RoomAssignment>>(`${BASE}/tenant/my-room`);
}
```

The `BASE` constant points to `http://localhost:8082/api`. To change in prod, change one line.

---

## 13. Frontend — Auth interceptor and guards

### `auth.interceptor.ts`

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('pg_token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: 'Bearer ' + token } });
  }
  return next(req);
};
```

**Runs on every HTTP request.** Reads the JWT from localStorage and adds it as the `Authorization` header. Components never have to do this themselves.

### `auth.guard.ts`

```typescript
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  router.navigate(['/login']);
  return false;
};
```

Blocks route navigation if user is not logged in.

### `role.guard.ts`

Reads the required role from `route.data.role`, compares to the current user's role. If mismatch, redirects to login.

---

## 14. Frontend — Components

Each component is a small page or part of a page.

### `login.component.ts`
Has demo-login buttons that auto-fill credentials. Calls `auth.login(...)` and on success navigates to `/owner/dashboard` or `/tenant/dashboard` based on role.

### `register.component.ts`
Form with role selector (OWNER vs TENANT), submits to `/api/auth/register`.

### Owner components

| Component | Purpose |
|---|---|
| `dashboard` | Shows aggregate stats (PGs, rooms, occupancy, pending rent, open complaints) |
| `pgs` | List/create/edit PGs. New: setup-rooms modal opens automatically after creating a PG with totalRooms count |
| `rooms` | List rooms grouped by PG; shows X/Y occupancy badge with progress bar; bulk-add button |
| `tenants` | Active tenant list + pending vacate requests panel + assign-tenant modal |
| `complaints` | List complaints by PG; update status |
| `maintenance` | Same for maintenance requests |
| `announcements` | Post and list announcements |
| `rent` | List rent records; mark as paid |

### Tenant components

| Component | Purpose |
|---|---|
| `dashboard` | Welcome card + quick stats |
| `my-room` | Current room details + Request to Vacate button + pending banner |
| `complaints` | File new complaint with image upload; view history |
| `maintenance` | Same for maintenance |
| `rent` | View rent history (read-only) |
| `announcements` | View PG announcements |
| `notifications` | Inbox-style list; mark as read |

### Component pattern

```typescript
@Component({
  selector: 'app-foo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './foo.component.html'
})
export class FooComponent implements OnInit {
  data: any[] = [];
  loading = true;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getStuff().subscribe({
      next: r => { this.data = r.data; this.loading = false; },
      error: () => this.loading = false
    });
  }
}
```

---

## 15. End-to-end request flow examples

### Example 1: Tenant logs in

1. **User** types `tenant@demo.com / demo123` in the login form, clicks Sign In
2. **`login.component.ts`** calls `authService.login({email, password})`
3. **`auth.service.ts`** calls `http.post('/api/auth/login', {...})`
4. **HTTP request** goes to backend on port 8082
5. **Spring Security filter chain** sees the path is `/api/auth/**`, allows it without token
6. **`AuthController.login`** receives the request, calls `authService.login()`
7. **`AuthService`** calls `authenticationManager.authenticate(...)` → which calls **`CustomUserDetailsService.loadUserByUsername(email)`** → fetches user from DB → BCrypt compares passwords
8. If valid, **`JwtUtil.generateToken(userDetails, role)`** creates a signed JWT
9. Returns `AuthResponse{token, userId, name, email, role}`
10. **`AuthController`** wraps it: `ApiResponse{success: true, data: AuthResponse}`
11. JSON flies back to browser
12. **`auth.service.ts`** stores `token` in `localStorage['pg_token']` and the user object in `localStorage['pg_user']`
13. **`login.component.ts`** sees role=TENANT, navigates to `/tenant/dashboard`
14. **`tenant/dashboard.component.ts`** mounts → calls `api.tenantDashboard()`
15. **`auth.interceptor.ts`** adds `Authorization: Bearer <token>` to the request
16. **`JwtAuthFilter`** validates the token on the backend, sets SecurityContext with role=TENANT
17. **`SecurityConfig`** sees role matches, allows the request
18. **`TenantController.dashboard`** returns dashboard stats
19. Component renders the page

### Example 2: Tenant requests to vacate

1. **User** clicks "Request to Vacate" on `/tenant/my-room`
2. Modal opens, user picks a date and reason
3. **`my-room.component.ts`** calls `api.requestVacate(leaveDate, reason)`
4. POST `/api/tenant/vacate` with body `{leaveDate, reason}`
5. **JWT filter** validates token, role=TENANT
6. **`TenantController.requestVacate`** parses the body, calls `tenantService.requestVacate(user, leaveDate, reason)`
7. **`TenantService`**:
   - Finds the tenant's active assignment via `roomAssignmentRepository.findByTenantAndIsActive(tenant, true)`
   - Validates: must have an active assignment, no existing pending request, leaveDate must be future
   - Sets `vacateRequestedAt`, `requestedLeaveDate`, `vacateReason`
   - Saves
   - Creates a `Notification` for the owner
8. Returns the updated assignment
9. **`my-room.component.ts`** reloads → sees `vacateRequestedAt != null` → shows the yellow pending banner

### Example 3: Owner approves the vacate

1. Owner navigates to `/owner/tenants`
2. Component calls `api.getPendingVacateRequests()`
3. Backend filter validates JWT (role=OWNER)
4. **`OwnerController.getPendingVacateRequests`** → `ownerService.getPendingVacateRequests(owner)`
5. **`OwnerService`** → `roomAssignmentRepository.findByRoom_PgOwnerAndIsActiveAndVacateRequestedAtIsNotNull(owner, true)`
6. Returns list, frontend renders panel
7. Owner clicks **Approve**
8. **`tenants.component.ts`** calls `api.approveVacate(assignmentId)`
9. POST `/api/owner/vacate-requests/{id}/approve`
10. **`OwnerService.approveVacate`**:
    - Finds the assignment, verifies owner authorization
    - Calls `finalizeVacate(...)`:
      - Sets `isActive=false`, `leaveDate = requestedLeaveDate`
      - Clears vacate fields
      - Saves assignment
      - Recomputes the room's `isOccupied` based on remaining active assignments
      - Saves room
      - Creates notification for tenant
11. Returns success
12. Frontend reloads tenant list → vacate panel is empty, the room appears as freed

---

## 16. Key concepts you MUST be able to explain

### 1. What is dependency injection?

Spring creates objects (beans) and "injects" them where needed. Instead of:
```java
RoomRepository repo = new RoomRepository();  // who provides this?
```
We declare:
```java
public OwnerService(RoomRepository repo) { this.repo = repo; }
```
Spring sees the constructor argument, finds the bean it created at startup, and passes it in. **Benefit:** loose coupling, easy to swap implementations, easy to test.

### 2. What is JPA / Hibernate?

JPA is a **specification** (interface). Hibernate is the **implementation**. JPA defines `@Entity`, `@Id`, etc. Hibernate translates them into actual SQL.

Lifecycle of a save:
1. `roomRepository.save(room)` is called
2. Hibernate sees `room.id == null` → executes `INSERT INTO rooms (...) VALUES (...)`
3. Hibernate retrieves the generated ID, sets it on `room`
4. Returns the persisted entity

If the same room (same id) is saved again, Hibernate uses `UPDATE` instead of `INSERT`.

### 3. Lazy loading

`@ManyToOne(fetch = FetchType.LAZY)` means: don't load the related entity until someone calls `getOwner()`. This avoids unnecessary SQL when you only need the PG, not its owner.

**Common pitfall:** if you try to access a lazy-loaded field outside of a database session, you get `LazyInitializationException`. Spring Boot's "Open Session in View" pattern keeps the session open during the HTTP request, which avoids this.

### 4. What is `@Transactional`?

A method-level annotation that wraps the method in a database transaction. If any exception is thrown, all changes inside are rolled back. Crucial for multi-step operations like `assignTenant` (which writes to multiple tables).

### 5. Why is the password hashed and not encrypted?

- **Encryption is reversible** (with the key) — bad for passwords.
- **Hashing is one-way** — even the server can't recover the original password from the hash.
- BCrypt also adds a **salt** (random per-password) so identical passwords have different hashes.
- BCrypt is **deliberately slow** to thwart brute-force attacks.

### 6. What is CORS?

Cross-Origin Resource Sharing. Browsers block requests from `http://localhost:4200` (Angular) to `http://localhost:8082` (backend) unless the backend explicitly allows it. We configure this in `SecurityConfig`:
```java
config.setAllowedOriginPatterns(List.of("http://localhost:*"));
```

### 7. How does Angular's change detection work?

Angular wraps every async event (timer, click, HTTP response) with **Zone.js**. When something happens, Zone tells Angular "check the components", and Angular re-evaluates all template bindings. Cheap because most components don't change.

### 8. What's the difference between `Observable` and `Promise`?

| Promise | Observable |
|---|---|
| Single value | Stream of 0..N values |
| Resolves once | Can emit many times |
| Built into JS | From RxJS library |
| `.then()` | `.subscribe()` |
| Eager (starts immediately) | Lazy (only runs when subscribed) |

HTTP calls in Angular are Observables — they don't fire until you `.subscribe()`.

### 9. What are guards and interceptors?

- **Guard** runs **before** route navigation. Returns true/false to allow/block.
- **Interceptor** runs **before** every HTTP request. Modifies the request (adds headers, etc.) and forwards it.

---

## 17. Common interview questions + sample answers

### Q1: "Walk me through what happens when a user clicks Login."

> *Sample answer:* "The login component takes the email and password, calls the AuthService's login method which sends a POST request to `/api/auth/login`. On the backend, the request hits AuthController which delegates to AuthService. AuthService uses Spring Security's AuthenticationManager — it loads the user from the database via CustomUserDetailsService, then BCrypt-compares the submitted password against the stored hash. If valid, it generates a JWT containing the user's email and role, signed with our secret key. The token comes back to the frontend, which stores it in localStorage. From that point on, the auth interceptor automatically attaches `Authorization: Bearer <token>` to every outgoing HTTP request. On every subsequent request, the JwtAuthFilter on the backend validates the token's signature, extracts the role, and puts an Authentication object in the SecurityContext, which Spring Security uses to authorize the request based on the URL pattern."*

### Q2: "Why JWT instead of sessions?"

> *Sample answer:* "JWT is stateless — the server doesn't need to store anything per user. Every request carries the proof of identity. This makes the backend easy to scale horizontally because any server can verify the token. Sessions, by contrast, require either a shared session store (Redis) or sticky load-balancing. JWT also works naturally with single-page apps — the token sits in localStorage, no cookies to manage. The trade-off is that you can't truly invalidate a JWT before it expires; you'd need a blacklist for that. We accept this because tokens have a short 24-hour lifespan."*

### Q3: "What does `@Transactional` do?"

> *Sample answer:* "It wraps the annotated method in a single database transaction. Spring uses AOP — at runtime, the bean is wrapped in a proxy that opens a transaction before the method runs and commits when it returns successfully. If any RuntimeException is thrown, the proxy rolls back. We use it on `assignTenant` because that method writes to three tables — RoomAssignment, Room, and Notification. Without `@Transactional`, if the notification save failed, we'd have an orphaned assignment with no notification — inconsistent state. With it, all three writes succeed or none of them do."*

### Q4: "Why is there a `@Transient` field on Room?"

> *Sample answer:* "`currentOccupancy` is the count of active room assignments for that room. We don't store it in the database because it would have to be kept in sync with the assignments table — a denormalization risk. Instead, the service computes it on-demand by querying the assignments table. The `@Transient` annotation tells Hibernate not to map this field to a column. Jackson still serializes it normally to JSON, so the frontend gets the field for free. The service has a helper `populateOccupancy(rooms)` that fills it in via a single batch query before returning rooms to the controller."*

### Q5: "What's the difference between `@RestController` and `@Controller`?"

> *Sample answer:* "`@RestController` is shorthand for `@Controller` + `@ResponseBody` on every method. With plain `@Controller`, you'd typically return a view name like 'home' for Thymeleaf templating. With `@RestController`, every method's return value is automatically serialized to JSON and written to the HTTP response body. We use it because this is a REST API consumed by an Angular SPA, not a server-rendered HTML app."*

### Q6: "How does Spring Security know my role?"

> *Sample answer:* "Two places: the JWT contains the role as a claim, and the JwtAuthFilter reads it on every request. Specifically, the filter parses the token, extracts the role string, builds a `SimpleGrantedAuthority("ROLE_" + role)`, wraps it in a `UsernamePasswordAuthenticationToken`, and sets it on the `SecurityContextHolder`. Then SecurityConfig's rule `requestMatchers("/api/owner/**").hasRole("OWNER")` checks that authority list. The role is in the JWT itself so the backend doesn't have to hit the database to authorize each request."*

### Q7: "Why use Spring Data JPA over JDBC directly?"

> *Sample answer:* "Spring Data JPA gives us a lot for free: automatic CRUD methods, query derivation from method names, dialect abstraction across MySQL and H2, and managed transactions. Writing JDBC manually would mean hundreds of lines of boilerplate per repository — opening connections, preparing statements, mapping result sets. JPA lets us focus on the domain model. The trade-off is some control over query plans, which we'd reach for native SQL or `@Query` if we needed."*

### Q8: "How does the frontend know when the user logs out?"

> *Sample answer:* "The AuthService has a BehaviorSubject called `currentUser$`. Components subscribe to it. When `logout()` is called, we clear localStorage and emit `null`. Any component subscribed gets the new value and re-renders accordingly. The Auth Guard then redirects the user back to login on the next route navigation because `isLoggedIn()` returns false."*

### Q9: "Walk me through error handling in the backend."

> *Sample answer:* "We have a `GlobalExceptionHandler` annotated with `@RestControllerAdvice`. It has `@ExceptionHandler` methods for different exception types: `IllegalArgumentException` returns 400, `BadCredentialsException` returns 401, `AccessDeniedException` returns 403, generic `RuntimeException` returns 500 (or 404 if the message contains 'not found'). Each method wraps the message in the standard `ApiResponse{success:false, message:<msg>}` shape so the frontend always knows where to find the error. Without this advice, Spring would return generic stack-trace HTML pages, which the SPA can't parse."*

### Q10: "What happens if the same JWT is used after logout?"

> *Sample answer:* "Honestly, it would still work until expiry — that's a known JWT limitation. Logout just clears the token from the client's localStorage, but the token itself remains cryptographically valid for up to 24 hours. To truly invalidate, you'd need a server-side blacklist (typically Redis with the token's jti claim). We chose not to implement this for simplicity, accepting the small risk because tokens are short-lived."*

---

## 18. Glossary

- **Bean** — an object managed by Spring's IoC container.
- **IoC** (Inversion of Control) — instead of you creating objects, Spring creates and wires them.
- **DI** (Dependency Injection) — Spring giving you the objects you need via constructor/setter.
- **JPA** (Jakarta Persistence API) — Java spec for ORM (entity → table mapping).
- **Hibernate** — the most common JPA implementation.
- **Repository** — a Spring Data interface that abstracts data access.
- **Entity** — a class mapped to a DB table.
- **DTO** (Data Transfer Object) — a flat object used to send/receive JSON; doesn't carry JPA annotations.
- **Bean validation** — `@Valid`, `@NotNull`, `@Email` annotations on DTOs.
- **JWT** (JSON Web Token) — signed token containing user claims, used for stateless auth.
- **CORS** — browser policy that blocks cross-origin requests unless the server allows them.
- **CSRF** — Cross-Site Request Forgery; we disable it because we use JWT (CSRF is a cookie-based attack).
- **Filter chain** — Spring Security's pipeline of filters that runs on every HTTP request.
- **Observable** — RxJS abstraction for async streams; used by Angular's HttpClient.
- **Standalone component** — Angular 17 component that imports its own dependencies (no NgModule).
- **Interceptor** — runs around every HTTP request, can modify req/res.
- **Guard** — runs before route navigation, returns true/false.
- **Lazy loading (Angular)** — splitting the bundle so each route loads its component on demand.
- **Lazy loading (JPA)** — fetching related entities only when accessed.

---

## 19. Quick study tactics for the next 7 days

### Day 1 — Setup & first run
- Get the project running on your machine
- Click through every page
- Note in your own words what each page does

### Day 2 — Study the auth flow
- Read `LoginComponent` → `AuthService` → backend `AuthController` → `AuthService` → `JwtUtil`
- Trace one full login, write the steps in your own words on paper
- Try to break it: send wrong password, see the 401 response

### Day 3 — Pick ONE feature to own deeply
- E.g., "Vacate request workflow"
- Read every file involved (about 6 files: 1 entity, 1 repo, 1 service method, 1 controller endpoint, 1 component, 1 model)
- Be able to add a small change live: make the cancel button require confirmation

### Day 4 — Study the DB layer
- Open the H2 console at http://localhost:8082/h2-console (JDBC URL: `jdbc:h2:file:./data/pg-h2`, user: `sa`, no password)
- Look at every table and its columns
- Run some SELECT queries to see the data

### Day 5 — Study the frontend layer
- Read `app.config.ts`, `app.routes.ts`, `auth.interceptor.ts`, `auth.guard.ts`
- Trace what happens when you visit `/owner/dashboard` without being logged in

### Day 6 — Mock interview
- Have a teammate ask you the questions in section 17
- Practice your answers out loud — recording yourself helps a lot
- Identify which questions you fumble on

### Day 7 — Polish the rough edges
- Re-read the sections of this doc that map to your weak spots
- Re-trace any flow that confused you
- Sleep well before the interview

---

**You've got this. The code is real, the logic is sound, and you have a path to learn it. Take it one section at a time.**
