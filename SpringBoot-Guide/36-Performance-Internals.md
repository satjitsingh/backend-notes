# Chapter 36: Performance & Server Internals

[← Back to Index](00-README.md) | [Previous: Testing Spring](35-Testing-Spring.md) | [Next: Docker & Deployment →](37-Docker-Deployment.md)

---

## Why This Chapter Matters

When your app runs slowly or crashes under load, you need to know what’s going on inside. This chapter explains how Spring Boot’s built‑in web server works, how it handles many people at once, and how to make it run faster and use less memory. You’ll learn why sometimes your app feels sluggish, how to fix it, and how to measure and improve performance in real apps.

---

## Layer 1 — Intuition Builder

### Part 1: The Restaurant That Serves the Internet

Imagine a restaurant. A **web server** is like that restaurant: it’s the place where requests (customers) come in and get responses (food). Your Spring Boot app *contains* a small restaurant inside it — you don’t need a separate building. That’s what we mean by an **embedded server** (a server that lives inside your app).

Spring Boot can use three different “restaurant styles” (built‑in servers):

- **Tomcat** — The most popular. Reliable, well‑tested, used everywhere.
- **Jetty** — Smaller and lighter. Good when you want a tiny app that starts fast.
- **Undertow** — Very fast, uses less memory. Good when you care a lot about speed and low memory.

The important idea: your application *is* the server. There’s no separate deployment step. Your app starts, and the server starts with it.

---

### Part 2: Waiters and the Kitchen — How One Request Gets Served

Think of your server as a restaurant again:

- The **kitchen** is the database or any slow service.
- **Waiters** are like **threads** — the workers that handle each request.
- Each waiter takes one customer’s order, goes to the kitchen, waits, brings the food back, and then takes the next customer.

That’s the **thread‑per‑request model**: one waiter (thread) for one customer (request) until the order is done.

Flow looks like this:

```
   Customer (Request)    →    Waiter (Thread)    →    Kitchen (Database)
                                      ↓
                              Waiter waits at kitchen
                                      ↓
   Food (Response)       ←    Waiter brings food   ←    Kitchen finishes
                                      ↓
                              Waiter goes back to pool
```

So:

1. A request comes in.
2. A waiter (worker thread) is assigned from the pool.
3. The waiter takes the order, waits at the kitchen (blocking), and brings the food back.
4. When done, the waiter returns to the pool for the next customer.

**Limitation**: If you have 200 waiters (threads), you can serve at most about 200 customers at the same time. If more come, they must wait in line.

---

### Part 3: Blocking — The Waiter Stuck at the Kitchen

When a waiter stands at the kitchen window and does nothing else until the food is ready, we say the waiter is **blocking** (waiting, doing nothing useful). The waiter could be serving other tables, but instead they’re idle. In code, **blocking** means a thread sits and waits until something (like a database or another service) finishes. While it waits, that thread can’t do any other work.

---

### Part 4: Non-Blocking — The Smart Waiter

**Non‑blocking** means the waiter doesn’t just stand at the kitchen. They take the order, tell the kitchen “call me when it’s ready,” and go serve other tables. When the kitchen is done, the waiter comes back and delivers the food. One waiter can serve many customers at once because they’re not stuck waiting. In code, **non‑blocking** means the thread hands off the work and is free to do something else until the result is ready.

---

### Part 5: The Janitor Who Cleans Up — Garbage Collection

Your app constantly creates and discards objects (like plates, napkins, trash). A **garbage collector (GC)** is like a janitor who walks around and clears tables: they pick up what’s no longer needed and free space so the restaurant can keep running. GC runs automatically in the background. If the janitor is too slow or too busy, the restaurant (your app) can slow down or even freeze for a moment. That’s why GC matters for performance.

---

### Part 6: Compressing the Food to Save Space

**Compression** is like squeezing a big sandwich into a smaller lunch box before sending it to the customer. The sandwich is the same, but it travels faster and uses less space (bandwidth). Your server can compress responses so they’re smaller over the network — great for slower connections and mobile users.

---

### Part 7: The Event Loop — One Host Managing Many Rooms

Instead of one waiter per table, picture one host who runs between rooms. The host doesn’t wait in any room; they check “Room 1: need anything? Room 2: need anything?” and keep moving. When Room 1’s food arrives, they deliver it and move on. This is the **event loop** model: a small number of workers handling many connections by never standing still and waiting. Reactive programming (WebFlux) uses this idea.

---

### Part 8: Virtual Waiters — Super Lightweight Workers (Java 21+)

Regular threads are like full‑time waiters: expensive to hire and each needs their own space. **Virtual threads** (in Java 21+) are like temporary helpers: very cheap, you can have millions of them, and they can still “block” (wait at the kitchen) without hurting performance, because they’re so lightweight. You write normal, blocking‑style code, but the runtime makes it efficient.

---

## Layer 2 — Professional Developer

### Embedded Servers Deep Dive

Spring Boot chooses an embedded servlet container so your app can serve HTTP without deploying a separate application server. Understanding the three options helps you tune and switch when needed.

#### Server Comparison

| Feature | Tomcat | Jetty | Undertow |
|---------|--------|-------|----------|
| **Default in Spring Boot** | Yes | No | No |
| **Memory footprint** | Medium | Low | Lowest |
| **Startup time** | Moderate | Fast | Fastest |
| **Best for** | General apps, stability | Microservices, lightweight | High throughput, low memory |
| **Maturity** | Very mature | Mature | Newer, fast adoption |

#### Basic Server Configuration

```yaml
# application.yml — Basic server settings
server:
  port: 8080
  address: 0.0.0.0          # Listen on all network interfaces
  servlet:
    context-path: /api      # All URLs start with /api
    session:
      timeout: 30m          # Session expires after 30 minutes
      cookie:
        secure: true        # HTTPS only in production
        http-only: true     # Not accessible via JavaScript (security)
```

---

### Thread-Per-Request Model — How It Really Works

Traditional Spring MVC uses a thread pool. Each incoming request gets a worker thread from the pool. That thread runs your controller, calls services, and sends the response. When done, the thread goes back to the pool.

#### Request Lifecycle (ASCII)

```
                    ┌─────────────────────────────────────────────────┐
                    │                 YOUR APPLICATION                  │
                    └─────────────────────────────────────────────────┘
                                          │
    HTTP Request ─────────────────────────┼─────────────────────────────►
                                          │
                                          ▼
                    ┌──────────────────────────────────────┐
                    │     Server Socket (Acceptor Thread)   │  ← Accepts new connections
                    └──────────────────────────────────────┘
                                          │
                                          ▼
                    ┌──────────────────────────────────────┐
                    │     Thread Pool (Worker Threads)      │  ← Picks a free thread
                    │     [Thread 1] [Thread 2] [Thread 3]  │
                    └──────────────────────────────────────┘
                                          │
                                          ▼
                    ┌──────────────────────────────────────┐
                    │         DispatcherServlet            │  ← Routes to correct controller
                    └──────────────────────────────────────┘
                                          │
                                          ▼
                    ┌──────────────────────────────────────┐
                    │         Controller Method            │  ← Your @GetMapping, etc.
                    └──────────────────────────────────────┘
                                          │
                                          ▼
                    ┌──────────────────────────────────────┐
                    │    Service / Database / External API  │  ← Thread may BLOCK here
                    └──────────────────────────────────────┘
                                          │
                                          ▼
    ◄────────────────────────────────────┼────────────────────────────── HTTP Response
                                          │
                    Thread returns to pool (ready for next request)
```

#### Why Thread Count Matters

Each thread uses around 1 MB of stack memory. With 200 threads, that’s ~200 MB just for stacks. Too many threads increase memory use and context‑switching overhead. Too few, and requests queue and latency goes up. You tune the pool based on CPU, I/O, and memory.

---

### Thread Pool Configuration — Tomcat

```yaml
# application.yml — Tomcat thread pool tuning
server:
  tomcat:
    threads:
      max: 200              # Max worker threads (limits concurrent requests)
      min-spare: 10          # Minimum idle threads (faster first response)
    max-connections: 10000   # Max TCP connections (threads + queued)
    accept-count: 100        # Queue size when all threads are busy
    connection-timeout: 20000   # Milliseconds to wait for request data
    keep-alive-timeout: 60000   # Keep-alive timeout for connections
    max-keep-alive-requests: 100
```

**Parameter meanings:**

- **max-threads**: Maximum concurrent requests the server can process.
- **min-spare**: Minimum idle threads to avoid cold‑start latency.
- **max-connections**: Total connections (in flight + queued).
- **accept-count**: How many connections can wait when all threads are busy.
- **connection-timeout**: How long to wait for the client to send the request.

#### Tuning Examples

```yaml
# High-traffic application (many concurrent users)
server:
  tomcat:
    threads:
      max: 500
      min-spare: 50
    max-connections: 20000
    accept-count: 200

# Low-memory environment (constrained servers)
server:
  tomcat:
    threads:
      max: 50
      min-spare: 5
    max-connections: 1000
    accept-count: 50
```

#### Programmatic Tomcat Customization

```java
@Configuration
public class TomcatConfig {

    /**
     * Customize Tomcat connector before the server starts.
     * Use when YAML is not flexible enough.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> {
                ProtocolHandler handler = connector.getProtocolHandler();
                if (handler instanceof AbstractProtocol) {
                    AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
                    protocol.setMaxThreads(200);
                    protocol.setMinSpareThreads(10);
                    protocol.setAcceptCount(100);
                    protocol.setConnectionTimeout(20000);
                }
            });
        };
    }
}
```

---

### Jetty and Undertow Configuration

```yaml
# Jetty — different parameter names
server:
  jetty:
    threads:
      max: 200
      min: 10
    acceptors: 1    # Threads accepting connections
    selectors: 4    # Threads handling I/O

# Undertow — io vs worker threads
server:
  undertow:
    threads:
      io: 4       # I/O threads (often 1–2 per CPU core)
      worker: 64  # Worker threads for request processing
    buffer-size: 1024
    direct-buffers: true   # Use direct memory for buffers (faster)
```

---

### Async Processing — Releasing the Request Thread

When a request does slow work (database, external API), the request thread sits idle. **Async processing** frees that thread so it can handle other requests while the slow work happens elsewhere.

#### Option 1: Callable

The controller returns a `Callable`. Spring runs it on a different thread and sends the result when it completes.

```java
@RestController
public class AsyncController {

    /**
     * Returns a Callable — Spring releases the request thread immediately.
     * The task runs on a separate thread; response is sent when it completes.
     */
    @GetMapping("/async")
    public Callable<String> asyncEndpoint() {
        return () -> {
            // Simulates slow work (e.g., database, external API)
            Thread.sleep(1000);
            return "Done after 1 second";
        };
    }

    @GetMapping("/async-data")
    public Callable<ResponseEntity<User>> asyncData() {
        return () -> {
            User user = userService.findUserById(1L);
            return ResponseEntity.ok(user);
        };
    }
}
```

#### Option 2: DeferredResult

Use when the result comes from somewhere else later (another thread, message, callback, long polling). You hold a `DeferredResult` and set it when ready.

```java
@RestController
public class DeferredController {

    // Store pending results by request ID (e.g., for long polling)
    private final Map<String, DeferredResult<String>> deferredResults = new ConcurrentHashMap<>();

    @GetMapping("/deferred/{id}")
    public DeferredResult<String> deferredEndpoint(@PathVariable String id) {
        DeferredResult<String> result = new DeferredResult<>(5000L);  // 5s timeout
        result.onTimeout(() -> result.setErrorResult("Timeout"));
        result.onCompletion(() -> deferredResults.remove(id));

        deferredResults.put(id, result);
        asyncService.process(id, result);  // Some async process will call setResult later
        return result;
    }

    @PostMapping("/complete/{id}")
    public ResponseEntity<String> complete(@PathVariable String id, @RequestBody String data) {
        DeferredResult<String> result = deferredResults.remove(id);
        if (result != null) {
            result.setResult(data);
            return ResponseEntity.ok("Completed");
        }
        return ResponseEntity.notFound().build();
    }
}
```

#### Option 3: @Async and CompletableFuture

Run service logic asynchronously and return a `CompletableFuture`. Callers can chain or block on it.

```java
@Service
public class AsyncService {

    /**
     * @Async runs this method on a separate thread pool.
     * Returns CompletableFuture so callers can chain or await.
     */
    @Async
    public CompletableFuture<String> processAsync(String data) {
        try {
            Thread.sleep(2000);
            return CompletableFuture.completedFuture("Processed: " + data);
        } catch (InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<List<User>> fetchUsersAsync() {
        List<User> users = userRepository.findAll();
        return CompletableFuture.completedFuture(users);
    }
}

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("Async method {} failed", method.getName(), ex);
    }
}
```

---

### Response Compression

Compression shrinks responses (JSON, HTML, etc.) before sending. Less data = faster transfer and lower bandwidth.

```yaml
server:
  compression:
    enabled: true
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
      - text/css
      - text/javascript
      - application/javascript
    min-response-size: 1024   # Only compress responses larger than 1 KB
```

---

### Reactive Basics — WebFlux, Mono, Flux

Reactive programming changes the model: instead of one thread per request, a small number of threads use non‑blocking I/O and handle many connections. Spring WebFlux provides this.

#### Blocking vs Non-Blocking Overview

| Aspect | Blocking (MVC) | Non-Blocking (WebFlux) |
|--------|----------------|------------------------|
| **Thread per request** | Yes | No |
| **Thread waits for I/O** | Yes | No — thread is released |
| **Typical concurrency** | Hundreds | Thousands+ |
| **Programming style** | Imperative | Declarative, reactive |
| **Server** | Tomcat, Jetty, Undertow | Netty |
| **Use case** | CRUD, traditional | High concurrency, streaming |

#### Event Loop vs Thread-Per-Request (ASCII)

```
THREAD-PER-REQUEST (Blocking):
┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐
│ T1  │    │ T2  │    │ T3  │    │ T4  │   Each thread = 1 request
│ Req1│    │ Req2│    │ Req3│    │ Req4│   Thread WAITS during I/O
└──┬──┘    └──┬──┘    └──┬──┘    └──┬──┘
   │          │          │          │
   └──────────┴──────────┴──────────┘
              │
              ▼
      [Database / I/O]
   (Threads blocked here)

EVENT LOOP (Non-Blocking):
┌─────────────────────────────────────┐
│  Event Loop Thread 1  │  Thread 2   │   Few threads
└───────────┬───────────┴──────┬──────┘
            │                  │
   Req1 ────┤ Req2 ──── Req3 ──┤ Req4 ─── Req5 ─── ...
            │                  │
   (No waiting: register callback, move to next)
            │                  │
            ▼                  ▼
      [Database / I/O] — when done, callback runs
```

#### Mono and Flux

- **Mono&lt;T&gt;** — Produces 0 or 1 value (like `Optional` for async).
- **Flux&lt;T&gt;** — Produces 0 to N values (like a stream).

```java
@RestController
@RequestMapping("/reactive")
public class ReactiveController {

    private final ReactiveUserRepository userRepository;

    // Returns many users (stream)
    @GetMapping("/users")
    public Flux<User> getAllUsers() {
        return userRepository.findAll()
                .delayElements(Duration.ofMillis(100))
                .log();
    }

    // Returns one user or empty
    @GetMapping("/users/{id}")
    public Mono<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException()));
    }

    // Server-Sent Events — stream users one by one
    @GetMapping(value = "/users/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<User> streamUsers() {
        return userRepository.findAll()
                .delayElements(Duration.ofSeconds(1));
    }

    @PostMapping("/users")
    public Mono<ResponseEntity<User>> createUser(@RequestBody Mono<User> userMono) {
        return userRepository.save(userMono)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }
}
```

---

### Connection Pooling (Database)

Reusing database connections avoids the cost of creating new ones for each request. HikariCP is the default pool in Spring Boot.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

## Layer 3 — Advanced Engineering Depth

### Virtual Threads (Java 21+)

Platform threads are OS threads: costly (~1 MB stack), limited in number. Virtual threads are JVM-managed, lightweight (KB-scale), and can block without the usual performance penalty. You keep blocking-style code but get better scalability.

#### Enable Virtual Threads in Spring Boot

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

#### Use Virtual Threads for Tomcat and @Async

```java
@Configuration
public class VirtualThreadConfig {

    /**
     * Tomcat uses virtual threads for request processing.
     * Blocking in controllers no longer blocks a platform thread.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(
                Executors.newVirtualThreadPerTaskExecutor());
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

Benefits: write normal blocking code; scale to many concurrent tasks; normal stack traces and debugging.

---

### JVM Memory — Heap, Metaspace, Direct Memory

```
┌─────────────────────────────────────────────────────────────┐
│                     JVM MEMORY LAYOUT                        │
├─────────────────────────────────────────────────────────────┤
│  HEAP (objects you create)                                   │
│  ├── Young Generation (Eden + Survivor) — short-lived        │
│  └── Old Generation — long-lived                             │
├─────────────────────────────────────────────────────────────┤
│  NON-HEAP                                                    │
│  ├── Metaspace — class metadata, method info                 │
│  ├── Code Cache — JIT-compiled code                          │
│  └── Direct Memory — NIO buffers (important for Netty)       │
└─────────────────────────────────────────────────────────────┘
```

#### Heap and Metaspace Sizing

```bash
# Development
java -Xms512m -Xmx1g -jar app.jar

# Production
java -Xms2g -Xmx4g -XX:+UseG1GC -jar app.jar

# Limit Metaspace (class metadata)
-XX:MaxMetaspaceSize=256m

# For WebFlux/Netty (direct buffers)
-XX:MaxDirectMemorySize=512m
```

---

### Garbage Collector Tuning

| GC | Use Case | Pause Time | Throughput |
|----|----------|------------|------------|
| **G1GC** | General purpose | Low | Good |
| **ZGC** | Large heaps, low pause | Very low | Good |
| **Parallel GC** | Batch, high throughput | Higher | Best |

```bash
# G1GC (recommended for most apps)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m

# ZGC (Java 21+ production-ready)
-XX:+UseZGC

# Parallel GC (throughput-first)
-XX:+UseParallelGC
```

---

### Profiling Tools

| Tool | What It Does |
|------|----------------|
| **JFR** | Low-overhead profiling, GC, CPU, allocations |
| **async-profiler** | CPU and allocation profiling, minimal impact |
| **jstack** | Thread dumps (deadlocks, stuck threads) |
| **jmap** | Heap dumps, heap summary |
| **jstat** | GC and memory stats over time |

#### JFR (Java Flight Recorder)

```bash
# One-time 60-second recording
java -XX:+FlightRecorder \
     -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -jar app.jar

# Continuous recording (analyze with JDK Mission Control)
java -XX:+FlightRecorder \
     -XX:StartFlightRecording=continuous=true \
     -jar app.jar
```

#### Async Profiler

```bash
# CPU profiling for 60 seconds
./profiler.sh -e cpu -d 60 -f profile.html <pid>

# Allocation profiling
./profiler.sh -e alloc -d 60 -f alloc.html <pid>
```

#### jstack and jmap

```bash
# Thread dump (for deadlocks, blocked threads)
jstack <pid> > thread-dump.txt

# Heap dump
jmap -dump:live,format=b,file=heap.hprof <pid>

# Heap summary
jmap -heap <pid>
```

---

### Back-Pressure in Reactive Streams

When a producer sends data faster than the consumer can process, you need **back-pressure**: the consumer signals how much it can handle, and the producer slows down. Reactor/WebFlux supports this natively.

```java
Flux.range(1, 1_000_000)
    .limitRate(100)  // Request 100 elements at a time
    .subscribe(data -> process(data));
```

---

### Caching for Performance

#### HTTP Caching

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
            .eTag(String.valueOf(user.getVersion()))
            .body(user);
}
```

#### Application-Level Caching (Caffeine)

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}

@Service
public class UserService {

    @Cacheable("users")
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    @CacheEvict(value = "users", key = "#id")
    public void updateUser(Long id, User user) {
        userRepository.save(user);
    }
}
```

---

## Layer 4 — Interview Mastery

### Q1: How does Tomcat handle requests?

**Answer:** Tomcat uses a thread-per-request model with a pool. An acceptor thread accepts connections; worker threads from the pool handle each request. When all workers are busy, new connections wait in a queue (accept-count). Key parameters: max-threads, min-spare, accept-count, connection-timeout.

---

### Q2: How do you tune the thread pool?

**Answer:** Start from load, CPU cores, and memory. I/O-bound work can use more threads; CPU-bound work stays closer to core count. Each thread ~1 MB stack. Tune via `server.tomcat.threads.max`, `min-spare`, `max-connections`, and `accept-count`. Load test and monitor with Actuator metrics.

---

### Q3: Spring MVC vs WebFlux?

**Answer:** MVC is thread-per-request and blocking; WebFlux uses an event loop and non-blocking I/O. MVC fits typical CRUD; WebFlux fits high concurrency, streaming, and reactive backends. MVC is simpler and more familiar; WebFlux scales to many more concurrent connections but needs a reactive stack (drivers, clients, etc.).

---

### Q4: What are virtual threads?

**Answer:** Virtual threads (Java 21+) are lightweight, JVM-managed threads. You can create millions of them. Blocking no longer blocks a platform thread, so you keep imperative/blocking code and get better scalability. Enable in Spring Boot with `spring.threads.virtual.enabled: true` or by using `Executors.newVirtualThreadPerTaskExecutor()` for Tomcat/async.

---

### Q5: How do you profile a Spring Boot application?

**Answer:** Use Actuator metrics for HTTP and custom metrics; JFR for low-overhead profiling (CPU, GC, allocations); async-profiler for CPU and allocation; jstack for thread dumps; jmap for heap dumps. Profile CPU hotspots, memory usage, GC pauses, and thread contention.

---

### Q6: How does connection pooling work?

**Answer:** A pool (e.g., HikariCP) keeps a set of database connections. Requests borrow a connection, use it, and return it instead of creating a new one. Configure `maximum-pool-size`, `minimum-idle`, `connection-timeout`, `idle-timeout`, and `max-lifetime`. The pool handles validation and recycling.

---

### Q7: Explain async processing in Spring MVC.

**Answer:** Three main options: (1) **Callable** — controller returns a Callable, Spring runs it on another thread and sends the result; (2) **DeferredResult** — for long polling or external callbacks, set the result later from another thread; (3) **@Async** — service methods run on a custom Executor and return CompletableFuture. All three free the request thread during slow work.

---

### Q8: What is back-pressure in reactive streams?

**Answer:** Back-pressure is flow control: the consumer requests how much data it can handle, and the producer respects that limit. Without it, a fast producer can overload a slow consumer and cause memory issues. Reactor’s `Flux`/`Mono` support back-pressure through the reactive streams spec.

---

## Summary

1. **Embedded servers** — Tomcat, Jetty, Undertow run inside your app.
2. **Thread-per-request** — One worker thread per request; blocked during I/O.
3. **Async** — Callable, DeferredResult, and @Async free the request thread.
4. **Compression** — Reduce response size for faster transfer and less bandwidth.
5. **WebFlux** — Event loop, non-blocking I/O, Mono/Flux for high concurrency.
6. **Virtual threads** — Lightweight threads in Java 21+; block without blocking platform threads.
7. **JVM memory** — Heap (objects), Metaspace (classes), direct memory (NIO).
8. **GC** — G1GC for most apps; ZGC for low-latency, large heaps.
9. **Profiling** — JFR, async-profiler, jstack, jmap for performance analysis.
10. **Tuning** — Match thread pools, connection pools, and GC to your load and hardware.

---

## Next Steps

- [Chapter 37: Docker & Deployment](37-Docker-Deployment.md)
- Experiment with thread pool tuning and load testing
- Try WebFlux for a high-concurrency or streaming endpoint
- Enable and use virtual threads in a Java 21+ app
- Set up Actuator, JFR, or async-profiler for performance monitoring

---

[← Back to Index](00-README.md) | [Previous: Testing Spring](35-Testing-Spring.md) | [Next: Docker & Deployment →](37-Docker-Deployment.md)
