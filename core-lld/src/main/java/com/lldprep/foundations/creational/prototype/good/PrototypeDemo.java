package com.lldprep.foundations.creational.prototype.good;

/**
 * PROTOTYPE PATTERN
 * =================
 *
 * WHY IT EXISTS:
 * Creates new objects by copying existing ones. Delegates the actual
 * cloning to the objects themselves through a clone() method.
 *
 * PROBLEMS IT SOLVES:
 * - Object creation is expensive (database queries, complex calculations)
 * - Creating objects requires access to private/protected state
 * - Subclass proliferation for minor variations of objects
 * - Runtime determination of object types to create
 * - Copying objects with circular references or complex graphs
 *
 * WHEN TO USE:
 * - Cost of creating object is more expensive than copying
 * - System needs to be independent of how products are created
 * - Classes to instantiate are specified at runtime (dynamic loading)
 * - Avoiding builder hierarchy parallel to product class hierarchy
 * - When objects have only a few different combinations of state
 *
 * KEY CONCEPTS:
 * - Shallow Copy: Copies primitives, shares references (fast, risk of shared state)
 * - Deep Copy: Clones all mutable references (safe, more expensive)
 * - Prototype Registry: Store prototypical instances, clone when needed
 *
 * COMPARISON WITH OTHER PATTERNS:
 * - Factory: Creates fresh objects from scratch
 * - Prototype: Creates by copying existing template
 * - Builder: Step-by-step construction
 *
 * REAL-WORLD EXAMPLES:
 * - Java's clone() method in Object class
 * - Spring's prototype bean scope
 * - JavaScript Object.create()
 * - Photoshop/GIMP clone stamp tool
 * - VM template cloning in cloud providers
 *
 * @see <a href="https://en.wikipedia.org/wiki/Prototype_pattern">Prototype Pattern</a>
 */
public class PrototypeDemo {
    
    public static void main(String[] args) {
        System.out.println("=== PROTOTYPE PATTERN DEMONSTRATIONS ===\n");
        
        // 1. Shallow Copy (sufficient for primitives/immutables)
        System.out.println("1. SHALLOW COPY (Primitives/Immutables):");
        Document doc1 = new Document("Template", "Content here", "Admin");
        Document doc2 = doc1.clone();
        
        doc2.setTitle("Modified");
        System.out.println("Original: " + doc1);
        System.out.println("Clone:    " + doc2);
        System.out.println("Different objects? " + (doc1 != doc2));
        System.out.println();
        
        // 2. Deep Copy (mutable collections/references)
        System.out.println("2. DEEP COPY (Mutable References):");
        DocumentWithAttachments doc3 = new DocumentWithAttachments("Report", "Annual Report", "Finance");
        doc3.addAttachment("Q1.pdf");
        doc3.addAttachment("Q2.pdf");
        doc3.setMetadata("version", "1.0");
        
        DocumentWithAttachments doc4 = doc3.clone();
        
        // Modify clone's mutable fields
        doc4.addAttachment("Q3.pdf");
        doc4.setMetadata("version", "2.0");
        
        System.out.println("Original attachments: " + doc3.getAttachments());
        System.out.println("Clone attachments:    " + doc4.getAttachments());
        System.out.println("Original metadata:    " + doc3.getMetadata().getProperty("version"));
        System.out.println("Clone metadata:       " + doc4.getMetadata().getProperty("version"));
        System.out.println("Independent? " + (!doc3.getAttachments().equals(doc4.getAttachments())));
        System.out.println();
        
        // 3. Prototype Registry
        System.out.println("3. PROTOTYPE REGISTRY:");
        ShapeCache cache = new ShapeCache();
        
        // Get clones and customize
        Shape circle1 = cache.get("CIRCLE");
        circle1.setX(10);
        circle1.setColor("Green");
        circle1.draw();
        
        Shape circle2 = cache.get("CIRCLE");
        circle2.setX(50);
        circle2.setY(50);
        circle2.draw();
        
        Shape rect1 = cache.get("RECTANGLE");
        rect1.setX(100);
        rect1.setColor("Yellow");
        rect1.draw();
        
        System.out.println("Same prototype source? " + (circle1.getClass() == circle2.getClass()));
        System.out.println("Different instances? " + (circle1 != circle2));
        System.out.println();
        
        System.out.println("=== WHEN TO USE ===");
        System.out.println("1. Object creation is expensive");
        System.out.println("2. Many similar objects needed");
        System.out.println("3. Subclass-specific creation avoided");
        System.out.println("4. Shallow copy: primitives/immutables only");
        System.out.println("5. Deep copy: when mutable fields exist");
    }
}
