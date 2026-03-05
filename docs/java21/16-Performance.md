# Chapter 16: Performance Engineering

[← Back to Index](00-README.md) | [Previous: Modules](15-Modules.md) | [Next: Testing →](17-Testing.md)

---

## Why This Chapter Matters

> **The Reality**: Performance engineering separates good developers from great engineers.
> In production systems, slow code costs money, frustrates users, and can bring down entire services.
> Understanding performance isn't just about making code faster—it's about making informed decisions,
> avoiding premature optimization, and knowing when to optimize vs when to scale.

Performance engineering is critical because:
- **User Experience**: Milliseconds matter. Amazon found that 100ms of latency costs 1% in sales.
- **Resource Costs**: Inefficient code wastes CPU, memory, and infrastructure dollars.
- **Scalability**: Poor performance limits your ability to handle growth.
- **Debugging**: Performance issues are often symptoms of deeper architectural problems.

This chapter teaches you to think like a performance engineer: measure first, optimize second, and always understand the trade-offs.

---

## Layer 1 — Beginner Foundation

### What is Performance?

Performance has three dimensions:

```
┌─────────────────────────────────────────────────────────────┐
│                    PERFORMANCE DIMENSIONS                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. LATENCY (Response Time)                                 │
│     How long does ONE operation take?                        │
│     Example: API call takes 50ms                            │
│                                                              │
│  2. THROUGHPUT (Operations per Second)                      │
│     How many operations can you do per unit time?           │
│     Example: Process 10,000 requests/second                  │
│                                                              │
│  3. RESOURCE USAGE                                          │
│     CPU, Memory, Disk I/O, Network bandwidth                │
│     Example: Uses 2GB RAM, 50% CPU                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Key Insight**: These are often trade-offs:
- Lower latency might reduce throughput
- Higher throughput might increase memory usage
- Optimizing one dimension can hurt another

### Big O Notation Basics

Big O describes how algorithm performance scales with input size:

```java
// O(1) - Constant time - Always same speed
public int getFirst(List<Integer> list) {
    return list.get(0);  // Direct access
}

// O(n) - Linear time - Scales with input size
public int sum(List<Integer> list) {
    int total = 0;
    for (int num : list) {  // Must visit each element
        total += num;
    }
    return total;
}

// O(n²) - Quadratic time - Gets slow quickly
public void printPairs(List<Integer> list) {
    for (int i = 0; i < list.size(); i++) {
        for (int j = 0; j < list.size(); j++) {  // Nested loops
            System.out.println(list.get(i) + ", " + list.get(j));
        }
    }
}

// O(log n) - Logarithmic - Very efficient
// Binary search: Each step eliminates half the data
public int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] == target) return mid;
        if (arr[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
```

**Common Complexities** (fastest to slowest):
- O(1) - Constant
- O(log n) - Logarithmic
- O(n) - Linear
- O(n log n) - Linearithmic
- O(n²) - Quadratic
- O(2ⁿ) - Exponential (avoid!)

### Common Performance Mistakes Beginners Make

```java
public class CommonMistakes {
    
    // ❌ Mistake 1: String concatenation in loops
    public String buildStringBad(List<String> items) {
        String result = "";
        for (String item : items) {
            result += item;  // Creates new String object each time!
        }
        return result;
    }
    
    // ✅ Correct: Use StringBuilder
    public String buildStringGood(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append(item);
        }
        return sb.toString();
    }
    
    // ❌ Mistake 2: Using wrong collection for the job
    public boolean containsBad(List<String> list, String target) {
        return list.contains(target);  // O(n) for ArrayList!
    }
    
    // ✅ Correct: Use HashSet for lookups
    public boolean containsGood(Set<String> set, String target) {
        return set.contains(target);  // O(1) average case
    }
    
    // ❌ Mistake 3: Not caching expensive computations
    public int fibonacciBad(int n) {
        if (n <= 1) return n;
        return fibonacciBad(n - 1) + fibonacciBad(n - 2);  // Recalculates!
    }
    
    // ✅ Correct: Memoization
    private Map<Integer, Integer> cache = new HashMap<>();
    public int fibonacciGood(int n) {
        if (n <= 1) return n;
        if (cache.containsKey(n)) return cache.get(n);
        int result = fibonacciGood(n - 1) + fibonacciGood(n - 2);
        cache.put(n, result);
        return result;
    }
    
    // ❌ Mistake 4: Premature optimization
    public void prematureOptimization() {
        // Spending hours optimizing code that runs once per day
        // Instead, optimize code that runs millions of times
    }
}
```

### When to Optimize (Premature Optimization)

> **Donald Knuth**: "Premature optimization is the root of all evil."

**The Rule**: Measure first, optimize second.

```java
// ❌ Bad: Optimizing without data
public void optimizeWithoutMeasuring() {
    // Spending days optimizing code that's not a bottleneck
    // This is wasted effort!
}

// ✅ Good: Profile first, then optimize
public void optimizeCorrectly() {
    // Step 1: Run your application normally
    // Step 2: Use profiling tools to find bottlenecks
    // Step 3: Optimize ONLY the hot paths (code that runs frequently)
    // Step 4: Measure again to verify improvement
}
```

**Optimization Checklist**:
1. ✅ Is this code a bottleneck? (Measure with profiler)
2. ✅ Is this code called frequently? (Hot path)
3. ✅ Will optimization improve user experience?
4. ✅ Is the optimization maintainable?

If any answer is "no", don't optimize yet.

---

## Layer 2 — Working Developer Level

### Profiling Basics

Profiling tells you WHERE your code spends time and memory.

#### CPU Profiling

Shows which methods consume the most CPU time:

```java
public class CpuProfilingExample {
    
    public void processOrders(List<Order> orders) {
        // This method might be slow - profile to find out why
        for (Order order : orders) {
            validateOrder(order);      // Is this slow?
            calculateTotal(order);    // Or this?
            sendConfirmation(order);  // Or this?
        }
    }
    
    private void validateOrder(Order order) {
        // Simulated work
        try { Thread.sleep(10); } catch (InterruptedException e) {}
    }
    
    private void calculateTotal(Order order) {
        // Simulated work
        try { Thread.sleep(5); } catch (InterruptedException e) {}
    }
    
    private void sendConfirmation(Order order) {
        // Simulated work
        try { Thread.sleep(20); } catch (InterruptedException e) {}
    }
}
```

**Profiling reveals**: `sendConfirmation` takes 40% of time → optimize this first!

#### Memory Profiling

Shows which objects consume memory:

```java
public class MemoryProfilingExample {
    
    // Memory profiler shows: This class creates 1 million String objects
    public void processData(List<String> data) {
        List<String> processed = new ArrayList<>();
        for (String item : data) {
            // Each iteration creates new String objects
            String upper = item.toUpperCase();
            String trimmed = upper.trim();
            processed.add(trimmed);
        }
        // All these objects stay in memory until GC runs
    }
    
    // Better: Process and discard immediately
    public void processDataBetter(List<String> data) {
        for (String item : data) {
            String processed = item.toUpperCase().trim();
            // Use immediately, let GC collect
            doSomething(processed);
        }
    }
}
```

### JVM Monitoring Tools

#### 1. JVisualVM (Built-in)

```bash
# Start with JMX enabled
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -jar myapp.jar

# Then connect with JVisualVM
jvisualvm
```

**What you can see**:
- CPU usage per thread
- Memory heap usage
- Thread dumps
- GC activity
- Method profiling

#### 2. JConsole (Built-in)

```bash
jconsole
```

**Features**:
- Real-time memory graphs
- Thread monitoring
- GC statistics
- MBean browser

#### 3. Command-Line Tools

```bash
# View running Java processes
jps

# Heap dump
jmap -dump:format=b,file=heap.hprof <pid>

# Thread dump
jstack <pid>

# GC statistics
jstat -gc <pid> 1000  # Every 1 second

# JVM flags
jinfo -flags <pid>
```

### String Performance

```java
public class StringPerformance {
    
    // ❌ SLOW: String concatenation creates new objects
    public String concatenateSlow(List<String> parts) {
        String result = "";
        for (String part : parts) {
            result += part;  // Each += creates new String object
        }
        return result;
        // Time complexity: O(n²) where n is total characters
    }
    
    // ✅ FAST: StringBuilder for multiple concatenations
    public String concatenateFast(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part);  // Modifies internal buffer
        }
        return sb.toString();
        // Time complexity: O(n)
    }
    
    // ✅ EVEN FASTER: Pre-size StringBuilder if you know size
    public String concatenateOptimal(List<String> parts) {
        int totalLength = parts.stream()
            .mapToInt(String::length)
            .sum();
        
        StringBuilder sb = new StringBuilder(totalLength);
        for (String part : parts) {
            sb.append(part);
        }
        return sb.toString();
        // Avoids internal array resizing
    }
    
    // ✅ For single concatenation, + is fine (compiler optimizes)
    public String singleConcatenation(String a, String b) {
        return a + b;  // Compiler uses StringBuilder internally
    }
}
```

**Rule of Thumb**:
- Single concatenation: Use `+` (readable, compiler optimizes)
- Multiple concatenations: Use `StringBuilder`
- In loops: Always use `StringBuilder`

### Collection Performance

Choosing the right collection is critical:

```java
public class CollectionPerformance {
    
    // ArrayList vs LinkedList
    public void listComparison() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        
        // Adding at end: Both O(1)
        arrayList.add(1);
        linkedList.add(1);
        
        // Adding at beginning: ArrayList O(n), LinkedList O(1)
        arrayList.add(0, 1);  // Must shift all elements
        linkedList.add(0, 1); // Just update pointers
        
        // Random access: ArrayList O(1), LinkedList O(n)
        arrayList.get(1000);  // Direct array access
        linkedList.get(1000); // Must traverse list
        
        // Search: Both O(n)
        arrayList.contains(5);
        linkedList.contains(5);
    }
    
    // HashSet vs TreeSet
    public void setComparison() {
        Set<String> hashSet = new HashSet<>();
        Set<String> treeSet = new TreeSet<>();
        
        // Insert: HashSet O(1), TreeSet O(log n)
        hashSet.add("item");
        treeSet.add("item");
        
        // Contains: HashSet O(1), TreeSet O(log n)
        hashSet.contains("item");
        treeSet.contains("item");
        
        // Iteration order: HashSet unordered, TreeSet sorted
    }
    
    // HashMap vs TreeMap
    public void mapComparison() {
        Map<String, Integer> hashMap = new HashMap<>();
        Map<String, Integer> treeMap = new TreeMap<>();
        
        // Put: HashMap O(1), TreeMap O(log n)
        hashMap.put("key", 1);
        treeMap.put("key", 1);
        
        // Get: HashMap O(1), TreeMap O(log n)
        hashMap.get("key");
        treeMap.get("key");
        
        // Use HashMap unless you need sorted order
    }
}
```

**Collection Selection Guide**:

| Operation | ArrayList | LinkedList | HashSet | TreeSet | HashMap | TreeMap |
|-----------|-----------|------------|---------|---------|---------|---------|
| Add (end) | O(1) | O(1) | O(1) | O(log n) | O(1) | O(log n) |
| Add (beginning) | O(n) | O(1) | - | - | - | - |
| Get by index | O(1) | O(n) | - | - | - | - |
| Contains | O(n) | O(n) | O(1) | O(log n) | O(1) | O(log n) |
| Remove | O(n) | O(1) | O(1) | O(log n) | O(1) | O(log n) |

### Lazy Evaluation Patterns

Lazy evaluation defers computation until needed:

```java
public class LazyEvaluation {
    
    // ❌ Eager: Computes immediately
    public class EagerComputation {
        private final List<String> expensiveResult;
        
        public EagerComputation() {
            // Computed even if never used!
            this.expensiveResult = computeExpensiveOperation();
        }
        
        private List<String> computeExpensiveOperation() {
            // Expensive database query or calculation
            return List.of("result1", "result2");
        }
    }
    
    // ✅ Lazy: Computes only when needed
    public class LazyComputation {
        private List<String> expensiveResult;
        
        public List<String> getResult() {
            if (expensiveResult == null) {
                expensiveResult = computeExpensiveOperation();
            }
            return expensiveResult;
        }
        
        private List<String> computeExpensiveOperation() {
            // Only called if getResult() is called
            return List.of("result1", "result2");
        }
    }
    
    // ✅ Thread-safe lazy initialization
    public class ThreadSafeLazy {
        private volatile List<String> expensiveResult;
        
        public List<String> getResult() {
            if (expensiveResult == null) {
                synchronized (this) {
                    if (expensiveResult == null) {
                        expensiveResult = computeExpensiveOperation();
                    }
                }
            }
            return expensiveResult;
        }
        
        private List<String> computeExpensiveOperation() {
            return List.of("result1", "result2");
        }
    }
    
    // ✅ Using Supplier for lazy evaluation
    public class SupplierLazy {
        private Supplier<List<String>> resultSupplier = () -> {
            System.out.println("Computing expensive operation");
            return computeExpensiveOperation();
        };
        
        public List<String> getResult() {
            return resultSupplier.get();  // Computes on first call
        }
        
        private List<String> computeExpensiveOperation() {
            return List.of("result1", "result2");
        }
    }
}
```

### Caching Strategies

Caching stores frequently accessed data in fast storage:

```java
public class CachingStrategies {
    
    // Simple in-memory cache
    public class SimpleCache<K, V> {
        private final Map<K, V> cache = new HashMap<>();
        private final int maxSize;
        
        public SimpleCache(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public V get(K key) {
            return cache.get(key);
        }
        
        public void put(K key, V value) {
            if (cache.size() >= maxSize) {
                // Evict oldest (simple strategy)
                K firstKey = cache.keySet().iterator().next();
                cache.remove(firstKey);
            }
            cache.put(key, value);
        }
    }
    
    // LRU Cache (Least Recently Used)
    public class LRUCache<K, V> {
        private final LinkedHashMap<K, V> cache;
        private final int capacity;
        
        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > capacity;
                }
            };
        }
        
        public V get(K key) {
            return cache.get(key);  // Moves to end (most recent)
        }
        
        public void put(K key, V value) {
            cache.put(key, value);  // Moves to end if exists
        }
    }
    
    // Time-based expiration cache
    public class ExpiringCache<K, V> {
        private static class CacheEntry<V> {
            final V value;
            final long expireTime;
            
            CacheEntry(V value, long ttlMillis) {
                this.value = value;
                this.expireTime = System.currentTimeMillis() + ttlMillis;
            }
            
            boolean isExpired() {
                return System.currentTimeMillis() > expireTime;
            }
        }
        
        private final Map<K, CacheEntry<V>> cache = new HashMap<>();
        
        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.value;
        }
        
        public void put(K key, V value, long ttlMillis) {
            cache.put(key, new CacheEntry<>(value, ttlMillis));
        }
    }
}
```

**When to Cache**:
- ✅ Expensive computations (database queries, API calls)
- ✅ Frequently accessed data
- ✅ Data that changes infrequently
- ❌ Don't cache: Data that changes frequently, very large datasets

### Database Query Optimization Basics

```java
public class DatabaseOptimization {
    
    // ❌ Bad: N+1 query problem
    public List<Order> getOrdersBad() {
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            // Separate query for each order!
            Customer customer = customerRepository.findById(order.getCustomerId());
            order.setCustomer(customer);
        }
        return orders;
        // If 100 orders, makes 101 queries (1 + 100)
    }
    
    // ✅ Good: Eager loading with JOIN
    @Query("SELECT o FROM Order o JOIN FETCH o.customer")
    public List<Order> getOrdersGood() {
        // Single query with JOIN
        return orderRepository.findAll();
        // Makes only 1 query
    }
    
    // ✅ Good: Batch loading
    public List<Order> getOrdersBatch() {
        List<Order> orders = orderRepository.findAll();
        Set<Long> customerIds = orders.stream()
            .map(Order::getCustomerId)
            .collect(Collectors.toSet());
        
        // Single query for all customers
        Map<Long, Customer> customers = customerRepository
            .findByIdIn(customerIds)
            .stream()
            .collect(Collectors.toMap(Customer::getId, c -> c));
        
        orders.forEach(order -> 
            order.setCustomer(customers.get(order.getCustomerId()))
        );
        return orders;
    }
    
    // ✅ Use indexes
    @Entity
    public class Order {
        @Index(name = "idx_customer_id", columnList = "customerId")
        private Long customerId;  // Indexed for fast lookups
    }
    
    // ✅ Limit result sets
    public List<Order> getRecentOrders() {
        return orderRepository.findAll(
            PageRequest.of(0, 100, Sort.by("createdDate").descending())
        );
        // Only fetch what you need
    }
}
```

**Database Optimization Rules**:
1. Use indexes on frequently queried columns
2. Avoid N+1 queries (use JOINs or batch loading)
3. Limit result sets (pagination)
4. Use prepared statements (prevents SQL injection + caching)
5. Avoid SELECT * (fetch only needed columns)

---

## Layer 3 — Advanced Engineering Depth

### JMH (Java Microbenchmark Harness)

JMH is the gold standard for Java benchmarking. Regular timing is unreliable due to JIT compilation, warmup, and GC.

#### Why Regular Timing Fails

```java
// ❌ This is WRONG - don't do this!
public void badBenchmark() {
    long start = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        doSomething();
    }
    long end = System.nanoTime();
    System.out.println("Time: " + (end - start) / 1_000_000 + " ms");
    // Problems:
    // - JIT hasn't warmed up
    // - GC might run during test
    // - No statistical analysis
    // - Results are unreliable
}
```

#### JMH Setup

**pom.xml**:
```xml
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-core</artifactId>
    <version>1.37</version>
</dependency>
<dependency>
    <groupId>org.openjdk.jmh</groupId>
    <artifactId>jmh-generator-annprocess</artifactId>
    <version>1.37</version>
    <scope>provided</scope>
</dependency>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>org.openjdk.jmh.Main</mainClass>
                    </transformer>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### Writing Correct Benchmarks

```java
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class CollectionBenchmark {
    
    private List<Integer> arrayList;
    private List<Integer> linkedList;
    private int[] array;
    
    @Setup
    public void setup() {
        arrayList = new ArrayList<>();
        linkedList = new LinkedList<>();
        array = new int[1000];
        
        // Pre-populate
        for (int i = 0; i < 1000; i++) {
            arrayList.add(i);
            linkedList.add(i);
            array[i] = i;
        }
    }
    
    @Benchmark
    public int arrayListGet() {
        return arrayList.get(500);
    }
    
    @Benchmark
    public int linkedListGet() {
        return linkedList.get(500);
    }
    
    @Benchmark
    public int arrayGet() {
        return array[500];
    }
    
    @Benchmark
    public void arrayListAdd() {
        arrayList.add(0, 999);
    }
    
    @Benchmark
    public void linkedListAdd() {
        linkedList.add(0, 999);
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(CollectionBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }
}
```

**JMH Annotations Explained**:

```java
@BenchmarkMode(Mode.AverageTime)  // What to measure
// Options: Throughput, AverageTime, SampleTime, SingleShotTime, All

@OutputTimeUnit(TimeUnit.NANOSECONDS)  // Output unit

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
// Warmup: Let JIT compile and optimize before measuring

@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
// Measurement: Actual benchmark runs

@Fork(1)  // Number of JVM forks (isolates JIT effects)

@State(Scope.Benchmark)  // State scope
// Benchmark: Shared across all threads
// Thread: Each thread has own instance
// Group: Shared within thread group
```

#### Advanced JMH Patterns

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class AdvancedBenchmark {
    
    private String data;
    
    @Param({"10", "100", "1000"})
    private int size;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        // Runs once per JVM fork
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Runs before each measurement iteration
        data = "x".repeat(size);
    }
    
    @Benchmark
    public String stringConcatenation() {
        String result = "";
        for (int i = 0; i < size; i++) {
            result += "a";
        }
        return result;
    }
    
    @Benchmark
    public String stringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append("a");
        }
        return sb.toString();
    }
    
    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int dontInline() {
        // Prevents JIT from inlining this method
        return compute();
    }
    
    private int compute() {
        return 42;
    }
}
```

### JIT Compilation Effects on Benchmarks

The JIT compiler optimizes code at runtime, which affects benchmarks:

```java
@State(Scope.Benchmark)
public class JITEffects {
    
    // JIT will inline this simple method
    @Benchmark
    public int simpleMethod() {
        return add(1, 2);  // JIT inlines add() method
    }
    
    private int add(int a, int b) {
        return a + b;
    }
    
    // JIT might optimize away dead code
    @Benchmark
    public int deadCodeElimination() {
        int result = compute();
        // If result is never used, JIT might remove compute()
        return 42;
    }
    
    private int compute() {
        // Expensive computation
        return (int) Math.sqrt(1000000);
    }
    
    // Solution: Use result
    @Benchmark
    public int preventDeadCodeElimination() {
        int result = compute();
        // Use result to prevent optimization
        return result + 42;
    }
    
    // JIT might optimize loops
    @Benchmark
    public int loopOptimization() {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i;
        }
        // JIT might optimize to: sum = 499500
        return sum;
    }
    
    // Solution: Use volatile or blackhole
    @Benchmark
    public int preventLoopOptimization(Blackhole bh) {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i;
            bh.consume(sum);  // Prevents optimization
        }
        return sum;
    }
}
```

**Key Points**:
- Always warm up before measuring (let JIT compile)
- Use `@CompilerControl` to control inlining
- Use `Blackhole` to consume values and prevent dead code elimination
- Run multiple forks to account for JIT variance

### Memory Profiling Deep Dive

#### Object Allocation Analysis

```java
public class AllocationAnalysis {
    
    // Track object allocations
    public void analyzeAllocations() {
        // Use JProfiler, YourKit, or Java Flight Recorder
        // to see:
        // - Which classes allocate most objects
        // - Allocation rate (objects/second)
        // - Object lifetime
        // - Memory leaks
    }
    
    // Common allocation hotspots
    public class AllocationHotspots {
        
        // ❌ Allocates many temporary objects
        public String processBad(List<String> items) {
            String result = "";
            for (String item : items) {
                // Each iteration creates:
                // - New String object (result + item)
                // - Temporary StringBuilder
                result = result + item.toUpperCase();
            }
            return result;
        }
        
        // ✅ Reuses objects
        public String processGood(List<String> items) {
            StringBuilder sb = new StringBuilder();
            for (String item : items) {
                // Reuses StringBuilder, fewer allocations
                sb.append(item.toUpperCase());
            }
            return sb.toString();
        }
        
        // ❌ Boxing creates objects
        public int sumBad(List<Integer> numbers) {
            int sum = 0;
            for (Integer num : numbers) {
                // Unboxing creates temporary objects
                sum += num;
            }
            return sum;
        }
        
        // ✅ Use primitives
        public int sumGood(int[] numbers) {
            int sum = 0;
            for (int num : numbers) {
                // No boxing/unboxing
                sum += num;
            }
            return sum;
        }
    }
}
```

#### Heap Dump Analysis

```bash
# Generate heap dump
jmap -dump:format=b,file=heap.hprof <pid>

# Or trigger programmatically
jcmd <pid> GC.run_finalization
jcmd <pid> VM.dump_heap heap.hprof
```

**Analyze with**:
- Eclipse MAT (Memory Analyzer Tool)
- JVisualVM
- YourKit

**What to Look For**:
- Objects with high retained size
- Duplicate strings (use `-XX:+UseStringDeduplication`)
- Memory leaks (objects that should be GC'd but aren't)
- Large arrays or collections

### GC Tuning for Performance

GC tuning balances throughput vs pause times:

```bash
# G1 GC Tuning (Default in Java 9+)
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \        # Target pause time
     -XX:G1HeapRegionSize=16m \        # Region size
     -XX:InitiatingHeapOccupancyPercent=45 \  # When to start concurrent marking
     -XX:ConcGCThreads=4 \             # Concurrent GC threads
     -Xlog:gc*:file=gc.log:time,uptime,level,tags

# ZGC Tuning (Low latency)
java -Xms8g -Xmx8g \
     -XX:+UseZGC \
     -XX:+UnlockExperimentalVMOptions \
     -Xlog:gc*:file=gc.log

# Parallel GC Tuning (High throughput)
java -Xms4g -Xmx4g \
     -XX:+UseParallelGC \
     -XX:ParallelGCThreads=8 \        # GC threads
     -XX:MaxGCPauseMillis=100
```

**GC Tuning Strategy**:

1. **Measure First**: Use GC logs to understand current behavior
   ```bash
   -Xlog:gc*:file=gc.log:time,uptime,level,tags
   ```

2. **Identify Issues**:
   - Long pause times → Use ZGC or Shenandoah
   - Low throughput → Increase heap or use Parallel GC
   - Frequent GC → Increase heap size
   - Memory leaks → Fix code, not GC

3. **Tune Parameters**:
   - Heap size: `-Xms` and `-Xmx` (set equal to avoid resizing)
   - GC algorithm: Choose based on latency vs throughput needs
   - GC threads: Match CPU cores

4. **Verify**: Measure again after tuning

### Lock Contention Analysis

Lock contention occurs when multiple threads compete for the same lock:

```java
public class LockContention {
    
    // ❌ High contention: All threads compete for one lock
    private final Object lock = new Object();
    private int counter = 0;
    
    public void incrementBad() {
        synchronized (lock) {
            counter++;  // All threads wait here
        }
    }
    
    // ✅ Low contention: Use AtomicInteger (lock-free)
    private final AtomicInteger atomicCounter = new AtomicInteger();
    
    public void incrementGood() {
        atomicCounter.incrementAndGet();  // No lock contention
    }
    
    // ✅ Reduce contention: Striped locks
    public class StripedCounter {
        private final AtomicInteger[] counters;
        private static final int STRIPES = 16;
        
        public StripedCounter() {
            counters = new AtomicInteger[STRIPES];
            for (int i = 0; i < STRIPES; i++) {
                counters[i] = new AtomicInteger();
            }
        }
        
        public void increment(int key) {
            // Distribute keys across stripes
            int stripe = key % STRIPES;
            counters[stripe].incrementAndGet();
        }
    }
    
    // ✅ Use ConcurrentHashMap instead of synchronized Map
    private final Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
    
    public void updateMap(String key) {
        concurrentMap.compute(key, (k, v) -> v == null ? 1 : v + 1);
    }
}
```

**Detecting Lock Contention**:
- Use JProfiler or YourKit to see thread wait times
- Use `jstack` to see threads blocked on locks
- Look for threads in `BLOCKED` state

### False Sharing in Concurrent Code

False sharing occurs when unrelated data shares the same cache line:

```java
public class FalseSharing {
    
    // ❌ False sharing: Both counters share same cache line (64 bytes)
    @Contended  // Java 8+ annotation to prevent false sharing
    public static class Counter {
        volatile long count1 = 0;  // 8 bytes
        volatile long count2 = 0;  // 8 bytes
        // Both likely in same 64-byte cache line
    }
    
    // Thread 1 updates count1 → invalidates cache line
    // Thread 2 updates count2 → must reload cache line
    // Result: Performance degradation
    
    // ✅ Solution 1: Use @Contended (Java 8+)
    @Contended
    public static class CounterFixed {
        volatile long count1 = 0;
        // Padding to separate cache lines
        volatile long count2 = 0;
    }
    
    // ✅ Solution 2: Separate objects
    public static class Counter1 {
        volatile long count = 0;
    }
    
    public static class Counter2 {
        volatile long count = 0;
    }
    
    // ✅ Solution 3: Manual padding (pre-Java 8)
    public static class CounterPadded {
        volatile long count1 = 0;
        long p1, p2, p3, p4, p5, p6, p7;  // Padding
        volatile long count2 = 0;
    }
}
```

**Detecting False Sharing**:
- Use `perf` on Linux: `perf c2c record` and `perf c2c report`
- Use Intel VTune Profiler
- Look for high cache miss rates in concurrent code

### CPU Cache Optimization

Understanding CPU cache hierarchy improves performance:

```
┌─────────────────────────────────────────┐
│         CPU REGISTERS (1 cycle)         │  Fastest, smallest
├─────────────────────────────────────────┤
│      L1 CACHE (2-4 cycles, 32KB)       │
├─────────────────────────────────────────┤
│      L2 CACHE (10 cycles, 256KB)        │
├─────────────────────────────────────────┤
│      L3 CACHE (40 cycles, 8-16MB)       │
├─────────────────────────────────────────┤
│         MAIN MEMORY (100+ cycles)       │  Slowest, largest
└─────────────────────────────────────────┘
```

**Cache-Friendly Code**:

```java
public class CacheOptimization {
    
    // ❌ Cache-unfriendly: Random access pattern
    public int sumMatrixBad(int[][] matrix) {
        int sum = 0;
        // Column-major order: poor cache locality
        for (int col = 0; col < matrix[0].length; col++) {
            for (int row = 0; row < matrix.length; row++) {
                sum += matrix[row][col];  // Jumps around memory
            }
        }
        return sum;
    }
    
    // ✅ Cache-friendly: Sequential access
    public int sumMatrixGood(int[][] matrix) {
        int sum = 0;
        // Row-major order: good cache locality
        for (int row = 0; row < matrix.length; row++) {
            for (int col = 0; col < matrix[row].length; col++) {
                sum += matrix[row][col];  // Sequential memory access
            }
        }
        return sum;
    }
    
    // ✅ Keep related data together (data locality)
    public class CacheFriendlyData {
        // Good: Related fields together
        private int x, y, z;  // Accessed together
        
        // Bad: Unrelated data mixed
        // private int x;
        // private String name;  // Separates related ints
        // private int y;
    }
    
    // ✅ Use arrays instead of linked structures for cache
    public int sumArray(int[] arr) {
        int sum = 0;
        // Array: sequential memory, good cache usage
        for (int value : arr) {
            sum += value;
        }
        return sum;
    }
    
    public int sumLinkedList(LinkedList<Integer> list) {
        int sum = 0;
        // LinkedList: nodes scattered in memory, poor cache usage
        for (int value : list) {
            sum += value;
        }
        return sum;
    }
}
```

### Async Profiler and Flame Graphs

Async Profiler is a low-overhead profiler that produces flame graphs:

**Setup**:
```bash
# Download async-profiler
wget https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz
tar -xzf async-profiler-2.9-linux-x64.tar.gz

# Profile CPU
./profiler.sh -d 30 -f profile.html <pid>

# Profile allocations
./profiler.sh -e alloc -d 30 -f alloc.html <pid>

# Profile lock contention
./profiler.sh -e lock -d 30 -f lock.html <pid>
```

**Flame Graph Interpretation**:
- Width = time spent in method
- Height = call stack depth
- Colors = random (for visual separation)
- Click to zoom into specific methods

**Reading Flame Graphs**:
1. Look for wide boxes (hot methods)
2. Follow the stack upward to find callers
3. Identify methods taking significant time
4. Optimize the hot paths

### Production Profiling Strategies

Profiling production requires care to avoid impacting users:

```java
public class ProductionProfiling {
    
    // ✅ Sampling profiler (low overhead)
    // Use async-profiler in sampling mode
    // -d 60: Profile for 60 seconds
    // -e cpu: CPU sampling
    // Overhead: < 1%
    
    // ✅ Java Flight Recorder (JFR)
    // Built into JDK, low overhead
    public void enableJFR() {
        // Start with JFR
        // java -XX:+FlightRecorder \
        //      -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
        //      -jar app.jar
        
        // Or programmatically
        // try (Recording recording = new Recording()) {
        //     recording.start();
        //     // ... run application
        //     recording.stop();
        //     recording.dump(Paths.get("recording.jfr"));
        // }
    }
    
    // ✅ Continuous profiling
    // Profile continuously with low sampling rate
    // Aggregate results over time
    // Identify trends and regressions
    
    // ✅ On-demand profiling
    // Trigger profiling via JMX or HTTP endpoint
    // Only profile when investigating issues
    
    // ❌ Avoid: High-frequency profiling in production
    // Can impact performance significantly
}
```

**Production Profiling Best Practices**:
1. Use sampling profilers (low overhead)
2. Profile during low-traffic periods first
3. Monitor profiler overhead
4. Use JFR for continuous monitoring
5. Set up alerts for performance regressions

---

## Layer 4 — Interview Mastery

### How Would You Diagnose a Slow Application?

**Step-by-Step Approach**:

```java
public class PerformanceDiagnosis {
    
    /*
     * INTERVIEW ANSWER STRUCTURE:
     * 
     * 1. Gather Information
     * 2. Reproduce the Issue
     * 3. Measure and Profile
     * 4. Identify Bottlenecks
     * 5. Hypothesize Root Cause
     * 6. Test Hypothesis
     * 7. Implement Fix
     * 8. Verify Improvement
     */
    
    public void diagnoseSlowApplication() {
        // Step 1: Gather Information
        // - When did it start?
        // - What changed recently?
        // - Is it always slow or intermittent?
        // - Which operations are slow?
        // - How many users affected?
        
        // Step 2: Check Basic Metrics
        checkBasicMetrics();
        
        // Step 3: Profile the Application
        profileApplication();
        
        // Step 4: Analyze Results
        analyzeResults();
        
        // Step 5: Fix and Verify
        fixAndVerify();
    }
    
    private void checkBasicMetrics() {
        // CPU usage
        // - High CPU: CPU-bound operations, inefficient algorithms
        // - Low CPU: I/O bound, waiting on external services
        
        // Memory usage
        // - High memory: Memory leaks, inefficient data structures
        // - Frequent GC: Too many allocations, small heap
        
        // Thread states
        // - Many BLOCKED threads: Lock contention
        // - Many WAITING threads: I/O waits
        
        // GC logs
        // - Long pause times: GC tuning needed
        // - Frequent GC: Memory pressure
    }
    
    private void profileApplication() {
        // Use profiling tools:
        // - JProfiler / YourKit: Detailed analysis
        // - async-profiler: Low overhead, flame graphs
        // - JFR: Built-in, continuous monitoring
        // - JVisualVM: Quick checks
        
        // Profile:
        // - CPU: Which methods take most time?
        // - Memory: Which objects allocate most?
        // - Lock contention: Which locks cause waits?
    }
    
    private void analyzeResults() {
        // Look for:
        // - Hot methods (wide in flame graph)
        // - Memory leaks (growing object counts)
        // - Lock contention (threads waiting)
        // - I/O waits (database, network)
        // - GC pauses (long pause times)
    }
    
    private void fixAndVerify() {
        // Fix the identified issue
        // Measure again to verify improvement
        // Use A/B testing if possible
    }
}
```

**Interview Answer Template**:

> "I'd start by gathering information: when did the slowness start, what changed recently, and which specific operations are slow. Then I'd check basic metrics—CPU usage, memory, thread states, and GC logs. If CPU is high, it's likely a CPU-bound issue. If CPU is low but response time is high, it's likely I/O bound.
>
> Next, I'd use a profiling tool like async-profiler or JFR to identify hot methods. I'd look at flame graphs to see where time is spent. I'd also check for memory leaks by analyzing heap dumps and GC logs.
>
> Once I identify the bottleneck—whether it's an inefficient algorithm, lock contention, memory leaks, or I/O waits—I'd create a hypothesis and test it. After implementing a fix, I'd measure again to verify the improvement."

### Explain Your Approach to Performance Tuning

**Systematic Approach**:

```java
public class PerformanceTuningApproach {
    
    /*
     * PERFORMANCE TUNING METHODOLOGY:
     * 
     * 1. Establish Baseline
     * 2. Set Performance Goals
     * 3. Profile to Find Bottlenecks
     * 4. Optimize Hot Paths
     * 5. Measure Impact
     * 6. Iterate
     */
    
    public void tuningMethodology() {
        // Step 1: Establish Baseline
        establishBaseline();
        
        // Step 2: Set Goals
        setGoals();
        
        // Step 3: Profile
        profile();
        
        // Step 4: Optimize (80/20 rule)
        optimize();
        
        // Step 5: Measure
        measure();
        
        // Step 6: Iterate or stop
        iterate();
    }
    
    private void establishBaseline() {
        // Measure current performance:
        // - Response times (p50, p95, p99)
        // - Throughput (requests/second)
        // - Resource usage (CPU, memory)
        // - Error rates
        
        // Use production metrics or load testing
    }
    
    private void setGoals() {
        // Set specific, measurable goals:
        // - Reduce p95 latency by 50%
        // - Increase throughput by 2x
        // - Reduce memory usage by 30%
        
        // Goals should be realistic and business-aligned
    }
    
    private void profile() {
        // Use profiling tools to find bottlenecks
        // Focus on hot paths (code that runs frequently)
        // Apply 80/20 rule: 20% of code uses 80% of resources
    }
    
    private void optimize() {
        // Optimize in order of impact:
        // 1. Algorithm changes (biggest impact)
        // 2. Data structure choices
        // 3. Caching
        // 4. GC tuning
        // 5. Micro-optimizations (last resort)
    }
    
    private void measure() {
        // Measure after each optimization
        // Verify improvement meets goals
        // Check for regressions
    }
    
    private void iterate() {
        // If goals not met: profile again, optimize more
        // If goals met: stop (avoid over-optimization)
    }
}
```

**Key Principles**:
1. **Measure First**: Never optimize without data
2. **80/20 Rule**: Focus on hot paths
3. **Set Goals**: Know when to stop
4. **Verify**: Measure after each change
5. **Trade-offs**: Understand what you're sacrificing

### Common Performance Interview Scenarios

#### Scenario 1: Slow API Endpoint

**Problem**: API endpoint takes 2 seconds to respond

**Diagnosis Steps**:
1. Check if it's always slow or intermittent
2. Profile the endpoint (CPU, memory, I/O)
3. Check database queries (N+1 problem?)
4. Check external service calls
5. Check serialization/deserialization

**Common Causes**:
- N+1 database queries
- Missing database indexes
- Synchronous external API calls
- Large response payloads
- Inefficient algorithms

**Solutions**:
- Use JOINs or batch loading
- Add database indexes
- Make external calls async
- Paginate large results
- Optimize algorithms

#### Scenario 2: High Memory Usage

**Problem**: Application uses 8GB RAM and frequently GCs

**Diagnosis Steps**:
1. Generate heap dump
2. Analyze with MAT or JVisualVM
3. Look for memory leaks
4. Check object allocation rates
5. Review GC logs

**Common Causes**:
- Memory leaks (static collections, listeners)
- Too many cached objects
- Large data structures
- Inefficient data structures

**Solutions**:
- Fix memory leaks
- Implement cache eviction
- Use streaming for large datasets
- Choose appropriate data structures

#### Scenario 3: CPU Spikes

**Problem**: CPU usage spikes to 100% periodically

**Diagnosis Steps**:
1. Capture thread dump during spike
2. Profile CPU usage
3. Check for infinite loops
4. Check for expensive computations
5. Check for lock contention

**Common Causes**:
- Inefficient algorithms (O(n²))
- Infinite loops
- Lock contention
- Expensive computations in hot paths

**Solutions**:
- Optimize algorithms
- Fix infinite loops
- Reduce lock contention
- Cache expensive computations

### Memory Leak Detection Strategies

```java
public class MemoryLeakDetection {
    
    /*
     * MEMORY LEAK DETECTION STRATEGIES:
     * 
     * 1. Monitor Heap Usage Over Time
     * 2. Analyze Heap Dumps
     * 3. Use Memory Profilers
     * 4. Review GC Logs
     * 5. Code Review for Common Patterns
     */
    
    public void detectMemoryLeaks() {
        // Strategy 1: Monitor heap usage
        monitorHeapUsage();
        
        // Strategy 2: Heap dump analysis
        analyzeHeapDump();
        
        // Strategy 3: Memory profiler
        useMemoryProfiler();
        
        // Strategy 4: GC logs
        analyzeGCLogs();
        
        // Strategy 5: Code patterns
        reviewCodePatterns();
    }
    
    private void monitorHeapUsage() {
        // Heap should stabilize after warmup
        // If heap keeps growing → potential leak
        
        // Use JVisualVM or monitoring tools
        // Graph heap usage over time
        // Look for upward trend
    }
    
    private void analyzeHeapDump() {
        // Generate heap dump:
        // jmap -dump:format=b,file=heap.hprof <pid>
        
        // Analyze with Eclipse MAT:
        // 1. Look for objects with high retained size
        // 2. Find objects that shouldn't exist
        // 3. Trace references to find root cause
        // 4. Look for duplicate strings/collections
    }
    
    private void useMemoryProfiler() {
        // Use JProfiler or YourKit:
        // - Track object allocation
        // - See object lifetime
        // - Identify objects that never get GC'd
        // - Find allocation hotspots
    }
    
    private void analyzeGCLogs() {
        // Check GC logs:
        // - Heap size before/after GC
        // - If heap doesn't decrease after GC → leak
        // - Frequency of GC (increasing = leak)
        
        // Example log analysis:
        // [GC (Allocation Failure) [PSYoungGen: 1024K->512K(1536K)] 
        // 1024K->512K(5632K), 0.0012345 secs]
        // 
        // If "after GC" size keeps growing → leak
    }
    
    private void reviewCodePatterns() {
        // Common leak patterns:
        checkStaticCollections();
        checkListeners();
        checkThreadLocals();
        checkInnerClasses();
    }
    
    private void checkStaticCollections() {
        // ❌ Leak: Static collection that grows
        private static List<Object> cache = new ArrayList<>();
        
        // ✅ Fix: Use bounded cache with eviction
        private static Cache<String, Object> cache = 
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }
    
    private void checkListeners() {
        // ❌ Leak: Listeners not unregistered
        eventBus.register(this);
        
        // ✅ Fix: Always unregister
        try {
            eventBus.register(this);
            // ... use listener
        } finally {
            eventBus.unregister(this);
        }
    }
    
    private void checkThreadLocals() {
        // ❌ Leak: ThreadLocal in thread pool
        private static ThreadLocal<byte[]> data = new ThreadLocal<>();
        data.set(new byte[1024 * 1024]);
        // If thread is reused, data never cleared
        
        // ✅ Fix: Always remove
        try {
            data.set(new byte[1024 * 1024]);
            // ... use data
        } finally {
            data.remove();  // IMPORTANT!
        }
    }
    
    private void checkInnerClasses() {
        // ❌ Leak: Inner class holds outer reference
        public class Outer {
            private byte[] largeData = new byte[1024 * 1024];
            
            public Runnable createTask() {
                // Inner class implicitly holds reference to Outer
                return new Runnable() {
                    @Override
                    public void run() {
                        // Can access Outer.this.largeData
                    }
                };
            }
        }
        
        // ✅ Fix: Use static inner class or lambda
        public class Outer {
            private byte[] largeData = new byte[1024 * 1024];
            
            public Runnable createTask() {
                // Static inner class doesn't hold outer reference
                return new StaticTask();
            }
            
            private static class StaticTask implements Runnable {
                @Override
                public void run() {
                    // No access to outer instance
                }
            }
        }
    }
}
```

**Memory Leak Detection Checklist**:
1. ✅ Heap usage grows over time (not stable)
2. ✅ GC doesn't reduce heap size
3. ✅ Old generation keeps growing
4. ✅ OutOfMemoryError after running for a while
5. ✅ Objects in heap dump that shouldn't exist

### When to Optimize vs When to Scale

**Optimize When**:
- Single instance performance matters
- Resource costs are high
- Latency is critical
- You have identified bottlenecks
- Optimization will have significant impact

**Scale When**:
- Performance is acceptable per instance
- Adding instances is cheaper than optimization
- You need to handle more load
- Optimization would be too complex/risky
- You need redundancy/availability

**Decision Framework**:

```java
public class OptimizeVsScale {
    
    public void decideStrategy() {
        // Consider:
        // 1. Cost of optimization vs scaling
        // 2. Time to implement
        // 3. Risk of changes
        // 4. Current performance
        // 5. Expected load growth
        
        if (shouldOptimize()) {
            optimize();
        } else if (shouldScale()) {
            scale();
        } else {
            // Do both: optimize critical paths, scale for capacity
            optimizeAndScale();
        }
    }
    
    private boolean shouldOptimize() {
        // Optimize if:
        // - Performance is significantly below requirements
        // - Optimization is low-risk and high-impact
        // - Single instance performance matters
        // - Resource costs are high
        return performanceGap > threshold 
            && optimizationRisk < threshold
            && optimizationImpact > threshold;
    }
    
    private boolean shouldScale() {
        // Scale if:
        // - Per-instance performance is acceptable
        // - Scaling is cheaper/faster than optimization
        // - You need to handle more load
        // - You need redundancy
        return perInstancePerformance >= acceptable
            && scalingCost < optimizationCost
            && needMoreCapacity;
    }
    
    private void optimizeAndScale() {
        // Best approach: Do both
        // - Optimize critical hot paths (biggest impact)
        // - Scale horizontally for capacity
        // - Get best of both worlds
    }
}
```

**Real-World Examples**:

**Optimize**:
- Database query taking 5 seconds → Optimize query, add index
- Algorithm is O(n²) → Change to O(n log n)
- Memory leak causing OOM → Fix leak

**Scale**:
- API handles 100 req/s, need 1000 req/s → Add more instances
- Need high availability → Scale to multiple regions
- Current performance is fine, just need capacity → Scale

**Both**:
- Optimize critical paths (reduce latency)
- Scale horizontally (increase capacity)
- Result: Better performance AND more capacity

---

[← Back to Index](00-README.md) | [Previous: Modules](15-Modules.md) | [Next: Testing →](17-Testing.md)
