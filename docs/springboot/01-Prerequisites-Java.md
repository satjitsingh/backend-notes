# Chapter 1: Advanced Java Foundations (Prerequisites)

[← Back to Index](00-README.md) | [Next: OOP & SOLID →](02-Prerequisites-OOP-SOLID.md)

---

## Why This Chapter Matters

Imagine learning to drive a car without understanding the steering wheel. You could memorize "turn left, turn right"—but when something goes wrong, you'd have no idea why. Spring Boot is like a powerful car. To use it well—and to fix it when it breaks—you need to understand the engine underneath: **Java**.

This chapter gives you that foundation. Every `@Autowired`, every `List<User>`, every stream operation in Spring—they all rest on these concepts. Skip this, and you'll copy-paste code without understanding it. Master this, and you'll debug Spring issues, ace interviews, and write production-grade code.

**What happens without this foundation?**
- You see `ClassCastException` and have no clue what caused it
- Your app runs slowly because you picked the wrong collection
- A senior developer asks "How does dependency injection work?" and you freeze
- Spring's error messages look like gibberish

**What you gain with this foundation?**
- You'll *understand* why Spring works the way it does
- You'll write code that's fast, safe, and maintainable
- You'll confidently explain concepts in technical interviews
- You'll debug production issues instead of guessing

---

## Layer 1 — Intuition Builder

### Part 1: What is Java? (The Recipe and the Chef)

Think of your Java code as a **recipe** written in a special language. The recipe says things like "mix these ingredients" or "bake for 20 minutes." But a recipe doesn't bake a cake by itself—you need a **chef** to read it and do the work.

In Java:
- **Your code** = the recipe (the `.java` file)
- **The JVM (Java Virtual Machine)** = the chef that reads the recipe and executes it

```
    ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
    │  YourRecipe.java │  --->   │  RecipeCompiler   │  --->   │  Recipe.bytecode │
    │  (Human-readable) │        │  (javac - chef's  │         │  (Machine format) │
    └─────────────────┘         │  assistant)       │         └─────────────────┘
                                └─────────────────┘                    │
                                                                       v
    ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
    │  Your Computer   │  <---   │  JVM (The Chef)  │  <---   │  Runs bytecode   │
    │  (Windows/Mac)   │         │  Reads & runs    │         │  on any machine  │
    └─────────────────┘         └─────────────────┘         └─────────────────┘
```

**Why does this matter for Spring Boot?** Spring Boot is a Java application. It runs on the JVM. When Spring starts, the JVM loads your classes, runs your `main` method, and keeps everything in memory. Understanding the JVM helps you understand memory usage, performance, and why certain Spring configurations work the way they do.

---

### Part 2: Data Types (Different Boxes for Different Things)

Imagine you're organizing a toy room. You have:
- **Small boxes** for marbles (each holds one marble)
- **Bigger boxes** for stuffed animals
- **Labels** that say what's inside each box

In Java, **data types** are like these boxes. Each type tells the computer: "This box holds *this kind* of thing."

| Box Type (Java) | Holds | Real-Life Analogy |
|-----------------|-------|-------------------|
| `int` | Whole numbers (1, 2, 3) | Number of toys |
| `double` | Decimal numbers (3.14) | Weight in kg |
| `boolean` | True or False | Is the box empty? |
| `String` | Text ("Hello") | A label with words |

**Before (confusing):** Putting a toy car in a box labeled "apples"—the computer gets confused!

**After (clear):** `int score = 100;` — The computer knows exactly what's in the box.

---

### Part 3: Classes and Objects (Blueprints and Houses)

**A class** is like a **blueprint** for a house. The blueprint shows: "A house has a door, windows, and a roof." It doesn't *build* anything—it just describes what a house *would* look like.

**An object** is like an **actual house** built from that blueprint. You can build many houses from one blueprint—each is a separate object, but they all follow the same design.

```
    BLUEPRINT (Class: Dog)              ACTUAL HOUSES (Objects)
    ┌─────────────────────┐             ┌──────┐  ┌──────┐  ┌──────┐
    │  - name             │      -->    │ Max  │  │Buddy│  │Luna  │
    │  - age              │             │ 3yr  │  │ 5yr  │  │ 2yr  │
    │  + bark()           │             └──────┘  └──────┘  └──────┘
    └─────────────────────┘             Three different dogs, same blueprint
```

**Why Spring Boot cares:** Spring creates *objects* (called "beans") from your *classes*. When you write `@Service`, you're giving Spring a blueprint. Spring builds the actual objects and manages them for you.

---

### Part 4: Inheritance and Interfaces (Family Traits and Promises)

**Inheritance** = Like a child inheriting traits from a parent. A "Labrador" dog *is a* "Dog"—it gets everything a Dog has (bark, tail) plus its own special traits (retrieves balls).

```
         Animal (Parent)
              │
         has: breathe(), eat()
              │
         ┌────┴────┐
         ▼         ▼
       Dog       Cat (Children)
   has: bark()   has: meow()
   + inherits   + inherits
   Animal stuff  Animal stuff
```

**Interfaces** = Like a **contract** or **promise**. "I promise I can do X." A "Flying" contract might say: "Anything that signs this must have a `fly()` method." A bird signs it. An airplane signs it. They're different, but both *promise* to fly.

**Why Spring Boot cares:** Spring uses interfaces everywhere. When you write `UserRepository`, Spring finds the *implementation* (the actual class that does the work) and injects it. Understanding interfaces helps you understand dependency injection.

---

### Part 5: Collections (Different Ways to Store Things)

Imagine organizing your toys:

- **List** = A **shelf with numbered spots**. Toy 1, Toy 2, Toy 3. Order matters. You can have two red balls (duplicates OK).
- **Set** = A **"no duplicates" rule**. Like a stamp collection—you can't have the same stamp twice.
- **Map** = A **dictionary**. You look up a *word* (key) to get its *meaning* (value). "Apple" → "A fruit."

```
    LIST (Shelf)              SET (Unique Club)           MAP (Dictionary)
    ┌───┬───┬───┬───┐         ┌───┬───┬───┐               Apple  → Red fruit
    │ 1 │ 2 │ 1 │ 3 │         │ 1 │ 2 │ 3 │               Banana → Yellow fruit
    └───┴───┴───┴───┘         └───┴───┴───┘               Key      Value
    Duplicates OK             No duplicates
```

**Why Spring Boot cares:** Spring stores beans in collections. It uses `Map` structures to find beans by name or type. Understanding collections helps you choose the right one when building features.

---

### Part 6: When Things Go Wrong (Exceptions)

Sometimes things go wrong. You ask for the 10th toy on a shelf that only has 5—that's an error! In Java, when something unexpected happens, the program **throws an exception**—like raising your hand and saying "Hey, something's wrong!"

**Two types:**
- **Checked exceptions** = Things you're *told* might go wrong (like "the file might not exist"). You *must* handle them.
- **Unchecked exceptions** = Surprise problems (like "you divided by zero"). The program crashes if you don't handle them.

```
    Normal flow:     Do thing → Success → Continue
                         │
    Exception flow:      └──> Something wrong! → Throw exception → Handle or crash
```

**Why Spring Boot cares:** Spring has global exception handling. When your code throws an exception, Spring can catch it and return a nice error message to the user. Understanding exceptions helps you write robust APIs.

---

### Part 7: Generic Boxes (Type-Safe Containers)

Imagine a toy box. Without a label, you might grab a marble when you expected a ball—ouch! **Generics** are like putting a label on the box: "This box holds ONLY marbles."

**Before (unsafe):**
```
Box (no label) → You put in toy car, marbles, sandwich... 
                 Later you grab "marble" and get a sandwich. Yuck!
```

**After (generics):**
```
Box<Marble> → Only marbles allowed. 
              When you take something out, you KNOW it's a marble.
```

In Java: `List<String>` means "a list that holds ONLY strings." The compiler checks this for you. If you try to add a number, it won't compile.

**Why Spring Boot cares:** Spring uses generics everywhere—`Repository<User>`, `List<Bean>`. Type safety prevents bugs and helps Spring know what to inject where.

---

### Part 8: Lambdas and Streams (Shortcut Instructions)

**Lambda** = A tiny, compact way to give an instruction. Instead of writing a whole paragraph, you write one line.

*Before (verbose):* "For each toy in the box, if it's red, add it to the red pile."

*After (lambda):* `toys.filter(toy -> toy.isRed())`

**Stream** = A **conveyor belt** for your data. Items move down the belt. At each station, you can filter, transform, or sort them. At the end, you collect the result.

```
    [Apple, Banana, Cherry]  -->  Filter (length>5)  -->  Uppercase  -->  [BANANA, CHERRY]
         Input                        Station 1            Station 2         Output
```

**Why Spring Boot cares:** Modern Spring code uses streams for filtering beans, processing collections, and writing readable code. You'll see `.stream()`, `.filter()`, `.map()` everywhere.

---

### Part 9: Annotations (Sticky Notes for the Computer)

An **annotation** is like a sticky note you put on your code. It doesn't change what the code *does*—it adds extra information. "Hey, Spring! This is a service!" or "This method needs a transaction!"

```
    ┌─────────────────────────────────┐
    │  @Service                        │  <-- Sticky note: "I'm a service!"
    │  public class UserService {       │
    │      ...                         │
    │  }                               │
    └─────────────────────────────────┘
```

Spring reads these sticky notes at startup and decides: "I need to create a bean for UserService" or "I need to inject UserRepository here."

**Why Spring Boot cares:** Almost every Spring feature uses annotations—`@Component`, `@Autowired`, `@GetMapping`. Understanding annotations is understanding how Spring knows what to do with your code.

---

## Layer 2 — Professional Developer

### JVM Basics: What Actually Happens When You Run Java

When you run `java MyApp`, the JVM loads your bytecode and executes it. Here's the flow:

```java
// MyApp.java - You write this
public class MyApp {
    public static void main(String[] args) {
        System.out.println("Hello!");
    }
}

// Step 1: javac MyApp.java  → Creates MyApp.class (bytecode)
// Step 2: java MyApp       → JVM loads MyApp.class, finds main(), runs it
// Step 3: JVM manages: memory (heap/stack), garbage collection, threads
```

**Key JVM concepts for Spring Boot developers:**

| Concept | What It Means |
|---------|----------------|
| **Heap** | Where objects live. Spring beans are objects—they live here. |
| **Stack** | Where method calls and local variables live. Smaller, faster. |
| **Class Loader** | Loads `.class` files. Spring uses this to discover your components. |
| **Garbage Collector** | Frees memory when objects are no longer used. |

---

### Data Types: Primitives vs Objects

```java
// PRIMITIVES - Stored directly, no "wrapper"
int count = 10;           // 4 bytes, fast
double price = 19.99;     // 8 bytes
boolean active = true;    // true or false

// OBJECTS (Reference types) - Variable holds a "address" to the object
String name = "Alice";    // name points to a String object
Integer boxed = 10;       // Integer is a wrapper around int (object)
```

**When to use which:**
- Use **primitives** in loops and math—they're faster
- Use **objects** (including wrapper types) when you need `null` or when using collections (e.g., `List<Integer>`)

---

### OOP in Practice: Classes, Objects, Inheritance

```java
// CLASS = Blueprint
public class User {
    // Fields (data)
    private String name;
    private int age;

    // Constructor (how to build it)
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Methods (behavior)
    public String getName() {
        return name;
    }
}

// INHERITANCE = Child gets parent's stuff
public class AdminUser extends User {
    private String role = "admin";

    public AdminUser(String name, int age) {
        super(name, age);  // Call parent constructor
    }
}

// INTERFACE = Contract
public interface Savable {
    void save();
}

public class UserService implements Savable {
    @Override
    public void save() {
        // Must implement this method
    }
}
```

---

### Exception Handling: The Professional Way

```java
// CHECKED EXCEPTION - Must handle (compile error if you don't)
public String readFile(String path) {
    try {
        return Files.readString(Path.of(path));
    } catch (IOException e) {
        // Log and handle
        throw new RuntimeException("Could not read file: " + path, e);
    }
}

// UNCHECKED - Optional to handle, but good practice
public int divide(int a, int b) {
    if (b == 0) {
        throw new IllegalArgumentException("Cannot divide by zero");
    }
    return a / b;
}

// try-with-resources - Auto-closes resources
try (Connection conn = dataSource.getConnection()) {
    // Use connection
}  // conn.close() called automatically
```

---

### Collections: Choosing the Right One

| Need | Use | Why |
|------|-----|-----|
| Ordered, duplicates OK, fast access by index | `ArrayList` | Array under the hood, O(1) get |
| No duplicates, fast lookup | `HashSet` | Hash table, O(1) contains |
| Key-value, fast lookup | `HashMap` | Hash table, O(1) get/put |
| Key-value, sorted by key | `TreeMap` | Red-black tree, O(log n) |
| Key-value, insertion order | `LinkedHashMap` | Linked list + hash |

**ArrayList vs LinkedList:**

| Operation | ArrayList | LinkedList |
|-----------|-----------|------------|
| get(index) | O(1) | O(n) |
| add(element) at end | O(1) amortized | O(1) |
| add(index, element) | O(n) shift | O(n) traverse |
| remove(index) | O(n) shift | O(n) traverse |

**Decision rule:** Use ArrayList 90% of the time. LinkedList only for frequent insertions/deletions in the middle.

**HashMap vs TreeMap vs LinkedHashMap:**

```
Need key-value storage?
├─ Need sorted order?     → TreeMap (O(log n))
├─ Need insertion order?  → LinkedHashMap
└─ Just fast lookup?      → HashMap (O(1) average)
```

```java
// ArrayList - most common
List<String> names = new ArrayList<>();
names.add("Alice");
names.add("Bob");
String first = names.get(0);  // Indexed access

// HashMap - for lookups
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 95);
int score = scores.get("Alice");
```

---

### Generics: Type-Safe Code

```java
// Without generics - DANGEROUS (old style)
List list = new ArrayList();
list.add("hello");
list.add(123);              // No error!
String s = (String) list.get(1);  // ClassCastException at runtime!

// With generics - SAFE
List<String> list = new ArrayList<>();
list.add("hello");
list.add(123);              // Compile error!
String s = list.get(0);     // No cast needed

// Generic class
public class Box<T> {
    private T value;
    public void set(T value) { this.value = value; }
    public T get() { return value; }
}
Box<String> stringBox = new Box<>();
stringBox.set("Hello");

// Bounded type: T must extend Number
public class NumberBox<T extends Number> {
    private T value;
    public double getDouble() { return value.doubleValue(); }
}

// Wildcards - PECS: Producer Extends, Consumer Super
public void copy(List<? extends Number> src, List<? super Integer> dest) {
    for (Number n : src) dest.add(n.intValue());  // src=producer, dest=consumer
}
```

---

### Streams: Pipeline and Operations

**Stream pipeline:** Source → Intermediate (lazy) → Terminal (eager). Nothing runs until terminal.

```java
List<Integer> result = numbers.stream()      // SOURCE
    .filter(n -> n > 0)                      // INTERMEDIATE (lazy)
    .map(n -> n * 2)                         // INTERMEDIATE (lazy)
    .distinct()                               // INTERMEDIATE (lazy)
    .limit(10)                                // INTERMEDIATE (lazy)
    .collect(Collectors.toList());            // TERMINAL (executes all)
```

**Common terminal ops:** `collect`, `forEach`, `reduce`, `count`, `anyMatch`, `findFirst`.

**Collectors:**
```java
// Group by
Map<String, List<Person>> byRole = people.stream()
    .collect(Collectors.groupingBy(Person::getRole));

// Partition (true/false)
Map<Boolean, List<Integer>> evensOdds = numbers.stream()
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));
```

**Parallel streams:** Use only for large datasets (10k+), CPU-heavy ops. Avoid shared mutable state.

---

### Functional Interfaces: The Building Blocks

| Interface | Takes | Returns | Example |
|-----------|-------|---------|---------|
| `Function<T,R>` | T | R | `s -> s.length()` |
| `Predicate<T>` | T | boolean | `n -> n > 0` |
| `Consumer<T>` | T | void | `System.out::println` |
| `Supplier<T>` | nothing | T | `() -> Math.random()` |

```java
// Method reference: Class::method
names.stream().map(String::toUpperCase);
names.stream().filter(Objects::nonNull);
```

---

### Concurrency: synchronized, volatile, ExecutorService

```java
// synchronized - one thread at a time
public synchronized void increment() { count++; }

// volatile - visibility across threads (not atomicity!)
private volatile boolean running = true;

// ExecutorService - prefer over raw Thread
ExecutorService executor = Executors.newFixedThreadPool(10);
Future<String> future = executor.submit(() -> "result");
String result = future.get();
executor.shutdown();
```

**Thread-safe collections:** `ConcurrentHashMap`, `CopyOnWriteArrayList` (read-heavy).

---

### Lambdas: Before vs After

```java
// OLD: Anonymous class
Comparator<String> old = new Comparator<String>() {
    public int compare(String a, String b) {
        return a.length() - b.length();
    }
};

// NEW: Lambda
Comparator<String> better = (a, b) -> a.length() - b.length();

// Stream example (see Streams section above for full pipeline)
List<String> result = names.stream()
    .filter(n -> n.length() > 3)
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

---

### Annotations: How Spring Finds Your Code

```java
// @Component - "Spring, create a bean from this class"
@Component
public class EmailService {
    public void send(String to, String message) {
        // ...
    }
}

// @Autowired - "Spring, inject the right dependency here"
@Service
public class UserService {
    @Autowired
    private UserRepository repository;  // Spring finds and injects
}

// Custom annotation (for AOP, validation, etc.)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    String value() default "";
}
```

---

## Layer 3 — Advanced Engineering Depth

### Collections Internals: HashMap Under the Hood

```
    HashMap Structure (simplified)
    ┌─────────────────────────────────────────────┐
    │  Array of "buckets" (default 16)             │
    │  ┌─────┬─────┬─────┬─────┬─────┬─────┐      │
    │  │  0  │  1  │  2  │ ... │  5  │ ... │      │
    │  └──┬──┴─────┴─────┴─────┴──┬──┴─────┘      │
    │     │                      │                │
    │     v                      v                │
    │  [Entry]                [Entry]->[Entry]     │
    │  key,value              (collision: linked) │
    └─────────────────────────────────────────────┘

    Hash: key.hashCode() % bucketCount → bucket index
    Load factor 0.75: when 75% full, resize (double capacity)
```

**Java 8+**: When a bucket has >8 entries and capacity ≥64, the linked list becomes a **red-black tree** (O(log n) instead of O(n) worst case).

**Load factor & capacity:** Default load factor 0.75, initial 16 buckets. When size > 12, HashMap resizes. Pre-allocate if you know size: `new HashMap<>((int)(expectedSize/0.75)+1, 0.75f)`.

---

### ConcurrentHashMap vs HashMap

| | HashMap | ConcurrentHashMap |
|--|---------|-------------------|
| Thread-safe | No | Yes |
| Null key/value | One null key, many null values | No nulls |
| Locking | N/A | Fine-grained (per-bucket or CAS) |
| Use when | Single-threaded | Multi-threaded, high concurrency |

---

### Type Erasure: What Happens to Generics at Runtime

```java
// Compile time: List<String> and List<Integer> are different
List<String> strings = new ArrayList<>();
List<Integer> ints = new ArrayList<>();

// Runtime: BOTH become just "List" - type parameter is ERASED
// This is why: you can't do "new T[]", can't do "instanceof List<String>"
// Java did this for backward compatibility with pre-generics code
```

---

### Reflection: How Spring Reads Your Annotations

```java
// Spring does something like this internally:
Class<?> clazz = UserService.class;

// Check for @Service
if (clazz.isAnnotationPresent(Service.class)) {
    // Create instance
    Object instance = clazz.getDeclaredConstructor().newInstance();

    // Find @Autowired fields
    for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(Autowired.class)) {
            field.setAccessible(true);  // Can set private!
            Object dependency = context.getBean(field.getType());
            field.set(instance, dependency);
        }
    }
}
```

**Performance note:** Reflection is slow. Spring caches reflection metadata and uses bytecode generation (CGLIB) for proxies. The cost is paid at startup, not per-request.

---

### Memory and Performance Traps

```java
// BAD: Autoboxing in loop (creates millions of Integer objects)
Long sum = 0L;
for (long i = 0; i < 1_000_000; i++) {
    sum += i;  // Unbox Long, add, rebox to Long
}

// GOOD: Use primitive
long sum = 0L;

// BAD: String concatenation in loop
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // New String each time!
}

// GOOD: StringBuilder
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
```

---

### Concurrency: Thread Safety in Spring

Spring Boot handles **one request per thread** by default. Multiple users = multiple threads accessing shared data.

```java
// UNSAFE: Shared mutable state
public class Counter {
    private int count = 0;
    public void increment() { count++; }  // NOT atomic!
}

// SAFE: Atomic class
public class Counter {
    private AtomicInteger count = new AtomicInteger(0);
    public void increment() { count.incrementAndGet(); }
}

// SAFE: synchronized
public synchronized void increment() { count++; }
```

### CopyOnWriteArrayList: When to Use

Read-heavy, write-rarely. Writes copy the entire array (expensive) but reads need no locking. Safe to iterate while another thread modifies.

```java
CopyOnWriteArrayList<String> listeners = new CopyOnWriteArrayList<>();
// Many threads read, few write - no ConcurrentModificationException
```

### Annotation Retention: Why Spring Uses RUNTIME

| Retention | When Available | Example |
|-----------|----------------|--------|
| SOURCE | Discarded after compile | @Override |
| CLASS | In .class file, not loaded at runtime | Rare |
| RUNTIME | Available via reflection | @Component, @Autowired |

Spring reads annotations at startup—hence RUNTIME.

---

### Modifying a Collection During Iteration

Standard collections (ArrayList, HashMap) maintain a modification count. If you modify while iterating, you get `ConcurrentModificationException`.

**Solutions:**
- Use `Iterator.remove()` for safe removal
- Use `removeIf(predicate)` (Java 8+)
- Collect items to remove, then `removeAll()`
- Use `CopyOnWriteArrayList` for concurrent read/write (iterator sees snapshot)

---

## Layer 4 — Interview Mastery

### Q1: What is the difference between `ArrayList` and `LinkedList`?

**Answer:** `ArrayList` uses a dynamic array—contiguous memory. `LinkedList` uses doubly-linked nodes.

| Operation | ArrayList | LinkedList |
|-----------|------------|------------|
| get(i) | O(1) | O(n) |
| add(end) | O(1) amortized | O(1) |
| add(i, element) | O(n) | O(n) (but no shifting) |

**Use ArrayList** for 90% of cases. Use LinkedList only for frequent insertions/deletions in the middle or when implementing queues.

---

### Q2: How does `HashMap` work internally?

**Answer:** HashMap uses an array of buckets. For a key, it computes `hashCode() % bucketCount` to find the bucket. Collisions are stored as linked lists (or red-black trees when >8 in a bucket, Java 8+). Load factor 0.75 triggers resizing. Default initial capacity is 16.

---

### Q3: What is type erasure?

**Answer:** At compile time, generics provide type safety. At runtime, the JVM removes type parameters—`List<String>` and `List<Integer>` both become `List`. This was for backward compatibility with Java 1.4. Implications: no `new T[]`, no `instanceof List<String>`, no overloading by generic type.

---

### Q4: Explain PECS (Producer Extends, Consumer Super).

**Answer:** When working with generics:
- **Producer** (you read from it): use `? extends T`
- **Consumer** (you write to it): use `? super T`

Example: `Collections.copy(List<? super T> dest, List<? extends T> src)`—dest consumes, src produces.

---

### Q5: Why is `String` immutable?

**Answer:** (1) **Security**—strings used for passwords, URLs; mutability would allow tampering. (2) **Thread safety**—no synchronization needed. (3) **String pool**—literals can be shared safely. (4) **Hash caching**—hashCode computed once. (5) **Predictability**—passed around without fear of side effects.

---

### Q6: `volatile` vs `synchronized`?

**Answer:** `volatile` ensures **visibility** (changes visible to all threads) but NOT **atomicity**. Use for simple flags. `synchronized` provides both visibility and atomicity. Use for compound operations (e.g., increment). `volatile` is lighter; `synchronized` acquires a lock.

---

### Q7: How does Spring use reflection?

**Answer:** Spring uses reflection to: (1) Scan for `@Component`, `@Service`, etc.; (2) Read `@Autowired` on fields/methods; (3) Invoke `@Bean` methods; (4) Create proxies for `@Transactional`, AOP. It caches metadata and uses bytecode generation to minimize reflection overhead at runtime.

---

### Q8: When does `HashMap` convert to a tree?

**Answer:** When a bucket has more than 8 entries **and** the total capacity is at least 64. The linked list converts to a red-black tree, improving worst-case lookup from O(n) to O(log n). Reverts to list when bucket drops to 6 or fewer entries.

---

### Q9: What happens if you modify a collection while iterating?

**Answer:** For `ArrayList`, `HashMap`, etc.—`ConcurrentModificationException`. The iterator checks a modification count. Solutions: use `Iterator.remove()`, collect-to-remove-then-remove, `removeIf()`, or `CopyOnWriteArrayList` / `ConcurrentHashMap` for concurrent access.

---

### Q10: Stream vs Collection—when to use what?

**Answer:** **Collection** = materialized data, reusable, mutable. **Stream** = lazy, single-use, immutable, declarative. Use streams for one-time transformations/filtering. Use collections when you need to reuse data or random access.

---

### Q11: How would you make a class thread-safe?

**Answer:** (1) **Synchronization**—`synchronized` method or block. (2) **Atomic classes**—`AtomicInteger`, `AtomicReference`. (3) **Immutability**—make fields `final`, no setters. (4) **Thread-safe collections**—`ConcurrentHashMap`, `CopyOnWriteArrayList`. (5) **Volatile**—for visibility of simple flags (not compound ops). Prefer immutability and atomics when possible.

---

## Summary

- **JVM** runs your bytecode; heap holds objects (including Spring beans), stack holds method frames.
- **Data types**: Primitives for speed, objects for flexibility and null.
- **OOP**: Classes are blueprints, objects are instances. Inheritance shares behavior; interfaces define contracts.
- **Collections**: List (ordered, duplicates), Set (unique), Map (key-value). Choose by access pattern.
- **Exceptions**: Handle checked exceptions; design clear exception hierarchies for APIs.
- **Generics**: Provide compile-time type safety; erased at runtime (type erasure).
- **Lambdas/Streams**: Concise functions and declarative data processing.
- **Annotations**: Metadata for Spring (and other tools) to configure and wire your application.
- **Concurrency**: Spring is multi-threaded; use thread-safe collections and atomic operations when sharing state.
- **Reflection**: Spring relies on it for DI, scanning, and proxies; understand it to debug Spring.

---

[← Back to Index](00-README.md) | [Next: OOP & SOLID →](02-Prerequisites-OOP-SOLID.md)
