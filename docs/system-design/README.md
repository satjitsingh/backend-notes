# System Design Mastery — Deep, Descriptive Study Notes

> A staff-engineer-style walkthrough of System Design, built for **deep understanding**, not surface-level memorization.
>
> Language is kept **conversational and jargon-free**. Every concept is anchored with a **real-world example**, with a strong lean toward **high-traffic fintech** (payments, transactions, ledgers) and the **Java / Spring Boot** ecosystem.

---

## How to use these notes

System Design is not a list of facts to memorize — it's a way of *thinking about trade-offs*. There is rarely a single "correct" answer; there's only "the right answer for this context, at this scale, with these constraints."

So as you read, keep asking yourself three questions:

1. **What problem does this solve?** (Never learn a tool before you understand the pain it removes.)
2. **What does it cost me?** (Every design choice trades one thing for another — money, complexity, consistency, latency.)
3. **When would I *not* use it?** (Knowing when something is the wrong tool is more valuable than knowing how it works.)

Read the files in order. Each one builds the vocabulary for the next.

---

## Note structure (every topic follows this)

For consistency, each topic is written in the same scannable shape:

- **What is it?** — a detailed explanation of the concept
- **How it Works Under the Hood** — a step-by-step look at the mechanics
- **Why do we need it?** — the core problems it solves and when to use it
- **Real-World / Fintech Example** — a concrete, worked-through scenario
- **Trade-offs (Pros & Cons)** — what you sacrifice to get the benefit

---

## Table of contents

### Batch 1 — Foundations ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 1 | [01-Introduction-to-System-Design.md](01-Introduction-to-System-Design.md) | What system design really is, the mental model, key vocabulary, how to approach any design problem |
| 2 | [02-Scalability-and-Performance.md](02-Scalability-and-Performance.md) | Vertical vs horizontal scaling, statelessness, bottlenecks, how systems grow |
| 3 | [03-Latency-and-Throughput.md](03-Latency-and-Throughput.md) | The difference between speed and volume, percentiles (p99), the latency numbers every engineer should know |

### Batch 2 — Structure, Uptime & Data Copies ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 4 | [04-Architectural-Patterns.md](04-Architectural-Patterns.md) | Layered, monolithic, and microservices patterns; where to draw boundaries and why |
| 5 | [05-Availability-and-Availability-Patterns.md](05-Availability-and-Availability-Patterns.md) | The "nines", SPOFs, redundancy (active-passive/active-active), failover, graceful degradation |
| 6 | [06-Replication.md](06-Replication.md) | Leader-follower replication, synchronous vs asynchronous, replication lag, failover, split-brain |

### Batch 3 — The Theoretical Core ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 7 | [07-Consistency-and-Consistency-Patterns.md](07-Consistency-and-Consistency-Patterns.md) | Strong vs eventual vs causal consistency, read-your-writes & monotonic reads, choosing consistency per data type |
| 8 | [08-CAP-Theorem.md](08-CAP-Theorem.md) | Why it's "C or A *during a partition*", CP vs AP systems, deciding per operation |
| 9 | [09-PACELC-Theorem.md](09-PACELC-Theorem.md) | The Else clause: latency vs consistency during normal operation, the four labels (PA/EL, PC/EC, ...) |

### Batch 4 — Data Storage Foundations ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 10 | [10-Database-and-Storage.md](10-Database-and-Storage.md) | Memory hierarchy, indexes, B-Tree vs LSM-Tree storage engines, the Write-Ahead Log (WAL) |
| 11 | [11-Relational-Databases.md](11-Relational-Databases.md) | Tables, SQL, JOINs, ACID transactions, MVCC, why relational is the default for money |
| 12 | [12-Database-Isolation-Levels.md](12-Database-Isolation-Levels.md) | The 4 isolation levels, concurrency anomalies, lost updates/double-spends, `SELECT ... FOR UPDATE` |

### Batch 5 — Scaling Beyond One Machine ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 13 | [13-Scaling-Databases.md](13-Scaling-Databases.md) | The ordered scaling toolbox: scale up → optimize → pool → cache → read replicas (reads vs writes) |
| 14 | [14-Sharding-and-Partitioning.md](14-Sharding-and-Partitioning.md) | Vertical/horizontal partitioning, shard keys, range/hash/directory strategies, cross-shard sagas |
| 15 | [15-Non-Relational-Databases.md](15-Non-Relational-Databases.md) | The 4 NoSQL families, denormalization, query-first modeling, polyglot persistence |

### Batch 6 — Choosing & Performance Multipliers ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 16 | [16-Choosing-the-Right-Database.md](16-Choosing-the-Right-Database.md) | The 5-question framework, default-and-deviate, requirement-driven (not hype-driven) choice |
| 17 | [17-Caching.md](17-Caching.md) | Cache strategies (aside/through/back), eviction & TTL, invalidation, stampede/penetration/avalanche |
| 18 | [18-Asynchronous-Processing.md](18-Asynchronous-Processing.md) | Producer-queue-consumer, load leveling, idempotency, delivery semantics, critical path vs background |

### Batch 7 — Messaging & Architectural Styles ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 19 | [19-Message-Queues-Kafka-RabbitMQ.md](19-Message-Queues-Kafka-RabbitMQ.md) | Traditional brokers vs log-based streams, point-to-point vs pub-sub, partitions/offsets, delivery guarantees |
| 20 | [20-Monolithic-vs-Microservices-Architecture.md](20-Monolithic-vs-Microservices-Architecture.md) | The organizational decision, Conway's Law, modular monolith, Strangler Fig migration |
| 21 | [21-Event-Driven-Architecture.md](21-Event-Driven-Architecture.md) | Events vs commands, orchestration vs choreography, event sourcing & CQRS, audit trails |

### Batch 8 — How Clients Talk to the System ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 22 | [22-API-Gateway-and-Backend-for-Frontend.md](22-API-Gateway-and-Backend-for-Frontend.md) | Single front door, centralized cross-cutting concerns, request aggregation, the BFF pattern |
| 23 | [23-REST-GraphQL-gRPC.md](23-REST-GraphQL-gRPC.md) | Resources vs queries vs RPC, over/under-fetching, Protobuf/HTTP2, choosing per traffic type |
| 24 | [24-Long-Polling-WebSockets-SSE.md](24-Long-Polling-WebSockets-SSE.md) | Server push, one-way (SSE) vs two-way (WebSockets), pub-sub backplane for scaling connections |

### Batch 9 — Code Craft & Robustness ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 25 | [25-Design-Patterns.md](25-Design-Patterns.md) | GoF families, the high-value patterns (Strategy/Factory/Builder/Adapter/Facade/Proxy/Observer), SOLID, Spring's built-in patterns |
| 26 | [26-Resiliency.md](26-Resiliency.md) | Cascading failures, the resiliency toolbox (timeouts, retries, circuit breakers, bulkheads, fallbacks, load shedding) |
| 27 | [27-Designing-for-Resiliency.md](27-Designing-for-Resiliency.md) | The design mindset, redundancy/isolation, observability (metrics/logs/traces), chaos engineering |

### Batch 10 — Building Blocks & Essentials ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 28 | [28-Load-Balancers.md](28-Load-Balancers.md) | L4 vs L7, algorithms (round-robin, least-connections), health checks, sticky sessions vs statelessness |
| 29 | [29-Circuit-Breakers.md](29-Circuit-Breakers.md) | The 3 states (closed/open/half-open), tuning, composing with timeouts/fallbacks/retries |
| 30 | [30-System-Essentials.md](30-System-Essentials.md) | Idempotency, rate limiting, unique IDs (Snowflake/UUID), AuthN/AuthZ, observability, secrets |

### Batch 11 — Specialized Topics & Capstone ✅
| # | File | What you'll learn |
|---|------|-------------------|
| 31 | [31-Consistent-Hashing.md](31-Consistent-Hashing.md) | The hash ring, why `hash % N` is disastrous, virtual nodes, elastic scaling & failure handling |
| 32 | [32-Networking-and-Communication.md](32-Networking-and-Communication.md) | The layered model, TCP vs UDP, HTTP/2-3, DNS, TLS, the fallacies of distributed computing |
| 33 | [33-Real-World-Architectures-and-Engineering-Blogs.md](33-Real-World-Architectures-and-Engineering-Blogs.md) | The design method, synthesis, famous-system case studies, lifelong learning |

---

## ✅ Roadmap complete — all 33 topics across 11 batches

You've covered the full arc: foundations → scaling → distributed-systems theory → storage → performance → architecture & messaging → client communication → resiliency → building blocks → capstone synthesis. Re-run the **design method** (Chapter 33) on real problems and keep reading engineering blogs to turn this knowledge into lasting judgment.

---

## The golden rules of system design (keep these in mind throughout)

- **There is no perfect design, only trade-offs.** Anyone who gives you an answer without asking "what are the requirements?" is guessing.
- **Start simple. Scale when it hurts.** A monolith on one server handles more than you think. Don't build for a billion users you don't have yet.
- **Estimate before you architect.** Know your numbers: requests per second, data size, read/write ratio. The numbers decide the design.
- **Bottlenecks move, they don't disappear.** Fix the database and the network becomes the limit. Design is a game of whack-a-mole — know where the mole is.
- **The network is unreliable, and so is everything else.** Assume failures will happen. Good design is about *surviving* failure, not *preventing* it.
