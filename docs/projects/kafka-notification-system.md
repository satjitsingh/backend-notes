# Project: Kafka Notification System

## Why Build a Notification System with Kafka?

Your e-commerce app needs to send notifications. Order confirmed? Send email. Payment failed? Send SMS and email. New promo? Send to email, SMS, and push. The list grows. And here's the catch: **every service that needs to send a notification would have to know how to send it.**

Without Kafka, your Order Service would need to:
- Call the Email API
- Call the SMS API
- Call the Push API
- Handle retries if any fail
- Not block the order flow while waiting for notifications

That's tight coupling. If you add WhatsApp tomorrow, you'd change Order Service, Payment Service, and every other service that sends notifications.

**With Kafka:** Any service publishes a single event: "Send this notification." It doesn't care how. Dedicated consumers—Email Consumer, SMS Consumer, Push Consumer—each listen and do their job. Add WhatsApp? Add a new consumer. Zero changes to producers.

!!! tip "Business Need"
    Notifications are a cross-cutting concern. Order, Payment, User, Marketing—they all need to notify users. Kafka lets you centralize notification logic and scale each channel independently.

---

## The Fan-Out Pattern: A Newspaper Analogy

Imagine a newspaper publisher. They print one edition. Then they distribute it:
- Some copies go to subscribers' doorsteps (Email)
- Some go to newsstands (SMS)
- Some go to digital readers (Push)

**One publication, many delivery channels.** The publisher doesn't drive to each house. They hand the paper to the postal service, the newsstand distributor, and the digital platform. Each channel handles delivery in its own way.

Kafka does the same. One topic: `notifications`. One producer publishes: "Notify user X about Y." Multiple consumers (different consumer groups) each get a copy:
- Email Consumer → sends email
- SMS Consumer → sends SMS
- Push Consumer → sends push notification

**Why different consumer groups?** Each group gets every message. If Email and SMS were in the same group, they'd share the work—each message would go to only one of them. We want **all** of them to process **every** message (when the channel matches). So each channel has its own `groupId`.

---

## Why Each Consumer Has a Different groupId

This confuses beginners. Let's break it down.

**Kafka consumer groups:** Messages in a topic are distributed among consumers in the same group. One message goes to one consumer in the group. If you have 3 consumers in group "email-service," each message goes to exactly one of them.

**Different groups:** Each group gets a full copy of every message. Group "email-service" gets the message. Group "sms-service" gets the same message. Group "push-service" gets it too.

```
    Topic: notifications
    Message: { userId: "U1", channel: "ALL", title: "Order confirmed!" }
                    │
                    ├──▶ groupId: email-service  → Email Consumer processes it
                    ├──▶ groupId: sms-service    → SMS Consumer processes it
                    └──▶ groupId: push-service   → Push Consumer processes it
```

**If they shared a groupId:** Only one consumer would get the message. We'd have to route inside that consumer. Messy. With separate groups, each channel is independent. Email can scale to 5 instances, SMS to 2, Push to 3—each group scales on its own.

---

## What Happens When channel="ALL" vs channel="EMAIL"

### channel="ALL"

A producer sends: `{ "userId": "U1", "channel": "ALL", "title": "50% Off!", "message": "..." }`

**Each consumer receives the message.** They check the `channel` field:
- Email Consumer: `"ALL".equals(channel) || "EMAIL".equals(channel)` → true → send email
- SMS Consumer: `"ALL".equals(channel) || "SMS".equals(channel)` → true → send SMS
- Push Consumer: `"ALL".equals(channel) || "PUSH".equals(channel)` → true → send push

**Result:** User gets email, SMS, and push. All three channels fire.

### channel="EMAIL"

A producer sends: `{ "userId": "U1", "channel": "EMAIL", "title": "Welcome!", "message": "..." }`

**Each consumer still receives the message.** They check the `channel` field:
- Email Consumer: `"EMAIL".equals(channel)` → true → send email
- SMS Consumer: `"SMS".equals(channel)` → false → **skip** (return early)
- Push Consumer: `"PUSH".equals(channel)` → false → **skip**

**Result:** User gets only email. SMS and Push do nothing. This avoids spamming users who only want email.

---

## Step 1: Notification Event

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String notificationId;
    private String userId;
    private String type;        // ORDER_CONFIRMED, PAYMENT_FAILED, etc.
    private String channel;     // ALL, EMAIL, SMS, PUSH
    private String title;
    private String message;
    private Map<String, String> metadata;
    private LocalDateTime timestamp;
}
```

**What's happening here?**

- `channel` — Controls which consumers act. `ALL` = all channels; `EMAIL` = only email; etc.
- `metadata` — Extra data (e.g., orderId, link) for templates.
- `type` — Helps with analytics and routing (e.g., treat PAYMENT_FAILED as high priority).

---

## Step 2: Topic Configuration

```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name("notifications")
            .partitions(6)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic notificationDltTopic() {
        return TopicBuilder.name("notifications-dlt")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
```

**What's happening here?**

- `partitions(6)` — More partitions = more parallelism. Notifications can be high volume.
- `notifications-dlt` — Dead Letter Topic. When a consumer fails after retries, the message goes here for manual inspection.

---

## Step 3: Notification Producer

```java
@Service
@Slf4j
public class NotificationProducer {

    @Autowired
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void sendNotification(NotificationEvent event) {
        if (event.getNotificationId() == null) {
            event.setNotificationId(
                "NOTIF-" + UUID.randomUUID().toString()
                    .substring(0, 8).toUpperCase());
        }
        event.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send("notifications",
                event.getUserId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Notification {} sent to partition {}",
                        event.getNotificationId(),
                        result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to send notification: {}",
                        ex.getMessage());
                }
            });
    }
}
```

**What's happening here?**

- Key = `userId`. Notifications for the same user go to the same partition. Preserves order per user (e.g., "Order confirmed" before "Shipped").
- Producer is fire-and-forget from the caller's perspective. The API returns immediately; delivery happens asynchronously.

---

## Step 4: Email Consumer

```java
@Service
@Slf4j
public class EmailConsumer {

    @KafkaListener(
        topics = "notifications",
        groupId = "email-service"
    )
    public void handleNotification(NotificationEvent event) {
        if (!"ALL".equals(event.getChannel())
                && !"EMAIL".equals(event.getChannel())) {
            return;
        }

        log.info("[EMAIL] Sending to user {}: {}",
            event.getUserId(), event.getTitle());
        log.info("[EMAIL] Subject: {}", event.getTitle());
        log.info("[EMAIL] Body: {}", event.getMessage());

        // Real implementation:
        // emailService.send(userEmail, event.getTitle(), event.getMessage());
    }
}
```

**What's happening here?**

- Filter first: if channel is neither ALL nor EMAIL, skip. No need to send email for SMS-only notifications.
- `containerFactory` is optional if you have one default; omit if not needed.

---

## Step 5: SMS Consumer

```java
@Service
@Slf4j
public class SmsConsumer {

    @KafkaListener(
        topics = "notifications",
        groupId = "sms-service"
    )
    public void handleNotification(NotificationEvent event) {
        if (!"ALL".equals(event.getChannel())
                && !"SMS".equals(event.getChannel())) {
            return;
        }

        log.info("[SMS] Sending to user {}: {}",
            event.getUserId(), event.getMessage());

        // Real implementation:
        // smsService.send(userPhone, event.getMessage());
    }
}
```

---

## Step 6: Push Notification Consumer

```java
@Service
@Slf4j
public class PushConsumer {

    @KafkaListener(
        topics = "notifications",
        groupId = "push-service"
    )
    public void handleNotification(NotificationEvent event) {
        if (!"ALL".equals(event.getChannel())
                && !"PUSH".equals(event.getChannel())) {
            return;
        }

        log.info("[PUSH] Sending to user {}: {}",
            event.getUserId(), event.getTitle());

        // Real implementation:
        // firebaseService.sendPush(userDeviceToken, event.getTitle(), event.getMessage());
    }
}
```

---

## Adding a New Channel: WhatsApp (Step-by-Step)

Let's add WhatsApp without touching any existing code.

### Step 1: Create WhatsAppConsumer

```java
@Service
@Slf4j
public class WhatsAppConsumer {

    @KafkaListener(
        topics = "notifications",
        groupId = "whatsapp-service"
    )
    public void handleNotification(NotificationEvent event) {
        if (!"ALL".equals(event.getChannel())
                && !"WHATSAPP".equals(event.getChannel())) {
            return;
        }

        log.info("[WHATSAPP] Sending to user {}: {}",
            event.getUserId(), event.getMessage());

        // Real: WhatsApp Business API
        // whatsAppService.send(userPhone, event.getMessage());
    }
}
```

### Step 2: Update the Event Channel

Producers can now use `channel: "WHATSAPP"` for WhatsApp-only, or keep `channel: "ALL"` and add `WHATSAPP` to the ALL logic. For ALL to include WhatsApp, update the filter:

```java
if (!"ALL".equals(event.getChannel())
        && !"WHATSAPP".equals(event.getChannel())) {
    return;
}
```

### Step 3: Done

No changes to Order Service, Payment Service, or any producer. They keep publishing to `notifications`. The new consumer just starts listening. **Zero producer changes.**

---

## What Happens When SMS Service Is Down?

1. Producer sends notification with `channel: "ALL"`.
2. Email Consumer processes it. Push Consumer processes it.
3. SMS Consumer is down. It never consumes the message.

**What happens?** The message sits in Kafka. When SMS Consumer comes back, it reads from its last offset and processes all missed messages. Users might get SMS a few minutes late, but they get it. No loss.

!!! info "Backpressure"
    If SMS is down for hours, messages pile up. Monitor consumer lag. If lag grows, scale up SMS consumers or fix the SMS service.

---

## Rate Limiting Notifications

You don't want to spam users. "One promo email per user per day" or "Max 5 SMS per hour."

**Option 1: In the consumer**

Before sending, check Redis: `INCR user:U1:sms:count` with TTL 1 hour. If count > 5, skip.

**Option 2: In the producer**

Before publishing, check if user has received this type recently. If yes, don't publish.

**Option 3: Dedicated rate-limiter service**

A service consumes notifications, checks rate limits, and republishes to a "to-send" topic. Consumers read from "to-send." More complex but centralized.

!!! example "Simple Redis Rate Limit"
    ```java
    // In SMS Consumer, before sending:
    String key = "sms:limit:" + event.getUserId();
    Long count = redisTemplate.opsForValue().increment(key);
    if (count == 1) redisTemplate.expire(key, 1, TimeUnit.HOURS);
    if (count > 5) return; // Skip, over limit
    ```

---

## Priority Notifications

Some notifications are urgent (payment failed, security alert). Others are not (promo, newsletter).

**Option 1: Separate topics**

- `notifications-urgent` — Payment failed, security
- `notifications-normal` — Promo, order confirmed

Consumers listen to both. Process urgent first, or run separate consumer instances with different priorities.

**Option 2: Priority field in the event**

Add `priority: "HIGH" | "NORMAL"`. Consumers can use a priority queue internally: process HIGH before NORMAL.

**Option 3: Separate partitions**

Topic with 2 partitions: partition 0 = urgent, partition 1 = normal. Producer chooses partition by priority. Consumers process partition 0 first.

!!! tip "Kafka Best Practice"
    Simpler is better. Start with one topic and a priority field. Only split topics if you need different retention or different consumer groups.

---

## Step 7: REST Controller

```java
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationProducer producer;

    @PostMapping
    public ResponseEntity<Map<String, String>> send(
            @RequestBody NotificationEvent event) {

        producer.sendNotification(event);

        return ResponseEntity.ok(Map.of(
            "status", "queued",
            "notificationId", event.getNotificationId()
        ));
    }

    @PostMapping("/order-confirmed")
    public ResponseEntity<Map<String, String>> orderConfirmed(
            @RequestParam String userId,
            @RequestParam String orderId) {

        NotificationEvent event = new NotificationEvent();
        event.setUserId(userId);
        event.setType("ORDER_CONFIRMED");
        event.setChannel("ALL");
        event.setTitle("Order Confirmed!");
        event.setMessage("Your order " + orderId +
            " has been confirmed and is being processed.");
        event.setMetadata(Map.of("orderId", orderId));

        producer.sendNotification(event);

        return ResponseEntity.ok(Map.of(
            "status", "queued",
            "notificationId", event.getNotificationId()
        ));
    }

    @PostMapping("/payment-failed")
    public ResponseEntity<Map<String, String>> paymentFailed(
            @RequestParam String userId,
            @RequestParam String orderId) {

        NotificationEvent event = new NotificationEvent();
        event.setUserId(userId);
        event.setType("PAYMENT_FAILED");
        event.setChannel("ALL");
        event.setTitle("Payment Failed");
        event.setMessage("Payment for order " + orderId +
            " failed. Please try again.");
        event.setMetadata(Map.of("orderId", orderId));

        producer.sendNotification(event);

        return ResponseEntity.ok(Map.of(
            "status", "queued",
            "notificationId", event.getNotificationId()
        ));
    }
}
```

---

## Step 8: Test It

```bash
# Send to all channels
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER-001",
    "type": "PROMO",
    "channel": "ALL",
    "title": "50% Off!",
    "message": "Use code SAVE50 for 50% off on your next order!"
  }'

# Order confirmed (triggers all channels)
curl -X POST \
  "http://localhost:8080/api/notifications/order-confirmed?userId=USER-001&orderId=ORD-123"

# Send only email
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER-002",
    "type": "WELCOME",
    "channel": "EMAIL",
    "title": "Welcome!",
    "message": "Welcome to our platform!"
  }'
```

### Expected Logs

**For channel=ALL:**
```
[EMAIL] Sending to user USER-001: 50% Off!
[EMAIL] Subject: 50% Off!
[EMAIL] Body: Use code SAVE50 for 50% off on your next order!
[SMS]   Sending to user USER-001: Use code SAVE50 for 50% off...
[PUSH]  Sending to user USER-001: 50% Off!
```

**For channel=EMAIL:**
```
[EMAIL] Sending to user USER-002: Welcome!
```
*(SMS and PUSH skip this message.)*

---

## Production Considerations

| Consideration | Recommendation |
|---------------|----------------|
| **Dead Letter Topic** | Configure consumers to send failed messages to DLT. Monitor and replay or fix manually. |
| **Retries** | Use `@RetryableTopic` or `SeekToCurrentErrorHandler` with retry. Avoid infinite retries on bad data. |
| **Idempotency** | Track `notificationId` in DB. Before sending, check if already sent. Prevents duplicates on retry. |
| **Monitoring** | Track delivery success rate per channel. Alert when email/SMS failure rate spikes. |
| **Template engine** | Store templates (e.g., Thymeleaf, Handlebars) and render with event data. |
| **User preferences** | Check user's channel preferences before sending. User might have opted out of SMS. |

---

## Quick Summary

| Concept | Implementation |
|---------|-----------------|
| Fan-out pattern | One topic, multiple consumer groups |
| Channel routing | Filter by channel field in each consumer |
| Extensibility | Add new channels without changing producers |
| Message ordering | Same userId → same partition (userId as key) |
| Dead Letter Topic | Failed notifications go to DLT for review |

---

**Next:** [Redis vs Kafka Comparison](../comparison/redis-vs-kafka.md)
