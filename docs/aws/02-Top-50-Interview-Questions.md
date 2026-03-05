# Top 50 AWS DevOps Interview Questions & Answers

> Short, structured answers. Designed for quick revision. Say these out loud before your interview.

---

## 🟢 BASIC (Questions 1–15)

---

### Q1. What is Cloud Computing?
**Answer:** Using someone else's computers (servers, storage, databases) over the internet instead of buying your own. You pay only for what you use. AWS, Azure, and GCP are the big three providers.

---

### Q2. What are the 3 cloud service models?
**Answer:**
- **IaaS** (Infrastructure) — You manage OS, apps. AWS manages hardware. *Example: EC2.*
- **PaaS** (Platform) — You manage apps and data. AWS manages OS, runtime. *Example: Elastic Beanstalk, RDS.*
- **SaaS** (Software) — You just use it. Provider manages everything. *Example: Gmail, Salesforce.*

---

### Q3. What is the Shared Responsibility Model?
**Answer:**
- **AWS** is responsible for security **OF** the cloud — hardware, network, facilities, managed service engines.
- **You** are responsible for security **IN** the cloud — IAM, security groups, encryption, OS patching (EC2), data, access policies.

---

### Q4. What is IAM? What are its components?
**Answer:** IAM (Identity and Access Management) controls who can do what in your AWS account.
- **Users** — Individual identities (people or apps).
- **Groups** — Collection of users. Attach policies to groups, not individual users.
- **Roles** — Temporary permissions assumed by services or users. No permanent credentials.
- **Policies** — JSON documents that define Allow/Deny on specific actions and resources.

---

### Q5. What is the principle of least privilege?
**Answer:** Give users and services the **minimum permissions** they need to do their job — nothing more. If a developer only needs to read S3, don't give them S3 full access. Limits blast radius if credentials are compromised.

---

### Q6. What is a VPC?
**Answer:** A Virtual Private Cloud is your own isolated network inside AWS. You define the IP range (CIDR), create subnets, set up route tables, and control traffic with Security Groups and NACLs. All your resources (EC2, RDS, etc.) run inside a VPC.

---

### Q7. What makes a subnet public vs private?
**Answer:** The **route table** — not a setting on the subnet.
- **Public subnet:** Route table has `0.0.0.0/0 → Internet Gateway`. Instance has a public IP.
- **Private subnet:** Route table has `0.0.0.0/0 → NAT Gateway` (or no internet route at all).

---

### Q8. What is the difference between Security Groups and NACLs?
**Answer:**

| Feature | Security Group | NACL |
|---------|---------------|------|
| Level | Instance | Subnet |
| Stateful/Stateless | **Stateful** (return traffic auto-allowed) | **Stateless** (must allow return traffic explicitly) |
| Rules | Allow only | Allow AND Deny |
| Evaluation | All rules evaluated together | Rules evaluated in number order, first match wins |

---

### Q9. What is S3?
**Answer:** Simple Storage Service. Serverless object storage with unlimited capacity. Files (objects) up to 5 TB stored in buckets. 11 nines durability. Used for static hosting, backups, data lakes, logs, artifacts. Strong read-after-write consistency.

---

### Q10. What are S3 storage classes?
**Answer:** From hot to cold:
- **Standard** — Frequent access. Default.
- **Intelligent-Tiering** — Auto-moves between tiers. For unknown access patterns.
- **Standard-IA** — Infrequent access. Cheaper storage, retrieval fee.
- **One Zone-IA** — Same but single AZ. For re-creatable data.
- **Glacier Instant** — Archive with instant retrieval.
- **Glacier Flexible** — Archive. Minutes to hours retrieval.
- **Glacier Deep Archive** — Cheapest. 12-48 hour retrieval.

---

### Q11. What is EC2?
**Answer:** Elastic Compute Cloud. Virtual servers in the cloud. You pick the OS, instance type (CPU/RAM), storage, and network. Pay per second. Combined with AMIs (templates), Security Groups (firewall), Key Pairs (SSH access), and User Data (bootstrap scripts).

---

### Q12. What is an AMI?
**Answer:** Amazon Machine Image. A template to launch EC2 instances. Contains the OS, pre-installed software, and configurations. Used for golden images — bake a configured server into an AMI and launch identical copies. Region-specific — copy to use in other Regions.

---

### Q13. What is an Auto Scaling Group?
**Answer:** Automatically adjusts the number of EC2 instances based on demand. You set min, desired, and max count. It launches instances when load increases, terminates when load drops, and replaces unhealthy instances. Always pair with an ALB for traffic distribution.

---

### Q14. What is the difference between ALB and NLB?
**Answer:**
- **ALB** (Application LB) — Layer 7. HTTP/HTTPS. Path-based and host-based routing. For web apps and microservices.
- **NLB** (Network LB) — Layer 4. TCP/UDP. Ultra-low latency. Static IPs. For gaming, IoT, real-time apps.

Use ALB for 99% of web workloads. Use NLB when you need static IPs or non-HTTP protocols.

---

### Q15. What is CloudWatch?
**Answer:** AWS's monitoring service. Four pillars:
- **Metrics** — Numerical data (CPU, errors, latency).
- **Logs** — Centralized log storage and search.
- **Alarms** — Trigger actions when metrics breach thresholds.
- **Events/EventBridge** — React to state changes in AWS.

**Key gotcha:** EC2 memory and disk metrics are NOT available by default — requires the CloudWatch Agent.

---

## 🟡 INTERMEDIATE (Questions 16–35)

---

### Q16. What is the difference between RDS Multi-AZ and Read Replicas?
**Answer:**

| Feature | Multi-AZ | Read Replica |
|---------|---------|-------------|
| Purpose | High Availability (failover) | Read scaling |
| Replication | Synchronous | Asynchronous |
| Readable? | No (standby only) | Yes |
| Failover | Automatic (~60s) | Manual (promote) |
| Cross-Region | No | Yes |

Use **both together** in production: Multi-AZ for HA + Read Replicas for scaling reads.

---

### Q17. What is Aurora? How is it different from RDS?
**Answer:** Aurora is AWS's cloud-native relational database engine. MySQL/PostgreSQL compatible.
- **6 copies** of data across 3 AZs (vs RDS: 2 copies with Multi-AZ).
- **Failover in ~30 seconds** (vs RDS: 60-120 seconds).
- **Up to 15 Read Replicas** (vs RDS: 5).
- **Auto-scaling storage** up to 128 TB.
- **Aurora Serverless v2** scales to near-zero for dev/test.

---

### Q18. When would you use DynamoDB vs RDS?
**Answer:**
- **RDS/Aurora** — Need JOINs, complex queries, ACID transactions, relational data model.
- **DynamoDB** — Need single-digit ms latency at massive scale, flexible schema, key-value access patterns, serverless.

Rule: If you need JOINs → relational. If you need speed at scale with simple access patterns → DynamoDB.

---

### Q19. What is a DynamoDB partition key and why does it matter?
**Answer:** The partition key determines which physical partition stores your data. A good key (high cardinality like `userId`) spreads data evenly. A bad key (low cardinality like `status`) creates hot partitions → throttling → downtime. It's the single most important DynamoDB design decision.

---

### Q20. What is Lambda cold start? How do you mitigate it?
**Answer:** A cold start happens when Lambda creates a new execution environment — downloading code, starting runtime, running init code. Adds 100ms–10s latency.

**Mitigations:**
- Initialize SDK clients **outside** the handler (reused on warm starts).
- Keep packages small. Use Layers for big dependencies.
- Use **Provisioned Concurrency** for latency-sensitive functions.
- Use **SnapStart** for Java.
- Choose Python/Node.js for fastest cold starts.
- Avoid VPC unless you need private resource access.

---

### Q21. What is the difference between SQS and SNS?
**Answer:**
- **SQS** = Queue. Pull-based. One consumer per message. Messages persist until processed. For decoupling and buffering.
- **SNS** = Pub/Sub. Push-based. Many subscribers get the same message simultaneously. For fan-out and notifications.

**Best practice:** Use **SNS + SQS fan-out** together — SNS distributes to multiple SQS queues, each with its own consumer.

---

### Q22. What is EventBridge and when would you use it over SNS?
**Answer:** EventBridge is a serverless event bus with rich content-based filtering, schema registry, archive/replay, and 20+ targets. Use EventBridge when you need complex routing rules, AWS service event matching, or event replay. Use SNS for simple fan-out and notifications.

---

### Q23. What is Infrastructure as Code? Why does it matter?
**Answer:** Managing infrastructure through code files instead of manual console clicks. Benefits:
- **Repeatable** — Same template → identical environments.
- **Version controlled** — Every change tracked in Git.
- **Reviewable** — Pull requests for infra changes.
- **Fast** — Deploy in minutes, not hours.
- **DR** — Rebuild entire infrastructure from templates.

Tools: CloudFormation, CDK, Terraform, SAM.

---

### Q24. What is CloudFormation? What is a Change Set?
**Answer:** CloudFormation is AWS's native IaC service. You write YAML/JSON templates describing resources. CloudFormation creates them as a **stack**. A **Change Set** previews what will change before you apply an update — like a `terraform plan`. Always use Change Sets before production updates.

---

### Q25. What is CDK? How does it relate to CloudFormation?
**Answer:** CDK lets you write infrastructure in real programming languages (TypeScript, Python, Java). It compiles to CloudFormation templates under the hood. Benefits: loops, conditionals, type checking, unit testing, IDE autocomplete. L2/L3 constructs provide sensible defaults — 15 lines of CDK can replace 200 lines of YAML.

---

### Q26. Explain blue/green deployment.
**Answer:** Run two identical environments. **Blue** = current production. **Green** = new version. Deploy to Green, test it, then switch all traffic from Blue to Green at the load balancer. If Green has issues, switch back to Blue instantly. CodeDeploy supports this natively for ECS and EC2.

---

### Q27. Explain canary deployment. When would you use it over blue/green?
**Answer:** Send a small percentage (e.g., 10%) of traffic to the new version. Monitor for errors/latency. If healthy, gradually increase to 100%. If not, roll back — only 10% of users were affected.

Use canary over blue/green for **high-risk changes in large-scale systems** where you want to limit blast radius. Blue/green is all-or-nothing; canary is gradual.

---

### Q28. What is the difference between ECS and EKS?
**Answer:**
- **ECS** — AWS-native container orchestrator. Simpler. Deep AWS integration. No Kubernetes knowledge needed.
- **EKS** — Managed Kubernetes. Portable. Massive ecosystem (Helm, ArgoCD, Istio). Steeper learning curve.

**Choose ECS** for AWS-native teams wanting simplicity. **Choose EKS** when you need Kubernetes portability, ecosystem, or have existing K8s expertise.

---

### Q29. What is Fargate?
**Answer:** Serverless compute engine for containers on ECS and EKS. No EC2 instances to manage. You define CPU and memory per task. AWS handles everything else. Per-second billing. Each task gets its own ENI (private IP). Default choice for most container workloads.

---

### Q30. What is CloudTrail and why is it important?
**Answer:** CloudTrail logs **every API call** in your AWS account — who did what, when, from where. It's your audit trail for security and compliance. Must-have: Enable for all Regions, send to S3 in a centralized security account, enable log file integrity validation, alert on root account usage and IAM changes.

---

### Q31. What is X-Ray? When do you use it?
**Answer:** X-Ray is a distributed tracing service. It traces requests across multiple services (API GW → Lambda → DynamoDB → SQS → Lambda) and shows where time is spent and where errors occur. Use it to debug latency issues in microservices. The **Service Map** gives a visual overview. Add **annotations** for business context (orderId, userId).

---

### Q32. What is drift detection in CloudFormation?
**Answer:** Drift detection checks if actual AWS resources still match your CloudFormation template. If someone changed a Security Group in the console, that's drift. Dangerous because your template no longer represents reality. Run drift detection regularly. Fix by updating the template or reimporting the resource.

---

### Q33. How do you secure an RDS database?
**Answer:**
- Deploy in **private subnets** — never publicly accessible.
- Security Group allows port 3306/5432 **only from app-sg** — not `0.0.0.0/0`.
- Enable **encryption at rest** (KMS) — must be set at creation.
- Enable **encryption in transit** (SSL/TLS).
- Store credentials in **Secrets Manager** with auto-rotation.
- Enable **Multi-AZ** for HA.
- Enable **automated backups** with appropriate retention.
- Enable **deletion protection**.

---

### Q34. What is a NAT Gateway and why is it expensive?
**Answer:** NAT Gateway lets private subnet instances access the internet (outbound only) without being reachable from outside. It's expensive because you pay ~$0.045/hour + $0.045/GB processed. **Cost reduction:** Use VPC Gateway Endpoints for S3/DynamoDB (free), Interface Endpoints for other AWS services, and create one NAT Gateway per AZ for HA.

---

### Q35. What is the difference between CodeBuild and CodeDeploy?
**Answer:**
- **CodeBuild** = Build phase. Compiles code, runs tests, builds Docker images, produces artifacts. Defined by `buildspec.yml`.
- **CodeDeploy** = Deploy phase. Deploys artifacts to EC2, ECS, or Lambda. Supports rolling, blue/green, and canary. Defined by `appspec.yml`.

They work together inside **CodePipeline**: Source → CodeBuild → CodeDeploy.

---

## 🔴 ADVANCED / SCENARIO-BASED (Questions 36–50)

---

### Q36. Design a highly available web application on AWS.
**Answer:**
```
Route 53 (DNS) → CloudFront (CDN) → ALB (3 AZs)
  → ASG (min:2, max:6) across 3 AZs (private subnets)
  → ElastiCache Redis (Multi-AZ) for caching
  → Aurora MySQL (writer + 2 readers, Multi-AZ)
  → S3 for static assets
  → VPC with public/private subnets per AZ
  → NAT Gateway per AZ for outbound
  → SG chaining: ALB-SG → App-SG → DB-SG
  → CloudWatch alarms + Auto Scaling policies
```
Key points: Multi-AZ everything, no single point of failure, health checks at every tier, auto-scaling for variable load.

---

### Q37. Your application is slow. How do you troubleshoot?
**Answer:** Systematic approach, layer by layer:
1. **CloudWatch Metrics** — Check ALB response time, EC2 CPU, RDS connections, Lambda duration.
2. **X-Ray** — Trace a slow request. Find which service/call is the bottleneck.
3. **RDS Performance Insights** — Is it a slow database query? Missing index?
4. **CloudWatch Logs** — Search for errors, timeouts, OOM events.
5. **ElastiCache** — Check cache hit rate. Low hit rate = too many DB queries.
6. **ALB access logs** — Identify slow endpoints.
7. **Fix:** Add caching, optimize queries, right-size instances, add read replicas, or move heavy work to async (SQS + Lambda).

---

### Q38. Your EC2 instance can't reach the internet. How do you debug?
**Answer:** Check in this order:
1. **Route Table** — Does the subnet's route table have `0.0.0.0/0 → IGW` (public) or `→ NAT` (private)?
2. **Internet Gateway** — Is an IGW attached to the VPC?
3. **Public IP** — Does the instance have a public IP or Elastic IP? (needed for public subnet)
4. **Security Group** — Is outbound traffic allowed? (default: yes)
5. **NACL** — Is the subnet NACL blocking outbound or inbound return traffic (ephemeral ports)?
6. **NAT Gateway** — If private subnet, is the NAT Gateway running and in a public subnet with an Elastic IP?
7. **DNS** — Is DNS resolution enabled on the VPC?

---

### Q39. How would you migrate a monolith to microservices on AWS?
**Answer:**
1. **Strangler Fig pattern** — Don't rewrite everything. Extract one function at a time.
2. Extract the first service → Deploy as ECS/Fargate behind ALB with path-based routing.
3. Route `/api/orders/*` to new service, everything else to monolith.
4. Each service gets its own database (database per service pattern).
5. Use **SQS/SNS** for inter-service communication (async, decoupled).
6. Use **API Gateway** or **ALB path routing** as the front door.
7. Repeat until monolith is empty.
8. CI/CD pipeline per service. Independent deployments.

---

### Q40. A developer accidentally deleted a production S3 bucket's data. How do you recover?
**Answer:**
- **If versioning was enabled:** The delete only added delete markers. Go to S3 → Show versions → Remove delete markers. Data is restored.
- **If versioning was NOT enabled:** Check for S3 Cross-Region Replication to a backup bucket. Check for recent S3 backup snapshots (if you had a backup process).
- **If no backups exist:** Data is gone. This is why you always enable versioning + lifecycle rules.

**Prevention:** Enable versioning, MFA Delete on critical buckets, S3 Object Lock (WORM), and bucket policies that deny `s3:DeleteObject` for non-admin roles.

---

### Q41. How would you implement a CI/CD pipeline for ECS on AWS?
**Answer:**
```
GitHub (push to main)
  → CodePipeline (Source stage)
  → CodeBuild (Build stage):
      - Run unit tests
      - Build Docker image
      - Push to ECR
      - Output imagedefinitions.json
  → Manual Approval (for production)
  → CodeDeploy (Deploy stage):
      - Blue/Green deployment to ECS Fargate
      - CloudWatch Alarms as rollback triggers
      - 10-minute bake time before terminating old tasks
```
Buildspec.yml handles the build. Appspec.yml handles the deployment. CloudWatch alarms (5xx, latency) trigger automatic rollback.

---

### Q42. Your Lambda function is hitting concurrency limits. What do you do?
**Answer:**
1. **Immediate:** Request a concurrency limit increase via AWS Support (default: 1,000 per Region).
2. **Buffer:** Put an SQS queue in front of Lambda — messages wait instead of being throttled.
3. **Reserved concurrency:** Allocate specific concurrency per critical function so they're guaranteed capacity.
4. **Optimize:** Reduce function duration (faster execution = fewer concurrent executions).
5. **Architecture:** Move long-running work to Step Functions or Fargate.
6. **Monitor:** Alarm on `Throttles` metric → alert before it becomes critical.

---

### Q43. How do you handle secrets in a DevOps pipeline?
**Answer:** Never hardcode secrets anywhere.
- **Application secrets:** Store in **Secrets Manager** (with auto-rotation) or **SSM Parameter Store** (SecureString).
- **In ECS:** Reference secrets in task definition using `valueFrom` → Secrets Manager ARN. Injected at runtime.
- **In Lambda:** Fetch from SSM/Secrets Manager during init (outside handler). Cache for reuse.
- **In CodeBuild:** Use `buildspec.yml` `secrets-manager` section. Never put secrets in environment variables.
- **In Kubernetes:** Use **External Secrets Operator** or **Secrets Store CSI Driver** to sync from Secrets Manager.
- **Rotate** credentials automatically. Never commit secrets to Git.

---

### Q44. Your AWS bill spiked 40% this month. How do you investigate?
**Answer:**
1. **Cost Explorer** → Filter by service → Identify which service spiked.
2. **Cost Explorer** → Filter by tag → Identify which team/project is responsible.
3. **Common culprits:** NAT Gateway data processing, forgotten EC2 instances, EBS snapshots piling up, data transfer charges, undeleted test environments.
4. **Trusted Advisor** → Check cost optimization recommendations.
5. **Check for zombies:** Unattached EBS volumes, unused Elastic IPs, idle load balancers.
6. **Immediate fix:** Stop/terminate unused resources. Add lifecycle policies.
7. **Prevention:** Budgets with alerts, mandatory tagging, weekly cost reviews, scheduled dev/test shutdown.

---

### Q45. How do you implement zero-downtime deployments for a database schema change?
**Answer:** Use the **expand-and-contract** pattern:
1. **Expand:** Add new column (don't remove or rename old ones). Deploy migration.
2. **Deploy app v2:** Writes to both old and new columns. Reads from new column with fallback to old.
3. **Backfill:** Migrate data from old column to new column (background script).
4. **Verify:** All data in new column. App only uses new column.
5. **Contract:** Deploy app v3 that only uses new column. Drop old column.

Both Blue and Green environments can coexist at every step. Never break the old version.

---

### Q46. Design a multi-Region active-active architecture.
**Answer:**
```
Users → Route 53 (latency-based routing)
  ├── us-east-1: ALB → ASG → App
  │     └── DynamoDB Global Table (auto-replicated)
  └── eu-west-1: ALB → ASG → App
        └── DynamoDB Global Table (auto-replicated)

- DynamoDB Global Tables handle multi-Region data replication (async, <1s lag).
- Route 53 latency routing sends users to the nearest Region.
- Health checks failover if one Region goes down.
- S3 Cross-Region Replication for static assets.
- CloudFront in front for edge caching.
- Each Region is fully independent — can serve all traffic alone.
```
Key trade-off: Eventually consistent data between Regions. Design for conflict resolution.

---

### Q47. An EC2 instance was compromised. What are your incident response steps?
**Answer:**
1. **Isolate immediately:** Change the Security Group to deny all inbound/outbound traffic. Do NOT terminate — preserve evidence.
2. **Capture evidence:** Create an EBS snapshot. Capture memory dump if possible.
3. **Investigate with CloudTrail:** Who accessed the instance? What API calls were made? From which IP?
4. **Check GuardDuty:** Look for related findings (crypto mining, unusual API calls, data exfiltration).
5. **Review VPC Flow Logs:** What network connections were made to/from the instance?
6. **Rotate credentials:** Revoke any IAM keys, change passwords, rotate secrets associated with this instance.
7. **Remediate:** Patch the vulnerability. Harden the AMI. Update Security Groups.
8. **Post-mortem:** Document root cause, timeline, and prevention measures.

---

### Q48. How do you enforce governance across 20 AWS accounts?
**Answer:**
- **AWS Organizations** — Centralized multi-account management.
- **SCPs (Service Control Policies)** — Guardrails. Block dangerous actions across all accounts (e.g., deny leaving the organization, deny disabling CloudTrail).
- **CloudFormation Stack Sets** — Deploy baselines (CloudTrail, Config, GuardDuty) across all accounts.
- **AWS Config** — Continuous compliance checks (encryption enabled, public access blocked).
- **IAM Identity Center (SSO)** — Centralized login with role-based access per account.
- **Centralized logging account** — CloudTrail, Config, and VPC Flow Logs aggregated in one S3 bucket.
- **Budgets** — Per-account spending limits with alerts.
- **Tagging policies** — Enforce mandatory tags (team, project, environment).

---

### Q49. Your application experiences periodic traffic spikes (10x normal). How do you architect for it?
**Answer:**
1. **Auto Scaling Group** — Target tracking policy on ALBRequestCountPerTarget. Scales EC2 with demand.
2. **Predictive Scaling** — ML-based. Pre-scales before known peak times.
3. **SQS buffer** — Queue burst requests. Lambda/workers process at a controlled rate. Protects the database.
4. **ElastiCache** — Cache hot data. Absorb read spikes without hitting DB.
5. **Aurora Auto Scaling** — Add read replicas automatically during peak.
6. **CloudFront** — Cache static content at edge. Reduce origin load.
7. **DynamoDB On-Demand** — Auto-scales reads/writes with zero capacity planning.
8. **Fargate Spot** — Burst containers for cost-effective spike handling.
9. **Pre-warm:** If spikes are predictable, schedule desired capacity increases via ASG scheduled actions.

---

### Q50. Walk me through the 6 pillars of the Well-Architected Framework and how you apply them.
**Answer:**

**1. Operational Excellence** — "How do we run systems and improve?"
- IaC (CDK/CloudFormation). CI/CD (CodePipeline). Runbooks for incidents. Blameless post-mortems. Small, frequent, reversible changes.

**2. Security** — "How do we protect data and systems?"
- IAM least privilege. MFA everywhere. Encryption at rest (KMS) and in transit (TLS). CloudTrail for audit. GuardDuty for threat detection. SG chaining. WAF on ALB.

**3. Reliability** — "How do we recover from failure?"
- Multi-AZ. Auto Scaling. Health checks. RDS Multi-AZ. Backups and tested DR. SQS for buffering. Chaos engineering.

**4. Performance Efficiency** — "How do we use resources efficiently?"
- Right instance types. CloudFront caching. ElastiCache. Read replicas. Async processing. X-Ray for bottleneck detection. Graviton for price-performance.

**5. Cost Optimization** — "How do we eliminate waste?"
- Right-sizing (Compute Optimizer). Savings Plans for baseline. Spot for burst/CI-CD. S3 lifecycle policies. VPC endpoints to avoid NAT costs. Tagging. Weekly cost reviews.

**6. Sustainability** — "How do we minimize environmental impact?"
- Managed services (higher utilization). Graviton (energy-efficient). Serverless (runs only when needed). Data lifecycle management. Right-size to avoid waste.

---

## 💡 Interview Tips

```
Before answering any question:
1. Pause for 2 seconds. Don't rush.
2. Start with WHAT (1 sentence definition).
3. Then WHY (why it matters).
4. Then HOW (how you'd implement it).
5. End with a real example or interview quote.

For scenario questions:
1. Clarify requirements (ask about scale, budget, SLA).
2. State your approach (high-level first).
3. Walk through the architecture (name specific services).
4. Mention trade-offs (cost vs complexity, speed vs consistency).
5. End with monitoring and improvement ("and we'd monitor with...").

Power phrases:
- "In our production environment, we..."
- "We follow the principle of least privilege..."
- "We use Infrastructure as Code for all changes..."
- "We monitor with CloudWatch and trace with X-Ray..."
- "We test failover quarterly..."
- "We review costs weekly in Cost Explorer..."
```

---

*50 questions. Structured answers. Basic to Advanced. You're ready.*
*Go ace that interview!*
