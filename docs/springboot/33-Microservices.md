# Chapter 33: Microservices Fundamentals

[← Back to Index](00-README.md) | [Previous: Logging & Monitoring](32-Logging-Monitoring.md) | [Next: Event-Driven Architecture →](34-Event-Driven.md)

---

## Why This Chapter Matters

Microservices are one of the **most talked-about topics in software interviews**. Companies like Netflix, Amazon, Uber, and Spotify built their success on this style of architecture. Many job postings mention microservices.

But here’s what many people get wrong: **microservices are not just “small services.”** Splitting everything into tiny services too early often creates a “distributed monolith” — more complexity, more failures, and slower development.

This chapter teaches you:

- What microservices are and what problems they solve
- **When to use them** and **when not to**
- How to handle the real-world challenges they create
- How to think through architecture like a senior engineer

Without this understanding, you risk splitting services too early, making costly mistakes, and struggling in system design interviews. With it, you can make informed decisions, explain your choices, and design systems that scale.

---

## Layer 1 — Intuition Builder

### Part 1: One Big Restaurant vs a Food Court

Imagine you open a small restaurant. At first, you do everything: take orders, cook, handle money, clean tables. One person, one place, one way of doing things. That’s a **monolith** — one big application doing everything.

It works great when you’re small. Orders are simple, you know where everything is, and there’s no confusion. If something breaks, it’s easy to see what went wrong.

Now imagine your restaurant gets really popular. You have long queues, more dishes, more customers. One person can’t keep up. So you split the work:

- **Cashier** — only takes orders
- **Kitchen** — only cooks
- **Delivery** — only delivers
- **Payment desk** — only handles money

Each person has a clear job and works somewhat independently. If the kitchen is busy, you add another cook, not another cashier. Each part can grow at its own pace. That idea — separate, focused parts that work together — is what **microservices** are about: smaller, independent services instead of one giant application.

But: if you only have a few customers per hour, splitting everything like a food court is overkill. The extra coordination (who does what, when, where) costs more than having one person do it all. **Most small projects should stay as one big application (a monolith) until they really need to split.**

---

### Part 2: Monolith vs Microservices — A Clear Picture

| Monolith (One Big Restaurant) | Microservices (Food Court) |
|------------------------------|----------------------------|
| One building, one kitchen, one counter | Separate stalls: pizza, burgers, drinks |
| Easy to start and run at first | More setup and planning up front |
| One thing breaks and everything might stop | If one stall closes, others keep working |
| Hard to grow one part without growing everything | Each stall can scale on its own |
| One deployment can affect the whole system | Each service can be changed separately |
| One team usually works on everything | Different teams can own different services |

**Simple rule of thumb:** Start as one restaurant. Split into a food court only when you have real reasons (traffic, different teams, different scaling needs).

---

### Part 3: When to Split — And When to Stay Together

**Use microservices when:**

- Your team is big (10+ developers, multiple teams)
- Different parts need very different scaling (e.g. search gets 100x more traffic than reports)
- You need to deploy one part often without touching others
- Different parts need different technologies (e.g. payments in Java, search in another language)
- Your business domain is large and complex

**Stay with a monolith when:**

- Your team is small (1–5 people)
- You’re building something simple (e.g. blog, small CRUD app)
- You have tight deadlines and need to ship fast
- You don’t yet have experience with distributed systems
- You can’t clearly say *why* you need microservices

**The Golden Rule:** Start with a well-organized monolith. Split into services only when you feel real pain (slow deployments, scaling issues, team bottlenecks).

Shopify, Amazon, and Netflix all started as monoliths and only moved to microservices after they hit real limits.

---

### Part 4: Service Independence — Each Stall Owns Its Space

In a food court, each stall:

- Has its own space (like its own database)
- Can close for maintenance without shutting the whole mall
- Can hire more staff when needed (scaling)
- Manages its own recipes and processes

In software, that means:

- Each service has its own **database** — no shared database across services
- Each service can be **deployed** without affecting others
- Each service can be **scaled** independently
- Each service is responsible for its own domain

This independence is what makes microservices flexible — but it also makes coordination and consistency harder.

---

### Part 5: How Services Talk — Phone Calls vs Text Messages

Services need to communicate. There are two main ways:

**Synchronous (like a phone call):**

- Service A asks Service B something and **waits** for the answer before continuing
- If B doesn’t answer, A is stuck waiting
- Example: Order service asks User service “Is this user valid?” — it must get an answer before placing the order

**Asynchronous (like a text message):**

- Service A sends a message and **continues** doing other things
- Service B handles the message whenever it can
- Example: Order service says “Order #123 was created” — it doesn’t wait. Notification service picks it up later and sends an email.

| Synchronous (REST, gRPC) | Asynchronous (Message Queues) |
|--------------------------|------------------------------|
| You need the answer right now | You can wait for it later |
| Real-time user requests | Background tasks, notifications |
| “Is this user valid?” | “Send a confirmation email” |
| Simple request–response | Event-driven workflows |
| Can block if the other service is slow or down | Less coupling; failure of one doesn’t block others as badly |

Choosing sync vs async is one of the main design decisions in a microservices system.

---

### Part 6: Service Discovery — Finding the Right Stall

In a mall, you check a directory to find “Pizza Stall” or “Juice Bar.” Services need the same: a way to find each other by name, even when their addresses (IPs, ports) change.

- Services **register** themselves: “I’m the User Service, I’m available here”
- Other services **look up** “User Service” and get the current address
- Tools like **Eureka** act like a phone book that’s always up to date

This lets you add or remove service instances without rewriting every URL in the system.

---

### Part 7: API Gateway — The Main Entrance

Imagine customers entering a food court. They could walk to each stall directly — but that’s confusing. Instead, there’s usually one main entrance that:

- Directs people to the right stall
- Checks tickets or IDs (like authentication)
- Manages crowds and flow

An **API Gateway** is that main entrance for your system:

- One URL for clients (e.g. `api.myapp.com`)
- The gateway sends each request to the correct service (User, Order, Payment, etc.)
- Central place for security, rate limiting, and routing

Without a gateway, clients would need to know multiple URLs and deal with each service separately.

---

### Part 8: Circuit Breaker — Stop the Cascade

In your house, a **circuit breaker** trips when there’s a power surge. It cuts the flow so one problem doesn’t cause a fire.

In microservices, if Service B is down and Service A keeps calling it:

- A waits for B (timeout)
- A becomes slow, then unavailable
- Services that call A also slow down or fail

One failing service can bring down the whole chain. That’s a **cascading failure**.

A **circuit breaker** in software works like an electrical one:

- While B is healthy: requests go through (circuit **closed**)
- After too many failures: stop calling B for a while (circuit **open**) and return a safe default or error
- After some time: try one or two calls to see if B recovered (circuit **half-open**)

This keeps one failing service from pulling down the rest of the system.

---

### Part 9: Data Consistency — The Tricky Part

In one big application, you can use a single database and transactions to keep data consistent. In microservices, each service has its own database.

Example:

- Order Service creates an order
- Payment Service processes payment
- Inventory Service reserves stock

If payment fails, you must cancel the order and put stock back. But each step lives in a different service and a different database. There’s no single transaction.

**Eventual consistency** means: data will become correct over time, but not instantly everywhere.

- User updates profile → User Service saves it
- User Service publishes an event
- Search and Analytics services update when they receive the event (maybe seconds or minutes later)

For many cases (analytics, search, notifications), that delay is fine. For others (payments, inventory), you need more care — patterns like the **Saga pattern** (sequences of steps with compensating actions if something fails).

---

### Part 10: The Real Challenges Nobody Wants to Talk About

Splitting an app into services is the easy part. The hard parts are:

1. **Data consistency** — keeping data correct across many databases
2. **Communication** — what happens when a service is slow or down
3. **Debugging** — a request goes through 5 services; which one broke?
4. **Testing** — how to test when each service depends on several others
5. **Deployment** — how to deploy and roll back many services safely

That’s why the rule is: don’t go microservices until you really need them. A well-structured monolith is simpler than a poorly designed microservice system.

---

## Layer 2 — Professional Developer

### Monolith vs Microservices — Decision Table

| Factor | Monolith | Microservices |
|--------|----------|---------------|
| **Team size** | 1–10 developers | 10+ developers, multiple teams |
| **Deployment** | One deployment, everything changes | Independent deployments per service |
| **Scaling** | Scale the whole app | Scale individual services |
| **Database** | Single shared database | Database per service |
| **Communication** | In-memory method calls | Network calls (REST, gRPC, queues) |
| **Consistency** | ACID transactions | Eventual consistency, Sagas |
| **Complexity** | Lower at small scale | Higher from day one |
| **Best for** | MVPs, simple apps, small teams | Large-scale, complex domains |

---

### Microservice Architecture — ASCII Diagram

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    CLIENTS (Web, Mobile, etc.)            │
                    └────────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                    ┌─────────────────────────────────────────────────────────┐
                    │                     API GATEWAY                           │
                    │  (Routing, Auth, Rate Limiting)                           │
                    └────────────────────────────┬────────────────────────────┘
                                                 │
                    ┌────────────────────────────┼────────────────────────────┐
                    │                            │                            │
                    ▼                            ▼                            ▼
           ┌───────────────┐           ┌───────────────┐           ┌───────────────┐
           │ User Service  │           │ Order Service │           │Payment Service │
           │ ┌───────────┐ │           │ ┌───────────┐ │           │ ┌───────────┐  │
           │ │ User DB   │ │           │ │ Order DB  │ │           │ │ Payment   │  │
           │ └───────────┘ │           │ └───────────┘ │           │ │    DB     │  │
           └───────┬───────┘           └───────┬───────┘           └───────┬─────┘
                   │                           │                           │
                   │    ┌──────────────────────┼──────────────────────┐   │
                   │    │                      │                      │   │
                   └────┼──────────────────────┼──────────────────────┼───┘
                        │                      │                      │
                        ▼                      ▼                      ▼
                    ┌─────────────────────────────────────────────────────────┐
                    │              SERVICE REGISTRY (Eureka)                   │
                    │         "Where is each service running?"                 │
                    └─────────────────────────────────────────────────────────┘
                        │                      │                      │
                        └──────────────────────┼──────────────────────┘
                                               │
                                    Message Queue (async communication)
                                               │
                    ┌──────────────────────────┼──────────────────────────┐
                    ▼                          ▼                          ▼
           ┌───────────────┐           ┌───────────────┐           ┌───────────────┐
           │ Notification  │           │  Inventory    │           │   Analytics   │
           │   Service     │           │   Service     │           │   Service     │
           └───────────────┘           └───────────────┘           └───────────────┘
```

---

### Database per Service — Why and How

**Why:** Each service owns its data. No shared tables across services — that would couple them and break the benefits of independence.

**How:** Use IDs as references, not foreign keys. Order Service stores `userId`, but does not have a foreign key to User Service’s database.

```java
// ✅ Order Service - uses userId as REFERENCE only (no foreign key)
@Entity
public class Order {
    @Id
    private Long id;
    private Long userId;  // Reference to User Service - we don't own User table
    private BigDecimal total;
    private OrderStatus status;
}

// ✅ User Service - owns and manages User data
@Entity
public class User {
    @Id
    private Long id;
    private String email;
    private String name;
}
```

To get user details for an order, Order Service calls User Service’s API; it does not join against User’s database.

---

### Synchronous Communication — REST and Feign

**Why REST?** Simple, widely supported, easy to debug. Good for request–response when you need an answer immediately.

**Why Feign?** Declarative HTTP client. You define an interface; Spring generates the implementation and handles load balancing with Eureka.

```java
// Feign Client - declares WHAT we need, Spring handles HOW (HTTP calls, retries)
@FeignClient(name = "user-service")  // "user-service" resolved by Eureka
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable Long id);
}

// Order Service uses it like a local call - but it's actually an HTTP request
@Service
public class OrderService {
    private final UserServiceClient userServiceClient;

    public Order createOrder(OrderDto dto) {
        // Validates user exists BEFORE creating order - must wait for answer (sync)
        User user = userServiceClient.getUser(dto.getUserId());
        if (user == null) {
            throw new UserNotFoundException("User " + dto.getUserId() + " not found");
        }
        return orderRepository.save(new Order(dto));
    }
}
```

---

### Asynchronous Communication — Message Queues

**Why async?** Decouples producers and consumers. Order Service doesn’t wait for emails to be sent. If Notification Service is slow or down, orders still get created.

```java
// Order Service - publishes event, doesn't wait for consumers
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;  // or KafkaTemplate for Kafka

    public Order createOrder(OrderDto dto) {
        Order order = orderRepository.save(new Order(dto));
        
        // Fire-and-forget: notify others without blocking
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getTotal()
        );
        rabbitTemplate.convertAndSend("order.exchange", "order.created", event);
        
        return order;
    }
}

// Notification Service - consumes event when ready
@Component
public class OrderEventConsumer {
    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Runs independently - could be seconds later
        emailService.sendOrderConfirmation(event.getUserId(), event.getOrderId());
    }
}
```

---

### Service Discovery with Eureka

**Why Eureka?** Services can come and go (scaling, failures, deploys). Hardcoding URLs breaks. Eureka keeps a registry of where each service is running.

**Eureka Server** — the registry:

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
# application.yml - Eureka Server config
server:
  port: 8761
eureka:
  client:
    register-with-eureka: false  # Server doesn't register itself
    fetch-registry: false        # Server doesn't need to fetch other servers
```

**Eureka Client** — each service registers itself:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

```yaml
# application.yml - Service registers with Eureka
spring:
  application:
    name: user-service  # This is the name others use to find us
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

**Using discovery** — call by service name, not IP:

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced  // Enables client-side load balancing via Eureka
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class OrderService {
    private final RestTemplate restTemplate;

    public User getUser(Long userId) {
        // "user-service" is resolved by Eureka to actual host:port
        return restTemplate.getForObject(
            "http://user-service/api/users/" + userId,
            User.class
        );
    }
}
```

---

### API Gateway with Spring Cloud Gateway

**Why a gateway?** Single entry point, centralized security, routing, rate limiting. Clients talk to one URL; the gateway forwards to the right service.

```yaml
# application.yml - Gateway routes
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service  # lb = load balance via Eureka
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1   # Remove /api from path before forwarding

        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1

        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/**
          filters:
            - StripPrefix=1
```

**Custom auth filter** — validate JWT at the gateway:

```java
@Component
public class AuthFilter implements GatewayFilter {
    private final JwtTokenProvider tokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = extractToken(request);

        if (token == null || !tokenProvider.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Pass user context to downstream services
        String username = tokenProvider.getUsernameFromToken(token);
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-User-Id", username)
            .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst("Authorization");
        return (bearer != null && bearer.startsWith("Bearer "))
            ? bearer.substring(7) : null;
    }
}
```

---

### Circuit Breaker with Resilience4j

**Why a circuit breaker?** Avoid cascading failures. If a downstream service is down, fail fast and return a fallback instead of waiting and timing out.

```java
@Service
public class OrderService {
    private final UserServiceClient userServiceClient;

    // When user-service fails too often, circuit opens → fallback is called
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    public User getUser(Long userId) {
        return userServiceClient.getUser(userId);
    }

    // Same signature + Exception - invoked when circuit is open or call fails
    public User getUserFallback(Long userId, Exception e) {
        log.warn("User service unavailable, using cached/default", e);
        return User.defaultUser(userId);
    }
}
```

```yaml
# application.yml - Circuit breaker config
resilience4j:
  circuitbreaker:
    instances:
      userService:
        slidingWindowSize: 10           # Last 10 calls evaluated
        minimumNumberOfCalls: 5         # Need 5 calls before deciding
        failureRateThreshold: 50        # Open if 50% fail
        waitDurationInOpenState: 10s    # Try again after 10 seconds
        permittedNumberOfCallsInHalfOpenState: 3  # Test with 3 calls
```

States: **CLOSED** (normal) → **OPEN** (stop calling) → **HALF_OPEN** (test a few calls) → **CLOSED** if they succeed.

---

### Retry Pattern

**Why retry?** Temporary network or overload issues often succeed on a second try. Retrying avoids failing the whole request for transient errors.

```java
@Service
public class OrderService {
    @Retry(name = "userService", fallbackMethod = "getUserFallback")
    public User getUser(Long userId) {
        return userServiceClient.getUser(userId);
    }

    public User getUserFallback(Long userId, Exception e) {
        return User.defaultUser(userId);
    }
}
```

```yaml
resilience4j:
  retry:
    instances:
      userService:
        maxAttempts: 3
        waitDuration: 1s
        retryExceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
```

---

## Layer 3 — Advanced Engineering Depth

### Saga Pattern — Distributed Transactions

**Problem:** Create order, charge payment, reserve inventory across multiple services and databases. There is no single transaction.

**Saga:** A sequence of local transactions. If one step fails, run compensating actions (undoes) in reverse order.

**Choreography (event-driven):** Each service reacts to events and publishes its own. No central coordinator.

```
Order Created → Payment Service listens → processes payment
    → Payment Success → Inventory Service listens → reserves items
    → Payment Failed → Order Service listens → cancels order
```

```java
// Order Service - choreography style
@Service
public class OrderService {
    private final RabbitTemplate rabbitTemplate;

    public Order createOrder(OrderDto dto) {
        Order order = orderRepository.save(new Order(dto));
        rabbitTemplate.convertAndSend("order.exchange", "order.created",
            new OrderCreatedEvent(order.getId(), order.getTotal()));
        return order;
    }

    @RabbitListener(queues = "payment.failed.queue")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        // Compensating action: undo the order
        orderRepository.findById(event.getOrderId())
            .ifPresent(order -> {
                order.cancel();
                orderRepository.save(order);
                rabbitTemplate.convertAndSend("order.exchange", "order.cancelled",
                    new OrderCancelledEvent(order.getId()));
            });
    }
}
```

**Orchestration (central coordinator):** One service drives the flow and calls others.

```java
@Service
public class OrderOrchestrator {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    public Order createOrder(OrderDto dto) {
        Order order = null;
        try {
            order = orderService.createOrder(dto);
            paymentService.processPayment(order.getId(), order.getTotal());
            inventoryService.reserveItems(order.getId(), order.getItems());
            return order;
        } catch (PaymentException e) {
            if (order != null) orderService.cancelOrder(order.getId());
            throw e;
        } catch (InventoryException e) {
            if (order != null) {
                paymentService.refundPayment(order.getId());
                orderService.cancelOrder(order.getId());
            }
            throw e;
        }
    }
}
```

**Trade-offs:** Choreography scales well but is harder to reason about. Orchestration is clearer but the orchestrator can become a bottleneck.

---

### Eventual Consistency — When and Why

**Definition:** Data becomes consistent over time through event propagation. Different services may be briefly out of sync.

**When acceptable:**

- Analytics, reporting, search indexes
- Notifications, audit logs
- Non-critical caches and replicas

**When to avoid:**

- Payments, inventory (usually need stronger guarantees)
- Legal or compliance-sensitive data

**CAP:** In a partition (network split), you choose between Consistency and Availability. Microservices often choose **AP** (Availability + Partition Tolerance) and accept eventual consistency.

---

### Spring Cloud Config — Centralized Configuration

**Why?** Many services need similar config (DB URLs, feature flags, timeouts). Managing it per service is error-prone. A Config Server holds config in Git (or similar) and serves it to clients.

**Config Server:**

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/myorg/config-repo
          search-paths: '{application}'
```

**Client (service):**

```yaml
spring:
  cloud:
    config:
      uri: http://config-server:8888
      name: user-service
      profile: production
```

---

### Distributed Tracing — Following a Request Across Services

**Problem:** A request goes through Gateway → User Service → Order Service → Payment Service. Which one failed or was slow?

**Solution:** Trace IDs. Every request gets a unique ID. Each service logs that ID. Tools like Zipkin or Jaeger collect and visualize the full path.

```java
// Spring Cloud Sleuth adds trace IDs automatically to logs
@RestController
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) {
        // Log includes [appname,traceId,spanId] automatically
        log.info("Fetching order {}", id);
        return orderService.getOrder(id);
    }
}
```

Log output: `[order-service,abc123,def456] Fetching order 42`

---

### CQRS Basics — Separate Read and Write Models

**Idea:** Reads and writes often have different needs. Write model: normalized, consistent. Read model: denormalized, optimized for queries.

**CQRS:** Separate command (write) and query (read) models. Often with different databases or storage types.

```java
// Command side - writes to primary DB
@Service
public class OrderCommandService {
    private final OrderRepository orderRepository;

    public Order createOrder(OrderDto dto) {
        return orderRepository.save(new Order(dto));
    }
}

// Query side - reads from read-optimized view/DB
@Service
public class OrderQueryService {
    private final OrderViewRepository orderViewRepository;

    public List<OrderView> getOrdersByUser(Long userId) {
        return orderViewRepository.findByUserId(userId);
    }
}
```

Read model is typically updated asynchronously from events (eventual consistency).

---

### Deployment Patterns

**Blue-Green:**

- Two identical environments (blue, green)
- Deploy new version to inactive environment
- Switch traffic when ready
- Rollback = switch back

```
         Load Balancer
                │
        ┌───────┴───────┐
        ▼               ▼
   ┌─────────┐    ┌─────────┐
   │  Blue   │    │  Green  │
   │  (v1)   │    │  (v2)   │
   │ Active  │    │ Standby │
   └─────────┘    └─────────┘
```

**Canary:**

- New version gets a small share of traffic (e.g. 10%)
- Monitor errors and latency
- Gradually increase or roll back

```
         Load Balancer
                │
        ┌───────┴───────┐
        ▼               ▼
   ┌─────────┐    ┌─────────┐
   │ Canary  │    │ Stable  │
   │  (v2)   │    │  (v1)   │
   │  10%    │    │  90%    │
   └─────────┘    └─────────┘
```

**Rolling update:** Replace instances one by one. Zero downtime. Default in Kubernetes.

---

### Edge Cases and Gotchas

1. **Partial failures:** One saga step succeeds, the next fails. Design compensating actions for every step.
2. **Duplicate events:** Message consumers may process the same event twice. Use idempotency (e.g. unique keys, checks).
3. **Circuit breaker tuning:** Too sensitive → unnecessary fallbacks. Too loose → cascading failures. Tune thresholds for your traffic.
4. **Database per service:** Joins across services are not allowed. Use API aggregation or denormalized read models.
5. **Config refresh:** Changing config at runtime requires refresh mechanisms (Spring Cloud Config `/actuator/refresh` or similar).

---

## Layer 4 — Interview Mastery

### Q1: When would you choose microservices over a monolith?

**Answer:** Use microservices when: (1) large team with multiple ownership boundaries, (2) different scaling needs per area, (3) need for independent deployments, (4) different technology requirements per service, (5) complex domain. Use a monolith when: small team, simple domain, tight deadlines, limited distributed systems experience. Golden rule: start with a monolith; extract services when you hit clear pain.

---

### Q2: How do microservices communicate?

**Answer:**  
**Synchronous:** REST (simple, widely used), gRPC (binary, fast, typed). Use when you need an immediate response.  
**Asynchronous:** Message queues (RabbitMQ, Kafka). Use when you can tolerate delay (notifications, events, background work).

---

### Q3: What is a circuit breaker?

**Answer:** A pattern to stop cascading failures. States: CLOSED (normal), OPEN (stop calling after too many failures), HALF_OPEN (test a few calls). When open, return a fallback instead of calling the failing service. Lets the failing service recover and prevents callers from being blocked.

---

### Q4: How do you handle distributed transactions?

**Answer:**  
(1) **Saga:** Sequence of local transactions with compensating actions (choreography or orchestration).  
(2) **Eventual consistency:** Accept temporary inconsistency; propagate changes via events.  
(3) **Outbox pattern:** Write event to DB in same transaction as business data; background job publishes it.  
Avoid 2PC in microservices — blocking and poor fit for distributed systems.

---

### Q5: What is eventual consistency?

**Answer:** Data will become consistent over time but not at the same instant across all services. Suitable for analytics, search, notifications. Not suitable for payments and other strong-consistency requirements. Often chosen for availability (AP in CAP).

---

### Q6: What is an API Gateway?

**Answer:** Single entry point for clients. Handles routing, authentication, rate limiting, load balancing, and sometimes caching. Clients talk to one URL; the gateway routes to the correct microservice.

---

### Q7: What is service discovery?

**Answer:** Mechanism for services to find each other by name instead of hardcoded URLs. Services register (e.g. with Eureka); clients query the registry or use a load-balanced client that resolves names. Needed because instances and addresses change (scaling, failures, deploys).

---

### Q8: How do you handle service failures?

**Answer:** Circuit breaker (fail fast, fallback), retry with backoff (transient errors), timeouts, bulkhead (isolate resources), fallback (default/cached response). Combine circuit breaker + retry: retry a few times, then open circuit if still failing.

---

### Q9: What is the database-per-service pattern?

**Answer:** Each microservice has its own database. No shared tables. Services expose APIs; others call them instead of querying their DBs. Benefits: independence, technology choice, scaling. Costs: no distributed ACID; need Sagas/eventual consistency and careful design for joins and consistency.

---

### Q10: Monolith vs microservices — quick comparison?

**Answer:**

| Monolith | Microservices |
|----------|---------------|
| Single deployable unit | Many deployable units |
| One database | Database per service |
| In-process calls | Network calls |
| ACID transactions | Sagas, eventual consistency |
| Simpler at small scale | Higher complexity from start |

---

## Summary

**Core ideas:**
1. **Microservices** = small, independent services, each with its own database and deployment.
2. **Start with a monolith** until you have clear reasons to split (team size, scaling, deployment).
3. **Service communication:** Sync (REST, gRPC) when you need an answer now; async (queues) when you can wait.
4. **Service discovery (e.g. Eureka):** Find services by name as instances change.
5. **API Gateway:** Single entry point for routing, auth, and rate limiting.
6. **Circuit breaker (e.g. Resilience4j):** Avoid cascading failures; fail fast and fallback.
7. **Database per service:** No shared DB; use APIs and events to coordinate.
8. **Saga:** Handle distributed flows with local transactions and compensating actions.
9. **Eventual consistency:** Accept temporary inconsistency for better availability.
10. **Deployment:** Blue-green, canary, rolling updates for safe releases.

**Common mistakes:**
- Using microservices when a monolith is enough
- Sharing a database between services
- Overusing synchronous calls (consider async for events)
- Skipping circuit breakers (cascading failures)
- Hardcoding service URLs (use discovery)
- No API Gateway (clients coupled to internal services)

**Next steps:**
- Set up Eureka and try service discovery
- Add Spring Cloud Gateway
- Add Resilience4j circuit breaker and retry
- Experiment with message queues and events
- Add distributed tracing (e.g. Zipkin)

---

**Related Chapters:**
- [Chapter 32: Logging & Monitoring](32-Logging-Monitoring.md) — Distributed tracing
- [Chapter 34: Event-Driven Architecture](34-Event-Driven.md) — Async and event patterns
- [Chapter 18: Configuration & Profiles](18-Configuration-Profiles.md) — Config Server

---

[← Back to Index](00-README.md) | [Previous: Logging & Monitoring](32-Logging-Monitoring.md) | [Next: Event-Driven Architecture →](34-Event-Driven.md)
