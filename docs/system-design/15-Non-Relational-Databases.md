# 15. Non-Relational Databases (NoSQL)

> The other escape hatch from the single-relational-database wall. NoSQL isn't "better than SQL" or "the modern choice" — it's a *family of specialized tools*, each trading away some relational guarantees to win big at a specific job. Knowing which tool fits which job is the entire skill.

---

## What is it?

**Non-relational databases (NoSQL) are databases that don't use the rigid table-and-row relational model.** Instead, each type uses a data model optimized for a particular shape of data and access pattern. "NoSQL" is best read as **"Not Only SQL"** — it's an *umbrella term* for several very different database families, not a single technology.

The historical reason NoSQL exists: relational databases (Chapter 11) are fantastic but have two pain points at extreme scale — they're **hard to scale writes horizontally** (Chapter 14's sharding pain) and their **rigid schema** is awkward for rapidly-changing or loosely-structured data. NoSQL databases were built to relax these constraints, typically by **giving up some of what makes relational great** (ACID transactions, JOINs, strong consistency) in exchange for **massive horizontal scalability, flexible schemas, and high availability.**

> **The core trade-off in one line:** relational databases optimize for *correctness and flexible querying* on a single machine; most NoSQL databases optimize for *scale and availability* across many machines — and they pay for it by relaxing consistency (often **eventual consistency**, Chapter 7) and dropping JOINs/multi-row transactions. NoSQL is usually an **AP / PA-EL** citizen (CAP/PACELC, Chapters 8–9), where relational is typically **CP / PC-EC**.

### The four families of NoSQL (each a different tool)

**1. Key-Value stores** (Redis, DynamoDB, Riak)
The simplest model: a giant dictionary/hash map. You store a **value** under a **key** and look it up by that key. Blazingly fast, dead simple, scales beautifully. But you can *only* query by key — no rich queries.
> *Use for:* caching, session storage, user preferences, real-time leaderboards, rate-limiting counters.

**2. Document stores** (MongoDB, Couchbase)
Store **documents** (JSON/BSON), each a self-contained, nested object. Schema-flexible — different documents in the same collection can have different fields. Supports richer queries than key-value (query by fields inside the document).
> *Use for:* content management, product catalogs, user profiles, anything where each record is a rich, self-contained object that varies in shape.

**3. Wide-Column stores** (Cassandra, HBase, ScyllaDB)
Data is stored in rows that can have huge numbers of columns, grouped into column families. Built on LSM-Trees (Chapter 10) for **enormous write throughput** and designed from the ground up to scale across hundreds of nodes with no single leader.
> *Use for:* write-heavy workloads at massive scale — time-series data, event logging, IoT sensor data, the fraud-event firehose from Chapter 10.

**4. Graph databases** (Neo4j, Amazon Neptune)
Data is modeled as **nodes** (entities) and **edges** (relationships), with relationships as first-class citizens. Optimized for traversing connections ("friends of friends of friends") that would require many expensive JOINs in a relational DB.
> *Use for:* social networks, recommendation engines, **fraud-ring detection**, knowledge graphs — anything where the *relationships* are the point.

---

## How it Works Under the Hood

### Why NoSQL scales writes so easily: denormalization + no cross-row guarantees

Recall the two things that made relational sharding painful (Chapter 14): **JOINs** and **cross-row transactions**. NoSQL sidesteps both by design:

- **Denormalization instead of JOINs.** Where relational *normalizes* (store each fact once, JOIN to combine), NoSQL **denormalizes** — it *duplicates* data so that everything needed for a query lives together in one document/row. A document store keeps a user *and* their recent orders embedded in one document. No JOIN needed → the data can live on one shard → it scales horizontally trivially. The cost: duplicated data and the burden of keeping copies in sync on writes.

- **No multi-row ACID (usually) → easy distribution.** Because most NoSQL stores don't promise atomic transactions across many rows/documents, they don't need the coordination that ties relational data to one machine. Each write is independent, so writes distribute across nodes naturally. (Many do guarantee atomicity *within a single document/row* — e.g., MongoDB single-document writes are atomic — just not *across* them.)

### Schema-on-read vs schema-on-write

- **Relational = schema-on-write** (Chapter 11): you define the schema upfront, and the DB rejects anything that doesn't fit. Structure enforced at write time.
- **NoSQL (document/key-value) = schema-on-read**: you can store whatever shape you want; the *application* interprets the structure when it reads. 

This makes NoSQL flexible for evolving or varied data (add a field to new documents without migrating old ones), but shifts the burden of data integrity to your application code — the database won't stop you from storing inconsistent or malformed data.

### "Query-first" data modeling (the mental flip)

This is the biggest mindset shift for someone coming from relational. In relational, you model the *data* (normalized tables) and then write whatever queries you need. In NoSQL — *especially* wide-column stores like Cassandra — you do the opposite:

> **You model your data around the queries you'll run.** You decide the access patterns *first*, then structure (and duplicate) the data so each query is a fast single-key/single-partition lookup. If you later need a *new* access pattern, you may have to create a whole new denormalized copy of the data.

This is powerful for known, high-volume access patterns and terrible for ad-hoc querying — the reverse of relational's strengths.

### How NoSQL achieves availability and scale (tying back to theory)

Wide-column and key-value stores like Cassandra/DynamoDB are typically **leaderless** (Chapter 6) and **AP/EL** (Chapters 8–9): any node accepts reads/writes, data is replicated across nodes, and consistency is **tunable** via quorums (the `R + W > N` rule from Chapter 6). This is *why* they stay available during partitions and scale to hundreds of nodes — they made the CAP/PACELC choice to favor availability and latency over strong consistency. Everything connects: NoSQL's scalability *is* the consistency trade-offs we studied, made concrete.

---

## Why do we need it?

We need NoSQL because **some workloads simply don't fit the relational model well, and forcing them into it is slow, awkward, or impossible at scale.** Specifically:

1. **To scale writes without sharding pain.** When you need massive write throughput, a wide-column store like Cassandra distributes writes across many nodes *natively* — no manual shard-key agonizing, no cross-shard saga complexity. It was *born* distributed.

2. **For flexible / evolving data.** When records vary in shape or the schema changes constantly (early-stage products, user-generated content), a document store's schema flexibility avoids constant migrations.

3. **For specialized access patterns.** Some queries are pathological in relational: deep relationship traversals (→ graph DB), simple ultra-fast key lookups (→ key-value), or huge time-series ingestion (→ wide-column). The right NoSQL tool turns an expensive relational operation into a cheap native one.

4. **For high availability across regions.** AP-style NoSQL stays up and writable during partitions, ideal for globally-distributed, always-on systems.

**When NOT to use it (equally important):**
> Don't reach for NoSQL just because it's trendy. If your data is relational (lots of relationships), you need **multi-row ACID transactions** (money!), or you need **flexible ad-hoc querying**, relational is still the better, safer choice. The biggest NoSQL mistake is using it for transactional financial data that desperately needs ACID — and then rebuilding transactions badly in application code.

The honest modern answer is **polyglot persistence**: use *both*. Pick the right database for each part of the system rather than forcing everything into one.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** is a textbook case of **polyglot persistence** — relational for the money, NoSQL for everything it's bad at. The mature design uses *multiple* databases, each for its strength:

**The money ledger → stays RELATIONAL (PostgreSQL).**
This is the crucial decision. Account balances and transactions need **ACID transactions and strong consistency** (Chapters 11–12) — the double-spend protection, the atomic transfer, the `balance >= 0` constraint. **No NoSQL store gives this as cleanly.** So the ledger stays relational (sharded if necessary, Chapter 14). *They do not move money into NoSQL* — a tempting mistake that would force them to reinvent ACID poorly. **Right tool: relational for money.**

**Sessions & balance cache → KEY-VALUE (Redis).**
User sessions and cached balances are simple key→value lookups needing blazing speed (Chapter 13's caching, Chapter 2's stateless sessions). Redis is perfect: `GET session:abc123`. No relationships, no transactions — just fast key lookups. **Right tool: key-value.**

**User profiles & app config → DOCUMENT (MongoDB).**
A user profile has varied, nested, evolving fields (KYC docs, preferences, linked cards, notification settings) that differ per user and change often. A flexible document fits naturally and avoids constant schema migrations. **Right tool: document store.**

**Fraud/event firehose → WIDE-COLUMN (Cassandra).**
Every tap, login, and transaction emits events — millions per minute, write-heavy, rarely read individually (Chapter 10's LSM firehose). Cassandra's leaderless, LSM-based, write-optimized design swallows this effortlessly and scales across nodes natively. **Right tool: wide-column.**

**Fraud-ring detection → GRAPH (Neo4j).**
Fraudsters share devices, phone numbers, and bank accounts in interconnected rings. Asking "which accounts are connected to this known-fraud account within 3 hops?" is a nightmare of recursive JOINs in SQL but a fast, natural traversal in a graph database. **Right tool: graph.**

> The masterstroke isn't choosing NoSQL *or* relational — it's using **each where it shines**: PostgreSQL guards the money (ACID), Redis serves speed (key-value), MongoDB holds flexible profiles (document), Cassandra absorbs the event firehose (wide-column), and Neo4j hunts fraud rings (graph). A junior asks "SQL or NoSQL?"; a staff engineer answers "for *which* data?" — exactly the polyglot mindset.

In a Spring Boot system this is very natural: Spring Data provides `JpaRepository` (PostgreSQL), `RedisTemplate`/`@Cacheable` (Redis), `MongoRepository` (MongoDB), and `CassandraRepository` (Cassandra) — different repositories pointed at different stores, all in one codebase.

---

## Trade-offs (Pros & Cons)

### NoSQL in general (vs relational)
**Pros**
- **Horizontal write scalability** — many NoSQL stores are born distributed; no sharding agony.
- **Flexible / evolving schema** — store varied data without migrations (schema-on-read).
- **High availability** — AP-style stores stay up and writable during partitions.
- **Specialized performance** — the right family turns an expensive relational operation into a cheap native one.

**Cons**
- **Weak/eventual consistency** (usually) — stale reads; dangerous for data needing exactness.
- **Limited or no multi-row ACID transactions** — must handle consistency in app code.
- **No JOINs** — denormalization duplicates data and pushes sync burden onto writes.
- **Query-first modeling** — great for known patterns, painful for ad-hoc queries; new access patterns may need new data copies.
- **Less mature tooling/expertise** in some cases vs decades-hardened relational.

### Quick family selector
| Family | Data shape | Killer use case | Example |
|---|---|---|---|
| **Key-Value** | key → blob | cache, sessions, counters | Redis, DynamoDB |
| **Document** | nested JSON objects | profiles, catalogs, CMS | MongoDB |
| **Wide-Column** | rows w/ many columns | write-heavy, time-series, events | Cassandra |
| **Graph** | nodes + edges | relationships, fraud rings, social | Neo4j |

> **Staff-engineer takeaway:** NoSQL is **"Not Only SQL"** — a toolbox of specialized databases that trade relational guarantees (ACID, JOINs, strong consistency) for **scale, flexibility, and availability**. There are four families, each a different tool: **key-value** (speed), **document** (flexible objects), **wide-column** (write-heavy scale), **graph** (relationships). Don't pick NoSQL for fashion or replace your ACID ledger with it. Instead, embrace **polyglot persistence**: keep **money in relational**, and route each *other* workload to the NoSQL family that fits it best.

---

➡️ **End of Batch 5.** You've now broken through the single-machine wall in both directions — scaling relational (replicas, sharding) and reaching for purpose-built NoSQL. With the full storage landscape mapped, the next batch makes you the *decider*: **Choosing the Right Database**, then the performance powerhouses **Caching** and **Asynchronous Processing**.
