# VPC Core (Virtual Private Cloud)

> Networking is the #1 most tested topic in DevOps interviews. Master VPC inside out.

---

## 1. VPC (Virtual Private Cloud)

### What is it?
A VPC is your own private, isolated network inside AWS.
You define the IP range, create subnets, set up routing, and control who gets in or out.
Every resource you launch (EC2, RDS, Lambda in VPC) lives inside a VPC.

### Why it matters in production
- VPC is the foundation of ALL AWS networking.
- Misconfigured VPC = resources can't talk to each other, or worse, they're exposed to the internet.
- Every single AWS architecture diagram starts with a VPC.

### Core Ideas
- **CIDR block** — The IP address range for your VPC. Example: `10.0.0.0/16` = 65,536 IPs.
- **Default VPC** — AWS creates one in every Region. Everything is public. Don't use for production.
- **Custom VPC** — You create it. Full control. Use this for production.
- One VPC per Region. Can span **all AZs** in that Region.
- VPC is **free**. You only pay for resources inside it (NAT, VPN, etc.).
- **DNS resolution** and **DNS hostnames** — Enable both for internal DNS to work.
- VPC cannot overlap CIDR with peered VPCs.

**Common CIDR blocks:**

| CIDR | IPs Available | Use Case |
|------|--------------|----------|
| `/16` | 65,536 | Large production VPC |
| `/20` | 4,096 | Medium workload |
| `/24` | 256 | Small / single subnet |
| `/28` | 16 | Minimum AWS subnet size |

**Private IP ranges (RFC 1918):**
- `10.0.0.0/8` — Most commonly used in AWS.
- `172.16.0.0/12`
- `192.168.0.0/16`

### Quick Analogy
VPC = Your own private office building.
You decide how many floors (subnets), where the doors are (gateways), and who can enter (security groups, NACLs). The building is invisible to other tenants in the same complex (AWS cloud).

### Architecture View
```
AWS Region: us-east-1
┌──────────────────────────────────────────────────────┐
│ VPC: 10.0.0.0/16                                     │
│                                                      │
│  ┌─────────────────┐    ┌─────────────────┐          │
│  │ AZ: us-east-1a  │    │ AZ: us-east-1b  │          │
│  │                 │    │                 │          │
│  │ Public Subnet   │    │ Public Subnet   │          │
│  │ 10.0.1.0/24     │    │ 10.0.2.0/24     │          │
│  │                 │    │                 │          │
│  │ Private Subnet  │    │ Private Subnet  │          │
│  │ 10.0.3.0/24     │    │ 10.0.4.0/24     │          │
│  └─────────────────┘    └─────────────────┘          │
│                                                      │
│  Internet Gateway ◄──► Internet                      │
│  NAT Gateway ◄──► Internet (outbound only)           │
└──────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **VPC → Create VPC**.
2. Select **VPC only** (not "VPC and more" — do it manually to learn).
3. Name: `my-prod-vpc`. CIDR: `10.0.0.0/16`.
4. Tenancy: Default.
5. Enable **DNS resolution** and **DNS hostnames**.
6. Create → You now have an empty VPC with no subnets, no gateway, no route table.
7. Verify: Go to VPC dashboard → See your VPC listed.

### Common Mistakes
- ❌ Using the default VPC for production — it's all public, not secure.
- ❌ Choosing a CIDR that's too small — you'll run out of IPs and can't resize easily.
- ❌ Overlapping CIDRs with on-prem or peered VPCs — peering/VPN won't work.
- ❌ Not enabling DNS hostnames — internal DNS resolution breaks.
- ❌ Forgetting that VPC is Region-scoped, not global.

### Pro Tips
- ✅ Plan your CIDR carefully. Use `/16` for production VPCs — room to grow.
- ✅ Don't overlap with your on-prem network or other VPCs.
- ✅ Use **VPC Flow Logs** to capture all network traffic metadata (for debugging and security).
- ✅ Use **secondary CIDR blocks** if you run out of IPs (added to existing VPC).
- ✅ In interviews: "We use custom VPCs with /16 CIDR, non-overlapping with on-prem. VPC Flow Logs are enabled for all production VPCs and sent to CloudWatch for monitoring."

---

## 2. Subnets

### What is it?
A subnet is a range of IP addresses within your VPC.
Each subnet lives in **one AZ** (it cannot span AZs).
You put your resources (EC2, RDS, etc.) into subnets.

### Why it matters in production
- Subnets let you segment your network into public and private zones.
- They control which AZ your resources run in.
- Proper subnet design is essential for HA and security.

### Core Ideas
- Subnet CIDR must be a **subset** of the VPC CIDR.
- Each subnet is in **exactly one AZ**.
- AWS reserves **5 IPs** in every subnet:
  - `.0` = Network address
  - `.1` = VPC router
  - `.2` = DNS server
  - `.3` = Reserved for future
  - `.255` = Broadcast (not used, but reserved)
  - So a `/24` subnet (256 IPs) has **251 usable** IPs.
- **Auto-assign public IP** — If enabled on the subnet, instances get a public IP automatically.
- A subnet is associated with **one route table** (default or custom).

### Quick Analogy
VPC = An office building.
Subnet = A floor in that building.
Each floor (subnet) is in a specific wing (AZ). Some floors are open to visitors (public), others are employees-only (private).

### Architecture View
```
VPC: 10.0.0.0/16
│
├── AZ: us-east-1a
│   ├── Public Subnet:  10.0.1.0/24  (251 usable IPs)
│   └── Private Subnet: 10.0.3.0/24  (251 usable IPs)
│
├── AZ: us-east-1b
│   ├── Public Subnet:  10.0.2.0/24  (251 usable IPs)
│   └── Private Subnet: 10.0.4.0/24  (251 usable IPs)
│
└── AZ: us-east-1c
    ├── Public Subnet:  10.0.5.0/24  (251 usable IPs)
    └── Private Subnet: 10.0.6.0/24  (251 usable IPs)

Total: 6 subnets across 3 AZs (3 public + 3 private)
```

### Hands-On (Step-by-step Lab)
1. Go to **VPC → Subnets → Create Subnet**.
2. Select your VPC (`my-prod-vpc`).
3. Create 4 subnets:

| Name | AZ | CIDR |
|------|----|------|
| `public-1a` | us-east-1a | `10.0.1.0/24` |
| `public-1b` | us-east-1b | `10.0.2.0/24` |
| `private-1a` | us-east-1a | `10.0.3.0/24` |
| `private-1b` | us-east-1b | `10.0.4.0/24` |

4. For public subnets → Enable **Auto-assign public IPv4 address**.
5. Verify: You now have 4 subnets, but they're all the same — what makes them public or private is the **route table**. That comes next.

### Common Mistakes
- ❌ Thinking "public subnet" is a setting — it's defined by the route table pointing to an Internet Gateway.
- ❌ Making subnets too small (`/28` = only 11 usable IPs).
- ❌ Putting all subnets in one AZ — defeats HA.
- ❌ Forgetting AWS reserves 5 IPs per subnet.
- ❌ Not planning for growth — adding subnets later is harder if CIDR space is used up.

### Pro Tips
- ✅ Naming convention: `{env}-{public/private}-{az}` → `prod-private-1a`.
- ✅ Use `/24` subnets (251 IPs) as a good default.
- ✅ Always create subnets in **at least 2 AZs** for HA.
- ✅ Plan for 3 tiers: Public (ALB), Private (App), Private (DB) — per AZ.
- ✅ In interviews: "We use a 3-tier subnet architecture across 3 AZs: public for load balancers, private for application, and isolated private for databases."

---

## 3. Public vs Private Subnet

### What is it?
A **public subnet** has a route to the Internet Gateway — instances can reach the internet and be reached from it.
A **private subnet** has NO route to the Internet Gateway — instances are hidden from the internet.
The ONLY difference is the route table. There is no "public subnet" checkbox.

### Why it matters in production
- Most production resources (app servers, databases) should be in **private subnets**.
- Only load balancers and bastion hosts go in **public subnets**.
- This is fundamental network security architecture.

### Core Ideas

| Aspect | Public Subnet | Private Subnet |
|--------|--------------|----------------|
| **Route to IGW** | Yes | No |
| **Public IP** | Yes (auto-assign or Elastic IP) | No |
| **Internet access** | Full (inbound + outbound) | Outbound only (via NAT) |
| **Reached from internet** | Yes | No |
| **Used for** | ALB, Bastion, NAT Gateway | App servers, databases, caches |

**What makes a subnet public:**
1. Route table has a route: `0.0.0.0/0 → Internet Gateway`.
2. Instances have a public IP (auto-assign or Elastic IP).

**What makes a subnet private:**
1. Route table does NOT have a route to IGW.
2. For outbound internet (updates, API calls): Route `0.0.0.0/0 → NAT Gateway`.

### Quick Analogy
- **Public subnet** = The reception area of an office. Visitors can walk in from the street.
- **Private subnet** = The back office. You can't enter from the street. Employees inside can go out (via NAT), but outsiders can't come in.

### Architecture View
```
Internet
    │
    ▼
Internet Gateway (IGW)
    │
    ├──────────────────────────────────────────┐
    │                                          │
    ▼                                          │
┌──────────────────────┐                       │
│ PUBLIC SUBNET        │                       │
│ Route: 0.0.0.0/0 → IGW                      │
│                      │                       │
│ ┌──────┐ ┌────────┐ │                       │
│ │ ALB  │ │NAT GW  │─┼────────────┐          │
│ └──┬───┘ └────────┘ │            │          │
└────┼────────────────┘            │          │
     │                             ▼          │
┌────┼────────────────────────────────────────┐
│ PRIVATE SUBNET                              │
│ Route: 0.0.0.0/0 → NAT Gateway             │
│                                             │
│ ┌──────┐ ┌──────┐ ┌──────┐                 │
│ │ EC2  │ │ EC2  │ │ RDS  │                 │
│ │ App  │ │ App  │ │  DB  │                 │
│ └──────┘ └──────┘ └──────┘                 │
│                                             │
│ Can reach internet (via NAT) ✅             │
│ Cannot be reached FROM internet ✅          │
└─────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Launch EC2 in the **public subnet** → It gets a public IP → You can SSH from home.
2. Launch EC2 in the **private subnet** → No public IP → You CANNOT SSH from home.
3. From the public instance → SSH to the private instance using its private IP (this is how bastion hosts work).
4. From the private instance → Try `curl google.com` → **Fails** (no NAT yet).
5. Add a NAT Gateway (next section) → Try again → **Works**.

### Common Mistakes
- ❌ Putting databases in public subnets — they should NEVER be internet-accessible.
- ❌ Thinking a subnet is public because of its name — it's the route table that matters.
- ❌ Forgetting to assign a public IP in the public subnet — instance has IGW route but no public IP = still no internet.
- ❌ Putting all resources in public subnets "for simplicity" — security nightmare.

### Pro Tips
- ✅ **Rule of thumb:** Only ALBs, NAT Gateways, and Bastion hosts go in public subnets. Everything else goes in private subnets.
- ✅ Use **VPC endpoints** for private instances to reach AWS services (S3, DynamoDB) without going through NAT.
- ✅ Enable **VPC Flow Logs** on both subnet types for security monitoring.
- ✅ In interviews: "We use a strict public/private separation. Public subnets hold only ALBs and NAT Gateways. All compute and data layers are in private subnets. We use VPC endpoints for AWS service access to reduce NAT costs."

---

## 4. Route Tables

### What is it?
A route table is a set of rules (routes) that determine where network traffic goes.
Every subnet is associated with exactly one route table.
Routes tell traffic: "To reach this destination, go through this target."

### Why it matters in production
- Route tables are what actually make subnets public or private.
- Misconfigured routes = instances can't reach the internet, each other, or AWS services.
- They're the "road map" of your VPC.

### Core Ideas
- **Every VPC has a Main Route Table** — auto-associated with subnets that don't have a custom one.
- **Local route** — `10.0.0.0/16 → local` (auto-created, can't delete). All subnets in the VPC can talk to each other.
- **Custom route tables** — You create them for public/private subnets.
- **Most specific route wins** — `/24` is more specific than `/16` which is more specific than `/0`.
- A subnet can only be associated with **ONE** route table at a time.

**Typical routes:**

| Route Table | Destination | Target | Purpose |
|-------------|-------------|--------|---------|
| Public RT | `10.0.0.0/16` | local | VPC internal traffic |
| Public RT | `0.0.0.0/0` | igw-xxx | Internet access |
| Private RT | `10.0.0.0/16` | local | VPC internal traffic |
| Private RT | `0.0.0.0/0` | nat-xxx | Outbound internet via NAT |

### Quick Analogy
Route table = A road sign at every intersection.
"To reach the internet (0.0.0.0/0), take the highway to the Internet Gateway."
"To reach another subnet (10.0.x.0/24), take the local road."
Each neighborhood (subnet) can have different signs (route table).

### Architecture View
```
VPC: 10.0.0.0/16

Public Route Table (associated with public subnets):
┌────────────────────────────────────────┐
│ Destination      │ Target              │
├──────────────────┼─────────────────────┤
│ 10.0.0.0/16      │ local               │ ← All VPC traffic stays local
│ 0.0.0.0/0        │ igw-abc123          │ ← Everything else → Internet
└────────────────────────────────────────┘

Private Route Table (associated with private subnets):
┌────────────────────────────────────────┐
│ Destination      │ Target              │
├──────────────────┼─────────────────────┤
│ 10.0.0.0/16      │ local               │ ← All VPC traffic stays local
│ 0.0.0.0/0        │ nat-xyz789          │ ← Everything else → NAT Gateway
└────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **VPC → Route Tables**.
2. Notice the **Main Route Table** was auto-created with your VPC.
3. Create a new route table: Name `public-rt` → Associate with your VPC.
4. Add route: Destination `0.0.0.0/0` → Target: Internet Gateway (create one first if needed).
5. Associate `public-rt` with your public subnets.
6. Create another route table: Name `private-rt`.
7. Add route: Destination `0.0.0.0/0` → Target: NAT Gateway (create one first).
8. Associate `private-rt` with your private subnets.
9. Test: EC2 in public subnet can reach internet. EC2 in private subnet can reach internet via NAT.

### Common Mistakes
- ❌ Editing the Main Route Table to add IGW → Makes ALL subnets public by default.
- ❌ Forgetting to associate the custom route table with the subnet.
- ❌ Having no route to NAT in private subnets → Instances can't download updates.
- ❌ Conflicting routes — though AWS uses most-specific-match, misunderstanding this causes issues.
- ❌ Not creating separate route tables for public and private subnets.

### Pro Tips
- ✅ **Never put the IGW route in the Main Route Table.** Create a separate public RT.
- ✅ Name your route tables clearly: `prod-public-rt`, `prod-private-rt`.
- ✅ Add **VPC endpoint routes** for S3 and DynamoDB (they appear automatically as `pl-xxx`).
- ✅ For VPC peering, add routes to the peered VPC CIDR pointing to the peering connection.
- ✅ In interviews: "We maintain separate route tables for public and private subnets. Public subnets route to IGW. Private subnets route to NAT. We never modify the main route table to avoid accidental public exposure."

---

## 5. Internet Gateway (IGW)

### What is it?
An Internet Gateway is the door between your VPC and the public internet.
It allows resources in public subnets to send and receive traffic from the internet.
It's horizontally scaled, redundant, and highly available — AWS manages it.

### Why it matters in production
- Without an IGW, nothing in your VPC can reach the internet (or be reached).
- It's required for public subnets to work.
- It's free — no hourly charge. You only pay for data transfer.

### Core Ideas
- **One IGW per VPC.** You cannot attach multiple IGWs to one VPC.
- IGW is **highly available** by design — no need for redundancy.
- IGW performs **NAT** (Network Address Translation) for instances with public IPs.
- IGW does NOT do anything by itself — you must add a route in the route table pointing to it.
- **Two things needed for internet access from public subnet:**
  1. Route table has `0.0.0.0/0 → IGW`.
  2. Instance has a public IP (auto-assign or Elastic IP).

### Quick Analogy
IGW = The main entrance door of your office building.
The door exists, but each floor (subnet) needs a hallway (route) leading to it.
If a floor has no hallway to the door, people on that floor can't leave the building.

### Architecture View
```
Internet
    │
    ▼
┌────────────┐
│   IGW      │ (1 per VPC, highly available, free)
│ igw-abc123 │
└─────┬──────┘
      │
      │ Route: 0.0.0.0/0 → igw-abc123
      │ (only in public subnet route table)
      │
      ▼
┌─────────────────────────┐
│ Public Subnet           │
│ EC2 (public IP: 3.x.x.x)│──▶ Internet ✅
└─────────────────────────┘

┌─────────────────────────┐
│ Private Subnet          │
│ EC2 (no public IP)       │──▶ Internet ❌ (no IGW route)
└─────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **VPC → Internet Gateways → Create**.
2. Name: `my-prod-igw`.
3. **Attach** it to your VPC.
4. Go to your **public route table** → Add route: `0.0.0.0/0` → Target: `my-prod-igw`.
5. Launch EC2 in the public subnet (with auto-assign public IP ON).
6. Try to `ping 8.8.8.8` from inside → Works.
7. Detach the IGW → Try again → Fails.

### Common Mistakes
- ❌ Creating an IGW but forgetting to attach it to the VPC.
- ❌ Attaching IGW but not adding the route in the route table.
- ❌ Adding IGW route to the private subnet route table — makes it public.
- ❌ Thinking IGW provides outbound-only access — it's bidirectional.

### Pro Tips
- ✅ IGW is free and fully managed. No sizing, no patching, no redundancy config.
- ✅ Use **Egress-Only Internet Gateway** for IPv6 outbound traffic (blocks inbound).
- ✅ Security comes from **Security Groups and NACLs**, not from removing the IGW.
- ✅ In interviews: "We attach one IGW per VPC and only route public subnets to it. Private subnets never have an IGW route — they use NAT Gateway for outbound access."

---

## 6. NAT Gateway

### What is it?
A NAT Gateway lets instances in **private subnets** access the internet for outbound traffic (downloads, updates, API calls) without being reachable from the internet.
Traffic goes out through NAT. Responses come back. But nobody from outside can initiate a connection in.
It lives in a **public subnet** and is managed by AWS.

### Why it matters in production
- Private instances need internet for: OS updates, pulling Docker images, calling external APIs.
- NAT Gateway provides this without exposing instances to inbound internet traffic.
- NAT Gateway costs can be significant — one of the top surprise charges on AWS bills.

### Core Ideas
- NAT Gateway lives in a **public subnet** (it needs IGW access).
- Private subnet route table: `0.0.0.0/0 → NAT Gateway`.
- **Managed by AWS** — highly available within one AZ.
- For multi-AZ HA: Create one NAT Gateway **per AZ**.
- **Pricing:** ~$0.045/hour + $0.045/GB processed. Can get expensive fast.
- Supports up to **45 Gbps** bandwidth (scales automatically).
- **NAT Instance** = Old way (self-managed EC2 as NAT). Cheaper but less reliable. Don't use.

**NAT Gateway vs NAT Instance:**

| Feature | NAT Gateway | NAT Instance |
|---------|------------|-------------|
| **Managed** | Yes (AWS) | No (you manage) |
| **Availability** | HA in single AZ | You configure |
| **Bandwidth** | Up to 45 Gbps | Depends on instance type |
| **Cost** | ~$32/month + data | EC2 cost (can be cheaper) |
| **Maintenance** | None | Patching, monitoring |
| **Recommendation** | Use this | Legacy, avoid |

### Quick Analogy
NAT Gateway = A mailroom in a private office.
Employees (private instances) can send mail out to the world through the mailroom.
Responses come back to the mailroom and get delivered. But strangers can't walk in through the mailroom.

### Architecture View
```
Internet
    │
    ▼
Internet Gateway
    │
    ▼
┌───────────────────────────────────────────────┐
│ PUBLIC SUBNET (us-east-1a)                    │
│  ┌──────────────┐                             │
│  │ NAT Gateway  │ ← Has Elastic IP            │
│  │ (nat-xxx)    │                             │
│  └──────┬───────┘                             │
└─────────┼─────────────────────────────────────┘
          │
          │  Private RT: 0.0.0.0/0 → nat-xxx
          ▼
┌───────────────────────────────────────────────┐
│ PRIVATE SUBNET (us-east-1a)                   │
│  ┌──────┐  ┌──────┐  ┌──────┐                │
│  │ EC2  │  │ EC2  │  │ EC2  │                │
│  └──────┘  └──────┘  └──────┘                │
│                                               │
│  yum update ──▶ NAT GW ──▶ IGW ──▶ Internet  │
│  Internet ──▶ ❌ BLOCKED (no inbound path)    │
└───────────────────────────────────────────────┘

Multi-AZ NAT (production):
┌──────────────┐    ┌──────────────┐
│ AZ-1a        │    │ AZ-1b        │
│ NAT-GW-1     │    │ NAT-GW-2     │
│ (public sub) │    │ (public sub) │
│      │       │    │      │       │
│      ▼       │    │      ▼       │
│ Private sub  │    │ Private sub  │
│ RT → NAT-1   │    │ RT → NAT-2   │
└──────────────┘    └──────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **VPC → NAT Gateways → Create**.
2. Place it in your **public subnet**.
3. Allocate a new **Elastic IP** for it.
4. Create the NAT Gateway → Wait for "Available" status.
5. Go to your **private route table** → Add route: `0.0.0.0/0` → Target: NAT Gateway.
6. SSH into a private instance (via bastion or SSM).
7. Run `curl https://google.com` → Works! Outbound internet via NAT.
8. Try `ping <private-instance-ip>` from the internet → Fails. No inbound access.

### Common Mistakes
- ❌ Placing NAT Gateway in a private subnet — it needs to be in a PUBLIC subnet.
- ❌ Forgetting to allocate an Elastic IP for the NAT Gateway.
- ❌ Creating only one NAT Gateway for multi-AZ — if that AZ goes down, all private subnets lose internet.
- ❌ Not monitoring NAT Gateway costs — data processing charges add up fast.
- ❌ Using NAT Gateway for traffic that should go through **VPC endpoints** (S3, DynamoDB).

### Pro Tips
- ✅ Create **one NAT Gateway per AZ** in production for HA.
- ✅ Use **VPC Gateway Endpoints** for S3 and DynamoDB — free and avoids NAT.
- ✅ Use **VPC Interface Endpoints (PrivateLink)** for other AWS services — avoids NAT costs.
- ✅ Monitor NAT Gateway with CloudWatch: `BytesOutToDestination`, `PacketsDropCount`.
- ✅ Set **billing alerts** specifically for NAT Gateway charges.
- ✅ In interviews: "We deploy one NAT Gateway per AZ for HA. We use VPC Gateway Endpoints for S3 and DynamoDB to reduce NAT costs. All private subnet route tables point to the NAT Gateway in their respective AZ."
