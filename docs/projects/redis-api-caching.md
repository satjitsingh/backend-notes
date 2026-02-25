# Project: Redis API Caching

## What Problem Are We Solving?

Imagine you run an e-commerce website. Your product catalog API gets hit thousands of times per minute. Every single request goes to your database, runs a query, and returns the result. The database is doing the same work over and over again—fetching the same "Laptop Pro" product details for hundreds of users who are all looking at the same page.

**Here's the business case:** Your database is expensive. It's slow compared to memory. And when you get a traffic spike (say, a flash sale), your database becomes the bottleneck. Users see slow page loads. Some requests might even time out. You lose sales.

**The solution?** Cache the most frequently accessed data in Redis—an in-memory store that's 100x faster than hitting the database. The first user to request a product pays the "cold" cost (we hit the database). Every subsequent user gets the data from Redis in milliseconds. Your database breathes easier. Your users get instant responses.

!!! tip "Real-World Analogy"
    Think of Redis like a waiter's notepad. Instead of running to the kitchen (database) every time someone asks "What's the special today?", the waiter writes it down once and reads from the notepad for the next 50 customers. The kitchen only needs to be consulted when the special actually changes.

---

## Architecture Overview

Let's understand the flow before we write any code. Here's what happens when a client requests product data:

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    REQUEST FLOW                          │
                    └─────────────────────────────────────────────────────────┘

    Client                    Spring Boot API                    Redis              Database
       │                            │                              │                    │
       │  GET /api/products/1       │                              │                    │
       │ ──────────────────────────▶│                              │                    │
       │                            │  Check: Does key "1" exist?  │                    │
       │                            │ ───────────────────────────▶│                    │
       │                            │                              │                    │
       │                            │  ┌───────────────────────────────────────────────┤
       │                            │  │ CACHE HIT?                                    │
       │                            │  │   YES → Return data from Redis (5ms)           │
       │                            │   NO  → Continue to database...                  │
       │                            │  └───────────────────────────────────────────────┤
       │                            │                              │                    │
       │                            │  SELECT * FROM products      │                    │
       │                            │  WHERE id = 1                │                    │
       │                            │ ───────────────────────────────────────────────▶│
       │                            │                              │                    │
       │                            │  Store in Redis: products::1  │                    │
       │                            │ ◀────────────────────────────────────────────────│
       │                            │ ───────────────────────────▶│                    │
       │                            │                              │                    │
       │  JSON Response             │                              │                    │
       │ ◀──────────────────────────│                              │                    │
       │                            │                              │                    │
```

**Key insight:** The application always checks Redis first. Only when Redis doesn't have the data (a "cache miss") do we go to the database. After fetching from the database, we store the result in Redis so the next request is fast.

---

## Why We Chose Each Dependency

Before we dive into code, let's understand why we're using each piece of our tech stack.

| Component | Technology | Why This Choice? |
|-----------|------------|------------------|
| **Framework** | Spring Boot 3 | Built-in caching abstraction. We write `@Cacheable` and Spring handles the rest. No manual Redis connection management. |
| **Database** | H2 (in-memory) | For learning/demo only. No setup. In production you'd use PostgreSQL or MySQL. |
| **Cache** | Redis | Industry standard for caching. Sub-millisecond reads. TTL support. Battle-tested at scale. |
| **Spring Data Redis** | Dependency | Provides `RedisTemplate`, serialization, and integrates with Spring's `@Cacheable` annotations. |
| **Lombok** | Dependency | Reduces boilerplate (`@Data`, `@NoArgsConstructor`). Not required for caching—just convenience. |

!!! info "Why Not Use Spring's Simple In-Memory Cache?"
    Spring has a built-in `ConcurrentMapCacheManager` that caches in your application's memory. It works, but it has a critical flaw: **it doesn't survive server restarts**, and **it doesn't work across multiple server instances**. If you run 3 API servers behind a load balancer, each server has its own cache. User A hits Server 1 (cache miss, slow). User B hits Server 2 (cache miss again!). Redis gives you one shared cache for all servers.

---

## Step 1: Create the Project

Use [Spring Initializr](https://start.spring.io/) with:

- **Spring Web** — REST API
- **Spring Data JPA** — Database access
- **Spring Data Redis** — Redis integration
- **H2 Database** — In-memory DB for demo
- **Lombok** — Reduces boilerplate

---

## Step 2: application.properties

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.datasource.url=jdbc:h2:mem:productdb
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=create-drop
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

**What's happening here?**

- `spring.data.redis.host` and `port` — Where is Redis running? Default is localhost:6379.
- `spring.cache.type=redis` — Tell Spring to use Redis as the cache backend (not the default in-memory cache).
- `spring.cache.redis.time-to-live=600000` — Default TTL in milliseconds. 600000 ms = 10 minutes. After 10 minutes of no access, Redis automatically deletes the key.
- `spring.jpa.hibernate.ddl-auto=create-drop` — Hibernate creates tables on startup and drops them on shutdown. Fine for demos; use `validate` or `update` in production.

---

## Step 3: Entity

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private double price;
    private String category;
}
```

**What's happening here?**

- `implements Serializable` — **Critical for Redis!** When we store a Product in Redis, it gets serialized to bytes. Java's serialization (or JSON, which we'll use) requires objects to be serializable. Without this, you might get serialization errors.
- The rest is standard JPA: `@Entity` for database mapping, `@Id` for the primary key.

---

## Step 4: Repository

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
}
```

Standard Spring Data JPA. No Redis-specific code here. The repository talks to the database. Our service layer will add the caching logic.

---

## Step 5: Redis Configuration — What Each Bean Does

Before we show the code, let's understand **why we need custom Redis configuration** at all. Spring Boot auto-configures Redis, but the default cache manager might not do what we want. We need:

1. **Custom TTL per cache region** — Product details might change frequently; we want 15 minutes. Category lists change less often; we want 30 minutes.
2. **JSON serialization** — The default uses Java serialization, which produces binary blobs. JSON is human-readable in Redis and works across different languages.
3. **Disable caching null values** — If a product doesn't exist, we shouldn't cache "null". Otherwise, every request for product ID 99999 would hit the cache, get null, and we'd never know to return 404 properly.

Here's the configuration:

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))      // Default: 10 min TTL
            .disableCachingNullValues()            // Don't cache null (e.g., product not found)
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));  // Store as JSON

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("products",    // Cache region "products"
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("categories", // Cache region "categories"
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)))
            .build();
    }
}
```

**What's happening here?**

- `@EnableCaching` — Turns on Spring's caching infrastructure. Without this, `@Cacheable` does nothing.
- `RedisCacheManager` — The bridge between Spring's `@Cacheable` and Redis. When you call `@Cacheable("products")`, Spring uses this manager to store/retrieve from Redis.
- `GenericJackson2JsonRedisSerializer` — Converts Java objects to JSON before storing in Redis. You can inspect keys in Redis CLI and see readable JSON.
- `withCacheConfiguration("products", ...)` — Override the default TTL for the "products" cache region. Individual product lookups: 15 min. Category lists: 30 min.

---

## Step 6: Service Layer

```java
@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Cacheable(value = "products", key = "#id")
    public Product getById(Long id) {
        log.info("DB QUERY: Fetching product {}", id);
        simulateSlowQuery();
        return repository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Product not found: " + id));
    }

    @Cacheable(value = "categories", key = "#category")
    public List<Product> getByCategory(String category) {
        log.info("DB QUERY: Fetching products in category {}", category);
        simulateSlowQuery();
        return repository.findByCategory(category);
    }

    @CachePut(value = "products", key = "#product.id")
    public Product create(Product product) {
        log.info("Creating product: {}", product.getName());
        return repository.save(product);
    }

    @CachePut(value = "products", key = "#product.id")
    public Product update(Product product) {
        log.info("Updating product: {}", product.getId());
        return repository.save(product);
    }

    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "categories", allEntries = true)
    })
    public void delete(Long id) {
        log.info("Deleting product: {}", id);
        repository.deleteById(id);
    }

    @CacheEvict(value = {"products", "categories"}, allEntries = true)
    public void clearAllCache() {
        log.info("All caches cleared!");
    }

    private void simulateSlowQuery() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**What's happening here?**

- `@Cacheable(value = "products", key = "#id")` — Before running `getById()`, Spring checks Redis for key `products::1` (if id=1). If found, return immediately. If not, run the method, store the result in Redis, then return.
- `@CachePut` — Always runs the method (we need to save to DB). After saving, it puts the result into the cache. Used for create/update so the cache stays in sync.
- `@CacheEvict` — Removes entries from cache. On delete, we evict the specific product and all category entries (since the category list changed).
- `simulateSlowQuery()` — A 2-second delay to make the cache hit/miss difference obvious. In production, your DB might take 50-200ms; the effect is the same.

---

## Let's Trace a Request Step by Step

### First Request: GET /api/products/1

| Step | What Happens | Where | Time |
|------|--------------|-------|------|
| 1 | Client sends GET request | Network | 0 ms |
| 2 | Controller calls `service.getById(1)` | Spring | 1 ms |
| 3 | Spring checks Redis for key `products::1` | Redis | 2 ms |
| 4 | **Cache MISS** — key doesn't exist | Redis | — |
| 5 | Method executes: `repository.findById(1)` | App + DB | 2000 ms (simulated) |
| 6 | Product returned from DB | Database | — |
| 7 | Spring stores Product in Redis as `products::1` | Redis | 5 ms |
| 8 | Response sent to client | Network | 1 ms |
| **Total** | | | **~2008 ms** |

**Log output:** `DB QUERY: Fetching product 1`

---

### Second Request: GET /api/products/1 (Same Product, 10 Seconds Later)

| Step | What Happens | Where | Time |
|------|--------------|-------|------|
| 1 | Client sends GET request | Network | 0 ms |
| 2 | Controller calls `service.getById(1)` | Spring | 1 ms |
| 3 | Spring checks Redis for key `products::1` | Redis | 2 ms |
| 4 | **Cache HIT** — data found! | Redis | — |
| 5 | Spring returns cached Product **without calling the method** | Redis | 3 ms |
| 6 | Response sent to client | Network | 1 ms |
| **Total** | | | **~6 ms** |

**Log output:** *(Nothing! The method never ran.)*

!!! example "The Time Difference"
    First request: ~2000 ms. Second request: ~6 ms. That's over **300x faster**. In a real system with 50ms DB queries, you'd see 50ms vs 2ms—still a 25x improvement.

---

## Verify It in Redis CLI

After making a few requests, connect to Redis and see what's actually stored:

```bash
# Start Redis (if not running)
docker run --name redis -p 6379:6379 -d redis

# Connect to Redis CLI
redis-cli

# List all keys (Spring uses a pattern like cacheName::key)
KEYS *

# Example output:
# 1) "products::1"
# 2) "categories::electronics"

# View the cached product (JSON format)
GET "products::1"

# Example output (pretty-printed):
# {"@class":"com.example.product.Product","id":1,"name":"Laptop",
#  "description":"MacBook Pro 16","price":150000.0,"category":"electronics"}

# Check how long until this key expires (in seconds)
TTL "products::1"
# Returns: 847  (about 14 minutes left)
```

---

## What Happens When...?

### User Updates Data

When you call `PUT /api/products` with updated product info:

1. `update()` runs (it's not cached, so it always executes).
2. Database is updated.
3. `@CachePut` stores the **new** product in Redis with the same key `products::1`.
4. The old cached value is overwritten.
5. Next GET request gets the fresh data from cache.

**No stale data.** The cache and DB stay in sync on updates.

---

### Cache Expires (TTL Reached)

After 15 minutes with no access to product 1:

1. Redis automatically deletes the key `products::1` (TTL expired).
2. Next GET request: cache miss.
3. Method runs, hits database, stores in Redis again.
4. Cycle repeats.

!!! warning "Stale Reads Between Update and Expire"
    If you have a bug where you forget `@CachePut` on update, the cache could serve old data until TTL expires. Always invalidate or update cache on writes!

---

### Redis Goes Down

If Redis is unavailable:

1. Spring's cache operations will throw exceptions (e.g., `RedisConnectionException`).
2. Your API might return 500 errors.

**Mitigation:** Use `@Cacheable` with a fallback, or wrap cache calls in try-catch and fall through to the database. In production, run Redis in a cluster with replicas for high availability.

---

## Performance Comparison

| Scenario | Without Caching | With Redis Caching |
|----------|------------------|---------------------|
| 1st request for product 1 | ~2000 ms | ~2000 ms (cold) |
| 2nd request for product 1 | ~2000 ms | ~5 ms |
| 100 concurrent users, same product | DB overloaded, timeouts | All served from Redis, ~5 ms each |
| Database load | Every request hits DB | Only cache misses hit DB |

!!! tip "Rule of Thumb"
    In many production systems, 80% of traffic is for 20% of the data. Caching that 20% in Redis can reduce database load by 80% or more.

---

## Common Errors and Fixes

### Error: "Connection refused" to Redis

**Cause:** Redis isn't running or wrong host/port.

**Fix:**
```bash
docker run --name redis -p 6379:6379 -d redis
```
Verify: `redis-cli ping` should return `PONG`.

---

### Error: "SerializationException" or "ClassCastException" when reading from cache

**Cause:** Object structure changed (e.g., you added a field to Product) but old JSON in Redis has different structure.

**Fix:** Clear the cache: `curl -X DELETE http://localhost:8080/api/products/cache` or run `redis-cli FLUSHDB` (careful in production!).

---

### Cache never seems to work — always see "DB QUERY" in logs

**Possible causes:**
1. `@EnableCaching` missing on a `@Configuration` class.
2. Method is called from within the same class (e.g., `this.getById(1)`). Spring's proxy doesn't intercept internal calls. Call through another bean or restructure.

---

### Caching null when product not found

**Cause:** `disableCachingNullValues()` not set in Redis config.

**Fix:** Ensure your `RedisCacheConfiguration` includes `.disableCachingNullValues()`.

---

## How to Extend This Project

| Idea | What to Do |
|------|------------|
| **Cache warming** | On startup, pre-load popular products into Redis so the first user doesn't pay the cold cost. |
| **Cache aside pattern** | Manually check Redis, then DB, then populate Redis. Gives you more control than `@Cacheable`. |
| **Multi-level cache** | Use Caffeine (in-memory) as L1 and Redis as L2. Ultra-fast for hot data. |
| **Cache per user** | Add `key = "#id + '-' + #userId"` for user-specific product recommendations. |
| **Metrics** | Add Micrometer and track cache hit rate. Aim for >80% hit rate on read-heavy endpoints. |

---

## Step 7: Controller

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService service;

    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/category/{category}")
    public List<Product> getByCategory(
            @PathVariable String category) {
        return service.getByCategory(category);
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return service.create(product);
    }

    @PutMapping
    public Product update(@RequestBody Product product) {
        return service.update(product);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @DeleteMapping("/cache")
    public String clearCache() {
        service.clearAllCache();
        return "Cache cleared!";
    }
}
```

---

## Step 8: Seed Data

```java
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ProductRepository repository;

    @Override
    public void run(String... args) {
        repository.save(new Product(null, "Laptop",
            "MacBook Pro 16", 150000, "electronics"));
        repository.save(new Product(null, "Phone",
            "iPhone 15 Pro", 130000, "electronics"));
        repository.save(new Product(null, "T-Shirt",
            "Cotton Round Neck", 500, "clothing"));
        repository.save(new Product(null, "Jeans",
            "Slim Fit Denim", 1200, "clothing"));
        repository.save(new Product(null, "Headphones",
            "Sony WH-1000XM5", 25000, "electronics"));
    }
}
```

---

## Step 9: Test It

### Start Redis

```bash
docker run --name redis -p 6379:6379 -d redis
```

### Run the App

```bash
mvn spring-boot:run
```

### Test Script

```bash
# 1st call - DB QUERY (slow, ~2 seconds)
curl http://localhost:8080/api/products/1

# 2nd call - CACHE HIT (fast, instant!)
curl http://localhost:8080/api/products/1

# Category (DB QUERY first time, cached after)
curl http://localhost:8080/api/products/category/electronics

# Update (updates both DB and cache)
curl -X PUT http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "name": "Laptop Pro", "description": "Updated", "price": 160000, "category": "electronics"}'

# Clear cache
curl -X DELETE http://localhost:8080/api/products/cache
```

---

## Quick Summary

| Concept | Implementation |
|---------|-----------------|
| Cache-aside pattern | `@Cacheable` checks Redis first, DB on miss |
| Cache regions | `products` (15 min TTL), `categories` (30 min TTL) |
| Cache invalidation | `@CachePut` on create/update, `@CacheEvict` on delete |
| JSON in Redis | `GenericJackson2JsonRedisSerializer` |
| Key format | `cacheName::key` (e.g., `products::1`) |

| Action | Log Output | Response Time |
|--------|------------|---------------|
| 1st GET /products/1 | "DB QUERY: Fetching product 1" | ~2 seconds |
| 2nd GET /products/1 | *No log* (served from cache) | ~5 ms |
| After update | Cache automatically refreshed | Instant |
| After delete | Cache key removed | Next GET hits DB |

---

**Next project:** [Redis Session Storage](redis-session-storage.md)
