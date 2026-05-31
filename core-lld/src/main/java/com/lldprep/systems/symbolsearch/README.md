# Symbol Search Engine

An IntelliJ-style "Search Everywhere" engine with trigram inverted index, multiple match strategies, and LRU query result caching. Demonstrating **Strategy**, **Decorator**, **Builder**, and **Template Method** patterns.

## Features

- **Trigram Inverted Index** — O(K) candidate pre-filtering before any string matching; K tiny even for 100K+ symbols
- **Three Match Strategies** — run in priority order, best match wins:
  - `ExactSubstringStrategy` — literal substring match (case-insensitive)
  - `CamelCaseStrategy` — query chars span camelCase token boundaries
  - `SubsequenceStrategy` — all query chars appear in order (two-pointer)
- **Ranked Results** — composite scoring: match type bonus + prefix bonus + type bonus + recency bonus
- **LRU Query Cache** — `SearchResultCache` decorates the engine; repeated queries served from cache
- **Type Filter** — restrict results to CLASS, METHOD, FIELD, FILE, or VARIABLE
- **Incremental Index** — add/remove individual symbols without full rebuild
- **Thread-Safe** — concurrent queries via `ConcurrentHashMap` in the index

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Strategy** | `MatchStrategy` interface (3 implementations) | Swap/add matching algorithms without touching engine (OCP) |
| **Strategy** | `RankingStrategy` interface | Swap ranking logic without touching engine |
| **Decorator** | `SearchResultCache` wraps `SymbolSearchEngine` | Caching added transparently; engine knows nothing about it |
| **Builder** | `SearchQuery.Builder` | Optional fields (maxResults, typeFilter) avoid telescoping constructors |
| **Template Method** | `SymbolSearchEngine.search()` | Fixed algorithm: index → match → rank → top-K; steps are injected |

## Quick Start

```bash
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.symbolsearch.demo.SymbolSearchDemo" -pl core-lld
```

## Package Structure

```
com.lldprep.symbolsearch/
├── model/
│   ├── Symbol.java                  # Searchable entity: name, type, file, lastAccessed
│   ├── SymbolType.java              # enum: CLASS, METHOD, FIELD, FILE, VARIABLE
│   ├── SearchQuery.java             # Builder pattern: query string, maxResults, typeFilter
│   ├── SearchResult.java            # Symbol + score + MatchType; Comparable by score desc
│   └── MatchType.java               # enum: EXACT, CAMEL_CASE, SUBSEQUENCE
├── index/
│   ├── SymbolIndex.java             # Interface: add / remove / getCandidates
│   └── TrigramInvertedIndex.java    # Map<trigram, Set<Symbol>> — ConcurrencyHashMap
├── match/
│   ├── MatchStrategy.java           # Interface: matches(query, symbolName) + baseScore
│   ├── ExactSubstringStrategy.java  # Case-insensitive contains (score: 100)
│   ├── CamelCaseStrategy.java       # Query chars span camel tokens in order (score: 80)
│   └── SubsequenceStrategy.java     # Two-pointer: all chars in order (score: 60)
├── rank/
│   ├── RankingStrategy.java         # Interface: score(symbol, query, matchType)
│   └── DefaultRankingStrategy.java  # matchBonus + prefixBonus + typeBonus + recencyBonus
├── service/
│   ├── SymbolIndexer.java           # Bulk loader: indexAll(List<Symbol>)
│   └── SymbolSearchEngine.java      # Orchestrator: index → match → rank → top-K
├── cache/
│   └── SearchResultCache.java       # LRU cache decorator over SymbolSearchEngine
├── exception/
│   └── SymbolNotFoundException.java
└── demo/
    └── SymbolSearchDemo.java        # 8 scenarios + curveball
```

## How It Works

```
User types "amthe"
  → SearchQuery(min 3 chars → extract trigrams: ["amt", "mth", "the"])
  → TrigramInvertedIndex.getCandidates("amthe")
      → Intersect sets: symbols_with_"amt" ∩ symbols_with_"mth" ∩ symbols_with_"the"
      → Returns {IAmTheWord, ExponentialSmoother, ...} — tiny candidate set
  → For each candidate, try strategies in order:
      1. ExactSubstringStrategy: does "amthe" appear literally? Score=100 if yes
      2. CamelCaseStrategy: span tokens Am+The? Score=80 if yes
      3. SubsequenceStrategy: all chars in order? Score=60 if yes
  → Best match per symbol
  → DefaultRankingStrategy scores: base + prefix + type + recency
  → Sort descending, slice top-K
  → Cache result for identical future queries
```

## Scoring Breakdown

| Component | Value | Condition |
|-----------|-------|-----------|
| Match type: EXACT | 100 | Literal substring found |
| Match type: CAMEL_CASE | 80 | Query spans camel tokens |
| Match type: SUBSEQUENCE | 60 | All chars in order |
| Prefix bonus | +20 | Query matches at start of name |
| Type bonus | +10/5 | CLASS or METHOD |
| Recency bonus | +15/8 | Accessed < 60min or < 24hr |

## Demo Scenarios

1. The IntelliJ classic: `"amthe"` matches `"IAmTheWord"` via substring
2. Filter by type: only show CLASS results
3. Subsequence match: `"tsx"` matches `"TypescriptXmlParser"`
4. CamelCase match: `"amthe"` matches `"AmortizedThermostat"`
5. Cache hit: second identical query returns instantly
6. Symbol removal: remove then search — no stale results
7. Recency boost: recently accessed symbol ranks higher

## Extending the System

### Add Fuzzy/Typo-Tolerant Matching

```java
public class FuzzyMatchStrategy implements MatchStrategy {
    public boolean matches(String query, String symbolName) {
        return levenshteinDistance(query, symbolName.toLowerCase()) <= 1;
    }
    public int baseScore() { return 50; }
    public MatchType type() { return MatchType.FUZZY; }
}
// Add to engine's strategy list — zero changes to existing code
```

### Add Ranking Strategy (e.g., Frequency-Based)

```java
public class FrequencyRankingStrategy implements RankingStrategy {
    private final Map<Symbol, Integer> accessCounts = new ConcurrentHashMap<>();
    public double score(Symbol s, String q, MatchType m) {
        return accessCounts.getOrDefault(s, 0) * 10;
    }
}
```

### Add Scope Filter (Current File Only)

```java
// SearchQuery gains optional scopeFile field via Builder
SearchQuery query = SearchQuery.builder()
    .rawQuery("amthe")
    .scopeFile("src/Main.java")
    .build();
```

## Thread Safety

| Component | Strategy |
|-----------|----------|
| `TrigramInvertedIndex` | `ConcurrentHashMap<K, CopyOnWriteArraySet<V>>` |
| `SearchResultCache` | LRU cache with `synchronized` access |
| `SearchQuery` / `SearchResult` | Immutable value objects |

## Documentation

- `DESIGN.md` — Full D.I.C.E. workflow with class diagram, relationship table, and implementation order

---

**Completed:** 2026-04-29 | **Patterns:** Strategy, Decorator, Builder, Template Method
