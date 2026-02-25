# Common Mistakes

## Why This Page Exists

Every developer makes these mistakes when starting with Redis. Some of them are harmless during development but cause production outages when your app goes live. This page helps you avoid learning these lessons the hard way.

---

## Mistake 1: Using Redis as Your Primary Database

### The Mistake

"Redis is so fast, let me just store everything in Redis. Who needs MySQL?"

### Why It's Wrong

Redis stores data in RAM. RAM is expensive and limited. A server with 64 GB of RAM costs significantly more than one with 4 TB of disk. If your data grows beyond what RAM can hold, you're stuck.

Also, Redis data structures are optimized for speed, not for complex queries. You can't do `JOIN`, `GROUP BY`, or complex `WHERE` clauses. Try finding "all users who signed up in the last 7 days AND live in Delhi AND have ordered more than 3 times" in Redis. Good luck.

### What To Do Instead

Use Redis **alongside** your database. Your database (MySQL, PostgreSQL, MongoDB) is the source of truth. Redis is a fast layer in front of it.

```
Correct architecture:
  App ──▶ Redis (fast cache) ──miss──▶ Database (source of truth)

Wrong:
  App ──▶ Redis (everything lives here, no database) 💀
```

!!! tip "Interview Tip"
    If an interviewer asks "Can you use Redis as your main database?", say: "Technically possible for simple use cases like session storage, but it's not recommended for primary data. Redis lacks complex querying, is limited by RAM, and while it has persistence (RDB/AOF), it's not as durable as a traditional database. It's best used as a cache or complementary store alongside a relational database."

---

## Mistake 2: Running Without maxmemory in Production

### The Mistake

You deploy Redis with default settings. No `maxmemory` configured. Redis grows and grows. Eventually it consumes all available RAM on the server. The Linux OOM (Out of Memory) killer steps in and kills the Redis process. Or worse, it kills other critical processes.

### Why It's Dangerous

Without a memory limit, Redis will happily consume every byte of RAM. Your application, your OS, and other services on the same server all compete for the remaining memory. Everything slows down. Then things start dying.

### What To Do Instead

Always set `maxmemory` in production:

```
maxmemory 4gb
maxmemory-policy allkeys-lru
```

This tells Redis: "Use up to 4 GB. When full, evict the least recently used keys." Your Redis stays within bounds, and old cache data is automatically evicted to make room for new data.

---

## Mistake 3: Using KEYS * in Production

### The Mistake

You want to find all keys matching a pattern. You run:

```bash
KEYS user:*
```

### Why It's Dangerous

`KEYS` scans **every single key** in Redis. If you have 10 million keys, Redis is blocked for seconds. During those seconds, every other request to Redis is queued. Your entire application hangs. Users see timeouts. Monitoring alerts fire. It's a mini-outage.

Redis is single-threaded. While it's running `KEYS`, it literally cannot do anything else.

### What To Do Instead

Use `SCAN`. It does the same thing but in small, non-blocking batches:

```bash
SCAN 0 MATCH user:* COUNT 100
# Returns a small batch + cursor for next batch
```

In Java:

```java
ScanOptions options = ScanOptions.scanOptions()
    .match("user:*")
    .count(100)
    .build();
Cursor<byte[]> cursor = redis.scan(options);
while (cursor.hasNext()) {
    String key = new String(cursor.next());
    // Process key
}
```

---

## Mistake 4: Not Setting TTL on Cache Data

### The Mistake

You cache data in Redis but forget to set an expiry:

```bash
SET product:101 '{"name":"Laptop","price":50000}'
# No EXPIRE. Lives forever.
```

### Why It's a Problem

Over time, your cache fills up with stale data that's never accessed again. Memory usage grows. Eventually Redis is full of old, irrelevant data while fresh data can't be stored.

If the product price changes to 60000, the cache still shows 50000. Forever.

### What To Do Instead

Always use `SETEX` or `SET ... EX` for cache data:

```bash
SETEX product:101 900 '{"name":"Laptop","price":50000}'
# Expires in 15 minutes
```

In Spring Boot:

```java
@Cacheable(value = "products", key = "#id")
// TTL is configured in RedisCacheConfiguration.entryTtl()
```

Rule: **If it's cache data, it needs a TTL. No exceptions.**

---

## Mistake 5: Storing Large Values

### The Mistake

You store entire JSON responses, complete HTML pages, or large binary data as Redis values:

```bash
SET page:home "<html>... 500 KB of HTML ...</html>"
```

### Why It's a Problem

Redis is single-threaded. A single large value (say 5 MB) takes time to serialize, transmit over the network, and deserialize. While Redis is sending this 5 MB blob to one client, every other client waits.

Large values also consume disproportionate memory. A few thousand 5 MB values = 5-10 GB of RAM gone.

### What To Do Instead

- Keep values small. Ideally under 100 KB.
- Store only what you need, not entire objects. If you only need the product name and price, don't cache the entire product description with reviews.
- For large data, store it in an object store (S3) and cache just the reference URL in Redis.
- If you must store large values, compress them first.

```java
// Instead of storing everything:
redis.set("user:42", entireUserObjectAsJson);  // 50 KB

// Store only what's frequently accessed:
redis.hset("user:42", Map.of(
    "name", "Satjit",
    "email", "satjit@mail.com"
));  // 200 bytes
```

---

## Mistake 6: Ignoring Serialization

### The Mistake

In Spring Boot, you use `RedisTemplate` with default serialization. You store an object. It works. Then you try to read it from `redis-cli` and see gibberish:

```bash
GET user:42
# "\xac\xed\x00\x05t\x00\x0cSome Object..."
```

That's Java's default serialization (JDK serializer). It's not human-readable, not cross-language compatible, and wastes space.

### What To Do Instead

Configure Redis to use JSON serialization:

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(
        new GenericJackson2JsonRedisSerializer());
    return template;
}
```

Now data in Redis is human-readable JSON. You can inspect it with `redis-cli`, debug issues, and even read it from other languages.

---

## Mistake 7: Not Handling Redis Downtime

### The Mistake

Your app assumes Redis is always available. Redis goes down. Every request throws an exception. Your entire app crashes. Users see 500 errors.

### Why It's Dangerous

Redis is a cache, not your database. If the cache is down, your app should still work — just slower (hitting the database directly).

### What To Do Instead

Add a fallback. If Redis is unavailable, skip the cache and go to the database:

```java
public Product getProduct(Long id) {
    try {
        Product cached = redis.get("product:" + id);
        if (cached != null) return cached;
    } catch (RedisConnectionException e) {
        log.warn("Redis down, falling back to database");
    }

    // Cache miss or Redis down → query database
    Product product = database.findById(id);

    try {
        redis.setex("product:" + id, 600, product);
    } catch (RedisConnectionException e) {
        log.warn("Could not write to cache");
    }

    return product;
}
```

Your app degrades gracefully. Slower, but functional.

!!! tip "Interview Insight"
    Interviewers love asking "What happens if Redis goes down?" The answer is: "The application should have a fallback. For caching use cases, the app falls back to the database. I'd use a circuit breaker pattern (like Resilience4j) to stop hammering a dead Redis and automatically retry when it recovers."

### Circuit Breaker Pattern with Resilience4j

When Redis is down, your app shouldn't keep hammering it with requests. That wastes resources and slows everything down. A circuit breaker stops trying after repeated failures. It "opens" the circuit. After a cooldown period, it "half-opens" to test if Redis is back. If it works, the circuit "closes" and normal operation resumes.

Add Resilience4j to your project:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>1.7.1</version>
</dependency>
```

Configure the circuit breaker in `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      redisCache:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
```

Wrap your Redis calls with the circuit breaker:

```java
@CircuitBreaker(name = "redisCache", fallbackMethod = "getProductFromDb")
public Product getProduct(Long id) {
    Product cached = redis.get("product:" + id);
    if (cached != null) return cached;

    Product product = database.findById(id);
    try {
        redis.setex("product:" + id, 600, product);
    } catch (RedisConnectionException e) {
        log.warn("Could not write to cache");
    }
    return product;
}

private Product getProductFromDb(Long id, Exception e) {
    log.warn("Circuit open or Redis failed, falling back to DB: {}", e.getMessage());
    return database.findById(id);
}
```

**How it works:** After 5 calls, if 50% fail, the circuit opens. No more Redis calls for 10 seconds. Then it tries 3 calls. If they succeed, the circuit closes. If they fail, it opens again for another 10 seconds. Your app stays responsive even when Redis is struggling.

---

## Mistake 8: Caching Too Aggressively

### The Mistake

You cache everything. Every database query, every API response, every computed value. Your Redis has 50 million keys. Most of them are accessed once and never again.

### Why It's a Problem

You're paying for RAM to store data nobody reads. Your memory is full of one-time queries. The actually popular data gets evicted because there's no room.

### What To Do Instead

Cache data that is:

- **Read frequently** (accessed > 10 times before it changes)
- **Expensive to compute** (complex queries, external API calls)
- **Shared across users** (product listings, config, common search results)

Don't cache data that is:

- Unique per user and rarely re-read
- Written more often than read
- Too large to justify the RAM cost

---

## Mistake 9: Not Monitoring Cache Hit Ratio

### The Mistake

You set up caching but never check if it's actually working. Your hit ratio is 20%. For every 5 requests, only 1 is served from cache. The other 4 hit the database anyway. Your cache is barely helping.

### How To Check

```bash
INFO stats
# keyspace_hits:80000
# keyspace_misses:20000
# Hit ratio = 80000 / (80000 + 20000) = 80%
```

| Hit Ratio | Assessment |
|-----------|-----------|
| 90-99% | Excellent. Cache is doing its job. |
| 80-90% | Good. Typical for most apps. |
| 50-80% | Needs improvement. Check TTLs and what you're caching. |
| Below 50% | Problem. You might be caching the wrong things. |

### How To Improve Hit Ratio

1. **Increase TTL** — if data doesn't change often, let it live longer.
2. **Cache warming** — pre-load popular data on app startup.
3. **Cache the right things** — focus on high-traffic, low-change data.
4. **Check key patterns** — are you generating unique keys that never repeat?

---

## Mistake 10: Storing Passwords or Secrets in Redis

### The Mistake

Storing sensitive data like plaintext passwords, credit card numbers, or API keys in Redis without encryption.

### Why It's Dangerous

Redis doesn't encrypt data at rest by default. Anyone with access to the Redis server (or a memory dump) can read everything. Also, if your Redis is exposed to the network without authentication, anyone can connect and read all data.

### What To Do Instead

- **Never store plaintext passwords** (anywhere, not just Redis).
- Encrypt sensitive data before storing in Redis.
- Enable Redis authentication: `requirepass your_strong_password`
- Use TLS for connections: `tls-port 6380`
- Never expose Redis to the public internet.

---

## Mistake 11: Using Redis for Full-Text Search

### The Mistake

You need to search products by name. You think: "Redis is fast. I'll store products and search them." You try to use Redis commands to find "laptop" across thousands of product names. You end up with `KEYS *` or `SCAN` plus filtering in your application. It's slow. It doesn't support fuzzy matching, relevance scoring, or proper search features.

### Why It's Wrong

Redis is a key-value store. It's built for lookups by key, not for searching inside values. There's no `FIND * WHERE value CONTAINS "laptop"` command. You'd have to scan every key, fetch every value, and filter in your app. That defeats the purpose of Redis.

For real full-text search (autocomplete, fuzzy match, relevance ranking, faceted search), you need a search engine. Elasticsearch, OpenSearch, or Meilisearch are designed for this.

### What To Do Instead

- **Simple key lookups:** Use Redis. `GET product:123` is perfect.
- **Full-text search:** Use Elasticsearch or similar. Index your data. Query with proper search syntax.
- **If you must use Redis:** Consider the **RediSearch** module. It adds search capabilities to Redis. But it's an add-on, not core Redis. Many managed Redis services don't support it. Evaluate carefully.

!!! info "RediSearch Module"
    RediSearch is a Redis module that adds secondary indexes and full-text search. You can create indexes on Hashes and search with `FT.SEARCH`. It's powerful but requires Redis Stack or a Redis build with the module. For most teams, Elasticsearch is the standard choice for search.

---

## Mistake 12: Not Using Connection Pooling

### The Mistake

You open a new Redis connection for every request. Request comes in. Create connection. Run command. Close connection. Next request. Repeat. Your app handles 1,000 requests per second. That's 1,000 connections created and destroyed every second.

### Why It's Expensive

Creating a TCP connection costs time. DNS lookup, TCP handshake, TLS handshake (if using TLS), Redis AUTH. Each new connection adds 1-5 ms of latency. And the OS has to allocate file descriptors, buffers, and kernel resources. Under load, you exhaust connection limits. "Too many open files." Your app crashes.

### What To Do Instead

Use connection pooling. Create a pool of connections at startup. Reuse them. When a request needs Redis, it borrows a connection from the pool. When done, it returns the connection. No create/destroy per request.

**With Lettuce (Spring Boot default):**

Lettuce uses a single shared connection by default. It's non-blocking and efficient. For high throughput, configure a connection pool:

```java
@Bean
public LettuceConnectionFactory redisConnectionFactory() {
    LettuceClientConfiguration config = LettucePoolingClientConfiguration.builder()
        .poolConfig(GenericObjectPoolConfig.builder()
            .minIdle(5)
            .maxIdle(10)
            .maxTotal(20)
            .build())
        .build();

    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration("localhost", 6379);
    return new LettuceConnectionFactory(serverConfig, config);
}
```

**With Jedis:**

```java
JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", 6379);
try (Jedis jedis = pool.getResource()) {
    jedis.get("key");
}
```

**Rule of thumb:** Pool size = number of threads that need Redis concurrently. Don't set maxTotal to 1000 if you have 10 worker threads. Start with 20-50. Monitor. Adjust.

!!! warning "Spring Boot Default"
    Spring Boot uses Lettuce by default. Lettuce is single-connection by default but handles multiplexing well. For very high throughput (10,000+ ops/sec), enable pooling as shown above.

---

## Quick Summary: The 12 Commandments

| # | Commandment |
|---|------------|
| 1 | Redis is a cache, not a primary database |
| 2 | Always set `maxmemory` in production |
| 3 | Use `SCAN`, never `KEYS *` |
| 4 | Every cache key needs a TTL |
| 5 | Keep values small (< 100 KB) |
| 6 | Use JSON serialization, not Java default |
| 7 | Handle Redis downtime gracefully |
| 8 | Cache selectively, not everything |
| 9 | Monitor your cache hit ratio |
| 10 | Never store secrets in plaintext |
| 11 | Don't use Redis for full-text search (use Elasticsearch or RediSearch) |
| 12 | Use connection pooling, never open a new connection per request |

---

## Debugging Checklist: When Something Goes Wrong with Redis

When Redis isn't behaving, work through this list. It covers the most common failure points.

### Step 1: Check Connectivity

Can your app reach Redis?

```bash
# From the app server, test connection
redis-cli -h your-redis-host -p 6379 PING
# Expected: PONG
```

If this fails: firewall, network, wrong host/port, Redis not running. Check `redis-cli` from the same machine as your app. If it works locally but not from the app, it's a network/firewall issue.

### Step 2: Check Memory

Is Redis out of memory?

```bash
redis-cli INFO memory
# used_memory_human: 3.2G
# maxmemory_human: 4G
# mem_fragmentation_ratio: 1.2
```

If `used_memory` is at or near `maxmemory`, Redis is evicting keys. Check `maxmemory-policy`. If you see `OOM command not allowed`, Redis rejected a write because it's full. Increase `maxmemory` or fix your eviction policy.

### Step 3: Check the Slow Log

Is Redis slow? What's taking time?

```bash
redis-cli SLOWLOG GET 10
# Shows the last 10 slow commands with execution time in microseconds
```

If you see `KEYS *` or commands taking 100ms+, that's your culprit. Fix the slow commands. Use `SCAN` instead of `KEYS`. Optimize your data structures.

### Step 4: Check Connected Clients

Who is connected? Are there too many?

```bash
redis-cli CLIENT LIST
# Shows all connected clients with idle time, flags, etc.
```

If you see hundreds of connections from your app, you might not be using connection pooling. Or you have a connection leak. Check `CLIENT KILL` to disconnect idle clients if needed. Fix the leak in your code.

### Step 5: Check Keyspace and Hit Ratio

```bash
redis-cli INFO stats
# keyspace_hits, keyspace_misses
# Hit ratio = hits / (hits + misses)
```

Low hit ratio? You're caching the wrong things or TTLs are too short. Check `INFO keyspace` for key counts per database.

### Step 6: Check Replication (if using Redis Cluster or Sentinel)

```bash
redis-cli INFO replication
# role: master/slave
# connected_slaves
# master_link_status (for slaves)
```

If `master_link_status` is `down`, replication is broken. Check network, credentials, and broker logs.

### Step 7: Check Persistence (if using RDB/AOF)

```bash
redis-cli INFO persistence
# rdb_last_save_time
# aof_enabled
# aof_last_write_status
```

If AOF rewrite is stuck or RDB hasn't saved in days, you might have disk issues or a very large dataset.

### Quick Reference Table

| Symptom | Check | Fix |
|---------|-------|-----|
| Connection refused | Connectivity, firewall | Open port, check Redis is running |
| Timeouts | Slow log, network | Optimize commands, check latency |
| OOM errors | Memory, maxmemory | Set maxmemory, tune eviction |
| Low hit ratio | What you cache, TTL | Cache popular data, increase TTL |
| Too many connections | CLIENT LIST | Use connection pooling |
| Slow commands | SLOWLOG | Replace KEYS with SCAN, optimize |

---

## Exercise

1. Check your Redis `maxmemory` setting. Is it configured?
2. Run `INFO stats` and calculate your cache hit ratio.
3. Write a Java method that falls back to the database when Redis is down.
4. Find a key in your Redis that has no TTL (`TTL key` returns -1). Why is that a problem?

---

**Next up:** [Redis with Spring Boot](redis-with-springboot.md) — Build real applications with Redis.
