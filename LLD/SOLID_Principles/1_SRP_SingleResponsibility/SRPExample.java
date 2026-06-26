/**
 * ============================================================================
 *                 SINGLE RESPONSIBILITY PRINCIPLE (SRP)
 * ============================================================================
 * 
 * WHAT IT MEANS (Simple Words):
 * -----------------------------
 * A class should have ONLY ONE reason to change.
 * In other words, a class should do ONE thing and do it well.
 * 
 * Think of it like a restaurant:
 * - The chef ONLY cooks food
 * - The waiter ONLY serves customers
 * - The cashier ONLY handles payments
 * Each person has ONE job. They don't mix responsibilities.
 * 
 * WHY IT'S IMPORTANT:
 * -------------------
 * 1. Easier to understand - Each class has a clear purpose
 * 2. Easier to maintain - Changes in one area don't affect others
 * 3. Easier to test - You can test each responsibility separately
 * 4. Reduces bugs - Less chance of breaking unrelated features
 * 
 * REAL-WORLD ANALOGY:
 * -------------------
 * Imagine an Employee who is also the Accountant, HR Manager, and IT Support.
 * If you need to change the accounting rules, you might accidentally affect
 * how HR works! It's better to have separate people for each role.
 * 
 * ============================================================================
 */

// ============================================================================
// BAD EXAMPLE (VIOLATES SRP) - DON'T DO THIS
// ============================================================================
// This class does TOO MANY things:
// 1. Stores employee data
// 2. Calculates salary
// 3. Saves to database
// 4. Generates reports

/*
class BadEmployee {
    private String name;
    private double baseSalary;
    
    // Responsibility 1: Store data (OK)
    public BadEmployee(String name, double baseSalary) {
        this.name = name;
        this.baseSalary = baseSalary;
    }
    
    // Responsibility 2: Calculate salary (Should be separate!)
    public double calculateSalary() {
        return baseSalary + (baseSalary * 0.1); // 10% bonus
    }
    
    // Responsibility 3: Database operations (Should be separate!)
    public void saveToDatabase() {
        System.out.println("Saving " + name + " to database...");
    }
    
    // Responsibility 4: Report generation (Should be separate!)
    public String generateReport() {
        return "Report for " + name + ": Salary = " + calculateSalary();
    }
}
// Problem: If you change how database works, you modify the Employee class!
// Problem: If you change report format, you modify the Employee class!
*/

// ============================================================================
// GOOD EXAMPLE (FOLLOWS SRP) - DO THIS
// ============================================================================

/**
 * CLASS 1: Employee
 * -----------------
 * SINGLE RESPONSIBILITY: Only stores and manages employee data.
 * This class knows nothing about databases, reports, or salary calculations.
 */
class Employee {
    // Private fields - only this class can directly access them
    private String name;
    private String employeeId;
    private double baseSalary;
    
    /**
     * Constructor - Creates a new employee with basic information
     * @param name - Employee's full name
     * @param employeeId - Unique identifier for the employee
     * @param baseSalary - Base salary before any calculations
     */
    public Employee(String name, String employeeId, double baseSalary) {
        this.name = name;
        this.employeeId = employeeId;
        this.baseSalary = baseSalary;
    }
    
    // GETTERS: Allow other classes to read the data
    public String getName() {
        return name;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public double getBaseSalary() {
        return baseSalary;
    }
    
    // SETTER: Allow updating the salary
    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
    }
}

/**
 * CLASS 2: SalaryCalculator
 * -------------------------
 * SINGLE RESPONSIBILITY: Only calculates salary-related amounts.
 * This class knows nothing about databases or reports.
 */
class SalaryCalculator {
    
    private static final double BONUS_PERCENTAGE = 0.10;  // 10% bonus
    private static final double TAX_PERCENTAGE = 0.20;    // 20% tax
    
    /**
     * Calculates the total salary including bonus
     * @param employee - The employee whose salary we're calculating
     * @return Total salary with bonus
     */
    public double calculateTotalSalary(Employee employee) {
        double baseSalary = employee.getBaseSalary();
        double bonus = baseSalary * BONUS_PERCENTAGE;
        return baseSalary + bonus;
    }
    
    /**
     * Calculates the tax amount
     * @param employee - The employee whose tax we're calculating
     * @return Tax amount to be deducted
     */
    public double calculateTax(Employee employee) {
        double totalSalary = calculateTotalSalary(employee);
        return totalSalary * TAX_PERCENTAGE;
    }
    
    /**
     * Calculates the final take-home salary after tax
     * @param employee - The employee
     * @return Net salary after tax deduction
     */
    public double calculateNetSalary(Employee employee) {
        double totalSalary = calculateTotalSalary(employee);
        double tax = calculateTax(employee);
        return totalSalary - tax;
    }
}

/**
 * CLASS 3: EmployeeRepository
 * ---------------------------
 * SINGLE RESPONSIBILITY: Only handles database operations (save, load, delete).
 * This class knows nothing about salary calculations or reports.
 * 
 * Note: This is a simplified simulation. In real apps, this would connect
 * to an actual database like MySQL, PostgreSQL, or MongoDB.
 */
class EmployeeRepository {
    
    /**
     * Saves an employee to the database
     * @param employee - The employee to save
     */
    public void save(Employee employee) {
        // In real application, this would execute SQL INSERT/UPDATE
        System.out.println("[DATABASE] Saving employee: " + employee.getName() + 
                          " (ID: " + employee.getEmployeeId() + ")");
        System.out.println("[DATABASE] Save successful!");
    }
    
    /**
     * Finds an employee by their ID
     * @param employeeId - The ID to search for
     * @return The found employee (simulated)
     */
    public Employee findById(String employeeId) {
        // In real application, this would execute SQL SELECT
        System.out.println("[DATABASE] Searching for employee ID: " + employeeId);
        // Returning a dummy employee for demonstration
        return new Employee("John Doe", employeeId, 50000);
    }
    
    /**
     * Deletes an employee from the database
     * @param employeeId - The ID of employee to delete
     */
    public void delete(String employeeId) {
        // In real application, this would execute SQL DELETE
        System.out.println("[DATABASE] Deleting employee ID: " + employeeId);
        System.out.println("[DATABASE] Delete successful!");
    }
}

/**
 * CLASS 4: EmployeeReportGenerator
 * --------------------------------
 * SINGLE RESPONSIBILITY: Only generates reports in various formats.
 * This class knows nothing about databases or salary calculations logic.
 * It uses SalaryCalculator to get the numbers it needs.
 */
class EmployeeReportGenerator {
    
    // We use SalaryCalculator to get salary info - this is composition
    private SalaryCalculator salaryCalculator;
    
    /**
     * Constructor - needs a salary calculator to get salary information
     */
    public EmployeeReportGenerator(SalaryCalculator salaryCalculator) {
        this.salaryCalculator = salaryCalculator;
    }
    
    /**
     * Generates a simple text report for an employee
     * @param employee - The employee to report on
     * @return Formatted text report
     */
    public String generateTextReport(Employee employee) {
        StringBuilder report = new StringBuilder();
        report.append("========================================\n");
        report.append("         EMPLOYEE SALARY REPORT         \n");
        report.append("========================================\n");
        report.append("Name:        " + employee.getName() + "\n");
        report.append("Employee ID: " + employee.getEmployeeId() + "\n");
        report.append("----------------------------------------\n");
        report.append("Base Salary: $" + String.format("%.2f", employee.getBaseSalary()) + "\n");
        report.append("Total (with bonus): $" + String.format("%.2f", salaryCalculator.calculateTotalSalary(employee)) + "\n");
        report.append("Tax Deduction: $" + String.format("%.2f", salaryCalculator.calculateTax(employee)) + "\n");
        report.append("Net Salary:  $" + String.format("%.2f", salaryCalculator.calculateNetSalary(employee)) + "\n");
        report.append("========================================\n");
        return report.toString();
    }
    
    /**
     * Generates a simple one-line summary
     * @param employee - The employee to summarize
     * @return One-line summary
     */
    public String generateSummary(Employee employee) {
        return employee.getName() + " - Net Salary: $" + 
               String.format("%.2f", salaryCalculator.calculateNetSalary(employee));
    }
}

// ============================================================================
// MAIN CLASS - Demonstrates how all the classes work together
// ============================================================================

/**
 * Main class to run the SRP demonstration
 */
public class SRPExample {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   SINGLE RESPONSIBILITY PRINCIPLE (SRP) DEMONSTRATION      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // STEP 1: Create an Employee (using Employee class)
        // The Employee class ONLY handles employee data - nothing else
        System.out.println("STEP 1: Creating an employee...");
        Employee alice = new Employee("Alice Johnson", "EMP001", 60000);
        System.out.println("Created: " + alice.getName() + " with base salary $" + alice.getBaseSalary());
        System.out.println();
        
        // STEP 2: Calculate salary (using SalaryCalculator class)
        // The SalaryCalculator class ONLY handles salary math - nothing else
        System.out.println("STEP 2: Calculating salary...");
        SalaryCalculator calculator = new SalaryCalculator();
        System.out.println("Total Salary (with 10% bonus): $" + calculator.calculateTotalSalary(alice));
        System.out.println("Tax (20%): $" + calculator.calculateTax(alice));
        System.out.println("Net Salary: $" + calculator.calculateNetSalary(alice));
        System.out.println();
        
        // STEP 3: Save to database (using EmployeeRepository class)
        // The EmployeeRepository class ONLY handles database operations - nothing else
        System.out.println("STEP 3: Saving to database...");
        EmployeeRepository repository = new EmployeeRepository();
        repository.save(alice);
        System.out.println();
        
        // STEP 4: Generate report (using EmployeeReportGenerator class)
        // The ReportGenerator class ONLY handles report creation - nothing else
        System.out.println("STEP 4: Generating report...");
        EmployeeReportGenerator reportGenerator = new EmployeeReportGenerator(calculator);
        System.out.println(reportGenerator.generateTextReport(alice));
        
        // STEP 5: Show summary
        System.out.println("STEP 5: Quick summary...");
        System.out.println(reportGenerator.generateSummary(alice));
        
        // =====================================================================
        // WHY THIS IS BETTER (SRP Benefits Demonstrated)
        // =====================================================================
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    WHY SRP IS BETTER                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Each class has ONE job:");
        System.out.println("  • Employee            → Stores employee data");
        System.out.println("  • SalaryCalculator    → Calculates salaries");
        System.out.println("  • EmployeeRepository  → Database operations");
        System.out.println("  • ReportGenerator     → Creates reports");
        System.out.println();
        System.out.println("Benefits:");
        System.out.println("  ✓ Change tax rules? Only modify SalaryCalculator");
        System.out.println("  ✓ Change database? Only modify EmployeeRepository");
        System.out.println("  ✓ Change report format? Only modify ReportGenerator");
        System.out.println("  ✓ Each class can be tested independently");
        System.out.println("  ✓ Easy to understand what each class does");
    }
}

/*
 * ============================================================================
 *                         EXPECTED OUTPUT
 * ============================================================================
 * 
 * ╔════════════════════════════════════════════════════════════╗
 * ║   SINGLE RESPONSIBILITY PRINCIPLE (SRP) DEMONSTRATION      ║
 * ╚════════════════════════════════════════════════════════════╝
 * 
 * STEP 1: Creating an employee...
 * Created: Alice Johnson with base salary $60000.0
 * 
 * STEP 2: Calculating salary...
 * Total Salary (with 10% bonus): $66000.0
 * Tax (20%): $13200.0
 * Net Salary: $52800.0
 * 
 * STEP 3: Saving to database...
 * [DATABASE] Saving employee: Alice Johnson (ID: EMP001)
 * [DATABASE] Save successful!
 * 
 * STEP 4: Generating report...
 * ========================================
 *          EMPLOYEE SALARY REPORT         
 * ========================================
 * Name:        Alice Johnson
 * Employee ID: EMP001
 * ----------------------------------------
 * Base Salary: $60000.00
 * Total (with bonus): $66000.00
 * Tax Deduction: $13200.00
 * Net Salary:  $52800.00
 * ========================================
 * 
 * STEP 5: Quick summary...
 * Alice Johnson - Net Salary: $52800.00
 * 
 * ╔════════════════════════════════════════════════════════════╗
 * ║                    WHY SRP IS BETTER                       ║
 * ╚════════════════════════════════════════════════════════════╝
 * 
 * Each class has ONE job:
 *   • Employee            → Stores employee data
 *   • SalaryCalculator    → Calculates salaries
 *   • EmployeeRepository  → Database operations
 *   • ReportGenerator     → Creates reports
 * 
 * Benefits:
 *   ✓ Change tax rules? Only modify SalaryCalculator
 *   ✓ Change database? Only modify EmployeeRepository
 *   ✓ Change report format? Only modify ReportGenerator
 *   ✓ Each class can be tested independently
 *   ✓ Easy to understand what each class does
 * 
 * ============================================================================
 * 
 * HOW TO COMPILE AND RUN:
 * -----------------------
 * 1. Save this file as SRPExample.java
 * 2. Open terminal/command prompt
 * 3. Navigate to the folder containing the file
 * 4. Compile: javac SRPExample.java
 * 5. Run: java SRPExample
 * 
 * ============================================================================
 */

