# Redis with Spring Boot

## Why Spring Boot + Redis?

You've learned what Redis is and how caching works. Now you want to use it in a real application. Spring Boot makes this easy. Instead of writing boilerplate code to connect to Redis, serialize objects, and manage cache keys, Spring gives you annotations and auto-configuration. You add a dependency, write a few lines of config, and you're done.

**What you get:** Declarative caching with `@Cacheable`, `@CachePut`, and `@CacheEvict`. Spring handles the "check cache, if miss then call method and store result" logic. You focus on business logic. Spring focuses on the plumbing.

**Why Spring Boot specifically?** Spring Boot auto-configures Redis connection based on your `application.properties`. You don't need to write connection pool code. You don't need to manually create `RedisTemplate` beans (though you can customize them). Start the app, and it works.

---

## What We'll Build

A simple REST API with Redis caching. When you fetch a product by ID, the app checks Redis first. If the product is in the cache (cache hit), it returns immediately. If not (cache miss), it queries the database, stores the result in Redis, and returns it. The next request for the same product will be a cache hit.

---

## Step 1: Project Setup

### Dependencies (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**What each dependency does:**

- **spring-boot-starter-web:** REST controllers, JSON serialization, embedded Tomcat. You need this for your API.
- **spring-boot-starter-data-redis:** Redis client (Lettuce by default), `RedisTemplate`, and Spring Cache support for Redis. This is the key dependency for Redis integration.
- **spring-boot-starter-data-jpa:** Database access. We'll use it for the Product entity and repository. In a real app, you'd use MySQL or PostgreSQL; H2 is for quick demos.
- **h2:** In-memory database. No setup. Good for learning. Replace with MySQL/PostgreSQL for production.
- **lombok:** Reduces boilerplate (`@Data`, `@Slf4j`, etc.). Optional but convenient.

---

### application.properties

```properties
# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# H2 Database (for demo)
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true

# Cache
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

**What each property does:**

- **spring.data.redis.host:** Where Redis is running. `localhost` if Redis is on the same machine. In production, use your Redis server hostname or IP.
- **spring.data.redis.port:** Redis port. Default is 6379. Change only if you've configured Redis to use a different port.
- **spring.datasource.url:** H2 in-memory database. `mem:testdb` means the database lives in memory and is named `testdb`. Data is lost when the app stops.
- **spring.h2.console.enabled=true:** Enables the H2 web console at `/h2-console`. Useful for inspecting the database during development.
- **spring.cache.type=redis:** Tells Spring to use Redis as the cache backend. Without this, Spring might use a simple in-memory cache.
- **spring.cache.redis.time-to-live=600000:** Default TTL for cache entries in milliseconds. 600000 ms = 10 minutes. Entries expire after 10 minutes unless overridden in code.

!!! info "Optional properties"
    You can also set `spring.data.redis.password` if your Redis has a password. And `spring.data.redis.database=0` to use a specific Redis database (0–15). Default is 0.

---

## Step 2: Enable Caching

Add `@EnableCaching` to your main class.

```java
@SpringBootApplication
@EnableCaching
public class RedisApp {
    public static void main(String[] args) {
        SpringApplication.run(RedisApp.class, args);
    }
}
```

**What this does:** `@EnableCaching` turns on Spring's caching infrastructure. It scans your beans for `@Cacheable`, `@CachePut`, and `@CacheEvict` and wraps those methods with cache logic. Without this annotation, those annotations do nothing.

---

## Step 3: Redis Configuration

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(
            new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

### Line-by-Line Explanation

**RedisTemplate bean:**
- **RedisConnectionFactory:** Injected by Spring. It knows how to connect to Redis (host, port, etc.). Created from your `application.properties`.
- **setConnectionFactory:** Tells the template which Redis to use.
- **setKeySerializer(StringRedisSerializer):** Keys are always strings in Redis. This serializer converts Java strings to Redis byte format. Human-readable in `redis-cli`.
- **setValueSerializer(GenericJackson2JsonRedisSerializer):** Values can be objects. Jackson converts them to JSON. When you store a `Product`, Redis gets a JSON string. When you read, Jackson deserializes it back to a `Product`. This also preserves type information so you don't get `LinkedHashMap` instead of `Product` when reading.

**CacheManager bean:**
- **RedisCacheConfiguration.defaultCacheConfig():** Starts with default settings.
- **entryTtl(Duration.ofMinutes(10)):** Each cache entry expires after 10 minutes. Overrides the global `spring.cache.redis.time-to-live` for this manager.
- **serializeValuesWith(GenericJackson2JsonRedisSerializer):** Same as the template. Cache values (your `Product` objects) are stored as JSON in Redis.
- **RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build():** Creates the cache manager that `@Cacheable` uses. It will create Redis keys like `products::1` (cache name `products`, key `1`).

---

## Step 4: Entity

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
    private double price;
    private String category;
}
```

!!! warning "Important"
    The entity **must implement `Serializable`** for Redis to store it. Spring's Redis cache serializes the object to JSON (with `GenericJackson2JsonRedisSerializer`), but the `Serializable` requirement is often enforced by the caching abstraction. It's a good practice for any object you cache.

---

## Step 5: Repository

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

Standard JPA repository. `findById(id)` will query the database. Nothing Redis-specific here.

---

## Step 6: Service with Caching Annotations

This is where the magic happens.

```java
@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        log.info("Fetching product {} from DATABASE", id);
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Not found"));
    }

    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        log.info("Updating product {} in DATABASE and CACHE", product.getId());
        return repository.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        log.info("Deleting product {} from DATABASE and CACHE", id);
        repository.deleteById(id);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {
        log.info("Cache cleared!");
    }
}
```

### Understanding the Annotations

| Annotation | What It Does |
|-----------|-------------|
| `@Cacheable` | Checks cache first. If found, returns cached data and **does not run the method**. If not found, runs the method and caches the result |
| `@CachePut` | Always runs the method. Then updates the cache with the return value. Use for updates |
| `@CacheEvict` | Removes data from cache. Use when deleting or when you want to invalidate |
| `@CacheEvict(allEntries=true)` | Clears the entire cache (all keys in that cache name) |

### How @Cacheable Works Internally (Proxy Pattern, AOP)

Spring uses **AOP (Aspect-Oriented Programming)** to implement caching. When you call `getProduct(1)`, you're not calling the actual method directly. You're calling a **proxy** that wraps the method.

**First call (cache miss):**
1. Controller calls `productService.getProduct(1)`.
2. The proxy intercepts the call.
3. Proxy checks Redis for key `products::1`. Not found.
4. Proxy calls the **actual** `getProduct(1)` method.
5. The method runs, logs "Fetching product 1 from DATABASE", queries the database, returns the product.
6. Proxy stores the result in Redis under `products::1` with TTL 10 minutes.
7. Proxy returns the product to the controller.

**Second call (cache hit):**
1. Controller calls `productService.getProduct(1)`.
2. The proxy intercepts the call.
3. Proxy checks Redis for key `products::1`. Found!
4. Proxy returns the cached product **without calling the method**.
5. No log. No database query. Just Redis.

!!! tip "Think about it this way"
    The proxy is like a bouncer. First time you show up, they check the list (cache). You're not on it. They let you in and add you to the list. Next time, you're on the list. They let you in without checking with the manager (database).

### Console Output: First Call vs Second Call

**First call:** `curl http://localhost:8080/api/products/1`
```
Fetching product 1 from DATABASE
```

**Second call:** `curl http://localhost:8080/api/products/1`
```
(No log! The method was never called.)
```

That's how you know caching is working. The log appears only on cache miss.

---

## Step 7: Controller

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService service;

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return service.getProduct(id);
    }

    @PutMapping
    public Product updateProduct(@RequestBody Product product) {
        return service.updateProduct(product);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        service.deleteProduct(id);
    }

    @DeleteMapping("/cache/clear")
    public String clearCache() {
        service.clearCache();
        return "Cache cleared!";
    }
}
```

Standard REST controller. Delegates to the service. The caching is transparent to the controller.

---

## Step 8: Test It

### Start Redis

```bash
docker run --name my-redis -p 6379:6379 -d redis
```

### Run the app

```bash
mvn spring-boot:run
```

### Seed some data (optional)

If you're using H2, add a `CommandLineRunner` or a `data.sql` file to insert a product with id 1. Otherwise `findById(1)` will throw.

### Test with curl

```bash
# First call - hits database (check logs for "Fetching from DATABASE")
curl http://localhost:8080/api/products/1

# Second call - served from Redis (no log printed!)
curl http://localhost:8080/api/products/1

# Clear cache
curl -X DELETE http://localhost:8080/api/products/cache/clear

# Third call - cache was cleared, so database again
curl http://localhost:8080/api/products/1
```

---

## Debugging: How to Verify Data is in Redis

Open `redis-cli` and run:

```bash
# Connect
redis-cli

# List all keys (you might see products::1, products::2, etc.)
KEYS *

# Get a specific key (Spring stores as JSON)
GET "products::1"

# Check TTL (seconds until expiry)
TTL "products::1"

# Delete a key manually (same as cache evict)
DEL "products::1"
```

**What you'll see:** `GET "products::1"` returns a JSON string like `{"@class":"com.example.Product","id":1,"name":"Laptop","price":50000.0,"category":"Electronics"}`. The `@class` field is added by `GenericJackson2JsonRedisSerializer` so Spring knows which Java class to deserialize to.

---

## Common Errors and How to Fix Them

### 1. "Unable to connect to Redis"

**Cause:** Redis is not running, or wrong host/port.

**Fix:** 
- Start Redis: `docker run --name my-redis -p 6379:6379 -d redis`
- Check `spring.data.redis.host` and `spring.data.redis.port` in `application.properties`.

### 2. "SerializationException" or "Cannot deserialize"

**Cause:** The object you're caching doesn't have a no-arg constructor, or has complex types that Jackson can't handle.

**Fix:** 
- Ensure your entity has a no-arg constructor (Lombok's `@NoArgsConstructor` helps).
- Implement `Serializable`.
- For complex objects, consider a custom serializer or store a DTO instead of the entity.

### 3. "NullPointerException" when calling @Cacheable method from same class

**Cause:** You're calling `getProduct(1)` from **within** the same class (e.g., from another method in `ProductService`). The proxy only works when the method is called from **outside** the bean. Internal calls bypass the proxy.

**Fix:** Call the method from a different bean (e.g., the controller), or inject `ProductService` into itself (not recommended), or extract the cached method to a separate bean.

### 4. Cache not working / Method always runs

**Cause:** `@EnableCaching` is missing, or `CacheManager` bean is not configured, or the cache name doesn't match.

**Fix:** 
- Add `@EnableCaching` to your main class.
- Ensure `RedisConfig` creates a `CacheManager` bean.
- Check that `@Cacheable(value = "products", ...)` matches the cache name you expect.

### 5. "Connection refused" on port 6379

**Cause:** Redis is not running, or Docker container is stopped.

**Fix:** `docker start my-redis` or run a new container.

---

## When to Use Annotations vs RedisTemplate

| Approach | When to Use | Example |
|----------|-------------|---------|
| **@Cacheable, @CachePut, @CacheEvict** | Simple caching. Same key pattern. Standard TTL. | Caching product by ID, user by ID |
| **RedisTemplate** | Custom logic. Conditional caching. Complex keys. Different data structures (List, Set, Hash). | Rate limiting, session storage, leaderboards, task queues |

### Detailed Comparison

**Annotations are great when:**
- You're caching method return values by a simple key (e.g., method argument).
- You want minimal code. One annotation does the job.
- Your caching needs are standard: cache on read, invalidate on write.

**RedisTemplate is better when:**
- You need to use Redis data structures: `opsForList()`, `opsForSet()`, `opsForHash()`, `opsForZSet()`.
- You need conditional caching: "Only cache if the result is not null" or "Cache with different TTL based on some condition."
- You're building something that isn't "cache a method result": rate limiter, online users set, leaderboard, etc.
- You need fine-grained control over key names or serialization.

### Example: Manual RedisTemplate for Custom Logic

```java
@Service
public class ProductServiceManual {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductRepository repository;

    private static final String CACHE_PREFIX = "product:";

    public Product getProduct(Long id) {
        String key = CACHE_PREFIX + id;

        // Check cache
        Product cached = (Product) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // Cache miss - fetch from DB
        Product product = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Not found"));

        // Store in cache with 10-minute TTL
        redisTemplate.opsForValue().set(key, product,
            Duration.ofMinutes(10));

        return product;
    }

    public void deleteProduct(Long id) {
        repository.deleteById(id);
        redisTemplate.delete(CACHE_PREFIX + id);
    }
}
```

Use this when you need logic that annotations can't express—e.g., "only cache if product is in stock" or "use a different TTL for featured products."

---

## Common Redis Operations with RedisTemplate

```java
// String operations
redisTemplate.opsForValue().set("key", "value");
redisTemplate.opsForValue().get("key");
redisTemplate.opsForValue().set("key", "value", Duration.ofMinutes(5));

// Hash operations
redisTemplate.opsForHash().put("user:1", "name", "Satjit");
redisTemplate.opsForHash().get("user:1", "name");
redisTemplate.opsForHash().entries("user:1");

// List operations
redisTemplate.opsForList().leftPush("queue", "task1");
redisTemplate.opsForList().rightPop("queue");

// Set operations
redisTemplate.opsForSet().add("tags", "java", "spring", "redis");
redisTemplate.opsForSet().members("tags");

// Sorted Set operations
redisTemplate.opsForZSet().add("leaderboard", "player1", 100);
redisTemplate.opsForZSet().reverseRange("leaderboard", 0, 9);

// Delete
redisTemplate.delete("key");

// Check existence
redisTemplate.hasKey("key");

// Set TTL
redisTemplate.expire("key", Duration.ofMinutes(30));
```

---

## Quick Summary

| Concept | How |
|---------|-----|
| Add Redis to Spring Boot | `spring-boot-starter-data-redis` |
| Enable caching | `@EnableCaching` on main class |
| Cache a method result | `@Cacheable(value, key)` |
| Update cache on write | `@CachePut(value, key)` |
| Remove from cache | `@CacheEvict(value, key)` |
| Manual control | Use `RedisTemplate` |
| Config TTL | `spring.cache.redis.time-to-live` or `Duration` in config |
| Verify in Redis | `redis-cli` → `KEYS *` → `GET "products::1"` |

---

## Exercise

1. Create a Spring Boot app with Redis caching for a `User` entity.
2. Call `GET /users/1` twice. Check logs to confirm the second call skips the database.
3. Update user, then GET again. Verify the cache has fresh data (use `@CachePut` or evict on update).
4. Try the manual `RedisTemplate` approach for one endpoint. Compare the code to the annotation approach.
5. Add a `@Cacheable` method that takes two parameters (e.g., `getProductByCategoryAndId`). Use `key = "#category + '::' + #id"` in the annotation.
6. Open `redis-cli` and run `KEYS *` while your app is running. What keys do you see? What happens after you clear the cache?

---

**Next up:** [Redis Interview Questions](redis-interview-questions.md) - Prepare for your interview!
