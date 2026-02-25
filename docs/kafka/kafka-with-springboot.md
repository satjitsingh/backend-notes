# Kafka with Spring Boot

## Before We Write Code: What Are We Building and Why?

Let's step back. You've learned what Kafka is. You've seen the use cases. Now you want to write code. But before we dive into dependencies and configuration, let's understand the *what* and the *why*.

**What we're building:** A simple order notification system. When someone places an order (via a REST API), we'll publish that order to Kafka. A separate service—the notification service—will read from Kafka and "send" a notification (we'll log it for simplicity). In a real app, that notification might be an email, SMS, or push.

**Why this example?** It's the smallest useful Kafka flow. One producer. One consumer. One topic. You'll see the full cycle. Once you understand this, adding more consumers (payment, inventory, etc.) is trivial—same pattern, different logic.

**Think about it this way:** Imagine a restaurant. The waiter (REST API) takes an order and writes it on a ticket. The ticket goes to the kitchen order rail (Kafka topic). The chef (consumer) picks up tickets when ready and cooks. The waiter doesn't stand there waiting for the chef. The ticket waits on the rail. That's our architecture.

---

## The Architecture: What We'll Build

Here's the full picture before we write a single line of code:

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                    OUR SPRING BOOT APP                    │
                    │                                                           │
                    │   ┌─────────────┐     ┌──────────────┐     ┌───────────┐  │
                    │   │   REST      │     │    Order     │     │ Notification│  │
                    │   │ Controller  │────▶│   Producer  │     │  Consumer   │  │
                    │   │ POST/orders │     │              │     │             │  │
                    │   └─────────────┘     └──────┬───────┘     └──────▲──────┘  │
                    │         │                   │                    │         │
                    │         │                   │  send()            │ poll()  │
                    │         │                   │                    │         │
                    └─────────┼───────────────────┼────────────────────┼─────────┘
                              │                   │                    │
                              │   curl POST       │                    │
                              │                   ▼                    │
                    ┌─────────┴────────────────────────────────────────┴─────────┐
                    │                    KAFKA BROKER (localhost:9092)            │
                    │                                                              │
                    │   Topic: "order-events" (3 partitions)                       │
                    │   ┌──────────┬──────────┬──────────┐                         │
                    │   │ Part 0   │ Part 1   │ Part 2   │                         │
                    │   │ [msg][msg]│ [msg][msg]│ [msg][msg]│                        │
                    │   └──────────┴──────────┴──────────┘                         │
                    │                                                              │
                    └──────────────────────────────────────────────────────────────┘
```

**Flow in plain English:**

1. User sends `POST /api/orders` with order details (product, quantity, price).
2. The REST Controller receives it, creates an `OrderEvent`, and calls the Producer.
3. The Producer sends the event to the Kafka topic "order-events."
4. Kafka stores the message in a partition (based on the key we send).
5. The Consumer is polling Kafka in the background. It receives the message.
6. The Consumer's method runs—we log it. In production, you'd send an email/SMS here.

**Key insight:** The Producer and Consumer run in the *same* Spring Boot app in our example. In production, they'd often be separate services. But the Kafka code is identical either way. The Producer doesn't know who's consuming. The Consumer doesn't know who produced. They're decoupled by the topic.

---

## Step 1: Project Setup

### Dependencies Explained (pom.xml)

Don't just copy-paste. Understand what each dependency does.

```xml
<dependencies>
    <!-- Spring Boot Web: Gives us REST controllers, the embedded Tomcat server, etc. -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Kafka: The main one. Wraps the Kafka Java client. Provides KafkaTemplate,
         @KafkaListener, and all the configuration we need. Without this, we'd write
         raw KafkaProducer/KafkaConsumer code ourselves. -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- Lombok: Generates getters, setters, constructors, etc. Saves boilerplate.
         @Data, @NoArgsConstructor, @AllArgsConstructor come from here. -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Jackson: For JSON serialization. Kafka needs to convert our OrderEvent object
         to bytes for the network. JsonSerializer uses Jackson. We need this for
         JsonSerializer to work with our custom objects. -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

!!! info "Why Jackson?"
    Kafka only sends bytes. When we send an `OrderEvent` object, we need to serialize it. We could use Java's built-in serialization (not recommended—it's fragile). We could use Avro or Protobuf (great for production). For learning, JSON is simplest. Spring Kafka's `JsonSerializer` uses Jackson under the hood. Hence the dependency.

---

### application.yml: Every Property Explained

```yaml
spring:
  kafka:
    # Where is Kafka? This is the address of your Kafka broker(s).
    # For a cluster, you'd list multiple: "broker1:9092,broker2:9092,broker3:9092"
    # The client uses this to discover all brokers in the cluster.
    bootstrap-servers: localhost:9092

    producer:
      # The key is often a string (order ID, user ID). This tells Kafka how to serialize it.
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      # The value is our OrderEvent. JsonSerializer converts it to JSON bytes.
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

    consumer:
      # Consumer group: All consumers with this ID share the partitions.
      # "notification-service" means "we're the notification service reading this topic."
      group-id: notification-service

      # When a consumer first starts and has no saved offset, where does it start?
      # - earliest: From the very first message (good for "process everything")
      # - latest: Only new messages from now (good for "ignore old stuff")
      # - none: Throw error if no offset exists (fail fast)
      auto-offset-reset: earliest

      # Kafka sends bytes. We need to deserialize back to strings and objects.
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

      properties:
        # JsonDeserializer needs to know which Java packages it can deserialize.
        # "*" means "trust all" (fine for dev). In production, restrict to your package.
        spring.json.trusted.packages: "*"
```

**What happens if you change each one?**

| Property | Change | Effect |
|----------|--------|--------|
| `bootstrap-servers` | Wrong port | "Connection refused" — app can't reach Kafka |
| `key-serializer` | Wrong type | Serialization error when sending |
| `value-serializer` | Wrong type | Consumer gets "Deserialization error" |
| `group-id` | Different | Same consumer code, but different group = new offset, might read from start |
| `auto-offset-reset` | `latest` | New consumer skips all existing messages |
| `trusted.packages` | Missing | "Deserialization error: Class not trusted" |

---

## Step 2: The Event Class

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String orderId;
    private String product;
    private int quantity;
    private double totalPrice;
    private String status;
}
```

**Simple.** This is the POJO we'll send through Kafka. `@Data` creates getters, setters, `toString`, `equals`, `hashCode`. `@NoArgsConstructor` and `@AllArgsConstructor` are needed for Jackson to deserialize JSON into this object. When the consumer receives JSON bytes, Jackson will create an `OrderEvent` from it.

!!! tip "Production Best Practice"
    In production, use a schema registry (Avro, Protobuf) so producers and consumers agree on the message format. If you add a new field, old consumers won't break. For learning, JSON is fine.

---

## Step 3: Kafka Topic Configuration

```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name("order-events")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

**What this does:** When the Spring Boot app starts, it creates the topic "order-events" if it doesn't exist. 3 partitions for parallelism. 1 replica (fine for local dev; production would use 2 or 3).

**Why 3 partitions?** We can have up to 3 consumers in the same group reading in parallel. More partitions = more parallelism. But don't over-partition—each partition has overhead.

---

## Step 4: Producer (Send Messages)

```java
@Service
@Slf4j
public class OrderProducer {

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private static final String TOPIC = "order-events";

    public void sendOrderEvent(OrderEvent event) {
        log.info("Sending order event: {}", event.getOrderId());

        kafkaTemplate.send(TOPIC, event.getOrderId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent to partition {} offset {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send: {}", ex.getMessage());
                }
            });
    }
}
```

### Line-by-Line Walkthrough

| Line | What It Does |
|------|--------------|
| `KafkaTemplate<String, OrderEvent>` | Spring's helper for sending messages. The key is `String`, the value is our `OrderEvent`. Spring injects this bean—we configured it via `application.yml`. |
| `kafkaTemplate.send(TOPIC, event.getOrderId(), event)` | Sends a message. **Topic:** "order-events." **Key:** `orderId`—Kafka uses this to decide which partition. Same key = same partition = ordering guarantee for that order. **Value:** `event`—the full object. |
| `event.getOrderId()` as key | All events for order "abc-123" go to the same partition. If we later send "order updated" for the same order, it goes to the same partition. Consumer processes in order. |
| `.whenComplete(...)` | `send()` returns a `CompletableFuture`. It's async. We don't block. When Kafka acknowledges (or fails), this callback runs. We log success (partition, offset) or failure. |

**Why use the order ID as the key?** So that all messages for the same order go to the same partition. That guarantees ordering. If we used `null` as the key, Kafka would round-robin across partitions—no ordering guarantee.

!!! example "Expected Console Output (Producer)"
    When you send an order, you'll see:
    ```
    Sending order event: 3f2a1b4c-8d9e-4f5a-b6c7-1234567890ab
    Sent to partition 1 offset 0
    ```
    The partition might be 0, 1, or 2 (depends on the hash of the key). The offset is the position within that partition.

---

## Step 5: Consumer (Read Messages)

```java
@Service
@Slf4j
public class NotificationConsumer {

    @KafkaListener(
        topics = "order-events",
        groupId = "notification-service"
    )
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: {}", event);
        log.info("Order {} - Status: {} - Sending notification...",
            event.getOrderId(), event.getStatus());

        // In real app: send email, SMS, push notification
    }
}
```

### What @KafkaListener Does Behind the Scenes

When Spring Boot starts, it scans for `@KafkaListener` methods. For each one, it:

1. **Creates a Kafka consumer** (or uses a shared one if configured).
2. **Subscribes to the topic** with the specified `groupId`.
3. **Starts a background thread** that polls Kafka in a loop: "Any new messages?"
4. **When a message arrives**, it deserializes the bytes into an `OrderEvent` (using our `JsonDeserializer`).
5. **Invokes your method** with that object.
6. **Commits the offset** after your method returns successfully (unless you configure manual commit).

You don't write the polling loop. You don't write the deserialization. You just write the method. Spring does the rest.

**What if your method throws an exception?** By default, the offset is not committed. The consumer will retry the same message. We'll cover retry and Dead Letter Topic later.

!!! example "Expected Console Output (Consumer)"
    A few milliseconds after the producer sends, you'll see:
    ```
    Received order event: OrderEvent(orderId=3f2a1b4c-8d9e-4f5a-b6c7-1234567890ab, product=Laptop, quantity=1, totalPrice=50000.0, status=CREATED)
    Order 3f2a1b4c-8d9e-4f5a-b6c7-1234567890ab - Status: CREATED - Sending notification...
    ```

---

## Step 6: REST Controller (Trigger Events)

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderProducer producer;

    @PostMapping
    public ResponseEntity<String> placeOrder(
            @RequestBody OrderEvent order) {

        order.setOrderId(UUID.randomUUID().toString());
        order.setStatus("CREATED");

        producer.sendOrderEvent(order);

        return ResponseEntity.ok(
            "Order placed: " + order.getOrderId());
    }
}
```

**Flow:** User POSTs JSON. We generate an order ID, set status, send to Kafka, return immediately. We don't wait for the consumer. We don't wait for the notification. Fire and forget.

---

## Step 7: Test It

### Start Kafka

```bash
docker-compose up -d
```

*(Make sure you have a `docker-compose.yml` with Kafka and Zookeeper. Standard setup.)*

### Run the App

```bash
mvn spring-boot:run
```

### Send a Test Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product": "Laptop", "quantity": 1, "totalPrice": 50000}'
```

### Expected Logs (Full Sequence)

**Producer side:**
```
Sending order event: 3f2a1b4c-8d9e-4f5a-b6c7-1234567890ab
Sent to partition 1 offset 0
```

**Consumer side:**
```
Received order event: OrderEvent(orderId=3f2a1b4c-8d9e-4f5a-b6c7-1234567890ab, product=Laptop, quantity=1, totalPrice=50000.0, status=CREATED)
Order 3f2a1b4c-8d9e-4f5a-b6c7-1234567890ab - Status: CREATED - Sending notification...
```

---

## Debugging Guide

### How to Check if Kafka Is Receiving Messages

**Use the Kafka console consumer.** It's a built-in CLI tool that reads from a topic and prints messages to the console.

```bash
# Read from the beginning of the topic
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events --from-beginning

# Or just new messages (from now)
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events
```

**What you'll see:** If you're sending JSON, you might see raw bytes. To see readable JSON, add:

```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events \
  --from-beginning \
  --property print.key=true \
  --property key.separator=": "
```

**Interpretation:** If messages appear here when you POST an order, your producer works. If your Spring consumer doesn't show logs, the issue is on the consumer side (group ID, deserialization, etc.).

### How to Check Consumer Lag

Consumer lag = "How many messages are waiting for the consumer to process?"

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group notification-service
```

**Output:**
```
GROUP                 TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
notification-service  order-events    0          5               5               0
notification-service  order-events    1          3               3               0
notification-service  order-events    2          4               4               0
```

**LAG = 0** means the consumer is caught up. **LAG = 1000** means 1000 messages are waiting. High lag = consumer is slow or stuck.

### Common Errors and Their Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Connection refused` | Kafka not running or wrong host/port | Start Kafka. Check `bootstrap-servers` in `application.yml`. |
| `Topic 'order-events' not present` | Topic doesn't exist | Create it manually or ensure `KafkaTopicConfig` runs. Add `spring.kafka.admin.auto-create: true` if needed. |
| `Deserialization error` | Wrong format or class not trusted | Check `value-deserializer`. Add `spring.json.trusted.packages: "*"` or your package name. |
| `Class not found` for OrderEvent | Consumer doesn't have the class | Ensure both producer and consumer have the same `OrderEvent` class (or shared library). |
| `Consumer group is rebalancing` | Consumers joining/leaving | Normal during startup. Wait a few seconds. If it keeps happening, a consumer might be crashing. |
| `Offset out of range` | Stored offset is invalid (e.g., topic was deleted) | Set `auto-offset-reset: earliest` or `latest` to start fresh. |

!!! warning "Serialization Error Debugging"
    If you see "Deserialization error" or "Unknown magic byte," the consumer received bytes it can't parse. Common causes: (1) Producer sends JSON, consumer expects String. (2) Schema changed—old messages have different format. (3) Wrong `trusted.packages`. Check both sides use the same serializers/deserializers.

---

## Error Handling: Retry and Dead Letter Topic

### The Postal Mail Analogy

Imagine you're the post office. A letter arrives. You try to deliver it. The recipient's door is locked. What do you do?

**Option 1:** Throw the letter away. Bad—lost mail.

**Option 2:** Retry. Try again in 1 hour. Still locked? Try again. After 3 attempts, you give up.

**Option 3:** After 3 failed attempts, you don't throw it away. You put it in a "Return to Sender" pile. Someone reviews it later. Maybe the address was wrong. Maybe the recipient moved. You handle it manually.

**Kafka equivalent:** Option 2 = Retry. Option 3 = Dead Letter Topic (DLT). Messages that fail after all retries go to a separate topic. You can inspect them, fix the issue, and retry manually.

### How to Implement

```java
@KafkaListener(topics = "order-events", groupId = "notification-service")
@RetryableTopic(
    attempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2),
    dltTopic = "order-events-dlt"
)
public void handleOrderEvent(OrderEvent event) {
    log.info("Processing: {}", event.getOrderId());
    // If this throws an exception, it retries 3 times with 1s, 2s, 4s delays
    // After 3 failures, message goes to order-events-dlt
}

@DltHandler
public void handleDlt(OrderEvent event) {
    log.error("Failed after retries. Order: {} - Manual review needed.", event.getOrderId());
    // Save to database for manual review
    // Or send alert to ops team
}
```

!!! info "Requires spring-kafka 2.8+"
    `@RetryableTopic` is included in Spring Kafka 2.8+. No extra dependency. It works automatically with `@KafkaListener`. For older versions, use manual retry logic or a custom `RetryingMessageListenerAdapter`.

**Flow:**
```
order-events → Consumer → Success ✅
                ↓
                Fail → Retry 1 (1s delay) → Fail → Retry 2 (2s delay) → Fail → Retry 3 (4s delay) → Fail
                                                                                                    ↓
                                                                                        order-events-dlt
                                                                                        (Dead Letter Topic)
```

---

## Testing: Unit Tests for Kafka Producers and Consumers

### Testing the Producer

You don't need a real Kafka broker. Use an embedded Kafka or mock the `KafkaTemplate`.

**Option 1: Mock KafkaTemplate**

```java
@SpringBootTest
class OrderProducerTest {

    @Autowired
    private OrderProducer producer;

    @MockBean
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Test
    void sendOrderEvent_shouldInvokeKafkaTemplate() {
        OrderEvent event = new OrderEvent("order-1", "Laptop", 1, 50000, "CREATED");

        producer.sendOrderEvent(event);

        verify(kafkaTemplate, times(1)).send(eq("order-events"), eq("order-1"), eq(event));
    }
}
```

**Option 2: Embedded Kafka**

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "order-events" })
class OrderProducerIntegrationTest {

    @Autowired
    private OrderProducer producer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void sendOrderEvent_shouldBeReceivedByConsumer() throws Exception {
        OrderEvent event = new OrderEvent("order-1", "Laptop", 1, 50000, "CREATED");

        producer.sendOrderEvent(event);

        Consumer<String, OrderEvent> consumer = createConsumer();
        ConsumerRecords<String, OrderEvent> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(1);
        assertThat(records.iterator().next().value().getOrderId()).isEqualTo("order-1");
    }
}
```

**Add dependency for embedded Kafka:**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Testing the Consumer

**Option 1: Mock the logic**

Your consumer might call a `NotificationService`. Mock that. Use `@KafkaListener` with a test-specific topic, or use `@EmbeddedKafka` and produce a message, then assert the mock was called.

**Option 2: Integration test with Embedded Kafka**

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "order-events" })
class NotificationConsumerTest {

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    private NotificationService notificationService; // Mock this

    @Test
    void whenOrderEventPublished_consumerShouldProcess() throws Exception {
        OrderEvent event = new OrderEvent("order-1", "Laptop", 1, 50000, "CREATED");

        kafkaTemplate.send("order-events", "order-1", event);

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> verify(notificationService).send(any()));
    }
}
```

!!! tip "Testing Best Practice"
    Unit test the business logic (e.g., "given this order, what notification do we send?"). Integration test the Kafka wiring ("does the consumer receive and process?"). Don't over-test the framework—trust Spring Kafka.

---

## Adding Multiple Consumers

### Payment Consumer

```java
@Service
@Slf4j
public class PaymentConsumer {

    @KafkaListener(
        topics = "order-events",
        groupId = "payment-service"
    )
    public void handlePayment(OrderEvent event) {
        log.info("Processing payment for order: {}", event.getOrderId());
        // Process payment logic
    }
}
```

### Inventory Consumer

```java
@Service
@Slf4j
public class InventoryConsumer {

    @KafkaListener(
        topics = "order-events",
        groupId = "inventory-service"
    )
    public void handleInventory(OrderEvent event) {
        log.info("Updating inventory for order: {}", event.getOrderId());
        // Update inventory logic
    }
}
```

!!! info "Key Point"
    **Different `groupId`** = each consumer gets **ALL** messages independently. Same topic, three different groups. Notification, Payment, and Inventory each get every order. **Same `groupId`** = consumers **share** partitions. Two consumers in "notification-service" would split the work—each gets half the messages.

---

## Producer and Consumer with Custom Configuration

Sometimes you need more control than `application.yml` provides. For example, you want `acks=all` and idempotent producer for critical data.

### Custom Producer Config

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, OrderEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");           // Wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3);           // Retry 3 times on failure
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // No duplicates on retry
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Custom Consumer Config

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, OrderEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent>
            kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 3 consumer threads = 3 partitions in parallel
        return factory;
    }
}
```

**`setConcurrency(3)`:** Creates 3 consumer threads. If you have 3 partitions, each thread gets one partition. Throughput triples.

---

## Quick Summary

| Concept | Spring Kafka Way |
|---------|------------------|
| Send message | `KafkaTemplate.send(topic, key, value)` |
| Receive message | `@KafkaListener(topics = "...", groupId = "...")` |
| Create topic | `TopicBuilder.name().partitions().replicas().build()` |
| Serialize | `JsonSerializer` / `StringSerializer` |
| Deserialize | `JsonDeserializer` / `StringDeserializer` |
| Error handling | `@RetryableTopic` + `@DltHandler` |
| Concurrency | `factory.setConcurrency(N)` |
| Config | `application.yml` or Java config beans |

---

## Exercise

1. Create a Spring Boot app with a producer and consumer. Send an order event via REST API and see the consumer log it.
2. Add a second consumer with a different `groupId`. Verify both receive the message.
3. Make the consumer throw an exception. See retries in action.
4. Add a Dead Letter Topic handler.

---

**Next up:** [Kafka Interview Questions](kafka-interview-questions.md) - Get interview ready!
