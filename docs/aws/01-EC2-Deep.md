# EC2 (Elastic Compute Cloud) Deep Dive

---

## 1. EC2 (Elastic Compute Cloud)

### What is it?
EC2 gives you virtual servers (called instances) in the cloud.
You pick the OS, CPU, RAM, storage, and network. You pay per second.
It's the most fundamental AWS compute service — everything builds on it.

### Why it matters in production
- EC2 runs the majority of workloads in AWS.
- Every DevOps engineer must know how to launch, configure, secure, and scale EC2.
- It's the backbone behind ECS, EKS, Auto Scaling, and many managed services.

### Core Ideas
- **Instance** = A virtual server running in AWS.
- **AMI** = The template used to launch an instance (OS + software).
- **Instance Type** = The hardware configuration (CPU, RAM, network).
- **Security Group** = Virtual firewall controlling inbound/outbound traffic.
- **Key Pair** = SSH key for logging into Linux instances.
- **User Data** = Script that runs on first boot (bootstrap).
- **EBS** = The hard drive attached to your instance (persistent storage).
- **Instance Store** = Temporary storage physically on the host (lost on stop/terminate).
- **Elastic IP** = Static public IP that persists across stop/start.
- **Instance Metadata** = Info about the instance accessible at `http://169.254.169.254`.

### Quick Analogy
EC2 = Renting a computer in a data center.
You choose the specs, install your software, and connect remotely. When done, you return it and stop paying.

### Architecture View
```
User
  │
  ▼
Internet ──▶ Security Group (firewall)
                  │
                  ▼
          ┌──────────────────┐
          │   EC2 Instance   │
          │  ┌─────────────┐ │
          │  │ OS (from AMI)│ │
          │  │ Your App     │ │
          │  │ User Data    │ │
          │  └─────────────┘ │
          │  Storage:        │
          │  ├── EBS (root)  │
          │  └── EBS (data)  │
          └──────────────────┘
                  │
          Subnet (in a VPC, in an AZ)
```

### Hands-On (Step-by-step Lab)
1. Go to **EC2 → Launch Instance**.
2. Name: `my-first-server`.
3. AMI: **Amazon Linux 2023** (Free Tier).
4. Instance type: `t2.micro` (Free Tier).
5. Key pair: Create new → Download `.pem` file.
6. Security group: Allow SSH (port 22) from your IP only.
7. Launch → Wait for "Running" status.
8. Connect: `ssh -i my-key.pem ec2-user@<public-ip>`.
9. Run `whoami` → You're on a cloud server.
10. **Terminate** when done.

### Common Mistakes
- ❌ Leaving SSH open to `0.0.0.0/0` — anyone can try to brute-force in.
- ❌ Losing the key pair `.pem` file — you can't SSH in without it.
- ❌ Not terminating instances — charges pile up.
- ❌ Using `t2.micro` for production — it's for testing only.
- ❌ Not attaching an IAM Role — using access keys instead.

### Pro Tips
- ✅ Always use **IAM Roles** (instance profiles) instead of access keys on EC2.
- ✅ Use **Session Manager (SSM)** instead of SSH — no key pairs needed, fully audited.
- ✅ Enable **detailed monitoring** (1-minute CloudWatch metrics) for production.
- ✅ Use **IMDSv2** (Instance Metadata Service v2) — more secure than v1.
- ✅ In interviews: "We use SSM Session Manager for access, IAM roles for credentials, IMDSv2 for metadata, and never open SSH to the internet."

---

## 2. Instance Types

### What is it?
Instance types define the hardware of your EC2 instance — CPU, RAM, network, and storage.
AWS has hundreds of instance types grouped into families.
Each family is optimized for a specific workload type.

### Why it matters in production
- Choosing the wrong instance type wastes money or starves your app of resources.
- Interview question: "How do you choose the right instance type?"
- Right-sizing is a core cost optimization skill.

### Core Ideas

**Instance type naming:**
```
m5.xlarge
│ │  │
│ │  └── Size (nano, micro, small, medium, large, xlarge, 2xlarge...)
│ └── Generation (5 = 5th gen, higher = newer & better)
│
└── Family (m = general purpose)
```

**Key families to remember:**

| Family | Optimized For | Use Case | Example |
|--------|--------------|----------|---------|
| **T** (T3, T3a) | Burstable | Dev/test, small web apps | `t3.micro` |
| **M** (M5, M6i) | General purpose | Web servers, app servers | `m5.xlarge` |
| **C** (C5, C6i) | Compute | CPU-heavy: batch, ML inference, gaming | `c5.2xlarge` |
| **R** (R5, R6i) | Memory | Databases, in-memory caches | `r5.large` |
| **I** (I3) | Storage I/O | NoSQL DBs, data warehousing | `i3.xlarge` |
| **G/P** (G4, P4) | GPU | ML training, video encoding, graphics | `p4d.24xlarge` |
| **D** (D2) | Dense storage | Hadoop, distributed file systems | `d2.xlarge` |

**T-series burstable model:**
- T instances earn **CPU credits** when idle.
- Credits are spent when CPU bursts above baseline.
- `T3 Unlimited` — Pay extra per vCPU-hour if credits run out (no throttling).
- Good for variable workloads. Bad for sustained high CPU.

### Quick Analogy
Instance types = Car models.
- T3 = City car (cheap, fine for light use, struggles on highways).
- M5 = Sedan (balanced, good for most things).
- C5 = Sports car (fast engine, for speed).
- R5 = Minivan (lots of space/memory for passengers/data).
- P4 = Truck with special equipment (GPU for heavy lifting).

### Architecture View
```
Workload Analysis → Instance Type Selection

Web server (balanced)          → m5.large (4 vCPU, 16 GB RAM)
Batch processing (CPU heavy)   → c5.2xlarge (8 vCPU, 16 GB RAM)
Redis cache (memory heavy)     → r5.large (2 vCPU, 16 GB RAM)
Dev/test (light, bursty)       → t3.small (2 vCPU, 2 GB RAM)
ML training (GPU needed)       → p4d.24xlarge (8 GPUs, 96 vCPU)
```

### Hands-On (Step-by-step Lab)
1. Go to [instances.vantage.sh](https://instances.vantage.sh) → Browse all instance types with pricing.
2. Launch a `t3.micro` → Run `htop` to see CPU/RAM.
3. Check CPU credits: Go to **CloudWatch → EC2 Metrics → CPUCreditBalance**.
4. Stress test: `sudo yum install stress -y && stress --cpu 2 --timeout 300`.
5. Watch CPU credits drain in CloudWatch.
6. Go to **AWS Compute Optimizer** → See right-sizing recommendations.

### Common Mistakes
- ❌ Picking the biggest instance "just to be safe" — massive waste.
- ❌ Using T-series for sustained high CPU workloads — credits run out.
- ❌ Not using the latest generation (M5 is cheaper and faster than M4).
- ❌ Ignoring network performance — some types have 10 Gbps, others have 25 Gbps.

### Pro Tips
- ✅ Start with **M-series** (general purpose), then optimize after monitoring.
- ✅ Use **Compute Optimizer** to get ML-based right-sizing suggestions.
- ✅ `a` suffix = AMD processors (cheaper). Example: `m5a.large`.
- ✅ `g` suffix = Graviton (ARM) processors. Up to 40% better price-performance.
- ✅ In interviews: "We start with general purpose M-series, monitor with CloudWatch, then right-size using Compute Optimizer. We use Graviton instances for 40% cost savings where compatible."

---

## 3. AMI (Amazon Machine Image)

### What is it?
An AMI is a template to launch an EC2 instance.
It contains the operating system, pre-installed software, and configurations.
Think of it as a snapshot of a ready-to-go server that you can reuse.

### Why it matters in production
- Custom AMIs let you launch pre-configured servers in seconds.
- They ensure consistency — every server starts exactly the same.
- AMIs are core to immutable infrastructure and golden image patterns.

### Core Ideas
- AMI includes: **Root volume** (OS) + **launch permissions** + **block device mapping**.
- **AWS Managed AMIs** — Amazon Linux, Ubuntu, Windows (maintained by AWS/vendors).
- **Custom AMIs** — You create them from your configured EC2 instance.
- **AMI is Region-specific** — Copy it to other Regions if needed.
- AMIs can be **private** (your account), **shared** (specific accounts), or **public**.
- AMIs are stored in **S3** behind the scenes (you don't see this).

**AMI types by root storage:**
- **EBS-backed** — Root volume is an EBS snapshot. Instance can be stopped. Most common.
- **Instance Store-backed** — Root on ephemeral storage. Cannot be stopped. Rarely used.

### Quick Analogy
AMI = A master copy of a USB boot drive.
You set up one perfect USB with your OS and tools. Then you clone it every time you need a new machine. Every machine starts identical.

### Architecture View
```
Create AMI flow:
  EC2 (configured) ──▶ Create Image ──▶ AMI (stored in S3)
                                            │
Launch from AMI:                            │
  AMI ──▶ Launch ──▶ New EC2 (exact copy)   │
  AMI ──▶ Launch ──▶ New EC2 (exact copy)   │
  AMI ──▶ Launch ──▶ New EC2 (exact copy)   │

Cross-Region:
  AMI (us-east-1) ──Copy──▶ AMI (ap-south-1) ──▶ Launch in Mumbai
```

### Hands-On (Step-by-step Lab)
1. Launch a `t3.micro` with Amazon Linux.
2. SSH in → Install Apache: `sudo yum install httpd -y && sudo systemctl start httpd`.
3. Create a simple webpage: `echo "Hello from AMI" | sudo tee /var/www/html/index.html`.
4. Go to **EC2 → Actions → Image and Templates → Create Image**.
5. Name: `my-web-server-ami`.
6. Wait for AMI to be "Available".
7. Launch a new instance from this AMI → Apache is already running with your webpage.

### Common Mistakes
- ❌ Not updating AMIs regularly — old AMIs have unpatched vulnerabilities.
- ❌ Including secrets/keys in AMIs — they get baked into every instance.
- ❌ Forgetting AMIs are regional — won't appear in other Regions.
- ❌ Not deregistering old AMIs and deleting their snapshots — storage costs.

### Pro Tips
- ✅ Use **AMI pipelines** with EC2 Image Builder — auto-build, test, and distribute AMIs.
- ✅ Use **Packer** (by HashiCorp) for AMI automation in CI/CD.
- ✅ Tag AMIs with build date and version.
- ✅ Golden AMI pattern: Harden OS + install agents + bake AMI → Use in Auto Scaling.
- ✅ In interviews: "We use EC2 Image Builder to create hardened, tested AMIs weekly. Auto Scaling Groups use the latest AMI. Old AMIs are deregistered automatically."

---

## 4. Security Groups

### What is it?
A Security Group is a virtual firewall that controls traffic to/from your EC2 instance.
It works at the **instance level**. You define rules for what traffic is allowed in and out.
By default: **all inbound is blocked, all outbound is allowed.**

### Why it matters in production
- Security Groups are the first line of defense for your instances.
- Misconfigured SGs are one of the top causes of breaches.
- Every EC2, RDS, Lambda (in VPC), and ELB uses Security Groups.

### Core Ideas
- **Stateful** — If you allow inbound traffic, the response is automatically allowed out (and vice versa).
- **Allow rules ONLY** — You can't create Deny rules in SGs. Use NACLs for deny.
- **Multiple SGs** — An instance can have up to 5 Security Groups attached.
- **Rules are evaluated together** — All rules across all SGs are combined (union).
- **Reference other SGs** — Instead of IP addresses, you can allow traffic from another SG.

**Inbound vs Outbound:**

| Direction | Default | You Typically Add |
|-----------|---------|-------------------|
| **Inbound** | All DENIED | SSH (22), HTTP (80), HTTPS (443), App ports |
| **Outbound** | All ALLOWED | Usually keep default (allow all out) |

### Quick Analogy
Security Group = A guest list at a private event.
Only people on the list (allowed rules) can enter. Everyone else is turned away.
Once inside, they can freely leave (stateful — return traffic allowed).

### Architecture View
```
Internet
    │
    ▼
┌─────────────────────────────────┐
│ Security Group: web-sg          │
│                                 │
│ Inbound Rules:                  │
│   ✅ Port 80 (HTTP) from 0.0.0.0/0    │
│   ✅ Port 443 (HTTPS) from 0.0.0.0/0  │
│   ✅ Port 22 (SSH) from 10.0.0.0/16   │
│                                 │
│ Outbound Rules:                 │
│   ✅ All traffic to 0.0.0.0/0   │
│                                 │
│  ┌────────────┐                 │
│  │ EC2 Instance│                │
│  └────────────┘                 │
└─────────────────────────────────┘
```

**SG chaining (best practice):**
```
ALB-SG  ──allows port 80──▶  App-SG  ──allows port 5432──▶  DB-SG
(public)                    (private)                      (private)

App-SG inbound: Allow port 8080 from ALB-SG (reference by SG ID)
DB-SG inbound:  Allow port 5432 from App-SG (reference by SG ID)
```

### Hands-On (Step-by-step Lab)
1. Launch an EC2 with a new Security Group.
2. Add inbound rule: **HTTP (80) from Anywhere** + **SSH (22) from My IP**.
3. SSH in → Install Apache → Verify webpage loads from browser.
4. Remove the HTTP rule → Refresh browser → **Connection times out**.
5. Add it back → Works again.
6. Create a second SG for a database → Allow port 3306 only from the web server's SG ID.

### Common Mistakes
- ❌ Opening SSH (22) to `0.0.0.0/0` — bots will find it in minutes.
- ❌ Opening all ports (`0-65535`) — no security at all.
- ❌ Using IP addresses when you should reference SG IDs (SG chaining).
- ❌ Forgetting SGs are stateful — no need to open return ports.
- ❌ Confusing SGs (stateful, instance-level) with NACLs (stateless, subnet-level).

### Pro Tips
- ✅ Use **SG chaining** — ALB-SG → App-SG → DB-SG. Lock down each layer.
- ✅ Use **prefix lists** for common IP ranges (like your office IPs).
- ✅ Never open SSH to the internet. Use **SSM Session Manager** instead.
- ✅ Tag SGs clearly: `web-sg`, `app-sg`, `db-sg`, `bastion-sg`.
- ✅ In interviews: "We chain Security Groups so the database only accepts traffic from the app tier SG, and the app tier only accepts from the ALB SG. No direct internet access to backend or database."

---

## 5. Key Pairs

### What is it?
A Key Pair is an SSH key (public + private) used to securely connect to Linux EC2 instances.
AWS stores the public key on the instance. You keep the private key (`.pem` file).
Without the private key, you cannot SSH into the instance.

### Why it matters in production
- Key pairs are how you initially access EC2 instances.
- Losing the key = losing access (unless you have SSM or a backup plan).
- Proper key management is a basic security requirement.

### Core Ideas
- **Public key** → Stored on the EC2 instance (in `~/.ssh/authorized_keys`).
- **Private key** → Downloaded once when created. AWS does NOT keep a copy.
- You can create key pairs in AWS or **import your own** public key.
- One key pair can be used for multiple instances.
- **Windows instances** use key pairs to decrypt the admin password (not for SSH).
- **ED25519** and **RSA** key types are supported.

### Quick Analogy
Key Pair = A house key and lock.
AWS installs the lock (public key) on the server. You keep the key (private key). Only your key opens that lock.

### Architecture View
```
Your laptop                    EC2 Instance
┌──────────────┐              ┌──────────────────────┐
│ Private key  │──SSH──▶      │ Public key stored in │
│ (my-key.pem) │              │ ~/.ssh/authorized_keys│
└──────────────┘              └──────────────────────┘

ssh -i my-key.pem ec2-user@<public-ip>
```

### Hands-On (Step-by-step Lab)
1. Go to **EC2 → Key Pairs → Create Key Pair**.
2. Name: `my-dev-key`. Type: RSA. Format: `.pem` (Linux/Mac) or `.ppk` (PuTTY/Windows).
3. Download the `.pem` file — store it safely.
4. Set permissions: `chmod 400 my-dev-key.pem`.
5. Launch an EC2 instance using this key pair.
6. Connect: `ssh -i my-dev-key.pem ec2-user@<public-ip>`.

### Common Mistakes
- ❌ Losing the `.pem` file — no way to recover it from AWS.
- ❌ Not setting `chmod 400` — SSH refuses keys with open permissions.
- ❌ Sharing one key pair across the entire team — no accountability.
- ❌ Using key pairs as the only access method — what if you lose it?

### Pro Tips
- ✅ **Prefer SSM Session Manager** over SSH key pairs — no ports to open, fully audited.
- ✅ Store key pairs in a **secrets manager** or vault, not on desktops.
- ✅ Use **ec2-instance-connect** for temporary SSH access without managing keys.
- ✅ Rotate keys regularly. Use configuration management (Ansible) to distribute new public keys.
- ✅ In interviews: "We use SSM Session Manager as our primary access method. No SSH ports open, no key pairs to manage, and every session is logged in CloudTrail."

---

## 6. User Data

### What is it?
User Data is a script that runs automatically when an EC2 instance launches for the first time.
It's used to bootstrap the instance — install software, configure settings, start services.
The script runs as `root` and executes only on the **first boot** (by default).

### Why it matters in production
- User Data automates instance setup — no manual SSH needed.
- It's essential for Auto Scaling — new instances must self-configure.
- Combined with AMIs, it creates fully automated server deployments.

### Core Ideas
- Passed as **bash script** (Linux) or **PowerShell** (Windows).
- Runs once on first boot (can be changed to run on every boot with cloud-init config).
- Maximum size: **16 KB**.
- Executed as `root` — no need for `sudo`.
- Logs are at `/var/log/cloud-init-output.log` (Linux).
- User Data is **not encrypted** — don't put secrets in it. Use SSM Parameter Store or Secrets Manager.
- Can be viewed via instance metadata: `http://169.254.169.254/latest/user-data`.

### Quick Analogy
User Data = A setup checklist you leave for the new office IT guy.
"When the new computer arrives: install Chrome, set up email, and configure the VPN."
The checklist runs automatically on the first day.

### Architecture View
```
AMI (base OS)
    +
User Data (bootstrap script)
    =
Fully configured instance on first boot

Example User Data:
┌──────────────────────────────────────┐
│ #!/bin/bash                          │
│ yum update -y                        │
│ yum install -y httpd                 │
│ systemctl start httpd                │
│ systemctl enable httpd               │
│ echo "Hello World" > /var/www/html/  │
│                       index.html     │
└──────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **EC2 → Launch Instance**.
2. Scroll to **Advanced Details → User Data**.
3. Paste this script:
```bash
#!/bin/bash
yum update -y
yum install -y httpd
systemctl start httpd
systemctl enable httpd
echo "<h1>Bootstrapped via User Data!</h1>" > /var/www/html/index.html
```
4. Launch the instance with HTTP (80) allowed in the Security Group.
5. Wait 2 minutes → Open `http://<public-ip>` → See your page.
6. SSH in → Check logs: `cat /var/log/cloud-init-output.log`.

### Common Mistakes
- ❌ Putting secrets (passwords, API keys) in User Data — it's visible in metadata.
- ❌ Forgetting `#!/bin/bash` at the top — script won't execute.
- ❌ Script exceeding 16 KB — it will be truncated.
- ❌ Assuming User Data runs on every reboot — it runs only on first boot by default.
- ❌ Not checking cloud-init logs when the script fails silently.

### Pro Tips
- ✅ Keep User Data small — use it to pull a larger script from S3.
- ✅ Fetch secrets at runtime from **SSM Parameter Store** or **Secrets Manager**.
- ✅ Combine **Golden AMI + minimal User Data** = Fast, consistent launches.
- ✅ Use **cfn-signal** in CloudFormation to confirm User Data completed successfully.
- ✅ In interviews: "We bake common software into a Golden AMI and use User Data only for environment-specific configuration. Secrets are fetched from SSM Parameter Store at boot time, never hardcoded."

---

## 7. Placement Groups

### What is it?
Placement Groups let you control how EC2 instances are physically placed on AWS hardware.
You choose between performance (close together) or resilience (spread apart).
There are three types: Cluster, Spread, and Partition.

### Why it matters in production
- Some workloads need ultra-low latency between instances (HPC, big data).
- Some workloads need guaranteed isolation (critical apps that can't fail together).
- Placement groups give you this control.

### Core Ideas

| Type | Placement | Best For | Limit |
|------|-----------|----------|-------|
| **Cluster** | All instances on same rack/hardware | HPC, low-latency networking, big data | One AZ only |
| **Spread** | Each instance on different hardware | Critical apps needing max isolation | 7 instances per AZ |
| **Partition** | Groups of instances on separate racks | HDFS, HBase, Cassandra, Kafka | Up to 7 partitions per AZ |

- **Cluster** — Maximum network performance (10-25 Gbps between instances). Risk: if rack fails, all go down.
- **Spread** — Maximum fault isolation. Each instance on physically separate hardware.
- **Partition** — Middle ground. Groups (partitions) on separate racks. Instances in same partition share hardware.

### Quick Analogy
- **Cluster** = All teammates sitting at the same table. Great communication. But if the table breaks, everyone falls.
- **Spread** = Each teammate in a separate room. One room flooding doesn't affect others.
- **Partition** = Teams in separate wings of the building. One wing has issues, other wings are fine.

### Architecture View
```
Cluster Placement Group:
┌─────────────────────────────┐
│ Same Rack / Same AZ        │
│ [EC2] [EC2] [EC2] [EC2]    │
│ Ultra-low latency between   │
└─────────────────────────────┘

Spread Placement Group:
┌──────┐  ┌──────┐  ┌──────┐
│Rack 1│  │Rack 2│  │Rack 3│
│[EC2] │  │[EC2] │  │[EC2] │
└──────┘  └──────┘  └──────┘
  AZ-1a     AZ-1b     AZ-1c

Partition Placement Group:
┌──────────┐  ┌──────────┐  ┌──────────┐
│Partition 1│  │Partition 2│  │Partition 3│
│[EC2][EC2] │  │[EC2][EC2] │  │[EC2][EC2] │
│ Rack A    │  │ Rack B    │  │ Rack C    │
└──────────┘  └──────────┘  └──────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **EC2 → Placement Groups → Create**.
2. Create a **Spread** placement group named `critical-spread`.
3. Launch 3 instances in that placement group across 3 AZs.
4. Verify: Each instance is on separate hardware (check placement info in details).
5. Create a **Cluster** placement group → Launch instances in it → Test network performance with `iperf3`.

### Common Mistakes
- ❌ Using Cluster placement group across AZs — it only works in a single AZ.
- ❌ Exceeding 7 instances per AZ in Spread — hard limit.
- ❌ Choosing Cluster for HA — it's the opposite (all on same rack).
- ❌ Adding instances with different instance types to Cluster — may cause capacity issues.

### Pro Tips
- ✅ Use **Cluster** for HPC, genomics, machine learning training with distributed GPU.
- ✅ Use **Spread** for critical stateful services (master nodes, ZooKeeper, etcd).
- ✅ Use **Partition** for distributed databases (Cassandra, Kafka, HDFS).
- ✅ Use **Enhanced Networking (ENA)** with Cluster placement for best performance.
- ✅ In interviews: "We use Spread placement for critical control plane nodes to ensure hardware-level isolation. For big data processing, we use Partition placement to align with HDFS rack awareness."
