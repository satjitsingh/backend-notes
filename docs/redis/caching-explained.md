# Caching Explained

## What is Caching?

**Caching** means keeping a copy of data in a **fast place** so you don't have to fetch it from a **slow place** every time. It's one of the oldest tricks in computing—and one of the most powerful.

When you cache something, you're saying: "This data was expensive to get. I'll keep a copy somewhere quick. Next time someone asks for it, I'll check the quick place first. If it's there, I'll use it. If not, I'll go to the slow place and then save a copy for next time."

That's it. Caching is that simple in concept. The complexity comes from *when* to cache, *what* to cache, and *when* to throw away (invalidate) cached data.

---

## The Restaurant Kitchen Analogy: A Full Story

Let me walk you through a story that will make caching feel intuitive. Imagine you run a busy restaurant.

**The setup:** Your kitchen has two storage areas. **Cold storage** (the walk-in freezer and fridge in the basement) holds all your ingredients. It's the source of truth. Everything is there. But going down to cold storage takes time—maybe 2–3 minutes per trip. You have to walk down the stairs, find the right shelf, bring the ingredients back.

**The problem:** Your most popular dish is Butter Chicken. You make it 50 times a night. If the chef went to cold storage every single time to get chicken, butter, cream, and spices, they'd spend half the night walking up and down stairs. Orders would pile up. Customers would leave.

**The solution:** You have a **prep counter** right next to the stove. At the start of the shift, the chef brings up a batch of chicken, butter, cream, and spices from cold storage and puts them on the prep counter. For the next 20 orders of Butter Chicken, the chef grabs from the counter. Fast. No trips downstairs. Only when the counter runs low does the chef go back to cold storage to restock.

**The mapping:**
- **Cold storage** = your database (MySQL, PostgreSQL). The source of truth. Slow to access.
- **Prep counter** = Redis (your cache). Fast to access. Holds a copy of what you need often.
- **Butter Chicken ingredients** = frequently accessed data (e.g., product catalog, user profile).
- **Restocking** = cache miss. You go to the database, get the data, put it in the cache.

**Cache Hit:** Customer orders Butter Chicken. Chef checks the counter. Ingredients are there. Chef cooks. Done in 2 minutes.

**Cache Miss:** Customer orders Butter Chicken. Counter is empty (maybe the cache expired or was cleared). Chef goes to cold storage, gets ingredients, brings them back, puts some on the counter for next time, cooks. Takes 5 minutes this time—but the next 19 orders will be fast.

**Stale data:** What if the butter in cold storage went bad, but the butter on the counter is still the old batch? The counter has *stale* data. You need a strategy: either restock the counter regularly (TTL—time to live) or restock when you know something changed (event-based invalidation). We'll cover this in detail.

---

## The Speed Difference: A Human-Relatable Analogy

How much faster is Redis than a database? Let me put it in human terms.

**Reading from Redis** is like checking your pocket for your keys. You reach in. You feel them. You're done. A fraction of a second.

**Reading from a database on the same server** is like walking to the next room to get your keys. A few seconds. Still fast, but noticeably slower.

**Reading from a database on another server** is like driving to the next city to get your keys. Tens of seconds to a minute. You feel the delay.

**Calling an external API** is like flying to another country. Minutes. You might as well make coffee while you wait.

When you cache, you're moving the "keys" from the next city (or the next country) into your pocket. The next time you need them, you just reach in. That's why caching matters—it turns slow operations into instant ones.

---

## Life of a Request: With and Without Cache

Let's trace the full journey of a request. Imagine a user visits your product page: `GET /api/products/101`.

### Without Cache

```
1. User's browser sends request to your server
2. Your server receives the request
3. Server opens a connection to the database
4. Server sends SQL: SELECT * FROM products WHERE id = 101
5. Database reads from disk (or SSD), finds the row
6. Database sends the result back over the network
7. Server receives the result
8. Server formats the response (JSON)
9. Server sends response back to the user

Total time: ~20–50 ms (typical)
```

Every single request does this. 1000 users viewing the same product = 1000 database queries. All for the same data.

### With Cache

**First request (cache miss):**
```
1. User sends request
2. Server checks Redis: GET product:101
3. Redis returns (nil) — not in cache
4. Server queries database (same as before)
5. Database returns the product
6. Server stores in Redis: SET product:101 <json> EX 3600
7. Server sends response to user

Total time: ~25–55 ms (slightly slower than no cache, because we also wrote to Redis)
```

**Second request (cache hit):**
```
1. User sends request
2. Server checks Redis: GET product:101
3. Redis returns the cached product (from RAM)
4. Server sends response to user

Total time: ~2–5 ms
```

No database. No disk. No network hop to the database. Just Redis and your app. That's a 10x improvement in latency. And for the database, 999 fewer queries. That's the power of caching.

---

## Why Do We Need Caching?

### The Speed Problem

| Operation | Time | What It Feels Like |
|-----------|------|---------------------|
| Read from RAM (Redis) | ~0.1 ms | Instant |
| Read from SSD (Database) | ~1–10 ms | Quick |
| Network call to another service | ~50–200 ms | Noticeable delay |
| API call to external service | ~200–1000 ms | Slow |

Redis is **100x to 1000x faster** than a typical database query. When you're serving thousands of requests per second, that difference is the difference between a snappy app and a sluggish one.

### The Load Problem

**Without caching:**
```
1000 users request "/api/products"
= 1000 database queries (same data!)
= Database CPU spikes
= Connection pool exhausted
= Timeouts, errors, unhappy users 😩
```

**With caching:**
```
1st request  → Cache Miss → Query DB → Store in Redis → Return
2nd request  → Cache Hit  → Return from Redis
3rd request  → Cache Hit  → Return from Redis
...
1000th request → Cache Hit → Return from Redis
= Only 1 database query! 🎉
```

The database breathes. Your app can handle 10x more traffic with the same hardware.

---

## Cache Hit vs Cache Miss

| Term | Meaning | Restaurant Analogy |
|------|---------|---------------------|
| **Cache Hit** | Data found in cache | Ingredients on the prep counter |
| **Cache Miss** | Data NOT in cache, must fetch from DB | Counter empty, trip to cold storage |
| **Hit Ratio** | % of requests served from cache | Higher is better. Aim for 80%+ |

```
Hit Ratio = Cache Hits / (Cache Hits + Cache Misses) × 100

Example: 800 hits, 200 misses
Hit Ratio = 800 / 1000 × 100 = 80% ✅
```

!!! tip "Interview Insight"
    A good cache hit ratio is **80–95%**. If it's below 50%, your caching strategy needs improvement. You might be caching the wrong things, or your TTL might be too short.

---

## What to Cache?

### Good Candidates for Caching

| Data Type | Why Cache It? |
|-----------|--------------|
| Product catalog | Rarely changes, read very often |
| User profile | Read on every page load |
| API responses | Same response for many users |
| Session data | Accessed on every request |
| Configuration | Rarely changes |
| Search results | Common searches repeat often |

### Bad Candidates for Caching

| Data Type | Why NOT Cache? |
|-----------|---------------|
| Constantly changing data | Cache becomes stale instantly |
| One-time queries | No reuse benefit |
| Sensitive data (credit cards) | Security risk |
| Very large datasets | Wastes expensive RAM |

---

## Cache Invalidation: The Hardest Problem

> "There are only two hard things in Computer Science: cache invalidation and naming things."  
> — Phil Karlton

### What is it?

When the original data changes, the cached copy becomes **stale** (outdated). You need to remove or update it. That's cache invalidation.

### Why is it hard?

**1. You need to know *when* data changed.** Did the database get updated by your app? By a background job? By another service? By a direct SQL run by a DBA? You need triggers or application logic to know when to invalidate.

**2. You need to know *which* cache keys to invalidate.** A product update might invalidate `product:101`, but also `product_list:category_electronics` (if that list includes product 101). Cache keys can be complex. You might have many keys that depend on one piece of data.

**3. In distributed systems, **multiple servers** might have different cache states.** Server A invalidates a key. Server B and C might still have the old value in their local cache (if they use one). Or Redis is shared, but there's a race: Server A deletes the key, Server B (with a stale in-memory copy) writes it back before Server C fetches fresh data. Consistency is tricky.

### Strategy 1: Time-Based Expiry (TTL)

```bash
SETEX "product:101" 3600 "{name: Laptop, price: 50000}"
# Expires in 1 hour automatically
```

**How it works:** You set a TTL when you cache. After that time, Redis automatically deletes the key. The next request will be a cache miss and will fetch fresh data from the database.

**Pros:** Simple. No application logic needed. Eventually consistent.

**Cons:** Data might be stale for up to TTL duration. If you set TTL to 1 hour, a price change could take up to 1 hour to appear.

**Real-world scenario:** Product catalog. A product's price rarely changes. If it does, waiting 5–15 minutes is acceptable. Set TTL to 600–900 seconds.

### Strategy 2: Event-Based Invalidation

```
User updates profile → App deletes cache → Next request fetches fresh data
```

```bash
# When user updates their profile:
DEL "user:profile:42"
```

**How it works:** When you *know* the data changed (e.g., user clicked "Save"), you explicitly delete the cache key. The next request will be a cache miss and will load fresh data from the database.

**Pros:** Accurate. Cache is always fresh after a known change.

**Cons:** You must remember to invalidate everywhere the data is written. Easy to miss a code path.

**Real-world scenario:** User profile. When the user updates their name or email, you must invalidate immediately. Users expect to see their changes right away.

### Strategy 3: Combination (Best Practice)

Use **TTL as a safety net** + **Event-based** for known changes.

```java
// On read: cache with TTL
redis.setex("user:42", 3600, userJson);

// On update: invalidate immediately
userRepository.save(user);
redis.del("user:42");
```

TTL ensures that even if you forget to invalidate somewhere, the cache will eventually refresh. Event-based invalidation ensures that known changes are reflected immediately.

---

## Cache Invalidation: 3 Detailed Real-World Scenarios

### Scenario 1: E-commerce Product Update

**Context:** A product's price is updated by an admin. The product is cached under `product:101` and also appears in `category:electronics:page_1` (a list of products).

**What to invalidate:** 
- `product:101` — the product itself
- `category:electronics:page_1` — the listing that includes this product (or all category pages if you cache them)

**How:** When the admin saves the product, your backend runs:
```java
productRepository.save(product);
redis.del("product:" + product.getId());
redis.del("category:" + product.getCategoryId() + ":page_*");  // or use a pattern delete
```

**Mistake to avoid:** Only invalidating `product:101` but not the category listing. Users browsing the category would still see the old price until the category cache expires.

### Scenario 2: User Updates Their Email

**Context:** User changes their email in the settings page. The email is stored in the user profile, which is cached for session and display purposes.

**What to invalidate:** 
- `user:profile:42` — the full profile cache
- Any session or token that might include the email (e.g., JWT payloads are usually not cached, but session objects might be)

**How:** 
```java
userRepository.updateEmail(userId, newEmail);
redis.del("user:profile:" + userId);
// If you cache sessions with user data: find and invalidate those too
```

**Mistake to avoid:** Only updating the database and forgetting to invalidate the cache. The user might see their old email on the next page load until the cache expires.

### Scenario 3: Config Change Across Multiple Servers

**Context:** You change a feature flag. The flag is cached on 10 app servers. Each server has its own in-memory cache (or a shared Redis).

**What to invalidate:** 
- If using shared Redis: `config:feature_dark_mode`
- If using local cache per server: you need a broadcast mechanism (e.g., Redis Pub/Sub) to tell all servers to invalidate their local cache

**How (shared Redis):** 
```java
configRepository.update("feature_dark_mode", true);
redis.del("config:feature_dark_mode");
```

**How (local cache + Redis Pub/Sub):** 
```java
configRepository.update("feature_dark_mode", true);
redis.publish("config:invalidate", "feature_dark_mode");
// Each server subscribes and invalidates its local cache when it receives the message
```

---

## TTL - Time To Live

TTL is like setting a **timer on food**. After the timer runs out, the food (data) is thrown away.

```bash
SET weather "sunny"
EXPIRE weather 300              # Expires in 5 minutes

TTL weather                     # Seconds remaining
# (integer) 297

# After 5 minutes:
GET weather
# (nil)  ← Automatically deleted
```

### Choosing TTL Values

| Data Type | Suggested TTL | Why |
|-----------|--------------|-----|
| Product listing | 5–15 minutes | Changes occasionally |
| User session | 30 minutes | Standard session timeout |
| OTP | 5 minutes | Security requirement |
| API rate limit | 1 minute | Reset every minute |
| Static config | 1–24 hours | Rarely changes |
| Real-time stock price | 5–10 seconds | Changes frequently |

---

## Caching in the Real World: How Big Companies Use It

**Netflix:** Caches movie metadata, thumbnails, and recommendations. When you browse, most of what you see comes from cache. They use a multi-tier cache: CDN for static assets, Redis (or similar) for dynamic data. They also cache "what's popular in your region" so that computation is done once and served many times.

**Amazon:** Product pages are heavily cached. The product title, price, description, and reviews are cached. When you change a product, they invalidate the cache for that product. They use a combination of TTL and event-based invalidation. Their "customers who bought this also bought" section is pre-computed and cached.

**Flipkart:** During Big Billion Days, they rely heavily on caching. Product listings, prices, and availability are cached. They use cache warming—before the sale starts, they pre-load popular products into the cache. They also use aggressive TTL for prices (short, to handle flash deals) and longer TTL for static product info.

**Common pattern:** Cache the read path. Invalidate on the write path. Use TTL as a safety net. Pre-warm the cache for known traffic spikes.

---

## Cache Stampede: A Detailed Story

### What is it?

When a popular cache key expires, **thousands of requests** hit the database at the same time. The database gets overwhelmed. It might crash. Or slow down so much that everyone times out.

### The Black Friday Sale Scenario

Imagine you're running an e-commerce site. Product "iPhone 15" is cached with a 1-hour TTL. At 10:00 AM, 10,000 users have the product page open. The cache was populated at 9:00 AM. At 10:00 AM, the key expires.

**10:00:00.001** — Cache key `product:iphone15` expires. Redis deletes it.

**10:00:00.002** — User 1 clicks "Refresh". Cache miss. App starts a database query.

**10:00:00.003** — User 2 clicks. Cache miss. App starts a database query.

**10:00:00.003** — User 3 to User 10,000 click or their pages auto-refresh. All cache miss. All 10,000 requests trigger a database query for the same product.

**10:00:00.500** — Database has 10,000 concurrent queries for the same row. Connection pool exhausted. CPU at 100%. Database starts timing out.

**10:00:00.501** — App servers can't get a response. They time out. Users see errors. "Site is down."

That's a cache stampede. One expired key. One product. Thousands of requests. Database dies.

### Solutions

**1. Lock (Mutex)**

Only ONE request fetches from the database. Others wait.

```
Request 1: Cache miss → Acquires lock → Fetches from DB → Updates cache → Releases lock
Request 2: Cache miss → Lock taken → Waits → Eventually gets data from cache (after Request 1 fills it)
Request 3: Cache miss → Lock taken → Waits → Gets data from cache
...
Request 10000: Waits → Gets data from cache
```

**2. Early Refresh**

Refresh the cache **before** it expires. So it never actually expires.

```
TTL = 300 seconds
At 240 seconds (80% of TTL), a background job or the first request refreshes the cache
Cache never expires. No stampede.
```

**3. Stale-While-Revalidate**

Serve the old (stale) data immediately while refreshing in the background.

```
Request hits expired key. Instead of going to DB:
1. Return the stale value from cache (if you kept it) — or serve from DB
2. Trigger an async refresh in the background
3. Next request gets fresh data
```

---

## Cache Penetration

### What is it?

Requests for data that **doesn't exist** in the cache or the database. Every request is a cache miss. Every request hits the database. The cache never helps.

### Example

An attacker (or a bug) sends repeated requests: `GET /user/999999999`. User ID 999999999 doesn't exist. Your app does:

1. Check cache: `GET user:999999999` → (nil)
2. Query database: `SELECT * FROM users WHERE id = 999999999` → No rows
3. Return 404

The attacker sends 10,000 requests per second. Every single one hits the database. Cache is useless. Database is overloaded.

### Solutions

**1. Cache null values**

When the database returns "not found", cache that result with a short TTL.

```java
User user = userRepository.findById(id);
if (user == null) {
    redis.setex("user:null:" + id, 60, "null");  // Cache "not found" for 1 minute
    return null;
}
```

Now the next 10,000 requests for the same non-existent ID will hit the cache instead of the database.

**2. Bloom filter**

A Bloom filter is a probabilistic structure that can tell you "this key definitely doesn't exist" or "this key might exist." Before querying the database, check the Bloom filter. If it says "definitely doesn't exist," return 404 without hitting the database. If it says "might exist," then proceed to cache/database.

---

## Cache Avalanche

### What is it?

Many cache keys expire at the **same time**, causing a flood of database queries. Unlike a stampede (one key), an avalanche is many keys expiring together.

### Example

Your app starts at 9:00 AM. You cache 1000 products with TTL of 1 hour. All keys expire at 10:00 AM. At 10:00 AM, users request products. All 1000 keys are cache miss. The database gets 1000 different queries at once. Load spikes.

### Solutions

**1. Add random jitter to TTL**

Instead of `TTL = 3600` for everyone, use `TTL = 3600 + random(0, 300)`.

```java
int ttl = 3600 + (int)(Math.random() * 300);  // 3600 to 3900 seconds
redis.setex(key, ttl, value);
```

Now keys expire at different times. The load is spread over 5 minutes instead of 1 second.

**2. Stagger expiry times**

Cache different data at different times. Don't cache everything at startup with the same TTL.

**3. Use cache warming**

After a restart, pre-load the cache gradually. Don't load 1000 keys at once. Load in batches with small delays.

---

## Cold Cache vs Warm Cache

| Term | Meaning | When It Happens |
|------|---------|----------------|
| **Cold Cache** | Cache is empty | App just started, Redis restarted |
| **Warm Cache** | Cache has data | App running normally |
| **Cache Warming** | Pre-loading data into cache on startup | Prevents cold start problems |

### When Cold Cache Matters

**Scenario:** Your app restarts at 2 AM for a deployment. Redis restarts too (or it's a new instance). Cache is empty. At 9 AM, users start arriving. Every request is a cache miss. The database gets hammered. Response times spike from 5 ms to 50 ms. Users complain about slowness.

**Solution: Cache warming.** When the app starts, load the most frequently accessed data into the cache before users arrive.

```java
@PostConstruct
public void warmCache() {
    List<Product> topProducts = database.getTopProducts(100);
    for (Product p : topProducts) {
        redis.setex("product:" + p.getId(), 3600, serialize(p));
    }
    log.info("Cache warmed with {} products", topProducts.size());
}
```

Now when users arrive, the top 100 products are already in the cache. The first requests are cache hits instead of misses.

### Real Scenario: E-commerce Sale

**Without warming:** Sale starts at 10 AM. Cache is cold. First 10,000 users all get cache miss. Database gets 10,000 queries in the first second. Database struggles. Some requests time out.

**With warming:** At 9:30 AM, a job runs. It loads the top 500 sale products into the cache. At 10 AM, the first 10,000 users mostly hit the cache. Database is fine. Sale goes smoothly.

---

## Quick Summary

| Concept | Explanation |
|---------|-------------|
| Cache | Fast copy of slow data |
| Cache Hit | Data found in cache |
| Cache Miss | Data not in cache, fetch from DB |
| TTL | Auto-expire cached data |
| Invalidation | Removing/updating stale cache |
| Stampede | Many requests hit DB when one key expires |
| Penetration | Requests for non-existent data hit DB every time |
| Avalanche | Many keys expire at once, flood of DB queries |
| Cold Cache | Empty cache (app just started) |
| Warm Cache | Cache has data (app running normally) |

---

## Exercise

1. You have an API `/api/products` that gets 10,000 requests/minute. The product list updates once per hour. What TTL would you set? Why?

2. A user updates their email. Which invalidation strategy would you use? Why?

3. Your app just restarted. How would you handle the cold cache problem? Write a simple cache warming function in pseudocode.

4. Calculate the hit ratio: 4500 hits, 500 misses. Is that good?

5. **Scenario:** An attacker is sending 1000 requests/second for `GET /user/999999` (user doesn't exist). How would you protect your database? Describe the solution.

6. **Scenario:** You cache 500 products with TTL = 1 hour. All keys were set at the same time. What problem might occur at the 1-hour mark? How would you prevent it?

---

**Next up:** [Caching Patterns](caching-patterns.md) - Learn the standard caching strategies used in production.
