# 10 AWS System Design Scenarios for DevOps Interviews

> These are the most commonly asked "Design a system that..." questions.
> Each scenario follows the same structure: Overview → Architecture → Services → Why → Failure Handling.
> Practice explaining these out loud in 5-7 minutes each.

---

## Scenario 1: E-Commerce Platform (3-Tier Web App)

### Problem
"Design a scalable, highly available e-commerce platform that handles 50,000 concurrent users, with product search, shopping cart, checkout, and order management."

### Architecture Overview
```
Users
  │
  ▼
Route 53 (DNS, latency routing)
  │
  ▼
CloudFront (CDN — static assets, images, CSS/JS)
  │
  ▼
WAF (block SQL injection, XSS, bad bots)
  │
  ▼
ALB (public subnets, 3 AZs)
  ├── /api/products/*  → Product Service (ECS Fargate)
  ├── /api/cart/*       → Cart Service (ECS Fargate)
  ├── /api/orders/*     → Order Service (ECS Fargate)
  └── /api/search/*     → Search Service (ECS Fargate)
         │
         ├──▶ ElastiCache Redis (session store + product cache)
         ├──▶ Aurora MySQL (users, products, orders — writer + 2 readers)
         ├──▶ DynamoDB (shopping cart — TTL for expiry)
         ├──▶ OpenSearch (product full-text search)
         └──▶ SQS → Lambda (order processing, email, inventory update)
                         │
                         ▼
                   SNS → SES (order confirmation emails)

Static Assets: S3 → CloudFront
Secrets: Secrets Manager (DB credentials, API keys)
Monitoring: CloudWatch + X-Ray + CloudTrail
CI/CD: CodePipeline → CodeBuild → CodeDeploy (ECS Blue/Green)
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **ECS Fargate** | Microservices | Serverless containers. No EC2 management. Per-service scaling. |
| **Aurora MySQL** | Primary DB | 5x MySQL speed. Multi-AZ. Reader endpoints for read scaling. |
| **DynamoDB** | Shopping cart | Key-value access. TTL auto-expires abandoned carts. Serverless. |
| **ElastiCache Redis** | Cache + sessions | Microsecond reads. Reduces DB load by 90%. Session store for stateless app tier. |
| **OpenSearch** | Product search | Full-text search with filters, facets, relevance ranking. |
| **SQS + Lambda** | Async processing | Decouple order placement from payment, email, inventory. Absorbs spikes. |
| **CloudFront + S3** | Static content | Edge-cached images/CSS/JS. Reduces ALB load and latency. |
| **WAF** | Security | Blocks OWASP top 10 attacks at the edge. |

### Failure Handling
- **AZ failure:** ALB + ASG + Aurora span 3 AZs. Traffic reroutes automatically.
- **Database failure:** Aurora failover in ~30s. Readers absorb reads during failover.
- **Cache failure:** Redis Multi-AZ failover. App falls back to DB on cache miss.
- **Service failure:** ECS replaces unhealthy tasks. SQS buffers orders during processing outage.
- **Traffic spike:** Auto Scaling on each ECS service. DynamoDB On-Demand scales automatically. SQS absorbs burst.
- **Deployment failure:** Blue/Green via CodeDeploy. CloudWatch alarms trigger automatic rollback.

---

## Scenario 2: Real-Time Chat Application

### Problem
"Design a real-time chat application supporting 1 million concurrent WebSocket connections with message persistence and online presence."

### Architecture Overview
```
Users (mobile + web)
  │
  ▼
Route 53
  │
  ▼
NLB (Layer 4 — WebSocket is TCP, needs persistent connections)
  │
  ▼
ECS Fargate (WebSocket servers — auto-scaled)
  │
  ├──▶ API Gateway (REST for auth, history, user profile)
  │       └──▶ Lambda (user management, message history)
  │
  ├──▶ ElastiCache Redis
  │       ├── Pub/Sub (message fanout across server instances)
  │       ├── Online presence (SET user:status "online" EX 60)
  │       └── Recent messages cache
  │
  ├──▶ DynamoDB
  │       ├── Messages table (PK: chatRoomId, SK: timestamp)
  │       ├── Users table (PK: userId)
  │       └── TTL: Delete messages after 90 days
  │
  └──▶ DynamoDB Streams → Lambda
          ├── Push notifications (SNS → mobile push)
          └── Unread count update

Media (images/files):
  Client → S3 pre-signed URL → Upload to S3 → CloudFront for delivery
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **NLB** | WebSocket entry | Layer 4. Handles millions of TCP connections. Static IPs. |
| **ECS Fargate** | WebSocket servers | Scales horizontally. Each task handles thousands of connections. |
| **ElastiCache Redis** | Message routing + presence | Pub/Sub fans messages across server instances. Sub-ms latency. |
| **DynamoDB** | Message storage | Partition key = chatRoom. Scales infinitely. TTL for auto-cleanup. |
| **DynamoDB Streams + Lambda** | Event processing | Triggers push notifications on new messages without polling. |
| **S3 + CloudFront** | Media delivery | Pre-signed upload URLs. CDN for fast global delivery. |

### Failure Handling
- **Server failure:** NLB health checks detect dead tasks. ECS replaces them. Clients auto-reconnect.
- **Redis failure:** Multi-AZ failover. Clients reconnect to new server and re-subscribe.
- **Message loss:** DynamoDB is durable (3 AZ replication). Messages never lost even if servers crash.
- **Spike (viral chat):** ECS auto-scales WebSocket servers. DynamoDB On-Demand handles write surge. Redis Cluster scales reads.
- **Region failure:** DynamoDB Global Tables replicate to DR Region. Route 53 failover.

---

## Scenario 3: CI/CD Pipeline for Microservices

### Problem
"Design a CI/CD pipeline for 15 microservices, each with independent deployments, using containers on ECS. Include testing, security scanning, and production safety."

### Architecture Overview
```
Developer → Git push (GitHub)
  │
  ▼
CodePipeline (one pipeline per microservice — 15 pipelines)
  │
  ├── SOURCE: GitHub (webhook on push to main)
  │
  ├── BUILD (CodeBuild):
  │     ├── Run unit tests
  │     ├── Run SAST (static code analysis — CodeGuru/Snyk)
  │     ├── Build Docker image
  │     ├── Scan image for vulnerabilities (ECR image scanning)
  │     ├── Push to ECR (tagged with commit SHA)
  │     └── Output: imagedefinitions.json
  │
  ├── DEPLOY TO STAGING (CodeDeploy → ECS Blue/Green):
  │     ├── Deploy to staging ECS cluster
  │     ├── Run integration tests (CodeBuild)
  │     └── Run load tests (CodeBuild + Artillery/k6)
  │
  ├── APPROVAL: Manual approval (SNS → Slack notification)
  │
  └── DEPLOY TO PRODUCTION (CodeDeploy → ECS Blue/Green):
        ├── Canary: 10% traffic for 10 min
        ├── CloudWatch Alarms: 5xx, latency, error rate
        ├── Alarms fire → Automatic rollback
        └── No alarms → 100% traffic to new version

Shared Infrastructure (deployed via CDK):
  ├── VPC, subnets, NAT, endpoints
  ├── ALB with path-based routing per service
  ├── ECR repositories per service
  ├── Secrets Manager for credentials
  └── CloudWatch dashboards per service
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **CodePipeline** | Orchestration | AWS-native. One pipeline per service for independent deploys. |
| **CodeBuild** | Build + test | Serverless. Parallel builds. Caches dependencies. No Jenkins to manage. |
| **CodeDeploy** | Deployment | ECS Blue/Green with canary. Automatic rollback on alarms. |
| **ECR** | Image registry | AWS-native. Image scanning for CVEs. Lifecycle policies for cleanup. |
| **CDK** | Infra as Code | Shared infra and pipeline definitions in TypeScript. Testable. |
| **CloudWatch Alarms** | Rollback trigger | Auto-rollback if error rate or latency spikes during canary window. |

### Failure Handling
- **Build failure:** Pipeline stops. Developer notified via SNS → Slack. Fix and re-push.
- **Security vulnerability found:** ECR scan fails → Pipeline blocks deployment. Image never reaches production.
- **Staging test failure:** Pipeline stops before production. No impact to users.
- **Production canary failure:** CloudWatch alarm fires → CodeDeploy auto-rolls back to previous version in <1 minute. Only 10% of traffic was affected.
- **Full Region CI/CD failure:** Pipeline definitions are in CDK (Git). Recreate in another Region.

---

## Scenario 4: Serverless Data Processing Pipeline

### Problem
"Design a pipeline that processes 10 million CSV files uploaded daily to S3, transforms the data, and loads it into a data warehouse for analytics."

### Architecture Overview
```
Data Producers → Upload CSVs to S3 (landing bucket)
  │
  ▼
S3 Event Notification → SQS (buffer)
  │
  ▼
Lambda (Transformer):
  ├── Read CSV from S3
  ├── Validate and clean data
  ├── Convert to Parquet (columnar, compressed)
  ├── Write to S3 (processed bucket)
  └── On failure → DLQ (SQS) → Lambda (alert + retry)
  │
  ▼
S3 (processed bucket — Parquet files)
  │
  ├──▶ Glue Crawler → Glue Data Catalog (schema discovery)
  │
  ├──▶ Athena (ad-hoc SQL queries on S3 — pay per query)
  │
  └──▶ Redshift Spectrum / COPY → Redshift (data warehouse)
          │
          ▼
       QuickSight (BI dashboards for business users)

Monitoring:
  ├── CloudWatch Metrics: Lambda errors, SQS queue depth, processing time
  ├── CloudWatch Alarms: DLQ depth > 0 → SNS → PagerDuty
  └── S3 Lifecycle: Move processed files to Glacier after 90 days
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **S3** | Storage (landing + processed) | Infinite scale. Event notifications trigger processing. Cheap. |
| **SQS** | Buffer | Absorbs upload spikes. Controls Lambda concurrency. Prevents overwhelming. |
| **Lambda** | Transform | Serverless. Scales to thousands concurrent. Pay per invocation. Perfect for file-by-file processing. |
| **Glue** | Schema discovery | Auto-crawls Parquet files and creates a queryable catalog. |
| **Athena** | Ad-hoc queries | Serverless SQL on S3. No infrastructure. Pay per query. |
| **Redshift** | Data warehouse | Structured analytics. Complex aggregations. Powers BI dashboards. |
| **S3 Lifecycle** | Cost | Move old data to Glacier. Delete after retention period. |

### Failure Handling
- **Lambda failure:** SQS retries 3 times. After 3 failures → DLQ. Alarm triggers investigation.
- **Bad data:** Validation in Lambda catches malformed CSVs. Bad files moved to error bucket with metadata.
- **SQS overflow:** Lambda reserved concurrency prevents runaway scaling. SQS holds messages for up to 14 days.
- **S3 data loss:** 11 nines durability. Cross-Region replication for critical data.
- **Redshift down:** Athena provides backup query capability directly on S3. No single point of failure for analytics.

---

## Scenario 5: Multi-Account Enterprise Landing Zone

### Problem
"Design a secure multi-account AWS strategy for a 500-person company with dev, staging, production, security, and shared services accounts."

### Architecture Overview
```
AWS Organizations (Management Account)
  │
  ├── OU: Security
  │     ├── Security Account (centralized logs, GuardDuty, Security Hub)
  │     └── Audit Account (read-only access to all accounts)
  │
  ├── OU: Infrastructure
  │     ├── Shared Services (Transit Gateway, DNS, CI/CD)
  │     └── Networking (central VPC, Direct Connect, VPN)
  │
  ├── OU: Workloads
  │     ├── Dev Account
  │     ├── Staging Account
  │     └── Production Account
  │
  └── OU: Sandbox
        └── Individual sandbox accounts for experimentation

SCPs (Guardrails):
  ├── Deny leaving the organization
  ├── Deny disabling CloudTrail
  ├── Deny creating resources outside approved Regions
  ├── Deny deleting VPC Flow Logs
  └── Require encryption on S3 and EBS

Account Baselines (via CloudFormation StackSets):
  ├── CloudTrail → Security account S3 bucket
  ├── AWS Config → Security account
  ├── GuardDuty → Security account (delegated admin)
  ├── VPC Flow Logs → CloudWatch Logs
  ├── IAM password policy
  └── Budget alerts

Networking:
  Transit Gateway (Shared Services)
    ├── Dev VPC (10.1.0.0/16)
    ├── Staging VPC (10.2.0.0/16)
    ├── Prod VPC (10.3.0.0/16)
    └── On-Prem (Direct Connect + VPN backup)

Access:
  IAM Identity Center (SSO)
    ├── Developers → Dev account (PowerUser), Staging (ReadOnly)
    ├── DevOps → All accounts (Admin for Dev/Staging, restricted for Prod)
    ├── Security → Security account (Admin), all others (SecurityAudit)
    └── Executives → Billing dashboard (ReadOnly)
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **Organizations + SCPs** | Account governance | Centralized management. SCPs enforce non-negotiable guardrails. |
| **IAM Identity Center** | Access management | Single sign-on across all accounts. Role-based. No per-account IAM users. |
| **CloudFormation StackSets** | Account baselines | Deploy security baseline to all accounts automatically on creation. |
| **Transit Gateway** | Networking | Hub-and-spoke. All VPCs communicate centrally. Scales to 5,000 attachments. |
| **Centralized Security Account** | Audit + monitoring | CloudTrail, Config, GuardDuty findings in one place. Immutable logs. |
| **Direct Connect + VPN** | Hybrid | DX for primary connectivity. VPN as automatic failover. |

### Failure Handling
- **Account compromise:** SCPs prevent attacker from disabling CloudTrail or leaving the org. Security account logs are in a separate account — attacker can't delete them.
- **Network failure:** Transit Gateway spans 3 AZs. Direct Connect has VPN failover.
- **Guardrail bypass:** SCPs are enforced at the Organization level — even account admins can't override them.
- **New account creation:** Control Tower auto-applies baseline stack. No manual setup needed.
- **Cost runaway:** Per-account budgets with alerts. SCPs can restrict expensive instance types in sandbox accounts.

---

## Scenario 6: IoT Data Ingestion Platform

### Problem
"Design a platform that ingests sensor data from 100,000 IoT devices sending readings every 5 seconds, stores the data, and provides real-time dashboards."

### Architecture Overview
```
IoT Devices (100K devices × 1 msg/5s = 20,000 msgs/sec)
  │
  ▼
AWS IoT Core (MQTT broker — managed, auto-scales)
  │
  ├──▶ IoT Rules Engine
  │       │
  │       ├──▶ Kinesis Data Streams (real-time ingestion)
  │       │       │
  │       │       ├──▶ Lambda (real-time anomaly detection)
  │       │       │       └── SNS → Alert if temperature > threshold
  │       │       │
  │       │       └──▶ Kinesis Data Firehose → S3 (batch, Parquet)
  │       │               └── Glue Catalog → Athena (historical queries)
  │       │
  │       └──▶ Timestream (time-series database)
  │               └── Grafana (real-time dashboards)
  │
  └──▶ IoT Device Shadow (last known state per device)

Device Management:
  ├── IoT Device Defender (security monitoring)
  ├── IoT Jobs (OTA firmware updates)
  └── X.509 certificates for device authentication

Storage Lifecycle:
  S3: Raw data (Standard 30d) → IA (90d) → Glacier (365d) → Delete (7yr)
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **IoT Core** | MQTT broker | Managed. Handles millions of connections. Built-in device auth (X.509). |
| **Kinesis Data Streams** | Real-time ingestion | Handles 20K+ msg/sec with sharding. Real-time processing. |
| **Timestream** | Time-series DB | Purpose-built for sensor data. Auto-tiering (memory → magnetic). Fast queries. |
| **Kinesis Firehose** | Batch to S3 | Auto-batches and converts to Parquet. No code needed. |
| **Lambda** | Anomaly detection | Real-time processing per record. Triggers alerts on thresholds. |
| **Grafana** | Dashboards | Rich time-series visualization. Connects natively to Timestream. |

### Failure Handling
- **Device offline:** IoT Device Shadow stores last known state. Device syncs when reconnected.
- **Kinesis shard overload:** Auto-scale shards. Use enhanced fan-out for multiple consumers.
- **Lambda processing failure:** Kinesis retries until success or record expires (24h–7d retention).
- **Data loss:** Kinesis retains data 24h (up to 7 days). Firehose buffers in S3. Timestream auto-replicates across 3 AZs.
- **Dashboard down:** Grafana is stateless. Redeploy from IaC. Data in Timestream is unaffected.

---

## Scenario 7: Video Streaming Platform

### Problem
"Design a platform where creators upload videos, the system transcodes them into multiple formats, and viewers stream globally with low latency."

### Architecture Overview
```
Creator Upload Flow:
  Creator → API Gateway → Lambda (generate S3 pre-signed URL)
    → Creator uploads directly to S3 (raw-videos bucket)
      → S3 Event → SQS → Lambda (trigger MediaConvert job)
        → MediaConvert (transcode to 360p, 720p, 1080p, 4K)
          → Output to S3 (processed-videos bucket)
            → DynamoDB (update video status: "ready")
              → SNS → Creator notification ("Video published!")

Viewer Streaming Flow:
  Viewer → Route 53 → CloudFront (global CDN — 400+ edge locations)
    → Origin: S3 (processed-videos bucket)
    → CloudFront Signed URLs (auth — only paid/authorized users)

Metadata:
  DynamoDB: Video metadata (title, creator, views, status)
  OpenSearch: Video search (title, tags, description)
  ElastiCache Redis: Trending videos, view count caching

Analytics:
  CloudFront access logs → S3 → Athena (viewership analytics)
  Kinesis → Lambda → DynamoDB (real-time view counter)
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **S3** | Video storage | Unlimited. Durable. Direct upload via pre-signed URLs. |
| **MediaConvert** | Transcoding | Serverless. Supports HLS/DASH adaptive bitrate. Pay per minute. |
| **CloudFront** | Global delivery | 400+ edge locations. Signed URLs for access control. Low latency. |
| **DynamoDB** | Video metadata | Scalable key-value. Fast lookups by videoId. |
| **SQS** | Job queue | Buffers transcode requests. Handles upload spikes. |
| **OpenSearch** | Video search | Full-text search with relevance ranking on titles and tags. |

### Failure Handling
- **Transcode failure:** SQS retries. After 3 failures → DLQ. Ops team investigates. Video marked "failed" in DynamoDB.
- **Upload failure:** S3 multipart upload auto-retries. Client can resume incomplete uploads.
- **CDN origin failure:** CloudFront serves cached content. S3 has 11 nines durability.
- **Traffic spike (viral video):** CloudFront absorbs 99% of requests at edge. S3 and DynamoDB auto-scale.
- **Regional outage:** S3 Cross-Region Replication. CloudFront auto-fails over to healthy origin.

---

## Scenario 8: Centralized Logging & Monitoring Platform

### Problem
"Design a centralized observability platform for 50 microservices across 3 environments (dev, staging, prod) that provides logs, metrics, traces, and alerting."

### Architecture Overview
```
Data Sources (All Services):
  ├── ECS/Fargate → Fluent Bit sidecar → CloudWatch Logs
  ├── Lambda → Auto → CloudWatch Logs
  ├── EC2 → CloudWatch Agent → CloudWatch Logs + Custom Metrics
  ├── ALB → Access Logs → S3
  ├── VPC → Flow Logs → CloudWatch Logs
  ├── CloudTrail → S3 + CloudWatch Logs
  └── All services → X-Ray (distributed tracing)

Centralized Processing:
  CloudWatch Logs
    │
    ├──▶ CloudWatch Logs Insights (ad-hoc queries)
    │
    ├──▶ Subscription Filter → Kinesis Firehose → OpenSearch
    │       └── Kibana dashboards (log search, analysis)
    │
    ├──▶ Metric Filters → CloudWatch Metrics
    │       └── Alarms → SNS → Slack / PagerDuty
    │
    └──▶ S3 (long-term archive, 1 year retention)
              └── Athena (historical investigation)

Dashboards:
  ┌──────────────────────────────────────────┐
  │ CloudWatch Dashboard (operational):      │
  │   ALB 5xx, ECS CPU, RDS connections,     │
  │   Lambda errors, SQS queue depth         │
  │                                          │
  │ OpenSearch/Kibana (log analysis):        │
  │   Search across all services, correlate  │
  │                                          │
  │ X-Ray Service Map (tracing):             │
  │   Request flow, latency, error sources   │
  └──────────────────────────────────────────┘

Alerting Hierarchy:
  P1 (Critical): Healthy hosts < 2, DB down → PagerDuty (auto-call)
  P2 (High):     5xx > 10/min, latency P99 > 2s → Slack #alerts
  P3 (Warning):  CPU > 70%, queue age > 5 min → Slack #warnings
  P4 (Info):     Deployment complete, scaling event → Slack #deployments
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **CloudWatch Logs** | Central log collection | Every AWS service sends logs here natively. Metric Filters create alarms from log patterns. |
| **OpenSearch + Kibana** | Log search & analysis | Full-text search across millions of log lines. Visual dashboards. |
| **X-Ray** | Distributed tracing | Traces requests across 50 microservices. Finds latency bottlenecks. |
| **CloudWatch Alarms** | Alerting | Native. Composite alarms reduce noise. Actions: SNS, Auto Scaling, Lambda. |
| **Fluent Bit** | Log shipping | Lightweight. ECS sidecar. Routes logs to multiple destinations. |
| **Athena** | Historical queries | SQL on archived S3 logs. Cheap. Good for incident post-mortems. |

### Failure Handling
- **Logging pipeline failure:** Fluent Bit buffers locally. CloudWatch Logs is HA by design. OpenSearch has Multi-AZ.
- **Alert fatigue:** Composite alarms (AND logic) reduce false positives. Tiered severity routes to correct channel.
- **Storage cost:** CloudWatch retention set to 30 days. S3 archive for long-term. Glacier after 1 year.
- **OpenSearch cluster failure:** Multi-AZ. Automated snapshots to S3 for recovery.
- **Incident investigation:** Correlate CloudWatch Logs (what happened), X-Ray (which service), CloudTrail (who changed what), VPC Flow Logs (network connections).

---

## Scenario 9: Scheduled Batch Processing with Cost Optimization

### Problem
"Design a system that runs financial report generation nightly (2 AM–6 AM), processes 50 million records from a database, and delivers PDF reports to S3. Optimize for cost."

### Architecture Overview
```
Trigger:
  EventBridge Schedule (cron: 0 2 * * ? *) → Step Functions

Step Functions Workflow:
  ┌──────────────────────────────────────────────────────┐
  │                                                      │
  │  1. [Extract] Lambda: Query Aurora Read Replica       │
  │     └── Write raw data to S3 (CSV, partitioned)      │
  │                                                      │
  │  2. [Transform] AWS Batch (Fargate Spot):             │
  │     └── Process 50M records in parallel               │
  │     └── Generate aggregations, calculations           │
  │     └── Write results to S3 (Parquet)                 │
  │                                                      │
  │  3. [Generate] Lambda + ReportLab:                    │
  │     └── Generate PDF reports per client               │
  │     └── Upload to S3 (reports bucket)                 │
  │                                                      │
  │  4. [Notify] Lambda:                                  │
  │     └── Send email via SES with S3 pre-signed URLs    │
  │     └── Update DynamoDB (report status: delivered)    │
  │                                                      │
  │  Error handling:                                      │
  │     └── Retry each step 3x with exponential backoff   │
  │     └── On final failure → SNS → PagerDuty            │
  └──────────────────────────────────────────────────────┘

Cost Optimization:
  ├── Aurora Read Replica: Only for reads. Doesn't impact production writer.
  ├── Fargate Spot: 70% cheaper. Batch jobs are fault-tolerant (restart on interruption).
  ├── Step Functions Standard: $0.025/1000 transitions. Cheap for nightly batch.
  ├── Lambda: Pay per invocation only. $0 when not running.
  ├── S3 Lifecycle: Reports → IA after 30 days → Glacier after 90 days.
  └── No idle infrastructure: Everything is serverless or Spot. $0 between 6 AM–2 AM.
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **EventBridge** | Scheduler | Cron trigger. Serverless. Replaces CloudWatch Events. |
| **Step Functions** | Orchestration | Visual workflow. Built-in retries. Error handling. Timeout management. |
| **AWS Batch (Fargate Spot)** | Heavy processing | Managed batch. Spot = 70% cheaper. Auto-retries on interruption. |
| **Aurora Read Replica** | Data source | Reads don't impact production. Dedicated capacity for the batch job. |
| **Lambda** | Light tasks | Extract, report generation, notification. Pay only when running. |
| **SES** | Email delivery | $0.10/1000 emails. Managed deliverability. |

### Failure Handling
- **Spot interruption:** AWS Batch auto-retries the interrupted job on new Spot capacity.
- **Step failure:** Step Functions retries 3x. On final failure → SNS alert + DynamoDB status: "failed".
- **Data inconsistency:** Read Replica snapshot at 2 AM ensures consistent data throughout the job.
- **Report not delivered:** SES bounce/complaint notifications → DLQ → Ops investigation.
- **Entire job failure:** Re-trigger Step Functions manually. Idempotent design — safe to rerun.

---

## Scenario 10: Zero-Downtime Migration from On-Prem to AWS

### Problem
"Migrate a monolithic application from an on-premises data center to AWS with zero downtime, including a 2 TB PostgreSQL database."

### Architecture Overview
```
Phase 1: Foundation (Weeks 1-2)
  ├── Set up AWS Landing Zone (Organizations, VPC, Transit Gateway)
  ├── Establish Direct Connect (primary) + VPN (backup)
  ├── Deploy target VPC: public + private subnets, 3 AZs
  └── Set up CI/CD pipeline in AWS

Phase 2: Database Migration (Weeks 3-4)
  On-Prem PostgreSQL
    │
    ▼
  DMS (Database Migration Service)
    ├── Full load (initial 2 TB copy)
    └── Ongoing CDC (Change Data Capture — real-time replication)
    │
    ▼
  Aurora PostgreSQL (target — Multi-AZ)

  Validation: DMS validation task ensures row counts match.
  Both databases in sync. On-prem is still primary.

Phase 3: Application Migration (Weeks 5-6)
  ├── Re-platform app to ECS Fargate (containerize)
  ├── Deploy behind ALB in AWS
  ├── Test thoroughly against Aurora (using CDC-replicated data)
  └── Both environments running: on-prem (live) + AWS (shadow/test)

Phase 4: Traffic Cutover (Week 7)
  Route 53 Weighted Routing:
    ├── 100% on-prem → 90/10 → 50/50 → 10/90 → 0/100 AWS
    └── Health checks on both. Instant rollback to on-prem if issues.

  Once stable on AWS:
    ├── Stop DMS replication
    ├── Decommission on-prem database
    └── Remove on-prem infrastructure

Post-Migration:
  ├── Right-size instances (Compute Optimizer)
  ├── Add ElastiCache (performance improvement)
  ├── Enable CloudWatch + X-Ray (observability)
  ├── Enable Auto Scaling (elasticity)
  └── Begin strangler fig → microservices (Phase 2 of modernization)
```

### Services Used & Why Chosen

| Service | Purpose | Why This One |
|---------|---------|-------------|
| **DMS** | Database migration | Supports full load + CDC. Zero downtime. Validates data integrity. |
| **Aurora PostgreSQL** | Target database | PostgreSQL compatible. 3x faster. Multi-AZ. No application code changes. |
| **Direct Connect** | Network | High-bandwidth, low-latency link for 2 TB data transfer and ongoing CDC. |
| **Route 53** | Traffic cutover | Weighted routing for gradual cutover. Health checks for instant rollback. |
| **ECS Fargate** | Application hosting | Containerized app. No EC2 management. Easy scaling post-migration. |
| **SCT (Schema Conversion Tool)** | Schema migration | Converts stored procedures, functions if switching engines. |

### Failure Handling
- **DMS replication failure:** DMS auto-recovers and resumes CDC. Full load can be restarted.
- **Data mismatch:** DMS validation task runs after full load. Reports mismatched rows for remediation.
- **Cutover failure:** Route 53 weighted routing → switch 100% back to on-prem in seconds.
- **Application issues on AWS:** Health checks detect problems. Traffic auto-routes to on-prem via Route 53.
- **Direct Connect failure:** VPN backup activates automatically. DMS continues over VPN (slower but no data loss).
- **Post-migration issues:** On-prem not decommissioned for 2 weeks after full cutover. Quick rollback if needed.

---

## 💡 How to Answer System Design Questions

```
1. CLARIFY (1 min):
   Ask: Scale? Users? Read/write ratio? Latency requirements?
         Budget constraints? Compliance? Global or regional?

2. HIGH-LEVEL DESIGN (2 min):
   Draw the architecture. Name key services.
   "Users → CloudFront → ALB → App (ECS) → DB (Aurora)"

3. DEEP DIVE (3 min):
   Explain WHY each service was chosen.
   Discuss data flow. Mention specific configs.
   "Aurora because we need JOINs + 30s failover + read replicas"

4. FAILURE HANDLING (1 min):
   What happens when X fails? How do you detect and recover?
   "If AZ fails, ALB routes to other AZs. RDS fails over in 30s."

5. TRADE-OFFS (1 min):
   What did you optimize for? What did you sacrifice?
   "We chose eventual consistency for DynamoDB to gain speed.
    We chose Fargate over EC2 for simplicity over cost control."

Key phrases:
  "We chose X because..."
  "If this component fails, traffic routes to..."
  "We monitor this with CloudWatch alarms on..."
  "For cost optimization, we use Savings Plans for baseline and Spot for burst."
  "This follows the Well-Architected Framework's reliability pillar."
```

---

*10 scenarios. Real architectures. Production-grade answers.*
*Practice drawing these on a whiteboard. You'll nail the system design round.*
