/**
 * ============================================================================
 *              INTERFACE SEGREGATION PRINCIPLE (ISP)
 * ============================================================================
 * 
 * WHAT IT MEANS (Simple Words):
 * -----------------------------
 * Clients should NOT be forced to depend on interfaces they don't use.
 * 
 * In simpler terms:
 * Don't create "fat" interfaces with too many methods.
 * Split them into smaller, more specific interfaces.
 * 
 * REAL-WORLD ANALOGY:
 * -------------------
 * Imagine a restaurant menu that forces you to order:
 * - An appetizer, main course, dessert, AND drinks
 * - Even if you only want coffee!
 * 
 * BAD: One giant menu where you must "implement" all courses
 * GOOD: Separate menus for drinks, appetizers, main courses, desserts
 *       You only use what you need!
 * 
 * Another analogy - Swiss Army Knife vs Individual Tools:
 * - Swiss Army Knife: Has scissors, knife, screwdriver, bottle opener all-in-one
 *   For simple tasks (opening a bottle), you carry unnecessary tools
 * - Individual Tools: Pick only the tool you need
 *   More focused, less baggage
 * 
 * WHY IT'S IMPORTANT:
 * -------------------
 * 1. Cleaner code - Classes only implement what they need
 * 2. Less coupling - Changes to unused methods don't affect you
 * 3. Easier maintenance - Smaller interfaces are easier to understand
 * 4. Better testing - Test only what's relevant
 * 
 * ============================================================================
 */

// ============================================================================
// BAD EXAMPLE (VIOLATES ISP) - DON'T DO THIS
// ============================================================================
// One giant interface that forces all workers to implement everything

/*
// This is a "FAT" interface - too many responsibilities!
interface BadWorker {
    void work();
    void eat();
    void sleep();
    void code();
    void attendMeeting();
    void designSystem();
    void manageTeam();
    void calculateSalary();
}

// Human worker - can do most things, but not calculateSalary (that's HR's job)
class BadHumanWorker implements BadWorker {
    public void work() { System.out.println("Working..."); }
    public void eat() { System.out.println("Eating..."); }
    public void sleep() { System.out.println("Sleeping..."); }
    public void code() { System.out.println("Coding..."); }
    public void attendMeeting() { System.out.println("In meeting..."); }
    public void designSystem() { System.out.println("Designing..."); }
    public void manageTeam() { System.out.println("Managing..."); }
    
    // PROBLEM: This worker doesn't calculate salary, but MUST implement it!
    public void calculateSalary() {
        throw new UnsupportedOperationException("Not my job!");
    }
}

// Robot worker - can work and code, but can't eat, sleep, or manage!
class BadRobotWorker implements BadWorker {
    public void work() { System.out.println("Robot working..."); }
    public void code() { System.out.println("Robot coding..."); }
    
    // FORCED to implement methods that don't make sense for a robot!
    public void eat() {
        throw new UnsupportedOperationException("Robots don't eat!");
    }
    public void sleep() {
        throw new UnsupportedOperationException("Robots don't sleep!");
    }
    public void attendMeeting() {
        throw new UnsupportedOperationException("Robots skip meetings!");
    }
    public void designSystem() {
        throw new UnsupportedOperationException("Not implemented!");
    }
    public void manageTeam() {
        throw new UnsupportedOperationException("Robots don't manage!");
    }
    public void calculateSalary() {
        throw new UnsupportedOperationException("Not applicable!");
    }
}
*/

// ============================================================================
// GOOD EXAMPLE (FOLLOWS ISP) - DO THIS
// ============================================================================

/**
 * STEP 1: Split the fat interface into smaller, focused interfaces
 * -----------------------------------------------------------------
 * Each interface represents a specific CAPABILITY.
 * Classes only implement the interfaces that make sense for them.
 */

/**
 * Interface for entities that can work
 */
interface Workable {
    void work();
}

/**
 * Interface for entities that need to eat
 */
interface Eatable {
    void eat();
    void takeBreak();
}

/**
 * Interface for entities that need to sleep
 */
interface Sleepable {
    void sleep();
    void rest();
}

/**
 * Interface for entities that can code/program
 */
interface Codeable {
    void writeCode();
    void reviewCode();
    void debugCode();
}

/**
 * Interface for entities that can attend meetings
 */
interface Meetable {
    void attendMeeting();
    void scheduleMeeting();
}

/**
 * Interface for entities that can manage teams
 */
interface Manageable {
    void manageTeam();
    void assignTasks();
    void evaluatePerformance();
}

/**
 * Interface for entities that can design systems
 */
interface Designable {
    void designSystem();
    void createDiagrams();
    void documentArchitecture();
}

/**
 * Interface for entities that can be maintained (machines)
 */
interface Maintainable {
    void performMaintenance();
    void recharge();
    void updateSoftware();
}

// ============================================================================
// STEP 2: Implement classes that only use the interfaces they need
// ============================================================================

/**
 * SOFTWARE DEVELOPER
 * ------------------
 * Implements: Workable, Eatable, Sleepable, Codeable, Meetable
 * Does NOT implement: Manageable (not a manager), Maintainable (not a robot)
 */
class SoftwareDeveloper implements Workable, Eatable, Sleepable, Codeable, Meetable {
    
    private String name;
    
    public SoftwareDeveloper(String name) {
        this.name = name;
    }
    
    // Workable
    @Override
    public void work() {
        System.out.println("  " + name + " starts working on assigned tasks.");
    }
    
    // Eatable
    @Override
    public void eat() {
        System.out.println("  " + name + " is having lunch at the cafeteria.");
    }
    
    @Override
    public void takeBreak() {
        System.out.println("  " + name + " takes a coffee break.");
    }
    
    // Sleepable
    @Override
    public void sleep() {
        System.out.println("  " + name + " goes home to sleep.");
    }
    
    @Override
    public void rest() {
        System.out.println("  " + name + " takes a power nap.");
    }
    
    // Codeable
    @Override
    public void writeCode() {
        System.out.println("  " + name + " writes clean, efficient code.");
    }
    
    @Override
    public void reviewCode() {
        System.out.println("  " + name + " reviews pull requests.");
    }
    
    @Override
    public void debugCode() {
        System.out.println("  " + name + " debugs the mysterious bug.");
    }
    
    // Meetable
    @Override
    public void attendMeeting() {
        System.out.println("  " + name + " attends the daily standup.");
    }
    
    @Override
    public void scheduleMeeting() {
        System.out.println("  " + name + " schedules a design review meeting.");
    }
}

/**
 * ENGINEERING MANAGER
 * -------------------
 * Implements: Workable, Eatable, Sleepable, Meetable, Manageable
 * Does NOT implement: Codeable (doesn't code anymore), Maintainable
 */
class EngineeringManager implements Workable, Eatable, Sleepable, Meetable, Manageable {
    
    private String name;
    
    public EngineeringManager(String name) {
        this.name = name;
    }
    
    // Workable
    @Override
    public void work() {
        System.out.println("  " + name + " works on team strategy and planning.");
    }
    
    // Eatable
    @Override
    public void eat() {
        System.out.println("  " + name + " has a working lunch meeting.");
    }
    
    @Override
    public void takeBreak() {
        System.out.println("  " + name + " takes a short break between meetings.");
    }
    
    // Sleepable
    @Override
    public void sleep() {
        System.out.println("  " + name + " goes home late and sleeps.");
    }
    
    @Override
    public void rest() {
        System.out.println("  " + name + " relaxes while thinking about deadlines.");
    }
    
    // Meetable
    @Override
    public void attendMeeting() {
        System.out.println("  " + name + " attends stakeholder meeting.");
    }
    
    @Override
    public void scheduleMeeting() {
        System.out.println("  " + name + " schedules 1-on-1s with team members.");
    }
    
    // Manageable
    @Override
    public void manageTeam() {
        System.out.println("  " + name + " manages the engineering team.");
    }
    
    @Override
    public void assignTasks() {
        System.out.println("  " + name + " assigns sprint tasks to developers.");
    }
    
    @Override
    public void evaluatePerformance() {
        System.out.println("  " + name + " conducts performance reviews.");
    }
}

/**
 * SOFTWARE ARCHITECT
 * ------------------
 * Implements: Workable, Eatable, Sleepable, Codeable, Designable, Meetable
 * Architects can code AND design systems
 */
class SoftwareArchitect implements Workable, Eatable, Sleepable, Codeable, Designable, Meetable {
    
    private String name;
    
    public SoftwareArchitect(String name) {
        this.name = name;
    }
    
    // Workable
    @Override
    public void work() {
        System.out.println("  " + name + " works on system architecture.");
    }
    
    // Eatable
    @Override
    public void eat() {
        System.out.println("  " + name + " eats while sketching diagrams.");
    }
    
    @Override
    public void takeBreak() {
        System.out.println("  " + name + " takes a break to think about design patterns.");
    }
    
    // Sleepable
    @Override
    public void sleep() {
        System.out.println("  " + name + " dreams about microservices.");
    }
    
    @Override
    public void rest() {
        System.out.println("  " + name + " rests while reading tech blogs.");
    }
    
    // Codeable
    @Override
    public void writeCode() {
        System.out.println("  " + name + " writes critical core components.");
    }
    
    @Override
    public void reviewCode() {
        System.out.println("  " + name + " reviews architecture-impacting code.");
    }
    
    @Override
    public void debugCode() {
        System.out.println("  " + name + " debugs complex distributed system issues.");
    }
    
    // Designable
    @Override
    public void designSystem() {
        System.out.println("  " + name + " designs the overall system architecture.");
    }
    
    @Override
    public void createDiagrams() {
        System.out.println("  " + name + " creates UML and system diagrams.");
    }
    
    @Override
    public void documentArchitecture() {
        System.out.println("  " + name + " documents architecture decisions.");
    }
    
    // Meetable
    @Override
    public void attendMeeting() {
        System.out.println("  " + name + " attends architecture review meeting.");
    }
    
    @Override
    public void scheduleMeeting() {
        System.out.println("  " + name + " schedules design review sessions.");
    }
}

/**
 * ROBOT WORKER
 * ------------
 * Implements: Workable, Codeable, Maintainable
 * Does NOT implement: Eatable, Sleepable (robots don't eat or sleep!)
 * No need to throw exceptions for methods that don't apply!
 */
class RobotWorker implements Workable, Codeable, Maintainable {
    
    private String robotId;
    
    public RobotWorker(String robotId) {
        this.robotId = robotId;
    }
    
    // Workable
    @Override
    public void work() {
        System.out.println("  Robot " + robotId + " executes assigned tasks 24/7.");
    }
    
    // Codeable
    @Override
    public void writeCode() {
        System.out.println("  Robot " + robotId + " generates code using AI.");
    }
    
    @Override
    public void reviewCode() {
        System.out.println("  Robot " + robotId + " performs automated code review.");
    }
    
    @Override
    public void debugCode() {
        System.out.println("  Robot " + robotId + " runs automated debugging analysis.");
    }
    
    // Maintainable (specific to machines)
    @Override
    public void performMaintenance() {
        System.out.println("  Robot " + robotId + " runs self-diagnostic checks.");
    }
    
    @Override
    public void recharge() {
        System.out.println("  Robot " + robotId + " goes to charging station.");
    }
    
    @Override
    public void updateSoftware() {
        System.out.println("  Robot " + robotId + " installs latest software update.");
    }
}

// ============================================================================
// UTILITY CLASSES - Show how ISP enables flexible code
// ============================================================================

/**
 * This class manages work assignments.
 * It only needs Workable interface - doesn't care about eating, sleeping, etc.
 */
class WorkManager {
    
    /**
     * Assign work to any workable entity
     * Can be a developer, manager, or robot - doesn't matter!
     */
    public void assignWork(Workable worker) {
        System.out.println("\n  [WorkManager] Assigning work...");
        worker.work();
    }
}

/**
 * This class manages cafeteria operations.
 * It only needs Eatable interface - doesn't care about coding skills!
 */
class CafeteriaManager {
    
    /**
     * Serve lunch to any entity that can eat
     * Robots won't be passed here - and that's correct!
     */
    public void serveLunch(Eatable entity) {
        System.out.println("\n  [CafeteriaManager] Serving lunch...");
        entity.eat();
        entity.takeBreak();
    }
}

/**
 * This class manages code review process.
 * It only needs Codeable interface.
 */
class CodeReviewSystem {
    
    /**
     * Request a code review from any entity that can code
     * Works with developers, architects, and even robots!
     */
    public void requestReview(Codeable coder) {
        System.out.println("\n  [CodeReviewSystem] Requesting code review...");
        coder.reviewCode();
    }
}

/**
 * This class manages robot maintenance.
 * It only needs Maintainable interface.
 */
class MaintenanceSystem {
    
    /**
     * Schedule maintenance for any maintainable entity
     * Only works with robots/machines - humans won't be passed here!
     */
    public void scheduleMaintenance(Maintainable machine) {
        System.out.println("\n  [MaintenanceSystem] Scheduling maintenance...");
        machine.performMaintenance();
        machine.recharge();
        machine.updateSoftware();
    }
}

// ============================================================================
// MAIN CLASS - Demonstrates ISP
// ============================================================================

/**
 * Main class to demonstrate Interface Segregation Principle
 */
public class ISPExample {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   INTERFACE SEGREGATION PRINCIPLE (ISP) DEMONSTRATION      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Create different types of workers
        SoftwareDeveloper alice = new SoftwareDeveloper("Alice");
        EngineeringManager bob = new EngineeringManager("Bob");
        SoftwareArchitect carol = new SoftwareArchitect("Carol");
        RobotWorker robotR2 = new RobotWorker("R2-D2");
        
        // Create utility managers
        WorkManager workManager = new WorkManager();
        CafeteriaManager cafeteria = new CafeteriaManager();
        CodeReviewSystem codeReview = new CodeReviewSystem();
        MaintenanceSystem maintenance = new MaintenanceSystem();
        
        // =====================================================================
        // DEMONSTRATION 1: Work Assignment (uses Workable interface)
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("    PART 1: WORK ASSIGNMENT (Workable interface)           ");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("\nAll these entities can work:");
        
        workManager.assignWork(alice);    // Developer works
        workManager.assignWork(bob);      // Manager works
        workManager.assignWork(carol);    // Architect works
        workManager.assignWork(robotR2);  // Robot works too!
        
        // =====================================================================
        // DEMONSTRATION 2: Cafeteria Service (uses Eatable interface)
        // =====================================================================
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("    PART 2: CAFETERIA SERVICE (Eatable interface)          ");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("\nHumans can eat, robots cannot:");
        
        cafeteria.serveLunch(alice);  // Developer eats
        cafeteria.serveLunch(bob);    // Manager eats
        cafeteria.serveLunch(carol);  // Architect eats
        // cafeteria.serveLunch(robotR2);  // COMPILE ERROR! Robot doesn't implement Eatable
        System.out.println("\n  [Note: Robot R2-D2 doesn't go to cafeteria - no Eatable interface!]");
        
        // =====================================================================
        // DEMONSTRATION 3: Code Review (uses Codeable interface)
        // =====================================================================
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("    PART 3: CODE REVIEW (Codeable interface)               ");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("\nEntities that can code:");
        
        codeReview.requestReview(alice);   // Developer reviews
        codeReview.requestReview(carol);   // Architect reviews
        codeReview.requestReview(robotR2); // Robot can review too!
        // codeReview.requestReview(bob);  // COMPILE ERROR! Manager doesn't implement Codeable
        System.out.println("\n  [Note: Manager Bob doesn't do code reviews - no Codeable interface!]");
        
        // =====================================================================
        // DEMONSTRATION 4: Maintenance (uses Maintainable interface)
        // =====================================================================
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("    PART 4: MAINTENANCE (Maintainable interface)           ");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("\nOnly machines need maintenance:");
        
        maintenance.scheduleMaintenance(robotR2);  // Robot gets maintenance
        // maintenance.scheduleMaintenance(alice); // COMPILE ERROR! Human doesn't need charging!
        System.out.println("\n  [Note: Humans don't need recharging - no Maintainable interface!]");
        
        // =====================================================================
        // SUMMARY
        // =====================================================================
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    WHY ISP WORKS                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Interface Summary:");
        System.out.println("  • Workable      → work()");
        System.out.println("  • Eatable       → eat(), takeBreak()");
        System.out.println("  • Sleepable     → sleep(), rest()");
        System.out.println("  • Codeable      → writeCode(), reviewCode(), debugCode()");
        System.out.println("  • Meetable      → attendMeeting(), scheduleMeeting()");
        System.out.println("  • Manageable    → manageTeam(), assignTasks(), evaluatePerformance()");
        System.out.println("  • Designable    → designSystem(), createDiagrams()");
        System.out.println("  • Maintainable  → performMaintenance(), recharge()");
        System.out.println();
        System.out.println("Who implements what:");
        System.out.println("  • Developer  → Workable, Eatable, Sleepable, Codeable, Meetable");
        System.out.println("  • Manager    → Workable, Eatable, Sleepable, Meetable, Manageable");
        System.out.println("  • Architect  → Workable, Eatable, Sleepable, Codeable, Designable, Meetable");
        System.out.println("  • Robot      → Workable, Codeable, Maintainable");
        System.out.println();
        System.out.println("Benefits:");
        System.out.println("  ✓ No empty or throwing methods");
        System.out.println("  ✓ Each class only implements relevant interfaces");
        System.out.println("  ✓ Compile-time safety (wrong types won't even compile)");
        System.out.println("  ✓ Flexible composition of capabilities");
        System.out.println("  ✓ Easy to add new capabilities without affecting existing code");
    }
}

/*
 * ============================================================================
 *                         EXPECTED OUTPUT
 * ============================================================================
 * 
 * ╔════════════════════════════════════════════════════════════╗
 * ║   INTERFACE SEGREGATION PRINCIPLE (ISP) DEMONSTRATION      ║
 * ╚════════════════════════════════════════════════════════════╝
 * 
 * ═══════════════════════════════════════════════════════════
 *     PART 1: WORK ASSIGNMENT (Workable interface)           
 * ═══════════════════════════════════════════════════════════
 * 
 * All these entities can work:
 * 
 *   [WorkManager] Assigning work...
 *   Alice starts working on assigned tasks.
 * 
 *   [WorkManager] Assigning work...
 *   Bob works on team strategy and planning.
 * 
 *   [WorkManager] Assigning work...
 *   Carol works on system architecture.
 * 
 *   [WorkManager] Assigning work...
 *   Robot R2-D2 executes assigned tasks 24/7.
 * 
 * (... more output ...)
 * 
 * ============================================================================
 * 
 * HOW TO COMPILE AND RUN:
 * -----------------------
 * 1. Save this file as ISPExample.java
 * 2. Open terminal/command prompt
 * 3. Navigate to the folder containing the file
 * 4. Compile: javac ISPExample.java
 * 5. Run: java ISPExample
 * 
 * ============================================================================
 */

