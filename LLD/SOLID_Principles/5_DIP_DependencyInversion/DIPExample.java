/**
 * ============================================================================
 *              DEPENDENCY INVERSION PRINCIPLE (DIP)
 * ============================================================================
 * 
 * WHAT IT MEANS (Simple Words):
 * -----------------------------
 * 1. High-level modules should NOT depend on low-level modules.
 *    Both should depend on ABSTRACTIONS (interfaces).
 * 
 * 2. Abstractions should NOT depend on details.
 *    Details should depend on abstractions.
 * 
 * In even simpler terms:
 * Don't let your main business logic depend directly on specific implementations.
 * Instead, depend on INTERFACES, and inject the specific implementations.
 * 
 * REAL-WORLD ANALOGY:
 * -------------------
 * Think about a LAPTOP and its CHARGER:
 * 
 * BAD Design (Hard-wired):
 * - The laptop has a specific charger built into it
 * - If the charger breaks, you need a new laptop!
 * - You can't use any other charger
 * 
 * GOOD Design (Using abstraction - the power port):
 * - The laptop has a charging PORT (abstraction)
 * - Any compatible charger can plug into the port
 * - Easy to replace, upgrade, or use different chargers
 * 
 * Another analogy - Wall Outlets:
 * - Wall outlet is an INTERFACE (abstraction)
 * - Any device with a compatible plug works
 * - The house (high-level) doesn't care if it's a lamp, TV, or toaster (low-level)
 * 
 * WHY IT'S IMPORTANT:
 * -------------------
 * 1. Flexibility - Easy to swap implementations
 * 2. Testability - Can use mock/fake implementations for testing
 * 3. Loose coupling - Components are independent
 * 4. Maintainability - Changes in one area don't break others
 * 
 * ============================================================================
 */

// ============================================================================
// BAD EXAMPLE (VIOLATES DIP) - DON'T DO THIS
// ============================================================================
// High-level class directly depends on low-level class

/*
// Low-level class - specific email implementation
class BadEmailService {
    public void sendEmail(String to, String subject, String body) {
        System.out.println("Sending email to " + to);
    }
}

// High-level class - depends DIRECTLY on the low-level class
class BadOrderService {
    // PROBLEM: Direct dependency on EmailService
    private BadEmailService emailService = new BadEmailService();
    
    public void placeOrder(String customerEmail) {
        // ... order logic ...
        
        // PROBLEM: Can't easily switch to SMS, push notifications, etc.
        // PROBLEM: Can't mock this for testing
        emailService.sendEmail(customerEmail, "Order Placed", "Your order was placed!");
    }
}

// Problems:
// 1. What if we want to use SMS instead of Email? We must modify OrderService!
// 2. What if we want to use both Email AND SMS? Major changes needed!
// 3. How do we test OrderService without actually sending emails?
*/

// ============================================================================
// GOOD EXAMPLE (FOLLOWS DIP) - DO THIS
// ============================================================================

/**
 * STEP 1: Define an ABSTRACTION (Interface)
 * ------------------------------------------
 * This interface defines WHAT a notification service should do,
 * but not HOW it does it.
 * 
 * High-level modules will depend on this interface,
 * not on specific implementations.
 */
interface NotificationService {
    
    /**
     * Send a notification to a recipient
     * @param recipient - Who to send to (email, phone, device ID, etc.)
     * @param subject - Subject/title of the notification
     * @param message - The message body
     * @return true if sent successfully
     */
    boolean sendNotification(String recipient, String subject, String message);
    
    /**
     * Get the type of this notification service
     * @return The service type name
     */
    String getServiceType();
}

/**
 * STEP 2: Create LOW-LEVEL implementations
 * -----------------------------------------
 * Each implementation knows HOW to send a specific type of notification.
 * They all implement the same interface.
 */

/**
 * Email notification implementation
 */
class EmailNotificationService implements NotificationService {
    
    private String smtpServer;
    private String senderEmail;
    
    public EmailNotificationService(String smtpServer, String senderEmail) {
        this.smtpServer = smtpServer;
        this.senderEmail = senderEmail;
    }
    
    @Override
    public boolean sendNotification(String recipient, String subject, String message) {
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║          📧 EMAIL SENT               ║");
        System.out.println("  ╠══════════════════════════════════════╣");
        System.out.println("  ║ Server: " + String.format("%-24s", smtpServer) + "║");
        System.out.println("  ║ From: " + String.format("%-26s", senderEmail) + "║");
        System.out.println("  ║ To: " + String.format("%-28s", recipient) + "║");
        System.out.println("  ║ Subject: " + String.format("%-23s", truncate(subject, 20)) + "║");
        System.out.println("  ╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getServiceType() {
        return "Email";
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}

/**
 * SMS notification implementation
 */
class SmsNotificationService implements NotificationService {
    
    private String smsGateway;
    private String senderId;
    
    public SmsNotificationService(String smsGateway, String senderId) {
        this.smsGateway = smsGateway;
        this.senderId = senderId;
    }
    
    @Override
    public boolean sendNotification(String recipient, String subject, String message) {
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║          📱 SMS SENT                 ║");
        System.out.println("  ╠══════════════════════════════════════╣");
        System.out.println("  ║ Gateway: " + String.format("%-23s", smsGateway) + "║");
        System.out.println("  ║ Sender ID: " + String.format("%-21s", senderId) + "║");
        System.out.println("  ║ To: " + String.format("%-28s", recipient) + "║");
        System.out.println("  ║ Message: " + String.format("%-23s", truncate(message, 20)) + "║");
        System.out.println("  ╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getServiceType() {
        return "SMS";
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}

/**
 * Push notification implementation
 */
class PushNotificationService implements NotificationService {
    
    private String firebaseKey;
    
    public PushNotificationService(String firebaseKey) {
        this.firebaseKey = firebaseKey;
    }
    
    @Override
    public boolean sendNotification(String recipient, String subject, String message) {
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║          🔔 PUSH NOTIFICATION        ║");
        System.out.println("  ╠══════════════════════════════════════╣");
        System.out.println("  ║ Firebase Key: " + String.format("%-18s", truncate(firebaseKey, 15)) + "║");
        System.out.println("  ║ Device: " + String.format("%-24s", truncate(recipient, 21)) + "║");
        System.out.println("  ║ Title: " + String.format("%-25s", truncate(subject, 22)) + "║");
        System.out.println("  ╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getServiceType() {
        return "Push Notification";
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}

/**
 * Slack notification implementation (for team notifications)
 */
class SlackNotificationService implements NotificationService {
    
    private String webhookUrl;
    private String channelName;
    
    public SlackNotificationService(String webhookUrl, String channelName) {
        this.webhookUrl = webhookUrl;
        this.channelName = channelName;
    }
    
    @Override
    public boolean sendNotification(String recipient, String subject, String message) {
        System.out.println("  ╔══════════════════════════════════════╗");
        System.out.println("  ║          💬 SLACK MESSAGE            ║");
        System.out.println("  ╠══════════════════════════════════════╣");
        System.out.println("  ║ Channel: " + String.format("%-23s", channelName) + "║");
        System.out.println("  ║ Mention: " + String.format("%-23s", "@" + recipient) + "║");
        System.out.println("  ║ Subject: " + String.format("%-23s", truncate(subject, 20)) + "║");
        System.out.println("  ╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getServiceType() {
        return "Slack";
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}

/**
 * STEP 3: Create a HIGH-LEVEL module that depends on the ABSTRACTION
 * -------------------------------------------------------------------
 * This is the business logic class. It handles orders.
 * 
 * NOTICE: It depends on NotificationService INTERFACE, not on
 * EmailService, SmsService, or any specific implementation!
 * 
 * This is DEPENDENCY INVERSION - the high-level class depends on
 * an abstraction, and the specific service is "injected" from outside.
 */
class OrderService {
    
    // Depends on the INTERFACE, not a concrete class!
    private NotificationService notificationService;
    
    /**
     * CONSTRUCTOR INJECTION (Dependency Injection)
     * ---------------------------------------------
     * The notification service is PASSED IN from outside.
     * This class doesn't create its own dependencies.
     * 
     * @param notificationService - Any implementation of NotificationService
     */
    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService;
        System.out.println("  [OrderService] Initialized with " + 
                          notificationService.getServiceType() + " notifications");
    }
    
    /**
     * Place an order and notify the customer
     * Works with ANY notification service!
     */
    public void placeOrder(String orderId, String customerContact, String productName) {
        System.out.println("\n  [OrderService] Processing order " + orderId + "...");
        System.out.println("  [OrderService] Product: " + productName);
        
        // Business logic for placing order
        System.out.println("  [OrderService] Validating order...");
        System.out.println("  [OrderService] Updating inventory...");
        System.out.println("  [OrderService] Order placed successfully!\n");
        
        // Notify customer - uses whatever service was injected!
        String subject = "Order Confirmed: " + orderId;
        String message = "Your order for " + productName + " has been placed!";
        
        notificationService.sendNotification(customerContact, subject, message);
    }
    
    /**
     * Cancel an order and notify the customer
     */
    public void cancelOrder(String orderId, String customerContact) {
        System.out.println("\n  [OrderService] Cancelling order " + orderId + "...");
        System.out.println("  [OrderService] Refund initiated...");
        System.out.println("  [OrderService] Order cancelled!\n");
        
        String subject = "Order Cancelled: " + orderId;
        String message = "Your order has been cancelled. Refund will be processed.";
        
        notificationService.sendNotification(customerContact, subject, message);
    }
}

/**
 * Another high-level module: User Registration
 * Also depends on the abstraction (NotificationService)
 */
class UserRegistrationService {
    
    private NotificationService notificationService;
    
    public UserRegistrationService(NotificationService notificationService) {
        this.notificationService = notificationService;
        System.out.println("  [UserRegistration] Initialized with " + 
                          notificationService.getServiceType() + " notifications");
    }
    
    /**
     * Register a new user and send welcome notification
     */
    public void registerUser(String username, String contact) {
        System.out.println("\n  [UserRegistration] Creating account for " + username + "...");
        System.out.println("  [UserRegistration] Setting up profile...");
        System.out.println("  [UserRegistration] Account created!\n");
        
        String subject = "Welcome to Our Platform!";
        String message = "Hi " + username + "! Your account is ready.";
        
        notificationService.sendNotification(contact, subject, message);
    }
}

/**
 * BONUS: Multi-Channel Notification Service
 * ------------------------------------------
 * This service can send to multiple channels at once.
 * It also implements NotificationService, so it can be used anywhere!
 */
class MultiChannelNotificationService implements NotificationService {
    
    private NotificationService[] services;
    
    public MultiChannelNotificationService(NotificationService... services) {
        this.services = services;
    }
    
    @Override
    public boolean sendNotification(String recipient, String subject, String message) {
        System.out.println("  [MultiChannel] Sending to all channels...\n");
        boolean allSuccess = true;
        for (NotificationService service : services) {
            boolean success = service.sendNotification(recipient, subject, message);
            if (!success) allSuccess = false;
            System.out.println();
        }
        return allSuccess;
    }
    
    @Override
    public String getServiceType() {
        return "Multi-Channel (" + services.length + " services)";
    }
}

// ============================================================================
// MAIN CLASS - Demonstrates DIP
// ============================================================================

/**
 * Main class to demonstrate Dependency Inversion Principle
 */
public class DIPExample {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║    DEPENDENCY INVERSION PRINCIPLE (DIP) DEMONSTRATION      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // =====================================================================
        // DEMONSTRATION 1: Using Email Notification
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("   SCENARIO 1: E-COMMERCE WITH EMAIL NOTIFICATIONS         ");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        // Create email notification service (low-level)
        NotificationService emailService = new EmailNotificationService(
            "smtp.gmail.com",
            "orders@myshop.com"
        );
        
        // Inject email service into order service (high-level)
        OrderService emailOrderService = new OrderService(emailService);
        
        // Place an order - notification goes via email
        emailOrderService.placeOrder("ORD-001", "customer@email.com", "Wireless Headphones");
        
        // =====================================================================
        // DEMONSTRATION 2: Using SMS Notification (Easy to swap!)
        // =====================================================================
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("   SCENARIO 2: SAME SERVICE, DIFFERENT NOTIFICATION (SMS)  ");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        // Create SMS notification service
        NotificationService smsService = new SmsNotificationService(
            "twilio.com",
            "MyShop"
        );
        
        // Inject SMS service - NO CHANGES to OrderService code!
        OrderService smsOrderService = new OrderService(smsService);
        
        // Place an order - notification goes via SMS
        smsOrderService.placeOrder("ORD-002", "+1-555-123-4567", "Smart Watch");
        
        // =====================================================================
        // DEMONSTRATION 3: Using Push Notification
        // =====================================================================
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("   SCENARIO 3: MOBILE APP WITH PUSH NOTIFICATIONS          ");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        NotificationService pushService = new PushNotificationService("FCM_SERVER_KEY_123");
        
        // User registration with push notifications
        UserRegistrationService mobileRegistration = new UserRegistrationService(pushService);
        mobileRegistration.registerUser("JohnDoe", "device_token_abc123");
        
        // =====================================================================
        // DEMONSTRATION 4: Using Slack for Internal Team
        // =====================================================================
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("   SCENARIO 4: INTERNAL TEAM NOTIFICATIONS (SLACK)         ");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        NotificationService slackService = new SlackNotificationService(
            "https://hooks.slack.com/...",
            "#orders"
        );
        
        OrderService internalOrderService = new OrderService(slackService);
        internalOrderService.cancelOrder("ORD-003", "support-team");
        
        // =====================================================================
        // DEMONSTRATION 5: Multi-Channel Notifications
        // =====================================================================
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("   SCENARIO 5: VIP CUSTOMERS - MULTI-CHANNEL               ");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        // Create a multi-channel service that sends to email AND SMS AND push
        NotificationService multiChannel = new MultiChannelNotificationService(
            new EmailNotificationService("smtp.premium.com", "vip@myshop.com"),
            new SmsNotificationService("premium-gateway.com", "VIPShop"),
            new PushNotificationService("FCM_VIP_KEY")
        );
        
        // VIP order service - sends notifications to ALL channels!
        OrderService vipOrderService = new OrderService(multiChannel);
        vipOrderService.placeOrder("VIP-001", "vip_customer", "Diamond Ring");
        
        // =====================================================================
        // SUMMARY
        // =====================================================================
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    WHY DIP IS POWERFUL                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("What we demonstrated:");
        System.out.println("  • Same OrderService class, 5 different notification methods");
        System.out.println("  • Zero changes to OrderService code when switching services");
        System.out.println("  • Easy to combine multiple services (Multi-Channel)");
        System.out.println();
        System.out.println("The Key Insight:");
        System.out.println("  • HIGH-LEVEL (OrderService) depends on ABSTRACTION (NotificationService)");
        System.out.println("  • LOW-LEVEL (Email, SMS, Push, Slack) implements ABSTRACTION");
        System.out.println("  • Dependencies are INJECTED from outside (Constructor Injection)");
        System.out.println();
        System.out.println("Benefits:");
        System.out.println("  ✓ Flexibility: Swap Email for SMS without changing business logic");
        System.out.println("  ✓ Testability: Inject mock service for unit testing");
        System.out.println("  ✓ Extensibility: Add WhatsApp/Telegram without modifying existing code");
        System.out.println("  ✓ Loose Coupling: OrderService doesn't know or care HOW notifications work");
        System.out.println();
        System.out.println("Without DIP (Bad):");
        System.out.println("  OrderService → EmailService (direct dependency, hard to change)");
        System.out.println();
        System.out.println("With DIP (Good):");
        System.out.println("  OrderService → NotificationService (interface)");
        System.out.println("                      ↑");
        System.out.println("  EmailService, SmsService, PushService, SlackService, etc.");
    }
}

/*
 * ============================================================================
 *                         EXPECTED OUTPUT
 * ============================================================================
 * 
 * ╔════════════════════════════════════════════════════════════╗
 * ║    DEPENDENCY INVERSION PRINCIPLE (DIP) DEMONSTRATION      ║
 * ╚════════════════════════════════════════════════════════════╝
 * 
 * ═══════════════════════════════════════════════════════════
 *    SCENARIO 1: E-COMMERCE WITH EMAIL NOTIFICATIONS         
 * ═══════════════════════════════════════════════════════════
 * 
 *   [OrderService] Initialized with Email notifications
 * 
 *   [OrderService] Processing order ORD-001...
 *   [OrderService] Product: Wireless Headphones
 *   [OrderService] Validating order...
 *   [OrderService] Updating inventory...
 *   [OrderService] Order placed successfully!
 * 
 *   ╔══════════════════════════════════════╗
 *   ║          📧 EMAIL SENT               ║
 *   ╠══════════════════════════════════════╣
 *   ║ Server: smtp.gmail.com               ║
 *   ║ From: orders@myshop.com              ║
 *   ║ To: customer@email.com               ║
 *   ║ Subject: Order Confirmed: O...       ║
 *   ╚══════════════════════════════════════╝
 * 
 * (... more scenarios ...)
 * 
 * ============================================================================
 * 
 * HOW TO COMPILE AND RUN:
 * -----------------------
 * 1. Save this file as DIPExample.java
 * 2. Open terminal/command prompt
 * 3. Navigate to the folder containing the file
 * 4. Compile: javac DIPExample.java
 * 5. Run: java DIPExample
 * 
 * ============================================================================
 */

