# 22. API Gateway and Backend for Frontend (BFF)

> Once you have many microservices (Batch 7), a new question appears: how does the *outside world* talk to them? If clients call each service directly, you get chaos. The API Gateway is the single front door that solves this — and the BFF is a specialized variant for when different clients need different doors.

---

## What is it?

**An API Gateway is a single entry point that sits between clients and your backend services, receiving all incoming requests and routing them to the right service — while handling shared concerns like authentication, rate limiting, and routing in one place.**

To see why it's needed, picture the alternative. You've split your app into 20 microservices (payments, accounts, notifications, ...). Without a gateway, the mobile app would need to:
- Know the network address of all 20 services.
- Implement authentication against each one.
- Handle each service's protocol, retries, and failures.
- Make many separate calls to render one screen.

That's a nightmare — the client is now tightly coupled to your internal architecture, and every service must independently re-implement cross-cutting concerns. The **API Gateway** fixes this by being the **one front door**:

```
                          ┌─────────────────┐
[Mobile] ─┐               │                 │ → [Payments Service]
[Web]   ──┼──► all calls ►│   API GATEWAY   │ → [Accounts Service]
[Partner]─┘               │ (single entry)  │ → [Notifications Service]
                          └─────────────────┘ → [Fraud Service]
                           handles: auth, routing, rate-limiting,
                                    SSL, logging, aggregation
```

The gateway is an implementation of the **Façade pattern** (a later design-patterns chapter): it hides the messy internal structure behind one clean interface.

**Backend for Frontend (BFF)** is a *refinement* of this idea. Instead of one gateway serving all clients, you build **a separate, tailored gateway/backend for each type of client** — one BFF for the mobile app, one for the web app, one for partners. Each BFF is optimized for *its* client's specific needs:

> One API Gateway is a single front door for everyone. The BFF pattern says: *different clients have different needs, so give each its own front door, shaped exactly for it.* A mobile app (small screen, limited bandwidth, battery-conscious) wants different, smaller, fewer responses than a data-rich web dashboard.

---

## How it Works Under the Hood

### What an API Gateway actually does (its responsibilities)

The gateway's power is **centralizing cross-cutting concerns** — the things *every* request needs, which you don't want re-implemented in each service:

1. **Routing / Reverse Proxy.** It inspects each request (path, headers) and forwards it to the correct backend service. `/payments/*` → Payment service; `/accounts/*` → Accounts service. Clients only ever see the gateway's address.

2. **Authentication & Authorization.** It validates the caller's identity (e.g., checks the JWT token / API key) *once*, at the edge, before any request reaches internal services. Services can then trust that requests are authenticated. This is huge — auth logic lives in one place, not 20.

3. **Rate Limiting & Throttling.** It caps how many requests a client can make (e.g., 100/min), protecting backends from abuse, runaway clients, and DDoS. (Often implemented with a counter in Redis — Chapter 17.)

4. **Request Aggregation / Composition.** For a screen needing data from 3 services, the gateway (or BFF) can make those 3 calls and **combine** them into one response — so the client makes *one* call instead of three. This is especially valuable for mobile (fewer round trips = less latency, Chapter 3).

5. **Protocol Translation.** Clients speak REST/HTTP; internal services might speak gRPC (next chapter). The gateway translates between them.

6. **Cross-cutting utilities:** SSL/TLS termination, response caching, logging/metrics/tracing (the observability entry point, Chapter 20), request/response transformation, and load balancing across service instances.

> **The mental model:** the API Gateway is where you put everything that *every* request needs but *no single service* should own. It keeps services focused purely on business logic, and keeps clients ignorant of internal structure.

### How the BFF differs and when it kicks in

A single gateway works great until clients diverge significantly. The problem it solves:

- The **web dashboard** wants a rich payment object with full transaction details, charts data, and metadata.
- The **mobile app** wants a *trimmed* payment object — just amount, name, status — to save bandwidth and battery, and pre-formatted for a small screen.

With one shared gateway/API, you either over-serve mobile (wasteful) or build awkward conditional logic. The **BFF pattern** gives each client its own backend layer:

```
[Mobile App] → [Mobile BFF]  ─┐
[Web App]    → [Web BFF]      ─┼──► [Payments][Accounts][Fraud]... (shared services)
[Partners]   → [Partner BFF]  ─┘
```

Each BFF:
- Calls the *same* shared downstream services, but **shapes and aggregates** the data specifically for its client.
- Is typically **owned by the team that owns that frontend** (the mobile team owns the Mobile BFF) — so they can move fast without waiting on a central API team.
- Avoids "lowest common denominator" APIs and conditional bloat.

The trade-off: more components to build and maintain, and some **logic duplication** across BFFs (each may re-implement similar aggregation). You adopt BFF when client needs genuinely diverge enough to justify it — not by default.

### Gateway as a potential bottleneck / SPOF

Since *all* traffic flows through the gateway, it could become a **single point of failure** or bottleneck (Chapters 2, 5). So in production the gateway is itself **horizontally scaled** (multiple stateless instances behind a load balancer) and made highly available. It must also be kept *thin* — heavy business logic in the gateway is an anti-pattern that recreates a monolith at the edge.

---

## Why do we need it?

We need the API Gateway because **it solves the "many services, many clients" problem cleanly — without it, microservices expose their full complexity to every client and duplicate cross-cutting concerns everywhere:**

1. **Decouples clients from internal architecture.** Clients talk to one stable endpoint; you can split, merge, rename, or relocate internal services freely without breaking clients. This freedom to evolve is essential for microservices (Chapter 20).

2. **Centralizes cross-cutting concerns.** Auth, rate limiting, SSL, logging, and tracing are implemented *once* at the gateway instead of being re-built (and inconsistently) in every service. Less code, fewer bugs, consistent security.

3. **Improves client performance.** Aggregation means fewer round trips (critical for mobile latency); caching and compression at the edge speed things up.

4. **Security perimeter.** Internal services aren't exposed to the internet at all — only the gateway is. It's a controlled, hardened front door (and the place to enforce auth and stop abuse).

**Why BFF specifically:** when different clients have genuinely different data/shape/performance needs, BFFs let each client get an optimal API *and* let frontend teams own their backend layer — avoiding the friction of a one-size-fits-all API owned by a separate team.

**When to use:**
- **API Gateway:** essentially any microservices system with external clients. Near-mandatory.
- **BFF:** when you have multiple distinct client types (mobile/web/partner) with diverging needs, and team autonomy benefits outweigh the extra maintenance.
- **When NOT to:** a simple monolith with one client doesn't need a separate gateway (the monolith *is* the single entry point). Don't add a gateway/BFF layer for an app that doesn't have the multi-service or multi-client complexity to justify it.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** now has many microservices (Chapter 20) and several client types — a textbook case for a gateway and BFFs.

**The API Gateway as the single secure front door.** Every request from every client hits the gateway first, which handles:
- **Authentication:** validates the user's JWT *once* at the edge. The internal Payment, Accounts, and Fraud services never re-check identity — they trust the gateway. (For a money app, this single, hardened auth point is a security advantage.)
- **Rate limiting:** caps payment-initiation requests per user (e.g., to blunt automated fraud/abuse), using a Redis counter (Chapter 17). A client hammering the API is throttled *before* reaching the precious payment services.
- **Routing:** `/payments/*` → Payment service, `/kyc/*` → KYC service, etc. The mobile app has no idea these are separate services.
- **Security perimeter:** the internal ledger and fraud services are *not* exposed to the internet at all — only the gateway is, dramatically shrinking the attack surface for a system holding money.

**BFFs for diverging clients.** The app has a mobile app, a web dashboard, and a partner/merchant API — with very different needs:
- **Mobile BFF:** when Alice opens her home screen, the mobile BFF makes *one* call that **aggregates** balance (Accounts service), recent transactions (Ledger service), and notifications (Notification service) into a single, *trimmed* response — small payload, pre-formatted, minimizing round trips on a phone with spotty network and limited battery. Without aggregation, the app would make 3+ separate calls (3× the latency, Chapter 3).
- **Web BFF:** the dashboard's BFF returns *richer* data for the same payment — full details, data for charts, metadata — because the browser has the bandwidth and screen for it.
- **Partner BFF:** exposes a stable, versioned, well-documented API tuned for third-party integrators, with its own stricter rate limits and contracts.

Each BFF calls the *same* underlying services but shapes the data for its client — and the mobile, web, and partner teams each own their BFF, shipping independently (the team-autonomy win from Chapter 20).

**The gateway is scaled and not fat.** Because all traffic flows through it, the gateway runs as **multiple stateless instances behind a load balancer** (Chapters 2, 5) — no single point of failure. The team is disciplined about keeping business logic *out* of the gateway (it does auth/routing/rate-limiting, not payment rules) so it doesn't become a bottleneck monolith.

In Spring Boot terms: the gateway is often **Spring Cloud Gateway** (declarative routes, filters for auth/rate-limiting, integrating with a JWT/OAuth2 resource server and Redis-backed rate limiters); BFFs are typically separate Spring Boot apps owned by each frontend team, calling shared services via REST/gRPC and aggregating responses.

---

## Trade-offs (Pros & Cons)

### API Gateway
**Pros**
- **Single entry point** — clients decoupled from internal service structure; services can evolve freely.
- **Centralized cross-cutting concerns** — auth, rate limiting, SSL, logging/tracing implemented once.
- **Security perimeter** — internal services hidden from the internet.
- **Better client performance** — aggregation (fewer round trips), edge caching, compression.
- **Protocol translation** — clients use REST while services use gRPC, etc.

**Cons**
- **Potential bottleneck / SPOF** — must be scaled out and made highly available.
- **Added latency** — one extra network hop on every request.
- **Risk of becoming a "fat gateway"** — creeping business logic recreates a monolith at the edge.
- **Another component to operate, deploy, and secure.**

### Backend for Frontend (BFF)
**Pros**
- **Client-optimized APIs** — each client gets exactly the data/shape it needs (great for mobile).
- **Team autonomy** — frontend teams own their BFF and ship independently.
- **Avoids one-size-fits-all API bloat** and conditional logic.

**Cons**
- **More components** to build, deploy, and maintain (one per client type).
- **Logic duplication** across BFFs (similar aggregation re-implemented).
- **Overkill** when clients have similar needs — adds complexity for little gain.

> **Staff-engineer takeaway:** An **API Gateway** is the single front door for a microservices system — it decouples clients from internal structure and centralizes cross-cutting concerns (auth, rate limiting, routing, SSL, observability) so services stay focused and clients stay simple. Keep it **thin, stateless, and horizontally scaled** so it doesn't become a fat SPOF. Adopt the **BFF pattern** when client types genuinely diverge (mobile vs web vs partner), giving each its own tailored backend and letting frontend teams own it — but only when the diverging needs justify the extra maintenance. For fintech, the gateway is also your **security perimeter and rate-limiting choke point**, keeping money services off the public internet.

---

➡️ Next: [23-REST-GraphQL-gRPC.md](23-REST-GraphQL-gRPC.md) — the actual protocols and API styles these gateways and services use to communicate, and how to choose among them.
