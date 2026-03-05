# Messaging (SQS, SNS, EventBridge, Step Functions)

---

## 1. SQS (Simple Queue Service)

### What is it?
SQS is a fully managed message queue. Producers send messages to the queue. Consumers pull and process them.
It decouples components — the producer and consumer don't need to be online at the same time.
Messages are stored until a consumer processes and deletes them.

### Why it matters in production
- SQS is the backbone of decoupled, resilient architectures.
- If a downstream service is slow or down, messages queue up instead of failing.
- It absorbs traffic spikes — producers keep sending, consumers process at their own pace.

### Core Ideas

**Two queue types:**

| Feature | Standard Queue | FIFO Queue |
|---------|---------------|------------|
| **Throughput** | Unlimited | 300 msg/s (3,000 with batching) |
| **Ordering** | Best-effort (no guarantee) | Strict FIFO (guaranteed) |
| **Duplicates** | Possible (at-least-once) | No duplicates (exactly-once) |
| **Use case** | High throughput, order doesn't matter | Order matters (payments, commands) |

**Key concepts:**
- **Message** — Up to 256 KB. For larger payloads, store in S3, send the reference.
- **Visibility Timeout** — After a consumer reads a message, it's hidden from others for X seconds (default 30s). If not deleted in time, it reappears.
- **Retention Period** — How long messages stay in the queue if not processed (default 4 days, max 14 days).
- **Dead-Letter Queue (DLQ)** — Messages that fail processing X times go here for investigation.
- **Long Polling** — Consumer waits up to 20 seconds for messages (saves API calls vs short polling).
- **Delay Queue** — Messages are invisible for X seconds after being sent (max 15 min).
- **Batch operations** — Send, receive, delete up to 10 messages at once (cost-efficient).

### Quick Analogy
SQS = A to-do tray on a desk.
Someone drops a task (message) in the tray (queue). You pick it up when you're ready (pull). Once done, you throw it away (delete). If you get sick, the task goes back in the tray (visibility timeout expires) for someone else to pick up.

### Architecture View
```
Producer                  SQS Queue              Consumer
┌──────────┐           ┌──────────────┐        ┌──────────┐
│ Web App  │──send────▶│              │◄──poll──│ Lambda   │
│ or       │           │  Message 1   │        │ or       │
│ Lambda   │──send────▶│  Message 2   │        │ EC2      │
│ or       │           │  Message 3   │        │ Worker   │
│ API      │           │  Message 4   │        │          │
└──────────┘           │  ...         │        └──────────┘
                       └──────┬───────┘
                              │ Failed 3x
                              ▼
                       ┌──────────────┐
                       │ Dead-Letter  │
                       │ Queue (DLQ)  │
                       │ (investigate)│
                       └──────────────┘

Lambda + SQS Pattern:
SQS ◄── Lambda (event source mapping)
        ├── Batch size: 10
        ├── On success: delete message
        └── On failure: message returns to queue
            After 3 failures → DLQ
```

### Hands-On (Step-by-step Lab)
1. Go to **SQS → Create Queue → Standard**.
2. Name: `order-queue`. Default settings.
3. Create a **DLQ**: Name `order-queue-dlq`.
4. Edit `order-queue` → Set DLQ: `order-queue-dlq`, max receives: 3.
5. **Send a message:** Go to queue → Send → Body: `{"orderId": "123"}`.
6. **Receive a message:** Click "Poll for messages" → See your message.
7. Create a Lambda → Add **SQS trigger** → Select `order-queue`.
8. Lambda code:
```python
def lambda_handler(event, context):
    for record in event['Records']:
        body = record['body']
        print(f"Processing: {body}")
    return {'statusCode': 200}
```
9. Send another message → Lambda processes it automatically.

### Common Mistakes
- ❌ Not configuring a DLQ — failed messages retry forever and never get investigated.
- ❌ Visibility timeout too short — message reappears while still being processed, causing duplicates.
- ❌ Not deleting messages after processing — they reappear and get processed again.
- ❌ Using short polling (default) — wasteful. Always enable long polling (`WaitTimeSeconds: 20`).
- ❌ Sending messages > 256 KB — use S3 + SQS Extended Client Library instead.

### Pro Tips
- ✅ **Always configure a DLQ** with max receives = 3-5.
- ✅ Set **visibility timeout = 6x your function timeout** (AWS recommendation for Lambda).
- ✅ Enable **long polling** (`ReceiveMessageWaitTimeSeconds: 20`) — reduces costs.
- ✅ Use **SQS + Lambda** as the default async processing pattern.
- ✅ Use **FIFO queues** when order matters (financial transactions, command sequences).
- ✅ In interviews: "We use SQS to decouple services. Lambda polls the queue with event source mapping. Failed messages go to a DLQ after 3 attempts. Long polling is enabled to reduce empty API calls. For ordered processing, we use FIFO queues."

---

## 2. SNS (Simple Notification Service)

### What is it?
SNS is a fully managed **pub/sub** messaging service.
A publisher sends a message to a **topic**. All **subscribers** of that topic receive the message.
It's a push model — SNS pushes to subscribers immediately.

### Why it matters in production
- SNS enables fan-out — one event goes to many consumers simultaneously.
- It decouples the publisher from subscribers. Add new subscribers without changing the publisher.
- Common use cases: alerts, notifications, triggering multiple Lambda functions.

### Core Ideas
- **Topic** — A communication channel. Publishers send to a topic. Subscribers listen.
- **Publisher** — Sends messages to a topic (your app, CloudWatch Alarms, S3 events, etc.).
- **Subscriber** — Receives messages from a topic. Types:
  - **Lambda** — Trigger a function.
  - **SQS** — Queue the message for processing.
  - **HTTP/HTTPS** — POST to a webhook.
  - **Email** — Send email notification.
  - **SMS** — Send text message.
  - **Kinesis Firehose** — Stream to S3, Redshift, etc.
- **Fan-out** — One message → many subscribers. All get it simultaneously.
- **Message filtering** — Subscribers can set filter policies to receive only matching messages.
- **FIFO Topics** — Ordered, deduplicated (pairs with FIFO SQS queues).

### Quick Analogy
SNS = A radio station.
The station (topic) broadcasts a message. Everyone tuned in (subscribers) hears it at the same time. You can tune in to specific channels (message filtering). The station doesn't know or care who's listening.

### Architecture View
```
Publisher                SNS Topic              Subscribers
┌──────────┐          ┌──────────────┐
│CloudWatch│──push──▶ │              │──push──▶ Lambda (process alert)
│Alarm     │          │ alerts-topic │──push──▶ SQS (queue for analysis)
└──────────┘          │              │──push──▶ Email (notify on-call)
                      │              │──push──▶ SMS (critical alerts)
                      └──────────────┘

Fan-Out Pattern (SNS + SQS):
                      ┌──────────────┐
                      │  SNS Topic   │
Order Placed ──push──▶│ "new-order"  │
                      └──────┬───────┘
                    ┌────────┼────────┐
                    │        │        │
                    ▼        ▼        ▼
               ┌────────┐┌────────┐┌────────┐
               │SQS:    ││SQS:    ││SQS:    │
               │payment ││email   ││inventory│
               │-queue  ││-queue  ││-queue   │
               └───┬────┘└───┬────┘└───┬────┘
                   ▼         ▼         ▼
               Lambda    Lambda     Lambda
               (charge)  (confirm)  (update stock)
```

### Hands-On (Step-by-step Lab)
1. Go to **SNS → Create Topic → Standard**.
2. Name: `order-notifications`.
3. Create **subscription 1:** Protocol: Email → Enter your email → Confirm via email link.
4. Create **subscription 2:** Protocol: Lambda → Select a Lambda function.
5. Create **subscription 3:** Protocol: SQS → Select an SQS queue.
6. **Publish a message:** Go to topic → Publish → Body: `New order received: #12345`.
7. Check: Email arrives, Lambda executes, SQS has the message. All simultaneously.

### Common Mistakes
- ❌ Confusing SNS (push) with SQS (pull) — they're complementary, not alternatives.
- ❌ Not confirming email subscriptions — messages don't deliver to unconfirmed emails.
- ❌ No message filtering — all subscribers get all messages (wasteful).
- ❌ Not using SNS + SQS fan-out — sending to SQS directly loses the fan-out benefit.
- ❌ Publishing sensitive data without encryption — enable SSE on the topic.

### Pro Tips
- ✅ Use **SNS + SQS fan-out** as the default pattern for one-to-many messaging.
- ✅ Use **message filtering** so subscribers only get relevant messages.
- ✅ Use **SNS for alerts** — CloudWatch Alarm → SNS → Email/Slack/PagerDuty.
- ✅ Use **FIFO topics + FIFO queues** when ordering matters.
- ✅ Enable **server-side encryption** on sensitive topics.
- ✅ In interviews: "We use SNS for fan-out. When an order is placed, one SNS message triggers payment processing, email confirmation, and inventory update simultaneously via SQS queues. Message filtering ensures each subscriber only gets relevant events."

---

## 3. EventBridge

### What is it?
EventBridge is a serverless **event bus** that routes events between AWS services, SaaS apps, and your code.
It's the evolution of CloudWatch Events. Think of it as the central nervous system for event-driven architectures.
You define rules that match events and route them to targets.

### Why it matters in production
- EventBridge is the recommended service for building event-driven architectures on AWS.
- It supports 100+ AWS service event sources natively.
- It's more powerful than SNS for complex event routing, filtering, and transformation.

### Core Ideas
- **Event Bus** — The channel events flow through. Default bus receives AWS events. Custom buses for your apps.
- **Event** — A JSON object describing what happened (e.g., EC2 state change, S3 upload, custom app event).
- **Rule** — Matches events based on patterns and routes them to targets.
- **Target** — Where matched events go (Lambda, SQS, SNS, Step Functions, API Gateway, etc.).
- **Event Pattern** — JSON filter matching specific fields in the event.
- **Schedule** — Cron or rate expressions to trigger on a schedule.

**EventBridge vs SNS:**

| Feature | EventBridge | SNS |
|---------|------------|-----|
| **Event filtering** | Rich (content-based, nested fields) | Basic (attribute filtering) |
| **Schema registry** | Yes (auto-discovers event schemas) | No |
| **SaaS integration** | Yes (Salesforce, Zendesk, etc.) | No |
| **Cross-account** | Yes (event bus policies) | Yes (topic policies) |
| **Targets** | 20+ (Lambda, Step Functions, API destinations, etc.) | Limited (Lambda, SQS, HTTP, email) |
| **Transformation** | Yes (input transformer) | No |
| **Archive & Replay** | Yes (replay old events) | No |
| **Best for** | Complex event routing, AWS native events | Simple fan-out, notifications |

### Quick Analogy
EventBridge = An airport control tower.
Planes (events) arrive from everywhere. The tower (rules) decides which runway (target) each plane goes to based on its type, origin, and destination. New runways can be added without changing the planes.

### Architecture View
```
Event Sources                EventBridge                  Targets
┌───────────────┐          ┌──────────────┐
│ AWS Services  │          │              │──rule──▶ Lambda (process)
│ EC2, S3, RDS  │──event──▶│  Event Bus   │──rule──▶ Step Functions
│               │          │  (default)   │──rule──▶ SQS Queue
├───────────────┤          │              │──rule──▶ SNS Topic
│ Custom App    │──event──▶│              │──rule──▶ API Destination
│ (PutEvents)   │          │              │         (webhook)
├───────────────┤          └──────────────┘
│ SaaS Apps     │                │
│ (Salesforce)  │──event──▶      │
└───────────────┘          ┌─────▼──────┐
                           │ Archive    │
                           │ (replay    │
                           │ later)     │
                           └────────────┘

Event Pattern Example:
{
  "source": ["aws.ec2"],
  "detail-type": ["EC2 Instance State-change Notification"],
  "detail": {
    "state": ["stopped", "terminated"]
  }
}
→ Match: EC2 instance stopped or terminated
→ Target: Lambda (send Slack alert)
```

### Hands-On (Step-by-step Lab)
1. Go to **EventBridge → Rules → Create Rule**.
2. Name: `ec2-state-change`. Event bus: default.
3. Rule type: **Rule with an event pattern**.
4. Event source: AWS events. Service: EC2. Event type: EC2 Instance State-change.
5. Target: Lambda function (that sends a notification).
6. Create the rule.
7. **Stop an EC2 instance** → Lambda triggers → Check CloudWatch Logs.
8. **Scheduled rule:** Create new rule → Schedule: `rate(5 minutes)` → Target: Lambda.

### Common Mistakes
- ❌ Using CloudWatch Events instead of EventBridge — EventBridge is the successor with more features.
- ❌ Not using custom event buses for application events — mixing with AWS default bus.
- ❌ Forgetting input transformers — sending the full raw event when the target only needs a few fields.
- ❌ Not archiving events — can't replay or debug without history.
- ❌ Overly broad event patterns — matching too many events wastes Lambda invocations.

### Pro Tips
- ✅ Use **EventBridge** as your default event router over SNS for complex patterns.
- ✅ Use **custom event buses** for application events to keep them separate from AWS events.
- ✅ Use **input transformers** to reshape events before sending to targets.
- ✅ Enable **Archive and Replay** for debugging and disaster recovery.
- ✅ Use **Schema Registry** to auto-discover and document event structures.
- ✅ Use **API Destinations** to call external APIs (Slack, PagerDuty) directly from EventBridge.
- ✅ In interviews: "We use EventBridge as our central event bus. AWS service events (EC2 changes, RDS failovers) trigger alerting Lambda functions. Application events flow through custom buses with content-based rules. We archive all events for replay capability during incident investigation."

---

## 4. Step Functions

### What is it?
Step Functions is a serverless **workflow orchestration** service.
You define a state machine — a series of steps (states) with logic (choices, retries, parallel execution).
Each step can call Lambda, ECS, DynamoDB, SQS, SNS, or other AWS services.

### Why it matters in production
- Complex workflows with multiple steps, conditions, and error handling need orchestration.
- Without Step Functions, you'd chain Lambda functions directly — fragile and hard to debug.
- Step Functions provide a visual workflow, built-in retries, and error handling.

### Core Ideas
- **State Machine** — The workflow definition (written in Amazon States Language — JSON).
- **States** — The building blocks:

| State Type | What It Does |
|-----------|-------------|
| **Task** | Execute work (Lambda, API call, SDK integration) |
| **Choice** | Branching logic (if/else) |
| **Parallel** | Run multiple branches simultaneously |
| **Wait** | Pause for X seconds or until a timestamp |
| **Map** | Loop over an array — process each item |
| **Pass** | Transform data, pass through |
| **Succeed/Fail** | End the execution |

**Two workflow types:**

| Type | Max Duration | Price | Use Case |
|------|-------------|-------|----------|
| **Standard** | Up to 1 year | Per state transition ($0.025/1000) | Long-running, durable |
| **Express** | Up to 5 minutes | Per execution + duration | High-volume, short |

**Key features:**
- **Built-in retries** — Retry failed steps with configurable backoff.
- **Catch/error handling** — Route errors to fallback states.
- **Visual debugger** — See exactly which step failed and why.
- **SDK integrations** — Call 200+ AWS services directly (no Lambda needed).
- **Human approval** — Pause workflow, wait for external approval, then continue.

### Quick Analogy
Step Functions = An assembly line in a factory.
Each station (state) does one job. The conveyor belt (workflow) moves the product between stations. If a station fails, there's a retry mechanism. Some stations run in parallel. A quality check (choice) decides the next path. The whole process is visible on a factory floor monitor (visual debugger).

### Architecture View
```
Order Processing Workflow:
┌─────────────────────────────────────────────────────┐
│ Step Function: ProcessOrder                         │
│                                                     │
│  ┌──────────┐    ┌──────────────┐    ┌───────────┐  │
│  │ Validate │───▶│ Check        │───▶│ Process   │  │
│  │ Order    │    │ Inventory    │    │ Payment   │  │
│  │ (Lambda) │    │ (DynamoDB)   │    │ (Lambda)  │  │
│  └──────────┘    └──────┬───────┘    └─────┬─────┘  │
│                         │                  │        │
│                    ┌────▼────┐         ┌───▼─────┐  │
│                    │ Choice  │         │ Choice  │  │
│                    │In stock?│         │Success? │  │
│                    └────┬────┘         └────┬────┘  │
│                   Yes   │   No         Yes  │  No   │
│                    │    │    │          │    │  │    │
│                    ▼    │    ▼          ▼    │  ▼    │
│               Continue  │ ┌──────┐  ┌─────┐ │ Retry │
│                         │ │Notify│  │Ship │ │ (3x)  │
│                         │ │Back- │  │Order│ │  │    │
│                         │ │order │  │(ECS)│ │  ▼    │
│                         │ └──────┘  └─────┘ │ Fail  │
│                         │                   │ State  │
│                         ▼                   ▼       │
│                    Send Email          Send Email    │
│                    (SNS)              (SNS: error)   │
└─────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **Step Functions → Create State Machine**.
2. Use **Workflow Studio** (visual drag-and-drop editor).
3. Add a **Lambda Task** → "Validate Input".
4. Add a **Choice** state → If `status == "valid"` → Next step. Else → Fail.
5. Add another **Lambda Task** → "Process Order".
6. Add a **Succeed** state.
7. Add error handling: On the Process Order task → Add **Retry** (3 attempts, exponential backoff).
8. Add **Catch** → On error → Route to a "Send Error Notification" (SNS Task).
9. Create → Start execution with sample input → Watch the visual execution flow.

### Common Mistakes
- ❌ Chaining Lambda functions directly (Lambda calling Lambda) instead of using Step Functions.
- ❌ Using Standard workflows for high-volume, short-lived tasks — Express is cheaper.
- ❌ Not configuring retries — first failure kills the workflow.
- ❌ Passing large payloads between states — max 256 KB. Store data in S3/DynamoDB and pass references.
- ❌ Not using SDK integrations — creating unnecessary Lambda functions to call DynamoDB/SQS when Step Functions can call them directly.

### Pro Tips
- ✅ Use **Step Functions** for any workflow with 3+ steps, branching, or error handling.
- ✅ Use **SDK integrations** to call AWS services directly — no Lambda needed for simple operations.
- ✅ Use **Map state** for processing lists of items in parallel.
- ✅ Use **Express workflows** for high-volume event processing (e.g., IoT, streaming).
- ✅ Store large data in **S3/DynamoDB** and pass ARNs/keys between states.
- ✅ In interviews: "We use Step Functions to orchestrate complex workflows like order processing. Each step is a Lambda function or direct SDK integration. Built-in retries with exponential backoff handle transient failures. The visual debugger makes troubleshooting straightforward."

---

## 5. SQS vs SNS — When to Use Which

### What is it?
SQS and SNS are both messaging services but solve different problems.
SQS is a queue (pull model, one consumer). SNS is pub/sub (push model, many consumers).
They're often used **together** for powerful fan-out patterns.

### Why it matters in production
- "When would you use SQS vs SNS?" is a guaranteed interview question.
- Using the wrong one leads to lost messages, duplicate processing, or architectural bottlenecks.
- Most real systems use both together.

### Core Ideas

**The Core Difference:**

| Aspect | SQS | SNS |
|--------|-----|-----|
| **Model** | Queue (pull) | Pub/Sub (push) |
| **Consumers** | 1 consumer per message | Many subscribers per message |
| **Persistence** | Messages stored until processed (up to 14 days) | No persistence (deliver and forget) |
| **Delivery** | Pull — consumer requests messages | Push — SNS sends to subscribers |
| **Retry** | Message reappears after visibility timeout | SNS retries delivery to subscriber |
| **Ordering** | FIFO queues guarantee order | FIFO topics guarantee order |
| **Use case** | Decouple producer/consumer, buffer | Fan-out, notifications, alerts |

**When to use SQS:**
- One producer, one consumer (or competing consumers).
- You need to **buffer** messages during traffic spikes.
- Consumer processes at its own pace.
- Messages must **not be lost** (persisted until deleted).
- Example: Order processing queue, background job queue.

**When to use SNS:**
- One message needs to reach **multiple consumers** simultaneously.
- You need to notify (email, SMS, webhook, Lambda).
- Publisher doesn't know or care about consumers.
- Example: Alert notifications, fan-out to multiple services.

**When to use BOTH (SNS + SQS fan-out):**
- One event needs to be processed by multiple independent consumers, each at their own pace.
- SNS fans out to multiple SQS queues. Each queue has its own consumer.
- Example: New order → SNS → Payment Queue + Email Queue + Inventory Queue.

### Quick Analogy
- **SQS** = A to-do list. One person picks up each task and completes it.
- **SNS** = A loudspeaker announcement. Everyone in the room hears it at the same time.
- **SNS + SQS** = An announcement that puts a task on each department's to-do list.

### Architecture View
```
SQS Only (Point-to-Point):
Producer ──▶ [SQS Queue] ◄── Consumer
             Messages buffered until processed.
             One consumer per message.

SNS Only (Fan-Out, No Buffer):
Publisher ──▶ [SNS Topic] ──push──▶ Lambda A
                           ──push──▶ Lambda B
                           ──push──▶ Email
             All get the message simultaneously.
             If Lambda B is down, message is lost.

SNS + SQS (Fan-Out with Buffer — BEST PRACTICE):
Publisher ──▶ [SNS Topic] ──▶ [SQS Queue A] ◄── Lambda A (payment)
                           ──▶ [SQS Queue B] ◄── Lambda B (email)
                           ──▶ [SQS Queue C] ◄── Lambda C (inventory)
             Each queue buffers independently.
             If Lambda B is down, messages wait in Queue B.
             No messages lost. Each consumer processes at its own pace.

EventBridge + SQS (Modern Alternative):
Publisher ──▶ [EventBridge] ──rule──▶ [SQS Queue A]
                             ──rule──▶ [SQS Queue B]
                             ──rule──▶ Lambda C
             More powerful filtering and routing.
```

**Decision Flow Chart:**
```
Do multiple consumers need the same message?
├── YES → Do consumers need to buffer/process at different speeds?
│         ├── YES → SNS + SQS fan-out ✅
│         └── NO  → SNS alone (if consumers are always available)
└── NO  → Do you need to buffer messages?
          ├── YES → SQS ✅
          └── NO  → Direct invocation (Lambda, API call)
```

### Hands-On (Step-by-step Lab)
1. **SQS only:** Create queue → Send message → Lambda polls and processes.
2. **SNS only:** Create topic → Subscribe Lambda + Email → Publish → Both receive.
3. **SNS + SQS fan-out:**
   - Create SNS topic: `order-events`.
   - Create 3 SQS queues: `payment-q`, `email-q`, `inventory-q`.
   - Subscribe all 3 queues to the SNS topic.
   - Publish a message to SNS.
   - Check: All 3 queues have the message independently.
   - Attach Lambda to each queue → Each processes the same event differently.

### Common Mistakes
- ❌ Using SQS when you need fan-out → Only one consumer gets each message.
- ❌ Using SNS alone when consumers need buffering → Messages lost if consumer is down.
- ❌ Not using the SNS + SQS combo → Missing the best of both worlds.
- ❌ Confusing "subscribers" (SNS) with "consumers" (SQS).
- ❌ Not configuring SNS subscription filter policies → Every subscriber gets every message.

### Pro Tips
- ✅ **Default pattern:** SNS + SQS fan-out for any one-to-many async messaging.
- ✅ Use **EventBridge** instead of SNS when you need rich content-based filtering.
- ✅ SQS is perfect as a **buffer in front of Lambda** — absorbs spikes, controls concurrency.
- ✅ Use **FIFO Topic + FIFO Queues** when you need ordered fan-out.
- ✅ Monitor SQS `ApproximateAgeOfOldestMessage` — alerts if consumers fall behind.
- ✅ In interviews: "For one-to-many messaging, we use SNS + SQS fan-out. SNS distributes the event, and each SQS queue buffers for its respective consumer. This gives us decoupling, buffering, and independent processing. For complex event routing, we prefer EventBridge. For simple point-to-point decoupling, SQS alone."
