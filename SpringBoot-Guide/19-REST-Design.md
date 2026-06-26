# Chapter 19: REST API Design Best Practices

[← Back to Index](00-README.md) | [Previous: Configuration & Profiles](18-Configuration-Profiles.md) | [Next: Controllers →](20-Controllers.md)

---

## Why This Chapter Matters

**API design is permanent. Bad APIs haunt you forever. Good design is interview tested.**

### The Reality Check

Most developers can write REST endpoints, but few understand **why** certain patterns exist. They copy-paste controller methods without understanding:
- Why resource naming matters
- When to use PUT vs PATCH
- How to design for scalability
- What makes an API truly professional

### Interview Expectations

For **backend roles**, interviewers expect you to:
- Design REST APIs from scratch for given scenarios
- Explain versioning strategies and trade-offs
- Design pagination, filtering, and sorting
- Handle errors consistently
- Understand idempotency and safety
- Make architectural decisions (HATEOAS, field selection, etc.)

### Real-World Impact

**Scenario 1**: Your API uses `/getUser` and `/createUser`. New developers can't discover endpoints. You need documentation for everything. **Good design**: Resource-based URLs (`/users/{id}`) are self-documenting.

**Scenario 2**: Your API returns 200 OK for everything, with errors in the body. Frontend developers write complex parsing logic. **Good design**: Proper status codes enable standard HTTP handling.

**Scenario 3**: Your API doesn't support pagination. Loading 10,000 records crashes clients. **Good design**: Pagination from day one prevents scalability issues.

**Scenario 4**: You change an endpoint and break 50 mobile apps. **Good design**: Versioning strategy prevents breaking changes.

---

## Layer 1 — Intuition Builder

### Part 1: What Is an API? (Think of a Restaurant)

Imagine you walk into a **restaurant**. You don't go into the kitchen and tell the chef exactly what to do. Instead, you look at a **menu** and place an **order**. The waiter takes your order to the kitchen and brings back your food.

An **API** (Application Programming Interface) is like that menu and ordering system. Your app doesn't reach into another computer's "kitchen." It sends an **order** (a request) and receives a **response** (the result). The **menu** is the list of things you're allowed to ask for, and the **rules** of how to ask are the API design.

**Key idea**: The waiter doesn't remember you. Every time you order, you show your table number and say what you want. That's **stateless** — each request stands on its own.

---

### Part 2: REST — A Set of Rules for the Restaurant

**REST** sounds technical, but it's really just a set of rules for designing APIs — like a style guide for how restaurants should write their menus and take orders.

Think of it this way:

| REST Rule | Restaurant Analogy |
|-----------|-------------------|
| **Resources** | The menu items. Not "give me food" but "I want Pizza Margherita" — you point to a specific thing. |
| **URLs** | The **address** of each dish. `/menu/pizza/margherita` — where to find it. |
| **HTTP Methods** | The **action**. "I want to see the menu" (GET), "I'm placing an order" (POST), "Change my order" (PATCH), "Cancel my order" (DELETE). |
| **Stateless** | The waiter doesn't remember you. Each time, you say your table number and what you want. No memory between requests. |

**Mental model**: A REST API is like a restaurant that follows strict rules. Everything has an address (URL). Every action has a clear verb (HTTP method). And nobody remembers the last order — you always say everything fresh.

---

### Part 3: URLs Are Addresses, Not Instructions

Imagine asking for directions:
- ❌ "Go-get-me-some-milk" — That's an instruction, not an address.
- ✅ "The milk is in aisle 3, shelf 2" — That's an address.

**Bad URLs** (instructions):
```
/getUser?id=123        ← "Go get user" (verb + action)
/createUser           ← "Create user" (verb)
/deleteUser/123       ← "Delete user" (verb)
```

**Good URLs** (addresses):
```
/users/123            ← "The user with ID 123 lives here"
/users                ← "All users are here"
/users/123/orders     ← "User 123's orders are here"
```

**Rule of thumb**: The URL answers "**WHERE** is the resource?" The HTTP method answers "**WHAT** do you want to do with it?"

---

### Part 4: HTTP Methods — The Action Words

Think of these like the verbs you use when talking to the waiter:

| Method | What It Means (Simple) | Restaurant Analogy |
|--------|------------------------|---------------------|
| **GET** | "Show me" or "Let me see" | "Can I see the menu?" |
| **POST** | "Create new" or "Add" | "I'd like to place an order" |
| **PUT** | "Replace entirely" | "Actually, change my whole order to this" |
| **PATCH** | "Update just part" | "Add extra cheese to my pizza" |
| **DELETE** | "Remove" | "Cancel my order" |

**Important**: GET and DELETE usually don't need a "body" (extra details). You're either asking to see something or to remove it. POST, PUT, and PATCH usually send a body — like writing your order on a piece of paper.

---

### Part 5: Status Codes — The Waiter's Response

When you order food, the waiter doesn't just say "OK" for everything. Sometimes they say:
- "Your order is ready!" → Success
- "We're out of that item" → Not found
- "You need to pay first" → Unauthorized
- "We don't serve that here" → Not allowed

**Status codes** are numbers that tell the client what happened. Think of them as the waiter's quick hand signals:

| Code | Meaning (Simple) | When to Use |
|------|------------------|-------------|
| **200** | "Here it is!" | Success — data returned |
| **201** | "I created it for you!" | Success — something new was created |
| **204** | "Done, nothing to show" | Success — action completed (e.g., delete) |
| **400** | "Your request is wrong" | Bad input — missing or invalid data |
| **401** | "Who are you?" | Not logged in / no identity |
| **403** | "You can't do that" | Logged in but not allowed |
| **404** | "We don't have that" | Resource not found |
| **500** | "Our mistake" | Server broke / internal error |

**Key idea**: Use the right number so the client knows what happened without reading a long message.

---

### Part 6: Consistency — Same Shape Every Time

Imagine a restaurant where sometimes your bill comes as a printed receipt, sometimes as a napkin with scribbles, and sometimes the waiter just tells you a number. Confusing! You'd want the bill to always look the same so you know where to find the total.

**APIs work the same way.** Success responses should always have the same structure. Error responses should always have the same structure. Clients expect:
- "Where is the data?" → Same place every time
- "Where is the error message?" → Same place every time

**Good** (consistent success):
```json
{
  "data": { "id": 1, "name": "John" }
}
```

**Good** (consistent error):
```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "User not found"
  }
}
```

**Bad**: Sometimes `{ "user": ... }`, sometimes `{ "result": ... }`, sometimes `{ "payload": ... }` — clients never know where to look.

---

### Part 7: Versioning — The New Menu vs the Old Menu

Restaurants sometimes change their menu. But regular customers might still want the old dishes. So the restaurant might say: "We have Menu v1 (classic) and Menu v2 (new items)."

**API versioning** is the same. When you change how your API works (remove a field, change a name), old apps that rely on the old format will break. So you keep both:
- `/api/v1/users` — Old way (for old apps)
- `/api/v2/users` — New way (for new apps)

Everyone gets what they need. Nobody breaks.

---

### Part 8: Pagination — Don't Dump the Whole Catalog

Imagine a library with 100,000 books. The librarian doesn't hand you all 100,000 at once. They say: "Here are the first 20. Want more? Ask for the next 20."

**Pagination** means giving data in small chunks (pages) instead of everything at once.

```
Page 1: Books 1–20
Page 2: Books 21–40
Page 3: Books 41–60
...
```

**Without pagination**: Loading 10,000 users → app crashes, slow, wasted data.  
**With pagination**: Load 20 users at a time → fast, smooth, efficient.

---

### Part 9: Rate Limiting — Fair Use for Everyone

Imagine a buffet. One person can't take all the food and leave nothing for others. There are unwritten rules: take a reasonable amount, come back if you need more.

**Rate limiting** means: "You can only make X requests per minute." If you make too many, the server says "Slow down! Try again in 60 seconds" (like a 429 status).

**Why?** To stop one user or one buggy app from overwhelming the server and making it slow for everyone else.

---

### Part 10: Idempotency — Safe to Try Again

**Idempotency** means: "If you do the same thing twice, the result is the same as doing it once."

Example: Deleting a user. Delete once → user is gone. Delete again → user is still gone (there's nothing left to delete). Same outcome. **Safe to retry.**

Example of *not* idempotent: "Add $10 to account." Do it once → +$10. Do it twice (by accident) → +$20. **Dangerous to retry.**

For things like payments, we use **idempotency keys**: "This request has ID abc123." If the server sees abc123 again, it says "I already did that, here's the same result" instead of charging twice.

---

## Layer 2 — Professional Developer

### Resource Naming (Deep Dive)

#### Nouns, Not Verbs

**Why?** HTTP methods already express actions. URLs should identify resources.

```
GET    /users              # List users
GET    /users/{id}         # Get single user
POST   /users              # Create user
PUT    /users/{id}         # Replace user
PATCH  /users/{id}         # Update user partially
DELETE /users/{id}         # Delete user
GET    /users/{id}/orders  # User's orders
```

❌ **Bad Examples:**
```
POST /getUser
POST /createUser
POST /deleteUser
GET /user_list
```

✅ **Good Examples:**
```
GET /users/{id}
POST /users
DELETE /users/{id}
GET /users
```

#### Plural vs Singular

**Rule**: Use plural for collections; individual resources are identified by ID in the path.

```
✅ GET /users              # Collection endpoint
✅ GET /users/123          # Individual resource (ID in path)
✅ GET /users/123/orders   # Nested collection
```

**Exception**: When the resource is conceptually singular:
```
✅ GET /profile            # Current user's profile (singular concept)
✅ GET /settings           # Application settings (singular concept)
```

#### Hierarchical Relationships

Represent parent-child relationships in the path:

```
GET    /users/{userId}/orders           # User's orders
GET    /users/{userId}/orders/{orderId} # Specific order
POST   /users/{userId}/orders           # Create order for user
```

⚠️ **Don't nest too deep** (more than 2–3 levels becomes unwieldy):
```
❌ GET /users/{id}/orders/{orderId}/items/{itemId}/reviews/{reviewId}
```

**Alternative**: Use query parameters or flat endpoints:
```
✅ GET /orders?userId=123
✅ GET /reviews?orderId=456&itemId=789
```

#### Naming Conventions

Use **lowercase with hyphens** (kebab-case):
```
✅ /user-profiles
✅ /order-items
✅ /api-keys
```

Avoid:
```
❌ /userProfiles          (camelCase)
❌ /order_items           (snake_case)
❌ /UserProfiles          (PascalCase)
```

---

### HTTP Methods Reference Table

| Method | Purpose | Request Body | Idempotent | Safe |
|--------|---------|--------------|------------|------|
| **GET** | Read resource(s) | No | Yes | Yes |
| **POST** | Create new resource | Yes | No* | No |
| **PUT** | Replace entire resource | Yes | Yes | No |
| **PATCH** | Partial update | Yes | Depends | No |
| **DELETE** | Remove resource | No | Yes | No |

*POST can be made idempotent with idempotency keys.

---

### Response Design

#### Consistent Success Structure

**Single resource:**
```json
{
  "data": {
    "id": 123,
    "name": "John Doe",
    "email": "john@example.com"
  }
}
```

**Or simpler (direct resource):**
```json
{
  "id": 123,
  "name": "John Doe",
  "email": "john@example.com"
}
```

**Collection with pagination:**
```json
{
  "data": [
    {"id": 1, "name": "John"},
    {"id": 2, "name": "Jane"}
  ],
  "total": 2,
  "page": 1,
  "size": 20
}
```

#### Proper Status Codes in Spring Boot

```java
// 200 OK — Standard success
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.findById(id));
}

// 201 Created — Resource created, include Location header
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody UserDto userDto) {
    User created = userService.create(userDto);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .header("Location", "/api/users/" + created.getId())
        .body(created);
}

// 204 No Content — Success, no body (typical for DELETE)
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
}

// 404 Not Found — Resource doesn't exist
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return userService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
}
```

#### Error Response Format

**Standard error shape:**
```json
{
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "User with ID 123 does not exist",
    "timestamp": "2026-02-17T10:30:00Z",
    "path": "/api/users/123"
  }
}
```

**Validation errors (422):**
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "errors": [
      {
        "field": "email",
        "message": "Email is required"
      },
      {
        "field": "age",
        "message": "Age must be between 18 and 100"
      }
    ]
  }
}
```

---

### API Versioning Strategies

#### Strategy 1: URL Versioning (Most Common)

**Format:** `/api/v{version}/{resource}`

```
GET /api/v1/users/123
GET /api/v2/users/123
```

**Pros:** Simple, discoverable, cache-friendly, easy to route.  
**Cons:** URLs change (bookmarks, cached links may break).

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {
    // v1 implementation
}

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {
    // v2 implementation with breaking changes
}
```

#### Strategy 2: Header Versioning

**Format:** Use `Accept` header

```
GET /api/users/123
Accept: application/vnd.api.v1+json
```

**Pros:** Clean URLs, true content negotiation.  
**Cons:** Less discoverable, harder to test, caching considerations.

```java
@GetMapping(value = "/users/{id}", produces = "application/vnd.api.v1+json")
public ResponseEntity<UserV1> getUserV1(@PathVariable Long id) {
    // v1 implementation
}

@GetMapping(value = "/users/{id}", produces = "application/vnd.api.v2+json")
public ResponseEntity<UserV2> getUserV2(@PathVariable Long id) {
    // v2 implementation
}
```

#### Strategy 3: Query Parameter Versioning

**Format:** `/api/users/123?version=1`

**Pros:** Simple, clean base URLs.  
**Cons:** Not RESTful (query params should filter), easy to forget, caching issues.

**Recommendation:** Use **URL versioning** (`/api/v1/`) for most APIs. It's the industry standard (GitHub, Stripe, Twitter) and simplest to implement.

---

### HATEOAS Concept

**HATEOAS** (Hypermedia As The Engine Of Application State): responses include links to related resources so clients can discover the API by following links.

**Example response:**
```json
{
  "id": 123,
  "name": "John Doe",
  "email": "john@example.com",
  "_links": {
    "self": { "href": "/api/users/123" },
    "orders": { "href": "/api/users/123/orders" },
    "update": { "href": "/api/users/123", "method": "PUT" },
    "delete": { "href": "/api/users/123", "method": "DELETE" }
  }
}
```

**Use when:** Public APIs with many clients, hypermedia-driven applications.  
**Skip when:** Internal APIs, simple CRUD, mobile apps (they often hardcode URLs).

**Spring HATEOAS example:**
```java
@GetMapping("/users/{id}")
public EntityModel<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return EntityModel.of(user,
        linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
        linkTo(methodOn(UserController.class).getOrders(id)).withRel("orders")
    );
}
```

---

## Layer 3 — Advanced Engineering Depth

### Pagination Patterns

#### Offset-Based Pagination

**Format:** `?page={page}&size={size}`

```
GET /api/users?page=1&size=20
GET /api/users?page=2&size=20
```

**Flow (ASCII):**
```
┌─────────────────────────────────────────────────────────────┐
│  Client Request: GET /users?page=1&size=20                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Database: SELECT * FROM users ORDER BY id LIMIT 20 OFFSET 0 │
│  Page 1 = rows 1-20                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Client Request: GET /users?page=2&size=20                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Database: SELECT * FROM users ORDER BY id LIMIT 20 OFFSET 20│
│  Page 2 = rows 21-40                                        │
└─────────────────────────────────────────────────────────────┘
```

**Response:**
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**Pros:** Simple, supports jumping to a page.  
**Cons:** `OFFSET` degrades with large values; results can shift if data changes.

```java
@GetMapping("/users")
public ResponseEntity<Page<User>> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userService.findAll(pageable);
    return ResponseEntity.ok(users);
}
```

#### Cursor-Based Pagination

**Format:** `?cursor={cursor}&size={size}`

```
GET /api/users?cursor=eyJpZCI6MTIzfQ&size=20
```

**Flow (ASCII):**
```
┌─────────────────────────────────────────────────────────────┐
│  Client: GET /users?size=20                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  DB: SELECT * FROM users WHERE id > 0 ORDER BY id LIMIT 20  │
│  Return rows 1-20 + encode(lastId=20) as nextCursor         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Client: GET /users?cursor=eyJpZCI6MjB9&size=20             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  DB: SELECT * FROM users WHERE id > 20 ORDER BY id LIMIT 20  │
│  No OFFSET — uses WHERE clause (index-friendly)             │
└─────────────────────────────────────────────────────────────┘
```

**Response:**
```json
{
  "data": [...],
  "pagination": {
    "nextCursor": "eyJpZCI6MTQzfQ",
    "hasNext": true,
    "size": 20
  }
}
```

**Pros:** Stable results, better performance, no OFFSET.  
**Cons:** Can't jump to page N; cursor must be opaque/encoded.

**Use cursor when:** Large datasets, real-time data, infinite scroll.  
**Use offset when:** Small datasets, need page jump (e.g., "Go to page 5").

---

### Filtering and Sorting

#### Filtering

**Format:** `?field=value&field2=value2`

```
GET /api/users?status=active&role=admin
GET /api/users?createdAfter=2026-01-01&createdBefore=2026-12-31
```

```java
@GetMapping("/users")
public ResponseEntity<List<User>> getUsers(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter) {
    List<User> users = userService.findByFilters(status, role, createdAfter);
    return ResponseEntity.ok(users);
}
```

#### Sorting

**Format:** `?sort=field1,asc&sort=field2,desc`

```
GET /api/users?sort=createdAt,desc
GET /api/users?sort=name,asc&sort=createdAt,desc
```

```java
@GetMapping("/users")
public ResponseEntity<Page<User>> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @SortDefault.SortDefaults({
            @SortDefault(sort = "name", direction = Sort.Direction.ASC),
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        }) Sort sort) {
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<User> users = userService.findAll(pageable);
    return ResponseEntity.ok(users);
}
```

#### Field Selection

**Format:** `?fields=id,name,email`

```
GET /api/users/123?fields=id,name,email
```

Reduces payload and gives clients control over what they receive.

**Combined example:**
```
GET /api/users?status=active&sort=created_at,desc&fields=id,name&page=1&size=20
```

---

### Rate Limiting

**Headers:**
```
X-RateLimit-Limit: 1000        # Requests per window
X-RateLimit-Remaining: 999     # Remaining in current window
X-RateLimit-Reset: 1641398400  # Unix timestamp when limit resets
```

**Response when exceeded (429):**
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60

{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Try again in 60 seconds.",
    "retryAfter": 60
  }
}
```

**Strategies:** Fixed window, sliding window, token bucket, leaky bucket.

```java
@GetMapping("/api/data")
public ResponseEntity<?> getData(HttpServletRequest request) {
    String clientId = getClientId(request);
    if (!rateLimiter.allowRequest(clientId)) {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(rateLimiter.getRetryAfter()))
            .body(Map.of("error", "Rate limit exceeded"));
    }
    return ResponseEntity.ok(data);
}
```

---

### Idempotency Design

**Idempotent methods:** GET, PUT, DELETE, HEAD, OPTIONS — same request, same outcome.  
**Not idempotent:** POST (by default).

**Making POST idempotent with Idempotency-Key:**

```
POST /api/payments
Idempotency-Key: unique-key-123
Body: {"amount": 100, "currency": "USD"}
```

- First request: Process payment, store key → return result.  
- Retry with same key: Return stored result, do not process again.

```java
@PostMapping("/payments")
public ResponseEntity<Payment> createPayment(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody PaymentDto paymentDto) {
    Optional<Payment> existing = paymentService.findByKey(idempotencyKey);
    if (existing.isPresent()) {
        return ResponseEntity.ok(existing.get());
    }
    Payment payment = paymentService.create(paymentDto, idempotencyKey);
    return ResponseEntity.status(201).body(payment);
}
```

**Storage:** Cache (e.g. Redis) with TTL matching idempotency window (e.g. 24 hours).

---

## Layer 4 — Interview Mastery

### Q1: Design an API for an e-commerce platform.

**Answer:**

```
# Products
GET    /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/products
PUT    /api/v1/products/{id}
DELETE /api/v1/products/{id}

# Categories
GET    /api/v1/categories
GET    /api/v1/categories/{id}/products

# Orders
GET    /api/v1/orders
GET    /api/v1/orders/{id}
POST   /api/v1/orders
PATCH  /api/v1/orders/{id}/status
DELETE /api/v1/orders/{id}

# Users
GET    /api/v1/users/{id}/orders
GET    /api/v1/users/{id}/cart

# Search
GET    /api/v1/search?q=keyword&category=electronics
```

Resources as nouns, proper HTTP methods, nesting for relationships, query params for filtering/search, versioning in URL.

---

### Q2: How do you handle API versioning?

**Answer:** Use **URL versioning** (`/api/v1/`, `/api/v2/`) because it’s simple, widely adopted (GitHub, Stripe), works with caching, and is easy to route. Major versions for breaking changes; deprecate with notice before removing. Alternatives: header (`Accept`) or query param, but both are less discoverable and less common.

---

### Q3: What makes a good REST API?

**Answer:** Resource-based URLs (nouns), correct HTTP methods, appropriate status codes, consistent response shape, statelessness, clear versioning, documentation (e.g. OpenAPI/Swagger), structured error handling, pagination for collections, and support for filtering/sorting.

---

### Q4: How do you design pagination?

**Answer:** Use **offset** (`?page=1&size=20`) for most cases: easy to implement and allows jumping to a page. Use **cursor** (`?cursor=xxx&size=20`) for large or changing datasets and infinite scroll. Trade-offs: offset can degrade with large offsets; cursor can’t jump to a specific page.

---

### Q5: PUT vs PATCH?

**Answer:** **PUT** replaces the entire resource; missing fields become null/default. **PATCH** updates only the provided fields; others stay unchanged. Use PUT when you have the full representation; use PATCH for partial updates.

---

### Q6: How do you handle errors?

**Answer:** Use a consistent error envelope, e.g. `{ "error": { "code": "...", "message": "...", "timestamp": "...", "path": "..." } }`. Use proper HTTP status codes (400, 401, 403, 404, 422, 500). Use machine-readable codes for clients; keep messages human-readable. For validation, include per-field errors. Never expose stack traces in production.

---

### Q7: What is idempotency and why does it matter?

**Answer:** Idempotency means executing the same request multiple times produces the same result. Important for retries and avoiding duplicate side effects. GET, PUT, DELETE are naturally idempotent. POST can be made idempotent with an `Idempotency-Key` header so retries return the stored result instead of reprocessing.

---

### Q8: What is HATEOAS and when would you use it?

**Answer:** HATEOAS means responses include links to related resources so clients can navigate the API. Use it in public or hypermedia-driven APIs. Often unnecessary for internal CRUD APIs or mobile apps that hardcode URLs. Focus on clear docs unless you have a strong case for hypermedia.

---

### Common Pitfalls

| Pitfall | Bad | Good |
|---------|-----|------|
| Over-nesting | `/users/{id}/orders/{oid}/items/{iid}/reviews` | `/reviews?orderId=x&itemId=y` |
| Inconsistent naming | `/user/123`, `/users`, `/user-list` | `/users`, `/users/123` |
| Wrong methods | `GET /users/123/delete` | `DELETE /users/123` |
| No pagination | `GET /users` returns all | `GET /users?page=1&size=20` |
| Inconsistent errors | Mixed `error` / `failure` / `msg` | Single `error` object with `code`, `message` |

---

## Summary

| Topic | Key Takeaway |
|-------|--------------|
| **REST** | Resources as URLs, HTTP methods for actions, stateless. |
| **URL Design** | Nouns, plural collections, lowercase-hyphens, hierarchical relationships. |
| **HTTP Methods** | GET (read), POST (create), PUT (replace), PATCH (partial update), DELETE (remove). |
| **Status Codes** | Use 2xx/4xx/5xx to signal outcome; don’t always return 200. |
| **Response** | Consistent structure for success and error. |
| **Versioning** | Prefer URL versioning (`/api/v1/`). |
| **Pagination** | Offset for simplicity; cursor for large or changing datasets. |
| **Filtering/Sorting** | Query params; support `fields` for sparse responses. |
| **HATEOAS** | Optional; add links when discovery/navigation matters. |
| **Rate Limiting** | Protect services; use headers and 429 with `Retry-After`. |
| **Idempotency** | Design for safe retries; use Idempotency-Key for POST. |

---

[← Back to Index](00-README.md) | [Previous: Configuration & Profiles](18-Configuration-Profiles.md) | [Next: Controllers →](20-Controllers.md)
