# 24. Long Polling, WebSockets, and Server-Sent Events (SSE)

> Everything in the last chapter was *request/response*: the client asks, the server answers. But sometimes the **server** has new data and needs to tell the client *right now* — a payment confirmed, a price changed, a message arrived. Normal HTTP can't do that, because the server can't initiate. These three techniques solve the "server push" problem in increasingly powerful ways.

---

## What is it?

These are techniques for achieving **real-time, server-to-client communication** — getting fresh data to the client the moment it's available, rather than the client having to repeatedly ask.

The fundamental problem: standard HTTP is **client-initiated**. The client sends a request, the server responds, and the connection closes. The server has **no way to start a conversation** — it can't "call" the client to say "hey, something happened." So how does a client learn about server-side changes in real time? Four approaches, from crudest to most powerful:

**0. Short Polling (the naive baseline).** The client repeatedly asks "any updates?" every few seconds. Simple but wasteful — most requests return "nothing new," burning bandwidth and server resources, and updates are delayed up to the polling interval.

**1. Long Polling.** The client asks "any updates?" but the server **holds the request open** (doesn't respond) until it actually *has* something — then responds. The client immediately re-asks. This simulates push using ordinary HTTP, with far less waste than short polling.

**2. Server-Sent Events (SSE).** A **one-way** persistent connection from server to client. The client opens a connection once, and the server **streams** events down it whenever it wants, over standard HTTP. Built for "server talks, client listens."

**3. WebSockets.** A **full-duplex** (two-way) persistent connection. After an initial handshake, client *and* server can send messages to each other freely, anytime, over a single long-lived connection. The most powerful — true real-time bidirectional communication.

> **The mental model:** Long polling *fakes* push using repeated HTTP requests. SSE is a *one-way pipe* (server → client) — perfect for notifications/feeds. WebSockets is a *two-way phone call* (both can talk) — needed for chat, gaming, collaborative editing. Pick the *least* powerful one that meets your need (simpler = more robust).

---

## How it Works Under the Hood

### Short Polling vs Long Polling

**Short polling** — fixed-interval requests:
```
Client: "updates?" → Server: "no"   (waste)
[wait 3s]
Client: "updates?" → Server: "no"   (waste)
[wait 3s]
Client: "updates?" → Server: "YES, here"  (finally)
```
Wasteful and laggy (up to interval-length delay).

**Long polling** — server holds the request:
```
Client: "updates?" → Server: ...(holds open, waits)...
                     Server: "YES!" (responds the instant data exists)
Client: "updates?" → Server: ...(holds open again)...
```
- The server *parks* the request (no immediate response) until data is ready or a timeout. Then it responds, and the client instantly reconnects.
- **Pro:** works over plain HTTP, compatible with everything, near-real-time. 
- **Con:** still has per-request HTTP overhead (headers, reconnection each cycle); holding many open requests consumes server resources; not great for high-frequency updates.

### Server-Sent Events (SSE)

SSE opens **one long-lived HTTP connection** over which the server **streams** a sequence of events:
```
Client: opens connection to /stream  (using EventSource)
Server: keeps it open, and pushes whenever it wants ──►
        data: {"balance": 500}\n\n
        data: {"balance": 450}\n\n   (server sends these over time, one connection)
```
- **One-directional** (server → client only). The client can't send data back over this connection (it uses normal HTTP requests for that).
- Runs over **standard HTTP**, so it works through most proxies/firewalls and is simple to implement.
- **Built-in auto-reconnect** — if the connection drops, the browser's `EventSource` automatically reconnects and can resume from the last event ID. This robustness is a real, underrated advantage.
- **Limitation:** over HTTP/1.1, browsers cap connections per domain (~6), which can constrain many SSE streams (HTTP/2 mitigates this via multiplexing).

### WebSockets

WebSockets establish a **full-duplex** channel:
```
1. Client sends an HTTP request with "Upgrade: websocket" header.
2. Server agrees → the connection is UPGRADED from HTTP to the WebSocket protocol (ws:// or wss://).
3. Now a persistent two-way pipe exists:
   Client ⇄ Server  — either side sends messages anytime, low overhead.
```
- After the handshake, it's **no longer HTTP** — it's a persistent TCP connection with a lightweight framing protocol. Tiny per-message overhead (no HTTP headers each time).
- **Bidirectional** — both sides push freely. Essential for chat, multiplayer games, collaborative docs, live trading.
- **Costs:** more complex; the persistent connection is **stateful**, which complicates horizontal scaling (Chapter 2) — you need sticky sessions or a shared backplane (e.g., Redis pub/sub) so that a message can reach whichever server holds a given client's connection. Some proxies/load balancers need special config. Reconnection logic is your responsibility (unlike SSE's built-in reconnect).

### The comparison
| | Long Polling | SSE | WebSockets |
|---|---|---|---|
| Direction | Client-initiated (simulated push) | **Server → client** (one-way) | **Both ways** (full-duplex) |
| Protocol | HTTP | HTTP (streaming) | WebSocket (upgraded from HTTP) |
| Persistent connection | No (repeated) | Yes (one) | Yes (one) |
| Overhead | High (per request) | Low | **Lowest** (per message) |
| Auto-reconnect | Manual | **Built-in** | Manual |
| Complexity | Low | Low–medium | **Higher** (stateful, scaling) |
| Best for | Simple near-real-time, fallback | Notifications, feeds, live updates | Chat, gaming, collaboration, trading |

### The scaling challenge of persistent connections

Both SSE and WebSockets keep **long-lived, stateful connections**, which clashes with the stateless-horizontal-scaling ideal (Chapter 2). Challenges:
- **Connection limits:** each server can hold only so many open connections (tens of thousands); at scale you need many servers and a way to manage them.
- **Routing pushes:** if Alice's WebSocket is on Server 3, and an event for Alice arrives at Server 7, Server 7 must get the message to Server 3. The standard solution is a **pub/sub backplane** (Redis pub/sub or Kafka, Chapters 17/19): servers subscribe to relevant channels, and any server can publish a message that reaches the server holding the target connection.
- **Load balancer config:** must support connection upgrades (WebSockets) and long-lived connections, often with sticky sessions.

---

## Why do we need them?

We need these because **a huge class of modern features requires the server to push data in real time, which plain request/response cannot do** — and choosing the right technique balances real-time-ness against complexity:

1. **Real-time UX is now expected.** Live notifications, instant chat, live dashboards, price tickers, "your payment is confirmed" updates, order tracking — users expect data to appear *instantly*, without refreshing. Polling either lags or wastes enormous resources.

2. **Efficiency.** Constant short polling at scale is brutally wasteful — millions of "anything new? no" requests. Persistent connections (SSE/WebSockets) push only when there's actual data, dramatically reducing overhead.

3. **Bidirectional interactivity** (WebSockets). Some features *require* both sides to send freely and instantly — multiplayer games, collaborative editing, live trading where the client both receives prices and sends orders. Only WebSockets handle this cleanly.

**When to use which (pick the least powerful that works):**
- **Long polling:** simple near-real-time needs, or as a **fallback** when WebSockets/SSE aren't available (old browsers, restrictive proxies). Low complexity.
- **SSE:** **server-to-client only** streaming — notifications, live feeds, status/progress updates, dashboards. Simpler and more robust than WebSockets (auto-reconnect, plain HTTP) when you don't need the client to push back over the same channel.
- **WebSockets:** **true two-way real-time** — chat, gaming, collaboration, live trading. Use when you genuinely need bidirectional, low-latency messaging — and accept the scaling complexity.

> A common, pragmatic rule: **default to SSE for one-way real-time** (it's simpler and auto-reconnects), and **reach for WebSockets only when you truly need bidirectional** communication.

---

## Real-World / Fintech Example

Our **digital wallet / payments app** has several real-time needs, each best served by a different technique — illustrating "pick the least powerful that fits."

**SSE — payment status & notifications (server → client).** When Bob receives a payment, he should see "₹500 received from Alice" *instantly*, without refreshing. This is **one-way** (server pushes to Bob; Bob doesn't reply over this channel) — a perfect fit for **SSE**. Bob's app opens an `EventSource` to a notifications stream; the server pushes events as they happen. The key integration: when Alice's payment completes, the Payment service emits a `PaymentCompleted` event to **Kafka** (Chapter 19/21); a notification service consuming that event **pushes it down Bob's SSE stream**. And because Bob's connection might be on any of many servers, a **Redis/Kafka pub-sub backplane** routes the event to the right server holding Bob's connection. SSE's **built-in auto-reconnect** is a bonus — if Bob's phone briefly loses signal, the stream silently re-establishes. *One-way real-time → SSE, simpler and robust.*

**WebSockets — live support chat (two-way).** The in-app support chat needs **bidirectional** real-time: Alice types and sends messages, the agent replies, both see typing indicators — both sides push freely and instantly. This *requires* **WebSockets**. The team accepts the added complexity: connections are stateful, so they use **sticky sessions** at the load balancer and a **Redis pub/sub backplane** so an agent's message reaches whichever server holds Alice's socket (the routing challenge above). *True two-way real-time → WebSockets.*

**Long polling — the compatibility fallback.** Some corporate/banking networks have restrictive proxies that block WebSockets and even SSE. For those clients, the app **falls back to long polling** — less efficient, but works over plain HTTP everywhere, ensuring no user is left without near-real-time updates. *Robust fallback → long polling.*

**Live transaction feed (web dashboard) — SSE.** A merchant's web dashboard shows incoming payments scrolling in live. One-way server→client stream of payment events → **SSE** again, fed by the same Kafka `payment-events` stream. The dashboard just listens.

**Why not WebSockets for everything?** A junior might use WebSockets for *all* real-time features "to be consistent." But for the one-way notification/feed cases, WebSockets add needless complexity (manual reconnect, heavier scaling) over SSE. The staff-engineer move is **matching the technique to the directionality**: SSE for one-way, WebSockets only for genuinely two-way. *Least powerful tool that meets the need = most robust system.*

In Spring Boot: SSE via `SseEmitter` (or reactive `Flux<ServerSentEvent>` in WebFlux), WebSockets via `spring-boot-starter-websocket` (often with **STOMP** messaging and a Redis/external broker relay for the backplane), and long polling via deferred results (`DeferredResult`/`Callable`) that hold the request open. The pushed data typically originates from Kafka consumers reacting to domain events (Chapter 21).

---

## Trade-offs (Pros & Cons)

### Long Polling
**Pros:** works over plain HTTP everywhere (great fallback); simple; near-real-time; no special infrastructure.
**Cons:** per-request HTTP overhead; holding many open requests strains servers; not suited to high-frequency updates; manual reconnect handling.

### Server-Sent Events (SSE)
**Pros:** simple, efficient **one-way** streaming over standard HTTP; **built-in auto-reconnect** with resume; firewall/proxy-friendly; ideal for notifications/feeds.
**Cons:** **one-directional only** (client can't push back over it); per-domain connection limits on HTTP/1.1; text-only (no binary); persistent connections still complicate scaling.

### WebSockets
**Pros:** true **full-duplex** real-time; lowest per-message overhead; supports binary; the right tool for chat/gaming/collaboration/trading.
**Cons:** **higher complexity**; stateful connections complicate horizontal scaling (need sticky sessions + pub/sub backplane); manual reconnection; some proxies/LBs need special config; overkill for one-way needs.

> **Staff-engineer takeaway:** When the **server** must push data to clients in real time, plain HTTP request/response isn't enough. Climb the ladder by need: **long polling** (HTTP-compatible, great fallback) → **SSE** (efficient *one-way* server→client streaming with auto-reconnect — ideal for notifications and feeds) → **WebSockets** (full-duplex *two-way* — for chat, gaming, collaboration, trading). **Pick the least powerful technique that meets the need** — SSE over WebSockets whenever it's one-directional. And remember persistent connections are **stateful**, so at scale you need sticky sessions and a **Redis/Kafka pub-sub backplane** to route pushes to the server holding each client's connection.

---

➡️ **End of Batch 8.** You now understand how clients reach your system (API Gateway/BFF), the API styles and protocols they use (REST/GraphQL/gRPC), and how to push data back to clients in real time (long polling/SSE/WebSockets). The next batch shifts to code-level craft and robustness: **Design Patterns**, **Resiliency**, and **Designing for Resiliency**.
