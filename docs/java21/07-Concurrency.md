# Chapter 7: Multithreading & Concurrency

[← Back to Index](00-README.md) | [Previous: Exceptions](06-Exceptions.md) | [Next: Streams & FP →](08-Streams-FP.md)

---

## Why Concurrency is Critical

> **The Reality**: Modern applications MUST handle concurrency. Single-threaded code 
> cannot leverage multi-core CPUs. But incorrect concurrent code causes the worst bugs.

### Why Every Developer Must Understand Concurrency

**1. Hardware Reality**: Modern CPUs have multiple cores. A single-threaded application on an 8-core machine uses only 12.5% of available computing power. To utilize modern hardware, you MUST write concurrent code.

**2. Responsiveness**: In GUI applications and web servers, long-running operations block the main thread, making the application unresponsive. Concurrency allows background processing while keeping the UI/server responsive.

**3. Throughput**: Web servers handle thousands of simultaneous requests. Without concurrency, each request would wait for all previous requests to complete.

**4. The Interview Reality**: Concurrency is tested in EVERY senior Java interview. Companies know that concurrency bugs are the most costly—they're non-deterministic, hard to reproduce, and can cause data corruption in production.

### Why Concurrency Bugs Are So Dangerous

Concurrency bugs are fundamentally different from other bugs:

| Aspect | Regular Bugs | Concurrency Bugs |
|--------|--------------|------------------|
| **Reproducibility** | Same input = same bug | Same input might work 999 times, fail once |
| **Debugging** | Add print statements, use debugger | Debugger changes timing, hides bugs |
| **Testing** | Tests catch most bugs | Tests rarely catch race conditions |
| **Manifestation** | Immediate crash or wrong output | Silent data corruption, discovered months later |
| **Root Cause** | Logic error | Timing-dependent interleaving |

> ⚠️ **Warning**: A race condition that appears "fixed" might just be hidden. The bug still exists—it's just less likely to trigger. This is why understanding concurrency fundamentals is more important than just knowing APIs.

---

## Layer 1 — Beginner Foundation

### What is a Thread? (The Mental Model)

Think of your program as a **kitchen** in a restaurant:

- **Process** = The entire kitchen (has its own space, equipment, ingredients)
- **Thread** = A chef working in that kitchen
- **Single-threaded** = One chef doing everything (takes orders, cooks, serves)
- **Multi-threaded** = Multiple chefs working simultaneously

With one chef, tasks happen sequentially:
```
Take Order → Prepare Appetizer → Cook Main → Make Dessert → Serve
(Customer waits for everything to be done in sequence)
```

With multiple chefs (threads), tasks can overlap:
```
Chef 1: Take Order → Cook Main
Chef 2: Prepare Appetizer → Make Dessert  
Chef 3: Prepare Drinks → Serve
(Much faster! But chefs might collide at the stove...)
```

The "collision at the stove" is exactly what we call a **race condition** in programming.

### Process vs Thread: Technical Deep Dive

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         PROCESS vs THREAD                                   │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PROCESS                                  THREAD                            │
│  ═══════                                  ══════                            │
│                                                                             │
│  • An independent program in execution    • A unit of execution within      │
│                                            a process                        │
│                                                                             │
│  • Has its OWN memory space               • SHARES memory with other        │
│    (heap, stack, data segments)            threads in same process          │
│                                                                             │
│  • Heavy to create (OS allocates          • Light to create (shares         │
│    new memory, resources)                  process resources)               │
│                                                                             │
│  • Isolated from other processes          • Can directly access other       │
│    (crash in one doesn't affect others)    thread's data (dangerous!)       │
│                                                                             │
│  • Communication via IPC (pipes,          • Communication via shared        │
│    sockets, shared memory)                 memory (fast but needs sync)     │
│                                                                             │
│  Example: Chrome tabs (each tab is        Example: Browser rendering        │
│           a separate process)             thread, JavaScript thread,        │
│                                           network thread                    │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

**What Each Thread Has (Private)**:
- **Program Counter (PC)**: Which instruction is currently executing
- **Stack**: Local variables, method call chain
- **Thread-local storage**: Data specific to this thread

**What Threads Share (Shared = Danger Zone)**:
- **Heap**: All objects live here—shared by all threads
- **Method Area/Metaspace**: Class data, static variables
- **Open files, network connections**: Process-level resources

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              JAVA PROCESS                                 │
│                                                                           │
│   ┌────────────────────────────────────────────────────────────────────┐ │
│   │                     SHARED MEMORY (Heap)                            │ │
│   │                                                                     │ │
│   │    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │ │
│   │    │   Object A   │  │   Object B   │  │   Object C   │           │ │
│   │    │ (e.g., List) │  │(e.g., Cache) │  │(e.g., Config)│           │ │
│   │    └──────────────┘  └──────────────┘  └──────────────┘           │ │
│   │                                                                     │ │
│   │    ⚠️ ALL threads can read AND write these objects                 │ │
│   │    ⚠️ This is where race conditions happen                         │ │
│   │                                                                     │ │
│   └────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│   ┌────────────────┐  ┌────────────────┐  ┌────────────────┐            │
│   │    THREAD 1    │  │    THREAD 2    │  │    THREAD 3    │            │
│   │    (main)      │  │   (worker-1)   │  │   (worker-2)   │            │
│   │                │  │                │  │                │            │
│   │  ┌──────────┐  │  │  ┌──────────┐  │  │  ┌──────────┐  │            │
│   │  │ Stack    │  │  │  │ Stack    │  │  │  │ Stack    │  │            │
│   │  │          │  │  │  │          │  │  │  │          │  │            │
│   │  │ methodA()│  │  │  │ methodX()│  │  │  │ methodY()│  │            │
│   │  │ main()   │  │  │  │ run()    │  │  │  │ run()    │  │            │
│   │  │          │  │  │  │          │  │  │  │          │  │            │
│   │  │ Local:   │  │  │  │ Local:   │  │  │  │ Local:   │  │            │
│   │  │ int x=5  │  │  │  │ int y=10 │  │  │  │ int z=15 │  │            │
│   │  └──────────┘  │  │  └──────────┘  │  │  └──────────┘  │            │
│   │                │  │                │  │                │            │
│   │  PC: line 42   │  │  PC: line 78   │  │  PC: line 23   │            │
│   │                │  │                │  │                │            │
│   └────────────────┘  └────────────────┘  └────────────────┘            │
│                                                                           │
│   ✅ Each thread's local variables are SAFE (not shared)                 │
│   ⚠️ Heap objects accessed by multiple threads need PROTECTION            │
│                                                                           │
└──────────────────────────────────────────────────────────────────────────┘
```

### Creating Threads in Java

There are three main ways to create threads. Understanding when to use each is important.

#### Method 1: Extending the Thread Class

```java
public class MyThread extends Thread {
    
    private String taskName;
    
    public MyThread(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        // This code executes in the NEW thread
        System.out.println("Executing: " + taskName);
        System.out.println("Running on thread: " + Thread.currentThread().getName());
    }
}

// Usage:
public class Main {
    public static void main(String[] args) {
        MyThread thread = new MyThread("Data Processing");
        
        // CRITICAL: Call start(), NOT run()!
        thread.start();  // ✅ Creates new thread, calls run() in that thread
        // thread.run();  // ❌ Just calls run() in the CURRENT thread (main)
        
        System.out.println("Main thread continues...");
    }
}
```

**When to use**: Almost never. This approach "uses up" your single inheritance slot. Java allows only one superclass, so if you extend Thread, you can't extend anything else.

#### Method 2: Implementing Runnable (Preferred)

```java
public class MyTask implements Runnable {
    
    private String taskName;
    
    public MyTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        System.out.println("Executing: " + taskName);
        System.out.println("Running on thread: " + Thread.currentThread().getName());
    }
}

// Usage:
public class Main {
    public static void main(String[] args) {
        // Create the task (Runnable)
        Runnable task = new MyTask("Data Processing");
        
        // Wrap it in a Thread
        Thread thread = new Thread(task);
        
        // Start the thread
        thread.start();
    }
}
```

**Why is Runnable preferred?**
1. **Separation of concerns**: Task logic (what to do) is separate from thread mechanics (how to run)
2. **Flexibility**: Your class can extend another class AND implement Runnable
3. **Reusability**: Same Runnable can be executed by different threads or thread pools
4. **Thread pool friendly**: ExecutorService works with Runnable

#### Method 3: Lambda Expression (Most Concise)

```java
public class Main {
    public static void main(String[] args) {
        // Since Runnable is a functional interface (one abstract method),
        // we can use lambda expressions
        
        Thread thread = new Thread(() -> {
            System.out.println("Executing in thread: " + Thread.currentThread().getName());
            
            // Your task code here
            for (int i = 0; i < 5; i++) {
                System.out.println("Count: " + i);
                try {
                    Thread.sleep(500);  // Pause for 500 milliseconds
                } catch (InterruptedException e) {
                    System.out.println("Thread was interrupted!");
                    return;  // Exit gracefully
                }
            }
        });
        
        thread.setName("CounterThread");  // Give it a meaningful name
        thread.start();
        
        System.out.println("Main thread continues while CounterThread runs...");
    }
}
```

**When to use**: For simple, short tasks. For complex logic, extract to a named class.

### The Critical Difference: start() vs run()

This is a **very common beginner mistake** and a frequent interview question:

```java
Thread thread = new Thread(() -> {
    System.out.println("Running on: " + Thread.currentThread().getName());
});

// CORRECT: Creates a new thread
thread.start();
// Output: "Running on: Thread-0"

// WRONG: Runs in the current thread
thread.run();
// Output: "Running on: main"
```

**What `start()` does**:
1. Allocates resources for a new thread (stack, program counter)
2. Puts the thread in RUNNABLE state
3. JVM schedules it—when it gets CPU time, `run()` is called automatically
4. Returns immediately (does NOT wait for thread to complete)

**What `run()` does**:
- Just calls the method—like any other method call
- Executes in the CURRENT thread
- Provides NO concurrency

> 🎯 **Interview Tip**: If asked "What happens if you call run() instead of start()?", 
> answer: "The code in run() executes synchronously in the calling thread. No new thread 
> is created, so there's no concurrency. It's just a regular method call."

### Thread Lifecycle States

Understanding thread states helps with debugging and reasoning about concurrent programs:

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                           THREAD LIFECYCLE                                     │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                │
│                          Thread created                                        │
│                               ▼                                                │
│                               │                                                │
│                         ┌─────────┐                                            │
│                         │   NEW   │  Thread object exists, but start()         │
│                         │         │  hasn't been called yet                    │
│                         └────┬────┘                                            │
│                              │                                                 │
│                              │ start() called                                  │
│                              ▼                                                 │
│                      ┌──────────────┐                                          │
│              ┌──────►│   RUNNABLE   │◄──────────────────────────┐              │
│              │       │              │                           │              │
│              │       │ Ready to run │                           │              │
│              │       │ OR running   │                           │              │
│              │       └──────┬───────┘                           │              │
│              │              │                                    │             │
│              │              │ Various blocking conditions        │             │
│              │              ▼                                    │             │
│              │    ┌─────────────────────────────────────────────┴───┐          │
│              │    │                                                 │          │
│   Acquired   │    │                                                 │ Time     │
│   lock or    │    ▼                   ▼                   ▼         │ elapsed  │
│   notify()   │ ┌──────────┐     ┌──────────┐     ┌────────────────┐ │ or       │
│   called     │ │ BLOCKED  │     │ WAITING  │     │ TIMED_WAITING  │ │ notify() │
│              │ │          │     │          │     │                │ │ or join  │
│              │ │Waiting   │     │Waiting   │     │Waiting with    │ │ complete │
│              │ │for lock  │     │for       │     │timeout         │ │          │
│              │ │          │     │notify()  │     │                │ │          │
│              │ │e.g.,     │     │          │     │e.g.,           │ │          │
│              │ │trying to │     │e.g.,     │     │Thread.sleep()  │ │          │
│              │ │enter     │     │wait()    │     │join(timeout)   │ │          │
│              │ │synchro-  │     │join()    │     │wait(timeout)   │ │          │
│              │ │nized     │     │park()    │     │                │ │          │
│              │ │block     │     │          │     │                │ │          │
│              │ └─────┬────┘     └────┬─────┘     └───────┬────────┘ │          │
│              │       │               │                   │          │          │
│              │       └───────────────┴───────────────────┘          │          │
│              │                       │                               │         │
│              └───────────────────────┴───────────────────────────────┘         │
│                                                                                │
│                              run() completes or                                │
│                              exception thrown                                  │
│                                      │                                         │
│                                      ▼                                         │
│                               ┌──────────────┐                                 │
│                               │  TERMINATED  │  Thread is dead.                │
│                               │              │  Cannot be started again.       │
│                               └──────────────┘                                 │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

**State Descriptions**:

| State | Description | How to Get Here | How to Leave |
|-------|-------------|-----------------|--------------|
| **NEW** | Thread object created, `start()` not yet called | `new Thread()` | Call `start()` |
| **RUNNABLE** | Ready to run or currently running | `start()` called, or woke up from blocking | Block, wait, or complete |
| **BLOCKED** | Waiting to acquire a lock | Try to enter synchronized block that's locked | Lock becomes available |
| **WAITING** | Waiting indefinitely for another thread | `wait()`, `join()`, `LockSupport.park()` | `notify()`, `notifyAll()`, joined thread ends |
| **TIMED_WAITING** | Waiting with a timeout | `sleep(ms)`, `wait(ms)`, `join(ms)` | Timeout expires or notify/join |
| **TERMINATED** | Thread has completed execution | `run()` returns or throws exception | Cannot leave—thread is dead |

```java
// Demonstrating thread states:
public class ThreadStateDemo {
    public static void main(String[] args) throws InterruptedException {
        Object lock = new Object();
        
        Thread thread = new Thread(() -> {
            try {
                // Will enter TIMED_WAITING
                Thread.sleep(2000);
                
                synchronized (lock) {
                    // Will enter WAITING
                    lock.wait();
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted!");
            }
        });
        
        System.out.println("After creation: " + thread.getState());  // NEW
        
        thread.start();
        System.out.println("After start: " + thread.getState());  // RUNNABLE
        
        Thread.sleep(500);
        System.out.println("During sleep: " + thread.getState());  // TIMED_WAITING
        
        Thread.sleep(2000);
        System.out.println("During wait: " + thread.getState());  // WAITING
        
        // Wake it up
        synchronized (lock) {
            lock.notify();
        }
        
        thread.join();
        System.out.println("After completion: " + thread.getState());  // TERMINATED
    }
}
```

### Essential Thread Methods

Understanding these methods is crucial for working with threads:

```java
public class ThreadMethodsExplained {
    
    public static void main(String[] args) throws InterruptedException {
        
        // ═══════════════════════════════════════════════════════════════
        // THREAD CREATION AND CONFIGURATION
        // ═══════════════════════════════════════════════════════════════
        
        Thread workerThread = new Thread(() -> {
            System.out.println("Worker executing on: " + Thread.currentThread().getName());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("Worker was interrupted!");
            }
        });
        
        // Set a meaningful name (VERY helpful for debugging)
        workerThread.setName("DataProcessor");
        
        // Set priority (1-10, default is 5)
        // Note: Priority is just a HINT to the OS scheduler, not a guarantee
        workerThread.setPriority(Thread.NORM_PRIORITY);  // 5
        // Thread.MIN_PRIORITY = 1
        // Thread.MAX_PRIORITY = 10
        
        // ═══════════════════════════════════════════════════════════════
        // DAEMON THREADS
        // ═══════════════════════════════════════════════════════════════
        
        // Daemon threads don't prevent JVM from exiting
        // When all non-daemon threads finish, JVM exits (killing daemon threads)
        workerThread.setDaemon(true);
        
        // Good for: Background tasks, monitoring, garbage collection
        // Bad for: Tasks that MUST complete (data saving, cleanup)
        
        // ⚠️ Must set daemon BEFORE start()
        
        // ═══════════════════════════════════════════════════════════════
        // STARTING AND INFORMATION
        // ═══════════════════════════════════════════════════════════════
        
        workerThread.start();
        
        System.out.println("Thread name: " + workerThread.getName());
        System.out.println("Thread ID: " + workerThread.threadId());  // Java 19+
        System.out.println("Thread state: " + workerThread.getState());
        System.out.println("Is alive: " + workerThread.isAlive());
        System.out.println("Is daemon: " + workerThread.isDaemon());
        
        // Get the currently executing thread
        Thread currentThread = Thread.currentThread();
        System.out.println("Current thread: " + currentThread.getName());  // "main"
        
        // ═══════════════════════════════════════════════════════════════
        // WAITING FOR COMPLETION: join()
        // ═══════════════════════════════════════════════════════════════
        
        // join() blocks the current thread until the target thread completes
        
        workerThread.join();          // Wait indefinitely
        // workerThread.join(1000);   // Wait at most 1 second
        // workerThread.join(1000, 500000);  // Wait at most 1 second + 500,000 nanoseconds
        
        System.out.println("Worker has completed!");
        
        // ═══════════════════════════════════════════════════════════════
        // INTERRUPTION
        // ═══════════════════════════════════════════════════════════════
        
        // Interruption is a COOPERATIVE mechanism for stopping threads
        // It doesn't FORCE the thread to stop—it asks politely
        
        Thread longTask = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Working...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Sleep was interrupted
                    System.out.println("Sleep interrupted, exiting gracefully");
                    Thread.currentThread().interrupt();  // Restore interrupt status
                    return;
                }
            }
            System.out.println("Thread noticed interrupt flag and is exiting");
        });
        
        longTask.start();
        Thread.sleep(3000);  // Let it run for 3 seconds
        
        longTask.interrupt();  // Request interruption
        
        // The thread should check isInterrupted() or catch InterruptedException
        // and respond appropriately (usually by exiting)
    }
}
```

### Thread.sleep() vs Object.wait() - A Common Confusion

| Aspect | Thread.sleep(ms) | Object.wait() |
|--------|-----------------|---------------|
| **Purpose** | Pause current thread for specified time | Wait for notification from another thread |
| **Lock behavior** | Keeps all locks held | Releases the lock on the object |
| **Wake up** | After time elapses (or interrupt) | After notify()/notifyAll() (or interrupt/timeout) |
| **Requires lock?** | No | Yes—must be in synchronized block |
| **Static?** | Yes—`Thread.sleep()` | No—`object.wait()` |

```java
// sleep() - Just pauses, keeps locks
synchronized (lock) {
    // Still holding lock during sleep!
    Thread.sleep(1000);  // Other threads can't enter this synchronized block
}

// wait() - Pauses AND releases lock
synchronized (lock) {
    // Releases lock while waiting!
    lock.wait();  // Other threads CAN enter synchronized blocks on 'lock'
    // Re-acquires lock before continuing
}
```

---

## Layer 2 — Working Developer Level

### The Fundamental Problem: Race Conditions

A **race condition** occurs when two or more threads access shared data, at least one modifies it, and the outcome depends on the timing of execution.

**Why is `count++` NOT thread-safe?**

It looks like one operation, but it's actually THREE:
1. **READ**: Load current value of `count` from memory into CPU register
2. **MODIFY**: Increment the value in the register
3. **WRITE**: Store the new value back to memory

```
THE RACE CONDITION EXPLAINED:

Initial state: count = 0

Time   Thread 1                    Thread 2                     Actual count
─────  ──────────────────────────  ───────────────────────────  ────────────
T1     READ count (sees 0)                                       0
T2                                  READ count (sees 0)          0
T3     INCREMENT (0 → 1)                                         0
T4                                  INCREMENT (0 → 1)            0
T5     WRITE count (writes 1)                                    1
T6                                  WRITE count (writes 1)       1

Result: count = 1, but we did TWO increments!
        We LOST an update because both threads read the same initial value.
```

Here's a demonstration you can run:

```java
public class RaceConditionDemo {
    
    private int unsafeCount = 0;
    
    public void unsafeIncrement() {
        unsafeCount++;  // NOT ATOMIC!
    }
    
    public static void main(String[] args) throws InterruptedException {
        RaceConditionDemo demo = new RaceConditionDemo();
        
        // Create two threads, each incrementing 100,000 times
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                demo.unsafeIncrement();
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) {
                demo.unsafeIncrement();
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        System.out.println("Expected: 200000");
        System.out.println("Actual: " + demo.unsafeCount);
        // Actual will be LESS than 200000 due to lost updates!
        // Run multiple times—you'll get different results each time!
    }
}
```

### The Solution: Synchronization

**Synchronization** ensures that only one thread can execute a critical section at a time. Java provides the `synchronized` keyword for this.

#### Understanding synchronized

The `synchronized` keyword creates a **mutual exclusion lock** (mutex). When a thread enters a synchronized block:
1. It acquires the lock (or waits if another thread holds it)
2. Executes the code
3. Releases the lock (even if an exception occurs)

```java
public class SynchronizationExplained {
    
    private int count = 0;
    
    // ═══════════════════════════════════════════════════════════════════════
    // OPTION 1: Synchronized Method
    // ═══════════════════════════════════════════════════════════════════════
    
    // The lock is on 'this' (the current object instance)
    public synchronized void incrementMethod() {
        count++;  // Now safe!
    }
    
    // This is equivalent to:
    public void incrementMethodEquivalent() {
        synchronized (this) {
            count++;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // OPTION 2: Synchronized Block (More Granular Control)
    // ═══════════════════════════════════════════════════════════════════════
    
    public void incrementBlock() {
        // Code before synchronized block runs without lock
        System.out.println("Preparing...");  // No lock needed here
        
        synchronized (this) {
            count++;  // Only this part is synchronized
        }
        
        // Code after synchronized block runs without lock
        System.out.println("Done");  // No lock needed here
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // OPTION 3: Custom Lock Object (Recommended for Complex Cases)
    // ═══════════════════════════════════════════════════════════════════════
    
    private final Object countLock = new Object();  // Dedicated lock
    private final Object otherLock = new Object();  // For other resources
    
    public void incrementWithCustomLock() {
        synchronized (countLock) {  // Only locks countLock, not 'this'
            count++;
        }
    }
    
    // This allows different locks for different resources,
    // enabling more concurrency (threads can work on different resources)
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATIC SYNCHRONIZATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private static int staticCount = 0;
    
    // For static methods, the lock is on the Class object
    public static synchronized void staticIncrement() {
        staticCount++;
    }
    
    // Equivalent to:
    public static void staticIncrementEquivalent() {
        synchronized (SynchronizationExplained.class) {
            staticCount++;
        }
    }
    
    // ⚠️ IMPORTANT: Instance locks and class locks are DIFFERENT!
    // A thread holding an instance lock does NOT block threads
    // trying to acquire the class lock, and vice versa.
}
```

#### Common Synchronization Mistakes

```java
public class SynchronizationMistakes {
    
    // ❌ MISTAKE 1: Synchronizing on a changing reference
    private String value = "initial";
    
    public void badSync() {
        synchronized (value) {  // WRONG! 'value' reference changes
            value = "new value";  // Now lock object is different!
        }
    }
    
    // ✅ FIX: Use a final lock object
    private final Object lock = new Object();
    
    public void goodSync() {
        synchronized (lock) {  // Lock object never changes
            value = "new value";
        }
    }
    
    // ❌ MISTAKE 2: Synchronizing on boxed primitives
    private Integer count = 0;
    
    public void badCountSync() {
        synchronized (count) {  // WRONG! Integer objects are cached/reused
            count++;  // Creates new Integer object each time!
        }
    }
    
    // ❌ MISTAKE 3: Partial synchronization
    private int data = 0;
    
    public synchronized void write(int value) {
        data = value;
    }
    
    public int read() {  // NOT synchronized!
        return data;  // May see stale value
    }
    
    // ✅ FIX: Synchronize both read and write, or use volatile
    public synchronized int safeRead() {
        return data;
    }
}
```

### The volatile Keyword: Visibility Without Locking

**The Problem**: Each CPU core has its own cache. Without special handling, a thread might read a cached value that's out of date.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    THE VISIBILITY PROBLEM                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│   Thread 1 (Core 1)              Thread 2 (Core 2)                           │
│   ┌─────────────────┐            ┌─────────────────┐                         │
│   │  CPU Cache      │            │  CPU Cache      │                         │
│   │  ┌───────────┐  │            │  ┌───────────┐  │                         │
│   │  │ running=  │  │            │  │ running=  │  │                         │
│   │  │   true    │  │            │  │   true    │  │                         │
│   │  └───────────┘  │            │  └───────────┘  │                         │
│   └────────┬────────┘            └────────┬────────┘                         │
│            │                              │                                   │
│            │                              │                                   │
│   ─────────┴──────────────────────────────┴───────────────────               │
│                                                                               │
│                      MAIN MEMORY                                              │
│                   ┌─────────────────┐                                        │
│                   │ running = false │  ← Thread 1 wrote false                │
│                   └─────────────────┘    but Thread 2's cache                │
│                                          still has true!                      │
│                                                                               │
│   Thread 2 might loop forever because it never sees the update!              │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

**`volatile` forces reads and writes to go directly to main memory**:

```java
public class VolatileExplained {
    
    // WITHOUT volatile - might run forever!
    private boolean normalFlag = true;
    
    // WITH volatile - guaranteed to see updates
    private volatile boolean volatileFlag = true;
    
    public void demonstrateProblem() {
        // Background thread checks flag
        new Thread(() -> {
            System.out.println("Worker starting...");
            while (normalFlag) {
                // Tight loop - compiler/CPU might optimize to read from cache forever
                // NEVER seeing the update to normalFlag!
            }
            System.out.println("Worker stopped");  // Might never print!
        }).start();
        
        // Main thread changes flag after 1 second
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        normalFlag = false;  // Worker might not see this!
        System.out.println("Flag set to false");
    }
    
    public void demonstrateSolution() {
        new Thread(() -> {
            System.out.println("Worker starting...");
            while (volatileFlag) {
                // With volatile, each read goes to main memory
                // GUARANTEED to eventually see the update
            }
            System.out.println("Worker stopped");  // Will print!
        }).start();
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        volatileFlag = false;  // Worker will see this!
        System.out.println("Flag set to false");
    }
}
```

**What `volatile` guarantees**:
1. **Visibility**: Writes are immediately visible to other threads
2. **Ordering**: Prevents reordering of reads/writes around volatile access

**What `volatile` does NOT guarantee**:
1. **Atomicity**: `volatileCount++` is still NOT thread-safe!

```java
private volatile int count = 0;

// Still a race condition!
public void increment() {
    count++;  // READ + INCREMENT + WRITE - can still interleave!
}

// For atomic compound operations, use:
// - synchronized
// - AtomicInteger
// - Locks
```

**When to use `volatile`**:
- Simple flags (start/stop signals)
- Publishing immutable objects
- Single writer, multiple reader scenarios
- Double-checked locking pattern (in combination with synchronization)

### wait(), notify(), and notifyAll(): Thread Communication

These methods allow threads to communicate and coordinate. They must be called from within a synchronized block on the object.

```java
public class ProducerConsumerExplained {
    
    private final Queue<Integer> buffer = new LinkedList<>();
    private final int MAX_CAPACITY = 5;
    
    /**
     * Producer adds items to the buffer.
     * If buffer is full, it waits until consumer removes something.
     */
    public synchronized void produce(int item) throws InterruptedException {
        // Use WHILE, not IF! (Spurious wakeups can occur)
        while (buffer.size() == MAX_CAPACITY) {
            System.out.println("Buffer full, producer waiting...");
            wait();  // 1. Release lock, 2. Wait for notify, 3. Re-acquire lock
        }
        
        buffer.add(item);
        System.out.println("Produced: " + item + " (buffer size: " + buffer.size() + ")");
        
        notifyAll();  // Wake up waiting consumers
    }
    
    /**
     * Consumer removes items from the buffer.
     * If buffer is empty, it waits until producer adds something.
     */
    public synchronized int consume() throws InterruptedException {
        while (buffer.isEmpty()) {
            System.out.println("Buffer empty, consumer waiting...");
            wait();
        }
        
        int item = buffer.remove();
        System.out.println("Consumed: " + item + " (buffer size: " + buffer.size() + ")");
        
        notifyAll();  // Wake up waiting producers
        
        return item;
    }
    
    public static void main(String[] args) {
        ProducerConsumerExplained pc = new ProducerConsumerExplained();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    pc.produce(i);
                    Thread.sleep(100);  // Simulate work
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Producer");
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    pc.consume();
                    Thread.sleep(150);  // Consumer is slower
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Consumer");
        
        producer.start();
        consumer.start();
    }
}
```

**Why use `while` instead of `if` for wait()?**

```java
// ❌ WRONG - Using if
synchronized (lock) {
    if (condition) {
        wait();
    }
    // After waking up, condition might not be true anymore!
    // Another thread might have changed it between notify and this line
    doSomething();  // Might fail!
}

// ✅ CORRECT - Using while
synchronized (lock) {
    while (condition) {
        wait();
    }
    // Re-check condition after waking up
    // Only proceed when condition is definitely false
    doSomething();  // Safe!
}
```

**notify() vs notifyAll()**:
- `notify()`: Wakes up ONE waiting thread (arbitrary choice by JVM)
- `notifyAll()`: Wakes up ALL waiting threads (they compete for the lock)

> 🎯 **Best Practice**: Prefer `notifyAll()`. Using `notify()` can cause deadlock if the wrong thread is chosen. `notifyAll()` is safer—waiting threads will re-check their conditions.

### Common Concurrency Problems

#### 1. Deadlock: Two Threads Waiting for Each Other

```java
public class DeadlockExplained {
    
    private final Object resourceA = new Object();
    private final Object resourceB = new Object();
    
    // Thread 1 calls this: locks A, then wants B
    public void method1() {
        synchronized (resourceA) {
            System.out.println(Thread.currentThread().getName() + " locked A");
            
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized (resourceB) {  // Waits for B... forever
                System.out.println(Thread.currentThread().getName() + " locked B");
            }
        }
    }
    
    // Thread 2 calls this: locks B, then wants A
    public void method2() {
        synchronized (resourceB) {  // Opposite order!
            System.out.println(Thread.currentThread().getName() + " locked B");
            
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized (resourceA) {  // Waits for A... forever
                System.out.println(Thread.currentThread().getName() + " locked A");
            }
        }
    }
    
    public static void main(String[] args) {
        DeadlockExplained deadlock = new DeadlockExplained();
        
        new Thread(deadlock::method1, "Thread-1").start();
        new Thread(deadlock::method2, "Thread-2").start();
        
        // Output:
        // Thread-1 locked A
        // Thread-2 locked B
        // ... program hangs forever (deadlock!)
    }
}
```

**Deadlock requires ALL four conditions** (Coffman conditions):
1. **Mutual Exclusion**: Resources can't be shared (only one thread can hold)
2. **Hold and Wait**: Thread holds one resource while waiting for another
3. **No Preemption**: Can't forcibly take a resource from a thread
4. **Circular Wait**: Thread 1 waits for Thread 2, which waits for Thread 1

**How to prevent deadlock**:

```java
// SOLUTION: Always acquire locks in the same order
public void method1Fixed() {
    synchronized (resourceA) {  // Always A first
        synchronized (resourceB) {  // Then B
            // Work
        }
    }
}

public void method2Fixed() {
    synchronized (resourceA) {  // Same order: A first
        synchronized (resourceB) {  // Then B
            // Work
        }
    }
}
```

#### 2. Livelock: Threads Keep Responding But Make No Progress

```java
// Analogy: Two people in a hallway, both step aside, then both step back,
// then both step aside again... forever politely blocking each other.

public class LivelockExample {
    
    static class Spoon {
        private Diner owner;
        
        public synchronized void use() {
            System.out.println(owner.name + " is eating!");
        }
        
        public synchronized void setOwner(Diner newOwner) {
            this.owner = newOwner;
        }
        
        public synchronized Diner getOwner() {
            return owner;
        }
    }
    
    static class Diner {
        private String name;
        private boolean isHungry;
        
        public Diner(String name) {
            this.name = name;
            this.isHungry = true;
        }
        
        public void eatWith(Spoon spoon, Diner spouse) {
            while (isHungry) {
                if (spoon.getOwner() != this) {
                    try { Thread.sleep(1); } catch (InterruptedException e) {}
                    continue;
                }
                
                // Be polite: if spouse is hungry, give them the spoon
                if (spouse.isHungry) {
                    System.out.println(name + ": You eat first, dear " + spouse.name);
                    spoon.setOwner(spouse);  // Give up spoon
                    continue;  // Both keep being polite forever!
                }
                
                spoon.use();
                isHungry = false;
                spoon.setOwner(spouse);
            }
        }
    }
}
```

#### 3. Starvation: Thread Never Gets Resources

```java
// Thread starvation happens when:
// - Low priority threads never get CPU time
// - A thread holds a lock for too long
// - Unfair lock acquisition favors certain threads

// Example: Thread with MIN_PRIORITY might never run
// if higher priority threads are always ready
```

---

## Layer 3 — Advanced Engineering Depth

### java.util.concurrent: The Modern Concurrency Toolkit

The `java.util.concurrent` package (added in Java 5) provides higher-level, safer, and more powerful concurrency utilities than `synchronized` and `wait/notify`.

#### ReentrantLock: A More Flexible Lock

```java
public class ReentrantLockExplained {
    
    // ReentrantLock provides more control than synchronized
    private final ReentrantLock lock = new ReentrantLock();
    private int balance = 0;
    
    /**
     * Basic usage - equivalent to synchronized
     */
    public void deposit(int amount) {
        lock.lock();  // Acquire lock
        try {
            balance += amount;
        } finally {
            lock.unlock();  // ALWAYS unlock in finally!
        }
    }
    
    /**
     * Try-lock: Attempt to acquire without blocking
     * Useful for avoiding deadlocks
     */
    public boolean tryDeposit(int amount) {
        if (lock.tryLock()) {  // Returns immediately
            try {
                balance += amount;
                return true;
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Could not acquire lock, doing something else");
            return false;
        }
    }
    
    /**
     * Try-lock with timeout: Wait up to specified time
     */
    public boolean tryDepositWithTimeout(int amount) throws InterruptedException {
        if (lock.tryLock(1, TimeUnit.SECONDS)) {  // Wait up to 1 second
            try {
                balance += amount;
                return true;
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Timeout waiting for lock");
            return false;
        }
    }
    
    /**
     * Interruptible lock: Can be interrupted while waiting
     */
    public void interruptibleDeposit(int amount) throws InterruptedException {
        lock.lockInterruptibly();  // Can be interrupted
        try {
            balance += amount;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Fair lock: Threads acquire in order they requested
     * (Prevents starvation but has performance cost)
     */
    private final ReentrantLock fairLock = new ReentrantLock(true);  // true = fair
}
```

**When to use ReentrantLock vs synchronized**:

| Feature | synchronized | ReentrantLock |
|---------|--------------|---------------|
| Simplicity | ✅ Simpler, auto-release | ❌ Must unlock in finally |
| Try-lock | ❌ Not available | ✅ `tryLock()` |
| Timeout | ❌ Not available | ✅ `tryLock(timeout)` |
| Interruptible | ❌ Can't interrupt waiting thread | ✅ `lockInterruptibly()` |
| Fairness | ❌ Unfair | ✅ Optional fairness |
| Multiple conditions | ❌ One wait set | ✅ Multiple Conditions |

#### ReadWriteLock: Optimizing Read-Heavy Workloads

Many real-world scenarios have many more reads than writes. ReadWriteLock allows concurrent reads while ensuring exclusive writes.

```java
public class ReadWriteLockExplained {
    
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    
    private Map<String, Object> cache = new HashMap<>();
    
    /**
     * Multiple threads can read simultaneously
     */
    public Object read(String key) {
        readLock.lock();  // Multiple threads can hold this
        try {
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Only one thread can write, and no readers during write
     */
    public void write(String key, Object value) {
        writeLock.lock();  // Exclusive access
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

**Rules**:
- Multiple threads can hold the read lock simultaneously
- Only one thread can hold the write lock
- If any thread holds the read lock, no thread can acquire write lock
- If a thread holds write lock, no thread can acquire any lock

#### Atomic Classes: Lock-Free Thread Safety

Atomic classes use CPU-level atomic operations (Compare-And-Swap / CAS) instead of locks, making them faster under low-to-moderate contention.

```java
public class AtomicExplained {
    
    // Atomic primitives
    private AtomicInteger atomicInt = new AtomicInteger(0);
    private AtomicLong atomicLong = new AtomicLong(0);
    private AtomicBoolean atomicBool = new AtomicBoolean(false);
    
    // Atomic reference (for objects)
    private AtomicReference<String> atomicRef = new AtomicReference<>("initial");
    
    public void demonstrateAtomicInteger() {
        // Basic operations
        int value = atomicInt.get();           // Read
        atomicInt.set(10);                     // Write
        
        // Atomic compound operations (thread-safe!)
        int oldValue = atomicInt.getAndIncrement();   // i++ (returns old)
        int newValue = atomicInt.incrementAndGet();   // ++i (returns new)
        
        atomicInt.getAndAdd(5);    // Add and return old
        atomicInt.addAndGet(5);    // Add and return new
        
        // Compare-And-Set (CAS): The foundation of lock-free algorithms
        boolean success = atomicInt.compareAndSet(10, 20);
        // If current value is 10, set to 20 and return true
        // Otherwise, don't change and return false
        
        // Functional updates (Java 8+)
        atomicInt.updateAndGet(x -> x * 2);           // Double the value
        atomicInt.accumulateAndGet(10, Integer::sum); // Add 10
    }
    
    /**
     * HOW CAS WORKS (Compare-And-Swap):
     * 
     * 1. Read current value
     * 2. Compute new value
     * 3. Atomically: if current == expected, set to new value
     * 4. If CAS failed (value changed), retry from step 1
     * 
     * This is lock-free: no thread ever blocks, they just retry.
     * Under low contention, this is faster than locking.
     */
    public void incrementWithCAS() {
        int current;
        int next;
        do {
            current = atomicInt.get();
            next = current + 1;
        } while (!atomicInt.compareAndSet(current, next));
        // Keep trying until we successfully update
    }
}
```

**LongAdder: For High-Contention Counters**

When many threads update a counter frequently, `AtomicLong` becomes a bottleneck because all threads compete for the same memory location. `LongAdder` distributes updates across multiple "cells".

```java
public class LongAdderExplained {
    
    // Under high contention, LongAdder is much faster than AtomicLong
    private LongAdder counter = new LongAdder();
    
    public void increment() {
        counter.increment();  // Distributes to internal cell
    }
    
    public long getCount() {
        return counter.sum();  // Aggregates all cells
        // Note: sum() is eventually consistent during concurrent updates
    }
    
    // Use LongAdder for:
    // - Counters updated by many threads
    // - Statistics collection
    // - Metrics
    
    // Use AtomicLong for:
    // - When you need the exact value immediately after update
    // - Sequence generators
    // - Low-contention scenarios
}
```

### The Executor Framework: Thread Pool Management

Creating threads is expensive. Thread pools reuse threads, improving performance and providing better resource management.

```java
public class ExecutorFrameworkExplained {
    
    public static void main(String[] args) throws Exception {
        
        // ═══════════════════════════════════════════════════════════════════
        // THREAD POOL TYPES
        // ═══════════════════════════════════════════════════════════════════
        
        // 1. Fixed Thread Pool: Exactly N threads
        //    Best for: Known, steady workload
        ExecutorService fixed = Executors.newFixedThreadPool(4);
        
        // 2. Cached Thread Pool: Creates threads as needed, reuses idle ones
        //    Best for: Many short-lived tasks
        //    ⚠️ Warning: Can create unbounded threads under load!
        ExecutorService cached = Executors.newCachedThreadPool();
        
        // 3. Single Thread Executor: One thread, tasks execute sequentially
        //    Best for: Sequential task processing
        ExecutorService single = Executors.newSingleThreadExecutor();
        
        // 4. Scheduled Thread Pool: For delayed/periodic tasks
        ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);
        
        // 5. Virtual Thread Executor (Java 21): Lightweight threads
        //    Best for: IO-bound workloads with high concurrency
        ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor();
        
        // ═══════════════════════════════════════════════════════════════════
        // SUBMITTING TASKS
        // ═══════════════════════════════════════════════════════════════════
        
        // Runnable: No return value
        fixed.execute(() -> System.out.println("Runnable task"));
        
        // Callable: Returns a value
        Future<String> future = fixed.submit(() -> {
            Thread.sleep(1000);
            return "Task result";
        });
        
        // Get result (blocks until complete)
        String result = future.get();  // Can throw ExecutionException
        
        // Get with timeout
        String resultWithTimeout = future.get(2, TimeUnit.SECONDS);
        
        // Check status
        boolean isDone = future.isDone();
        boolean isCancelled = future.isCancelled();
        boolean wasCancelled = future.cancel(true);  // true = interrupt if running
        
        // ═══════════════════════════════════════════════════════════════════
        // SCHEDULED TASKS
        // ═══════════════════════════════════════════════════════════════════
        
        // Run once after delay
        scheduled.schedule(() -> System.out.println("Delayed task"),
                5, TimeUnit.SECONDS);
        
        // Run periodically at fixed rate (every N time units)
        scheduled.scheduleAtFixedRate(() -> System.out.println("Fixed rate"),
                0,      // Initial delay
                1,      // Period
                TimeUnit.SECONDS);
        
        // Run periodically with fixed delay between end of one and start of next
        scheduled.scheduleWithFixedDelay(() -> System.out.println("Fixed delay"),
                0,      // Initial delay
                1,      // Delay between executions
                TimeUnit.SECONDS);
        
        // ═══════════════════════════════════════════════════════════════════
        // SHUTDOWN (CRITICAL!)
        // ═══════════════════════════════════════════════════════════════════
        
        // Graceful shutdown: no new tasks, complete existing
        fixed.shutdown();
        
        // Wait for completion with timeout
        boolean terminated = fixed.awaitTermination(30, TimeUnit.SECONDS);
        
        // Force shutdown: interrupt running, return queued
        List<Runnable> notExecuted = fixed.shutdownNow();
        
        // Best practice shutdown pattern:
        fixed.shutdown();
        try {
            if (!fixed.awaitTermination(60, TimeUnit.SECONDS)) {
                fixed.shutdownNow();
                if (!fixed.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            fixed.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

**Thread Pool Sizing Guidelines**:

```java
// For CPU-bound tasks (computation, no waiting):
int cpuPoolSize = Runtime.getRuntime().availableProcessors();

// For IO-bound tasks (network, file, database):
// Threads spend most time waiting, so more threads are beneficial
int ioPoolSize = Runtime.getRuntime().availableProcessors() * 
                 (1 + waitTime / computeTime);

// Example: 4 cores, tasks spend 80% waiting (IO), 20% computing
// ioPoolSize = 4 * (1 + 80/20) = 4 * 5 = 20 threads
```

### CompletableFuture: Modern Async Programming

CompletableFuture (Java 8+) provides a powerful way to compose asynchronous operations.

```java
public class CompletableFutureExplained {
    
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    
    /**
     * Basic async operations
     */
    public void basicOperations() throws Exception {
        // Run async task with no return value
        CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
            System.out.println("Running async on: " + Thread.currentThread().getName());
        });
        
        // Run async task with return value
        CompletableFuture<String> supplyAsync = CompletableFuture.supplyAsync(() -> {
            return "Result from async task";
        });
        
        // Use custom executor
        CompletableFuture<String> withExecutor = CompletableFuture.supplyAsync(() -> {
            return "Result";
        }, executor);
        
        // Block and get result
        String result = supplyAsync.get();
    }
    
    /**
     * Chaining operations (the power of CompletableFuture)
     */
    public void chainingOperations() {
        CompletableFuture.supplyAsync(() -> "Hello")
            
            // Transform result (synchronous)
            .thenApply(s -> s + " World")
            
            // Transform result (asynchronous)
            .thenApplyAsync(s -> s.toUpperCase())
            
            // Consume result (synchronous)
            .thenAccept(s -> System.out.println("Result: " + s))
            
            // Run action after completion
            .thenRun(() -> System.out.println("All done!"));
    }
    
    /**
     * Combining multiple futures
     */
    public void combiningFutures() {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Result 1";
        });
        
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            sleep(2000);
            return "Result 2";
        });
        
        // Combine two futures
        CompletableFuture<String> combined = future1.thenCombine(future2,
            (r1, r2) -> r1 + " + " + r2);
        
        // Wait for first to complete
        CompletableFuture<Object> first = CompletableFuture.anyOf(future1, future2);
        
        // Wait for all to complete
        CompletableFuture<Void> all = CompletableFuture.allOf(future1, future2);
    }
    
    /**
     * Error handling
     */
    public void errorHandling() {
        CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("Something went wrong!");
            }
            return "Success";
        })
        
        // Handle exception, provide fallback
        .exceptionally(ex -> {
            System.out.println("Error: " + ex.getMessage());
            return "Default Value";
        })
        
        // Or handle both success and failure
        .handle((result, ex) -> {
            if (ex != null) {
                return "Error occurred: " + ex.getMessage();
            }
            return "Success: " + result;
        })
        
        .thenAccept(System.out::println);
    }
    
    /**
     * Real-world example: Parallel API calls
     */
    public CompletableFuture<UserProfile> getUserProfile(long userId) {
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() ->
            fetchUser(userId));
        
        CompletableFuture<List<Order>> ordersFuture = CompletableFuture.supplyAsync(() ->
            fetchOrders(userId));
        
        CompletableFuture<Preferences> prefsFuture = CompletableFuture.supplyAsync(() ->
            fetchPreferences(userId));
        
        // Combine all results
        return userFuture.thenCombine(ordersFuture, (user, orders) ->
            new UserWithOrders(user, orders)
        ).thenCombine(prefsFuture, (userWithOrders, prefs) ->
            new UserProfile(userWithOrders.user, userWithOrders.orders, prefs)
        );
    }
    
    // Helper
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
    
    // Placeholder methods
    private User fetchUser(long id) { return null; }
    private List<Order> fetchOrders(long id) { return null; }
    private Preferences fetchPreferences(long id) { return null; }
    
    // Placeholder classes
    static class User {}
    static class Order {}
    static class Preferences {}
    static class UserWithOrders { User user; List<Order> orders; 
        UserWithOrders(User u, List<Order> o) { user = u; orders = o; }}
    static class UserProfile { 
        UserProfile(User u, List<Order> o, Preferences p) {} }
}
```

### Virtual Threads (Java 21): Scalable Concurrency

Virtual threads are lightweight threads managed by the JVM, not the OS. You can create millions of them.

```java
public class VirtualThreadsExplained {
    
    /**
     * Why Virtual Threads?
     * 
     * PLATFORM THREADS (traditional):
     * - One-to-one mapping with OS threads
     * - Expensive to create (1MB+ stack per thread)
     * - Limited number (thousands, not millions)
     * - Blocking = wasting an expensive resource
     * 
     * VIRTUAL THREADS:
     * - Many-to-few mapping (millions of virtual → dozens of platform)
     * - Cheap to create (few KB)
     * - Can have millions
     * - Blocking = JVM just schedules another virtual thread on the carrier
     */
    
    public static void main(String[] args) throws Exception {
        
        // Create a single virtual thread
        Thread vThread = Thread.ofVirtual()
            .name("my-virtual-thread")
            .start(() -> {
                System.out.println("Running on: " + Thread.currentThread());
                System.out.println("Is virtual: " + Thread.currentThread().isVirtual());
            });
        
        vThread.join();
        
        // Create many virtual threads (this is the power!)
        // Try doing this with platform threads - your system will crash
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 100_000; i++) {
                final int taskNum = i;
                executor.submit(() -> {
                    // Simulate IO-bound work (database query, HTTP call)
                    Thread.sleep(Duration.ofSeconds(1));
                    return "Task " + taskNum + " done";
                });
            }
        }  // Executor auto-shuts down and waits
        
        System.out.println("All 100,000 tasks completed!");
    }
    
    /**
     * WHEN TO USE VIRTUAL THREADS:
     * 
     * ✅ IO-bound workloads:
     *    - HTTP servers
     *    - Database clients
     *    - File I/O
     *    - Any code that spends most time waiting
     * 
     * ✅ High concurrency requirements:
     *    - Handling thousands of concurrent connections
     *    - Parallel API calls
     * 
     * ❌ NOT for CPU-bound workloads:
     *    - Mathematical computations
     *    - Image processing
     *    - Anything that keeps CPU busy
     *    (Virtual threads won't help - you still only have N cores)
     * 
     * ⚠️ CAUTION with synchronized:
     *    - Virtual thread pinned to carrier during synchronized block
     *    - Use ReentrantLock instead for long-held locks
     */
}
```

### Concurrent Collections

Thread-safe collections that don't require external synchronization:

```java
public class ConcurrentCollectionsExplained {
    
    // ═══════════════════════════════════════════════════════════════════════
    // ConcurrentHashMap: Thread-safe, high-performance map
    // ═══════════════════════════════════════════════════════════════════════
    
    ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
    
    public void concurrentMapExample() {
        // Thread-safe operations
        concurrentMap.put("key", 1);
        Integer value = concurrentMap.get("key");
        
        // Atomic compound operations (the real power)
        concurrentMap.putIfAbsent("newKey", 100);
        concurrentMap.computeIfAbsent("count", k -> expensiveComputation());
        concurrentMap.merge("key", 1, Integer::sum);  // Atomic increment
        
        // Note: iterators are weakly consistent (don't throw ConcurrentModificationException)
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CopyOnWriteArrayList: Thread-safe list for read-heavy workloads
    // ═══════════════════════════════════════════════════════════════════════
    
    // Every write creates a new copy of the array
    // Reads are lock-free and fast
    // Writes are expensive (full copy)
    CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
    
    // Good for: Listener lists, configuration that rarely changes
    // Bad for: Frequent writes
    
    // ═══════════════════════════════════════════════════════════════════════
    // BlockingQueue: Thread-safe queue for producer-consumer
    // ═══════════════════════════════════════════════════════════════════════
    
    BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);
    
    public void blockingQueueExample() throws InterruptedException {
        // Blocking operations (wait if can't proceed)
        queue.put("item");      // Blocks if full
        String item = queue.take();  // Blocks if empty
        
        // Timed operations
        boolean added = queue.offer("item", 1, TimeUnit.SECONDS);
        String polled = queue.poll(1, TimeUnit.SECONDS);
        
        // Non-blocking operations
        boolean success = queue.offer("item");  // Returns false if full
        String immediate = queue.poll();        // Returns null if empty
    }
    
    private Integer expensiveComputation() { return 42; }
}
```

---

## Layer 4 — Interview Mastery

### Frequently Asked Interview Questions

---

**Q1: "What is a race condition? How do you prevent it?"**

> **Answer**: "A race condition occurs when multiple threads access shared mutable data, at least one modifies it, and the outcome depends on the unpredictable timing of thread execution.
>
> For example, `count++` is not atomic—it's read, increment, and write. Two threads might both read 0, both increment to 1, and both write 1, losing an update.
>
> **Prevention strategies**:
> 1. **Synchronization**: Use `synchronized` to ensure mutual exclusion
> 2. **Atomic classes**: Use `AtomicInteger.incrementAndGet()` for lock-free atomicity
> 3. **Immutability**: Immutable objects are inherently thread-safe
> 4. **Thread confinement**: Don't share data between threads
> 5. **Concurrent collections**: Use `ConcurrentHashMap` instead of synchronizing `HashMap`
>
> The right choice depends on context—atomic classes are faster for simple counters, synchronized for complex invariants, immutability for data that doesn't need to change."

---

**Q2: "Explain the difference between synchronized and ReentrantLock."**

| Feature | synchronized | ReentrantLock |
|---------|--------------|---------------|
| Syntax | Implicit, scoped | Explicit lock/unlock |
| Lock release | Automatic on exit | Manual (must unlock in finally) |
| Try-lock | Not possible | `tryLock()` returns immediately |
| Timeout | Not possible | `tryLock(timeout)` |
| Interruptible | No | `lockInterruptibly()` |
| Fairness | Unfair (no ordering) | Configurable |
| Condition variables | Single wait set | Multiple `Condition` objects |
| Performance | Similar | Similar (was faster in old JVMs) |

> **When to use which**:
> - Use `synchronized` for simple cases—it's cleaner and auto-releases
> - Use `ReentrantLock` when you need try-lock, timeouts, interruptibility, or multiple conditions

---

**Q3: "What is volatile? Is it enough for thread safety?"**

> **Answer**: "`volatile` guarantees visibility—reads and writes go directly to main memory, so all threads see the latest value. It also prevents instruction reordering around volatile accesses.
>
> However, `volatile` is **NOT sufficient** for compound operations. `volatileCount++` involves three steps that can interleave:
> 1. Read current value
> 2. Increment
> 3. Write new value
>
> If two threads do this simultaneously, they can both read the same initial value and lose an update.
>
> **Use volatile for**:
> - Simple flags (`volatile boolean shouldStop`)
> - Publishing immutable objects
> - Single-writer, multiple-reader scenarios
>
> **Don't use volatile for**:
> - Check-then-act (`if (x == 0) x = 1`)
> - Read-modify-write (`x++`)
> - Multiple variables that must be consistent"

---

**Q4: "How would you prevent deadlock?"**

> **Answer**: "Deadlock occurs when threads are waiting for each other's locks in a cycle. It requires four conditions (Coffman conditions):
>
> 1. **Mutual exclusion**: Resources can't be shared
> 2. **Hold and wait**: Hold one lock while waiting for another
> 3. **No preemption**: Can't forcibly take locks
> 4. **Circular wait**: A→B→A cycle
>
> **Prevention strategies**:
>
> 1. **Lock ordering**: Always acquire locks in a consistent global order
>    ```java
>    // Always lock A before B, everywhere in the code
>    synchronized (lockA) {
>        synchronized (lockB) { }
>    }
>    ```
>
> 2. **Lock timeout**: Use `tryLock` with timeout
>    ```java
>    if (lock.tryLock(1, TimeUnit.SECONDS)) {
>        try { /* work */ } finally { lock.unlock(); }
>    } else {
>        // Back off, retry later
>    }
>    ```
>
> 3. **Deadlock detection**: Monitor for long-held locks, log thread dumps
>
> 4. **Lock-free algorithms**: Use atomic classes and CAS
>
> In my experience, consistent lock ordering is most practical for application code."

---

**Q5: "Explain virtual threads and when you'd use them."**

> **Answer**: "Virtual threads (Java 21) are lightweight threads managed by the JVM, not the OS. While platform threads map 1:1 to OS threads and are expensive (~1MB stack each), virtual threads are cheap (a few KB) and can number in the millions.
>
> The JVM multiplexes many virtual threads onto a small pool of 'carrier' platform threads. When a virtual thread blocks (IO, sleep, etc.), the JVM unmounts it and mounts another one on the carrier—keeping the carrier busy.
>
> **Best for**:
> - **IO-bound workloads**: HTTP servers, database clients, file operations
> - **High concurrency**: Thousands of concurrent connections
> - **Simplifying async code**: Write blocking code, get async performance
>
> **Not good for**:
> - **CPU-bound work**: You still only have N cores; virtual threads don't add CPU power
> - **Long `synchronized` blocks**: Virtual thread gets 'pinned' to carrier
>
> The key insight is that blocking a virtual thread is cheap—it just moves aside for another virtual thread. This enables the simple 'thread-per-request' model at massive scale."

---

**Q6: "What is the difference between notify() and notifyAll()?"**

> **Answer**: "Both are used with `wait()` for thread coordination:
>
> - `notify()`: Wakes up ONE arbitrary waiting thread
> - `notifyAll()`: Wakes up ALL waiting threads (they compete for the lock)
>
> **Why prefer notifyAll()**:
> - With `notify()`, if the wrong thread is awakened (one that can't proceed), you might deadlock
> - `notifyAll()` is safer—all threads wake up, check their conditions, and the right ones proceed
>
> **When notify() might be okay**:
> - All waiting threads are equivalent (any can do the work)
> - There's exactly one condition being waited on
>
> In practice, I almost always use `notifyAll()` for safety, unless there's a performance-critical reason and I'm certain the conditions are safe."

---

**Q7: "What happens if you call run() instead of start()?"**

> **Answer**: "This is a classic mistake that beginners make:
>
> - `start()`: Creates a new thread, puts it in RUNNABLE state, and the JVM calls `run()` in that new thread. Returns immediately.
>
> - `run()`: Just calls the method directly in the CURRENT thread. No new thread is created. It's like any other method call—completely synchronous.
>
> If you call `run()`, you get no concurrency. Your 'multithreaded' code runs sequentially in the main thread."

---

**Q8: "How do you handle InterruptedException?"**

> **Answer**: "InterruptedException signals that someone wants the thread to stop. There are two proper responses:
>
> 1. **Propagate it**: If your method can't handle it, declare `throws InterruptedException` and let the caller decide.
>
> 2. **Handle it and restore interrupt status**:
>    ```java
>    try {
>        Thread.sleep(1000);
>    } catch (InterruptedException e) {
>        // Restore the interrupt flag
>        Thread.currentThread().interrupt();
>        // Clean up and exit
>        return;
>    }
>    ```
>
> **What NOT to do**:
> ```java
> catch (InterruptedException e) {
>     // WRONG: swallowing the interrupt
> }
> ```
>
> Swallowing the interrupt hides the fact that cancellation was requested. Always either propagate or restore the interrupt status."

---

**Q9: "Explain the Java Memory Model (briefly)."**

> **Answer**: "The Java Memory Model (JMM) defines how threads interact through memory, particularly regarding visibility and ordering.
>
> **Key concepts**:
> - **Visibility**: Changes made by one thread may not be immediately visible to others (due to CPU caches)
> - **Ordering**: Compilers and CPUs can reorder instructions for optimization
>
> **JMM guarantees**:
> - **synchronized**: Entering a synchronized block sees all previous writes by threads that exited synchronized blocks on the same lock
> - **volatile**: Reads see the most recent write; establishes happens-before relationship
> - **final fields**: Properly constructed objects with final fields are safely published
>
> The 'happens-before' relationship is the key abstraction: if action A happens-before action B, then A's effects are visible to B. Synchronized blocks, volatile accesses, and thread starts/joins establish happens-before."

---

**Q10: "Design a thread-safe singleton."**

> **Answer**: "There are several approaches:
>
> **1. Eager initialization** (simplest):
> ```java
> public class Singleton {
>     private static final Singleton INSTANCE = new Singleton();
>     private Singleton() {}
>     public static Singleton getInstance() { return INSTANCE; }
> }
> ```
>
> **2. Enum** (best, per Joshua Bloch):
> ```java
> public enum Singleton {
>     INSTANCE;
>     public void doSomething() { }
> }
> ```
>
> **3. Lazy with holder class** (lazy + thread-safe):
> ```java
> public class Singleton {
>     private Singleton() {}
>     
>     private static class Holder {
>         static final Singleton INSTANCE = new Singleton();
>     }
>     
>     public static Singleton getInstance() {
>         return Holder.INSTANCE;
>     }
> }
> ```
>
> **4. Double-checked locking** (if you need lazy + parameters):
> ```java
> public class Singleton {
>     private static volatile Singleton instance;
>     
>     public static Singleton getInstance() {
>         if (instance == null) {
>             synchronized (Singleton.class) {
>                 if (instance == null) {
>                     instance = new Singleton();
>                 }
>             }
>         }
>         return instance;
>     }
> }
> ```
>
> I recommend the enum or holder pattern for most cases. Double-checked locking is error-prone and usually unnecessary."

---

### Common Mistakes by Experience Level

**Beginner Mistakes**:
- ❌ Calling `run()` instead of `start()`
- ❌ Not synchronizing shared mutable state
- ❌ Extending Thread instead of implementing Runnable
- ❌ Using deprecated methods (`stop()`, `suspend()`)
- ❌ Ignoring InterruptedException (swallowing it silently)

**Intermediate Mistakes**:
- ❌ Synchronizing on a reference that changes (`synchronized(list)` then `list = newList`)
- ❌ Not using volatile for visibility of flags
- ❌ Creating too many threads instead of using thread pools
- ❌ Using `if` instead of `while` for wait conditions
- ❌ Not handling thread pool shutdown properly

**Advanced Mistakes**:
- ❌ Over-synchronizing (creating bottlenecks)
- ❌ Using virtual threads for CPU-bound work
- ❌ Long `synchronized` blocks pinning virtual threads
- ❌ Incorrect double-checked locking (missing volatile)
- ❌ Assuming non-atomic operations are atomic

---

## Summary: Key Takeaways

### Mental Models

> 🧠 **Threads as Chefs**: Multiple chefs (threads) sharing a kitchen (heap). They need coordination to avoid collisions at shared resources (stove, ingredients).

> 🧠 **Synchronized as Traffic Light**: Only one direction (thread) can go at a time. Others wait.

> 🧠 **Volatile as Real-Time GPS**: Everyone sees the current position immediately, not a cached old position.

> 🧠 **Virtual Threads as Motorcycles**: Many more can fit on the same roads (carriers). Great for city traffic (IO-bound), doesn't help on empty highway (CPU-bound).

### Quick Reference

| Concept | Purpose | When to Use |
|---------|---------|-------------|
| `synchronized` | Mutual exclusion + visibility | Simple locking, short critical sections |
| `volatile` | Visibility only | Flags, publishing immutable objects |
| `ReentrantLock` | Flexible locking | Try-lock, timeouts, multiple conditions |
| `AtomicInteger` | Lock-free atomic operations | Counters, simple atomic updates |
| `ConcurrentHashMap` | Thread-safe map | Shared caches, concurrent access |
| `ExecutorService` | Thread pool management | Managing worker threads |
| `CompletableFuture` | Async composition | Chaining async operations |
| Virtual Threads | Lightweight concurrency | IO-bound high-concurrency workloads |

### Checkpoint: Test Your Understanding

- [ ] Can you explain why `count++` is not thread-safe?
- [ ] Do you understand the difference between `synchronized` and `volatile`?
- [ ] Can you explain when to use ReentrantLock vs synchronized?
- [ ] Do you know how to prevent deadlock?
- [ ] Can you explain virtual threads and when to use them?
- [ ] Do you understand thread pool sizing for CPU-bound vs IO-bound tasks?

---

[← Back to Index](00-README.md) | [Previous: Exceptions](06-Exceptions.md) | [Next: Streams & FP →](08-Streams-FP.md)
