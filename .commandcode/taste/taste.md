# Taste (Continuously Learned by [CommandCode][cmd])

[cmd]: https://commandcode.ai/

# documentation
- Do not universally mandate SCHEMA.md or API.md for all systems — apply discretion and only create these docs for systems where they make sense (e.g., systems with relational data for schema, systems with external-facing interfaces for API contracts). Confidence: 0.70
- Always follow great quality when populating MD file notes — produce high-quality markdown documentation with thorough detail. Confidence: 0.85

# design-philosophy
- Solutions must be interview-appropriate — design for what can be reasoned about and coded in ~1 hour, not over-engineered production systems. Confidence: 0.85

# implementation
- When DESIGN.md specifies an entity, pattern, or class, fully wire it into the implementation — don't leave design artifacts (entities, listeners, enums) declared in design docs but unimplemented or unintegrated in code. Confidence: 0.70

# java
- Java package declarations must match the file path exactly — use `com.lldprep.systems.<systemname>.*` (plural "systems") to match the directory structure under `com/lldprep/systems/`. Confidence: 0.85

# system-design
- When designing systems (HLD and LLD), always consider AI agents as first-class API consumers alongside humans. For existing implementations, add curveball scenarios testing agent traffic patterns (high-volume requests, programmatic retries, automated workflows, token-based rate limiting). This is a permanent design principle — humans and AI agents will coexist, and systems must be designed for that reality. Confidence: 0.85