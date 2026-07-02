# Taste (Continuously Learned by [CommandCode][cmd])

[cmd]: https://commandcode.ai/

# documentation
- Always extract the `classDiagram` block from `DESIGN_DICE.md` into a standalone `class_diagram.mermaid` file in the same package directory — no markdown fences, raw Mermaid syntax only. Do this every time a class diagram is created or updated. Confidence: 0.90


- Do not universally mandate SCHEMA.md or API.md for all systems — apply discretion and only create these docs for systems where they make sense (e.g., systems with relational data for schema, systems with external-facing interfaces for API contracts). Confidence: 0.70
- Always follow great quality when populating MD file notes — produce high-quality markdown documentation with thorough detail. Confidence: 0.85

# design-philosophy
- Solutions must be interview-appropriate — design for what can be reasoned about and coded in ~1 hour, not over-engineered production systems. Confidence: 0.85
- Avoid over-abstracting with inner interfaces and static mock implementations — keep listener/observer classes simple and direct without unnecessary abstraction layers. Confidence: 0.70

# implementation
- When DESIGN.md specifies an entity, pattern, or class, fully wire it into the implementation — don't leave design artifacts (entities, listeners, enums) declared in design docs but unimplemented or unintegrated in code. Confidence: 0.70

# java
- Java package declarations must match the file path exactly — use `com.lldprep.systems.<systemname>.*` (plural "systems") to match the directory structure under `com/lldprep/systems/`. Confidence: 0.85
- For educational content explaining Java concepts, create Java runnable files (with a main method) instead of markdown documentation files. Confidence: 0.65

# communication-style
- Never present inline code in conversation responses — it's not readable. Use file references, summaries, or write to files instead. Confidence: 0.85

# system-design
- When designing systems (HLD and LLD), always consider AI agents as first-class API consumers alongside humans. For existing implementations, add curveball scenarios testing agent traffic patterns (high-volume requests, programmatic retries, automated workflows, token-based rate limiting). This is a permanent design principle — humans and AI agents will coexist, and systems must be designed for that reality. Confidence: 0.85

# critical-thinking
- Design specs (like DESIGN_DICE.md) are not infallible — always cross-check facts, validate relationships, and identify mistakes rather than treating design documents as unquestionable source of truth. Confidence: 0.85

# testing
- Do not write JUnit test files; instead create comprehensive demo main methods with clear scenario headers, inputs, expected effects, and actual outputs for manual verification. Confidence: 0.75