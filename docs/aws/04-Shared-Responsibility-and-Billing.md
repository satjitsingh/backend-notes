# Shared Responsibility Model & Billing

---

## 1. Shared Responsibility Model

### What is it?
AWS and you split the security work. AWS secures the cloud itself. You secure what you put IN the cloud.
This is a legal and operational agreement. Know who owns what.
If your data leaks because of a bad S3 policy, that's on YOU, not AWS.

### Why it matters in production
- Auditors and compliance teams will ask: "Who is responsible for X?"
- If you misconfigure security groups, it's your fault — not AWS.
- This model defines your security obligations as a DevOps engineer.

### Core Ideas

**AWS is responsible for ("Security OF the cloud"):**
- Physical data centers (guards, biometrics, fences)
- Hardware and infrastructure
- Hypervisor and host OS
- Network infrastructure
- Managed service internals (RDS engine patching, S3 durability)

**You are responsible for ("Security IN the cloud"):**
- IAM users, roles, policies
- Security group and NACL rules
- OS patching on EC2 (your VMs)
- Application code security
- Data encryption (at rest and in transit)
- S3 bucket policies and access settings
- Network configuration (VPC, subnets)

**How responsibility shifts by service type:**

| Service Type | AWS Manages | You Manage |
|-------------|-------------|------------|
| **IaaS (EC2)** | Hardware, hypervisor | OS, apps, firewall, data |
| **PaaS (RDS)** | Hardware, OS, engine patching | Data, access, backups config |
| **SaaS (S3)** | Almost everything | Access policies, encryption settings |

### Quick Analogy
Shared Responsibility = Renting an apartment.
Landlord (AWS) maintains the building structure, plumbing, and electricity.
You (tenant) are responsible for locking your door, not leaving the stove on, and keeping your stuff safe.

### Architecture View
```
┌──────────────────────────────────────────────┐
│                YOUR RESPONSIBILITY           │
│  ┌─────────────────────────────────────────┐ │
│  │ Data, Encryption, IAM, App security     │ │
│  │ OS patching (EC2), Firewall rules       │ │
│  │ Network config, S3 bucket policies      │ │
│  └─────────────────────────────────────────┘ │
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│               AWS RESPONSIBILITY             │
│  ┌─────────────────────────────────────────┐ │
│  │ Physical security, Hardware, Hypervisor │ │
│  │ Network infra, Managed service engines  │ │
│  │ Global infrastructure, Edge locations   │ │
│  └─────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Launch an EC2 instance → You must patch the OS yourself. (Your responsibility)
2. Create an RDS database → AWS patches the engine. (AWS responsibility)
3. Create an S3 bucket → Check if "Block Public Access" is on. (Your responsibility)
4. Look at Security Groups → You define inbound/outbound rules. (Your responsibility)
5. Go to **AWS Artifact** → Download AWS compliance reports (SOC, ISO). (AWS's responsibility to maintain these certifications)

### Common Mistakes
- ❌ Thinking AWS will patch your EC2 OS — they won't (unless you use SSM Patch Manager).
- ❌ Assuming S3 data is private by default — it is now, but always verify.
- ❌ Forgetting encryption is YOUR job — enable it explicitly.
- ❌ Blaming AWS for a security breach caused by a misconfigured security group.

### Pro Tips
- ✅ Memorize this: "AWS = security OF the cloud. Customer = security IN the cloud."
- ✅ For managed services (RDS, Lambda, S3), your responsibility is LESS — but never zero.
- ✅ Use **AWS Config** to continuously check if your configs are compliant.
- ✅ In interviews: "I follow the shared responsibility model. AWS manages infrastructure security. I manage access control, encryption, patching, and application security. For EC2, I use SSM Patch Manager for automated OS patching."

---

## 2. AWS Pricing Basics

### What is it?
AWS charges based on what you use. No upfront commitment (unless you choose one).
There are three main pricing models: On-Demand, Reserved, and Spot.
Different services have different pricing units (per hour, per GB, per request).

### Why it matters in production
- Cloud bills can spiral out of control without understanding pricing.
- Choosing the right pricing model can save 30–72% on compute costs.
- DevOps engineers are expected to optimize costs, not just deploy.

### Core Ideas

**Three pricing principles:**
1. **Pay as you go** — Pay for what you consume, per second/hour.
2. **Pay less when you reserve** — Commit for 1–3 years, save up to 72%.
3. **Pay less when you use more** — Volume discounts (e.g., S3 storage tiers).

**Compute pricing models:**

| Model | Discount | Commitment | Best For |
|-------|---------|-----------|----------|
| **On-Demand** | 0% (full price) | None | Short-term, unpredictable workloads |
| **Reserved Instances** | Up to 72% | 1 or 3 years | Steady, predictable workloads |
| **Savings Plans** | Up to 72% | 1 or 3 years | Flexible (any instance type/Region) |
| **Spot Instances** | Up to 90% | None (can be interrupted) | Fault-tolerant, batch jobs |
| **Dedicated Hosts** | Varies | On-demand or reserved | Compliance, licensing |

**What costs money (common sources):**
- **EC2** — Per-second billing (Linux), per-hour (Windows).
- **EBS** — Per GB provisioned per month + IOPS.
- **S3** — Per GB stored + per request (GET/PUT) + data transfer out.
- **Data Transfer** — Inbound is free. Outbound to internet costs money.
- **RDS** — Instance hours + storage + data transfer.
- **Lambda** — Per request + per GB-second of compute.
- **NAT Gateway** — Per hour + per GB processed (often surprisingly expensive).

### Quick Analogy
On-Demand = Paying full price for a movie ticket.
Reserved = Buying an annual movie pass — cheaper per movie.
Spot = Buying last-minute discounted tickets — cheapest, but might be cancelled.

### Architecture View
```
Typical AWS Bill Breakdown:
┌────────────────────────────────┐
│ EC2 Instances        40%  ████ │
│ Data Transfer        20%  ██   │
│ RDS Databases        15%  ██   │
│ S3 Storage           10%  █    │
│ NAT Gateway           8%  █    │
│ Other                 7%  █    │
└────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **AWS Pricing Calculator** → [calculator.aws](https://calculator.aws).
2. Add an EC2 `t3.medium` in `us-east-1` → See On-Demand monthly cost.
3. Compare with 1-year Reserved Instance → See the savings.
4. Go to **Billing Dashboard** → Review your current charges by service.
5. Go to **Cost Explorer** → Filter by service, Region, tag.
6. Create a **Budget** → Set a $10/month alert.

### Common Mistakes
- ❌ Leaving EC2 instances running 24/7 when only needed during business hours.
- ❌ Not using Reserved Instances for predictable workloads — wasting 40–72%.
- ❌ Ignoring data transfer costs — they add up fast with cross-Region or internet traffic.
- ❌ Forgetting NAT Gateway charges — a common bill shock.
- ❌ Not monitoring costs until the bill arrives.

### Pro Tips
- ✅ Use **Spot Instances** for batch processing, CI/CD runners, testing.
- ✅ Use **Savings Plans** instead of Reserved Instances for more flexibility.
- ✅ Set **Billing Alarms** on Day 1. Use CloudWatch or AWS Budgets.
- ✅ Use **Cost Allocation Tags** — tag everything by team, project, environment.
- ✅ In interviews: "We use a mix of Savings Plans for baseline and Spot for burst. Budgets and Cost Explorer keep us within targets. We tag everything for chargeback."

---

## 3. Cost Optimization Basics

### What is it?
Cost optimization means spending the least money while meeting performance needs.
It's one of the 6 pillars of the AWS Well-Architected Framework.
It's not about cutting costs blindly — it's about eliminating waste.

### Why it matters in production
- Cloud waste averages 30% in most organizations.
- DevOps engineers who optimize costs are highly valued.
- Cost optimization is a recurring interview topic.

### Core Ideas

**The cost optimization checklist:**

1. **Right-sizing** — Use the smallest instance that meets your needs.
   - Use CloudWatch metrics to check CPU/memory usage.
   - `t3.medium` at 10% CPU? Switch to `t3.small`.

2. **Purchasing options** — Match workload to pricing model.
   - Steady baseline → Savings Plans or Reserved.
   - Burst/fault-tolerant → Spot Instances.
   - Dev/test → Spot or schedule start/stop.

3. **Storage optimization**
   - Use S3 Lifecycle Policies → Move old data to cheaper tiers.
   - S3 Standard → S3 IA → S3 Glacier → Glacier Deep Archive.
   - Delete unattached EBS volumes.
   - Use gp3 instead of gp2 (cheaper, better performance).

4. **Kill zombie resources**
   - Unattached EBS volumes, unused Elastic IPs, idle load balancers.
   - Old snapshots, unused NAT Gateways.
   - Use **AWS Trusted Advisor** to find these.

5. **Architect for cost**
   - Use serverless (Lambda, Fargate) for variable workloads.
   - Use caching (ElastiCache, CloudFront) to reduce backend load.
   - Use auto scaling to match capacity to demand.

6. **Monitor and govern**
   - AWS Budgets — Set spending limits with alerts.
   - Cost Explorer — Analyze spend trends.
   - Cost Anomaly Detection — AI-based alerts for unusual spending.
   - Tagging — Track costs by team/project/environment.

### Quick Analogy
Cost optimization = Managing a household budget.
- Turn off lights in empty rooms (stop idle instances).
- Buy in bulk when you know you'll use it (Reserved/Savings Plans).
- Cancel subscriptions you don't use (delete zombie resources).
- Check your bank statement regularly (Cost Explorer).

### Architecture View
```
Cost-Optimized Architecture:
┌───────────────────────────────────────────────┐
│ CloudFront (cache) ──▶ reduces origin hits    │
│         │                                     │
│ ALB ──▶ Auto Scaling Group                    │
│         ├── 2x Reserved (baseline)            │
│         └── 0-4x Spot (burst traffic)         │
│                   │                           │
│         RDS (Reserved, Multi-AZ)              │
│                   │                           │
│         S3 (Lifecycle: Standard → IA → Glacier)│
│                                               │
│ Lambda (serverless — pay per invocation)      │
│                                               │
│ Monitoring: Budgets + Cost Explorer + Tags    │
└───────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **Cost Explorer** → Identify your top 3 spending services.
2. Go to **Trusted Advisor** → Check "Cost Optimization" recommendations.
3. Look for: idle EC2, unattached EBS, low-utilization RDS.
4. Go to **EC2 → Volumes** → Delete any unattached volumes.
5. Go to **S3** → Add a lifecycle rule: Move to IA after 30 days, Glacier after 90.
6. Go to **AWS Budgets** → Create a monthly budget with email alerts.
7. Check **Compute Optimizer** → See right-sizing recommendations.

### Common Mistakes
- ❌ Over-provisioning "just in case" — use auto scaling instead.
- ❌ Not using lifecycle policies on S3 — paying Standard prices for old data.
- ❌ Running dev/test environments 24/7 — schedule them to stop at night/weekends.
- ❌ Ignoring data transfer costs between Regions and to the internet.
- ❌ Not tagging resources — impossible to track who's spending what.
- ❌ Buying Reserved Instances for the wrong instance type/size.

### Pro Tips
- ✅ Use **AWS Compute Optimizer** — ML-based right-sizing suggestions.
- ✅ Use **S3 Intelligent-Tiering** for unknown access patterns — auto moves data.
- ✅ Schedule dev/test EC2 to **stop** at 7 PM and **start** at 9 AM (use Lambda + EventBridge).
- ✅ Use **AWS Organizations** consolidated billing for volume discounts.
- ✅ Review costs **weekly**, not monthly.
- ✅ In interviews: "We optimize costs through right-sizing with Compute Optimizer, Savings Plans for baseline, Spot for burst, S3 lifecycle policies, and weekly cost reviews in Cost Explorer. Everything is tagged for accountability."
