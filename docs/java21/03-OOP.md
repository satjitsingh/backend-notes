# Chapter 3: Object-Oriented Programming

[← Back to Index](00-README.md) | [Previous: Syntax & Fundamentals](02-Syntax-Fundamentals.md) | [Next: Collections →](04-Collections.md)

---

## Why OOP Mastery is Non-Negotiable

> **Reality Check**: OOP is the foundation of almost every Java interview. 
> Poor OOP understanding is the #1 reason candidates fail at top companies.

### The Stakes Are High

Object-Oriented Programming isn't just a programming paradigm—it's the **language** that software engineers use to communicate. When you discuss code in interviews, code reviews, or architecture meetings, you'll use OOP terminology constantly:

- "This class violates the Single Responsibility Principle"
- "We should use composition instead of inheritance here"
- "Let's extract an interface to decouple these modules"
- "This method should be polymorphic"

If you don't deeply understand OOP, you can't participate in these conversations effectively. **This chapter goes extremely deep** because OOP is where junior developers remain stuck for years while thinking they understand it.

### What You'll Learn

By the end of this chapter, you'll understand:
- Why OOP was invented and what problems it solves
- The four pillars and how they work together
- When to use inheritance vs composition (a critical design decision)
- How to implement equals(), hashCode(), and toString() correctly
- Modern Java features: Records, Sealed Classes, Pattern Matching
- How to answer 15+ common OOP interview questions with senior-level depth

---

## Layer 1 — Beginner Foundation

### The Philosophy: Why Do Objects Exist?

#### The World Before OOP

In the 1970s-80s, most programs were written in **procedural** style (like C):

```
PROCEDURAL APPROACH:
- Data structures (structs) were just passive containers
- Functions were separate from data
- Any function could modify any data it could access
- Programs were organized as sequences of procedure calls
```

**The Problems**:

1. **Spaghetti Code**: As programs grew, functions became interconnected in unpredictable ways. Changing one function broke others.

2. **No Data Protection**: Any part of the code could modify any data. A bug anywhere could corrupt data everywhere.

3. **Poor Reusability**: Functions were tightly coupled to specific data structures. Reusing code meant copy-pasting.

4. **Difficult Maintenance**: Understanding code required tracing through all the functions that might touch a piece of data.

#### The OOP Insight

OOP's revolutionary idea: **Bundle data WITH the functions that operate on it**.

Think of it this way:

```
PROCEDURAL: 
  Data lives here: [name, age, salary]
  Functions live there: calculateTax(), giveRaise(), printInfo()
  Any function can touch any data

OOP:
  Employee object = {
    Data: [name, age, salary]
    Behavior: calculateTax(), giveRaise(), printInfo()
  }
  Data is protected, only this object's methods can modify it
```

**Real-World Analogy**: Think of a smartphone.

- **Procedural**: The battery, screen, processor, and apps are all separate. Anyone can rewire anything. To add a feature, you might need to modify the battery, screen, AND processor.

- **OOP**: The phone is a single unit. You interact with it through defined interfaces (buttons, touchscreen). The internal components are hidden and protected. To add an app, you don't touch the hardware—you use the defined app interface.

### The Four Pillars of OOP

These four concepts are the foundation of object-oriented design. Every OOP interview will test your understanding of these.

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        THE FOUR PILLARS OF OOP                              │
├───────────────────┬───────────────────┬────────────────┬───────────────────┤
│   ENCAPSULATION   │    INHERITANCE    │  POLYMORPHISM  │    ABSTRACTION    │
├───────────────────┼───────────────────┼────────────────┼───────────────────┤
│                   │                   │                │                   │
│  "Hide the HOW,   │  "Build on what   │  "One name,    │  "Focus on WHAT,  │
│   expose the      │   exists. Don't   │   many forms.  │   not HOW."       │
│   WHAT."          │   reinvent."      │   Same action, │                   │
│                   │                   │   different    │                   │
│                   │                   │   behavior."   │                   │
│                   │                   │                │                   │
├───────────────────┼───────────────────┼────────────────┼───────────────────┤
│  • private fields │  • extends        │  • Overloading │  • abstract class │
│  • public methods │  • super          │  • Overriding  │  • interface      │
│  • getters/setters│  • method override│  • late binding│  • hide complexity│
│                   │                   │                │                   │
├───────────────────┼───────────────────┼────────────────┼───────────────────┤
│                   │                   │                │                   │
│  BENEFIT:         │  BENEFIT:         │  BENEFIT:      │  BENEFIT:         │
│  Protection,      │  Code reuse,      │  Flexibility,  │  Simplicity,      │
│  Validation,      │  Organization     │  Extensibility │  Focus            │
│  Flexibility      │                   │                │                   │
│                   │                   │                │                   │
└───────────────────┴───────────────────┴────────────────┴───────────────────┘
```

Let's explore each one in depth.

---

### Pillar 1: Encapsulation — The Art of Information Hiding

**What is Encapsulation?**

Encapsulation means:
1. Bundling data (fields) and methods that operate on that data into a single unit (class)
2. Restricting direct access to some of the object's components
3. Providing controlled access through public methods

**The Capsule Analogy**:

Think of a medicine capsule. The medicine (data) is inside, protected by the shell. You don't need to know the chemical composition—you just take the capsule. The capsule:
- Protects the medicine from contamination (data protection)
- Releases medicine in a controlled way (controlled access)
- Hides complexity (you don't mix chemicals yourself)

**Why Encapsulation Matters**:

```java
// ══════════════════════════════════════════════════════════════════════════
// ❌ WITHOUT ENCAPSULATION: Disaster waiting to happen
// ══════════════════════════════════════════════════════════════════════════

public class BadBankAccount {
    public double balance;  // Anyone can access and modify directly!
    public String accountNumber;
}

// Usage - anything goes!
BadBankAccount account = new BadBankAccount();
account.balance = -5000;     // Negative balance? Sure!
account.balance = Double.NaN; // Not a number? Why not!
account.accountNumber = "";   // Empty account number? Fine!

// No validation, no protection, no history, no control
// If balance becomes wrong, which part of the code did it? Good luck finding out!
```

```java
// ══════════════════════════════════════════════════════════════════════════
// ✅ WITH ENCAPSULATION: Safe, controlled, maintainable
// ══════════════════════════════════════════════════════════════════════════

public class BankAccount {
    // Private = only this class can access directly
    private double balance;
    private final String accountNumber;  // final = cannot change after creation
    private final List<String> transactionHistory;
    
    // Constructor: The only way to create an account
    public BankAccount(String accountNumber, double initialDeposit) {
        // VALIDATION at the point of creation
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be empty");
        }
        if (initialDeposit < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }
        
        this.accountNumber = accountNumber;
        this.balance = initialDeposit;
        this.transactionHistory = new ArrayList<>();
        this.transactionHistory.add("Account opened with $" + initialDeposit);
    }
    
    // GETTER: Read-only access to balance
    public double getBalance() {
        return balance;
    }
    
    // No setBalance()! Balance can only change through deposit/withdraw
    
    // CONTROLLED MODIFICATION with validation and logging
    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance += amount;
        transactionHistory.add("Deposit: $" + amount + " | New Balance: $" + balance);
    }
    
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount > balance) {
            throw new IllegalStateException("Insufficient funds. Balance: $" + balance);
        }
        balance -= amount;
        transactionHistory.add("Withdrawal: $" + amount + " | New Balance: $" + balance);
    }
    
    // DEFENSIVE COPY: Return a copy so external code can't modify our list
    public List<String> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);  // Copy, not original!
    }
}
```

**The Five Benefits of Encapsulation**:

| Benefit | Explanation | Example |
|---------|-------------|---------|
| **1. Validation** | Ensure data is always valid | Can't set negative balance |
| **2. Flexibility** | Change internal implementation without breaking external code | Switch from `double` to `BigDecimal` internally |
| **3. Debugging** | Add logging/breakpoints in getters/setters | Track who changed a value |
| **4. Thread Safety** | Add synchronization to methods | Make operations atomic |
| **5. Invariant Protection** | Maintain relationships between fields | Balance must equal sum of transactions |

**Access Modifiers: The Tools of Encapsulation**

Java provides four access levels to control visibility:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ACCESS MODIFIERS                                     │
├─────────────────┬───────────┬───────────┬────────────┬─────────────────────┤
│    Modifier     │ Same Class│ Same Pkg  │ Subclass   │ Other Packages      │
│                 │           │           │(diff pkg)  │                     │
├─────────────────┼───────────┼───────────┼────────────┼─────────────────────┤
│   private       │    ✅     │    ❌      │    ❌      │        ❌           │
│   (default)     │    ✅     │    ✅      │    ❌      │        ❌           │
│   protected     │    ✅     │    ✅      │    ✅      │        ❌           │
│   public        │    ✅     │    ✅      │    ✅      │        ✅           │
├─────────────────┴───────────┴───────────┴────────────┴─────────────────────┤
│                                                                              │
│  RULE OF THUMB: Start with private, widen only when necessary.             │
│                                                                              │
│  • Fields: Almost always private                                            │
│  • Methods: Public for API, private for internal helpers                    │
│  • Classes: Public for API, package-private for internal                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### Pillar 2: Inheritance — Building on Existing Code

**What is Inheritance?**

Inheritance allows a class (child/subclass) to inherit fields and methods from another class (parent/superclass). The child "IS-A" type of the parent.

**The Family Analogy**:

Just as children inherit traits from parents (eye color, height potential), classes inherit characteristics from parent classes. But children can also have their own unique traits and even do things differently than their parents.

```
                      ┌───────────┐
                      │  Animal   │  ← Parent (Superclass)
                      │           │
                      │ • name    │
                      │ • eat()   │
                      │ • sleep() │
                      └─────┬─────┘
                            │ extends
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        ┌───────────┐ ┌───────────┐ ┌───────────┐
        │    Dog    │ │    Cat    │ │   Bird    │  ← Children (Subclasses)
        │           │ │           │ │           │
        │ • breed   │ │ • indoor? │ │ • canFly  │  ← Own unique fields
        │ • bark()  │ │ • meow()  │ │ • fly()   │  ← Own unique methods
        │ • eat() ↺ │ │ • eat() ↺ │ │ • eat() ↺ │  ← Override parent's eat()
        └───────────┘ └───────────┘ └───────────┘
```

**The IS-A Test**:

Before using inheritance, ask: "Is [Child] a [Parent]?"

- ✅ "A Dog IS-A Animal" → Inheritance makes sense
- ✅ "A Car IS-A Vehicle" → Good inheritance
- ❌ "A Car IS-A Engine" → NO! A Car HAS-A Engine → Use composition

```java
// ══════════════════════════════════════════════════════════════════════════
// BASE CLASS (Superclass)
// ══════════════════════════════════════════════════════════════════════════

public class Animal {
    // Protected: accessible to subclasses
    protected String name;
    protected int age;
    
    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // Methods that all animals share
    public void eat() {
        System.out.println(name + " is eating.");
    }
    
    public void sleep() {
        System.out.println(name + " is sleeping.");
    }
    
    public String getDescription() {
        return name + " (Age: " + age + ")";
    }
}

// ══════════════════════════════════════════════════════════════════════════
// SUBCLASS: Inherits from Animal
// ══════════════════════════════════════════════════════════════════════════

public class Dog extends Animal {
    // Additional field specific to Dog
    private String breed;
    
    public Dog(String name, int age, String breed) {
        // super() MUST be the first statement in constructor
        // It calls the parent's constructor
        super(name, age);
        this.breed = breed;
    }
    
    // NEW METHOD: Only dogs have this
    public void bark() {
        System.out.println(name + " says: Woof! Woof!");
    }
    
    public void fetch() {
        System.out.println(name + " is fetching the ball!");
    }
    
    // OVERRIDE: Same method signature, different implementation
    @Override
    public void eat() {
        // Can call parent's version with super
        // super.eat();  // Would print "X is eating."
        System.out.println(name + " is eating dog food enthusiastically!");
    }
    
    // OVERRIDE: Extend parent's behavior
    @Override
    public String getDescription() {
        // Use parent's implementation and add to it
        return super.getDescription() + ", Breed: " + breed;
    }
}
```

**Key Inheritance Rules**:

| Rule | Explanation |
|------|-------------|
| **1. Single inheritance only** | A class can extend only ONE class (unlike interfaces) |
| **2. super() must be first** | If you call parent constructor, it must be the first line |
| **3. All classes extend Object** | If no explicit extends, class implicitly extends java.lang.Object |
| **4. Constructors aren't inherited** | Subclass must define its own constructors |
| **5. private members aren't inherited** | Subclass can't access parent's private fields directly |
| **6. @Override is optional but recommended** | Compiler catches mistakes if you use it |

**When NOT to Use Inheritance** (Very Important!):

```java
// ❌ BAD: Misusing inheritance for code reuse

// Someone thinks: "Stack needs the methods of ArrayList, let me extend it"
public class Stack<E> extends ArrayList<E> {
    public void push(E item) {
        add(item);
    }
    
    public E pop() {
        return remove(size() - 1);
    }
}

// PROBLEM: Stack now has ALL ArrayList methods!
Stack<String> stack = new Stack<>();
stack.push("A");
stack.push("B");
stack.add(0, "INSERTED");  // 😱 This breaks stack semantics!
stack.remove(1);            // 😱 This too!

// A Stack IS-NOT-A ArrayList. It HAS similar storage needs.
// Use COMPOSITION instead!
```

```java
// ✅ GOOD: Composition - Stack HAS-A list (doesn't expose it)

public class Stack<E> {
    private final List<E> elements = new ArrayList<>();  // HAS-A
    
    public void push(E item) {
        elements.add(item);
    }
    
    public E pop() {
        if (elements.isEmpty()) {
            throw new EmptyStackException();
        }
        return elements.remove(elements.size() - 1);
    }
    
    public E peek() {
        if (elements.isEmpty()) {
            throw new EmptyStackException();
        }
        return elements.get(elements.size() - 1);
    }
    
    public boolean isEmpty() {
        return elements.isEmpty();
    }
    
    // No add(), remove(), get() exposed - only stack operations!
}
```

---

### Pillar 3: Polymorphism — One Interface, Many Forms

**What is Polymorphism?**

Polymorphism (Greek: "many forms") means that the same method call can behave differently depending on the actual object type. It's the ability to treat objects of different classes through a common interface.

**The Power Outlet Analogy**:

A power outlet provides a standard interface (the socket shape). Many different devices (lamp, phone charger, laptop, TV) can plug into it. The outlet doesn't know or care what's plugged in—it just provides power. Each device uses that power differently.

```
         ┌─────────────────┐
         │  Power Outlet   │  ← Common Interface
         │  (provides 120V)│
         └────────┬────────┘
                  │
    ┌─────────────┼─────────────┬─────────────┐
    │             │             │             │
    ▼             ▼             ▼             ▼
┌───────┐    ┌───────┐    ┌───────┐    ┌───────┐
│ Lamp  │    │ Phone │    │Laptop │    │  TV   │
│       │    │Charger│    │       │    │       │
│ makes │    │ makes │    │ makes │    │ makes │
│ light │    │battery│    │compute│    │ video │
└───────┘    └───────┘    └───────┘    └───────┘
```

In code:
```java
// The "outlet" - common interface
Animal animal = new Dog("Buddy", 3, "Golden");

// The "plug" - calling a method
animal.eat();  // What happens depends on what's ACTUALLY plugged in

// Same method call, different behavior based on actual type
Animal a1 = new Dog("Buddy", 3, "Golden");
Animal a2 = new Cat("Whiskers", 5, true);
Animal a3 = new Bird("Tweety", 2, true);

a1.eat();  // "Buddy is eating dog food enthusiastically!"
a2.eat();  // "Whiskers is eating cat food delicately."
a3.eat();  // "Tweety is pecking at seeds."

// POWER: Process different types uniformly
Animal[] animals = {a1, a2, a3};
for (Animal a : animals) {
    a.eat();  // Each behaves according to its actual type
}
```

**Two Types of Polymorphism**:

| Type | When Decided | Mechanism | Example |
|------|--------------|-----------|---------|
| **Compile-time** (Static) | At compile time | Method **Overloading** | Same method name, different parameters |
| **Runtime** (Dynamic) | At runtime | Method **Overriding** | Subclass provides different implementation |

```java
// ══════════════════════════════════════════════════════════════════════════
// COMPILE-TIME POLYMORPHISM: Method Overloading
// Compiler decides which method to call based on arguments
// ══════════════════════════════════════════════════════════════════════════

public class Calculator {
    // Same method name, different parameter types
    public int add(int a, int b) {
        return a + b;
    }
    
    public double add(double a, double b) {
        return a + b;
    }
    
    public int add(int a, int b, int c) {
        return a + b + c;
    }
    
    public String add(String a, String b) {
        return a + b;  // Concatenation
    }
}

// Usage - compiler decides at compile time
Calculator calc = new Calculator();
calc.add(5, 3);           // Calls int version
calc.add(5.0, 3.0);       // Calls double version
calc.add(1, 2, 3);        // Calls three-int version
calc.add("Hello", "World"); // Calls String version

// ══════════════════════════════════════════════════════════════════════════
// RUNTIME POLYMORPHISM: Method Overriding
// JVM decides which method to call based on actual object type
// ══════════════════════════════════════════════════════════════════════════

public class Shape {
    public double calculateArea() {
        return 0;  // Default - subclasses override
    }
}

public class Circle extends Shape {
    private double radius;
    
    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

public class Rectangle extends Shape {
    private double width, height;
    
    @Override
    public double calculateArea() {
        return width * height;
    }
}

// The magic: same method call, different behavior
Shape s1 = new Circle(5);       // Reference: Shape, Object: Circle
Shape s2 = new Rectangle(4, 6); // Reference: Shape, Object: Rectangle

s1.calculateArea();  // JVM sees it's a Circle → calls Circle's method
s2.calculateArea();  // JVM sees it's a Rectangle → calls Rectangle's method
```

**The Key Insight**:

```
REFERENCE TYPE determines WHAT you can call (compile-time check)
OBJECT TYPE determines HOW it behaves (runtime behavior)

Animal animal = new Dog("Buddy", 3, "Golden");
  │                    │
  │                    └── Object type: Dog (actual object in memory)
  └── Reference type: Animal (the variable's declared type)

animal.eat();     ✅ Animal has eat() → compiles. Dog's eat() runs.
animal.bark();    ❌ Animal has no bark() → compile error!
((Dog)animal).bark(); ✅ Cast to Dog, then call bark().
```

---

### Pillar 4: Abstraction — Hiding Complexity

**What is Abstraction?**

Abstraction means exposing only the essential features while hiding the implementation details. Users interact with a simple interface without needing to understand the complexity beneath.

**The Car Analogy**:

When you drive a car:
- You interact with: steering wheel, pedals, gear shift
- You don't interact with: fuel injection system, timing belt, brake hydraulics

The car **abstracts** away the mechanical complexity. You just need to know "press pedal → car accelerates."

**Abstraction in Java: Abstract Classes and Interfaces**

```java
// ══════════════════════════════════════════════════════════════════════════
// ABSTRACT CLASS: Partial implementation + contract
// ══════════════════════════════════════════════════════════════════════════

public abstract class PaymentProcessor {
    // Regular fields - shared by all payment processors
    protected String merchantId;
    protected double transactionFee;
    
    // Constructor - yes, abstract classes can have constructors!
    public PaymentProcessor(String merchantId, double transactionFee) {
        this.merchantId = merchantId;
        this.transactionFee = transactionFee;
    }
    
    // ABSTRACT METHOD: No implementation, subclasses MUST provide
    public abstract boolean processPayment(double amount);
    public abstract void refund(String transactionId);
    
    // CONCRETE METHOD: Has implementation, inherited by subclasses
    public double calculateFee(double amount) {
        return amount * transactionFee;
    }
    
    // FINAL METHOD: Cannot be overridden
    public final String getMerchantId() {
        return merchantId;
    }
}

// Subclass MUST implement all abstract methods
public class StripePaymentProcessor extends PaymentProcessor {
    
    public StripePaymentProcessor(String merchantId) {
        super(merchantId, 0.029);  // Stripe's 2.9% fee
    }
    
    @Override
    public boolean processPayment(double amount) {
        // Stripe-specific implementation
        System.out.println("Processing $" + amount + " through Stripe...");
        // API calls to Stripe...
        return true;
    }
    
    @Override
    public void refund(String transactionId) {
        System.out.println("Refunding transaction " + transactionId + " via Stripe");
    }
}

// ══════════════════════════════════════════════════════════════════════════
// INTERFACE: Pure contract, no state
// ══════════════════════════════════════════════════════════════════════════

public interface Drawable {
    // Methods are implicitly public abstract
    void draw();
    void resize(double factor);
    
    // DEFAULT METHOD (Java 8+): Provides implementation
    default void drawWithBorder() {
        System.out.println("Drawing border...");
        draw();
    }
    
    // STATIC METHOD (Java 8+): Utility methods
    static int getMaxResolution() {
        return 4096;
    }
    
    // CONSTANTS: implicitly public static final
    int DEFAULT_SIZE = 100;
}

// A class can implement MULTIPLE interfaces
public class Button implements Drawable, Clickable, Serializable {
    @Override
    public void draw() { /* ... */ }
    
    @Override
    public void resize(double factor) { /* ... */ }
    
    @Override
    public void onClick() { /* ... */ }
}
```

**Abstract Class vs Interface: The Decision Framework**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              ABSTRACT CLASS vs INTERFACE: WHEN TO USE WHICH                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ASK YOURSELF:                                                              │
│                                                                              │
│  1. "Is this an IS-A relationship?"                                         │
│     • YES, and classes share code → ABSTRACT CLASS                          │
│     • YES, but no shared code → Either works, prefer INTERFACE              │
│                                                                              │
│  2. "Do I need instance fields with state?"                                 │
│     • YES → ABSTRACT CLASS (interfaces can't have instance fields)          │
│     • NO  → INTERFACE                                                        │
│                                                                              │
│  3. "Might unrelated classes need this capability?"                          │
│     • YES → INTERFACE (a Dog and a Robot can both be "Trainable")           │
│     • NO  → ABSTRACT CLASS                                                   │
│                                                                              │
│  4. "Do I need multiple inheritance of behavior?"                            │
│     • YES → INTERFACE (Java allows multiple interfaces)                      │
│     • NO  → Either works                                                     │
│                                                                              │
│  5. "Am I defining what something IS or what something CAN DO?"              │
│     • IS   → ABSTRACT CLASS (Animal, Vehicle, Employee)                      │
│     • CAN DO → INTERFACE (Drawable, Comparable, Serializable)               │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  EXAMPLES:                                                                   │
│                                                                              │
│  ABSTRACT CLASS:                          INTERFACE:                         │
│  • Animal (Dog IS-A Animal)              • Comparable (Dog CAN compare)     │
│  • Shape (Circle IS-A Shape)             • Serializable (Dog CAN serialize) │
│  • HttpServlet (MyServlet IS-A Servlet)  • Runnable (Task CAN run)          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Layer 2 — Working Developer Level

### The `static` Keyword: Class-Level vs Instance-Level

**Understanding static is crucial for interviews.**

`static` means "belongs to the CLASS, not to any specific OBJECT."

**The Blueprint Analogy**:

Think of a class as a blueprint for houses:
- **Instance fields**: Each house has its own color, address, owner
- **Static fields**: The blueprint has a counter tracking total houses built
- **Instance methods**: "Paint this house blue"
- **Static methods**: "How many houses have been built from this blueprint?"

```java
public class House {
    // STATIC: Shared by ALL houses (belongs to the class)
    private static int totalHousesBuilt = 0;
    private static final String BLUEPRINT_VERSION = "2.0";
    
    // INSTANCE: Each house has its own (belongs to the object)
    private String address;
    private String color;
    private int squareFeet;
    
    // STATIC BLOCK: Runs ONCE when class is first loaded
    static {
        System.out.println("House class is being loaded...");
        System.out.println("Blueprint version: " + BLUEPRINT_VERSION);
    }
    
    // INSTANCE BLOCK: Runs EVERY time an object is created (before constructor)
    {
        totalHousesBuilt++;
        System.out.println("Building house #" + totalHousesBuilt);
    }
    
    public House(String address, String color, int squareFeet) {
        this.address = address;
        this.color = color;
        this.squareFeet = squareFeet;
    }
    
    // STATIC METHOD: Can only access static members
    public static int getTotalHousesBuilt() {
        // return address;  ❌ Cannot access instance field from static method
        return totalHousesBuilt;
    }
    
    // INSTANCE METHOD: Can access both static and instance members
    public String getDescription() {
        return color + " house at " + address + 
               " (" + squareFeet + " sqft) - #" + totalHousesBuilt + " built";
    }
}

// Usage:
House.getTotalHousesBuilt();  // ✅ Call static method on CLASS
// House.getDescription();    // ❌ Cannot call instance method on class

House h1 = new House("123 Main St", "Blue", 2000);
House h2 = new House("456 Oak Ave", "Red", 1500);

h1.getDescription();          // ✅ Call instance method on object
h1.getTotalHousesBuilt();     // ⚠️ Works but bad style - call on class instead
```

**Static Memory Visualization**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MEMORY LAYOUT                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  METASPACE (Class-level, one per class)                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  House.class                                                         │    │
│  │  ├── static totalHousesBuilt = 2                                    │    │
│  │  ├── static final BLUEPRINT_VERSION = "2.0"                         │    │
│  │  ├── static getTotalHousesBuilt() method                            │    │
│  │  └── instance method definitions (shared)                           │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  HEAP (Object instances, one per object)                                     │
│  ┌──────────────────────────┐  ┌──────────────────────────┐                 │
│  │  h1 (House instance)     │  │  h2 (House instance)     │                 │
│  │  ├── address="123 Main"  │  │  ├── address="456 Oak"   │                 │
│  │  ├── color="Blue"        │  │  ├── color="Red"         │                 │
│  │  └── squareFeet=2000     │  │  └── squareFeet=1500     │                 │
│  └──────────────────────────┘  └──────────────────────────┘                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### The `final` Keyword: Immutability and Prevention

`final` means "cannot be changed after initialization." It applies to:

| Context | Meaning |
|---------|---------|
| **final variable** | Value cannot be changed after assignment |
| **final field** | Must be initialized by end of constructor |
| **final method** | Cannot be overridden by subclasses |
| **final class** | Cannot be extended (no subclasses allowed) |
| **final parameter** | Cannot be reassigned inside the method |

```java
public final class ImmutablePerson {  // Cannot extend this class
    private final String name;         // Must be set in constructor, cannot change
    private final int birthYear;
    private final List<String> hobbies; // ⚠️ Reference is final, content is NOT
    
    public ImmutablePerson(String name, int birthYear, List<String> hobbies) {
        this.name = name;
        this.birthYear = birthYear;
        // DEFENSIVE COPY: Don't store the passed reference
        this.hobbies = new ArrayList<>(hobbies);
    }
    
    public String getName() { return name; }  // Strings are immutable, safe to return
    
    public int getBirthYear() { return birthYear; }
    
    // DEFENSIVE COPY: Don't expose internal list
    public List<String> getHobbies() {
        return new ArrayList<>(hobbies);  // Return copy, not original
    }
    
    // No setters! Object cannot be modified after creation.
    
    // FINAL METHOD: Subclasses cannot override (if this wasn't a final class)
    public final int getAge(int currentYear) {
        return currentYear - birthYear;
    }
}
```

> 🎯 **Interview Tip**: "final reference ≠ immutable object"
> A `final List` means you can't reassign the variable to a different list,
> but you CAN still add/remove elements from the list!

---

### Object Class: The Root of Everything

Every class in Java implicitly extends `java.lang.Object`. This class provides essential methods that you often need to override.

**The Most Important Methods to Override**:

```java
public class Employee {
    private int id;
    private String name;
    private String department;
    
    public Employee(int id, String name, String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // equals() - Defines when two objects are considered "equal"
    // ══════════════════════════════════════════════════════════════════════
    
    /*
     * THE EQUALS CONTRACT (YOU MUST FOLLOW THIS):
     * 1. REFLEXIVE:    x.equals(x) must be true
     * 2. SYMMETRIC:    x.equals(y) must equal y.equals(x)
     * 3. TRANSITIVE:   if x.equals(y) and y.equals(z), then x.equals(z)
     * 4. CONSISTENT:   multiple calls return same result (if objects unchanged)
     * 5. NON-NULL:     x.equals(null) must be false
     */
    @Override
    public boolean equals(Object obj) {
        // 1. Same reference? Same object!
        if (this == obj) return true;
        
        // 2. Null or different class? Not equal.
        if (obj == null || getClass() != obj.getClass()) return false;
        
        // 3. Cast and compare relevant fields
        Employee other = (Employee) obj;
        return id == other.id &&
               Objects.equals(name, other.name) &&
               Objects.equals(department, other.department);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // hashCode() - Returns an integer hash for use in hash-based collections
    // ══════════════════════════════════════════════════════════════════════
    
    /*
     * THE HASHCODE CONTRACT:
     * 1. CONSISTENT: Same object returns same hashCode (if not modified)
     * 2. EQUALS IMPLIES SAME HASH: If x.equals(y), then x.hashCode() == y.hashCode()
     * 3. UNEQUAL OBJECTS MAY HAVE SAME HASH: (but shouldn't, for performance)
     * 
     * ⚠️ CRITICAL: If you override equals(), you MUST override hashCode()!
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, department);
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // toString() - Returns a string representation for debugging/logging
    // ══════════════════════════════════════════════════════════════════════
    
    /*
     * Default toString() returns: ClassName@hexHashCode (useless!)
     * Override to return useful information.
     */
    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + name + "', dept='" + department + "'}";
    }
}
```

---

## Layer 3 — Advanced Engineering Depth

### Composition Over Inheritance (Critical Principle)

This is one of the most important design principles in OOP. Many codebases have been ruined by overuse of inheritance.

**The Fragile Base Class Problem**:

```java
// Parent class - seems innocent
public class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;
    
    @Override
    public boolean add(E e) {
        addCount++;
        return super.add(e);
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);
    }
    
    public int getAddCount() { return addCount; }
}

// Test it:
InstrumentedHashSet<String> s = new InstrumentedHashSet<>();
s.addAll(List.of("A", "B", "C"));
System.out.println(s.getAddCount());  // Expected: 3, Actual: 6!

// WHY?!
// Because HashSet.addAll() internally calls add() for each element!
// Our addAll: +3, then HashSet.addAll calls our add() 3 times: +3 more
```

**The Solution: Composition**

```java
// Wrapper class using composition
public class InstrumentedSet<E> implements Set<E> {
    private final Set<E> delegate;  // HAS-A relationship
    private int addCount = 0;
    
    public InstrumentedSet(Set<E> delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public boolean add(E e) {
        addCount++;
        return delegate.add(e);  // Forward to wrapped set
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return delegate.addAll(c);  // Forward - no double counting!
    }
    
    public int getAddCount() { return addCount; }
    
    // Forward all other Set methods to delegate
    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    // ... etc
}

// Now it works correctly:
Set<String> set = new InstrumentedSet<>(new HashSet<>());
set.addAll(List.of("A", "B", "C"));
// getAddCount() returns 3! Correct!
```

**When to Use Each**:

| Use Inheritance | Use Composition |
|-----------------|-----------------|
| True IS-A relationship | HAS-A relationship |
| Want to be treated as parent type | Just need functionality |
| Parent designed for extension | Parent not designed for extension |
| Rare, think hard before using | Should be your default choice |

---

### Records (Java 16+): Immutable Data Classes

Records eliminate boilerplate for simple data carrier classes.

```java
// OLD WAY: 50+ lines for a simple data class
public class PersonOld {
    private final String name;
    private final int age;
    private final String email;
    
    public PersonOld(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    
    @Override public boolean equals(Object o) { /* ... */ }
    @Override public int hashCode() { /* ... */ }
    @Override public String toString() { /* ... */ }
}

// NEW WAY: One line!
public record Person(String name, int age, String email) {}

// Usage:
Person p = new Person("Alice", 30, "alice@example.com");
p.name();    // "Alice" (accessor, not getName())
p.age();     // 30
p.email();   // "alice@example.com"
System.out.println(p);  // Person[name=Alice, age=30, email=alice@example.com]
p.equals(new Person("Alice", 30, "alice@example.com"));  // true

// Records can have:
public record Product(String sku, String name, BigDecimal price) {
    
    // COMPACT CONSTRUCTOR: Validation without repeating field assignments
    public Product {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        name = name.trim();  // Can modify before assignment
    }
    
    // Additional instance methods
    public boolean isExpensive() {
        return price.compareTo(new BigDecimal("100")) > 0;
    }
}
```

---

### Sealed Classes (Java 17+): Controlled Inheritance

Sealed classes restrict which classes can extend them, enabling exhaustive pattern matching.

```java
// Only these three classes can extend Shape
public sealed class Shape 
    permits Circle, Rectangle, Triangle {
    
    public abstract double area();
}

public final class Circle extends Shape {
    private final double radius;
    public Circle(double radius) { this.radius = radius; }
    
    @Override
    public double area() { return Math.PI * radius * radius; }
    public double radius() { return radius; }
}

public final class Rectangle extends Shape {
    private final double width, height;
    // ... similar implementation
}

public non-sealed class Triangle extends Shape {
    // non-sealed means OTHER classes CAN extend Triangle
}

// The power: Exhaustive pattern matching (Java 21)
public String describe(Shape shape) {
    return switch (shape) {
        case Circle c    -> "Circle with radius " + c.radius();
        case Rectangle r -> "Rectangle " + r.width() + "x" + r.height();
        case Triangle t  -> "Triangle with area " + t.area();
        // No default needed! Compiler knows these are the only possibilities
    };
}
```

---

## Layer 4 — Interview Mastery

### Comprehensive Interview Questions

---

**Q1: "What are the four pillars of OOP? Explain each briefly."**

> **Answer**: "The four pillars are:
>
> 1. **Encapsulation**: Bundling data and methods together, hiding internal details behind a public interface. We use private fields and public methods to control access and ensure data validity.
>
> 2. **Inheritance**: Creating new classes based on existing ones, establishing IS-A relationships. Subclasses inherit fields and methods, can override behavior, and can add new capabilities.
>
> 3. **Polymorphism**: The ability for the same method call to behave differently based on the actual object type. There's compile-time polymorphism (overloading) and runtime polymorphism (overriding).
>
> 4. **Abstraction**: Hiding complex implementation details behind simple interfaces. We use abstract classes and interfaces to define WHAT should happen without specifying HOW."

---

**Q2: "What's the difference between method overloading and overriding?"**

| Aspect | Overloading | Overriding |
|--------|-------------|------------|
| **Definition** | Same method name, different parameters | Same method signature, different implementation |
| **Where** | Same class (or inherited) | Subclass overrides parent |
| **When resolved** | Compile-time | Runtime |
| **Parameters** | Must differ | Must be exactly same |
| **Return type** | Can differ | Same or covariant (subtype) |
| **Access** | Can differ | Same or more accessible |
| **static methods** | Can be overloaded | Cannot be overridden (hidden) |
| **private methods** | Can be overloaded | Cannot be overridden |

---

**Q3: "Why do we need to override hashCode() when we override equals()?"**

> **Answer**: "The contract states: if two objects are equal according to equals(), they MUST have the same hashCode(). Hash-based collections like HashMap and HashSet use hashCode() to determine which bucket an object goes in, then use equals() to check for actual equality.
>
> If we override equals() but not hashCode(), two equal objects might go into different buckets. This means:
> - HashSet might store 'duplicate' objects that are equal
> - HashMap might not find a key even though an equal key exists
>
> This is a very common bug that's hard to diagnose because the code 'looks right' but behaves incorrectly."

---

**Q4: "Explain the difference between abstract class and interface. When would you use each?"**

> **Answer**: 
>
> **Abstract Class**:
> - Can have instance fields with state
> - Can have constructors
> - Single inheritance only
> - Methods can have any access modifier
> - Represents IS-A relationship
> - Use when classes share common code/state
>
> **Interface**:
> - Only constants (no instance state)
> - No constructors
> - Multiple inheritance allowed
> - Methods are public (or private since Java 9)
> - Represents CAN-DO capability
> - Use for contracts across unrelated classes
>
> **My decision framework**:
> - Need instance state? → Abstract class
> - Unrelated classes need this behavior? → Interface
> - Defining a capability/contract? → Interface
> - Need multiple inheritance? → Interface
> - Sharing code in a hierarchy? → Abstract class

---

**Q5: "What is the 'diamond problem' and how does Java solve it?"**

> **Answer**: "The diamond problem occurs when a class inherits from two classes that both inherit from a common ancestor. If both parent classes override a method, which version does the child get?
>
> Java avoids this for classes by only allowing single inheritance. For interfaces with default methods, if two interfaces provide conflicting default implementations, the implementing class MUST override the method to resolve the conflict. The compiler enforces this.
>
> ```java
> interface A { default void foo() { } }
> interface B { default void foo() { } }
> class C implements A, B {
>     @Override
>     public void foo() {
>         A.super.foo();  // Choose explicitly
>     }
> }
> ```"

---

**Q6: "Why is 'composition over inheritance' recommended?"**

> **Answer**: "Inheritance creates tight coupling between parent and child. Problems include:
>
> 1. **Fragile Base Class**: Changes to parent can break subclasses
> 2. **Broken Encapsulation**: Subclass depends on parent's implementation
> 3. **Inflexibility**: Can't change parent at runtime
> 4. **Misuse**: Often used for code reuse when there's no true IS-A relationship
>
> Composition (HAS-A) is more flexible:
> - Looser coupling
> - Can swap implementations at runtime
> - Easier to test with mocks
> - More explicit dependencies
>
> Use inheritance only for true IS-A relationships where you want polymorphism. Use composition for everything else."

---

**Q7: "What is the Liskov Substitution Principle (LSP)?"**

> **Answer**: "LSP states that objects of a subclass should be usable anywhere objects of the superclass are expected, without breaking the program.
>
> In other words: if S is a subtype of T, then objects of type T can be replaced with objects of type S without altering the correctness of the program.
>
> **Classic violation** - Rectangle/Square problem:
> ```java
> class Rectangle {
>     void setWidth(int w) { width = w; }
>     void setHeight(int h) { height = h; }
> }
> class Square extends Rectangle {
>     void setWidth(int w) { width = w; height = w; }  // Violates LSP!
>     void setHeight(int h) { width = h; height = h; }
> }
> ```
>
> A Square cannot substitute for Rectangle because changing width unexpectedly changes height, breaking client code that expects independent dimensions."

---

**Q8: "What's the difference between `==` and `.equals()`?"**

| Aspect | `==` | `.equals()` |
|--------|------|-------------|
| **For primitives** | Compares values | N/A (primitives aren't objects) |
| **For objects** | Compares references (memory addresses) | Compares content (if properly overridden) |
| **Default behavior** | Always same | Same as `==` until overridden |
| **Can be null** | Yes (a == null is fine) | No! null.equals() throws NullPointerException |
| **String special case** | May work due to string pool, but unreliable | Always works correctly |

---

**Q9: "Explain `static` in Java. What can and can't static methods do?"**

> **Answer**: "`static` means the member belongs to the class, not any instance.
>
> **Static methods CAN**:
> - Access static fields
> - Call other static methods
> - Create objects
>
> **Static methods CANNOT**:
> - Access instance fields (no object to access them from)
> - Call instance methods directly
> - Use `this` or `super` (no object context)
>
> **Use static for**:
> - Utility methods (Math.max(), Collections.sort())
> - Factory methods
> - Constants (with final)
>
> **Avoid static for**:
> - State that should vary per instance
> - Methods that need polymorphism"

---

**Q10: "What makes a class immutable? Why would you want immutability?"**

> **Answer**: "To make a class immutable:
> 1. Make class `final` (prevent subclasses that might add mutability)
> 2. Make all fields `private` and `final`
> 3. Don't provide setters
> 4. If fields are objects, don't expose them directly
> 5. Use defensive copies in constructor and getters
>
> **Benefits**:
> - Thread-safe without synchronization
> - Can be cached and shared freely
> - Great for hash keys (hashCode won't change)
> - Easier to reason about
>
> **Examples**: String, Integer, LocalDate, BigDecimal"

---

**Q11: "What are Records in Java and when should you use them?"**

> **Answer**: "Records (Java 16+) are immutable data carriers. The compiler generates constructor, accessors, equals(), hashCode(), and toString().
>
> ```java
> record Point(int x, int y) {}
> ```
>
> **Use when**:
> - You need a simple data carrier
> - Immutability is acceptable
> - You want to avoid boilerplate
>
> **Don't use when**:
> - You need mutable state
> - You need inheritance (records are implicitly final)
> - You need to extend another class"

---

**Q12: "Can you override a private method? A static method?"**

> **Answer**: "**Private methods**: No. Private methods aren't visible to subclasses, so there's nothing to override. A subclass can have a method with the same signature, but it's a completely new method, not an override.
>
> **Static methods**: Technically no. Static methods belong to the class, not instances. If a subclass declares a method with the same signature, it 'hides' (shadows) the parent's method rather than overriding it. The version called depends on the reference type, not the object type. This is called method hiding.
>
> ```java
> class Parent { static void foo() { } }
> class Child extends Parent { static void foo() { } }
> Parent p = new Child();
> p.foo();  // Calls Parent.foo(), not Child.foo()!
> ```"

---

**Q13: "What happens if you don't provide any constructor in a class?"**

> **Answer**: "Java provides a default no-argument constructor automatically. This default constructor:
> - Takes no parameters
> - Calls super() (parent's no-arg constructor)
> - Has the same access level as the class
>
> However, once you define ANY constructor, Java no longer provides the default. If you need a no-arg constructor AND a parameterized one, you must explicitly write both."

---

**Q14: "Explain covariant return types."**

> **Answer**: "Since Java 5, when overriding a method, you can return a more specific type than the parent method declared. This is called a covariant return type.
>
> ```java
> class Animal {
>     Animal reproduce() { return new Animal(); }
> }
> class Dog extends Animal {
>     @Override
>     Dog reproduce() { return new Dog(); }  // Returns Dog, not Animal
> }
> ```
>
> This is useful because it provides more type information to callers without requiring a cast."

---

**Q15: "What is the difference between aggregation and composition?"**

> **Answer**: "Both are 'HAS-A' relationships, but they differ in ownership:
>
> **Composition** (strong ownership):
> - Child can't exist without parent
> - Parent controls child's lifecycle
> - Example: House and Rooms. Rooms don't exist without the House.
>
> **Aggregation** (weak ownership):
> - Child can exist independently
> - Parent just 'uses' the child
> - Example: Team and Players. Players exist even without the Team.
>
> In code, composition usually means the parent creates the child; aggregation means the child is passed in."

---

### Common Mistakes by Level

**Beginner Mistakes**:
- ❌ Making all fields public
- ❌ Using `==` to compare objects
- ❌ Not overriding toString() for debugging
- ❌ Returning mutable internal state from getters

**Intermediate Mistakes**:
- ❌ Overriding equals() without hashCode()
- ❌ Using inheritance for code reuse instead of composition
- ❌ Not making immutable classes properly
- ❌ Overusing static for convenience

**Advanced Mistakes**:
- ❌ Violating Liskov Substitution Principle
- ❌ Creating deep inheritance hierarchies
- ❌ Not considering thread safety in shared objects
- ❌ Ignoring sealed classes where they'd help

---

## Summary: Mental Models

> 🧠 **Encapsulation**: "My internals are my business. Talk to me through my public methods."

> 🧠 **Inheritance**: "I am a specialized version of my parent. I can do everything they can, plus more."

> 🧠 **Polymorphism**: "You can treat me as my parent type, but I'll behave according to who I really am."

> 🧠 **Abstraction**: "You don't need to know HOW I work. Just know WHAT I can do."

> 🧠 **Composition over Inheritance**: "Don't be something; have something."

---

[← Back to Index](00-README.md) | [Previous: Syntax & Fundamentals](02-Syntax-Fundamentals.md) | [Next: Collections →](04-Collections.md)
