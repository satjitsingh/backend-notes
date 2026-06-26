# 1. Introduction to System Design

> Before we talk about databases, queues, or caches, we need to agree on what "system design" actually *is* — and how a senior engineer thinks when staring at a blank whiteboard.

---

## What is it?

**System design is the process of deciding how the different pieces of software and hardware in an application will fit together to meet a set of goals.**

Let's slow that down, because there's a lot packed in there.

When you write a small program — say, a function that adds two numbers — you only worry about *correctness*: does it give the right answer? But when you build a real product used by thousands or millions of people, correctness is just the starting point. Now you also have to worry about questions like:

- What happens when **10 million people** use it at the same time?
- What happens when a **server crashes** at 3 AM?
- What happens when the **database fills up**?
- How do we make sure a user in Tokyo gets a response as fast as a user in New York?
- How do we make sure a payment is **never charged twice**, even if the network glitches?

System design is the discipline of answering these questions *on purpose, in advance* — instead of finding out the hard way when production goes down.

A helpful way to think about it: **coding is building a single room; system design is being the architect of the entire building.** The architect doesn't lay every brick. Instead, they decide how many floors there are, where the load-bearing walls go, where the plumbing and electrical run, and how people move through the building. If they get those big decisions wrong, no amount of beautiful interior decorating will save the building.

System design lives at the level of **"the big decisions"**: which components exist, how they talk to each other, where data lives, and how the whole thing behaves when things go wrong.

### Functional vs. Non-Functional requirements (the most important distinction to learn first)

Every system has two kinds of requirements, and beginners almost always focus only on the first one:

- **Functional requirements** — *what the system does.* "Users can transfer money." "Users can view their transaction history." These are the features. They answer the question *"What should it do?"*

- **Non-functional requirements (NFRs)** — *how well the system does it.* "The transfer must complete in under 2 seconds." "The system must stay up 99.99% of the time." "It must handle 50,000 transactions per second during peak." These answer *"How good must it be?"*

> **The entire field of system design is mostly about non-functional requirements.** Anyone can build a money-transfer feature that works for one user on their laptop. The hard part — the part worth a staff engineer's salary — is making it work for 50 million users, reliably, quickly, and without ever losing a cent. Those are NFRs.

The common NFRs you'll meet again and again (and which later chapters cover in depth):

| NFR | Plain-English meaning | The question it answers |
|-----|----------------------|------------------------|
| **Scalability** | Can it grow to handle more load? | "What happens at 100x traffic?" |
| **Availability** | Is it up when users need it? | "How much downtime is acceptable?" |
| **Latency / Performance** | How fast does it respond? | "How long does a user wait?" |
| **Consistency** | Does everyone see the same data? | "If I update my balance, when does everyone else see it?" |
| **Reliability / Durability** | Does it lose data or give wrong answers? | "Will a committed payment ever vanish?" |
| **Maintainability** | Can humans understand and change it? | "Can a new engineer safely add a feature?" |

---

## How it Works Under the Hood

System design isn't a single mechanism you can pop open and inspect like an engine. Instead, "how it works" is really **how a good engineer works through a design problem.** There's a repeatable process, and learning it is the single most useful thing in this whole roadmap.

Here is the step-by-step mental flow that senior engineers use, whether on a real project or in an interview:

### Step 1 — Clarify the requirements (don't skip this, ever)
Before drawing a single box, you ask questions to pin down both functional and non-functional requirements. *"Who uses this? How many users? Read-heavy or write-heavy? How fresh must the data be? What's the acceptable downtime?"* 

Skipping this step is the #1 mistake. If you don't know the requirements, every later decision is a guess. A design for 100 users looks completely different from a design for 100 million.

### Step 2 — Estimate the scale (back-of-the-envelope math)
You turn vague words into **numbers**. "Popular" means nothing; "5,000 requests per second and 2 TB of new data per year" means everything. You roughly calculate:

- **Traffic:** requests per second (RPS), and the read-to-write ratio.
- **Storage:** how much data per day/year.
- **Bandwidth:** how many bytes flow in and out per second.
- **Memory:** how much could realistically fit in a cache.

These numbers *directly* drive your architecture. For example, if writes are rare but reads are massive, you'll lean heavily on caching and read replicas. The math tells you where to spend effort.

### Step 3 — Define the high-level components (the boxes)
Now you draw the major building blocks and how requests flow between them. A typical web system has a recognizable skeleton:

```
[ Client / Mobile App ]
          |
          v
   [ Load Balancer ]          <- spreads traffic across servers
          |
          v
 [ Application Servers ]       <- your business logic (e.g., Spring Boot apps)
       /        \
      v          v
[  Cache  ]   [ Database ]     <- fast memory store + durable storage
                   |
                   v
        [ Replicas / Backups ] <- copies for reading & safety
```

At this stage you're naming the *roles*: "something to spread traffic" (load balancer), "something to run logic" (app servers), "something fast to read from" (cache), "something durable to store the truth" (database).

### Step 4 — Drill into each component (the deep dive)
You zoom into the boxes that matter most and make detailed decisions: Which database — SQL or NoSQL? How do we cache, and when do we invalidate it? How do services communicate — synchronous REST calls or asynchronous messages over a queue? This is where the bulk of real design work happens, and it's what most of the later chapters are about.

### Step 5 — Identify and remove bottlenecks
You ask: *"If traffic suddenly went 10x, what breaks first?"* That weakest link is your bottleneck. You then add the tool that fixes it — maybe more app servers, maybe a cache, maybe database sharding. Then you ask the question again, because **fixing one bottleneck always reveals the next one.** Design is iterative; you never "finish."

### Step 6 — Address failures and edge cases
Finally, you assume things break — because at scale, they *always* do. A disk dies, a network link drops, a whole data center loses power. Good design plans for this with redundancy (spare copies), replication (data in multiple places), and graceful degradation (the app still partly works even when a piece is broken).

> **The mental model to internalize:** System design is a loop of *"What do we need? → How much load? → What boxes? → What breaks? → How do we survive it?"* — repeated until the design satisfies the requirements at the required scale.

---

## Why do we need it?

You might wonder: *why not just write the code and fix problems as they appear?* For a tiny hobby project, that's actually fine. But for any serious system, skipping design upfront leads to predictable, expensive disasters:

1. **Things fall over under load.** A system that wasn't designed to scale will simply crash when it gets popular — exactly at the moment of maximum business opportunity. (Think of a ticketing site dying the instant a big concert goes on sale.)

2. **Rewrites are brutally expensive.** Architecture decisions are like the foundation of a house — cheap to change on paper, ruinously expensive to change after the building is up. Choosing the wrong database or coupling everything together can cost a company *years* of engineering time to undo.

3. **Failures become catastrophes instead of hiccups.** Without designing for failure, a single dead server takes down the whole product. With good design, users never even notice.

4. **You can't reason about cost.** Cloud bills are driven by architecture. A poorly designed system can cost 10x more to run for the same workload.

**When do you actually invest in system design?** The honest answer: proportionally to the stakes. A weekend prototype needs almost none. A fintech payments platform handling real money needs an enormous amount, because the cost of getting it wrong is measured in lost money, regulatory fines, and destroyed trust. The skill is matching the depth of design to the real requirements — not over-engineering a simple thing, and not under-engineering a critical one.

---

## Real-World / Fintech Example

Let's make this concrete with a fintech scenario you can carry through the whole roadmap: **designing a digital wallet / payments app** (think something like a UPI app, PayPal, or a neobank).

**The functional requirement** sounds simple: *"A user can send money to another user."*

But watch how the *non-functional* requirements completely change the design:

- **Scale:** Suppose it's a popular app during a festival sale. You estimate **80,000 payment requests per second** at peak, and reads (people checking their balance and history) at maybe **10x** that — so ~800,000 reads/sec. Immediately you know: a single database server cannot do this. You'll need read replicas, caching for balances/history, and probably sharding (all covered later).

- **Consistency:** This is money, so the rules are strict. If Alice sends ₹500 to Bob, the system must **never** show that ₹500 left Alice's account but never arrived in Bob's. And Alice must **never** be able to spend the same ₹500 twice by tapping fast. This pushes you toward strong consistency and database transactions for the core ledger — even though that's slower and harder to scale.

- **Availability:** If the app is down for even 5 minutes during peak, you lose millions in transactions and users' trust. So you target very high availability (e.g., 99.99%, ~52 minutes of downtime *per year*). That means redundancy: multiple servers, multiple data centers, automatic failover.

- **Durability:** Once a payment is confirmed, it can **never** be lost — even if a server's disk physically dies one second later. This forces you to replicate every committed transaction to multiple machines before telling the user "success."

- **Latency:** A user tapping "Pay" expects feedback in well under 2 seconds, or they panic and tap again (potentially causing a double-payment — another problem to design around with *idempotency*).

Notice what just happened: the feature ("send money") was one sentence, but the **non-functional requirements generated an entire architecture** — load balancers, caches, read replicas, transactions, replication, multi-data-center redundancy, and idempotency handling. 

In a Spring Boot world, you'd see this manifest as: a stateless `@RestController` for the payment endpoint (so you can run many copies behind a load balancer), a transactional service layer (`@Transactional`) wrapping the debit-and-credit on the core ledger, Redis caching for balance reads, and an event published to Kafka for downstream things like notifications and fraud checks — so the slow stuff happens *asynchronously* and doesn't make the user wait.

That, end to end, is system design: **turning "send money" into a building that won't collapse when 80,000 people press the button at once.**

---

## Trade-offs (Pros & Cons)

System design itself is the *practice of managing trade-offs*, so here the trade-offs are about **how much design effort to invest**:

**Pros of investing in upfront system design**
- **Prevents catastrophic, expensive rewrites** by getting the foundation right early.
- **Lets the system survive growth and failure** instead of collapsing at the worst moment.
- **Creates a shared mental model** so a whole team can build the same thing coherently.
- **Makes cost and capacity predictable**, which the business needs for planning.

**Cons / costs of system design**
- **It takes time upfront** — time not spent shipping features, which can feel slow early on.
- **Risk of over-engineering** — building for a billion users you don't have wastes money and adds complexity that slows you down. (Premature scaling is a very real and very common mistake.)
- **Designs are based on predictions**, and predictions are often wrong — you may design for the wrong bottleneck.
- **Added complexity has its own cost**: every extra component (cache, queue, replica) is one more thing that can break and must be operated, monitored, and understood.

> **The staff-engineer takeaway:** Good system design is not about using the most components or the fanciest tools. It's about using the *fewest* components that satisfy the *actual* requirements — and clearly understanding what each one costs you. Start simple, measure, and add complexity only when the numbers prove you need it.

---

➡️ Next: [02-Scalability-and-Performance.md](02-Scalability-and-Performance.md) — how systems actually grow to handle more load.
