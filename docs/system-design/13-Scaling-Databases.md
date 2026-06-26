# 13. Scaling Databases

> The database is almost always the first thing to buckle under growth. App servers scale out easily (just add stateless copies), but the database holds *state* — the shared truth everyone reads and writes. This chapter is the ordered playbook for scaling it, from cheapest move to most drastic.

---

## What is it?

**Scaling a database means increasing its capacity to handle more reads, more writes, and more data — without it becoming the bottleneck that drags the whole system down.**

Recall from Chapter 2 that scaling app servers is easy: make them **stateless** and add more copies behind a load balancer. The database can't be scaled that way, because it *is* the state — the single source of truth. You can't just run ten independent copies, because then which one has the *real* current balance? Coordinating shared, changing data across machines is the hard problem, and it's why the database is the classic bottleneck.

The key insight that organizes everything in this chapter:

> **There isn't one way to "scale a database" — there's an *ordered toolbox*, and you reach for the tools in order of increasing power and increasing pain.** Each tool solves a *specific* kind of pressure (too many reads? too much data? too many writes?). Using the right tool for the actual pressure — and not a heavier one than you need — is the whole skill.

The toolbox, in the order you should generally apply it:

1. **Vertical scaling** — bigger database server. (Simplest; do this first.)
2. **Indexing & query optimization** — make existing queries cheaper. (Often the highest ROI.)
3. **Connection pooling** — reuse connections instead of overwhelming the DB.
4. **Caching** — stop hitting the database for repeated reads. (Huge relief; gets its own chapter.)
5. **Read replicas** — copies that absorb read traffic. (Scales *reads*.)
6. **Sharding / partitioning** — split data across many databases. (Scales *writes* and *data size*; the nuclear option — next chapter.)

We'll cover 1–5 here in depth; sharding (#6) is big enough for its own chapter.

---

## How it Works Under the Hood

Let's walk the toolbox in order, understanding *what pressure each one relieves*.

### Tool 1 — Vertical scaling (scale up)
Give the database machine more CPU, RAM, and faster disks (NVMe SSDs). This is the same idea as Chapter 2's vertical scaling, and for databases it's *especially* effective early on, because:
- **More RAM** means more of the data (and indexes) fit in memory — and remember from Chapter 10, memory is ~100,000× faster than disk. A database that fits its "working set" in RAM is dramatically faster.
- **Faster disks** speed up the WAL writes and any reads that miss the cache.

It's the first move because it requires *zero* application changes. But it hits the familiar ceiling: there's a biggest machine, it's expensive, and it's a single point of failure.

### Tool 2 — Indexing & query optimization
Before adding *hardware*, make your existing queries do *less work*. This is frequently the single highest-return action:
- **Add the right indexes** (Chapter 10) so queries do O(log n) lookups instead of full-table scans. One missing index on a hot query can be the entire bottleneck.
- **Read the query plan** (`EXPLAIN ANALYZE` in PostgreSQL) to find queries doing sequential scans, bad joins, or sorting huge result sets.
- **Fix N+1 query problems** — a classic ORM (Hibernate/JPA) trap where loading 100 accounts secretly fires 101 queries. Batch them into one.

> A poorly-indexed query can be *thousands of times* slower than a well-indexed one. Optimizing queries is often cheaper and more impactful than any hardware upgrade — always exhaust this before scaling out.

### Tool 3 — Connection pooling
Opening a database connection is *expensive* (authentication, TCP setup, memory allocation on the DB side). Databases also have a hard limit on concurrent connections (a few hundred). If 40 app servers each open fresh connections per request, you exhaust the database's connection limit and it falls over — *not* from data load, but from connection churn.

A **connection pool** (e.g., **HikariCP**, the default in Spring Boot) keeps a small set of connections **open and reused**:
```
[ App servers ] → [ Connection Pool (e.g. 20 reused conns) ] → [ Database ]
```
Requests borrow a connection from the pool, use it, and return it. This caps the load on the database and removes per-request connection overhead. It's a quiet but essential scaling tool — many "database is overloaded" incidents are really "connections are mismanaged."

### Tool 4 — Caching
Put a fast in-memory store (e.g., **Redis**) in front of the database. For the *huge* fraction of reads that are repetitive ("what's Alice's balance?", "what's this product's details?"), serve them from memory and never touch the database. Since most systems are read-heavy (Chapter 2's ~10:1 ratio), caching can remove the *majority* of database load. This is so important it gets its own full chapter (Caching) — for now, know it sits here in the toolbox as a massive read-pressure reliever.

### Tool 5 — Read replicas (read scaling)
Now we scale *out* for reads, using the **leader-follower replication** from Chapter 6. One leader handles writes; multiple **read replicas** handle reads. The application sends:
- **Writes** → the leader.
- **Reads** → any replica (load-balanced across them).

```
        WRITES                         READS
          |                       /      |      \
          v                      v       v       v
      [ LEADER ] ──replicate──> [Replica][Replica][Replica]
```

This works brilliantly *because most systems are read-heavy* — you offload the 90% (reads) and leave the leader to handle just the 10% (writes). Add more replicas, absorb more reads. Nearly linear read scaling.

**The two catches** (both from Chapter 6/7):
- **Replication lag → stale reads.** Replicas are slightly behind, so a read right after a write may return old data. Fix with **read-your-writes** (route a user's reads to the leader briefly after they write).
- **It does NOT scale writes.** Every write still goes to the single leader. Read replicas multiply read capacity but do *nothing* for write capacity. When *writes* become the bottleneck, you've exhausted this tool and must shard (next chapter).

### The decision flow (how to pick the tool)

```
Database struggling?
  │
  ├─ Slow queries?            → Tool 2: optimize/index FIRST
  ├─ Running out of horsepower?→ Tool 1: scale up
  ├─ Too many connections?    → Tool 3: connection pool
  ├─ Too many READS?          → Tool 4: cache, then Tool 5: read replicas
  └─ Too many WRITES / too much DATA? → Tool 6: SHARD (next chapter, last resort)
```

> **The golden rule of database scaling:** *Exhaust the cheap tools before the expensive ones.* Optimize queries and add caching/replicas long before you shard. Sharding is powerful but introduces permanent complexity — it's the tool of last resort, not first.

---

## Why do we need it?

We need a *deliberate* database-scaling strategy because **the database is where growth hurts first and worst, and panic-reaching for the wrong tool causes lasting damage.**

1. **It's the universal bottleneck.** You can scale app servers infinitely, but they all funnel into the database. Until the database scales, nothing else you do matters — it's the floor on your whole system's capacity.

2. **The tools solve *different* problems.** Read replicas do nothing for a write bottleneck. Caching does nothing for a write bottleneck. Indexing does nothing if you're simply out of RAM. Diagnosing *which* pressure you're under (reads? writes? data size? query inefficiency?) tells you which tool to grab. Grabbing the wrong one wastes time and money while the system stays down.

3. **Cheap tools have huge ROI; expensive tools have lasting costs.** Adding an index or a cache might 10x your capacity in an afternoon with no architectural change. Sharding might also 10x capacity but permanently complicates every query, transaction, and migration forever. The *order* matters enormously.

4. **Premature sharding is a classic, painful mistake.** Teams that shard too early inherit massive complexity to solve a write-scale problem they didn't yet have — exactly the over-engineering trap from Chapter 1. Knowing the toolbox order prevents this.

**When to use what:** match tool to symptom. Slow specific queries → indexing. General sluggishness with spare RAM headroom gone → scale up. Read-bound → cache + replicas. **Write-bound or data-too-big-for-one-machine → shard** (and only then).

---

## Real-World / Fintech Example

Watch our **digital wallet / payments app** climb the toolbox in order — exactly as a real team would.

**Stage 1 — One database, scale up.** Early on, a single PostgreSQL instance handles everything. As load grows, the team **scales up** — more RAM (so the hot accounts and indexes live in memory) and NVMe disks (faster WAL writes). Zero code changes, big wins. This carries them comfortably through early growth.

**Stage 2 — Optimize before adding hardware.** Balance checks are slow. Investigation (`EXPLAIN ANALYZE`) reveals the transaction-history query was doing a full-table scan — there was no index on `(account_id, timestamp)`. Adding it turns a multi-second scan into a millisecond range-scan (Chapter 10). They also discover an **N+1 query** in the statement generator (loading each transaction's details in a separate query) and batch it. These query fixes reclaim more capacity than a hardware upgrade would have — **always optimize first.**

**Stage 3 — Connection pooling.** As they scale to 40 stateless Spring Boot instances (Chapter 2), the database starts rejecting connections — each app server was opening too many. They configure **HikariCP** so each app server reuses a small pool. The "database overloaded" errors vanish; the actual data load was fine — it was connection churn.

**Stage 4 — Cache + read replicas for the read flood.** Balance and history reads dominate (~10:1). They add:
- **Redis caching**: a user's balance (read constantly, changes rarely) is served from memory. The vast majority of balance reads never touch PostgreSQL.
- **Read replicas**: remaining reads (history, search) are load-balanced across 3 replicas. The leader is now freed up to focus on writes.
- They hit the **stale-read** bug (Alice tops up, sees old balance) and fix it with **read-your-writes** routing (Chapter 7) — her reads briefly go to the leader/cache-bust after she writes.

**Stage 5 — The write wall.** At festival peak, even with reads fully offloaded, *writes* (the actual payments) overwhelm the single leader — 80,000 payments/sec is more than one machine can durably commit. **No amount of replicas or caching helps**, because they don't scale writes. The team has finally, legitimately, reached the limit of every cheap tool. *Now* — and only now — sharding is justified (next chapter): split accounts across multiple PostgreSQL leaders so each handles a fraction of the writes.

> The lesson: they climbed the *entire* toolbox before sharding. A less disciplined team would have sharded at Stage 2 and spent years fighting cross-shard transactions they never needed. The order *is* the expertise.

---

## Trade-offs (Pros & Cons)

### Vertical scaling (scale up)
**Pros:** zero code changes; more RAM hugely speeds a database; instant relief early on.
**Cons:** hard ceiling; expensive at the top; single point of failure.

### Indexing & query optimization
**Pros:** often the highest ROI; can 1000x a bad query; no architecture change.
**Cons:** every index slows writes and costs space (Chapter 10); requires expertise to diagnose.

### Connection pooling
**Pros:** prevents connection exhaustion; removes per-request overhead; essential and cheap.
**Cons:** pool sizing is subtle (too small starves the app, too large overwhelms the DB); doesn't add data capacity.

### Caching
**Pros:** removes the majority of read load; dramatic latency improvement.
**Cons:** cache invalidation is hard ("one of the two hard problems"); risk of stale data; added component to operate. (Full treatment in the Caching chapter.)

### Read replicas
**Pros:** near-linear *read* scaling; also provides availability (replicas double as failover targets, Chapter 5).
**Cons:** **doesn't scale writes at all**; replication lag → stale reads; more machines to operate.

> **Staff-engineer takeaway:** Scaling a database is an *ordered toolbox*, applied cheapest-first: **scale up → optimize queries → pool connections → cache → read replicas → (only then) shard.** Diagnose the *actual* pressure — reads, writes, data size, or query inefficiency — and apply the matching tool, not a heavier one. The critical limit to remember: **caching and read replicas scale reads, never writes.** When *writes* are the wall, you've exhausted the easy tools and earned the right to shard.

---

➡️ Next: [14-Sharding-and-Partitioning.md](14-Sharding-and-Partitioning.md) — the last-resort, most powerful tool: splitting your data across many machines to finally scale writes (and the serious complexity that comes with it).
