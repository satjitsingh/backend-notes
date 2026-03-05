# Chapter 34: Event-Driven Architecture

[← Back to Index](00-README.md) | [Previous: Microservices](33-Microservices.md) | [Next: Testing Spring →](35-Testing-Spring.md)

---

## Why This Chapter Matters

Every large-scale system you use daily — Netflix, Uber, Amazon, LinkedIn — handles millions of things happening every second. Someone places an order. A ride is requested. A video is watched. How do these systems handle so much activity without breaking? They use a design pattern called **event-driven architecture** — where parts of the system announce what happened, and other parts react when they're ready.

When you build applications with multiple services, you'll quickly discover that having services call each other directly (like making a phone call and waiting on the line) creates a tangled web. If one service is slow or broken, everyone waits. Event-driven architecture solves this by letting services communicate through announcements — like posting a notice on a bulletin board — instead of direct phone calls.

**Without event-driven architecture:**
- Services are tightly connected ("Order Service can't work without Email Service being up")
- One slow or failed service creates a domino effect
- Adding a new feature (like analytics) requires changing existing code
- You can't scale individual parts independently
- The system is fragile under high load

**With event-driven architecture:**
- Services communicate through events (they don't know about each other)
- If Email Service goes down, orders still work — emails are sent when it recovers
- Adding analytics? Create a new service that listens to existing events. Zero changes to Order Service.
- Each service can scale independently based on its own load
- The system is resilient and handles high traffic gracefully

---

## Layer 1 — Intuition Builder

### Part 1: The Phone Call vs The Bulletin Board

**Imagine two ways to tell your friends about your birthday party:**

**Way 1 — The Phone Call (Traditional/Synchronous):**
You call Friend A. You wait while they answer. You tell them. They say "OK!" You hang up.
You call Friend B. Busy signal. You wait. You call again. Still busy. **You're stuck.** You can't tell Friend C until Friend B answers. If Friend B never picks up, you never get to Friend C, D, and E.

**Way 2 — The Bulletin Board (Event-Driven):**
You write "Party on Saturday at 3pm!" on a big bulletin board in the town square.
Friend A walks by, reads it, and shows up.
Friend B is on vacation — they'll read it when they return.
Friend C reads it and tells Friend D.
**You're done.** You didn't wait for anyone. Everyone gets the message when they're ready.

**That's the core idea.** Traditional = call and wait. Event-driven = announce and move on.

---

### Part 2: What Is an "Event"? (No Jargon Yet)

An **event** is simply **something that happened**. A fact. Past tense.

Think of it like a wedding photographer announcing to the crowd: *"The ceremony is over!"*

That announcement doesn't tell anyone *what to do*. It just states a fact. The caterer decides to start serving. The DJ decides to play music. The photographer decides to take group photos. Each person *reacts* to the fact in their own way.

**In software:**
- "An order was created" is an event.
- "A payment was processed" is an event.
- "A user signed up" is an event.

The event doesn't say "please send an email" or "please charge the card." It just says *this happened*. Whoever is interested can react.

---

### Part 3: The Event Bus — A Central Announcement Point

Where do events go? Who delivers them?

Imagine a **town crier** in the town square. When something important happens, the town crier shouts it out. Anyone nearby can hear it. The person who caused the event (maybe the baker who finished the bread) doesn't walk around knocking on every door. They tell the town crier once. Done.

In software, we have something similar: an **event bus** or **message broker**. It's the central place where:
1. Someone *publishes* (announces) an event
2. The event is stored or passed along
3. Anyone *subscribed* (listening) receives it and can act

```
        ┌─────────────────────────────────────┐
        │     EVENT BUS / MESSAGE BROKER       │
        │   (The Town Square Bulletin Board)   │
        └─────────────────────────────────────┘
                    ▲                │
                    │ publish        │ deliver
                    │                ▼
        ┌───────────┴───────┐   ┌───────────────────┐
        │   Order Service    │   │  Email Service    │
        │ "Order was created"│   │  (listens)        │
        └───────────────────┘   │  Payment Service  │
                                │  (listens)        │
                                │  Inventory (listens)│
                                └───────────────────┘
```

---

### Part 4: Traditional vs Event-Driven — Side by Side

**Traditional (Synchronous) — The Phone Chain:**

```
You (Order Service) ──call──► Payment Service ──call──► Email Service
       │                            │                        │
   You WAIT                    It WAITS                 It does work
   (can't hang up)             (can't proceed)          (sends email)
```

**Problem:** If Email Service is slow or broken, the whole chain freezes. The order might have been paid, but you never get a confirmation because everyone is stuck waiting.

**Event-Driven (Asynchronous) — The Bulletin Board:**

```
You (Order Service) ──posts "Order Created"──► Event Bus ──► DONE! You move on.
                              │
                    ┌─────────┴─────────┐
                    │                   │
              Payment Service    Email Service    Inventory Service
              (reads when ready)  (reads when     (reads when ready)
                                  ready)
```

**Benefit:** Order Service finishes quickly. If Email Service is down, the event waits in the bus until Email Service is back. Nobody else is blocked.

---

### Part 5: When Would You Use Which?

| Situation | Use Direct Call (Phone) | Use Events (Bulletin Board) |
|-----------|--------------------------|-----------------------------|
| Need an answer *right now* (e.g., "Is this item in stock?") | ✅ Yes | ❌ No |
| One thing triggers many unrelated actions (e.g., order created → email, payment, inventory) | ❌ No | ✅ Yes |
| The action can happen later (e.g., send a summary email) | ❌ No | ✅ Yes |
| One service depends on another being up 24/7 | ✅ Maybe | ❌ Prefer events |
| Simple flow: A does something, then B does one thing | ✅ Yes | Either works |

**Rule of thumb:** If you can say "I'll tell everyone what happened, and they can react when they want," use events. If you need an immediate reply, use a direct call.

---

### Part 6: Events vs Messages — A Quick Distinction

- **Event:** Something that *already happened*. "Order was created." "Payment was processed."
- **Message (as command):** Something you *want done*. "Create this order." "Send this email."

Events = facts. Messages (as commands) = requests. We focus on events in this chapter.

---

## Layer 2 — Professional Developer

### Spring's Built-in Event System — In-Process Events

Spring gives you a simple way to publish and listen to events **inside the same application** (same JVM). No message broker needed. Think of it as an in-memory bulletin board.

**Why use it?** To decouple parts of your code. The `OrderService` doesn't need to know about `EmailService` or `AnalyticsService`. It just publishes "order created." Listeners react.

---

#### Step 1: Create the Event Class

An event is a simple Java class. We extend `ApplicationEvent` (or use a POJO with `ApplicationEventPublisher.publishEvent()` in modern Spring).

```java
/**
 * Represents the fact that an order was created.
 * Immutable: once created, it doesn't change.
 */
public class OrderCreatedEvent extends ApplicationEvent {

    private final Order order;   // The order that was created
    private final Long userId;   // Who placed it

    public OrderCreatedEvent(Object source, Order order, Long userId) {
        super(source);
        this.order = order;
        this.userId = userId;
    }

    public Order getOrder() {
        return order;
    }

    public Long getUserId() {
        return userId;
    }
}
```

---

#### Step 2: Publish the Event

Use `ApplicationEventPublisher`. Spring injects it automatically. After saving the order, we publish the event so any listener can react.

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order createOrder(OrderDto dto) {
        // 1. Save the order to the database
        Order order = orderRepository.save(new Order(dto));

        // 2. Publish event — listeners will be notified
        //    We don't know or care who's listening
        eventPublisher.publishEvent(
            new OrderCreatedEvent(this, order, dto.getUserId())
        );

        return order;
    }
}
```

---

#### Step 3: Listen with @EventListener

Any Spring bean can listen by adding `@EventListener` and a method that accepts the event type.

```java
@Component
public class EmailNotificationListener {

    private final EmailService emailService;

    public EmailNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // Spring calls this method when OrderCreatedEvent is published
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        emailService.sendOrderConfirmation(
            event.getUserId(),
            event.getOrder()
        );
    }
}
```

---

#### Step 4: Multiple Listeners — Same Event, Different Reactions

One event can trigger many listeners. Spring invokes all of them.

```java
@Component
public class PaymentListener {

    private final PaymentService paymentService;

    @EventListener
    public void processPayment(OrderCreatedEvent event) {
        paymentService.charge(event.getOrder());
    }
}

@Component
public class InventoryListener {

    private final InventoryService inventoryService;

    @EventListener
    public void updateInventory(OrderCreatedEvent event) {
        inventoryService.reserveItems(event.getOrder());
    }
}
```

---

### @Async Events — Don't Block the Publisher

**Default behavior:** Listeners run in the **same thread** as the publisher. If a listener is slow, the whole `createOrder` method waits.

**With @Async:** Listeners run in a **separate thread**. The publisher returns immediately.

```java
@SpringBootApplication
@EnableAsync  // Must enable async support
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Component
public class EmailNotificationListener {

    @Async  // Runs in a different thread — publisher doesn't wait
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // This executes in the async thread pool
        emailService.sendOrderConfirmation(
            event.getUserId(),
            event.getOrder()
        );
    }
}
```

**When to use @Async:** When the listener does slow or external work (email, HTTP calls) and the main flow doesn't need to wait.

---

### @TransactionalEventListener — Run After Transaction Commits

**Problem:** You publish an event inside `@Transactional`. If the transaction **rolls back** later, the event was already published. Listeners might act on data that never got committed.

**Solution:** `@TransactionalEventListener` runs the listener only when the transaction **commits**. If the transaction rolls back, the listener is never called.

```java
@Component
public class EmailNotificationListener {

    // default: AFTER_COMMIT — run only after transaction succeeds
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Order is guaranteed to be in DB when this runs
        emailService.sendOrderConfirmation(
            event.getUserId(),
            event.getOrder()
        );
    }
}
```

**Phases:**

| Phase | When it runs |
|-------|--------------|
| `BEFORE_COMMIT` | Right before commit |
| `AFTER_COMMIT` | After successful commit |
| `AFTER_ROLLBACK` | After rollback |
| `AFTER_COMPLETION` | After commit or rollback |

---

### Conditional Listeners — React Only When a Condition Is Met

```java
@Component
public class PremiumUserListener {

    private final RewardService rewardService;

    // Only handle orders over $1000
    @EventListener(condition = "#event.order.total > 1000")
    public void handlePremiumOrder(OrderCreatedEvent event) {
        rewardService.addPoints(event.getUserId(), 100);
    }
}
```

---

### Listener Ordering with @Order

```java
@Component
@Order(1)  // Runs first
public class ValidationListener {

    @EventListener
    public void validateOrder(OrderCreatedEvent event) {
        // Validate before any processing
    }
}

@Component
@Order(2)  // Runs second
public class PaymentListener {

    @EventListener
    public void processPayment(OrderCreatedEvent event) {
        paymentService.charge(event.getOrder());
    }
}
```

---

### Domain Events — Events from Your Domain Model

Domain events are events that come from your business logic, not from infrastructure. The `Order` entity itself can raise events.

```java
@Entity
public class Order {

    @Transient  // Not stored in DB
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public Order create(OrderDto dto) {
        Order order = new Order(dto);
        order.domainEvents.add(new OrderCreatedEvent(order, dto.getUserId()));
        return order;
    }

    public List<DomainEvent> getDomainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

In the service:

```java
@Transactional
public Order createOrder(OrderDto dto) {
    Order order = Order.create(dto);
    orderRepository.save(order);

    for (DomainEvent event : order.getDomainEvents()) {
        eventPublisher.publishEvent(event);
    }
    order.clearDomainEvents();

    return order;
}
```

---

### Message Brokers — Events Across Services

In-process events work only inside one JVM. For **microservices**, you need a **message broker** — a separate system that stores and delivers events.

| Broker | Best For | Key Trait |
|--------|----------|-----------|
| RabbitMQ | Traditional messaging, routing, queues | Flexible, reliable, easier to start |
| Kafka | High throughput, event streaming, replay | Log-based, scales horizontally |
| Redis Pub/Sub | Simple, fast, ephemeral | No persistence; best for cache invalidation |

---

### RabbitMQ with Spring Boot

**1. Add dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**2. Configuration (`application.yml`):**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

**3. Define exchange, queue, and binding:**
```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange("orders.exchange");
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue("order.created.queue", true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
            .bind(orderCreatedQueue())
            .to(ordersExchange())
            .with("order.created");
    }
}
```

**4. Publish (Producer):**
```java
@Service
public class OrderService {

    private final RabbitTemplate rabbitTemplate;

    public Order createOrder(OrderDto dto) {
        Order order = orderRepository.save(new Order(dto));

        OrderCreatedEvent event = new OrderCreatedEvent(order, dto.getUserId());
        rabbitTemplate.convertAndSend(
            "orders.exchange",
            "order.created",
            event
        );

        return order;
    }
}
```

**5. Consume (Listener):**
```java
@Component
public class EmailNotificationListener {

    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        emailService.sendOrderConfirmation(
            event.getUserId(),
            event.getOrder()
        );
    }
}
```

---

### Kafka with Spring Boot

**1. Add dependency:**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**2. Configuration (`application.yml`):**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: order-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

**3. Producer:**
```java
@Service
public class OrderService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public Order createOrder(OrderDto dto) {
        Order order = orderRepository.save(new Order(dto));

        OrderEvent event = new OrderEvent(
            order.getId(),
            order.getUserId(),
            order.getTotal(),
            "CREATED"
        );

        // Use orderId as key for ordering (same key → same partition)
        kafkaTemplate.send("orders", order.getId().toString(), event);

        return order;
    }
}
```

**4. Consumer:**
```java
@Component
public class OrderEventConsumer {

    @KafkaListener(topics = "orders", groupId = "email-service")
    public void handleOrderEvent(OrderEvent event) {
        if ("CREATED".equals(event.getStatus())) {
            emailService.sendOrderConfirmation(
                event.getUserId(),
                event.getOrderId()
            );
        }
    }
}
```

---

### When to Use Events vs Direct Calls

| Scenario | Prefer | Reason |
|----------|--------|--------|
| Need immediate response (e.g., "Is item in stock?") | Direct call | Event would be too slow and complex |
| Fire-and-forget (e.g., send email, update analytics) | Event | Decouples and doesn't block |
| Multiple consumers for one action | Event | One publish, many listeners |
| Single consumer, tight coupling OK | Either | Events add complexity you may not need |
| Cross-service / cross-JVM | Event + broker | In-process events don't cross process boundaries |
| Must be transactional with DB | Direct call or Transactional Outbox | Events are async; consistency is harder |

---

### Event Best Practices

**1. Immutable events:**
```java
public class OrderCreatedEvent {
    private final Long orderId;
    private final Long userId;
    private final BigDecimal total;
    private final Instant timestamp;

    public OrderCreatedEvent(Long orderId, Long userId, BigDecimal total) {
        this.orderId = orderId;
        this.userId = userId;
        this.total = total;
        this.timestamp = Instant.now();
    }
    // Getters only, no setters
}
```

**2. Keep payloads small:** Send IDs and essential data, not full graphs.

**3. Version events:** Include a `version` field for schema evolution.

**4. Idempotent listeners:** Assume events can be delivered more than once; design logic so duplicate handling is safe.

---

## Layer 3 — Advanced Engineering Depth

### Event Sourcing — Store What Happened, Not Just Current State

**Traditional:** Store current state in tables (e.g., `orders` with `status`, `total`).

**Event sourcing:** Store a log of events. Current state is derived by replaying events.

```
Traditional:
orders: id | userId | total | status
        1  | 100    | 99.99 | CREATED

Event Sourcing:
events: id | orderId | type             | payload
        1  | 1       | OrderCreated     | {"userId":100,"total":99.99}
        2  | 1       | PaymentProcessed | {"amount":99.99}
        3  | 1       | OrderShipped     | {"tracking":"ABC123"}
```

**Rebuilding state:**
```java
public Order getOrder(Long orderId) {
    List<OrderEvent> events = eventStore.getEvents(orderId);
    Order order = new Order();
    for (OrderEvent event : events) {
        order.apply(event);
    }
    return order;
}
```

**Benefits:** Full audit trail, time travel, replay, no implicit data loss. **Trade-offs:** More complexity, need snapshots for performance, event versioning.

---

### CQRS with Events — Separate Reads and Writes

**CQRS** = Command Query Responsibility Segregation. Writes go through a command model; reads use a separate, often denormalized, read model. Events connect them.

```
Write side:                    Read side:
Command → Aggregate → Event    Event → Update Read Model
                ↓                              ↓
           Event Store                   Denormalized View
                                              ↓
                                         Query Service
```

```java
// Write: publish event when order is created
@Service
public class OrderCommandService {
    public void createOrder(CreateOrderCommand cmd) {
        Order order = new Order(cmd);
        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
    }
}

// Read: listen and update view
@Component
public class OrderViewUpdater {
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        OrderView view = new OrderView(
            event.getOrderId(),
            event.getUserId(),
            event.getTotal()
        );
        orderViewRepository.save(view);
    }
}
```

---

### Eventual Consistency

With events, consumers update their state **asynchronously**. For a short time, different parts of the system can see different data.

Example: Order is created → event published → Payment Service processes → event published → Email Service sends. Until all consumers process their events, data is "eventually" consistent, not immediately consistent.

**Mitigation:**
- Use timestamps/versions to detect stale data
- Design UIs to handle "loading" or "pending" states
- Use saga patterns for cross-service consistency where needed

---

### Transactional Outbox Pattern — Ensure Events Are Published

**Problem:** You save an order and publish an event. If publish fails, the order exists but no event — listeners never know.

**Solution:** Write the event to an `outbox` table in the **same transaction** as the order. A separate process reads the outbox and publishes to the broker.

```java
@Entity
public class OutboxEvent {
    @Id
    private String id;
    private String eventType;
    private String payload;
    private Instant createdAt;
    private boolean processed;
}

@Transactional
public Order createOrder(OrderDto dto) {
    Order order = orderRepository.save(new Order(dto));

    OutboxEvent outbox = new OutboxEvent(
        UUID.randomUUID().toString(),
        "OrderCreated",
        objectMapper.writeValueAsString(new OrderCreatedEvent(order)),
        Instant.now(),
        false
    );
    outboxEventRepository.save(outbox);

    return order;
}

@Component
public class OutboxEventPublisher {
    @Scheduled(fixedRate = 5000)
    public void publishOutboxEvents() {
        outboxEventRepository.findByProcessedFalse()
            .forEach(event -> {
                eventPublisher.publishEvent(toEvent(event.getPayload()));
                event.setProcessed(true);
                outboxEventRepository.save(event);
            });
    }
}
```

---

### Dead Letter Queues (DLQ)

When a consumer fails to process a message, it can end up in a **Dead Letter Queue** instead of being lost or endlessly retried.

**RabbitMQ DLQ:**
```java
@Bean
public Queue orderCreatedQueue() {
    return QueueBuilder.durable("order.created.queue")
        .withArgument("x-dead-letter-exchange", "dlx")
        .withArgument("x-dead-letter-routing-key", "order.created.dlq")
        .build();
}
```

Failed messages go to `dlx` → `order.created.dlq`. You can monitor, retry, or handle manually.

---

### Retry Strategies

**1. Spring Retry:**
```java
@Retryable(
    value = { TransientException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void processOrder(OrderEvent event) {
    orderService.process(event);
}
```

**2. RabbitMQ:** Use `basicNack` with requeue, or send to DLQ after N failures.

**3. Kafka:** Use `SeekToCurrentErrorHandler` with `BackOff` to retry before committing offset.

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderEvent>();
    factory.setConsumerFactory(consumerFactory());

    var errorHandler = new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafkaTemplate),
        new FixedBackOff(1000L, 3L)
    );
    factory.setCommonErrorHandler(errorHandler);

    return factory;
}
```

---

### Saga Pattern — Distributed Transactions with Compensation

When a business flow spans multiple services, you can't use a single DB transaction. A **saga** coordinates steps and runs **compensating actions** if something fails.

**Choreography (event-driven):**
```
OrderCreated → InventoryReserved → PaymentProcessed → OrderConfirmed
                     ↓
              PaymentFailed → InventoryReleased (compensation)
```

Each service listens to events and publishes new events. Failures trigger compensating events.

**Orchestration (central coordinator):**
A saga orchestrator calls services in sequence and runs compensation if any step fails.

---

### Event Versioning and Schema Evolution

Events change over time. Include a version:

```java
public class OrderCreatedEvent {
    private final String version = "1.0";
    private final Long orderId;
    private final Long userId;
    private final BigDecimal total;
}
```

Handling multiple versions:
```java
@RabbitListener(queues = "order.created.queue")
public void handle(Object event) {
    if (event instanceof OrderCreatedEventV2 v2) {
        handleV2(v2);
    } else if (event instanceof OrderCreatedEvent v1) {
        handleV1(v1);
    }
}
```

---

### Event Ordering — Kafka Partitions

For ordered processing per entity, use the same partition key (e.g., `orderId`):

```java
kafkaTemplate.send("orders", orderId.toString(), event);
```

Events with the same key go to the same partition. A single consumer per partition processes them in order.

---

## Layer 4 — Interview Mastery

### Q: What is event-driven architecture?

**A:** A design where components communicate by publishing and subscribing to events (asynchronous messages) instead of direct synchronous calls. Publishers announce what happened; subscribers react when ready. Benefits: loose coupling, scalability, resilience.

---

### Q: Event vs message?

**A:** **Event** = something that already happened (past tense, fact). **Message** (as command) = a request to do something. Events describe facts; commands request actions.

---

### Q: Kafka vs RabbitMQ?

**A:**

| Aspect | Kafka | RabbitMQ |
|--------|-------|----------|
| Model | Event log / stream | Message broker |
| Throughput | Very high | High |
| Replay | Yes (log retained) | No (consumed = gone) |
| Ordering | Per partition | Per queue |
| Use case | Streaming, analytics, event sourcing | Traditional messaging, routing |

---

### Q: When to use events vs direct calls?

**A:** Use **events** when: multiple consumers, fire-and-forget, cross-service, need loose coupling. Use **direct calls** when: need immediate response, single consumer, simple flow, strong consistency.

---

### Q: What is @TransactionalEventListener?

**A:** Runs the listener only after the transaction commits (or at another phase). Avoids acting on data that might be rolled back. Use `phase = TransactionPhase.AFTER_COMMIT` so listeners run only when data is committed.

---

### Q: How do you ensure event delivery?

**A:** (1) **Transactional outbox** — write event to DB in same transaction as business data, then publish. (2) **Idempotent consumers** — safe to process same event twice. (3) **Acknowledgments** — only ack after successful processing. (4) **Dead letter queues** — capture failed messages for retry or manual handling.

---

### Q: What is eventual consistency?

**A:** In distributed systems, data may be temporarily inconsistent because updates propagate asynchronously. Systems converge to consistency over time. Design for this: avoid assuming immediate consistency, use versions/timestamps, handle "pending" states in the UI.

---

### Q: How do you handle duplicate events?

**A:** Make consumers **idempotent**. Use a `processed_events` table or idempotency key to detect already-processed events and skip duplicates. Or design operations so repeating them has the same effect (e.g., "set status to X" vs "increment by 1").

---

### Q: What is event sourcing?

**A:** Storing a log of events as the source of truth instead of storing only current state. State is rebuilt by replaying events. Benefits: audit trail, time travel, replay. Used in finance, compliance, complex domains.

---

### Q: What is CQRS?

**A:** Separating write (command) and read (query) models. Writes go through aggregates and publish events. Reads use denormalized views updated by event listeners. Lets you scale and optimize reads independently from writes.

---

### Q: What is the Saga pattern?

**A:** A way to implement distributed transactions using local transactions and compensating actions. If step N fails, run compensating transactions for steps 1..N-1. Two styles: **choreography** (event-driven, each service reacts) and **orchestration** (central coordinator).

---

### Q: What is a Dead Letter Queue (DLQ)?

**A:** A queue where failed messages are sent after retries are exhausted. Prevents loss and infinite retry loops. You can monitor, manually retry, or alert on DLQ content.

---

## Summary

**Layer 1 — Intuition:**
- Traditional = call and wait (phone). Event-driven = announce and move on (bulletin board).
- Events are facts (what happened). An event bus/broker is the central place to publish and deliver them.

**Layer 2 — Professional:**
- Spring `ApplicationEventPublisher` and `@EventListener` for in-process events.
- `@Async` for non-blocking listeners; `@TransactionalEventListener` for transaction-aware handling.
- RabbitMQ and Kafka for cross-service events.
- Prefer events for decoupling, multiple consumers, fire-and-forget; direct calls when you need immediate responses.

**Layer 3 — Advanced:**
- Event sourcing: store events, derive state.
- CQRS: separate write/read models, connected by events.
- Transactional outbox for reliable event publishing.
- Eventual consistency, DLQ, retries, saga patterns, event versioning.

**Layer 4 — Interview:**
- Event-driven = publish/subscribe, loose coupling, async.
- Events vs messages; Kafka vs RabbitMQ; when to use which.
- Idempotency, transactional outbox, eventual consistency, event sourcing, CQRS, saga, DLQ.

---

**Related Chapters:**
- [Chapter 33: Microservices](33-Microservices.md) — Event-driven communication in microservices
- [Chapter 26: Transactions](26-Transactions.md) — Transactional outbox pattern
- [Chapter 32: Logging & Monitoring](32-Logging-Monitoring.md) — Monitoring event flow

---

[← Back to Index](00-README.md) | [Previous: Microservices](33-Microservices.md) | [Next: Testing Spring →](35-Testing-Spring.md)
