# AWS Cheat Sheet — All Major Services

> Print this. Read it on the bus. Skim it 10 minutes before your interview.

---

## Compute

| Service | One-Line Definition |
|---------|-------------------|
| **EC2** | Virtual servers you rent by the second. You choose OS, CPU, RAM, storage. |
| **Lambda** | Run code without servers. Pay per invocation. Max 15 min. Event-driven. |
| **Fargate** | Serverless containers. You define CPU/memory per task. AWS manages the rest. |
| **ECS** | AWS-native container orchestrator. Runs Docker containers on EC2 or Fargate. |
| **EKS** | Managed Kubernetes. AWS runs the control plane. You run worker nodes or Fargate. |
| **Elastic Beanstalk** | Upload code, AWS provisions everything (EC2, ALB, ASG). PaaS for web apps. |
| **App Runner** | Simplest way to deploy containers or code. No infra config at all. |
| **Lightsail** | Simple VPS for small projects. Fixed monthly pricing. Like a mini-EC2. |
| **Batch** | Run large-scale batch jobs. AWS manages compute. Good for HPC and data processing. |
| **Outposts** | AWS hardware in your own data center. Run AWS services on-premises. |

---

## Storage

| Service | One-Line Definition |
|---------|-------------------|
| **S3** | Infinite object storage. Files up to 5 TB. 11 nines durability. Serverless. |
| **S3 Glacier** | Archive storage. Cheap. Retrieval takes minutes to hours. For compliance/backup. |
| **EBS** | Block storage attached to one EC2. Persists after stop. Like a hard drive. |
| **EFS** | Shared file storage (NFS). Multiple EC2s can mount it. Auto-scales. |
| **Instance Store** | Temporary storage on the physical host. Fastest. Lost on stop/terminate. |
| **FSx** | Managed file systems: Windows (SMB), Lustre (HPC), NetApp, OpenZFS. |
| **Storage Gateway** | Bridge between on-prem storage and S3/Glacier. Hybrid cloud storage. |
| **Snow Family** | Physical devices (Snowcone, Snowball, Snowmobile) for offline data transfer to AWS. |

---

## Database

| Service | One-Line Definition |
|---------|-------------------|
| **RDS** | Managed relational database. MySQL, PostgreSQL, MariaDB, Oracle, SQL Server. |
| **Aurora** | Cloud-native relational DB. 5x MySQL speed. 6 copies across 3 AZs. 30s failover. |
| **DynamoDB** | Serverless NoSQL key-value DB. Single-digit ms latency at any scale. |
| **ElastiCache** | Managed in-memory cache. Redis or Memcached. Microsecond reads. |
| **Redshift** | Data warehouse for analytics (OLAP). Petabyte-scale SQL queries. |
| **DocumentDB** | Managed MongoDB-compatible document database. |
| **Neptune** | Graph database. For social networks, fraud detection, recommendations. |
| **Keyspaces** | Managed Apache Cassandra-compatible wide-column database. |
| **Timestream** | Time-series database. For IoT, DevOps metrics, sensor data. |
| **QLDB** | Immutable ledger database. Cryptographically verifiable transaction log. |
| **MemoryDB** | Redis-compatible database with full durability. Ultra-fast + persistent. |

---

## Networking

| Service | One-Line Definition |
|---------|-------------------|
| **VPC** | Your private, isolated network in AWS. You define IP ranges, subnets, routing. |
| **Subnet** | A range of IPs within a VPC. Lives in one AZ. Public or private based on route table. |
| **Internet Gateway** | The door between your VPC and the internet. Free. One per VPC. |
| **NAT Gateway** | Lets private subnet instances reach the internet (outbound only). Costs money. |
| **Route Table** | Rules that decide where network traffic goes. Makes subnets public or private. |
| **Security Group** | Stateful firewall at the instance level. Allow rules only. |
| **NACL** | Stateless firewall at the subnet level. Allow and Deny rules. Evaluated in order. |
| **ALB** | Layer 7 load balancer. HTTP/HTTPS. Path/host routing. For web apps. |
| **NLB** | Layer 4 load balancer. TCP/UDP. Ultra-fast. Static IP. For non-HTTP workloads. |
| **Route 53** | Managed DNS. Domain registration + routing policies + health checks. |
| **CloudFront** | CDN. Caches content at 400+ edge locations worldwide. Reduces latency. |
| **VPC Peering** | Private connection between two VPCs. No transitive routing. |
| **Transit Gateway** | Central hub connecting multiple VPCs, VPNs, and Direct Connect. |
| **Direct Connect** | Dedicated physical fiber from your data center to AWS. Low latency, high bandwidth. |
| **Site-to-Site VPN** | Encrypted IPSec tunnel from on-prem to AWS over the public internet. |
| **VPC Endpoint** | Private connection from VPC to AWS services without going through the internet. |
| **Global Accelerator** | Routes traffic through AWS global network for faster, more reliable connections. |
| **PrivateLink** | Expose your service privately to other VPCs. No internet, no peering. |

---

## Security & Identity

| Service | One-Line Definition |
|---------|-------------------|
| **IAM** | Control who can do what in your AWS account. Users, groups, roles, policies. |
| **IAM Roles** | Temporary permissions assumed by services or users. No permanent credentials. |
| **STS** | Security Token Service. Issues temporary credentials for AssumeRole. |
| **Organizations** | Manage multiple AWS accounts centrally. SCPs for guardrails. Consolidated billing. |
| **SSO / IAM Identity Center** | Single sign-on for multiple AWS accounts. Centralized login. |
| **Cognito** | User authentication for apps. Sign-up, sign-in, social login, MFA. |
| **KMS** | Managed encryption keys. Encrypt EBS, S3, RDS, and more. |
| **Secrets Manager** | Store and auto-rotate passwords, API keys, database credentials. |
| **ACM** | Free SSL/TLS certificates. Auto-renew. For ALB, CloudFront, API Gateway. |
| **WAF** | Web Application Firewall. Block SQL injection, XSS, bad bots. Sits on ALB/CF. |
| **Shield** | DDoS protection. Standard is free. Advanced has 24/7 response team. |
| **GuardDuty** | Threat detection. Analyzes CloudTrail, VPC Flow Logs, DNS for suspicious activity. |
| **Inspector** | Vulnerability scanning for EC2 and container images (ECR). |
| **Macie** | Discovers and protects sensitive data (PII) in S3 using ML. |
| **Config** | Tracks resource configuration changes. Evaluates compliance rules continuously. |
| **CloudTrail** | Logs every API call in your account. Who did what, when, from where. |
| **Security Hub** | Central dashboard aggregating findings from GuardDuty, Inspector, Macie, Config. |

---

## Serverless & Application Integration

| Service | One-Line Definition |
|---------|-------------------|
| **Lambda** | Run code without servers. Triggered by events. Pay per execution. |
| **API Gateway** | Create, publish, and manage REST/HTTP/WebSocket APIs. Fronts Lambda or HTTP. |
| **SQS** | Managed message queue. Decouples producers and consumers. Pull-based. |
| **SNS** | Pub/sub messaging. One message pushed to many subscribers simultaneously. |
| **EventBridge** | Serverless event bus. Routes events between AWS services and custom apps. |
| **Step Functions** | Visual workflow orchestration. Chain Lambda, SDK calls, with retries and branching. |
| **AppSync** | Managed GraphQL API. Real-time data sync. Connects to DynamoDB, Lambda, HTTP. |
| **SES** | Simple Email Service. Send transactional and marketing emails at scale. |

---

## Containers

| Service | One-Line Definition |
|---------|-------------------|
| **ECS** | AWS-native container orchestrator. Deep integration with ALB, IAM, CloudWatch. |
| **EKS** | Managed Kubernetes. AWS manages the control plane. You manage workloads. |
| **Fargate** | Serverless compute for containers. No EC2 to manage. Per-task billing. |
| **ECR** | Container image registry. Store, scan, and manage Docker images. |
| **App Mesh** | Service mesh for containers. Traffic management, observability between services. |
| **Copilot** | CLI tool for deploying containerized apps on ECS. Simplifies setup. |

---

## DevOps & CI/CD

| Service | One-Line Definition |
|---------|-------------------|
| **CodePipeline** | Managed CI/CD pipeline. Automates source → build → test → deploy. |
| **CodeBuild** | Managed build service. Compile code, run tests, produce artifacts. No servers. |
| **CodeDeploy** | Automates deployments to EC2, ECS, Lambda. Supports blue/green and canary. |
| **CodeCommit** | Managed Git repository. AWS-hosted. Being deprecated — use GitHub. |
| **CloudFormation** | IaC. Define AWS resources in YAML/JSON. Stacks, Change Sets, drift detection. |
| **CDK** | Define infrastructure in real programming languages. Compiles to CloudFormation. |
| **SAM** | Serverless Application Model. Simplified CloudFormation for Lambda apps. |
| **Systems Manager** | Manage EC2 fleet. Patch, run commands, Session Manager (SSH replacement), Parameter Store. |
| **OpsWorks** | Managed Chef/Puppet for configuration management. Legacy — use SSM or Ansible. |

---

## Monitoring & Observability

| Service | One-Line Definition |
|---------|-------------------|
| **CloudWatch Metrics** | Time-series data from AWS services. CPU, network, custom metrics. |
| **CloudWatch Logs** | Centralized log storage and search. Logs Insights for SQL-like queries. |
| **CloudWatch Alarms** | Trigger actions (SNS, scaling, recovery) when metrics cross thresholds. |
| **CloudWatch Dashboards** | Custom visualizations of metrics and logs in one view. |
| **CloudWatch Synthetics** | Canary scripts that proactively test your endpoints and APIs. |
| **X-Ray** | Distributed tracing. See request flow across services. Find latency bottlenecks. |
| **CloudTrail** | API audit log. Every action in your account is recorded. Security and compliance. |

---

## Architecture & Well-Architected

| Concept | One-Line Definition |
|---------|-------------------|
| **High Availability** | System stays running even when parts fail. Multi-AZ, ALB, ASG. |
| **Fault Tolerance** | System has zero user impact during failures. N+1, circuit breakers, bulkheads. |
| **Multi-AZ** | Deploy across AZs in one Region. Standard for all production. |
| **Multi-Region** | Deploy across Regions. For global users, compliance, or ultra-critical DR. |
| **RPO** | Recovery Point Objective. How much data you can afford to lose. |
| **RTO** | Recovery Time Objective. How long you can be down. |
| **Well-Architected (6 pillars)** | Ops Excellence, Security, Reliability, Performance, Cost, Sustainability. |
| **Shared Responsibility** | AWS secures the cloud. You secure what's IN the cloud. |

---

## Cost & Billing

| Concept | One-Line Definition |
|---------|-------------------|
| **On-Demand** | Pay full price, no commitment. For unpredictable workloads. |
| **Reserved Instances** | 1-3 year commitment. Up to 72% savings. For steady workloads. |
| **Savings Plans** | Flexible 1-3 year commitment. Covers EC2, Fargate, Lambda. Up to 72%. |
| **Spot Instances** | Unused EC2 capacity. Up to 90% cheaper. Can be interrupted. |
| **Cost Explorer** | Analyze and visualize your AWS spending by service, Region, tag. |
| **Budgets** | Set spending limits with alerts. Get notified before you overspend. |
| **Trusted Advisor** | Checks for cost waste, security gaps, performance issues, and service limits. |
| **Compute Optimizer** | ML-based right-sizing recommendations for EC2, EBS, Lambda. |

---

## Deployment Strategies

| Strategy | One-Line Definition |
|----------|-------------------|
| **Rolling** | Deploy to instances one at a time. Slow but safe. |
| **Blue/Green** | Run two environments. Switch traffic from old (Blue) to new (Green). Instant rollback. |
| **Canary** | Send small % of traffic to new version. Monitor. If healthy, roll out fully. |
| **All-at-once** | Deploy everywhere simultaneously. Fast but risky. |
| **Immutable** | Deploy to brand new instances. Replace old ones. No in-place changes. |
| **Linear** | Gradually shift traffic in equal increments (e.g., 10% every 3 min). |

---

## Quick Memory Aids

```
IAM:       "Who can do what on which resource"
VPC:       "Your private network. Subnets + Route Tables + Gateways"
SG vs NACL: "SG = stateful + instance. NACL = stateless + subnet"
ALB vs NLB: "ALB = HTTP smart routing. NLB = TCP fast routing"
S3 classes: "Standard → IA → Glacier → Deep Archive (hot → cold → frozen)"
RDS vs Aurora: "RDS = managed. Aurora = cloud-native, 5x faster, 6 copies"
DynamoDB:  "Key-value NoSQL. Serverless. Partition key = most important decision"
ECS vs EKS: "ECS = AWS simple. EKS = Kubernetes portable"
Lambda:    "Event → Code → Done. No servers. Max 15 min"
SQS vs SNS: "SQS = queue (pull, 1 consumer). SNS = pub/sub (push, many)"
CloudWatch: "Metrics + Logs + Alarms = See everything, alert on anything"
CloudTrail: "Who did what, when, from where. Audit everything."
X-Ray:     "Trace requests across services. Find the slow one."
CF/CDK:    "CF = YAML infra. CDK = real code that generates CF"
CodePipeline: "Source → Build → Test → Approve → Deploy. Automated."
Blue/Green: "Two envs. Switch traffic. Instant rollback."
Canary:    "10% traffic to new version. Watch. Then 100%."
Well-Arch: "6 pillars: Ops, Security, Reliability, Perf, Cost, Sustain"
```

---

*Generated for fast revision. One line per service. All 80+ services covered.*
*Good luck with your interview!*
