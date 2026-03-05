# EKS (Elastic Kubernetes Service)

---

## 1. EKS (Elastic Kubernetes Service)

### What is it?
EKS is AWS's managed Kubernetes service. AWS runs the Kubernetes **control plane** for you.
You run your workloads (pods) on **worker nodes** — either EC2 instances or Fargate.
It's certified Kubernetes-conformant — all standard K8s tools and APIs work.

### Why it matters in production
- Kubernetes is the industry standard for container orchestration.
- EKS lets you use Kubernetes without managing the complex control plane.
- It's required if your organization has a multi-cloud strategy or existing K8s investments.

### Core Ideas
- **Managed Control Plane** — AWS runs etcd, API server, scheduler, controller manager across 3 AZs.
- **Worker Nodes** — You manage these. EC2 instances (managed node groups) or Fargate.
- **kubectl** — The CLI tool to interact with your EKS cluster.
- **kubeconfig** — Configuration file that tells kubectl how to connect to your cluster.
- **Pods** — The smallest deployable unit. One or more containers.
- **Services** — Expose pods internally (ClusterIP) or externally (LoadBalancer, NodePort).
- **Deployments** — Manage desired state of pods (replicas, updates, rollbacks).
- **Namespaces** — Logical isolation within a cluster (e.g., dev, staging, prod).

**EKS pricing:**
- **Control plane:** $0.10/hour (~$73/month) per cluster.
- **Worker nodes:** EC2 pricing (On-Demand, Spot, Reserved) or Fargate pricing.
- **Add-ons:** Some add-ons (e.g., GuardDuty for EKS) cost extra.

### Quick Analogy
EKS = Hiring a CEO (control plane) and giving them an office (managed by AWS).
You hire the workers (nodes) and assign them tasks (pods). The CEO coordinates everything — who works where, restarts if someone fails, and handles scheduling. You just define what work needs to be done.

### Architecture View
```
┌─────────────────────────────────────────────────────────────┐
│ EKS Cluster                                                 │
│                                                             │
│  ┌────────────────────────────────────────┐                 │
│  │ CONTROL PLANE (Managed by AWS)         │                 │
│  │                                        │                 │
│  │ ┌──────────┐ ┌──────────┐ ┌─────────┐  │                 │
│  │ │API Server│ │Scheduler │ │  etcd   │  │                 │
│  │ └──────────┘ └──────────┘ └─────────┘  │                 │
│  │ ┌───────────────┐                      │                 │
│  │ │Controller Mgr │  Runs across 3 AZs   │                 │
│  │ └───────────────┘  (HA by default)     │                 │
│  └────────────────────────────────────────┘                 │
│           │                                                 │
│           │ kubectl / API                                   │
│           ▼                                                 │
│  ┌────────────────────────────────────────────────────┐     │
│  │ DATA PLANE (Worker Nodes — You manage)             │     │
│  │                                                    │     │
│  │  Node Group (EC2)              Fargate Profile      │     │
│  │  ┌──────────────────────┐     ┌──────────────────┐ │     │
│  │  │ Node 1 (m5.large)   │     │ Pod A (serverless)│ │     │
│  │  │ ┌─────┐ ┌─────┐     │     │ Pod B (serverless)│ │     │
│  │  │ │Pod 1│ │Pod 2│     │     └──────────────────┘ │     │
│  │  │ └─────┘ └─────┘     │                          │     │
│  │  ├──────────────────────┤                          │     │
│  │  │ Node 2 (m5.large)   │                          │     │
│  │  │ ┌─────┐ ┌─────┐     │                          │     │
│  │  │ │Pod 3│ │Pod 4│     │                          │     │
│  │  │ └─────┘ └─────┘     │                          │     │
│  │  └──────────────────────┘                          │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **Create an EKS cluster:**
```bash
eksctl create cluster \
  --name demo-cluster \
  --region us-east-1 \
  --nodegroup-name workers \
  --node-type t3.medium \
  --nodes 2 \
  --managed
```
2. Wait ~15 minutes for the cluster to provision.
3. Update kubeconfig:
```bash
aws eks update-kubeconfig --name demo-cluster --region us-east-1
```
4. Verify:
```bash
kubectl get nodes     # See your 2 worker nodes
kubectl get pods -A   # See system pods (CoreDNS, kube-proxy, etc.)
```
5. Deploy an Nginx app:
```bash
kubectl create deployment nginx --image=nginx --replicas=3
kubectl expose deployment nginx --port=80 --type=LoadBalancer
kubectl get svc       # Get the ALB/NLB URL
```
6. Open the URL → See Nginx page.
7. Clean up: `eksctl delete cluster --name demo-cluster`.

### Common Mistakes
- ❌ Running EKS without understanding Kubernetes basics — steep learning curve.
- ❌ Using EKS when ECS would be simpler — paying the K8s complexity tax for no benefit.
- ❌ Not configuring RBAC properly — wide-open cluster access.
- ❌ Forgetting EKS control plane costs $73/month even with no workloads running.
- ❌ Not updating the Kubernetes version — EKS versions reach end of life.

### Pro Tips
- ✅ Use **`eksctl`** for cluster creation — much simpler than CloudFormation or console.
- ✅ Use **Managed Node Groups** — AWS handles node provisioning, updates, and draining.
- ✅ Use **Karpenter** for intelligent, fast node autoscaling (better than Cluster Autoscaler).
- ✅ Use **AWS Load Balancer Controller** (not the in-tree K8s controller) for ALB/NLB.
- ✅ Enable **envelope encryption** for etcd secrets using KMS.
- ✅ In interviews: "We run EKS with managed node groups and Karpenter for autoscaling. RBAC is configured via IAM roles mapped to Kubernetes groups. We use the AWS Load Balancer Controller for ALB ingress and encrypt etcd secrets with KMS."

---

## 2. Control Plane

### What is it?
The Control Plane is the brain of the Kubernetes cluster.
It makes all scheduling decisions, stores cluster state, and exposes the Kubernetes API.
In EKS, AWS **fully manages** the control plane — you never see the underlying EC2 instances.

### Why it matters in production
- If the control plane goes down, you can't deploy, scale, or manage workloads.
- AWS guarantees 99.95% SLA for the EKS control plane.
- Understanding what runs in the control plane helps you debug cluster issues.

### Core Ideas

**Control plane components (all managed by AWS):**

| Component | What It Does |
|-----------|-------------|
| **API Server** | The front door. All kubectl commands go here. REST API for everything. |
| **etcd** | Key-value store. Holds ALL cluster state (pods, services, configs, secrets). |
| **Scheduler** | Decides which node a new pod runs on based on resources and constraints. |
| **Controller Manager** | Runs controllers (ReplicaSet, Deployment, Node) that maintain desired state. |
| **Cloud Controller Manager** | AWS-specific: provisions ALBs, EBS volumes, manages node lifecycle. |

**What AWS manages for you:**
- Control plane runs across **3 AZs** for HA.
- **etcd** is backed up automatically.
- AWS handles **version upgrades** (you initiate, AWS does the work).
- API server endpoint can be **public** (accessible from internet) or **private** (VPC only).

**EKS API endpoint access:**

| Mode | Who Can Access API | Use Case |
|------|-------------------|----------|
| **Public** | Anyone on the internet | Dev/test, simple setup |
| **Public + Private** | Internet + Nodes via VPC | Default. Nodes don't go through internet. |
| **Private Only** | Only from within VPC | Production. Most secure. Requires VPN/bastion for kubectl. |

### Quick Analogy
Control Plane = The manager's office in a warehouse.
The manager (API server) takes orders (kubectl commands), checks the inventory list (etcd), assigns workers to tasks (scheduler), and makes sure everything matches the plan (controller manager). AWS builds and maintains the office. You just send orders.

### Architecture View
```
┌─────────────────────────────────────────────────────┐
│ AWS-Managed Control Plane (3 AZs)                   │
│                                                     │
│  AZ-1a          AZ-1b          AZ-1c                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │API Server│  │API Server│  │API Server│          │
│  │etcd      │  │etcd      │  │etcd      │          │
│  │Scheduler │  │Controller│  │  (HA)    │          │
│  └──────────┘  └──────────┘  └──────────┘          │
│                                                     │
│  ✅ Auto-scaled by AWS                              │
│  ✅ Auto-backed up (etcd)                           │
│  ✅ Patched by AWS                                  │
│  ✅ 99.95% SLA                                      │
└───────────────────────┬─────────────────────────────┘
                        │
                        │ HTTPS (API endpoint)
                        │
            ┌───────────▼────────────┐
            │                        │
      kubectl (you)           Worker Nodes
      (manages cluster)       (run workloads)
```

### Hands-On (Step-by-step Lab)
1. After creating an EKS cluster, check control plane status:
```bash
aws eks describe-cluster --name demo-cluster --query "cluster.status"
# Should return "ACTIVE"
```
2. Check the API endpoint:
```bash
aws eks describe-cluster --name demo-cluster --query "cluster.endpoint"
```
3. Check Kubernetes version:
```bash
kubectl version --short
```
4. View control plane logs (if enabled):
   - Go to **EKS → Cluster → Logging** → Enable: API server, Audit, Authenticator.
   - Logs go to **CloudWatch Logs** group `/aws/eks/<cluster>/cluster`.
5. View system components:
```bash
kubectl get pods -n kube-system
# See: coredns, kube-proxy, aws-node (VPC CNI)
```

### Common Mistakes
- ❌ Making the API endpoint public-only in production — anyone can attempt to reach your API.
- ❌ Not enabling control plane logging — can't debug auth failures or audit API calls.
- ❌ Skipping Kubernetes version updates — versions reach end-of-support (~14 months).
- ❌ Thinking you can SSH into the control plane — you can't. It's fully managed.

### Pro Tips
- ✅ Set API endpoint to **Private** for production clusters. Access via VPN or bastion.
- ✅ Enable **all control plane log types** (API, Audit, Authenticator, Controller Manager, Scheduler).
- ✅ Use **Amazon GuardDuty for EKS** — detects suspicious Kubernetes API activity.
- ✅ Plan Kubernetes version upgrades — test in staging first. EKS supports ~3 minor versions.
- ✅ In interviews: "Our EKS control plane is configured with a private API endpoint. Control plane logs are enabled and sent to CloudWatch. We upgrade Kubernetes versions proactively, testing in staging first. GuardDuty monitors for anomalous API calls."

---

## 3. Worker Nodes

### What is it?
Worker nodes are the EC2 instances (or Fargate pods) where your actual workloads (pods) run.
Each node runs a **kubelet** (agent) and **kube-proxy** (networking) to communicate with the control plane.
You choose the instance types, number of nodes, and scaling strategy.

### Why it matters in production
- Worker nodes are where your application lives. Undersized = slow. Oversized = expensive.
- Node management is the biggest operational task in Kubernetes.
- Choosing the right node strategy (managed, self-managed, Fargate, Karpenter) impacts cost and reliability.

### Core Ideas

**Three node options:**

| Type | Who Manages | Best For |
|------|------------|---------|
| **Managed Node Groups** | AWS provisions, updates, drains | Most workloads (recommended) |
| **Self-Managed Nodes** | You manage EC2 + AMI + updates | Custom AMI, GPU, special requirements |
| **Fargate** | AWS (fully serverless) | Small/variable workloads, no node management |

**What runs on each worker node:**

| Component | Purpose |
|-----------|---------|
| **kubelet** | Agent that receives pod assignments from the scheduler and runs them |
| **kube-proxy** | Manages networking rules so pods can communicate |
| **Container runtime** | Runs the containers (containerd) |
| **AWS VPC CNI** | Assigns VPC IP addresses to pods |
| **Your Pods** | Your actual application containers |

**Node autoscaling:**

| Tool | How It Works | Speed |
|------|-------------|-------|
| **Cluster Autoscaler** | Watches pending pods. Adds nodes from ASG. | Slower (minutes) |
| **Karpenter** | Watches pending pods. Provisions right-sized nodes directly. | Faster (seconds). Smarter instance selection. |

### Quick Analogy
Control Plane = The HQ that assigns work orders.
Worker Nodes = The factory floor workers. Each worker (node) can handle multiple tasks (pods). The HQ sends tasks to workers that have capacity. If all workers are busy, it hires more (autoscaling). If a worker gets sick (node failure), tasks are reassigned to healthy workers.

### Architecture View
```
EKS Cluster Worker Nodes:

Managed Node Group (Auto Scaling Group under the hood):
┌────────────────────────────────────────────────────┐
│                                                    │
│  Node 1 (m5.large)     Node 2 (m5.large)          │
│  ┌──────────────────┐  ┌──────────────────┐        │
│  │ kubelet          │  │ kubelet          │        │
│  │ kube-proxy       │  │ kube-proxy       │        │
│  │ aws-vpc-cni      │  │ aws-vpc-cni      │        │
│  │                  │  │                  │        │
│  │ ┌────┐ ┌────┐   │  │ ┌────┐ ┌────┐   │        │
│  │ │Pod │ │Pod │   │  │ │Pod │ │Pod │   │        │
│  │ │ A  │ │ B  │   │  │ │ C  │ │ D  │   │        │
│  │ └────┘ └────┘   │  │ └────┘ └────┘   │        │
│  │                  │  │                  │        │
│  │ Capacity:        │  │ Capacity:        │        │
│  │ 2 vCPU, 8GB     │  │ 2 vCPU, 8GB     │        │
│  │ Used: 1.5 vCPU  │  │ Used: 1 vCPU    │        │
│  └──────────────────┘  └──────────────────┘        │
│                                                    │
│  Scaling: Karpenter watches for pending pods       │
│  → Provisions right-sized node in seconds          │
└────────────────────────────────────────────────────┘

Mixed Node Strategy (Cost Optimization):
┌─────────────────────────────────────────┐
│ On-Demand Nodes (baseline):             │
│ 2x m5.large (always running)            │
│                                         │
│ Spot Nodes (variable workloads):        │
│ 0-10x c5.xlarge (scale with demand)     │
│ 70% cheaper, can be interrupted         │
│                                         │
│ Fargate (specific workloads):           │
│ Batch jobs, scheduled tasks             │
│ No nodes to manage                      │
└─────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. List your nodes:
```bash
kubectl get nodes -o wide
```
2. Inspect a node's capacity:
```bash
kubectl describe node <node-name>
# Look at: Capacity, Allocatable, Conditions, Pods running
```
3. Add a new managed node group:
```bash
eksctl create nodegroup \
  --cluster demo-cluster \
  --name extra-workers \
  --node-type t3.large \
  --nodes-min 1 \
  --nodes-max 5 \
  --managed
```
4. Deploy a workload that needs more capacity:
```bash
kubectl create deployment stress --image=nginx --replicas=20
```
5. Watch nodes auto-scale: `kubectl get nodes -w`.
6. Delete the deployment: `kubectl delete deployment stress` → Nodes scale down.

### Common Mistakes
- ❌ Using the same instance type for all workloads — some need more memory, some need more CPU.
- ❌ Not configuring node autoscaling — nodes stay at fixed count.
- ❌ Running 100% Spot nodes — can lose all capacity at once during a Spot reclaim.
- ❌ Not draining nodes before terminating — kills running pods ungracefully.
- ❌ Ignoring kubelet resource reservations — not all node resources are available for pods.

### Pro Tips
- ✅ Use **Karpenter** over Cluster Autoscaler — faster, smarter, instance-type flexible.
- ✅ Mix **On-Demand** (baseline) + **Spot** (burst) for cost optimization.
- ✅ Use **Bottlerocket OS** for nodes — minimal, secure, purpose-built for containers.
- ✅ Configure **PodDisruptionBudgets** — ensure minimum available replicas during node drains.
- ✅ Use **node taints and tolerations** to isolate workloads (e.g., GPU only on GPU nodes).
- ✅ In interviews: "We use managed node groups with Karpenter for autoscaling. Baseline is On-Demand, burst is Spot with diversified instance types. Nodes run Bottlerocket OS for security. PodDisruptionBudgets protect availability during node upgrades."

---

## 4. Kubernetes Integration with AWS

### What is it?
EKS integrates Kubernetes with AWS-native services so you can use IAM, ALB, EBS, EFS, and more directly from Kubernetes manifests.
AWS provides controllers and plugins that bridge K8s resources to AWS resources.
This gives you the power of Kubernetes with the AWS ecosystem.

### Why it matters in production
- Without these integrations, you'd have to manually create and manage AWS resources alongside K8s.
- They let you use `kubectl` to provision AWS load balancers, volumes, and permissions.
- Interview question: "How do you integrate EKS with AWS services?"

### Core Ideas

**Key AWS-Kubernetes integrations:**

| AWS Service | K8s Integration | How |
|-------------|----------------|-----|
| **IAM** | Pod-level permissions | **IRSA** (IAM Roles for Service Accounts) |
| **ALB / NLB** | Ingress and Service | **AWS Load Balancer Controller** |
| **EBS** | Persistent volumes | **EBS CSI Driver** |
| **EFS** | Shared persistent volumes | **EFS CSI Driver** |
| **Secrets Manager** | K8s secrets | **Secrets Store CSI Driver** |
| **CloudWatch** | Logging and metrics | **Fluent Bit / CloudWatch Agent** |
| **ECR** | Container images | Built-in (node IAM role has pull access) |
| **VPC** | Pod networking | **AWS VPC CNI** (every pod gets a VPC IP) |

**IRSA (IAM Roles for Service Accounts) — VERY IMPORTANT:**
- K8s Service Account → mapped to → IAM Role.
- Each pod can have a **different IAM role** via its service account.
- No access keys. No node-level IAM role sharing. True pod-level least privilege.
- Uses OIDC federation under the hood.

**AWS VPC CNI:**
- Every pod gets a **real VPC IP address** from your subnet.
- Pods are directly addressable within the VPC — no overlay network.
- This means Security Groups and NACLs apply to pods directly.
- Downside: Uses IP addresses from your subnet — plan for large subnets.

### Quick Analogy
Kubernetes integration = Adding AWS power tools to a workshop.
The workshop (K8s) already has basic tools. Adding IAM (security badge reader), ALB (automatic front door), EBS (extra storage shelves), and CloudWatch (security cameras) makes the workshop enterprise-grade.

### Architecture View
```
EKS + AWS Integrations:

┌──────────────────────────────────────────────────────────┐
│ EKS Cluster                                              │
│                                                          │
│  Pod: order-api                                          │
│  ┌──────────────────────────────────────────────┐        │
│  │ Service Account: order-api-sa                │        │
│  │   → IAM Role: order-api-role (via IRSA)      │        │
│  │   → Permissions: DynamoDB RW, SQS Send       │        │
│  │                                              │        │
│  │ Container: order-api:v2                      │        │
│  │   → Image from ECR                           │        │
│  │   → Secrets from Secrets Manager (CSI)       │        │
│  │   → Logs to CloudWatch (Fluent Bit)          │        │
│  │   → Volume: EBS (gp3, 50GB)                  │        │
│  └──────────────────────────────────────────────┘        │
│                                                          │
│  Ingress (ALB):                                          │
│  ┌──────────────────────────────────────────────┐        │
│  │ AWS Load Balancer Controller                  │        │
│  │ K8s Ingress → Creates ALB automatically       │        │
│  │ Path /orders/* → order-api Service            │        │
│  │ Path /users/*  → user-api Service             │        │
│  │ TLS termination via ACM certificate           │        │
│  └──────────────────────────────────────────────┘        │
│                                                          │
│  Networking (VPC CNI):                                   │
│  Every pod has a VPC IP (10.0.3.x)                       │
│  Security Groups apply at pod level                      │
└──────────────────────────────────────────────────────────┘
```

### Hands-On (Step-by-step Lab)
1. **IRSA setup:**
```bash
eksctl create iamserviceaccount \
  --cluster demo-cluster \
  --name s3-reader-sa \
  --namespace default \
  --attach-policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess \
  --approve
```
2. Deploy a pod using this service account:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: s3-test
spec:
  serviceAccountName: s3-reader-sa
  containers:
  - name: aws-cli
    image: amazon/aws-cli
    command: ["aws", "s3", "ls"]
```
3. Check: `kubectl logs s3-test` → Should list S3 buckets (no access keys configured!).

4. **ALB Ingress:**
```bash
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=demo-cluster
```
5. Create an Ingress resource → AWS auto-creates an ALB.

6. **EBS volume:**
```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-ebs-volume
spec:
  accessModes: [ReadWriteOnce]
  storageClassName: gp3
  resources:
    requests:
      storage: 10Gi
EOF
```

### Common Mistakes
- ❌ Using the node IAM role for pod permissions — every pod on the node gets the same permissions.
- ❌ Not using IRSA — critical for pod-level least privilege.
- ❌ Using the in-tree Kubernetes load balancer instead of the AWS Load Balancer Controller.
- ❌ Not planning subnet size for VPC CNI — pods consume VPC IPs quickly.
- ❌ Not installing the EBS CSI driver — PersistentVolumeClaims won't work.

### Pro Tips
- ✅ **IRSA is mandatory in production.** Every pod gets its own scoped IAM role.
- ✅ Use **AWS Load Balancer Controller** for ALB/NLB — supports Ingress and TargetGroupBinding.
- ✅ Plan subnets for VPC CNI: A `/19` subnet gives ~8,000 IPs for pods.
- ✅ Use **Security Groups for Pods** — apply AWS Security Groups directly to pods.
- ✅ Use **External Secrets Operator** or **Secrets Store CSI Driver** to sync Secrets Manager → K8s secrets.
- ✅ In interviews: "We use IRSA for pod-level IAM — each microservice has its own scoped role via its service account. The AWS Load Balancer Controller manages our ALBs via Kubernetes Ingress resources. VPC CNI gives every pod a real VPC IP, and we apply Security Groups directly at the pod level."

---

## 5. When to Choose EKS vs ECS

### What is it?
Both EKS and ECS are container orchestration services on AWS.
ECS is AWS-native and simpler. EKS is Kubernetes-based and more portable.
Choosing between them is one of the most important architecture decisions for container workloads.

### Why it matters in production
- This is asked in almost every DevOps interview involving containers.
- The wrong choice wastes time (EKS complexity) or limits you later (ECS lock-in).
- The answer should always tie back to your team's skills and business requirements.

### Core Ideas

**The Complete Comparison:**

| Aspect | ECS | EKS |
|--------|-----|-----|
| **Orchestrator** | AWS proprietary | Kubernetes (open-source) |
| **Learning curve** | Low | High |
| **Portability** | AWS only | Any cloud, on-prem (K8s is portable) |
| **Ecosystem** | AWS-native integrations | Massive K8s ecosystem (Helm, ArgoCD, Istio, etc.) |
| **Control plane cost** | Free | $73/month per cluster |
| **Configuration** | Task definitions (JSON) | YAML manifests (Deployments, Services, etc.) |
| **Networking** | awsvpc (simple) | VPC CNI or overlay (complex, flexible) |
| **Service mesh** | App Mesh (AWS) | Istio, Linkerd, App Mesh |
| **GitOps** | Limited | ArgoCD, Flux (mature) |
| **Multi-cloud** | No | Yes (K8s runs anywhere) |
| **Community** | Smaller (AWS only) | Massive (CNCF, global) |
| **Fargate support** | Yes (primary) | Yes (limited feature set) |
| **Team requirement** | AWS knowledge | Kubernetes + AWS knowledge |

**Choose ECS when:**
- ✅ Team is AWS-native, no K8s experience.
- ✅ You want simplicity and fast time-to-production.
- ✅ Your workloads are entirely on AWS (no multi-cloud).
- ✅ You prefer Fargate for serverless containers.
- ✅ You have a small DevOps team (< 5 people).
- ✅ You don't need Helm charts, custom operators, or K8s-specific features.

**Choose EKS when:**
- ✅ Team has existing Kubernetes expertise.
- ✅ You need multi-cloud portability or hybrid K8s.
- ✅ You need the K8s ecosystem: Helm, ArgoCD, Istio, Prometheus, etc.
- ✅ You have a platform engineering team that manages K8s for multiple dev teams.
- ✅ You're running complex microservices needing advanced scheduling, CRDs, or operators.
- ✅ Company mandates Kubernetes (compliance, standardization).

### Quick Analogy
- **ECS** = An automatic car. Easy to drive. Gets you from A to B. Limited customization.
- **EKS** = A manual car with a turbo kit. More power and control. But you need to know how to drive stick. If you can, it's faster and more versatile.

### Architecture View
```
ECS Architecture (Simple):
  ALB → ECS Service → Fargate Tasks → RDS
  ✅ 3 components to configure
  ✅ All AWS-native
  ✅ Time to production: days

EKS Architecture (Powerful but Complex):
  Route53 → Ingress (ALB Controller) → K8s Service → Deployment → Pods
  + IRSA for IAM
  + EBS CSI for volumes
  + Fluent Bit for logs
  + Prometheus for metrics
  + ArgoCD for GitOps
  + Istio for service mesh
  ⚠️ 10+ components to configure
  ⚠️ Time to production: weeks

Decision Matrix:
┌─────────────────────┬──────┬──────┐
│ Factor              │ ECS  │ EKS  │
├─────────────────────┼──────┼──────┤
│ Simplicity          │ ✅✅  │ ✅    │
│ Portability         │ ❌    │ ✅✅  │
│ Ecosystem           │ ✅    │ ✅✅  │
│ Cost (small scale)  │ ✅✅  │ ✅    │
│ Learning curve      │ Easy │ Hard │
│ GitOps maturity     │ ✅    │ ✅✅  │
│ Multi-cloud         │ ❌    │ ✅✅  │
│ AWS integration     │ ✅✅  │ ✅    │
└─────────────────────┴──────┴──────┘
```

### Hands-On (Step-by-step Lab)
1. **Deploy the same app on both ECS and EKS:**
2. **ECS deployment:**
   - Create task definition → Create service → Attach ALB → Done in 15 minutes.
3. **EKS deployment:**
   - Create cluster → Install ALB Controller → Create Deployment YAML → Create Service YAML → Create Ingress YAML → Configure IRSA → Done in 2 hours.
4. Compare: Same result (app running behind ALB). ECS was faster. EKS gives more flexibility.
5. Now add GitOps to EKS: Install ArgoCD → Point to Git repo → Auto-deploy on push.
6. Try doing the same with ECS: You'd need CodePipeline/CodeDeploy — more AWS-specific tooling.

### Common Mistakes
- ❌ Choosing EKS because "everyone uses Kubernetes" — without the team to support it.
- ❌ Choosing ECS when the company plans to go multi-cloud in 6 months.
- ❌ Underestimating EKS operational overhead — it needs dedicated platform engineers.
- ❌ Thinking ECS is inferior — it's simpler, not worse. Simplicity is a feature.
- ❌ Migrating from ECS to EKS mid-project without a clear business reason.

### Pro Tips
- ✅ **Default to ECS Fargate** unless you have a specific reason for Kubernetes.
- ✅ If your company is committed to K8s (platform team, CNCF stack), go **EKS**.
- ✅ You can run **both** in the same account — ECS for simple services, EKS for complex ones.
- ✅ Consider **EKS Anywhere** or **EKS on Outposts** for on-prem K8s with AWS management.
- ✅ Use **EKS Blueprints** (Terraform/CDK) to accelerate cluster setup with best-practice defaults.
- ✅ In interviews: "We evaluate container platforms based on team expertise, portability needs, and ecosystem requirements. For AWS-native teams wanting simplicity, we choose ECS with Fargate. For teams with Kubernetes experience needing portability, GitOps (ArgoCD), or the CNCF ecosystem, we choose EKS with managed node groups and Karpenter. The key decision factor is the team's ability to operate and maintain the platform long-term."
