# 17. Caching

> The single highest-impact performance technique in backend systems. A cache can turn a 50-millisecond database query into a 0.5-millisecond memory hit and remove the majority of load from your database. It's also the source of one of computer science's two famously hard problems. Master this and you've mastered one of the biggest levers you'll ever pull.

---

## What is it?

**A cache is a small, fast store that holds copies of frequently-accessed data so you can serve it quickly instead of recomputing it or fetching it from a slow source.**

The whole idea rests on the latency hierarchy from Chapter 3: reading from **memory is ~100,000× faster than disk/network**. A cache exploits this by keeping the *hot* data — the small fraction of data that's accessed over and over — in fast memory (RAM), so the common case avoids the slow source entirely.

The principle that makes caching work is the **Pareto / locality principle**:

> In most systems, a *small* fraction of the data accounts for a *huge* fraction of the accesses. 20% of products get 80% of the views; a handful of celebrity accounts get most of the traffic; the same balance is read hundreds of times between updates. **If you keep that hot 20% in fast memory, you serve ~80% of requests at memory speed** — without touching the slow database.

Two key terms you'll use constantly:
- **Cache hit** — the data was in the cache; served fast. 
- **Cache miss** — the data wasn't there; you fall back to the slow source (and usually populate the cache for next time).
- **Hit ratio** — the percentage of requests served from cache. A high hit ratio (say 90%+) is the goal; it's the single best measure of a cache's effectiveness.

Caches live at **many layers** of a system (caching is everywhere):
- **Browser cache** — the user's browser stores assets locally.
- **CDN (Content Delivery Network)** — caches static content (images, JS, video) at edge servers physically near users (recall Chapter 3: distance = latency).
- **Application/in-memory cache** — data cached inside the app process itself (fastest, but per-instance).
- **Distributed cache** — a shared cache cluster like **Redis** or **Memcached**, accessible by all app servers.
- **Database cache** — the DB's own buffer pool keeping hot pages in RAM (Chapter 10).

This chapter focuses on the **application-level distributed cache** (Redis-style), the one you actively design.

---

## How it Works Under the Hood

### Caching strategies (how data gets into and out of the cache)

The strategy determines *who* populates the cache and *when* — and it has big consequences for consistency and performance.

**1. Cache-Aside (Lazy Loading) — the most common.**
The application manages the cache directly:
```
READ:
  1. Check cache.
  2. HIT  → return it.
  3. MISS → read from DB, write it into cache, return it.
WRITE:
  1. Write to DB.
  2. Invalidate (delete) the cache entry.
```
- **Pro:** only requested data is cached (memory-efficient); cache failure doesn't break the app (it just falls back to DB).
- **Con:** first read of any item is always a miss (slow); risk of stale data if invalidation is mishandled.
- This is what Spring's `@Cacheable`/`@CacheEvict` annotations implement.

**2. Read-Through.** The cache itself (via a library/provider) loads from the DB on a miss, so the app only ever talks to the cache. Similar to cache-aside but the loading logic lives in the cache layer, not the app.

**3. Write-Through.** On a write, the app writes to the cache *and* the cache synchronously writes to the DB before returning.
- **Pro:** cache is always consistent with the DB; no stale data.
- **Con:** writes are slower (two writes); caches data that may never be read.

**4. Write-Back (Write-Behind).** Write to the cache and return immediately; the cache writes to the DB *asynchronously* later.
- **Pro:** very fast writes; can batch DB writes.
- **Con:** **risk of data loss** if the cache dies before flushing to DB — so it's dangerous for critical data (a recurring fintech red flag).

> **Mental model:** the strategy is a position on the *speed ↔ consistency ↔ durability* spectrum, just like sync/async replication (Chapter 6). Cache-aside is the pragmatic default; write-through trades speed for consistency; write-back trades durability for speed.

### Eviction policies (what to throw out when the cache is full)

A cache is small by design, so when it fills up it must evict something to make room. The policy decides what:
- **LRU (Least Recently Used)** — evict whatever hasn't been accessed in the longest time. The most common and usually best default (recently used = likely to be used again).
- **LFU (Least Frequently Used)** — evict the least-often-accessed item.
- **FIFO** — evict the oldest inserted, regardless of usage.
- **TTL (Time To Live)** — entries auto-expire after a set time, independent of the above. *Setting a sensible TTL is the simplest, most effective tool for bounding staleness* — even if you mishandle invalidation, a TTL guarantees data is at most N seconds old.

### The two hard problems

There's a famous joke: *"There are only two hard things in computer science: cache invalidation and naming things."* The first one is the heart of caching.

**Problem 1 — Cache Invalidation (keeping cache and source in sync).**
The cache holds a *copy*. When the source data changes, the copy is now **stale**. Deciding *when and how* to update/remove stale entries is genuinely hard:
- Invalidate too aggressively → low hit ratio, you lose caching's benefit.
- Invalidate too little → users see stale data (a wrong balance!).
- The fixes: **TTLs** (bound staleness automatically), **explicit invalidation on write** (delete the entry when you update the DB — cache-aside), and **event-driven invalidation** (publish a change event that busts the cache, often via Kafka — ties to Chapter 18).

**Problem 2 — Stale data tolerance.** Closely related: *how stale can this data be?* A product description can be hours stale; a bank balance used to *authorize a payment* must be exactly current. This is the **consistency-per-data-type** decision from Chapter 7, applied to caching. You cache aggressively where staleness is harmless and cache carefully (or not at all) where it isn't.

### The cache failure modes (the ways caches bite you)

These are classic interview and production topics:

**1. Cache Stampede / Thundering Herd.** A popular cached item expires, and *simultaneously* thousands of requests miss and all hammer the database at once to rebuild it — potentially crashing the DB. Fixes: **locking** (only one request rebuilds, others wait), **staggered/randomized TTLs**, or **refresh-ahead** (proactively refresh before expiry).

**2. Cache Penetration.** Requests for data that *doesn't exist* (e.g., querying random fake IDs) always miss the cache and hit the DB every time. Fix: cache the "not found" result too, or use a **Bloom filter** (Chapter 10) to reject keys that definitely don't exist.

**3. Cache Avalanche.** Many keys expire at the *same time* (e.g., all set with the same TTL), causing a sudden mass of misses → DB overload. Fix: **randomize/jitter TTLs** so expirations spread out.

---

## Why do we need it?

We need caching because it's the **highest-leverage performance and scalability tool available** — it simultaneously improves latency, throughput, *and* cost:

1. **Dramatically lower latency.** Memory hits (~microseconds) vs disk/DB queries (~milliseconds) — often a 100–1000× speedup on cached reads. This directly improves the user-facing latency (Chapter 3) that drives conversion and trust.

2. **Massive database load reduction.** Since most systems are read-heavy and follow the locality principle, a cache can absorb the *majority* of reads — recall it's a primary tool in the database scaling toolbox (Chapter 13). It lets a modest database serve enormous traffic by handling only the misses and writes.

3. **Higher throughput at lower cost.** Serving from memory is cheap and fast, so you handle more requests with less hardware. Caching often defers the need to scale up/shard the database at all.

4. **Protects expensive operations.** Beyond DB reads, caches store results of *expensive computations* (a complex report, an ML inference, an external API response) so you compute once and serve many times.

**When to cache:** data that is **read far more than written** and where some **staleness is acceptable**. Perfect for balances-display, product info, user profiles, configuration, computed feeds.

**When NOT to cache (or cache carefully):** data that changes on nearly every read (low hit ratio — no benefit), data that must be *perfectly* fresh and authoritative (the balance used to *authorize* a payment — read it from the source of truth), and write-heavy data with little reuse.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** uses **Redis** as a distributed cache — and the *nuance* of what to cache (and how carefully) is the lesson.

**Balance *display* → cache-aside, short TTL.** Users obsessively refresh their balance (~10:1 read ratio, Chapter 2). Caching it in Redis serves the vast majority of these reads from memory in microseconds, sparing PostgreSQL. Strategy: **cache-aside** — on a balance read, check Redis; on a miss, read Postgres and populate Redis with a **short TTL (e.g., 30s)**. On any write (payment), **invalidate** Alice's cached balance so the next read repopulates fresh.

**The critical distinction — *displaying* a balance vs *authorizing* a payment.** This is the staff-level insight. When Alice *looks* at her balance, a cached, slightly-stale value is fine (and read-your-writes from Chapter 7 patches her own recent change). But when the system *authorizes a payment* — deciding whether she has enough money — it must **never** trust the cache. It reads the authoritative balance from PostgreSQL (with `SELECT ... FOR UPDATE`, Chapter 12) inside the transaction. **Caching the display is great; caching the authorization decision would cause double-spends.** Same data, two completely different caching policies based on the cost of staleness.

**Product/reference data → long TTL.** Supported banks, fee schedules, merchant info change rarely. Cache them with a long TTL (hours). Huge hit ratio, near-zero staleness risk.

**Why write-back is banned for money.** A junior suggests write-back caching (write to Redis, flush to Postgres later) to speed up payments. **Rejected:** if Redis crashes before flushing, *committed payments would vanish* — catastrophic for money (violates the durability we fought for in Chapters 10–11). Write-back's data-loss risk makes it unacceptable for the ledger. Speed never beats durability for money.

**Handling the failure modes at peak.** During a festival sale:
- **Stampede:** the cached "trending merchants" list expires and thousands of requests miss at once. They add a **lock** so only one request rebuilds it while others briefly wait — protecting Postgres.
- **Avalanche:** they discover many keys were set with identical TTLs, all expiring together. They add **TTL jitter** (randomize expiry ±10%) so misses spread out.
- **Penetration:** bots query random fake account IDs that always miss. They cache the "not found" result briefly to stop the DB hammering.

In Spring Boot, the display-balance path uses `@Cacheable("balance")` with a configured TTL and `@CacheEvict` on payment writes, all backed by `spring-boot-starter-data-redis` — while the *authorization* path deliberately bypasses the cache and reads the primary datasource transactionally.

---

## Trade-offs (Pros & Cons)

**Pros**
- **Massive latency reduction** — memory hits are 100–1000× faster than DB/disk.
- **Huge database load relief** — absorbs the majority of reads (key scaling tool, Chapter 13).
- **Higher throughput at lower cost** — serve more with less hardware.
- **Protects expensive computations/external calls** — compute once, serve many.

**Cons**
- **Cache invalidation is hard** — the famous problem; stale data is a constant risk.
- **Stale data danger** — unacceptable for authoritative/financial decisions if misused.
- **Added complexity & failure modes** — stampede, penetration, avalanche to defend against.
- **Another component to operate** — the cache itself can fail, needs memory sizing, eviction tuning, and monitoring.
- **Cold-start penalty** — an empty cache (after restart) sends a burst of misses to the DB until it warms up.

### Strategy quick-compare
| Strategy | Write speed | Consistency | Durability risk | Best for |
|---|---|---|---|---|
| **Cache-aside** | Normal | Good (invalidate on write) | None | General default |
| **Write-through** | Slower | Strong | None | Read-after-write needs |
| **Write-back** | Fastest | Weak | **High (data loss)** | High-write, non-critical only |

> **Staff-engineer takeaway:** Caching is the **highest-leverage** performance lever — it cuts latency ~100–1000×, removes most database load, and lowers cost, all at once. Use **cache-aside with sensible TTLs** as your default, and pick the strategy along the speed↔consistency↔durability spectrum. The master skill is matching the caching policy to the **cost of staleness**: cache aggressively where stale is harmless (balance *display*), and **never trust the cache for authoritative decisions** (payment *authorization*). Respect cache invalidation and the stampede/penetration/avalanche failure modes — and never use write-back for money.

---

➡️ Next: [18-Asynchronous-Processing.md](18-Asynchronous-Processing.md) — the other great performance multiplier: doing work *later* and *off the critical path* so users never wait for what they don't need to.
