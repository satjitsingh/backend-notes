# Chapter 14: Design Patterns (Interview-Focused)

[← Back to Index](00-README.md) | [Previous: JDBC](13-JDBC.md) | [Next: Modules →](15-Modules.md)

---

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
            .age(30)
            .phone("555-1234")
            .build();
    }
}
```

### Strategy Pattern

```java
// Strategy interface
public interface PaymentStrategy {
    void pay(double amount);
}

// Concrete strategies
public class CreditCardPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " with credit card");
    }
}

public class PayPalPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " with PayPal");
    }
}

// Context
public class ShoppingCart {
    private PaymentStrategy strategy;
    
    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void checkout(double total) {
        strategy.pay(total);
    }
}
```

### Observer Pattern

```java
// Observer interface
public interface Observer {
    void update(String event);
}

// Subject
public class EventPublisher {
    private List<Observer> observers = new ArrayList<>();
    
    public void subscribe(Observer o) { observers.add(o); }
    public void unsubscribe(Observer o) { observers.remove(o); }
    
    public void notify(String event) {
        observers.forEach(o -> o.update(event));
    }
}

// Usage
EventPublisher publisher = new EventPublisher();
publisher.subscribe(event -> System.out.println("Received: " + event));
publisher.notify("User logged in");
```

---

[← Back to Index](00-README.md) | [Previous: JDBC](13-JDBC.md) | [Next: Modules →](15-Modules.md)
