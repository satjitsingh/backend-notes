# CI/CD on AWS

---

## 1. CodePipeline

### What is it?
CodePipeline is a fully managed **continuous delivery** service.
It automates the steps to release software: source → build → test → deploy.
You define stages, and CodePipeline orchestrates the flow automatically on every code change.

### Why it matters in production
- Manual deployments are slow, error-prone, and inconsistent.
- CodePipeline automates the entire release process — from Git push to production.
- It's the backbone of CI/CD on AWS.

### Core Ideas

**Pipeline stages:**

| Stage | What Happens | AWS Service |
|-------|-------------|-------------|
| **Source** | Detect code changes | CodeCommit, GitHub, S3, ECR |
| **Build** | Compile, test, package | CodeBuild |
| **Test** | Run integration/e2e tests | CodeBuild, third-party tools |
| **Approval** | Manual approval gate | SNS notification |
| **Deploy** | Push to production | CodeDeploy, ECS, CloudFormation, S3, Lambda |

**Key concepts:**
- **Pipeline** — The full workflow from source to deploy.
- **Stage** — A logical step (Source, Build, Deploy). Stages run sequentially.
- **Action** — A task within a stage. Actions within a stage can run in parallel.
- **Artifact** — Output from one stage, input to the next (e.g., build output → deploy input).
- **Transition** — The link between stages. Can be disabled to pause the pipeline.
- **Trigger** — What starts the pipeline (code push, schedule, manual).

### Quick Analogy
CodePipeline = A factory assembly line.
Raw materials come in (source code). Station 1 cuts and shapes them (build). Station 2 inspects quality (test). A manager signs off (approval). Station 3 packages and ships (deploy). The whole line runs automatically every time new materials arrive.

### Architecture View
```
CodePipeline: my-app-pipeline

┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│  SOURCE  │──▶│  BUILD   │──▶│  TEST    │──▶│ APPROVAL │──▶│  DEPLOY  │
│          │   │          │   │          │   │          │   │          │
│ GitHub   │   │CodeBuild │   │CodeBuild │   │ Manual   │   │CodeDeploy│
│ (main)   │   │          │   │          │   │ (SNS)    │   │ or ECS   │
│          │   │ compile  │   │ integ    │   │          │   │ or CF    │
│          │   │ unit test│   │ tests    │   │ Prod     │   │          │
│          │   │ docker   │   │ security │   │ gate     │   │ Blue/    │
│          │   │ build    │   │ scan     │   │          │   │ Green    │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
     │              │               │              │              │
     │         Artifacts:      Artifacts:      Email to       Deploy to
  Webhook     build.zip       test-report     on-call team   production
  trigger     docker image
```

### Hands-On (Step-by-step Lab)
1. Go to **CodePipeline → Create Pipeline**.
2. Name: `my-app-pipeline`.
3. **Source stage:** GitHub (v2 connection) → Select your repo and branch.
4. **Build stage:** CodeBuild → Create a new project (next section).
5. **Deploy stage:** Amazon ECS → Select your cluster and service.
6. Create → Push a code change to GitHub.
7. Watch the pipeline: Source ✅ → Build ✅ → Deploy ✅.
8. Add a **manual approval** stage between Build and Deploy for production.

### Common Mistakes
- ❌ No approval gate before production deployment — risky changes go straight to prod.
- ❌ Not using artifacts between stages — build output doesn't reach deploy.
- ❌ One massive pipeline for everything — create separate pipelines per microservice.
- ❌ Not monitoring pipeline failures — no alerts when a stage fails.
- ❌ Not securing the pipeline IAM role — it can access your entire account.

### Pro Tips
- ✅ **Always add a manual approval** before production deploys.
- ✅ Use **pipeline notifications** → SNS → Slack/email on failure.
- ✅ Use **cross-account deployments** — pipeline in DevOps account, deploy to prod account.
- ✅ Store **pipeline definition as code** (CloudFormation or CDK) in Git.
- ✅ Use **pipeline variables** for dynamic values passed between stages.
- ✅ In interviews: "Our CI/CD pipeline uses CodePipeline with GitHub as source, CodeBuild for building and testing, a manual approval gate for production, and CodeDeploy for blue/green deployments. Pipeline definition is in CDK, and failures trigger Slack alerts."

---

## 2. CodeBuild

### What is it?
CodeBuild is a fully managed **build service**. It compiles code, runs tests, and produces deployable artifacts.
No build servers to manage. It scales automatically — runs as many builds as you need in parallel.
You define build instructions in a `buildspec.yml` file.

### Why it matters in production
- The build step is where code turns into deployable artifacts (JARs, Docker images, zip files).
- CodeBuild runs on-demand — no Jenkins servers to maintain, patch, or scale.
- It integrates natively with CodePipeline, ECR, S3, and Secrets Manager.

### Core Ideas

**`buildspec.yml` structure:**

```yaml
version: 0.2

env:
  variables:
    ENV: production
  secrets-manager:
    DB_PASS: prod/db/password

phases:
  install:
    runtime-versions:
      nodejs: 18
    commands:
      - npm install

  pre_build:
    commands:
      - echo "Running unit tests..."
      - npm test
      - echo "Logging into ECR..."
      - aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_REPO

  build:
    commands:
      - echo "Building Docker image..."
      - docker build -t $ECR_REPO:$CODEBUILD_RESOLVED_SOURCE_VERSION .
      - docker push $ECR_REPO:$CODEBUILD_RESOLVED_SOURCE_VERSION

  post_build:
    commands:
      - echo "Writing image definition for ECS..."
      - printf '[{"name":"app","imageUri":"%s"}]' $ECR_REPO:$CODEBUILD_RESOLVED_SOURCE_VERSION > imagedefinitions.json

artifacts:
  files:
    - imagedefinitions.json

cache:
  paths:
    - 'node_modules/**/*'
```

**Key concepts:**

| Concept | What It Does |
|---------|-------------|
| **Build project** | Configuration: source, environment, buildspec, artifacts |
| **Build environment** | Docker container where the build runs (managed images or custom) |
| **Buildspec** | YAML file defining build phases and commands |
| **Phases** | `install`, `pre_build`, `build`, `post_build` |
| **Artifacts** | Output files passed to the next pipeline stage |
| **Cache** | Persist dependencies between builds (S3 or local cache) |
| **Environment variables** | Static, from Secrets Manager, or from SSM Parameter Store |
| **Compute types** | Small (3 GB), Medium (7 GB), Large (15 GB), 2XLarge (145 GB) |

### Quick Analogy
CodeBuild = A cloud kitchen.
You give it a recipe (`buildspec.yml`) and ingredients (source code). It rents a kitchen (compute environment), cooks (builds), tests the dish (runs tests), plates it (artifacts), and shuts down. You only pay for the time the kitchen was used.

### Architecture View
```
CodeBuild Workflow:

Source Code (GitHub/CodeCommit)
         │
         ▼
┌──────────────────────────────────────────┐
│ CodeBuild Environment (Docker container) │
│                                          │
│ Phase 1: INSTALL                         │
│   → Install Node.js 18, npm install      │
│                                          │
│ Phase 2: PRE_BUILD                       │
│   → Run unit tests                       │
│   → Login to ECR                         │
│                                          │
│ Phase 3: BUILD                           │
│   → Docker build                         │
│   → Docker push to ECR                   │
│                                          │
│ Phase 4: POST_BUILD                      │
│   → Write image definitions              │
│   → Create deployment artifacts          │
└──────────────────────┬───────────────────┘
                       │
                       ▼
              Artifacts (S3) → Next pipeline stage
              Logs → CloudWatch Logs
              Reports → Test/coverage reports
```

### Hands-On (Step-by-step Lab)
1. Create a `buildspec.yml` in your repo root:
```yaml
version: 0.2
phases:
  install:
    runtime-versions:
      python: 3.11
    commands:
      - pip install -r requirements.txt
  build:
    commands:
      - echo "Running tests..."
      - python -m pytest tests/
      - echo "Build complete"
artifacts:
  files:
    - '**/*'
```
2. Go to **CodeBuild → Create Build Project**.
3. Source: GitHub. Environment: Amazon Linux, Standard image.
4. Buildspec: Use the `buildspec.yml` from repo.
5. Click **Start Build** → Watch the phases execute.
6. Check logs in **CloudWatch Logs**.
7. Integrate with CodePipeline as the Build stage.

### Common Mistakes
- ❌ Not using a `buildspec.yml` — defining commands in the console (not version-controlled).
- ❌ Hardcoding secrets in buildspec — use `secrets-manager` or `parameter-store` environment.
- ❌ Not caching dependencies — every build re-downloads everything (slow and expensive).
- ❌ Choosing too small a compute type — builds time out on large projects.
- ❌ Not checking build logs when a stage fails.

### Pro Tips
- ✅ **Always use `buildspec.yml`** in your repo — version-controlled, reviewable.
- ✅ Use **caching** (`cache` section) for `node_modules`, `.m2`, `pip cache` — builds 2-5x faster.
- ✅ Use **Secrets Manager references** for API keys, Docker Hub credentials.
- ✅ Use **build reports** to publish test results and code coverage to CodeBuild.
- ✅ Use **custom Docker images** as the build environment for complex toolchains.
- ✅ In interviews: "Our buildspec.yml is in Git. It runs unit tests, builds a Docker image, pushes to ECR, and produces artifacts. Secrets come from Secrets Manager. Dependencies are cached in S3. Build reports show test coverage."

---

## 3. CodeDeploy

### What is it?
CodeDeploy automates **application deployments** to EC2, Lambda, ECS, and on-premises servers.
It handles rolling updates, blue/green deployments, and automatic rollbacks.
No downtime deployments. If something fails, it rolls back automatically.

### Why it matters in production
- Deploying manually to production servers is risky and slow.
- CodeDeploy gives you zero-downtime deployments with automatic rollback.
- It's the deployment engine behind CodePipeline's deploy stage.

### Core Ideas

**Deployment targets:**

| Target | Deployment Type | How |
|--------|----------------|-----|
| **EC2 / On-prem** | In-place or Blue/Green | CodeDeploy agent on each instance |
| **ECS (Fargate)** | Blue/Green | Creates new task set, shifts traffic |
| **Lambda** | Canary, Linear, All-at-once | Shifts traffic between function versions |

**Deployment configurations:**

| Strategy | How It Works | Risk |
|----------|-------------|------|
| **All-at-once** | Deploy to all targets simultaneously | High (all down if failure) |
| **Half-at-a-time** | Deploy to 50% of targets, then the rest | Medium |
| **One-at-a-time** | Deploy to one target at a time | Low (slow) |
| **Blue/Green** | Deploy to new environment, switch traffic | Very low |
| **Canary** | Small % first, then all | Very low |
| **Linear** | Gradually shift traffic (e.g., 10% every 5 min) | Very low |

**Key files:**

| File | Purpose |
|------|---------|
| **`appspec.yml`** | Deployment instructions: what to deploy, where, lifecycle hooks |
| **`appspec.yml` (EC2)** | Files to copy, permissions, scripts to run at each lifecycle phase |
| **`appspec.yml` (ECS)** | Task definition, container name, port |
| **`appspec.yml` (Lambda)** | Function name, alias, current/target versions |

**EC2 lifecycle hooks (order of execution):**
```
ApplicationStop → DownloadBundle → BeforeInstall → Install →
AfterInstall → ApplicationStart → ValidateService
```

### Quick Analogy
CodeDeploy = A moving company with a safety guarantee.
They move your stuff (code) to the new house (servers) without breaking anything. If the new house has a problem (health check fails), they move everything back to the old house automatically (rollback).

### Architecture View
```
EC2 In-Place Deployment:
  ┌─────────┐   ┌─────────┐   ┌─────────┐
  │Server 1 │   │Server 2 │   │Server 3 │
  │ v1 → v2 │   │ v1 (wait)│   │ v1 (wait)│
  └─────────┘   └─────────┘   └─────────┘
  Step 1: Deploy to Server 1, validate ✅
  Step 2: Deploy to Server 2, validate ✅
  Step 3: Deploy to Server 3, validate ✅

ECS Blue/Green Deployment:
  ┌────────────────────────────────┐
  │ ALB                            │
  │   │                            │
  │   ├── Blue (current) ──▶ v1 tasks (100% traffic)
  │   │                            │
  │   └── Green (new) ──▶ v2 tasks (0% traffic, testing)
  │                                │
  │ After validation:              │
  │   Blue: 0% ──▶ terminated      │
  │   Green: 100% ──▶ becomes production
  └────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **EC2 deployment:**
   - Install the CodeDeploy agent on your EC2 instances.
   - Create `appspec.yml`:
```yaml
version: 0.0
os: linux
files:
  - source: /
    destination: /var/www/html
hooks:
  AfterInstall:
    - location: scripts/restart_server.sh
      timeout: 60
  ValidateService:
    - location: scripts/health_check.sh
      timeout: 60
```
   - Create a CodeDeploy application and deployment group.
   - Deploy → Watch lifecycle events in the console.

2. **ECS Blue/Green:**
   - Create ECS service with CodeDeploy deployment controller.
   - Push a new task definition revision.
   - CodeDeploy creates Green task set → Tests → Shifts traffic → Terminates Blue.

### Common Mistakes
- ❌ Forgetting to install the CodeDeploy agent on EC2 instances.
- ❌ `appspec.yml` not in the root of the deployment artifact.
- ❌ Not configuring automatic rollback — failed deployments stay in a broken state.
- ❌ Lifecycle hook scripts that exit non-zero — deployment fails but you don't know why.
- ❌ Not testing deployment in staging before production.

### Pro Tips
- ✅ **Always enable automatic rollback** on deployment failure and alarm triggers.
- ✅ Use **CloudWatch Alarms** as rollback triggers (error rate, latency, 5xx count).
- ✅ Use **Blue/Green for ECS** — it's the default and safest strategy.
- ✅ Test `appspec.yml` lifecycle hooks thoroughly in staging.
- ✅ Use **deployment groups** to separate staging and production.
- ✅ In interviews: "We use CodeDeploy for all deployments. ECS services use blue/green with automatic rollback triggered by CloudWatch alarms on error rate and latency. EC2 deployments use the one-at-a-time strategy with validation scripts at each lifecycle hook."

---

## 4. Blue/Green Deployments

### What is it?
Blue/Green is a deployment strategy where you run **two identical environments**.
Blue = current production. Green = new version.
You deploy to Green, test it, then switch all traffic from Blue to Green. If anything fails, switch back instantly.

### Why it matters in production
- Zero downtime during deployment.
- Instant rollback — just switch traffic back to Blue.
- You can test the new version in a production-like environment before real users see it.

### Core Ideas
- **Blue** — The current live environment serving real traffic.
- **Green** — The new version deployed and tested but not yet serving traffic.
- **Traffic switch** — Done at the load balancer level (ALB target group swap) or DNS level (Route 53 weighted).
- **Validation** — Run automated tests against Green before switching.
- **Rollback** — Simply route traffic back to Blue. Green gets terminated.
- **Cost** — You briefly run two environments (double cost during deployment window).

**AWS services that support Blue/Green:**

| Service | How Blue/Green Works |
|---------|---------------------|
| **CodeDeploy + ECS** | Creates new task set (Green), shifts ALB traffic, terminates old |
| **CodeDeploy + EC2** | Creates new ASG (Green), shifts ALB traffic, terminates old ASG |
| **Elastic Beanstalk** | Swap environment URLs |
| **Route 53** | Weighted routing: 100% Blue → 100% Green |
| **CloudFormation** | Create new stack (Green), switch DNS, delete old stack |

### Quick Analogy
Blue/Green = Moving to a new office.
Set up the new office (Green) with everything in place. On moving day, redirect the mailbox (traffic) to the new address. If the new office has problems, redirect back to the old one (rollback). Once confirmed, cancel the lease on the old office (terminate Blue).

### Architecture View
```
BEFORE DEPLOYMENT:
  Users ──▶ ALB ──▶ [Blue Target Group] ──▶ v1 Tasks (100%)
                    [Green Target Group] ──▶ (empty)

DURING DEPLOYMENT:
  Users ──▶ ALB ──▶ [Blue Target Group] ──▶ v1 Tasks (100%)
                    [Green Target Group] ──▶ v2 Tasks (testing ✅)

TRAFFIC SWITCH:
  Users ──▶ ALB ──▶ [Green Target Group] ──▶ v2 Tasks (100%)
                    [Blue Target Group] ──▶ v1 Tasks (draining...)

AFTER VALIDATION:
  Users ──▶ ALB ──▶ [Green Target Group] ──▶ v2 Tasks (100%)
                    [Blue Target Group] ──▶ (terminated)

ROLLBACK (if needed):
  Users ──▶ ALB ──▶ [Blue Target Group] ──▶ v1 Tasks (100%)
  (instant — Blue is still running during bake time)
```

### Hands-On (Step-by-step Lab)
1. Create an ECS service with **CodeDeploy** as the deployment controller.
2. Configure the ALB with two target groups: `blue-tg` and `green-tg`.
3. Deploy a new task definition revision.
4. CodeDeploy creates new tasks in `green-tg`.
5. Watch in the CodeDeploy console: Green tasks start, health checks pass.
6. CodeDeploy shifts traffic: 100% → Green.
7. **Bake time:** Wait 5-10 minutes. Monitor CloudWatch alarms.
8. If alarms fire → Automatic rollback to Blue.
9. If no alarms → Blue tasks terminated. Deployment complete.

### Common Mistakes
- ❌ No validation tests on Green before switching traffic.
- ❌ Terminating Blue immediately after switching — keep it for rollback during bake time.
- ❌ Not setting CloudWatch alarms as rollback triggers.
- ❌ Database schema changes that break Blue → Can't rollback cleanly.
- ❌ Forgetting that Blue/Green doubles cost temporarily — plan for it.

### Pro Tips
- ✅ Keep **database changes backward-compatible** — Blue and Green may both run during transition.
- ✅ Set a **bake time** (5-10 min) before terminating Blue — monitor for issues.
- ✅ Use **CloudWatch Alarms** (error rate, latency, 5xx) as automatic rollback triggers.
- ✅ For databases: Use **expand-and-contract** pattern — add new column (expand), deploy, then remove old column (contract).
- ✅ In interviews: "We use blue/green deployments for all ECS services via CodeDeploy. Green is validated with automated tests before traffic shifts. CloudWatch alarms on error rate and P99 latency trigger automatic rollback. A 10-minute bake time ensures stability before Blue is terminated."

---

## 5. Canary Deployments

### What is it?
A Canary deployment sends a **small percentage** of traffic to the new version first.
If the canary (small group) is healthy, gradually shift more traffic until 100% is on the new version.
If the canary shows errors, roll back — only a small percentage of users were affected.

### Why it matters in production
- Canary catches bugs that only appear under real production traffic.
- Blue/Green is all-or-nothing. Canary is gradual — lower risk.
- It's the gold standard for deploying to large-scale production systems.

### Core Ideas

**Canary vs Blue/Green:**

| Aspect | Blue/Green | Canary |
|--------|-----------|--------|
| **Traffic shift** | All at once (0% → 100%) | Gradual (1% → 5% → 25% → 100%) |
| **Risk** | Moderate (full switch) | Low (small % at a time) |
| **Rollback speed** | Instant | Instant (shift back to 0%) |
| **Duration** | Minutes | Minutes to hours |
| **Complexity** | Simple | Moderate (needs monitoring) |
| **Best for** | Most deployments | High-risk changes, large-scale systems |

**AWS Canary deployment options:**

| Service | How It Works |
|---------|-------------|
| **CodeDeploy + Lambda** | `Canary10Percent5Minutes` — 10% for 5 min, then 100% |
| **CodeDeploy + ECS** | `ECSCanary10Percent5Minutes` — Same for ECS tasks |
| **ALB Weighted Target Groups** | Manually set weights: 95% v1, 5% v2 |
| **Route 53 Weighted Routing** | DNS-level traffic splitting |
| **App Mesh** | Service mesh traffic splitting at the application layer |

**CodeDeploy traffic shift configurations:**

| Config | Behavior |
|--------|---------|
| `Canary10Percent5Minutes` | 10% for 5 min → 100% |
| `Canary10Percent15Minutes` | 10% for 15 min → 100% |
| `Linear10PercentEvery1Minute` | +10% every minute (10 steps) |
| `Linear10PercentEvery3Minutes` | +10% every 3 min (30 min total) |
| `AllAtOnce` | 100% immediately (no canary) |

### Quick Analogy
Canary deployment = Taste-testing a new recipe at a restaurant.
Before putting the new dish on the full menu, you offer it to 10% of customers (canary). If they love it (metrics are healthy), you roll it out to everyone. If they complain (errors spike), you pull it immediately — 90% of customers never noticed.

### Architecture View
```
Canary Deployment Timeline:

T=0 min:   [v1: 100%] [v2: 0%]     Deploy starts
T=1 min:   [v1: 90%]  [v2: 10%]    Canary: 10% on new version
                                     → Monitor error rate, latency
T=5 min:   Metrics OK? ──YES──▶ Continue
           ──NO──▶ ROLLBACK to [v1: 100%]

T=6 min:   [v1: 0%]   [v2: 100%]   Full rollout

Architecture:
  Users ──▶ ALB
              ├── Target Group v1 (weight: 90%) ──▶ Old tasks
              └── Target Group v2 (weight: 10%) ──▶ New tasks (canary)

              CloudWatch Alarms monitor:
              ├── Error rate > 1%?  → ROLLBACK
              ├── P99 latency > 500ms? → ROLLBACK
              └── 5xx count > 10? → ROLLBACK

Lambda Canary:
  ┌─────────────────────────────────────────┐
  │ Lambda Function: process-orders         │
  │                                         │
  │ Alias: live                             │
  │   ├── Version 5 (current): 90% traffic  │
  │   └── Version 6 (canary):  10% traffic  │
  │                                         │
  │ After 5 min (no alarms):                │
  │   Version 6: 100% traffic ✅            │
  └─────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
**Lambda Canary:**
1. Create a Lambda function → Publish version 1.
2. Create an **alias** `live` pointing to version 1.
3. Update the function code → Publish version 2.
4. Go to **CodeDeploy → Create Application** → Compute: Lambda.
5. Create deployment group with config: `Canary10Percent5Minutes`.
6. Add a CloudWatch alarm for function errors as a rollback trigger.
7. Deploy: Shift alias `live` from v1 to v2 using canary.
8. Watch: 10% goes to v2 for 5 minutes → If healthy → 100% to v2.

**ECS Canary:**
1. ECS service with CodeDeploy deployment controller.
2. Create deployment group with config: `ECSCanary10Percent5Minutes`.
3. Push new task definition → CodeDeploy starts canary.
4. 10% of ALB traffic goes to new tasks for 5 minutes.
5. CloudWatch alarms monitor → No alarms → Full rollout.

### Common Mistakes
- ❌ No monitoring during the canary window — defeats the purpose.
- ❌ Canary percentage too large (50%) — too many users affected if it fails.
- ❌ Canary window too short — not enough time to detect issues.
- ❌ Not having rollback alarms configured — manual rollback is too slow.
- ❌ Not testing the canary process in staging first.

### Pro Tips
- ✅ Start with **10% canary for 5-10 minutes** as a sensible default.
- ✅ Use **CloudWatch Alarms** on: error rate, latency (P99), 5xx count, custom business metrics.
- ✅ Use **Linear** deployment for even more gradual rollout (10% every 3 minutes).
- ✅ Combine canary with **synthetic monitoring** (CloudWatch Synthetics) — run automated tests against the canary.
- ✅ For databases: Ensure schema changes are backward-compatible (both versions coexist).
- ✅ In interviews: "We use canary deployments for all production releases. 10% of traffic goes to the new version for 10 minutes while CloudWatch alarms monitor error rate, latency, and business metrics. Automatic rollback triggers if any alarm fires. This limits blast radius to 10% of users if something goes wrong."
