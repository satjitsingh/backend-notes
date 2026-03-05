# Chapter 37: Docker & Deployment

[← Back to Index](00-README.md) | [Previous: Performance Internals](36-Performance-Internals.md) | [Next: Summary →](99-Summary.md)

---

## Why This Chapter Matters

You've built an amazing Spring Boot application. It works perfectly on your laptop. Now what? How do you get it running on a server somewhere in the world where real users can access it?

This is where Docker comes in. **Docker is how modern applications are shipped and deployed.** Companies like Google, Netflix, Spotify, and virtually every tech company use containers. If you don't know Docker, you can't deploy modern applications professionally.

**The classic developer nightmare:** "It works on my machine!" Your app runs fine locally but crashes on the server because it has a different Java version, missing libraries, or different settings. Docker eliminates this entire class of problems by packaging your app with all its dependencies into a portable box that runs the same everywhere.

---

## Layer 1 — Intuition Builder

### Part 1: The Lunchbox Problem (Why We Need Something Like Docker)

Imagine you're making a sandwich for your friend. You put bread, cheese, lettuce, and tomatoes in a lunchbox. When your friend opens it at school, they get exactly what you made — the same bread, same cheese, same everything.

**Without something like Docker:** You'd have to tell your friend: "Go to the store, buy bread (the kind from aisle 3), get cheddar cheese (the orange one, not the white one), find lettuce (make sure it's fresh)... oh, and the tomato has to be exactly this size." Your friend might get something slightly different, and the sandwich wouldn't taste the same.

**With Docker:** You hand them the lunchbox. They open it. It's exactly your sandwich. Every time.

**The same idea for software:** Instead of telling a server "install Java version 21.0.2, then PostgreSQL 15.3, then configure these 20 settings," you pack everything — your app, Java, all the settings — into one box. The server just opens the box and runs it. Same result everywhere.

---

### Part 2: What Is a Container? (The Lunchbox Itself)

A **container** is like a magic lunchbox for your app. Inside it you put:
- Your Spring Boot application (the code that does the work)
- The Java "engine" that runs it
- Anything else your app needs (like database connection info)

The magic part: This lunchbox works exactly the same on your laptop, your friend's computer, or a huge server in a data center. Same contents, same result. No "it works on my machine" — it works everywhere the lunchbox can be opened.

**Technical term in plain English:** A container is a lightweight, isolated environment that runs your application. "Lightweight" means it doesn't need a whole extra computer inside it. "Isolated" means what runs inside doesn't mess with what's outside.

---

### Part 3: What Is an Image? (The Recipe for the Lunchbox)

Before you make a lunchbox, you need a **recipe**. The recipe says: "Take bread, add cheese, add lettuce, wrap it up."

An **image** is the recipe for your container. It doesn't run by itself — it's the set of instructions that say: "Take Java 21, add my Spring Boot app, configure it this way." When you "run" an image, you create a container from it — like following the recipe to make one actual lunchbox.

**Analogy:**
- **Image** = The recipe card (stored, reusable)
- **Container** = The sandwich you made from the recipe (the running thing you can eat)

---

### Part 4: What Is a Dockerfile? (Writing the Recipe Down)

A **Dockerfile** is where you write the recipe. It's a text file with step-by-step instructions:

- **FROM** — "Start with this base" (e.g., "start with a box that already has Java")
- **COPY** — "Put this file into the box" (e.g., "copy my app JAR file")
- **RUN** — "Do this command" (e.g., "extract dependencies")
- **EXPOSE** — "This box listens on this port" (e.g., "people can reach my app on port 8080")
- **ENTRYPOINT** — "When someone opens the box, run this" (e.g., "start my Spring Boot app")

When you build an image, Docker reads the Dockerfile and follows each step. The result is an image you can use to create containers.

---

### Part 5: The Shipping Container Revolution (Why Docker Changed Everything)

In the 1950s, shipping cargo was messy. Different-sized crates, barrels, and bags had to be loaded one by one. Each port had different equipment. Loading a ship took weeks.

Then someone invented **standard shipping containers** — big steel boxes that fit on any ship, truck, or train. Suddenly, loading took hours instead of weeks. The same box could travel from a factory in China to a store in America without being repacked.

**Docker did the same for software:**
- **Before:** "Install these 15 things, configure them exactly right, and hope it works"
- **After:** "Run this container. It works everywhere."

---

### Part 6: Containers vs. Virtual Machines (Apartments vs. Full Houses)

Imagine you want to run 10 different apps on one computer.

**Virtual Machine (VM) approach:** For each app, you build a full house. Each house has its own kitchen, bathroom, bedrooms — a complete copy of an operating system. Heavy, slow to start, uses lots of space.

**Container approach:** You have one building (the computer) with many apartments. Each apartment shares the same building (the operating system) but has its own walls. Light, fast to start, efficient.

```
Virtual Machine:
┌─────────────────────────────────────────────────────────┐
│  App A        App B        App C                        │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                 │
│  │ Full OS │  │ Full OS │  │ Full OS │  ← Each has its  │
│  │ (Linux) │  │ (Linux) │  │ (Linux) │     own OS!      │
│  └─────────┘  └─────────┘  └─────────┘                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Hypervisor (VM Manager)                │  │
│  └──────────────────────────────────────────────────┘  │
│  Host Operating System                                  │
└─────────────────────────────────────────────────────────┘
Size: GBs each | Startup: Minutes | Like having 3 full houses

Docker Container:
┌─────────────────────────────────────────────────────────┐
│  App A        App B        App C                        │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                 │
│  │Container│  │Container│  │Container│  ← Share same OS!│
│  └─────────┘  └─────────┘  └─────────┘                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Docker Engine                          │  │
│  └──────────────────────────────────────────────────┘  │
│  Host Operating System (shared by all)                   │
└─────────────────────────────────────────────────────────┘
Size: MBs each | Startup: Seconds | Like apartments in one building
```

**Key difference:**
- **VM:** Full operating system per app (heavy, slow)
- **Container:** Shares the host's operating system (light, fast)

---

### Part 7: Docker Hub and Registries (The App Store for Containers)

Where do you get "base" images like Java, or PostgreSQL? From a **registry** — a library of images.

**Docker Hub** is the most popular public registry. It's like an app store: you can download images others built (e.g., `postgres`, `redis`, `eclipse-temurin`) or upload your own. Companies also run private registries (like GitHub Container Registry, AWS ECR) for their own images.

**Plain English:** A registry is a place to store and download container images, like a cloud drive for recipes.

---

### Part 8: Docker Compose (Running Multiple Boxes Together)

Your Spring Boot app might need a database and a cache. That's three things: app, database, cache.

**Docker Compose** lets you describe all of them in one file and say: "Start the database first, then the cache, then my app — and connect them on the same network." One command starts everything.

**Analogy:** Instead of setting up the TV, the sound system, and the game console one by one and hoping the cables match, you have a diagram that says exactly how to plug everything in. Docker Compose is that diagram.

---

### Part 9: CI/CD in Simple Terms (Robot Helper That Builds and Deploys)

**CI** (Continuous Integration): Every time you push code, a robot builds it and runs tests. If something breaks, you find out in minutes.

**CD** (Continuous Deployment): When the robot is happy (tests pass), it can automatically deploy the new version to a server. No manual "copy files and hope."

**Analogy:** You write a story. Every time you save a page, a robot proofreads it and puts it on the bookshelf if everything looks good. You don't have to remember to do it yourself.

---

### Part 10: Health Checks and Graceful Shutdown (Being a Good Neighbor)

**Health check:** A way for the system to ask: "Is this container still okay?" Your app responds "yes" or "no." If it says "no" or doesn't answer, the system can restart it or stop sending traffic.

**Graceful shutdown:** When someone says "please stop," your app finishes the work it's doing (like completing a request) before turning off. It doesn't just slam the door and leave people halfway through.

**Analogy:** Health check = "Are you still awake?" Graceful shutdown = "I'm closing the store in 5 minutes; let the customers finish their meals first."

---

## Layer 2 — Professional Developer

### Why Dockerfile Structure Matters (Before the Code)

A poorly structured Dockerfile leads to:
- **Slow rebuilds** — changing one line triggers a full rebuild
- **Huge images** — build tools and source code end up in the final image
- **Security risks** — unnecessary software and root user increase attack surface

We structure Dockerfiles to **maximize layer caching** (copy what changes rarely first) and **minimize final image size** (multi-stage builds). Let's see how.

---

### Dockerfile Instructions Reference Table

| Instruction | Purpose | Example |
|-------------|---------|---------|
| **FROM** | Base image to start from | `FROM eclipse-temurin:21-jre` |
| **WORKDIR** | Set working directory for later commands | `WORKDIR /app` |
| **COPY** | Copy files from host into image | `COPY target/*.jar app.jar` |
| **ADD** | Like COPY but can fetch URLs/unzip (prefer COPY) | `ADD file.tar.gz /app` |
| **RUN** | Execute command during build | `RUN chmod +x mvnw` |
| **ENV** | Set environment variable | `ENV JAVA_OPTS=-Xmx512m` |
| **EXPOSE** | Document which port the app uses | `EXPOSE 8080` |
| **ENTRYPOINT** | Command to run when container starts | `ENTRYPOINT ["java","-jar","app.jar"]` |
| **CMD** | Default arguments for ENTRYPOINT (can be overridden) | `CMD ["--server.port=8080"]` |
| **HEALTHCHECK** | How to check if container is healthy | `HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health` |

---

### Basic Dockerfile for Spring Boot (With Comments)

```dockerfile
# Stage 1: Build — We need JDK and Maven to compile. This stage won't be in the final image.
FROM eclipse-temurin:21-jdk AS builder

# All following commands run in /app
WORKDIR /app

# Copy Maven config first — dependencies change less often than source code.
# This lets Docker cache the "download dependencies" step when only code changes.
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download all dependencies. Cached if pom.xml unchanged.
RUN ./mvnw dependency:go-offline

# Now copy source code. This layer changes on every code edit.
COPY src ./src

# Compile and package. Produces a JAR in target/
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime — Final image only has what's needed to RUN the app.
# No JDK, no Maven, no source code. Much smaller and more secure.
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy only the built JAR from the builder stage.
COPY --from=builder /app/target/*.jar app.jar

# Document that the app listens on 8080 (doesn't actually publish it — that's docker run -p).
EXPOSE 8080

# When the container starts, run this. Uses exec form for proper signal handling.
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Optimized Dockerfile with Layered JAR (Better Caching)

Spring Boot can split its JAR into layers (dependencies, your code, etc.). We copy them in order from least-changing to most-changing so Docker caches better.

```dockerfile
# Build stage — same idea as before
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Download deps first (cached when pom.xml unchanged)
RUN ./mvnw dependency:go-offline -B

COPY src ./src

# Build the JAR
RUN ./mvnw clean package -DskipTests -B

# Spring Boot "layertools" splits the JAR into folders:
# dependencies, spring-boot-loader, snapshot-dependencies, application
RUN java -Djarmode=layertools -jar target/*.jar extract

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root user — security best practice. Don't run as root.
RUN groupadd -r spring && useradd -r -g spring spring

# Copy layers in order: least frequently changing first.
# dependencies change rarely → best cache hit rate
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
# application changes on every deploy → always rebuilt
COPY --from=builder /app/application/ ./

# Switch to non-root user
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

# Use JarLauncher to respect the layered structure
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

---

### Dockerfile with Health Check

```dockerfile
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080

# Docker will call this periodically. If it fails, container is marked unhealthy.
# interval=30s: check every 30 seconds
# timeout=3s: fail if no response in 3 seconds
# start-period=40s: give app 40s to start before failing checks
# retries=3: mark unhealthy after 3 consecutive failures
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Spring Boot Buildpacks (Alternative to Dockerfile)

Spring Boot 2.3+ can build a container image **without writing a Dockerfile**. It uses Cloud Native Buildpacks under the hood.

```bash
# Build an OCI image from the built JAR (run after mvn package)
./mvnw spring-boot:build-image

# Or with explicit image name
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=my-app:1.0
```

**Why use it:**
- No Dockerfile to maintain
- Sensible defaults (layered JAR, non-root user, health checks)
- Same result across environments

**When to use Dockerfile instead:**
- You need custom build steps
- You use a non-standard project layout
- You want explicit control over every layer

---

### Docker Commands Reference Table

| Command | Purpose |
|---------|---------|
| `docker build -t my-app:1.0 .` | Build image from Dockerfile, tag as my-app:1.0 |
| `docker run -p 8080:8080 my-app` | Run container, map host 8080 → container 8080 |
| `docker run -d -p 8080:8080 --name my-app my-app` | Run in background (detached), with a name |
| `docker run -e VAR=value my-app` | Run with environment variable |
| `docker ps` | List running containers |
| `docker ps -a` | List all containers (including stopped) |
| `docker stop <id>` | Stop a container gracefully |
| `docker rm <id>` | Remove a container |
| `docker images` | List images |
| `docker rmi <id>` | Remove an image |
| `docker logs <id>` | View container logs |
| `docker exec -it <id> /bin/bash` | Run a shell inside the container |
| `docker system prune -a` | Remove unused images, containers, networks |

---

### Building and Running (With Comments)

```bash
# Build: -t tags the image so we can reference it by name
docker build -t my-app:1.0 .

# Run: -p maps host port 8080 to container port 8080
#      -e sets environment variables (Spring reads SPRING_PROFILES_ACTIVE)
#      -d runs in background (detached)
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/mydb \
  --name my-app \
  my-app:1.0

# View logs (follow mode)
docker logs -f my-app

# Stop and remove
docker stop my-app
docker rm my-app
```

---

### Docker Compose: Multi-Service Setup

**Why:** Your app depends on PostgreSQL and Redis. Instead of starting each manually and wiring ports, define everything in one file and run `docker compose up`.

```yaml
# docker-compose.yml
# Version 3.8 supports health checks and deploy options we need
services:

  # Our Spring Boot app
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/mydb
      - SPRING_DATASOURCE_USERNAME=user
      - SPRING_DATASOURCE_PASSWORD=password
      - REDIS_HOST=redis
    depends_on:
      db:
        condition: service_healthy   # Wait for DB to be ready
      redis:
        condition: service_healthy
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # PostgreSQL database
  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=mydb
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data   # Persist data
    networks:
      - app-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U user"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis cache
  redis:
    image: redis:7-alpine
    networks:
      - app-network
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      retries: 5

volumes:
  postgres_data:
  redis_data:

networks:
  app-network:
    driver: bridge
```

**Commands:**
```bash
docker compose up -d          # Start all services in background
docker compose down           # Stop and remove containers
docker compose logs -f app    # Follow app logs
docker compose exec app sh   # Shell into app container
```

---

### Environment-Specific Configuration

**Principle:** Never hardcode database URLs or secrets. Use environment variables and Spring profiles.

```yaml
# application.yml — use placeholders with defaults
spring:
  application:
    name: my-app

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:h2:mem:testdb}
    username: ${SPRING_DATASOURCE_USERNAME:sa}
    password: ${SPRING_DATASOURCE_PASSWORD:}

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
```

```properties
# .env (for docker compose — never commit real secrets)
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/mydb
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=change_me_in_production
```

---

### .dockerignore (Keep the Image Small and Fast)

Without `.dockerignore`, Docker copies everything in the directory: `target/`, `.git/`, IDE files. That bloats the build context and can leak secrets.

```
# .dockerignore
target/          # Build output — we rebuild inside container
.git/             # Version control — not needed in image
.gitignore
README.md
.idea/            # IDE files
*.iml
.env              # May contain secrets — never bake into image
logs/
*.log
.DS_Store
```

---

### Graceful Shutdown in Spring Boot

When Kubernetes or Docker stops a container, it sends `SIGTERM`. Spring Boot can be configured to stop accepting new requests, finish in-flight ones, then shut down.

```yaml
# application.yml
server:
  shutdown: graceful    # Don't kill connections immediately

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # Max time to wait for requests
```

With this, when the container receives SIGTERM, Spring Boot will wait up to 30 seconds for active requests to complete before exiting.

---

## Layer 3 — Advanced Engineering Depth

### Multi-Stage Builds: Deep Dive

**What happens without multi-stage:**
- Build stage: JDK (~300MB) + Maven (~100MB) + source + dependencies
- All of that ends up in the final image
- Image size: ~500MB+, slower pushes, larger attack surface

**What happens with multi-stage:**
- Builder stage: Full build environment
- Final stage: Only JRE (~150MB) + JAR
- Image size: ~150MB, faster deploys, fewer vulnerabilities

**Edge case:** If you need to debug the builder stage (e.g., "why did the build fail?"), you can target it:
```bash
docker build --target builder -t my-app:debug .
docker run -it my-app:debug /bin/bash
```

---

### Layer Caching and Build Performance

Docker layers are cached. When you rebuild, only layers after the first changed layer are rebuilt.

**Bad order:**
```dockerfile
COPY src ./src              # Changes every commit
COPY pom.xml ./
RUN mvn dependency:go-offline  # Re-runs every time because layer above changed
```

**Good order:**
```dockerfile
COPY pom.xml ./
RUN mvn dependency:go-offline   # Cached when pom.xml unchanged
COPY src ./src
RUN mvn package                  # Only this + below rebuild on code change
```

**Advanced:** Use BuildKit cache mounts for Maven/Gradle to persist dependency cache across builds:
```dockerfile
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline
```

---

### CI/CD Pipeline Structure

**CI:** On every push/PR:
1. Checkout code
2. Set up JDK
3. Run tests
4. Build JAR
5. Build Docker image
6. (Optional) Push to registry

**CD:** On successful build (e.g., main branch):
1. Push image to registry with version tag
2. Deploy to staging/production (e.g., `kubectl set image`, ECS task definition update)

**GitHub Actions minimal example:**
```yaml
name: CI
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: ./mvnw clean test
      - run: ./mvnw package -DskipTests
      - run: docker build -t my-app .
```

---

### Cloud Deployment Overview

| Platform | Use Case | Key Concepts |
|----------|----------|--------------|
| **AWS ECS** | Run containers on AWS | Task Definition, Service, Cluster |
| **AWS Fargate** | Serverless containers | No EC2 to manage |
| **Kubernetes** | Portable orchestration | Pod, Deployment, Service, ConfigMap, Secret |
| **Google Cloud Run** | Serverless containers | Just push image, scales to zero |
| **Azure Container Instances** | Simple container hosting | Run container without managing VMs |

**AWS ECS minimal flow:**
1. Build and push image to Amazon ECR
2. Create Task Definition (image, CPU, memory, env vars)
3. Create ECS Service (desired count, load balancer)
4. Service runs tasks (containers) and keeps them healthy

**Kubernetes minimal flow:**
1. Build and push image to a registry
2. Create Deployment (replicas, image, probes, env)
3. Create Service (expose Deployment via ClusterIP/NodePort/LoadBalancer)
4. Optional: Ingress for HTTP routing, HPA for auto-scaling

---

### Kubernetes Basics for Spring Boot

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-boot-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: spring-boot-app
  template:
    metadata:
      labels:
        app: spring-boot-app
    spec:
      containers:
      - name: app
        image: my-registry/my-app:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: spring-boot-service
spec:
  selector:
    app: spring-boot-app
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

**Liveness vs readiness:**
- **Liveness:** "Is the process alive?" If not, Kubernetes restarts the pod.
- **Readiness:** "Is the app ready for traffic?" If not, Kubernetes removes it from the load balancer until it becomes ready.

---

### Security: Image Scanning and Non-Root User

**Scan images for vulnerabilities:**
```bash
trivy image my-app:1.0
docker scout cves my-app:1.0
```

**Non-root user in Dockerfile:**
```dockerfile
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
```

**Kubernetes security context:**
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
```

---

### Graceful Shutdown and terminationGracePeriodSeconds

When Kubernetes stops a pod, it sends SIGTERM, waits `terminationGracePeriodSeconds` (default 30s), then SIGKILL. Your app must shut down within that window.

```yaml
spec:
  terminationGracePeriodSeconds: 45   # Give app 45s to shut down
  containers:
  - name: app
    lifecycle:
      preStop:
        exec:
          command: ["/bin/sh", "-c", "sleep 15"]   # Extra buffer for LB to drain
```

**Why preStop sleep?** Load balancers need time to stop sending traffic. A short sleep ensures no new requests arrive while the app is shutting down.

---

### Edge Cases and Pitfalls

1. **`latest` tag:** Don't use in production. Pin to digest or version: `my-app:1.2.3`.
2. **Secrets in layers:** Even if you `RUN rm secret.txt` in a later layer, the secret remains in the image history. Use build secrets or runtime injection.
3. **Health check in base image:** Some base images run `curl` in HEALTHCHECK. Alpine images may not include `curl`; use `wget` or add `curl` in your Dockerfile.
4. **Database migrations:** Run Flyway/Liquibase on startup, or use an init container / separate job. Ensure DB is ready before app starts (`depends_on: condition: service_healthy`).
5. **Timezone:** Containers often use UTC. Set `TZ` or `-Duser.timezone` if needed.

---

## Layer 4 — Interview Mastery

### Q1: What is Docker, and why use it for Spring Boot?

**Answer:** Docker packages an application and its dependencies into a container—a lightweight, isolated environment that runs the same on any machine. For Spring Boot, this means the JAR, JRE, and config are bundled together, eliminating "works on my machine" issues and enabling consistent deployment from dev to production.

---

### Q2: What's the difference between an image and a container?

**Answer:** An **image** is an immutable template (like a class). A **container** is a running instance of that image (like an object). You build images from Dockerfiles, then create one or more containers from each image.

---

### Q3: Explain multi-stage builds and their benefits.

**Answer:** Multi-stage builds use multiple `FROM` statements. The first stage(s) use build tools (JDK, Maven) to produce artifacts. The final stage copies only those artifacts into a minimal runtime image (e.g., JRE only). Benefits: smaller images, faster pushes, fewer vulnerabilities, no build tools in production.

---

### Q4: What Dockerfile instructions do you use for a Spring Boot app?

**Answer:** Typically: `FROM` (eclipse-temurin JDK for build, JRE for runtime), `WORKDIR`, `COPY` (pom.xml first for caching, then src), `RUN` (mvn package), `COPY --from=builder` (JAR from build stage), `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","app.jar"]`. Optionally `HEALTHCHECK` and `USER` for non-root.

---

### Q5: How do you handle configuration in containers?

**Answer:** Use environment variables (e.g., `SPRING_DATASOURCE_URL`) and Spring's `${VAR:default}` placeholders. For secrets, use Kubernetes Secrets, AWS Secrets Manager, or Vault—never hardcode in images or Dockerfiles. Use profiles (`SPRING_PROFILES_ACTIVE`) for environment-specific config.

---

### Q6: What are Docker Compose and Kubernetes used for?

**Answer:** **Docker Compose** orchestrates multiple containers on a single host (app + DB + Redis). **Kubernetes** orchestrates containers across a cluster: scaling, self-healing, rolling updates, and service discovery. Use Compose for dev/local; Kubernetes for production at scale.

---

### Q7: How do you implement health checks for a Spring Boot container?

**Answer:** Expose Spring Boot Actuator health endpoint. In Docker: `HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health || exit 1`. In Kubernetes: `livenessProbe` and `readinessProbe` pointing to `/actuator/health/liveness` and `/actuator/health/readiness`. Liveness = restart if dead; readiness = remove from traffic if not ready.

---

### Q8: How do you achieve graceful shutdown?

**Answer:** Set `server.shutdown: graceful` and `spring.lifecycle.timeout-per-shutdown-phase` in Spring Boot. On SIGTERM, the app stops accepting new requests, waits for in-flight ones (up to the timeout), then exits. In Kubernetes, set `terminationGracePeriodSeconds` to allow enough time.

---

### Q9: How do you optimize Docker image size for Spring Boot?

**Answer:** (1) Multi-stage builds—no JDK/Maven in final image. (2) Use JRE, not JDK. (3) Spring Boot layered JAR—copy dependencies before application layer for better caching. (4) `.dockerignore` to exclude `target/`, `.git/`, etc. (5) Non-root user. (6) Alpine or slim base if compatible.

---

### Q10: What is a container registry, and name some.

**Answer:** A registry stores and distributes container images. Examples: Docker Hub (public), GitHub Container Registry (ghcr.io), Amazon ECR, Google Artifact Registry, Azure Container Registry, GitLab Container Registry.

---

### Q11: What are Spring Boot Buildpacks?

**Answer:** Buildpacks turn your application (e.g., JAR) into a container image without writing a Dockerfile. Run `./mvnw spring-boot:build-image`. Spring uses Cloud Native Buildpacks to produce an OCI image with sensible defaults (layered JAR, non-root user).

---

### Q12: How do you run database migrations in a containerized setup?

**Answer:** (1) Run Flyway/Liquibase on app startup (simple, but ties migration to app deploy). (2) Init container in Kubernetes that runs migrations before main container starts. (3) Separate migration job that runs before deploying new app version. Ensure DB is healthy before app starts (`depends_on` with health check).

---

## Summary

### Key Takeaways

1. **Docker** packages your Spring Boot app and dependencies into a portable container that runs the same everywhere.
2. **Image** = recipe; **Container** = running instance. **Dockerfile** = how you define the image.
3. **Multi-stage builds** keep final images small by separating build and runtime.
4. **Docker Compose** runs app + DB + Redis (and more) with one command; ideal for local/dev.
5. **Kubernetes** and **AWS ECS** orchestrate containers in production (scaling, health, updates).
6. **Health checks** and **graceful shutdown** make deployments reliable and zero-downtime friendly.
7. **Config** via environment variables and external secrets; never bake secrets into images.
8. **Spring Boot Buildpacks** build container images without a Dockerfile.

### Best Practices

- Use multi-stage builds and layered JARs
- Run as non-root user
- Add HEALTHCHECK (Docker) and liveness/readiness probes (Kubernetes)
- Configure graceful shutdown
- Use `.dockerignore` and pin base image versions
- Externalize config and secrets
- Scan images for vulnerabilities
- Set resource limits in production

### Common Pitfalls

- Using `latest` tag in production
- Baking secrets into images
- Skipping health checks
- Not setting `terminationGracePeriodSeconds`
- Copying `target/` or `.git/` into build context
- Running as root
- No resource limits leading to resource exhaustion

---

## Next Steps

- [Chapter 99: Summary & Cheat Sheet](99-Summary.md)
- Practice building images with `docker build` and `spring-boot:build-image`
- Set up a minimal CI/CD pipeline (e.g., GitHub Actions)
- Deploy to a cloud platform (AWS ECS, Cloud Run, or a small Kubernetes cluster)
- Configure health checks and graceful shutdown in a real app

---

[← Back to Index](00-README.md) | [Previous: Performance Internals](36-Performance-Internals.md) | [Next: Summary →](99-Summary.md)
