# Chapter 2: Syntax & Fundamentals

[← Back to Index](00-README.md) | [Previous: How Java Works](01-How-Java-Works.md) | [Next: OOP →](03-OOP.md)

---

## Why Fundamentals Matter Deeply

> **The Truth**: Most developers "know" Java syntax but don't understand it. 
> They can write code that works but can't explain why it works.

---

## Layer 1 — Beginner Foundation

### Your First Real Java Program (Dissected)

```java
// Every Java program starts with a class
// Class name MUST match filename (HelloWorld.java)
public class HelloWorld {
    
    // main() is the entry point - JVM looks for this exact signature
    // public: accessible from outside
    // static: can be called without creating an object
    // void: returns nothing
    // String[] args: command-line arguments
    public static void main(String[] args) {
        
        // System: a class in java.lang (auto-imported)
        // out: a static PrintStream object
        // println: method that prints and adds newline
        System.out.println("Hello, World!");
    }
}
```

### Primitive Data Types (The 8 Pillars)

Java has exactly 8 primitive types. Everything else is an object.

| Type | Size | Range | Default | Use Case |
|------|------|-------|---------|----------|
| `byte` | 1 byte | -128 to 127 | 0 | Memory-efficient arrays |
| `short` | 2 bytes | -32,768 to 32,767 | 0 | Rarely used |
| `int` | 4 bytes | -2.1B to 2.1B | 0 | Default for integers |
| `long` | 8 bytes | ±9.2 quintillion | 0L | Large numbers, timestamps |
| `float` | 4 bytes | ~7 decimal digits | 0.0f | Rarely used (precision issues) |
| `double` | 8 bytes | ~15 decimal digits | 0.0d | Default for decimals |
| `char` | 2 bytes | 0 to 65,535 | '\u0000' | Single Unicode character |
| `boolean` | ~1 bit* | true/false | false | Flags, conditions |

*JVM may use 1 byte or more for booleans internally

```java
public class PrimitiveDemo {
    public static void main(String[] args) {
        // Integer literals
        int decimal = 42;
        int hex = 0x2A;           // Same as 42
        int binary = 0b101010;    // Same as 42 (Java 7+)
        int withUnderscores = 1_000_000;  // Readability (Java 7+)
        
        // Long literals need 'L' suffix
        long bigNumber = 9_223_372_036_854_775_807L;
        
        // Floating point
        double precise = 3.141592653589793;
        float lessPrecise = 3.14159f;  // Need 'f' suffix
        
        // Characters
        char letter = 'A';
        char unicode = '\u0041';   // Also 'A'
        char tab = '\t';
        
        // Boolean
        boolean isJavaAwesome = true;
    }
}
```

### Variables: Declaration, Initialization, Scope

```java
public class VariableScopes {
    // Instance variable: belongs to object, default initialized
    private int instanceVar;  // Defaults to 0
    
    // Static variable: belongs to class, shared across instances
    private static int classVar;  // Defaults to 0
    
    public void demonstrateScopes() {
        // Local variable: MUST be initialized before use
        int localVar;
        // System.out.println(localVar);  // ❌ Compile error!
        
        localVar = 10;  // Now initialized
        System.out.println(localVar);  // ✅ Works
        
        // Block scope
        if (true) {
            int blockVar = 20;
            System.out.println(blockVar);  // ✅ Accessible
        }
        // System.out.println(blockVar);  // ❌ Out of scope
    }
    
    public void methodWithParameter(int paramVar) {
        // paramVar is local to this method
        System.out.println(paramVar);
    }
}
```

### Type Conversion (Casting)

```java
public class TypeConversion {
    public static void main(String[] args) {
        // WIDENING (implicit) - Safe, no data loss
        // byte → short → int → long → float → double
        int myInt = 100;
        long myLong = myInt;      // Automatic
        double myDouble = myLong; // Automatic
        
        // NARROWING (explicit) - May lose data
        double d = 9.78;
        int i = (int) d;  // i = 9 (truncated, not rounded!)
        
        // Watch out for overflow
        int big = 130;
        byte b = (byte) big;  // b = -126 (overflow wraps around!)
        
        // char to int (widening)
        char c = 'A';
        int ascii = c;  // 65
        
        // int to char (narrowing)
        int code = 66;
        char letter = (char) code;  // 'B'
    }
}
```

### Operators Explained

```java
public class OperatorsDemo {
    public static void main(String[] args) {
        // ARITHMETIC
        int a = 10, b = 3;
        System.out.println(a + b);   // 13
        System.out.println(a - b);   // 7
        System.out.println(a * b);   // 30
        System.out.println(a / b);   // 3 (integer division!)
        System.out.println(a % b);   // 1 (modulo - remainder)
        
        // Integer division truncates toward zero
        System.out.println(-7 / 2);  // -3 (not -4)
        
        // For decimal division, at least one operand must be double
        System.out.println(10.0 / 3); // 3.333...
        System.out.println((double) a / b); // 3.333...
        
        // INCREMENT/DECREMENT
        int x = 5;
        System.out.println(x++);  // Prints 5, THEN increments (post)
        System.out.println(x);    // Now 6
        System.out.println(++x);  // Increments FIRST, then prints 7 (pre)
        
        // COMPARISON (return boolean)
        System.out.println(5 == 5);  // true
        System.out.println(5 != 3);  // true
        System.out.println(5 > 3);   // true
        System.out.println(5 >= 5);  // true
        
        // LOGICAL
        boolean t = true, f = false;
        System.out.println(t && f);  // false (AND - short-circuits)
        System.out.println(t || f);  // true (OR - short-circuits)
        System.out.println(!t);      // false (NOT)
        
        // Short-circuit example
        int num = 0;
        if (num != 0 && 10/num > 1) {  // Safe! Second part not evaluated
            System.out.println("Won't reach here");
        }
        
        // BITWISE (operate on bits)
        System.out.println(5 & 3);   // 1 (0101 & 0011 = 0001)
        System.out.println(5 | 3);   // 7 (0101 | 0011 = 0111)
        System.out.println(5 ^ 3);   // 6 (0101 ^ 0011 = 0110)
        System.out.println(~5);      // -6 (inverts all bits)
        System.out.println(5 << 1);  // 10 (shift left = multiply by 2)
        System.out.println(5 >> 1);  // 2 (shift right = divide by 2)
        
        // TERNARY
        int age = 20;
        String status = age >= 18 ? "Adult" : "Minor";
    }
}
```

### Control Flow Structures

```java
public class ControlFlow {
    
    public static void main(String[] args) {
        // IF-ELSE-IF
        int score = 85;
        if (score >= 90) {
            System.out.println("A");
        } else if (score >= 80) {
            System.out.println("B");  // This executes
        } else if (score >= 70) {
            System.out.println("C");
        } else {
            System.out.println("F");
        }
        
        // SWITCH (Traditional)
        int day = 3;
        switch (day) {
            case 1:
                System.out.println("Monday");
                break;  // Required! Otherwise falls through
            case 2:
                System.out.println("Tuesday");
                break;
            case 3:
                System.out.println("Wednesday");
                break;
            default:
                System.out.println("Other day");
        }
        
        // SWITCH EXPRESSION (Java 14+) - Modern style
        String dayName = switch (day) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4, 5 -> "Work day";  // Multiple cases
            default -> "Weekend";
        };
        
        // FOR LOOP
        for (int i = 0; i < 5; i++) {
            System.out.println(i);  // 0, 1, 2, 3, 4
        }
        
        // ENHANCED FOR (for-each)
        int[] numbers = {1, 2, 3, 4, 5};
        for (int num : numbers) {
            System.out.println(num);
        }
        
        // WHILE
        int count = 0;
        while (count < 3) {
            System.out.println(count);
            count++;
        }
        
        // DO-WHILE (executes at least once)
        int x = 0;
        do {
            System.out.println(x);
        } while (x > 0);  // Condition false, but executes once
        
        // BREAK and CONTINUE
        for (int i = 0; i < 10; i++) {
            if (i == 3) continue;  // Skip 3
            if (i == 7) break;     // Stop at 7
            System.out.println(i);  // Prints: 0,1,2,4,5,6
        }
        
        // LABELED BREAK (breaking outer loops)
        outer:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) {
                    break outer;  // Exits both loops
                }
                System.out.println(i + "," + j);
            }
        }
    }
}
```

---

## Layer 2 — Working Developer Level

### Strings: The Most Important Reference Type

```java
public class StringDeepDive {
    public static void main(String[] args) {
        // String Immutability - CRITICAL CONCEPT
        String s1 = "Hello";
        String s2 = s1;
        s1 = s1 + " World";  // Creates NEW string
        
        System.out.println(s1);  // "Hello World"
        System.out.println(s2);  // "Hello" (unchanged!)
        
        // String Pool (Interning)
        String a = "Java";      // Goes to string pool
        String b = "Java";      // Reuses same pool reference
        String c = new String("Java");  // Creates new object on heap
        
        System.out.println(a == b);      // true (same reference)
        System.out.println(a == c);      // false (different objects)
        System.out.println(a.equals(c)); // true (same content)
        
        // ALWAYS use equals() for string comparison!
        
        // Essential String Methods
        String text = "  Hello World  ";
        
        text.length();                  // 15
        text.charAt(2);                 // 'H'
        text.substring(2, 7);           // "Hello"
        text.indexOf("World");          // 8
        text.contains("World");         // true
        text.startsWith("  He");        // true
        text.endsWith("ld  ");          // true
        text.toLowerCase();             // "  hello world  "
        text.toUpperCase();             // "  HELLO WORLD  "
        text.trim();                    // "Hello World"
        text.strip();                   // "Hello World" (Java 11+, Unicode-aware)
        text.replace("World", "Java");  // "  Hello Java  "
        text.split(" ");                // ["", "", "Hello", "World", "", ""]
        text.isBlank();                 // false (Java 11+)
        text.isEmpty();                 // false
        
        // String Formatting
        String formatted = String.format("Name: %s, Age: %d", "John", 25);
        // Java 15+ text blocks
        String json = """
            {
                "name": "John",
                "age": 25
            }
            """;
        
        // Efficient String Concatenation
        // BAD for loops (creates many objects):
        String result = "";
        for (int i = 0; i < 1000; i++) {
            result += i;  // Creates new String each iteration!
        }
        
        // GOOD (use StringBuilder):
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append(i);
        }
        String efficient = sb.toString();
    }
}
```

### Arrays: Fixed-Size Collections

```java
public class ArraysDeep {
    public static void main(String[] args) {
        // Declaration and Initialization
        int[] numbers = new int[5];           // [0, 0, 0, 0, 0]
        int[] initialized = {1, 2, 3, 4, 5};  // Shorthand
        
        // Arrays are objects, stored on heap
        System.out.println(numbers.getClass().getName()); // [I
        
        // Accessing elements (0-indexed)
        numbers[0] = 10;
        int first = numbers[0];
        int length = numbers.length;  // 5 (note: property, not method)
        
        // ArrayIndexOutOfBoundsException
        // numbers[5] = 100;  // Runtime error!
        
        // Multidimensional Arrays
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        System.out.println(matrix[1][2]);  // 6
        
        // Jagged Arrays (rows of different lengths)
        int[][] jagged = new int[3][];
        jagged[0] = new int[2];
        jagged[1] = new int[4];
        jagged[2] = new int[1];
        
        // Array copying
        int[] copy1 = numbers.clone();  // Shallow copy
        int[] copy2 = Arrays.copyOf(numbers, 10);  // With new length
        int[] copy3 = Arrays.copyOfRange(numbers, 1, 4);  // Subset
        
        // Array utilities (java.util.Arrays)
        Arrays.sort(initialized);                    // In-place sort
        int index = Arrays.binarySearch(initialized, 3);  // Must be sorted
        boolean equal = Arrays.equals(numbers, copy1);
        Arrays.fill(numbers, 42);                    // Fill with value
        String str = Arrays.toString(numbers);       // [42, 42, 42, 42, 42]
        String deepStr = Arrays.deepToString(matrix); // For multi-dim
    }
}
```

### var (Local Variable Type Inference - Java 10+)

```java
public class VarKeyword {
    // var ONLY works for local variables with initializers
    // private var field = 10;  // ❌ Not allowed for fields
    
    public void demonstrate() {
        // ✅ Good uses of var
        var name = "John";           // Compiler infers String
        var numbers = List.of(1, 2, 3);  // List<Integer>
        var map = new HashMap<String, List<Integer>>();  // Reduces verbosity
        
        // ✅ Great for complex generic types
        var entries = map.entrySet().iterator();  // Iterator<Entry<String,List<Integer>>>
        
        // ❌ When NOT to use var
        var result = processData();  // Type not obvious from name
        var x = 0;                   // Could be int, long, double?
        
        // var must be initialized
        // var uninitialized;  // ❌ Compile error
        
        // var cannot be null without cast
        // var nothing = null;  // ❌ Can't infer type
        var nothing = (String) null;  // ✅ Works but pointless
    }
    
    // Best Practice: Use var when the type is obvious
    // from the right-hand side or would be extremely verbose
}
```

---

## Layer 3 — Advanced Engineering Depth

### How Strings Work Internally

```java
/*
 * String Internal Structure (Java 9+):
 * 
 * Before Java 9: char[] (2 bytes per char)
 * After Java 9: byte[] + encoding flag (Compact Strings)
 * 
 * Compact Strings:
 * - Latin-1 strings use 1 byte per char
 * - UTF-16 strings use 2 bytes per char
 * - Coder field indicates encoding (LATIN1=0, UTF16=1)
 * - Significant memory savings for ASCII-heavy applications
 */

public class StringInternals {
    public static void main(String[] args) {
        // String Pool Mechanics
        String s1 = "Hello";  // Pool lookup or creation
        String s2 = "Hello";  // Pool hit
        String s3 = new String("Hello");  // Bypasses pool
        String s4 = s3.intern();  // Explicitly adds to pool
        
        System.out.println(s1 == s2);  // true
        System.out.println(s1 == s3);  // false
        System.out.println(s1 == s4);  // true (now points to pool)
        
        // Memory implications
        String concat = "Hel" + "lo";  // Compile-time constant folding
        System.out.println(s1 == concat);  // true!
        
        String dynamic = "Hel";
        String concat2 = dynamic + "lo";  // Runtime concatenation
        System.out.println(s1 == concat2);  // false (new object)
        
        // StringBuilder vs StringBuffer
        // StringBuilder: Not thread-safe, faster (use 99% of time)
        // StringBuffer: Thread-safe, synchronized methods (legacy)
        
        // Understanding StringBuilder capacity
        StringBuilder sb = new StringBuilder();  // Default capacity: 16
        sb.append("Hello World!");  // length=12, capacity=16
        sb.append(" More text here");  // Exceeds capacity, grows
        
        // Growth strategy: (oldCapacity * 2) + 2
        // 16 -> 34 -> 70 -> 142 -> ...
    }
}
```

### Numeric Edge Cases and Gotchas

```java
public class NumericEdgeCases {
    public static void main(String[] args) {
        // INTEGER OVERFLOW (Silent!)
        int max = Integer.MAX_VALUE;  // 2,147,483,647
        int overflow = max + 1;
        System.out.println(overflow);  // -2,147,483,648 (wraps around!)
        
        // Safe arithmetic (Java 8+)
        try {
            int safe = Math.addExact(max, 1);  // Throws ArithmeticException
        } catch (ArithmeticException e) {
            System.out.println("Overflow detected!");
        }
        
        // FLOATING POINT IMPRECISION
        double result = 0.1 + 0.2;
        System.out.println(result);  // 0.30000000000000004 (!)
        System.out.println(0.1 + 0.2 == 0.3);  // false!
        
        // For precise decimals (money!), use BigDecimal
        BigDecimal a = new BigDecimal("0.1");
        BigDecimal b = new BigDecimal("0.2");
        BigDecimal sum = a.add(b);
        System.out.println(sum);  // 0.3 (exact)
        
        // SPECIAL FLOAT VALUES
        double posInf = 1.0 / 0.0;      // Infinity
        double negInf = -1.0 / 0.0;     // -Infinity
        double nan = 0.0 / 0.0;         // NaN (Not a Number)
        
        System.out.println(posInf > 1000000);  // true
        System.out.println(nan == nan);         // false! NaN != anything
        System.out.println(Double.isNaN(nan));  // Use this instead
        
        // INTEGER DIVISION TRUNCATION
        System.out.println(5 / 2);     // 2 (not 2.5!)
        System.out.println(5.0 / 2);   // 2.5
        System.out.println((double) 5 / 2);  // 2.5
        
        // NEGATIVE MODULO
        System.out.println(-7 % 3);    // -1 (sign follows dividend in Java)
        System.out.println(Math.floorMod(-7, 3));  // 2 (Python-style)
    }
}
```

### Pass by Value: The Definitive Explanation

```java
/*
 * JAVA IS ALWAYS PASS BY VALUE
 * 
 * But what's passed differs:
 * - Primitives: The actual value is copied
 * - Objects: The reference (pointer) is copied
 * 
 * This means:
 * - You CANNOT change what a reference points to from inside a method
 * - You CAN change the object's internal state through the reference copy
 */

public class PassByValueDeep {
    
    public static void main(String[] args) {
        // Primitive example
        int x = 10;
        modifyPrimitive(x);
        System.out.println(x);  // Still 10 (copy was modified)
        
        // Object example
        StringBuilder sb = new StringBuilder("Hello");
        modifyObject(sb);
        System.out.println(sb);  // "Hello World" (object was mutated)
        
        // Reference reassignment example
        StringBuilder sb2 = new StringBuilder("Original");
        reassignReference(sb2);
        System.out.println(sb2);  // Still "Original" (reference copy was reassigned)
    }
    
    static void modifyPrimitive(int num) {
        num = 100;  // Modifies local copy only
    }
    
    static void modifyObject(StringBuilder s) {
        s.append(" World");  // Modifies the actual object on heap
    }
    
    static void reassignReference(StringBuilder s) {
        s = new StringBuilder("New");  // Reassigns local reference copy
        // Original reference in main() unchanged
    }
}
```

**Memory Visualization**:

```
BEFORE modifyObject(sb):

main() stack:                 Heap:
┌─────────────────┐          ┌──────────────────────┐
│ sb = 0x1000     │─────────►│ StringBuilder        │
└─────────────────┘          │ value = "Hello"      │
                             └──────────────────────┘

DURING modifyObject(sb):

main() stack:                 Heap:
┌─────────────────┐          ┌──────────────────────┐
│ sb = 0x1000     │─────────►│ StringBuilder        │
└─────────────────┘          │ value = "Hello World"│◄─┐
                             └──────────────────────┘  │
modifyObject() stack:                                   │
┌─────────────────┐                                    │
│ s = 0x1000      │────────────────────────────────────┘
└─────────────────┘           (Same object!)

DURING reassignReference(sb):

main() stack:                 Heap:
┌─────────────────┐          ┌──────────────────────┐
│ sb2 = 0x2000    │─────────►│ StringBuilder        │
└─────────────────┘          │ value = "Original"   │
                             └──────────────────────┘
reassignReference() stack:    ┌──────────────────────┐
┌─────────────────┐          │ StringBuilder        │
│ s = 0x3000      │─────────►│ value = "New"        │
└─────────────────┘          └──────────────────────┘
(Reference copy points        (New object, never
 to NEW object)               accessible from main)
```

---

## Layer 4 — Interview Mastery

### Frequently Asked Interview Questions

**Q1: "Why are Strings immutable in Java?"**

**Senior Answer**:
> "Strings are immutable for several critical reasons:
> 
> 1. **Security**: Strings are used for class loading, network connections, file paths. If mutable, malicious code could change a 'filename' after security checks.
> 
> 2. **Thread Safety**: Immutable objects are inherently thread-safe. Multiple threads can share Strings without synchronization.
> 
> 3. **String Pool**: Immutability enables the String Pool optimization. If Strings were mutable, one reference changing the content would affect all references.
> 
> 4. **Hash Code Caching**: String caches its hashCode. Since it can't change, this is safe and improves HashMap performance.
> 
> 5. **Architectural Simplicity**: Pass a String to any method without defensive copying."

---

**Q2: "Explain the difference between == and equals()"**

> "For primitives, `==` compares values directly.
> 
> For objects:
> - `==` compares **references** (memory addresses)
> - `equals()` compares **content** (logical equality)
> 
> By default, `Object.equals()` uses `==`. Classes like String override it to compare characters. When overriding `equals()`, you must also override `hashCode()` to maintain the contract: equal objects must have equal hash codes."

---

**Q3: "What's the output?"**

```java
String s1 = "Java";
String s2 = "Java";
String s3 = new String("Java");
String s4 = s3.intern();

System.out.println(s1 == s2);      // ?
System.out.println(s1 == s3);      // ?
System.out.println(s1 == s4);      // ?
System.out.println(s1.equals(s3)); // ?
```

> "Output: `true`, `false`, `true`, `true`
> 
> - s1 and s2 point to same pooled String (literal)
> - s3 creates new object on heap (bypasses pool)
> - s4 gets the pooled reference via intern()
> - equals() always compares content"

---

**Q4: "Why can't we use primitives with generics?"**

> "Generics use type erasure - at runtime, `List<Integer>` becomes `List<Object>`. Primitives don't inherit from Object, so they can't be used directly. Java uses wrapper classes (Integer, Boolean, etc.) as a workaround. Autoboxing/unboxing handles conversion automatically but has performance overhead. Project Valhalla aims to bring value types and primitive generics to Java."

---

### Common Mistakes by Level

**Beginner Mistakes**:
- ❌ Using `==` for String comparison
- ❌ Not initializing local variables
- ❌ Forgetting `break` in switch statements
- ❌ Integer division when expecting decimal result

**Intermediate Mistakes**:
- ❌ String concatenation in loops (use StringBuilder)
- ❌ Not understanding autoboxing overhead
- ❌ Comparing wrapper objects with `==`
- ❌ Ignoring floating-point precision issues for money

**Advanced Mistakes**:
- ❌ Not considering String pool memory implications
- ❌ Excessive String interning
- ❌ Using float when double precision needed
- ❌ Not using Math.addExact for critical calculations

---

### Checkpoint: Syntax & Fundamentals

✅ You should now understand:
- [ ] The 8 primitive types and their characteristics
- [ ] String immutability and the String pool
- [ ] Why `==` vs `equals()` matters
- [ ] Pass by value for primitives vs references
- [ ] var keyword usage and limitations
- [ ] Common numeric edge cases and gotchas

**Mental Model for Strings**:
> Think of Strings as books in a library (String Pool):
> - When you need "Java", check if the library has it
> - If yes, you get a reference to that book (everyone shares it)
> - `new String()` is like buying your own copy (not from library)
> - `intern()` is donating your copy to the library

---

[← Back to Index](00-README.md) | [Previous: How Java Works](01-How-Java-Works.md) | [Next: OOP →](03-OOP.md)
