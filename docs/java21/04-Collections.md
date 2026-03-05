# Chapter 4: Collections Framework

[← Back to Index](00-README.md) | [Previous: OOP](03-OOP.md) | [Next: Generics →](05-Generics.md)

---

## Why Collections Mastery is Critical

> **Interview Reality**: Collections questions appear in 95% of Java interviews.
> Understanding internal workings separates the 10x developer from the rest.

The Java Collections Framework is one of the most well-designed APIs ever created. Deep understanding of it is non-negotiable.

---

## Layer 1 — Beginner Foundation

### The Collections Hierarchy

```
                          ┌─────────────┐
                          │  Iterable   │
                          └──────┬──────┘
                                 │
                          ┌──────▼──────┐
                          │ Collection  │
                          └──────┬──────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          │                      │                      │
    ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
    │   List    │          │    Set    │          │   Queue   │
    └─────┬─────┘          └─────┬─────┘          └─────┬─────┘
          │                      │                      │
    ┌─────┴─────┐          ┌─────┴─────┐          ┌─────┴─────┐
    │ArrayList  │          │ HashSet   │          │LinkedList │
    │LinkedList │          │ TreeSet   │          │ArrayDeque │
    │Vector     │          │LinkedHash │          │Priority   │
    └───────────┘          └───────────┘          └───────────┘

                          ┌─────────────┐
                          │    Map      │  (Not a Collection!)
                          └──────┬──────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
              ┌─────▼─────┐ ┌────▼─────┐ ┌────▼─────┐
              │ HashMap   │ │ TreeMap  │ │LinkedHash│
              │           │ │          │ │   Map    │
              └───────────┘ └──────────┘ └──────────┘
```

### List: Ordered, Allows Duplicates

```java
import java.util.*;

public class ListBasics {
    public static void main(String[] args) {
        // ArrayList: Best for random access, dynamic array
        List<String> arrayList = new ArrayList<>();
        arrayList.add("Apple");
        arrayList.add("Banana");
        arrayList.add("Apple");  // Duplicates allowed
        arrayList.add(1, "Orange");  // Insert at index
        
        System.out.println(arrayList);  // [Apple, Orange, Banana, Apple]
        System.out.println(arrayList.get(2));  // Banana (O(1) access)
        
        // LinkedList: Best for frequent insertions/deletions
        List<String> linkedList = new LinkedList<>();
        linkedList.add("First");
        linkedList.addFirst("New First");  // O(1) at ends
        linkedList.addLast("Last");
        
        // Common operations
        arrayList.size();                    // Number of elements
        arrayList.contains("Apple");         // true
        arrayList.indexOf("Apple");          // 0 (first occurrence)
        arrayList.lastIndexOf("Apple");      // 3 (last occurrence)
        arrayList.remove("Banana");          // Remove by object
        arrayList.remove(0);                 // Remove by index
        arrayList.set(0, "Mango");           // Replace at index
        arrayList.clear();                   // Remove all
        arrayList.isEmpty();                 // true
        
        // Modern creation (Java 9+)
        List<String> immutable = List.of("A", "B", "C");  // Immutable!
        // immutable.add("D");  // ❌ UnsupportedOperationException
        
        List<String> mutableCopy = new ArrayList<>(List.of("A", "B", "C"));
        mutableCopy.add("D");  // ✅ Works
    }
}
```

### Set: No Duplicates

```java
public class SetBasics {
    public static void main(String[] args) {
        // HashSet: Fastest, unordered
        Set<String> hashSet = new HashSet<>();
        hashSet.add("Apple");
        hashSet.add("Banana");
        hashSet.add("Apple");  // Ignored! Already exists
        System.out.println(hashSet.size());  // 2
        
        // LinkedHashSet: Maintains insertion order
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("C");
        linkedHashSet.add("A");
        linkedHashSet.add("B");
        System.out.println(linkedHashSet);  // [C, A, B] - order preserved
        
        // TreeSet: Sorted order
        Set<String> treeSet = new TreeSet<>();
        treeSet.add("C");
        treeSet.add("A");
        treeSet.add("B");
        System.out.println(treeSet);  // [A, B, C] - sorted!
        
        // Set operations
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));
        
        // Union
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);  // [1, 2, 3, 4, 5, 6]
        
        // Intersection
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);  // [3, 4]
        
        // Difference
        Set<Integer> difference = new HashSet<>(set1);
        difference.removeAll(set2);  // [1, 2]
        
        // Modern creation
        Set<String> immutableSet = Set.of("A", "B", "C");  // Immutable, no nulls
    }
}
```

### Map: Key-Value Pairs

```java
public class MapBasics {
    public static void main(String[] args) {
        // HashMap: Fastest, unordered
        Map<String, Integer> ages = new HashMap<>();
        ages.put("Alice", 30);
        ages.put("Bob", 25);
        ages.put("Alice", 31);  // Replaces old value!
        
        System.out.println(ages.get("Alice"));  // 31
        System.out.println(ages.get("Unknown"));  // null
        System.out.println(ages.getOrDefault("Unknown", 0));  // 0
        
        // Check existence
        ages.containsKey("Alice");  // true
        ages.containsValue(25);     // true
        
        // Iteration patterns
        // 1. Over entries (most efficient)
        for (Map.Entry<String, Integer> entry : ages.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        
        // 2. Over keys
        for (String name : ages.keySet()) {
            System.out.println(name + ": " + ages.get(name));
        }
        
        // 3. Over values
        for (Integer age : ages.values()) {
            System.out.println(age);
        }
        
        // 4. forEach (Java 8+)
        ages.forEach((name, age) -> System.out.println(name + ": " + age));
        
        // Useful methods
        ages.putIfAbsent("Charlie", 35);  // Only if key absent
        ages.computeIfAbsent("Dave", k -> k.length() * 10);  // Lazy compute
        ages.merge("Bob", 1, Integer::sum);  // Add 1 to Bob's age
        
        // Modern creation
        Map<String, Integer> immutableMap = Map.of(
            "Alice", 30,
            "Bob", 25
        );
        
        // For more than 10 entries
        Map<String, Integer> bigMap = Map.ofEntries(
            Map.entry("A", 1),
            Map.entry("B", 2)
            // ... up to any number
        );
    }
}
```

### Queue and Deque: FIFO and Double-Ended

```java
public class QueueBasics {
    public static void main(String[] args) {
        // Queue: FIFO (First In, First Out)
        Queue<String> queue = new LinkedList<>();
        queue.offer("First");   // Add to end
        queue.offer("Second");
        queue.offer("Third");
        
        System.out.println(queue.peek());  // "First" (view, don't remove)
        System.out.println(queue.poll());  // "First" (remove and return)
        System.out.println(queue.poll());  // "Second"
        
        // Deque: Double-ended queue (can be used as stack too)
        Deque<String> deque = new ArrayDeque<>();  // Preferred over Stack
        
        // As Queue (FIFO)
        deque.addLast("A");
        deque.addLast("B");
        deque.removeFirst();  // "A"
        
        // As Stack (LIFO - Last In, First Out)
        deque.push("C");  // Same as addFirst
        deque.push("D");
        deque.pop();      // "D" - Same as removeFirst
        
        // PriorityQueue: Elements ordered by priority
        PriorityQueue<Integer> pq = new PriorityQueue<>();  // Min-heap
        pq.offer(30);
        pq.offer(10);
        pq.offer(20);
        
        System.out.println(pq.poll());  // 10 (smallest first)
        System.out.println(pq.poll());  // 20
        System.out.println(pq.poll());  // 30
        
        // Max-heap
        PriorityQueue<Integer> maxPq = new PriorityQueue<>(Comparator.reverseOrder());
        maxPq.offer(30);
        maxPq.offer(10);
        maxPq.offer(20);
        System.out.println(maxPq.poll());  // 30 (largest first)
    }
}
```

---

## Layer 2 — Working Developer Level

### Choosing the Right Collection

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    COLLECTION SELECTION GUIDE                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Do you need key-value pairs?                                           │
│  ├── YES → Map                                                          │
│  │         ├── Need ordering? → TreeMap (sorted) / LinkedHashMap        │
│  │         ├── Need thread-safety? → ConcurrentHashMap                  │
│  │         └── None of above? → HashMap (default choice)                │
│  │                                                                       │
│  └── NO → Continue below                                                │
│                                                                          │
│  Do you need duplicates?                                                │
│  ├── NO → Set                                                           │
│  │        ├── Need ordering? → TreeSet (sorted) / LinkedHashSet         │
│  │        └── None of above? → HashSet (default choice)                 │
│  │                                                                       │
│  └── YES → Continue below                                               │
│                                                                          │
│  Do you need index access or ordered?                                   │
│  ├── YES → List                                                         │
│  │         ├── Mostly random access? → ArrayList                        │
│  │         └── Mostly insert/delete at ends? → LinkedList               │
│  │                                                                       │
│  └── NO → Queue/Deque                                                   │
│           ├── Priority ordering? → PriorityQueue                        │
│           ├── Stack operations? → ArrayDeque                            │
│           └── General FIFO? → LinkedList or ArrayDeque                  │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Time Complexity Reference

| Operation | ArrayList | LinkedList | HashSet | TreeSet | HashMap | TreeMap |
|-----------|-----------|------------|---------|---------|---------|---------|
| add() | O(1)* | O(1) | O(1) | O(log n) | O(1) | O(log n) |
| get(index) | O(1) | O(n) | N/A | N/A | N/A | N/A |
| get(key) | N/A | N/A | N/A | N/A | O(1) | O(log n) |
| contains() | O(n) | O(n) | O(1) | O(log n) | O(1) | O(log n) |
| remove() | O(n) | O(1)** | O(1) | O(log n) | O(1) | O(log n) |

\* Amortized - occasional resize takes O(n)  
\** O(1) only if you have iterator/reference, O(n) to find

### Comparable vs Comparator

```java
// COMPARABLE: Natural ordering (class implements comparison)
public class Employee implements Comparable<Employee> {
    private String name;
    private int salary;
    
    public Employee(String name, int salary) {
        this.name = name;
        this.salary = salary;
    }
    
    // Natural ordering: by salary ascending
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.salary, other.salary);
        // Returns: negative (this < other), 0 (equal), positive (this > other)
    }
    
    @Override
    public String toString() {
        return name + "($" + salary + ")";
    }
    
    public String getName() { return name; }
    public int getSalary() { return salary; }
}

// Usage with natural ordering
List<Employee> employees = new ArrayList<>(List.of(
    new Employee("Alice", 50000),
    new Employee("Bob", 60000),
    new Employee("Charlie", 45000)
));
Collections.sort(employees);  // Uses compareTo
System.out.println(employees);  // [Charlie($45000), Alice($50000), Bob($60000)]

// COMPARATOR: Custom ordering (external comparison)
// Sort by name
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
employees.sort(byName);
System.out.println(employees);  // [Alice($50000), Bob($60000), Charlie($45000)]

// Sort by salary descending
Comparator<Employee> bySalaryDesc = Comparator.comparing(Employee::getSalary).reversed();
employees.sort(bySalaryDesc);

// Complex comparators
Comparator<Employee> complex = Comparator
    .comparing(Employee::getSalary)
    .reversed()
    .thenComparing(Employee::getName);  // Secondary sort

// Null-safe comparators
Comparator<Employee> nullSafe = Comparator
    .nullsFirst(Comparator.comparing(Employee::getName));
```

### Fail-Fast vs Fail-Safe Iterators

```java
public class IteratorBehavior {
    public static void main(String[] args) {
        // FAIL-FAST: Throws ConcurrentModificationException
        List<String> list = new ArrayList<>(List.of("A", "B", "C"));
        
        // ❌ This will throw ConcurrentModificationException
        try {
            for (String s : list) {
                if (s.equals("B")) {
                    list.remove(s);  // Modifying during iteration
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught exception!");
        }
        
        // ✅ Correct way: Use Iterator.remove()
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String s = iterator.next();
            if (s.equals("B")) {
                iterator.remove();  // Safe removal
            }
        }
        
        // ✅ Or use removeIf (Java 8+)
        list = new ArrayList<>(List.of("A", "B", "C"));
        list.removeIf(s -> s.equals("B"));
        
        // FAIL-SAFE: Uses copy, doesn't throw
        CopyOnWriteArrayList<String> copyOnWrite = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));
        for (String s : copyOnWrite) {
            if (s.equals("B")) {
                copyOnWrite.remove(s);  // Works! (but iterates over snapshot)
            }
        }
        // Note: Expensive for frequent writes, good for read-heavy scenarios
    }
}
```

### Collections Utility Class

```java
import java.util.Collections;

public class CollectionsUtility {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(3, 1, 4, 1, 5, 9, 2, 6));
        
        // Sorting
        Collections.sort(list);           // Natural order
        Collections.sort(list, Collections.reverseOrder());  // Reverse
        
        // Searching (list must be sorted!)
        Collections.sort(list);
        int index = Collections.binarySearch(list, 5);  // Returns index or negative
        
        // Shuffling
        Collections.shuffle(list);
        Collections.shuffle(list, new Random(42));  // Reproducible shuffle
        
        // Reversing
        Collections.reverse(list);
        
        // Min/Max
        int min = Collections.min(list);
        int max = Collections.max(list);
        
        // Frequency
        int count = Collections.frequency(list, 1);  // Count of 1s
        
        // Fill
        Collections.fill(list, 0);  // All elements become 0
        
        // Rotate
        list = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        Collections.rotate(list, 2);  // [4, 5, 1, 2, 3]
        
        // Unmodifiable wrappers
        List<String> original = new ArrayList<>(List.of("A", "B"));
        List<String> unmodifiable = Collections.unmodifiableList(original);
        // unmodifiable.add("C");  // ❌ UnsupportedOperationException
        original.add("C");  // ✅ Still works! And unmodifiable sees it!
        
        // Synchronized wrappers (not recommended for new code)
        List<String> synced = Collections.synchronizedList(new ArrayList<>());
        
        // Singleton collections
        Set<String> singletonSet = Collections.singleton("Only");
        List<String> singletonList = Collections.singletonList("Only");
        Map<String, Integer> singletonMap = Collections.singletonMap("Key", 1);
        
        // Empty collections
        List<String> emptyList = Collections.emptyList();
        Set<String> emptySet = Collections.emptySet();
        Map<String, String> emptyMap = Collections.emptyMap();
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### HashMap Internals: How It Really Works

```java
/*
 * HashMap Internal Structure:
 * 
 * 1. Array of "buckets" (Node[] table)
 * 2. Each bucket is a linked list (or tree if too many collisions)
 * 3. Key's hashCode determines bucket index
 */

// Simplified HashMap implementation concept
public class HashMapInternals {
    
    /*
     * HOW PUT WORKS:
     * 
     * 1. Calculate hashCode of key
     * 2. Compute bucket index: (n-1) & hash  (where n is array length)
     * 3. If bucket empty: insert new Node
     * 4. If bucket has nodes: 
     *    - Check each for key equality
     *    - If key exists: update value
     *    - If key doesn't exist: append to list (or tree)
     * 5. If size > threshold: resize (rehash)
     * 
     * HASH COLLISION HANDLING:
     * - Initially: linked list in bucket
     * - If list exceeds TREEIFY_THRESHOLD (8): convert to red-black tree
     * - If tree shrinks below UNTREEIFY_THRESHOLD (6): convert back to list
     * 
     * RESIZING:
     * - When loadFactor (default 0.75) exceeded
     * - New capacity = old capacity * 2
     * - All entries rehashed to new bucket positions
     */
    
    public static void main(String[] args) {
        // Why initial capacity matters
        Map<String, Integer> map1 = new HashMap<>();  // Default capacity: 16
        Map<String, Integer> map2 = new HashMap<>(1000);  // Avoids resizing
        
        // For known size, set initial capacity to avoid resizing
        int expectedSize = 1000;
        int initialCapacity = (int) (expectedSize / 0.75) + 1;  // Account for load factor
        Map<String, Integer> optimal = new HashMap<>(initialCapacity);
        
        // Visualizing bucket distribution
        Map<BadHashKey, String> badMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            badMap.put(new BadHashKey(i), "Value" + i);
        }
        // All entries in ONE bucket → O(n) for everything!
        
        Map<GoodHashKey, String> goodMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            goodMap.put(new GoodHashKey(i), "Value" + i);
        }
        // Evenly distributed → O(1) average
    }
}

class BadHashKey {
    private int value;
    
    public BadHashKey(int value) { this.value = value; }
    
    @Override
    public int hashCode() {
        return 1;  // ❌ TERRIBLE! All objects in same bucket
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return value == ((BadHashKey) o).value;
    }
}

class GoodHashKey {
    private int value;
    
    public GoodHashKey(int value) { this.value = value; }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(value);  // ✅ Good distribution
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return value == ((GoodHashKey) o).value;
    }
}
```

```
┌────────────────────────────────────────────────────────────────────────┐
│                    HASHMAP INTERNAL STRUCTURE                           │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  HashMap (capacity=16, loadFactor=0.75)                                 │
│                                                                         │
│  table[] (bucket array):                                                │
│  ┌───────┬───────┬───────┬───────┬───────┬───────┬───────┬───────┐     │
│  │ [0]   │ [1]   │ [2]   │ [3]   │ [4]   │ [5]   │  ...  │ [15]  │     │
│  │ null  │ Node  │ null  │ Node  │ null  │ Tree  │       │ null  │     │
│  └───────┴───┬───┴───────┴───┬───┴───────┴───┬───┴───────┴───────┘     │
│              │               │               │                          │
│              ▼               ▼               ▼                          │
│         ┌────────┐      ┌────────┐     ┌──────────┐                    │
│         │Key:Val │      │Key:Val │     │ TreeNode │                    │
│         │hash    │      │hash    │     │ (RB-tree)│                    │
│         │next:───┼──┐   │next:null│    │          │                    │
│         └────────┘  │   └────────┘     └──────────┘                    │
│                     │                                                   │
│                     ▼                                                   │
│                ┌────────┐                                               │
│                │Key:Val │  (Collision - same bucket)                   │
│                │hash    │                                               │
│                │next:null│                                              │
│                └────────┘                                               │
│                                                                         │
│  TREEIFICATION:                                                         │
│  - When bucket has > 8 nodes AND table size >= 64                      │
│  - Linked list → Red-Black Tree                                        │
│  - Lookup: O(n) → O(log n)                                             │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

### ArrayList Internals

```java
/*
 * ArrayList is backed by Object[] (dynamic array)
 * 
 * GROWTH STRATEGY:
 * - Default initial capacity: 10
 * - When full: newCapacity = oldCapacity + (oldCapacity >> 1) = 1.5x
 * - Example: 10 → 15 → 22 → 33 → 49 → ...
 * 
 * WHY 1.5x NOT 2x?
 * - Memory efficiency: Less wasted space
 * - Still amortized O(1) for add
 * - Java's choice; other languages differ
 */

public class ArrayListInternals {
    public static void main(String[] args) {
        // Specify initial capacity for known sizes
        List<String> list = new ArrayList<>(1000);  // Avoids 7 resizes
        
        // trimToSize() releases unused capacity
        ArrayList<String> trimable = new ArrayList<>(1000);
        trimable.add("One");
        trimable.add("Two");
        // Internal array has 1000 slots, only 2 used
        trimable.trimToSize();
        // Internal array now has 2 slots
        
        // ensureCapacity() for bulk adds
        ArrayList<Integer> numbers = new ArrayList<>();
        numbers.ensureCapacity(10000);  // Pre-allocate
        for (int i = 0; i < 10000; i++) {
            numbers.add(i);  // No resizing needed
        }
    }
}
```

### LinkedList Internals

```java
/*
 * LinkedList is a doubly-linked list
 * 
 * Node Structure:
 * ┌──────────────────────────────────┐
 * │  prev │   item   │     next     │
 * └──────────────────────────────────┘
 * 
 * Properties:
 * - first: pointer to head node
 * - last: pointer to tail node
 * - size: element count
 * 
 * Tradeoffs:
 * - O(1) add/remove at ends
 * - O(n) get by index (must traverse)
 * - Higher memory overhead (node objects)
 * - Poor cache locality
 */

public class LinkedListInternals {
    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>();
        
        // O(1) operations
        list.addFirst("A");
        list.addLast("Z");
        list.removeFirst();
        list.removeLast();
        
        // O(n) operations - avoid!
        list.addAll(List.of("A", "B", "C", "D", "E"));
        String middle = list.get(2);  // Must traverse from head
        
        // WHEN TO USE LINKEDLIST:
        // 1. Frequent insertions/deletions at both ends (use as Deque)
        // 2. Implementing queue/stack
        // 3. When you never access by index
        
        // WHEN NOT TO USE:
        // 1. Need random access
        // 2. General-purpose list (ArrayList almost always faster)
        // 3. Memory is a concern
    }
}
```

### TreeMap Internals: Red-Black Tree

```java
/*
 * TreeMap uses a Red-Black Tree:
 * - Self-balancing binary search tree
 * - Guarantees O(log n) for all operations
 * - Maintains sorted order
 * 
 * Red-Black Properties:
 * 1. Every node is red or black
 * 2. Root is black
 * 3. All leaves (NIL) are black
 * 4. Red node's children are black
 * 5. All paths from node to leaves have same black nodes
 */

public class TreeMapFeatures {
    public static void main(String[] args) {
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(5, "Five");
        treeMap.put(2, "Two");
        treeMap.put(8, "Eight");
        treeMap.put(1, "One");
        treeMap.put(9, "Nine");
        
        // Navigation methods (not in HashMap)
        System.out.println(treeMap.firstKey());      // 1
        System.out.println(treeMap.lastKey());       // 9
        System.out.println(treeMap.lowerKey(5));     // 2 (strictly less than)
        System.out.println(treeMap.floorKey(5));     // 5 (less than or equal)
        System.out.println(treeMap.higherKey(5));    // 8 (strictly greater)
        System.out.println(treeMap.ceilingKey(5));   // 5 (greater than or equal)
        
        // Submap views
        SortedMap<Integer, String> sub = treeMap.subMap(2, 8);  // [2, 8)
        NavigableMap<Integer, String> desc = treeMap.descendingMap();
        
        // Polling (retrieve and remove)
        Map.Entry<Integer, String> first = treeMap.pollFirstEntry();
        Map.Entry<Integer, String> last = treeMap.pollLastEntry();
    }
}
```

### ConcurrentHashMap: Thread-Safe Map

```java
import java.util.concurrent.*;

/*
 * ConcurrentHashMap (Java 8+):
 * - No global lock
 * - Uses CAS (Compare-And-Swap) for updates
 * - Locks individual buckets (fine-grained)
 * - Allows concurrent reads without locking
 * - Atomic compound operations
 */

public class ConcurrentHashMapDemo {
    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // Basic operations are thread-safe
        map.put("A", 1);
        Integer value = map.get("A");
        
        // Atomic compound operations (not possible with regular HashMap)
        map.putIfAbsent("B", 2);
        
        map.compute("A", (key, val) -> val == null ? 1 : val + 1);
        
        map.merge("A", 1, Integer::sum);  // Increment by 1
        
        // Bulk operations (parallel)
        map.forEach(2, (key, val) -> System.out.println(key + "=" + val));
        
        Integer sum = map.reduceValues(2, Integer::sum);
        
        // size() is approximate during concurrent modifications
        // mappingCount() returns long (for huge maps)
        long count = map.mappingCount();
        
        // ❌ STILL NOT SAFE: Check-then-act without synchronization
        // Multiple threads might see the same value and both increment
        if (!map.containsKey("C")) {
            map.put("C", 1);  // Race condition!
        }
        
        // ✅ SAFE: Use atomic operation
        map.putIfAbsent("C", 1);
    }
}
```

---

## Layer 4 — Interview Mastery

### Frequently Asked Interview Questions

**Q1: "Explain the difference between ArrayList and LinkedList. When would you use each?"**

**Senior Answer**:
> "ArrayList uses a dynamic array, giving O(1) random access but O(n) insertions in the middle due to shifting. LinkedList is a doubly-linked list with O(1) insertions at ends but O(n) access by index.
>
> In practice, I almost always use ArrayList because:
> 1. Better cache locality (contiguous memory)
> 2. Random access is common
> 3. Even middle insertions are often faster due to CPU cache effects
>
> I use LinkedList only when:
> - I need constant-time insertions/removals at both ends (as Deque)
> - I'm implementing a queue
> - I never access by index
>
> The 'LinkedList for insertions' advice is outdated. Modern CPUs make ArrayList's array copying cheaper than LinkedList's pointer chasing."

---

**Q2: "How does HashMap handle hash collisions?"**

> "When two keys have the same hash (or map to the same bucket), HashMap stores them in the same bucket using:
>
> 1. **Linked List** (default): Nodes form a chain. Lookup requires traversing the chain, O(n) worst case.
>
> 2. **Red-Black Tree** (Java 8+): When a bucket exceeds 8 nodes and table size ≥64, the list converts to a tree. This improves worst-case lookup from O(n) to O(log n).
>
> 3. **Untreeify**: When tree shrinks below 6 nodes, it converts back to a list.
>
> Good hash functions minimize collisions by distributing keys evenly across buckets. That's why hashCode() implementation is critical."

---

**Q3: "Why must we override hashCode when we override equals?"**

> "The hashCode contract states: if two objects are equal according to equals(), they must have the same hashCode(). Hash-based collections use hashCode() to determine the bucket.
>
> If I override equals() but not hashCode():
> - Two equal objects may have different hash codes
> - They end up in different buckets
> - The collection can't find the object even though an equal one exists
>
> Example: I add `person1` to a HashSet, then check `contains(person2)` where `person1.equals(person2)` is true. Without proper hashCode(), it may return false because they're in different buckets."

---

**Q4: "What's the difference between fail-fast and fail-safe iterators?"**

> "Fail-fast iterators (ArrayList, HashMap) throw ConcurrentModificationException if the collection is modified during iteration (except through the iterator itself). They check a modCount variable.
>
> Fail-safe iterators (CopyOnWriteArrayList, ConcurrentHashMap) work on a copy or use special algorithms to handle concurrent modifications. They don't throw exceptions but may not reflect the latest changes.
>
> For concurrent access, I prefer:
> - ConcurrentHashMap over synchronizedMap
> - CopyOnWriteArrayList for read-heavy scenarios
> - For write-heavy scenarios, consider other concurrent collections"

---

**Q5: "What happens internally when you add an element to a HashMap that causes it to resize?"**

> "When size exceeds capacity × loadFactor (default 0.75 × 16 = 12), HashMap resizes:
>
> 1. Creates new array with double capacity (16 → 32)
> 2. Rehashes every entry to its new bucket position
> 3. For each entry, recalculates bucket index: (newCapacity - 1) & hash
>
> This is O(n) and temporarily increases memory usage. To avoid resizing:
> - Set initial capacity when size is known
> - Formula: (expectedSize / 0.75) + 1
>
> Java 8+ optimization: During resize, entries either stay at the same index or move to index + oldCapacity, eliminating the need to recalculate the hash."

---

### Common Mistakes by Level

**Beginner Mistakes**:
- ❌ Using `==` to check if List contains object
- ❌ Not understanding that HashSet uses HashMap internally
- ❌ Modifying collection while iterating with for-each
- ❌ Using LinkedList for general-purpose lists

**Intermediate Mistakes**:
- ❌ Not specifying initial capacity for large known-size collections
- ❌ Using Collections.synchronized* instead of concurrent collections
- ❌ Mutating keys in a Map after insertion (breaks hash)
- ❌ Not understanding that TreeMap requires Comparable/Comparator

**Advanced Mistakes**:
- ❌ Using ConcurrentHashMap.size() for critical logic (it's approximate)
- ❌ Incorrect loadFactor tuning (rarely beneficial to change)
- ❌ Not considering memory overhead of LinkedList
- ❌ Over-synchronizing when single-threaded

---

### Checkpoint: Collections Framework

✅ You should now understand:
- [ ] The Collections hierarchy and when to use each type
- [ ] How HashMap and HashSet work internally
- [ ] Time complexity of common operations
- [ ] Comparable vs Comparator for sorting
- [ ] Fail-fast vs fail-safe iteration
- [ ] Thread-safe collections (ConcurrentHashMap, CopyOnWriteArrayList)
- [ ] Why hashCode and equals must be consistent

**Mental Model**:
> Think of collections as containers with different properties:
> - **ArrayList**: Numbered boxes on a shelf (fast access by number)
> - **LinkedList**: Chain of boxes (easy to add links, hard to find Nth)
> - **HashSet**: Labeled boxes with unique labels
> - **HashMap**: Mailboxes (each address maps to one slot)
> - **TreeMap**: Alphabetically sorted file cabinet

---

[← Back to Index](00-README.md) | [Previous: OOP](03-OOP.md) | [Next: Generics →](05-Generics.md)
