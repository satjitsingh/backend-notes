# VPC Security & Connectivity

---

## 1. Security Groups vs NACLs

### What is it?
Security Groups and NACLs are two layers of VPC firewalls that filter traffic.
Security Groups work at the **instance level**. NACLs work at the **subnet level**.
Together, they form defense-in-depth: two walls an attacker must get through.

### Why it matters in production
- This is one of the most asked interview questions: "What's the difference?"
- Using both correctly gives you layered security.
- Misconfiguring either one causes outages or security holes.

### Core Ideas

| Feature | Security Group | NACL (Network ACL) |
|---------|---------------|-------------------|
| **Level** | Instance (ENI) | Subnet |
| **State** | **Stateful** | **Stateless** |
| **Rules** | Allow only | Allow AND Deny |
| **Evaluation** | All rules evaluated together | Rules evaluated in order (by number) |
| **Default** | Denies all inbound, allows all outbound | Allows all inbound and outbound |
| **Return traffic** | Automatic (stateful) | Must explicitly allow (stateless) |
| **Association** | Attached to instances (up to 5 SGs) | One NACL per subnet |
| **Use case** | Primary firewall for instances | Subnet-level guardrails, block IPs |

**Stateful vs Stateless — The most important concept:**
- **Security Group (Stateful):** If you allow inbound HTTP (port 80), the response automatically goes out. No outbound rule needed for the response.
- **NACL (Stateless):** If you allow inbound HTTP (port 80), you MUST also allow outbound on **ephemeral ports** (1024-65535) for the response. Otherwise, the response is dropped.

**NACL rule evaluation:**
- Rules are numbered (e.g., 100, 200, 300).
- Evaluated **in order**, lowest number first.
- First matching rule wins. The rest are ignored.
- Rule `*` (asterisk) is the default deny-all (last rule, catches everything).

### Quick Analogy
- **Security Group** = A personal bodyguard for each person (instance). Knows who you let in, automatically lets them leave.
- **NACL** = A security checkpoint at the neighborhood entrance (subnet). Checks everyone coming and going separately. Doesn't remember who came in.

### Architecture View
```
Internet
    │
    ▼
┌────────────────────────────────────────┐
│ NACL (Subnet-level firewall)           │
│                                        │
│ Inbound Rules:                         │
│   100: Allow TCP 80 from 0.0.0.0/0    │
│   110: Allow TCP 443 from 0.0.0.0/0   │
│   120: Deny TCP 22 from 1.2.3.4/32    │ ← Block specific IP
│   *:   Deny all                        │
│                                        │
│ Outbound Rules:                        │
│   100: Allow TCP 1024-65535 to 0.0.0.0/0│ ← Ephemeral ports (responses)
│   *:   Deny all                        │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ Security Group (Instance-level)    │ │
│ │                                    │ │
│ │ Inbound:                           │ │
│ │   Allow TCP 80 from 0.0.0.0/0     │ │
│ │   Allow TCP 443 from 0.0.0.0/0    │ │
│ │                                    │ │
│ │ Outbound:                          │ │
│ │   Allow All (default)              │ │
│ │                                    │ │
│ │ ┌──────────┐                       │ │
│ │ │ EC2      │                       │ │
│ │ └──────────┘                       │ │
│ └────────────────────────────────────┘ │
└────────────────────────────────────────┘

Traffic path: Internet → NACL → Security Group → EC2
```

### Hands-On (Step-by-step Lab)
1. Launch an EC2 in a public subnet with HTTP (80) allowed in the Security Group.
2. Access the webpage → Works.
3. Go to **VPC → Network ACLs** → Select the NACL for your subnet.
4. Add inbound rule 50: **Deny TCP 80 from your IP** (`x.x.x.x/32`).
5. Try accessing the webpage → **Blocked** (NACL denied it before SG even sees it).
6. Remove the NACL deny rule → Works again.
7. Add outbound rule: Deny TCP 1024-65535 → Webpage loads but **response can't get back** (stateless!).

### Common Mistakes
- ❌ Forgetting ephemeral ports in NACL outbound rules — responses get dropped.
- ❌ Relying only on Security Groups — NACLs add an important second layer.
- ❌ Not understanding stateful vs stateless — causes mysterious connection failures.
- ❌ NACL rule ordering wrong — a DENY at rule 100 blocks traffic before an ALLOW at rule 200.
- ❌ Modifying the default NACL to deny everything — breaks all subnets using it.

### Pro Tips
- ✅ **Security Groups** = Your primary firewall. Fine-grained, per-instance.
- ✅ **NACLs** = Your secondary guardrail. Use for subnet-wide rules and IP blocking.
- ✅ Use NACLs to **block known bad IPs** (e.g., attackers, scrapers).
- ✅ NACL rules: Use increments of 10 or 100 (100, 200, 300) so you can insert rules later.
- ✅ In interviews: "We use Security Groups as the primary firewall per instance, with SG chaining across tiers. NACLs serve as subnet-level guardrails to block malicious IPs and add defense-in-depth. We understand NACLs are stateless, so we always include ephemeral port rules."

---

## 2. Bastion Host (Jump Box)

### What is it?
A Bastion Host is a hardened EC2 instance in a **public subnet** used to SSH into instances in **private subnets**.
It's the secure gateway into your private network.
You SSH to the bastion first, then SSH from the bastion to the private instance.

### Why it matters in production
- Private instances have no public IP — you can't SSH directly from the internet.
- The bastion is the controlled entry point — all access is funneled through it.
- It's being replaced by **SSM Session Manager** in modern architectures.

### Core Ideas
- Bastion sits in a **public subnet** with a public IP.
- SSH to bastion (port 22) → Then SSH from bastion to private instances.
- **Harden the bastion:** Minimal software, restricted SG, enable logging.
- **SG rules:**
  - Bastion SG: Allow SSH (22) only from your corporate IP or VPN.
  - Private instance SG: Allow SSH (22) only from the Bastion SG.
- **Alternative (recommended): AWS SSM Session Manager** — No bastion needed, no SSH port open, fully audited via CloudTrail.

### Quick Analogy
Bastion = A security reception desk in a restricted building.
You check in at reception (bastion), show your ID (SSH key), and then you're escorted (SSH hop) to the private office (private instance). No one gets to the office without going through reception.

### Architecture View
```
Your Laptop (Corporate IP: 203.0.113.0/24)
    │
    │ SSH (port 22)
    ▼
┌────────────────────────────────────┐
│ PUBLIC SUBNET                      │
│ ┌──────────────────┐               │
│ │ Bastion Host     │               │
│ │ SG: Allow SSH    │               │
│ │ from 203.0.113.0/24 ONLY        │
│ └────────┬─────────┘               │
└──────────┼─────────────────────────┘
           │
           │ SSH (port 22, private IP)
           ▼
┌────────────────────────────────────┐
│ PRIVATE SUBNET                     │
│ ┌──────────────────┐               │
│ │ App Server       │               │
│ │ SG: Allow SSH    │               │
│ │ from bastion-sg ONLY            │
│ └──────────────────┘               │
└────────────────────────────────────┘

MODERN (SSM — No bastion needed):
Your Laptop ──▶ SSM Session Manager ──▶ Private Instance
                (over HTTPS, port 443)
                No SSH, no bastion, no key pair, fully audited
```

### Hands-On (Step-by-step Lab)
**Traditional Bastion:**
1. Launch an EC2 (Bastion) in the **public subnet**. SG: SSH from your IP only.
2. Launch an EC2 (App) in the **private subnet**. SG: SSH from bastion's SG only.
3. Copy your private key to the bastion (or use SSH agent forwarding).
4. SSH to bastion: `ssh -i key.pem ec2-user@<bastion-public-ip>`.
5. From bastion, SSH to app: `ssh -i key.pem ec2-user@<app-private-ip>`.

**Modern SSM (Recommended):**
1. Attach IAM role with `AmazonSSMManagedInstanceCore` policy to your private EC2.
2. Ensure the instance has outbound internet (NAT) or a VPC endpoint for SSM.
3. Go to **Systems Manager → Session Manager → Start Session**.
4. Select your private instance → Connect. No SSH, no bastion, no keys.

### Common Mistakes
- ❌ Opening bastion SSH to `0.0.0.0/0` — defeats the purpose.
- ❌ Not hardening the bastion — install only what you need.
- ❌ Storing private keys on the bastion — use SSH agent forwarding instead.
- ❌ Using bastion when SSM is available — bastion is legacy approach.
- ❌ Forgetting to log bastion access — no audit trail.

### Pro Tips
- ✅ **Use SSM Session Manager instead of bastion hosts.** It's more secure and simpler.
- ✅ If you must use a bastion: restrict to corporate IPs, enable OS-level logging, use MFA.
- ✅ Use **SSH agent forwarding** (`ssh -A`) so you don't need to copy keys to the bastion.
- ✅ Auto-terminate the bastion when not in use (schedule with Lambda).
- ✅ In interviews: "We replaced bastion hosts with SSM Session Manager. No SSH ports open, no key management, and every session is logged in CloudTrail. For compliance, we also have VPC endpoints for SSM to avoid NAT dependency."

---

## 3. Private Architecture (Best Practice Pattern)

### What is it?
A Private Architecture is a VPC design where application and data tiers have **zero direct internet exposure**.
ALBs in public subnets handle incoming traffic. Everything else is private.
This is the standard production architecture on AWS.

### Why it matters in production
- It's the expected answer when an interviewer asks: "Design a secure AWS architecture."
- Minimizes attack surface — fewer things exposed to the internet.
- Required for compliance (PCI-DSS, HIPAA, SOC2).

### Core Ideas

**The 3-Tier Architecture:**

| Tier | Subnet Type | Components | Internet Access |
|------|------------|-----------|-----------------|
| **Web/LB Tier** | Public | ALB, NAT Gateway | Full (inbound + outbound) |
| **App Tier** | Private | EC2, ECS, Lambda | Outbound only (via NAT) |
| **Data Tier** | Private (isolated) | RDS, ElastiCache, DynamoDB | None (or VPC endpoints) |

**Key principles:**
- ALB in public subnets — only thing exposed to internet.
- App servers in private subnets — receive traffic ONLY from ALB.
- Databases in private subnets — receive traffic ONLY from app tier.
- NAT Gateway for outbound internet (updates, external APIs).
- VPC Endpoints for AWS services (avoid NAT for S3, DynamoDB, etc.).
- SG chaining enforces tier-to-tier traffic rules.

### Quick Analogy
Private Architecture = A bank.
- **Lobby (public)** = Customers walk in (ALB handles requests).
- **Back office (private)** = Bank employees process transactions (app servers).
- **Vault (isolated private)** = Money stored securely (database). Only employees can access it. Customers can never reach the vault directly.

### Architecture View
```
                        Internet
                           │
                           ▼
                    ┌─────────────┐
                    │   Route 53  │ (DNS)
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ CloudFront  │ (CDN, optional)
                    └──────┬──────┘
                           │
┌──────────────────────────▼────────────────────────────┐
│ VPC: 10.0.0.0/16                                      │
│                                                       │
│ PUBLIC SUBNETS (10.0.1.0/24, 10.0.2.0/24)            │
│ ┌─────────┐  ┌─────────┐  ┌─────────┐                │
│ │  ALB    │  │ NAT GW  │  │ NAT GW  │                │
│ │         │  │ (AZ-1a) │  │ (AZ-1b) │                │
│ └────┬────┘  └─────────┘  └─────────┘                │
│      │                                                │
│ PRIVATE SUBNETS — APP (10.0.3.0/24, 10.0.4.0/24)     │
│ ┌────▼────┐  ┌─────────┐                              │
│ │  EC2    │  │  EC2    │  ← Auto Scaling Group        │
│ │  App    │  │  App    │  ← SG: Allow from ALB-SG     │
│ └────┬────┘  └────┬────┘                              │
│      │            │                                    │
│ PRIVATE SUBNETS — DATA (10.0.5.0/24, 10.0.6.0/24)    │
│ ┌────▼────┐  ┌────▼────┐                              │
│ │  RDS    │  │  Redis  │  ← SG: Allow from App-SG     │
│ │ Primary │  │ Cache   │  ← NO internet access         │
│ │  (1a)   │  │  (1b)   │                              │
│ └─────────┘  └─────────┘                              │
│                                                       │
│ VPC Endpoints: S3 (Gateway), SSM, CloudWatch (Interface)│
└───────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Create a VPC with CIDR `10.0.0.0/16`.
2. Create 6 subnets: 2 public, 2 app-private, 2 data-private (across 2 AZs).
3. Create an IGW → Attach to VPC.
4. Create public route table: `0.0.0.0/0 → IGW` → Associate with public subnets.
5. Create NAT Gateway in each public subnet.
6. Create private route table per AZ: `0.0.0.0/0 → NAT GW` → Associate with private subnets.
7. Create Security Groups:
   - `alb-sg`: Inbound 80/443 from `0.0.0.0/0`.
   - `app-sg`: Inbound 8080 from `alb-sg`.
   - `db-sg`: Inbound 5432 from `app-sg`.
8. Deploy: ALB in public → EC2s in app-private → RDS in data-private.
9. Test: Internet → ALB → App → Database. Database unreachable from internet. ✅

### Common Mistakes
- ❌ Putting RDS in a public subnet with a public IP.
- ❌ Allowing all traffic between tiers — SG chaining should be strict.
- ❌ Forgetting NAT Gateway for private instances that need outbound access.
- ❌ Not using VPC endpoints — unnecessary NAT costs for AWS service traffic.
- ❌ Single AZ deployment — no high availability.

### Pro Tips
- ✅ This 3-tier private architecture is the **default answer** for any "design a secure system" interview question.
- ✅ Add **WAF** on the ALB for web application protection.
- ✅ Add **AWS Shield** for DDoS protection.
- ✅ Use **VPC Flow Logs** → S3 or CloudWatch for network monitoring.
- ✅ Use **PrivateLink** (interface endpoints) for SSM, CloudWatch, ECR.
- ✅ In interviews: "Our standard architecture uses 3 tiers across multiple AZs. ALB in public subnets, app in private, databases in isolated private. SG chaining ensures each tier only talks to adjacent tiers. VPC endpoints reduce NAT costs. WAF and Shield protect the edge."

---

## 4. VPC Peering

### What is it?
VPC Peering is a private network connection between two VPCs.
Traffic flows over AWS's backbone — no internet involved.
The two VPCs can be in the same account, different accounts, or even different Regions.

### Why it matters in production
- Multi-account setups (dev/staging/prod) often need VPCs to communicate.
- Shared services (logging, monitoring, CI/CD) often live in a central VPC.
- VPC Peering is the simplest way to connect two VPCs.

### Core Ideas
- **No transitive peering.** VPC A ↔ VPC B and VPC B ↔ VPC C does NOT mean A can talk to C.
- CIDRs **must not overlap** — If both VPCs use `10.0.0.0/16`, peering fails.
- Both sides must **accept** the peering request.
- You must add **routes** in both VPCs pointing to the peering connection.
- You must update **Security Groups** to allow traffic from the peered VPC CIDR.
- Supports **cross-Region peering** (traffic encrypted automatically).
- Supports **cross-account peering** (requires peering request + acceptance).

### Quick Analogy
VPC Peering = Building a private bridge between two islands.
Cars can travel between the two islands directly. But if there's a third island, you need a separate bridge to each one — you can't drive through an island to reach another (no transitive routing).

### Architecture View
```
┌─────────────────────┐         ┌─────────────────────┐
│ VPC A: 10.0.0.0/16  │         │ VPC B: 172.16.0.0/16│
│ (Dev Account)       │         │ (Prod Account)       │
│                     │         │                     │
│ Route Table:        │         │ Route Table:        │
│ 172.16.0.0/16 →     │◄─pcx──►│ 10.0.0.0/16 →       │
│   pcx-abc123        │  peer   │   pcx-abc123        │
│                     │         │                     │
│ SG: Allow from      │         │ SG: Allow from      │
│   172.16.0.0/16     │         │   10.0.0.0/16       │
└─────────────────────┘         └─────────────────────┘

NO Transitive Peering:
VPC A ↔ VPC B ↔ VPC C
  A can talk to B ✅
  B can talk to C ✅
  A can talk to C ❌ (need separate peering A↔C)
```

### Hands-On (Step-by-step Lab)
1. Create **VPC A** (`10.0.0.0/16`) and **VPC B** (`172.16.0.0/16`).
2. Go to **VPC → Peering Connections → Create**.
3. Select VPC A as requester, VPC B as accepter.
4. Go to the peering connection → **Accept Request**.
5. VPC A Route Table → Add: `172.16.0.0/16 → pcx-xxxxx`.
6. VPC B Route Table → Add: `10.0.0.0/16 → pcx-xxxxx`.
7. Update Security Groups in both VPCs to allow traffic from the other VPC's CIDR.
8. Launch EC2 in each VPC → Ping private IPs → Works!

### Common Mistakes
- ❌ Expecting transitive peering — it doesn't work. Use Transit Gateway instead.
- ❌ Overlapping CIDRs — peering fails silently.
- ❌ Forgetting to accept the peering request on the other side.
- ❌ Forgetting to add routes in BOTH VPCs.
- ❌ Forgetting to update Security Groups to allow the peered VPC CIDR.

### Pro Tips
- ✅ For 3+ VPCs, use **AWS Transit Gateway** instead — hub-and-spoke, transitive routing.
- ✅ VPC Peering is free for same-Region. Cross-Region peering has data transfer costs.
- ✅ Use **peering with DNS resolution** — resolve private hostnames across VPCs.
- ✅ Reference peered VPC's SG IDs in your SG rules (same-Region peering only).
- ✅ In interviews: "We use VPC Peering for simple two-VPC connections and Transit Gateway for hub-and-spoke topologies with many VPCs. We ensure non-overlapping CIDRs and update route tables and security groups on both sides."

---

## 5. Hybrid Connectivity Basics

### What is it?
Hybrid connectivity connects your **on-premises data center** to your **AWS VPC**.
This lets your corporate network and AWS work as one seamless network.
Two main options: **VPN** (over internet) and **Direct Connect** (dedicated physical line).

### Why it matters in production
- Most enterprises don't move 100% to cloud overnight — they run hybrid for years.
- Some workloads must stay on-prem (legacy, compliance, data sovereignty).
- Hybrid is the #1 real-world enterprise architecture pattern.

### Core Ideas

| Feature | Site-to-Site VPN | AWS Direct Connect |
|---------|-----------------|-------------------|
| **Connection** | Over public internet (encrypted) | Dedicated physical fiber line |
| **Setup time** | Minutes | Weeks to months |
| **Bandwidth** | Limited by internet speed | 1 Gbps, 10 Gbps, or 100 Gbps |
| **Latency** | Variable (internet-dependent) | Low and consistent |
| **Cost** | Low (~$0.05/hr per connection) | Higher (port fee + data transfer) |
| **Encryption** | IPSec (built-in) | NOT encrypted by default (add VPN over DX) |
| **Redundancy** | Dual tunnels (automatic) | Need 2 connections for HA |
| **Use case** | Quick setup, backup, dev/test | Production, high-bandwidth, low-latency |

**Key components:**
- **Virtual Private Gateway (VGW)** — AWS-side endpoint for VPN/DX. Attached to VPC.
- **Customer Gateway (CGW)** — Your on-prem router/firewall device.
- **Site-to-Site VPN** — Encrypted tunnel over internet between VGW and CGW.
- **Direct Connect (DX)** — Physical cable from your data center to an AWS DX location.
- **Transit Gateway** — Central hub that connects VPCs, VPNs, and Direct Connect together.

### Quick Analogy
- **VPN** = Sending a secure, encrypted letter through regular mail (internet). Cheap, works anywhere, but speed depends on the postal service.
- **Direct Connect** = Building a private tunnel between your house and the office. Expensive to build, but once done, it's fast, private, and reliable.

### Architecture View
```
SITE-TO-SITE VPN:
┌─────────────┐         Internet          ┌─────────────┐
│ On-Premises │◄══════IPSec Tunnel══════►│ AWS VPC     │
│             │                           │             │
│ Customer GW │                           │ Virtual     │
│ (your router)│                          │ Private GW  │
└─────────────┘                           └─────────────┘

DIRECT CONNECT:
┌─────────────┐    Dedicated Fiber     ┌──────────┐    ┌─────────────┐
│ On-Premises │◄═══════════════════►  │ DX       │◄──►│ AWS VPC     │
│ Data Center │                       │ Location │    │             │
│             │                       │ (co-lo)  │    │ Virtual     │
│             │                       │          │    │ Private GW  │
└─────────────┘                       └──────────┘    └─────────────┘

BEST PRACTICE (DX + VPN backup):
┌─────────────┐    Direct Connect (primary, fast)     ┌─────────────┐
│ On-Premises │◄════════════════════════════════════►│ AWS VPC     │
│             │    Site-to-Site VPN (backup, slower)   │             │
│             │◄════════════════════════════════════►│             │
└─────────────┘                                       └─────────────┘

HUB-AND-SPOKE (Transit Gateway):
                    ┌──────────────────┐
                    │  Transit Gateway │
                    └────────┬─────────┘
                ┌────────────┼────────────┐
                │            │            │
          ┌─────▼───┐  ┌────▼────┐  ┌────▼────┐
          │ VPC A   │  │ VPC B   │  │ On-Prem │
          │ (Prod)  │  │ (Dev)   │  │  (VPN)  │
          └─────────┘  └─────────┘  └─────────┘
          All can talk to each other via TGW
```

### Hands-On (Step-by-step Lab)
**Site-to-Site VPN (you can simulate this):**
1. Create a **Virtual Private Gateway (VGW)** → Attach to your VPC.
2. Create a **Customer Gateway** → Enter your on-prem router's public IP.
3. Create a **Site-to-Site VPN Connection** → Between VGW and CGW.
4. Download the VPN configuration → Apply to your on-prem router.
5. Both tunnels come up → Update VPC route table: on-prem CIDR → VGW.
6. Test: Ping an on-prem server from an EC2 in the VPC.

**Direct Connect (conceptual — requires physical setup):**
1. Order a connection at an AWS Direct Connect location.
2. Work with your network provider to establish the fiber.
3. Create a **Virtual Interface (VIF)** — Private VIF for VPC, Public VIF for AWS public services.
4. Associate with VGW or Direct Connect Gateway.
5. Update routing and test connectivity.

### Common Mistakes
- ❌ Relying on VPN alone for production — internet can be slow/unreliable.
- ❌ Not setting up VPN as a backup for Direct Connect — DX can fail.
- ❌ Forgetting Direct Connect is NOT encrypted by default.
- ❌ Not using Transit Gateway when connecting 3+ VPCs to on-prem.
- ❌ Forgetting to update route tables after VPN/DX setup.

### Pro Tips
- ✅ **Production pattern:** Direct Connect (primary) + VPN (failover backup).
- ✅ Use **Transit Gateway** to centralize all connections — VPCs, VPN, DX.
- ✅ For DX encryption, run a **VPN over DX** (IPSec tunnel over the physical link).
- ✅ Use **DX Gateway** to connect one DX to VPCs in multiple Regions.
- ✅ Monitor VPN tunnel status with **CloudWatch** — get alerts when tunnels go down.
- ✅ In interviews: "Our hybrid architecture uses Direct Connect as the primary link for bandwidth and latency-sensitive workloads, with Site-to-Site VPN as automatic failover. Transit Gateway serves as the central hub connecting all VPCs and the on-premises network. We encrypt DX traffic using VPN over Direct Connect."
