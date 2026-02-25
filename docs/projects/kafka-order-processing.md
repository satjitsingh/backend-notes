# Project: Kafka Order Processing

## The Business Scenario

Meet **ShopFast**, a growing e-commerce company. They started with a simple monolith: user places order, payment processes, inventory updates, and an email goes out—all in one application. It worked. But as they scaled, problems appeared.

**The pain points:**

- Payment processing sometimes took 5 seconds. The whole request blocked. Users saw timeouts.
- Inventory updates failed silently. Orders went through but stock wasn't reduced. Overselling became a nightmare.
- The notification team wanted to add SMS and push notifications. That meant modifying the core order flow. Risky.
- Black Friday hit. The database couldn't keep up. One slow service (payment gateway) slowed everything.

**The solution:** Event-driven architecture with Kafka. When an order is placed, we publish an event to Kafka. We don't wait for payment, inventory, or notifications. We just say "order placed" and return immediately. Downstream services—Payment, Inventory, Notification—each listen to the same event and do their job independently. If payment is slow, that's fine. The order is recorded. Payment will catch up. If the notification service is down, messages queue up in Kafka. No data loss.

!!! tip "Real-World Analogy"
    Think of it like a restaurant. The waiter (Order Service) takes your order and hands a ticket to the kitchen. The waiter doesn't stand there waiting for the chef to cook. The chef (Payment), the person managing ingredients (Inventory), and the person who calls you when your order is ready (Notification) each get a copy of the ticket and work in parallel. The waiter is free to take more orders.

---

## Architecture Overview: Each Service's Role

```
                    ┌─────────────────────────────────────────────────────────────────┐
                    │                    KAFKA ORDER PROCESSING FLOW                    │
                    └─────────────────────────────────────────────────────────────────┘

    User                Order Service              Kafka                Payment Service
      │                       │                       │                        │
      │  POST /orders         │                       │                        │
      │ ────────────────────▶│                       │                        │
      │                       │  Save to DB           │                        │
      │                       │  Publish "orders"     │                        │
      │                       │ ────────────────────▶│                        │
      │  Order created        │                       │  Consume order         │
      │ ◀────────────────────│                       │ ──────────────────────▶│
      │  (instant response)   │                       │                        │
      │                       │                       │  Validate & charge     │
      │                       │                       │  Publish "payment-     │
      │                       │                       │  results"              │
      │                       │                       │ ◀──────────────────────│
      │                       │                       │                        │
      │                       │                       │  Inventory Service     │
      │                       │                       │  Consume "orders"       │
      │                       │                       │ ──────────────────────▶│
      │                       │                       │  Reduce stock          │
      │                       │                       │                        │
      │                       │                       │  Notification Service   │
      │                       │                       │  Consume "orders" +     │
      │                       │                       │  "payment-results"     │
      │                       │                       │ ──────────────────────▶│
      │                       │                       │  Send email/SMS         │
      │                       │                       │                        │
      │                       │  Order Service         │                        │
      │                       │  Consume "payment-   │                        │
      │                       │  results"             │                        │
      │                       │ ◀─────────────────────│                        │
      │                       │  Update order status  │                        │
      │                       │  (PAID / FAILED)      │                        │
```

### Service Responsibilities

| Service | Role | Consumes | Produces |
|---------|------|----------|----------|
| **Order Service** | Receives order from user, saves to DB, publishes to Kafka. Updates order status when payment completes. | `payment-results` | `orders` |
| **Payment Service** | Validates and charges. Publishes success/failure. | `orders` | `payment-results` |
| **Inventory Service** | Reduces stock for the ordered product. | `orders` | — |
| **Notification Service** | Sends order confirmation and payment status emails. | `orders`, `payment-results` | — |

---

## Event Flow Step by Step

### 1. Order Placed

User sends: `POST /api/orders` with `{ "customerId": "CUST-001", "product": "Laptop", "quantity": 1, "totalAmount": 50000 }`

**What happens:**

1. Order Service generates `orderId` (e.g., `ORD-A1B2C3D4`).
2. Order is saved to the database with status `CREATED`.
3. Order Service publishes `OrderEvent` to Kafka topic `orders` with key `orderId`.
4. API returns immediately with the order details. **User gets a response in ~50ms.**

### 2. Payment Service Receives Order

**What happens:**

1. Payment Service (consumer with `groupId=payment-service`) receives the event.
2. It simulates payment: if `totalAmount < 100000`, success; else failure.
3. It publishes `PaymentEvent` to topic `payment-results` with status `SUCCESS` or `FAILED`.

### 3. Inventory Service Receives Order

**What happens:**

1. Inventory Service (consumer with `groupId=inventory-service`) receives the same event.
2. It logs "Reducing stock for Laptop by 1".
3. In a real app, it would update a `stock` table in the database.

### 4. Notification Service Receives Order

**What happens:**

1. Notification Service (consumer with `groupId=notification-service`) receives the event.
2. It logs "Sending confirmation for order ORD-A1B2C3D4".
3. In a real app, it would call an email/SMS API.

### 5. Payment Result → Order Update

**What happens:**

1. Order Service has another consumer listening to `payment-results` (groupId `order-service`).
2. When Payment Service publishes a result, Order Service receives it.
3. Order Service updates the order in the database: status becomes `PAID` or `PAYMENT_FAILED`.

### 6. Notification Service Receives Payment Result

**What happens:**

1. Notification Service also listens to `payment-results`.
2. If success: logs "Payment successful for order X".
3. If failed: logs "Payment FAILED. Alerting customer."

---

## Step 1: Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
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

---

## Step 2: application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      properties:
        enable.idempotence: true
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"

  datasource:
    url: jdbc:h2:mem:orderdb
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**What's happening here?**

- `acks: all` — Producer waits for all replicas to acknowledge. Ensures no data loss if a broker fails.
- `enable.idempotence: true` — Prevents duplicate messages if the producer retries. Critical for order processing.
- `auto-offset-reset: earliest` — If consumer has no offset (e.g., first run), start from the beginning of the topic.
- `spring.json.trusted.packages: "*"` — Allow deserialization of our event classes. In production, specify exact packages for security.

---

## Step 3: Event Classes

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String orderId;
    private String customerId;
    private String product;
    private int quantity;
    private double totalAmount;
    private String status;
    private LocalDateTime timestamp;
}
```

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String orderId;
    private String paymentId;
    private double amount;
    private String status; // SUCCESS or FAILED
    private LocalDateTime timestamp;
}
```

**What's happening here?**

- These are DTOs (Data Transfer Objects) for Kafka messages. They get serialized to JSON when sent and deserialized when received.
- `orderId` in both events links the payment result back to the order.

---

## Step 4: Topic Configuration

```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic paymentResultsTopic() {
        return TopicBuilder.name("payment-results")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

**What's happening here?**

- `partitions(3)` — Topic is split into 3 partitions. Messages with the same key (orderId) go to the same partition, preserving order per order.
- `replicas(1)` — Single broker for demo. In production, use at least 2 for fault tolerance.

---

## Step 5: Order Entity & Repository

```java
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private String orderId;
    private String customerId;
    private String product;
    private int quantity;
    private double totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## Step 6: Order Service (Producer)

```java
@Service
@Slf4j
public class OrderService {

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    public OrderEvent placeOrder(OrderEvent request) {
        String orderId = "ORD-" + UUID.randomUUID()
            .toString().substring(0, 8).toUpperCase();

        request.setOrderId(orderId);
        request.setStatus("CREATED");
        request.setTimestamp(LocalDateTime.now());

        Order order = new Order(
            orderId, request.getCustomerId(),
            request.getProduct(), request.getQuantity(),
            request.getTotalAmount(), "CREATED",
            LocalDateTime.now(), LocalDateTime.now()
        );
        orderRepository.save(order);

        kafkaTemplate.send("orders", orderId, request)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Order {} sent to Kafka, partition {}",
                        orderId,
                        result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to send order {}: {}",
                        orderId, ex.getMessage());
                }
            });

        return request;
    }

    public void updateOrderStatus(String orderId, String status) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Order {} status updated to {}", orderId, status);
        });
    }
}
```

**What's happening here?**

- `kafkaTemplate.send("orders", orderId, request)` — Key is `orderId`. All events for the same order go to the same partition. Ordering is preserved.
- We save to DB first, then send to Kafka. If Kafka fails, we could add retry or a dead-letter queue. The order is already persisted.

**Expected log output:**

```
Order ORD-A1B2C3D4 sent to Kafka, partition 1
```

---

## Step 7: Payment Service (Consumer + Producer)

```java
@Service
@Slf4j
public class PaymentService {

    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @KafkaListener(topics = "orders", groupId = "payment-service")
    public void processPayment(OrderEvent order) {
        log.info("Payment Service received order: {}", order.getOrderId());

        boolean paymentSuccess = order.getTotalAmount() < 100000;

        PaymentEvent paymentEvent = new PaymentEvent(
            order.getOrderId(),
            "PAY-" + UUID.randomUUID().toString().substring(0, 8),
            order.getTotalAmount(),
            paymentSuccess ? "SUCCESS" : "FAILED",
            LocalDateTime.now()
        );

        kafkaTemplate.send("payment-results",
            order.getOrderId(), paymentEvent);

        log.info("Payment {} for order {}: {}",
            paymentEvent.getPaymentId(),
            order.getOrderId(),
            paymentEvent.getStatus());
    }
}
```

**What's happening here?**

- `groupId = "payment-service"` — This consumer group gets its own copy of every message. It doesn't compete with Inventory or Notification.
- Our "payment logic" is a simple rule: amount < 100000 = success. In reality, you'd call a payment gateway.

**Expected log output:**

```
Payment Service received order: ORD-A1B2C3D4
Payment PAY-X1Y2Z3 for order ORD-A1B2C3D4: SUCCESS
```

---

## Step 8: Inventory Service (Consumer)

```java
@Service
@Slf4j
public class InventoryService {

    @KafkaListener(topics = "orders", groupId = "inventory-service")
    public void updateInventory(OrderEvent order) {
        log.info("Inventory Service: Reducing stock for {} by {}",
            order.getProduct(), order.getQuantity());

        // In real app: update database stock count
        log.info("Inventory updated for order: {}",
            order.getOrderId());
    }
}
```

**Expected log output:**

```
Inventory Service: Reducing stock for Laptop by 1
Inventory updated for order: ORD-A1B2C3D4
```

---

## Step 9: Notification Service (Consumer)

```java
@Service
@Slf4j
public class NotificationService {

    @KafkaListener(topics = "orders", groupId = "notification-service")
    public void sendOrderConfirmation(OrderEvent order) {
        log.info("Notification: Sending confirmation for order {}",
            order.getOrderId());
        log.info("Email sent to customer {} for product {}",
            order.getCustomerId(), order.getProduct());
    }

    @KafkaListener(
        topics = "payment-results",
        groupId = "notification-service"
    )
    public void sendPaymentNotification(PaymentEvent payment) {
        if ("SUCCESS".equals(payment.getStatus())) {
            log.info("Notification: Payment successful for order {}",
                payment.getOrderId());
        } else {
            log.info("Notification: Payment FAILED for order {}. " +
                "Alerting customer.", payment.getOrderId());
        }
    }
}
```

**Expected log output:**

```
Notification: Sending confirmation for order ORD-A1B2C3D4
Email sent to customer CUST-001 for product Laptop
Notification: Payment successful for order ORD-A1B2C3D4
```

---

## Step 10: Payment Result Handler

```java
@Service
@Slf4j
public class PaymentResultHandler {

    @Autowired
    private OrderService orderService;

    @KafkaListener(
        topics = "payment-results",
        groupId = "order-service"
    )
    public void handlePaymentResult(PaymentEvent payment) {
        String newStatus = "SUCCESS".equals(payment.getStatus())
            ? "PAID" : "PAYMENT_FAILED";

        orderService.updateOrderStatus(
            payment.getOrderId(), newStatus);

        log.info("Order {} updated to {} based on payment result",
            payment.getOrderId(), newStatus);
    }
}
```

**Expected log output:**

```
Order ORD-A1B2C3D4 updated to PAID based on payment result
```

---

## What Happens When Payment Fails?

Let's say the order total is 150000 (above our 100000 threshold). Payment Service will publish `status: FAILED`.

**Flow:**

1. Payment Service: `Payment PAY-X1Y2Z3 for order ORD-A1B2C3D4: FAILED`
2. Order Service (PaymentResultHandler): Updates order status to `PAYMENT_FAILED`
3. Notification Service: `Notification: Payment FAILED for order ORD-A1B2C3D4. Alerting customer.`

**What about Inventory?** Inventory already ran when the order was placed. In a real system, you'd need a **compensation flow**: when payment fails, you might publish an "order-cancelled" event that Inventory consumes to restore stock. This project keeps it simple.

!!! warning "Saga Pattern"
    In production, you'd implement a Saga: a sequence of local transactions with compensating actions when something fails. Payment failed → send "cancel order" event → Inventory restores stock.

---

## What Happens When Notification Service Is Down?

1. Order is placed. Event goes to Kafka.
2. Payment Service processes. Inventory Service processes. Order Service updates status.
3. Notification Service is down. It never consumes the message.

**What happens to the message?** It stays in Kafka. When Notification Service comes back up, it reads from its last committed offset and processes all missed messages. No loss. The customer might get the confirmation email a few minutes late, but they'll get it.

!!! tip "Kafka's Superpower"
    Messages are retained (default 7 days, configurable). Consumers can catch up. This is why Kafka beats traditional message queues for event-driven systems.

---

## Testing the Full Flow

### Start Kafka

```bash
docker-compose up -d
```

*(Ensure you have a docker-compose.yml with Kafka and Zookeeper.)*

### Run the App

```bash
mvn spring-boot:run
```

### Test 1: Place a Successful Order (amount < 100000)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "product": "Laptop",
    "quantity": 1,
    "totalAmount": 50000
  }'
```

**Expected response:**

```json
{
  "orderId": "ORD-A1B2C3D4",
  "customerId": "CUST-001",
  "product": "Laptop",
  "quantity": 1,
  "totalAmount": 50000,
  "status": "CREATED",
  "timestamp": "2025-02-25T10:30:00"
}
```

**Expected logs (in order):**

```
1. Order ORD-A1B2C3D4 sent to Kafka, partition X
2. Payment Service received order: ORD-A1B2C3D4
3. Payment PAY-X1Y2Z3 for order ORD-A1B2C3D4: SUCCESS
4. Inventory Service: Reducing stock for Laptop by 1
5. Notification: Sending confirmation for order ORD-A1B2C3D4
6. Notification: Payment successful for order ORD-A1B2C3D4
7. Order ORD-A1B2C3D4 updated to PAID based on payment result
```

### Test 2: Place a Failing Order (amount >= 100000)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-002",
    "product": "Car",
    "quantity": 1,
    "totalAmount": 150000
  }'
```

**Expected logs:**

```
Payment PAY-XXXX for order ORD-XXXX: FAILED
Notification: Payment FAILED for order ORD-XXXX. Alerting customer.
Order ORD-XXXX updated to PAYMENT_FAILED based on payment result
```

### Test 3: Check Order Status

```bash
curl http://localhost:8080/api/orders/ORD-A1B2C3D4
```

**Expected response:** Status should be `PAID` for the 50000 order.

---

## How to Monitor This System

| What to Monitor | How |
|-----------------|-----|
| **Consumer lag** | Kafka tracks how far behind each consumer group is. Use Kafka Manager, Confluent Control Center, or `kafka-consumer-groups.sh --describe`. High lag = consumers can't keep up. |
| **Failed payments** | Add metrics (Micrometer) for payment failures. Alert when failure rate spikes. |
| **Order status distribution** | Track CREATED vs PAID vs PAYMENT_FAILED. Detect if orders are stuck in CREATED (payment service down?). |
| **End-to-end latency** | Measure time from order placed to status PAID. Should be seconds. |

---

## Production Considerations

| Current State | Production Recommendation |
|---------------|---------------------------|
| Single Kafka broker | Use 3+ brokers, replication factor 2 or 3 |
| `replicas(1)` | `replicas(2)` or `replicas(3)` |
| H2 in-memory DB | PostgreSQL or MySQL with proper connection pooling |
| Simulated payment | Integrate Stripe, PayPal, or your payment gateway |
| No retry on consumer failure | Add `@RetryableTopic` or manual retry with dead-letter topic |
| No idempotency key | Use orderId + paymentId to avoid duplicate processing |
| Trusted packages "*" | Specify exact package names for deserialization |

---

## Quick Summary

| Concept | Implementation |
|---------|-----------------|
| Event-driven architecture | Services communicate via Kafka topics |
| Multiple consumer groups | Each service processes independently |
| Event chaining | Payment result triggers order update |
| Idempotent producer | `enable.idempotence: true` |
| Message keys | orderId ensures ordering per order |

---

**Next project:** [Kafka Notification System](kafka-notification-system.md)
