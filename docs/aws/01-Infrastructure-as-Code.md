# Infrastructure as Code

---

## 1. Why IaC Matters

### What is it?
Infrastructure as Code means managing your servers, networks, databases, and all cloud resources through **code files** instead of clicking in the AWS Console.
You write a file describing what you want. A tool creates it for you. Every time, exactly the same way.
No manual steps. No "it worked on my account." Just code.

### Why it matters in production
- Manual console changes are the #1 source of production drift, outages, and "who changed this?"
- IaC is the foundation of DevOps — you can't have CI/CD without it.
- Every serious AWS organization uses IaC. Interviews **expect** you to know it.

### Core Ideas
- **Declarative** — You describe the desired end state. The tool figures out how to get there. (CloudFormation, Terraform, CDK)
- **Imperative** — You describe the exact steps to execute. (Scripts, AWS CLI, SDK)
- **Idempotent** — Run the same template 10 times → Same result every time. No duplicates.
- **Version controlled** — Templates live in Git. Every change is tracked, reviewed, and auditable.
- **Repeatable** — Same template creates identical environments: dev, staging, prod.
- **Self-documenting** — The code IS the documentation of your infrastructure.

**Benefits of IaC:**

| Benefit | Without IaC | With IaC |
|---------|------------|---------|
| **Consistency** | "Works in my account" | Identical environments every time |
| **Speed** | Hours of clicking | Minutes of `deploy` |
| **Audit** | "Who changed the security group?" | Git blame shows who, when, why |
| **Disaster Recovery** | Rebuild manually from memory | Re-run the template in a new Region |
| **Scaling** | Copy-paste configs, hope for the best | Parameterize and deploy to 10 Regions |
| **Review** | No review process for infra changes | Pull request → Code review → Deploy |

**IaC tools on AWS:**

| Tool | Type | Language | Best For |
|------|------|---------|---------|
| **CloudFormation** | AWS-native | JSON/YAML | AWS-only, enterprise compliance |
| **CDK** | AWS-native (generates CF) | TypeScript, Python, Java, etc. | Developers who prefer real code |
| **Terraform** | Multi-cloud | HCL | Multi-cloud, large community |
| **Pulumi** | Multi-cloud | TypeScript, Python, Go, etc. | Multi-cloud with real languages |
| **SAM** | AWS-native (extends CF) | YAML | Serverless applications |

### Quick Analogy
IaC = Building a house from blueprints.
Without IaC: You tell the builder "make a nice house" and hope for the best. Every house is different.
With IaC: You give the builder detailed blueprints. Every house built from those blueprints is identical. Change the blueprint → change all future houses.

### Architecture View
```
IaC Workflow:

Developer ──▶ Write template ──▶ Git commit ──▶ Pull Request
                                                    │
                                              Code Review ✅
                                                    │
                                              Merge to main
                                                    │
                                              CI/CD Pipeline
                                                    │
                              ┌──────────────────────┼────────────────────┐
                              │                      │                    │
                              ▼                      ▼                    ▼
                        Deploy to DEV         Deploy to STAGING     Deploy to PROD
                        (same template,       (same template,       (same template,
                         dev params)           staging params)       prod params)

Result: All 3 environments are structurally identical.
        Only parameters differ (instance size, domain name, etc.).
```

### Hands-On (Step-by-step Lab)
1. Create a simple CloudFormation template (YAML) — covered in next section.
2. Deploy it → Creates resources.
3. Modify the template → Update the stack → Resources change.
4. Delete the stack → All resources cleaned up automatically.
5. Deploy the same template in another Region → Identical infrastructure.
6. Compare: Doing this manually in the console would take 10x longer and never be exactly the same.

### Common Mistakes
- ❌ Making changes in the console AND using IaC — causes drift.
- ❌ Not parameterizing templates — hardcoding values for one environment.
- ❌ Not storing templates in version control — losing the audit trail.
- ❌ Writing a 5,000-line monolith template — break into smaller, nested stacks.
- ❌ "We'll add IaC later" — you won't. Start with IaC from Day 1.

### Pro Tips
- ✅ **IaC from Day 1.** Never create production resources manually.
- ✅ Store all templates in **Git** with pull request reviews.
- ✅ Use **parameters and mappings** to make templates reusable across environments.
- ✅ Use **linting tools**: `cfn-lint` for CloudFormation, `tflint` for Terraform.
- ✅ Treat infrastructure changes like code changes: branch → review → test → deploy.
- ✅ In interviews: "All our infrastructure is managed as code in Git. Changes go through pull requests and code review. CI/CD deploys to dev, staging, and prod using the same templates with different parameters. No manual console changes are allowed."

---

## 2. CloudFormation

### What is it?
CloudFormation is AWS's native IaC service. You write a template (JSON or YAML) describing AWS resources.
CloudFormation creates, updates, and deletes those resources as a **stack**.
One template → one stack → all the resources managed as a unit.

### Why it matters in production
- CloudFormation is the most widely used IaC tool in AWS-only organizations.
- It's tightly integrated with AWS — supports every AWS service.
- It's free — you only pay for the resources it creates.

### Core Ideas

**Template structure:**

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: My first stack

Parameters:          # Input values (environment, instance size, etc.)
  Environment:
    Type: String
    Default: dev

Mappings:            # Lookup tables (AMI per region, etc.)
  RegionMap:
    us-east-1:
      AMI: ami-0abcdef1234567890

Resources:           # THE CORE — What to create (required section)
  MyEC2:
    Type: AWS::EC2::Instance
    Properties:
      InstanceType: t3.micro
      ImageId: !FindInMap [RegionMap, !Ref 'AWS::Region', AMI]
      Tags:
        - Key: Environment
          Value: !Ref Environment

Outputs:             # Values to export (instance ID, URL, etc.)
  InstanceId:
    Value: !Ref MyEC2
```

**Key concepts:**

| Concept | What It Does |
|---------|-------------|
| **Stack** | A collection of resources created from one template |
| **Change Set** | Preview changes before applying (like a `terraform plan`) |
| **Drift Detection** | Check if resources have been modified outside CloudFormation |
| **Nested Stacks** | Stack that references other stacks (modular design) |
| **Stack Sets** | Deploy one template across multiple accounts/Regions |
| **Rollback** | On failure, CloudFormation rolls back to the previous state |
| **Intrinsic Functions** | `!Ref`, `!Sub`, `!GetAtt`, `!Join`, `!If`, `!FindInMap` |

**Important intrinsic functions:**

| Function | Purpose | Example |
|----------|---------|---------|
| `!Ref` | Reference a parameter or resource | `!Ref MyEC2` → Instance ID |
| `!GetAtt` | Get an attribute of a resource | `!GetAtt MyEC2.PublicIp` |
| `!Sub` | String substitution | `!Sub 'arn:aws:s3:::${BucketName}'` |
| `!Join` | Join strings with delimiter | `!Join ['-', [prod, app, bucket]]` |
| `!If` | Conditional value | `!If [IsProd, m5.large, t3.micro]` |
| `!FindInMap` | Lookup from Mappings | `!FindInMap [RegionMap, !Ref 'AWS::Region', AMI]` |

### Quick Analogy
CloudFormation = An IKEA instruction manual.
The template lists all the parts (resources), how they connect, and the order to assemble. You give it to CloudFormation (the builder), and it assembles everything. If something goes wrong halfway, it disassembles everything back to the original state (rollback).

### Architecture View
```
CloudFormation Stack: prod-web-app
┌─────────────────────────────────────────────────────┐
│ Created from: web-app.yaml                          │
│                                                     │
│ Resources:                                          │
│ ├── VPC (10.0.0.0/16)                              │
│ ├── 2 Public Subnets                               │
│ ├── 2 Private Subnets                              │
│ ├── Internet Gateway                               │
│ ├── NAT Gateway                                    │
│ ├── ALB                                            │
│ ├── Auto Scaling Group (2-6 instances)             │
│ ├── Launch Template (AMI, instance type, user data)│
│ ├── RDS MySQL (Multi-AZ)                           │
│ ├── Security Groups (ALB, App, DB)                 │
│ └── CloudWatch Alarms                              │
│                                                     │
│ Status: CREATE_COMPLETE ✅                           │
│ Drift: IN_SYNC ✅                                    │
│                                                     │
│ Update: Change instance type in template             │
│ → Create Change Set → Review → Execute               │
│ → CloudFormation updates only what changed            │
└─────────────────────────────────────────────────────┘

Stack Sets (Multi-account/Region):
  Template: security-baseline.yaml
  ├── Deploy to Account A (us-east-1) ✅
  ├── Deploy to Account B (us-east-1) ✅
  ├── Deploy to Account B (eu-west-1) ✅
  └── Deploy to Account C (ap-south-1) ✅
```

### Hands-On (Step-by-step Lab)
1. Create a file `s3-bucket.yaml`:
```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: Simple S3 bucket

Parameters:
  Environment:
    Type: String
    AllowedValues: [dev, staging, prod]
    Default: dev

Resources:
  MyBucket:
    Type: AWS::S3::Bucket
    Properties:
      BucketName: !Sub 'my-app-${Environment}-${AWS::AccountId}'
      VersioningConfiguration:
        Status: Enabled
      BucketEncryption:
        ServerSideEncryptionConfiguration:
          - ServerSideEncryptionByDefault:
              SSEAlgorithm: AES256

Outputs:
  BucketName:
    Value: !Ref MyBucket
  BucketArn:
    Value: !GetAtt MyBucket.Arn
```
2. Go to **CloudFormation → Create Stack** → Upload template.
3. Parameter: `Environment = dev` → Create.
4. Watch: Status goes from `CREATE_IN_PROGRESS` → `CREATE_COMPLETE`.
5. Go to S3 → See the bucket was created.
6. **Update:** Change `AES256` to `aws:kms` in the template → Create Change Set → Execute.
7. **Delete stack** → S3 bucket is deleted (if empty). All cleaned up.

### Common Mistakes
- ❌ Editing resources in the console after CloudFormation created them — causes drift.
- ❌ Not using Change Sets for production updates — always preview changes first.
- ❌ Massive monolith templates (3,000+ lines) — use Nested Stacks to modularize.
- ❌ Hardcoding AMI IDs — use Mappings or SSM Parameter Store references.
- ❌ Not enabling termination protection on production stacks.
- ❌ Forgetting `DeletionPolicy: Retain` on databases — stack delete kills your data.

### Pro Tips
- ✅ **Always use Change Sets** before updating production stacks.
- ✅ Use `DeletionPolicy: Retain` or `Snapshot` on RDS and S3 to protect data.
- ✅ Use **SSM Parameter Store** for AMI IDs: `{{resolve:ssm:/golden-ami/latest}}`.
- ✅ Use **Nested Stacks** for reusable modules (VPC, Security, App, DB).
- ✅ Use **Stack Sets** to deploy guardrails (CloudTrail, Config) across all accounts.
- ✅ Enable **termination protection** on all production stacks.
- ✅ In interviews: "We use CloudFormation for all AWS infrastructure. Templates are in Git, changes go through pull requests, and Change Sets are mandatory before production updates. We use nested stacks for modularity and Stack Sets for organization-wide baselines."

---

## 3. CDK (Cloud Development Kit)

### What is it?
CDK lets you define AWS infrastructure using **real programming languages** — TypeScript, Python, Java, C#, Go.
It generates CloudFormation templates under the hood. You write code, CDK compiles to YAML/JSON, CloudFormation deploys it.
It combines the power of programming with the reliability of CloudFormation.

### Why it matters in production
- YAML/JSON templates are verbose and hard to test. CDK uses loops, conditions, and abstractions natively.
- CDK is AWS's fastest-growing IaC tool. Increasingly adopted for new projects.
- Developers already know Python/TypeScript — lower barrier than learning CloudFormation YAML.

### Core Ideas

**Key concepts:**

| Concept | What It Is |
|---------|-----------|
| **App** | The top-level CDK application. Contains one or more stacks. |
| **Stack** | Maps to a CloudFormation stack. Contains constructs. |
| **Construct** | A cloud component. Three levels: L1 (raw CF), L2 (opinionated defaults), L3 (patterns). |
| **L1 Construct** | Direct CloudFormation resource. Starts with `Cfn`. Full control. Verbose. |
| **L2 Construct** | AWS-curated with sensible defaults. Most commonly used. |
| **L3 Construct (Pattern)** | Pre-built architectures (e.g., `ApplicationLoadBalancedFargateService`). |
| **`cdk synth`** | Generate the CloudFormation template from your code. |
| **`cdk diff`** | Preview changes (like a Change Set). |
| **`cdk deploy`** | Deploy the stack. |
| **`cdk destroy`** | Delete the stack. |

**CDK vs CloudFormation:**

| Aspect | CloudFormation | CDK |
|--------|---------------|-----|
| **Language** | YAML / JSON | TypeScript, Python, Java, C#, Go |
| **Abstraction** | Low (every property specified) | High (L2/L3 constructs have defaults) |
| **Testing** | Hard (limited linting) | Easy (unit tests, snapshot tests) |
| **Loops/Conditions** | Ugly (`!If`, `!Condition`) | Native language constructs |
| **IDE support** | Basic | Full autocomplete, type checking |
| **Output** | Direct YAML/JSON | Generates CloudFormation under the hood |

### Quick Analogy
CloudFormation (YAML) = Writing assembly instructions by hand. Detailed, precise, but tedious.
CDK = Using a high-level programming language that compiles to assembly. Faster to write, easier to read, same end result.

### Architecture View
```
CDK Workflow:

 Your Code (TypeScript/Python)
         │
         ▼
    cdk synth  ──▶  CloudFormation Template (YAML)
                            │
                            ▼
                     cdk deploy  ──▶  CloudFormation Stack  ──▶  AWS Resources

CDK Code Example (TypeScript):
┌─────────────────────────────────────────────────────┐
│ import * as cdk from 'aws-cdk-lib';                 │
│ import * as ec2 from 'aws-cdk-lib/aws-ec2';        │
│ import * as ecs from 'aws-cdk-lib/aws-ecs';        │
│ import * as patterns from                           │
│   'aws-cdk-lib/aws-ecs-patterns';                   │
│                                                     │
│ const vpc = new ec2.Vpc(this, 'MyVpc', {            │
│   maxAzs: 3                                        │
│ });                                                 │
│                                                     │
│ new patterns                                        │
│   .ApplicationLoadBalancedFargateService(            │
│     this, 'MyApp', {                                │
│       vpc,                                          │
│       taskImageOptions: {                           │
│         image: ecs.ContainerImage                   │
│           .fromRegistry('nginx'),                   │
│       },                                            │
│       desiredCount: 3,                              │
│   });                                               │
│                                                     │
│ // ~15 lines of code creates:                       │
│ // VPC, subnets, IGW, NAT, ALB, ECS cluster,       │
│ // Fargate service, task def, SGs, log group,       │
│ // auto scaling — ALL with best-practice defaults   │
└─────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Install CDK:
```bash
npm install -g aws-cdk
```
2. Create a new project:
```bash
mkdir my-cdk-app && cd my-cdk-app
cdk init app --language typescript
```
3. Edit `lib/my-cdk-app-stack.ts`:
```typescript
import * as cdk from 'aws-cdk-lib';
import * as s3 from 'aws-cdk-lib/aws-s3';

export class MyCdkAppStack extends cdk.Stack {
  constructor(scope: cdk.App, id: string) {
    super(scope, id);

    new s3.Bucket(this, 'MyBucket', {
      versioned: true,
      encryption: s3.BucketEncryption.S3_MANAGED,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });
  }
}
```
4. Preview: `cdk diff` → See what will be created.
5. Synthesize: `cdk synth` → See the generated CloudFormation YAML.
6. Deploy: `cdk deploy` → Stack created.
7. Clean up: `cdk destroy`.

### Common Mistakes
- ❌ Not running `cdk diff` before `cdk deploy` — deploying blind changes.
- ❌ Using L1 constructs everywhere — defeats the purpose of CDK. Use L2/L3.
- ❌ Not writing tests — CDK supports unit tests. Use them.
- ❌ Generating massive templates without understanding what CDK creates.
- ❌ Mixing CDK and console changes — same drift problem as CloudFormation.

### Pro Tips
- ✅ Use **L2 constructs** as default — they have sensible, secure defaults.
- ✅ Use **L3 patterns** for common architectures (Fargate + ALB, Lambda + API Gateway).
- ✅ Write **CDK unit tests** with the `assertions` module — test your infra like app code.
- ✅ Use `cdk diff` in CI/CD to show reviewers exactly what will change.
- ✅ Use **CDK Pipelines** construct for self-mutating CI/CD (pipeline updates itself).
- ✅ In interviews: "We use CDK with TypeScript for all new infrastructure. L2 constructs provide secure defaults. We write unit tests for our stacks, run cdk diff in pull requests for review, and use CDK Pipelines for self-mutating deployment pipelines."

---

## 4. Drift Detection

### What is it?
Drift detection checks if your actual AWS resources still match what CloudFormation thinks they should be.
If someone changed a Security Group in the console, that's **drift** — the real state doesn't match the template.
Drift is dangerous because your IaC no longer represents reality.

### Why it matters in production
- Drift means your template is lying. Deploying it might overwrite manual changes or cause outages.
- Compliance audits require proof that infrastructure matches defined baselines.
- Drift detection catches unauthorized or accidental changes.

### Core Ideas
- **In-sync** — Resource matches the template. All good.
- **Modified** — Resource exists but properties have been changed outside CloudFormation.
- **Deleted** — Resource was deleted outside CloudFormation. Template still expects it.
- **Not checked** — Drift detection hasn't been run or doesn't support this resource type.

**Common causes of drift:**
- Someone edited a Security Group in the console.
- An automation script changed a parameter directly via AWS CLI.
- A team member "quick-fixed" a production issue by changing a resource manually.
- Auto Scaling modified capacity outside of CloudFormation.

**How to handle drift:**
1. **Detect it** — Run drift detection regularly.
2. **Investigate** — Was it intentional? Emergency fix? Unauthorized?
3. **Resolve** — Either update the template to match reality OR reimport the resource, OR re-deploy the template to overwrite the drift.

### Quick Analogy
Drift = Remodeling a house without updating the blueprints.
You added a new door (console change) but the blueprints (template) still show a wall. Now anyone building from the blueprints will get the wrong house. And if you "rebuild from blueprints" (redeploy), the door disappears.

### Architecture View
```
Normal (No Drift):
  Template says:         Reality:
  SG: Port 443 open     SG: Port 443 open  ✅ IN SYNC

Drift Detected:
  Template says:         Reality:
  SG: Port 443 open     SG: Port 443 + 22 open  ⚠️ DRIFTED
                         (someone added SSH in console)

What happens on next deployment?
  CloudFormation redeploys template → Port 22 is REMOVED
  → Someone's SSH access breaks
  → They blame you

Resolution Options:
  Option A: Update template to include port 22 → Deploy → Back in sync
  Option B: Remove port 22 from reality → Redeploy → Back in sync
  Option C: Import the drifted resource → CloudFormation adopts the real state
```

### Hands-On (Step-by-step Lab)
1. Create a stack with a Security Group (allow port 443).
2. Go to the **AWS Console → EC2 → Security Groups** → Manually add port 22 (SSH).
3. Go to **CloudFormation → Stack → Drift Detection → Detect Drift**.
4. Wait → Status: **DRIFTED**.
5. View drift details → See: "Port 22 was added outside CloudFormation."
6. **Fix Option A:** Update your template to include port 22 → Update stack.
7. **Fix Option B:** Remove port 22 in console → Run drift detection again → IN SYNC.

### Common Mistakes
- ❌ Never running drift detection — you don't know your infra has drifted.
- ❌ Making "temporary" console changes that become permanent drift.
- ❌ Redeploying a stack without checking for drift — overwrites someone's emergency fix.
- ❌ Ignoring drift alerts — "we'll fix it later" turns into production incidents.
- ❌ Not having a policy against manual console changes.

### Pro Tips
- ✅ **Run drift detection weekly** via a scheduled Lambda or AWS Config rule.
- ✅ Use **AWS Config rule `cloudformation-stack-drift-detection-check`** for continuous monitoring.
- ✅ Create a **team policy: no manual console changes** for CloudFormation-managed resources.
- ✅ Use **resource import** (`aws cloudformation create-change-set --change-set-type IMPORT`) to adopt manually created resources into a stack.
- ✅ For emergency fixes: Make the change in console AND update the template immediately.
- ✅ In interviews: "We run drift detection weekly using AWS Config rules. Our team policy prohibits manual console changes on IaC-managed resources. Emergency changes require a corresponding template update within 24 hours. Drift alerts go to our Slack channel for immediate investigation."
