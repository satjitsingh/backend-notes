# Caching Patterns

## Why Patterns Matter

There are different **strategies** for how your application talks to the cache and database. Each pattern has a different flow for reads and writes. Choosing the right one depends on your use case: read-heavy vs write-heavy, how fresh your data needs to be, and how much complexity you're willing to handle.

Think of it like choosing a workflow for a team. Cache-Aside is like "everyone manages their own desk." Read-Through is like "the assistant fetches things for you." Write-Through is like "every update goes through the filing cabinet and the desk at the same time." Write-Behind is like "update the desk now, file later." Each has trade-offs.

Let's walk through each pattern with complete stories and real scenarios.

---

## 1. Cache-Aside (Lazy Loading)

The **most common** pattern. The application manages the cache itself. The cache doesn't know about the database. The database doesn't know about the cache. Your app is the conductor.

### How It Works

**READ:**
1. App checks cache first
2. Cache Hit → Return data to the user
3. Cache Miss → Query database → Store result in cache → Return data to the user

**WRITE:**
1. App writes to database
2. App deletes the cache key (invalidate)
3. Next read will be a cache miss and will load fresh data

### Complete Story: Swiggy Restaurant Listing (Step by Step)

Imagine you're building Swiggy. A user opens the app and searches for "restaurants near Connaught Place, Delhi."

**Step 1:** User taps "Search." Your app receives the request. It needs to show a list of restaurants.

**Step 2:** App checks the cache. Key: `restaurants:delhi:connaught_place`. 
```
GET restaurants:delhi:connaught_place
```

**Step 3a (Cache Hit):** Redis returns the list. Your app sends it to the user. Done. Total time: 3 ms.

**Step 3b (Cache Miss):** Redis returns `(nil)`. Your app must fetch from the database.

**Step 4:** App queries the database:
```sql
SELECT * FROM restaurants 
WHERE city = 'Delhi' AND area = 'Connaught Place' 
ORDER BY rating DESC
```

**Step 5:** Database returns 50 restaurants. App stores them in Redis:
```
SETEX restaurants:delhi:connaught_place 600 <json_of_50_restaurants>
```
TTL of 10 minutes (600 seconds). Next user searching the same area will get a cache hit.

**Step 6:** App returns the list to the user. Total time: 45 ms (first user pays the cost).

**Step 7:** A new restaurant opens in Connaught Place. Admin adds it via the dashboard. Your app runs:
```java
restaurantRepository.save(newRestaurant);
redis.del("restaurants:delhi:connaught_place");
```
The cache is invalidated. The next user searching for Connaught Place will get a cache miss and see the new restaurant in the list.

### Diagram

```
READ Flow:
                    ┌─────────┐
           1. GET   │         │  2. Hit? Return
  App ────────────▶ │  Redis  │ ────────────▶ App
   │                │         │
   │                └─────────┘
   │                     │
   │               3. Miss?
   │                     │
   │                     ▼
   │                ┌─────────┐
   │    4. Query    │         │
   └───────────────▶│   DB    │
   ◀────────────────│         │
   │  5. Result     └─────────┘
   │
   └──▶ 6. Store in Redis, return to user
```

### Code Example

```java
public Product getProduct(String id) {
    // Step 1: Check cache
    String cached = redis.get("product:" + id);
    if (cached != null) {
        return deserialize(cached);  // Cache Hit
    }

    // Step 2: Cache Miss - query database
    Product product = database.findById(id);

    // Step 3: Store in cache for next time
    redis.setex("product:" + id, 3600, serialize(product));

    return product;
}

public void updateProduct(Product product) {
    // Step 1: Update database
    database.save(product);

    // Step 2: Invalidate cache
    redis.del("product:" + product.getId());
}
```

### When Things Go Wrong

**Redis is down:** Your app gets an error when calling `redis.get()`. You should catch it and fall back to the database. The app still works—just slower. Design for cache failure.

**You forget to invalidate on write:** User updates a product. You update the database but forget to delete the cache. Users see stale data until TTL expires. Always invalidate on write.

**Cache stampede:** Popular key expires. Thousands of requests hit the database. Use a lock (mutex) so only one request fetches; others wait and then read from the refreshed cache.

### Pros and Cons

| Pros | Cons |
|------|------|
| Only caches data that is actually requested | First request is always slow (miss) |
| Simple to implement | Data can be stale until TTL expires |
| Cache failure doesn't break the app | App must handle cache logic |
| Flexible—you control what to cache | Must remember to invalidate on every write path |
| No wasted cache space on unused data | Cache stampede risk when popular key expires |

!!! tip "Interview Insight"
    Cache-Aside is the **default answer** in interviews. Say: "I would use Cache-Aside with TTL and event-based invalidation on writes."

---

## 2. Read-Through

Similar to Cache-Aside, but the **cache layer** (or a library) handles the database fetch. The application only talks to the cache. The app doesn't know the database exists for reads.

### How It Works

**READ:**
1. App asks cache for data
2. Cache Hit → Return data
3. Cache Miss → Cache layer itself queries the database → Stores result → Returns to app

**WRITE:**
Same as Cache-Aside. App writes to database and invalidates cache. (Or pair with Write-Through.)

### How It Differs from Cache-Aside (Same Swiggy Example)

**Cache-Aside:** Your app code does:
```java
data = redis.get(key);
if (data == null) {
    data = database.query(...);
    redis.setex(key, ttl, data);
}
```

**Read-Through:** Your app code does:
```java
data = cache.get(key);  // That's it. Cache handles the rest.
```

The cache layer (e.g., a library like Spring Cache with a custom CacheLoader) is configured to know how to load data when there's a miss. When you call `cache.get("restaurants:delhi:connaught_place")`, the library checks Redis. On miss, it calls your loader function (which queries the database), stores the result in Redis, and returns it. Your app code is simpler.

### Diagram

```
  App ──────────▶ Cache ──────────▶ DB (on miss)
  App ◀────────── Cache ◀────────── DB

  App never talks to DB directly for reads!
```

### Difference from Cache-Aside

| | Cache-Aside | Read-Through |
|---|------------|--------------|
| Who fetches from DB on miss? | **App** | **Cache layer** |
| App knows about DB? | Yes | No (for reads) |
| Complexity | In app code | In cache library/config |
| Code | More boilerplate | Cleaner, less code |

### When Things Go Wrong

**Cache layer bug:** If the loader has a bug, every cache miss fails. Test the loader thoroughly.

**Cache and DB both down:** Same as Cache-Aside. Design for failure. The cache layer should throw, and your app should have a fallback (e.g., return a default or error page).

### Pros and Cons

| Pros | Cons |
|------|------|
| Cleaner app code (no DB logic in reads) | Cache layer is a single point of logic |
| Same benefits as Cache-Aside for cache hits | Loader bugs affect all cache misses |
| Centralized cache logic | Less control over cache behavior from app |

---

## 3. Write-Through

Every write goes to the cache **and** the database **at the same time**. The cache is always up-to-date. No stale data.

### How It Works

**WRITE:**
1. App writes to cache
2. Cache layer synchronously writes to database
3. Both are updated before the write returns

**READ:**
Cache always has the latest data. Just read from cache. (Or the cache layer loads from DB on miss—Read-Through style.)

### Complete Story: Bank Account Update

Imagine you're building a banking app. A user transfers ₹1000 from their savings to their checking account.

**With Write-Through:**
1. User clicks "Transfer." App calls `updateBalance(accountId, newBalance)`.
2. The cache layer receives the write. It does two things in one transaction (or sequence):
   - Updates Redis: `HSET account:42 balance 50000`
   - Updates database: `UPDATE accounts SET balance = 50000 WHERE id = 42`
3. Only when both succeed does the call return. User sees "Transfer complete."
4. Next read of the account balance comes from Redis. It's correct. No stale data.

**Why this matters for banking:** You cannot show stale balances. A user might make multiple transfers. If the cache had an old balance, they could overdraw. Write-Through ensures the cache and database are always in sync.

### Diagram

```
  App ──▶ Cache ──▶ DB
         (both updated together, synchronously)
```

### Code Example

```java
public void saveProduct(Product product) {
    // Cache library handles both
    cache.put("product:" + product.getId(), product);
    // Internally: cache writes to Redis AND database before returning
}
```

### When Things Go Wrong

**Database write fails:** The cache might have been updated but the database failed. Now they're out of sync. You need transactional semantics or compensating logic. Write-Through is often implemented with "write to DB first, then cache" to avoid this—if DB fails, cache is never updated.

**Slower writes:** Every write hits two systems. Latency increases. For write-heavy workloads, this can be a bottleneck.

### Pros and Cons

| Pros | Cons |
|------|------|
| Cache is always up-to-date | Every write is slower (writes to 2 places) |
| No stale data | Writes data that may never be read (waste) |
| Simple read logic | Higher write latency |
| Good for data that must be fresh | Not ideal for high write volume |
| No invalidation logic needed | Write failure can leave cache and DB out of sync |

---

## 4. Write-Behind (Write-Back)

Similar to Write-Through, but the database write happens **later** (asynchronously). The app gets a fast response. The cache is updated immediately. The database is updated in the background, often in batches.

### How It Works

**WRITE:**
1. App writes to cache
2. Cache acknowledges immediately (fast!)
3. Cache queues the write. A background process writes to the database later (batched).

**READ:**
Cache has the latest data. Reads are fast.

### Complete Story: Analytics / Page View Counting

Imagine you're building an analytics dashboard. Every page view needs to be recorded. You get 10,000 page views per second.

**With Write-Behind:**
1. User loads a page. Your app runs `incrementPageView(pageId)`.
2. The cache layer does: `INCR page:42:views` in Redis. Returns immediately. 0.1 ms.
3. In the background, a worker batches these increments. Every 5 seconds, it flushes: "Page 42: +150 views, Page 43: +200 views..." to the database.
4. The database gets 1 batch write every 5 seconds instead of 10,000 writes per second. It can handle that.

**Why this works for analytics:** If you lose a few page views (e.g., Redis crashes before the batch is flushed), it's acceptable. Analytics is approximate. Speed and database load matter more than perfect accuracy.

### Diagram

```
  App ──▶ Cache ──────▶ DB (later, in background, batched)
          ↑
          Fast response!
```

### When Things Go Wrong

**Redis crashes before flush:** Data in the cache that hasn't been written to the database is lost. Use Write-Behind only when some data loss is acceptable (analytics, logs, non-critical counters).

**Background writer falls behind:** If the database is slow, the queue of pending writes grows. You need monitoring. If the queue gets too large, you might need to backpressure (slow down or reject writes).

!!! warning "Risk"
    If Redis crashes before flushing to the database, **you lose data**. Use Write-Behind only when some data loss is acceptable (e.g., analytics, page views, activity logs).

### Pros and Cons

| Pros | Cons |
|------|------|
| Very fast writes | Data loss risk if cache crashes before DB write |
| Reduces DB load (batches writes) | Complex to implement |
| Good for write-heavy apps | Eventual consistency |
| Database doesn't get hammered | Need to handle failure cases |
| Handles write bursts gracefully | Queue can grow if DB is slow |

---

## 5. Cache Warming (Pre-loading)

Load frequently used data into the cache **before** users request it. Not a read/write pattern—it's a startup or scheduled strategy.

### How It Works

App starts up (or a scheduled job runs) → Load top N products, popular categories, config, etc. into Redis → When users arrive, cache is already warm.

### Code Example

```java
@PostConstruct
public void warmCache() {
    List<Product> popular = database.getTopProducts(100);
    for (Product p : popular) {
        redis.setex("product:" + p.getId(), 3600, serialize(p));
    }
    log.info("Cache warmed with {} products", popular.size());
}
```

### When to Use

- After app restart
- After Redis restart
- Before a known traffic spike (e.g., sale starts at 10 AM, warm at 9:30 AM)
- For data you **know** will be requested often

---

## 6. Refresh-Ahead

Proactively refresh the cache **before** TTL expires. A background thread or scheduler checks keys that are about to expire and refreshes them. Users rarely see a cache miss.

### How It Works

1. Key has TTL of 10 minutes.
2. At 8 minutes (or when TTL < 2 min), a background job triggers.
3. The job fetches fresh data from the database and updates the cache.
4. TTL resets. User requests keep hitting a warm cache.

### Benefits Over Cache-Aside

In Cache-Aside, when TTL expires, the first request pays the cost (cache miss, DB query). With Refresh-Ahead, the refresh happens in the background. Users get fast responses. No spike when popular keys expire.

```java
@Scheduled(fixedRate = 60000)  // Every minute
public void refreshAhead() {
    Set<String> keys = redis.keys("product:*");
    for (String key : keys) {
        long ttl = redis.getExpire(key);
        if (ttl > 0 && ttl < 120) {  // Expires in < 2 min
            String id = key.replace("product:", "");
            Product fresh = database.getProduct(id);
            redis.setex(key, 600, serialize(fresh));
        }
    }
}
```

!!! info "When to use"
    High-traffic product pages, config that changes rarely. Avoids cache stampede. Slightly more complex than Cache-Aside.

---

## 7. Cache Versioning

Put a version number in the cache key. When your schema or data format changes, use a new version. Old keys expire naturally. No big-bang invalidation.

### How It Works

```
product:101:v1   →  Old format (e.g., {name, price})
product:101:v2   →  New format (e.g., {name, price, discount})
```

When you roll out v2, new requests use `product:101:v2`. Old keys `product:101:v1` stay until TTL. No need to delete them. Gradual rollout.

### Useful for Schema Changes

You add a new field to the product model. Old cache entries don't have it. Instead of invalidating everything, bump the version. New cache entries use the new schema.

```java
private static final int CACHE_VERSION = 3;
String key = "product:" + id + ":v" + CACHE_VERSION;
```

!!! tip "Rolling out new versions"
    Deploy code that reads both v2 and v3. Then switch writes to v3. After TTL, v2 keys disappear. Clean.

---

## 8. Multi-Level Caching

Use multiple cache layers. L1 (local, in-process) → L2 (Redis) → L3 (Database). Each level is faster but smaller. L1 avoids network for hot data.

### When to Use

- Very hot keys (e.g., top 100 products).
- High Redis latency (e.g., Redis in another region).
- Reduce Redis load.

### Spring Boot Example

Use Caffeine for L1 (local) and Redis for L2. Check L1 first, then L2, then DB.

```java
// L1: Caffeine (in-memory, per instance)
Cache<String, Product> l1 = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build();

// L2: Redis (shared across instances)
// In your service:
public Product getProduct(String id) {
    Product p = l1.getIfPresent(id);
    if (p != null) return p;
    p = redisTemplate.opsForValue().get("product:" + id);
    if (p != null) {
        l1.put(id, p);
        return p;
    }
    p = database.findById(id);
    redisTemplate.opsForValue().set("product:" + id, p, Duration.ofMinutes(10));
    l1.put(id, p);
    return p;
}
```

L1 is checked first. On miss, L2 (Redis). On miss, database. Write-invalidate must clear both L1 and L2.

---

## Real Production Example: Amazon Product Page

How might Amazon cache a product detail page? It's a mix of patterns.

**Product info (name, description, images):** Cache-Aside. Cached for 15–60 minutes. Invalidated when seller updates. Key: `product:{id}`.

**Price:** Often **not** cached for long. Or cached with short TTL (seconds). Price can change (dynamic pricing, flash deals). Some parts of the page are real-time.

**Reviews:** Cache-Aside. Cached for 5–15 minutes. Key: `product:{id}:reviews`. Invalidated when new review is added (or TTL).

**"Frequently bought together":** Cache-Aside or Refresh-Ahead. Computed from purchase history. Expensive to compute. Cached for hours. Refresh-Ahead keeps it warm.

**"Customers who viewed this":** Similar to reviews. Cached. Eventual consistency is fine.

**Summary:** One page uses multiple cache keys and TTLs. Product info: long TTL. Price: short or real-time. Reviews: medium TTL. Recommendations: long TTL with Refresh-Ahead. That's how big e-commerce sites do it.

---

## What Pattern Does Your Favorite Company Use?

**E-commerce (Amazon, Flipkart):** Mostly **Cache-Aside** for product data. Read-heavy. Products don't change every second. On product update, they invalidate. They use **Cache Warming** before big sales.

**Social media (Instagram, Twitter):** **Cache-Aside** for feeds and profiles. **Write-Behind** for some analytics (like counts, engagement metrics). They need fast writes for counts; eventual consistency is fine.

**Banking / Finance:** **Write-Through** for account balances and transactions. Data must always be fresh. They can't afford stale cache.

**Netflix / Streaming:** **Cache-Aside** + **Cache Warming**. They pre-compute recommendations and popular content. Heavy use of CDN (which is also a cache) for video. Redis for metadata.

**Uber / Ola:** **Cache-Aside** for ride data, driver locations. **Write-Behind** for trip logs and analytics. Real-time location might use a different system (e.g., in-memory stores with pub/sub).

---

## Pattern Comparison (Detailed)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Pattern Comparison                                   │
├─────────────────┬───────────┬───────────┬──────────────────┬───────────────┤
│ Pattern         │ Read Perf │ Write Perf│ Data Freshness    │ Complexity    │
├─────────────────┼───────────┼───────────┼──────────────────┼───────────────┤
│ Cache-Aside     │ Fast*     │ Normal    │ Can be stale      │ Low           │
│ Read-Through    │ Fast*     │ Normal    │ Can be stale      │ Low (in lib)  │
│ Write-Through   │ Fast      │ Slow     │ Always fresh      │ Medium        │
│ Write-Behind    │ Fast      │ Very Fast │ Fresh**           │ High          │
│ Cache Warming   │ Fast      │ N/A      │ Depends on TTL    │ Low           │
└─────────────────┴───────────┴───────────┴──────────────────┴───────────────┘

* After first request (cold miss)
** Risk of data loss if cache crashes before DB write
```

### When to Use Which

| Use Case | Recommended Pattern | Why |
|----------|---------------------|-----|
| General-purpose caching | **Cache-Aside** | Simple, flexible, works for most apps |
| Read-heavy workload | Cache-Aside or Read-Through | Minimize DB reads |
| Write-heavy workload | **Write-Behind** | Batch writes, reduce DB load |
| Data must always be fresh | **Write-Through** | Cache and DB in sync |
| App just started | **Cache Warming** + Cache-Aside | Avoid cold start spike |
| Banking, finance | **Write-Through** | No stale balances |
| Analytics, logs | **Write-Behind** | High write volume, eventual consistency OK |
| Most interview answers | **Cache-Aside** | Safe default, easy to explain |

---

## Quick Summary

| Pattern | Who writes to cache? | DB write timing | Best for |
|---------|---------------------|-----------------|----------|
| Cache-Aside | App | App writes to DB, deletes cache | General use |
| Read-Through | Cache layer | App writes to DB | Cleaner code |
| Write-Through | Cache layer | Synchronous (with cache) | Data consistency |
| Write-Behind | Cache layer | Asynchronous (batched) | Write-heavy apps |
| Cache Warming | App (on startup) | N/A | Avoiding cold starts |

---

## Exercise

1. You're building an e-commerce product page. Which pattern would you use? Why? Walk through one read and one write.

2. You're building a real-time analytics dashboard (lots of writes, few reads). Which pattern fits best? What happens if Redis crashes?

3. Your app just restarted and users are complaining about slow responses. What pattern helps here? How would you implement it?

4. Explain Cache-Aside in 3 sentences (practice for interviews).

5. **Scenario:** You're building a banking app. User transfers money. Should you use Write-Through or Write-Behind? Why?

6. **Scenario:** You're building a social media feed. New posts are written frequently. Reads are 100x more than writes. Which pattern? Why?

---

**Next up:** [Redis with Spring Boot](redis-with-springboot.md) - Write real code!
