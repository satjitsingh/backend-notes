# Lambda Deep Dive

---

## 1. Lambda

### What is it?
Lambda is a serverless compute service. You upload your code, and AWS runs it for you.
No servers to manage. No OS to patch. No capacity to plan. Pay only when your code runs.
Lambda executes code in response to events — an S3 upload, an API call, a queue message, a schedule.

### Why it matters in production
- Lambda is the backbone of serverless architectures on AWS.
- It eliminates all infrastructure management — DevOps focuses on code and pipelines.
- It scales automatically from 0 to thousands of concurrent executions.

### Core Ideas
- **Function** — Your code packaged and deployed to Lambda. One function = one unit of work.
- **Runtime** — The language environment: Python, Node.js, Java, Go, .NET, Ruby, or custom (via container image).
- **Handler** — The entry point function that Lambda calls (e.g., `index.handler`).
- **Execution environment** — A lightweight container that runs your function. Reused across invocations (warm start).
- **Invocation models:**
  - **Synchronous** — Caller waits for the response (API Gateway, ALB).
  - **Asynchronous** — Fire and forget. Lambda retries on failure (S3, SNS, EventBridge).
  - **Event source mapping** — Lambda polls the source (SQS, Kinesis, DynamoDB Streams).

**Key configuration:**
- **Memory:** 128 MB to 10,240 MB (10 GB). CPU scales proportionally with memory.
- **Timeout:** Max 15 minutes (900 seconds). Default 3 seconds.
- **Environment variables** — Config values injected at runtime. Can be encrypted with KMS.
- **Execution Role** — IAM Role that grants Lambda permissions (e.g., read S3, write DynamoDB).
- **VPC access** — Lambda can run inside your VPC to access private resources (RDS, ElastiCache).
- **Layers** — Shared libraries/code packaged separately. Up to 5 layers per function.
- **Concurrency** — Number of simultaneous executions. Default account limit: 1,000 (soft limit).
- **Provisioned Concurrency** — Pre-warm execution environments to avoid cold starts.

### Quick Analogy
Lambda = A vending machine.
You press a button (trigger event), it does its thing (executes code), gives you a result (response), and goes idle. You don't know or care what's inside the machine — just that it works. You only pay when you press the button.

### Architecture View
```
Event Sources                    Lambda                     Destinations
┌─────────────┐               ┌──────────────┐           ┌──────────────┐
│ API Gateway │──sync──▶      │              │──────────▶│ DynamoDB     │
│ S3 Upload   │──async─▶      │   Lambda     │──────────▶│ S3           │
│ SQS Queue   │──poll──▶      │   Function   │──────────▶│ SQS          │
│ DynamoDB    │──poll──▶      │              │──────────▶│ SNS          │
│ EventBridge │──async─▶      │  Runtime:    │──────────▶│ Step Function│
│ CloudWatch  │──async─▶      │  Python 3.12 │           │ CloudWatch   │
│ Kinesis     │──poll──▶      │  Memory: 512M│           │              │
│ SNS         │──async─▶      │  Timeout: 30s│           └──────────────┘
│ ALB         │──sync──▶      │              │
└─────────────┘               └──────────────┘
                                     │
                              IAM Execution Role
                              (permissions to access
                               AWS services)
```

### Hands-On (Step-by-step Lab)
1. Go to **Lambda → Create Function**.
2. Name: `hello-lambda`. Runtime: **Python 3.12**.
3. Use the default code:
```python
def lambda_handler(event, context):
    return {
        'statusCode': 200,
        'body': 'Hello from Lambda!'
    }
```
4. Click **Test** → Create a test event → Run it → See "Hello from Lambda!" in the response.
5. Go to **Configuration → General** → Set memory to 256 MB, timeout to 10 seconds.
6. Go to **Configuration → Environment variables** → Add: `ENV=production`.
7. Update code to read it:
```python
import os

def lambda_handler(event, context):
    env = os.environ.get('ENV', 'unknown')
    return {'statusCode': 200, 'body': f'Running in {env}'}
```
8. Test again → See "Running in production".

### Common Mistakes
- ❌ Setting timeout too low — function times out before finishing.
- ❌ Not setting memory high enough — CPU scales with memory. Low memory = slow function.
- ❌ Putting Lambda in a VPC unnecessarily — adds cold start latency. Only use VPC if you need private resources.
- ❌ Not configuring dead-letter queue (DLQ) for async invocations — failed events are lost.
- ❌ Hardcoding secrets in code or environment variables — use Secrets Manager or SSM Parameter Store.

### Pro Tips
- ✅ Keep functions **small and single-purpose** — one function, one job.
- ✅ Set memory based on profiling, not guessing. More memory = more CPU = faster = sometimes cheaper.
- ✅ Use **Lambda Power Tuning** tool to find the optimal memory setting.
- ✅ Use **Layers** for shared dependencies (e.g., `boto3` upgrades, common libraries).
- ✅ Use **Lambda Destinations** instead of DLQ for async — more flexible routing for success/failure.
- ✅ In interviews: "We use Lambda for event-driven workloads. Functions are single-purpose with minimal dependencies. Memory is tuned with Lambda Power Tuning. Secrets come from SSM Parameter Store. Async functions use destinations for error handling."

---

## 2. Event-Driven Architecture

### What is it?
Event-driven architecture is a design pattern where components communicate through **events**.
Something happens (event) → A service reacts to it (handler). No direct coupling between services.
Lambda is the default event handler in AWS serverless architectures.

### Why it matters in production
- Event-driven systems are loosely coupled, scalable, and resilient.
- Services don't need to know about each other — they just emit and consume events.
- It's the foundation of modern microservices and serverless systems.

### Core Ideas
- **Event** — A record that something happened: "file uploaded", "order placed", "user signed up".
- **Producer** — The service that generates the event (S3, API Gateway, your app).
- **Consumer** — The service that reacts to the event (Lambda, ECS, Step Functions).
- **Event bus / broker** — The middleman that routes events (EventBridge, SNS, SQS).
- **Loose coupling** — Producer doesn't know or care who consumes the event.
- **Eventual consistency** — Events are processed asynchronously. Data may be momentarily stale.

**Synchronous vs Asynchronous:**

| Type | Flow | Example |
|------|------|---------|
| **Synchronous** | Request → Wait → Response | API Gateway → Lambda → Response to user |
| **Asynchronous** | Emit event → Move on. Consumer processes later. | S3 upload → Lambda processes thumbnail in background |

### Quick Analogy
Event-driven = A restaurant kitchen.
A waiter (producer) places an order ticket (event) on the counter. The chef (consumer) picks it up and cooks. The waiter doesn't stand there watching — they go serve other tables. The kitchen processes orders independently.

### Architecture View
```
Traditional (Tightly Coupled):
  Service A ──direct call──▶ Service B ──direct call──▶ Service C
  (If B is down, A fails. If C is slow, everything is slow.)

Event-Driven (Loosely Coupled):
  Service A ──event──▶ EventBridge ──▶ Service B
                           │
                           └──▶ Service C
                           │
                           └──▶ Service D (added later, no changes to A)

Real Example — E-commerce Order:
┌─────────────┐
│ Order Placed│ (event)
└──────┬──────┘
       │
  EventBridge
       │
  ┌────┼──────────────────┐
  │    │                  │
  ▼    ▼                  ▼
Lambda    Lambda          Lambda
(payment) (send email)    (update inventory)
  │         │               │
  ▼         ▼               ▼
Stripe    SES             DynamoDB
```

### Hands-On (Step-by-step Lab)
1. Create an S3 bucket: `event-demo-bucket`.
2. Create a Lambda function that logs the event:
```python
import json

def lambda_handler(event, context):
    print(json.dumps(event))
    filename = event['Records'][0]['s3']['object']['key']
    print(f"New file uploaded: {filename}")
    return {'statusCode': 200}
```
3. Go to S3 bucket → **Properties → Event Notifications** → Create.
4. Event type: `s3:ObjectCreated:*` → Destination: Your Lambda function.
5. Upload a file to S3.
6. Check Lambda logs in **CloudWatch Logs** → See the filename printed.
7. You just built an event-driven pipeline: S3 event → Lambda handler.

### Common Mistakes
- ❌ Creating infinite loops — Lambda writes to S3 → Triggers Lambda → Writes to S3 → Repeat forever.
- ❌ Not handling duplicate events — Events can be delivered more than once. Make handlers idempotent.
- ❌ No error handling — Failed events disappear unless you configure DLQ or destinations.
- ❌ Overcomplicating with events when a simple API call would work.
- ❌ Not monitoring event flows — hard to debug when things go wrong.

### Pro Tips
- ✅ **Make every handler idempotent** — Processing the same event twice should produce the same result.
- ✅ Use **EventBridge** as the central event bus — it supports rules, filtering, and cross-account routing.
- ✅ Use **dead-letter queues** on every async consumer for failed events.
- ✅ Use **X-Ray** for distributed tracing across event-driven services.
- ✅ In interviews: "We use event-driven architecture with EventBridge as the central bus. All handlers are idempotent. Failed events go to DLQs. We trace the full event chain with X-Ray."

---

## 3. Cold Start

### What is it?
A cold start happens when Lambda creates a **new execution environment** to handle a request.
It includes downloading your code, starting the runtime, and running initialization.
Cold starts add latency — typically 100ms to several seconds depending on the runtime and config.

### Why it matters in production
- Cold starts cause noticeable latency for user-facing APIs.
- They're the #1 concern when using Lambda for real-time workloads.
- Understanding and mitigating cold starts is a key Lambda skill.

### Core Ideas

**What happens during a cold start:**
1. AWS provisions a new execution environment (container).
2. Downloads your deployment package (code + dependencies).
3. Starts the runtime (Python, Node.js, Java, etc.).
4. Runs initialization code (outside the handler — imports, DB connections, SDK setup).
5. Executes the handler function.

**Warm start:** Steps 1-4 are skipped. Only step 5 runs. Much faster.

**Cold start latency by runtime:**

| Runtime | Typical Cold Start |
|---------|-------------------|
| Python | 200-500 ms |
| Node.js | 200-500 ms |
| Go | 100-200 ms |
| Java | 1-5 seconds |
| .NET | 500ms-2 seconds |
| Custom (Container) | 1-10 seconds |

**Factors that increase cold start:**
- Larger deployment package → Longer download.
- More dependencies → Longer initialization.
- VPC-attached Lambda → Adds ENI creation time (improved but still adds ~1s).
- Higher memory → Slightly faster cold starts (more CPU for init).
- Java/C# → JVM/.NET startup is inherently slower.

### Quick Analogy
Cold start = Starting a car on a winter morning.
First start of the day (cold start) — engine takes time to warm up. Subsequent trips (warm starts) — engine is already warm, starts instantly. Provisioned Concurrency = leaving the engine running so it's always ready.

### Architecture View
```
Cold Start (first invocation or after idle):
  Request ──▶ [Provision Container] ──▶ [Download Code] ──▶ [Start Runtime]
              ──▶ [Run Init Code] ──▶ [Execute Handler] ──▶ Response
              |←──── Cold Start Latency (100ms - 10s) ────►|

Warm Start (reusing existing container):
  Request ──▶ [Execute Handler] ──▶ Response
              |←── ~1-10ms ──►|

Timeline of Lambda Container Lifecycle:
  ┌──────┐  ┌──────┐  ┌──────┐       ┌──────┐  ┌──────┐
  │ COLD │  │ WARM │  │ WARM │ idle  │ COLD │  │ WARM │
  │ 800ms│  │ 5ms  │  │ 5ms  │ ...   │ 800ms│  │ 5ms  │
  └──────┘  └──────┘  └──────┘ ───▶  └──────┘  └──────┘
                                 Container recycled
                                 after ~15 min idle
```

### Hands-On (Step-by-step Lab)
1. Create a Lambda with Python. Add a 1-second `time.sleep` in init (outside handler):
```python
import time
print("INIT: Cold start happening...")
time.sleep(1)

def lambda_handler(event, context):
    return {'statusCode': 200, 'body': 'Done'}
```
2. Invoke it → Notice ~1.5s response (cold start + init sleep).
3. Invoke again immediately → ~5ms response (warm start — init code skipped).
4. Wait 15 minutes → Invoke again → ~1.5s (cold start again — container recycled).
5. Enable **Provisioned Concurrency = 1** → Invoke → Always fast (no cold start).

### Common Mistakes
- ❌ Putting heavy initialization inside the handler — it runs on EVERY invocation.
- ❌ Using Java for latency-sensitive functions without Provisioned Concurrency.
- ❌ Bundling unnecessary dependencies — larger package = slower cold start.
- ❌ Ignoring cold starts for user-facing APIs — users notice the delay.
- ❌ Putting Lambda in VPC when it doesn't need it — adds cold start overhead.

### Pro Tips
- ✅ **Move initialization outside the handler** — DB connections, SDK clients, config loading. They persist across warm invocations.
- ✅ Use **Provisioned Concurrency** for latency-sensitive functions (API endpoints).
- ✅ Keep deployment packages **small** — Use layers for large dependencies.
- ✅ Use **SnapStart** (Java) — Caches initialized snapshot. Reduces Java cold start by 90%.
- ✅ Choose **Python or Node.js** for latency-sensitive functions — fastest cold starts.
- ✅ In interviews: "We minimize cold starts by keeping packages small, initializing outside the handler, and using Provisioned Concurrency for user-facing APIs. For Java, we use SnapStart. We avoid putting Lambda in a VPC unless it needs private resource access."

---

## 4. Triggers (Event Sources)

### What is it?
A trigger is an AWS service or event that **invokes your Lambda function**.
When the trigger event happens, Lambda automatically runs your code.
Lambda supports 200+ event sources — from S3 uploads to API calls to scheduled cron jobs.

### Why it matters in production
- Triggers connect Lambda to the rest of your AWS ecosystem.
- Understanding which triggers are sync vs async vs polling is critical for error handling.
- Choosing the right trigger pattern determines your architecture's reliability.

### Core Ideas

**Common triggers and their invocation types:**

| Trigger | Invocation | Use Case |
|---------|-----------|----------|
| **API Gateway** | Synchronous | REST/HTTP APIs |
| **ALB** | Synchronous | HTTP behind load balancer |
| **S3** | Asynchronous | File processing, thumbnails |
| **SNS** | Asynchronous | Fan-out notifications |
| **EventBridge** | Asynchronous | Scheduled tasks, event routing |
| **CloudWatch Events** | Asynchronous | Cron jobs, monitoring reactions |
| **SQS** | Event Source Mapping (poll) | Queue processing, decoupling |
| **DynamoDB Streams** | Event Source Mapping (poll) | Change data capture, replication |
| **Kinesis** | Event Source Mapping (poll) | Real-time streaming |
| **Cognito** | Synchronous | Auth triggers (pre-signup, post-confirm) |

**Error handling by invocation type:**

| Type | Retries | Failed Events |
|------|---------|--------------|
| **Synchronous** | 0 retries (caller handles) | Error returned to caller |
| **Asynchronous** | 2 retries (with backoff) | Sent to DLQ or Destination |
| **Event Source Mapping** | Retries until success or expiry | Bisect batch, DLQ |

### Quick Analogy
Triggers = Doorbell buttons around your house.
The front door bell (API Gateway), the back door bell (S3), the intercom (SQS), the alarm sensor (CloudWatch). Each one rings your phone (Lambda) in a slightly different way, and you handle each caller differently.

### Architecture View
```
Common Trigger Patterns:

1. API Pattern (Sync):
   User ──▶ API Gateway ──▶ Lambda ──▶ DynamoDB ──▶ Response to User

2. File Processing (Async):
   S3 Upload ──▶ Lambda (resize image) ──▶ S3 (save thumbnail)

3. Queue Processing (Polling):
   Producer ──▶ SQS Queue ◄── Lambda (polls, processes, deletes)

4. Cron Job (Async):
   EventBridge Rule (every 5 min) ──▶ Lambda (cleanup, reports)

5. Stream Processing (Polling):
   DynamoDB ──▶ DynamoDB Stream ◄── Lambda (sync to Elasticsearch)

6. Fan-Out (Async):
   SNS Topic ──▶ Lambda A (send email)
              ──▶ Lambda B (update analytics)
              ──▶ Lambda C (push notification)
```

### Hands-On (Step-by-step Lab)
1. **API Gateway trigger:**
   - Create Lambda function.
   - Add trigger → API Gateway → Create new HTTP API.
   - Get the URL → Call it from browser → See Lambda response.

2. **S3 trigger:**
   - Create Lambda that logs file names.
   - Add trigger → S3 → Event: `ObjectCreated`.
   - Upload a file → Check CloudWatch Logs.

3. **Scheduled trigger:**
   - Create Lambda that prints the current time.
   - Add trigger → EventBridge → Schedule: `rate(5 minutes)`.
   - Wait → Check logs every 5 minutes.

4. **SQS trigger:**
   - Create an SQS queue.
   - Create Lambda → Add trigger → SQS queue.
   - Send a message to the queue → Lambda processes it automatically.

### Common Mistakes
- ❌ Not configuring DLQ for async triggers — failed events vanish.
- ❌ Creating recursive triggers (Lambda writes to S3 → S3 triggers Lambda → Loop).
- ❌ Not understanding batch size for SQS triggers — can send up to 10 messages per invocation.
- ❌ Forgetting Lambda needs permission (execution role) to read from the trigger source.
- ❌ Not setting reserved concurrency — one trigger can consume all available concurrency.

### Pro Tips
- ✅ Always configure **DLQ or Destinations** for async and event source mapping triggers.
- ✅ Use **reserved concurrency** to prevent one function from starving others.
- ✅ Use **event filtering** on SQS/DynamoDB/Kinesis — Lambda only processes matching events.
- ✅ For S3 triggers, use **prefix/suffix filters** to avoid triggering on every file.
- ✅ In interviews: "We use API Gateway for sync APIs, S3 triggers for file processing, SQS for decoupled queue processing, and EventBridge for cron and event routing. All async triggers have DLQs. Event filtering reduces unnecessary invocations."

---

## 5. Lambda Limits

### What is it?
Lambda has hard and soft limits on execution time, memory, package size, and concurrency.
Knowing these limits prevents surprises in production and helps you design within boundaries.
Some limits can be increased via AWS Support. Others are hard limits.

### Why it matters in production
- Hitting a limit in production causes failures — timeouts, throttling, or deployment errors.
- Interview question: "What are Lambda's limitations?"
- Designing around limits is a sign of production experience.

### Core Ideas

| Limit | Value | Type |
|-------|-------|------|
| **Timeout** | Max 15 minutes (900s) | Hard |
| **Memory** | 128 MB – 10,240 MB | Hard |
| **Deployment package** | 50 MB (zipped), 250 MB (unzipped) | Hard |
| **Container image** | 10 GB | Hard |
| **Environment variables** | 4 KB total | Hard |
| **Concurrent executions** | 1,000 per Region (default) | Soft (can increase) |
| **Burst concurrency** | 500–3,000 (varies by Region) | Hard |
| **/tmp storage** | 512 MB (default), up to 10 GB | Configurable |
| **Layers** | 5 per function | Hard |
| **Invocation payload (sync)** | 6 MB | Hard |
| **Invocation payload (async)** | 256 KB | Hard |
| **Response payload** | 6 MB | Hard |

### Quick Analogy
Lambda limits = Rules of a food truck.
You can only cook for 15 minutes per order (timeout). Your kitchen is small (memory). You can only store a few ingredients (/tmp). You can serve a limited number of customers at once (concurrency). If you need more, you request a bigger permit (increase soft limits).

### Architecture View
```
When You Hit Limits — What Happens:

Timeout (15 min):
  Long task ──▶ Lambda runs 15 min ──▶ TIMES OUT ❌
  Fix: Use Step Functions to orchestrate. Break into smaller steps.

Concurrency (1000):
  1001st request ──▶ THROTTLED (429 error) ❌
  Fix: Request limit increase. Use reserved concurrency. Add SQS queue to buffer.

Package Size (250 MB):
  Large ML model + code ──▶ DEPLOYMENT FAILS ❌
  Fix: Use container image (up to 10 GB). Use EFS for large files.

Payload (6 MB):
  API returns 8 MB response ──▶ FAILS ❌
  Fix: Return S3 pre-signed URL instead of raw data.
```

### Hands-On (Step-by-step Lab)
1. Create a Lambda with timeout set to **3 seconds**.
2. Add `time.sleep(5)` in the handler → Invoke → **Task timed out after 3.00 seconds**.
3. Increase timeout to 10 seconds → Works.
4. Try deploying a package > 50 MB → Deployment fails. Use a Layer or container image instead.
5. Set reserved concurrency to 1 → Invoke twice simultaneously → Second invocation is throttled.

### Common Mistakes
- ❌ Designing workloads that need > 15 minutes — Lambda isn't for long-running jobs.
- ❌ Not requesting concurrency increases before a big launch.
- ❌ Bundling massive dependencies (ML models, FFmpeg) without using container images or EFS.
- ❌ Returning large payloads directly instead of using S3 pre-signed URLs.
- ❌ Ignoring burst concurrency limits — initial burst is limited even if your account limit is high.

### Pro Tips
- ✅ For jobs > 15 minutes: Use **Step Functions** to chain Lambda calls, or use **ECS/Fargate**.
- ✅ For large packages: Use **container images** (up to 10 GB) or mount **EFS**.
- ✅ For large data: Store in **S3** and pass the key to Lambda. Return S3 pre-signed URLs.
- ✅ Request concurrency increase **weeks before** a big event or launch.
- ✅ Use **/tmp** (up to 10 GB) for temporary file processing within an invocation.
- ✅ In interviews: "We design within Lambda's 15-minute timeout by breaking long workflows into Step Functions. For large packages, we use container images. Concurrency limits are managed with reserved concurrency per function and SQS queues for buffering during spikes."

---

## 6. Lambda Best Practices

### What is it?
A checklist of patterns and rules for writing production-quality Lambda functions.
Following these practices ensures your functions are fast, reliable, secure, and cost-effective.
These come from real-world experience and AWS recommendations.

### Why it matters in production
- Poorly written Lambda functions cause cold starts, timeouts, and runaway costs.
- Well-written functions are a sign of senior-level DevOps/serverless expertise.
- Interviewers expect you to know these.

### Core Ideas

**Performance:**
- ✅ **Initialize outside the handler** — SDK clients, DB connections, config. Reused on warm starts.
```python
import boto3
dynamodb = boto3.resource('dynamodb')  # OUTSIDE handler — persists across warm starts
table = dynamodb.Table('my-table')

def lambda_handler(event, context):
    response = table.get_item(Key={'id': event['id']})  # Fast — reuses connection
    return response
```
- ✅ **Right-size memory** — Use Lambda Power Tuning to find the sweet spot.
- ✅ **Keep packages small** — Fewer dependencies = faster cold starts.
- ✅ **Use ARM (Graviton)** — 20% cheaper, often 15% faster. Change architecture to `arm64`.

**Security:**
- ✅ **Least-privilege execution role** — Only the permissions the function needs.
- ✅ **Never hardcode secrets** — Use SSM Parameter Store or Secrets Manager.
- ✅ **Encrypt environment variables** with KMS.
- ✅ **Use VPC only when needed** — Private DB access? Yes. Public API? No.

**Reliability:**
- ✅ **Make handlers idempotent** — Same event processed twice = same result.
- ✅ **Configure DLQ / Destinations** — For every async invocation.
- ✅ **Set appropriate timeouts** — Not too short (timeout errors), not too long (wasted money on stuck functions).
- ✅ **Use reserved concurrency** — Protect critical functions from being starved.
- ✅ **Handle partial batch failures** — For SQS, report failed messages individually.

**Cost:**
- ✅ **Optimize memory** — Sometimes more memory = faster = cheaper (shorter duration).
- ✅ **Use ARM (Graviton2)** — 20% cheaper per ms.
- ✅ **Avoid over-invocation** — Use event filtering, deduplication.
- ✅ **Use Provisioned Concurrency wisely** — Only for latency-critical paths.

**Observability:**
- ✅ **Structured logging** — JSON logs with correlation IDs.
- ✅ **Enable X-Ray tracing** — See the full request path across services.
- ✅ **Custom CloudWatch metrics** — Business metrics (orders processed, errors by type).
- ✅ **Set CloudWatch Alarms** — On errors, throttles, duration.

### Quick Analogy
Lambda best practices = Rules for running a food truck efficiently.
Keep ingredients prepped (init outside handler). Don't overload the menu (small packages). Have a backup plan for busy days (DLQ, concurrency). Track your sales (observability). Lock the cash register (security).

### Architecture View
```
Production Lambda Setup:
┌──────────────────────────────────────────────────┐
│ Lambda Function: process-orders                  │
│                                                  │
│ Runtime: Python 3.12 (ARM/Graviton)              │
│ Memory: 512 MB (tuned with Power Tuning)         │
│ Timeout: 30 seconds                              │
│ Reserved Concurrency: 100                        │
│ Provisioned Concurrency: 10 (for API path)       │
│                                                  │
│ Environment:                                     │
│ ├── TABLE_NAME = orders (plain text — OK)        │
│ └── DB_SECRET = /prod/db/creds (from SSM — ✅)   │
│                                                  │
│ Execution Role: process-orders-role              │
│ ├── dynamodb:PutItem on orders table only        │
│ ├── ssm:GetParameter on /prod/db/*               │
│ └── logs:CreateLogGroup/Stream/PutLogEvents      │
│                                                  │
│ Async Config:                                    │
│ ├── DLQ: process-orders-dlq (SQS)               │
│ ├── Max retries: 2                               │
│ └── Max event age: 6 hours                       │
│                                                  │
│ Monitoring:                                      │
│ ├── X-Ray: Enabled                               │
│ ├── CloudWatch Alarm: Errors > 5 in 5 min        │
│ └── CloudWatch Alarm: Throttles > 0              │
└──────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Create a Lambda → Move `boto3.resource()` outside the handler.
2. Invoke twice → Check CloudWatch → First invocation has INIT duration, second doesn't.
3. Add environment variable `TABLE_NAME` → Reference with `os.environ['TABLE_NAME']`.
4. Create a DLQ (SQS queue) → Configure on Lambda async settings.
5. Enable **X-Ray active tracing** on the function.
6. Deploy with ARM architecture: `--architectures arm64`.
7. Run **Lambda Power Tuning** → Find optimal memory.

### Common Mistakes
- ❌ Initializing clients inside the handler — runs on every invocation, adding latency.
- ❌ No DLQ — failed async events silently disappear.
- ❌ `AdministratorAccess` as execution role — massive security risk.
- ❌ No alarms on Lambda errors or throttles.
- ❌ Not using structured (JSON) logging — makes log analysis painful.

### Pro Tips
- ✅ Use **Powertools for AWS Lambda** (Python/Node.js/Java) — adds structured logging, tracing, metrics, and idempotency with minimal code.
- ✅ Use **Lambda Power Tuning** — an open-source tool that tests your function at different memory sizes and shows cost/performance graphs.
- ✅ Enable **partial batch failure reporting** for SQS triggers — only retry failed messages, not the whole batch.
- ✅ Use **function URLs** for simple HTTP endpoints (skip API Gateway if you don't need its features).
- ✅ In interviews: "Our Lambda functions follow production best practices: init outside the handler, ARM architecture for 20% cost savings, least-privilege IAM roles, DLQs for all async invocations, structured logging with Powertools, and X-Ray for distributed tracing. We use Lambda Power Tuning to optimize memory allocation."
