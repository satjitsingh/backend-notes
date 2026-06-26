# SOLID Principles in Java - Complete Guide

This repository contains **beginner-friendly, fully runnable Java examples** demonstrating each of the 5 SOLID principles of Object-Oriented Design.

---

## 📁 Folder Structure

```
SOLID_Principles/
├── 1_SRP_SingleResponsibility/
│   └── SRPExample.java
├── 2_OCP_OpenClosed/
│   └── OCPExample.java
├── 3_LSP_LiskovSubstitution/
│   └── LSPExample.java
├── 4_ISP_InterfaceSegregation/
│   └── ISPExample.java
├── 5_DIP_DependencyInversion/
│   └── DIPExample.java
└── README.md (this file)
```

---

## 🚀 How to Compile and Run

Each example is self-contained. Navigate to the respective folder and run:

```bash
# Example: Running the SRP example
cd 1_SRP_SingleResponsibility
javac SRPExample.java
java SRPExample
```

---

## 📋 SOLID Principles Overview

| Principle | Acronym | One-Line Summary |
|-----------|---------|------------------|
| **S**ingle Responsibility | SRP | A class should have only ONE reason to change |
| **O**pen/Closed | OCP | Open for extension, closed for modification |
| **L**iskov Substitution | LSP | Subtypes must be substitutable for their base types |
| **I**nterface Segregation | ISP | Don't force clients to implement unused methods |
| **D**ependency Inversion | DIP | Depend on abstractions, not concrete implementations |

---

## 1️⃣ Single Responsibility Principle (SRP)

### 📂 File: `1_SRP_SingleResponsibility/SRPExample.java`

### 💡 What It Means
A class should have **only ONE reason to change** - meaning it should do ONE thing and do it well.

### 🌍 Real-World Analogy
In a restaurant:
- **Chef** → Only cooks food
- **Waiter** → Only serves customers  
- **Cashier** → Only handles payments

Each person has ONE job. They don't mix responsibilities.

### 📝 Example Summary
Instead of one `Employee` class that handles data, calculates salary, saves to database, AND generates reports, we split into:
- `Employee` → Stores employee data
- `SalaryCalculator` → Calculates salaries
- `EmployeeRepository` → Database operations
- `EmployeeReportGenerator` → Creates reports

### ✅ Benefits
- Change tax rules? Only modify `SalaryCalculator`
- Change database? Only modify `EmployeeRepository`
- Each class can be tested independently

---

## 2️⃣ Open/Closed Principle (OCP)

### 📂 File: `2_OCP_OpenClosed/OCPExample.java`

### 💡 What It Means
Software entities should be **OPEN for extension** but **CLOSED for modification**.

### 🌍 Real-World Analogy
Think of a **power strip**:
- You can plug in NEW devices → **Open for extension**
- You don't rewire it each time → **Closed for modification**

### 📝 Example Summary
Instead of a payment processor with if-else for each payment type, we:
1. Create a `PaymentMethod` interface
2. Each payment type implements the interface: `CreditCardPayment`, `PayPalPayment`, `CryptoPayment`
3. `PaymentProcessor` works with ANY payment method without modification

### ✅ Benefits
- Add Apple Pay? Just create `ApplePayPayment implements PaymentMethod`
- Zero changes to `PaymentProcessor`
- No risk of breaking existing payment methods

---

## 3️⃣ Liskov Substitution Principle (LSP)

### 📂 File: `3_LSP_LiskovSubstitution/LSPExample.java`

### 💡 What It Means
If class B is a subtype of class A, you should be able to **replace A with B** without breaking the program.

### 🌍 Real-World Analogy
If you ask for a **"vehicle to drive to work"**:
- Car ✅ → Can drive
- Truck ✅ → Can drive
- Boat ❌ → Can't drive on roads (breaks the expectation!)

### 📝 Example Summary
**Bad Design:**
```java
class Square extends Rectangle  // Violates LSP!
```
Setting width changes height unexpectedly.

**Good Design:**
- Both `Rectangle` and `Square` implement `Shape` interface
- `Bird`, `FlyingBird`, and `SwimmingBird` are separate interfaces
- `Penguin` doesn't pretend it can fly!

### ✅ Benefits
- Any `Shape` works with `ShapeCalculator`
- Compile-time safety prevents misuse
- No surprise behaviors

---

## 4️⃣ Interface Segregation Principle (ISP)

### 📂 File: `4_ISP_InterfaceSegregation/ISPExample.java`

### 💡 What It Means
Clients should NOT be forced to depend on interfaces they don't use.

### 🌍 Real-World Analogy
A restaurant menu that forces you to order appetizer, main course, dessert, AND drinks - even if you just want coffee! Better to have **separate menus**.

### 📝 Example Summary
Instead of one giant `Worker` interface, we split into:
- `Workable` → work()
- `Eatable` → eat(), takeBreak()
- `Codeable` → writeCode(), reviewCode()
- `Maintainable` → recharge(), updateSoftware()

Classes implement only what applies:
- `Developer` → Workable, Eatable, Codeable
- `Robot` → Workable, Codeable, Maintainable (no Eatable!)

### ✅ Benefits
- No empty or throwing methods
- Robots don't pretend to eat
- Compile-time safety

---

## 5️⃣ Dependency Inversion Principle (DIP)

### 📂 File: `5_DIP_DependencyInversion/DIPExample.java`

### 💡 What It Means
High-level modules should depend on **abstractions (interfaces)**, not low-level implementations.

### 🌍 Real-World Analogy
A laptop with a **charging port** (abstraction) vs. a laptop with a hardwired charger:
- Port → Any compatible charger works
- Hardwired → If charger breaks, laptop is useless!

### 📝 Example Summary
Instead of:
```java
class OrderService {
    private EmailService emailService = new EmailService(); // Hard dependency!
}
```

We use:
```java
class OrderService {
    private NotificationService service; // Depends on interface!
    
    public OrderService(NotificationService service) { // Injected!
        this.service = service;
    }
}
```

Now `OrderService` works with Email, SMS, Push, Slack, or even Multi-Channel notifications!

### ✅ Benefits
- Swap Email for SMS without changing business logic
- Easy unit testing with mock services
- Add WhatsApp/Telegram without modifying existing code

---

## 🎯 Quick Reference Table

| Principle | Problem It Solves | Solution |
|-----------|-------------------|----------|
| **SRP** | Classes doing too many things | Split into focused classes |
| **OCP** | Modifying existing code for new features | Use interfaces/abstraction |
| **LSP** | Subclasses breaking parent behavior | Proper hierarchy design |
| **ISP** | Fat interfaces forcing unused implementations | Split into small interfaces |
| **DIP** | Hard dependencies on concrete classes | Depend on interfaces, inject dependencies |

---

## 📚 Learning Tips

1. **Start with SRP** - It's the foundation
2. **OCP and DIP work together** - Interfaces enable both
3. **LSP catches bad inheritance** - "Is-a" must really mean "behaves-like-a"
4. **ISP prevents bloat** - Keep interfaces focused
5. **Practice by refactoring** - Take existing code and apply these principles

---

## 🏃‍♂️ Quick Test

Run all examples:

```bash
# Windows
cd SOLID_Principles\1_SRP_SingleResponsibility && javac SRPExample.java && java SRPExample
cd ..\2_OCP_OpenClosed && javac OCPExample.java && java OCPExample
cd ..\3_LSP_LiskovSubstitution && javac LSPExample.java && java LSPExample
cd ..\4_ISP_InterfaceSegregation && javac ISPExample.java && java ISPExample
cd ..\5_DIP_DependencyInversion && javac DIPExample.java && java DIPExample

# Linux/Mac
cd SOLID_Principles/1_SRP_SingleResponsibility && javac SRPExample.java && java SRPExample
cd ../2_OCP_OpenClosed && javac OCPExample.java && java OCPExample
cd ../3_LSP_LiskovSubstitution && javac LSPExample.java && java LSPExample
cd ../4_ISP_InterfaceSegregation && javac ISPExample.java && java ISPExample
cd ../5_DIP_DependencyInversion && javac DIPExample.java && java DIPExample
```

---

Happy Learning! 🎉

