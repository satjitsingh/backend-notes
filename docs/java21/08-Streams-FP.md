# Chapter 8: Streams & Functional Programming

[← Back to Index](00-README.md) | [Previous: Concurrency](07-Concurrency.md) | [Next: GC & Memory →](09-GC-Memory.md)

---

# Chapter 8: Streams & Functional Programming

## Why Functional Programming Matters

> **The Shift**: Modern Java embraces functional programming for cleaner, 
> more maintainable code. Streams replace verbose loops with declarative pipelines.

---

## Layer 1 — Beginner Foundation

### Lambda Expressions

```java
/*
 * LAMBDA: Anonymous function (inline implementation)
 * Syntax: (parameters) -> expression  OR  (parameters) -> { statements }
 */

public class LambdaBasics {
    public static void main(String[] args) {
        // Before lambdas: Anonymous inner class
        Runnable oldWay = new Runnable() {
            @Override
            public void run() {
                System.out.println("Old way");
            }
        };
        
        // With lambdas: Concise
        Runnable newWay = () -> System.out.println("New way");
        
        // Lambda variations
        // No parameters
        Runnable r = () -> System.out.println("Hello");
        
        // One parameter (parentheses optional)
        Consumer<String> c = s -> System.out.println(s);
        Consumer<String> c2 = (s) -> System.out.println(s);
        
        // Multiple parameters
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        
        // With type declarations
        BiFunction<Integer, Integer, Integer> multiply = 
            (Integer a, Integer b) -> a * b;
        
        // Block body (for multiple statements)
        BiFunction<Integer, Integer, Integer> complex = (a, b) -> {
            int result = a + b;
            result *= 2;
            return result;  // Explicit return needed in block
        };
    }
}
```

### Functional Interfaces

```java
/*
 * FUNCTIONAL INTERFACE: Interface with exactly one abstract method
 * Lambdas can implement any functional interface
 */

// Custom functional interface
@FunctionalInterface  // Optional but recommended
public interface Calculator {
    int calculate(int a, int b);
    
    // Can have default methods
    default void printResult(int a, int b) {
        System.out.println(calculate(a, b));
    }
    
    // Can have static methods
    static Calculator adder() {
        return (a, b) -> a + b;
    }
}

// Built-in functional interfaces (java.util.function)
public class BuiltInFunctionalInterfaces {
    // Supplier<T>: () -> T
    Supplier<Double> random = () -> Math.random();
    
    // Consumer<T>: T -> void
    Consumer<String> printer = s -> System.out.println(s);
    
    // Function<T, R>: T -> R
    Function<String, Integer> length = s -> s.length();
    
    // Predicate<T>: T -> boolean
    Predicate<String> isEmpty = s -> s.isEmpty();
    
    // BiFunction<T, U, R>: (T, U) -> R
    BiFunction<String, String, String> concat = (a, b) -> a + b;
    
    // BiConsumer<T, U>: (T, U) -> void
    BiConsumer<String, Integer> printPair = (k, v) -> System.out.println(k + "=" + v);
    
    // BiPredicate<T, U>: (T, U) -> boolean
    BiPredicate<String, String> equals = (a, b) -> a.equals(b);
    
    // UnaryOperator<T>: T -> T (same type)
    UnaryOperator<String> toUpper = s -> s.toUpperCase();
    
    // BinaryOperator<T>: (T, T) -> T
    BinaryOperator<Integer> max = (a, b) -> a > b ? a : b;
}
```

### Stream Basics

```java
public class StreamBasics {
    public static void main(String[] args) {
        List<String> names = List.of("Alice", "Bob", "Charlie", "Diana");
        
        // Creating streams
        Stream<String> fromList = names.stream();
        Stream<String> fromArray = Stream.of("A", "B", "C");
        Stream<Integer> fromRange = IntStream.range(1, 10).boxed();
        Stream<String> infinite = Stream.generate(() -> "Hello");
        Stream<Integer> iterate = Stream.iterate(0, n -> n + 2);  // 0, 2, 4, 6...
        
        // Basic pipeline: Source -> Intermediate -> Terminal
        long count = names.stream()          // Source
            .filter(s -> s.length() > 3)     // Intermediate
            .count();                         // Terminal
        
        // Common intermediate operations
        names.stream()
            .filter(s -> s.startsWith("A"))  // Keep matching
            .map(String::toUpperCase)         // Transform
            .sorted()                          // Sort
            .distinct()                        // Remove duplicates
            .limit(5)                          // Take first N
            .skip(2);                          // Skip first N
        
        // Common terminal operations
        names.stream().forEach(System.out::println);      // Iterate
        names.stream().count();                            // Count
        names.stream().collect(Collectors.toList());      // Collect to List
        names.stream().toArray(String[]::new);            // To array
        names.stream().findFirst();                        // Optional<T>
        names.stream().findAny();                          // Optional<T>
        names.stream().anyMatch(s -> s.isEmpty());        // boolean
        names.stream().allMatch(s -> s.length() > 1);     // boolean
        names.stream().noneMatch(s -> s.isEmpty());       // boolean
        names.stream().min(Comparator.naturalOrder());    // Optional<T>
        names.stream().max(Comparator.naturalOrder());    // Optional<T>
    }
}
```

---

## Layer 2 — Working Developer Level

### Collectors

```java
public class CollectorsDemo {
    public static void main(String[] args) {
        List<Person> people = List.of(
            new Person("Alice", 30, "Engineering"),
            new Person("Bob", 25, "Marketing"),
            new Person("Charlie", 35, "Engineering"),
            new Person("Diana", 28, "Marketing")
        );
        
        // Collect to List
        List<String> names = people.stream()
            .map(Person::getName)
            .collect(Collectors.toList());
        
        // Collect to Set
        Set<String> departments = people.stream()
            .map(Person::getDepartment)
            .collect(Collectors.toSet());
        
        // Collect to Map
        Map<String, Integer> nameToAge = people.stream()
            .collect(Collectors.toMap(
                Person::getName,      // Key mapper
                Person::getAge        // Value mapper
            ));
        
        // Handle duplicate keys
        Map<String, Person> byName = people.stream()
            .collect(Collectors.toMap(
                Person::getName,
                Function.identity(),
                (existing, replacement) -> existing  // Merge function
            ));
        
        // Joining strings
        String allNames = people.stream()
            .map(Person::getName)
            .collect(Collectors.joining(", ", "[", "]"));
        // Result: "[Alice, Bob, Charlie, Diana]"
        
        // Grouping
        Map<String, List<Person>> byDept = people.stream()
            .collect(Collectors.groupingBy(Person::getDepartment));
        
        // Grouping with downstream collector
        Map<String, Long> countByDept = people.stream()
            .collect(Collectors.groupingBy(
                Person::getDepartment,
                Collectors.counting()
            ));
        
        Map<String, Double> avgAgeByDept = people.stream()
            .collect(Collectors.groupingBy(
                Person::getDepartment,
                Collectors.averagingInt(Person::getAge)
            ));
        
        // Partitioning (by true/false)
        Map<Boolean, List<Person>> over30 = people.stream()
            .collect(Collectors.partitioningBy(p -> p.getAge() > 30));
        
        // Statistics
        IntSummaryStatistics stats = people.stream()
            .collect(Collectors.summarizingInt(Person::getAge));
        System.out.println("Average: " + stats.getAverage());
        System.out.println("Max: " + stats.getMax());
        System.out.println("Sum: " + stats.getSum());
        
        // Reducing
        Optional<Integer> totalAge = people.stream()
            .map(Person::getAge)
            .collect(Collectors.reducing(Integer::sum));
    }
}
```

### flatMap and Complex Transformations

```java
public class FlatMapDemo {
    public static void main(String[] args) {
        // flatMap: Flatten nested structures
        List<List<Integer>> nested = List.of(
            List.of(1, 2, 3),
            List.of(4, 5, 6),
            List.of(7, 8, 9)
        );
        
        // map would give Stream<List<Integer>>
        // flatMap gives Stream<Integer>
        List<Integer> flat = nested.stream()
            .flatMap(List::stream)
            .toList();  // [1, 2, 3, 4, 5, 6, 7, 8, 9]
        
        // Practical example: Orders with line items
        List<Order> orders = getOrders();
        
        // Get all products across all orders
        List<Product> allProducts = orders.stream()
            .flatMap(order -> order.getLineItems().stream())
            .map(LineItem::getProduct)
            .distinct()
            .toList();
        
        // Get all words from sentences
        List<String> sentences = List.of("Hello world", "Java streams");
        List<String> words = sentences.stream()
            .flatMap(s -> Arrays.stream(s.split(" ")))
            .toList();  // [Hello, world, Java, streams]
        
        // flatMap with Optional
        Optional<String> optional = Optional.of("hello");
        Optional<String> transformed = optional
            .flatMap(s -> s.isEmpty() ? Optional.empty() : Optional.of(s.toUpperCase()));
    }
}
```

### Optional: Avoiding Null

```java
public class OptionalDemo {
    public static void main(String[] args) {
        // Creating Optional
        Optional<String> empty = Optional.empty();
        Optional<String> present = Optional.of("Hello");
        Optional<String> nullable = Optional.ofNullable(getPossiblyNull());
        
        // Checking and getting
        if (present.isPresent()) {
            System.out.println(present.get());
        }
        
        // Better: Use ifPresent
        present.ifPresent(System.out::println);
        
        // ifPresentOrElse (Java 9+)
        present.ifPresentOrElse(
            System.out::println,
            () -> System.out.println("Empty!")
        );
        
        // Default values
        String value1 = empty.orElse("default");
        String value2 = empty.orElseGet(() -> computeDefault());  // Lazy
        String value3 = empty.orElseThrow(() -> new RuntimeException("Missing!"));
        String value4 = empty.orElseThrow();  // NoSuchElementException
        
        // Transforming
        Optional<Integer> length = present.map(String::length);
        
        // flatMap for nested Optionals
        Optional<Optional<String>> nested = Optional.of(Optional.of("Hi"));
        Optional<String> flattened = nested.flatMap(Function.identity());
        
        // Filtering
        Optional<String> filtered = present.filter(s -> s.length() > 3);
        
        // Stream (Java 9+)
        Stream<String> stream = present.stream();  // Empty or single-element stream
        
        // Chaining with or() (Java 9+)
        Optional<String> result = empty
            .or(() -> Optional.of("fallback"));
        
        // ❌ BAD: Using Optional wrong
        // Optional<String> o = null;  // Never do this
        // if (o.isPresent()) { o.get(); }  // Use ifPresent or orElse instead
        // Optional as field or parameter  // Use only for return types
    }
    
    // ✅ GOOD: Optional for return types that might be absent
    public Optional<User> findUserById(String id) {
        User user = database.find(id);
        return Optional.ofNullable(user);
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### Stream Internals: Lazy Evaluation

```java
/*
 * LAZY EVALUATION: Intermediate operations don't execute until terminal
 * Elements processed one-at-a-time, not all-at-once
 */

public class StreamLaziness {
    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // Nothing happens here - just pipeline setup
        Stream<Integer> stream = numbers.stream()
            .filter(n -> {
                System.out.println("Filtering: " + n);
                return n > 5;
            })
            .map(n -> {
                System.out.println("Mapping: " + n);
                return n * 2;
            });
        
        System.out.println("Pipeline created, nothing executed yet");
        
        // Execution happens here
        List<Integer> result = stream.toList();
        
        /*
         * Output shows interleaved processing:
         * Filtering: 1
         * Filtering: 2
         * ... (1-5 filtered out, no mapping)
         * Filtering: 6
         * Mapping: 6    <- First element that passes goes through full pipeline
         * Filtering: 7
         * Mapping: 7
         * ...
         */
    }
    
    // SHORT-CIRCUIT operations can skip processing
    public void shortCircuit() {
        Optional<Integer> first = Stream.iterate(1, n -> n + 1)
            .filter(n -> n > 100)
            .findFirst();  // Stops after finding first match
        // Doesn't process infinite stream!
    }
}
```

### Parallel Streams

```java
public class ParallelStreams {
    public static void main(String[] args) {
        List<Integer> numbers = IntStream.range(0, 1_000_000)
            .boxed()
            .toList();
        
        // Create parallel stream
        long sum1 = numbers.parallelStream()
            .mapToLong(Integer::longValue)
            .sum();
        
        // Or convert existing stream
        long sum2 = numbers.stream()
            .parallel()
            .mapToLong(Integer::longValue)
            .sum();
        
        // WHEN TO USE PARALLEL:
        // ✅ Large datasets (10,000+ elements)
        // ✅ CPU-intensive operations
        // ✅ Independent operations (no shared mutable state)
        // ✅ Splittable sources (ArrayList, arrays - not LinkedList)
        //
        // WHEN NOT TO USE:
        // ❌ Small datasets (overhead > benefit)
        // ❌ IO-bound operations
        // ❌ Order-dependent operations
        // ❌ Already in thread pool (uses common ForkJoinPool)
        // ❌ LinkedList or single-threaded streams
        
        // DANGER: Parallel with mutable state
        List<Integer> unsafeList = new ArrayList<>();
        numbers.parallelStream()
            .forEach(unsafeList::add);  // ❌ Race condition!
        
        // SAFE: Use thread-safe collection or collect
        List<Integer> safeList = numbers.parallelStream()
            .collect(Collectors.toList());  // ✅ Safe
        
        // Custom thread pool for parallel streams
        ForkJoinPool customPool = new ForkJoinPool(4);
        long result = customPool.submit(() ->
            numbers.parallelStream()
                .mapToLong(Integer::longValue)
                .sum()
        ).join();
    }
}
```

### Performance Considerations

```java
public class StreamPerformance {
    /*
     * STREAM OVERHEAD:
     * - Object creation (lambda, stream objects)
     * - Method call overhead
     * - Boxing/unboxing for primitives
     * 
     * For small/simple operations, loops may be faster.
     * For complex transformations, streams are cleaner and often faster.
     */
    
    // Use primitive streams to avoid boxing
    public void primitiveStreams() {
        // ❌ Boxing overhead
        int sum1 = List.of(1, 2, 3, 4, 5).stream()
            .mapToInt(Integer::intValue)
            .sum();
        
        // ✅ No boxing
        int sum2 = IntStream.of(1, 2, 3, 4, 5).sum();
        
        // Primitive stream operations
        IntStream.range(0, 100)
            .average()    // OptionalDouble
            .sum()        // int
            .min()        // OptionalInt
            .max();       // OptionalInt
        
        // Convert boxed to primitive
        List<Integer> boxed = List.of(1, 2, 3);
        int[] array = boxed.stream()
            .mapToInt(Integer::intValue)
            .toArray();
    }
    
    // Avoid stateful lambdas
    public void statelessVsStateful() {
        // ❌ Stateful: Lambda captures external state
        List<Integer> external = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(external::add);
        
        // ✅ Stateless: Collect instead
        List<Integer> result = Stream.of(1, 2, 3)
            .collect(Collectors.toList());
    }
}
```

---

## Layer 4 — Interview Mastery

### Frequently Asked Interview Questions

**Q1: "What is a Stream and how does it differ from a Collection?"**

| Aspect | Collection | Stream |
|--------|-----------|--------|
| Storage | Stores elements | Doesn't store, processes |
| Consumption | Can traverse multiple times | Single use |
| Eagerness | Eagerly populated | Lazily evaluated |
| Modification | Can add/remove | Read-only pipeline |
| Infinite | Must be finite | Can be infinite |

> "A Stream is a sequence of elements supporting functional-style operations. Unlike collections that store data, streams process data from a source through a pipeline. They're lazy—intermediate operations only execute when a terminal operation is invoked. Streams are also single-use; you can't reuse a stream after a terminal operation."

---

**Q2: "Explain the difference between map() and flatMap()."**

> "Both transform elements, but:
>
> `map()` applies a function to each element, producing one output per input. `Stream<T>` becomes `Stream<R>`.
>
> `flatMap()` applies a function that returns a stream for each element, then flattens all streams into one. It's used to unwrap nested structures.
>
> Example: For a list of sentences, `map(s -> s.split(' '))` gives `Stream<String[]>`. `flatMap(s -> Arrays.stream(s.split(' ')))` gives `Stream<String>` with all words."

---

**Q3: "When should you use parallel streams?"**

> "Parallel streams should be used when:
> 1. Large dataset (10K+ elements)
> 2. CPU-bound operations (not IO)
> 3. Operations are independent (no shared state)
> 4. Source splits well (ArrayList, arrays—not LinkedList)
>
> Avoid when:
> - Small datasets (overhead exceeds benefit)
> - IO operations (threads sit idle)
> - Order matters (ordering has overhead)
> - Already in a thread pool (contention on ForkJoinPool)
>
> Always benchmark—parallel isn't automatically faster."

---

**Q4: "Why are lambdas effectively final?"**

> "Lambdas can only capture variables that are final or effectively final (never reassigned). This is because:
>
> 1. **Thread safety**: Lambda might execute on different thread than variable
> 2. **No boxing**: Without finality, Java would need to box primitives for capture
> 3. **Simplicity**: Avoids confusion about variable lifetime
>
> If you need mutable state, use atomic classes or single-element arrays as a workaround (though this is a code smell)."

---

### Checkpoint: Streams & Functional Programming

✅ You should now understand:
- [ ] Lambda syntax and when to use method references
- [ ] Common functional interfaces (Supplier, Consumer, Function, Predicate)
- [ ] Stream pipeline: source → intermediate → terminal
- [ ] Collectors for complex aggregations
- [ ] flatMap for nested structures
- [ ] Optional for null-safe programming
- [ ] Lazy evaluation and parallel streams

**Mental Model**:
> Think of streams as assembly lines:
> - **Source**: Raw materials arriving
> - **Intermediate ops**: Workstations (filter, transform)
> - **Terminal op**: Final product packaged and shipped
> - **Lazy**: Nothing moves until customer orders

---

# Chapter 9: Garbage Collection & Memory Management

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

# Chapter 14: Design Patterns (Interview-Focused)

## Core Patterns Every Developer Must Know

---

### Singleton Pattern

```java
// Thread-safe lazy singleton (Bill Pugh)
public class Singleton {
    private Singleton() { }
    
    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}

// Enum singleton (Effective Java recommended)
public enum EnumSingleton {
    INSTANCE;
    
    public void doSomething() { }
}
```

### Factory Pattern

```java
public interface Animal {
    void speak();
}

public class Dog implements Animal {
    public void speak() { System.out.println("Woof"); }
}

public class Cat implements Animal {
    public void speak() { System.out.println("Meow"); }
}

// Simple Factory
public class AnimalFactory {
    public static Animal create(String type) {
        return switch (type.toLowerCase()) {
            case "dog" -> new Dog();
            case "cat" -> new Cat();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

### Builder Pattern

```java
public class User {
    private final String name;      // Required
    private final String email;     // Required
    private final int age;          // Optional
    private final String phone;     // Optional
    
    private User(Builder builder) {
        this.name = builder.name;
        this.email = builder.email;
        this.age = builder.age;
        this.phone = builder.phone;
    }
    
    public static class Builder {
        private final String name;
        private final String email;
        private int age = 0;
        private String phone = "";
        
        public Builder(String name, String email) {
            this.name = name;
            this.email = email;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public User build() {
            return new User(this);
        }
    }
    
    // Usage
    public static void main(String[] args) {
        User user = new User.Builder("John", "john@email.com")


---

[← Back to Index](00-README.md) | [Previous: Concurrency](07-Concurrency.md) | [Next: GC & Memory →](09-GC-Memory.md)
