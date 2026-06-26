# Chapter 5: Generics

[← Back to Index](00-README.md) | [Previous: Collections](04-Collections.md) | [Next: Exceptions →](06-Exceptions.md)

---

## Why Generics Matter

> **The Problem They Solved**: Before Java 5, collections stored Objects. 
> Every retrieval required casting, and errors only appeared at runtime.

Generics provide compile-time type safety and eliminate casts. They're essential for writing reusable, type-safe code.

---

## Layer 1 — Beginner Foundation

### The Problem Without Generics

```java
// Before Generics (Pre-Java 5)
public class OldWay {
    public static void main(String[] args) {
        List names = new ArrayList();  // Raw type
        names.add("Alice");
        names.add("Bob");
        names.add(123);  // Oops! Compiler doesn't catch this
        
        // Every retrieval needs casting
        String first = (String) names.get(0);  // Works
        String third = (String) names.get(2);  // ClassCastException at runtime!
    }
}

// With Generics (Java 5+)
public class NewWay {
    public static void main(String[] args) {
        List<String> names = new ArrayList<String>();
        names.add("Alice");
        names.add("Bob");
        // names.add(123);  // ❌ Compile error! Type safety
        
        String first = names.get(0);  // No cast needed
    }
}
```

### Basic Generic Syntax

```java
// Generic Class
public class Box<T> {
    private T content;
    
    public void put(T item) {
        this.content = item;
    }
    
    public T get() {
        return content;
    }
}

// Usage
Box<String> stringBox = new Box<>();  // Diamond operator (Java 7+)
stringBox.put("Hello");
String value = stringBox.get();

Box<Integer> intBox = new Box<>();
intBox.put(42);
Integer number = intBox.get();

// Generic Method
public class Util {
    // Type parameter <T> declared before return type
    public static <T> T firstOrNull(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }
    
    // Multiple type parameters
    public static <K, V> Map<K, V> mapOf(K key, V value) {
        Map<K, V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}

// Usage
String first = Util.firstOrNull(List.of("A", "B", "C"));  // Type inferred
Integer num = Util.<Integer>firstOrNull(List.of(1, 2, 3));  // Explicit type
```

### Common Type Parameter Naming Conventions

```java
/*
 * Standard type parameter names (conventions, not rules):
 * 
 * T - Type (general)
 * E - Element (used in collections)
 * K - Key
 * V - Value
 * N - Number
 * S, U, V - 2nd, 3rd, 4th types
 */

public class Container<T> { }                    // General type
public interface List<E> { }                     // Element
public interface Map<K, V> { }                   // Key, Value
public class NumberBox<N extends Number> { }     // Number
public class Pair<T, U> { }                      // Two types
```

### Generic Interfaces

```java
// Generic interface
public interface Repository<T, ID> {
    T findById(ID id);
    List<T> findAll();
    void save(T entity);
    void delete(ID id);
}

// Implementation with concrete types
public class UserRepository implements Repository<User, Long> {
    @Override
    public User findById(Long id) {
        // Implementation
        return null;
    }
    
    @Override
    public List<User> findAll() {
        return new ArrayList<>();
    }
    
    @Override
    public void save(User entity) { }
    
    @Override
    public void delete(Long id) { }
}

// Implementation that remains generic
public class InMemoryRepository<T, ID> implements Repository<T, ID> {
    private Map<ID, T> storage = new HashMap<>();
    
    @Override
    public T findById(ID id) {
        return storage.get(id);
    }
    
    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }
    
    @Override
    public void save(T entity) {
        // Would need ID extraction strategy
    }
    
    @Override
    public void delete(ID id) {
        storage.remove(id);
    }
}
```

---

## Layer 2 — Working Developer Level

### Bounded Type Parameters

```java
// UPPER BOUND: T must be Number or its subclass
public class NumberContainer<T extends Number> {
    private T number;
    
    public NumberContainer(T number) {
        this.number = number;
    }
    
    // Can call Number methods on T
    public double getDoubleValue() {
        return number.doubleValue();
    }
}

NumberContainer<Integer> intContainer = new NumberContainer<>(42);
NumberContainer<Double> doubleContainer = new NumberContainer<>(3.14);
// NumberContainer<String> stringContainer = new NumberContainer<>("Hi"); // ❌ Error

// Multiple bounds: T must extend Class AND implement interfaces
public class MultiBound<T extends Number & Comparable<T>> {
    private T value;
    
    public MultiBound(T value) {
        this.value = value;
    }
    
    public boolean isGreaterThan(T other) {
        return value.compareTo(other) > 0;  // Comparable method
    }
    
    public double asDouble() {
        return value.doubleValue();  // Number method
    }
}

// Note: Class must come first, then interfaces
// <T extends Comparable<T> & Number>  // ❌ Wrong order
// <T extends Number & Comparable<T>>  // ✅ Correct
```

### Wildcards: ?, extends, and super

```java
/*
 * WILDCARD TYPES:
 * 
 * ?             - Unknown type
 * ? extends X   - X or any subtype of X (upper bound)
 * ? super X     - X or any supertype of X (lower bound)
 */

public class WildcardDemo {
    
    // Unbounded wildcard: accepts any type
    public static void printList(List<?> list) {
        for (Object item : list) {
            System.out.println(item);
        }
        // list.add("anything");  // ❌ Can't add (except null)
    }
    
    // Upper bounded: accepts Number or subclasses
    public static double sum(List<? extends Number> numbers) {
        double total = 0;
        for (Number n : numbers) {
            total += n.doubleValue();
        }
        // numbers.add(42);  // ❌ Can't add - we don't know exact type
        return total;
    }
    
    // Lower bounded: accepts Integer or superclasses
    public static void addIntegers(List<? super Integer> list) {
        list.add(1);   // ✅ Can add Integer
        list.add(2);
        list.add(3);
        // Integer x = list.get(0);  // ❌ Can only get Object
    }
    
    public static void main(String[] args) {
        List<Integer> integers = List.of(1, 2, 3);
        List<Double> doubles = List.of(1.1, 2.2, 3.3);
        List<Number> numbers = new ArrayList<>();
        List<Object> objects = new ArrayList<>();
        
        // printList works with any
        printList(integers);
        printList(doubles);
        
        // sum works with Number or subclasses
        sum(integers);  // ✅
        sum(doubles);   // ✅
        sum(numbers);   // ✅
        // sum(objects);  // ❌ Object is not Number
        
        // addIntegers works with Integer or superclasses
        addIntegers(numbers);  // ✅ Number is supertype of Integer
        addIntegers(objects);  // ✅ Object is supertype of Integer
        // addIntegers(doubles);  // ❌ Double is not supertype of Integer
    }
}
```

### PECS: Producer Extends, Consumer Super

```java
/*
 * PECS Rule (from Effective Java):
 * 
 * - Producer (read from it): use "extends"
 * - Consumer (write to it): use "super"
 * - Both: use exact type (no wildcards)
 */

public class PECSDemo {
    
    // PRODUCER: We READ from source (use extends)
    public static <T> void copy(
            List<? extends T> source,     // Producer: reading from
            List<? super T> destination   // Consumer: writing to
    ) {
        for (T item : source) {
            destination.add(item);
        }
    }
    
    public static void main(String[] args) {
        List<Integer> integers = new ArrayList<>(List.of(1, 2, 3));
        List<Number> numbers = new ArrayList<>();
        List<Object> objects = new ArrayList<>();
        
        copy(integers, numbers);  // Copy Integer to Number
        copy(integers, objects);  // Copy Integer to Object
        copy(numbers, objects);   // Copy Number to Object
    }
}

// Real-world example from Collections
public class Collections {
    // addAll: source produces elements (extends)
    public static <T> boolean addAll(Collection<? super T> c, T... elements) {
        // c consumes elements (super)
        boolean modified = false;
        for (T element : elements)
            if (c.add(element))
                modified = true;
        return modified;
    }
    
    // copy: src produces (extends), dest consumes (super)
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        // ...
    }
}
```

### Generic Constructors and Static Methods

```java
public class GenericConstructors {
    
    // Constructor can have its own type parameter
    private final Object value;
    
    public <T> GenericConstructors(T value) {
        this.value = value;
    }
    
    // Static methods can be generic even if class is not
    public static <T> GenericConstructors of(T value) {
        return new GenericConstructors(value);
    }
}

// Generic class with additional constructor type parameter
public class Wrapper<T> {
    private T wrapped;
    
    public Wrapper(T wrapped) {
        this.wrapped = wrapped;
    }
    
    // Additional type parameter for this constructor
    public <U extends T> Wrapper(U source, Function<U, T> converter) {
        this.wrapped = converter.apply(source);
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### Type Erasure: How Generics Really Work

```java
/*
 * TYPE ERASURE: At runtime, generic type information is removed
 * 
 * List<String> and List<Integer> are the same class at runtime!
 * 
 * Compiler does:
 * 1. Replaces type parameters with bounds (or Object if unbounded)
 * 2. Inserts casts where needed
 * 3. Generates bridge methods for polymorphism
 */

// What you write:
public class Box<T> {
    private T item;
    public void set(T item) { this.item = item; }
    public T get() { return item; }
}

// What bytecode becomes (after erasure):
public class Box {
    private Object item;
    public void set(Object item) { this.item = item; }
    public Object get() { return item; }
}

// With bounds:
public class NumberBox<T extends Number> {
    private T number;
    public T get() { return number; }
}

// Becomes:
public class NumberBox {
    private Number number;  // Erased to bound
    public Number get() { return number; }
}

// Implications of Type Erasure:
public class ErasureImplications {
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        
        // Same class at runtime
        System.out.println(strings.getClass() == integers.getClass());  // true
        
        // Cannot use instanceof with generics
        // if (strings instanceof List<String>) { }  // ❌ Error
        if (strings instanceof List<?>) { }  // ✅ OK
        
        // Cannot create generic arrays
        // T[] array = new T[10];  // ❌ Error
        
        // Cannot instantiate type parameters
        // T obj = new T();  // ❌ Error
        
        // Runtime type is erased
        List<String> list = new ArrayList<>();
        list.add("Hello");
        // At runtime, this is just ArrayList, no String info
    }
}
```

### Bridge Methods

```java
/*
 * BRIDGE METHODS: Compiler-generated methods for type-safe polymorphism
 * 
 * Problem: After erasure, method signatures change. 
 * How does overriding still work?
 */

// Before erasure:
interface Comparable<T> {
    int compareTo(T o);
}

class MyString implements Comparable<MyString> {
    @Override
    public int compareTo(MyString o) {
        return 0;
    }
}

// After erasure in interface:
interface Comparable {
    int compareTo(Object o);  // T becomes Object
}

// After erasure in class, compiler adds BRIDGE METHOD:
class MyString implements Comparable {
    // Your method (with specific type)
    public int compareTo(MyString o) {
        return 0;
    }
    
    // BRIDGE METHOD (compiler-generated)
    // This is what actually overrides the interface method
    public int compareTo(Object o) {
        return compareTo((MyString) o);  // Delegates with cast
    }
}

// You can see bridge methods with reflection
public class BridgeMethodDemo {
    public static void main(String[] args) {
        for (Method m : MyString.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " - isBridge: " + m.isBridge());
        }
        // Output:
        // compareTo - isBridge: false  (your method)
        // compareTo - isBridge: true   (compiler-generated)
    }
}
```

### Reification and Workarounds

```java
/*
 * REIFICATION: Having type information available at runtime
 * 
 * Reified types in Java:
 * - Primitives: int, boolean, etc.
 * - Non-generic classes: String, Integer
 * - Raw types: List, Map (without parameters)
 * - Unbound wildcards: List<?>
 * - Arrays: String[], int[]
 * 
 * Non-reified (erased):
 * - Generic types: List<String>, Map<String, Integer>
 * - Bounded wildcards: List<? extends Number>
 */

// Workaround 1: Pass Class token
public class TypedFactory<T> {
    private Class<T> type;
    
    public TypedFactory(Class<T> type) {
        this.type = type;
    }
    
    public T create() throws Exception {
        return type.getDeclaredConstructor().newInstance();
    }
    
    public T[] createArray(int size) {
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(type, size);
        return array;
    }
}

// Usage:
TypedFactory<String> factory = new TypedFactory<>(String.class);
String s = factory.create();
String[] arr = factory.createArray(10);

// Workaround 2: Type tokens with ParameterizedTypeReference (like in Spring)
abstract class TypeReference<T> {
    private final Type type;
    
    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }
    
    public Type getType() {
        return type;
    }
}

// Usage (anonymous subclass captures type)
TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {};
System.out.println(typeRef.getType());  // java.util.List<java.lang.String>
```

### Heap Pollution and Varargs

```java
/*
 * HEAP POLLUTION: When a variable of a parameterized type 
 * refers to an object that is NOT of that parameterized type
 */

public class HeapPollutionDemo {
    
    // Unsafe varargs: potential heap pollution
    @SafeVarargs  // Suppresses warning (use carefully!)
    public static <T> void unsafe(T... elements) {
        Object[] objArray = elements;  // Arrays are covariant
        objArray[0] = "String";        // Heap pollution!
        // If T is Integer, this corrupts the array
    }
    
    // Safe varargs: only reads from array
    @SafeVarargs
    public static <T> T getFirst(T... elements) {
        return elements.length > 0 ? elements[0] : null;
    }
    
    // When is @SafeVarargs appropriate?
    // 1. Method doesn't store anything in the varargs array
    // 2. Method doesn't pass the array to untrusted code
    
    public static void main(String[] args) {
        // This can cause ClassCastException later
        List<String> stringList = new ArrayList<>();
        stringList.add("Hello");
        
        List<Integer> intList = new ArrayList<>();
        
        // Mixing lists through raw types
        @SuppressWarnings("unchecked")
        List<String>[] arrayOfLists = new List[] { stringList, intList };
        
        // intList now masquerades as List<String>
        List<String> disguised = arrayOfLists[1];
        // disguised.add("World");  // Actually adds to intList!
        // Integer i = intList.get(0);  // ClassCastException!
    }
}
```

### Recursive Type Bounds

```java
/*
 * RECURSIVE TYPE BOUNDS: Type parameter refers to itself
 * Used for fluent APIs, comparability, etc.
 */

// Classic example: Comparable
public interface Comparable<T> {
    int compareTo(T o);
}

public class Person implements Comparable<Person> {
    private String name;
    
    @Override
    public int compareTo(Person other) {
        return this.name.compareTo(other.name);
    }
}

// Finding max with Comparable
public static <T extends Comparable<T>> T max(List<T> list) {
    if (list.isEmpty()) throw new IllegalArgumentException();
    T result = list.get(0);
    for (T t : list) {
        if (t.compareTo(result) > 0) result = t;
    }
    return result;
}

// Fluent builder pattern with recursive bounds
public abstract class Builder<T extends Builder<T>> {
    protected String name;
    protected int age;
    
    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }
    
    public T withName(String name) {
        this.name = name;
        return self();
    }
    
    public T withAge(int age) {
        this.age = age;
        return self();
    }
    
    public abstract Object build();
}

public class PersonBuilder extends Builder<PersonBuilder> {
    private String address;
    
    public PersonBuilder withAddress(String address) {
        this.address = address;
        return self();  // Returns PersonBuilder, not Builder
    }
    
    @Override
    public Person build() {
        return new Person(name, age, address);
    }
}

// Fluent chaining works correctly
Person person = new PersonBuilder()
    .withName("John")      // Returns PersonBuilder
    .withAge(30)           // Returns PersonBuilder  
    .withAddress("NYC")    // Returns PersonBuilder
    .build();
```

---

## Layer 4 — Interview Mastery

### Frequently Asked Interview Questions

**Q1: "What is type erasure and why does Java use it?"**

> "Type erasure is the process where generic type information is removed at compile time. The compiler replaces type parameters with their bounds (or Object if unbounded) and inserts necessary casts.
>
> Java uses type erasure for backward compatibility. When generics were added in Java 5, millions of lines of code already existed using raw types. Erasure allows generic code to interoperate with legacy code seamlessly. The bytecode for `List<String>` is identical to the old `List`, so the JVM didn't need changes.
>
> The tradeoff: no runtime type information for generics, which leads to limitations like no generic arrays, no `instanceof` with generics, and no `new T()`."

---

**Q2: "Explain PECS (Producer Extends, Consumer Super)"**

> "PECS is a mnemonic for wildcard bounds:
>
> - **Producer Extends**: If you're reading from a structure (it produces values), use `? extends T`. You can get T's out, but can't put anything in (except null).
>
> - **Consumer Super**: If you're writing to a structure (it consumes values), use `? super T`. You can put T's in, but only get Objects out.
>
> Example: `Collections.copy(List<? super T> dest, List<? extends T> src)`. Source produces elements (extends), destination consumes them (super).
>
> If you need both read and write, don't use wildcards—use the exact type parameter."

---

**Q3: "What's the difference between List<?> and List<Object>?"**

> "`List<?>` is a list of unknown type. I can read elements as Object, but I can't add anything (except null) because I don't know the actual type. It's the most flexible for accepting any list.
>
> `List<Object>` is specifically a list of Objects. I can add any object to it. But it's NOT a supertype of `List<String>` due to generic invariance—generics don't follow inheritance.
>
> Key insight: `List<String>` is NOT a subtype of `List<Object>`. But `List<String>` IS assignable to `List<?>`."

```java
List<String> strings = new ArrayList<>();
// List<Object> objects = strings;  // ❌ Compile error
List<?> unknown = strings;          // ✅ OK
```

---

**Q4: "Can you create a generic array? Why or why not?"**

> "You cannot create a generic array like `new T[10]` or `new List<String>[10]` because of type erasure and array covariance.
>
> Arrays are reified (retain type at runtime) and covariant (`String[]` is a `Object[]`). Generics are erased and invariant. These conflict:
>
> If we could do `List<String>[] array = new List<String>[10]`, the array at runtime would just be `List[]`. We could assign it to `Object[] obj = array`, then do `obj[0] = new ArrayList<Integer>()`. This would corrupt the array, and later code expecting `List<String>` would fail.
>
> Workaround: Use `List<List<String>>` instead of `List<String>[]`, or create `Object[]` and cast (with `@SuppressWarnings`)."

---

### Common Mistakes by Level

**Beginner Mistakes**:
- ❌ Using raw types (`List` instead of `List<String>`)
- ❌ Ignoring type safety warnings
- ❌ Not understanding that `List<Integer>` is not a `List<Number>`

**Intermediate Mistakes**:
- ❌ Overusing wildcards when specific types work
- ❌ Not applying PECS correctly
- ❌ Creating generic arrays unsafely

**Advanced Mistakes**:
- ❌ Ignoring heap pollution with varargs
- ❌ Overusing `@SuppressWarnings("unchecked")`
- ❌ Not understanding bridge methods when debugging

---

### Checkpoint: Generics

✅ You should now understand:
- [ ] Type parameters in classes, interfaces, and methods
- [ ] Bounded type parameters (`extends`)
- [ ] Wildcards (`?`, `? extends`, `? super`)
- [ ] PECS rule and when to use which wildcard
- [ ] Type erasure and its implications
- [ ] Why generic arrays aren't allowed
- [ ] Bridge methods and how polymorphism works with generics

**Mental Model**:
> Think of generics as a contract:
> - **At compile time**: Full type checking, catches errors early
> - **At runtime**: Types are erased, but safety is guaranteed by compile-time checks
> - `extends` = "at most this type" (upper limit)
> - `super` = "at least this type" (lower limit)

---

[← Back to Index](00-README.md) | [Previous: Collections](04-Collections.md) | [Next: Exceptions →](06-Exceptions.md)
