# Chapter 12: Networking

[← Back to Index](00-README.md) | [Previous: File I/O](11-File-IO.md) | [Next: JDBC →](13-JDBC.md)

---

## Why This Chapter Matters

> **The Reality**: Modern applications are distributed. Whether you're building microservices, REST APIs, 
> real-time chat systems, or integrating with third-party services, networking is fundamental to backend development.

Networking enables applications to communicate across machines, making distributed systems possible. Understanding Java's networking capabilities—from basic sockets to modern HTTP clients—is essential for building scalable, production-ready applications. This chapter covers everything from TCP/UDP fundamentals to advanced NIO patterns and virtual threads.

---

## Layer 1 — Beginner Foundation

### Networking Basics

```
┌────────────────────────────────────────────────────────────────────┐
│                    NETWORKING FUNDAMENTALS                          │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  IP ADDRESS (IPv4)          PORT              PROTOCOL             │
│  ─────────────────          ────              ────────            │
│  192.168.1.100              8080              TCP/UDP             │
│  (Identifies machine)       (Identifies app)  (Communication rule)│
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐     │
│  │                    CLIENT-SERVER MODEL                    │     │
│  │                                                           │     │
│  │  ┌──────────┐                    ┌──────────┐           │     │
│  │  │  CLIENT  │  ────Request────→  │  SERVER  │           │     │
│  │  │          │  ←───Response────  │          │           │     │
│  │  │ Port:    │                    │ Port:    │           │     │
│  │  │ Random   │                    │ 8080     │           │     │
│  │  └──────────┘                    └──────────┘           │     │
│  │                                                           │     │
│  │  • Client initiates connection                           │     │
│  │  • Server listens and accepts                           │     │
│  │  • Both communicate via sockets                         │     │
│  └──────────────────────────────────────────────────────────┘     │
│                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

**Key Concepts:**
- **IP Address**: Identifies a machine on the network (e.g., `192.168.1.100`)
- **Port**: Identifies a specific application/service on that machine (0-65535)
- **Protocol**: Rules for communication (TCP, UDP, HTTP, etc.)
- **Socket**: Endpoint for communication between two machines

### TCP vs UDP

```java
┌──────────────────────────────────────────────────────────────────┐
│                      TCP vs UDP                                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  TCP (Transmission Control Protocol)                             │
│  ────────────────────────────────                                │
│  ✅ Connection-oriented (handshake required)                     │
│  ✅ Reliable (guaranteed delivery)                                │
│  ✅ Ordered (data arrives in order)                              │
│  ✅ Error-checked                                                │
│  ❌ Slower (overhead for reliability)                             │
│  📌 Use for: HTTP, FTP, email, file transfer                    │
│                                                                   │
│  UDP (User Datagram Protocol)                                    │
│  ──────────────────────────────                                  │
│  ✅ Connectionless (no handshake)                                 │
│  ✅ Fast (low overhead)                                          │
│  ✅ Lightweight                                                  │
│  ❌ Unreliable (packets may be lost)                             │
│  ❌ No ordering guarantee                                        │
│  📌 Use for: Video streaming, gaming, DNS, real-time data       │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Socket Programming Basics

**Socket**: A communication endpoint that allows two machines to communicate.

```java
import java.io.*;
import java.net.*;

// Simple Server
public class SimpleServer {
    public static void main(String[] args) throws IOException {
        // Create server socket listening on port 8080
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Server started on port 8080...");
        
        // Wait for client connection (BLOCKING)
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected: " + clientSocket.getInetAddress());
        
        // Get input stream to read from client
        BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream())
        );
        
        // Get output stream to write to client
        PrintWriter out = new PrintWriter(
            clientSocket.getOutputStream(), true
        );
        
        // Read message from client
        String message = in.readLine();
        System.out.println("Received: " + message);
        
        // Send response
        out.println("Echo: " + message);
        
        // Close connections
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
}

// Simple Client
public class SimpleClient {
    public static void main(String[] args) throws IOException {
        // Connect to server at localhost:8080
        Socket socket = new Socket("localhost", 8080);
        
        // Get streams
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream())
        );
        
        // Send message
        out.println("Hello Server!");
        
        // Read response
        String response = in.readLine();
        System.out.println("Server response: " + response);
        
        // Close
        out.close();
        in.close();
        socket.close();
    }
}
```

**Running the Example:**
1. Start the server first: `java SimpleServer`
2. Then run the client: `java SimpleClient`

### URL and URLConnection

```java
import java.net.*;
import java.io.*;

public class URLExample {
    public static void main(String[] args) throws IOException {
        // Parse URL
        URL url = new URL("https://api.github.com/users/octocat");
        
        System.out.println("Protocol: " + url.getProtocol());  // https
        System.out.println("Host: " + url.getHost());          // api.github.com
        System.out.println("Path: " + url.getPath());          // /users/octocat
        System.out.println("Port: " + url.getPort());          // -1 (default)
        
        // Open connection
        URLConnection connection = url.openConnection();
        
        // Set request properties
        connection.setRequestProperty("User-Agent", "Java Client");
        connection.setConnectTimeout(5000);  // 5 seconds
        connection.setReadTimeout(10000);    // 10 seconds
        
        // Read response
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
```

**Key URL Methods:**
- `getProtocol()`: http, https, ftp, etc.
- `getHost()`: Domain name or IP
- `getPath()`: Path component
- `getPort()`: Port number (-1 if default)
- `getQuery()`: Query string parameters

---

## Layer 2 — Working Developer Level

### ServerSocket for Multi-Client Handling

The basic server example only handles one client. Real servers need to handle multiple clients concurrently.

```java
import java.io.*;
import java.net.*;

public class MultiClientServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Server started on port 8080...");
        
        while (true) {
            // Accept new client (blocks until client connects)
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client: " + clientSocket.getInetAddress());
            
            // Handle each client in a separate thread
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }
    
    // Inner class to handle individual client
    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
                );
                PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), true
                )
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Client " + clientSocket.getPort() + 
                                     " says: " + inputLine);
                    
                    // Echo back
                    out.println("Echo: " + inputLine);
                    
                    // Exit condition
                    if ("bye".equalsIgnoreCase(inputLine)) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Client disconnected: " + 
                                     clientSocket.getPort());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

**Thread-Per-Connection Model:**
- ✅ Simple to understand and implement
- ✅ Each client gets dedicated thread
- ❌ Limited scalability (thread overhead)
- ❌ Not suitable for thousands of concurrent connections

### HttpURLConnection for REST Calls

```java
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HttpURLConnectionExample {
    
    // GET request
    public static void getRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // Set request method
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        
        // Check response code
        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Read response
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                System.out.println("Response: " + response.toString());
            }
        } else {
            // Read error stream
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            }
        }
        
        conn.disconnect();
    }
    
    // POST request
    public static void postRequest(String urlString, String jsonData) 
            throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);  // Enable writing to connection
        
        // Write request body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        System.out.println("POST Response Code: " + responseCode);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        
        conn.disconnect();
    }
    
    public static void main(String[] args) throws IOException {
        // GET example
        getRequest("https://api.github.com/users/octocat");
        
        // POST example
        String jsonData = "{\"name\":\"John\",\"age\":30}";
        postRequest("https://httpbin.org/post", jsonData);
    }
}
```

### java.net.http.HttpClient (Java 11+)

The modern, recommended way to make HTTP requests in Java 11+.

```java
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpClientExample {
    
    public static void main(String[] args) throws Exception {
        // Create HttpClient with configuration
        HttpClient client = HttpClient.newBuilder()
            .version(Version.HTTP_2)              // Use HTTP/2
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        
        // Synchronous GET request
        synchronousGet(client);
        
        // Asynchronous GET request
        asynchronousGet(client);
        
        // POST request
        postRequest(client);
    }
    
    // Synchronous request
    static void synchronousGet(HttpClient client) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/users/octocat"))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
        System.out.println("Headers: " + response.headers().map());
    }
    
    // Asynchronous request
    static void asynchronousGet(HttpClient client) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/users/octocat"))
            .GET()
            .build();
        
        CompletableFuture<HttpResponse<String>> future = client.sendAsync(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        future.thenApply(HttpResponse::body)
              .thenAccept(body -> System.out.println("Async Response: " + body))
              .join();  // Wait for completion
    }
    
    // POST request
    static void postRequest(HttpClient client) throws Exception {
        String jsonBody = "{\"name\":\"John\",\"age\":30}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://httpbin.org/post"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        System.out.println("POST Status: " + response.statusCode());
        System.out.println("POST Response: " + response.body());
    }
}
```

**HttpClient Advantages:**
- ✅ Modern API (Java 11+)
- ✅ Built-in support for HTTP/2
- ✅ Asynchronous support
- ✅ Better timeout handling
- ✅ Cleaner API than HttpURLConnection

### Synchronous vs Asynchronous HTTP Requests

```java
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.CompletableFuture;
import java.util.List;

public class SyncVsAsync {
    
    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();
        List<String> urls = List.of(
            "https://api.github.com/users/octocat",
            "https://api.github.com/users/torvalds",
            "https://api.github.com/users/gvanrossum"
        );
        
        // Synchronous: Sequential (slow)
        long startSync = System.currentTimeMillis();
        synchronousRequests(client, urls);
        long syncTime = System.currentTimeMillis() - startSync;
        System.out.println("Synchronous time: " + syncTime + "ms\n");
        
        // Asynchronous: Parallel (fast)
        long startAsync = System.currentTimeMillis();
        asynchronousRequests(client, urls);
        long asyncTime = System.currentTimeMillis() - startAsync;
        System.out.println("Asynchronous time: " + asyncTime + "ms");
    }
    
    // Synchronous: One request at a time
    static void synchronousRequests(HttpClient client, List<String> urls) {
        for (String url : urls) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                
                HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                System.out.println("Sync: " + url + " -> " + response.statusCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Asynchronous: All requests in parallel
    static void asynchronousRequests(HttpClient client, List<String> urls) {
        List<CompletableFuture<HttpResponse<String>>> futures = urls.stream()
            .map(url -> {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                
                return client.sendAsync(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
            })
            .toList();
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                futures.forEach(future -> {
                    try {
                        HttpResponse<String> response = future.get();
                        System.out.println("Async: " + response.uri() + 
                                         " -> " + response.statusCode());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            })
            .join();
    }
}
```

**When to Use:**
- **Synchronous**: Simple requests, sequential dependencies, easier error handling
- **Asynchronous**: Multiple independent requests, better performance, non-blocking operations

### Handling Timeouts and Retries

```java
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class TimeoutAndRetry {
    
    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        
        // Request with timeout
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/users/octocat"))
            .timeout(Duration.ofSeconds(3))  // Request timeout
            .GET()
            .build();
        
        // Retry logic
        retryRequest(client, request, 3);
    }
    
    static HttpResponse<String> retryRequest(
            HttpClient client, 
            HttpRequest request, 
            int maxRetries) {
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("Attempt " + attempt + " of " + maxRetries);
                
                HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                
                if (response.statusCode() == 200) {
                    System.out.println("Success!");
                    return response;
                }
                
            } catch (java.net.http.HttpTimeoutException e) {
                System.err.println("Timeout on attempt " + attempt);
                if (attempt == maxRetries) {
                    throw new RuntimeException("Max retries exceeded", e);
                }
                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000 * attempt);  // 1s, 2s, 3s...
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Max retries exceeded", e);
                }
            }
        }
        
        throw new RuntimeException("Failed after " + maxRetries + " attempts");
    }
}
```

### SSL/TLS Basics

```java
import javax.net.ssl.*;
import java.net.URI;
import java.net.http.*;
import java.security.cert.X509Certificate;

public class SSLExample {
    
    public static void main(String[] args) throws Exception {
        // Trust all certificates (NOT for production!)
        HttpClient client = HttpClient.newBuilder()
            .sslContext(createTrustAllSSLContext())
            .build();
        
        // HTTPS request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/users/octocat"))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        System.out.println("HTTPS Response: " + response.statusCode());
    }
    
    // ⚠️ WARNING: This trusts all certificates - ONLY for development/testing!
    static SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc;
    }
}
```

**SSL/TLS Key Points:**
- **TLS (Transport Layer Security)**: Encrypts data in transit
- **Certificate**: Proves server identity
- **HTTPS**: HTTP over TLS
- **Production**: Always use proper certificate validation

---

## Layer 3 — Advanced Engineering Depth

### NIO Networking (Non-Blocking I/O)

Traditional socket I/O is blocking—each operation waits until complete. NIO provides non-blocking I/O for better scalability.

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {
    public static void main(String[] args) throws IOException {
        // Create server socket channel (non-blocking)
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);  // Non-blocking mode
        serverChannel.bind(new InetSocketAddress(8080));
        
        // Create selector to monitor channels
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("NIO Server started on port 8080...");
        
        while (true) {
            // Block until at least one channel is ready (or timeout)
            int readyChannels = selector.select(1000);  // 1 second timeout
            
            if (readyChannels == 0) {
                continue;  // No channels ready, check again
            }
            
            // Get set of ready channels
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                
                if (key.isAcceptable()) {
                    // New client connection
                    handleAccept(selector, serverChannel);
                } else if (key.isReadable()) {
                    // Data available to read
                    handleRead(key);
                } else if (key.isWritable()) {
                    // Channel ready for writing
                    handleWrite(key);
                }
                
                keyIterator.remove();  // Remove processed key
            }
        }
    }
    
    static void handleAccept(Selector selector, ServerSocketChannel serverChannel) 
            throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        
        // Register for read operations
        clientChannel.register(selector, SelectionKey.OP_READ);
        
        System.out.println("Client connected: " + clientChannel.getRemoteAddress());
    }
    
    static void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        int bytesRead = channel.read(buffer);
        
        if (bytesRead == -1) {
            // Client disconnected
            channel.close();
            System.out.println("Client disconnected");
            return;
        }
        
        if (bytesRead > 0) {
            buffer.flip();  // Prepare for reading
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String message = new String(bytes);
            
            System.out.println("Received: " + message);
            
            // Echo back
            buffer.clear();
            buffer.put(("Echo: " + message).getBytes());
            buffer.flip();
            channel.write(buffer);
            
            // Register for write if buffer not fully written
            if (buffer.hasRemaining()) {
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        }
    }
    
    static void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        // Write logic here
        key.interestOps(SelectionKey.OP_READ);  // Back to read mode
    }
}
```

**NIO Key Components:**
- **Channel**: Represents connection (SocketChannel, ServerSocketChannel)
- **Buffer**: Container for data (ByteBuffer)
- **Selector**: Monitors multiple channels for events
- **SelectionKey**: Represents channel registration with selector

**NIO vs Traditional I/O:**
- **Traditional**: One thread per connection (blocking)
- **NIO**: One thread handles many connections (non-blocking)
- **Scalability**: NIO can handle thousands of connections with few threads

### Non-Blocking I/O with Selectors

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class SelectorExample {
    
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        
        // Register multiple channels
        ServerSocketChannel server1 = createServer(8080, selector);
        ServerSocketChannel server2 = createServer(8081, selector);
        
        System.out.println("Servers started on ports 8080 and 8081");
        
        while (true) {
            // Wait for events (blocks until at least one channel ready)
            int readyCount = selector.select();
            
            if (readyCount == 0) continue;
            
            // Process all ready channels
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                
                if (key.isAcceptable()) {
                    acceptConnection(key, selector);
                } else if (key.isReadable()) {
                    readData(key);
                }
                
                iterator.remove();  // Important!
            }
        }
    }
    
    static ServerSocketChannel createServer(int port, Selector selector) 
            throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(port));
        channel.register(selector, SelectionKey.OP_ACCEPT);
        return channel;
    }
    
    static void acceptConnection(SelectionKey key, Selector selector) 
            throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        
        System.out.println("Accepted connection on port " + 
                          ((InetSocketAddress) serverChannel.getLocalAddress()).getPort());
    }
    
    static void readData(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        int bytesRead = channel.read(buffer);
        if (bytesRead > 0) {
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            System.out.println("Read: " + new String(bytes));
        } else if (bytesRead < 0) {
            channel.close();
        }
    }
}
```

**SelectionKey Operations:**
- `OP_ACCEPT`: Server socket ready to accept connection
- `OP_CONNECT`: Client socket ready to complete connection
- `OP_READ`: Channel ready for reading
- `OP_WRITE`: Channel ready for writing

### Building Scalable Servers

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScalableServer {
    private static final int WORKER_THREADS = Runtime.getRuntime().availableProcessors();
    private final ExecutorService workerPool = Executors.newFixedThreadPool(WORKER_THREADS);
    
    public void start(int port) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("Scalable server started on port " + port);
        System.out.println("Worker threads: " + WORKER_THREADS);
        
        // Main selector loop (runs in single thread)
        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                
                if (key.isAcceptable()) {
                    handleAccept(selector, serverChannel);
                } else if (key.isReadable()) {
                    // Offload processing to worker thread
                    workerPool.submit(() -> handleRead(key));
                }
            }
        }
    }
    
    private void handleAccept(Selector selector, ServerSocketChannel serverChannel) 
            throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Client connected: " + clientChannel.getRemoteAddress());
    }
    
    private void handleRead(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            
            int bytesRead = channel.read(buffer);
            if (bytesRead > 0) {
                buffer.flip();
                // Process request (could be complex business logic)
                processRequest(buffer, channel);
            } else if (bytesRead < 0) {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void processRequest(ByteBuffer buffer, SocketChannel channel) 
            throws IOException {
        // Simulate processing
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String request = new String(bytes);
        
        // Business logic here...
        String response = "Processed: " + request;
        
        // Send response
        ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
        channel.write(responseBuffer);
    }
    
    public static void main(String[] args) throws IOException {
        new ScalableServer().start(8080);
    }
}
```

**Scalability Patterns:**
- **Reactor Pattern**: Single thread handles I/O, workers handle processing
- **Proactor Pattern**: Asynchronous I/O operations
- **Connection Pooling**: Reuse connections instead of creating new ones

### Connection Pooling Concepts

```java
import java.net.http.*;
import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPoolExample {
    
    // Simple connection pool using HttpClient's built-in pooling
    public static void main(String[] args) {
        // HttpClient automatically pools connections
        HttpClient client = HttpClient.newBuilder()
            .build();  // Uses default connection pool
        
        // Multiple requests reuse connections
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        for (int i = 0; i < 100; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/users/octocat"))
                        .GET()
                        .build();
                    
                    HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                    );
                    
                    System.out.println("Request " + requestId + 
                                     ": " + response.statusCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        executor.shutdown();
    }
}

// Custom connection pool (conceptual)
class SimpleConnectionPool {
    private final BlockingQueue<HttpClient> pool;
    private final AtomicInteger created = new AtomicInteger(0);
    private final int maxSize;
    
    public SimpleConnectionPool(int maxSize) {
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>();
    }
    
    public HttpClient borrow() {
        HttpClient client = pool.poll();
        if (client == null && created.get() < maxSize) {
            client = HttpClient.newHttpClient();
            created.incrementAndGet();
        }
        return client;
    }
    
    public void returnToPool(HttpClient client) {
        if (pool.size() < maxSize) {
            pool.offer(client);
        }
    }
}
```

**Connection Pooling Benefits:**
- ✅ Reuse TCP connections (avoid handshake overhead)
- ✅ Limit resource usage
- ✅ Better performance for multiple requests
- ✅ Control connection lifecycle

### WebSocket Basics

```java
import java.net.URI;
import java.net.http.*;
import java.net.http.WebSocket.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class WebSocketClient {
    
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        // Create WebSocket
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
            .buildAsync(
                URI.create("wss://echo.websocket.org"),
                new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("WebSocket opened");
                        webSocket.sendText("Hello Server!", true);
                    }
                    
                    @Override
                    public void onText(WebSocket webSocket, CharSequence data, 
                                     boolean last) {
                        System.out.println("Received: " + data);
                    }
                    
                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.err.println("Error: " + error.getMessage());
                    }
                    
                    @Override
                    public void onClose(WebSocket webSocket, int statusCode, 
                                      String reason) {
                        System.out.println("WebSocket closed: " + reason);
                    }
                }
            );
        
        WebSocket ws = wsFuture.join();
        
        // Keep connection alive
        Thread.sleep(5000);
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Goodbye");
    }
}
```

**WebSocket Characteristics:**
- ✅ Full-duplex communication
- ✅ Low latency
- ✅ Persistent connection
- 📌 Use for: Real-time chat, live updates, gaming, collaborative editing

### HTTP/2 Support in HttpClient

```java
import java.net.URI;
import java.net.http.*;

public class HTTP2Example {
    
    public static void main(String[] args) throws Exception {
        // HTTP/2 is default in Java 11+ HttpClient
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)  // Explicitly set HTTP/2
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://http2.akamai.com/demo"))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        System.out.println("HTTP Version: " + response.version());
        System.out.println("Status: " + response.statusCode());
    }
}
```

**HTTP/2 Advantages:**
- ✅ Multiplexing (multiple requests over single connection)
- ✅ Header compression (HPACK)
- ✅ Server push capability
- ✅ Binary protocol (more efficient)

### Virtual Threads with Networking (Java 21)

Virtual threads (Project Loom) revolutionize concurrent networking by allowing millions of lightweight threads.

```java
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class VirtualThreadServer {
    
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Virtual Thread Server started on port 8080...");
        
        // Use virtual thread executor
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        while (true) {
            Socket clientSocket = serverSocket.accept();
            
            // Each client gets a virtual thread (very lightweight!)
            executor.submit(() -> handleClient(clientSocket));
        }
    }
    
    static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true
            )
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Thread: " + Thread.currentThread() + 
                                 " | Message: " + line);
                out.println("Echo: " + line);
                
                if ("bye".equalsIgnoreCase(line)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

// Virtual threads with HttpClient
class VirtualThreadHttpClient {
    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();
        
        // Can handle millions of concurrent requests!
        for (int i = 0; i < 10_000; i++) {
            final int id = i;
            CompletableFuture.runAsync(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/users/octocat"))
                        .GET()
                        .build();
                    
                    HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                    );
                    
                    System.out.println("Request " + id + ": " + response.statusCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
```

**Virtual Threads Benefits:**
- ✅ Millions of threads possible (vs thousands with platform threads)
- ✅ Blocking I/O is fine (threads are cheap)
- ✅ Simpler code (no need for complex NIO)
- ✅ Better resource utilization

### Network Performance Tuning

```java
import java.net.http.*;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.*;

public class PerformanceTuning {
    
    public static void main(String[] args) {
        // Optimized HttpClient configuration
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)           // Use HTTP/2
            .connectTimeout(Duration.ofSeconds(5))       // Fast connection timeout
            .executor(Executors.newVirtualThreadPerTaskExecutor())  // Virtual threads
            .build();
        
        // Connection pooling is automatic
        // HttpClient reuses connections for same host
        
        // Batch requests for better performance
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(1000);
        
        long start = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/users/octocat"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                    
                    client.send(request, HttpResponse.BodyHandlers.ofString());
                    latch.countDown();
                } catch (Exception e) {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            long duration = System.currentTimeMillis() - start;
            System.out.println("Completed 1000 requests in " + duration + "ms");
            System.out.println("Throughput: " + (1000.0 / duration * 1000) + " req/s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
    }
}
```

**Performance Tips:**
- Use HTTP/2 for multiplexing
- Enable connection pooling
- Use virtual threads for high concurrency
- Set appropriate timeouts
- Batch requests when possible
- Monitor connection usage

---

## Layer 4 — Interview Mastery

### Design a Chat Server

**Requirements:**
- Support multiple clients
- Real-time messaging
- Handle disconnections gracefully
- Scalable architecture

**Design Approach:**

```java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Chat Server Design
public class ChatServer {
    private final ServerSocket serverSocket;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    public ChatServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }
    
    public void start() {
        System.out.println("Chat server started on port " + serverSocket.getLocalPort());
        
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                executor.submit(handler);
            } catch (IOException e) {
                System.err.println("Error accepting client: " + e.getMessage());
            }
        }
    }
    
    public void broadcast(String message, String sender) {
        clients.values().forEach(client -> {
            if (!client.getUsername().equals(sender)) {
                client.sendMessage(sender + ": " + message);
            }
        });
    }
    
    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        broadcast(username + " joined the chat", "SYSTEM");
    }
    
    public void removeClient(String username) {
        clients.remove(username);
        broadcast(username + " left the chat", "SYSTEM");
    }
    
    public static void main(String[] args) throws IOException {
        new ChatServer(8080).start();
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private String username;
    private PrintWriter out;
    private BufferedReader in;
    
    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Get username
            out.println("Enter username:");
            username = in.readLine();
            server.addClient(username, this);
            
            // Handle messages
            String message;
            while ((message = in.readLine()) != null) {
                if ("/quit".equals(message)) {
                    break;
                }
                server.broadcast(message, username);
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                server.removeClient(username);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public String getUsername() {
        return username;
    }
}
```

**Interview Points:**
- **Architecture**: Thread-per-connection vs NIO vs Virtual threads
- **Scalability**: How to handle 10K+ concurrent users
- **Reliability**: Message delivery guarantees, reconnection handling
- **Features**: Private messaging, rooms, file sharing

### How does HTTP/2 differ from HTTP/1.1?

**Key Differences:**

```
┌──────────────────────────────────────────────────────────────────┐
│                    HTTP/1.1 vs HTTP/2                            │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  HTTP/1.1                        HTTP/2                          │
│  ─────────                        ──────                          │
│  • Text-based protocol           • Binary protocol                │
│  • One request per connection    • Multiplexing (many requests)  │
│  • Headers sent every time       • Header compression (HPACK)    │
│  • No server push                • Server push supported         │
│  • Head-of-line blocking         • No head-of-line blocking      │
│                                                                   │
│  Example:                        Example:                        │
│  ┌────────┐                      ┌────────┐                      │
│  │Client  │                      │Client  │                      │
│  └───┬────┘                      └───┬────┘                      │
│      │                                │                           │
│      │ Request 1 ──────────┐         │ Request 1 ────┐           │
│      │ Request 2 ──────────┤         │ Request 2 ────┤           │
│      │ Request 3 ──────────┤         │ Request 3 ────┤  All     │
│      │                     │         │               │  over    │
│  ┌───┴────┐                │         │               │  single  │
│  │Server  │                │         │               │  conn    │
│  └────────┘                │         │               │           │
│      │                     │         │               │           │
│      │ Response 1 ─────────┘         │ Response 1 ───┘           │
│      │ Response 2 ─────────┐         │ Response 2 ────┐          │
│      │ Response 3 ─────────┤         │ Response 3 ────┤          │
│      │                     │         │               │           │
│  Sequential                 │         │               │           │
│  (blocking)                 │         │               │           │
│                             │         │               │           │
│  Multiple connections       │         │ Single connection         │
│  needed for parallelism     │         │ with multiplexing         │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

**Technical Details:**
- **Multiplexing**: Multiple requests/responses interleaved on single connection
- **HPACK**: Header compression reduces overhead
- **Server Push**: Server can send resources before client requests them
- **Binary Framing**: More efficient than text parsing

### Explain Blocking vs Non-Blocking I/O

**Blocking I/O:**

```java
// Blocking: Thread waits until operation completes
Socket socket = new Socket("example.com", 80);
InputStream in = socket.getInputStream();

// This BLOCKS the thread until data arrives
int data = in.read();  // Thread waits here, can't do anything else
```

**Characteristics:**
- Thread is blocked during I/O operation
- Simple to understand and code
- One thread per connection
- Limited scalability (thread overhead)

**Non-Blocking I/O:**

```java
// Non-blocking: Returns immediately, check later if data available
SocketChannel channel = SocketChannel.open();
channel.configureBlocking(false);
Selector selector = Selector.open();
channel.register(selector, SelectionKey.OP_READ);

// This returns immediately (doesn't block)
selector.select();  // Returns when data available
```

**Characteristics:**
- Thread continues execution immediately
- Check readiness before performing I/O
- One thread can handle many connections
- Better scalability

**Comparison:**

| Aspect | Blocking I/O | Non-Blocking I/O |
|--------|-------------|------------------|
| Thread behavior | Waits for I/O | Continues immediately |
| Scalability | Limited (1 thread/conn) | High (1 thread/many conns) |
| Complexity | Simple | More complex |
| Use case | Low concurrency | High concurrency |

### Connection Pooling Interview Questions

**Q: What is connection pooling and why is it important?**

**A:** Connection pooling reuses existing TCP connections instead of creating new ones for each request.

**Benefits:**
- Avoids TCP handshake overhead (3-way handshake)
- Reduces resource usage (file descriptors, memory)
- Better performance for multiple requests
- Controls connection lifecycle

**Q: How does HttpClient implement connection pooling?**

**A:** HttpClient automatically pools connections per `(scheme, host, port)` tuple. Connections are reused when:
- Same host and port
- Connection is still alive
- Pool hasn't reached maximum size

**Q: What happens when connection pool is exhausted?**

**A:** New requests either:
- Wait for available connection (if queue enabled)
- Create new connection (if under limit)
- Fail (if max connections reached)

**Q: How to tune connection pool size?**

**A:** Consider:
- Concurrent request volume
- Server connection limits
- Network latency
- Resource constraints

```java
// HttpClient uses system properties for tuning
System.setProperty("jdk.httpclient.connectionPoolSize", "100");
System.setProperty("jdk.httpclient.keepAliveTimeout", "30");
```

### Debugging Network Issues

**Common Issues and Solutions:**

1. **Connection Timeout**
   ```java
   // Symptom: java.net.SocketTimeoutException
   // Solution: Increase timeout or check network
   conn.setConnectTimeout(10000);  // 10 seconds
   ```

2. **Read Timeout**
   ```java
   // Symptom: Read operation times out
   // Solution: Increase read timeout or optimize server
   conn.setReadTimeout(30000);  // 30 seconds
   ```

3. **Connection Refused**
   ```java
   // Symptom: java.net.ConnectException: Connection refused
   // Causes: Server not running, wrong port, firewall
   // Debug: Check server status, verify port, check firewall rules
   ```

4. **SSL/TLS Issues**
   ```java
   // Symptom: javax.net.ssl.SSLHandshakeException
   // Causes: Certificate issues, protocol mismatch
   // Debug: Check certificate validity, verify TLS version
   ```

5. **Too Many Open Files**
   ```java
   // Symptom: java.net.SocketException: Too many open files
   // Cause: Not closing connections properly
   // Solution: Use try-with-resources, implement connection pooling
   ```

**Debugging Tools:**
- **Wireshark**: Network packet analysis
- **tcpdump**: Command-line packet capture
- **netstat**: View network connections
- **Java logging**: Enable HttpClient logging
  ```java
  System.setProperty("jdk.httpclient.HttpClient.log", "all");
  ```

**Best Practices:**
- Always close connections (try-with-resources)
- Set appropriate timeouts
- Handle exceptions properly
- Monitor connection usage
- Use connection pooling
- Log network operations for debugging

---

[← Back to Index](00-README.md) | [Previous: File I/O](11-File-IO.md) | [Next: JDBC →](13-JDBC.md)
