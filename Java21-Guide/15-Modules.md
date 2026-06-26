# Chapter 15: Modular Architecture (JPMS)

[← Back to Index](00-README.md) | [Previous: Design Patterns](14-Design-Patterns.md) | [Next: Performance →](16-Performance.md)

---

## Why This Chapter Matters

> **Interview Reality**: Most developers have never worked with Java modules, but understanding JPMS demonstrates deep Java knowledge and architectural thinking.

Java's Module System (JPMS - Java Platform Module System) was introduced in Java 9 to solve fundamental problems that plagued Java applications for over two decades. While not every project uses modules, understanding them is crucial because:

- **Large-scale applications** benefit from strong encapsulation and explicit dependencies
- **Modern frameworks** (Spring Boot, Quarkus) leverage modules internally
- **Interview questions** frequently explore why modules exist and how they differ from classpath
- **Architectural decisions** require understanding when to use modules vs. classpath
- **Java 9+ features** often assume module knowledge

This chapter will take you from understanding the "why" behind modules to mastering advanced concepts like module layers and custom runtime images.

---

## Layer 1 — Beginner Foundation

### What is Modularity and Why Was It Introduced?

**The Problem Before Java 9 (Classpath Hell)**

Imagine you're building a house, but all your tools are thrown into one giant toolbox. When you need a hammer, you dig through everything. Sometimes you grab the wrong tool. Sometimes tools conflict with each other. That's what Java development was like before modules.

**Real-World Problems:**

1. **JAR Hell**: Multiple versions of the same library on the classpath
2. **No Encapsulation**: Any class could access any other class, even internal implementation details
3. **Unclear Dependencies**: Hard to know what your code actually needs
4. **Large JRE**: Had to ship entire Java runtime, even unused parts

**Java 9's Solution: Modules**

A **module** is a self-contained unit of code that:
- Explicitly declares what it **exports** (makes public)
- Explicitly declares what it **requires** (depends on)
- Hides internal implementation details

```java
// Before Java 9: Everything is public
package com.myapp.util;
public class InternalHelper {  // Anyone can use this!
    public void doSomething() { }
}
```

```java
// With modules: Explicit control
module com.myapp.util {
    exports com.myapp.util;  // Only this package is public
    // InternalHelper is hidden!
}
```

### Understanding module-info.java

Every module has a `module-info.java` file that defines the module's identity and boundaries.

**Basic Structure:**

```java
module com.example.mymodule {
    // Module declaration
}
```

**Key Concepts:**

- **Module name**: Usually matches package structure (e.g., `com.example.mymodule`)
- **Location**: Must be in the root of your source directory
- **Compilation**: Compiled to `module-info.class` by `javac`

**Example Project Structure:**

```
myproject/
├── src/
│   └── com.example.mymodule/
│       ├── module-info.java
│       └── com/
│           └── example/
│               └── mymodule/
│                   └── Main.java
```

### The `exports` Keyword

**What it does**: Makes a package visible to other modules.

```java
module com.example.mymodule {
    exports com.example.mymodule.api;  // Public API
    // com.example.mymodule.internal is NOT exported - hidden!
}
```

**Why it matters**: Only exported packages can be accessed by other modules. This enforces encapsulation.

**Example:**

```java
// module-info.java
module com.example.mymodule {
    exports com.example.mymodule.api;
}

// com/example/mymodule/api/PublicClass.java
package com.example.mymodule.api;
public class PublicClass {  // Can be used by other modules
    public void doWork() { }
}

// com/example/mymodule/internal/InternalClass.java
package com.example.mymodule.internal;
public class InternalClass {  // Hidden! Other modules can't access
    public void helper() { }
}
```

### The `requires` Keyword

**What it does**: Declares a dependency on another module.

```java
module com.example.app {
    requires com.example.mymodule;  // I need this module
}
```

**Why it matters**: Makes dependencies explicit. If a module isn't required, you can't use its exported packages.

**Example:**

```java
// Module A: com.example.utils
module com.example.utils {
    exports com.example.utils;
}

// Module B: com.example.app
module com.example.app {
    requires com.example.utils;  // Must declare dependency
}

// In com.example.app code:
import com.example.utils.StringHelper;  // ✅ Works - module is required
```

### Creating Your First Module

**Step 1: Create Directory Structure**

```bash
myfirstmodule/
├── src/
│   └── com.greetings/
│       ├── module-info.java
│       └── com/
│           └── greetings/
│               └── Main.java
```

**Step 2: Write module-info.java**

```java
module com.greetings {
    // Empty module - no dependencies, no exports
}
```

**Step 3: Write Main.java**

```java
package com.greetings;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from a module!");
    }
}
```

**Step 4: Compile**

```bash
javac -d mods/com.greetings src/com.greetings/module-info.java src/com.greetings/com/greetings/Main.java
```

**Step 5: Run**

```bash
java --module-path mods -m com.greetings/com.greetings.Main
```

**Key Points:**
- `--module-path` (or `-p`): Where to find modules (replaces classpath)
- `-m`: Specify module and main class

### Problems with Classpath Hell

**The Classic Problem:**

```bash
# Your application needs library X version 2.0
# But library Y needs library X version 1.5
# Both are on the classpath - which one wins? 🤷
```

**What Happens:**
- ClassLoader finds classes in order
- First match wins (unpredictable!)
- Runtime errors when wrong version is used
- No way to know what you're actually using

**Modules Solve This:**
- Each module has its own namespace
- Explicit dependencies prevent conflicts
- Module system validates dependencies at startup

---

## Layer 2 — Working Developer Level

### Module Types

Java has three types of modules:

#### 1. Named Modules

**Definition**: Explicit modules with a `module-info.java` file.

**Characteristics:**
- Explicit dependencies (`requires`)
- Explicit exports (`exports`)
- Strong encapsulation
- Must be on module path

**Example:**

```java
module com.example.app {
    requires java.base;
    requires com.example.utils;
    exports com.example.app.api;
}
```

#### 2. Automatic Modules

**Definition**: Regular JAR files placed on module path without `module-info.java`.

**Characteristics:**
- Automatically becomes a module
- Module name derived from JAR filename
- Exports ALL packages
- Can read all other modules (including unnamed)
- Useful for migration

**Naming Rules:**
- `my-library-1.2.3.jar` → module name: `my.library`
- Hyphens become dots
- Version numbers removed

**Example:**

```bash
# Place regular JAR on module path
java --module-path libs/my-library.jar -m com.example.app/com.example.app.Main

# my-library.jar becomes automatic module "my.library"
```

**In module-info.java:**

```java
module com.example.app {
    requires my.library;  // Automatic module
}
```

#### 3. Unnamed Module

**Definition**: Code on classpath (traditional Java).

**Characteristics:**
- All code not in a module
- Can access all other modules (including named)
- Cannot be required by named modules
- Used for legacy code

**Example:**

```bash
# Traditional classpath usage
java -cp libs/* com.example.Main  # Runs in unnamed module
```

### Migrating from Classpath to Modules

**Migration Strategy:**

#### Step 1: Start with Automatic Modules

```bash
# Keep existing JARs, just move to module path
java --module-path libs/* -m com.example.app/com.example.app.Main
```

#### Step 2: Create module-info.java for Your Code

```java
module com.example.app {
    requires java.base;  // Always implicit, but good to be explicit
    requires my.library;  // Automatic module from JAR
}
```

#### Step 3: Gradually Convert Dependencies

As libraries release module versions, update:

```java
module com.example.app {
    requires my.library;  // Now a proper named module
}
```

**Common Migration Issues:**

1. **Split Packages**: Same package in multiple modules
   ```java
   // Module A exports com.example.util
   // Module B exports com.example.util
   // ❌ Error: Split package!
   ```

2. **Missing Exports**: Internal packages accessed
   ```java
   // Module doesn't export com.example.internal
   // But your code tries to use it
   // ❌ Error: Package not exported!
   ```

### `requires transitive`

**Problem**: When module A requires module B, and module B requires module C, module A's users must also require module C.

**Solution**: `requires transitive` makes dependencies transitive.

**Example:**

```java
// Without transitive
module com.example.api {
    requires com.example.utils;
}

module com.example.app {
    requires com.example.api;
    requires com.example.utils;  // Must also require this!
}
```

```java
// With transitive
module com.example.api {
    requires transitive com.example.utils;  // Transitive dependency
}

module com.example.app {
    requires com.example.api;  // Automatically gets com.example.utils
    // No need to require com.example.utils separately
}
```

**Real-World Use Case:**

```java
// SLF4J API module
module org.slf4j {
    exports org.slf4j;
    requires transitive org.slf4j.spi;  // Users get SPI automatically
}

// Your application
module com.example.app {
    requires org.slf4j;  // Gets both API and SPI
}
```

### Qualified Exports: `exports...to`

**What it does**: Exports a package to specific modules only.

**Use Case**: Internal API that only certain modules should use.

**Example:**

```java
module com.example.core {
    exports com.example.core.api;  // Public API
    exports com.example.core.internal to com.example.plugin;  // Internal API
}

module com.example.plugin {
    requires com.example.core;
    // Can access com.example.core.internal
}

module com.example.app {
    requires com.example.core;
    // ❌ Cannot access com.example.core.internal
}
```

**When to Use:**
- Framework internals (Spring, Hibernate)
- Plugin systems
- Testing frameworks

### `opens` for Reflection

**The Reflection Problem:**

Modules hide internal packages, but reflection needs access to them.

**Solution**: `opens` allows reflection access without exporting.

**Example:**

```java
module com.example.model {
    exports com.example.model.api;  // Public API
    opens com.example.model.internal;  // Reflection access only
}

// Reflection can access internal classes
Class<?> clazz = Class.forName("com.example.model.internal.InternalClass");
```

**Common Use Cases:**
- Serialization frameworks (Jackson, Gson)
- ORM frameworks (Hibernate, JPA)
- Dependency injection (Spring)

**Best Practice:**

```java
module com.example.model {
    // Option 1: Open specific package
    opens com.example.model.entities;
    
    // Option 2: Open to specific module only
    opens com.example.model.entities to org.hibernate.orm.core;
}
```

### ServiceLoader with Modules

**What is ServiceLoader?**

A mechanism for discovering and loading service implementations.

**Traditional Approach (Classpath):**

```java
// META-INF/services/com.example.Service
com.example.impl.ServiceImpl1
com.example.impl.ServiceImpl2
```

**Module Approach:**

**Step 1: Define Service Interface**

```java
// module: com.example.api
package com.example.api;

public interface PaymentService {
    void processPayment(double amount);
}
```

**Step 2: Provide Implementation**

```java
// module: com.example.impl
module com.example.impl {
    requires com.example.api;
    provides com.example.api.PaymentService 
        with com.example.impl.CreditCardPayment;
}

package com.example.impl;

public class CreditCardPayment implements PaymentService {
    public void processPayment(double amount) {
        System.out.println("Processing credit card: " + amount);
    }
}
```

**Step 3: Use Service**

```java
// module: com.example.app
module com.example.app {
    requires com.example.api;
    uses com.example.api.PaymentService;  // Declare service usage
}

package com.example.app;

import com.example.api.PaymentService;
import java.util.ServiceLoader;

public class App {
    public static void main(String[] args) {
        ServiceLoader<PaymentService> services = 
            ServiceLoader.load(PaymentService.class);
        
        for (PaymentService service : services) {
            service.processPayment(100.0);
        }
    }
}
```

**Key Points:**
- `provides`: Declares service implementation
- `uses`: Declares service consumption
- No META-INF/services needed!

### Multi-Release JARs

**Problem**: Different Java versions need different implementations.

**Solution**: Multi-Release JARs contain version-specific classes.

**Structure:**

```
my-library.jar
├── META-INF/
│   └── MANIFEST.MF
│       └── Multi-Release: true
├── com/example/Util.class          (Java 8)
└── META-INF/versions/
    ├── 9/com/example/Util.class    (Java 9+)
    └── 11/com/example/Util.class   (Java 11+)
```

**Example:**

```java
// Java 8 version
package com.example;
public class Util {
    public static void process() {
        // Java 8 implementation
    }
}

// Java 9+ version (META-INF/versions/9/com/example/Util.java)
package com.example;
public class Util {
    public static void process() {
        // Java 9+ optimized implementation
    }
}
```

**Benefits:**
- Single JAR works on multiple Java versions
- Optimized code per version
- Backward compatible

**Creating Multi-Release JAR:**

```bash
# Compile for different versions
javac --release 8 -d classes-8 src/main/java/com/example/Util.java
javac --release 9 -d classes-9 src/main/java/com/example/Util.java

# Package
jar --create --file mylib.jar \
    --main-class com.example.Main \
    -C classes-8 . \
    --release 9 -C classes-9 .
```

---

## Layer 3 — Advanced Engineering Depth

### Strong Encapsulation Internals

**What is Strong Encapsulation?**

Modules enforce access control at compile-time AND runtime. Unlike traditional access modifiers, module boundaries cannot be bypassed.

**How It Works:**

```java
// Module A
module com.example.a {
    exports com.example.a.public;
    // com.example.a.internal is NOT exported
}

// Module B tries to access internal
module com.example.b {
    requires com.example.a;
}

// In Module B code:
import com.example.a.internal.InternalClass;  // ❌ Compile error!
```

**Reflection Cannot Bypass:**

```java
// Even reflection is blocked
Class<?> clazz = Class.forName("com.example.a.internal.InternalClass");
// ❌ IllegalAccessError at runtime
```

**Only `opens` Allows Reflection:**

```java
module com.example.a {
    opens com.example.a.internal;  // Allows reflection
}

// Now reflection works
Class<?> clazz = Class.forName("com.example.a.internal.InternalClass");
// ✅ Works
```

### Module Resolution Algorithm

**How Java Resolves Modules:**

1. **Root Modules**: Modules specified with `-m` or main class modules
2. **Dependency Resolution**: Recursively resolve `requires`
3. **Conflict Detection**: Same module name with different versions = error
4. **Accessibility Check**: Only exported packages are accessible

**Resolution Steps:**

```
Step 1: Start with root modules
  com.example.app

Step 2: Resolve dependencies
  com.example.app requires com.example.utils
  com.example.app requires java.base

Step 3: Resolve transitive dependencies
  com.example.utils requires com.example.logging
  com.example.logging requires java.base (already resolved)

Step 4: Build module graph
  com.example.app
    ├── com.example.utils
    │   └── com.example.logging
    └── java.base
```

**Resolution Failures:**

```java
// Module not found
module com.example.app {
    requires nonexistent.module;  // ❌ Resolution error
}

// Circular dependency
module A { requires B; }
module B { requires A; }  // ❌ Circular dependency error

// Split package
module A { exports com.example.util; }
module B { exports com.example.util; }  // ❌ Split package error
```

### Split Packages Problem and Solutions

**What is a Split Package?**

Same package exists in multiple modules.

**Example:**

```java
// Module A
module com.example.a {
    exports com.example.util;
}

// Module B
module com.example.b {
    exports com.example.util;  // ❌ Split package!
}
```

**Why It's a Problem:**

- Ambiguity: Which module's classes are used?
- Breaks encapsulation: Packages should belong to one module
- Causes runtime errors

**Solutions:**

#### Solution 1: Consolidate Packages

Move all classes from the split package into one module.

```java
// Before: Split across modules
// After: All in one module
module com.example.util {
    exports com.example.util;
}
```

#### Solution 2: Rename Packages

Give each module its own package namespace.

```java
// Module A
module com.example.a {
    exports com.example.a.util;  // Unique namespace
}

// Module B
module com.example.b {
    exports com.example.b.util;  // Unique namespace
}
```

#### Solution 3: Use Automatic Modules

Keep one module as automatic (on classpath), convert others gradually.

### Reflection and Modules: `--add-opens` and `--add-reads`

**Runtime Workarounds for Reflection:**

Sometimes you need reflection access but can't modify module declarations.

#### `--add-opens`

Opens a package for reflection at runtime.

```bash
java --add-opens com.example.core/com.example.core.internal=ALL-UNNAMED \
     -m com.example.app/com.example.app.Main
```

**Use Cases:**
- Testing frameworks accessing internals
- Serialization libraries
- Legacy code migration

#### `--add-reads`

Adds a read edge between modules.

```bash
java --add-reads com.example.app=com.example.utils \
     -m com.example.app/com.example.app.Main
```

**Use Cases:**
- Dynamic module loading
- Plugin systems
- Testing scenarios

**Example:**

```java
// Module doesn't require another module
module com.example.app {
    // Missing: requires com.example.utils;
}

// Runtime workaround
java --add-reads com.example.app=com.example.utils \
     -m com.example.app/com.example.app.Main
```

### `jlink` for Custom Runtime Images

**What is jlink?**

Tool to create custom Java runtime images containing only needed modules.

**Problem**: Full JDK is ~300MB. Your app might only need 50MB.

**Solution**: jlink creates minimal runtime with only required modules.

**Basic Usage:**

```bash
# Create custom runtime
jlink --module-path $JAVA_HOME/jmods \
      --add-modules com.example.app \
      --output myruntime

# Result: myruntime/ directory with minimal JRE
```

**Advanced Example:**

```bash
jlink --module-path $JAVA_HOME/jmods:mods \
      --add-modules com.example.app \
      --add-modules java.base \
      --add-modules java.logging \
      --strip-debug \
      --compress=2 \
      --output myruntime
```

**Options Explained:**

- `--add-modules`: Modules to include
- `--strip-debug`: Remove debug info (smaller size)
- `--compress`: Compress resources (0-2, 2 = maximum)
- `--output`: Output directory

**Benefits:**

- Smaller deployment size
- Faster startup (fewer modules to load)
- Better security (only needed code)
- Perfect for containers/Docker

**Example Project:**

```bash
# Project structure
myapp/
├── mods/
│   ├── com.example.app/
│   └── com.example.utils/

# Create runtime
jlink --module-path $JAVA_HOME/jmods:mods \
      --add-modules com.example.app \
      --output myapp-runtime

# Runtime size: ~40MB (vs 300MB full JDK)

# Run application
myapp-runtime/bin/java -m com.example.app/com.example.app.Main
```

### Module Layers

**What are Module Layers?**

Multiple module graphs that can coexist, allowing dynamic module loading.

**Use Cases:**
- Plugin systems
- Application servers
- Testing frameworks
- Hot reloading

**Creating Layers:**

```java
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleLayer;

// Create a new layer
ModuleFinder finder = ModuleFinder.of(Paths.get("plugins"));
ModuleLayer parent = ModuleLayer.boot();  // Boot layer

Configuration config = parent.configuration()
    .resolve(finder, ModuleFinder.of(), Set.of("com.example.plugin"));

ModuleLayer layer = parent.defineModulesWithOneLoader(
    config, ClassLoader.getSystemClassLoader());

// Load module from layer
Class<?> pluginClass = layer.findLoader("com.example.plugin")
    .loadClass("com.example.plugin.Plugin");
```

**Layer Hierarchy:**

```
Boot Layer (java.base, java.logging, etc.)
    │
    ├── Application Layer (com.example.app)
    │
    └── Plugin Layer (com.example.plugin)
```

**Real-World Example: Plugin System**

```java
public class PluginManager {
    private final List<ModuleLayer> pluginLayers = new ArrayList<>();
    
    public void loadPlugin(Path pluginPath) {
        ModuleFinder finder = ModuleFinder.of(pluginPath);
        ModuleLayer parent = ModuleLayer.boot();
        
        Configuration config = parent.configuration()
            .resolve(finder, ModuleFinder.of(), 
                    finder.findAll().stream()
                          .map(ModuleReference::descriptor)
                          .map(ModuleDescriptor::name)
                          .collect(Collectors.toSet()));
        
        ModuleLayer layer = parent.defineModulesWithOneLoader(
            config, new URLClassLoader(new URL[0]));
        
        pluginLayers.add(layer);
    }
}
```

### Impact on Frameworks

#### Spring Framework

**Spring Boot and Modules:**

Spring Boot 2.1+ supports modules, but most applications still use classpath.

**Module Configuration:**

```java
module com.example.springapp {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.web;
    requires spring.context;
    
    opens com.example.app to spring.core;  // Reflection access
    opens com.example.app.entities to spring.beans, hibernate.core;
}
```

**Common Issues:**

1. **Reflection Access**: Spring uses reflection heavily
   ```java
   opens com.example.app to spring.core, spring.beans;
   ```

2. **Auto-Configuration**: Spring Boot scans classpath
   ```java
   // Use @ComponentScan explicitly
   @SpringBootApplication
   @ComponentScan(basePackages = "com.example.app")
   ```

3. **Third-Party Libraries**: Many don't support modules yet
   ```java
   // Use automatic modules
   requires my.library;  // Automatic module from JAR
   ```

#### Hibernate/JPA

**Module Configuration:**

```java
module com.example.jpaapp {
    requires java.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    
    opens com.example.app.entities to org.hibernate.orm.core;
    opens com.example.app.entities to org.hibernate.proxy;
}
```

**Key Points:**

- Entity classes need reflection access
- Use `opens` not `exports` (entities are not public API)
- Hibernate 5.3+ supports modules

### When NOT to Use Modules

**Modules are NOT always the right choice:**

#### 1. Small Applications

**When**: Simple applications with few dependencies

**Why**: Overhead of module declarations outweighs benefits

```java
// Small app - modules add complexity
module com.example.simpleapp {
    requires java.base;
}
// vs just using classpath
```

#### 2. Legacy Codebases

**When**: Large existing codebase without module support

**Why**: Migration cost is high, risk of breaking changes

**Better Approach**: Gradual migration or stay on classpath

#### 3. Libraries Without Module Support

**When**: Dependencies don't have module-info.java

**Why**: Automatic modules work but lose benefits

**Example:**

```java
module com.example.app {
    requires old.library;  // Automatic module - no strong encapsulation
}
```

#### 4. Rapid Prototyping

**When**: Quick prototypes, proof of concepts

**Why**: Classpath is faster to set up

#### 5. Microservices with Containers

**When**: Each service is small, containerized

**Why**: jlink benefits minimal, classpath simpler

**Decision Matrix:**

| Scenario | Use Modules? | Reason |
|----------|--------------|--------|
| Large enterprise app | ✅ Yes | Strong encapsulation, clear dependencies |
| Small utility | ❌ No | Overhead not worth it |
| Library development | ✅ Yes | Better API design |
| Legacy migration | ⚠️ Maybe | Gradual migration possible |
| Spring Boot app | ⚠️ Maybe | Works but not required |

---

## Layer 4 — Interview Mastery

### Explain JPMS to an Interviewer

**The Elevator Pitch:**

> "Java Platform Module System (JPMS) is Java's solution to the classpath problem. Introduced in Java 9, it provides strong encapsulation and explicit dependencies. Instead of everything being accessible on the classpath, modules must explicitly declare what they export and what they require. This prevents JAR hell, makes dependencies clear, and allows creating smaller runtime images with jlink."

**Key Points to Mention:**

1. **Problem Solved**: Classpath hell, weak encapsulation
2. **Core Concepts**: `module-info.java`, `exports`, `requires`
3. **Benefits**: Strong encapsulation, explicit dependencies, smaller runtimes
4. **Migration**: Can coexist with classpath (automatic modules)

**Follow-up Questions:**

**Q: Why was it introduced in Java 9?**

**A:** 
- Java had been around for 20+ years with classpath-based system
- Large applications suffered from dependency conflicts
- No way to hide internal APIs (everything was accessible)
- JRE was monolithic - had to ship entire runtime
- Modules solve all these problems

**Q: How does it differ from OSGi?**

**A:**
- OSGi: Dynamic, runtime module loading, multiple versions
- JPMS: Static, compile-time resolution, single version per module
- OSGi: More complex, used in application servers
- JPMS: Simpler, built into Java, better for most applications

### Classpath vs Module Path

**Comparison Table:**

| Aspect | Classpath | Module Path |
|--------|-----------|-------------|
| **Encapsulation** | Weak (all packages accessible) | Strong (only exported packages) |
| **Dependencies** | Implicit (discovered at runtime) | Explicit (declared in module-info.java) |
| **Resolution** | First match wins | Validated at startup |
| **Split Packages** | Allowed (problematic) | Not allowed (error) |
| **Reflection** | Full access | Only to opened packages |
| **JAR Hell** | Common problem | Prevented by module system |

**Code Example:**

```java
// Classpath approach
java -cp libs/* com.example.Main
// - All classes accessible
// - No dependency validation
// - First JAR wins conflicts

// Module path approach
java --module-path mods -m com.example/com.example.Main
// - Only exported packages accessible
// - Dependencies validated at startup
// - Conflicts cause errors (fail fast)
```

**When to Use Each:**

- **Classpath**: Legacy code, small apps, rapid prototyping
- **Module Path**: Large applications, libraries, production systems

### Why Reflection Breaks with Modules

**The Core Issue:**

Modules enforce encapsulation at runtime. Reflection tries to access classes, but modules block access to non-exported/non-opened packages.

**Example:**

```java
// Module A
module com.example.a {
    exports com.example.a.api;
    // com.example.a.internal is NOT exported
}

// Module B tries reflection
Class<?> clazz = Class.forName("com.example.a.internal.InternalClass");
// ❌ IllegalAccessError: class com.example.a.internal.InternalClass 
//    cannot be accessed
```

**Why This Happens:**

1. **Strong Encapsulation**: Modules enforce boundaries at runtime
2. **Security**: Prevents unauthorized access to internals
3. **Design Intent**: Internal classes should stay internal

**Solutions:**

1. **Use `opens`**:
   ```java
   module com.example.a {
       opens com.example.a.internal;  // Allow reflection
   }
   ```

2. **Qualified Opens**:
   ```java
   module com.example.a {
       opens com.example.a.internal to com.example.serializer;
   }
   ```

3. **Runtime Workaround**:
   ```bash
   java --add-opens com.example.a/com.example.a.internal=ALL-UNNAMED
   ```

**Framework Impact:**

- **Spring**: Needs `opens` for dependency injection
- **Hibernate**: Needs `opens` for entity mapping
- **Jackson**: Needs `opens` for JSON serialization

### How to Migrate Legacy Applications

**Migration Strategy:**

#### Phase 1: Assessment

1. **Audit Dependencies**: List all JARs and their purposes
2. **Identify Split Packages**: Find packages used across multiple JARs
3. **Check Module Support**: See which libraries have module-info.java
4. **Map Dependencies**: Understand dependency graph

#### Phase 2: Start with Automatic Modules

```bash
# Move JARs to module path (no code changes needed)
java --module-path libs/* -m com.example.app/com.example.app.Main
```

**Benefits:**
- No code changes
- Test module system
- Identify issues early

#### Phase 3: Create module-info.java

```java
module com.example.app {
    requires java.base;
    requires java.logging;
    requires my.library;  // Automatic module
    // Add requires as you discover dependencies
}
```

#### Phase 4: Handle Reflection

```java
module com.example.app {
    // Open packages used by frameworks
    opens com.example.app.entities to org.hibernate.orm.core;
    opens com.example.app to spring.core;
}
```

#### Phase 5: Gradual Conversion

As dependencies release module versions:

```java
module com.example.app {
    requires my.library;  // Now a proper named module
}
```

**Common Migration Challenges:**

1. **Split Packages**
   - **Solution**: Consolidate or rename packages

2. **Internal API Usage**
   - **Solution**: Use public APIs or `--add-opens` temporarily

3. **Dynamic Class Loading**
   - **Solution**: Use ModuleLayer API

4. **Testing**
   - **Solution**: Use `--add-opens` for test access

**Example Migration:**

```java
// Before: Classpath application
// com/example/app/Main.java
public class Main {
    public static void main(String[] args) {
        // Uses internal.util.Helper (from another JAR)
    }
}

// Step 1: Create module-info.java
module com.example.app {
    requires internal.util;  // Automatic module
}

// Step 2: Fix access issues
// If internal.util doesn't export needed package:
// Option A: Use --add-opens (temporary)
// Option B: Contact library maintainer
// Option C: Find alternative library
```

### Pros and Cons of Modular Architecture

#### Pros ✅

1. **Strong Encapsulation**
   - Internal APIs truly hidden
   - Prevents accidental dependencies
   - Better API design

2. **Explicit Dependencies**
   - Clear what code depends on
   - Easier to understand architecture
   - Fail-fast on missing dependencies

3. **Smaller Runtime Images**
   - jlink creates minimal JREs
   - Perfect for containers
   - Faster startup

4. **Better Tooling**
   - IDEs show module graph
   - Better refactoring support
   - Clearer error messages

5. **Prevents JAR Hell**
   - No version conflicts
   - Clear module boundaries
   - Validated at startup

#### Cons ❌

1. **Learning Curve**
   - New concepts to learn
   - Different from classpath
   - Steeper learning curve

2. **Migration Effort**
   - Existing code needs updates
   - Dependencies may not support modules
   - Time-consuming process

3. **Reflection Complexity**
   - Frameworks need `opens`
   - More configuration needed
   - Can be verbose

4. **Ecosystem Readiness**
   - Not all libraries support modules
   - Automatic modules lose benefits
   - Mixed module/classpath can be confusing

5. **Overhead for Small Projects**
   - Module declarations add complexity
   - Benefits minimal for small apps
   - Classpath simpler for prototypes

**When to Choose Modules:**

✅ **Choose modules when:**
- Large application (>100K LOC)
- Library development
- Need strong encapsulation
- Deploying to containers
- Long-term maintenance important

❌ **Avoid modules when:**
- Small utility applications
- Rapid prototyping
- Legacy codebase migration too costly
- Dependencies don't support modules
- Team unfamiliar with modules

**Interview Answer Template:**

> "Modules provide strong encapsulation and explicit dependencies, which is great for large applications. However, they add complexity and require ecosystem support. For a new large-scale project, I'd recommend modules. For a small utility or legacy migration, I'd evaluate the cost-benefit. The key is understanding when the benefits outweigh the overhead."

### Advanced Interview Questions

**Q: What happens if two modules export the same package?**

**A:** This creates a "split package" situation, which causes a module resolution error. The module system doesn't allow the same package to be exported by multiple modules because it creates ambiguity about which module's classes should be used.

**Q: Can a module require itself?**

**A:** No, a module cannot require itself. This would create a circular dependency on itself, which doesn't make sense. The module system will reject such a declaration.

**Q: What's the difference between `exports` and `opens`?**

**A:** 
- `exports`: Makes package accessible for compile-time and runtime access (normal imports)
- `opens`: Allows reflection access only (for frameworks like Spring, Hibernate)
- A package can be both exported and opened if needed

**Q: How do you test modules?**

**A:** 
- Use `--add-opens` to open packages for test access
- Create test modules that require the module under test
- Use ModuleLayer API for dynamic testing scenarios
- Many testing frameworks handle this automatically

**Q: What is the unnamed module?**

**A:** The unnamed module contains all code on the classpath (traditional Java). It can read all other modules but cannot be required by named modules. It's used for legacy code and gradual migration.

---

[← Back to Index](00-README.md) | [Previous: Design Patterns](14-Design-Patterns.md) | [Next: Performance →](16-Performance.md)
