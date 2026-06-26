# Chapter 9: Garbage Collection & Memory Management

[← Back to Index](00-README.md) | [Previous: Streams & FP](08-Streams-FP.md) | [Next: JVM Internals →](10-JVM-Internals.md)

---

## Why GC Knowledge Matters

> **The Reality**: Understanding GC is crucial for debugging memory issues 
> and performance tuning. Most developers only learn this after a production crisis.

---

## Layer 1 — Beginner Foundation

### How Garbage Collection Works

```
┌────────────────────────────────────────────────────────────────────┐
│                    JVM HEAP MEMORY                                  │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                      YOUNG GENERATION                         │  │
│  │  ┌───────────────────┐ ┌────────────┐ ┌────────────┐         │  │
│  │  │       Eden        │ │ Survivor 0 │ │ Survivor 1 │         │  │
│  │  │  (New objects)    │ │    (S0)    │ │    (S1)    │         │  │
│  │  │                   │ │            │ │            │         │  │
│  │  └───────────────────┘ └────────────┘ └────────────┘         │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              │ Survives multiple GCs                │
│                              ▼                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                      OLD GENERATION                           │  │
│  │                   (Long-lived objects)                        │  │
│  │                                                               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘

GC Process:
1. New objects allocated in Eden
2. When Eden fills, Minor GC runs
3. Live objects move to Survivor space
4. Objects surviving multiple GCs promote to Old Gen
5. When Old Gen fills, Major/Full GC runs (more expensive)
```

### Types of Garbage Collectors

```java
/*
 * GARBAGE COLLECTOR OPTIONS:
 * 
 * 1. Serial GC: -XX:+UseSerialGC
 *    - Single-threaded
 *    - Good for small apps, single CPU
 *    
 * 2. Parallel GC: -XX:+UseParallelGC
 *    - Multi-threaded young gen
 *    - Good for throughput
 *    
 * 3. G1 GC: -XX:+UseG1GC (Default since Java 9)
 *    - Divides heap into regions
 *    - Balance throughput and latency
 *    - Good default choice
 *    
 * 4. ZGC: -XX:+UseZGC
 *    - Sub-millisecond pauses
 *    - Good for large heaps (TB)
 *    
 * 5. Shenandoah: -XX:+UseShenandoahGC
 *    - Low pause times
 *    - Concurrent compaction
 */
```

### Memory Leaks in Java

```java
public class MemoryLeakExamples {
    
    // Leak 1: Static collections growing unbounded
    private static List<Object> cache = new ArrayList<>();
    
    public void addToCache(Object obj) {
        cache.add(obj);  // Never removed - leak!
    }
    
    // Leak 2: Listeners not unregistered
    public void registerListener() {
        EventBus.register(this);  // If never unregistered, this object leaks
    }
    
    // Leak 3: Inner class holding outer reference
    public class InnerClass {
        // Implicitly holds reference to outer instance
        // Even if outer is no longer needed
    }
    
    // Leak 4: ThreadLocal not cleaned up
    private static ThreadLocal<byte[]> threadLocal = new ThreadLocal<>();
    
    public void threadLocalLeak() {
        threadLocal.set(new byte[1024 * 1024]);  // 1MB
        // If thread is pooled and reused, this never gets GC'd
    }
    
    // Solution: Always clean up
    public void cleanup() {
        cache.clear();
        EventBus.unregister(this);
        threadLocal.remove();  // IMPORTANT!
    }
}
```

---

## Layer 2 — Working Developer Level

### JVM Memory Options

```bash
# Heap size
-Xms512m          # Initial heap size
-Xmx2g            # Maximum heap size

# Generation sizing
-XX:NewRatio=2              # Old/Young ratio (default 2)
-XX:SurvivorRatio=8         # Eden/Survivor ratio
-XX:MaxTenuringThreshold=15 # Age to promote to Old Gen

# Metaspace (class metadata)
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m

# GC selection
-XX:+UseG1GC      # Use G1 (default)
-XX:+UseZGC       # Use ZGC

# GC logging
-Xlog:gc*:file=gc.log    # Log GC events (Java 9+)

# Example production settings
java -Xms4g -Xmx4g -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Xlog:gc*:file=gc.log \
     -jar myapp.jar
```

### Monitoring and Profiling

```java
public class MemoryMonitoring {
    public static void main(String[] args) {
        // Runtime memory info
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();       // -Xmx
        long totalMemory = runtime.totalMemory();   // Current heap size
        long freeMemory = runtime.freeMemory();     // Free in current heap
        long usedMemory = totalMemory - freeMemory;
        
        System.out.printf("Max: %d MB%n", maxMemory / 1024 / 1024);
        System.out.printf("Used: %d MB%n", usedMemory / 1024 / 1024);
        
        // Memory MXBeans for detailed info
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        System.out.printf("Heap Init: %d%n", heapUsage.getInit());
        System.out.printf("Heap Used: %d%n", heapUsage.getUsed());
        System.out.printf("Heap Committed: %d%n", heapUsage.getCommitted());
        System.out.printf("Heap Max: %d%n", heapUsage.getMax());
        
        // GC info
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.printf("GC %s: count=%d, time=%d ms%n",
                gcBean.getName(), 
                gcBean.getCollectionCount(),
                gcBean.getCollectionTime());
        }
    }
}

/*
 * TOOLS FOR MEMORY ANALYSIS:
 * 
 * 1. jconsole / jvisualvm: Visual monitoring
 * 2. jmap: Heap dumps
 *    jmap -dump:format=b,file=heap.hprof <pid>
 * 3. jstat: GC statistics
 *    jstat -gc <pid> 1000  # Every 1 second
 * 4. Eclipse MAT: Analyze heap dumps
 * 5. async-profiler: Low-overhead profiling
 */
```

---

## Layer 4 — Interview Mastery

**Q: "How would you troubleshoot an OutOfMemoryError?"**

> "First, I'd identify the type:
>
> - **Java heap space**: Objects filling heap. Increase -Xmx or find leak.
> - **Metaspace**: Classes filling metaspace. Check for classloader leaks.
> - **GC overhead limit**: Too much time in GC. Memory leak or need more heap.
>
> Investigation steps:
> 1. Enable heap dump on OOM: `-XX:+HeapDumpOnOutOfMemoryError`
> 2. Analyze dump with Eclipse MAT or jhat
> 3. Look for large object trees and dominator analysis
> 4. Check for common leaks: caches, listeners, ThreadLocals
> 5. Review code for objects held longer than needed"

---

[← Back to Index](00-README.md) | [Previous: Streams & FP](08-Streams-FP.md) | [Next: JVM Internals →](10-JVM-Internals.md)
