/**
 * ============================================================================
 *                LISKOV SUBSTITUTION PRINCIPLE (LSP)
 * ============================================================================
 * 
 * WHAT IT MEANS (Simple Words):
 * -----------------------------
 * If class B is a subclass of class A, then we should be able to replace
 * A with B WITHOUT breaking the program.
 * 
 * In even simpler terms:
 * A child class should be able to do everything its parent can do.
 * The child should NOT break the behavior that the parent promises.
 * 
 * REAL-WORLD ANALOGY:
 * -------------------
 * Think about a "Vehicle" that can "drive":
 * - Car extends Vehicle → Can drive ✓ (LSP satisfied)
 * - Truck extends Vehicle → Can drive ✓ (LSP satisfied)
 * - Boat extends Vehicle → Can it drive on roads? ✗ (LSP VIOLATED!)
 * 
 * If someone says "I need a vehicle to drive to work", and you give them
 * a boat, the program (their commute) breaks!
 * 
 * Another analogy - Remote Control:
 * - Universal Remote promises: power(), volumeUp(), volumeDown()
 * - TV Remote extends Universal Remote → Works ✓
 * - AC Remote extends Universal Remote → volumeUp() doesn't make sense! ✗
 * 
 * WHY IT'S IMPORTANT:
 * -------------------
 * 1. Predictable behavior - Code works as expected with any subclass
 * 2. Reusability - You can truly use polymorphism safely
 * 3. Fewer bugs - No surprises when using subclasses
 * 4. Better design - Forces you to think about proper inheritance
 * 
 * KEY RULE TO REMEMBER:
 * ---------------------
 * "If it looks like a duck, quacks like a duck, but needs batteries - 
 *  you probably have the wrong abstraction."
 * 
 * ============================================================================
 */

// ============================================================================
// BAD EXAMPLE (VIOLATES LSP) - DON'T DO THIS
// ============================================================================
// Classic example: Rectangle and Square problem

/*
// Parent class
class BadRectangle {
    protected int width;
    protected int height;
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getArea() {
        return width * height;
    }
}

// Child class - VIOLATES LSP!
class BadSquare extends BadRectangle {
    
    // In a square, width must equal height
    // So we override to keep them in sync
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width;  // Force height = width
    }
    
    @Override
    public void setHeight(int height) {
        this.width = height;  // Force width = height
        this.height = height;
    }
}

// THE PROBLEM:
void printArea(BadRectangle rect) {
    rect.setWidth(5);
    rect.setHeight(10);
    // For Rectangle: Area = 5 * 10 = 50 ✓
    // For Square: Area = 10 * 10 = 100 ✗ (height setter changed width!)
    System.out.println("Expected: 50, Got: " + rect.getArea());
}
// The Square breaks the expected behavior of Rectangle!
// This violates LSP - we cannot substitute Square for Rectangle!
*/

// ============================================================================
// GOOD EXAMPLE (FOLLOWS LSP) - DO THIS
// ============================================================================

/**
 * STEP 1: Create a proper abstraction (interface)
 * ------------------------------------------------
 * Instead of using inheritance poorly, we define a clear contract.
 * A Shape must be able to calculate its area and describe itself.
 */
interface Shape {
    /**
     * Calculate the area of this shape
     * @return The area as a double
     */
    double getArea();
    
    /**
     * Get a description of this shape
     * @return Human-readable description
     */
    String getDescription();
}

/**
 * STEP 2: Implement Rectangle properly
 * -------------------------------------
 * Rectangle has independent width and height.
 * Implements Shape interface correctly.
 */
class Rectangle implements Shape {
    
    private double width;
    private double height;
    
    /**
     * Create a rectangle with given dimensions
     * @param width - The width of the rectangle
     * @param height - The height of the rectangle
     */
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }
    
    // Getters
    public double getWidth() {
        return width;
    }
    
    public double getHeight() {
        return height;
    }
    
    @Override
    public double getArea() {
        return width * height;
    }
    
    @Override
    public String getDescription() {
        return "Rectangle [width=" + width + ", height=" + height + "]";
    }
}

/**
 * STEP 3: Implement Square properly
 * ----------------------------------
 * Square is NOT a subclass of Rectangle!
 * It's a separate implementation of Shape.
 * Square has only one dimension: side.
 */
class Square implements Shape {
    
    private double side;
    
    /**
     * Create a square with given side length
     * @param side - The length of each side
     */
    public Square(double side) {
        this.side = side;
    }
    
    public double getSide() {
        return side;
    }
    
    @Override
    public double getArea() {
        return side * side;
    }
    
    @Override
    public String getDescription() {
        return "Square [side=" + side + "]";
    }
}

/**
 * STEP 4: Implement Circle
 * -------------------------
 * Another shape that follows the same contract.
 */
class Circle implements Shape {
    
    private double radius;
    
    /**
     * Create a circle with given radius
     * @param radius - The radius of the circle
     */
    public Circle(double radius) {
        this.radius = radius;
    }
    
    public double getRadius() {
        return radius;
    }
    
    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }
    
    @Override
    public String getDescription() {
        return "Circle [radius=" + radius + "]";
    }
}

/**
 * STEP 5: Implement Triangle
 * ---------------------------
 * Yet another shape following the same contract.
 */
class Triangle implements Shape {
    
    private double base;
    private double height;
    
    /**
     * Create a triangle with given base and height
     * @param base - The base of the triangle
     * @param height - The height of the triangle
     */
    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }
    
    @Override
    public double getArea() {
        return 0.5 * base * height;
    }
    
    @Override
    public String getDescription() {
        return "Triangle [base=" + base + ", height=" + height + "]";
    }
}

// ============================================================================
// ANOTHER GOOD EXAMPLE: Bird Hierarchy (Common LSP Discussion)
// ============================================================================

/**
 * A Bird interface that properly defines what ALL birds can do.
 * Note: We don't include fly() here because not all birds can fly!
 */
interface Bird {
    /**
     * Make the bird's sound
     */
    void makeSound();
    
    /**
     * All birds can eat
     */
    void eat();
    
    /**
     * Get the bird's name
     */
    String getName();
}

/**
 * A FlyingBird interface for birds that CAN fly.
 * This is a separate capability - not all birds have it.
 */
interface FlyingBird extends Bird {
    /**
     * Make the bird fly
     */
    void fly();
    
    /**
     * Get the bird's flying speed
     */
    double getFlyingSpeed();
}

/**
 * A SwimmingBird interface for birds that CAN swim.
 */
interface SwimmingBird extends Bird {
    /**
     * Make the bird swim
     */
    void swim();
}

/**
 * Sparrow - A flying bird
 * Properly implements FlyingBird, can be used anywhere Bird is expected
 */
class Sparrow implements FlyingBird {
    
    private String name;
    
    public Sparrow(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name + " (Sparrow)";
    }
    
    @Override
    public void makeSound() {
        System.out.println("    " + name + " says: Chirp chirp!");
    }
    
    @Override
    public void eat() {
        System.out.println("    " + name + " eats seeds.");
    }
    
    @Override
    public void fly() {
        System.out.println("    " + name + " flies through the sky! 🐦");
    }
    
    @Override
    public double getFlyingSpeed() {
        return 35.0; // km/h
    }
}

/**
 * Eagle - Another flying bird
 */
class Eagle implements FlyingBird {
    
    private String name;
    
    public Eagle(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name + " (Eagle)";
    }
    
    @Override
    public void makeSound() {
        System.out.println("    " + name + " says: Screeeech!");
    }
    
    @Override
    public void eat() {
        System.out.println("    " + name + " catches and eats fish.");
    }
    
    @Override
    public void fly() {
        System.out.println("    " + name + " soars majestically! 🦅");
    }
    
    @Override
    public double getFlyingSpeed() {
        return 120.0; // km/h - eagles are fast!
    }
}

/**
 * Penguin - A swimming bird (CANNOT fly!)
 * 
 * BAD DESIGN would be: class Penguin implements FlyingBird { void fly() { throw Exception; } }
 * This would violate LSP!
 * 
 * GOOD DESIGN: Penguin implements SwimmingBird (what it CAN do)
 */
class Penguin implements SwimmingBird {
    
    private String name;
    
    public Penguin(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name + " (Penguin)";
    }
    
    @Override
    public void makeSound() {
        System.out.println("    " + name + " says: Squawk squawk!");
    }
    
    @Override
    public void eat() {
        System.out.println("    " + name + " catches and eats fish.");
    }
    
    @Override
    public void swim() {
        System.out.println("    " + name + " swims gracefully underwater! 🐧");
    }
}

/**
 * Duck - Can both fly AND swim!
 * Implements both interfaces - this is valid and follows LSP
 */
class Duck implements FlyingBird, SwimmingBird {
    
    private String name;
    
    public Duck(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name + " (Duck)";
    }
    
    @Override
    public void makeSound() {
        System.out.println("    " + name + " says: Quack quack!");
    }
    
    @Override
    public void eat() {
        System.out.println("    " + name + " eats bread crumbs.");
    }
    
    @Override
    public void fly() {
        System.out.println("    " + name + " flies in a V-formation! 🦆");
    }
    
    @Override
    public double getFlyingSpeed() {
        return 80.0; // km/h
    }
    
    @Override
    public void swim() {
        System.out.println("    " + name + " paddles across the pond! 🦆");
    }
}

// ============================================================================
// UTILITY CLASS - Works with any Shape or Bird (LSP in action!)
// ============================================================================

/**
 * Calculator class that works with ANY Shape
 * Because all shapes follow LSP, this works perfectly with all of them.
 */
class ShapeCalculator {
    
    /**
     * Print the area of any shape
     * LSP ensures ANY shape can be passed here and it will work correctly!
     */
    public void printArea(Shape shape) {
        System.out.println("  " + shape.getDescription());
        System.out.println("  Area = " + String.format("%.2f", shape.getArea()) + " square units");
        System.out.println();
    }
    
    /**
     * Calculate total area of multiple shapes
     */
    public double calculateTotalArea(Shape[] shapes) {
        double total = 0;
        for (Shape shape : shapes) {
            total += shape.getArea();
        }
        return total;
    }
}

/**
 * Bird Handler class that works with ANY Bird
 */
class BirdHandler {
    
    /**
     * Feed any bird - works because all birds can eat()
     */
    public void feedBird(Bird bird) {
        System.out.println("  Feeding " + bird.getName() + "...");
        bird.eat();
    }
    
    /**
     * Make any flying bird fly - only accepts FlyingBird
     * Penguin cannot be passed here (and that's correct!)
     */
    public void makeFly(FlyingBird bird) {
        System.out.println("  Making " + bird.getName() + " fly...");
        bird.fly();
        System.out.println("  Flying at " + bird.getFlyingSpeed() + " km/h");
    }
    
    /**
     * Make any swimming bird swim - only accepts SwimmingBird
     */
    public void makeSwim(SwimmingBird bird) {
        System.out.println("  Making " + bird.getName() + " swim...");
        bird.swim();
    }
}

// ============================================================================
// MAIN CLASS - Demonstrates LSP
// ============================================================================

/**
 * Main class to demonstrate Liskov Substitution Principle
 */
public class LSPExample {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   LISKOV SUBSTITUTION PRINCIPLE (LSP) DEMONSTRATION        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // =====================================================================
        // PART 1: Shape Example - Demonstrating LSP with Shapes
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("         PART 1: SHAPES - LSP IN ACTION                    ");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        // Create different shapes
        Shape rectangle = new Rectangle(5, 10);
        Shape square = new Square(7);
        Shape circle = new Circle(5);
        Shape triangle = new Triangle(8, 6);
        
        // The ShapeCalculator works with ANY shape!
        // This is LSP: we can substitute any Shape subtype
        ShapeCalculator calculator = new ShapeCalculator();
        
        System.out.println("Calculating areas for different shapes:\n");
        calculator.printArea(rectangle);  // Works!
        calculator.printArea(square);     // Works!
        calculator.printArea(circle);     // Works!
        calculator.printArea(triangle);   // Works!
        
        // Calculate total area - all shapes work together
        Shape[] allShapes = {rectangle, square, circle, triangle};
        double totalArea = calculator.calculateTotalArea(allShapes);
        System.out.println("Total area of all shapes: " + String.format("%.2f", totalArea) + " square units\n");
        
        // =====================================================================
        // PART 2: Bird Example - Proper Hierarchy Design
        // =====================================================================
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("         PART 2: BIRDS - PROPER HIERARCHY                  ");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        // Create different birds
        Sparrow sparrow = new Sparrow("Tweety");
        Eagle eagle = new Eagle("Eddie");
        Penguin penguin = new Penguin("Pablo");
        Duck duck = new Duck("Donald");
        
        BirdHandler handler = new BirdHandler();
        
        // ALL birds can be fed (Bird interface)
        System.out.println("Feeding all birds (all implement Bird.eat()):\n");
        handler.feedBird(sparrow);
        System.out.println();
        handler.feedBird(eagle);
        System.out.println();
        handler.feedBird(penguin);  // Penguin IS a Bird, can eat!
        System.out.println();
        handler.feedBird(duck);
        System.out.println();
        
        // Only FLYING birds can fly (FlyingBird interface)
        System.out.println("\nMaking flying birds fly (only FlyingBird):\n");
        handler.makeFly(sparrow);
        System.out.println();
        handler.makeFly(eagle);
        System.out.println();
        handler.makeFly(duck);  // Duck can fly!
        System.out.println();
        // handler.makeFly(penguin);  // COMPILE ERROR! Penguin is not FlyingBird
        // This is GOOD - the compiler prevents us from making a penguin fly!
        
        // Swimming birds
        System.out.println("\nMaking swimming birds swim (only SwimmingBird):\n");
        handler.makeSwim(penguin);
        System.out.println();
        handler.makeSwim(duck);  // Duck can also swim!
        System.out.println();
        // handler.makeSwim(sparrow);  // COMPILE ERROR! Sparrow can't swim
        
        // =====================================================================
        // SUMMARY
        // =====================================================================
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    WHY LSP MATTERS                         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("LSP means: Subtypes must be substitutable for their base types");
        System.out.println();
        System.out.println("What we demonstrated:");
        System.out.println("  ✓ Any Shape can be passed to ShapeCalculator.printArea()");
        System.out.println("  ✓ Rectangle, Square, Circle, Triangle all work correctly");
        System.out.println("  ✓ Any Bird can be fed using BirdHandler.feedBird()");
        System.out.println("  ✓ Only FlyingBird can fly - Penguin correctly excluded");
        System.out.println("  ✓ Only SwimmingBird can swim - Sparrow correctly excluded");
        System.out.println();
        System.out.println("Bad design (violates LSP):");
        System.out.println("  ✗ Square extends Rectangle (setWidth/setHeight breaks)");
        System.out.println("  ✗ Penguin extends FlyingBird (fly() would throw exception)");
        System.out.println();
        System.out.println("Good design (follows LSP):");
        System.out.println("  ✓ Both Rectangle and Square implement Shape interface");
        System.out.println("  ✓ Bird, FlyingBird, SwimmingBird are separate interfaces");
        System.out.println("  ✓ Each class only implements what it CAN actually do");
        System.out.println();
        System.out.println("Remember: \"If it looks like a duck but can't quack, your design needs work!\"");
    }
}

/*
 * ============================================================================
 *                         EXPECTED OUTPUT
 * ============================================================================
 * 
 * ╔════════════════════════════════════════════════════════════╗
 * ║   LISKOV SUBSTITUTION PRINCIPLE (LSP) DEMONSTRATION        ║
 * ╚════════════════════════════════════════════════════════════╝
 * 
 * ═══════════════════════════════════════════════════════════
 *          PART 1: SHAPES - LSP IN ACTION                    
 * ═══════════════════════════════════════════════════════════
 * 
 * Calculating areas for different shapes:
 * 
 *   Rectangle [width=5.0, height=10.0]
 *   Area = 50.00 square units
 * 
 *   Square [side=7.0]
 *   Area = 49.00 square units
 * 
 *   Circle [radius=5.0]
 *   Area = 78.54 square units
 * 
 *   Triangle [base=8.0, height=6.0]
 *   Area = 24.00 square units
 * 
 * Total area of all shapes: 201.54 square units
 * 
 * (... bird examples ...)
 * 
 * ============================================================================
 * 
 * HOW TO COMPILE AND RUN:
 * -----------------------
 * 1. Save this file as LSPExample.java
 * 2. Open terminal/command prompt
 * 3. Navigate to the folder containing the file
 * 4. Compile: javac LSPExample.java
 * 5. Run: java LSPExample
 * 
 * ============================================================================
 */

