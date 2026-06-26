/**
 * ============================================================================
 *                    OPEN/CLOSED PRINCIPLE (OCP)
 * ============================================================================
 * 
 * WHAT IT MEANS (Simple Words):
 * -----------------------------
 * Software entities (classes, modules, functions) should be:
 *   - OPEN for extension (you can add new behavior)
 *   - CLOSED for modification (you don't change existing code)
 * 
 * In simpler terms: You should be able to add new features WITHOUT
 * changing the code that already works.
 * 
 * REAL-WORLD ANALOGY:
 * -------------------
 * Think of a power strip (extension board):
 * - You can plug in NEW devices (laptop, phone, lamp) - it's OPEN for extension
 * - You don't need to rewire the power strip each time - it's CLOSED for modification
 * 
 * Or think of a game console:
 * - You can add new games (extension) without modifying the console hardware
 * 
 * WHY IT'S IMPORTANT:
 * -------------------
 * 1. Reduces risk - Existing tested code stays unchanged
 * 2. Easier maintenance - New features don't break old ones
 * 3. More flexible - Easy to add new functionality
 * 4. Better testing - Only test the new code, old tests still pass
 * 
 * HOW TO ACHIEVE IT:
 * ------------------
 * Use ABSTRACTION (interfaces/abstract classes) and POLYMORPHISM.
 * Instead of if-else or switch statements that check types,
 * create an interface and let each type implement its own behavior.
 * 
 * ============================================================================
 */

// ============================================================================
// BAD EXAMPLE (VIOLATES OCP) - DON'T DO THIS
// ============================================================================
// Every time you add a new payment method, you must MODIFY this class.
// This is DANGEROUS because you might break existing payment logic!

/*
class BadPaymentProcessor {
    
    public void processPayment(String paymentType, double amount) {
        // Every new payment type requires modifying this method!
        // This violates OCP - not closed for modification
        
        if (paymentType.equals("CREDIT_CARD")) {
            System.out.println("Processing credit card payment of $" + amount);
            // Credit card logic...
        } 
        else if (paymentType.equals("PAYPAL")) {
            System.out.println("Processing PayPal payment of $" + amount);
            // PayPal logic...
        }
        else if (paymentType.equals("BITCOIN")) {
            // PROBLEM: To add Bitcoin, we had to MODIFY existing code!
            System.out.println("Processing Bitcoin payment of $" + amount);
            // Bitcoin logic...
        }
        // What if we need to add Apple Pay, Google Pay, Bank Transfer?
        // We keep modifying this class - RISKY!
    }
}
*/

// ============================================================================
// GOOD EXAMPLE (FOLLOWS OCP) - DO THIS
// ============================================================================

/**
 * STEP 1: Create an INTERFACE (Abstraction)
 * ------------------------------------------
 * This interface defines WHAT a payment method should do.
 * Any new payment type just needs to implement this interface.
 * The processor doesn't need to know about specific payment types.
 */
interface PaymentMethod {
    
    /**
     * Process a payment of the given amount
     * @param amount - The amount to charge
     * @return true if payment successful, false otherwise
     */
    boolean pay(double amount);
    
    /**
     * Get the name of this payment method
     * @return Payment method name
     */
    String getName();
}

/**
 * STEP 2: Implement the interface for CREDIT CARD
 * ------------------------------------------------
 * This class handles all credit card specific logic.
 */
class CreditCardPayment implements PaymentMethod {
    
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    
    /**
     * Constructor for credit card payment
     * @param cardNumber - The credit card number
     * @param cardHolderName - Name on the card
     * @param expiryDate - Expiry date (MM/YY)
     */
    public CreditCardPayment(String cardNumber, String cardHolderName, String expiryDate) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expiryDate = expiryDate;
    }
    
    @Override
    public boolean pay(double amount) {
        // Credit card specific logic
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        CREDIT CARD PAYMENT           ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║ Card: " + maskCardNumber() + "            ║");
        System.out.println("║ Name: " + String.format("%-24s", cardHolderName) + " ║");
        System.out.println("║ Amount: $" + String.format("%-22.2f", amount) + "║");
        System.out.println("║ Status: ✓ APPROVED                   ║");
        System.out.println("╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getName() {
        return "Credit Card";
    }
    
    // Helper method to mask card number for security
    private String maskCardNumber() {
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}

/**
 * STEP 2: Implement the interface for PAYPAL
 * -------------------------------------------
 * This class handles all PayPal specific logic.
 */
class PayPalPayment implements PaymentMethod {
    
    private String email;
    
    /**
     * Constructor for PayPal payment
     * @param email - PayPal account email
     */
    public PayPalPayment(String email) {
        this.email = email;
    }
    
    @Override
    public boolean pay(double amount) {
        // PayPal specific logic
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║          PAYPAL PAYMENT              ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║ Email: " + String.format("%-24s", email) + "║");
        System.out.println("║ Amount: $" + String.format("%-22.2f", amount) + "║");
        System.out.println("║ Status: ✓ COMPLETED                  ║");
        System.out.println("╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getName() {
        return "PayPal";
    }
}

/**
 * STEP 2: Implement the interface for CRYPTOCURRENCY
 * ---------------------------------------------------
 * NOTICE: We added this new payment type WITHOUT modifying
 * CreditCardPayment, PayPalPayment, or PaymentProcessor!
 * This is the power of OCP - OPEN for extension!
 */
class CryptoPayment implements PaymentMethod {
    
    private String walletAddress;
    private String cryptoType; // BTC, ETH, etc.
    
    /**
     * Constructor for cryptocurrency payment
     * @param walletAddress - The crypto wallet address
     * @param cryptoType - Type of cryptocurrency (BTC, ETH, etc.)
     */
    public CryptoPayment(String walletAddress, String cryptoType) {
        this.walletAddress = walletAddress;
        this.cryptoType = cryptoType;
    }
    
    @Override
    public boolean pay(double amount) {
        // Cryptocurrency specific logic
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       CRYPTOCURRENCY PAYMENT         ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║ Crypto: " + String.format("%-23s", cryptoType) + "║");
        System.out.println("║ Wallet: " + String.format("%-23s", walletAddress.substring(0, 10) + "...") + "║");
        System.out.println("║ Amount: $" + String.format("%-22.2f", amount) + "║");
        System.out.println("║ Status: ✓ CONFIRMED (3 blocks)       ║");
        System.out.println("╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getName() {
        return cryptoType + " Cryptocurrency";
    }
}

/**
 * NEW PAYMENT TYPE: Bank Transfer
 * --------------------------------
 * Adding another payment method to show how easy it is!
 * We just create a new class - NO changes to existing code!
 */
class BankTransferPayment implements PaymentMethod {
    
    private String bankName;
    private String accountNumber;
    
    public BankTransferPayment(String bankName, String accountNumber) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
    }
    
    @Override
    public boolean pay(double amount) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        BANK TRANSFER PAYMENT         ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║ Bank: " + String.format("%-25s", bankName) + "║");
        System.out.println("║ Account: ****" + String.format("%-19s", accountNumber.substring(accountNumber.length() - 4)) + "║");
        System.out.println("║ Amount: $" + String.format("%-22.2f", amount) + "║");
        System.out.println("║ Status: ✓ TRANSFERRED                ║");
        System.out.println("╚══════════════════════════════════════╝");
        return true;
    }
    
    @Override
    public String getName() {
        return "Bank Transfer";
    }
}

/**
 * STEP 3: The Payment Processor (CLOSED for modification)
 * --------------------------------------------------------
 * This class processes ANY payment method.
 * 
 * NOTICE: This class NEVER needs to change when we add new payment types!
 * It works with the PaymentMethod interface, not specific implementations.
 * This is CLOSED for modification.
 */
class PaymentProcessor {
    
    /**
     * Process a payment using any payment method
     * @param paymentMethod - Any class that implements PaymentMethod
     * @param amount - The amount to charge
     * @return true if payment successful
     */
    public boolean processPayment(PaymentMethod paymentMethod, double amount) {
        System.out.println("\n>>> Processing " + paymentMethod.getName() + " payment...\n");
        
        // Validate amount
        if (amount <= 0) {
            System.out.println("ERROR: Invalid amount!");
            return false;
        }
        
        // Process the payment - works with ANY payment type!
        boolean success = paymentMethod.pay(amount);
        
        if (success) {
            System.out.println("\n✓ Payment of $" + String.format("%.2f", amount) + 
                             " via " + paymentMethod.getName() + " completed!\n");
        }
        
        return success;
    }
}

// ============================================================================
// MAIN CLASS - Demonstrates how OCP works
// ============================================================================

/**
 * Main class to demonstrate the Open/Closed Principle
 */
public class OCPExample {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     OPEN/CLOSED PRINCIPLE (OCP) DEMONSTRATION              ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Create the payment processor - this NEVER needs to change!
        PaymentProcessor processor = new PaymentProcessor();
        
        // =====================================================================
        // DEMONSTRATION 1: Credit Card Payment
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("                     PAYMENT 1: Credit Card                 ");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        PaymentMethod creditCard = new CreditCardPayment(
            "4532015112830366",  // Card number
            "John Smith",        // Card holder
            "12/25"              // Expiry
        );
        processor.processPayment(creditCard, 99.99);
        
        // =====================================================================
        // DEMONSTRATION 2: PayPal Payment
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("                     PAYMENT 2: PayPal                      ");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        PaymentMethod paypal = new PayPalPayment("john.smith@email.com");
        processor.processPayment(paypal, 49.99);
        
        // =====================================================================
        // DEMONSTRATION 3: Cryptocurrency Payment (NEW - added without modifying processor!)
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("                 PAYMENT 3: Cryptocurrency                  ");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        PaymentMethod crypto = new CryptoPayment(
            "0x742d35Cc6634C0532925a3b844Bc9e7595f",  // Wallet address
            "Ethereum (ETH)"                           // Crypto type
        );
        processor.processPayment(crypto, 150.00);
        
        // =====================================================================
        // DEMONSTRATION 4: Bank Transfer (ANOTHER NEW - still no changes to processor!)
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("                  PAYMENT 4: Bank Transfer                  ");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        PaymentMethod bankTransfer = new BankTransferPayment(
            "Chase Bank",
            "1234567890"
        );
        processor.processPayment(bankTransfer, 500.00);
        
        // =====================================================================
        // SUMMARY: Why OCP is powerful
        // =====================================================================
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    WHY OCP IS POWERFUL                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("We processed 4 different payment types!");
        System.out.println();
        System.out.println("Key Points:");
        System.out.println("  • PaymentProcessor class was NEVER modified");
        System.out.println("  • Each payment type is a separate class");
        System.out.println("  • To add Apple Pay, just create: class ApplePayPayment implements PaymentMethod");
        System.out.println();
        System.out.println("Benefits:");
        System.out.println("  ✓ OPEN for extension   - Easy to add new payment types");
        System.out.println("  ✓ CLOSED for modification - Existing code stays untouched");
        System.out.println("  ✓ No risk of breaking existing payments when adding new ones");
        System.out.println("  ✓ Each payment type can be developed and tested independently");
        System.out.println();
        System.out.println("How it works:");
        System.out.println("  1. Define an interface (PaymentMethod)");
        System.out.println("  2. Write code that uses the interface (PaymentProcessor)");
        System.out.println("  3. Add new features by creating new implementations");
        System.out.println("  4. Never touch the existing code!");
    }
}

/*
 * ============================================================================
 *                         EXPECTED OUTPUT
 * ============================================================================
 * 
 * ╔════════════════════════════════════════════════════════════╗
 * ║     OPEN/CLOSED PRINCIPLE (OCP) DEMONSTRATION              ║
 * ╚════════════════════════════════════════════════════════════╝
 * 
 * ═══════════════════════════════════════════════════════════
 *                      PAYMENT 1: Credit Card                 
 * ═══════════════════════════════════════════════════════════
 * 
 * >>> Processing Credit Card payment...
 * 
 * ╔══════════════════════════════════════╗
 * ║        CREDIT CARD PAYMENT           ║
 * ╠══════════════════════════════════════╣
 * ║ Card: ****-****-****-0366            ║
 * ║ Name: John Smith                     ║
 * ║ Amount: $99.99                       ║
 * ║ Status: ✓ APPROVED                   ║
 * ╚══════════════════════════════════════╝
 * 
 * ✓ Payment of $99.99 via Credit Card completed!
 * 
 * (... more payments ...)
 * 
 * ============================================================================
 * 
 * HOW TO COMPILE AND RUN:
 * -----------------------
 * 1. Save this file as OCPExample.java
 * 2. Open terminal/command prompt
 * 3. Navigate to the folder containing the file
 * 4. Compile: javac OCPExample.java
 * 5. Run: java OCPExample
 * 
 * ============================================================================
 */

