# Chapter 31: Caching Strategies

[← Back to Index](00-README.md) | [Previous: JWT Auth](30-JWT-Auth.md) | [Next: Logging & Monitoring →](32-Logging-Monitoring.md)

---

## Why This Chapter Matters

Here's a fact that changes how you think about performance: **the fastest database query is the one you never make.**

If 1,000 users request the same product page, and each request hits the database, that's 1,000 identical database queries. With caching, the first request hits the database, and the next 999 get an instant answer from memory. Your database barely notices.

Caching is the **#1 performance optimization** in every production system. Netflix, Amazon, Twitter, Google — they all cache aggressively. Without caching, they'd need 10-100x more servers.

**Without caching:**
- Every request hits the database (slow, expensive)
- Your application collapses under moderate load
- You waste money on database servers doing the same work repeatedly
- Users experience slow page loads and leave (53% leave if page takes >3 seconds)

**With proper caching:**
- 10-100x faster response times (milliseconds instead of seconds)
- Reduced database load by 80-90%
- Better user experience (instant responses feel magical)
- Lower infrastructure costs (fewer database servers needed)
- Your system handles 10x more concurrent users on the same hardware

**But here's the catch:** Caching introduces one of the hardest problems in computer science — **cache invalidation** (knowing when cached data is outdated). This chapter teaches you how to handle it.

---

## Layer 1 — Intuition Builder

### Part 1: Why Cache? — A Real-Life Story

Imagine you're a librarian. A student asks: "What year was the Declaration of Independence signed?" You walk to the history section, find the book, look up the answer: **1776**. You walk back and tell the student.

Five minutes later, ANOTHER student asks the same question. Do you walk all the way back to the history section? No! You remember the answer: **1776**. You tell them instantly.

That's caching. **Your memory is the cache. The library shelves are the database.** Cache is just a fancy word for "a notepad of recent answers" — a fast place to store things you might need again soon.

Now imagine 100 students ask the same question in one hour. Without your memory (cache), you'd walk to the history section 100 times. With your memory, you walk once and answer 99 others from memory.

**In code terms:**
```
WITHOUT CACHING (walk to library every time):
User Request → Database Query (10ms) → Response
User Request → Database Query (10ms) → Response  ← Same data!
User Request → Database Query (10ms) → Response  ← Same data again!
1000 requests = 1000 database queries = 10,000ms total

WITH CACHING (remember the answer):
User Request → Cache Check (0.1ms) → Response ✅  (from memory)
User Request → Cache Check (0.1ms) → Response ✅  (from memory)
User Request → Cache Check (0.1ms) → Response ✅  (from memory)
1000 requests = 1 database query + 999 cache hits = 109.9ms total
That's ~100x faster!
```

**But what if the data changes?** What if the library gets a new edition of the book with a correction? Your memory (cache) has the old answer. This is the **cache invalidation problem** — and it's the hardest part of caching.

---

### Part 2: Cache Vocabulary for Beginners

Before going deeper, let's define terms you'll see everywhere. Don't worry — each one has a simple everyday meaning:

| Term | What It Means (Plain English) | Analogy |
|------|-------------------------------|---------|
| **Cache** | Fast temporary storage — like a notepad | Your desk — quick to grab things from |
| **Cache Hit** | You found the data in the cache! | Book is on your desk, instant access |
| **Cache Miss** | Data NOT in cache — you must go get it | Book not on desk, walk to library |
| **TTL (Time-To-Live)** | How long something stays in the cache before it "expires" | "This note is valid for 1 hour" |
| **Eviction** | Kicking old data out of the cache to make room | Clearing your desk when it's too full |
| **Cache Invalidation** | Saying "this cached data is wrong/old now" | "The library got a new edition, my note is wrong" |
| **Cache Warming** | Putting popular data in the cache BEFORE anyone asks | Putting the most popular books on your desk before class starts |
| **Stale Data** | Old, outdated data still sitting in the cache | An old phone number in your contacts |

---

### Part 3: Cache Hit vs Cache Miss — The Librarian's Cheat Sheet

Think of the cache as the librarian's **cheat sheet** — a small piece of paper with answers to questions she gets all the time.

**Cache Hit** = The answer is on the cheat sheet! ✅  
She glances at it, tells the student instantly. No walking to the bookshelves.

**Cache Miss** = The answer is NOT on the cheat sheet. ❌  
She has to walk to the books, find the book, look it up, write the answer on her cheat sheet for next time, then tell the student.

```
┌─────────────────────────────────────────────────┐
│              CACHE OPERATION FLOW                │
├─────────────────────────────────────────────────┤
│                                                 │
│  Request → Check Cache                          │
│     │                                            │
│     ├─→ Cache Hit (data found)                 │
│     │   └─→ Return cached data ✅               │
│     │                                            │
│     └─→ Cache Miss (data not found)            │
│         ├─→ Query Database                      │
│         ├─→ Store in Cache                      │
│         └─→ Return data ✅                      │
│                                                 │
└─────────────────────────────────────────────────┘
```

**Cache Hit Rate** tells you how good your cheat sheet is:
- **Good:** 80-95% of requests find the answer on the cheat sheet
- **Excellent:** 95-99% — your cache is doing great!
- **Poor:** Less than 50% — maybe you're caching the wrong things

---

### Part 4: How Fast Is Cache? (The Speed Ladder)

Different storage is like a ladder of speed. The top is fastest. Cache lives near the top.

```
┌─────────────────────────────────────────────────┐
│              ACCESS TIME COMPARISON              │
├─────────────────────────────────────────────────┤
│  CPU L1 Cache:     0.5 ns  (fastest)           │
│  CPU L2 Cache:     7 ns                          │
│  RAM (Cache):      100 ns                        │
│  SSD:              150,000 ns (150 μs)          │
│  Database (local): 1,000,000 ns (1 ms)          │
│  Database (remote): 10,000,000 ns (10 ms)      │
│  Network API:      100,000,000 ns (100 ms)      │
└─────────────────────────────────────────────────┘
```

**The magic:**  
- Database query: **10 milliseconds** (feels slow)  
- Cache lookup: **0.1 milliseconds** (feels instant)  
- **That's 100x faster!** Like the difference between walking across town vs. reaching into your pocket.

---

### Part 5: The Three Caching Strategies — When to Use Which

Imagine three different ways the librarian could use her cheat sheet:

**1. Cache-Aside (Lazy Loading) — "Check My Notes First"**

She checks her notes first. If the answer isn't there, she goes to the library, finds it, writes it in her notes for next time. When a book gets updated, she crosses that answer off her notes so she'll look it up fresh next time.

```
Read: Check cache → if miss → Database → put in cache → return
Write: Database → delete from cache (so next read fetches fresh data)
```

**Use when:** Most situations. This is your default choice. Read-heavy apps (people read way more than they write).

---

**2. Write-Through — "Always Keep My Notes Updated"**

Every time a book is updated in the library, she also updates her cheat sheet at the same time. Her notes always match the library exactly.

```
Write: Update cache AND database (simultaneously)
Read: Read from cache (always has fresh data)
```

**Use when:** You need the cache and database to always match. Strong consistency.

---

**3. Write-Behind (Write-Back) — "Jot It Down Fast, File It Later"**

She writes updates on sticky notes immediately (super fast!). Later, when she has time, she files those updates into the library.

```
Write: Write to cache (instant!) → Background job → Database (later)
Read: Read from cache (always fast)
```

**Use when:** You need extremely fast writes. Can tolerate the database being updated a bit later.

---

### Part 6: The Two Hard Problems (And Why Stale Data Hurts)

There's a famous joke in computer science:

> "There are only two hard things in Computer Science: **cache invalidation** and **naming things**."
> — Phil Karlton

**Why cache invalidation is so hard:**
- **When do you update the cache?** Too soon = wasted effort. Too late = stale (old) data.
- **What if data changes in the database?** The cache doesn't know automatically.
- **How do you keep multiple caches in sync?** If you have 5 servers, each with its own cache...
- **What if the cache has stale data?** Users see wrong information.

**Real-World Example of Stale Data:**  
Imagine an e-commerce site. Product price is cached as $99. The admin changes the price to $79 in the database. But the cache still says $99. Customers see $99, add to cart, but get charged $79. The receipt doesn't match what they saw. That's a stale cache problem.

---

## Layer 2 — Professional Developer

### Spring Cache Abstraction — Why It Exists

Spring provides a **cache abstraction** — think of it as a universal remote control for caches. You write your code once using Spring's annotations, and you can switch from an in-memory cache to Redis (or another provider) by changing configuration. No code changes.

**Why this matters:** Your app might start with a simple in-memory cache for development, then move to Redis for production. The abstraction lets you do that without rewriting caching logic.

### Cache Annotations — What Each One Does

| Annotation | Purpose | When Method Runs |
|------------|---------|------------------|
| `@Cacheable` | Store the result so we don't have to compute it again | Only on cache MISS; result is cached |
| `@CachePut` | Always update the cache with the new result | Always; method always runs |
| `@CacheEvict` | Remove something from the cache | Before or after method runs |
| `@Caching` | Combine multiple cache operations | Groups other annotations |
| `@CacheConfig` | Shared settings for a class | Configuration only |

### Basic Setup

**Step 1: Enable caching in your application**

```java
@SpringBootApplication
@EnableCaching  // ← Turns on Spring's cache system — required!
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Step 2: Use caching in your service**

```java
@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    // @Cacheable: "Check cache first. If not there, run method and cache result."
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        // This line only runs on CACHE MISS
        System.out.println("Fetching user from database: " + id);
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    // @CachePut: "Always run method, and put result in cache (update cache)"
    @CachePut(value = "users", key = "#user.id")
    public User update(User user) {
        System.out.println("Updating user: " + user.getId());
        return userRepository.save(user);
    }
    
    // @CacheEvict: "Remove this entry from cache when method runs"
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        System.out.println("Deleting user: " + id);
        userRepository.deleteById(id);
    }
    
    // @CacheEvict with allEntries: "Clear the entire users cache"
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllUsersCache() {
        System.out.println("Clearing all users cache");
    }
}
```

**Step 3: Call it from a controller**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        // First call: hits database, caches result
        // Second call: returns from cache (no database query!)
        return userService.findById(id);
    }
}
```

### Cache Key Strategies — Why Keys Matter

The cache key is like a label on a filing cabinet drawer. It must be unique for each piece of data, or you'll overwrite or mix up cached items.

**Simple key (single parameter):**
```java
@Cacheable(value = "users", key = "#id")
public User findById(Long id) { }
```

**Composite key (multiple parameters):**
```java
@Cacheable(value = "orders", key = "#userId + '_' + #orderId")
public Order findOrder(Long userId, Long orderId) { }
```

**Using method result:**
```java
@CachePut(value = "users", key = "#result.id")
public User save(User user) {
    return userRepository.save(user);  // result.id = saved user's ID
}
```

**Custom key generator (for complex logic):**
```java
@Cacheable(value = "users", keyGenerator = "customKeyGenerator")
public User findById(Long id) { }

@Bean
public KeyGenerator customKeyGenerator() {
    return (target, method, params) -> {
        // Build a key from method name + all parameters
        return method.getName() + "_" + Arrays.toString(params);
    };
}
```

### Conditional Caching — Cache Only When It Makes Sense

**Cache only if condition is true (condition):**
```java
@Cacheable(value = "users", condition = "#id > 10")
public User findById(Long id) {
    // Only cache if id > 10 (e.g., don't cache admin/system users)
    return userRepository.findById(id).orElseThrow();
}

@Cacheable(value = "users", condition = "#id != null")
public User findById(Long id) {
    // Only cache when id is not null
    return userRepository.findById(id).orElseThrow();
}
```

**Don't cache if result matches condition (unless):**
```java
@Cacheable(value = "users", unless = "#result == null")
public User findById(Long id) {
    // Cache unless we got null (don't cache "not found" as a result)
    return userRepository.findById(id).orElse(null);
}

@Cacheable(value = "users", unless = "#result.status == 'INACTIVE'")
public User findById(Long id) {
    // Don't cache inactive users — they might get reactivated
    return userRepository.findById(id).orElseThrow();
}
```

### Multiple Cache Operations with @Caching

When one update affects multiple caches (e.g., a user list and a user detail cache):

```java
@Caching(
    evict = {
        @CacheEvict(value = "users", key = "#user.id"),
        @CacheEvict(value = "userList", allEntries = true)  // Clear list cache
    },
    put = {
        @CachePut(value = "users", key = "#user.id")  // Update detail cache
    }
)
public User updateUser(User user) {
    // After save: evict from userList, update users cache with new data
    return userRepository.save(user);
}
```

### Cache Providers — Choosing Where Your Cache Lives

| Provider | Type | Best For | Limitation |
|----------|------|----------|------------|
| **ConcurrentMapCacheManager** | In-memory (default) | Development, single app | Lost on restart; not shared across servers |
| **Caffeine** | In-memory (high-perf) | Single instance, speed | Same as above |
| **Redis** | Distributed | Multiple servers, production | Requires Redis server; network latency |
| **EhCache** | In-memory / disk | Rich features, XML config | More setup |

---

**1. ConcurrentMapCacheManager (Default — No Config Needed)**

```java
// Spring Boot uses this by default when you add @EnableCaching
// Good for: Development, single-instance apps
// Bad for: Distributed systems, production scale (cache is lost on restart)
```

---

**2. Caffeine (High-Performance In-Memory)**

Add dependency:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

Configuration:
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)  // TTL: entries expire after 10 min
            .maximumSize(1000)                       // Max 1000 entries per cache
            .recordStats());                         // Enable hit/miss statistics
        return cacheManager;
    }
}
```

---

**3. Redis (Distributed Cache)**

Add dependency:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

`application.yml`:
```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

Configuration:
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))  // Default TTL: 10 minutes
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))  // Store as JSON
            .disableCachingNullValues();  // Don't cache null (saves space, avoids bugs)
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("users", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1)))   // Users: 1 hour TTL
            .withCacheConfiguration("orders",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5)))  // Orders: 5 min TTL
            .transactionAware()  // Respect DB transactions
            .build();
    }
}
```

---

**4. EhCache (Feature-Rich, XML Config)**

Add dependency:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
</dependency>
```

Configuration:
```java
@Configuration
@EnableCaching
public class EhcacheConfig {
    
    @Bean
    public JCacheCacheManager cacheManager() {
        return new JCacheCacheManager(
            Caching.getCachingProvider().getCacheManager(
                getClass().getResource("/ehcache.xml").toURI(),
                getClass().getClassLoader()
            )
        );
    }
}
```

### Cache Best Practices

**✅ Do:**
- Set appropriate TTLs (don't let cache grow forever)
- Use `disableCachingNullValues()` to avoid caching "not found"
- Use different TTLs for different cache types (users vs. orders)
- Prefix cache names (e.g., `app:users`) for easier debugging
- Use `transactionAware()` when using DB transactions

**❌ Don't:**
- Use `entryTtl(Duration.ZERO)` — cache never expires, memory grows forever
- Cache null values — wastes space and can cause bugs
- Use the same TTL for everything — tune per data type

### Programmatic Cache Access — When You Need Direct Control

Sometimes you need to clear or inspect the cache without going through a service method:

```java
@Service
public class CacheService {
    
    private final CacheManager cacheManager;
    
    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();  // Remove all entries
        }
    }
    
    public void evictKey(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);  // Remove single entry
        }
    }
    
    public <T> T get(String cacheName, Object key, Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
        }
        return null;
    }
}
```

### Testing Cached Methods

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Test
    void testCacheable() {
        // First call - should hit database
        User user1 = userService.findById(1L);
        
        // Second call - should come from cache (no DB)
        User user2 = userService.findById(1L);
        
        assertThat(user2).isEqualTo(user1);
        
        // Verify cache was populated
        Cache cache = cacheManager.getCache("users");
        assertThat(cache.get(1L)).isNotNull();
    }
    
    @Test
    void testCacheEvict() {
        userService.findById(1L);   // Populate cache
        userService.delete(1L);    // Should evict
        
        Cache cache = cacheManager.getCache("users");
        assertThat(cache.get(1L)).isNull();
    }
}
```

### Hibernate Caching — Three Levels

When using Hibernate/JPA, there are **three** cache levels. Spring Cache is application-level; Hibernate has its own built-in caches.

| Level | Scope | Lifecycle | What It Caches |
|-------|-------|-----------|----------------|
| **First-Level** | Single session/transaction | Request/session | Entities loaded in that session |
| **Second-Level** | Entire application | Until eviction | Entities shared across sessions |
| **Query Cache** | Application | Until invalidation | Results of specific queries |

**First-Level Cache (Session Cache):**
- **What:** Hibernate keeps entities in memory during a single `EntityManager` / `Session`.
- **Why:** If you load User #1 twice in the same transaction, Hibernate returns the same object — no second DB hit.
- **Scope:** Request or transaction only. Cleared when session closes.
- **No configuration:** Always on.

**Second-Level Cache:**
- **What:** Shared cache across all sessions in the application.
- **Why:** User #1 loaded by Request A can be served from cache when Request B asks for it.
- **Requires:** A provider (EhCache, Infinispan, etc.) and `@Cache` on entities.

```java
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    @Id
    private Long id;
    // ...
}
```

**Query Cache:**
- **What:** Caches the *result set* of specific queries (e.g., "all products in category X").
- **Why:** Repeated identical queries return cached IDs/rows.
- **Caution:** Deprecated in Hibernate 6; consider Spring Cache or second-level cache instead.

---

## Layer 3 — Advanced Engineering Depth

### Cache Patterns — Deep Dive

**1. Cache-Aside (Lazy Loading) — Most Common**

```
Read:  Check cache → If hit, return → If miss, load from DB → store in cache → return
Write: Write to DB → Invalidate cache
```

**Pros:** Simple, cache only holds requested data, works with any provider.  
**Cons:** Cache miss penalty, possible race conditions, stale data risk.

**2. Write-Through — Strong Consistency**

```
Write: Write to cache AND database (both updated together)
Read:  Read from cache (always fresh)
```

**Pros:** Cache always matches DB.  
**Cons:** Slower writes, writes to DB even for rarely-read data.

**3. Write-Behind (Write-Back) — Maximum Write Speed**

```
Write: Write to cache (instant) → Queue DB write (async)
Read:  Read from cache
```

**Pros:** Very fast writes, batched DB updates.  
**Cons:** Data loss risk if cache fails before DB write, eventual consistency.

### Cache Invalidation Strategies

| Strategy | How It Works | Pros | Cons |
|----------|--------------|------|------|
| **TTL** | Entries expire after X minutes | Simple, automatic | Stale until expiry |
| **Event-Based** | Evict on update/delete | Immediate, no stale data | Must remember to evict |
| **Version-Based** | Key includes version; update = new key | Automatic invalidation | Requires version field |
| **Smart Events** | Listen to events, evict related caches | Handles complex cases | More complex setup |

### Cache Stampede — When Expiry Causes a Storm

**Problem:** Cache expires. 1000 requests arrive at once. All get a miss. All 1000 hit the database.

**Solutions:**
1. **Locking:** One thread loads; others wait.
2. **Probabilistic early expiration:** Refresh cache before TTL (e.g., Caffeine `refreshAfterWrite`).
3. **Background refresh:** Refresh in background; serve stale if necessary.

**Caffeine built-in protection:**
```java
cacheManager.setCaffeine(Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .refreshAfterWrite(8, TimeUnit.MINUTES)  // Refresh before expiry
    .recordStats());
```

### Distributed Caching — Redis Considerations

- **Consistency vs speed:** Strong consistency = slower. Eventual consistency = faster.
- **Network latency:** Redis in same datacenter ≈ 1ms; cross-region can be 50–200ms.
- **Serialization:** JSON is readable; JDK serialization can be faster but less portable.
- **Cache coherency:** Use events (e.g., Kafka) to invalidate caches across services.

### Multi-Level Caching — L1 + L2

- **L1 (Local, e.g., Caffeine):** ~0.1ms, per-instance.
- **L2 (Redis):** ~1–5ms, shared.

Flow: Check L1 → if miss, check L2 → if miss, load from DB → store in both L1 and L2.

### Cache Warming — Pre-Populate on Startup

```java
@Component
public class CacheWarmer implements ApplicationListener<ContextRefreshedEvent> {
    
    private final ProductService productService;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        List<Long> popularIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        popularIds.forEach(productService::findById);
    }
}
```

### Cache Monitoring

Enable stats with Caffeine:
```java
.recordStats()
```

Use `CacheStats` for hit rate, miss count, etc.

---

## Layer 4 — Interview Mastery

### Q: How does Spring caching work?

**A:** Spring uses **AOP (Aspect-Oriented Programming)**:

1. Creates a proxy around `@Cacheable` methods.
2. Before the method runs, the proxy checks the cache for the key.
3. **Cache hit:** Return cached value; method is not executed.
4. **Cache miss:** Execute method, cache result, return.
5. `@CachePut` always runs the method and updates the cache.
6. `@CacheEvict` removes entries before or after the method runs.

---

### Q: Redis vs in-memory cache (e.g., Caffeine)?

**A:**

| Aspect | In-Memory (Caffeine) | Redis |
|--------|----------------------|-------|
| Speed | Fastest (~0.1ms) | Fast (~1–5ms) |
| Scope | Single instance | Distributed |
| Persistence | Lost on restart | Can persist |
| Use case | Single app, dev | Clusters, microservices |

**Best practice:** Use both — L1 (Caffeine) + L2 (Redis).

---

### Q: How do you handle cache invalidation?

**A:**
1. **TTL:** Automatic expiry.
2. **Event-based:** `@CacheEvict` on update/delete.
3. **Version-based:** Include version in cache key.
4. **Best practice:** Combine TTL (safety) + event-based (immediate updates).

---

### Q: What is cache stampede?

**A:** When a cache entry expires, many requests get a miss at once and all hit the database. **Solutions:** Locking, early refresh (`refreshAfterWrite`), or background refresh.

---

### Q: Cache-Aside vs Write-Through?

**A:**  
- **Cache-Aside:** Read from cache; on miss, load from DB and cache. Write to DB, then evict cache.  
- **Write-Through:** Write to cache and DB together; reads always from cache.

**Cache-Aside:** Read-heavy, simpler, eventual consistency.  
**Write-Through:** Strong consistency, slower writes.

---

### Q: How do you test caching?

**A:**  
1. Call method twice; verify second call does not hit DB (mock repository).  
2. After evict/delete, verify cache no longer contains the key.  
3. Use `CacheManager` to assert cache state directly.

---

### Q: How to handle cache failures?

**A:** Cache failures must not break the app. Use:  
1. **Fail-safe:** Catch cache errors, fall back to DB.  
2. **Circuit breaker:** If cache is down, skip it.  
3. **Fallback cache:** In-memory backup when Redis is unavailable.

---

## Summary

**Key takeaways:**

1. **Caching** = fast temporary storage — the fastest query is the one you skip.
2. **Cache hit** = data in cache; **cache miss** = load from source and cache.
3. **Spring Cache** uses `@EnableCaching`, `@Cacheable`, `@CachePut`, `@CacheEvict`, `@Caching`.
4. **Providers:** ConcurrentMapCache (default), Caffeine, Redis, EhCache.
5. **Strategies:** Cache-Aside (default), Write-Through, Write-Behind.
6. **Hibernate:** First-level (session), second-level (shared), query cache.
7. **Invalidation:** Use TTL + event-based eviction.
8. **Cache stampede:** Mitigate with locking or early refresh.
9. **Distributed:** Redis for multi-instance; consider L1 + L2 caching.
10. **Cache failures:** Always have a DB fallback.

**Next steps:**
- Add caching to a service in your project
- Monitor hit rates and tune TTLs
- Consider multi-level caching as you scale

---

[← Back to Index](00-README.md) | [Previous: JWT Auth](30-JWT-Auth.md) | [Next: Logging & Monitoring →](32-Logging-Monitoring.md)
