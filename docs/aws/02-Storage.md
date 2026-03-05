# Storage (S3, EBS, EFS, Instance Store)

---

## 1. S3 (Simple Storage Service) — Deep Dive

### What is it?
S3 is an object storage service. You store files (called objects) in buckets.
It's serverless, infinitely scalable, and 99.999999999% durable (11 nines).
S3 is one of the oldest and most used AWS services.

### Why it matters in production
- Static website hosting, backups, data lakes, log storage, artifact storage.
- Almost every AWS architecture uses S3 somewhere.
- S3 knowledge is tested in every AWS interview.

### Core Ideas
- **Bucket** — A container for objects. Name must be globally unique.
- **Object** — A file + metadata. Max size: 5 TB. Key = file path.
- **Flat structure** — There are no real folders. The `/` in keys is just a prefix.
- **Consistency** — Strong read-after-write consistency (since Dec 2020).
- **Object URL** — `https://bucket-name.s3.amazonaws.com/key`.

**Key features:**
- **Versioning** — Keep all versions of an object.
- **Encryption** — SSE-S3 (AWS managed), SSE-KMS (your key), SSE-C (customer provided).
- **Access control** — Bucket policies, ACLs, IAM policies, Block Public Access.
- **Event notifications** — Trigger Lambda, SQS, SNS on upload/delete.
- **Multipart upload** — Required for files > 5 GB. Recommended for > 100 MB.
- **Transfer Acceleration** — Uses CloudFront edge locations for faster uploads.
- **S3 Select / Athena** — Query data inside S3 without downloading.

### Quick Analogy
S3 = An infinite filing cabinet in the cloud.
Each drawer is a bucket. Each file is an object. You can label, lock, and version every file. The cabinet never runs out of space.

### Architecture View
```
┌─────────────────────────────────────────┐
│ S3 Bucket: my-app-assets                │
│                                         │
│ Objects:                                │
│   images/logo.png          (50 KB)      │
│   css/style.css            (12 KB)      │
│   backups/db-2024-01.sql   (2 GB)       │
│   logs/app-2024-01-15.log  (500 MB)     │
│                                         │
│ Config:                                 │
│   Versioning: Enabled                   │
│   Encryption: SSE-S3                    │
│   Lifecycle: → IA after 30d → Glacier   │
│   Block Public Access: ON               │
│                                         │
│ Events:                                 │
│   On PUT → Trigger Lambda (thumbnail)   │
│   On DELETE → Notify SNS               │
└─────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **S3 → Create Bucket** → Name: `my-test-bucket-<random>`.
2. Keep **Block Public Access** enabled (default — safe).
3. Enable **Versioning**.
4. Enable **Server-side encryption** (SSE-S3).
5. Upload a file → Download it → Verify.
6. Upload the same file again → Go to "Show versions" → See both versions.
7. Delete the file → It adds a "delete marker" → Toggle versions to see it's still there.
8. Enable a **Lifecycle rule**: Move to IA after 30 days.

### Common Mistakes
- ❌ Making buckets public accidentally — always keep Block Public Access ON.
- ❌ Not enabling versioning — accidental deletes are permanent.
- ❌ Not enabling encryption — compliance failure.
- ❌ Using S3 for frequent, small random reads — use EFS or DynamoDB instead.
- ❌ Uploading large files without multipart upload — slow and unreliable.

### Pro Tips
- ✅ Enable **S3 Block Public Access** at the account level — not just bucket level.
- ✅ Use **S3 Event Notifications** + Lambda for image processing, log parsing.
- ✅ Use **S3 Access Points** for managing access to shared datasets.
- ✅ Enable **S3 Object Lock** for compliance (WORM — Write Once Read Many).
- ✅ In interviews: "We use S3 with versioning, SSE-KMS encryption, lifecycle policies for cost optimization, and Block Public Access enforced at the account level. Event notifications trigger Lambda for automated processing."

---

## 2. S3 Storage Classes

### What is it?
S3 offers different storage classes with different cost and access characteristics.
Frequently accessed data goes in Standard. Rarely accessed data goes in Glacier.
Choosing the right class saves significant money.

### Why it matters in production
- Storage costs are a major part of AWS bills.
- Most data becomes cold over time — don't pay hot prices for cold data.
- Lifecycle policies automate the transitions.

### Core Ideas

| Storage Class | Access | Min Storage | Retrieval Cost | Use Case |
|--------------|--------|-------------|---------------|----------|
| **S3 Standard** | Instant | None | None | Active data, web assets |
| **S3 Intelligent-Tiering** | Instant | 30 days | None (auto-moves) | Unknown access patterns |
| **S3 Standard-IA** | Instant | 30 days | Per GB retrieved | Backups, DR |
| **S3 One Zone-IA** | Instant | 30 days | Per GB retrieved | Re-creatable data |
| **S3 Glacier Instant** | Instant | 90 days | Per GB retrieved | Archive needing instant access |
| **S3 Glacier Flexible** | Minutes to hours | 90 days | Per GB + per request | Long-term archive |
| **S3 Glacier Deep Archive** | 12–48 hours | 180 days | Per GB + per request | Compliance, 7+ year retention |

**Key points:**
- **Standard** — Default. 3 AZ replication. No retrieval fees.
- **IA (Infrequent Access)** — Cheaper storage, but you pay per retrieval.
- **One Zone-IA** — 20% cheaper than Standard-IA. Only 1 AZ (less durable).
- **Intelligent-Tiering** — Auto-moves data between tiers based on access. Small monthly monitoring fee.
- **Glacier** — Archive storage. Retrieval takes minutes to hours.
- **Deep Archive** — Cheapest. Retrieval takes 12–48 hours.

### Quick Analogy
- **Standard** = Desk drawer (instant access, prime location, costs more).
- **IA** = Filing cabinet across the room (takes a moment, cheaper).
- **Glacier** = Storage room in the basement (takes time to fetch).
- **Deep Archive** = Off-site warehouse (takes a day to retrieve).

### Architecture View
```
Data Lifecycle:
                    Day 0          Day 30         Day 90          Day 365
                      │               │              │               │
Upload ──▶  [S3 Standard] ──▶ [S3 Standard-IA] ──▶ [Glacier] ──▶ [Deep Archive]
                  $$$$              $$$              $$               $

Cost decreases ──────────────────────────────────────────────────────▶
Retrieval time increases ────────────────────────────────────────────▶
```

### Hands-On (Step-by-step Lab)
1. Upload a file to S3 → Note it's in **Standard** class.
2. Select the object → Change storage class to **Standard-IA** → See cost difference.
3. Go to **Bucket → Management → Lifecycle Rules**.
4. Create rule: Transition to Standard-IA after 30 days, Glacier after 90 days.
5. Create another rule: Expire (delete) objects after 365 days.
6. Upload a new file as **Intelligent-Tiering** → AWS auto-manages the tier.

### Common Mistakes
- ❌ Storing everything in Standard forever — paying premium for cold data.
- ❌ Using Glacier for data that needs instant access.
- ❌ Forgetting minimum storage duration — 30 days for IA, 90 for Glacier.
- ❌ Not accounting for retrieval costs when budgeting.
- ❌ Using One Zone-IA for irreplaceable data — only 1 AZ = less durability.

### Pro Tips
- ✅ Use **Intelligent-Tiering** when access patterns are unknown — no retrieval fees.
- ✅ Use **S3 Storage Lens** to analyze access patterns across all buckets.
- ✅ **Glacier Instant Retrieval** is perfect for quarterly compliance reports.
- ✅ Enable **S3 Analytics** to get recommendations on when to transition.
- ✅ In interviews: "We use lifecycle policies to transition data: Standard for 30 days, IA for 60 days, Glacier after 90 days, and Deep Archive for long-term compliance. Intelligent-Tiering handles unknown patterns."

---

## 3. Versioning

### What is it?
Versioning keeps every version of every object in your S3 bucket.
When you overwrite or delete a file, the old version is preserved.
You can restore any previous version at any time.

### Why it matters in production
- Protects against accidental deletes and overwrites.
- Required for S3 Cross-Region Replication.
- Important for compliance — prove what data existed at any point in time.

### Core Ideas
- **Enabled at the bucket level** — applies to all objects in the bucket.
- Once enabled, it can be **suspended** but never fully disabled.
- Each version gets a unique **Version ID**.
- **Delete** adds a "delete marker" — the object looks deleted but all versions remain.
- To truly delete, you must delete specific version IDs.
- Versioning + **MFA Delete** = Extra protection (need MFA to permanently delete).
- Versioning increases storage costs — old versions take space.

### Quick Analogy
Versioning = Google Docs version history.
Every edit creates a new version. You can view and restore any previous version. Nothing is truly lost unless you explicitly purge it.

### Architecture View
```
Bucket: my-app (Versioning: Enabled)

Key: config.json
├── Version: v3 (current) ← "latest"
├── Version: v2
└── Version: v1

DELETE config.json →
├── Delete Marker (current) ← "file appears deleted"
├── Version: v3
├── Version: v2
└── Version: v1

Remove Delete Marker → config.json is "restored" (v3 is current again)
```

### Hands-On (Step-by-step Lab)
1. Create an S3 bucket → Enable **Versioning**.
2. Upload `test.txt` with content "version 1".
3. Upload `test.txt` again with content "version 2" (same key).
4. Click **Show versions** → See both versions with different Version IDs.
5. Download v1 → Confirm it still has "version 1".
6. Delete `test.txt` → A delete marker appears.
7. Show versions → Delete the delete marker → File is restored.

### Common Mistakes
- ❌ Not enabling versioning on important buckets — accidental deletes are permanent.
- ❌ Forgetting old versions cost money — add lifecycle rules to expire them.
- ❌ Thinking "delete" actually deletes — it just adds a marker.
- ❌ Not using MFA Delete for critical data.

### Pro Tips
- ✅ Combine versioning with **lifecycle rules** to expire non-current versions after X days.
- ✅ Enable **MFA Delete** for compliance-critical buckets.
- ✅ Versioning is **required** for S3 Cross-Region Replication.
- ✅ Use **S3 Object Lock** with versioning for WORM compliance.
- ✅ In interviews: "We enable versioning on all production buckets with lifecycle rules to expire old versions after 90 days. MFA Delete is enabled on compliance-critical buckets."

---

## 4. Lifecycle Policies

### What is it?
Lifecycle policies automate moving objects between storage classes or deleting them.
You define rules like: "After 30 days, move to IA. After 90 days, move to Glacier."
This runs automatically — no manual intervention needed.

### Why it matters in production
- Automates cost optimization for S3.
- Without lifecycle rules, old data sits in expensive Standard storage forever.
- Compliance often requires keeping data for X years then deleting.

### Core Ideas
- **Transition actions** — Move objects to a cheaper storage class after X days.
- **Expiration actions** — Delete objects (or versions) after X days.
- Rules apply to the **entire bucket** or to objects matching a **prefix/tag filter**.
- Minimum days before transition: 30 days for IA, 90 for Glacier classes.
- Can target **current versions**, **non-current versions**, or both.
- Lifecycle rules for non-current versions: expire old versions after X days.

### Quick Analogy
Lifecycle policy = An office paper management rule.
"Active files stay on the desk (Standard). After a month, move to the filing cabinet (IA). After a quarter, send to the warehouse (Glacier). After 7 years, shred (expire)."

### Architecture View
```
Lifecycle Rule: "archive-and-cleanup"
Filter: prefix "logs/"

Timeline for logs/app-2024-01.log:
┌──────────┬───────────────┬──────────────┬────────────────┬──────────┐
│ Day 0    │ Day 30        │ Day 90       │ Day 365        │ Day 730  │
│ Standard │ → Std-IA      │ → Glacier    │ → Deep Archive │ → DELETE │
│ $0.023/GB│ → $0.0125/GB  │ → $0.004/GB  │ → $0.00099/GB  │ → $0    │
└──────────┴───────────────┴──────────────┴────────────────┴──────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **S3 Bucket → Management → Create Lifecycle Rule**.
2. Rule name: `archive-logs`.
3. Filter: Prefix `logs/`.
4. Add transition: Standard-IA after 30 days.
5. Add transition: Glacier Flexible after 90 days.
6. Add expiration: Delete after 365 days.
7. For versioned buckets: Add rule to expire non-current versions after 30 days.
8. Save → Transitions happen automatically.

### Common Mistakes
- ❌ Not adding lifecycle rules — paying Standard prices for years-old data.
- ❌ Transitioning to Glacier when data needs fast access.
- ❌ Forgetting about non-current versions — they pile up and cost money.
- ❌ Setting transition too early (before minimum days).
- ❌ Not filtering by prefix — accidentally moving ALL objects.

### Pro Tips
- ✅ Always combine **versioning + lifecycle rules** — expire non-current versions.
- ✅ Use **S3 Analytics** to find the optimal transition timing.
- ✅ Create separate rules for different prefixes (`logs/`, `backups/`, `assets/`).
- ✅ Lifecycle rules are evaluated daily — transitions don't happen instantly.
- ✅ In interviews: "We use lifecycle policies on every production bucket. Logs move to IA at 30 days, Glacier at 90, and auto-delete at 365. Non-current versions expire after 30 days."

---

## 5. EBS vs EFS vs Instance Store

### What is it?
These are three types of storage you can use with EC2 instances.
Each has different characteristics for performance, persistence, and sharing.
Choosing the right one depends on your workload.

### Why it matters in production
- Wrong storage choice = performance bottleneck or data loss.
- This is a very common interview comparison question.
- Understanding trade-offs is essential for architecture design.

### Core Ideas

| Feature | EBS | EFS | Instance Store |
|---------|-----|-----|---------------|
| **Type** | Block storage | File storage (NFS) | Block storage |
| **Persistence** | Persists after stop/terminate (if configured) | Persists always | **LOST on stop/terminate** |
| **Scope** | Single AZ, single instance | Multi-AZ, shared across instances | Local to instance |
| **Size** | 1 GB – 64 TB | Petabyte scale (auto-grows) | Fixed (depends on instance type) |
| **Performance** | Very high (gp3, io2) | Good, scales with size | Highest (NVMe, physically attached) |
| **Cost** | Per GB provisioned | Per GB used | Free (included with instance) |
| **Use case** | Boot volumes, databases | Shared files, CMS, home dirs | Temp data, cache, buffers |

**EBS volume types:**

| Type | IOPS | Throughput | Use Case |
|------|------|-----------|----------|
| **gp3** | 3,000 (baseline) up to 16,000 | 125–1000 MB/s | General purpose (DEFAULT choice) |
| **gp2** | Burst to 3,000 | 128–250 MB/s | Legacy general purpose |
| **io2 Block Express** | Up to 256,000 | 4,000 MB/s | High-perf databases |
| **st1** | N/A | 500 MB/s | Big data, log processing |
| **sc1** | N/A | 250 MB/s | Cold, infrequent access |

### Quick Analogy
- **EBS** = An external hard drive plugged into your computer. One computer at a time. Persists.
- **EFS** = A shared network folder everyone on the team can access simultaneously.
- **Instance Store** = A sticky note on your monitor. Fast to read, but if you throw the monitor away, the note is gone.

### Architecture View
```
┌────────────────────────────────────────────────┐
│ EC2 Instance                                   │
│ ┌──────────────┐  ┌───────────────────────┐    │
│ │ Instance Store│  │ EBS Volume (/dev/xvda)│    │
│ │ (ephemeral)  │  │ (root volume, gp3)    │    │
│ │ temp files,  │  │ OS + App              │    │
│ │ cache        │  │ Persists on stop      │    │
│ └──────────────┘  └───────────────────────┘    │
│                    ┌───────────────────────┐    │
│                    │ EBS Volume (/dev/xvdf)│    │
│                    │ (data volume, io2)    │    │
│                    │ Database files        │    │
│                    └───────────────────────┘    │
└────────────────────────────────────────────────┘
        │
        ├── NFS mount ──▶ ┌─────────────────────┐
        │                 │ EFS File System      │
Other ──┤                 │ Shared across AZs    │
EC2s ───┘                 │ /mnt/efs/shared-data │
                          └─────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **EBS:** Launch EC2 → Attach an additional 10 GB gp3 volume → `mkfs.xfs`, `mount`, write data → Stop/start → Data persists.
2. **EFS:** Create an EFS file system → Mount on two EC2s in different AZs → Write a file from one → Read from the other.
3. **Instance Store:** Launch an instance type with instance store (e.g., `c5d.large`) → Write data to `/dev/nvme1n1` → Stop the instance → Data is gone.
4. Compare: EBS survives stop. EFS is shared. Instance Store is fastest but ephemeral.

### Common Mistakes
- ❌ Storing important data on Instance Store — it's lost on stop/terminate.
- ❌ Using gp2 when gp3 is cheaper and faster (gp3 is the new default).
- ❌ Not enabling EBS encryption — should be enabled by default.
- ❌ Using EBS when you need shared storage — use EFS instead.
- ❌ Forgetting EBS is AZ-locked — can't attach a volume from `us-east-1a` to an instance in `us-east-1b`.
- ❌ Not taking EBS snapshots regularly.

### Pro Tips
- ✅ **gp3** should be your default EBS choice — better performance, lower cost than gp2.
- ✅ Enable **EBS encryption by default** at the account level.
- ✅ Use **EBS Snapshots** for backups → stored in S3 (you don't see this).
- ✅ EFS supports **Lifecycle Management** — move cold files to IA tier automatically.
- ✅ Use Instance Store for ephemeral scratch space, caches, and buffers only.
- ✅ In interviews: "We use gp3 EBS for boot and data volumes with encryption enabled by default. EFS for shared workloads across instances. Instance Store only for temporary caches. Regular snapshots for DR."
