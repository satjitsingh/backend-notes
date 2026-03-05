# RDS & Aurora

---

## 1. RDS (Relational Database Service)

### What is it?
RDS is a managed relational database service. AWS handles the heavy lifting — patching, backups, failover, and hardware.
You pick the database engine, instance size, and storage. AWS runs and maintains it.
Supported engines: **MySQL, PostgreSQL, MariaDB, Oracle, SQL Server, and Aurora**.

### Why it matters in production
- Almost every application needs a database. RDS is the go-to for relational data.
- Managing databases yourself (on EC2) is a massive operational burden.
- RDS eliminates 80% of DBA tasks — patching, backups, replication, failover.

### Core Ideas
- **Managed service (PaaS)** — AWS handles OS, engine patching, automated backups.
- **You still manage:** Schema design, queries, indexes, access control, parameter tuning.
- **Runs on EC2 under the hood** — but you don't have SSH access to the host.
- **Deployed in a VPC** — lives in private subnets (best practice).
- **DB Subnet Group** — You specify which subnets RDS can use (at least 2 AZs).
- **Storage:** EBS-backed. Options: gp3 (general), io2 (high perf), magnetic (legacy).
- **Storage Auto Scaling** — RDS auto-grows storage when running low.

**Key features:**
- **Automated Backups** — Daily snapshots + transaction logs. Retention: 0–35 days.
- **Manual Snapshots** — You trigger them. They persist until you delete them.
- **Point-in-Time Recovery** — Restore to any second within the retention period.
- **Encryption** — At rest (KMS) and in transit (SSL/TLS). Must enable at creation.
- **Parameter Groups** — Customize engine settings (max_connections, query_cache, etc.).
- **Option Groups** — Engine-specific features (Oracle TDE, SQL Server audit, etc.).
- **Enhanced Monitoring** — OS-level metrics (CPU, memory, disk I/O, processes).
- **Performance Insights** — Visual dashboard for query-level performance analysis.

### Quick Analogy
RDS = Hiring a property management company for your apartment building (database).
They handle plumbing (patching), electricity (backups), security guards (failover).
You decide who lives there (data), the layout (schema), and house rules (access policies).

### Architecture View
```
┌─────────────────────────────────────────────────────┐
│ VPC: 10.0.0.0/16                                    │
│                                                     │
│  ┌────────────────────────────────────────────┐     │
│  │ Private Subnets (DB Subnet Group)          │     │
│  │                                            │     │
│  │ AZ-1a               AZ-1b                  │     │
│  │ ┌──────────┐       ┌──────────┐            │     │
│  │ │   RDS    │       │  (empty) │            │     │
│  │ │ Primary  │       │ reserved │            │     │
│  │ │ MySQL    │       │ for      │            │     │
│  │ │ db.r5.lg │       │ Multi-AZ │            │     │
│  │ └──────────┘       └──────────┘            │     │
│  │                                            │     │
│  │ SG: Allow 3306 from app-sg ONLY            │     │
│  └────────────────────────────────────────────┘     │
│                                                     │
│  Config:                                            │
│  ├── Automated Backup: 7-day retention              │
│  ├── Encryption: KMS (aws/rds)                      │
│  ├── Storage: 100 GB gp3, Auto Scaling to 500 GB   │
│  ├── Enhanced Monitoring: 60s interval              │
│  └── Performance Insights: Enabled                  │
└─────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **RDS → Create Database**.
2. Engine: **MySQL**. Template: **Free Tier**.
3. DB Instance: `db.t3.micro`. Storage: 20 GB gp2.
4. VPC: Your custom VPC. Subnet Group: Private subnets.
5. **Public access: NO** (keep it private).
6. Security Group: Allow port 3306 from your app-sg only.
7. Enable **Automated Backups** (7-day retention).
8. Create → Wait ~5 minutes for "Available".
9. Connect from an EC2 in the same VPC: `mysql -h <rds-endpoint> -u admin -p`.
10. Create a table, insert data, verify.

### Common Mistakes
- ❌ Making RDS publicly accessible — databases should NEVER be public.
- ❌ Not enabling encryption at creation — you can't encrypt an existing unencrypted RDS.
- ❌ Setting backup retention to 0 — no automated backups, no point-in-time recovery.
- ❌ Using too small an instance and wondering why queries are slow.
- ❌ Not using a DB Subnet Group across 2+ AZs — required even for single-AZ RDS.

### Pro Tips
- ✅ Always deploy RDS in **private subnets** with strict Security Groups.
- ✅ Enable **deletion protection** for production databases.
- ✅ Enable **Performance Insights** — free for 7-day retention. Invaluable for debugging slow queries.
- ✅ Use **Secrets Manager** to store and auto-rotate database credentials.
- ✅ Use **RDS Proxy** for serverless/Lambda workloads — connection pooling, reduces DB load.
- ✅ In interviews: "We deploy RDS in private subnets with Multi-AZ for HA. Encryption is enabled at creation with KMS. Credentials are stored in Secrets Manager with auto-rotation. Performance Insights helps us identify slow queries."

---

## 2. Multi-AZ (High Availability)

### What is it?
Multi-AZ creates a **standby replica** of your RDS database in a different Availability Zone.
If the primary fails, AWS automatically fails over to the standby — typically in 60-120 seconds.
The standby is **synchronous** — every write to the primary is immediately replicated.

### Why it matters in production
- Without Multi-AZ, a single AZ failure takes your database down.
- Multi-AZ is the standard for any production database.
- Failover is automatic — no manual intervention needed.

### Core Ideas
- **Synchronous replication** — Standby has an exact copy of primary at all times.
- **Automatic failover** — AWS detects failure and switches the DNS endpoint to the standby.
- **Same endpoint** — Your application uses the same connection string. No code change needed.
- **Standby is NOT readable** — It's only for failover. You cannot query it.
- Failover triggers: AZ outage, instance failure, storage failure, OS patching, manual reboot with failover.
- **Doubles the cost** — You pay for the standby instance (same size as primary).
- Can be enabled **at creation** or **later** (causes brief downtime when enabling later).

**Multi-AZ vs Read Replica (critical comparison):**

| Feature | Multi-AZ | Read Replica |
|---------|---------|-------------|
| **Purpose** | High Availability | Read scalability |
| **Replication** | Synchronous | Asynchronous |
| **Readable?** | No (standby only) | Yes |
| **Failover** | Automatic | Manual (promote) |
| **Cross-Region** | No (same Region) | Yes |
| **Cost** | 2x primary | Additional instance cost |

### Quick Analogy
Multi-AZ = A backup goalkeeper sitting on the bench.
They mirror every move of the starting goalkeeper (synchronous). If the starter gets injured (primary fails), the backup immediately takes over (failover). The game (application) continues with minimal interruption.

### Architecture View
```
Normal Operation:
┌───────────────┐     Synchronous      ┌───────────────┐
│   AZ-1a       │     Replication      │   AZ-1b       │
│               │ ◄═══════════════════►│               │
│ ┌───────────┐ │                      │ ┌───────────┐ │
│ │  PRIMARY  │ │                      │ │  STANDBY  │ │
│ │  (active) │ │                      │ │  (idle)   │ │
│ │  Reads ✅  │ │                      │ │  Reads ❌  │ │
│ │  Writes ✅ │ │                      │ │  Writes ❌ │ │
│ └───────────┘ │                      │ └───────────┘ │
└───────────────┘                      └───────────────┘
        ▲
        │
  App connects to: mydb.xxxx.us-east-1.rds.amazonaws.com

During Failover (AZ-1a fails):
┌───────────────┐                      ┌───────────────┐
│   AZ-1a       │                      │   AZ-1b       │
│               │                      │               │
│ ┌───────────┐ │                      │ ┌───────────┐ │
│ │  PRIMARY  │ │                      │ │  STANDBY  │ │
│ │  FAILED ❌ │ │                      │ │ → PRIMARY │ │
│ │           │ │                      │ │  Reads ✅  │ │
│ └───────────┘ │                      │ │  Writes ✅ │ │
└───────────────┘                      │ └───────────┘ │
                                       └───────────────┘
        ▲                                      ▲
        │            DNS flip (~60s)           │
        └──────────────────────────────────────┘
  Same endpoint now points to new primary
```

### Hands-On (Step-by-step Lab)
1. Create an RDS MySQL instance → Enable **Multi-AZ**.
2. Note the endpoint — it doesn't change during failover.
3. Connect from an app and run queries.
4. Go to RDS → Select your instance → **Actions → Reboot → Check "Reboot with failover"**.
5. Watch: Connection drops for ~60-120 seconds → Reconnects automatically to new primary.
6. Check **Events** tab → See failover event logged.

### Common Mistakes
- ❌ Thinking Multi-AZ = Read Replica (it's NOT — standby is not readable).
- ❌ Not enabling Multi-AZ for production databases — single AZ = single point of failure.
- ❌ Expecting zero-downtime failover — there's a 60-120 second interruption.
- ❌ Not testing failover — do a manual failover test before going to production.
- ❌ Assuming Multi-AZ handles read scaling — it doesn't. Use Read Replicas for that.

### Pro Tips
- ✅ **Always enable Multi-AZ for production.** No exceptions.
- ✅ Test failover regularly — `aws rds reboot-db-instance --force-failover`.
- ✅ Your application should handle reconnection gracefully (connection retry logic).
- ✅ Multi-AZ failover is also triggered during **maintenance windows** — standby gets patched first, then failover, then old primary gets patched.
- ✅ In interviews: "We enable Multi-AZ on all production RDS instances for automatic failover. The standby replica uses synchronous replication, so zero data loss during failover. We test failover quarterly."

---

## 3. Read Replicas

### What is it?
A Read Replica is an **asynchronous copy** of your RDS database used for **read scaling**.
Your app sends writes to the primary and reads to the replica.
You can have up to **5 Read Replicas** per primary.

### Why it matters in production
- Most apps are read-heavy (80-90% reads, 10-20% writes).
- Read Replicas offload read traffic from the primary → primary handles writes better.
- They also enable cross-Region disaster recovery.

### Core Ideas
- **Asynchronous replication** — There's a small lag (milliseconds to seconds).
- **Readable** — Applications can query the replica for read operations.
- **Separate endpoint** — Each replica has its own DNS endpoint.
- **Cross-Region** — Replicas can be in a different Region (for DR and low-latency reads).
- **Promotion** — A replica can be promoted to a standalone database (manual, irreversible).
- **Replica of a replica** — Possible but adds latency. Not recommended.
- **No automatic failover** — If primary fails, you must manually promote a replica.

**Replication is free within the same Region.** Cross-Region replication has data transfer costs.

### Quick Analogy
Read Replica = Photocopies of a popular book in a library.
The original book (primary) is where new pages are added (writes). Multiple copies (replicas) are placed around the library so many people can read simultaneously without waiting for the original.

### Architecture View
```
Application
├── Writes ──▶ Primary DB (us-east-1a)
│               mydb.xxx.us-east-1.rds.amazonaws.com
│                    │              │
│              Async Replication    Async Replication
│                    │              │
│                    ▼              ▼
├── Reads ──▶ Replica 1 (us-east-1b)
│              mydb-replica1.xxx.us-east-1.rds.amazonaws.com
│
├── Reads ──▶ Replica 2 (us-east-1c)
│              mydb-replica2.xxx.us-east-1.rds.amazonaws.com
│
└── Reads ──▶ Replica 3 (eu-west-1)  ← Cross-Region for EU users
               mydb-replica3.xxx.eu-west-1.rds.amazonaws.com

DISASTER RECOVERY:
Primary fails → Promote Replica 3 → It becomes standalone primary
(Manual process — NOT automatic)
```

### Hands-On (Step-by-step Lab)
1. Start with an existing RDS instance.
2. Go to RDS → Select your instance → **Actions → Create Read Replica**.
3. Choose same Region or different Region.
4. Name: `mydb-replica`.
5. Wait for "Available" → Note the NEW endpoint.
6. Connect to the replica endpoint → Run SELECT queries → Works.
7. Try INSERT → **Fails** (replica is read-only).
8. Check **Replication Lag** in CloudWatch → `ReplicaLag` metric.

### Common Mistakes
- ❌ Sending writes to a Read Replica — it's read-only.
- ❌ Expecting automatic failover with Read Replicas — that's Multi-AZ, not replicas.
- ❌ Not monitoring replication lag — stale reads can cause bugs.
- ❌ Using Read Replicas as the only HA solution — you need Multi-AZ for failover.
- ❌ Forgetting each replica has a different endpoint — your app must know which to use.

### Pro Tips
- ✅ Use **RDS Proxy** to route reads to replicas automatically.
- ✅ Use Read Replicas for **reporting and analytics** — don't run heavy queries on the primary.
- ✅ Combine: **Multi-AZ (for HA) + Read Replicas (for scaling)**. They're not mutually exclusive.
- ✅ Cross-Region Read Replica = DR strategy + low-latency reads for global users.
- ✅ Monitor `ReplicaLag` metric — alert if it exceeds acceptable thresholds.
- ✅ In interviews: "We use Multi-AZ for HA and Read Replicas for read scaling. Heavy analytics queries go to a dedicated replica. Cross-Region replicas serve global users and act as DR targets."

---

## 4. Amazon Aurora

### What is it?
Aurora is AWS's cloud-native relational database engine. It's MySQL and PostgreSQL compatible.
It's designed for the cloud — up to **5x faster than MySQL** and **3x faster than PostgreSQL**.
It separates compute from storage and auto-replicates data across **6 copies in 3 AZs**.

### Why it matters in production
- Aurora is the recommended choice for new relational workloads on AWS.
- It offers better performance, durability, and availability than standard RDS.
- Aurora Serverless removes capacity planning entirely.

### Core Ideas
- **Compatible with MySQL 5.7/8.0 and PostgreSQL** — your existing app code works.
- **Storage:** Auto-scales from 10 GB to 128 TB. You never provision storage.
- **6 copies of data across 3 AZs** — survives loss of 2 copies for writes, 3 for reads.
- **Self-healing storage** — Automatically detects and repairs corrupt data blocks.
- **Up to 15 Read Replicas** (RDS MySQL only supports 5).
- **Failover:** Automatic in ~30 seconds (faster than RDS Multi-AZ).
- **Writer + Reader endpoints:** Aurora provides cluster-level endpoints for routing.

**Aurora endpoints:**

| Endpoint | Purpose |
|----------|---------|
| **Cluster (Writer) endpoint** | Points to the primary instance. For writes. |
| **Reader endpoint** | Load-balanced across all replicas. For reads. |
| **Instance endpoints** | Direct connection to a specific instance. |

**Aurora editions:**

| Feature | Aurora Standard | Aurora Serverless v2 |
|---------|----------------|---------------------|
| **Capacity** | Fixed instance size | Auto-scales (0.5–128 ACU) |
| **Pricing** | Per instance-hour | Per ACU-hour consumed |
| **Use case** | Predictable workloads | Variable/unpredictable workloads |
| **Scale to zero** | No | Yes (with min 0.5 ACU) |

### Quick Analogy
Standard RDS = A regular car (reliable, does the job).
Aurora = A Formula 1 car built specifically for the racetrack (cloud). Same driver's license (MySQL/PostgreSQL compatible), but engineered for speed, durability, and performance from the ground up.

### Architecture View
```
Aurora Cluster
┌───────────────────────────────────────────────────────────┐
│                                                           │
│  Writer Endpoint ──▶ ┌──────────────┐                     │
│  (writes)            │  Primary     │                     │
│                      │  Instance    │                     │
│                      │  (AZ-1a)    │                     │
│                      └──────┬───────┘                     │
│                             │                             │
│  Reader Endpoint ──▶ ┌──────┼───────────────────┐         │
│  (reads, LB'd)       │     │                   │         │
│                 ┌─────▼──┐ ┌▼────────┐ ┌───────▼──┐      │
│                 │Replica │ │Replica  │ │Replica   │      │
│                 │  #1    │ │  #2     │ │  #3      │      │
│                 │(AZ-1a) │ │(AZ-1b) │ │(AZ-1c)  │      │
│                 └────────┘ └────────┘ └──────────┘      │
│                                                           │
│  Shared Storage Layer (auto-scales to 128 TB)             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  6 copies across 3 AZs                              │  │
│  │  [Copy1][Copy2] [Copy3][Copy4] [Copy5][Copy6]       │  │
│  │   AZ-1a          AZ-1b          AZ-1c               │  │
│  │  Self-healing • Continuous backup to S3              │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘

Failover (~30 seconds):
Primary fails → Aurora promotes a replica to primary
               → Writer endpoint auto-updates
               → App reconnects automatically
```

### Hands-On (Step-by-step Lab)
1. Go to **RDS → Create Database → Amazon Aurora**.
2. Engine: Aurora MySQL-Compatible.
3. Template: Production. Instance: `db.r5.large`.
4. Create **1 writer + 2 readers**.
5. Note the **Cluster endpoint** (writer) and **Reader endpoint**.
6. Connect your app: writes → cluster endpoint, reads → reader endpoint.
7. Reboot the writer with failover → Watch a replica get promoted in ~30 seconds.
8. Check: Reader endpoint automatically excludes the old failed instance.

### Common Mistakes
- ❌ Using standard RDS when Aurora would be more cost-effective at scale.
- ❌ Not using Reader endpoint for reads — sending all queries to the writer.
- ❌ Confusing Aurora with standard RDS — Aurora has different storage architecture.
- ❌ Not leveraging Aurora Serverless v2 for dev/test or variable workloads.
- ❌ Forgetting that Aurora is more expensive per hour than standard RDS — worth it at scale but not for small workloads.

### Pro Tips
- ✅ Use **Aurora** for production. Use standard RDS only for Free Tier or very small workloads.
- ✅ Use **Aurora Serverless v2** for dev/test (scales to near-zero, saves money).
- ✅ Use **Aurora Global Database** for cross-Region DR (replication lag < 1 second).
- ✅ Enable **Backtrack** (Aurora MySQL) — rewind the database to any point without restoring from backup.
- ✅ Use **Aurora Cloning** for creating instant test environments (no data copy — copy-on-write).
- ✅ In interviews: "We use Aurora for all production relational workloads. 6-copy storage across 3 AZs gives us durability. Failover happens in 30 seconds. We use the reader endpoint for read scaling and Aurora Serverless v2 for dev/test to minimize costs. Aurora Global Database handles our cross-Region DR."
