# Chapter 10: JVM Internals

[← Back to Index](00-README.md) | [Previous: GC & Memory](09-GC-Memory.md) | [Next: File I/O →](11-File-IO.md)

---

## Why This Chapter Matters

> **The Reality**: Understanding JVM internals separates senior engineers from junior ones. 
> When production issues occur—memory leaks, performance degradation, or mysterious crashes—
> you need to know what's happening under the hood.

**Why it matters:**
- **Debugging**: Stack traces, OutOfMemoryErrors, and performance issues require JVM knowledge
- **Performance Tuning**: JVM flags and optimizations can make or break application performance
- **Interview Success**: JVM internals are a favorite topic for senior engineering interviews
- **Production Readiness**: Understanding bytecode, class loading, and JIT compilation helps diagnose real-world issues

Without JVM internals knowledge, you're flying blind when things go wrong.

---

## Layer 1 — Beginner Foundation

### What is the JVM?

The **Java Virtual Machine (JVM)** is a runtime engine that executes Java bytecode. It provides:
- **Platform Independence**: Write once, run anywhere (WORA)
- **Memory Management**: Automatic garbage collection
- **Security**: Sandboxed execution environment

```
┌────────────────────────────────────────────────────────────────────┐
│                    JVM ARCHITECTURE OVERVIEW                       │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    CLASS LOADER SUBSYSTEM                     │  │
│  │  • Loading: Reads .class files                               │  │
│  │  • Linking: Verifies, prepares, resolves                    │  │
│  │  • Initialization: Executes static initializers              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    RUNTIME DATA AREAS                         │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │  │
│  │  │ Method Area  │  │     Heap     │  │    Stack     │       │  │
│  │  │ (Class data) │  │  (Objects)   │  │  (Frames)    │       │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘       │  │
│  │  ┌──────────────┐  ┌──────────────┐                         │  │
│  │  │ PC Register  │  │ Native Stack │                         │  │
│  │  └──────────────┘  └──────────────┘                         │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              EXECUTION ENGINE                                 │  │
│  │  • Interpreter: Executes bytecode line by line               │  │
│  │  • JIT Compiler: Compiles hot code to native machine code    │  │
│  │  • Garbage Collector: Manages heap memory                    │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

### Runtime Data Areas

The JVM divides memory into several runtime data areas:

#### 1. Method Area (Metaspace in Java 8+)

Stores:
- Class metadata (class name, methods, fields)
- Static variables
- Constant pool
- Method bytecode

```java
public class Example {
    private static int count = 0;  // Stored in Method Area
    private String name;            // Field info stored here
    
    public void method() {          // Method bytecode stored here
        // ...
    }
}
```

#### 2. Heap Memory

Stores all object instances. Shared across all threads.

```
┌─────────────────────────────────────────────────────────────────┐
│                         HEAP MEMORY                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              YOUNG GENERATION                              │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐                  │  │
│  │  │  Eden    │ │Survivor 0│ │Survivor 1│                  │  │
│  │  └──────────┘ └──────────┘ └──────────┘                  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              OLD GENERATION                                │  │
│  │         (Tenured Space)                                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 3. Stack Memory (Per Thread)

Each thread has its own stack. Stores:
- Local variables
- Method parameters
- Return addresses
- Method frames

```java
public class StackExample {
    public static void main(String[] args) {
        int x = 10;              // Local variable in main() frame
        int y = 20;              // Local variable in main() frame
        int result = add(x, y);  // Creates new frame for add()
        System.out.println(result);
    }
    
    public static int add(int a, int b) {
        // New stack frame created here
        // a and b are local variables in this frame
        int sum = a + b;         // Local variable in add() frame
        return sum;              // Frame destroyed when method returns
    }
}
```

**Stack Frame Structure:**
```
┌─────────────────────────────────────────────────────────────┐
│                    STACK FRAME                              │
├─────────────────────────────────────────────────────────────┤
│  Local Variables Array                                      │
│  ┌──────┐ ┌──────┐ ┌──────┐                                │
│  │ var1 │ │ var2 │ │ var3 │                                │
│  └──────┘ └──────┘ └──────┘                                │
│                                                             │
│  Operand Stack (for calculations)                           │
│  ┌──────┐                                                   │
│  │ val1 │                                                   │
│  └──────┘                                                   │
│                                                             │
│  Frame Data (return address, reference to constant pool)    │
└─────────────────────────────────────────────────────────────┘
```

#### 4. PC Register (Program Counter)

Each thread has a PC register that points to the current instruction being executed.

#### 5. Native Method Stack

Used for native (non-Java) method calls. Separate from Java stack.

### Class File Structure Basics

A `.class` file contains:
- **Magic Number**: `0xCAFEBABE` (identifies Java class file)
- **Version Info**: Major and minor version numbers
- **Constant Pool**: Literals, class names, method names, etc.
- **Access Flags**: public, private, final, abstract, etc.
- **This Class**: Reference to current class
- **Super Class**: Reference to parent class
- **Interfaces**: Implemented interfaces
- **Fields**: Field information
- **Methods**: Method information
- **Attributes**: Additional metadata (source file, line numbers, etc.)

```java
// Simple.java
public class Simple {
    private int value = 42;
    
    public int getValue() {
        return value;
    }
}
```

**Class File Structure (Simplified):**
```
┌─────────────────────────────────────────────────────────────┐
│                    CLASS FILE STRUCTURE                      │
├─────────────────────────────────────────────────────────────┤
│  Magic Number: 0xCAFEBABE                                   │
│  Version: 61.0 (Java 17)                                    │
│  Constant Pool Count: 15                                     │
│  Constant Pool:                                              │
│    [1] Utf8 "Simple"                                         │
│    [2] Class #1                                              │
│    [3] Utf8 "value"                                          │
│    [4] Integer 42                                            │
│    ...                                                       │
│  Access Flags: ACC_PUBLIC                                    │
│  This Class: #2                                              │
│  Super Class: java/lang/Object                               │
│  Fields Count: 1                                             │
│  Methods Count: 2                                            │
│  Attributes Count: 1                                         │
└─────────────────────────────────────────────────────────────┘
```

### Understanding Bytecode

Bytecode is the intermediate representation that JVM executes. Let's see what bytecode looks like:

```java
public class BytecodeExample {
    public int calculate(int a, int b) {
        int result = a + b;
        return result;
    }
}
```

**Compiled Bytecode (javap -c output):**
```
public int calculate(int, int);
  Code:
     0: iload_1        // Load local variable 1 (parameter 'a') onto stack
     1: iload_2        // Load local variable 2 (parameter 'b') onto stack
     2: iadd           // Add top two int values on stack
     3: istore_3       // Store result into local variable 3 ('result')
     4: iload_3        // Load local variable 3 ('result') onto stack
     5: ireturn        // Return int value from stack
```

**Common Bytecode Instructions:**

| Instruction | Meaning | Example |
|------------|---------|---------|
| `iload_n` | Load int from local variable n | `iload_1` loads local var 1 |
| `istore_n` | Store int into local variable n | `istore_3` stores into var 3 |
| `iadd` | Add two integers | Pops two ints, pushes sum |
| `isub` | Subtract two integers | Pops two ints, pushes difference |
| `imul` | Multiply two integers | Pops two ints, pushes product |
| `iconst_n` | Push integer constant n | `iconst_0` pushes 0 |
| `bipush n` | Push byte value n | `bipush 10` pushes 10 |
| `aload_n` | Load object reference | `aload_0` loads 'this' |
| `astore_n` | Store object reference | `astore_1` stores ref |
| `invokevirtual` | Call instance method | `invokevirtual #5` |
| `invokestatic` | Call static method | `invokestatic #10` |
| `return` | Return void | End of void method |
| `ireturn` | Return int | End of int method |
| `areturn` | Return reference | End of Object method |

**Example: Understanding Bytecode Execution**

```java
public class MathExample {
    public static int multiply(int x, int y) {
        return x * y;
    }
    
    public static void main(String[] args) {
        int result = multiply(5, 3);
        System.out.println(result);
    }
}
```

**Bytecode for multiply():**
```
public static int multiply(int, int);
  Code:
     0: iload_0        // Load first parameter (x) onto stack
     1: iload_1        // Load second parameter (y) onto stack
     2: imul           // Multiply: stack now has (x * y)
     3: ireturn        // Return the result
```

**Execution Flow:**
```
Step 0: Stack = []           Local vars: [x=5, y=3]
Step 1: Stack = [5]          (after iload_0)
Step 2: Stack = [5, 3]       (after iload_1)
Step 3: Stack = [15]         (after imul: 5*3=15)
Step 4: Return 15            (after ireturn)
```

---

## Layer 2 — Working Developer Level

### JVM Startup Process

When you run `java MyClass`, the JVM goes through these steps:

```
┌─────────────────────────────────────────────────────────────┐
│                    JVM STARTUP PROCESS                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Load JVM Library                                        │
│     └─> Native library (jvm.dll/libjvm.so)                 │
│                                                             │
│  2. Initialize JVM                                         │
│     └─> Create JVM instance                                 │
│                                                             │
│  3. Create Bootstrap Class Loader                           │
│     └─> Loads core Java classes (java.lang.*)              │
│                                                             │
│  4. Create JVM Thread                                       │
│     └─> Main thread for execution                           │
│                                                             │
│  5. Link and Initialize Main Class                           │
│     └─> Load, verify, prepare, resolve, initialize         │
│                                                             │
│  6. Execute main() Method                                   │
│     └─> Your application code starts running                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Startup Sequence:**
```java
// When you run: java -Xms512m -Xmx2g MyApp

1. JVM loads and initializes
2. Bootstrap classloader loads java.lang.Object, String, etc.
3. Application classloader loads MyApp.class
4. Static initializers run (if any)
5. main() method executes
```

### Common JVM Options

#### Memory Options

```bash
# Heap Size
-Xms512m          # Initial heap size (512 MB)
-Xmx2g            # Maximum heap size (2 GB)
-Xmn256m          # Young generation size (256 MB)

# Metaspace (Java 8+)
-XX:MetaspaceSize=128m      # Initial metaspace size
-XX:MaxMetaspaceSize=512m   # Maximum metaspace size

# PermGen (Java 7 and earlier - deprecated)
-XX:PermSize=128m           # Initial permgen size
-XX:MaxPermSize=256m        # Maximum permgen size
```

#### GC Options

```bash
# Garbage Collector Selection
-XX:+UseG1GC                 # Use G1 garbage collector
-XX:+UseParallelGC           # Use Parallel GC
-XX:+UseConcMarkSweepGC      # Use CMS GC (deprecated in Java 14+)
-XX:+UseZGC                   # Use ZGC (Java 11+)

# GC Logging
-Xlog:gc*:file=gc.log        # Log all GC events to file
-XX:+PrintGCDetails          # Print detailed GC info (deprecated)
-XX:+PrintGCDateStamps       # Include timestamps
```

#### Performance Options

```bash
# JIT Compiler
-XX:+TieredCompilation       # Enable tiered compilation (default)
-XX:CompileThreshold=10000   # Method invocation threshold for compilation

# Debugging
-XX:+HeapDumpOnOutOfMemoryError    # Create heap dump on OOM
-XX:HeapDumpPath=/path/to/dump.hprof
-XX:+PrintFlagsFinal         # Print all JVM flags
```

#### Common Flag Patterns

```bash
# Production-ready JVM flags
java -Xms2g \
     -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -Xlog:gc*:file=gc.log:time,level,tags \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/heapdump.hprof \
     -jar myapp.jar
```

### Understanding Stack Traces Deeply

A stack trace shows the call chain when an exception occurs or when you request it.

```java
public class StackTraceExample {
    public static void main(String[] args) {
        method1();
    }
    
    public static void method1() {
        method2();
    }
    
    public static void method2() {
        method3();
    }
    
    public static void method3() {
        throw new RuntimeException("Error occurred!");
    }
}
```

**Stack Trace Output:**
```
Exception in thread "main" java.lang.RuntimeException: Error occurred!
    at StackTraceExample.method3(StackTraceExample.java:15)
    at StackTraceExample.method2(StackTraceExample.java:11)
    at StackTraceExample.method1(StackTraceExample.java:7)
    at StackTraceExample.main(StackTraceExample.java:3)
```

**Reading a Stack Trace:**
```
┌─────────────────────────────────────────────────────────────┐
│                    STACK TRACE ANATOMY                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Exception Type: java.lang.RuntimeException                 │
│  Message: "Error occurred!"                                 │
│                                                             │
│  Call Stack (bottom to top):                                │
│  ┌───────────────────────────────────────────────────────┐ │
│  │ main()                    ← Entry point               │ │
│  │   └─> method1()           ← Called from main          │ │
│  │       └─> method2()       ← Called from method1       │ │
│  │           └─> method3()   ← Called from method2       │ │
│  │               └─> throw   ← Exception thrown HERE     │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  Each line shows:                                           │
│  • Class name                                               │
│  • Method name                                              │
│  • File name                                                │
│  • Line number                                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Programmatic Stack Trace Access:**

```java
public class StackTraceDemo {
    public static void main(String[] args) {
        printStackTrace();
    }
    
    public static void printStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        System.out.println("Stack Trace Elements:");
        for (StackTraceElement element : stackTrace) {
            System.out.printf("  %s.%s(%s:%d)%n",
                element.getClassName(),
                element.getMethodName(),
                element.getFileName(),
                element.getLineNumber());
        }
    }
}
```

**Output:**
```
Stack Trace Elements:
  java.lang.Thread.getStackTrace(Thread.java:1559)
  StackTraceDemo.printStackTrace(StackTraceDemo.java:8)
  StackTraceDemo.main(StackTraceDemo.java:3)
```

### Monitoring Tools

#### jps - Java Process Status

Lists all running Java processes.

```bash
# List all Java processes
jps

# Output:
# 12345 MyApplication
# 67890 AnotherApp

# List with main class and arguments
jps -lvm

# Output:
# 12345 com.example.MyApplication -Xms512m -Xmx2g
```

#### jstat - JVM Statistics

Monitors JVM statistics in real-time.

```bash
# GC statistics (every 1 second, 10 times)
jstat -gc <pid> 1000 10

# Output columns:
# S0C: Survivor 0 capacity (KB)
# S1C: Survivor 1 capacity (KB)
# S0U: Survivor 0 used (KB)
# S1U: Survivor 1 used (KB)
# EC: Eden capacity (KB)
# EU: Eden used (KB)
# OC: Old generation capacity (KB)
# OU: Old generation used (KB)
# MC: Metaspace capacity (KB)
# MU: Metaspace used (KB)
# YGC: Young GC count
# YGCT: Young GC time (seconds)
# FGC: Full GC count
# FGCT: Full GC time (seconds)
# GCT: Total GC time (seconds)

# Compiler statistics
jstat -compiler <pid>

# Class loader statistics
jstat -class <pid>
```

**Example jstat Output:**
```
S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT
5120.0 5120.0  0.0   2560.0  40960.0  20480.0   102400.0   51200.0  20480  18000  2560   2000   15      0.120   2      0.250    0.370
```

#### jmap - Memory Map

Generates heap dumps and memory statistics.

```bash
# Heap summary
jmap -heap <pid>

# Generate heap dump
jmap -dump:format=b,file=heapdump.hprof <pid>

# Live objects only (smaller dump)
jmap -dump:live,format=b,file=heapdump.hprof <pid>

# Histogram of objects
jmap -histo <pid>

# Histogram output example:
# num     #instances         #bytes  class name
# ----------------------------------------------
#   1:       1000000     100000000  [B
#   2:        500000      50000000  java.lang.String
#   3:        100000      10000000  java.util.HashMap$Node
```

#### jstack - Stack Trace

Prints thread dumps for a Java process.

```bash
# Print thread dump
jstack <pid>

# Thread dump to file
jstack <pid> > threaddump.txt

# Thread dump with locks
jstack -l <pid>
```

**Thread Dump Example:**
```
"main" #1 prio=5 os_prio=0 tid=0x00007f8b8c00a800 nid=0x1234 runnable [0x00007f8b9c5f0000]
   java.lang.Thread.State: RUNNABLE
        at java.io.FileInputStream.readBytes(Native Method)
        at java.io.FileInputStream.read(FileInputStream.java:255)
        at java.io.BufferedInputStream.fill(BufferedInputStream.java:246)
        at java.io.BufferedInputStream.read1(BufferedInputStream.java:286)
        at java.io.BufferedInputStream.read(BufferedInputStream.java:345)
        - locked <0x000000076ab62208> (a java.io.BufferedInputStream)
        at MyApp.readFile(MyApp.java:42)
        at MyApp.main(MyApp.java:15)
```

#### jcmd - Java Command

Multi-purpose tool for JVM diagnostics.

```bash
# List all available commands
jcmd <pid> help

# GC.run - Run GC
jcmd <pid> GC.run

# VM.flags - Print JVM flags
jcmd <pid> VM.flags

# Thread.print - Print thread dump
jcmd <pid> Thread.print

# GC.class_histogram - Object histogram
jcmd <pid> GC.class_histogram

# VM.native_memory - Native memory usage
jcmd <pid> VM.native_memory summary

# Generate heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.classloader_stats
```

### VisualVM and JConsole Basics

#### VisualVM

VisualVM is a visual tool for monitoring JVM performance.

**Features:**
- **Overview**: CPU, memory, threads, classes
- **Monitor**: Real-time graphs of heap, CPU, threads
- **Threads**: Thread states and stack traces
- **Sampler**: CPU and memory sampling
- **Profiler**: Method-level profiling

**Usage:**
```bash
# Launch VisualVM
jvisualvm

# Or with specific JDK
/path/to/jdk/bin/jvisualvm
```

**Key Tabs:**
1. **Overview**: General JVM information
2. **Monitor**: Real-time performance graphs
3. **Threads**: Thread visualization and dumps
4. **Sampler**: CPU and memory sampling
5. **Profiler**: Detailed method profiling

#### JConsole

JConsole is a monitoring and management console.

**Usage:**
```bash
# Launch JConsole
jconsole

# Connect to local process
jconsole <pid>

# Connect to remote process
jconsole hostname:port
```

**Key Tabs:**
1. **Overview**: Memory, threads, classes, CPU usage
2. **Memory**: Heap and non-heap memory usage
3. **Threads**: Thread information and deadlock detection
4. **Classes**: Loaded classes count
5. **VM Summary**: JVM information
6. **MBeans**: MBean browser

**Example: Monitoring Memory Leak**

```java
import java.util.ArrayList;
import java.util.List;

public class MemoryLeakDemo {
    private static List<Object> list = new ArrayList<>();
    
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            // Continuously adding objects without removing
            for (int i = 0; i < 1000; i++) {
                list.add(new byte[1024 * 1024]); // 1MB objects
            }
            Thread.sleep(100);
            
            // In VisualVM/JConsole, you'll see heap growing continuously
            System.out.println("List size: " + list.size());
        }
    }
}
```

**Monitoring Checklist:**
- ✅ Heap usage trending upward
- ✅ GC frequency increasing
- ✅ GC pause times increasing
- ✅ Old generation filling up
- ✅ Thread count stable or increasing

---

## Layer 3 — Advanced Engineering Depth

### Bytecode Instructions Deep Dive

#### Method Invocation Instructions

The JVM has different bytecode instructions for different method types:

**1. invokespecial**
- Calls instance initialization methods (`<init>`)
- Calls private methods
- Calls methods in the same class

```java
public class InvokeSpecialExample {
    private void privateMethod() {
        System.out.println("Private method");
    }
    
    public InvokeSpecialExample() {
        this.privateMethod();  // Uses invokespecial
    }
}
```

**Bytecode:**
```
public InvokeSpecialExample();
  Code:
     0: aload_0
     1: invokespecial #1  // Method java/lang/Object."<init>":()V
     4: aload_0
     5: invokespecial #2  // Method privateMethod:()V
     8: return
```

**2. invokevirtual**
- Calls instance methods with virtual dispatch
- Most common for instance method calls
- Supports polymorphism

```java
public class InvokeVirtualExample {
    public void instanceMethod() {
        System.out.println("Instance method");
    }
    
    public static void main(String[] args) {
        InvokeVirtualExample obj = new InvokeVirtualExample();
        obj.instanceMethod();  // Uses invokevirtual
    }
}
```

**Bytecode:**
```
public static void main(java.lang.String[]);
  Code:
     0: new           #2  // class InvokeVirtualExample
     3: dup
     4: invokespecial #3  // Method "<init>":()V
     7: astore_1
     8: aload_1
     9: invokevirtual #4  // Method instanceMethod:()V
    12: return
```

**3. invokeinterface**
- Calls interface methods
- Similar to invokevirtual but for interfaces
- Requires interface method resolution

```java
interface MyInterface {
    void interfaceMethod();
}

public class InvokeInterfaceExample implements MyInterface {
    @Override
    public void interfaceMethod() {
        System.out.println("Interface method");
    }
    
    public static void main(String[] args) {
        MyInterface obj = new InvokeInterfaceExample();
        obj.interfaceMethod();  // Uses invokeinterface
    }
}
```

**Bytecode:**
```
public static void main(java.lang.String[]);
  Code:
     0: new           #2  // class InvokeInterfaceExample
     3: dup
     4: invokespecial #3  // Method "<init>":()V
     7: astore_1
     8: aload_1
     9: invokeinterface #4, 1  // InterfaceMethod MyInterface.interfaceMethod:()V
    14: return
```

**4. invokestatic**
- Calls static methods
- No object instance needed
- Direct method call

```java
public class InvokeStaticExample {
    public static void staticMethod() {
        System.out.println("Static method");
    }
    
    public static void main(String[] args) {
        staticMethod();  // Uses invokestatic
    }
}
```

**Bytecode:**
```
public static void main(java.lang.String[]);
  Code:
     0: invokestatic  #2  // Method staticMethod:()V
     3: return
```

**5. invokedynamic (Java 7+)**
- Dynamic method invocation
- Used for lambda expressions, method references
- Allows runtime method resolution

```java
import java.util.function.Function;

public class InvokeDynamicExample {
    public static void main(String[] args) {
        Function<String, Integer> func = s -> s.length();
        // Lambda uses invokedynamic
        int length = func.apply("Hello");
        System.out.println(length);
    }
}
```

**Bytecode (simplified):**
```
public static void main(java.lang.String[]);
  Code:
     0: invokedynamic #2, 0  // InvokeDynamic #0:apply:()Ljava/util/function/Function;
     5: astore_1
     6: aload_1
     7: ldc           #3  // String Hello
     9: invokeinterface #4, 2  // InterfaceMethod Function.apply:(Ljava/lang/Object;)Ljava/lang/Object;
    12: checkcast     #5  // class java/lang/Integer
    15: invokevirtual #6  // Method Integer.intValue:()I
    18: istore_2
    19: getstatic     #7  // Field System.out:Ljava/io/PrintStream;
    22: iload_2
    23: invokevirtual #8  // Method PrintStream.println:(I)V
    26: return
```

### Method Dispatch Mechanisms

#### Virtual Method Dispatch

Virtual dispatch enables polymorphism. The JVM uses virtual method tables (vtables).

```
┌─────────────────────────────────────────────────────────────┐
│              VIRTUAL METHOD DISPATCH                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  class Animal {                                             │
│      void makeSound() { ... }  ← vtable[0]                 │
│      void move() { ... }        ← vtable[1]                │
│  }                                                           │
│                                                             │
│  class Dog extends Animal {                                 │
│      void makeSound() { ... }  ← vtable[0] (overridden)     │
│      void move() { ... }        ← vtable[1] (inherited)    │
│      void bark() { ... }        ← vtable[2] (new method)    │
│  }                                                           │
│                                                             │
│  Animal animal = new Dog();                                 │
│  animal.makeSound();  ← Looks up vtable[0] in Dog class    │
│                        Calls Dog.makeSound()                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Example:**
```java
class Animal {
    public void makeSound() {
        System.out.println("Animal makes sound");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Dog barks");
    }
}

public class DispatchDemo {
    public static void main(String[] args) {
        Animal animal = new Dog();
        animal.makeSound();  // Virtual dispatch: calls Dog.makeSound()
    }
}
```

**VTable Structure:**
```
┌─────────────────────────────────────────────────────────────┐
│                    VTABLE (Virtual Method Table)             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Animal Class VTable:                                       │
│  ┌─────────────┐                                            │
│  │ [0] makeSound → Animal.makeSound()                      │
│  │ [1] move     → Animal.move()                            │
│  └─────────────┘                                            │
│                                                             │
│  Dog Class VTable:                                          │
│  ┌─────────────┐                                            │
│  │ [0] makeSound → Dog.makeSound()    (overridden)         │
│  │ [1] move     → Animal.move()       (inherited)          │
│  │ [2] bark     → Dog.bark()          (new method)         │
│  └─────────────┘                                            │
│                                                             │
│  When calling animal.makeSound():                           │
│  1. Get object's class (Dog)                                │
│  2. Look up vtable[0] in Dog's vtable                       │
│  3. Call Dog.makeSound()                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### Interface Method Dispatch

Interfaces use interface method tables (itable) for dispatch.

```java
interface Flyable {
    void fly();
}

class Bird implements Flyable {
    @Override
    public void fly() {
        System.out.println("Bird flies");
    }
}

public class InterfaceDispatch {
    public static void main(String[] args) {
        Flyable flyable = new Bird();
        flyable.fly();  // Interface dispatch via itable
    }
}
```

### Class Loading Architecture

#### Class Loader Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│              CLASS LOADER HIERARCHY                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    Bootstrap ClassLoader                    │
│                    (Native, loads rt.jar)                   │
│                            │                                │
│                            ▼                                │
│                 Platform/Extension ClassLoader               │
│                 (Loads platform classes)                    │
│                            │                                │
│                            ▼                                │
│                  Application/System ClassLoader              │
│                  (Loads application classes)                 │
│                            │                                │
│                            ▼                                │
│                    Custom ClassLoaders                       │
│                    (User-defined loaders)                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 1. Bootstrap ClassLoader

- Written in native code (C++)
- Loads core Java classes (`java.lang.*`, `java.util.*`, etc.)
- Parent is `null` (top of hierarchy)
- Loads from `rt.jar` or modules

#### 2. Platform/Extension ClassLoader

- Java implementation
- Loads platform extension classes
- Parent is Bootstrap ClassLoader
- In Java 9+: Platform ClassLoader (replaces Extension ClassLoader)

#### 3. Application/System ClassLoader

- Loads application classes from classpath
- Parent is Platform ClassLoader
- Default classloader for `Class.forName()`

**Class Loading Process:**

```java
public class ClassLoadingDemo {
    public static void main(String[] args) {
        // Show classloader hierarchy
        ClassLoader loader = ClassLoadingDemo.class.getClassLoader();
        System.out.println("Application ClassLoader: " + loader);
        System.out.println("Platform ClassLoader: " + loader.getParent());
        System.out.println("Bootstrap ClassLoader: " + loader.getParent().getParent());
        
        // Bootstrap classloader is null (native)
        ClassLoader bootstrap = String.class.getClassLoader();
        System.out.println("String classloader: " + bootstrap); // null
    }
}
```

**Output:**
```
Application ClassLoader: jdk.internal.loader.ClassLoaders$AppClassLoader@2f0e140b
Platform ClassLoader: jdk.internal.loader.ClassLoaders$PlatformClassLoader@279f2327
Bootstrap ClassLoader: null
String classloader: null
```

#### Class Loading Steps

```
┌─────────────────────────────────────────────────────────────┐
│              CLASS LOADING PROCESS                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. LOADING                                                 │
│     └─> Read .class file into memory                        │
│     └─> Create Class object                                │
│                                                             │
│  2. LINKING                                                 │
│     a) VERIFICATION                                         │
│        └─> Verify bytecode correctness                      │
│        └─> Check format, constraints                         │
│                                                             │
│     b) PREPARATION                                          │
│        └─> Allocate memory for static variables             │
│        └─> Initialize to default values                      │
│                                                             │
│     c) RESOLUTION                                           │
│        └─> Resolve symbolic references                      │
│        └─> Replace with direct references                   │
│                                                             │
│  3. INITIALIZATION                                          │
│     └─> Execute static initializers                         │
│     └─> Initialize static variables                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Example: Class Loading Sequence**

```java
public class ClassLoadSequence {
    static {
        System.out.println("1. Static block executed");
    }
    
    private static int value = initializeValue();
    
    private static int initializeValue() {
        System.out.println("2. Static variable initialized");
        return 42;
    }
    
    public static void main(String[] args) {
        System.out.println("3. Main method executed");
        System.out.println("Value: " + value);
    }
}
```

**Output:**
```
1. Static block executed
2. Static variable initialized
3. Main method executed
Value: 42
```

### Custom ClassLoaders

Custom classloaders allow loading classes from non-standard locations.

**When to Use Custom ClassLoaders:**
- Loading classes from network
- Hot-reloading classes (development)
- Isolating application modules
- Loading encrypted/obfuscated classes
- Plugin architectures

**Example: Custom ClassLoader**

```java
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class CustomClassLoader extends ClassLoader {
    private String classPath;
    
    public CustomClassLoader(String classPath) {
        this.classPath = classPath;
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classData = loadClassData(name);
        if (classData == null) {
            throw new ClassNotFoundException("Class not found: " + name);
        }
        return defineClass(name, classData, 0, classData.length);
    }
    
    private byte[] loadClassData(String className) {
        String fileName = className.replace('.', File.separatorChar) + ".class";
        File file = new File(classPath, fileName);
        
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            int byteValue;
            while ((byteValue = fis.read()) != -1) {
                baos.write(byteValue);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
```

**Usage:**
```java
public class CustomClassLoaderDemo {
    public static void main(String[] args) throws Exception {
        CustomClassLoader loader = new CustomClassLoader("/custom/class/path");
        Class<?> clazz = loader.loadClass("com.example.MyClass");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        // Use the instance...
    }
}
```

**Delegation Model:**

```java
public class DelegationDemo {
    public static void main(String[] args) {
        // ClassLoader delegation: child asks parent first
        CustomClassLoader customLoader = new CustomClassLoader("/path");
        
        // When loading a class:
        // 1. Check if already loaded (cache)
        // 2. Delegate to parent (Application → Platform → Bootstrap)
        // 3. If parent can't load, try custom loader
        // 4. If still not found, throw ClassNotFoundException
    }
}
```

### JIT Compilation Internals

#### Interpretation vs Compilation

```
┌─────────────────────────────────────────────────────────────┐
│         INTERPRETATION vs JIT COMPILATION                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  INTERPRETER:                                                │
│  ┌──────────┐                                               │
│  │ Bytecode │ ──> Interpret ──> Execute ──> Result          │
│  └──────────┘     (slow, but starts fast)                   │
│                                                             │
│  JIT COMPILER:                                               │
│  ┌──────────┐                                               │
│  │ Bytecode │ ──> Compile ──> Native Code ──> Execute      │
│  └──────────┘     (takes time, but runs fast)              │
│                                                             │
│  HYBRID APPROACH (Tiered Compilation):                      │
│  1. Start with interpreter (fast startup)                   │
│  2. Identify hot methods (frequently called)                 │
│  3. Compile hot methods to native code                      │
│  4. Use compiled code for subsequent calls                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### C1 and C2 Compilers

**C1 Compiler (Client Compiler):**
- Fast compilation
- Less optimization
- Good for startup performance
- Used for methods with low invocation count

**C2 Compiler (Server Compiler):**
- Slower compilation
- Aggressive optimizations
- Better for long-running applications
- Used for hot methods

**Tiered Compilation (Java 8+):**

```
┌─────────────────────────────────────────────────────────────┐
│              TIERED COMPILATION                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Level 0: Interpreter                                       │
│    └─> All methods start here                               │
│                                                             │
│  Level 1: C1 (Simple)                                      │
│    └─> Fast compilation, basic optimizations                │
│    └─> Triggered after ~1,500 invocations                   │
│                                                             │
│  Level 2: C1 (Full)                                        │
│    └─> More optimizations                                   │
│    └─> Triggered after ~10,000 invocations                 │
│                                                             │
│  Level 3: C2 (Full Optimization)                            │
│    └─> Aggressive optimizations                             │
│    └─> Triggered after ~10,000+ invocations                │
│                                                             │
│  Methods can be deoptimized if assumptions fail             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Compilation Thresholds:**

```bash
# View compilation thresholds
java -XX:+PrintFlagsFinal | grep CompileThreshold

# Output:
# intx CompileThreshold = 10000
# intx Tier2CompileThreshold = 0
# intx Tier3CompileThreshold = 2000
# intx Tier4CompileThreshold = 15000
```

#### Escape Analysis

Escape Analysis determines if an object escapes a method scope.

**Object Escapes If:**
- Returned from method
- Assigned to static field
- Passed to another method that stores it
- Assigned to instance field

**Optimizations Enabled by Escape Analysis:**

```java
public class EscapeAnalysisDemo {
    public int calculate(int x, int y) {
        // Point object doesn't escape this method
        Point p = new Point(x, y);
        return p.x + p.y;  // Can be optimized: no allocation needed!
    }
    
    // After optimization (conceptual):
    public int calculateOptimized(int x, int y) {
        // Point fields allocated on stack, not heap
        int px = x;
        int py = y;
        return px + py;  // No object allocation!
    }
}

class Point {
    int x, y;
    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
```

**Benefits:**
- **Stack Allocation**: Objects allocated on stack instead of heap
- **Scalar Replacement**: Object fields replaced with local variables
- **Lock Elision**: Locks on non-escaping objects can be removed

**Example: Lock Elision**

```java
public class LockElisionDemo {
    public synchronized void method() {
        // If 'this' doesn't escape, lock can be elided
        int value = 42;
        // ... work with value
    }
}
```

#### Method Inlining

Inlining replaces method calls with the method body.

```java
public class InliningDemo {
    public int add(int a, int b) {
        return a + b;  // Small method, good candidate for inlining
    }
    
    public int calculate(int x, int y) {
        return add(x, y);  // Call to add()
    }
    
    // After inlining (conceptual):
    public int calculateInlined(int x, int y) {
        return x + y;  // add() body inlined, no method call overhead
    }
}
```

**Inlining Benefits:**
- Eliminates method call overhead
- Enables further optimizations
- Better instruction cache usage

**Inlining Constraints:**
- Method must be small
- Method must be frequently called
- Method must not be overridden (or JIT knows exact type)

#### Loop Unrolling

Loop unrolling reduces loop overhead by executing multiple iterations.

```java
public class LoopUnrollingDemo {
    public int sumArray(int[] arr) {
        int sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum;
    }
    
    // After unrolling (conceptual, for arr.length = 8):
    public int sumArrayUnrolled(int[] arr) {
        int sum = 0;
        // Process 4 elements per iteration
        int i = 0;
        for (; i < arr.length - 3; i += 4) {
            sum += arr[i];
            sum += arr[i + 1];
            sum += arr[i + 2];
            sum += arr[i + 3];
        }
        // Handle remaining elements
        for (; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum;
    }
}
```

**Benefits:**
- Reduces loop control overhead
- Better instruction-level parallelism
- More opportunities for optimization

### Native Memory vs Heap Memory

```
┌─────────────────────────────────────────────────────────────┐
│              JVM MEMORY LAYOUT                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  HEAP MEMORY (Managed by GC)                                │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Young Generation (Eden, Survivor)                     │  │
│  │ Old Generation                                        │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  NON-HEAP MEMORY (Native Memory)                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Metaspace (Class metadata)                            │  │
│  │ Code Cache (Compiled native code)                     │  │
│  │ Compressed Class Space                                │  │
│  │ Thread Stacks (Per thread)                            │  │
│  │ Direct Memory (NIO buffers)                            │  │
│  │ GC Structures                                         │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Native Memory Components:**

1. **Metaspace**: Class metadata (replaces PermGen)
2. **Code Cache**: Compiled native code from JIT
3. **Thread Stacks**: Each thread's stack (default 1MB)
4. **Direct Memory**: NIO direct buffers
5. **GC Structures**: GC algorithm data structures

**Monitoring Native Memory:**

```bash
# Native memory summary
jcmd <pid> VM.native_memory summary

# Detailed native memory
jcmd <pid> VM.native_memory detail

# Native memory tracking (requires -XX:NativeMemoryTracking=summary)
java -XX:NativeMemoryTracking=summary MyApp
jcmd <pid> VM.native_memory baseline
jcmd <pid> VM.native_memory summary.diff
```

**Example: Native Memory Usage**

```java
import java.nio.ByteBuffer;

public class NativeMemoryDemo {
    public static void main(String[] args) {
        // Direct memory allocation (native)
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 100); // 100MB
        
        // Heap memory allocation
        byte[] heapArray = new byte[1024 * 1024 * 100]; // 100MB
        
        // Direct buffer uses native memory (not managed by GC)
        // Heap array uses heap memory (managed by GC)
        
        System.out.println("Direct buffer allocated: " + directBuffer.isDirect());
    }
}
```

### Metaspace Internals

Metaspace replaced PermGen in Java 8. It stores class metadata.

**Metaspace Structure:**

```
┌─────────────────────────────────────────────────────────────┐
│                    METASPACE                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Metaspace (Class Metadata):                                 │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Class definitions                                     │  │
│  │ Method metadata                                       │  │
│  │ Field metadata                                        │  │
│  │ Constant pool                                         │  │
│  │ Annotations                                           │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  Compressed Class Space (32-bit compressed pointers):       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Compressed class pointers (saves memory)              │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  Metaspace is allocated from native memory                  │
│  Grows automatically (up to MaxMetaspaceSize)               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Metaspace vs PermGen:**

| Feature | PermGen (Java 7) | Metaspace (Java 8+) |
|---------|------------------|---------------------|
| Location | Heap | Native Memory |
| Size | Fixed | Grows dynamically |
| GC | Full GC | Class unloading |
| OOM Risk | High (fixed size) | Lower (grows) |
| Tuning | -XX:PermSize, -XX:MaxPermSize | -XX:MetaspaceSize, -XX:MaxMetaspaceSize |

**Metaspace Tuning:**

```bash
# Metaspace size options
-XX:MetaspaceSize=128m          # Initial metaspace size
-XX:MaxMetaspaceSize=512m       # Maximum metaspace size
-XX:MinMetaspaceFreeRatio=40    # Minimum free ratio before GC
-XX:MaxMetaspaceFreeRatio=70    # Maximum free ratio before shrinking

# Metaspace GC logging
-Xlog:gc+metaspace*:file=metaspace.log
```

**Metaspace OOM Example:**

```java
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;

public class MetaspaceOOMDemo {
    public static void main(String[] args) throws Exception {
        // Continuously load classes to fill metaspace
        for (int i = 0; i < 100000; i++) {
            // Create new classloader
            URLClassLoader loader = new URLClassLoader(
                new URL[]{new URL("file:/path/to/classes/")}
            );
            
            // Load class
            Class<?> clazz = loader.loadClass("SomeClass");
            
            // Don't close loader - causes metaspace leak
            // loader.close(); // Should close to allow GC
        }
    }
}
```

**Preventing Metaspace Issues:**
- Monitor metaspace usage
- Set appropriate MaxMetaspaceSize
- Close custom classloaders when done
- Avoid dynamic class generation without cleanup
- Use classloader isolation properly

---

## Layer 4 — Interview Mastery

### Common Interview Questions

#### Q1: Explain the JVM architecture

**Answer:**

The JVM consists of three main subsystems:

1. **Class Loader Subsystem**: Loads, links, and initializes classes
   - Loading: Reads .class files
   - Linking: Verification, preparation, resolution
   - Initialization: Executes static initializers

2. **Runtime Data Areas**: Memory areas for execution
   - **Method Area**: Class metadata, static variables
   - **Heap**: Object instances (shared across threads)
   - **Stack**: Per-thread stack frames (local variables, method calls)
   - **PC Register**: Current instruction pointer per thread
   - **Native Method Stack**: For native method calls

3. **Execution Engine**: Executes bytecode
   - **Interpreter**: Executes bytecode line by line
   - **JIT Compiler**: Compiles hot code to native code
   - **Garbage Collector**: Manages heap memory

**Key Points:**
- JVM provides platform independence (WORA)
- Automatic memory management via GC
- Multi-threaded execution model
- Security sandbox for untrusted code

#### Q2: What is the difference between heap and stack memory?

**Answer:**

| Aspect | Heap Memory | Stack Memory |
|--------|-------------|--------------|
| **Purpose** | Stores objects and arrays | Stores method frames, local variables |
| **Scope** | Shared across all threads | Per-thread (each thread has own stack) |
| **Lifetime** | Objects live until GC'd | Frame destroyed when method returns |
| **Size** | Larger, configurable (-Xmx) | Smaller, fixed per thread (~1MB default) |
| **Speed** | Slower access | Faster access (LIFO structure) |
| **Management** | Managed by GC | Automatic (pushed/popped with method calls) |
| **Allocation** | Dynamic, can grow/shrink | Fixed size per thread |

**Example:**
```java
public class HeapStackDemo {
    private static Object staticObj = new Object();  // Heap
    
    public void method() {
        int localVar = 10;           // Stack (local variable)
        Object obj = new Object();   // obj reference: Stack
                                     // Object instance: Heap
    }
}
```

#### Q3: Explain method dispatch in Java

**Answer:**

Java uses **virtual method dispatch** for polymorphism:

1. **Virtual Method Table (VTable)**: Each class has a vtable containing method pointers
2. **Dynamic Dispatch**: Method call resolved at runtime based on actual object type
3. **Polymorphism**: Same method call can invoke different implementations

**Process:**
```
1. Object reference stored in variable
2. At method call, JVM checks object's actual class
3. Looks up method in that class's vtable
4. Calls the method from vtable
```

**Example:**
```java
class Animal {
    void makeSound() { System.out.println("Animal sound"); }
}

class Dog extends Animal {
    @Override
    void makeSound() { System.out.println("Bark"); }
}

Animal animal = new Dog();
animal.makeSound();  // Virtual dispatch: calls Dog.makeSound()
```

**Bytecode:**
- `invokevirtual`: For instance methods (virtual dispatch)
- `invokespecial`: For constructors, private methods, super calls
- `invokestatic`: For static methods (no dispatch needed)
- `invokeinterface`: For interface methods

#### Q4: What is the class loading process?

**Answer:**

Class loading happens in three phases:

**1. Loading:**
- ClassLoader reads .class file
- Creates Class object in memory
- Stores in Method Area

**2. Linking:**
   - **Verification**: Validates bytecode correctness
   - **Preparation**: Allocates memory for static variables, sets default values
   - **Resolution**: Resolves symbolic references to direct references

**3. Initialization:**
- Executes static initializers
- Initializes static variables
- Runs `<clinit>` method

**ClassLoader Hierarchy:**
```
Bootstrap → Platform → Application → Custom
```

**Delegation Model:**
- Child classloader delegates to parent first
- Only if parent can't load, child attempts loading
- Prevents duplicate class loading

**Example:**
```java
public class ClassLoadDemo {
    static {
        System.out.println("Static block - Initialization phase");
    }
    
    private static int value = 42;  // Preparation phase allocates, 
                                     // Initialization phase assigns
    
    public static void main(String[] args) {
        System.out.println("Main method");
    }
}
```

#### Q5: Explain JIT compilation and optimizations

**Answer:**

**JIT (Just-In-Time) Compilation:**
- Interpreter executes bytecode initially (fast startup)
- JIT identifies "hot" methods (frequently called)
- Compiles hot methods to native machine code
- Subsequent calls use compiled native code (much faster)

**Tiered Compilation (Java 8+):**
- **Level 0**: Interpreter
- **Level 1**: C1 compiler (fast, basic optimizations)
- **Level 2**: C1 compiler (more optimizations)
- **Level 3**: C2 compiler (aggressive optimizations)

**Key Optimizations:**

1. **Method Inlining**: Replace method calls with method body
   ```java
   int result = add(a, b);  // Becomes: int result = a + b;
   ```

2. **Escape Analysis**: Determine if object escapes method scope
   - Enables stack allocation
   - Enables lock elision
   - Enables scalar replacement

3. **Loop Unrolling**: Execute multiple loop iterations per iteration
   - Reduces loop overhead
   - Better instruction-level parallelism

4. **Dead Code Elimination**: Remove unreachable code

5. **Constant Folding**: Evaluate constant expressions at compile time

**Benefits:**
- Combines fast startup (interpreter) with fast execution (compiled code)
- Adapts to actual usage patterns
- Can deoptimize if assumptions fail

#### Q6: What causes OutOfMemoryError and how do you debug it?

**Answer:**

**Types of OutOfMemoryError:**

1. **Java heap space**: Heap is full
   ```java
   // Causes: Large objects, memory leaks, insufficient -Xmx
   List<Object> list = new ArrayList<>();
   while (true) {
       list.add(new byte[1024 * 1024]); // 1MB objects
   }
   ```

2. **Metaspace**: Class metadata area is full
   ```java
   // Causes: Too many classes loaded, classloader leaks
   // Solution: Increase -XX:MaxMetaspaceSize
   ```

3. **Direct buffer memory**: Native memory for NIO buffers
   ```java
   // Causes: Too many direct ByteBuffers
   ByteBuffer.allocateDirect(1024 * 1024 * 100);
   ```

4. **Unable to create native thread**: Too many threads
   ```java
   // Causes: Thread stack size too large or too many threads
   // Solution: Reduce -Xss or limit thread creation
   ```

**Debugging Steps:**

1. **Enable heap dump on OOM:**
   ```bash
   -XX:+HeapDumpOnOutOfMemoryError
   -XX:HeapDumpPath=/path/to/dump.hprof
   ```

2. **Analyze heap dump:**
   - Use Eclipse MAT, VisualVM, or jhat
   - Look for largest objects
   - Find memory leaks (objects that shouldn't be retained)

3. **Monitor memory:**
   ```bash
   jstat -gc <pid> 1000
   jmap -histo <pid>
   ```

4. **Check for leaks:**
   - Objects growing over time
   - Classes not being unloaded
   - Custom classloaders not closed

**Example Debugging:**
```java
// Memory leak example
public class MemoryLeak {
    private static List<Object> cache = new ArrayList<>();
    
    public void processData(Object data) {
        cache.add(data);  // Leak: never removes from cache
        // Should use bounded cache or cleanup
    }
}
```

#### Q7: Explain the difference between invokevirtual, invokespecial, invokeinterface, and invokedynamic

**Answer:**

**invokevirtual:**
- Most common for instance method calls
- Virtual dispatch (polymorphism)
- Looks up method in object's class vtable
- Used for public/protected instance methods

**invokespecial:**
- Direct method call (no virtual dispatch)
- Used for:
  - Constructors (`<init>`)
  - Private methods
  - `super.method()` calls
  - Methods in same class

**invokeinterface:**
- For interface method calls
- Similar to invokevirtual but for interfaces
- Uses interface method table (itable)
- Slightly slower than invokevirtual (more indirection)

**invokedynamic:**
- Dynamic method invocation (Java 7+)
- Runtime method resolution
- Used for:
  - Lambda expressions
  - Method references
  - Dynamic languages (e.g., JRuby)
- Most flexible, allows custom method resolution logic

**Example:**
```java
interface MyInterface {
    void method();
}

class MyClass implements MyInterface {
    public void method() { }           // invokeinterface
    
    private void privateMethod() { }   // invokespecial
    
    public void publicMethod() { }      // invokevirtual
    
    public MyClass() { }               // invokespecial (<init>)
    
    public static void main(String[] args) {
        MyInterface obj = new MyClass();
        obj.method();                   // invokeinterface
        
        MyClass instance = new MyClass();
        instance.publicMethod();        // invokevirtual
        instance.privateMethod();       // invokespecial
        
        Runnable r = () -> {};          // invokedynamic (lambda)
    }
}
```

#### Q8: What is a classloader leak and how do you prevent it?

**Answer:**

**Classloader Leak:**
- Custom ClassLoader not garbage collected
- Classes loaded by it remain in memory
- Metaspace fills up, causing OOM

**Common Causes:**

1. **Thread holding reference to classloader:**
   ```java
   public class LeakyClassLoader extends URLClassLoader {
       public LeakyClassLoader() {
           super(new URL[]{});
       }
   }
   
   // Leak: Thread uses class from loader
   Thread thread = new Thread(() -> {
       Class<?> clazz = loader.loadClass("MyClass");
       // Thread holds reference, loader never GC'd
   });
   ```

2. **Static references:**
   ```java
   public class LeakyApp {
       private static Class<?> loadedClass;  // Static reference!
       
       public void loadClass(ClassLoader loader) {
           loadedClass = loader.loadClass("MyClass");
           // loader can't be GC'd while loadedClass exists
       }
   }
   ```

3. **Not closing classloader:**
   ```java
   // Java 7+: ClassLoader implements Closeable
   URLClassLoader loader = new URLClassLoader(urls);
   try {
       Class<?> clazz = loader.loadClass("MyClass");
       // Use class...
   } finally {
       loader.close();  // Must close!
   }
   ```

**Prevention:**

1. **Close classloaders:**
   ```java
   try (URLClassLoader loader = new URLClassLoader(urls)) {
       Class<?> clazz = loader.loadClass("MyClass");
   }  // Automatically closed
   ```

2. **Avoid static references to loaded classes**

3. **Use weak references if needed:**
   ```java
   WeakReference<Class<?>> weakClass = new WeakReference<>(clazz);
   ```

4. **Monitor metaspace:**
   ```bash
   jstat -gc <pid> | grep MC  # Metaspace capacity
   ```

**Detection:**
```bash
# Check for classloader leaks
jmap -clstats <pid>

# Look for:
# - Growing number of classloaders
# - Classes not being unloaded
# - Metaspace growing continuously
```

### Tricky Scenarios

#### Scenario 1: Metaspace OOM in Production

**Symptoms:**
- `OutOfMemoryError: Metaspace`
- Application becomes unresponsive
- Restart required

**Investigation:**
```bash
# 1. Check metaspace usage
jstat -gc <pid> | grep MC

# 2. Check classloader stats
jmap -clstats <pid>

# 3. Generate heap dump
jmap -dump:format=b,file=dump.hprof <pid>
```

**Common Causes:**
- Dynamic class generation (proxies, reflection)
- Custom classloaders not being closed
- Hot-reloading in development leaking to production
- Too many classes loaded

**Solutions:**
```bash
# Increase metaspace
-XX:MaxMetaspaceSize=512m

# Enable metaspace GC logging
-Xlog:gc+metaspace*:file=metaspace.log

# Fix code: Close classloaders, limit dynamic class generation
```

#### Scenario 2: High GC Pause Times

**Symptoms:**
- Application freezes periodically
- GC logs show long pause times
- User complaints about latency

**Investigation:**
```bash
# GC logging
-Xlog:gc*:file=gc.log:time,level,tags

# Analyze GC log
# Look for: long pause times, frequent GCs
```

**Solutions:**
```bash
# Switch to low-pause GC
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# Or use ZGC (Java 11+)
-XX:+UseZGC

# Tune heap size
-Xms4g -Xmx4g  # Avoid heap resizing

# Reduce allocation rate
# Profile application to find allocation hotspots
```

#### Scenario 3: Stack Overflow Error

**Symptoms:**
- `StackOverflowError`
- Deep recursion or infinite recursion

**Investigation:**
```java
// Check stack trace for recursion
Exception in thread "main" java.lang.StackOverflowError
    at MyClass.recursiveMethod(MyClass.java:10)
    at MyClass.recursiveMethod(MyClass.java:10)
    // ... many more
```

**Solutions:**
```bash
# Increase stack size (if legitimate deep recursion)
-Xss2m  # Default is usually 1MB

# Or fix infinite recursion in code
```

### How to Explain JVM to an Interviewer

**Structure Your Answer:**

1. **High-Level Overview** (30 seconds):
   - "JVM is a runtime engine that executes Java bytecode"
   - "Provides platform independence and automatic memory management"

2. **Architecture** (2-3 minutes):
   - Class Loader Subsystem
   - Runtime Data Areas (Heap, Stack, Method Area)
   - Execution Engine (Interpreter, JIT, GC)

3. **Deep Dive** (based on question):
   - If asked about memory: Explain heap vs stack, GC
   - If asked about performance: Explain JIT, optimizations
   - If asked about class loading: Explain classloader hierarchy, delegation

4. **Practical Experience** (1 minute):
   - "I've used jmap/jstack to debug production issues"
   - "Tuned JVM flags for better performance"
   - "Investigated memory leaks using heap dumps"

**Key Points to Emphasize:**
- ✅ Understanding of memory management
- ✅ Knowledge of performance tuning
- ✅ Experience with debugging tools
- ✅ Awareness of common pitfalls (memory leaks, classloader issues)

**Example Explanation:**

> "The JVM is essentially a virtual computer that executes Java bytecode. It has three main components:
> 
> First, the **Class Loader** loads classes from .class files, verifies them, and initializes them. It uses a delegation model where child classloaders ask parents first.
> 
> Second, **Runtime Data Areas** manage memory. The heap stores all objects and is shared across threads. Each thread has its own stack for method calls and local variables. The method area stores class metadata.
> 
> Third, the **Execution Engine** runs the code. It starts with an interpreter for fast startup, then uses a JIT compiler to compile hot methods to native code for better performance. The garbage collector manages heap memory automatically.
> 
> I've used tools like jmap and jstack to debug production memory issues, and I understand how to tune JVM flags like -Xmx and GC settings for optimal performance."

### Performance Debugging Scenarios

#### Scenario: Slow Application Startup

**Symptoms:**
- Application takes 30+ seconds to start
- Users complain about slow initial response

**Investigation:**
```bash
# Check what's happening during startup
-XX:+TraceClassLoading
-XX:+TraceClassUnloading
-Xlog:class+load:file=classload.log

# Profile startup
java -agentpath:/path/to/profiler.so MyApp
```

**Common Causes:**
- Too many classes being loaded
- Heavy static initializers
- Synchronous initialization blocking startup
- Large classpath scanning

**Solutions:**
```bash
# Use AppCDS (Application Class Data Sharing)
-Xshare:dump  # Create shared archive
-Xshare:on    # Use shared archive

# Lazy initialization
# Move heavy initialization to background threads

# Reduce classpath
# Remove unused JARs
```

#### Scenario: Intermittent Performance Degradation

**Symptoms:**
- Application slows down periodically
- No obvious pattern
- CPU spikes

**Investigation:**
```bash
# Continuous monitoring
jstat -gc <pid> 1000 > gc.log
jstat -compiler <pid> 1000 > compiler.log

# Thread dumps at intervals
while true; do
    jstack <pid> >> threaddumps.txt
    sleep 30
done

# CPU profiling
jcmd <pid> JFR.start duration=60s filename=profile.jfr
```

**Common Causes:**
- GC pauses
- JIT deoptimization
- Contention on shared resources
- Memory pressure causing frequent GC

**Solutions:**
- Analyze GC logs for patterns
- Check for lock contention in thread dumps
- Profile CPU to find hotspots
- Tune GC or reduce allocation rate

#### Scenario: Memory Usage Growing Over Time

**Symptoms:**
- Heap usage increases continuously
- Eventually causes OOM
- Application needs periodic restarts

**Investigation:**
```bash
# Monitor heap over time
jmap -heap <pid> > heap1.txt
# Wait 1 hour
jmap -heap <pid> > heap2.txt
# Compare

# Generate heap dumps at intervals
jmap -dump:live,format=b,file=dump1.hprof <pid>
# Wait 1 hour
jmap -dump:live,format=b,file=dump2.hprof <pid>
# Compare using Eclipse MAT
```

**Common Causes:**
- Memory leaks (objects not being GC'd)
- Caches growing unbounded
- Listeners not being removed
- Static collections accumulating data

**Solutions:**
- Use bounded caches
- Remove listeners when done
- Clear static collections periodically
- Fix memory leaks identified in heap dumps

---

[← Back to Index](00-README.md) | [Previous: GC & Memory](09-GC-Memory.md) | [Next: File I/O →](11-File-IO.md)
