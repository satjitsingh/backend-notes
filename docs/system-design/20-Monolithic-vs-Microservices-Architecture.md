# 20. Monolithic vs. Microservices Architecture

> The most debated decision in modern backend architecture — and the most misunderstood. We introduced both in Chapter 4; now we go deep on the *real* trade-offs, the migration path, and why "microservices" is an organizational decision dressed up as a technical one. The honest answer surprises most people: **start with a monolith.**

---

## What is it?

This is the choice between two ways of structuring an application's deployment (recap and deepen from Chapter 4):

**Monolithic architecture** — the entire application is **one deployable unit**. All features (payments, accounts, notifications, fraud) live in one codebase, run in one process, and usually share one database. Components talk via **in-memory function calls**.

**Microservices architecture** — the application is split into **many small, independent services**, each owning one business capability, each running as its own process (often its own database), communicating over the **network** (REST/gRPC calls or messages via Kafka/RabbitMQ from Chapter 19).

The single most important reframing of this whole debate:

> **This is not primarily a technical decision — it's an *organizational* one.** Microservices exist mainly to let *many teams work independently* without colliding. The technical benefits (independent scaling, fault isolation) are real but secondary. If you have a small team, you get all the microservices *pain* (distributed systems) and little of the *benefit* (team autonomy). This is **Conway's Law** in action (Chapter 4): your architecture ends up mirroring your org structure, so choose the architecture that fits your *org*, not the hype.

A crucial third option that's often forgotten: the **Modular Monolith** — a single deployable unit that is *internally* divided into clean, well-bounded modules with strict interfaces. You get the simplicity of a monolith with much of the organization of microservices, and an easy path to extract services later if needed. For most teams, this is the *right default*.

---

## How it Works Under the Hood

### The fundamental shift: in-memory calls become network calls

Everything that's harder about microservices flows from one change. In a monolith, when the payment module needs user data, it's a function call: instant, reliable, transactional, type-safe. In microservices, the Payment service must make a **network call** to the User service. That seemingly small change introduces an entire universe of problems:

- **The network is unreliable** — calls fail, time out, arrive twice, or arrive late. Every cross-service call must handle this (retries, timeouts, circuit breakers — later chapter).
- **Latency** — a network round trip is *thousands* of times slower than a function call (Chapter 3). A user action spanning 10 services accumulates 10 hops of latency.
- **Partial failure** — in a monolith, the process is either up or down. In microservices, *some* services are up and others down, simultaneously. The system must function in this messy in-between (graceful degradation, Chapter 5).
- **No distributed transactions** — the easy `@Transactional` money transfer (Chapter 11) is impossible across services; you need **sagas** (Chapter 14) with eventual consistency.
- **Data consistency** — each service owns its data, so there's no single database to query or JOIN across (Chapter 15's denormalization, events).

> This is the heart of it: **microservices trade in-process simplicity for the full complexity of distributed systems** — every concept in Batches 2–3 (availability math, CAP, eventual consistency) becomes a daily concern.

### What microservices require to not be a disaster

Microservices done without supporting infrastructure become a **"distributed monolith"** — all the pain, none of the gain (services so tangled they must deploy together, but now over the unreliable network). To do them properly you need substantial infrastructure:
- **Service discovery** — how services find each other's network addresses.
- **API Gateway** — a single entry point routing to services (its own chapter).
- **Centralized logging & distributed tracing** — to follow one request across many services (you can't just read one log file).
- **Robust monitoring & health checks** per service.
- **CI/CD per service** — independent build/test/deploy pipelines.
- **Resilience patterns** — circuit breakers, retries, timeouts (later chapter).
- **Containerization & orchestration** (Docker + Kubernetes) to manage many deployables.

That list is *why* microservices are expensive: you're not just writing services, you're operating a distributed platform.

### How you decide (the deciding factors)

| Factor | Favors Monolith | Favors Microservices |
|---|---|---|
| **Team size** | Small (1–2 teams) | Large (many teams) |
| **Domain clarity** | Still evolving/unclear | Well-understood boundaries |
| **Scaling needs** | Uniform | Different parts need very different scale |
| **Operational maturity** | Limited DevOps | Strong DevOps/platform team |
| **Deployment independence** | Not needed | Critical (teams ship on own cadence) |
| **Stage** | Startup / new product | Established, large-scale product |

### The right migration path (Strangler Fig)

You almost never *start* with microservices and you never *rewrite* a monolith into them in one big bang (a notorious way to fail). The proven approach is the **Strangler Fig pattern**:
1. Start with a **modular monolith**.
2. When real pain appears (team coordination, scaling, deploy speed), **extract one service at a time** — usually the most independent / most painful module first.
3. Route traffic to the new service, gradually "strangling" the monolith until what remains is small (or gone).

This incremental path lets you learn, keeps the system working throughout, and means you only extract what genuinely benefits — never paying for boundaries you don't need.

---

## Why do we need it (the choice)?

We need to make this choice deliberately because **getting it wrong in either direction is expensive — and the industry's default bias toward microservices causes enormous, avoidable pain.**

**Why monoliths are the right default (the under-told story):**
1. **Simplicity** — one codebase, one deploy, one database; easy to develop, test, and debug.
2. **Strong consistency is free** — cross-feature operations are single in-process ACID transactions (priceless for money, Chapter 11).
3. **Fast in-memory calls** — no network latency between components.
4. **You can move fast early** — a small team ships features instead of fighting infrastructure.
> A well-built modular monolith serves *far* more scale than most teams ever reach. Companies like Shopify and (early) Instagram ran enormous monoliths successfully.

**Why and when microservices become worth it:**
1. **Team autonomy at scale** — 50 teams can't coordinate on one codebase; independent services let them ship in parallel. *This is the #1 real reason.*
2. **Independent scaling** — scale the hot service (payments) to 40 instances and the cold one (settings) to 2, instead of scaling everything together.
3. **Fault isolation** — one service crashing needn't take down the others (with proper resilience).
4. **Independent deployment & tech choices** — ship one service without redeploying everything; use the right language per service.

**When to use which:**
- **Monolith / modular monolith:** new products, small-to-medium teams, unclear domains, limited DevOps. **The default.**
- **Microservices:** large organizations with many teams, well-understood domains, parts needing very different scaling, and the operational maturity to run a distributed platform. **Earn your way to it.**

---

## Real-World / Fintech Example

This is the same evolution we traced in Chapter 4, now with the *full* trade-offs visible. Our **digital wallet / payments app**:

**Stage 1 — Modular Monolith (6 engineers).** One Spring Boot app, clean internal modules (`payments`, `accounts`, `notifications`, `fraud`), one PostgreSQL. The money transfer is a single `@Transactional` method — atomic, strongly consistent, trivially correct (Chapters 11–12). Six engineers ship fast and hold the whole system in their heads. **Exactly right** — microservices here would be self-sabotage.

**Stage 2 — The pain becomes real (80 engineers, high load).** The honest triggers for change appear:
- The `fraud` module's heavy ML scoring hogs CPU and slows down *payments* (can't scale them separately in a monolith).
- 40-minute test suites and scary all-or-nothing deploys; teams collide constantly in one codebase.
- These are *organizational and scaling* pains — the legitimate signal for microservices.

**Stage 3 — Strangler Fig extraction.** They extract services **one at a time**, most-independent-first:
- **Notification service** first (it only consumes `payment-events` from Kafka — Chapter 19 — so it's loosely coupled and easy to carve off). Now it deploys and scales independently.
- **Fraud service** next, onto its own beefy ML machines, communicating **asynchronously** so a slow fraud scan never blocks a payment (Chapter 18). Its CPU appetite no longer starves payments.
- They deliberately **keep the core Payments + Ledger together** as one service — because splitting debit and credit across services would turn a simple ACID transaction into a hard cross-service **saga** (Chapter 14). *Keep strongly-consistent money operations in one service whenever possible.*

**The cost they consciously accept.** Post-extraction, a single payment flow now spans Payments → (async) Fraud → (async) Notification, communicating over Kafka. They've taken on: distributed tracing to debug across services, an API gateway, per-service CI/CD, circuit breakers, and eventual consistency for the async parts. They pay this **distributed-systems tax** *only* because the team-autonomy and independent-scaling benefits now outweigh it — which they did *not* at Stage 1.

> The masterstroke is the *judgment*: monolith when small (speed + consistency), extract services only when org/scale pain is real, and **keep the money core consolidated** for ACID even within a microservices world. They use microservices as a *tool for the parts that benefit*, not a religion applied everywhere.

---

## Trade-offs (Pros & Cons)

### Monolithic architecture
**Pros**
- **Simple** to build, test, deploy, and debug (one codebase, one process, one log).
- **Strong consistency for free** — cross-module ACID transactions.
- **Fast in-memory calls** — no network latency between components.
- **Great for small teams and early stages** — ship features, not infrastructure.

**Cons**
- **Scales as one blob** — can't scale a single hot feature independently.
- **Slow builds/deploys at size**; one bug can crash the whole app.
- **Tight coupling creeps in** over time; large teams collide in one codebase.

### Microservices architecture
**Pros**
- **Team autonomy** — many teams ship independently (the main reason).
- **Independent scaling, deployment, and tech choices** per service.
- **Fault isolation** — one service's failure needn't kill the rest.

**Cons**
- **Full distributed-systems complexity** — network failures, latency, partial failure.
- **No easy transactions** — cross-service ops need sagas + eventual consistency.
- **Heavy operational burden** — gateway, discovery, tracing, per-service CI/CD, orchestration.
- **Hard to debug** — one request spans many services and logs.
- **Easy to get wrong** → "distributed monolith": all pain, no gain.

> **Staff-engineer takeaway:** Monolith vs. microservices is mostly an **organizational** decision (Conway's Law), not a technical one. **Default to a modular monolith** — it's simpler, gives ACID for free, and serves more scale than most teams ever reach. Adopt microservices **incrementally via the Strangler Fig pattern**, and **only when real team-coordination or independent-scaling pain appears** *and* you have the DevOps maturity to run a distributed platform. Even then, **keep strongly-consistent cores (like a money ledger) consolidated** to preserve easy transactions. The goal is fitting the architecture to your organization and requirements — never adopting microservices for fashion.

---

➡️ Next: [21-Event-Driven-Architecture.md](21-Event-Driven-Architecture.md) — the architectural style that makes microservices communicate *well*: reacting to events instead of calling each other directly.
