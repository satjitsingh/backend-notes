# 4. Architectural Patterns

> Before we tune databases and caches, we need to decide the *shape* of the whole system. Architectural patterns are the proven blueprints for that shape — the difference between one big building and a city of connected buildings.

---

## What is it?

An **architectural pattern** is a **reusable, high-level blueprint for organizing an entire software system** — how you split the code into pieces, where those pieces run, and how they talk to each other.

Think of it this way. When an architect designs a building, they don't invent the concept of "a building" from scratch every time. They pick from known blueprints: a single-family house, an apartment complex, a shopping mall. Each blueprint solves a different problem and comes with known trade-offs. Software architecture works the same way. Over decades, the industry has converged on a handful of patterns that repeatedly solve the problem of *"how do I structure a system so it's understandable, changeable, and able to scale?"*

It's important to separate **architectural patterns** from **design patterns** (which get their own chapter later):

- **Design patterns** are small-scale. They solve problems *inside* one program — how to structure a class, how objects collaborate (Singleton, Factory, Observer, etc.). They're about *code*.
- **Architectural patterns** are large-scale. They decide how the *whole system* is split across processes, servers, and teams. They're about *systems*.

This chapter focuses on the big three you'll meet constantly:

1. **Layered (N-tier) architecture** — organize code into horizontal layers (presentation → business → data).
2. **Monolithic architecture** — the entire application is one single deployable unit.
3. **Microservices architecture** — the application is split into many small, independently deployable services.

There are others — **event-driven**, **client-server**, **serverless**, **hexagonal/ports-and-adapters** — and several get dedicated chapters later (event-driven and monolith-vs-microservices especially). Here we build the mental foundation: *what a pattern is, and how the foundational ones differ.*

> **The core idea to lock in:** An architectural pattern is a *decision about boundaries* — where you draw the lines that separate one part of the system from another. Everything else (deployment, scaling, team structure) flows from where you draw those lines.

---

## How it Works Under the Hood

Let's walk through how each foundational pattern actually organizes a system.

### 1. Layered (N-tier) architecture

This is the pattern most backend code already uses, often without naming it. You slice the application into **horizontal layers**, where each layer has one responsibility and only talks to the layer directly below it.

```
   ┌─────────────────────────────┐
   │  Presentation Layer          │  <- handles HTTP, JSON in/out (Controllers)
   ├─────────────────────────────┤
   │  Business / Service Layer    │  <- the actual rules & logic (Services)
   ├─────────────────────────────┤
   │  Data Access Layer           │  <- talks to the database (Repositories)
   ├─────────────────────────────┤
   │  Database                    │  <- stores the data
   └─────────────────────────────┘
```

The rule that makes it work: **dependencies point downward only.** The presentation layer calls the service layer; the service layer calls the data layer. The data layer never reaches *up* to call a controller. This one-way flow keeps responsibilities clean — you can rewrite the database layer without touching business logic, or swap the web framework without rewriting your rules.

If you've written Spring Boot, you've lived this pattern: `@RestController` (presentation) → `@Service` (business) → `@Repository` (data access) → database. That's textbook layered architecture.

> Layered architecture is about *internal* organization. Crucially, it says nothing about *deployment* — a layered app can still be a single monolith or be split into microservices. It's an orthogonal concern.

### 2. Monolithic architecture

A **monolith** packages the *entire* application — all features, all layers — into **one single deployable unit** (e.g., one `.jar` file you run on a server).

```
        ┌──────────────────────────────────────┐
        │            ONE Application             │
        │  ┌────────┐ ┌────────┐ ┌────────────┐ │
        │  │Payments│ │ Users  │ │Notifications│ │
        │  └────────┘ └────────┘ └────────────┘ │
        │       all sharing ONE database         │
        └──────────────────────────────────────┘
                          │
                          v
                   [ One Database ]
```

How it runs: all the modules (payments, users, notifications) live in the same codebase and run *in the same process*. When module A needs something from module B, it's just a **normal function call in memory** — instant, reliable, no network involved. You build it into one artifact and deploy that one artifact.

This simplicity is its superpower *and* its eventual problem. Early on, everything is easy: one codebase to understand, one thing to deploy, one database, transactions that "just work" because everything shares the same database connection. But as it grows to hundreds of developers and millions of lines, the monolith becomes hard to understand, slow to build and deploy, and impossible to scale *selectively* — you must scale the *whole* thing even if only one feature is hot.

### 3. Microservices architecture

**Microservices** split the application into **many small, independent services**, each owning one business capability, each **running as its own process** (often on its own servers), each typically with **its own database**.

```
   [ API Gateway ]
    /     |      \
   v      v       v
[Payment][User ][Notification]   <- separate apps, separate teams,
 Service  Service  Service          separate deployments
   |        |          |
   v        v          v
 [Pay DB][User DB ][Notif DB]    <- each owns its own data
```

How it runs: each service is a standalone application. When the Payment service needs user info, it can't just call a function — the User service is a *different process on a different machine*. So it makes a **network call** (a REST/gRPC request, or sends a message over a queue). This is the defining shift: **in-memory function calls become network calls.** That single change buys independence but introduces the entire universe of distributed-systems problems — network failures, latency, partial failures, and eventual consistency.

Each service can be:
- **Developed independently** by a separate team.
- **Deployed independently** — ship the Payment service without redeploying everything.
- **Scaled independently** — run 40 copies of the hot Payment service and just 2 of the rarely-used Notification service.
- **Built with different tech** — one service in Java/Spring Boot, another in Go, if it makes sense.

> **The deep trade-off in one sentence:** Monoliths make *development simple and operations complex at scale*; microservices make *operations flexible but development and coordination complex*. You're choosing *where* to put the complexity, not whether to have it.

### How you actually choose a pattern

You don't pick a pattern because it's trendy. You pick based on:
- **Team size** — 5 engineers? A monolith is almost always right. 500 engineers across 50 teams? Microservices let teams move without stepping on each other.
- **Scale needs** — do different features have wildly different load profiles that need independent scaling?
- **Domain clarity** — microservices need clear boundaries between business capabilities. If you don't understand your domain yet, drawing service boundaries early will cut them in the wrong places (very expensive to fix later).
- **Operational maturity** — microservices demand serious infrastructure: service discovery, monitoring, distributed tracing, CI/CD per service. Without it, you get a "distributed monolith" — all the pain, none of the benefits.

---

## Why do we need it?

We need architectural patterns because **structure is the thing that decides whether a system can survive growth — of users *and* of the engineering team.**

1. **They manage complexity.** A large system without a deliberate structure becomes a "big ball of mud" where everything depends on everything, and no one dares change anything. Patterns impose boundaries that keep complexity in check.

2. **They enable teams to work in parallel.** Clear boundaries (especially with microservices) mean Team A can ship without coordinating with Team B. The architecture literally shapes how your organization can work — this is **Conway's Law**: *systems tend to mirror the communication structure of the org that builds them.*

3. **They let you scale the right things.** A good architecture lets you throw resources at exactly the hot part. A bad one forces you to scale everything together, wasting money.

4. **They make change safe.** The whole point of boundaries is that you can change what's *inside* one boundary without breaking what's *outside* it. That's what keeps a system maintainable over years.

**When to use which (the honest guidance):**
- **Start with a well-structured monolith** (using clean layers and clear internal modules — a "modular monolith"). This is the right default for almost every new project. It's simple, fast to build, and easy to reason about.
- **Move to microservices when the pain is real**: the team is too large to coordinate on one codebase, build/deploy times are crippling, or different parts genuinely need independent scaling. Migrate by carving services off the monolith one at a time — never rewrite from scratch.

> The biggest architectural mistake in the industry is **starting with microservices for a small team and a product you don't fully understand yet.** You inherit massive operational complexity to solve scaling and team-coordination problems you don't have. Earn your way to microservices.

---

## Real-World / Fintech Example

Let's trace our **digital wallet / payments app** through architectural evolution.

**Day 1 — The modular monolith.**
The startup has 6 engineers. They build one Spring Boot application with clean internal modules: `payments`, `accounts`, `notifications`, `fraud`. Internally it's layered — controllers → services → repositories — and everything shares one PostgreSQL database. 

This is *exactly the right choice*. A money transfer (debit Alice, credit Bob) is a single in-process database transaction (`@Transactional`) — atomic, consistent, trivially correct, because it all happens in one database. Six engineers can hold the whole system in their heads. They ship fast.

**Year 2 — The strain shows.**
Now there are 80 engineers, the app handles huge load, and problems appear:
- The `fraud` module runs heavy ML scoring that hogs CPU and slows down *payments*, even though they're unrelated — because they share the same process and servers.
- Every tiny change requires rebuilding and redeploying the *entire* monolith, and the test suite takes 40 minutes. Deploys are scary and rare.
- Teams constantly collide in the same codebase.

**Year 2.5 — Carving off microservices.**
They don't rewrite. They **extract services one at a time**, starting with the most painful boundary:
- **Notification Service** goes first (it's the most independent — it just consumes "payment happened" events and sends messages). Now it scales and deploys on its own.
- **Fraud Service** goes next, so its hungry ML workload runs on its own beefy machines and never starves payments again. Payments talk to it asynchronously over **Kafka**, so a slow fraud check never blocks a user's confirmation.
- The **core Payments + Ledger** stays as a focused service, deliberately kept together — because splitting the debit and credit across two services would turn a simple database transaction into a hard distributed-transaction problem (the kind that needs sagas and eventual consistency).

Notice the wisdom in *what they kept together*: the money-movement core stayed monolithic precisely because **strong consistency is easiest within a single service and database.** They only split off the parts that benefited from independence (notifications, fraud) and could tolerate eventual consistency.

**The result:** a system shaped by its real needs — a strongly-consistent payments core, surrounded by independently-scaling satellite services, communicating via events. That's not "monolith vs. microservices" as a religion; it's *using each pattern where it fits.*

---

## Trade-offs (Pros & Cons)

### Layered (N-tier) architecture
**Pros**
- Simple, familiar, and enforces clean separation of concerns.
- Easy to test layers in isolation and swap one layer's implementation.

**Cons**
- Can become rigid; a simple change may have to ripple through every layer.
- Doesn't address deployment or scaling at all (it's only internal structure).

### Monolithic architecture
**Pros**
- **Simplest to build, test, and deploy** — one codebase, one artifact, one database.
- **Strong consistency is easy** — cross-module operations are single in-process transactions.
- **Fast in-memory calls** — no network latency between modules.
- **Easiest to debug** — one process, one log, one place to look.

**Cons**
- **Scales as one blob** — you can't scale just the hot feature; you scale everything.
- **Slow builds/deploys at size** — the whole thing rebuilds for a one-line change.
- **Tight coupling creeps in** — over time modules entangle and changes get risky.
- **One bug can take down everything** — a memory leak in one module crashes the whole app.

### Microservices architecture
**Pros**
- **Independent deploy, scale, and tech choices** per service.
- **Team autonomy** — many teams ship in parallel without colliding.
- **Fault isolation** — one service crashing doesn't necessarily kill the others.
- **Targeted scaling** — spend resources only where the load actually is.

**Cons**
- **Massive operational complexity** — service discovery, monitoring, tracing, CI/CD per service.
- **Distributed-systems problems everywhere** — network failures, latency, partial failures.
- **Consistency becomes hard** — cross-service transactions need sagas and eventual consistency.
- **Harder to debug** — one user action may span ten services and ten logs.
- **Easy to get wrong** — bad boundaries create a "distributed monolith": all the pain, none of the gain.

> **Staff-engineer takeaway:** Architecture is about *where you draw boundaries*. Default to a **modular monolith** — it's the cheapest place to keep complexity for most teams. Extract microservices **incrementally and only when the pain (team size, deploy speed, independent scaling) is real**, and keep your strongly-consistent core (like a money ledger) together. The pattern is a tool, not a trophy.

---

➡️ Next: [05-Availability-and-Availability-Patterns.md](05-Availability-and-Availability-Patterns.md) — how we keep a system *up* even when its parts fail.
