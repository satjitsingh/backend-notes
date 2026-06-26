# 21. Event-Driven Architecture

> Microservices need to communicate, but *how*? If every service calls every other service directly, you rebuild a tangled, fragile monolith over an unreliable network. Event-Driven Architecture flips the model: services announce *what happened* and others react. It's the communication style that lets distributed systems stay loosely coupled, resilient, and scalable.

---

## What is it?

**Event-Driven Architecture (EDA) is a style where services communicate by producing and reacting to *events* — notifications that "something happened" — instead of directly calling each other to make something happen.**

The key is the shift from **commands** to **events**:

- A **command** is an instruction *to* a specific service: *"PaymentService, process this payment."* The sender knows who should act and waits for it (often synchronous, tightly coupled).
- An **event** is a statement of fact *about the past*, broadcast to anyone interested: *"A payment was completed."* The sender **doesn't know or care who reacts** — it just announces and moves on.

This inversion is profound:

> In traditional (request-driven) architecture, the caller is **in charge** — it tells others what to do and orchestrates the flow. In EDA, the producer simply **publishes a fact** and is done; the *consumers decide* how to react. Control is **inverted** and **decentralized**. The producer has *zero knowledge* of its consumers — you can add ten new reactions to an event without the producer ever knowing.

EDA is built on the **publish-subscribe** model (Chapter 19) and runs on the messaging infrastructure (Kafka especially) from that chapter. It's the natural communication style for microservices (Chapter 20) and the foundation of async processing (Chapter 18) elevated to an architectural principle.

Three common EDA patterns to know:
1. **Event Notification** — a thin event ("payment X happened"); interested services react, possibly fetching more detail if needed.
2. **Event-Carried State Transfer** — the event carries *all* the data consumers need (full payment details), so they don't have to call back — maximizing decoupling.
3. **Event Sourcing** — the *events themselves* become the source of truth: you store the full sequence of events and derive current state by replaying them (more below).

---

## How it Works Under the Hood

### The flow: produce → broker → react

```
                          ┌──────────────────────────┐
[Payment Service] ──emit──►   "PaymentCompleted"      │ (event on Kafka topic)
  (producer; done!)       └──────────┬───────────────┘
                              ┌───────┼────────┬─────────────┐
                              ▼       ▼        ▼             ▼
                    [Notifications][Analytics][Fraud] [Loyalty(added later)]
                       (each consumer reacts independently, at its own pace)
```

1. A service does its work and **emits an event** to a broker (Kafka topic), then moves on. It does *not* wait for or know about reactions.
2. The broker holds/streams the event (and **retains** it, if Kafka — enabling replay, Chapter 19).
3. Any number of **consumers** subscribe and react independently — each at its own speed, each able to fail and recover without affecting the others or the producer.

### Orchestration vs. Choreography (two ways to coordinate a workflow)

When a business process spans multiple services (like our cross-shard payment saga, Chapter 14), there are two coordination styles — a central EDA debate:

**Orchestration** — a central **orchestrator** explicitly directs the workflow: "do step 1, now step 2, now step 3," handling failures and compensation. Like a conductor leading an orchestra.
- **Pro:** the flow is explicit and easy to see/debug in one place; easier error handling.
- **Con:** the orchestrator is a central coupling point and can become complex ("god service").

**Choreography** — there's *no* central coordinator; each service reacts to events and emits new events, and the workflow **emerges** from these reactions. Like dancers responding to each other.
- **Pro:** maximally decoupled; services are independent; easy to add new reactions.
- **Con:** the overall flow is *implicit* — no single place shows "what happens in a payment," making it harder to understand and debug.

> Neither is "correct" — orchestration suits complex flows needing clear control and error handling (often money sagas); choreography suits simple, highly-decoupled fan-out reactions. Many systems mix both.

### Event Sourcing (the powerful, advanced pattern)

Normally a database stores *current state* ("Alice's balance = ₹500"). **Event sourcing** instead stores the **full, immutable sequence of events** that led to that state ("account opened", "deposited ₹1000", "paid ₹500"), and computes current state by **replaying** them.

```
Traditional:  store final state →  balance = ₹500   (history lost)
Event-sourced: store events     →  [+₹1000][-₹500]  → replay → ₹500 (full history kept)
```

- **Benefits:** a perfect **audit log** (every change is recorded — *enormous* for fintech/regulation), the ability to **reconstruct state at any past point in time**, debug by replaying, and derive *new* read-views from the same event history.
- **Costs:** more complex; querying current state requires replaying or maintaining a derived view (often via **CQRS** — Command Query Responsibility Segregation, where writes append events and a separate read-model is built for queries); events are immutable so schema evolution and "fixing" data is tricky (you append corrective events, never edit).

Event sourcing pairs naturally with Kafka (the retained log *is* the event store) and is a major reason fintech systems adopt EDA — the audit trail is often a regulatory *requirement*, not just a nice-to-have.

### What EDA inherits (and demands)

Because EDA is async pub-sub, it carries forward everything from Chapters 18–19:
- **Eventual consistency** — reactions happen *after* the event; the system is briefly out of sync.
- **At-least-once delivery → idempotent consumers** (mandatory, especially for money).
- **Ordering** matters — use partition keys (e.g., `account_id`) to keep related events ordered (Chapter 19).
- **Observability is harder** — you need distributed tracing and good event monitoring because the flow is decentralized.

---

## Why do we need it?

We need EDA because it provides **loose coupling, resilience, and extensibility** that direct service-to-service calls cannot — it's what keeps a microservices system from collapsing into a fragile, tangled mess:

1. **Extreme decoupling & extensibility.** Producers don't know consumers. You add new functionality by adding a new consumer to an existing event stream — **without touching any existing service**. This is the single biggest win: the system grows by *addition*, not modification (recall adding "loyalty points" with zero payment-service changes, Chapter 19).

2. **Resilience.** Because communication is async through a broker, a down consumer doesn't break the producer or other consumers — events wait until it recovers (Chapter 18's resilience, architecturalized). Failures are isolated.

3. **Scalability.** Producers and consumers scale independently; the broker buffers spikes (load leveling). Each reaction scales to its own needs.

4. **Real-time responsiveness.** Systems react to events as they happen, enabling real-time analytics, notifications, and fraud detection.

5. **Audit & history (with event sourcing).** The event log is a complete, immutable record — a regulatory and debugging goldmine.

**When to use EDA:**
- Microservices needing loose coupling and independent evolution.
- Workflows with many independent reactions to a single occurrence (fan-out).
- Real-time data pipelines, analytics, and notifications.
- Domains needing strong audit trails (fintech!).

**When NOT to (or be cautious):**
- Simple request/response where the caller genuinely needs an immediate answer (e.g., "is this password correct?") — a direct synchronous call is simpler and right.
- Small systems where EDA's indirection adds complexity without payoff.
- When you can't tolerate eventual consistency on that path (the actual money debit stays synchronous).

---

## Real-World / Fintech Example

Our **digital wallet / payments app** uses EDA as its nervous system — and it's the architectural payoff of everything in this batch.

**The central event.** When a payment completes, the Payment service emits **`PaymentCompleted`** (carrying the relevant details — event-carried state transfer) to Kafka, **keyed by `account_id`** for ordering (Chapter 19), then moves on. It has *no idea* who consumes it. Independent consumers react:
- **Notifications** → push/email to Alice and Bob.
- **Analytics** → update spending dashboards.
- **Fraud** → score the transaction in real time.
- **Ledger projections** → update read-optimized balance/history views (CQRS-style).

**Extensibility in action.** Marketing wants a cashback feature. The team adds a **Cashback consumer** subscribing to `PaymentCompleted` — and because Kafka retained the events, it can even **replay history** to award retroactive cashback. **Zero changes** to the Payment service. This effortless extension is *the* reason EDA is worth its complexity.

**Orchestration vs. choreography — used deliberately.** For the **cross-shard money transfer saga** (Chapter 14), the team uses **orchestration**: a saga orchestrator explicitly directs "debit Alice → credit Bob → on failure, compensate by refunding Alice," because money flows need *explicit, debuggable control and error handling*. But for the *reactions* to a completed payment (notify/analytics/fraud/cashback), they use **choreography** — pure fan-out where each service independently reacts. **Right coordination style per problem.**

**Event sourcing for the audit trail.** The ledger is **event-sourced**: instead of only storing the current balance, it stores the immutable sequence of every account event (`AccountOpened`, `MoneyDeposited`, `PaymentSent`, `PaymentReceived`). Current balance is derived by replaying them. This gives:
- A **perfect, tamper-evident audit log** — a regulatory requirement for financial systems (auditors can see *exactly* how any balance came to be).
- The ability to **reconstruct any account's state at any past date** (e.g., for a dispute or a year-end statement).
- New read-views built by replaying events (e.g., a new "monthly spending" projection from historical events).

**Idempotency & ordering, because async.** The Fraud consumer might receive `PaymentCompleted` twice (at-least-once, Chapter 18) — it's **idempotent**, skipping duplicates so it never double-flags or double-acts. And because events are keyed by `account_id`, `AccountOpened` is always processed before `PaymentSent` for that account — ordering preserved where it matters.

**What stays synchronous.** Crucially, the *actual money debit/credit* is **not** event-driven — it's a synchronous, strongly-consistent operation (or orchestrated saga) completed before "success" is returned. EDA handles everything *around* the money movement (reactions, projections, audit), while the money movement itself keeps strong consistency. **EDA for the reactions; synchronous consistency for the money.**

In Spring Boot: events flow over `spring-kafka` (`KafkaTemplate` producers, `@KafkaListener` consumer groups); in-process events can use `ApplicationEventPublisher`/`@EventListener`; saga orchestration is often a dedicated service or a framework, and every consumer is idempotent with retry + dead-letter handling.

---

## Trade-offs (Pros & Cons)

**Pros**
- **Extreme decoupling & extensibility** — add consumers without touching producers; grow by addition.
- **Resilience & fault isolation** — async via broker; a down consumer doesn't break others.
- **Independent scalability** — producers/consumers scale separately; broker buffers spikes.
- **Real-time reactivity** — systems respond to events as they occur.
- **Audit & time-travel** (with event sourcing) — immutable history, regulatory gold for fintech.

**Cons**
- **Eventual consistency** — reactions lag the event; not for paths needing immediate consistency.
- **Harder to understand & debug** — flow is decentralized/implicit (especially choreography); needs distributed tracing.
- **Requires idempotency & ordering care** — at-least-once duplicates and partition ordering must be handled.
- **Operational complexity** — depends on a highly-available broker and good observability.
- **Event sourcing adds real complexity** — replay, schema evolution of immutable events, often CQRS for queries.

> **Staff-engineer takeaway:** Event-Driven Architecture **inverts control** — services publish *facts about what happened* and others *react*, instead of commanding each other directly. This buys **loose coupling, resilience, extensibility (grow by addition), and real-time reactivity** — the qualities that keep microservices healthy. Choose **orchestration** for complex flows needing explicit control (money sagas) and **choreography** for decoupled fan-out reactions. Use **event sourcing** where an immutable audit trail matters (fintech regulation). The price is **eventual consistency, decentralized/harder debugging, and mandatory idempotency** — so keep the actual **money movement synchronous and strongly consistent**, and let EDA handle everything *around* it.

---

➡️ **End of Batch 7.** You now understand the messaging infrastructure (Kafka vs RabbitMQ), the monolith-vs-microservices decision, and the event-driven style that ties distributed services together. The next batch covers how clients *talk* to all these services: **API Gateway & Backend-for-Frontend (BFF)**, **REST, GraphQL, and gRPC**, and **Long Polling, WebSockets, Server-Sent Events**.
