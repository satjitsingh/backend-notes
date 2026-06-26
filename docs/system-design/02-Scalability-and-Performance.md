# 2. Scalability and Performance

> "It works on my machine" is where every system starts. "It works for 50 million people on Black Friday" is where system design earns its keep. This chapter is about how systems *grow*.

---

## What is it?

Two words that sound similar but mean different things. Let's separate them cleanly, because mixing them up causes a lot of confused conversations.

**Performance** is about **how fast a single task completes** when the system is *not* overloaded. If one user clicks "view balance" and it returns in 100 milliseconds, that's a performance measure. It answers: *"How quick is one operation?"*

**Scalability** is about **what happens to performance as the load grows.** A system is scalable if it can handle *more* work — more users, more requests, more data — by adding more resources, *without* its performance falling off a cliff. It answers: *"Does it stay fast and stable as demand increases?"*

Here's the crucial insight that trips people up:

> **A system can have great performance but terrible scalability — and vice versa.**

Imagine a tiny sports car: blazing fast (great performance) but it only seats two people (terrible scalability — you can't move 50 people in it). Now imagine a fleet of city buses: each bus is slower than the sports car (lower per-trip performance), but you can add buses endlessly to move millions of people (excellent scalability). System design is usually about building the *bus fleet*, not the sports car.

A precise definition to lock in:

> **Scalability is the property of a system to handle a growing amount of work by adding resources to the system.**

The two flavors of "adding resources" are the heart of this entire topic, so let's go deep on them.

### The two ways to scale: Vertical vs. Horizontal

**Vertical Scaling (Scaling Up) — make the single machine bigger.**
You take your one server and give it a more powerful CPU, more RAM, faster disks. It's like trading your hatchback for a truck — same single vehicle, just beefier.

**Horizontal Scaling (Scaling Out) — add more machines.**
Instead of one giant server, you run *many* ordinary servers side by side and split the work among them. It's like adding more trucks to your delivery fleet instead of buying one impossibly huge truck.

This single choice — up vs. out — ripples through *every* other decision in your architecture, so we'll spend real time on it below.

---

## How it Works Under the Hood

### Vertical scaling, mechanically

When you scale vertically, the application code barely changes. You're still running **one instance** of your app on **one machine**; you've just given that machine more horsepower. In the cloud this is literally a dropdown — you stop the server, pick a larger instance type (more vCPUs, more GB of RAM), and start it again.

This is wonderfully simple. There's no coordination problem, because there's only one of everything. But it has a hard ceiling and a fragile shape:

- **There's a physical limit.** You cannot buy an infinitely powerful single machine. At some point the biggest server money can buy still isn't enough.
- **Cost grows non-linearly.** The top-end machines cost *far* more than twice a mid-range one for twice the power. You pay a steep premium at the high end.
- **It's a single point of failure.** One machine means: if it dies, *everything* is down. There's no backup serving traffic.
- **Scaling requires downtime** (usually). You typically have to restart the machine to resize it.

### Horizontal scaling, mechanically

When you scale horizontally, you run **many copies** of your application across many machines, and you put a **load balancer** in front of them. The load balancer is a traffic cop: every incoming request is routed to one of the available servers, spreading the load evenly.

```
                       [ Load Balancer ]
                      /        |         \
                     v         v          v
              [ App #1 ]  [ App #2 ]  [ App #3 ]   <- identical copies
                     \         |         /
                      v        v        v
                       [ Shared Database ]
```

This is powerful: you can keep adding servers almost without limit, and if one server dies, the load balancer simply stops sending it traffic — the others carry on, so there's **no single point of failure**. This is how every large internet system is built.

But it introduces *the* central challenge of distributed systems: **how do you coordinate many machines that are supposed to act like one?** This is where the single most important word in scaling comes in.

### The key that makes horizontal scaling work: Statelessness

Here's the problem. Suppose a user logs in, and server #1 stores their session ("this user is logged in") in its own local memory. The user's next request gets routed by the load balancer to server #2 — which has *never heard of this user* and thinks they're logged out. Chaos.

The fix is to make your application servers **stateless**:

> A **stateless** server keeps **no important data of its own between requests.** Any server can handle any request from any user, because none of them "remember" anything personal. All the state (sessions, data, user info) lives in a *shared* place outside the app servers — like a database or a distributed cache (e.g., Redis).

With stateless servers, the load balancer is free to send a user's requests to *any* server, because they're interchangeable — like identical cashiers at a bank, any of whom can serve you because all your account info is in the bank's central system, not in one cashier's head.

This is why, in the Spring Boot world, you're strongly encouraged to keep controllers and services stateless and push session/state into Redis or the database. It's the precondition that *lets you scale out at all.*

### Where the work piles up: bottlenecks

When you scale out the app servers, you'll quickly notice they all still talk to **one shared database.** So even with 50 app servers, the database can become the new chokepoint — everyone's queuing for the same resource. This is a **bottleneck**: the single slowest/most-constrained part that limits the whole system's throughput.

The art of scaling is a game of *moving the bottleneck*:
1. Too much traffic? → Add app servers (scale out).
2. Now the database is the bottleneck? → Add **read replicas** (copies for reads), and a **cache** to absorb repeated reads.
3. Now writes overwhelm the single database? → **Shard** it (split the data across many databases).
4. Repeat — the bottleneck always reappears somewhere new.

> The bottleneck never truly disappears; you just keep relocating it to a place you can afford. Knowing *where* the current bottleneck is, is most of the battle.

### Stateful vs. stateless: the comparison
| | Stateful server | Stateless server |
|---|---|---|
| Stores user data locally? | Yes (in its own memory) | No (in shared store) |
| Can any server handle any request? | No — must hit "their" server | Yes — fully interchangeable |
| Easy to scale horizontally? | Hard (sticky sessions, messy) | Easy (just add more) |
| Survives a server dying? | No — that user's state is lost | Yes — state is safe elsewhere |

---

## Why do we need it?

**Why scalability?** Because success is the thing that kills under-designed systems. The cruel irony of software is that a product becomes *most* likely to crash exactly when it becomes *most* popular — the viral moment, the marketing campaign, the festival sale. If you can't scale, growth itself becomes your enemy. Scalability lets you turn more users into more revenue instead of more outages.

**Why horizontal over vertical, usually?** Because at serious scale, horizontal scaling gives you two things vertical never can:
- **Effectively unlimited growth** — there's no "biggest machine" ceiling; you just add more boxes.
- **Fault tolerance for free** — many machines mean no single point of failure, which is non-negotiable for systems that must stay up.

That said, **vertical scaling is genuinely the right first move for many systems.** It's simpler, requires no code changes, and a single modern server is shockingly powerful. The wise path is often: scale *up* until it gets expensive or risky, *then* invest in scaling *out*. Don't build a 50-machine distributed system for an app with 500 users — that's the over-engineering trap from Chapter 1.

---

## Real-World / Fintech Example

Let's continue with our **digital wallet / payments app** and watch it grow through real stages.

**Stage 1 — Launch (a few thousand users).**
One Spring Boot application and one PostgreSQL database, both on a single decent server. Simple, cheap, fast to build. When load grows a bit, you **scale vertically** — bump the server to more RAM and CPU. This carries you surprisingly far and you ship features instead of fighting infrastructure. 

**Stage 2 — Growth (the app gets popular).**
The single server is maxing out during evening peaks. You can't just keep buying bigger machines (cost + single point of failure for a *money* app is unacceptable). So you **scale horizontally**: run 10 copies of the Spring Boot app behind a load balancer. 

To make this work, you fix a hidden bug: the app was storing login sessions in local memory. You make it **stateless** by moving sessions into **Redis**, a shared store. Now any of the 10 app servers can handle any user's "Pay" request, and if one server crashes mid-evening, the load balancer just routes around it — users never notice. This is the statelessness principle saving the day.

**Stage 3 — The database becomes the bottleneck.**
All 10 app servers still hammer one PostgreSQL instance. Reads dominate — people constantly refresh balances and transaction history (remember our ~10:1 read-to-write ratio). So you:
- Add **read replicas**: copies of the database that handle read-only queries (balance/history lookups), taking that load off the main database.
- Add a **cache** (Redis again): a user's balance and recent history are read thousands of times but change rarely, so you serve them from fast memory instead of hitting the database every time.

Now the main database only handles the *writes* — the actual money movements — which are far fewer.

**Stage 4 — Even writes get too heavy.**
At 80,000 payments/second, a single database can't keep up with writes either. So you **shard**: split users across many databases (e.g., users A–F on shard 1, G–M on shard 2, and so on). Each shard handles a fraction of the writes. (Sharding gets its own detailed chapter later — it's powerful but introduces real complexity, like cross-shard transactions.)

Notice the pattern: at each stage you **identified the current bottleneck and relocated it**, scaling out the layer that was choking. That iterative "find the chokepoint, widen it" loop *is* the practice of scaling.

---

## Trade-offs (Pros & Cons)

### Vertical Scaling (Scaling Up)
**Pros**
- **Dead simple** — usually no code changes; the app doesn't even know it happened.
- **No distributed-system complexity** — one machine means no coordination headaches.
- **Great first step** for small-to-medium systems; modern single servers are very powerful.

**Cons**
- **Hard physical ceiling** — you eventually hit the biggest machine available.
- **Expensive at the top end** — high-end hardware costs a steep premium per unit of power.
- **Single point of failure** — one machine down means total outage.
- **Resizing usually means downtime.**

### Horizontal Scaling (Scaling Out)
**Pros**
- **Near-limitless growth** — just keep adding commodity machines.
- **Built-in fault tolerance** — no single point of failure; survives individual server deaths.
- **Often cheaper** — many ordinary machines can beat one giant machine on cost-per-unit.
- **Scale up *and* down** elastically with demand (great for spiky traffic like sales events).

**Cons**
- **Real complexity** — you now need load balancers, statelessness, and ways to keep data consistent across machines.
- **Forces architectural discipline** — your app *must* be stateless, which constrains how you write code.
- **Data coordination is hard** — keeping data consistent across many nodes is the core difficulty of distributed systems (this is what CAP, replication, and consistency chapters are all about).
- **Operationally heavier** — more machines means more monitoring, deployment, and things that can break.

> **Staff-engineer takeaway:** Scale vertically first because it's simple, and switch to scaling horizontally when you hit cost, ceiling, or availability limits. And remember the golden rule of scaling: **make your servers stateless** — that one discipline is what unlocks everything else.

---

➡️ Next: [03-Latency-and-Throughput.md](03-Latency-and-Throughput.md) — the two numbers that define how a system *feels* and how much it can *handle*.
