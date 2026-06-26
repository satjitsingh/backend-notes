# Chapter 19: Modern Java (Java 21 Features)

[← Back to Index](00-README.md) | [Previous: Security](18-Security.md) | [Next: Summary →](99-Summary.md)

---

## Key Java 21 Features

### Record Patterns (Pattern Matching)

```java
public record Point(int x, int y) {}
public record Rectangle(Point topLeft, Point bottomRight) {}

// Deconstruct records in patterns
public void processShape(Object obj) {
    if (obj instanceof Rectangle(Point(int x1, int y1), Point(int x2, int y2))) {
        int width = x2 - x1;
        int height = y2 - y1;
        System.out.println("Area: " + (width * height));
    }
}

// In switch
public String describe(Object obj) {
    return switch (obj) {
        case Point(int x, int y) when x == y -> "On diagonal at " + x;
        case Point(int x, int y) -> "Point at (" + x + ", " + y + ")";
        case null -> "null";
        default -> "Unknown";
    };
}
```

### Virtual Threads (Production Ready)

```java
// Simple creation
Thread.startVirtualThread(() -> {
    System.out.println("Running on virtual thread");
});

// Executor for virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1));
            return i;
        });
    });
}

// With builder
Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> {
        // Task
    });
```

### Sequenced Collections

```java
// New interfaces with first/last access
SequencedCollection<String> list = new ArrayList<>(List.of("A", "B", "C"));
list.getFirst();      // "A"
list.getLast();       // "C"
list.addFirst("Z");   // Add at beginning
list.addLast("D");    // Add at end
list.reversed();      // Reversed view

SequencedMap<String, Integer> map = new LinkedHashMap<>();
map.firstEntry();     // First key-value
map.lastEntry();      // Last key-value
map.pollFirstEntry(); // Remove and return first
```

### String Templates (Preview)

```java
// String templates (preview feature)
String name = "World";
int x = 10, y = 20;

// STR processor (preview)
String greeting = STR."Hello, \{name}!";
String math = STR."Sum: \{x + y}";
String multiline = STR."""
    Name: \{name}
    Coordinates: (\{x}, \{y})
    """;

// FMT processor (formatted)
double price = 19.99;
String formatted = FMT."Price: $%.2f\{price}";
```

### Pattern Matching for switch (Finalized)

```java
public String format(Object obj) {
    return switch (obj) {
        case Integer i -> "Integer: " + i;
        case Long l    -> "Long: " + l;
        case Double d  -> "Double: " + d;
        case String s when s.length() > 10 -> "Long string";
        case String s  -> "String: " + s;
        case null      -> "null";
        default        -> "Unknown";
    };
}
```

---

[← Back to Index](00-README.md) | [Previous: Security](18-Security.md) | [Next: Summary →](99-Summary.md)
