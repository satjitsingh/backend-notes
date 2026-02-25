# Redis Interview Questions

!!! tip "How to Use This Page"
    Each answer has a **one-liner** for quick revision, a **detailed explanation**, an **example**, and a **"How to say it in an interview"** tip. Read the one-liner first. If you need more detail, read the full answer.

---

## Basic Questions

### Q1: What is Redis?

**One-liner:** Redis is an in-memory key-value store used for caching, sessions, and real-time data.

**Detailed explanation:** Redis stands for Remote Dictionary Server. It stores data in RAM, which makes it extremely fast—sub-millisecond response times. It supports multiple data structures: Strings, Lists, Sets, Hashes, Sorted Sets, Streams, and more. It's commonly used as a cache layer between your application and database, but it's also used for sessions, rate limiting, leaderboards, and message queues. Redis is single-threaded for command execution, which avoids race conditions and keeps the implementation simple.

**Example:** When a user requests a product page, your app checks Redis first. If the product is cached, it returns in 0.1 ms. If not, it queries the database (10 ms), stores the result in Redis, and returns. The next 1000 requests get the cached version.

**How to say it in an interview:** "Redis is an in-memory key-value store. It's used primarily for caching to reduce database load and improve response times. It supports rich data structures like Lists and Sorted Sets, which makes it useful for things like leaderboards and task queues, not just simple caching."

!!! warning "Red flags in your answer"
    Don't say "Redis is a database" without clarifying. It's a cache/session store that can persist, but it's not a replacement for MySQL or PostgreSQL. Don't say "Redis is like Memcached" and leave it at that—Redis does much more.

---

### Q2: Why is Redis so fast?

**One-liner:** Because it stores data in RAM, is single-threaded (no locking overhead), and uses efficient data structures.

**Detailed explanation:** 
- **In-Memory:** RAM access is 100x faster than disk. Redis keeps all data in memory, so there's no disk I/O for reads.
- **Single-Threaded:** Redis processes commands one at a time. No locks, no context switching, no race conditions. Each operation is microseconds, so a single thread can handle ~100,000 ops/sec.
- **Efficient data structures:** Internally uses hash tables, skip lists, and compact representations. Operations are O(1) or O(log N).
- **Non-blocking I/O:** Uses epoll (Linux) or kqueue (BSD) for network I/O. While waiting for network, it can process other commands. (Note: command execution is single-threaded; I/O can use background threads in Redis 6+.)

**Example:** A `GET` in Redis takes ~0.1 ms. A `SELECT` from MySQL on SSD might take 1–10 ms. For 10,000 reads/sec, Redis handles it easily; a database might struggle.

**How to say it in an interview:** "Redis is fast because of three things: in-memory storage, single-threaded execution that avoids locking, and efficient internal data structures. RAM is orders of magnitude faster than disk, and the single-threaded model keeps things simple and predictable."

---

### Q3: Is Redis single-threaded? Is that a problem?

**One-liner:** Yes, for command execution. No, it's not a problem because RAM operations are microsecond-fast.

**Detailed explanation:** Redis processes commands in a single thread. One command at a time. This avoids race conditions and locking overhead. Since each operation takes microseconds, a single thread can handle ~100,000 operations per second. For I/O (network, persistence to disk), Redis 6+ can use background threads, but the core command execution remains single-threaded. The bottleneck is rarely CPU—it's usually network or memory.

**Example:** Even with 50,000 concurrent connections, Redis processes commands sequentially. A `SET` might take 0.05 ms. So in 1 second, you can do 20,000 SETs. That's enough for most applications.

**How to say it in an interview:** "Redis is single-threaded for command execution. That's by design—it avoids locks and keeps the model simple. Because operations are so fast, a single thread can handle hundreds of thousands of ops per second. For scaling beyond that, you'd use Redis Cluster to shard data across multiple nodes."

!!! info "What the interviewer is really asking"
    They want to know if you understand the trade-off. Single-threaded = no locks, simple, but one slow command blocks others. You should mention that Redis is optimized for fast commands and that blocking commands (like KEYS *) should be avoided.

---

### Q4: What data structures does Redis support?

**One-liner:** Strings, Lists, Sets, Hashes, Sorted Sets, Streams, Bitmaps, HyperLogLogs.

| Structure | Use Case |
|-----------|----------|
| String | Caching, counters, OTPs |
| List | Message queues, activity feeds |
| Set | Unique tags, online users |
| Hash | User profiles, objects |
| Sorted Set | Leaderboards, rate limiting |
| Stream | Event sourcing, message queues |
| Bitmap | Feature flags, presence |
| HyperLogLog | Unique count approximation |

**Detailed explanation:** Redis is not just key-value strings. It has rich types. Strings can hold text, numbers, or JSON. Lists are ordered and support push/pop from both ends. Sets are unique, unordered. Hashes are like mini key-value stores inside a key. Sorted Sets add a score to each member for ranking. Streams are append-only logs. Bitmaps and HyperLogLog are for specific use cases (flags, approximate counts).

**Example:** For a leaderboard: `ZADD leaderboard 100 "Alice"` and `ZREVRANGE leaderboard 0 9` for top 10. For online users: `SADD online_users "user_42"` and `SMEMBERS online_users`.

**How to say it in an interview:** "Redis supports Strings, Lists, Sets, Hashes, and Sorted Sets as the main types. Plus Streams, Bitmaps, and HyperLogLog. I've used Sorted Sets for leaderboards and Sets for tracking online users. The right structure depends on the access pattern."

---

### Q5: What is the difference between Redis and Memcached?

| Feature | Redis | Memcached |
|---------|-------|-----------|
| Data Structures | Strings, Lists, Sets, Hashes, etc. | Only Strings |
| Persistence | Yes (RDB, AOF) | No |
| Replication | Yes (Master-Replica) | No |
| Pub/Sub | Yes | No |
| Transactions | Yes (MULTI/EXEC) | No |
| Threading | Single-threaded | Multi-threaded |

**Detailed explanation:** Memcached is simpler. It only does key-value strings. No persistence. No replication. Redis is a superset: it can do everything Memcached does, plus persistence, replication, pub/sub, and rich data structures. Memcached is multi-threaded, so on a single machine with many cores, it might handle more connections. But Redis's single-threaded model is often fast enough, and Redis offers more features.

**Example:** For simple caching of API responses, both work. For a leaderboard, you need Redis Sorted Sets. For session storage with TTL, both work, but Redis can persist if you need it.

**How to say it in an interview:** "Redis and Memcached are both in-memory key-value stores. Memcached is simpler—only strings, no persistence. Redis has rich data structures, persistence, and replication. I'd use Redis when I need those features. I'd use Memcached only for very simple caching where multi-threading might give an edge."

---

## Intermediate Questions

### Q6: Explain Cache-Aside pattern.

**One-liner:** App checks cache first. On miss, queries DB and stores result in cache. On write, app updates DB and deletes cache.

**Detailed explanation:** The application is responsible for the cache. On read: check Redis. If hit, return. If miss, query database, store in Redis, return. On write: update database, delete the cache key. The next read will be a miss and will repopulate the cache. The cache doesn't know about the database. The database doesn't know about the cache. The app orchestrates both.

**Example:**
```
READ:  GET product:101 → nil → SELECT * FROM products WHERE id=101 → SET product:101 <json> EX 3600 → return
WRITE: UPDATE products SET ... → DEL product:101
```

**How to say it in an interview:** "Cache-Aside is the most common pattern. The app checks the cache first. On a miss, it fetches from the database, stores in the cache, and returns. On writes, it updates the database and invalidates the cache. I'd use TTL as a safety net in case we miss an invalidation."

---

### Q7: What is cache invalidation? Why is it hard?

**One-liner:** Removing stale data from cache when the source data changes. It's hard because you need to know when and what to invalidate, especially in distributed systems.

**Detailed explanation:** When the database changes, the cached copy is stale. You must remove or update it. That's invalidation. It's hard because: (1) You need to know *when* data changed—every code path that writes must trigger invalidation. (2) You need to know *which* keys to invalidate—a product update might affect `product:101` and `category:electronics:page_1`. (3) In distributed systems, multiple app servers might have different views. Race conditions can occur.

**Strategies:** TTL (time-based expiry), event-based (delete on update), or both. Best practice: TTL as safety net + event-based for known changes.

**Example:** User updates profile. You run `userRepository.save(user)` and `redis.del("user:" + user.getId())`. If you forget the `redis.del`, users see old data until TTL expires.

**How to say it in an interview:** "Cache invalidation is removing stale data when the source changes. It's hard because you have to invalidate on every write path, and you have to figure out all the keys that depend on that data. I use event-based invalidation for known writes and TTL as a fallback."

!!! warning "Red flags in your answer"
    Don't say "we just use TTL" without mentioning that data can be stale for the whole TTL. Don't say "we invalidate everything"—that defeats the purpose of caching.

---

### Q8: What is a Cache Stampede? How do you prevent it?

**One-liner:** When a popular cache key expires, thousands of requests hit the database simultaneously. Prevent with mutex lock, early refresh, or stale-while-revalidate.

**Detailed explanation:** A hot key expires. Thousands of requests arrive at once. All get cache miss. All query the database. The database is overwhelmed. Solutions: (1) **Mutex:** Only one request fetches. Others wait and then read from the refreshed cache. (2) **Early refresh:** Refresh the cache at 80% of TTL so it never expires. (3) **Stale-while-revalidate:** Serve stale data immediately, refresh in background.

**Example:** Product "iPhone 15" cached with 1-hour TTL. At expiry, 10,000 users refresh. Without protection: 10,000 DB queries. With mutex: 1 DB query, 9,999 requests wait and get cache hit.

**How to say it in an interview:** "A cache stampede happens when a popular key expires and many requests hit the database at once. I'd use a distributed lock so only one request fetches; the rest wait and read from cache. Alternatively, refresh the cache before it expires so it never actually expires."

---

### Q9: What is cache penetration?

**One-liner:** Requests for data that doesn't exist in cache or database, so every request hits the database.

**Detailed explanation:** An attacker (or bug) queries for non-existent IDs: `GET /user/999999999`. The user doesn't exist. Cache returns nil. Database returns null. Every request is a cache miss. Cache never helps. Database gets hammered.

**Solutions:** (1) **Cache null values:** When DB returns "not found", cache that with a short TTL (e.g., 60 seconds). Next requests hit the cache. (2) **Bloom filter:** Probabilistic structure that can say "definitely doesn't exist." Check before DB. If "definitely doesn't exist," return 404 without querying.

**Example:** `redis.setex("user:null:999999", 60, "null")` when user not found. Next 10,000 requests for that ID hit Redis, not the database.

**How to say it in an interview:** "Cache penetration is when requests for non-existent data always miss the cache and hit the database. I'd cache null results with a short TTL so repeated requests for the same non-existent ID don't keep hitting the DB. A Bloom filter is another option to filter out known non-existent keys."

---

### Q10: What is cache avalanche?

**One-liner:** Many cache keys expire at the same time, causing a flood of database queries.

**Detailed explanation:** Unlike a stampede (one key), an avalanche is many keys. Example: app starts at 9 AM, caches 1000 products with TTL 1 hour. All expire at 10 AM. Users request products. All 1000 are cache miss. Database gets 1000 queries at once.

**Solutions:** (1) **Random jitter on TTL:** `TTL = 3600 + random(0, 300)`. Keys expire at different times. (2) **Stagger cache population:** Don't cache everything at once. (3) **Cache warming:** After restart, pre-load gradually.

**Example:** `redis.setex("product:" + id, 3600 + (int)(Math.random() * 300), product)` spreads expiry over 5 minutes.

**How to say it in an interview:** "Cache avalanche is when many keys expire together and cause a spike in DB load. I add random jitter to TTL so keys expire at different times. For example, 3600 + random(0, 300) seconds instead of exactly 3600."

---

## Advanced Questions

### Q11: How does Redis persist data?

**One-liner:** Two methods: RDB (snapshots) and AOF (append-only log).

| Method | How | Pros | Cons |
|--------|-----|------|------|
| **RDB** | Periodic full snapshots (e.g., every 5 min) | Fast recovery, compact files | Data loss between snapshots |
| **AOF** | Logs every write command | Minimal data loss | Larger files, slower recovery |
| **Both** | Use RDB + AOF together | Best of both worlds | More disk usage |

**Detailed explanation:** RDB takes a point-in-time snapshot of all data. Good for backups. If Redis crashes, you lose data since the last snapshot. AOF logs every SET, DEL, etc. On restart, Redis replays the log. More durable, but the log can grow large. Production often uses both: RDB for fast restarts, AOF for durability.

**Example:** With RDB every 5 minutes, you might lose up to 5 minutes of data on crash. With AOF with fsync every second, you lose at most 1 second.

**How to say it in an interview:** "Redis has RDB for snapshots and AOF for an append-only log. RDB is fast to recover but you can lose data since the last snapshot. AOF is more durable but recovery can be slower. For critical data, I'd use both—RDB for speed, AOF for durability."

---

### Q12: What is Redis replication?

**One-liner:** Master-Replica setup where replicas copy all data from the master. Writes go to master; reads can be distributed to replicas.

**Detailed explanation:** One Redis instance is the master. It handles all writes. Replicas connect to the master and receive a stream of commands to replicate. Replicas are read-only by default. You can scale reads by adding replicas. If the master fails, a replica can be promoted (manually or via Sentinel).

```
        ┌──────────┐
        │  Master  │  (Handles writes)
        └────┬─────┘
             │ Replication
      ┌──────┴──────┐
      ▼             ▼
┌──────────┐  ┌──────────┐
│ Replica 1│  │ Replica 2│  (Handle reads)
└──────────┘  └──────────┘
```

**How to say it in an interview:** "Redis replication is master-replica. The master handles writes; replicas sync from the master and can serve reads. It's useful for read scaling and high availability. If the master fails, we can promote a replica."

---

### Q13: What is Redis Sentinel?

**One-liner:** A system that monitors Redis instances and automatically promotes a replica to master if the master fails.

**Detailed explanation:** Sentinel runs as a separate process (or multiple for quorum). It monitors the master and replicas. If the master is down, Sentinel promotes a replica to master and notifies clients. Clients connect to Sentinel to discover the current master. It provides automatic failover without manual intervention.

**How to say it in an interview:** "Redis Sentinel provides automatic failover. It monitors the master and replicas. If the master fails, Sentinel promotes a replica and updates clients. It's the standard way to achieve high availability with Redis."

---

### Q14: What is Redis Cluster?

**One-liner:** Distributes data across multiple Redis nodes using hash slots for horizontal scaling.

**Detailed explanation:** Redis Cluster shards data across nodes. There are 16,384 hash slots. Each key is mapped to a slot: `CRC16(key) % 16384`. Each node owns a range of slots. When you SET a key, Redis routes it to the correct node. No single node holds all data. You can add nodes to scale.

```
Node 1: Slots 0 - 5460
Node 2: Slots 5461 - 10922
Node 3: Slots 10923 - 16383
```

**How to say it in an interview:** "Redis Cluster shards data across nodes using 16,384 hash slots. Each key maps to a slot, and each node owns a range. It's for horizontal scaling when a single node isn't enough. You need to design keys so related data lands on the same node, or use hash tags like `user:{123}:profile`."

---

### Q15: What is the difference between @Cacheable and @CachePut in Spring?

| Annotation | Behavior |
|-----------|----------|
| `@Cacheable` | Checks cache first. Skips method if hit |
| `@CachePut` | Always runs the method and updates cache |

**Detailed explanation:** `@Cacheable` is for reads. First call: cache miss, method runs, result cached. Second call: cache hit, method does not run. `@CachePut` is for writes. The method always runs. The return value is stored in the cache. Use `@CachePut` when you want to update the cache with the result of an update operation.

**Example:** `getProduct(id)` uses `@Cacheable`. `updateProduct(product)` uses `@CachePut` so the cache gets the updated product. Alternatively, use `@CacheEvict` on update to invalidate and let the next read repopulate.

**How to say it in an interview:** "`@Cacheable` checks the cache first and skips the method on a hit. `@CachePut` always runs the method and updates the cache. I use `@Cacheable` for reads and either `@CachePut` or `@CacheEvict` for writes, depending on whether I want to update the cache or invalidate it."

---

### Q16: What happens when Redis runs out of memory?

**One-liner:** Depends on `maxmemory-policy`. Default is `noeviction` (reject writes). For caching, use `allkeys-lru`.

| Policy | Behavior |
|--------|----------|
| `noeviction` | Returns error on writes (default) |
| `allkeys-lru` | Evicts least recently used keys |
| `volatile-lru` | Evicts LRU keys that have TTL set |
| `allkeys-random` | Evicts random keys |
| `volatile-ttl` | Evicts keys with shortest TTL |

**Detailed explanation:** When Redis hits `maxmemory`, it needs to decide what to do. `noeviction` means new writes fail. For a cache, you usually want `allkeys-lru` so Redis evicts old keys to make room. You lose cached data but the app keeps working.

**How to say it in an interview:** "When Redis runs out of memory, it depends on the eviction policy. For caching, I'd set `allkeys-lru` so Redis evicts least recently used keys. That way the app keeps working; we just get more cache misses. The default `noeviction` would cause writes to fail."

---

### Q17: How do you handle Redis downtime?

**One-liner:** Use fallback to database + circuit breaker. Design for cache failure.

**Detailed explanation:** Redis can go down. Your app should not crash. Wrap Redis calls in try-catch. On failure, fall back to the database. The app is slower but works. Use a circuit breaker to avoid hammering Redis when it's down. For high availability, use Redis Sentinel or a managed service (AWS ElastiCache, etc.).

**Example:**
```java
try {
    return redis.get("product:" + id);
} catch (RedisConnectionException e) {
    return database.findById(id);
}
```

**How to say it in an interview:** "I design for cache failure. If Redis is down, we fall back to the database. The app is slower but still works. I'd also use a circuit breaker so we don't keep trying Redis when it's clearly down. For HA, Redis Sentinel or a managed service handles failover."

---

### Q18: What is a Redis transaction?

**One-liner:** Group multiple commands using MULTI/EXEC so they run together without interruption from other clients.

**Detailed explanation:** `MULTI` starts a transaction. Commands are queued. `EXEC` runs them all. No other client's commands are interleaved. But: Redis transactions are NOT like SQL transactions. There is no rollback. If one command fails, the others still execute. It's more like "batch" than "transaction."

**Example:**
```bash
MULTI
SET user:1:name "Satjit"
SET user:1:age "25"
INCR total_users
EXEC
```

**How to say it in an interview:** "Redis transactions use MULTI and EXEC to group commands. They run atomically without interleaving from other clients. But there's no rollback—if one command fails, others still run. It's useful for batching, not for ACID guarantees."

!!! warning "Red flags in your answer"
    Don't say "Redis transactions provide ACID" or "rollback on failure." That's wrong.

---

## Scenario / Design Questions

### Q19: Design a rate limiter using Redis.

**One-liner:** Use INCR with EXPIRE for a fixed window, or Sorted Sets for a sliding window.

**Detailed explanation:** 
- **Fixed window:** `INCR rate:user:42`, `EXPIRE rate:user:42 60`. If count > 100, reject. Resets every 60 seconds. Simple but can allow 200 requests at the boundary (end of one window + start of next).
- **Sliding window:** Use Sorted Set. Score = timestamp. Add each request: `ZADD rate:user:42 <timestamp> <request-id>`. Remove old: `ZREMRANGEBYSCORE rate:user:42 0 <now-60>`. Count: `ZCARD rate:user:42`. If > 100, reject. More accurate.

**Example (fixed):**
```bash
INCR "rate:user:42"
EXPIRE "rate:user:42" 60
GET "rate:user:42"
# If > 100, reject
```

**How to say it in an interview:** "I'd use Redis INCR with EXPIRE for a simple fixed-window rate limiter. For a sliding window, I'd use a Sorted Set with timestamps as scores, remove old entries, and count. The sliding window is more accurate but a bit more complex."

---

### Q20: Design a session store using Redis.

**One-liner:** SETEX with session ID as key, JSON payload as value, TTL for expiry.

**Detailed explanation:** On login, create a session ID. Store `SETEX session:<id> 1800 '{"userId":42,"role":"admin"}'`. TTL of 30 minutes. On each request, GET the session. If nil, redirect to login. On logout, DEL the key. On activity, EXPIRE to extend the session.

**Example:**
```bash
SETEX "session:abc123" 1800 '{"userId": 42, "role": "admin"}'
GET "session:abc123"
EXPIRE "session:abc123" 1800   # Extend on activity
DEL "session:abc123"            # Logout
```

**How to say it in an interview:** "I'd use Redis SETEX with the session ID as the key and a JSON payload as the value. TTL handles expiry. On each request, I GET the session. If it's nil, the user is logged out. On logout, I DEL the key. For sliding expiry, I'd refresh EXPIRE on activity."

---

### Q21: Design a "trending posts" feature. Posts should be ranked by likes in the last 24 hours.

**One-liner:** Use a Sorted Set. Score = like count (or a time-decayed score). Add/update on like. ZREVRANGE for top N.

**Detailed explanation:** When a user likes a post, `ZINCRBY trending:posts 1 post_123`. Or use a time-decayed score: score = likes / (1 + decay * hours_old). To get top 10: `ZREVRANGE trending:posts 0 9 WITHSCORES`. To reset daily: use a key with the date, e.g., `trending:posts:2025-02-25`, and rotate.

**Example:**
```bash
ZINCRBY trending:posts 1 "post_101"
ZREVRANGE trending:posts 0 9 WITHSCORES
```

**How to say it in an interview:** "I'd use a Redis Sorted Set. Score is the number of likes. When someone likes a post, I ZINCRBY. To get trending, I ZREVRANGE for the top N. For 'last 24 hours,' I might use a key per day and rotate, or use a time-decayed score."

---

### Q22: How would you implement "who's online" for a chat app?

**One-liner:** Use a Redis Set. SADD on login/activity, SREM on logout. SMEMBERS to list. Use a short TTL or heartbeat to detect disconnects.

**Detailed explanation:** When a user comes online or sends a message, `SADD online_users user_42`. When they log out, `SREM online_users user_42`. To list online users: `SMEMBERS online_users`. For disconnect detection (user closed browser without logging out), use a heartbeat: every 30 seconds, refresh a key like `online:user_42` with SETEX 60. A background job removes users whose key expired. Or use a Sorted Set with last-seen timestamp as score.

**Example:**
```bash
SADD online_users "user_42"
SREM online_users "user_42"
SMEMBERS online_users
```

**How to say it in an interview:** "I'd use a Redis Set. SADD when the user comes online, SREM on logout. For detecting disconnects, I'd use a heartbeat—refresh a key with SETEX every 30 seconds. If the key expires, the user is considered offline. A background job can sync the Set with these keys."

---

### Q23: Your cache hit ratio is 30%. How do you improve it?

**One-liner:** Increase TTL, cache more data types, fix invalidation (maybe you're invalidating too aggressively), warm the cache, and analyze what's being requested.

**Detailed explanation:** Low hit ratio means most requests are cache misses. Possible causes: (1) TTL too short—data expires before it's reused. (2) Caching the wrong things—maybe you're not caching high-traffic endpoints. (3) Over-invalidation—deleting cache too often. (4) Cold cache—after restarts, everything is miss. (5) Unique keys—each request generates a different key (e.g., timestamp in key). Fix: Increase TTL where appropriate. Add cache warming. Use cache analytics to see which keys are hit. Avoid over-invalidation. Normalize keys.

**How to say it in an interview:** "I'd first analyze what's being requested and what's cached. I'd check if TTL is too short, if we're invalidating too aggressively, or if we're not caching the right endpoints. I'd add cache warming for popular data. I'd also ensure we're not putting unique values like timestamps in cache keys."

---

### Q24: How would you cache a user's feed (e.g., Instagram-style) that is personalized and changes frequently?

**One-liner:** Cache the feed for a short TTL (e.g., 1–5 minutes). Or cache the components (followed users' posts) and merge on read. Or pre-compute and cache per user with short TTL.

**Detailed explanation:** A feed is personalized (different for each user) and changes when new posts arrive. Options: (1) **Short TTL per user:** Cache `feed:user_42` with 1–5 min TTL. Simple but more cache misses. (2) **Cache components:** Cache each followed user's recent posts. On read, merge and sort. More complex but better reuse. (3) **Pre-compute in background:** When a new post is created, push to followers' feed cache. Write-heavy but read is instant. Choice depends on read/write ratio and consistency requirements.

**How to say it in an interview:** "I'd cache the feed per user with a short TTL—maybe 1–5 minutes—since it changes often. For better hit ratio, I could cache the components—each followed user's recent posts—and merge on read. For real-time, I'd push new posts to followers' cached feeds in the background. It's a trade-off between freshness and cache efficiency."

---

### Q25: Explain when you would use Redis vs a message queue like RabbitMQ or Kafka.

**One-liner:** Redis for simple, fast queues with low throughput and no persistence requirements. RabbitMQ/Kafka for durable, high-throughput, complex routing.

**Detailed explanation:** Redis Lists or Streams can act as a queue. Good for: simple task queues, low volume, speed over durability. If a worker dies, tasks might be lost (unless you use Redis Streams with consumer groups). RabbitMQ and Kafka are built for messaging: persistence, acknowledgments, dead-letter queues, complex routing. Use them when: you need durability, high throughput, or complex patterns (pub/sub, fanout, etc.).

**Example:** Redis List for "send email" tasks in a small app. Kafka for event streaming, order processing, or analytics pipelines.

**How to say it in an interview:** "Redis is good for simple, fast queues—task queues, job processing. If I need durability, retries, and complex routing, I'd use RabbitMQ or Kafka. Redis is in-memory first; message queues are built for persistence and reliability. For a small app with simple needs, Redis might be enough. For production event-driven systems, I'd use Kafka or RabbitMQ."

---

### Q26: What is Redis Cluster? How does it differ from Sentinel?

**One-liner:** Redis Cluster shards data across multiple nodes using hash slots. Sentinel provides HA for a single master-replica setup. Cluster = horizontal scaling. Sentinel = failover only.

**Detailed explanation:** Redis Cluster distributes data across multiple nodes. There are 16,384 hash slots. Each key maps to a slot: `CRC16(key) % 16384`. Each node owns a range of slots. Data is sharded. You can add nodes to scale. It's multi-master—each node can handle reads and writes for its slots. Sentinel, by contrast, monitors one master and its replicas. When the master fails, Sentinel promotes a replica. No data sharding. One logical dataset. Cluster is for when one node can't hold all your data. Sentinel is for when you need automatic failover but data fits on one node.

**Cross-slot limitations:** Commands that touch multiple keys (e.g., MGET, MSET, transactions) only work if all keys hash to the same slot. Use hash tags: `user:{123}:profile` and `user:{123}:settings` both hash to the same slot because `{123}` is used for the hash. Without hash tags, you get CROSSSLOT errors.

| Feature | Redis Cluster | Redis Sentinel |
|---------|---------------|----------------|
| Data sharding | Yes (hash slots) | No |
| Multi-master | Yes (each node handles its slots) | No (one master) |
| Scaling | Add nodes, rebalance slots | Add replicas (read scaling only) |
| Failover | Built-in (each shard has leader) | Sentinel promotes replica |
| Use case | Data too big for one node | HA for single dataset |

**How to say it in an interview:** "Redis Cluster shards data across nodes using 16,384 hash slots. It's for horizontal scaling when one node isn't enough. Sentinel is for high availability—it monitors master and replicas and promotes on failure. Cluster gives you both scaling and HA. Sentinel gives you HA only. For cross-slot commands, I'd use hash tags like `user:{id}:field` so related keys land on the same node."

---

### Q27: Explain the Redis pipeline. When would you use it?

**One-liner:** A pipeline batches multiple commands into one network round-trip. Use it when you need to send many commands and latency matters.

**Detailed explanation:** Normally, each Redis command is a separate request. Client sends GET. Waits for response. Sends another GET. Waits. If each round-trip takes 1ms, 100 commands take 100ms. A pipeline sends all commands at once. The client buffers them. Sends in one batch. Redis processes them in order. Returns all responses in one batch. One round-trip. 100 commands might take 2–3ms total. Huge win when you have many small commands.

**When to use:** (1) Bulk loading—inserting thousands of keys. (2) Reading many keys in one request (e.g., MGET, or pipeline of GETs if keys aren't in same slot in Cluster). (3) Any loop that does many Redis calls. Pipeline reduces network round-trips. That's the main bottleneck when the client and Redis are on different machines.

**Example (Java with Jedis):**
```java
Jedis jedis = new Jedis("localhost", 6379);
Pipeline pipeline = jedis.pipelined();
for (int i = 0; i < 1000; i++) {
    pipeline.set("key:" + i, "value" + i);
}
pipeline.sync();  // Sends all at once, waits for all responses
```

**Example (Redis CLI):**
```bash
(echo -en "SET key1 val1\r\nSET key2 val2\r\nGET key1\r\n"; sleep 0.1) | redis-cli --pipe
```

!!! warning "Pipeline vs Transaction"
    Pipeline is not a transaction. Commands from other clients can interleave. If you need atomicity, use MULTI/EXEC. Pipeline is purely for batching network calls.

**How to say it in an interview:** "A pipeline batches multiple commands into one network round-trip. Instead of 100 round-trips for 100 commands, you get one. I'd use it when bulk loading data or when a loop does many Redis calls. It doesn't provide atomicity—for that I'd use MULTI/EXEC. Pipeline is about reducing latency, not transactions."

---

### Q28: What is Redis Pub/Sub? What are its limitations?

**One-liner:** Pub/Sub lets publishers send messages to channels. Subscribers receive them. Fire-and-forget. No persistence. No replay. Offline subscribers miss messages.

**Detailed explanation:** PUBLISH sends a message to a channel. SUBSCRIBE makes a client listen. When a message is published, all subscribers get it. It's real-time. But: (1) **No persistence**—messages are not stored. If no one is subscribed, the message is gone. (2) **Fire-and-forget**—publisher gets no acknowledgment that anyone received it. (3) **Offline subscribers miss messages**—if a subscriber disconnects, they never see messages sent while offline. (4) **No backpressure**—if a subscriber is slow, Redis doesn't wait. It pushes. Slow subscribers can cause memory issues. (5) **No consumer groups**—unlike Kafka, you can't have multiple consumers sharing the load with each getting a copy.

**When to use Pub/Sub:** Live dashboards, live scores, "user is typing" indicators, presence notifications. When missing a message is acceptable. When you need the lowest latency.

**When to use Kafka instead:** Order events, payment events, chat history, audit logs. When you need guaranteed delivery, replay, or offline consumers to catch up.

**How to say it in an interview:** "Redis Pub/Sub is fire-and-forget. No persistence. No replay. Offline subscribers miss messages. I'd use it for live updates where missing one is okay—like live scores or presence. For anything that needs guaranteed delivery or replay, I'd use Kafka or Redis Streams."

---

### Q29: How would you implement a distributed lock using Redis?

**One-liner:** Use `SET key value NX EX seconds` to acquire. Only one client succeeds. Release by deleting the key, but only if you own it (check value). Use Lua script for atomic release.

**Detailed explanation:** The basic pattern: `SET lock:resource uuid NX EX 30`. NX = set only if not exists. EX 30 = expire in 30 seconds. Only one client gets the lock. Others get nil. To release: check that the value matches your uuid, then DEL. Why check? If your process pauses (GC, network) and the lock expires, another client might acquire it. When you wake up, you might DEL a lock you no longer own. So: `if GET lock:resource == my_uuid then DEL lock:resource`. This must be atomic—use a Lua script.

**Redlock algorithm:** For higher reliability across multiple Redis nodes, use Redlock. Acquire the lock on a majority of N independent Redis instances (e.g., 5). If you get it on 3+, you have the lock. Reduces risk of single-node failure. More complex. Use a library (e.g., Redisson) rather than implementing yourself.

**Edge cases:** (1) **Clock skew**—if servers have different times, TTL might be wrong. (2) **Long GC pause**—your process holds the lock but is paused. Lock expires. Another process gets it. You wake up and think you still have it. Use a "fencing token" (monotonically increasing value) when doing critical work. (3) **Lock extension**—if work takes longer than 30 seconds, extend the TTL. Use a background thread or Redlock's extension mechanism.

**How to say it in an interview:** "I'd use SET with NX and EX to acquire the lock. For release, I'd use a Lua script to atomically check the value matches my uuid and then DEL. For multi-node setups, I'd consider Redlock. I'd also handle the case where the lock expires while I'm still working—maybe a fencing token or extending the TTL."

---

### Q30: What is Redis Streams? How does it compare to Lists?

**One-liner:** Streams are append-only logs with consumer groups, message IDs, and acknowledgment. Lists are simple push/pop. Use Streams when you need Kafka-like semantics.

**Detailed explanation:** Redis Streams (added in Redis 5.0) are like a log. You XADD messages. Each gets a unique ID (timestamp-sequence, e.g., `1635789012345-0`). Consumers read with XREAD. Consumer groups let multiple consumers share the work—each message goes to one consumer in the group. XACK acknowledges processing. Unacked messages can be retried. You can replay from any ID. Lists (LPUSH/RPOP) are simpler. No IDs. No consumer groups. No ack. Once you pop, it's gone. If the consumer crashes before processing, the message is lost.

| Feature | Redis Lists | Redis Streams |
|---------|-------------|---------------|
| Message ID | No | Yes (auto-generated) |
| Consumer groups | No | Yes |
| Acknowledgment | No (pop = consume) | Yes (XACK) |
| Replay | No | Yes (read from ID) |
| Persistence | Optional | Optional (same as Redis) |
| Use case | Simple task queue | Reliable queue, event sourcing |

**When to use Lists:** Simple task queues. Fire-and-forget. Low volume. If a task is lost, it's okay.

**When to use Streams:** Need at-least-once delivery. Multiple consumers. Replay. Event sourcing. Higher reliability requirements.

**How to say it in an interview:** "Redis Streams are append-only logs with consumer groups and message IDs. Lists are push/pop—no IDs, no ack. Once you pop from a List, it's gone. Streams let you acknowledge and replay. I'd use Lists for simple task queues. Streams when I need reliability, consumer groups, or replay—similar to Kafka but lighter weight."

---

## Top 5 Mistakes in Redis Interviews

!!! warning "Mistake 1: Saying Redis is a database"
    Redis is a cache and data structure store. It can persist, but it's not a replacement for PostgreSQL. Don't say "we use Redis as our primary database" unless you mean something very specific (e.g., session store). Clarify: "Redis is our cache layer" or "Redis stores sessions."

!!! warning "Mistake 2: Confusing Cache-Aside with Write-Through"
    Cache-Aside: app manages cache. On miss, app fetches from DB and populates cache. Write-Through: cache sits in front of DB; writes go to cache, which writes to DB. They're different. Know both. Cache-Aside is more common.

!!! warning "Mistake 3: Saying Redis transactions have rollback"
    Redis MULTI/EXEC does NOT rollback on failure. If one command fails, others still run. There is no ROLLBACK. Say "Redis transactions are for batching and atomicity of execution order, not ACID rollback."

!!! warning "Mistake 4: Recommending KEYS * in production"
    KEYS * scans all keys. It blocks Redis. With millions of keys, it can freeze the server for seconds. Use SCAN instead. It's incremental and non-blocking. This is a classic interview trap.

!!! warning "Mistake 5: Not knowing when to use Cluster vs Sentinel"
    Sentinel = HA for single dataset. Cluster = data sharding + HA. If your data fits in RAM on one node, Sentinel is simpler. If you need to scale beyond one node's memory, you need Cluster. Don't mix them up.

---

## Quick Summary

| Topic | Key Point |
|-------|-----------|
| Speed | In-memory, single-threaded, ~100K ops/sec |
| Persistence | RDB (snapshots) + AOF (write log) |
| Scaling | Replication (reads) + Cluster (data sharding) |
| High Availability | Redis Sentinel for auto-failover |
| Caching Pattern | Cache-Aside is the default |
| Common Problems | Stampede, Penetration, Avalanche |
| Spring Integration | @Cacheable, @CachePut, @CacheEvict |
| Memory Full | Use `allkeys-lru` eviction policy |

---

## 30-Second Revision Cheat Sheet

| # | Topic | One-Line Answer |
|---|-------|-----------------|
| 1 | What is Redis? | In-memory key-value store for caching, sessions, real-time data |
| 2 | Why fast? | RAM, single-threaded, efficient data structures |
| 3 | Single-threaded? | Yes for commands. ~100K ops/sec. Scale with Cluster |
| 4 | Data structures? | String, List, Set, Hash, Sorted Set, Stream, Bitmap, HyperLogLog |
| 5 | Redis vs Memcached? | Redis: rich types, persistence, replication. Memcached: strings only |
| 6 | Cache-Aside? | App checks cache → miss → DB → store in cache. Write: update DB, delete cache |
| 7 | Cache invalidation? | Hard. Use event-based + TTL. Know what to invalidate |
| 8 | Cache stampede? | Hot key expires, many hit DB. Fix: mutex, early refresh, stale-while-revalidate |
| 9 | Cache penetration? | Requests for non-existent data. Fix: cache null, Bloom filter |
| 10 | Cache avalanche? | Many keys expire together. Fix: TTL jitter |
| 11 | Persistence? | RDB (snapshots) + AOF (append log). Use both for production |
| 12 | Replication? | Master-Replica. Writes to master, reads from replicas |
| 13 | Sentinel? | Auto-failover. Monitors master, promotes replica on failure |
| 14 | Cluster? | Hash slots (16384), data sharding, multi-master. Cross-slot: use hash tags |
| 15 | @Cacheable vs @CachePut? | Cacheable: check first, skip if hit. CachePut: always run, update cache |
| 16 | Out of memory? | maxmemory-policy. Use allkeys-lru for cache |
| 17 | Redis down? | Fallback to DB. Circuit breaker. Design for failure |
| 18 | Transactions? | MULTI/EXEC. No rollback. Batch only |
| 19 | Rate limiter? | INCR + EXPIRE (fixed) or Sorted Set (sliding) |
| 20 | Session store? | SETEX session:id TTL json |
| 21 | Trending posts? | Sorted Set, ZINCRBY on like, ZREVRANGE for top N |
| 22 | Who's online? | Set: SADD/SREM. Heartbeat with SETEX for disconnect |
| 23 | Low hit ratio? | Increase TTL, cache more, fix invalidation, warm cache |
| 24 | Personalized feed? | Short TTL, cache components, or push to cache on write |
| 25 | Redis vs Kafka? | Redis: fast queues, simple. Kafka: durable, replay, high throughput |
| 26 | Cluster vs Sentinel? | Cluster = sharding. Sentinel = failover only |
| 27 | Pipeline? | Batch commands, one round-trip. For bulk ops |
| 28 | Pub/Sub? | Fire-and-forget. No persistence. Offline = miss messages |
| 29 | Distributed lock? | SET NX EX. Lua for atomic release. Redlock for multi-node |
| 30 | Streams vs Lists? | Streams: IDs, consumer groups, ack, replay. Lists: simple push/pop |

---

**Done with Redis! Next:** [Kafka Basics](../kafka/kafka-basics.md) - Start your Kafka journey.
