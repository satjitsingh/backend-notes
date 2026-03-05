# Chapter 32: Logging & Monitoring

[← Back to Index](00-README.md) | [Previous: Caching](31-Caching.md) | [Next: Microservices →](33-Microservices.md)

---

## Why This Chapter Matters

Here's a truth that surprises many beginners: **you cannot use breakpoints in production**. You can't attach a debugger to a server handling thousands of requests per second. You can't step through code line by line while real users are waiting.

So how do you figure out what went wrong when a customer says "my payment failed" at 3 AM? **Logs and monitoring.** They are your only eyes and ears in production.

Every single production incident starts with someone looking at logs. Every on-call engineer's first instinct is to check dashboards and metrics. If your logging is bad, you're blind. If your monitoring is missing, you're deaf.

**This chapter matters for interviews too.** Senior engineers are expected to design logging strategies, set up monitoring, and debug production issues. "How would you investigate a production outage?" is one of the most common system design interview questions.

**Without proper logging and monitoring:**
- A customer reports "something broke" and you have NO idea what happened
- A performance problem silently gets worse for weeks until everything crashes
- A security breach goes undetected for months
- You waste hours guessing instead of minutes investigating
- Your team has no confidence deploying to production

**With proper logging and monitoring:**
- You can reconstruct exactly what happened for any request
- Performance problems trigger alerts BEFORE users notice
- Security incidents are detected and responded to in real-time
- Debugging production issues takes minutes instead of days
- Your team deploys with confidence because they can see what's happening

---

## Layer 1 — Intuition Builder

### Part 1: What Is Logging? — The Security Camera Analogy

**Logging is like installing security cameras in your application.**

Imagine a store with cameras everywhere:
- Nobody watches them all day (that would be boring and impossible)
- But when something goes wrong — a theft, a customer complaint, a broken machine — you **rewind the footage** and see exactly what happened, when, and who was involved

Your application does the same thing. It writes little notes (logs) as things happen:
- "10:30:45 — User John logged in"
- "10:30:47 — John asked to see his order #12345"
- "10:30:48 — Uh oh! The payment system didn't respond in time"

**Without logs:** "Something broke. No idea what. Good luck finding it."
**With logs:** "At 10:30:48, order #12345 failed because the payment system took too long to answer. The user was John. Here's the exact error message."

The word **log** comes from the old sailing practice of keeping a "ship's log" — a book where sailors wrote down everything that happened during a voyage. If something went wrong, they could look back and figure out why. Your application does the same thing, but with a file instead of a book.

---

### Part 2: What Is Monitoring? — The Car Dashboard Analogy

**Monitoring is like the gauges on a car dashboard.**

You don't stare at your car's gauges every second. But you glance at them:
- **Speedometer** — How fast are we going? (How fast is the app responding?)
- **Fuel gauge** — Do we have enough gas? (Is there enough memory or storage?)
- **Engine light** — Is something wrong? (Are there errors?)
- **Temperature gauge** — Is the engine overheating? (Is the database overloaded?)

When a warning light turns on, you know to pull over and investigate. Same with your application: when a metric goes red (error rate too high, response time too slow), someone gets alerted to investigate.

```
┌─────────────────────────────────────────────────────────────────┐
│              YOUR APPLICATION DASHBOARD                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Response Time:    █████████░░░  45ms (Good ✅)                  │
│  Error Rate:       █░░░░░░░░░░  0.1% (Good ✅)                  │
│  CPU Usage:        ████████░░░  75%  (Warning ⚠️)               │
│  Memory:           ██████░░░░░  55%  (Good ✅)                  │
│  DB Connections:   █████████░░  90%  (Danger 🔴)                │
│  Requests/sec:     ████████░░░  450  (Normal ✅)                │
│                                                                   │
│  🔔 ALERT: Database connection pool at 90% capacity!            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

**Key difference:**
- **Logs** answer: "**What** happened?" (like rewinding security footage)
- **Monitoring** answers: "**How** is it doing right now?" (like looking at your dashboard)

---

### Part 3: Why Can't We Just Debug in Production?

**You can't use a debugger in production.** Here's why, in kid terms:

Imagine you're playing a video game with 10,000 other people at once. If someone paused the game every time THEY wanted to look around, everyone else would freeze. That would be terrible!

Production is like that. Your app handles thousands of requests per second. If you attached a debugger and stepped through the code one line at a time, you'd freeze the entire application. Real users would see "Loading..." forever.

So we use **logs** (the recording) and **monitoring** (the live gauges) instead. We record what happens so we can look at it later, without stopping anything.

---

### Part 4: Log Levels — The Hospital Triage Analogy

**Not all messages are equally important.** A hospital uses **triage** to prioritize: a tiny scratch can wait; a heart attack cannot.

Logs use levels too. Some messages are "just FYI" (INFO). Others mean "something might be wrong" (WARN). Others mean "something broke!" (ERROR).

| Level   | Kid-Friendly Meaning         | When to Use It                          |
|---------|------------------------------|-----------------------------------------|
| TRACE   | "I'm telling you every tiny blink" | Extreme detail. Only during deep debugging. Rarely used. |
| DEBUG   | "Here's extra detail for developers" | Step-by-step what the code is doing. Usually off in production. |
| INFO    | "Something important happened" | Normal business events: "User logged in", "Order created". |
| WARN    | "Heads up — something weird but not broken" | Slow query, retry attempt, cache miss. Worth looking into. |
| ERROR   | "Something broke!"           | Payment failed, email didn't send. App is still running but this failed. |
| FATAL   | "The app is dying!"          | Out of memory, can't connect to database at startup. Rare. |

**The rule:** When you set a level, you see that level AND everything more serious.
- Set to **INFO** → you see INFO, WARN, ERROR, FATAL (standard for production)
- Set to **DEBUG** → you see DEBUG, INFO, WARN, ERROR, FATAL (lots of noise, use temporarily)
- Set to **WARN** → you see WARN, ERROR, FATAL (quiet mode — only problems)

---

### Part 5: The Three Pillars — Package Delivery Analogy

To truly understand your application, you need three types of information:

| Pillar   | Analogy                     | What It Answers                    |
|----------|-----------------------------|------------------------------------|
| **Logs** | Delivery notes at each stop | "What happened?" — "Package arrived at warehouse at 2 PM" |
| **Metrics** | Company performance report | "How are we doing?" — "99.5% on-time, average 2-day delivery" |
| **Traces** | Package tracking number    | "Where did it go?" — "Warehouse → Truck → Sorting → Your door" |

**Logs** = The security camera footage. What happened, when, and why.
**Metrics** = The dashboard gauges. Response time, error rate, throughput.
**Traces** = The GPS breadcrumb trail. Request went: API → UserService → PaymentService → Database.

You need all three. Logs tell you *what* failed. Metrics tell you *that* something is wrong. Traces tell you *where* in the chain it failed.

---

### Part 6: Sensitive Data — Never Log Secrets

**Imagine writing your password in your diary.** Bad idea! If someone finds your diary, they have your password.

Same with logs. Logs get stored in files. Many people might have access (developers, support, logs might be sent to external tools). **Never** put these in logs:
- Passwords
- Credit card numbers
- Social security numbers
- Secret tokens or API keys
- Full authentication tokens

**Good:** "User 12345 logged in successfully"
**Bad:** "User logged in with password: hunter2" 💀

---

## Layer 2 — Professional Developer

### SLF4J + Logback — Spring Boot's Logging System

Spring Boot uses two pieces for logging:

| Component | Role | Analogy |
|-----------|------|---------|
| **SLF4J** | The API (what you write) | The remote control — same buttons no matter which TV |
| **Logback** | The implementation (what runs) | The actual TV that displays the picture |

**Why separate them?** So you can switch to a different "TV" (e.g., Log4j2) without changing your code. You use the same `Logger` interface everywhere.

#### Basic Usage

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {
    
    // One logger per class — static final = shared, never changes
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    public Order createOrder(OrderRequest request) {
        // INFO = important business event
        log.info("Creating order for user: {}, items: {}", 
            request.getUserId(), request.getItems().size());
        
        try {
            // DEBUG = technical detail for developers
            log.debug("Validating order: total={}, currency={}", 
                request.getTotal(), request.getCurrency());
            
            Order order = processOrder(request);
            
            log.info("Order created successfully: orderId={}, total={}", 
                order.getId(), order.getTotal());
            
            return order;
            
        } catch (InsufficientStockException e) {
            // WARN = expected problem, not a bug
            log.warn("Order creation failed: insufficient stock for user: {}", 
                request.getUserId(), e);
            throw e;
            
        } catch (Exception e) {
            // ERROR = unexpected failure, needs investigation
            log.error("Unexpected error creating order for user: {}", 
                request.getUserId(), e);
            throw e;
        }
    }
}
```

**Lombok shortcut:**

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j  // Creates: private static final Logger log = LoggerFactory.getLogger(...)
@Service
public class OrderService {
    public Order createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        // ... rest of code
    }
}
```

---

### Critical Logging Rules

```java
// ✅ RULE 1: Parameterized logging (efficient)
log.debug("Processing order {} with {} items", orderId, itemCount);

// ❌ BAD: String concatenation
log.debug("Processing order " + orderId + " with " + itemCount + " items");
// Why: If DEBUG is disabled, Java STILL builds the string and throws it away.
// With millions of log calls, this wastes significant CPU and memory.

// ✅ RULE 2: Pass exception as LAST parameter (gets full stack trace)
log.error("Payment failed for order {}", orderId, exception);

// ❌ BAD: Loses stack trace
log.error("Payment failed: " + exception.getMessage());

// ✅ RULE 3: Include context (who, what, when)
log.info("User {} created order {} with total {}", userId, orderId, total);

// ❌ BAD: Useless for debugging
log.info("Order created");

// ✅ RULE 4: NEVER log sensitive data
log.info("User {} authenticated successfully", userId);

// ❌ NEVER:
log.info("User logged in with password: {}", password);    // 💀
log.debug("JWT token: {}", jwtToken);                       // 💀
log.info("Credit card: {}", cardNumber);                    // 💀
```

---

### Application.yml Log Configuration

```yaml
logging:
  level:
    root: INFO                           # Default: INFO and above
    com.myapp: DEBUG                     # Your app: more detail
    com.myapp.service: TRACE             # Specific package: everything
    org.springframework: WARN            # Spring: only warnings/errors
    org.hibernate: WARN                  # Hibernate: only warnings
    org.springframework.security: DEBUG  # Security: debug when troubleshooting
  
  pattern:
    # Format for console output
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    # Example: 10:30:45.123 [http-nio-8080-exec-1] INFO  OrderService - Creating order
  
  file:
    name: logs/application.log           # Where to write logs
  
  logback:
    rollingpolicy:
      max-file-size: 10MB                # New file when current reaches 10MB
      max-history: 30                    # Keep 30 days of logs
      total-size-cap: 1GB               # Max 1GB total disk usage
      file-name-pattern: logs/application-%d{yyyy-MM-dd}.%i.log
```

---

### Advanced: logback-spring.xml

For production-grade setups, create `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- CONSOLE: Pretty output during development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- FILE: All logs, rotated by size and date -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <!-- %X{correlationId} = from MDC, for request tracing -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- ERROR_FILE: Only errors — quick to scan when things go wrong -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.myapp" level="DEBUG" />
    <logger name="org.springframework" level="WARN" />
    <logger name="org.hibernate" level="WARN" />
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
        <appender-ref ref="ERROR_FILE" />
    </root>
</configuration>
```

**Why a separate error file?** At 3 AM when something breaks, you open `error.log` and see only errors — no sifting through millions of INFO lines.

---

### Spring Boot Actuator

**Actuator = built-in monitoring endpoints** for health, metrics, and more.

#### Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
      base-path: /actuator
  
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true    # For Kubernetes liveness/readiness probes
  
  metrics:
    tags:
      application: ${spring.application.name}
```

#### Actuator Endpoints

| Endpoint | Purpose | Real-World Use |
|----------|---------|----------------|
| `/actuator/health` | Is the app alive? | Load balancer, Kubernetes probes |
| `/actuator/info` | App version, build info | "Which version is deployed?" |
| `/actuator/metrics` | List all metrics | Discover what to monitor |
| `/actuator/metrics/{name}` | Specific metric value | JVM memory, HTTP counts |
| `/actuator/prometheus` | Metrics in Prometheus format | Grafana dashboards |
| `/actuator/loggers` | View/change log levels at runtime | Enable DEBUG without restart! |
| `/actuator/env` | Configuration values | Verify config in running app |

**Health check example:**
```bash
curl http://localhost:8080/actuator/health
```
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "diskSpace": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

**Change log level at runtime (no restart!):**
```bash
curl -X POST http://localhost:8080/actuator/loggers/com.myapp \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

### Custom Health Indicators

```java
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {
    
    private final PaymentGatewayClient paymentGateway;
    
    public PaymentGatewayHealthIndicator(PaymentGatewayClient paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
    
    @Override
    public Health health() {
        try {
            long start = System.currentTimeMillis();
            boolean reachable = paymentGateway.ping();
            long responseTime = System.currentTimeMillis() - start;
            
            if (reachable && responseTime < 5000) {
                return Health.up()
                    .withDetail("service", "Payment Gateway")
                    .withDetail("responseTimeMs", responseTime)
                    .build();
            } else if (reachable) {
                return Health.status("DEGRADED")
                    .withDetail("service", "Payment Gateway")
                    .withDetail("responseTimeMs", responseTime)
                    .withDetail("warning", "Response time above 5s threshold")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "Payment Gateway")
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

---

### Custom Metrics with Micrometer

**Micrometer** is to metrics what SLF4J is to logging — a facade that works with Prometheus, Datadog, etc.

| Type | What It Measures | Example |
|------|------------------|---------|
| **Counter** | Things that only go UP | Total orders, total errors |
| **Timer** | How long something takes | Request duration, processing time |
| **Gauge** | Value that goes up and down | Active connections, queue size |

```java
@Component
public class BusinessMetrics {
    
    private final Counter ordersCreated;
    private final Counter ordersFailed;
    private final Timer orderProcessingTime;
    private final AtomicInteger activeOrders;
    
    public BusinessMetrics(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("business.orders.created")
            .tag("type", "success")
            .register(registry);
        
        this.ordersFailed = Counter.builder("business.orders.created")
            .tag("type", "failure")
            .register(registry);
        
        this.orderProcessingTime = Timer.builder("business.orders.processing.duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        this.activeOrders = registry.gauge("business.orders.active", new AtomicInteger(0));
    }
    
    public void recordOrderSuccess() { ordersCreated.increment(); }
    public void recordOrderFailure() { ordersFailed.increment(); }
    public Timer.Sample startTimer() { return Timer.start(); }
    public void stopTimer(Timer.Sample sample) { sample.stop(orderProcessingTime); }
    public void incrementActive() { activeOrders.incrementAndGet(); }
    public void decrementActive() { activeOrders.decrementAndGet(); }
}
```

---

## Layer 3 — Advanced Engineering Depth

### Structured Logging (JSON)

**Plain text** is fine for humans. **JSON** is better for machines — searchable, filterable, analyzable.

**Plain text (hard to search):**
```
2026-02-05 10:30:45.123 [http-exec-1] INFO OrderService - Order created for user 123 total 99.99
```

**Structured JSON (easy to query):**
```json
{
  "timestamp": "2026-02-05T10:30:45.123Z",
  "level": "INFO",
  "thread": "http-exec-1",
  "logger": "com.myapp.service.OrderService",
  "message": "Order created",
  "orderId": "ORD-456",
  "userId": "USR-123",
  "total": 99.99,
  "correlationId": "abc-123-def"
}
```

With JSON, tools like Elasticsearch let you query: "Show all ERROR logs for userId=USR-123 in the last hour."

**Setup (logstash-logback-encoder):**

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>correlationId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/app-%d{yyyy-MM-dd}.%i.json</fileNamePattern>
        <maxFileSize>50MB</maxFileSize>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
</appender>
```

---

### Correlation IDs and MDC

**Problem:** One request flows through many services. When it fails, how do you find related logs everywhere?

**Solution:** Attach a unique **correlation ID** to each request. Every service logs it. Then you search by that ID.

```
Request → Gateway (abc-123) → UserService (abc-123) → PaymentService (abc-123) → DB
```

**MDC (Mapped Diagnostic Context)** = a thread-local map. Put `correlationId` in MDC at the start of the request, and it's automatically included in every log on that thread.

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain chain) throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put("correlationId", correlationId);
        MDC.put("requestUri", request.getRequestURI());
        
        try {
            response.setHeader(CORRELATION_HEADER, correlationId);
            chain.doFilter(request, response);
        } finally {
            MDC.clear();  // Always clear — MDC is thread-local
        }
    }
}
```

**Log pattern:**
```xml
<pattern>%d{HH:mm:ss.SSS} [%X{correlationId}] %-5level %logger{36} - %msg%n</pattern>
```

**Output:**
```
10:30:45.123 [abc-123-def] INFO  OrderService - Creating order
10:30:45.234 [abc-123-def] INFO  PaymentService - Processing payment
10:30:45.345 [abc-123-def] ERROR PaymentService - Gateway timeout
```

---

### Production Observability Stack

```
┌──────────────────────────────────────────────────────────────────┐
│                    PRODUCTION OBSERVABILITY                        │
├──────────────────────────────────────────────────────────────────┤
│                                                                    │
│  LOGS (ELK/EFK):                                                   │
│  App → Logstash/Fluentd → Elasticsearch → Kibana                  │
│                                                                    │
│  METRICS:                                                          │
│  App (Actuator /actuator/prometheus) → Prometheus → Grafana      │
│                                                                    │
│  TRACES:                                                           │
│  App (Micrometer Tracing) → Zipkin / Jaeger                       │
│                                                                    │
│  ALERTS:                                                           │
│  Prometheus → AlertManager → Slack / PagerDuty / Email           │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
```

---

### Distributed Tracing

**Distributed tracing** tracks a single request as it flows through multiple services.

- **Span** = one unit of work (e.g., "Call to Payment Service")
- **Trace** = the full journey (all spans for one request)

Tools like **Zipkin** or **Jaeger** visualize: API Gateway → User Service (50ms) → Payment Service (200ms) → Database (30ms). You see exactly where time was spent.

Spring Boot 3: add `micrometer-tracing-bridge-*` and a tracer (e.g., Zipkin). Traces are sent automatically.

---

### Slow Request Detection

```java
@Component
public class SlowRequestInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(SlowRequestInterceptor.class);
    private static final long SLOW_THRESHOLD_MS = 1000;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("_startTime", System.nanoTime());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("_startTime");
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        
        if (durationMs > SLOW_THRESHOLD_MS) {
            log.warn("SLOW REQUEST: {} {} took {}ms (threshold: {}ms)", 
                request.getMethod(), request.getRequestURI(), durationMs, SLOW_THRESHOLD_MS);
        }
    }
}
```

---

## Layer 4 — Interview Mastery

**Q: How would you investigate a production outage?**

1. Check dashboards (Grafana) — error rate, latency, resources.
2. Check `/actuator/health` — which components are DOWN.
3. Search logs (Kibana) in the failure time window for ERROR.
4. Use correlation IDs to trace the request across services.
5. Check metrics: connection pools, JVM memory, GC.
6. Enable DEBUG via `/actuator/loggers` if needed (no restart).
7. Document root cause and add alerts for next time.

---

**Q: What's the difference between logging and monitoring?**

**Logging** = discrete events, "what happened" — like security camera footage.
**Monitoring** = continuous metrics, "how it's performing now" — like dashboard gauges.

Logs for investigation; metrics for alerts. Use both: ELK for logs, Prometheus+Grafana for metrics.

---

**Q: What is MDC and why does it matter?**

**MDC (Mapped Diagnostic Context)** = thread-local map that adds context to every log on that thread. Put `correlationId` and `userId` in MDC at request start; every `log.info()` includes them. Critical for tracing requests across services and for context in debugging.

---

**Q: What metrics would you track?**

**Business:** Orders/min, revenue, signups.
**Application:** Response time P50/P95/P99, error rate, requests/sec, connection pool usage, cache hit rate.
**Infrastructure:** CPU, memory, GC, disk I/O.

Key alerts: error rate > 5%, P99 > 2s, connection pool pending > 0, JVM memory > 85%.

---

## Summary

| Concept | Takeaway |
|---------|----------|
| **Logs** | Diary/security camera — what happened |
| **Monitoring** | Dashboard/gauges — how it's performing now |
| **Log levels** | INFO for business, DEBUG for tech, WARN for potential issues, ERROR for failures |
| **Sensitive data** | Never log passwords, tokens, cards, SSN |
| **Parameterized logging** | `log.info("User {}", id)` not `log.info("User " + id)` |
| **Correlation IDs** | Trace one request across services via MDC |
| **Actuator** | `/health`, `/metrics`, `/loggers` (change levels without restart) |
| **Structured logging** | JSON logs for search and analysis |
| **Production stack** | ELK (logs) + Prometheus+Grafana (metrics) |

---

**Related Chapters:**
- [Chapter 18: Configuration & Profiles](18-Configuration-Profiles.md) — Environment-specific logging
- [Chapter 22: Exception Handling](22-Exception-Handling.md) — Logging exceptions properly
- [Chapter 33: Microservices](33-Microservices.md) — Distributed tracing

---

[← Back to Index](00-README.md) | [Previous: Caching](31-Caching.md) | [Next: Microservices →](33-Microservices.md)
