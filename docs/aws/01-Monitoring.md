# Monitoring & Observability

> "You can't fix what you can't see." — Observability is the #1 operational skill for DevOps.

---

## 1. CloudWatch (Overview)

### What is it?
CloudWatch is AWS's unified monitoring and observability service.
It collects metrics, logs, and events from almost every AWS service and your own applications.
Think of it as the single pane of glass for everything happening in your AWS account.

### Why it matters in production
- Without monitoring, you're flying blind. Problems become outages before you notice them.
- CloudWatch is the default monitoring for ALL AWS services — it's already collecting data.
- Every DevOps engineer must be fluent in CloudWatch. It's asked in every interview.

### Core Ideas

**CloudWatch has four pillars:**

| Pillar | What It Does | Example |
|--------|-------------|---------|
| **Metrics** | Numerical data points over time | CPU utilization, request count, error rate |
| **Logs** | Text-based records of events | Application logs, VPC Flow Logs, Lambda logs |
| **Alarms** | Trigger actions when metrics cross thresholds | Alert when CPU > 80% for 5 minutes |
| **Events / EventBridge** | React to state changes | EC2 instance stopped → trigger Lambda |

**Additional CloudWatch features:**

| Feature | What It Does |
|---------|-------------|
| **Dashboards** | Custom visualizations of metrics and logs |
| **Insights** | Query and analyze log data with a SQL-like syntax |
| **Synthetics** | Canary scripts that monitor endpoints (synthetic monitoring) |
| **Contributor Insights** | Identify top contributors (top IPs, top URLs) |
| **ServiceLens** | Visualize service health with X-Ray integration |
| **Application Insights** | Auto-discover and monitor application stacks |
| **Anomaly Detection** | ML-based detection of unusual metric behavior |

### Quick Analogy
CloudWatch = The dashboard in your car.
Speedometer (metrics), engine light (alarms), trip computer logs (logs), GPS alerts (events). You glance at it constantly while driving. Without it, you'd crash.

### Architecture View
```
Everything Feeds into CloudWatch:

┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│   EC2      │ │   RDS      │ │  Lambda    │ │   ECS      │
│ CPU, Net,  │ │ Connections│ │ Duration,  │ │ CPU, Mem,  │
│ Disk, Mem* │ │ Read/Write │ │ Errors,    │ │ Task count │
└─────┬──────┘ └─────┬──────┘ │ Throttles  │ └─────┬──────┘
      │               │       └─────┬──────┘       │
      └───────────────┼─────────────┼───────────────┘
                      │             │
                      ▼             ▼
              ┌───────────────────────────────┐
              │         CLOUDWATCH            │
              │                               │
              │  ┌─────────┐  ┌────────────┐  │
              │  │ Metrics  │  │   Logs     │  │
              │  └────┬────┘  └─────┬──────┘  │
              │       │             │          │
              │  ┌────▼────┐  ┌────▼───────┐  │
              │  │ Alarms  │  │ Insights   │  │
              │  └────┬────┘  └────────────┘  │
              │       │                       │
              │  ┌────▼─────────────────────┐ │
              │  │ Actions: SNS, Lambda,    │ │
              │  │ Auto Scaling, EC2 reboot │ │
              │  └──────────────────────────┘ │
              │                               │
              │  ┌──────────────────────────┐ │
              │  │ Dashboards (visualize)   │ │
              │  └──────────────────────────┘ │
              └───────────────────────────────┘

* EC2 memory and disk are NOT default metrics — requires CloudWatch Agent
```

### Hands-On (Step-by-step Lab)
1. Go to **CloudWatch → Metrics → EC2** → See CPU, Network, Disk metrics for your instances.
2. Select a metric → Add to a Dashboard.
3. Go to **CloudWatch → Dashboards → Create** → Add EC2 CPU, RDS connections, ALB request count.
4. Go to **CloudWatch → Alarms** → Create an alarm (covered in detail in the Alarms section).
5. Go to **CloudWatch → Logs** → Browse Lambda and ECS log groups.
6. Go to **CloudWatch → Insights** → Query logs with: `fields @timestamp, @message | filter @message like /ERROR/ | sort @timestamp desc | limit 20`.

### Common Mistakes
- ❌ Not looking at CloudWatch until something breaks — check dashboards daily.
- ❌ Thinking all metrics are free — detailed monitoring and custom metrics cost money.
- ❌ Not creating dashboards — raw metrics are hard to interpret without visualization.
- ❌ Ignoring CloudWatch Logs Insights — it's a powerful log analysis tool most people skip.
- ❌ Not enabling CloudWatch Agent on EC2 for memory and disk metrics.

### Pro Tips
- ✅ Create a **production dashboard** with: ALB 5xx, EC2 CPU, RDS connections, Lambda errors, SQS queue depth.
- ✅ Use **CloudWatch Anomaly Detection** to auto-detect unusual patterns without setting manual thresholds.
- ✅ Use **CloudWatch Synthetics** to proactively monitor API endpoints and web pages.
- ✅ Use **Metric Math** to create derived metrics: `error_rate = errors / requests * 100`.
- ✅ In interviews: "We use CloudWatch as our primary observability platform. Production dashboards show ALB health, Lambda errors, RDS performance, and SQS queue depth. Alarms trigger SNS notifications and auto-scaling. We query logs with Logs Insights and use Synthetics for endpoint monitoring."

---

## 2. CloudWatch Logs

### What is it?
CloudWatch Logs is a centralized log management service.
It collects, stores, and lets you search and analyze logs from EC2, Lambda, ECS, VPC, RDS, and more.
All your logs in one place — no need to SSH into servers to read log files.

### Why it matters in production
- Centralized logging is a DevOps fundamental. You can't debug production issues without logs.
- Lambda, ECS, and Fargate automatically send logs to CloudWatch Logs.
- Log analysis is how you find the root cause of incidents.

### Core Ideas

**Log hierarchy:**

| Concept | What It Is | Example |
|---------|-----------|---------|
| **Log Group** | A collection of log streams. Usually one per application or service. | `/ecs/web-api`, `/aws/lambda/my-function` |
| **Log Stream** | A sequence of log events from one source | One Lambda invocation, one EC2 instance |
| **Log Event** | A single log entry with timestamp and message | `2024-01-15 ERROR: Connection timeout` |
| **Retention** | How long logs are kept (default: never expire) | Set to 30, 90, 365 days to save cost |
| **Subscription Filter** | Stream logs in real-time to another destination | Logs → Lambda → Elasticsearch |

**Log sources and how they get to CloudWatch:**

| Source | How Logs Arrive |
|--------|----------------|
| **Lambda** | Automatic (via execution role) |
| **ECS / Fargate** | `awslogs` log driver in task definition |
| **EC2** | CloudWatch Agent (you install and configure) |
| **VPC Flow Logs** | Configured on VPC/subnet/ENI |
| **RDS** | Enable log export (slow query, error, general) |
| **API Gateway** | Enable access logging + execution logging |
| **CloudTrail** | Sends API call logs to CloudWatch Logs |

**CloudWatch Logs Insights (query language):**
```
fields @timestamp, @message
| filter @message like /ERROR/
| stats count(*) as error_count by bin(5m)
| sort @timestamp desc
```

### Quick Analogy
CloudWatch Logs = A centralized filing cabinet for all your office memos.
Every department (service) drops their memos (logs) in labeled folders (log groups). When something goes wrong, you open the folder and search for the relevant memo. Logs Insights = A smart search engine for the cabinet.

### Architecture View
```
Log Collection Architecture:

EC2 ──CloudWatch Agent──▶ ┌───────────────────────────────┐
Lambda ──auto──────────▶  │ CloudWatch Logs               │
ECS ──awslogs driver───▶  │                               │
VPC Flow Logs ─────────▶  │ Log Groups:                   │
API Gateway ───────────▶  │ ├── /ecs/web-api              │
CloudTrail ────────────▶  │ ├── /aws/lambda/process-order │
                          │ ├── /vpc/flow-logs             │
                          │ └── /aws/cloudtrail            │
                          │                               │
                          │ Features:                     │
                          │ ├── Logs Insights (query)     │
                          │ ├── Metric Filters (→ alarm)  │
                          │ ├── Subscription (→ Lambda)   │
                          │ └── Export (→ S3 for archive)  │
                          └───────────────────────────────┘

Metric Filter Example:
  Log Group: /ecs/web-api
  Filter: Pattern = "ERROR"
  → Creates CloudWatch Metric: "error-count"
  → Alarm: If error-count > 10 in 5 min → SNS → Slack
```

### Hands-On (Step-by-step Lab)
1. Go to **CloudWatch → Logs → Log Groups** → Browse existing groups.
2. Click a Lambda log group → Explore log streams → Read individual log events.
3. Go to **Logs Insights** → Select a log group → Run:
```
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 50
```
4. Create a **Metric Filter:**
   - Log group: `/ecs/web-api`.
   - Filter pattern: `ERROR`.
   - Metric name: `WebApiErrorCount`.
5. Create an **Alarm** on `WebApiErrorCount > 5` in 5 minutes → SNS notification.
6. Set **retention** on your log groups to 90 days (don't pay for infinite storage).

### Common Mistakes
- ❌ Default log retention is **forever** — logs pile up and cost money. Set retention.
- ❌ Not using Logs Insights — searching logs manually in the console is painful.
- ❌ Not creating Metric Filters — miss the opportunity to alarm on log patterns.
- ❌ Logging too much (DEBUG in production) — expensive and noisy.
- ❌ Not using structured (JSON) logging — Logs Insights can't parse unstructured text well.

### Pro Tips
- ✅ **Set retention on every log group** — 30 days for dev, 90 days for production.
- ✅ Use **structured JSON logging** so Logs Insights can query specific fields.
- ✅ Create **Metric Filters** for key log patterns: ERROR, TIMEOUT, OOM, 5xx.
- ✅ Use **Subscription Filters** to stream logs to OpenSearch (Elasticsearch) for advanced analysis.
- ✅ Export old logs to **S3** for long-term archive (cheaper than CloudWatch retention).
- ✅ Use **CloudWatch Agent** on EC2 to send application logs, OS logs, and custom metrics.
- ✅ In interviews: "We centralize all logs in CloudWatch Logs with structured JSON format. Metric Filters create alarms on ERROR patterns. Logs Insights provides fast ad-hoc queries during incidents. Retention is set to 90 days, with S3 export for long-term archive."

---

## 3. CloudWatch Metrics

### What is it?
Metrics are time-series numerical data points that represent the behavior of your resources.
AWS services automatically send metrics to CloudWatch (e.g., EC2 CPU, RDS connections, Lambda duration).
You can also publish custom metrics from your applications.

### Why it matters in production
- Metrics are the foundation for alarms, auto-scaling, and dashboards.
- Without metrics, you can't measure performance, detect problems, or plan capacity.
- Understanding what metrics are available (and which are missing) is essential.

### Core Ideas

**Metric structure:**
- **Namespace** — Category (e.g., `AWS/EC2`, `AWS/RDS`, `Custom/MyApp`).
- **Metric Name** — What's being measured (e.g., `CPUUtilization`).
- **Dimensions** — Key-value filters (e.g., `InstanceId=i-1234`).
- **Datapoint** — A single value with a timestamp.
- **Period** — Time granularity (60s, 300s, etc.).
- **Statistics** — `Average`, `Sum`, `Min`, `Max`, `SampleCount`, `Percentile (p99)`.

**Default vs Custom metrics:**

| Type | What You Get | Resolution | Cost |
|------|-------------|-----------|------|
| **Default (Basic)** | CPU, Network, Disk (I/O), Status checks | 5-minute | Free |
| **Detailed Monitoring** | Same metrics, more granular | 1-minute | ~$3.50/instance/month |
| **Custom Metrics** | Anything you publish (memory, queue depth, business metrics) | 1-second to 5-min | ~$0.30/metric/month |

**Important: Metrics NOT available by default on EC2:**
- ❌ **Memory utilization** — Requires CloudWatch Agent.
- ❌ **Disk space utilization** — Requires CloudWatch Agent.
- ❌ **Application-level metrics** — Requires custom metrics via SDK or Agent.

**Key metrics per service:**

| Service | Important Metrics |
|---------|------------------|
| **EC2** | `CPUUtilization`, `NetworkIn/Out`, `StatusCheckFailed` |
| **ALB** | `RequestCount`, `HTTPCode_Target_5XX`, `TargetResponseTime`, `HealthyHostCount` |
| **RDS** | `DatabaseConnections`, `FreeStorageSpace`, `CPUUtilization`, `ReadLatency` |
| **Lambda** | `Duration`, `Errors`, `Throttles`, `ConcurrentExecutions`, `Invocations` |
| **SQS** | `ApproximateNumberOfMessagesVisible`, `ApproximateAgeOfOldestMessage` |
| **ECS** | `CPUUtilization`, `MemoryUtilization` (task/service level) |

### Quick Analogy
Metrics = Vital signs at a hospital.
Heart rate (CPU), blood pressure (memory), oxygen level (disk space). Doctors (engineers) monitor these continuously. When something goes out of range, an alarm sounds.

### Architecture View
```
Metrics Flow:

AWS Services ──auto──▶ CloudWatch Metrics (default, free)

EC2 ──CloudWatch Agent──▶ Custom Metrics (memory, disk)

Your App ──SDK/API──▶ Custom Metrics (orders/min, errors/min)
  import boto3
  cloudwatch = boto3.client('cloudwatch')
  cloudwatch.put_metric_data(
      Namespace='Custom/MyApp',
      MetricData=[{
          'MetricName': 'OrdersProcessed',
          'Value': 42,
          'Unit': 'Count'
      }]
  )

Metric → Alarm → Action:
  CPUUtilization > 80% for 5 min
       │
       ▼
  CloudWatch Alarm (ALARM state)
       │
       ├──▶ SNS → Email/Slack
       ├──▶ Auto Scaling → Add instances
       └──▶ Lambda → Custom remediation

Metric Math:
  error_rate = (errors / invocations) * 100
  → Alarm when error_rate > 5%
```

### Hands-On (Step-by-step Lab)
1. Go to **CloudWatch → Metrics → All Metrics → EC2**.
2. Select `CPUUtilization` for an instance → View graph.
3. Change period to 1 minute (requires detailed monitoring).
4. Go to **Metrics → Browse → Lambda** → Select `Errors` metric.
5. Use **Metric Math**: Add `m1 / m2 * 100` where m1 = Errors, m2 = Invocations → Error rate %.
6. **Publish a custom metric** from CLI:
```bash
aws cloudwatch put-metric-data \
  --namespace "Custom/MyApp" \
  --metric-name "ActiveUsers" \
  --value 150 \
  --unit Count
```
7. Find it in CloudWatch → Metrics → Custom → `Custom/MyApp`.

### Common Mistakes
- ❌ Assuming memory and disk metrics exist by default on EC2 — they don't.
- ❌ Not enabling detailed monitoring for production instances — 5-minute granularity is too slow for alarms.
- ❌ Creating too many custom metrics without a naming convention — becomes unmanageable.
- ❌ Using Average when you should use P99 (percentile) — average hides tail latency.
- ❌ Not using Metric Math — manually calculating derived metrics is error-prone.

### Pro Tips
- ✅ **Install CloudWatch Agent on all EC2 instances** for memory and disk metrics.
- ✅ Enable **detailed monitoring** (1-minute) for all production instances.
- ✅ Use **P99 latency** instead of average — it shows what your slowest users experience.
- ✅ Use **Metric Math** for derived metrics: error rates, success ratios, cost per transaction.
- ✅ Use **Anomaly Detection** — ML-based bands that auto-adjust for seasonal patterns.
- ✅ Use **Embedded Metric Format (EMF)** in Lambda to publish custom metrics via log lines (no SDK calls, no extra cost per PutMetricData).
- ✅ In interviews: "We monitor default metrics plus custom metrics published via CloudWatch Agent on EC2 and EMF in Lambda. We use P99 latency for SLA tracking, Metric Math for error rate calculations, and anomaly detection for dynamic baselines."

---

## 4. CloudWatch Alarms

### What is it?
A CloudWatch Alarm monitors a metric and triggers an action when the metric crosses a threshold.
Three states: **OK** (within threshold), **ALARM** (threshold breached), **INSUFFICIENT_DATA** (not enough data).
Alarms drive automated responses — notifications, auto-scaling, instance recovery.

### Why it matters in production
- Alarms are your early warning system. They detect problems before users do.
- They're the trigger for auto-scaling (scale up when busy), notifications (alert the team), and auto-remediation (restart a server).
- Without alarms, you only find out about problems when users complain.

### Core Ideas

**Alarm components:**
- **Metric** — What to watch (e.g., `CPUUtilization`).
- **Threshold** — When to trigger (e.g., `> 80%`).
- **Period** — Time window per datapoint (e.g., 300 seconds).
- **Evaluation periods** — How many periods must breach to trigger (e.g., 3 out of 3).
- **Datapoints to alarm** — M out of N periods (e.g., 3 out of 5 — tolerates 2 brief spikes).
- **Comparison operator** — `GreaterThanThreshold`, `LessThanThreshold`, etc.
- **Actions** — What happens when the alarm fires.

**Alarm actions:**

| Action | What It Does | When to Use |
|--------|-------------|-------------|
| **SNS notification** | Send email/SMS/Slack | Alert the on-call team |
| **Auto Scaling policy** | Add or remove instances | Handle traffic spikes |
| **EC2 action** | Stop, terminate, reboot, or recover instance | Auto-remediate instance failures |
| **Lambda** | Run custom code | Complex remediation logic |
| **Systems Manager** | Run automation runbooks | Automated incident response |

**Composite Alarms:**
- Combine multiple alarms with AND/OR logic.
- Example: ALARM only if `CPUUtilization > 80%` AND `MemoryUtilization > 90%`.
- Reduces alarm noise — both conditions must be true.

### Quick Analogy
Alarm = A smoke detector in your house.
It monitors the air (metric). When smoke exceeds a level (threshold) for a sustained period (evaluation), it screams (action). A composite alarm = The alarm only goes off if smoke is detected AND the temperature is above 150°F — fewer false alarms.

### Architecture View
```
Alarm Configuration:
┌─────────────────────────────────────────────────────┐
│ Alarm: high-cpu-web-servers                         │
│                                                     │
│ Metric: AWS/EC2 → CPUUtilization                   │
│ Statistic: Average                                  │
│ Period: 300 seconds (5 min)                        │
│ Threshold: > 80%                                   │
│ Evaluation: 3 out of 3 periods (15 min sustained)  │
│                                                     │
│ Actions:                                           │
│ ├── ALARM → SNS: ops-team-alerts (Slack + Email)   │
│ ├── ALARM → Auto Scaling: scale-out-policy         │
│ └── OK → SNS: ops-team-recovery (resolved)         │
└─────────────────────────────────────────────────────┘

Composite Alarm:
┌──────────────────────────────────────┐
│ Composite: critical-web-health       │
│                                      │
│ Rule: high-cpu-alarm                 │
│       AND                            │
│       high-5xx-alarm                 │
│       AND                            │
│       low-healthy-hosts-alarm        │
│                                      │
│ Only triggers when ALL three fire    │
│ → Pages the on-call engineer         │
└──────────────────────────────────────┘

Essential Production Alarms:
┌────────────────────────────────────────────────┐
│ ALB:  5xx > 10 in 5 min        → Alert        │
│ ALB:  TargetResponseTime P99 > 2s → Alert     │
│ ALB:  HealthyHostCount < 2     → CRITICAL      │
│ EC2:  CPUUtilization > 80%     → Scale + Alert │
│ RDS:  FreeStorageSpace < 5GB   → Alert         │
│ RDS:  DatabaseConnections > 80% → Alert        │
│ Lambda: Errors > 5 in 5 min   → Alert         │
│ Lambda: Throttles > 0          → Alert         │
│ SQS:  AgeOfOldestMessage > 5min→ Alert         │
│ Root Login: Any root login     → CRITICAL       │
└────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **CloudWatch → Alarms → Create Alarm**.
2. Select metric: **EC2 → Per-Instance → CPUUtilization**.
3. Statistic: Average. Period: 5 minutes.
4. Threshold: Greater than 80%.
5. Evaluation: 3 out of 3 datapoints (15 min sustained).
6. Action: **Create SNS topic** → Add your email.
7. Name: `high-cpu-alarm`. Create.
8. Stress test the EC2: `stress --cpu 4 --timeout 900`.
9. Wait 15 minutes → Alarm triggers → Email arrives.
10. Stop the stress test → Alarm returns to OK → Recovery email arrives.

### Common Mistakes
- ❌ Setting 1 out of 1 evaluation — causes alarm flapping on brief spikes.
- ❌ No OK action — you get alerted but never know when it's resolved.
- ❌ Too many alarms → alert fatigue → team ignores them all.
- ❌ Not using composite alarms — too many false positives from individual alarms.
- ❌ No alarm on billing — surprise bills.

### Pro Tips
- ✅ Use **3 out of 3** or **3 out of 5** evaluation to avoid flapping.
- ✅ Set both **ALARM** and **OK** actions — know when the problem starts AND ends.
- ✅ Use **Composite Alarms** for critical alerts to reduce noise.
- ✅ Create a **billing alarm** on Day 1: `EstimatedCharges > $50`.
- ✅ Use alarms as **CodeDeploy rollback triggers** (error rate alarm → auto-rollback).
- ✅ Use **Anomaly Detection alarms** when you don't know the right threshold.
- ✅ In interviews: "We have alarms on all critical metrics: ALB 5xx, healthy host count, RDS connections, Lambda errors, and SQS queue age. We use composite alarms to reduce noise and 3-of-5 evaluation to avoid flapping. Alarms trigger SNS for notifications and are configured as rollback triggers in CodeDeploy."

---

## 5. AWS X-Ray

### What is it?
X-Ray is a distributed tracing service. It traces requests as they flow across multiple services.
You can see the complete journey of a request: API Gateway → Lambda → DynamoDB → SQS → Lambda.
It shows where time is spent, where errors occur, and which services are bottlenecks.

### Why it matters in production
- In microservices, a single user request touches 5-20 services. When it's slow, which service caused it?
- X-Ray answers: "Where did the latency come from?" and "Which service threw the error?"
- It's essential for debugging distributed systems.

### Core Ideas

**Key concepts:**

| Concept | What It Is |
|---------|-----------|
| **Trace** | The full end-to-end journey of a request across all services |
| **Segment** | A piece of the trace representing one service's work |
| **Subsegment** | A finer breakdown within a segment (e.g., a DynamoDB query within a Lambda function) |
| **Trace ID** | A unique identifier that ties all segments of one request together |
| **Service Map** | Visual graph showing all services and their connections, latency, and error rates |
| **Annotations** | Key-value pairs for filtering traces (e.g., `userId=abc`, `env=prod`) |
| **Metadata** | Additional data attached to segments (not indexed, not searchable) |
| **Sampling** | Controls what percentage of requests are traced (to reduce cost) |

**X-Ray integration:**

| Service | How to Enable |
|---------|--------------|
| **Lambda** | Toggle "Active tracing" ON in function config |
| **API Gateway** | Enable tracing in stage settings |
| **ECS / Fargate** | Run X-Ray daemon as a sidecar container |
| **EC2** | Install and run X-Ray daemon |
| **ALB** | Automatically adds trace headers |
| **SQS / SNS** | Propagate trace headers automatically |

**X-Ray SDK (instrument your code):**
```python
from aws_xray_sdk.core import xray_recorder
from aws_xray_sdk.core import patch_all

patch_all()  # Auto-instrument boto3, requests, etc.

@xray_recorder.capture('process_order')
def process_order(order_id):
    xray_recorder.current_subsegment().put_annotation('orderId', order_id)
    # Your code here — all AWS SDK calls are auto-traced
```

### Quick Analogy
X-Ray = A GPS tracker on a delivery package.
You can see where the package went (service map), how long it stayed at each stop (latency), and exactly where it got stuck if it was late (bottleneck). Without the tracker, you just know "the package was late" but not why.

### Architecture View
```
X-Ray Service Map (what you see):

  ┌───────────┐   45ms   ┌───────────┐   120ms  ┌───────────┐
  │ API       │─────────▶│  Lambda   │────────▶│ DynamoDB  │
  │ Gateway   │          │ process-  │         │ orders    │
  │           │          │ order     │         │ table     │
  │ 200ms avg │          │ 5% errors │         │ 2% errors │
  └───────────┘          └─────┬─────┘         └───────────┘
                               │
                          30ms │
                               ▼
                         ┌───────────┐
                         │   SQS     │
                         │ email-    │
                         │ queue     │
                         │ 0% errors │
                         └─────┬─────┘
                               │
                          50ms │
                               ▼
                         ┌───────────┐
                         │  Lambda   │
                         │ send-     │
                         │ email     │
                         │ 1% errors │
                         └───────────┘

Trace Detail (one request):
  ┌─── API Gateway (15ms) ──────────────────────────────────────────┐
  │  ┌─── Lambda: process-order (120ms) ──────────────────────────┐ │
  │  │  ┌─── DynamoDB: GetItem (8ms) ✅ ─────────┐               │ │
  │  │  ┌─── DynamoDB: PutItem (25ms) ✅ ─────────┐               │ │
  │  │  ┌─── SQS: SendMessage (12ms) ✅ ──────────┐               │ │
  │  └────────────────────────────────────────────────────────────┘ │
  └────────────────────────────────────────────────────────────────┘
  Total: 180ms
  Bottleneck: DynamoDB PutItem (25ms)
```

### Hands-On (Step-by-step Lab)
1. Create a Lambda function → **Enable Active Tracing** in configuration.
2. Add the X-Ray SDK to your function:
```python
from aws_xray_sdk.core import patch_all
patch_all()

def lambda_handler(event, context):
    import boto3
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('my-table')
    table.get_item(Key={'id': '123'})
    return {'statusCode': 200}
```
3. Invoke the function several times.
4. Go to **X-Ray → Service Map** → See Lambda → DynamoDB connection.
5. Click on a trace → See the full breakdown with timing for each segment.
6. Go to **X-Ray → Traces** → Filter: `annotation.orderId = "abc"`.
7. Identify the slowest subsegment → That's your bottleneck.

### Common Mistakes
- ❌ Not enabling X-Ray — flying blind in microservices.
- ❌ Tracing 100% of requests in production — expensive. Use sampling (5-10%).
- ❌ Not adding annotations — can't filter traces by business context (userId, orderId).
- ❌ Not instrumenting all services in the chain — trace breaks at uninstrumented services.
- ❌ Confusing X-Ray (traces) with CloudWatch Logs (text logs) — they complement each other.

### Pro Tips
- ✅ Use **X-Ray for latency debugging** and **CloudWatch Logs for error details**. Together = full picture.
- ✅ Add **annotations** for business context: `orderId`, `userId`, `region`.
- ✅ Set **sampling rules** to trace 5% of normal requests and 100% of errors.
- ✅ Use X-Ray with **ECS sidecar** for container-based microservices.
- ✅ Use **X-Ray Groups** to create filtered views (e.g., `fault = true` for error traces only).
- ✅ In interviews: "We use X-Ray for distributed tracing across our microservices. The service map shows dependency latency and error rates at a glance. We add annotations for business context and use sampling rules to trace 5% of normal traffic and 100% of errors. It's the first tool we open when investigating latency issues."

---

## 6. CloudTrail

### What is it?
CloudTrail records **every API call** made in your AWS account.
Who did what, when, from where, and what was the result — all logged.
It's your audit trail for security, compliance, and governance.

### Why it matters in production
- "Who deleted the production database?" — CloudTrail tells you.
- It's **required** for compliance (SOC2, PCI-DSS, HIPAA, ISO 27001).
- It detects unauthorized access, suspicious activity, and configuration changes.

### Core Ideas

**What CloudTrail logs:**
- **Management Events** — Control plane operations (create/delete/modify resources). Default: ON.
- **Data Events** — Data plane operations (S3 GetObject, Lambda Invoke). Default: OFF (high volume, extra cost).
- **Insights Events** — Unusual activity detection (spike in API calls). Optional.

**Log entry contains:**
- **Who** — IAM user, role, or root account (identity).
- **What** — API action (`RunInstances`, `DeleteBucket`, `PutObject`).
- **When** — Timestamp.
- **Where** — Source IP address, user agent.
- **Which resource** — Resource ARN affected.
- **Result** — Success or failure (and error code if failed).

**CloudTrail setup options:**

| Option | Coverage | Cost |
|--------|---------|------|
| **Event History** | Last 90 days, management events, one Region | Free |
| **Trail (single Region)** | All events, sent to S3 | S3 storage cost |
| **Trail (all Regions)** | All Regions, all events, sent to S3 | S3 storage cost |
| **Organization Trail** | All accounts in AWS Organization | S3 storage cost |
| **CloudTrail Lake** | Managed query engine for trail data | Per query + storage |

### Quick Analogy
CloudTrail = Security camera footage for your AWS account.
Every API call is recorded. If something goes wrong (security breach, accidental delete), you rewind the footage and see exactly who did it, when, and how. You can't tamper with the footage (log file integrity validation).

### Architecture View
```
Everything in AWS goes through the API:
  Console click → API call → CloudTrail logs it
  CLI command → API call → CloudTrail logs it
  SDK call → API call → CloudTrail logs it
  Automation → API call → CloudTrail logs it

CloudTrail Architecture:
┌────────────────────────────────────────────────────────┐
│ AWS Account                                            │
│                                                        │
│ Every API call ──▶ CloudTrail                          │
│                      │                                 │
│                      ├──▶ Event History (90 days, free)│
│                      │                                 │
│                      ├──▶ S3 Bucket (long-term archive)│
│                      │    └── Enable log file integrity│
│                      │        validation               │
│                      │                                 │
│                      ├──▶ CloudWatch Logs (real-time   │
│                      │    queries, metric filters)     │
│                      │    └── Metric Filter:           │
│                      │        "root login" → Alarm     │
│                      │                                 │
│                      └──▶ EventBridge (react to events)│
│                           └── Rule: DeleteBucket       │
│                               → Lambda → Slack alert   │
└────────────────────────────────────────────────────────┘

CloudTrail Log Entry Example:
{
  "eventTime": "2024-01-15T14:30:00Z",
  "userIdentity": {
    "type": "IAMUser",
    "userName": "bob",
    "arn": "arn:aws:iam::123456789012:user/bob"
  },
  "eventSource": "ec2.amazonaws.com",
  "eventName": "TerminateInstances",
  "sourceIPAddress": "203.0.113.50",
  "requestParameters": {
    "instancesSet": {"items": [{"instanceId": "i-1234567890"}]}
  },
  "responseElements": {"instancesSet": {"items": [{"currentState": {"name": "shutting-down"}}]}}
}
→ Bob terminated instance i-1234567890 at 2:30 PM from IP 203.0.113.50
```

### Hands-On (Step-by-step Lab)
1. Go to **CloudTrail → Event History** → Browse recent API calls in your account.
2. Filter by: Event name = `RunInstances` → See who launched EC2 instances.
3. Filter by: User name = `root` → See if anyone used the root account.
4. Create a **Trail:**
   - Name: `prod-audit-trail`.
   - Apply to all Regions: Yes.
   - S3 bucket: Create new.
   - Enable **Log file validation**: Yes.
   - Send to **CloudWatch Logs**: Yes.
5. In CloudWatch Logs, create a **Metric Filter:**
   - Pattern: `{ $.userIdentity.type = "Root" }`.
   - Metric: `RootAccountUsage`.
6. Create alarm: `RootAccountUsage > 0` → SNS → Email → CRITICAL.

### Common Mistakes
- ❌ Not creating a trail — Event History only keeps 90 days and is limited.
- ❌ Not enabling log file integrity validation — can't prove logs weren't tampered with.
- ❌ Not enabling for all Regions — attacks often target unused Regions.
- ❌ Storing CloudTrail logs in the same account — if account is compromised, attacker deletes logs.
- ❌ Not monitoring for root account usage — root login should be extremely rare.
- ❌ Not enabling Data Events for S3 — can't audit who accessed sensitive data.

### Pro Tips
- ✅ **Enable a trail for all Regions** in every AWS account. Non-negotiable.
- ✅ Send logs to a **centralized security account** S3 bucket (cross-account).
- ✅ Enable **log file integrity validation** to detect tampering.
- ✅ Send CloudTrail to **CloudWatch Logs** → Create Metric Filters for:
  - Root account login
  - Console login without MFA
  - Security Group changes
  - IAM policy changes
  - Unauthorized API calls (`errorCode = AccessDenied`)
- ✅ Use **CloudTrail Lake** or **Athena** to query large volumes of trail data with SQL.
- ✅ Enable **S3 Data Events** for buckets containing sensitive data (PII, financial).
- ✅ In interviews: "CloudTrail is enabled for all Regions in all accounts, with logs shipped to a centralized security account S3 bucket. Log file integrity validation is enabled. CloudWatch Logs metric filters alert on root usage, IAM changes, and unauthorized API calls. We use Athena to query historical CloudTrail data during incident investigations."
