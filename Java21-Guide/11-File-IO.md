# Chapter 11: File I/O & Serialization

[← Back to Index](00-README.md) | [Previous: JVM Internals](10-JVM-Internals.md) | [Next: Networking →](12-Networking.md)

---

## Why This Chapter Matters

File I/O operations are fundamental to almost every Java application. Whether you're reading configuration files, processing logs, storing user data, or implementing data persistence, understanding file operations is essential. In interviews, file I/O questions test your understanding of:

- **Resource management**: Properly closing streams and handling exceptions
- **Performance**: Choosing the right API for different scenarios
- **Memory efficiency**: Handling large files without running out of memory
- **Modern Java**: Understanding the evolution from `java.io` to `java.nio` and `java.nio.file`
- **Serialization**: Object persistence, security implications, and alternatives

Mastering file I/O demonstrates your ability to write production-ready code that handles real-world data efficiently and safely.

---

## Layer 1 — Beginner Foundation

### File and Path Classes

Java provides two main ways to represent file paths:

**java.io.File (Legacy)**
```java
import java.io.File;

File file = new File("data.txt");
System.out.println("Exists: " + file.exists());
System.out.println("Is file: " + file.isFile());
System.out.println("Is directory: " + file.isDirectory());
System.out.println("Size: " + file.length() + " bytes");
```

**java.nio.file.Path (Modern - Java 7+)**
```java
import java.nio.file.Path;
import java.nio.file.Paths;

Path path = Paths.get("data.txt");
// Or using Path.of() (Java 11+)
Path path2 = Path.of("data.txt");

System.out.println("File name: " + path.getFileName());
System.out.println("Parent: " + path.getParent());
System.out.println("Absolute path: " + path.toAbsolutePath());
```

**Key Differences:**
- `Path` is an interface; `File` is a concrete class
- `Path` works with the file system abstraction
- `Path` supports better cross-platform path handling
- `Path` integrates with `Files` utility class

### Reading and Writing Text Files

**Reading with FileReader (Basic)**
```java
import java.io.FileReader;
import java.io.IOException;

try (FileReader reader = new FileReader("input.txt")) {
    int character;
    while ((character = reader.read()) != -1) {
        System.out.print((char) character);
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

**Writing with FileWriter (Basic)**
```java
import java.io.FileWriter;
import java.io.IOException;

try (FileWriter writer = new FileWriter("output.txt")) {
    writer.write("Hello, World!\n");
    writer.write("This is a test file.");
} catch (IOException e) {
    e.printStackTrace();
}
```

**Reading Line by Line (Better Approach)**
```java
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

try (BufferedReader reader = new BufferedReader(
        new FileReader("input.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

### Basic File Operations

**Create, Delete, Copy, Move**
```java
import java.io.File;
import java.io.IOException;

// Create a file
File newFile = new File("newfile.txt");
try {
    boolean created = newFile.createNewFile();
    System.out.println("File created: " + created);
} catch (IOException e) {
    e.printStackTrace();
}

// Delete a file
boolean deleted = newFile.delete();
System.out.println("File deleted: " + deleted);

// Check if file exists
File file = new File("data.txt");
if (file.exists()) {
    System.out.println("File exists!");
} else {
    System.out.println("File does not exist!");
}

// Get file information
if (file.exists()) {
    System.out.println("Name: " + file.getName());
    System.out.println("Path: " + file.getPath());
    System.out.println("Absolute path: " + file.getAbsolutePath());
    System.out.println("Size: " + file.length() + " bytes");
    System.out.println("Last modified: " + new Date(file.lastModified()));
}
```

**Copy and Move Operations**
```java
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

// Copy file (manual approach)
try (FileInputStream fis = new FileInputStream("source.txt");
     FileOutputStream fos = new FileOutputStream("destination.txt")) {
    
    byte[] buffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = fis.read(buffer)) != -1) {
        fos.write(buffer, 0, bytesRead);
    }
    System.out.println("File copied successfully!");
} catch (IOException e) {
    e.printStackTrace();
}
```

### Understanding Streams (InputStream, OutputStream)

**Streams are byte-oriented:**
- `InputStream`: Reads bytes from a source
- `OutputStream`: Writes bytes to a destination

**Common Stream Classes:**
```java
// FileInputStream - reads from files
FileInputStream fis = new FileInputStream("data.bin");

// FileOutputStream - writes to files
FileOutputStream fos = new FileOutputStream("output.bin");

// ByteArrayInputStream - reads from byte array
byte[] data = {1, 2, 3, 4, 5};
ByteArrayInputStream bais = new ByteArrayInputStream(data);

// ByteArrayOutputStream - writes to byte array
ByteArrayOutputStream baos = new ByteArrayOutputStream();
```

**Reading Bytes:**
```java
import java.io.FileInputStream;
import java.io.IOException;

try (FileInputStream fis = new FileInputStream("data.bin")) {
    int byteValue;
    while ((byteValue = fis.read()) != -1) {
        System.out.print(byteValue + " ");
    }
} catch (IOException e) {
    e.printStackTrace();
}

// Reading into buffer (more efficient)
try (FileInputStream fis = new FileInputStream("data.bin")) {
    byte[] buffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = fis.read(buffer)) != -1) {
        // Process buffer[0] to buffer[bytesRead-1]
        System.out.println("Read " + bytesRead + " bytes");
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

**Writing Bytes:**
```java
import java.io.FileOutputStream;
import java.io.IOException;

try (FileOutputStream fos = new FileOutputStream("output.bin")) {
    byte[] data = {65, 66, 67, 68}; // A, B, C, D
    fos.write(data);
    fos.write(69); // E
} catch (IOException e) {
    e.printStackTrace();
}
```

### Reader vs Writer vs Stream

**Key Differences:**

| Aspect | Streams (byte) | Readers/Writers (character) |
|--------|----------------|----------------------------|
| **Unit** | Bytes (8 bits) | Characters (16-bit Unicode) |
| **Use Case** | Binary data (images, videos, executables) | Text data (human-readable) |
| **Encoding** | No encoding conversion | Handles character encoding |
| **Classes** | InputStream, OutputStream | Reader, Writer |

**When to Use What:**

```java
// Use Streams for binary data
FileInputStream fis = new FileInputStream("image.jpg");
FileOutputStream fos = new FileOutputStream("copy.jpg");

// Use Readers/Writers for text data
FileReader reader = new FileReader("text.txt");
FileWriter writer = new FileWriter("output.txt");

// Streams can read text, but you handle encoding manually
// Readers/Writers handle encoding automatically
FileInputStream fis2 = new FileInputStream("text.txt");
InputStreamReader isr = new InputStreamReader(fis2, "UTF-8");
```

**Character Encoding Example:**
```java
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

// Default encoding (platform-dependent)
FileReader reader1 = new FileReader("file.txt");

// Explicit encoding
FileReader reader2 = new FileReader("file.txt", StandardCharsets.UTF_8);

// Writing with encoding
FileWriter writer = new FileWriter("output.txt", StandardCharsets.UTF_8);
```

---

## Layer 2 — Working Developer Level

### java.nio.file Package (Files, Paths)

The `java.nio.file` package (NIO.2) provides a modern, more powerful API for file operations.

**Path Creation:**
```java
import java.nio.file.Path;
import java.nio.file.Paths;

// Multiple ways to create paths
Path path1 = Paths.get("file.txt");
Path path2 = Paths.get("/", "home", "user", "file.txt");
Path path3 = Path.of("file.txt"); // Java 11+

// Resolving paths
Path base = Paths.get("/home/user");
Path file = base.resolve("documents").resolve("file.txt");
// Result: /home/user/documents/file.txt

// Normalizing paths
Path path = Paths.get("/home/user/../documents/./file.txt");
Path normalized = path.normalize();
// Result: /home/documents/file.txt
```

**Files Utility Class:**
```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.List;

Path path = Paths.get("data.txt");

// Check if file exists
boolean exists = Files.exists(path);

// Check if file is readable/writable/executable
boolean readable = Files.isReadable(path);
boolean writable = Files.isWritable(path);
boolean executable = Files.isExecutable(path);

// Get file size
long size = Files.size(path);

// Get last modified time
long lastModified = Files.getLastModifiedTime(path).toMillis();

// Read all lines into a List
List<String> lines = Files.readAllLines(path);

// Read entire file as bytes
byte[] bytes = Files.readAllBytes(path);

// Read entire file as String
String content = Files.readString(path); // Java 11+

// Write lines
List<String> data = List.of("Line 1", "Line 2", "Line 3");
Files.write(path, data);

// Write String
Files.writeString(path, "Hello, World!"); // Java 11+

// Copy file
Path source = Paths.get("source.txt");
Path target = Paths.get("target.txt");
Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

// Move/Rename file
Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

// Delete file
Files.delete(path);
// Or delete if exists (no exception if file doesn't exist)
Files.deleteIfExists(path);

// Create directories
Path dir = Paths.get("newdir");
Files.createDirectories(dir); // Creates parent directories if needed
Files.createDirectory(dir);    // Throws exception if parent doesn't exist
```

### Try-with-Resources for File Handling

**Automatic Resource Management:**
```java
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// Old way (manual closing - error-prone)
FileReader reader = null;
try {
    reader = new FileReader("file.txt");
    // ... use reader
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (reader != null) {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Modern way (try-with-resources - automatic closing)
try (FileReader reader = new FileReader("file.txt")) {
    // ... use reader
    // Automatically closed when block exits
} catch (IOException e) {
    e.printStackTrace();
}

// Multiple resources
try (FileReader reader = new FileReader("input.txt");
     FileWriter writer = new FileWriter("output.txt")) {
    // Both automatically closed
} catch (IOException e) {
    e.printStackTrace();
}
```

**Custom Resources (implementing AutoCloseable):**
```java
class MyResource implements AutoCloseable {
    @Override
    public void close() throws Exception {
        System.out.println("Closing resource");
        // Cleanup code
    }
}

try (MyResource resource = new MyResource()) {
    // Use resource
} // Automatically calls close()
```

### BufferedReader/BufferedWriter Performance

**Why Buffering Matters:**
- Without buffering: Each read/write operation goes directly to the OS
- With buffering: Multiple operations are batched, reducing system calls

**BufferedReader Example:**
```java
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// Without buffering (slow for many small reads)
try (FileReader reader = new FileReader("largefile.txt")) {
    int character;
    while ((character = reader.read()) != -1) {
        // Each read() call = system call
    }
}

// With buffering (much faster)
try (BufferedReader reader = new BufferedReader(
        new FileReader("largefile.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        // Reads are buffered
    }
}

// Custom buffer size
try (BufferedReader reader = new BufferedReader(
        new FileReader("largefile.txt"), 8192)) { // 8KB buffer
    // ...
}
```

**BufferedWriter Example:**
```java
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

try (BufferedWriter writer = new BufferedWriter(
        new FileWriter("output.txt"))) {
    writer.write("Line 1");
    writer.newLine();
    writer.write("Line 2");
    writer.flush(); // Optional: force write to disk
} catch (IOException e) {
    e.printStackTrace();
}
```

**Performance Comparison:**
```java
// Slow: Unbuffered
FileWriter writer = new FileWriter("file.txt");
for (int i = 0; i < 10000; i++) {
    writer.write("Line " + i + "\n"); // Each write = system call
}

// Fast: Buffered
BufferedWriter bufferedWriter = new BufferedWriter(
    new FileWriter("file.txt"));
for (int i = 0; i < 10000; i++) {
    bufferedWriter.write("Line " + i + "\n"); // Batched writes
}
bufferedWriter.flush(); // Write all at once
```

### Reading Large Files Efficiently

**Problem: Reading entire file into memory**
```java
// BAD: For large files, this can cause OutOfMemoryError
List<String> allLines = Files.readAllLines(path);
String entireContent = Files.readString(path);
```

**Solution: Stream Processing**
```java
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

Path path = Paths.get("largefile.txt");

// Process line by line (memory efficient)
try (BufferedReader reader = Files.newBufferedReader(path)) {
    String line;
    while ((line = reader.readLine()) != null) {
        // Process one line at a time
        processLine(line);
    }
} catch (IOException e) {
    e.printStackTrace();
}

// Using Stream API (Java 8+)
try (Stream<String> lines = Files.lines(path)) {
    lines.filter(line -> line.startsWith("ERROR"))
         .forEach(System.out::println);
} catch (IOException e) {
    e.printStackTrace();
}

// Process in chunks (for binary files)
try (InputStream is = Files.newInputStream(path)) {
    byte[] buffer = new byte[8192]; // 8KB buffer
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
        processChunk(buffer, bytesRead);
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

**Parallel Processing (Java 8+):**
```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

Path path = Paths.get("verylargefile.txt");

try (Stream<String> lines = Files.lines(path)) {
    lines.parallel() // Process in parallel
         .filter(line -> line.contains("keyword"))
         .forEach(System.out::println);
} catch (IOException e) {
    e.printStackTrace();
}
```

### File Walking and Directory Operations

**Listing Directory Contents:**
```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.stream.Stream;

Path dir = Paths.get("/home/user/documents");

// List files in directory (non-recursive)
try (Stream<Path> paths = Files.list(dir)) {
    paths.forEach(System.out::println);
} catch (IOException e) {
    e.printStackTrace();
}

// List files matching pattern
try (Stream<Path> paths = Files.list(dir)) {
    paths.filter(path -> path.toString().endsWith(".txt"))
         .forEach(System.out::println);
} catch (IOException e) {
    e.printStackTrace();
}
```

**Walking Directory Tree:**
```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.stream.Stream;

Path root = Paths.get("/home/user/documents");

// Walk directory tree (recursive)
try (Stream<Path> paths = Files.walk(root)) {
    paths.forEach(System.out::println);
} catch (IOException e) {
    e.printStackTrace();
}

// Walk with max depth
try (Stream<Path> paths = Files.walk(root, 3)) { // Max depth 3
    paths.forEach(System.out::println);
} catch (IOException e) {
    e.printStackTrace();
}

// Find files matching criteria
try (Stream<Path> paths = Files.find(root, Integer.MAX_VALUE,
        (path, attrs) -> attrs.isRegularFile() && 
                        path.toString().endsWith(".java"))) {
    paths.forEach(System.out::println);
} catch (IOException e) {
    e.printStackTrace();
}
```

**Directory Operations:**
```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

Path dir = Paths.get("newdir");

// Create directory
Files.createDirectory(dir);

// Create directories (creates parent directories if needed)
Files.createDirectories(Paths.get("a/b/c/d"));

// Check if path is directory
boolean isDir = Files.isDirectory(dir);

// Get directory contents
try (Stream<Path> entries = Files.list(dir)) {
    entries.forEach(path -> {
        if (Files.isDirectory(path)) {
            System.out.println("DIR: " + path);
        } else {
            System.out.println("FILE: " + path);
        }
    });
} catch (IOException e) {
    e.printStackTrace();
}

// Delete directory (must be empty)
Files.delete(dir);

// Delete directory tree recursively
public static void deleteDirectory(Path dir) throws IOException {
    if (Files.exists(dir)) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a)) // Delete files before dirs
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                 });
        }
    }
}
```

### Basic Serialization with Serializable

**What is Serialization?**
- Converting an object into a byte stream for storage or transmission
- Deserialization: Reconstructing the object from the byte stream

**Basic Serializable Example:**
```java
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

// Class must implement Serializable interface
class Person implements Serializable {
    private String name;
    private int age;
    private transient String password; // Won't be serialized
    
    public Person(String name, int age, String password) {
        this.name = name;
        this.age = age;
        this.password = password;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getPassword() { return password; }
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + 
               ", password='" + password + "'}";
    }
}

// Serialize object
public static void serializePerson(Person person, String filename) {
    try (ObjectOutputStream oos = new ObjectOutputStream(
            new FileOutputStream(filename))) {
        oos.writeObject(person);
        System.out.println("Object serialized successfully");
    } catch (IOException e) {
        e.printStackTrace();
    }
}

// Deserialize object
public static Person deserializePerson(String filename) {
    try (ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream(filename))) {
        Person person = (Person) ois.readObject();
        System.out.println("Object deserialized successfully");
        return person;
    } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
        return null;
    }
}

// Usage
Person person = new Person("John", 30, "secret123");
serializePerson(person, "person.ser");
Person deserialized = deserializePerson("person.ser");
System.out.println(deserialized); // password will be null (transient)
```

**Key Points:**
- Class must implement `Serializable` (marker interface - no methods)
- `transient` keyword excludes fields from serialization
- Static fields are not serialized
- Parent class must also be Serializable if you want to serialize parent fields

---

## Layer 3 — Advanced Engineering Depth

### NIO.2 Architecture (Channels, Buffers, Selectors)

**NIO vs NIO.2:**
- **NIO (Java 1.4)**: Non-blocking I/O with Channels, Buffers, Selectors
- **NIO.2 (Java 7)**: Enhanced file system API (Files, Paths) + NIO features

**Buffers:**
```java
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;

// Creating buffers
ByteBuffer byteBuffer = ByteBuffer.allocate(1024); // Heap buffer
ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024); // Direct buffer

// Buffer operations
byteBuffer.put((byte) 65);
byteBuffer.put((byte) 66);
byteBuffer.put((byte) 67);

// Flip: Prepare buffer for reading
byteBuffer.flip();

// Reading from buffer
while (byteBuffer.hasRemaining()) {
    System.out.print((char) byteBuffer.get());
}

// Clear: Prepare buffer for writing again
byteBuffer.clear();

// Buffer properties
System.out.println("Capacity: " + byteBuffer.capacity());
System.out.println("Position: " + byteBuffer.position());
System.out.println("Limit: " + byteBuffer.limit());
```

**Channels:**
```java
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

// FileChannel for file I/O
try (FileChannel channel = FileChannel.open(
        Paths.get("data.txt"),
        StandardOpenOption.READ,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE)) {
    
    // Write to channel
    ByteBuffer buffer = ByteBuffer.wrap("Hello, World!".getBytes());
    channel.write(buffer);
    
    // Read from channel
    buffer.clear();
    channel.position(0); // Reset position
    channel.read(buffer);
    buffer.flip();
    
    byte[] data = new byte[buffer.remaining()];
    buffer.get(data);
    System.out.println(new String(data));
} catch (IOException e) {
    e.printStackTrace();
}
```

**Selectors (for non-blocking I/O):**
```java
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.io.IOException;

// Selector allows a single thread to handle multiple channels
Selector selector = Selector.open();

ServerSocketChannel serverChannel = ServerSocketChannel.open();
serverChannel.configureBlocking(false); // Non-blocking mode
serverChannel.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    int readyChannels = selector.select(); // Blocks until channels ready
    
    if (readyChannels == 0) continue;
    
    for (SelectionKey key : selector.selectedKeys()) {
        if (key.isAcceptable()) {
            // Handle accept
        } else if (key.isReadable()) {
            // Handle read
        } else if (key.isWritable()) {
            // Handle write
        }
    }
    selector.selectedKeys().clear();
}
```

### Memory-Mapped Files (MappedByteBuffer)

**What are Memory-Mapped Files?**
- Map a file (or portion) directly into virtual memory
- OS handles the mapping - very efficient for large files
- Changes can be written back to disk automatically

**Memory-Mapped File Example:**
```java
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

try (FileChannel channel = FileChannel.open(
        Paths.get("largefile.dat"),
        StandardOpenOption.READ,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE)) {
    
    // Map entire file into memory (or portion)
    MappedByteBuffer buffer = channel.map(
        FileChannel.MapMode.READ_WRITE, // Mode
        0,                              // Position
        channel.size()                  // Size
    );
    
    // Read from mapped buffer
    byte[] data = new byte[(int) channel.size()];
    buffer.get(data);
    
    // Write to mapped buffer
    buffer.put(0, (byte) 65);
    buffer.put(1, (byte) 66);
    
    // Force changes to disk
    buffer.force();
    
} catch (IOException e) {
    e.printStackTrace();
}
```

**Map Modes:**
- `READ_ONLY`: Read-only access
- `READ_WRITE`: Read and write access
- `PRIVATE`: Copy-on-write (changes not written to file)

**When to Use Memory-Mapped Files:**
- Large files that don't fit in memory
- Random access to file data
- Shared memory between processes
- High-performance I/O requirements

**Performance Comparison:**
```java
// Regular I/O (slower for large files)
try (FileInputStream fis = new FileInputStream("largefile.dat")) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = fis.read(buffer)) != -1) {
        // Process buffer
    }
}

// Memory-mapped (faster for large files)
try (FileChannel channel = FileChannel.open(Paths.get("largefile.dat"))) {
    MappedByteBuffer buffer = channel.map(
        FileChannel.MapMode.READ_ONLY, 0, channel.size());
    // Direct access to file data in memory
    byte[] data = new byte[(int) channel.size()];
    buffer.get(data);
}
```

### FileChannel for High-Performance I/O

**FileChannel Advantages:**
- Can be used with memory-mapped files
- Supports file locking
- Can transfer data directly between channels
- Better performance than regular streams for large files

**FileChannel Operations:**
```java
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

// Reading with FileChannel
try (FileChannel channel = FileChannel.open(
        Paths.get("input.txt"),
        StandardOpenOption.READ)) {
    
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int bytesRead;
    while ((bytesRead = channel.read(buffer)) != -1) {
        buffer.flip();
        // Process buffer
        buffer.clear();
    }
} catch (IOException e) {
    e.printStackTrace();
}

// Writing with FileChannel
try (FileChannel channel = FileChannel.open(
        Paths.get("output.txt"),
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE)) {
    
    ByteBuffer buffer = ByteBuffer.wrap("Hello, World!".getBytes());
    channel.write(buffer);
} catch (IOException e) {
    e.printStackTrace();
}

// Transfer between channels (very efficient)
try (FileChannel source = FileChannel.open(Paths.get("source.txt"));
     FileChannel dest = FileChannel.open(
         Paths.get("dest.txt"),
         StandardOpenOption.WRITE,
         StandardOpenOption.CREATE)) {
    
    // Transfer entire file
    source.transferTo(0, source.size(), dest);
    
    // Or transfer specific portion
    // source.transferTo(0, 1024, dest);
} catch (IOException e) {
    e.printStackTrace();
}
```

**File Locking:**
```java
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

try (FileChannel channel = FileChannel.open(
        Paths.get("data.txt"),
        StandardOpenOption.WRITE)) {
    
    // Exclusive lock (write lock)
    FileLock lock = channel.lock();
    try {
        // Perform write operations
        ByteBuffer buffer = ByteBuffer.wrap("Data".getBytes());
        channel.write(buffer);
    } finally {
        lock.release();
    }
    
    // Shared lock (read lock)
    FileLock readLock = channel.lock(0, Long.MAX_VALUE, true);
    try {
        // Perform read operations
    } finally {
        readLock.release();
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

### Asynchronous File I/O (AsynchronousFileChannel)

**Non-blocking file operations:**
```java
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.concurrent.Future;

// Asynchronous read with Future
try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
        Paths.get("data.txt"),
        StandardOpenOption.READ)) {
    
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    Future<Integer> result = channel.read(buffer, 0);
    
    // Do other work while reading...
    
    // Get result when ready
    int bytesRead = result.get(); // Blocks until complete
    buffer.flip();
    // Process buffer
    
} catch (IOException | java.util.concurrent.ExecutionException | 
         java.util.concurrent.InterruptedException e) {
    e.printStackTrace();
}

// Asynchronous read with CompletionHandler
try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
        Paths.get("data.txt"),
        StandardOpenOption.READ)) {
    
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    channel.read(buffer, 0, null, new CompletionHandler<Integer, Void>() {
        @Override
        public void completed(Integer result, Void attachment) {
            System.out.println("Read " + result + " bytes");
            buffer.flip();
            // Process buffer
        }
        
        @Override
        public void failed(Throwable exc, Void attachment) {
            System.err.println("Read failed: " + exc);
        }
    });
    
    // Continue with other work...
    Thread.sleep(1000); // Give time for async operation
    
} catch (IOException | InterruptedException e) {
    e.printStackTrace();
}

// Asynchronous write
try (AsynchronousFileChannel channel = AsynchronousFileChannel.open(
        Paths.get("output.txt"),
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE)) {
    
    ByteBuffer buffer = ByteBuffer.wrap("Hello, Async!".getBytes());
    Future<Integer> result = channel.write(buffer, 0);
    
    int bytesWritten = result.get();
    System.out.println("Wrote " + bytesWritten + " bytes");
    
} catch (IOException | java.util.concurrent.ExecutionException | 
         java.util.concurrent.InterruptedException e) {
    e.printStackTrace();
}
```

**When to Use Asynchronous I/O:**
- High-concurrency applications
- Non-blocking I/O requirements
- When you can do other work while I/O completes
- Network servers handling many connections

### Custom Serialization (Externalizable, readObject/writeObject)

**Externalizable Interface:**
```java
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.IOException;

class CustomPerson implements Externalizable {
    private String name;
    private int age;
    private transient String password;
    
    // Required: public no-arg constructor
    public CustomPerson() {
    }
    
    public CustomPerson(String name, int age, String password) {
        this.name = name;
        this.age = age;
        this.password = password;
    }
    
    // Custom write logic
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(age);
        // Password not written (security)
    }
    
    // Custom read logic
    @Override
    public void readExternal(ObjectInput in) 
            throws IOException, ClassNotFoundException {
        name = in.readUTF();
        age = in.readInt();
        password = "default"; // Set default
    }
    
    @Override
    public String toString() {
        return "CustomPerson{name='" + name + "', age=" + age + 
               ", password='" + password + "'}";
    }
}
```

**readObject/writeObject Methods (Serializable):**
```java
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

class PersonWithCustomSerialization implements Serializable {
    private String name;
    private int age;
    private transient String password;
    
    public PersonWithCustomSerialization(String name, int age, String password) {
        this.name = name;
        this.age = age;
        this.password = password;
    }
    
    // Custom serialization
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject(); // Serialize non-transient fields
        // Add custom serialization logic
        oos.writeUTF(name.toUpperCase()); // Store uppercase name
    }
    
    // Custom deserialization
    private void readObject(ObjectInputStream ois) 
            throws IOException, ClassNotFoundException {
        ois.defaultReadObject(); // Deserialize non-transient fields
        // Add custom deserialization logic
        name = ois.readUTF().toLowerCase(); // Convert back to lowercase
        password = "default"; // Set default password
    }
    
    @Override
    public String toString() {
        return "PersonWithCustomSerialization{name='" + name + 
               "', age=" + age + ", password='" + password + "'}";
    }
}
```

**readResolve and writeReplace:**
```java
import java.io.Serializable;
import java.io.ObjectStreamException;

class Singleton implements Serializable {
    private static final Singleton INSTANCE = new Singleton();
    
    private Singleton() {
    }
    
    public static Singleton getInstance() {
        return INSTANCE;
    }
    
    // Prevent creating new instance during deserialization
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
    
    // Replace object during serialization (if needed)
    private Object writeReplace() throws ObjectStreamException {
        return INSTANCE;
    }
}
```

### serialVersionUID Importance

**What is serialVersionUID?**
- Unique identifier for a serializable class
- Used to verify compatibility during deserialization
- If not specified, Java generates one automatically (can cause issues)

**Why It Matters:**
```java
import java.io.Serializable;

// Version 1 of the class
class Person implements Serializable {
    // Missing serialVersionUID - Java will generate one
    private String name;
    private int age;
}

// After adding a field (Version 2)
class Person implements Serializable {
    // Missing serialVersionUID - Java generates DIFFERENT one!
    private String name;
    private int age;
    private String email; // New field added
}

// Problem: Objects serialized with Version 1 cannot be deserialized 
// with Version 2 because serialVersionUIDs don't match!
```

**Solution: Explicit serialVersionUID:**
```java
import java.io.Serializable;

class Person implements Serializable {
    // Explicit serialVersionUID - remains constant
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int age;
    
    // Later, you can add fields and handle compatibility
    private String email; // New field - serialVersionUID still 1L
}

// Handling version changes
class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private int age;
    private String email; // Added later
    
    // Handle missing email during deserialization
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (email == null) {
            email = ""; // Default value for old serialized objects
        }
    }
}
```

**Best Practices:**
- Always declare `serialVersionUID` explicitly
- Increment it when making incompatible changes
- Keep it the same for compatible changes (add fields, etc.)

### Serialization Security Concerns

**Security Issues with Java Serialization:**

1. **Remote Code Execution (RCE)**
```java
// Dangerous: Deserializing untrusted data
try (ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream("untrusted.ser"))) {
    Object obj = ois.readObject(); // Can execute malicious code!
}
```

2. **Information Disclosure**
```java
class User implements Serializable {
    private String username;
    private String password; // Even if transient, can be recovered
    private String creditCard;
}
```

3. **Denial of Service**
```java
// Malicious object can cause excessive resource consumption
class Bomb implements Serializable {
    private int[] data = new int[Integer.MAX_VALUE]; // OutOfMemoryError
}
```

**Mitigation Strategies:**

1. **Avoid Java Serialization**
```java
// Use alternatives: JSON, Protocol Buffers, etc.
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(person);
Person deserialized = mapper.readValue(json, Person.class);
```

2. **Whitelist Approach**
```java
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

class SafeObjectInputStream extends ObjectInputStream {
    private static final Set<String> ALLOWED_CLASSES = 
        Set.of("com.example.Person", "com.example.Address");
    
    public SafeObjectInputStream(InputStream in) throws IOException {
        super(in);
    }
    
    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        String className = desc.getName();
        if (!ALLOWED_CLASSES.contains(className)) {
            throw new SecurityException("Deserialization not allowed: " + className);
        }
        return super.resolveClass(desc);
    }
}
```

3. **Input Validation**
```java
// Validate deserialized objects
try (SafeObjectInputStream ois = new SafeObjectInputStream(
        new FileInputStream("data.ser"))) {
    Object obj = ois.readObject();
    if (obj instanceof Person) {
        Person person = (Person) obj;
        // Validate person data
        if (person.getAge() < 0 || person.getAge() > 150) {
            throw new IllegalArgumentException("Invalid age");
        }
    }
}
```

### Alternatives to Java Serialization

**1. JSON (Jackson)**
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class Person {
    private String name;
    private int age;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}

// Serialize to JSON
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(person);
// Output: {"name":"John","age":30}

// Deserialize from JSON
Person person = mapper.readValue(json, Person.class);
```

**2. Protocol Buffers**
```java
// person.proto
// syntax = "proto3";
// message Person {
//   string name = 1;
//   int32 age = 2;
// }

// Generated Java code usage
Person person = Person.newBuilder()
    .setName("John")
    .setAge(30)
    .build();

byte[] data = person.toByteArray(); // Serialize
Person deserialized = Person.parseFrom(data); // Deserialize
```

**3. Apache Avro**
```java
// Schema-based serialization
// More compact than JSON, supports schema evolution
```

**4. Custom Binary Format**
```java
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

class PersonSerializer {
    public static void serialize(Person person, String filename) 
            throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(filename))) {
            dos.writeUTF(person.getName());
            dos.writeInt(person.getAge());
        }
    }
    
    public static Person deserialize(String filename) 
            throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new FileInputStream(filename))) {
            String name = dis.readUTF();
            int age = dis.readInt();
            return new Person(name, age);
        }
    }
}
```

**Comparison:**

| Format | Pros | Cons |
|--------|------|------|
| **Java Serialization** | Built-in, simple | Security issues, versioning problems |
| **JSON** | Human-readable, widely supported | Larger size, slower parsing |
| **Protocol Buffers** | Compact, fast, schema evolution | Requires code generation |
| **Avro** | Schema evolution, compact | Less popular than Protobuf |
| **Custom Binary** | Full control, optimized | More work to implement |

### WatchService for File System Events

**Monitor file system changes:**
```java
import java.nio.file.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class FileWatcher {
    public static void watchDirectory(Path dir) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        
        // Register for events
        dir.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY);
        
        System.out.println("Watching: " + dir);
        
        while (true) {
            WatchKey key;
            try {
                key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }
            } catch (InterruptedException e) {
                return;
            }
            
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                
                System.out.println(kind + ": " + filename);
                
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("File created: " + filename);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    System.out.println("File deleted: " + filename);
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    System.out.println("File modified: " + filename);
                }
            }
            
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get("watched_directory");
        watchDirectory(dir);
    }
}
```

**Use Cases:**
- Configuration file monitoring
- Log file processing
- Hot-reload mechanisms
- File synchronization systems

---

## Layer 4 — Interview Mastery

### Common Interview Questions with Answers

**Q1: What's the difference between `java.io` and `java.nio`?**

**Answer:**
- **java.io (Old I/O)**: Stream-based, blocking I/O. Each read/write operation blocks the thread until complete.
- **java.nio (New I/O)**: Buffer and channel-based, supports non-blocking I/O. More efficient for high-concurrency scenarios.

**Key Differences:**
- `java.io` uses streams; `java.nio` uses channels and buffers
- `java.io` is blocking; `java.nio` supports non-blocking operations
- `java.nio` supports selectors for multiplexing
- `java.nio.file` (NIO.2) provides modern file system API

**Q2: Explain the difference between `File`, `Path`, and `Files`.**

**Answer:**
- **File**: Legacy class representing a file/directory path. Concrete class with methods for file operations.
- **Path**: Modern interface (Java 7+) representing a file system path. Immutable, works with `Files` utility.
- **Files**: Utility class providing static methods for file operations (read, write, copy, delete, etc.).

```java
// File (legacy)
File file = new File("data.txt");
if (file.exists()) {
    file.delete();
}

// Path + Files (modern)
Path path = Paths.get("data.txt");
if (Files.exists(path)) {
    Files.delete(path);
}
```

**Q3: What happens if you don't close a file stream?**

**Answer:**
- Resource leak: File handle remains open
- Other processes may not be able to access the file
- On Windows, you may not be able to delete the file
- Memory may not be released properly

**Solution: Use try-with-resources**
```java
// Automatically closes
try (FileReader reader = new FileReader("file.txt")) {
    // Use reader
} // Automatically closed here
```

**Q4: How do you read a large file without loading it all into memory?**

**Answer:**
```java
// BAD: Loads entire file
List<String> lines = Files.readAllLines(path); // OutOfMemoryError!

// GOOD: Process line by line
try (BufferedReader reader = Files.newBufferedReader(path)) {
    String line;
    while ((line = reader.readLine()) != null) {
        processLine(line); // Process one line at a time
    }
}

// Or using Stream API
try (Stream<String> lines = Files.lines(path)) {
    lines.forEach(this::processLine);
}
```

**Q5: What is the purpose of `transient` keyword?**

**Answer:**
- Marks a field to be excluded from serialization
- Useful for sensitive data (passwords), derived fields, or non-serializable objects

```java
class User implements Serializable {
    private String username;
    private transient String password; // Won't be serialized
    private transient Date loginTime;  // Can't serialize Date easily
}
```

### Why is Java Serialization Considered Problematic?

**Answer:**

1. **Security Vulnerabilities**
   - Can lead to Remote Code Execution (RCE)
   - Deserializing untrusted data can execute arbitrary code
   - Many CVEs related to Java deserialization

2. **Versioning Issues**
   - Adding/removing fields can break compatibility
   - `serialVersionUID` management is error-prone
   - Difficult to evolve schemas

3. **Performance**
   - Slower than alternatives (JSON, Protobuf)
   - Larger serialized size
   - Reflection overhead

4. **Maintenance Burden**
   - Hard to debug serialization issues
   - Tight coupling between versions
   - Difficult to migrate between versions

**Best Practice:** Avoid Java serialization. Use JSON, Protocol Buffers, or other alternatives.

### How Would You Implement a File Copy Utility?

**Answer:**

**Basic Implementation:**
```java
import java.io.*;
import java.nio.file.*;

public class FileCopyUtility {
    // Method 1: Using Files.copy (simplest)
    public static void copyFileSimple(Path source, Path target) 
            throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
    
    // Method 2: Using FileChannel (efficient for large files)
    public static void copyFileChannel(Path source, Path target) 
            throws IOException {
        try (FileChannel sourceChannel = FileChannel.open(source);
             FileChannel targetChannel = FileChannel.open(
                 target, StandardOpenOption.WRITE, 
                 StandardOpenOption.CREATE)) {
            
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        }
    }
    
    // Method 3: Using streams with buffering
    public static void copyFileBuffered(Path source, Path target) 
            throws IOException {
        try (InputStream is = Files.newInputStream(source);
             OutputStream os = Files.newOutputStream(target)) {
            
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }
    
    // Method 4: With progress callback
    public static void copyFileWithProgress(Path source, Path target,
            ProgressCallback callback) throws IOException {
        long totalSize = Files.size(source);
        long copied = 0;
        
        try (InputStream is = Files.newInputStream(source);
             OutputStream os = Files.newOutputStream(target)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                copied += bytesRead;
                callback.onProgress(copied, totalSize);
            }
        }
    }
    
    interface ProgressCallback {
        void onProgress(long copied, long total);
    }
}
```

**Production-Ready Version:**
```java
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class AdvancedFileCopy {
    public static void copyDirectory(Path source, Path target) 
            throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, 
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
```

### Memory-Mapped Files vs Regular I/O Tradeoffs

**Answer:**

| Aspect | Regular I/O | Memory-Mapped Files |
|--------|-------------|---------------------|
| **Performance** | Good for sequential access | Excellent for random access |
| **Memory Usage** | Controlled (buffer size) | OS-managed (can be large) |
| **Use Case** | Small to medium files | Large files, random access |
| **Complexity** | Simple | More complex |
| **Portability** | Fully portable | OS-dependent behavior |
| **Concurrency** | Standard locking | File locking support |

**When to Use Memory-Mapped Files:**
- Large files (>100MB)
- Random access patterns
- Shared memory between processes
- High-performance requirements

**When to Use Regular I/O:**
- Small to medium files
- Sequential access
- Simple use cases
- Need precise memory control

**Example:**
```java
// Regular I/O: Good for sequential reading
try (BufferedReader reader = Files.newBufferedReader(path)) {
    String line;
    while ((line = reader.readLine()) != null) {
        processLine(line);
    }
}

// Memory-mapped: Good for random access
try (FileChannel channel = FileChannel.open(path)) {
    MappedByteBuffer buffer = channel.map(
        FileChannel.MapMode.READ_ONLY, 0, channel.size());
    // Random access to any position
    byte value = buffer.get(1000); // Read byte at position 1000
}
```

### Practical Debugging Scenarios

**Scenario 1: FileNotFoundException but file exists**

**Problem:**
```java
File file = new File("data.txt");
FileReader reader = new FileReader(file); // FileNotFoundException!
```

**Debugging Steps:**
1. Check if file exists: `file.exists()`
2. Check if it's a directory: `file.isDirectory()`
3. Check permissions: `file.canRead()`
4. Check path: Use absolute path `file.getAbsolutePath()`
5. Check working directory: `System.getProperty("user.dir")`

**Solution:**
```java
Path path = Paths.get("data.txt").toAbsolutePath();
if (!Files.exists(path)) {
    throw new FileNotFoundException("File not found: " + path);
}
if (!Files.isReadable(path)) {
    throw new IOException("File not readable: " + path);
}
```

**Scenario 2: OutOfMemoryError when reading file**

**Problem:**
```java
List<String> lines = Files.readAllLines(path); // OOM for large file!
```

**Solution:**
```java
// Process line by line
try (Stream<String> lines = Files.lines(path)) {
    lines.forEach(this::processLine);
}
```

**Scenario 3: File locked on Windows**

**Problem:**
```java
Files.delete(path); // Exception: file is being used by another process
```

**Solution:**
```java
// Ensure file is closed before deletion
try (FileChannel channel = FileChannel.open(path)) {
    // Use channel
} // Automatically closed
Files.delete(path); // Now safe to delete
```

**Scenario 4: Serialization version mismatch**

**Problem:**
```java
// Serialized with old version, deserializing with new version
// InvalidClassException: local class incompatible
```

**Solution:**
```java
class Person implements Serializable {
    private static final long serialVersionUID = 1L; // Explicit UID
    
    // Handle version compatibility
    private void readObject(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        // Handle missing fields from old versions
    }
}
```

**Scenario 5: Character encoding issues**

**Problem:**
```java
// Text appears garbled after reading
FileReader reader = new FileReader("file.txt"); // Wrong encoding!
```

**Solution:**
```java
// Specify encoding explicitly
try (BufferedReader reader = Files.newBufferedReader(
        path, StandardCharsets.UTF_8)) {
    // Read with correct encoding
}
```

---

[← Back to Index](00-README.md) | [Previous: JVM Internals](10-JVM-Internals.md) | [Next: Networking →](12-Networking.md)
