# ECS & Fargate

---

## 1. ECS (Elastic Container Service)

### What is it?
ECS is AWS's fully managed container orchestration service.
You give it Docker containers. ECS runs, scales, and monitors them for you.
It's AWS-native — deeply integrated with ALB, IAM, CloudWatch, VPC, and other services.

### Why it matters in production
- Containers are the standard for packaging and deploying applications.
- ECS removes the complexity of managing your own orchestrator.
- It's the fastest path from Docker to production on AWS.

### Core Ideas

**Key components:**

| Component | What It Is |
|-----------|-----------|
| **Cluster** | Logical grouping of tasks/services. Think of it as the "environment". |
| **Task Definition** | Blueprint for your container(s). Like a Docker Compose file. |
| **Task** | A running instance of a task definition. One or more containers. |
| **Service** | Manages desired count of tasks. Auto-restarts failed tasks. Integrates with ALB. |
| **Container Instance** | EC2 instance running the ECS agent (EC2 launch type only). |

**Two launch types:**

| Feature | EC2 Launch Type | Fargate Launch Type |
|---------|----------------|-------------------|
| **Infrastructure** | You manage EC2 instances | AWS manages infrastructure |
| **Scaling** | You scale EC2 + tasks | You scale tasks only |
| **Pricing** | Pay for EC2 instances | Pay per task (vCPU + memory) |
| **Control** | Full OS access, GPU, custom AMI | No OS access |
| **Patching** | You patch the OS | AWS patches |
| **Best for** | GPU, large workloads, cost control | Most workloads, simplicity |

**ECS integrations:**
- **ALB** — Route traffic to tasks. Path-based routing to different services.
- **IAM Task Roles** — Each task gets its own IAM permissions.
- **CloudWatch** — Logs, metrics, alarms.
- **ECR** — Store Docker images (AWS's container registry).
- **Secrets Manager / SSM** — Inject secrets into containers at runtime.
- **App Mesh** — Service mesh for microservices.
- **Service Discovery (Cloud Map)** — Tasks register DNS names automatically.

### Quick Analogy
ECS = A shipping port manager.
You build the cargo containers (Docker images). The port manager (ECS) loads them onto ships (compute), ensures the right number are running, and replaces any that fall overboard (self-healing). You choose: your own ships (EC2) or rented ships (Fargate).

### Architecture View
```
ECS Architecture:
┌─────────────────────────────────────────────────────────┐
│ ECS Cluster: production                                 │
│                                                         │
│  ┌──────────────────────────────────────────────┐       │
│  │ Service: web-api (desired: 3)                │       │
│  │                                              │       │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐      │       │
│  │ │ Task 1   │ │ Task 2   │ │ Task 3   │      │       │
│  │ │ (AZ-1a)  │ │ (AZ-1b)  │ │ (AZ-1a)  │      │       │
│  │ │┌────────┐│ │┌────────┐│ │┌────────┐│      │       │
│  │ ││  App   ││ ││  App   ││ ││  App   ││      │       │
│  │ ││Container│ ││Container│ ││Container│      │       │
│  │ │└────────┘│ │└────────┘│ │└────────┘│      │       │
│  │ │┌────────┐│ │┌────────┐│ │┌────────┐│      │       │
│  │ ││Sidecar ││ ││Sidecar ││ ││Sidecar ││      │       │
│  │ ││(logging)│ ││(logging)│ ││(logging)│      │       │
│  │ │└────────┘│ │└────────┘│ │└────────┘│      │       │
│  │ └──────────┘ └──────────┘ └──────────┘      │       │
│  └──────────────────────────────────────────────┘       │
│           ▲                                             │
│           │                                             │
│  ALB ─────┘ (distributes traffic across tasks)          │
│                                                         │
│  ECR: stores Docker images                              │
│  CloudWatch: logs + metrics                             │
│  IAM: task roles per service                            │
└─────────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **Push an image to ECR:**
```bash
aws ecr create-repository --repository-name my-app
docker build -t my-app .
docker tag my-app:latest <account-id>.dkr.ecr.<region>.amazonaws.com/my-app:latest
aws ecr get-login-password | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
docker push <account-id>.dkr.ecr.<region>.amazonaws.com/my-app:latest
```
2. Go to **ECS → Create Cluster** → Select **Fargate** (or EC2 if preferred).
3. Create a **Task Definition** (next section covers details).
4. Create a **Service** → Desired tasks: 2 → Attach ALB.
5. Wait for tasks to start → Access via ALB DNS.
6. Stop a task manually → Watch ECS auto-launch a replacement.

### Common Mistakes
- ❌ Running containers on raw EC2 without an orchestrator — no auto-restart, no scaling.
- ❌ Using EC2 launch type when Fargate would be simpler and cheaper for the workload.
- ❌ Not using task-level IAM roles — giving all containers the same broad permissions.
- ❌ Not enabling CloudWatch Logs — can't debug container crashes.
- ❌ Putting all containers in one task definition — they should be separate services.

### Pro Tips
- ✅ **Start with Fargate.** Switch to EC2 only if you need GPU, custom AMI, or deeper cost control.
- ✅ Use **ECR** for storing images. Enable image scanning for vulnerabilities.
- ✅ Use **task IAM roles** for least-privilege access per container.
- ✅ Use **Service Discovery (Cloud Map)** for internal service-to-service communication.
- ✅ Use **ECS Exec** (`aws ecs execute-command`) to debug running containers — like `docker exec`.
- ✅ In interviews: "We run containerized microservices on ECS Fargate. Each service has its own task definition with a dedicated IAM role. ALB routes traffic with path-based rules. Images are stored in ECR with vulnerability scanning. We use ECS Exec for debugging and CloudWatch for observability."

---

## 2. Task Definition

### What is it?
A Task Definition is the **blueprint** for running containers on ECS.
It specifies: which Docker image, how much CPU/memory, environment variables, ports, volumes, IAM role, and logging config.
Think of it as a Docker Compose file for ECS.

### Why it matters in production
- Every container ECS runs comes from a task definition.
- It controls resource allocation, networking, secrets injection, and logging.
- Task definitions are versioned — you deploy new versions for updates.

### Core Ideas

**Key fields in a task definition:**

| Field | What It Defines | Example |
|-------|----------------|---------|
| **Family** | Name of the task definition | `web-api` |
| **Container definitions** | One or more containers in the task | App container + sidecar |
| **Image** | Docker image URI | `123456.dkr.ecr.us-east-1.amazonaws.com/app:v2` |
| **CPU / Memory** | Resource allocation | 256 CPU (0.25 vCPU), 512 MB |
| **Port mappings** | Container port → Host port | 8080 → 8080 |
| **Environment variables** | Config injected at runtime | `ENV=production` |
| **Secrets** | From Secrets Manager / SSM | `DB_PASSWORD` from SSM |
| **Log configuration** | Where logs go | CloudWatch Logs (`awslogs` driver) |
| **Task Role** | IAM role for the container | S3 read, DynamoDB write |
| **Execution Role** | IAM role for ECS agent | Pull image from ECR, write logs |
| **Network mode** | How networking works | `awsvpc` (Fargate requires this) |
| **Volumes** | Storage mounts | EFS, bind mount, Docker volume |

**Task Role vs Execution Role:**

| Role | Who Uses It | For What |
|------|------------|---------|
| **Task Role** | Your application code inside the container | Access AWS services (S3, DynamoDB, SQS) |
| **Execution Role** | ECS agent (infrastructure) | Pull images from ECR, push logs to CloudWatch, fetch secrets |

**Revisions:**
- Every update creates a new **revision** (e.g., `web-api:1`, `web-api:2`, `web-api:3`).
- You update the service to use the new revision → Rolling deployment.

### Quick Analogy
Task Definition = A recipe card.
It lists: ingredients (Docker image), quantities (CPU/memory), cooking instructions (commands), plating (port mapping), and who's allowed in the kitchen (IAM role). Each time you modify the recipe, you get a new version number.

### Architecture View
```
Task Definition: web-api (Revision 3)
┌─────────────────────────────────────────────────┐
│ Network Mode: awsvpc                            │
│ CPU: 512 (0.5 vCPU)  Memory: 1024 MB           │
│ Task Role: web-api-task-role                    │
│ Execution Role: ecsTaskExecutionRole            │
│                                                 │
│ Container 1: app                                │
│ ├── Image: 123456.dkr.ecr.../app:v3            │
│ ├── Port: 8080                                  │
│ ├── CPU: 384    Memory: 768 MB                  │
│ ├── Env: ENV=production                         │
│ ├── Secrets: DB_PASS → arn:aws:ssm:.../db-pass  │
│ ├── Logs: CloudWatch → /ecs/web-api             │
│ └── Health Check: curl http://localhost:8080/health│
│                                                 │
│ Container 2: log-router (sidecar)               │
│ ├── Image: fluent-bit:latest                    │
│ ├── CPU: 128    Memory: 256 MB                  │
│ ├── Logs: CloudWatch → /ecs/log-router          │
│ └── Essential: false (app continues if this fails)│
│                                                 │
│ Volumes:                                        │
│ └── EFS: /mnt/shared-data                       │
└─────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **ECS → Task Definitions → Create new**.
2. Family: `web-api`. Launch type: **Fargate**.
3. Task size: 0.5 vCPU, 1 GB memory.
4. Add container:
   - Name: `app`. Image: `nginx:latest`. Port: 80.
   - Log driver: `awslogs`. Log group: `/ecs/web-api`.
5. Task Role: Create one with `AmazonDynamoDBReadOnlyAccess`.
6. Execution Role: Use `ecsTaskExecutionRole` (auto-created).
7. Create → You now have `web-api:1`.
8. Update the image tag to `nginx:alpine` → Create new revision → `web-api:2`.
9. Update your ECS Service to use revision 2 → Rolling deployment starts.

### Common Mistakes
- ❌ Confusing Task Role with Execution Role — one is for your app, one is for ECS infrastructure.
- ❌ Hardcoding secrets in environment variables — use Secrets Manager or SSM references.
- ❌ Not configuring log drivers — container crashes with no logs = impossible to debug.
- ❌ Not setting health checks in the task definition — ECS can't detect unhealthy containers.
- ❌ Over-allocating CPU/memory — paying for unused resources.

### Pro Tips
- ✅ **Always use `awsvpc` network mode** for Fargate. Each task gets its own ENI and private IP.
- ✅ Inject secrets with `valueFrom` pointing to **Secrets Manager or SSM** — never environment variables.
- ✅ Use **sidecar containers** for logging (Fluent Bit), monitoring (Datadog agent), or proxying (Envoy).
- ✅ Set **container health checks** so ECS replaces unhealthy tasks.
- ✅ Version your images with tags (`v1.2.3`), not `latest` — `latest` is unpredictable in production.
- ✅ In interviews: "Our task definitions specify exact image versions, inject secrets from SSM via valueFrom, use awsvpc networking for task-level ENIs, and include health checks. Each service has its own scoped IAM task role. We use sidecar containers for centralized logging with Fluent Bit."

---

## 3. Fargate

### What is it?
Fargate is a **serverless compute engine** for containers on ECS (and EKS).
You define CPU and memory for your task. AWS handles the underlying infrastructure.
No EC2 instances to manage, patch, or scale. Just containers.

### Why it matters in production
- Fargate eliminates the undifferentiated heavy lifting of managing EC2 for containers.
- You focus on your application, not the OS, patching, or cluster capacity.
- It's the recommended starting point for most container workloads.

### Core Ideas
- **No servers** — You never see or manage EC2 instances.
- **Per-task pricing** — Pay for the vCPU and memory your task uses, per second.
- **Networking** — Each task gets its own ENI (private IP) in your VPC via `awsvpc` mode.
- **Security** — Task-level isolation. Each task runs in its own kernel runtime environment.
- **Scaling** — ECS Service Auto Scaling adjusts the number of tasks. No cluster capacity planning.
- **Storage** — 20 GB ephemeral storage per task (expandable to 200 GB). Can mount EFS for persistent shared storage.

**Fargate pricing (us-east-1, approximate):**

| Resource | Per Second | Per Hour |
|----------|-----------|---------|
| vCPU | $0.000011 | ~$0.04 |
| Memory (GB) | $0.0000012 | ~$0.004 |

*A task with 0.5 vCPU + 1 GB memory ≈ $0.025/hour ≈ $18/month (24/7)*

**Fargate vs EC2 launch type:**

| Scenario | Best Choice |
|----------|------------|
| Small-medium workloads, simplicity | **Fargate** |
| You need GPU | EC2 |
| You need custom AMI or kernel modules | EC2 |
| Variable workloads (scale to zero) | **Fargate** |
| Very large, predictable workloads (cost-optimize) | EC2 (with Reserved/Savings Plans) |
| You don't want to manage anything | **Fargate** |

### Quick Analogy
EC2 launch type = Renting a truck and packing it yourself. You choose the truck, load the containers, drive, and maintain it.
Fargate = Calling a courier service. You hand over the package (container). They handle the truck, the route, and the delivery. You just pay per package.

### Architecture View
```
ECS with Fargate:
┌─────────────────────────────────────────────────┐
│ ECS Cluster (Fargate)                           │
│                                                 │
│   YOU MANAGE:                                   │
│   ┌───────────────┐ ┌───────────────┐           │
│   │ Task Def      │ │ Service Config│           │
│   │ (image, CPU,  │ │ (desired count│           │
│   │  memory, IAM) │ │  ALB, scaling)│           │
│   └───────────────┘ └───────────────┘           │
│                                                 │
│   AWS MANAGES:                                  │
│   ┌─────────────────────────────────────────┐   │
│   │ Compute infrastructure                  │   │
│   │ OS patching                             │   │
│   │ Cluster capacity                        │   │
│   │ Container runtime                       │   │
│   │ Security isolation                      │   │
│   └─────────────────────────────────────────┘   │
│                                                 │
│  Task 1 (AZ-1a)    Task 2 (AZ-1b)              │
│  ┌──────────────┐  ┌──────────────┐             │
│  │ ENI: 10.0.1.5│  │ENI: 10.0.2.8│             │
│  │ ┌──────────┐ │  │ ┌──────────┐ │             │
│  │ │   App    │ │  │ │   App    │ │             │
│  │ └──────────┘ │  │ └──────────┘ │             │
│  └──────────────┘  └──────────────┘             │
│          ▲                 ▲                     │
│          └────── ALB ──────┘                     │
└─────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. Go to **ECS → Create Cluster** → Infrastructure: **AWS Fargate**.
2. Name: `demo-cluster`.
3. Create a task definition (Fargate compatible): 0.25 vCPU, 0.5 GB, nginx image.
4. Create a **Service**: Launch type Fargate, desired tasks: 2, VPC subnets (private), ALB.
5. Wait for tasks to start → Access via ALB DNS → See Nginx page.
6. Go to **ECS → Service → Update** → Change desired count to 4 → Watch 2 more tasks spin up.
7. Set up **Service Auto Scaling**: Target tracking on CPU utilization at 50%.

### Common Mistakes
- ❌ Using Fargate for GPU workloads — Fargate doesn't support GPUs. Use EC2.
- ❌ Over-provisioning CPU/memory — you pay for what you allocate, not what you use.
- ❌ Not using private subnets for Fargate tasks — always run in private subnets behind an ALB.
- ❌ Forgetting that Fargate tasks need NAT Gateway for outbound internet (or VPC endpoints for ECR/CloudWatch).
- ❌ Not configuring service auto scaling — tasks stay at fixed count, wasting money or dropping requests.

### Pro Tips
- ✅ **Start with Fargate for every new workload.** Migrate to EC2 only if Fargate doesn't meet requirements.
- ✅ Use **Fargate Spot** for fault-tolerant workloads — up to 70% cheaper. Tasks can be interrupted.
- ✅ Use **ECS Service Auto Scaling** — target tracking on CPU, memory, or ALB request count.
- ✅ Use **VPC endpoints** for ECR, CloudWatch Logs, and Secrets Manager — avoids NAT Gateway costs.
- ✅ Apply **Savings Plans** (Compute) to Fargate — up to 50% savings with 1-year commitment.
- ✅ In interviews: "We default to Fargate for all container workloads. No infrastructure to manage, per-second billing, and task-level isolation. We use Fargate Spot for batch jobs and Compute Savings Plans for steady workloads. VPC endpoints reduce NAT costs."

---

## 4. When to Use ECS

### What is it?
ECS is not the only way to run containers on AWS. You can also use EKS, Lambda, App Runner, or Lightsail.
Choosing ECS is a design decision based on team skills, complexity needs, and AWS integration depth.
This section helps you decide when ECS is the right fit.

### Why it matters in production
- Choosing the wrong container platform wastes time and money.
- "Why ECS over EKS?" is a guaranteed interview question.
- The answer depends on team size, existing skills, and architecture needs.

### Core Ideas

**When to choose ECS:**
- Your team is **AWS-native** and doesn't need Kubernetes.
- You want **simplicity** — ECS is easier to learn and operate than EKS.
- You want **deep AWS integration** out of the box (ALB, IAM, CloudWatch, Secrets Manager).
- You're running **microservices** that don't need Kubernetes-specific features.
- You prefer **Fargate** for serverless containers (EKS also supports Fargate, but ECS is simpler).
- You have a **small to medium team** without dedicated Kubernetes expertise.

**When NOT to choose ECS:**
- Your team already has strong **Kubernetes expertise**.
- You need **portability** — avoid vendor lock-in, run on any cloud or on-prem.
- You need Kubernetes-specific features (Helm, CRDs, operators, service mesh, advanced scheduling).
- You have a **multi-cloud strategy** or might migrate away from AWS.
- Your existing infra is already on Kubernetes.

**AWS container service comparison:**

| Service | Orchestrator | Best For | Complexity |
|---------|-------------|---------|------------|
| **ECS + Fargate** | AWS-native | Most containerized apps, simplicity | Low |
| **ECS + EC2** | AWS-native | GPU, custom AMI, cost control | Medium |
| **EKS + Fargate** | Kubernetes | K8s ecosystem, serverless K8s pods | Medium-High |
| **EKS + EC2** | Kubernetes | Full K8s control, complex orchestration | High |
| **App Runner** | Fully managed | Simple web apps, fastest path to deploy | Very Low |
| **Lambda** | Serverless | Event-driven, short-lived tasks | Low |

### Quick Analogy
Choosing a container platform = Choosing how to get to work.
- **App Runner** = Uber (just get in, it takes you there).
- **ECS Fargate** = Company shuttle bus (easy, reliable, handles most routes).
- **ECS EC2** = Driving your own car (more control, but you maintain it).
- **EKS** = Flying your own plane (maximum control and range, but requires a pilot's license).

### Architecture View
```
Decision Tree:
                    Need containers?
                         │
                    ┌────▼────┐
                    │   YES   │
                    └────┬────┘
                         │
              Need Kubernetes specifically?
                    ┌────┼────┐
                    │         │
                   YES        NO
                    │         │
                    ▼         ▼
                  EKS       ECS
                    │         │
              Need servers?  Need servers?
               ┌────┼───┐   ┌───┼────┐
               │         │   │        │
              YES       NO  YES      NO
               │         │   │        │
               ▼         ▼   ▼        ▼
            EKS+EC2  EKS+   ECS+   ECS+
                   Fargate   EC2  Fargate ← DEFAULT RECOMMENDATION
```

### Hands-On (Step-by-step Lab)
1. **Scenario:** You need to deploy a Node.js API with 3 microservices.
2. **Team:** 3 DevOps engineers, AWS-experienced, no Kubernetes experience.
3. **Decision:** ECS + Fargate (simple, AWS-native, no K8s overhead).
4. Deploy Service 1: `users-api` on Fargate with ALB path `/users/*`.
5. Deploy Service 2: `orders-api` on Fargate with ALB path `/orders/*`.
6. Deploy Service 3: `payments-api` on Fargate with ALB path `/payments/*`.
7. All three share one ALB, one cluster, different task definitions and IAM roles.

### Common Mistakes
- ❌ Choosing EKS because "Kubernetes is popular" — ECS is simpler for AWS-only teams.
- ❌ Choosing ECS when the team already knows Kubernetes — retraining wastes time.
- ❌ Using ECS + EC2 when Fargate would be simpler.
- ❌ Running a single monolith on ECS — might be simpler on Elastic Beanstalk or App Runner.
- ❌ Not considering Lambda for simple event-driven tasks — not everything needs a container.

### Pro Tips
- ✅ **Default to ECS + Fargate** for AWS-native teams building microservices.
- ✅ Choose **EKS** only if you need Kubernetes ecosystem (Helm, Istio, ArgoCD, multi-cloud).
- ✅ Consider **App Runner** for simple web apps that don't need complex networking or scaling config.
- ✅ Consider **Lambda** for event-driven, short-lived workloads under 15 minutes.
- ✅ You can run ECS and EKS in the same account for different workloads.
- ✅ In interviews: "We choose ECS Fargate as our default for containerized microservices because our team is AWS-native and it offers the simplest path to production with deep ALB, IAM, and CloudWatch integration. We'd choose EKS if we needed Kubernetes portability or ecosystem tools like Helm and ArgoCD."
