# Preparation Strategy: JPMC & FAANG-Level LLD/Concurrency Focus

## Current Mandate
*   **Goal:** Land a Tier-1 financial institution role (e.g., JPMC, Goldman Sachs, Morgan Stanley) while maintaining FAANG-level preparation.
*   **Company Context:** JPMC prioritizes Core Java, Multi-threading, Clean Code (SOLID), and robust Low-Level Design (LLD).
*   **Strategy Split:**
    *   **70% LLD & Concurrency:** Focus on building and refining system components (Thread Pools, Rate Limiters, Caches) and mastering the Java Memory Model.
    *   **20% HLD:** Breadth-first approach to Distributed Systems concepts (Caching, Kafka, Database Sharding).
    *   **10% DSA:** "Maintenance Mode" (one POTD every 2 days) to keep algorithmic skills fresh without burnout.

## Current Mindset & Burnout Management
*   **Status:** Currently feeling **DSA burnout**. 
*   **Strategy:** Pivot focus to **LLD, Concurrency, and HLD** to regain momentum and build interesting systems.
*   **Low-Friction DSA:** Do not stop DSA entirely; keep it in "Maintenance Mode" (1 easy/medium problem every 2 days) to avoid losing touch with patterns.
*   **LLD/DSA Overlap:** When doing LLD, look for DSA-heavy components (e.g., using a Min-Heap for a Timer, or a DLL for a Cache) to keep algorithmic thinking active without the fatigue.

## Integrated Prep Directories
Always include these in the session context when starting:
*   **LLD & Concurrency:** `/Volumes/Crucial_X9/LLD_prep` (Current Root)
*   **DSA POTD:** `/Volumes/Crucial_X9/DSA_POTD`
*   **HLD Prep:** `/Volumes/Crucial_X9/HLD_prep`

## Recent Milestones
*   Established robust core-lld components: `BloomFilter`, `Cache`, `Logging`, `RateLimiter`, `CustomThreadPool`.
*   Extensive Java Concurrency foundation in `java-fundamentals/java-concurrency`.

## Guidance for Gemini CLI
1.  **Prioritize LLD/Concurrency:** When proposing tasks, lean towards LLD and Concurrency improvements.
2.  **JPMC Context:** Always consider thread-safety, performance, and clean code principles as paramount.
3.  **Automatic Context:** At the start of each session, remind the user to load the DSA and HLD directories if they aren't already present.

gemini --resume e05a89e1-ef37-4843-ba80-203e6089529e