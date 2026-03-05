# Chapter 28: Query Optimization & Connection Pooling

[← Back to Index](00-README.md) | [Previous: N+1 Problem](27-N+1-Problem.md) | [Next: Spring Security →](29-Spring-Security.md)

---

## Why This Chapter Matters

Your application code can be perfect, your architecture can be flawless, but if your database queries are slow, **everything is slow**. The database is the #1 bottleneck in most web applications.

Think about it: every page load, every API call, every background job — they all talk to the database. A query that takes 1 second instead of 10 milliseconds doesn't just slow down one request. Under 100 concurrent users, that's 100 seconds of combined wait time, connection pool exhaustion, and cascading failures.

**This chapter teaches you:**
- How to make your queries lightning fast
- How connection pooling works and why it's critical
- How to detect and fix performance bottlenecks
- How to configure databases for production scale
- How to ace performance-related interview questions

**Without query optimization:**
- Your app is slow under load (and you don't know why)
- Database connections get exhausted
- Users see timeout errors
- Server resources are wasted
- You fail performance interview questions

**With proper optimization:**
- Queries execute in milliseconds
- Connection pool is properly sized
- Application scales gracefully under load
- You can diagnose any database performance issue
- You excel in system design discussions

---

## Layer 1 — Intuition Builder

Imagine you're learning how to make a library super fast. Not a regular library — one where thousands of kids ask questions every minute. Let's build the intuition one piece at a time.

---

### Part 1: Why Does Speed Even Matter?

**Imagine a library with 1 million books.**

When someone asks "Do you have a book about dinosaurs?", what happens?

- **The slow way:** The librarian walks through every single shelf, looking at every book, one by one. It might take hours.
- **The fast way:** The librarian checks a special list (an index) at the front desk. "Dinosaurs? That's in aisle 7, shelf 3." Two seconds.

**The database is your library.** Every time your app loads a page, it asks the database questions. If each question takes too long, your app feels like a snail. If each question is answered in milliseconds, your app feels like a rocket.

---

### Part 2: What Is a Query? (Plain English First)

A **query** is simply a question you ask the database.

- "Show me all users named Alice" → That's a query.
- "How many orders were placed today?" → That's a query.
- "Give me the product with ID 42" → That's a query.

Every time your app does something — showing a list, saving data, displaying a profile — it sends one or more queries to the database. The faster those queries run, the faster your app feels.

---

### Part 3: The Book Index — Your Best Friend

**Think of the index at the back of a textbook.**

You have a 1,000-page science book. Your friend asks: "Where does it talk about photosynthesis?"

**Without an index:**
- You start at page 1
- You read every page until you find it
- Maybe you find it on page 734
- You had to look at 734 pages!

**With an index:**
- You flip to the back
- You find "Photosynthesis — page 734"
- You go straight to page 734
- You only had to do 2 lookups!

**In a database, an index works the same way.** It's a special list that helps the database find rows super fast without reading every single row. We'll learn more about B-Tree indexes later — for now, just remember: **indexes are like the index in a book. They make finding things lightning fast.**

---

### Part 4: The Taxi Stand — Connection Pooling

**Imagine two ways to get a taxi at an airport:**

**Option A — Call a taxi each time:**
- You arrive → Call taxi company → Wait 15 minutes → Taxi arrives → You ride
- Your friend arrives → Call taxi company → Wait 15 minutes → Taxi arrives → Your friend rides
- Every person waits 15 minutes. Ouch!

**Option B — Taxi stand (a row of taxis always waiting):**
- 10 taxis are always parked at the stand
- You arrive → Grab a taxi immediately → Ride → Taxi returns to the stand
- Your friend arrives → Grabs that same taxi (now back at the stand) → Ride → Taxi returns
- Almost no waiting! Taxis are reused!

**Connection pooling is the taxi stand for your database.** Instead of creating a new "connection" (like a phone line to the database) every time your app wants to ask a question, you keep a small pool of connections ready. Your app borrows one, asks its question, and returns it. The next request uses the same connection. No waiting!

**Why does creating a connection take time?** The database has to: introduce itself over the network, check your username and password, and set up a little workspace. That takes 50–200 milliseconds. Reusing a connection? Less than 1 millisecond.

---

### Part 5: The Notepad of Recent Answers — Caching

**Imagine you're a waiter. Customers keep asking: "What's the soup of the day?"**

**Without a notepad:**
- Customer 1 asks → You walk to the kitchen, ask the chef, come back → "Tomato soup"
- Customer 2 asks → You walk to the kitchen again, ask the chef again, come back → "Tomato soup"
- Customer 3 asks → Same walk, same question, same answer
- You're exhausted and the kitchen is annoyed!

**With a notepad:**
- Customer 1 asks → You walk to the kitchen, ask the chef, write "Tomato soup" on your notepad, tell the customer
- Customer 2 asks → You check your notepad → "Tomato soup" — no trip to the kitchen!
- Customer 3 asks → Same thing — instant answer!

**Caching is that notepad.** When your app asks the database a question and gets an answer, it can save that answer in a "cache" (a fast storage area). The next time someone asks the same question, the app checks the cache first. If the answer is there and still fresh, it doesn't need to bother the database. Faster for everyone!

---

### Part 6: Ordering the Whole Menu vs. Just What You Want

**Imagine going to a restaurant.**

**Bad way:** "Bring me everything on the menu!" The kitchen prepares 200 dishes. You only eat the salad. What a waste of time and food!

**Good way:** "I'll have the Caesar salad and water, please." The kitchen makes only what you need. Fast and efficient.

**Fetching only what you need from the database is the same idea.** If you only need a user's name and email, don't ask the database for their entire profile (address, phone, bio, 20 other fields). Ask only for name and email. Less data = faster response. This is called **projection** — you "project" only the columns you need.

---

### Part 7: Counting Every Car vs. Spotting One

**Someone asks: "Is there a red car in this parking lot?"**

**Silly way:** You count every red car. "One red car... two red cars... three red cars..." You count all 47 red cars, then say "Yes, there is." You did way more work than needed!

**Smart way:** You look around. You spot one red car. You immediately say "Yes!" and stop. You didn't need to count them all.

**In databases:** When you only need to know "does this exist?" (like "Is this email already taken?"), use **EXISTS** — it stops at the first match. Don't use **COUNT** — that counts every single matching row, which is slow when there are millions of rows.

---

### Part 8: One Trip vs. Many Trips to the Kitchen

**Imagine a waiter taking orders for a table of 10.**

**Bad way:** The waiter goes to the kitchen 10 times — once per person. Ten trips!

**Good way:** The waiter writes down all 10 orders, goes to the kitchen once, and brings everything back. One trip!

**Batch operations work the same way.** Instead of sending 1,000 separate "save this user" requests to the database, you bundle them into groups of 50 and send 20 batch requests. Far fewer trips = much faster.

---

### Part 9: The Slow vs. Fast Library — Visual Summary

```
┌─────────────────────────────────────────────────────────────────────┐
│                    THE DATABASE LIBRARY                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  📚 Problem 1: NO INDEX (Full table scan)                           │
│  Librarian searches every shelf to find one book.                     │
│  Fix: Add an index! (Like the index at the back of a textbook)      │
│                                                                       │
│  🚕 Problem 2: NO TAXI STAND (No connection pool)                   │
│  Every person calls a new taxi and waits 15 minutes.                  │
│  Fix: Use a connection pool! (Keep taxis ready at the stand)          │
│                                                                       │
│  📋 Problem 3: NO NOTEPAD (No caching)                               │
│  Same question asked 100 times = 100 trips to the kitchen.            │
│  Fix: Cache answers! (@Cacheable, Redis)                              │
│                                                                       │
│  📖 Problem 4: ORDERING THE WHOLE MENU (Over-fetching)              │
│  Asking for all 20 fields when you need only 3.                      │
│  Fix: Use projections! (Fetch only what you need)                     │
│                                                                       │
│  🔢 Problem 5: COUNTING INSTEAD OF EXISTS                            │
│  Counting millions of rows just to say "yes, one exists."             │
│  Fix: Use EXISTS! (Stop at the first match)                          │
│                                                                       │
│  🛤️ Problem 6: TOO MANY TRIPS (No batching)                         │
│  1000 separate database trips instead of 20 batch trips.              │
│  Fix: Batch your operations! (batch_size = 50)                       │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Layer 2 — Professional Developer

Now we build on that intuition with real configuration, code, and practices. We always explain **why** before **how**.

---

### Connection Pooling with HikariCP

**HikariCP** (光, Japanese for "light") is Spring Boot's default connection pool. It's fast, reliable, and widely used in production.

**Why connection pooling?** Creating a database connection costs 50–200ms (network handshake, auth, session setup). A pool keeps connections ready — borrowing one costs &lt;1ms. Under load, the difference is massive.

#### Basic HikariCP Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

    hikari:
      # === POOL SIZE ===
      maximum-pool-size: 10        # Never exceed 10 connections (see sizing below)
      minimum-idle: 5              # Keep at least 5 connections ready when idle

      # === TIMEOUTS (all in milliseconds) ===
      connection-timeout: 30000    # Max wait for a connection: 30 seconds
      idle-timeout: 600000         # Close idle connections after 10 minutes
      max-lifetime: 1800000        # Recycle connections after 30 minutes

      # === HEALTH & SAFETY ===
      connection-test-query: SELECT 1           # Simple ping to verify connection
      leak-detection-threshold: 60000            # Warn if connection held > 60s
```

**HikariCP settings explained:**

| Setting | Value | Meaning |
|---------|-------|---------|
| `maximum-pool-size` | 10 | Pool never grows beyond this. Like "max 10 taxi spots." |
| `minimum-idle` | 5 | Always keep at least 5 connections ready. |
| `connection-timeout` | 30000 | If no free connection in 30s, throw an error. |
| `idle-timeout` | 600000 | Close connections idle for 10 minutes to free resources. |
| `max-lifetime` | 1800000 | Recycle connections after 30 min to avoid stale ones. |
| `leak-detection-threshold` | 60000 | Log a warning if code holds a connection for 60s. |

#### Pool Sizing — Why Smaller Is Often Faster

**Common mistake:** Using a pool of 100 or 200 connections.

**Why smaller is better:** A database server has limited CPU cores. With 4 cores, it can truly run only ~4 queries in parallel. Extra connections cause context switching, memory use, and lock contention.

**PostgreSQL-style sizing:**

```
connections ≈ (CPU_cores × 2) + effective_spindle_count

Example: 4-core server with SSD
  → (4 × 2) + 1 ≈ 10 connections
```

**Guidelines:**

| Scenario | Suggested pool size |
|----------|---------------------|
| Small API (single server) | 5–10 |
| Medium app (2–3 servers) | 10–15 per server |
| Large app (10+ servers) | 5–10 per server |
| Batch jobs | 20–30 |

**Rule of thumb:** If 3 app servers each use `maximum-pool-size: 20`, the DB can see 60 connections. Leave headroom for admin and other clients.

---

### Query Logging — See What Your Queries Do

**Why:** You can't optimize what you can't see. Logging shows which queries run, how often, and how long they take.

```yaml
# application.yml — development only (verbose!)
spring:
  jpa:
    show-sql: true                    # Log SQL to console (simple but noisy)
    properties:
      hibernate:
        format_sql: true             # Pretty-print SQL for readability
        use_sql_comments: true       # Add hints in logs
        # Better: use a logging framework for control
logging:
  level:
    org.hibernate.SQL: DEBUG         # Log all SQL
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # Log parameters
```

**Production-safe slow query detection:**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        session:
          # Log queries slower than 1 second
          events:
            log:
              LOG_QUERIES_SLOWER_THAN_MS: 1000
```

**Or use Datasource Proxy / P6Spy for detailed query timing.** In short: enable logging in dev to understand behavior; in prod, focus on slow-query alerts.

---

### Projections — Fetch Only What You Need

**Why:** Loading full entities pulls all columns and triggers lazy loading. Projections fetch only the columns you need — less data, fewer joins, faster responses.

#### Interface projections

```java
// Define an interface — Spring generates implementation at runtime
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Returns only id, name, email — not the full User entity
    List<UserSummary> findByActiveTrue();
}
// Generated SQL: SELECT id, name, email FROM users WHERE active = true
```

#### Class-based projections (DTO)

```java
// Use a constructor — good when you need logic or multiple constructors
public record UserSummaryDto(Long id, String name, String email) {}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT new com.example.dto.UserSummaryDto(u.id, u.name, u.email) " +
           "FROM User u WHERE u.active = true")
    List<UserSummaryDto> findActiveUserSummaries();
}
```

**Comparison:**

| Approach | Columns fetched | Typical use |
|----------|------------------|-------------|
| Full `User` entity | All (~20+) | When you need full object graph |
| `UserSummary` interface | 3 | Read-only list views |
| `UserSummaryDto` class | 3 | DTOs, APIs, custom logic |

---

### Pagination — Never Load Everything

**Why:** `findAll()` loads all rows into memory. With millions of rows, this leads to OOM and slow responses.

```java
// ❌ BAD — loads all rows into memory
List<User> all = userRepository.findAll();

// ✅ GOOD — loads one page at a time
Page<User> page = userRepository.findAll(
    PageRequest.of(0, 20, Sort.by("createdAt").descending())
);

// Use the result
page.getContent();        // The 20 users
page.getTotalElements();  // Total count (e.g. 1,000,000)
page.getTotalPages();     // e.g. 50,000
page.hasNext();           // More pages?
```

---

### EXISTS vs COUNT

**Why:** For "does this exist?" you only need one row. COUNT scans all matching rows; EXISTS stops at the first.

```java
// ❌ BAD — counts all matching rows
@Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email")
boolean emailExistsBad(@Param("email") String email);

// ✅ GOOD — stops at first match
boolean existsByEmail(String email);  // Spring Data generates EXISTS
```

**Performance:** On a large table, EXISTS can be orders of magnitude faster than COUNT.

---

### Batch Operations

**Why:** One INSERT per row means one network round-trip per row. Batching groups many statements into fewer round-trips.

**Enable batching:**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50           # Group up to 50 statements
        order_inserts: true        # Batch INSERTs by entity
        order_updates: true        # Batch UPDATEs by entity
```

**Bulk insert example:**

```java
@Service
@Transactional
public class UserImportService {

    private final UserRepository userRepository;

    // ❌ BAD — one query per user
    public void importBad(List<User> users) {
        for (User u : users) {
            userRepository.save(u);
        }
        // 1000 users ≈ 1000 queries
    }

    // ✅ GOOD — batched
    public void importGood(List<User> users) {
        int batchSize = 50;
        for (int i = 0; i < users.size(); i += batchSize) {
            int end = Math.min(i + batchSize, users.size());
            List<User> batch = users.subList(i, end);
            userRepository.saveAll(batch);
            userRepository.flush();  // Send batch to DB
        }
        // 1000 users ≈ 20 batched queries
    }
}
```

**Bulk UPDATE/DELETE with JPQL:**

```java
@Modifying
@Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids")
int updateStatus(@Param("ids") List<Long> ids, @Param("status") UserStatus status);

@Modifying
@Query("DELETE FROM User u WHERE u.lastLoginDate < :cutoff")
int deleteInactive(@Param("cutoff") LocalDate cutoff);
```

---

### Native Queries — When JPQL Isn't Enough

**Why:** Use native SQL when you need DB-specific features: window functions, CTEs, JSON, full-text search, or when JPQL produces inefficient SQL.

```java
@Query(value = """
    WITH monthly_stats AS (
        SELECT
            DATE_TRUNC('month', created_at) AS month,
            COUNT(*) AS order_count,
            SUM(total_amount) AS revenue
        FROM orders
        WHERE created_at >= :startDate
        GROUP BY DATE_TRUNC('month', created_at)
    )
    SELECT * FROM monthly_stats ORDER BY month DESC
    """, nativeQuery = true)
List<Object[]> getMonthlyReport(@Param("startDate") LocalDate startDate);
```

**When to use:** Complex analytics, window functions, CTEs, DB-specific features.  
**When not to use:** Simple CRUD or when database portability matters.

---

### Index Optimization

**Why:** Without indexes, the database does a full table scan (read every row). With indexes, it can jump directly to matching rows.

#### B-Tree index (typical default)

A B-Tree index is like a sorted tree: the DB navigates from root to leaf to find values in O(log n) steps instead of scanning all rows.

```
Without index: Scan 10,000,000 rows → seconds
With B-Tree index: ~log2(10,000,000) ≈ 24 steps → milliseconds
```

#### Where to add indexes

```sql
-- WHERE columns
CREATE INDEX idx_users_email ON users(email);

-- Foreign keys used in JOINs
CREATE INDEX idx_orders_user_id ON orders(user_id);

-- ORDER BY columns (for sorts)
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- Composite index — column order matters (most selective first)
CREATE INDEX idx_orders_status_date ON orders(status, created_at);
-- Good for: WHERE status = 'PENDING' AND created_at > '2024-01-01'
```

**JPA-defined indexes:**

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_status_created", columnList = "status, created_at")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    private LocalDateTime createdAt;
}
```

**Trade-off:** Indexes speed reads but slow writes (INSERT/UPDATE/DELETE) because indexes must be updated. Index only columns you filter, join, or sort on.

---

### EXPLAIN ANALYZE — See How the Database Executes

**Why:** EXPLAIN ANALYZE shows the real execution plan and timings so you can see full table scans, missing indexes, and costly operations.

```sql
-- PostgreSQL
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'alice@example.com';
```

**Without index (Seq Scan):**

```
Seq Scan on users (cost=0.00..195234.00 rows=1 width=120)
  Filter: (email = 'alice@example.com')
  Rows Removed by Filter: 9999999
  Execution Time: 847.233 ms       ← SLOW
```

**With index (Index Scan):**

```
Index Scan using idx_user_email on users (cost=0.43..8.45 rows=1 width=120)
  Index Cond: (email = 'alice@example.com')
  Execution Time: 0.031 ms         ← ~27,000× faster
```

**Terms to look for:**

| Term | Meaning | Typical action |
|------|---------|----------------|
| Seq Scan | Full table scan | Add index |
| Index Scan | Uses index | Usually fine |
| Index Only Scan | All data from index | Best when possible |
| Nested Loop | Loop join | OK for small sets |
| Hash Join | Hash-based join | Often good for large sets |
| Sort | In-memory sort | Consider index on ORDER BY |

---

## Layer 3 — Advanced Engineering Depth

---

### Read Replicas — Scaling Reads

**When:** Read-heavy workloads overwhelm a single database. Splitting reads to replicas spreads the load.

```
                    ┌──────────────────┐
                    │   Application    │
                    └────────┬─────────┘
                             │
              ┌──────────────┴──────────────┐
         WRITES                         READS
              │                              │
              ▼                    ┌────────┴────────┐
    ┌──────────────┐                ▼                 ▼
    │   PRIMARY    │ ──sync──▶ REPLICA 1      REPLICA 2
    │  (Master)    │           (Read-only)    (Read-only)
    └──────────────┘
```

**Implementation with AbstractRoutingDataSource:**

```java
// Route based on transaction type
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? "replica" : "primary";
    }
}

// Usage: read-only methods go to replica
@Transactional(readOnly = true)
public User getUser(Long id) {
    return userRepository.findById(id).orElseThrow();
}

@Transactional  // Writes go to primary
public User createUser(UserRequest req) {
    return userRepository.save(new User(req));
}
```

**Replication lag:** Replicas can be slightly behind. For read-after-write consistency, read from the primary right after a write.

---

### Caching Layers

| Layer | Scope | Automatic? | Typical use |
|-------|--------|-----------|-------------|
| **First-Level** | Single Hibernate session/transaction | Yes | Avoid duplicate loads in same transaction |
| **Second-Level** | Application-wide (e.g. Ehcache, Infinispan) | No (config) | Shared entity cache |
| **Query Cache** | Per query | No (opt-in) | Specific read-heavy queries |
| **Application** | Service methods (@Cacheable) | No | Method result caching |

**First-Level Cache (automatic):**

```java
@Transactional
public void demo() {
    User u1 = userRepository.findById(1L).orElseThrow();  // DB hit
    User u2 = userRepository.findById(1L).orElseThrow();  // From cache, no DB
    assert u1 == u2;  // Same instance
}
```

**Application cache with @Cacheable:**

```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {}
}
```

**Redis backend:**

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour
  redis:
    host: localhost
    port: 6379
```

**When to cache:** Read-heavy, rarely changing data.  
**When not to cache:** Frequently updated data, user-specific data, data that must be real-time.

---

### Advanced Index Strategies

**Composite index column order:** Put the most selective column first. An index `(status, created_at)` helps `WHERE status = ? AND created_at > ?`; `(created_at, status)` is less useful for that pattern.

**Partial indexes (e.g. PostgreSQL):**

```sql
-- Index only active users
CREATE INDEX idx_users_active_email ON users(email) WHERE status = 'ACTIVE';
```

**Covering index:** Include all columns needed by the query so the DB can satisfy it from the index alone (Index Only Scan).

---

### Materialized Views for Analytics

```sql
CREATE MATERIALIZED VIEW daily_revenue AS
SELECT
    DATE(created_at) AS sale_date,
    COUNT(*) AS order_count,
    SUM(total_amount) AS daily_revenue
FROM orders
GROUP BY DATE(created_at);

-- Refresh on schedule (e.g. cron)
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_revenue;
```

| | View | Materialized View |
|---|------|-------------------|
| Stored | No (computed each time) | Yes (on disk) |
| Speed | Slower | Faster |
| Freshness | Always current | Stale until refreshed |

---

## Layer 4 — Interview Mastery

### Q1: How do you optimize database queries?

**Answer:** Measure first with query logging and Hibernate stats. Then:

1. Fix N+1 (JOIN FETCH, @EntityGraph, @BatchSize).
2. Add indexes on WHERE, JOIN, ORDER BY — avoid over-indexing.
3. Use projections instead of loading full entities.
4. Paginate instead of loading everything.
5. Use EXISTS instead of COUNT when checking existence.
6. Batch inserts/updates (batch_size, bulk JPQL).
7. Cache read-heavy data with @Cacheable.
8. Consider read replicas for read-heavy workloads.

---

### Q2: Why is a smaller connection pool often better?

**Answer:** The database has limited CPU. With 4 cores it can run ~4 queries in parallel. Extra connections add context switching, memory use, and lock contention. A pool of 10 can outperform 100. Common formula: `(CPU_cores × 2) + disk_spindles`, e.g. ~10 for a 4-core SSD server.

---

### Q3: Explain caching levels in a Spring/Hibernate app.

**Answer:**

- **First-Level:** Per-session, automatic. Same entity loaded twice in one transaction returns the same instance.
- **Second-Level:** Shared across transactions. Requires provider (Ehcache, Infinispan); entities marked with @Cache.
- **Query Cache:** Caches query results; invalidated when involved entities change.
- **Application Cache:** @Cacheable on service methods, backed by Redis/Caffeine. Most flexible for method-level caching.

---

### Q4: How would you implement read replicas?

**Answer:** Use `AbstractRoutingDataSource`. Configure primary and replica `HikariDataSource` beans. The router checks `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` — read-only → replica, otherwise → primary. Use `@Transactional(readOnly = true)` for read methods. Handle replication lag (e.g. read-your-own-writes for critical paths).

---

### Q5: App is slow under load — how do you diagnose?

**Answer:** Check connection pool first (e.g. `hikaricp.connections.pending`). Enable slow query logging. Look for N+1 (collection fetch count). Run EXPLAIN ANALYZE on slow queries. Check for missing indexes (e.g. `seq_scan` stats). Review batching for bulk ops. Consider caching and read replicas. Most issues come from N+1 and missing indexes.

---

## Summary

```
┌─────────────────────────────────────────────────────────────────┐
│            QUERY OPTIMIZATION — KEY TAKEAWAYS                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. CONNECTION POOLING — Use HikariCP, size ~10, monitor it       │
│  2. SMALLER pools can be FASTER — avoid 100+ connections        │
│  3. FETCH ONLY WHAT YOU NEED — projections, not SELECT *        │
│  4. INDEX wisely — WHERE, JOIN, ORDER BY; avoid over-indexing    │
│  5. BATCH bulk operations — 50x faster than one-by-one           │
│  6. PAGINATE — never load all rows into memory                   │
│  7. EXISTS for existence checks — not COUNT                     │
│  8. CACHE read-heavy data — @Cacheable + Redis                   │
│  9. READ REPLICAS for read-heavy apps                            │
│ 10. MEASURE FIRST — EXPLAIN, stats, slow query logs              │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

[← Back to Index](00-README.md) | [Previous: N+1 Problem](27-N+1-Problem.md) | [Next: Spring Security →](29-Spring-Security.md)
