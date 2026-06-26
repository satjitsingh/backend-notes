# 23. REST, GraphQL, and gRPC

> Services and clients need a *language and contract* to talk. These three are the dominant choices, and they make very different trade-offs. REST is the universal default; GraphQL solves the "too many/too few fields" problem; gRPC is the high-performance choice for service-to-service calls. Knowing which fits where is core API design.

---

## What is it?

These are three **API styles / protocols** — conventions for how a client requests data/actions from a server and gets responses. They all let machines talk, but they optimize for different things.

**REST (Representational State Transfer)** — an architectural *style* (not a strict protocol) built on top of HTTP. You model everything as **resources** (nouns) identified by URLs, and act on them with **HTTP verbs** (GET, POST, PUT, DELETE). Responses are usually JSON. It's the lingua franca of web APIs — universal, simple, cacheable.

**GraphQL** — a **query language for APIs** (created by Facebook). Instead of many fixed endpoints, there's usually **one endpoint**, and the *client* specifies exactly what data it wants in a query. The server returns precisely that — no more, no less. It solves the over-fetching/under-fetching problems of REST.

**gRPC** — a high-performance **Remote Procedure Call** framework (created by Google) built on **HTTP/2** and **Protocol Buffers** (a compact binary format). Instead of thinking in resources, you call **functions/methods** on a remote service as if they were local (`paymentService.ProcessPayment(request)`). It's fast, strongly-typed, and ideal for internal service-to-service communication.

The one-line mental model:

> **REST** = resources over HTTP, universal and simple (the default for public APIs). **GraphQL** = client asks for exactly the fields it wants from one flexible endpoint (great for varied/mobile clients and complex data). **gRPC** = fast, binary, typed function calls between services (the default for internal microservice-to-microservice traffic).

---

## How it Works Under the Hood

### REST

REST organizes the API around **resources** and uses HTTP's built-in semantics:
```
GET    /accounts/A1            → fetch account A1
GET    /accounts/A1/transactions → fetch A1's transactions
POST   /payments               → create a payment (body has details)
DELETE /cards/C5               → delete card C5
```
- **Stateless** (Chapter 2): each request carries everything needed; the server stores no client session — which is exactly what lets REST APIs scale horizontally.
- **Uses HTTP verbs & status codes** meaningfully (200 OK, 201 Created, 404 Not Found, 500 Server Error).
- **Cacheable:** GET responses can be cached by browsers, CDNs, and proxies using standard HTTP caching (Chapter 17) — a major, often-overlooked REST advantage.

**REST's two pain points** (which the others address):
- **Over-fetching:** an endpoint returns a *fixed* shape, often more than you need. `GET /accounts/A1` might return 30 fields when the mobile app wanted 3 — wasted bandwidth.
- **Under-fetching (N+1 / round trips):** one endpoint isn't enough, so you call several. To show a screen you might call `/accounts/A1`, then `/accounts/A1/transactions`, then `/accounts/A1/cards` — 3 round trips (latency, Chapter 3). This is the very problem the BFF aggregated away in Chapter 22.

### GraphQL

GraphQL flips control to the **client**. There's a **schema** (a strongly-typed contract of all available data and types), and clients send **queries** specifying the exact shape they want:
```graphql
query {
  account(id: "A1") {
    balance                    # only the fields I ask for
    transactions(last: 5) {    # nested, in ONE request
      amount
      merchant
    }
  }
}
```
The server resolves this and returns **exactly** that JSON shape — solving over-fetching (you get only requested fields) *and* under-fetching (nested/related data in **one** round trip).

How it works mechanically: the server defines **resolvers** — functions that know how to fetch each field/type. The GraphQL engine parses the query, calls the needed resolvers, assembles the response. 

**GraphQL's costs:**
- **Caching is harder** — it's typically one POST endpoint, so simple HTTP/URL caching (REST's strength) doesn't apply; you need application-level or specialized caching.
- **Complexity on the server** — schema, resolvers, and guarding against expensive/malicious deeply-nested queries (a client could request a hugely expensive query).
- **The N+1 problem moves to resolvers** — naive resolvers fire a DB query per item; mitigated with batching tools (e.g., DataLoader).

### gRPC

gRPC is about **speed and strong typing for service-to-service** calls. Its foundations:
- **Protocol Buffers (Protobuf):** you define the service and message types in a `.proto` file (the contract). This compiles into typed client/server code in many languages. Messages are serialized to a **compact binary format** — much smaller and faster to parse than JSON text.
```protobuf
service PaymentService {
  rpc ProcessPayment(PaymentRequest) returns (PaymentResponse);
}
message PaymentRequest { string account_id = 1; int64 amount = 2; }
```
- **HTTP/2 transport:** enables **multiplexing** (many calls over one connection), header compression, and **bidirectional streaming** — far more efficient than REST's typical HTTP/1.1 request-per-connection.
- **RPC model:** you call remote methods like local functions; the framework handles serialization and networking. Strongly-typed contracts catch mismatches at compile time.

**gRPC's costs:**
- **Not browser-friendly** — browsers can't easily speak raw gRPC (needs a proxy like gRPC-Web), so it's mostly for *internal* traffic, not public/browser-facing APIs.
- **Binary = not human-readable** — harder to debug by eye than JSON; needs tooling.
- **Tighter coupling** to the shared `.proto` contract.

### The comparison
| | REST | GraphQL | gRPC |
|---|---|---|---|
| Style | Resources over HTTP | Client-specified queries | Function calls (RPC) |
| Format | JSON (text) | JSON (text) | Protobuf (**binary**) |
| Transport | HTTP/1.1+ | HTTP (usually one POST) | **HTTP/2** |
| Over/under-fetching | Common problem | **Solved** | N/A (fixed contracts) |
| Caching | **Easy** (HTTP) | Harder | Harder |
| Performance | Good | Good | **Best** (binary, HTTP/2) |
| Browser-friendly | **Yes** | Yes | No (needs proxy) |
| Streaming | Limited | Subscriptions | **Native** (bidirectional) |
| Best for | Public APIs, CRUD | Flexible/mobile clients, complex data | Internal microservices, low-latency |

---

## Why do we need them (and the choice)?

We need to choose deliberately because **each style optimizes for a different axis — universality, flexibility, or performance — and using the wrong one creates friction:**

1. **REST exists for universality and simplicity.** It's built on HTTP, so *everything* understands it — browsers, tools, caches, every language. For public APIs and straightforward CRUD, it's the safe, interoperable, cacheable default. The whole web runs on it.

2. **GraphQL exists to fix over/under-fetching.** When clients are varied (especially mobile) and need different slices of complex, interrelated data, REST forces wasteful over-fetching or chatty multi-call under-fetching. GraphQL lets each client get *exactly* what it needs in *one* request — a big win for mobile latency and for evolving frontends.

3. **gRPC exists for internal performance.** When services call each other millions of times (Chapter 20's network calls), JSON-over-HTTP/1.1 is wasteful. gRPC's binary format + HTTP/2 multiplexing + typed contracts make internal calls fast, efficient, and safe — ideal for the high-volume east-west traffic between microservices.

**When to use which:**
- **REST:** public-facing APIs, simple CRUD, when caching and broad compatibility matter. *The default.*
- **GraphQL:** client-facing APIs with diverse clients, complex nested data, mobile apps that need to minimize payload and round trips, rapidly-evolving frontends. (Often *is* the BFF layer, Chapter 22.)
- **gRPC:** internal service-to-service communication, low-latency/high-throughput needs, polyglot microservices needing strict typed contracts, streaming.

These aren't mutually exclusive — large systems commonly use **all three**: REST/GraphQL at the edge for clients, gRPC internally between services.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** uses all three, each where it fits — exactly the polyglot-protocol reality of modern systems.

**gRPC — internal service-to-service (the high-volume core).** When the Payment service needs a fraud check, it calls the Fraud service via **gRPC**: `fraudService.ScorePayment(request)`. Why gRPC here:
- These internal calls happen at huge volume (every payment), so the **binary Protobuf + HTTP/2** efficiency directly reduces latency and CPU vs JSON (Chapter 3).
- The strongly-typed `.proto` contract means a mismatch between Payment and Fraud is caught at **compile time**, not in production — invaluable when teams own different services (Chapter 20).
- It's internal, so gRPC's browser-unfriendliness doesn't matter.

**REST — the partner/public API.** Third-party merchants integrate via a **REST** API (`POST /payments`, `GET /transactions/{id}`). Why REST:
- **Universal compatibility** — any partner in any language/tool can consume it immediately.
- **Cacheable** GETs (transaction lookups) via standard HTTP caching/CDN (Chapter 17).
- Stable, well-understood, easy to document and version — the right choice for an external contract.

**GraphQL — the mobile/web BFF.** The mobile app's home screen needs balance + recent transactions + notifications. With REST that's 3 calls (under-fetching) or 1 bloated call (over-fetching). Instead, the **Mobile BFF (Chapter 22) exposes GraphQL**, and the app sends one query for *exactly* the fields it needs:
```graphql
query { account(id:"A1"){ balance  transactions(last:5){ amount merchant } notifications(unread:true){ text } } }
```
One round trip, minimal payload — perfect for a phone on a flaky network. The web dashboard uses the *same* GraphQL endpoint but requests *richer* fields (chart data, full details) — the client controls the shape, so one API serves both without bloat. This is GraphQL solving precisely the over/under-fetching problem that motivated the BFF.

**What stays where for safety.** The actual money-moving operation is exposed via **REST/gRPC with explicit, validated, idempotent endpoints** — not a free-form GraphQL mutation that could be abused — because money operations need tight control, idempotency keys (Chapter 18), and auditability. *Use GraphQL's flexibility for reads; keep money writes constrained.*

In Spring Boot: REST via `spring-boot-starter-web` (`@RestController`), GraphQL via `spring-boot-starter-graphql` (schema + `@QueryMapping`/`@SchemaMapping` resolvers), and gRPC via the `grpc-spring-boot-starter` (generated stubs from `.proto`). The API Gateway (Chapter 22) does **protocol translation** — accepting REST/GraphQL from clients and calling internal services over gRPC.

---

## Trade-offs (Pros & Cons)

### REST
**Pros:** universal compatibility; simple and well-understood; **excellent HTTP caching**; stateless and scalable; great for public APIs and CRUD.
**Cons:** over-fetching and under-fetching (multiple round trips); fixed response shapes; less efficient (verbose JSON text); no native streaming.

### GraphQL
**Pros:** client gets **exactly** the data it needs (no over/under-fetching); one request for complex nested data; strongly-typed schema; great for diverse/mobile clients and evolving frontends.
**Cons:** **caching is hard** (loses HTTP caching); server complexity (resolvers, query-cost limiting); N+1 risk in resolvers; can expose expensive/abusive queries if unguarded.

### gRPC
**Pros:** **highest performance** (binary Protobuf + HTTP/2 multiplexing); strongly-typed contracts (compile-time safety); native bidirectional **streaming**; polyglot codegen; ideal for internal microservices.
**Cons:** **not browser-friendly** (needs proxy); binary = harder to debug/read; tighter coupling to `.proto` contract; steeper learning curve; weaker ad-hoc tooling than REST.

> **Staff-engineer takeaway:** There's no single winner — match the style to the traffic. **REST** is the universal, cacheable default for **public APIs and CRUD**. **GraphQL** solves **over/under-fetching** for **diverse client-facing apps** (especially mobile, often as the BFF) — at the cost of caching simplicity. **gRPC** is the **fast, typed choice for internal service-to-service** calls — but not for browsers. Large systems use **all three**: GraphQL/REST at the edge, gRPC between services, with the API Gateway translating. For fintech, keep **money writes on tightly-controlled, idempotent REST/gRPC endpoints** and use GraphQL's flexibility for reads.

---

➡️ Next: [24-Long-Polling-WebSockets-SSE.md](24-Long-Polling-WebSockets-SSE.md) — all of the above are request/response (client asks, server answers). But what if the *server* needs to push data to the client in real time? That's a different problem.

---
