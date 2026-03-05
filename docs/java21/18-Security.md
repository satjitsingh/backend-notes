# Chapter 18: Security Basics

[← Back to Index](00-README.md) | [Previous: Testing](17-Testing.md) | [Next: Java 21 Features →](19-Java21-Features.md)

---

## Why This Chapter Matters

Security is not optional in modern software development. Every application that handles user data, processes transactions, or communicates over networks must be built with security as a fundamental concern, not an afterthought. Security vulnerabilities can lead to data breaches, financial losses, legal liabilities, and irreparable damage to reputation.

In interviews, security questions are extremely common because they test your understanding of:
- **Real-world impact**: How vulnerabilities affect production systems
- **Best practices**: Industry-standard approaches to secure coding
- **Problem-solving**: How you identify and mitigate security risks
- **Depth of knowledge**: Understanding beyond surface-level concepts

Whether you're building a simple web application or an enterprise system, security knowledge is essential. This chapter covers everything from basic principles to advanced cryptographic concepts, preparing you for both practical development and technical interviews.

---

## Layer 1 — Beginner Foundation

### Security Fundamentals: The CIA Triad

The foundation of information security rests on three core principles:

- **Confidentiality**: Ensuring that information is accessible only to authorized users
- **Integrity**: Ensuring that information remains accurate and unmodified
- **Availability**: Ensuring that information and systems are accessible when needed

```java
// Example: Understanding confidentiality
public class ConfidentialityExample {
    // BAD: Sensitive data exposed in logs
    public void processPayment(String creditCard) {
        System.out.println("Processing card: " + creditCard); // ❌ Never do this!
    }
    
    // GOOD: Mask sensitive data
    public void processPayment(String creditCard) {
        String masked = maskCardNumber(creditCard);
        System.out.println("Processing card: " + masked); // ✅ Safe
    }
    
    private String maskCardNumber(String card) {
        if (card.length() < 4) return "****";
        return "****-****-****-" + card.substring(card.length() - 4);
    }
}
```

### Common Vulnerabilities Overview

Understanding common vulnerabilities helps you avoid them:

1. **Injection Attacks**: SQL, NoSQL, OS command injection
2. **Broken Authentication**: Weak passwords, session hijacking
3. **Sensitive Data Exposure**: Unencrypted data, weak encryption
4. **XML External Entities (XXE)**: XML parser vulnerabilities
5. **Broken Access Control**: Unauthorized access to resources
6. **Security Misconfiguration**: Default credentials, exposed debug info
7. **Cross-Site Scripting (XSS)**: Malicious scripts in web pages
8. **Insecure Deserialization**: Object injection vulnerabilities
9. **Using Components with Known Vulnerabilities**: Outdated libraries
10. **Insufficient Logging & Monitoring**: Inability to detect attacks

### Input Validation Importance

**Never trust user input.** Always validate and sanitize:

```java
public class InputValidation {
    
    // BAD: No validation
    public void processUsername(String username) {
        // Direct use - dangerous!
        String query = "SELECT * FROM users WHERE name = '" + username + "'";
        // ❌ Vulnerable to SQL injection
    }
    
    // GOOD: Validate and sanitize
    public void processUsername(String username) {
        // Validate format
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        
        // Check length
        if (username.length() > 50) {
            throw new IllegalArgumentException("Username too long");
        }
        
        // Check for dangerous characters
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid username format");
        }
        
        // Now safe to use
        // ✅ Use PreparedStatement for database queries
    }
    
    // Email validation example
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Simple regex - use Apache Commons Validator or similar in production
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                           "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
}
```

### Password Handling Basics

**Golden Rule: Never store passwords in plain text.**

```java
public class PasswordBasics {
    
    // ❌ NEVER DO THIS
    public void storePasswordBad(String username, String password) {
        // Storing plain text password - major security flaw!
        database.save(username, password);
    }
    
    // ✅ CORRECT APPROACH
    public void storePasswordGood(String username, String password) {
        // Hash the password before storing
        String hashedPassword = hashPassword(password);
        database.save(username, hashedPassword);
    }
    
    // Basic hashing (we'll improve this in Layer 2)
    private String hashPassword(String password) {
        // This is simplified - use proper hashing in production
        // See Layer 2 for bcrypt/scrypt examples
        return Integer.toString(password.hashCode()); // Still not secure enough!
    }
    
    // Password strength validation
    public boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> 
            "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
```

### HTTPS vs HTTP

**HTTP (HyperText Transfer Protocol)**:
- Data transmitted in plain text
- No encryption
- Vulnerable to man-in-the-middle attacks
- Port 80

**HTTPS (HTTP Secure)**:
- Data encrypted using SSL/TLS
- Prevents eavesdropping and tampering
- Port 443
- Requires SSL certificate

```java
// Example: Configuring HTTPS in Spring Boot
// application.properties:
// server.ssl.key-store=classpath:keystore.p12
// server.ssl.key-store-password=changeit
// server.ssl.key-store-type=PKCS12

// Always redirect HTTP to HTTPS
@Configuration
public class HttpsRedirectConfig {
    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = 
            new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        factory.addAdditionalTomcatConnectors(redirectConnector());
        return factory;
    }
    
    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

---

## Layer 2 — Working Developer Level

### SQL Injection Prevention

**Always use PreparedStatement, never string concatenation:**

```java
import java.sql.*;

public class SqlInjectionPrevention {
    
    // ❌ VULNERABLE TO SQL INJECTION
    public User getUserBad(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = '" + username + "'";
        // If username = "admin' OR '1'='1", this becomes:
        // SELECT * FROM users WHERE username = 'admin' OR '1'='1'
        // This returns ALL users!
        
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        // ... process results
        return null;
    }
    
    // ✅ SECURE: Using PreparedStatement
    public User getUserGood(String username) throws SQLException {
        String query = "SELECT * FROM users WHERE username = ?";
        
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, username); // Parameters are escaped automatically
        
        ResultSet rs = pstmt.executeQuery();
        // ... process results
        return null;
    }
    
    // ✅ Using JPA/Hibernate (also safe)
    @Repository
    public class UserRepository {
        @Autowired
        private EntityManager entityManager;
        
        public User findByUsername(String username) {
            // JPA uses parameterized queries automatically
            return entityManager.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getSingleResult();
        }
    }
}
```

### XSS (Cross-Site Scripting) Prevention

XSS occurs when untrusted data is included in web pages without proper escaping:

```java
import org.springframework.web.util.HtmlUtils;

public class XssPrevention {
    
    // ❌ VULNERABLE TO XSS
    @GetMapping("/greet")
    public String greetBad(@RequestParam String name, Model model) {
        // If name = "<script>alert('XSS')</script>", this executes!
        model.addAttribute("message", "Hello, " + name);
        return "greeting";
    }
    
    // ✅ SECURE: Escape HTML
    @GetMapping("/greet")
    public String greetGood(@RequestParam String name, Model model) {
        // Escape special characters
        String safeName = HtmlUtils.htmlEscape(name);
        model.addAttribute("message", "Hello, " + safeName);
        return "greeting";
    }
    
    // ✅ Using Thymeleaf (auto-escaping enabled by default)
    // In Thymeleaf template:
    // <p th:text="${name}">Name</p> <!-- Auto-escaped -->
    // <p th:utext="${name}">Name</p> <!-- Unescaped - use carefully! -->
    
    // ✅ Content Security Policy header
    @Configuration
    public class SecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'")
                )
            );
            return http.build();
        }
    }
}
```

### CSRF Protection

Cross-Site Request Forgery (CSRF) protection prevents unauthorized actions:

```java
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
public class CsrfProtection {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Enable CSRF for state-changing operations
                .ignoringRequestMatchers("/api/public/**") // Disable for public APIs
            );
        return http.build();
    }
    
    // In your form (Thymeleaf example):
    // <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    
    // For REST APIs, include CSRF token in header:
    // X-CSRF-TOKEN: <token-value>
}
```

### Secure Password Storage

**Use proper hashing algorithms with salt:**

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurePasswordStorage {
    
    // ✅ RECOMMENDED: Using BCrypt (Spring Security)
    private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder(12);
    
    public String hashPasswordBcrypt(String password) {
        // BCrypt automatically generates and stores salt
        // Format: $2a$12$<salt><hash>
        return bcryptEncoder.encode(password);
    }
    
    public boolean verifyPasswordBcrypt(String password, String hashedPassword) {
        return bcryptEncoder.matches(password, hashedPassword);
    }
    
    // ✅ Using SCrypt (more secure, slower)
    private final SCryptPasswordEncoder scryptEncoder = new SCryptPasswordEncoder();
    
    public String hashPasswordScrypt(String password) {
        return scryptEncoder.encode(password);
    }
    
    // ✅ Using Argon2 (most secure, recommended for new projects)
    private final Argon2PasswordEncoder argon2Encoder = 
        Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    
    public String hashPasswordArgon2(String password) {
        return argon2Encoder.encode(password);
    }
    
    // Manual hashing with salt (for understanding, prefer libraries above)
    public String hashPasswordWithSalt(String password) throws NoSuchAlgorithmException {
        // Generate random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        
        // Hash password with salt
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hash = md.digest(password.getBytes());
        
        // Combine salt and hash for storage
        byte[] combined = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(hash, 0, combined, salt.length, hash.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    // Example usage
    public class UserService {
        private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        public void createUser(String username, String password) {
            String hashedPassword = passwordEncoder.encode(password);
            // Store username and hashedPassword in database
            // NEVER store the original password
        }
        
        public boolean authenticate(String username, String password) {
            // Retrieve hashed password from database
            String storedHash = getUserPasswordHash(username);
            return passwordEncoder.matches(password, storedHash);
        }
        
        private String getUserPasswordHash(String username) {
            // Fetch from database
            return null; // Implementation
        }
    }
}
```

### java.security Package Basics

```java
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class JavaSecurityBasics {
    
    // MessageDigest for hashing
    public String hashWithSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes());
        
        // Convert to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    // SecureRandom for cryptographically secure random numbers
    public void secureRandomExample() {
        SecureRandom random = new SecureRandom();
        
        // Generate random bytes
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        
        // Generate random integer
        int randomInt = random.nextInt();
        
        // Generate random long
        long randomLong = random.nextLong();
        
        // Generate random number in range [0, bound)
        int randomInRange = random.nextInt(100); // 0-99
    }
    
    // Key generation
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // 2048-bit key
        return keyGen.generateKeyPair();
    }
}
```

### Encryption Basics: Symmetric vs Asymmetric

**Symmetric Encryption**: Same key for encryption and decryption (faster, simpler)

```java
public class SymmetricEncryption {
    
    // AES (Advanced Encryption Standard) - most common symmetric cipher
    public String encryptAES(String plaintext, String password) 
            throws Exception {
        // Generate key from password
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes());
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        
        // Initialize cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        // Encrypt
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        
        // Return Base64 encoded string
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    public String decryptAES(String ciphertext, String password) 
            throws Exception {
        // Generate same key
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes());
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        
        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        // Decrypt
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        
        return new String(decrypted);
    }
    
    // Better approach: Use proper key derivation (PBKDF2)
    public SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
```

**Asymmetric Encryption**: Different keys for encryption and decryption (more secure, slower)

```java
import java.security.*;
import javax.crypto.Cipher;

public class AsymmetricEncryption {
    
    // RSA encryption example
    public byte[] encryptRSA(String plaintext, PublicKey publicKey) 
            throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plaintext.getBytes());
    }
    
    public String decryptRSA(byte[] ciphertext, PrivateKey privateKey) 
            throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted);
    }
    
    // Typical use case: Hybrid encryption
    // 1. Generate random symmetric key
    // 2. Encrypt data with symmetric key (fast)
    // 3. Encrypt symmetric key with recipient's public key (secure)
    // 4. Send both encrypted data and encrypted key
}
```

---

## Layer 3 — Advanced Engineering Depth

### Java Cryptography Architecture (JCA)

JCA provides a framework for cryptographic operations:

```java
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.cert.*;

public class JavaCryptographyArchitecture {
    
    // Provider management
    public void listProviders() {
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            System.out.println("Provider: " + provider.getName());
            System.out.println("Version: " + provider.getVersion());
        }
    }
    
    // Algorithm parameter generation
    public AlgorithmParameters generateAESParameters() throws Exception {
        AlgorithmParameterGenerator paramGen = 
            AlgorithmParameterGenerator.getInstance("AES");
        paramGen.init(256); // 256-bit key size
        return paramGen.generateParameters();
    }
    
    // Certificate handling
    public void loadCertificate(String certPath) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        FileInputStream fis = new FileInputStream(certPath);
        X509Certificate cert = (X509Certificate) factory.generateCertificate(fis);
        
        // Verify certificate
        cert.checkValidity();
        cert.verify(cert.getPublicKey());
    }
}
```

### KeyStore Management

KeyStores store cryptographic keys and certificates:

```java
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class KeyStoreManagement {
    
    // Create a new KeyStore
    public KeyStore createKeyStore(String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password.toCharArray());
        return keyStore;
    }
    
    // Load existing KeyStore
    public KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream(path);
        keyStore.load(fis, password.toCharArray());
        fis.close();
        return keyStore;
    }
    
    // Store a key in KeyStore
    public void storeKey(KeyStore keyStore, String alias, 
                        Key key, String keyPassword, Certificate[] chain) 
            throws Exception {
        keyStore.setKeyEntry(alias, key, keyPassword.toCharArray(), chain);
    }
    
    // Retrieve a key from KeyStore
    public Key retrieveKey(KeyStore keyStore, String alias, String password) 
            throws Exception {
        return keyStore.getKey(alias, password.toCharArray());
    }
    
    // Save KeyStore to file
    public void saveKeyStore(KeyStore keyStore, String path, String password) 
            throws Exception {
        FileOutputStream fos = new FileOutputStream(path);
        keyStore.store(fos, password.toCharArray());
        fos.close();
    }
    
    // Example: SSL/TLS KeyStore setup
    public void setupSSLKeyStore() {
        // System properties for SSL
        System.setProperty("javax.net.ssl.keyStore", "keystore.p12");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
        
        System.setProperty("javax.net.ssl.trustStore", "truststore.p12");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }
}
```

### Digital Signatures

Digital signatures provide authentication and integrity:

```java
import java.security.*;
import java.security.spec.*;

public class DigitalSignatures {
    
    // Generate signature
    public byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }
    
    // Verify signature
    public boolean verifySignature(byte[] data, byte[] signatureBytes, 
                                   PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
    
    // Complete example
    public class DocumentSigner {
        private KeyPair keyPair;
        
        public DocumentSigner() throws NoSuchAlgorithmException {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            this.keyPair = keyGen.generateKeyPair();
        }
        
        public SignedDocument sign(String document) throws Exception {
            byte[] documentBytes = document.getBytes();
            byte[] signature = signData(documentBytes, keyPair.getPrivate());
            
            return new SignedDocument(document, signature, keyPair.getPublic());
        }
        
        public boolean verify(SignedDocument signedDoc) throws Exception {
            return verifySignature(
                signedDoc.getDocument().getBytes(),
                signedDoc.getSignature(),
                signedDoc.getPublicKey()
            );
        }
    }
}
```

### SSL/TLS Certificate Handling

```java
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.KeyStore;

public class SSLCertificateHandling {
    
    // Create SSL context with custom trust manager
    public SSLContext createCustomSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc;
    }
    
    // Proper certificate validation (production use)
    public SSLContext createSecureSSLContext(KeyStore trustStore) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(trustStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
    
    // HTTPS connection with certificate validation
    public void makeSecureHTTPSRequest(String url) throws Exception {
        SSLContext sslContext = SSLContext.getDefault();
        HttpsURLConnection connection = 
            (HttpsURLConnection) new URL(url).openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        // Make request...
    }
}
```

### JWT (JSON Web Tokens) Basics

```java
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

public class JWTBasics {
    
    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    
    // Create JWT
    public String createJWT(String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .claim("role", "USER") // Custom claims
            .signWith(secretKey)
            .compact();
    }
    
    // Verify and parse JWT
    public Claims parseJWT(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException e) {
            throw new SecurityException("Invalid JWT token", e);
        }
    }
    
    // Example usage in Spring Security
    @Component
    public class JwtTokenProvider {
        private final String secret = "your-secret-key-min-256-bits";
        private final long validityInMilliseconds = 3600000; // 1 hour
        
        public String createToken(String username, List<String> roles) {
            Claims claims = Jwts.claims().setSubject(username);
            claims.put("roles", roles);
            
            Date now = new Date();
            Date validity = new Date(now.getTime() + validityInMilliseconds);
            
            return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
        }
        
        public boolean validateToken(String token) {
            try {
                Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        }
    }
}
```

### OAuth 2.0 Concepts

OAuth 2.0 is an authorization framework:

```java
// OAuth 2.0 Flow Overview:
// 1. Client requests authorization from resource owner
// 2. Resource owner grants authorization
// 3. Client receives authorization grant
// 4. Client requests access token with authorization grant
// 5. Authorization server validates grant and issues access token
// 6. Client uses access token to access protected resource

@RestController
public class OAuth2Controller {
    
    // Authorization Code Flow (most common)
    @GetMapping("/oauth/authorize")
    public ResponseEntity<String> authorize(
            @RequestParam String client_id,
            @RequestParam String redirect_uri,
            @RequestParam String response_type,
            @RequestParam String scope) {
        
        // Validate client_id
        // Generate authorization code
        // Store code with client_id and redirect_uri
        // Redirect to redirect_uri with code
        
        String authCode = generateAuthorizationCode(client_id);
        return ResponseEntity.ok("Redirect to: " + redirect_uri + "?code=" + authCode);
    }
    
    @PostMapping("/oauth/token")
    public ResponseEntity<TokenResponse> getToken(
            @RequestParam String grant_type,
            @RequestParam String code,
            @RequestParam String redirect_uri,
            @RequestParam String client_id,
            @RequestParam String client_secret) {
        
        // Validate authorization code
        // Verify client credentials
        // Generate access token and refresh token
        // Return tokens
        
        TokenResponse response = new TokenResponse();
        response.setAccess_token(generateAccessToken());
        response.setToken_type("Bearer");
        response.setExpires_in(3600);
        response.setRefresh_token(generateRefreshToken());
        
        return ResponseEntity.ok(response);
    }
    
    // Using Spring Security OAuth2
    @Configuration
    @EnableResourceServer
    public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                .authorizeRequests()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/api/**").authenticated();
        }
    }
}
```

### Security Manager (Deprecated but Important to Know)

The Security Manager was used to enforce security policies (deprecated in Java 17, removed in Java 21):

```java
public class SecurityManagerExample {
    
    // Security Manager was used like this (now deprecated):
    /*
    public void checkFileAccess() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            FilePermission perm = new FilePermission("/path/to/file", "read");
            sm.checkPermission(perm);
        }
    }
    */
    
    // Modern approach: Use module system (Java 9+) and proper access control
    // module-info.java:
    // module com.example.app {
    //     requires java.base;
    //     exports com.example.publicapi;
    //     // Control what is exported and accessible
    // }
}
```

### Secure Coding Practices

```java
public class SecureCodingPractices {
    
    // 1. Principle of Least Privilege
    // Grant minimum necessary permissions
    public class UserService {
        // Use specific roles, not admin for everything
        @PreAuthorize("hasRole('USER')")
        public void updateOwnProfile(User user) { }
        
        @PreAuthorize("hasRole('ADMIN')")
        public void updateAnyProfile(User user) { }
    }
    
    // 2. Defense in Depth
    // Multiple layers of security
    public class SecureAPI {
        // Layer 1: Network firewall
        // Layer 2: Authentication
        // Layer 3: Authorization
        // Layer 4: Input validation
        // Layer 5: Output encoding
        // Layer 6: Database parameterized queries
    }
    
    // 3. Fail Securely
    public boolean authenticate(String username, String password) {
        try {
            // Authentication logic
            return validateCredentials(username, password);
        } catch (Exception e) {
            // Log error but don't reveal details
            logger.error("Authentication failed for user: " + username);
            // Return false, not true on error
            return false; // ✅ Fail securely
        }
    }
    
    // 4. Don't trust client-side validation
    // Always validate on server side
    @PostMapping("/api/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Client-side validation is nice for UX, but:
        // ✅ Server-side validation is MANDATORY
        validateUserInput(user);
        return ResponseEntity.ok(userService.create(user));
    }
    
    // 5. Secure error handling
    public void handleError(Exception e) {
        // ❌ Don't expose stack traces to users
        // logger.error("Error: " + e.toString()); // May expose sensitive info
        
        // ✅ Log full details internally
        logger.error("Internal error occurred", e);
        
        // ✅ Return generic message to user
        throw new GenericException("An error occurred. Please try again.");
    }
    
    // 6. Secure session management
    public class SessionSecurity {
        // Use secure, HttpOnly cookies
        // Set appropriate session timeout
        // Regenerate session ID after login
        // Invalidate sessions on logout
    }
}
```

### Dependency Vulnerability Scanning

```java
// Use tools like:
// - OWASP Dependency-Check
// - Snyk
// - GitHub Dependabot
// - Maven/Gradle plugins

// Maven example (pom.xml):
/*
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
*/

// Gradle example (build.gradle):
/*
plugins {
    id 'org.owasp.dependencycheck' version '8.0.0'
}

dependencyCheck {
    failBuildOnCVSS = 7.0
    suppressionFile = 'dependency-check-suppressions.xml'
}
*/

// Runtime dependency checking
public class DependencyChecker {
    public void checkDependencies() {
        // Use libraries like:
        // - OWASP Dependency-Check Java API
        // - Snyk Java SDK
        // Scan dependencies at runtime or build time
    }
}
```

### OWASP Top 10 in Java Context

1. **Broken Access Control**
   ```java
   // ❌ Bad: No access control
   @GetMapping("/api/users/{id}")
   public User getUser(@PathVariable Long id) {
       return userRepository.findById(id); // Anyone can access any user
   }
   
   // ✅ Good: Check authorization
   @GetMapping("/api/users/{id}")
   public User getUser(@PathVariable Long id, Authentication auth) {
       User currentUser = getCurrentUser(auth);
       if (!currentUser.getId().equals(id) && !currentUser.isAdmin()) {
           throw new AccessDeniedException();
       }
       return userRepository.findById(id);
   }
   ```

2. **Cryptographic Failures**
   ```java
   // ❌ Bad: Weak encryption
   Cipher cipher = Cipher.getInstance("DES"); // Weak algorithm
   
   // ✅ Good: Strong encryption
   Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Strong algorithm
   ```

3. **Injection**
   ```java
   // Already covered: Use PreparedStatement, parameterized queries
   ```

4. **Insecure Design**
   ```java
   // Design security into the system from the start
   // Use threat modeling
   // Follow secure design principles
   ```

5. **Security Misconfiguration**
   ```java
   // ✅ Proper configuration
   @Configuration
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           http
               .headers(headers -> headers
                   .frameOptions().deny() // Prevent clickjacking
                   .contentTypeOptions().and()
                   .httpStrictTransportSecurity(hsts -> hsts
                       .maxAgeInSeconds(31536000)
                       .includeSubdomains(true)
                   )
               );
           return http.build();
       }
   }
   ```

6. **Vulnerable Components**
   ```java
   // Keep dependencies updated
   // Use dependency scanning tools
   // Remove unused dependencies
   ```

7. **Authentication Failures**
   ```java
   // Implement proper authentication
   // Use strong password policies
   // Implement account lockout
   // Use multi-factor authentication
   ```

8. **Software and Data Integrity Failures**
   ```java
   // Verify integrity of data
   // Use digital signatures
   // Verify software updates
   ```

9. **Security Logging and Monitoring Failures**
   ```java
   public class SecurityLogging {
       private static final Logger logger = LoggerFactory.getLogger(SecurityLogging.class);
       
       public void logSecurityEvent(String event, String details) {
           logger.warn("SECURITY_EVENT: {} - {}", event, details);
           // Send to SIEM system
           // Alert security team
       }
       
       public void logFailedLogin(String username, String ip) {
           logSecurityEvent("FAILED_LOGIN", 
               String.format("User: %s, IP: %s", username, ip));
       }
   }
   ```

10. **Server-Side Request Forgery (SSRF)**
    ```java
    // ❌ Bad: No validation
    public String fetchUrl(String url) {
        return new RestTemplate().getForObject(url, String.class);
    }
    
    // ✅ Good: Validate and whitelist URLs
    public String fetchUrl(String url) {
        if (!isAllowedUrl(url)) {
            throw new IllegalArgumentException("URL not allowed");
        }
        return new RestTemplate().getForObject(url, String.class);
    }
    
    private boolean isAllowedUrl(String url) {
        // Whitelist approach
        List<String> allowedDomains = Arrays.asList("api.example.com", "cdn.example.com");
        try {
            URI uri = new URI(url);
            return allowedDomains.contains(uri.getHost());
        } catch (URISyntaxException e) {
            return false;
        }
    }
    ```

---

## Layer 4 — Interview Mastery

### How to Store Passwords Securely?

**Answer:**

Never store passwords in plain text. Use a one-way hashing algorithm with salt:

1. **Use a strong hashing algorithm**: BCrypt, SCrypt, or Argon2 (not MD5, SHA-1, or plain SHA-256)
2. **Salt**: Each password should have a unique salt (BCrypt handles this automatically)
3. **Cost factor**: Use appropriate work factor (BCrypt rounds: 10-12 recommended)
4. **Never use reversible encryption** for passwords

```java
// ✅ Correct approach
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hashedPassword = encoder.encode(password);
// Store hashedPassword in database

// During login:
boolean matches = encoder.matches(inputPassword, storedHash);
```

**Key Points:**
- One-way hash (cannot be reversed)
- Unique salt per password
- Adaptive cost factor (can increase over time)
- Use established libraries (don't implement yourself)

### Explain Symmetric vs Asymmetric Encryption

**Answer:**

**Symmetric Encryption:**
- **Same key** for encryption and decryption
- **Faster** and more efficient
- **Key distribution problem**: How to securely share the key?
- Examples: AES, DES, 3DES
- Use cases: Bulk data encryption, database encryption

```java
// Same key for both operations
SecretKey key = generateAESKey();
byte[] encrypted = encrypt(data, key);
byte[] decrypted = decrypt(encrypted, key);
```

**Asymmetric Encryption:**
- **Different keys**: Public key (encrypt) and Private key (decrypt)
- **Slower** but more secure for key exchange
- Solves key distribution problem
- Examples: RSA, ECC, ElGamal
- Use cases: Key exchange, digital signatures, SSL/TLS

```java
// Key pair: public and private
KeyPair keyPair = generateRSAKeyPair();
byte[] encrypted = encrypt(data, keyPair.getPublic());
byte[] decrypted = decrypt(encrypted, keyPair.getPrivate());
```

**Hybrid Approach (Common in Practice):**
1. Use asymmetric encryption to exchange a symmetric key
2. Use symmetric encryption for actual data (faster)
3. Example: SSL/TLS uses this approach

### How Would You Secure a REST API?

**Answer:**

Multi-layered security approach:

1. **HTTPS Only**: Enforce TLS/SSL for all communications
2. **Authentication**: 
   - JWT tokens or OAuth 2.0
   - API keys for service-to-service
   - Multi-factor authentication for sensitive operations
3. **Authorization**: Role-based access control (RBAC)
4. **Input Validation**: Validate and sanitize all inputs
5. **Rate Limiting**: Prevent abuse and DoS attacks
6. **CORS Configuration**: Restrict cross-origin requests
7. **Security Headers**: 
   - Content-Security-Policy
   - X-Frame-Options
   - Strict-Transport-Security
8. **Error Handling**: Don't expose sensitive information in errors
9. **Logging**: Log security events (failed logins, access attempts)
10. **Dependency Management**: Keep dependencies updated, scan for vulnerabilities

```java
@Configuration
@EnableWebSecurity
public class APISecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .requiresChannel(channel -> channel.anyRequest().requiresSecure()) // HTTPS only
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .headers(headers -> headers
                .contentSecurityPolicy("default-src 'self'")
                .frameOptions().deny()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") // Stateless APIs
            );
        return http.build();
    }
    
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(100.0); // 100 requests per second
    }
}
```

### Common Java Security Vulnerabilities

**Answer:**

1. **SQL Injection**
   - **Cause**: String concatenation in SQL queries
   - **Prevention**: Always use `PreparedStatement` or JPA parameterized queries

2. **XSS (Cross-Site Scripting)**
   - **Cause**: Unescaped user input in HTML output
   - **Prevention**: Escape output, use Content Security Policy, validate input

3. **Insecure Deserialization**
   - **Cause**: Deserializing untrusted data
   - **Prevention**: Avoid Java serialization, use JSON, validate before deserializing

```java
// ❌ Dangerous
ObjectInputStream ois = new ObjectInputStream(inputStream);
Object obj = ois.readObject(); // Can execute arbitrary code!

// ✅ Safer
ObjectMapper mapper = new ObjectMapper();
MyObject obj = mapper.readValue(jsonString, MyObject.class);
```

4. **Weak Cryptography**
   - **Cause**: Using deprecated algorithms (MD5, SHA-1, DES)
   - **Prevention**: Use modern algorithms (SHA-256+, AES-256, RSA-2048+)

5. **Hardcoded Secrets**
   - **Cause**: Passwords, API keys in source code
   - **Prevention**: Use environment variables, secrets management systems

```java
// ❌ Bad
String apiKey = "sk_live_1234567890";

// ✅ Good
String apiKey = System.getenv("API_KEY");
// Or use Spring Cloud Config, AWS Secrets Manager, etc.
```

6. **Path Traversal**
   - **Cause**: Not validating file paths
   - **Prevention**: Validate and sanitize file paths, use whitelist approach

```java
// ❌ Vulnerable
File file = new File("/data/" + userInput);

// ✅ Secure
String sanitized = Paths.get("/data", userInput)
    .normalize()
    .toString();
if (!sanitized.startsWith("/data")) {
    throw new SecurityException("Invalid path");
}
```

7. **Insecure Random Number Generation**
   - **Cause**: Using `java.util.Random` instead of `SecureRandom`
   - **Prevention**: Always use `SecureRandom` for security-sensitive operations

### Security Code Review Checklist

**Answer:**

**Authentication & Authorization:**
- [ ] All endpoints require authentication (except public APIs)
- [ ] Role-based access control implemented correctly
- [ ] Session management is secure (timeout, invalidation)
- [ ] Password policies enforced (strength, expiration)
- [ ] Multi-factor authentication for sensitive operations

**Input Validation:**
- [ ] All user inputs validated and sanitized
- [ ] SQL queries use parameterized statements
- [ ] File uploads validated (type, size, content)
- [ ] Path traversal prevented
- [ ] Command injection prevented

**Cryptography:**
- [ ] Strong algorithms used (AES-256, RSA-2048+, SHA-256+)
- [ ] Proper key management (not hardcoded)
- [ ] Passwords hashed with salt (BCrypt/SCrypt/Argon2)
- [ ] Secure random number generation (`SecureRandom`)

**Data Protection:**
- [ ] Sensitive data encrypted at rest
- [ ] HTTPS enforced for all communications
- [ ] PII (Personally Identifiable Information) handled according to regulations
- [ ] No sensitive data in logs or error messages

**Configuration:**
- [ ] Default credentials changed
- [ ] Debug mode disabled in production
- [ ] Security headers configured
- [ ] CORS properly configured
- [ ] Error messages don't expose system details

**Dependencies:**
- [ ] Dependencies scanned for vulnerabilities
- [ ] Outdated dependencies updated
- [ ] Unused dependencies removed

**Logging & Monitoring:**
- [ ] Security events logged (failed logins, access attempts)
- [ ] Logs don't contain sensitive information
- [ ] Monitoring and alerting in place

**General:**
- [ ] Principle of least privilege applied
- [ ] Defense in depth implemented
- [ ] Fail securely (don't reveal information on errors)
- [ ] Security testing performed (penetration testing, code scanning)

---

[← Back to Index](00-README.md) | [Previous: Testing](17-Testing.md) | [Next: Java 21 Features →](19-Java21-Features.md)
