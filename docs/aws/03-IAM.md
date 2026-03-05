# IAM (Identity and Access Management)

> IAM is one of the MOST asked topics in AWS DevOps interviews. Know it inside out.

---

## 1. IAM Users

### What is it?
An IAM User is an identity that represents a person or application.
Each user has a unique name and credentials (password or access keys).
Users are created inside your AWS account. By default, a new user has **zero permissions**.

### Why it matters in production
- Every person and service accessing AWS should have its own IAM User (or Role).
- Without proper user management, you can't track who did what.
- Root account should NEVER be used for daily work.

### Core Ideas
- A user can have **console access** (password) and/or **programmatic access** (access key + secret key).
- Access keys = for CLI/SDK. Password = for AWS Console.
- Each user can have **at most 2 access keys** (for rotation).
- New user = NO permissions until you attach a policy.
- **Root user** = the email account that created the AWS account. Has God-mode access. Lock it down.

### Quick Analogy
IAM User = An employee badge. The badge gets you in the building, but each badge has different door access.

### Architecture View
```
AWS Account
├── Root User (God mode — lock it away)
├── IAM User: alice (admin access)
├── IAM User: bob (read-only access)
├── IAM User: ci-bot (programmatic access only)
└── IAM User: developer1 (custom policy)
```

### Hands-On (Step-by-step Lab)
1. Go to **IAM → Users → Create User**.
2. Name it `test-user`.
3. Enable **Console access** → Set a password.
4. Do NOT attach any policy yet.
5. Log in as `test-user` → Try to access EC2 → **Access Denied**.
6. Go back to root → Attach `AmazonEC2ReadOnlyAccess` policy to `test-user`.
7. Log in again → Now EC2 listing works but you can't create/delete anything.

### Common Mistakes
- ❌ Using root account for daily tasks.
- ❌ Sharing access keys between team members.
- ❌ Creating a user with `AdministratorAccess` for everyone.
- ❌ Not rotating access keys regularly.
- ❌ Hardcoding access keys in application code.

### Pro Tips
- ✅ Enable **MFA** on every IAM user, especially admins.
- ✅ Use **IAM Roles** for applications instead of embedding access keys.
- ✅ Delete access keys that haven't been used in 90 days.
- ✅ In interviews: "We create individual IAM users per person, enforce MFA, and use roles for EC2/Lambda instead of long-lived keys."

---

## 2. IAM Groups

### What is it?
A Group is a collection of IAM Users.
You attach policies to the group, and all users in that group inherit those permissions.
Groups make permission management easier — manage 1 group instead of 50 users.

### Why it matters in production
- In real companies, you have teams: Developers, DevOps, QA, Admins.
- Instead of attaching policies to each user, attach to the group.
- When someone joins/leaves a team, just add/remove them from the group.

### Core Ideas
- A user can belong to **multiple groups**.
- Groups **cannot be nested** (no group inside a group).
- Groups are **only for users** — you cannot add roles or other groups.
- A group is NOT an identity — you can't log in as a group.
- **Maximum 10 groups per user** (default limit).

### Quick Analogy
Group = A department in an office.
Join the "Engineering" department → you get access to the engineering floor, tools, and repositories. Leave the department → access revoked.

### Architecture View
```
IAM Groups
├── Group: Admins
│   ├── Policy: AdministratorAccess
│   └── Members: alice, bob
├── Group: Developers
│   ├── Policy: AmazonEC2FullAccess
│   ├── Policy: AmazonS3FullAccess
│   └── Members: charlie, dave, eve
├── Group: ReadOnly
│   ├── Policy: ReadOnlyAccess
│   └── Members: intern1, intern2
```

### Hands-On (Step-by-step Lab)
1. Go to **IAM → User Groups → Create Group**.
2. Name it `Developers`.
3. Attach `AmazonEC2FullAccess` and `AmazonS3ReadOnlyAccess`.
4. Add your `test-user` to this group.
5. Log in as `test-user` → Verify EC2 full access and S3 read-only.
6. Remove `test-user` from the group → Permissions gone instantly.

### Common Mistakes
- ❌ Attaching policies directly to users instead of using groups.
- ❌ Creating one mega-group with all permissions.
- ❌ Trying to nest groups (not supported in AWS).
- ❌ Not reviewing group memberships regularly.

### Pro Tips
- ✅ Create groups that mirror your org structure: `Admins`, `Developers`, `QA`, `Billing`.
- ✅ Use groups as the **primary** way to assign permissions to users.
- ✅ Combine with SCPs (Service Control Policies) in AWS Organizations for org-wide guardrails.
- ✅ In interviews: "We manage permissions through groups, not individual users. This keeps things scalable and auditable."

---

## 3. IAM Roles

### What is it?
A Role is an identity with permissions, but **nobody logs in as a role**.
Instead, roles are **assumed** temporarily by users, services, or applications.
When something assumes a role, it gets temporary credentials that expire automatically.

### Why it matters in production
- EC2 instances, Lambda functions, and ECS tasks use Roles — never hardcoded keys.
- Cross-account access uses Roles.
- Roles are THE way services talk to each other securely in AWS.

### Core Ideas
- Roles have **no permanent credentials** — they generate temporary ones via STS.
- **Trust Policy** — Who can assume this role (EC2, Lambda, another account, a user).
- **Permission Policy** — What the role can do once assumed (S3 access, DynamoDB, etc.).
- An EC2 instance can have **one IAM Role** attached (via Instance Profile).
- Temporary credentials auto-expire (default 1 hour, configurable up to 12 hours).

### Quick Analogy
Role = A contractor badge.
You don't own it permanently. You check it out at the front desk, use it for a few hours, and return it. While you have it, you can access specific areas.

### Architecture View
```
┌──────────────┐         ┌───────────────┐
│  EC2 Instance│──assumes──▶│  IAM Role:    │
│  (no keys!)  │         │  S3-ReadRole   │
└──────────────┘         │  - Trust: EC2  │
                         │  - Perm: S3 Get│
                         └───────────────┘
                                │
                                ▼
                         ┌───────────────┐
                         │  STS returns   │
                         │  temp creds    │
                         │  (expires 1hr) │
                         └───────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **IAM → Roles → Create Role**.
2. Trusted entity: **AWS Service → EC2**.
3. Attach policy: `AmazonS3ReadOnlyAccess`.
4. Name it `EC2-S3-ReadOnly`.
5. Launch an EC2 instance → Attach this role (under IAM Instance Profile).
6. SSH into the EC2 → Run `aws s3 ls` → Works! No access keys configured.
7. Try `aws s3 rm s3://bucket/file` → **Access Denied** (read-only role).

### Common Mistakes
- ❌ Using access keys on EC2 instead of roles.
- ❌ Creating overly permissive roles (`*` on everything).
- ❌ Forgetting the Trust Policy — the role exists but nothing can assume it.
- ❌ Not understanding that role credentials are temporary.

### Pro Tips
- ✅ **ALWAYS** use roles for AWS services. Never embed keys.
- ✅ Use **instance profiles** for EC2, **execution roles** for Lambda, **task roles** for ECS.
- ✅ For cross-account: Create role in Account B, let Account A assume it.
- ✅ In interviews: "We never use long-lived credentials. EC2 uses instance profiles with IAM roles. Lambda uses execution roles. All temporary, all auditable via CloudTrail."

---

## 4. IAM Policies

### What is it?
A Policy is a JSON document that defines permissions.
It says: "Allow or Deny this action on this resource under these conditions."
Policies are attached to Users, Groups, or Roles.

### Why it matters in production
- Policies are the actual rules that control who can do what.
- A misconfigured policy can expose your entire AWS account.
- Writing good policies is a core DevOps skill.

### Core Ideas
- **Policy structure (VERY IMPORTANT):**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

- **Effect** — `Allow` or `Deny`.
- **Action** — What operation (e.g., `s3:GetObject`, `ec2:StartInstances`).
- **Resource** — Which resource (specified by ARN).
- **Condition** (optional) — When this applies (IP range, time, MFA, etc.).

**Types of policies:**

| Type | Description |
|------|------------|
| **AWS Managed** | Pre-built by AWS. Example: `AmazonS3ReadOnlyAccess` |
| **Customer Managed** | You create and manage them. Reusable across your account. |
| **Inline** | Embedded directly into a user/group/role. Not reusable. |

### Quick Analogy
Policy = A permission slip.
It says exactly what you're allowed to do, where, and under what conditions.
"Alice can read files from the Documents folder, but only during office hours."

### Architecture View
```
IAM User: alice
├── Attached: Group Policy (from "Developers" group)
│   └── Allow: ec2:*, s3:Get*
├── Attached: Managed Policy (AmazonRDSReadOnlyAccess)
│   └── Allow: rds:Describe*
└── Inline Policy
    └── Deny: s3:DeleteBucket (explicit deny — overrides everything)

Final permissions = UNION of all Allow - any explicit Deny wins
```

### Hands-On (Step-by-step Lab)
1. Go to **IAM → Policies → Create Policy**.
2. Use the **JSON editor** and write:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::my-test-bucket",
        "arn:aws:s3:::my-test-bucket/*"
      ]
    }
  ]
}
```
3. Name it `S3-ReadOnly-MyBucket`.
4. Attach it to a user → Test by listing and reading from that bucket.
5. Try deleting a file → **Access Denied**.

### Common Mistakes
- ❌ Using `"Resource": "*"` — gives access to ALL resources.
- ❌ Using `"Action": "*"` — gives ALL actions. Way too permissive.
- ❌ Forgetting the `Version` field — old version may behave differently.
- ❌ Not using conditions — miss easy security wins (IP restriction, MFA required).
- ❌ Using inline policies instead of managed policies (harder to maintain).

### Pro Tips
- ✅ Start with zero permissions. Add only what's needed.
- ✅ Use the **IAM Policy Simulator** to test policies before deploying.
- ✅ Use **conditions** for extra security: require MFA, restrict to VPC, limit by IP.
- ✅ Prefer **Customer Managed Policies** over inline — reusable and version-tracked.
- ✅ In interviews: "We write least-privilege policies with specific resource ARNs and conditions. We test them with the IAM Policy Simulator."

---

## 5. Policy Evaluation Logic

### What is it?
When a request is made, AWS evaluates all applicable policies to decide: Allow or Deny.
There is a specific order of evaluation. Understanding it is critical for troubleshooting.
The golden rule: **Explicit Deny ALWAYS wins.**

### Why it matters in production
- When something isn't working, you need to understand WHY AWS denied it.
- Policy evaluation logic is a common interview question.
- Misconfigured policies cause outages when legitimate requests are denied.

### Core Ideas

**Evaluation order:**
1. **Explicit Deny?** → If ANY policy says Deny → **DENIED**. Full stop.
2. **SCP (Org level)?** → Does the Organization allow it? If not → Denied.
3. **Resource-based policy?** → Does the resource (e.g., S3 bucket policy) allow it?
4. **Identity-based policy?** → Does the user/role/group policy allow it?
5. **Permission boundary?** → Does the boundary allow it?
6. **Session policy?** → Does the session policy allow it?
7. **If nothing explicitly allows → DENIED** (implicit deny).

**The flow chart in your head:**
```
Request comes in
      │
      ▼
Any EXPLICIT DENY? ──YES──▶ DENIED ❌
      │ NO
      ▼
Any ALLOW? ──YES──▶ ALLOWED ✅
      │ NO
      ▼
DENIED ❌ (implicit deny — default)
```

### Quick Analogy
Think of a nightclub:
- Bouncer 1 (Explicit Deny) = Blacklist. If you're on it, you're NOT getting in. Period.
- Bouncer 2 (Allow) = Guest list. If you're on it, you can enter.
- Default = No list = No entry.

### Architecture View
```
User: bob → calls s3:DeleteObject on my-bucket

Evaluation:
  1. Any SCP deny?          → No
  2. Any explicit deny?     → bob's inline policy says Deny s3:Delete* → DENIED ❌
     (Even if group policy says Allow s3:*)

Result: DENIED — explicit deny wins over allow
```

### Hands-On (Step-by-step Lab)
1. Create a user with `AmazonS3FullAccess` (group or managed policy).
2. Add an **inline policy** to that user:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Deny",
      "Action": "s3:DeleteObject",
      "Resource": "*"
    }
  ]
}
```
3. Log in as that user → Try to delete an S3 object → **Denied**.
4. The user has S3 Full Access, but the explicit Deny wins.
5. Use the **IAM Policy Simulator** → Test the same scenario → See the Deny result.

### Common Mistakes
- ❌ Thinking Allow overrides Deny — it NEVER does.
- ❌ Forgetting implicit deny — if nothing says Allow, it's denied.
- ❌ Not considering SCPs when using AWS Organizations.
- ❌ Not checking Permission Boundaries on users/roles.

### Pro Tips
- ✅ Remember: **Deny → Allow → Deny (implicit)**. Explicit Deny always wins.
- ✅ Use the **IAM Policy Simulator** to debug access issues.
- ✅ Use **CloudTrail** to see which policy denied a request.
- ✅ In interviews: "AWS uses a deny-by-default model. Explicit denies always override allows. I use the IAM Policy Simulator and CloudTrail to debug access issues."

---

## 6. Least Privilege Principle

### What is it?
Give users and services the **minimum permissions** they need to do their job. Nothing more.
If a developer only needs to read S3, don't give them S3 full access.
This is the #1 security rule in IAM.

### Why it matters in production
- Over-permissioned accounts are the #1 cause of security breaches.
- If a compromised key has admin access, the attacker owns your account.
- Least privilege limits the blast radius of any compromise.

### Core Ideas
- Start with **zero permissions** → Add only what's needed.
- Use **specific resource ARNs** instead of `"Resource": "*"`.
- Use **specific actions** instead of `"Action": "*"` or `"Action": "s3:*"`.
- Review permissions regularly — remove unused ones.
- Use **IAM Access Analyzer** to find unused permissions.
- Use **CloudTrail** to see what APIs are actually being called.

### Quick Analogy
Least privilege = Hotel key card.
Your card opens YOUR room only. Not the whole hotel. If you lose it, the damage is limited to your room.

### Architecture View
```
BAD (Over-privileged):
  Role: App-Role
  Policy: { "Effect": "Allow", "Action": "*", "Resource": "*" }
  → If compromised, attacker can do ANYTHING

GOOD (Least privilege):
  Role: App-Role
  Policy: {
    "Effect": "Allow",
    "Action": ["s3:GetObject", "s3:PutObject"],
    "Resource": "arn:aws:s3:::app-data-bucket/*"
  }
  → If compromised, attacker can only read/write to one bucket
```

### Hands-On (Step-by-step Lab)
1. Go to **IAM → Access Analyzer → Generate Policy**.
2. Select a role → It analyzes CloudTrail logs (last 90 days).
3. It generates a policy with only the permissions that were actually used.
4. Compare this with the current policy → See how much can be removed.
5. Replace the old policy with the generated one.
6. Test that the application still works.

### Common Mistakes
- ❌ Starting with `AdministratorAccess` and "planning to restrict later" — you won't.
- ❌ Using wildcard `*` for actions or resources.
- ❌ Not reviewing permissions after project changes.
- ❌ Copy-pasting policies from Stack Overflow without understanding them.

### Pro Tips
- ✅ Use **IAM Access Analyzer** to auto-generate least-privilege policies.
- ✅ Start with AWS Managed Policies → then replace with custom tighter policies.
- ✅ Set up **permission boundaries** for developers — they can create roles but can't exceed the boundary.
- ✅ In interviews: "We follow least privilege strictly. We use IAM Access Analyzer to right-size policies based on actual CloudTrail usage."

---

## 7. Assume Role (STS)

### What is it?
Assume Role means temporarily switching to another IAM Role using AWS STS (Security Token Service).
You give up your current permissions and get the role's permissions temporarily.
This is how cross-account access, service access, and federated login work.

### Why it matters in production
- Cross-account access: Dev account assumes a role in Prod account.
- CI/CD pipelines assume deployment roles.
- Federated users (SSO, SAML) assume roles to get AWS access.
- It's the backbone of secure, temporary access in AWS.

### Core Ideas
- `sts:AssumeRole` → Returns temporary credentials (access key, secret key, session token).
- Credentials expire (default 1 hour, max 12 hours).
- The role must have a **Trust Policy** that allows the caller to assume it.
- The caller must have **permission to call `sts:AssumeRole`**.
- **Two sides:** Trust Policy (who can assume) + Permission Policy (what they can do).

### Quick Analogy
Assume Role = Checking out a costume at a costume shop.
You walk in as yourself (User A). You put on the costume (Role B). Now you look and act like Role B. When time's up, you return the costume and go back to being yourself.

### Architecture View
```
Account A (Dev)                    Account B (Prod)
┌──────────────┐                  ┌──────────────────┐
│ IAM User:    │                  │ IAM Role:         │
│ deploy-user  │──AssumeRole───▶  │ ProdDeployRole    │
│              │                  │                   │
│ Needs:       │                  │ Trust: Account A  │
│ sts:Assume   │                  │ Perm: S3, EC2     │
│ Role perm    │                  │ Expires: 1 hour   │
└──────────────┘                  └──────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **Create a Role** in your account:
   - Trust: Your own account (for testing).
   - Permissions: `AmazonS3ReadOnlyAccess`.
   - Name: `S3ReadRole`.
2. **From CLI** (as your IAM user):
```bash
aws sts assume-role \
  --role-arn arn:aws:iam::123456789012:role/S3ReadRole \
  --role-session-name test-session
```
3. Copy the temporary credentials from the output.
4. Export them:
```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_SESSION_TOKEN=...
```
5. Run `aws s3 ls` → Works with the role's permissions.
6. Run `aws ec2 describe-instances` → **Denied** (role only has S3 access).

### Common Mistakes
- ❌ Forgetting the Trust Policy — role exists but can't be assumed.
- ❌ Not including `AWS_SESSION_TOKEN` — temp credentials require all three parts.
- ❌ Setting session duration too long — reduces security.
- ❌ Confusing the role's permission policy with its trust policy.

### Pro Tips
- ✅ Use **external ID** in trust policies for third-party cross-account access (prevents confused deputy).
- ✅ Use **role chaining** carefully — each hop reduces max session duration to 1 hour.
- ✅ Require **MFA** for assume role in production accounts.
- ✅ In interviews: "We use STS AssumeRole for all cross-account access. CI/CD pipelines assume a deployment role in the target account with scoped permissions and MFA conditions."

---

## 8. IAM Best Practices

### What is it?
A checklist of security rules you should follow in every AWS account.
These come directly from AWS recommendations and real-world lessons.
Follow ALL of these in production.

### Why it matters in production
- IAM misconfigurations are the #1 cause of AWS breaches.
- Interviewers expect you to know these by heart.
- These practices form the baseline of any AWS security audit.

### Core Ideas — The Complete Checklist

**Account Level:**
- ✅ **Lock the root account** — Enable MFA, delete root access keys, never use root for daily tasks.
- ✅ **Enable CloudTrail** — Log every API call for audit.
- ✅ **Enable AWS Config** — Track configuration changes.

**User Management:**
- ✅ **One user per person** — Never share credentials.
- ✅ **Enforce MFA** — For all users, especially admins.
- ✅ **Use Groups** — Assign permissions via groups, not directly to users.
- ✅ **Regular access reviews** — Remove unused users and permissions quarterly.
- ✅ **Rotate access keys** — Every 90 days. Use IAM Credential Report to track.

**Permissions:**
- ✅ **Least privilege** — Minimum permissions needed. No wildcards.
- ✅ **Use IAM Roles for services** — EC2, Lambda, ECS should use roles, not keys.
- ✅ **Permission Boundaries** — Limit what developers can grant.
- ✅ **Use Conditions** — Restrict by IP, MFA, time, VPC endpoint.

**Monitoring:**
- ✅ **IAM Access Analyzer** — Find resources shared externally.
- ✅ **IAM Credential Report** — CSV of all users and their credential status.
- ✅ **CloudTrail + CloudWatch Alarms** — Alert on suspicious IAM activities.

**Advanced:**
- ✅ **Use AWS Organizations + SCPs** — Enforce guardrails across all accounts.
- ✅ **Use AWS SSO (IAM Identity Center)** — Centralized login for multiple accounts.
- ✅ **Use secrets manager** — Never hardcode secrets in code.

### Quick Analogy
IAM Best Practices = Fire safety rules in a building.
Smoke detectors (CloudTrail), fire exits (MFA), locked doors (least privilege), regular drills (access reviews). You need ALL of them, not just one.

### Architecture View
```
AWS Account — Secure Setup
├── Root: MFA ✅ | No access keys ✅ | Not used daily ✅
├── CloudTrail: Enabled ✅ | All regions ✅
├── Groups:
│   ├── Admins (MFA enforced, limited membership)
│   ├── Developers (scoped policies)
│   └── ReadOnly (for auditors)
├── Roles:
│   ├── EC2-AppRole (instance profile)
│   ├── Lambda-ExecRole (execution role)
│   └── CrossAccount-DeployRole (trust: CI/CD account)
├── Policies: All least-privilege, no wildcards
├── Monitoring: Access Analyzer + Credential Report + CloudWatch Alerts
└── Organizations: SCPs blocking dangerous actions
```

### Hands-On (Step-by-step Lab)
1. Go to **IAM → Credential Report** → Download → Review all users.
2. Check: Who has MFA? Who has old access keys? Who never logged in?
3. Go to **IAM → Access Analyzer** → Look for resources shared publicly.
4. Go to **IAM → Account Settings** → Set a **password policy** (min 14 chars, require symbols, expire in 90 days).
5. Go to **CloudTrail** → Ensure it's enabled for all Regions.
6. Create a **CloudWatch Alarm** for root account login.

### Common Mistakes
- ❌ Root account used daily with no MFA.
- ❌ Access keys in GitHub repos (this happens more than you'd think).
- ❌ Everyone has `AdministratorAccess`.
- ❌ No CloudTrail enabled — can't audit anything.
- ❌ Never reviewing permissions — old employees still have access.

### Pro Tips
- ✅ Run **IAM Credential Report** monthly.
- ✅ Use **AWS Config rules** to auto-detect non-compliant IAM configs.
- ✅ Use **SCPs** to prevent anyone from disabling CloudTrail or leaving a Region.
- ✅ In interviews: "Our IAM strategy follows least privilege with mandatory MFA, role-based access via groups, no long-lived credentials for services, and continuous monitoring via Access Analyzer and CloudTrail."
