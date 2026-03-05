# Cloud Basics

---

## 1. What is Cloud Computing

### What is it?
Cloud computing means using someone else's computers over the internet.
You don't buy servers. You rent them. You pay only for what you use.
AWS, Azure, and GCP are the big cloud providers. AWS is the largest.

### Why it matters in production
- No upfront hardware cost — start fast, scale fast.
- You don't manage physical servers — AWS does.
- If traffic spikes, you can add resources in minutes, not months.
- You only pay when resources are running — saves money.

### Core Ideas
- **On-demand** — Get resources whenever you need them.
- **Pay-as-you-go** — No long-term contracts needed. Pay per hour/second.
- **Elasticity** — Scale up when busy, scale down when idle.
- **Global reach** — Deploy in any region around the world.
- **Managed services** — AWS handles patching, backups, hardware failures.
- **5 characteristics (NIST):** On-demand self-service, broad network access, resource pooling, rapid elasticity, measured service.

### Quick Analogy
Cloud = Electricity from the grid.
You don't build your own power plant. You plug in and pay the bill.
More usage = higher bill. No usage = low bill.

### Architecture View
```
You (Developer)
   │
   ▼
Internet ──▶ AWS Cloud ──▶ Servers, Storage, Databases
                              (all managed by AWS)
```
- You write code and deploy.
- AWS handles hardware, networking, cooling, security of data centers.

### Hands-On (Step-by-step Lab)
1. Go to [aws.amazon.com](https://aws.amazon.com) → Create a **Free Tier** account.
2. Log into the **AWS Management Console**.
3. Notice the **Region selector** (top-right). Pick a region close to you.
4. Search for **EC2** → Launch a free-tier `t2.micro` instance.
5. Observe: You just rented a server in seconds. No hardware purchased.
6. **Terminate** the instance when done to avoid charges.

### Common Mistakes
- ❌ Forgetting to terminate resources → surprise bills.
- ❌ Thinking cloud is always cheaper — it depends on usage patterns.
- ❌ Leaving default region — may deploy in expensive or far-away regions.
- ❌ Not enabling billing alerts on a new account.

### Pro Tips
- ✅ Always enable **billing alerts** and set a **budget** on Day 1.
- ✅ Use **tags** on every resource (team, project, env) for cost tracking.
- ✅ Start with Free Tier eligible services when learning.
- ✅ In interviews, mention: "Cloud gives agility, elasticity, and cost optimization."

---

## 2. IaaS vs PaaS vs SaaS

### What is it?
These are three models of cloud services. They differ in **what you manage** vs **what the provider manages**.
IaaS = You manage the most. SaaS = You manage the least. PaaS = In between.

### Why it matters in production
- Choosing the right model saves time and money.
- DevOps teams mostly work with **IaaS** (EC2, VPC) and **PaaS** (Elastic Beanstalk, RDS).
- Knowing this helps you pick the right AWS service for the job.

### Core Ideas

| Model | You Manage | Provider Manages | AWS Example |
|-------|-----------|-----------------|-------------|
| **IaaS** | OS, apps, data, runtime | Hardware, networking, virtualization | EC2, VPC, EBS |
| **PaaS** | Apps and data only | OS, runtime, hardware, scaling | Elastic Beanstalk, RDS, Fargate |
| **SaaS** | Nothing (just use it) | Everything | Gmail, Dropbox, Salesforce |

- **IaaS** — You get a raw virtual machine. Install whatever you want.
- **PaaS** — You upload your code. Platform handles the rest.
- **SaaS** — Ready-made software. Just log in and use it.

### Quick Analogy
- **IaaS** = Renting an empty apartment. You bring your own furniture.
- **PaaS** = Renting a furnished apartment. Just move in.
- **SaaS** = Staying at a hotel. Everything is done for you.

### Architecture View
```
┌──────────────────────────────────────────────┐
│              YOU MANAGE ▲                     │
│                         │                     │
│  IaaS:  [App][Data][Runtime][OS]              │
│  PaaS:  [App][Data]                           │
│  SaaS:  (nothing)                             │
│                         │                     │
│          AWS MANAGES ▼                        │
│  IaaS:  [Virtualization][Hardware][Network]   │
│  PaaS:  [Runtime][OS][Infra]                  │
│  SaaS:  [Everything]                          │
└──────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **IaaS example:** Launch an EC2 instance → SSH in → Install Apache manually.
2. **PaaS example:** Go to Elastic Beanstalk → Upload a sample app → It auto-provisions EC2, Load Balancer, etc.
3. Compare: With EC2 you did everything. With Beanstalk, you just gave it code.
4. **SaaS example:** Open AWS WorkMail — it's a ready-to-use email service.

### Common Mistakes
- ❌ Using IaaS (EC2) when PaaS (Beanstalk/Fargate) would save time.
- ❌ Confusing PaaS with serverless — they overlap but are not the same.
- ❌ Not understanding the shared responsibility difference between models.

### Pro Tips
- ✅ In interviews, always say: "I choose the service model based on how much control vs convenience the team needs."
- ✅ DevOps trend is moving toward PaaS/serverless to reduce ops burden.
- ✅ Know this mapping cold: EC2 = IaaS, Beanstalk = PaaS, S3 = IaaS (storage).
- ✅ RDS is PaaS for databases — AWS handles patching, backups, failover.

---

## 3. Why Companies Moved to Cloud

### What is it?
Before cloud, companies bought and maintained their own servers (on-premises).
Cloud lets them skip hardware and focus on building products.
The shift happened because cloud is faster, cheaper at scale, and more flexible.

### Why it matters in production
- Understanding "why cloud" helps you make smart architecture decisions.
- It's a common interview opener: "Why did your company move to cloud?"
- DevOps exists largely because of cloud — automation, CI/CD, infra-as-code.

### Core Ideas
- **CapEx → OpEx** — No big upfront hardware purchase. Pay monthly instead.
- **Speed** — Provision servers in minutes, not weeks.
- **Scale** — Handle traffic spikes automatically.
- **Global** — Deploy in 30+ regions worldwide instantly.
- **Innovation** — Use AI/ML, analytics, IoT services without building from scratch.
- **Disaster Recovery** — Multi-region backups are easy and cheap.
- **Security** — AWS invests billions in security; more than most companies can.
- **Focus** — Teams focus on code, not cables.

### Quick Analogy
On-prem = Owning a car (maintenance, insurance, parking, depreciation).
Cloud = Using Uber (pay per ride, no maintenance, available everywhere).

### Architecture View
```
BEFORE (On-Prem):
  Company ──▶ Buy servers ──▶ Rack them ──▶ Wire network ──▶ Install OS ──▶ Deploy app
  Timeline: 3-6 months

AFTER (Cloud):
  Company ──▶ AWS Console / CLI ──▶ Deploy app
  Timeline: 3-6 minutes
```

### Hands-On (Step-by-step Lab)
1. Think about a simple web app (frontend + backend + database).
2. **On-prem approach:** You'd need 3+ physical servers, networking gear, a data center.
3. **Cloud approach:** EC2 for backend, S3 for frontend, RDS for database.
4. Estimate costs at [calculator.aws](https://calculator.aws) → See how cheap it is to start.
5. Try launching a full stack on Elastic Beanstalk with a sample app — takes 5 minutes.

### Common Mistakes
- ❌ Saying "cloud is always cheaper" — it's cheaper for variable workloads, not always for steady, predictable ones.
- ❌ Lifting and shifting without re-architecting — you miss cloud benefits.
- ❌ Ignoring compliance and data residency requirements during migration.
- ❌ Not training the team on cloud skills before migrating.

### Pro Tips
- ✅ In interviews, mention the **6 advantages of cloud** (AWS official list): trade CapEx for OpEx, massive economies of scale, stop guessing capacity, increase speed/agility, stop spending money on data centers, go global in minutes.
- ✅ Real migrations use the **6 R's**: Rehost, Replatform, Refactor, Repurchase, Retire, Retain.
- ✅ Say: "We moved to cloud to reduce operational overhead and focus on delivering features faster."
- ✅ Mention AWS Well-Architected Framework if asked about best practices.

