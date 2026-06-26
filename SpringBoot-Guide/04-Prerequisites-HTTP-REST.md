# Chapter 4: HTTP & REST Fundamentals (Prerequisites)

[← Back to Index](00-README.md) | [Previous: Design Patterns](03-Prerequisites-Design-Patterns.md) | [Next: Web Fundamentals →](05-Prerequisites-Web-Fundamentals.md)

---

## Why This Chapter Matters

Imagine building a house without understanding how doors and windows work. You could nail boards together, but something would feel off—and things would break when you least expect it.

**Building web APIs is the same.** When you write Spring Boot applications, you're building things that talk over the internet. The language they use is called HTTP. If you don't understand HTTP, you're building blindly.

### Why Beginners Get Stuck

Many developers know just enough to be dangerous:
- "GET gets stuff, POST creates stuff"
- "200 means success, 404 means not found"

But they **don't understand** *why* these rules exist. So when things go wrong—duplicate records when a user clicks twice, errors that show "success" on the screen, or mysterious "CORS" failures—they're stuck.

### What You'll Gain

By the end of this chapter, you'll understand:
- **What HTTP is** and how it works (like ordering food at a restaurant)
- **How requests and responses** flow between your app and the server
- **When to use each HTTP method** (and why it matters for retries and safety)
- **What REST means** and how to design APIs that make sense
- **How to avoid common mistakes** that plague beginner developers

Let's start from the very beginning.

---

## Layer 1 — Intuition Builder

### Part 1: The Restaurant Analogy — Understanding Requests and Responses

Picture a **restaurant**.

**You (the customer)** = The **client** (your phone app, a website, or another program)

**The kitchen** = The **server** (Spring Boot app running on a computer)

**How do you get food?** You don't walk into the kitchen and grab it. You **ask** for it. The waiter takes your request to the kitchen, and the kitchen sends back your food.

```
    YOU (Client)                    WAITER                    KITCHEN (Server)
    ─────────────                   ──────                    ─────────────────

    "I'd like a                    carries your                
    burger, please"    ────────►   order ────────►             cooks burger
                                                                   │
                                                                   │
    "Here's your      ◄────────   brings food   ◄─────────────────┘
    burger!"          
```

**HTTP works exactly like this.** One computer (the client) sends a **request** ("Give me this data"). Another computer (the server) sends back a **response** ("Here's the data").

- **Request** = What you ask for (like your food order)
- **Response** = What you get back (like your food on a plate)

The "waiter" in real life is the **internet**—it carries your messages back and forth. We call this whole system **HTTP** (HyperText Transfer Protocol). Don't worry about that fancy name—it just means "the rules for how computers ask each other for stuff on the web."

### Part 2: Different Types of "Orders" — HTTP Methods

In a restaurant, you can do different things:

| What you want to do | How you'd ask | In HTTP world |
|---------------------|---------------|---------------|
| **Look at the menu** | "Can I see the menu?" | **GET** — "Show me this" (you're just looking, not changing anything) |
| **Order new food** | "I'd like a pizza" | **POST** — "Create something new" (you're adding something) |
| **Change your order** | "Actually, make it a large pizza" | **PUT** or **PATCH** — "Update this" (you're changing something that exists) |
| **Cancel your order** | "Never mind, cancel it" | **DELETE** — "Remove this" (you're taking something away) |

**The most important idea:** Each "action" has a specific meaning. You wouldn't say "I'd like to DELETE a menu" when you mean "show me the menu." In the same way, we use the right HTTP method for the right job.

### Part 3: The Kitchen's Reply — Status Codes

When you order food, the kitchen doesn't just give you a plate. Sometimes they respond in different ways:

| What happens | What the kitchen might say | HTTP Status Code |
|--------------|----------------------------|------------------|
| Everything went great | "Here's your food!" | **200 OK** — Success! |
| They made something new for you | "Your special order is ready!" | **201 Created** — We created something new |
| They can't find your order | "We don't have that on our menu" | **404 Not Found** — We don't have what you asked for |
| You asked for something weird | "That's not a valid order" | **400 Bad Request** — Your request doesn't make sense |
| You're not allowed to order that | "Sorry, that's staff only" | **403 Forbidden** — You're not allowed |
| The kitchen is having problems | "Our oven is broken, come back later" | **500 Internal Server Error** — Something broke on our side |

**Status codes are numbers** (like 200, 404, 500) that tell the client **what happened**. No need to read a long message—the number says it all.

### Part 4: The Extra Details — Headers

When you order at a restaurant, you might add details:
- "I'm allergic to nuts" (extra information about you)
- "I'd like it to go" (how you want it)

In HTTP, we call these extra details **headers**. They're like sticky notes attached to your request or response.

**Examples:**
- "I want my response in JSON format" → `Accept: application/json`
- "Here's who I am" (like a membership card) → `Authorization: Bearer token123`
- "This is how big my message is" → `Content-Length: 256`

Headers don't change *what* you're asking for—they add *how* you want it or *who* you are.

### Part 5: The Address — URLs and Paths

When you call a restaurant, you need to know:
1. **Which restaurant?** (the server address, like `api.example.com`)
2. **What exactly do you want?** (the path, like `/users/123`)

A full URL looks like: `https://api.example.com/users/123`

Let's break it down:

```
https://  api.example.com  /users/123
   │            │              │
   │            │              └── PATH: "Show me user number 123"
   │            └── SERVER: "This restaurant"
   └── SECURE: Your message is encrypted (like a secret code)
```

- **Path** (`/users/123`) = What resource you want. Like saying "the user with ID 123"
- **Query parameters** = Extra filters. Like `?sort=name` means "and sort them by name"

Example: `https://api.example.com/users?role=admin&page=2` means "Give me users who are admins, and show me page 2."

### Part 6: The Message Body — Sending Data

When you **order new food** (POST) or **change your order** (PUT, PATCH), you need to send **details**—what kind of pizza? what size?

In HTTP, we put these details in the **body** of the request. It's like writing your order on a slip of paper.

```json
{
  "name": "John",
  "email": "john@example.com"
}
```

This might mean "Create a new user named John with this email." The server reads this "slip" and does the work.

**GET requests usually don't have a body**—you're just asking to see something, not sending new data.

### Part 7: What is REST? (The Big Picture)

**REST** is a set of rules for designing APIs—like having a consistent way for all restaurants to write their menus and take orders.

Imagine if every restaurant had different rules:
- Restaurant A: You shout your order
- Restaurant B: You write it in Chinese
- Restaurant C: You need to tap your order on a special machine

**Chaos!** REST says: "Let's all follow the same pattern." That way, any client can talk to any API without learning special tricks.

**REST principles (in simple terms):**

1. **Resources as nouns** — Talk about *things* (users, orders, products), not *actions* (getUser, createOrder). Like saying "I want the burger" instead of "I want to acquire a burger."

2. **Stateless** — The server doesn't "remember" you. Each request must bring everything needed. Like showing your membership card every time you order, instead of the waiter "remembering" you from last week.

3. **Use the right "verbs"** — GET for looking, POST for creating, PUT/DELETE for updating/deleting. Like using the right words when ordering food.

4. **URLs identify resources** — `/users/123` clearly means "user number 123." No confusion.

We'll go deeper into REST later. For now, remember: **REST = consistent, predictable rules for building APIs.**

### Part 8: Putting It All Together — A Visual Flow

Here's what a complete HTTP conversation looks like:

```
CLIENT (Your App)                                    SERVER (Spring Boot)
───────────────────                                  ────────────────────

"GET /users/123"        ──────────────────────────►   Receives request
"Accept: application/json"                            "They want user 123,
"Authorization: Bearer abc123"                          want JSON format,
                                                       here's their token"

                                                       Looks up user 123
                                                       Finds the data
                                                       Prepares response

"200 OK"               ◄──────────────────────────   Sends response
"Content-Type: application/json"
"{ \"id\": 123, \"name\": \"John\" }"
```

**Request has:** Method + Path + Headers + (optional) Body

**Response has:** Status Code + Headers + (optional) Body

---

## Layer 2 — Professional Developer

### HTTP Request Anatomy (Deep Dive)

Every HTTP request has three parts. Understanding each helps you debug and design better APIs.

```
┌─────────────────────────────────────┐
│  REQUEST LINE (Method + Path + HTTP Version)  │
├─────────────────────────────────────┤
│  HEADERS (Key: Value pairs)          │
├─────────────────────────────────────┤
│  BODY (Optional - for POST, PUT, PATCH)       │
└─────────────────────────────────────┘
```

**Real example:**

```http
GET /api/users/123 HTTP/1.1
Host: api.example.com
Accept: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
User-Agent: MyApp/1.0
```

**Breaking it down:**

| Part | Example | What it means |
|------|---------|---------------|
| **Method** | GET | What action we want (retrieve, not modify) |
| **Path** | /api/users/123 | Which resource (user with ID 123) |
| **Version** | HTTP/1.1 | Which "dialect" of HTTP we're speaking |
| **Host** | api.example.com | Required in HTTP/1.1 — which server to contact |
| **Accept** | application/json | "I want the response in JSON format" |
| **Authorization** | Bearer &lt;token&gt; | "Here's proof of who I am" |
| **User-Agent** | MyApp/1.0 | "Here's what client is making this request" |

**Why each matters:**
- **Accept**: Enables content negotiation—client and server agree on format (JSON, XML, etc.)
- **Authorization**: Required for protected endpoints; without it, you'd get 401 Unauthorized
- **Host**: Required because one server might host multiple websites (virtual hosting)

### HTTP Response Anatomy

Every response follows the same structure:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 156
Cache-Control: max-age=3600

{
  "id": 123,
  "name": "John Doe",
  "email": "john@example.com"
}
```

| Part | Example | What it means |
|------|---------|---------------|
| **Status Line** | HTTP/1.1 200 OK | Protocol, numeric code, human-readable phrase |
| **Content-Type** | application/json | "The body below is in JSON format" |
| **Content-Length** | 156 | Size of body in bytes (helps client know when message ends) |
| **Cache-Control** | max-age=3600 | "You can cache this for 1 hour" |

### HTTP Methods — When and Why

Each method has semantics. Using them correctly prevents bugs (like duplicate creates on retry) and makes your API self-documenting.

| Method | Purpose | Safe? | Idempotent? | Request Body? |
|--------|---------|-------|-------------|---------------|
| **GET** | Retrieve data | ✅ Yes | ✅ Yes | ❌ No (ignored) |
| **POST** | Create / trigger action | ❌ No | ❌ No | ✅ Yes |
| **PUT** | Replace entire resource | ❌ No | ✅ Yes | ✅ Yes |
| **PATCH** | Partial update | ❌ No | ⚠️ Depends | ✅ Yes |
| **DELETE** | Remove resource | ❌ No | ✅ Yes | ❌ No |

**Safe** = Does not modify server state. Browsers and crawlers can safely call it.

**Idempotent** = Same request repeated = same result. Critical for retries: if a PUT fails and the client retries, you don't get duplicate or inconsistent data.

**Why this matters in production:** Networks fail. Clients retry. If POST creates a user and the client retries, you might create two users. If PUT updates a user and the client retries, the user is still updated once—same result. Design with retries in mind.

#### GET — Retrieve Resources

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    // GET = read-only; no side effects
    // Spring's @GetMapping maps HTTP GET to this method
    User user = userService.findById(id);
    if (user == null) {
        return ResponseEntity.notFound().build(); // 404
    }
    return ResponseEntity.ok(user); // 200 OK
}
```

**Never use GET for operations that change data.** Browsers cache GET requests; crawlers may trigger them. You could accidentally delete data or create side effects.

#### POST — Create Resources

```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody UserDto userDto) {
    // POST = create new resource; not idempotent
    // @RequestBody tells Spring to parse JSON body into userDto
    User created = userService.create(userDto);
    return ResponseEntity
        .status(HttpStatus.CREATED)        // 201 Created
        .header("Location", "/api/users/" + created.getId())  // REST best practice
        .body(created);
}
```

**201 Created** tells the client "something new was created." The `Location` header points to the new resource—useful for clients that want to fetch it immediately.

#### PUT — Replace Entire Resource

```java
@PutMapping("/users/{id}")
public User updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
    // PUT = replace entire resource; idempotent
    // Same PUT sent twice = same final state
    return userService.replace(id, userDto);
}
```

**Important:** PUT replaces the *whole* resource. If the client sends `{name: "John"}` and omits `email`, your logic should treat `email` as null/removed—not "leave email unchanged."

#### PATCH — Partial Update

```java
@PatchMapping("/users/{id}")
public User partialUpdateUser(@PathVariable Long id,
                              @RequestBody Map<String, Object> updates) {
    // PATCH = update only provided fields
    // {"email": "new@example.com"} leaves name, phone, etc. unchanged
    return userService.partialUpdate(id, updates);
}
```

**PUT vs PATCH:** PUT = "here's the complete new version." PATCH = "here are the fields to change."

#### DELETE — Remove Resources

```java
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    // 204 No Content = success, nothing to return
    return ResponseEntity.noContent().build();
}
```

**204 No Content** is standard for successful DELETE—there's nothing to return.

### URL Structure and Query Parameters

**Path parameters** identify a specific resource:

```
GET /users/123          → User with ID 123
GET /users/123/orders   → Orders belonging to user 123
```

**Query parameters** filter, sort, and paginate:

```
GET /users?role=admin           → Filter by role
GET /users?page=2&size=20       → Pagination
GET /users?sort=name&order=asc  → Sorting
GET /search?q=spring+boot       → Search query
```

**Spring handles both:**

```java
@GetMapping("/users")
public List<User> getUsers(
        @RequestParam(required = false) String role,   // ?role=admin
        @RequestParam(defaultValue = "0") int page,   // ?page=0
        @RequestParam(defaultValue = "20") int size) { // ?size=20
    return userService.findAll(role, page, size);
}
```

### Status Codes — Choose Correctly

Status codes are part of the HTTP contract. Misusing them (e.g., 200 for errors) breaks client logic and makes debugging harder.

#### Success (2xx)

| Code | Name | When to Use |
|------|------|-------------|
| **200** | OK | Standard success; body contains data |
| **201** | Created | Resource created; include `Location` header |
| **204** | No Content | Success but no body (e.g., DELETE) |

#### Client Errors (4xx)

| Code | Name | When to Use |
|------|------|-------------|
| **400** | Bad Request | Malformed request, invalid syntax, bad JSON |
| **401** | Unauthorized | Not authenticated (missing/invalid token) |
| **403** | Forbidden | Authenticated but not allowed (wrong role) |
| **404** | Not Found | Resource does not exist |
| **409** | Conflict | Business rule violation (e.g., duplicate email) |
| **422** | Unprocessable Entity | Valid syntax but validation failed |
| **429** | Too Many Requests | Rate limit exceeded |

**401 vs 403:**
- **401** = "Who are you?" — Authentication failed
- **403** = "I know who you are, but you can't do this" — Authorization failed

#### Server Errors (5xx)

| Code | Name | When to Use |
|------|------|-------------|
| **500** | Internal Server Error | Unexpected server error |
| **502** | Bad Gateway | Upstream server returned invalid response |
| **503** | Service Unavailable | Overloaded, maintenance, temporarily down |

**Never return 5xx for client mistakes.** Bad input = 4xx. Server bugs = 5xx.

### Important Headers

| Header | Direction | Purpose |
|--------|-----------|---------|
| `Content-Type` | Request/Response | Format of body (e.g., `application/json`) |
| `Accept` | Request | Formats client accepts |
| `Authorization` | Request | Credentials (e.g., `Bearer <token>`) |
| `Cache-Control` | Response | Caching rules |
| `Location` | Response | URL of created resource (with 201) |
| `Access-Control-Allow-Origin` | Response | CORS; which origins can call this API |

---

## Layer 3 — Advanced Engineering Depth

### REST Architectural Constraints

REST (Representational State Transfer) is an **architectural style**, not a protocol. Roy Fielding defined six constraints:

1. **Client-Server** — Clear separation; client and server evolve independently.
2. **Stateless** — Each request is self-contained; no server-side session.
3. **Cacheable** — Responses declare whether they can be cached (`Cache-Control`, `ETag`).
4. **Uniform Interface** — Resources, URLs, HTTP methods, and status codes used consistently.
5. **Layered System** — Proxies, gateways, load balancers can sit between client and server.
6. **Code on Demand (optional)** — Server can send executable code; rarely used today.

### Statelessness — Why It Matters

**Stateless** means the server does not store request-specific state between calls. Every request carries all needed information (e.g., token in `Authorization`).

**Benefits:**
- **Horizontal scaling** — Any server can handle any request.
- **No sticky sessions** — Load balancers can route freely.
- **Simpler failure recovery** — No session replication.

**Anti-pattern:**

```
Request 1: POST /login (username, password)  → Server stores "user 123 is logged in"
Request 2: GET /profile  → Server expects to "remember" request 1
```

**Correct pattern:**

```
Request 1: POST /login → Returns token
Request 2: GET /profile (Authorization: Bearer <token>)  → Token carries identity
```

### REST Richardson Maturity Model

A model for how "RESTful" an API is:

| Level | Name | Description | Example |
|-------|------|-------------|---------|
| **0** | POX (Plain Old XML) | Single endpoint, all via POST | `POST /api` with `{action: "getUser", id: 123}` |
| **1** | Resources | One URI per resource | `POST /users/getUser`, `POST /users/createUser` |
| **2** | HTTP Verbs | Correct use of GET, POST, PUT, DELETE | `GET /users/123`, `POST /users` |
| **3** | Hypermedia (HATEOAS) | Responses include links to related resources | JSON with `_links` for self, orders, etc. |

Most production APIs are Level 2. Level 3 adds complexity and is rare.

### Content Negotiation

Client and server agree on representation format via headers:

**Client:**
```
Accept: application/json
Accept: application/json;q=1.0, application/xml;q=0.8
```

**Server:**
```
Content-Type: application/json
```

**Spring example:**

```java
@GetMapping(value = "/users/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public ResponseEntity<User> getUser(@PathVariable Long id) {
    // Spring selects format based on Accept header
    return ResponseEntity.ok(userService.findById(id));
}
```

`q` values indicate preference (1.0 = highest).

### RESTful URL Design

**Use nouns, not verbs:**

| ❌ Bad | ✅ Good |
|--------|---------|
| `GET /getUser/123` | `GET /users/123` |
| `POST /createUser` | `POST /users` |
| `DELETE /removeUser/123` | `DELETE /users/123` |

**Plural for collections:**
- `GET /users` — collection
- `GET /users/123` — single resource

**Avoid deep nesting:** Prefer `GET /orders?userId=123` over `GET /users/123/orders/456/items/789`.

### API Versioning

**URL versioning** (most common):

```
GET /api/v1/users/123
GET /api/v2/users/123
```

- Pros: Simple, cacheable, easy to route.
- Cons: URLs change across versions.

**Header versioning:**

```
GET /api/users/123
Accept: application/vnd.api.v1+json
```

- Pros: Clean URLs.
- Cons: Less discoverable, harder to test.

**Recommendation:** Use URL versioning (`/api/v1/`) for most APIs.

### Idempotency and Retries

**Idempotent:** Same request N times = same result as once.

- GET, PUT, DELETE, HEAD, OPTIONS: idempotent.
- POST: usually not idempotent.

**For POST that must be safe to retry** (e.g., payments), use **idempotency keys**:

```http
POST /api/payments
Idempotency-Key: unique-key-abc123
Content-Type: application/json

{"amount": 100, "currency": "USD"}
```

Server stores the key with the result. A retry with the same key returns the same result without processing again.

---

## Layer 4 — Interview Mastery

### Q1: What is HTTP? What is REST?

**HTTP** is a request–response protocol for the web. The client sends a request (method, URL, headers, optional body); the server responds with a status, headers, and optional body.

**REST** is an architectural style for web services. It emphasizes resources (nouns in URLs), standard HTTP methods, statelessness, and consistent use of status codes. Most "REST APIs" are Level 2 of the Richardson Maturity Model (resources + HTTP verbs).

### Q2: What's the difference between 401 and 403?

**401 Unauthorized** — Authentication failure. The client needs to prove identity (e.g., valid token). Use `WWW-Authenticate` to tell the client how.

**403 Forbidden** — Authorization failure. The client is authenticated but not allowed to perform this action.

Mnemonic: 401 = "Who are you?" / 403 = "I know you, but you can't do this."

### Q3: What's the difference between PUT and PATCH?

**PUT** — Replaces the entire resource. Sends full representation; unspecified fields become null/defaults. Idempotent.

**PATCH** — Partially updates a resource. Sends only changed fields; unspecified fields stay as-is. Idempotency depends on implementation.

### Q4: What does idempotent mean? Why does it matter?

**Idempotent** = Executing the same request multiple times has the same effect as executing it once.

**Why it matters:** Networks fail and clients retry. Idempotent methods (GET, PUT, DELETE) can be retried safely. POST usually is not; use idempotency keys for critical operations like payments.

### Q5: When would you use 400 vs 422?

**400 Bad Request** — Malformed request: invalid JSON, wrong syntax, invalid structure.

**422 Unprocessable Entity** — Valid syntax but semantic/validation errors: e.g., invalid email format, negative age, duplicate email.

### Q6: What makes an API "RESTful"?

- Resource-based URLs (nouns: `/users`, `/orders`).
- Correct use of HTTP methods (GET, POST, PUT, PATCH, DELETE).
- Stateless—no server-side session.
- Proper HTTP status codes (2xx, 4xx, 5xx).
- Consistent, self-descriptive messages (headers, content types).

### Q7: What is HATEOAS? Is it worth using?

**HATEOAS** (Hypermedia As The Engine Of Application State) means responses include links to related resources and available actions.

**Worth it?** Rarely. It adds complexity and most clients don't use it. Clear documentation and stable URLs usually suffice. Mention it to show REST depth, but most APIs don't implement it.

### Q8: How do you handle API versioning?

**URL versioning** (`/api/v1/users`) is most common: simple, cacheable, easy to test and route. Header or media-type versioning keeps URLs clean but is harder to work with. Use URL versioning unless there’s a strong reason not to.

---

## Summary

| Concept | Key Takeaway |
|---------|--------------|
| **HTTP** | Request–response protocol; client asks, server answers |
| **Methods** | GET (read), POST (create), PUT (replace), PATCH (partial update), DELETE (remove) |
| **Status codes** | 2xx success, 4xx client error, 5xx server error; use them correctly |
| **Headers** | Metadata for content type, auth, caching, CORS |
| **URLs** | Path = resource identity; query params = filter, sort, paginate |
| **REST** | Resources as nouns, stateless, proper methods and status codes |
| **Idempotency** | Safe retries; important for PUT, DELETE; use keys for critical POST |

Understanding these concepts turns you from "someone who uses Spring Boot" into "someone who designs and debugs HTTP APIs effectively."

---

[← Back to Index](00-README.md) | [Previous: Design Patterns](03-Prerequisites-Design-Patterns.md) | [Next: Web Fundamentals →](05-Prerequisites-Web-Fundamentals.md)
