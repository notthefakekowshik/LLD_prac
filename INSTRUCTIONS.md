# Architectural Design Protocol (The Architect's Brain)

This document defines the mandatory workflow for every Low-Level Design (LLD) and Machine Coding problem addressed in this repository. Unlike DSA, which focuses on algorithmic efficiency, this repository focuses on **extensibility, maintainability, and clean code**.

## 1. The D.I.C.E. Workflow
Every problem must follow this iterative design lifecycle:

### Step 1: **D**efine (Requirements & Constraints)
- List **Functional Requirements** (e.g., "User can park a car").
- List **Non-Functional Requirements** (e.g., "System must be thread-safe").
- Identify **Constraints** (e.g., "Maximum 10,000 slots").

### Step 2: **I**dentify (Schema & Entities)
- Identify **Core Entities** (e.g., Vehicle, Spot, Level).
- Define **Relationships** (One-to-Many, Many-to-Many).
- Design the **Class Diagram** (use Mermaid.js syntax in the JavaDoc).

### Step 3: **C**ode (Implementation)
- Start with **Interfaces** and **Abstract Classes** (Coding for interfaces).
- Apply relevant **Design Patterns** (Strategy, Factory, etc.).
- Ensure **SOLID** principles are followed strictly.

### Step 4: **E**volve (Curveball Handling)
- After the base solution, identify one "Requirement Change" (e.g., "Add EV charging slots") and refactor to accommodate it without breaking existing code.

## 2. Mandatory Architectural Standards

- **SOLID Compliance:** Every class should have a single responsibility.
- **Design Patterns:** Use appropriate patterns from `PATTERNS.md` and document *why* they were chosen.
- **Concurrency:** Systems must be thread-safe where applicable using `synchronized`, `Locks`, or `Atomic` variables.
- **Package Structure:** 
  - `model/`: Plain Old Java Objects (POJOs) and Entities.
  - `strategy/`: Implementation of behavioral patterns.
  - `service/`: Core business logic.
  - `repository/`: In-memory data storage (Maps/Sets).

## 3. Interview Simulation

- **Time Boxing:** Full machine coding problems (Phase 4) should be completed in **90-120 minutes**.
- **Testing:** Every system must have a `Main` class or JUnit tests demonstrating all functional requirements.
- **Documentation:** Include a `README.md` within each problem package explaining the design choices.
