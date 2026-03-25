# Pattern Recognition Index (LLD Index)

This document maps high-level problems to the underlying design patterns that solve them. Use this during your interview preparation to build pattern-recognition reflexes.

## 1. Creational Patterns (The Birth of Objects)
- [ ] **Singleton:** Ensure only one instance of a class exists (e.g., Database Connection).
- [ ] **Factory Method:** Defer object creation to subclasses (e.g., ShapeFactory).
- [ ] **Abstract Factory:** Families of related objects (e.g., UI Theme Factory).
- [ ] **Builder:** Complex object creation with many optional parameters (e.g., Pizza, QueryBuilder).
- [ ] **Prototype:** Clone objects when creation is expensive (e.g., Registry patterns).

## 2. Structural Patterns (The Skeleton of Systems)
- [ ] **Adapter:** Connect two incompatible interfaces (e.g., Legacy API wrapper).
- [ ] **Bridge:** Separate abstraction from implementation (e.g., Drawing APIs).
- [ ] **Decorator:** Add responsibilities dynamically (e.g., Pizza toppings, Logger wrappers).
- [ ] **Facade:** Unified interface for a complex subsystem (e.g., Computer Boot logic).
- [ ] **Flyweight:** Reuse small objects to save memory (e.g., Text characters, Game sprites).
- [ ] **Proxy:** Controlled access to an object (e.g., Virtual, Security, Smart pointers).

## 3. Behavioral Patterns (The Nervous System)
- [ ] **Strategy:** Swap algorithms at runtime (e.g., Payment Methods, Routing algorithms).
- [ ] **Observer:** One-to-many event notification (e.g., Newsletter subscription).
- [ ] **Command:** Encapsulate a request as an object (e.g., Undo/Redo, Transaction logic).
- [ ] **State:** Change behavior based on internal state (e.g., Vending Machine, Order processing).
- [ ] **Template Method:** Define the skeleton of an algorithm in a base class (e.g., Page render flow).
- [ ] **Iterator:** Sequential access without exposing the structure (e.g., Custom collection traversal).
- [ ] **Chain of Responsibility:** Pass a request along a chain of handlers (e.g., Logger, Middleware).
