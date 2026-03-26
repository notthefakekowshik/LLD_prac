# LLD Progress Journal

| S.No | Date | Problem/System | Complexity | Pattern(s) | Key Architectural Insight |
|------|------|----------------|------------|------------|---------------------------|
| 1 | 2026-03-24 | Repository Setup | N/A | Maven, Project Structure | Established foundational structure for LLD preparation. |
| 2 | 2026-03-24 | In-Memory Cache | O(1) | Strategy, Singleton, Factory | Using Strategy Pattern for Eviction Policies allows swapping LRU/LFU without touching the Cache core logic. |
| 3 | 2026-03-26 | Bloom Filter | O(k) | Strategy, Builder, Facade | Strategy Pattern for pluggable hash functions (MurmurHash/FNV/Simple) enables OCP. Builder Pattern handles complex parameter calculation (optimal bit array size & hash count from FPR). Demonstrated extensibility via CountingBloomFilter (deletion support) and BloomFilterMetrics (observability) without modifying core BloomFilter class. |
| --- | --- | --- | --- | --- | --- |
