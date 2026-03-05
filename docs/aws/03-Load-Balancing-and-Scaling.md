# Load Balancing & Auto Scaling

---

## 1. ALB vs NLB

### What is it?
Load balancers distribute incoming traffic across multiple targets (EC2, containers, IPs).
AWS has two main types: **ALB** (Application Load Balancer) for HTTP/HTTPS and **NLB** (Network Load Balancer) for TCP/UDP.
They sit between users and your servers, spreading the load evenly.

### Why it matters in production
- Load balancers are essential for high availability and scaling.
- Without them, a single server handles all traffic — single point of failure.
- Choosing ALB vs NLB is a common interview question.

### Core Ideas

| Feature | ALB | NLB |
|---------|-----|-----|
| **Layer** | Layer 7 (HTTP/HTTPS) | Layer 4 (TCP/UDP/TLS) |
| **Routing** | Path, host, header, query string | Port-based only |
| **Speed** | Moderate (inspects HTTP) | Ultra-fast (millions of req/s) |
| **Static IP** | No (use with Global Accelerator) | Yes (Elastic IP per AZ) |
| **WebSockets** | Yes | Yes |
| **SSL termination** | Yes | Yes (TLS) |
| **Use case** | Web apps, microservices, APIs | Gaming, IoT, real-time, VPN |
| **Target types** | Instance, IP, Lambda | Instance, IP, ALB |
| **Health checks** | HTTP/HTTPS | TCP, HTTP, HTTPS |
| **Cost** | Per hour + LCU | Per hour + NLCU |

**There's also a Classic Load Balancer (CLB) — legacy. Don't use for new projects.**

**ALB advanced routing:**
- **Path-based:** `/api/*` → API servers, `/images/*` → Image servers.
- **Host-based:** `api.example.com` → API, `www.example.com` → Web.
- **Header/Query string:** Route by custom headers or query params.
- **Weighted target groups:** Send 90% to v1, 10% to v2 (canary deployments).

### Quick Analogy
- **ALB** = A smart receptionist who reads your request and directs you to the right department.
- **NLB** = A high-speed revolving door — doesn't look at who you are, just gets you through fast.

### Architecture View
```
ALB Architecture:
Users ──▶ ALB
           ├── /api/*    ──▶ Target Group: API servers (port 8080)
           ├── /web/*    ──▶ Target Group: Web servers (port 80)
           └── /static/* ──▶ Target Group: S3 (via Lambda)

NLB Architecture:
Users ──▶ NLB (Static IP: 1.2.3.4)
           ├── Port 443  ──▶ Target Group: App servers
           ├── Port 8883 ──▶ Target Group: MQTT brokers
           └── Port 3306 ──▶ Target Group: Database proxy
```

### Hands-On (Step-by-step Lab)
1. Launch 2 EC2 instances → Install Apache on both with different pages ("Server 1", "Server 2").
2. Go to **EC2 → Load Balancers → Create ALB**.
3. Scheme: Internet-facing. Listeners: HTTP (80).
4. Select at least 2 AZs.
5. Create a **Target Group** → Register both instances.
6. Create the ALB → Get the DNS name.
7. Open the DNS in browser → Refresh multiple times → See traffic switching between servers.
8. Add a **path-based rule**: `/api/*` → new target group.

### Common Mistakes
- ❌ Using ALB when you need static IPs — ALB doesn't support Elastic IPs directly.
- ❌ Using NLB for HTTP path-based routing — NLB doesn't understand HTTP.
- ❌ Putting instances in only 1 AZ — defeats HA purpose.
- ❌ Not enabling cross-zone load balancing (enabled by default on ALB, optional on NLB).
- ❌ Forgetting to allow ALB's Security Group in the instance's Security Group.

### Pro Tips
- ✅ **ALB** for 99% of web applications — path and host routing is powerful.
- ✅ **NLB** for extreme performance, static IPs, or non-HTTP protocols.
- ✅ Use **ALB + WAF** for web application security (SQL injection, XSS protection).
- ✅ Use **NLB in front of ALB** when you need both static IPs AND path-based routing.
- ✅ Enable **access logs** on ALB → sends to S3 for analysis.
- ✅ In interviews: "We use ALB for HTTP workloads with path-based routing for microservices. NLB is used for TCP workloads needing ultra-low latency or static IPs. Both span multiple AZs for HA."

---

## 2. Target Groups

### What is it?
A Target Group is a group of targets (EC2 instances, IPs, or Lambda functions) that receive traffic from a load balancer.
You register targets in a target group. The load balancer routes traffic to healthy targets.
Each listener rule on the ALB/NLB points to a target group.

### Why it matters in production
- Target groups are how you organize and health-check your backend.
- They enable blue/green deployments, canary releases, and microservice routing.
- Auto Scaling Groups register new instances into target groups automatically.

### Core Ideas
- **Target types:** Instance (by ID), IP (for containers, on-prem), Lambda function.
- **Health checks** — Load balancer checks if targets are healthy. Unhealthy targets don't get traffic.
- **Deregistration delay** — Time to wait before removing a target (default 300s). Lets in-flight requests finish.
- **Stickiness** — Route the same client to the same target (session affinity). Use with caution.
- **Multiple target groups** — One ALB can route to many target groups via listener rules.
- **Weighted target groups** — ALB can split traffic between groups by percentage.

### Quick Analogy
Target Group = A team in a call center.
"Sales" team handles sales calls. "Support" team handles support calls. The phone system (load balancer) routes calls to the right team. If someone in a team is on break (unhealthy), calls go to others.

### Architecture View
```
ALB Listener (:443 HTTPS)
  │
  ├── Rule: host = api.example.com
  │         └── Target Group: api-tg
  │             ├── EC2-1 (10.0.1.10:8080) ✅ healthy
  │             ├── EC2-2 (10.0.2.10:8080) ✅ healthy
  │             └── EC2-3 (10.0.1.20:8080) ❌ unhealthy (skipped)
  │
  ├── Rule: host = web.example.com
  │         └── Target Group: web-tg
  │             ├── EC2-4 (10.0.1.30:80) ✅ healthy
  │             └── EC2-5 (10.0.2.30:80) ✅ healthy
  │
  └── Default rule
            └── Target Group: default-tg (404 page)
```

### Hands-On (Step-by-step Lab)
1. Go to **EC2 → Target Groups → Create**.
2. Target type: Instances. Protocol: HTTP. Port: 80.
3. Configure health check: Path `/health`, interval 30s, threshold 3.
4. Register 2 EC2 instances.
5. Check target health status → Wait for "healthy".
6. Stop one instance → Health check fails → Status changes to "unhealthy".
7. ALB automatically stops sending traffic to unhealthy target.

### Common Mistakes
- ❌ Health check path returns 404 → All targets appear unhealthy → No traffic flows.
- ❌ Deregistration delay too short → In-flight requests get dropped.
- ❌ Enabling stickiness without understanding the impact on even load distribution.
- ❌ Not matching the target group port with the application port.

### Pro Tips
- ✅ Always create a dedicated **health check endpoint** (`/health` or `/healthz`).
- ✅ Set deregistration delay based on your longest request time.
- ✅ Use **weighted target groups** for canary deployments: 95% old, 5% new.
- ✅ Use **slow start mode** — gives new targets time to warm up before full traffic.
- ✅ In interviews: "We use target groups per microservice with dedicated health check endpoints. Weighted routing enables canary deployments. Slow start prevents cold-start overload."

---

## 3. Auto Scaling Groups (ASG)

### What is it?
An Auto Scaling Group automatically adjusts the number of EC2 instances based on demand.
You define minimum, desired, and maximum instance counts. ASG does the rest.
It launches instances when load increases and terminates them when load drops.

### Why it matters in production
- Auto Scaling = cost savings (scale down when idle) + availability (scale up when busy).
- It automatically replaces unhealthy instances.
- Combined with ALB, it's the foundation of scalable AWS architecture.

### Core Ideas
- **Launch Template** — Defines WHAT to launch (AMI, instance type, SG, key pair, user data).
- **ASG Configuration** — Defines HOW to scale (min, desired, max, AZs, scaling policies).
- **Min capacity** — Never go below this number.
- **Desired capacity** — The number ASG tries to maintain.
- **Max capacity** — Never go above this number.
- **Cooldown period** — Wait time between scaling actions (default 300s). Prevents thrashing.
- **Health checks** — EC2 health check (default) or ELB health check (recommended with ALB).
- **Termination policy** — Which instance to kill when scaling in (default: oldest launch config, closest to billing hour).

### Quick Analogy
ASG = A restaurant manager.
Busy Friday night → Calls in more waiters (scale out).
Quiet Monday lunch → Sends waiters home (scale in).
Waiter gets sick → Replaces them immediately (self-healing).

### Architecture View
```
Auto Scaling Group (min: 2, desired: 2, max: 6)
┌─────────────────────────────────────────────────┐
│                                                 │
│  AZ-1a              AZ-1b              AZ-1c    │
│  ┌────────┐        ┌────────┐                   │
│  │ EC2 #1 │        │ EC2 #2 │        (empty)    │
│  └────────┘        └────────┘                   │
│                                                 │
│  ← Currently: 2 instances (desired)             │
│                                                 │
│  HIGH LOAD → Scale Out:                         │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   │
│  │ EC2 #1 │ │ EC2 #2 │ │ EC2 #3 │ │ EC2 #4 │   │
│  └────────┘ └────────┘ └────────┘ └────────┘   │
│  ← Now: 4 instances                             │
│                                                 │
│  LOW LOAD → Scale In:                           │
│  ┌────────┐ ┌────────┐                          │
│  │ EC2 #1 │ │ EC2 #2 │  (EC2 #3, #4 terminated)│
│  └────────┘ └────────┘                          │
│  ← Back to: 2 instances (min)                   │
└─────────────────────────────────────────────────┘
          │
     Registered in ──▶ ALB Target Group
```

### Hands-On (Step-by-step Lab)
1. Create a **Launch Template**: AMI (Amazon Linux), `t3.micro`, security group, user data (install Apache).
2. Go to **EC2 → Auto Scaling → Create ASG**.
3. Select your launch template.
4. Select 2 AZs.
5. Attach to an existing ALB target group.
6. Set: Min=2, Desired=2, Max=4.
7. Health check type: ELB.
8. Create → Watch 2 instances launch automatically.
9. **Terminate one manually** → ASG detects and launches a replacement.
10. Check ALB → Traffic flows to healthy instances during replacement.

### Common Mistakes
- ❌ Setting min=desired=max (no scaling happens — fixed capacity).
- ❌ Using EC2 health checks instead of ELB — EC2 thinks a crashed app is healthy if the VM is running.
- ❌ Cooldown period too short → Rapid scale up/down (thrashing).
- ❌ Not spreading across multiple AZs.
- ❌ Launch template without user data → New instances launch but app isn't configured.

### Pro Tips
- ✅ Always use **ELB health checks** with ASG (not just EC2).
- ✅ Use **Launch Templates** (not Launch Configurations — they're legacy).
- ✅ Enable **Instance Refresh** for rolling updates (deploy new AMI gradually).
- ✅ Use **Warm Pools** — Pre-initialized instances that scale in faster.
- ✅ Set **termination protection** on critical instances in the ASG.
- ✅ In interviews: "Our ASGs use launch templates with golden AMIs, span 3 AZs, use ELB health checks, and have target tracking scaling policies. Instance Refresh handles rolling AMI updates."

---

## 4. Scaling Policies

### What is it?
Scaling policies define WHEN and HOW the Auto Scaling Group should add or remove instances.
You choose triggers (CPU, request count, custom metrics) and actions (add 1 instance, set to 10 instances, etc.).
There are three types: Target Tracking, Step Scaling, and Simple Scaling.

### Why it matters in production
- Without scaling policies, ASG maintains a fixed number — no auto-scaling.
- Good policies save money and prevent outages during traffic spikes.
- Choosing the right policy type is a key architectural decision.

### Core Ideas

| Policy Type | How It Works | Best For |
|-------------|-------------|----------|
| **Target Tracking** | Maintain a target value (e.g., CPU = 50%) | Most workloads (recommended) |
| **Step Scaling** | Different actions at different thresholds | Fine-grained control |
| **Simple Scaling** | One action, then wait for cooldown | Legacy, avoid for new designs |
| **Scheduled Scaling** | Scale at specific times | Predictable traffic patterns |
| **Predictive Scaling** | ML-based, predicts future traffic | Recurring patterns |

**Target Tracking (Recommended):**
- You set: "Keep average CPU at 50%."
- ASG automatically adjusts instances to maintain that target.
- Works like a thermostat — set the temperature and forget.

**Step Scaling:**
- CPU 60-70% → Add 1 instance.
- CPU 70-80% → Add 2 instances.
- CPU > 80% → Add 3 instances.
- More control but more complex.

**Scheduled Scaling:**
- "Every weekday at 8 AM, set desired to 10. At 6 PM, set to 2."
- Great for known traffic patterns.

**Predictive Scaling:**
- Uses ML to analyze past patterns and pre-scale before traffic arrives.
- Combine with Target Tracking for best results.

### Quick Analogy
- **Target Tracking** = Thermostat. Set to 70°F. It auto-adjusts heating/cooling.
- **Step Scaling** = Manual AC with levels. Warm → Level 1. Hot → Level 2. Very hot → Level 3.
- **Scheduled** = Timer on the heater. Turn on at 7 AM, off at 10 PM.
- **Predictive** = Smart thermostat that learns your schedule and pre-heats.

### Architecture View
```
Target Tracking Policy Example:
                                   Target: CPU = 50%
                                         │
CloudWatch Metric: CPUUtilization        │
                                         │
CPU = 30% ──▶ Scale IN (remove instances to save cost)
CPU = 50% ──▶ No action (at target)
CPU = 70% ──▶ Scale OUT (add instances to reduce load)

Timeline:
  8AM          12PM          6PM          10PM
  ┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐
  │ 3 inst │   │ 8 inst │   │ 5 inst │   │ 2 inst │
  │ (ramp) │   │ (peak) │   │ (drop) │   │ (min)  │
  └────────┘   └────────┘   └────────┘   └────────┘
```

### Hands-On (Step-by-step Lab)
1. Open your existing ASG → Go to **Automatic Scaling** tab.
2. Create a **Target Tracking policy**: Metric = Average CPU, Target = 50%.
3. SSH into one instance → Run `stress --cpu 4 --timeout 600`.
4. Watch CloudWatch → CPU spikes above 50%.
5. After ~3 minutes → ASG launches new instances.
6. Stop the stress test → CPU drops → ASG terminates extra instances.
7. Add a **Scheduled Action**: Scale to 4 instances at 9 AM daily.

### Common Mistakes
- ❌ Using Simple Scaling (cooldown blocks fast response).
- ❌ Target tracking with wrong metric — CPU isn't always the bottleneck.
- ❌ Not accounting for instance boot time — scale happens but app takes 3 min to start.
- ❌ Setting max too low — ASG can't scale enough during spikes.
- ❌ No scale-in protection for instances doing long-running jobs.

### Pro Tips
- ✅ **Target Tracking** is the default recommendation — start here.
- ✅ Use **ALBRequestCountPerTarget** metric for request-based scaling (often better than CPU).
- ✅ Combine **Predictive Scaling + Target Tracking** for optimal results.
- ✅ Add **Warm Pools** to reduce launch time for new instances.
- ✅ Use **custom CloudWatch metrics** (queue depth, connection count) for precise scaling.
- ✅ In interviews: "We use Target Tracking on ALBRequestCountPerTarget to scale based on actual user load. Predictive Scaling pre-warms before known peaks. Warm Pools reduce spin-up time to under 30 seconds."

---

## 5. Health Checks

### What is it?
Health checks verify that your targets (EC2 instances, containers) are working correctly.
If a target fails health checks, it stops receiving traffic and gets replaced.
They are configured on load balancers, target groups, and Auto Scaling Groups.

### Why it matters in production
- Health checks are the foundation of self-healing infrastructure.
- Without them, traffic goes to dead or broken instances.
- Misconfigured health checks cause false positives (removing healthy instances).

### Core Ideas

**Types of health checks:**

| Source | What It Checks | Action on Failure |
|--------|---------------|-------------------|
| **EC2 Status Check** | VM is running, network reachable | EC2 auto-recovery |
| **ELB Health Check** | App responds on a specific path/port | ALB stops routing traffic |
| **ASG Health Check** | EC2 or ELB health | ASG terminates and replaces instance |
| **Route 53 Health Check** | Endpoint reachable from global locations | DNS failover to backup |

**ELB Health Check configuration:**
- **Protocol:** HTTP or HTTPS.
- **Path:** `/health` or `/healthz` (your app must return 200).
- **Interval:** How often to check (default 30s).
- **Timeout:** How long to wait for response (default 5s).
- **Healthy threshold:** How many consecutive successes to mark healthy (default 5).
- **Unhealthy threshold:** How many consecutive failures to mark unhealthy (default 2).

**Health check flow:**
```
ALB ──HTTP GET /health──▶ Target
                          │
         200 OK ◀─────────┘  → Healthy ✅
         or
         Timeout/500 ◀────┘  → Count failure
                               2 failures → Unhealthy ❌
                               → Stop routing traffic
                               → ASG replaces instance
```

### Quick Analogy
Health check = A doctor's checkup.
The load balancer is a doctor that checks each server's pulse every 30 seconds.
If the server fails 2 checks in a row, it's marked sick and gets sent home (removed from rotation).
A new healthy server takes its place.

### Architecture View
```
Health Check Chain:
┌─────────────────────────────────────────────────┐
│                                                 │
│  Route 53 Health Check                          │
│  "Is the ALB responding?"                       │
│        │                                        │
│        ▼                                        │
│  ALB Health Check                               │
│  "Is each target responding on /health?"        │
│        │                                        │
│        ├── EC2-1: GET /health → 200 ✅          │
│        ├── EC2-2: GET /health → 200 ✅          │
│        └── EC2-3: GET /health → timeout ❌      │
│                                                 │
│  ASG Health Check (ELB type)                    │
│  "EC2-3 is unhealthy → terminate → launch new"  │
│                                                 │
│  EC2 Status Check                               │
│  "Is the VM running? Network OK?"               │
│                                                 │
└─────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Create an ALB with a target group.
2. Set health check: Path = `/health`, Interval = 10s, Unhealthy threshold = 2.
3. On your EC2, create a health endpoint:
```bash
echo "OK" > /var/www/html/health
```
4. Register in target group → Check health status: **Healthy**.
5. Delete the health file: `rm /var/www/html/health`.
6. Wait 20 seconds → Target becomes **Unhealthy** → ALB stops sending traffic.
7. Restore the file → Target becomes **Healthy** again.
8. In ASG → Change health check type to **ELB** → Now ASG replaces unhealthy instances.

### Common Mistakes
- ❌ Health check path doesn't exist (404) → All instances marked unhealthy.
- ❌ Health check timeout > interval → Overlapping checks, confused state.
- ❌ ASG using EC2 health check instead of ELB → App is down but VM is fine, no replacement.
- ❌ Health check too aggressive (1s interval, 1 threshold) → Flapping (healthy/unhealthy/healthy).
- ❌ Health endpoint doesn't check real dependencies (DB connection, disk space).

### Pro Tips
- ✅ Create a **smart health endpoint** that checks: app running, DB connected, disk space OK.
- ✅ Use **ELB health checks in ASG** — always. EC2 status checks only catch VM-level issues.
- ✅ Set unhealthy threshold to 2-3 (don't overreact to a single timeout).
- ✅ Set healthy threshold to 2 (recover quickly once healthy again).
- ✅ Use **Route 53 health checks** for multi-Region failover.
- ✅ In interviews: "Our health check endpoint at /healthz validates the app process, database connectivity, and disk space. ASG uses ELB health checks so crashed apps get replaced automatically. Route 53 health checks handle regional failover."
