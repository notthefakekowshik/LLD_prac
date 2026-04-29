package com.lldprep.systems.symbolsearch.demo;

import com.lldprep.systems.symbolsearch.cache.SearchResultCache;
import com.lldprep.systems.symbolsearch.index.SymbolIndex;
import com.lldprep.systems.symbolsearch.index.TrigramInvertedIndex;
import com.lldprep.systems.symbolsearch.match.CamelCaseStrategy;
import com.lldprep.systems.symbolsearch.match.ExactSubstringStrategy;
import com.lldprep.systems.symbolsearch.match.SubsequenceStrategy;
import com.lldprep.systems.symbolsearch.model.SearchQuery;
import com.lldprep.systems.symbolsearch.model.SearchResult;
import com.lldprep.systems.symbolsearch.model.Symbol;
import com.lldprep.systems.symbolsearch.model.SymbolType;
import com.lldprep.systems.symbolsearch.rank.DefaultRankingStrategy;
import com.lldprep.systems.symbolsearch.service.SymbolIndexer;
import com.lldprep.systems.symbolsearch.service.SymbolSearchEngine;

import java.util.Arrays;
import java.util.List;

public class SymbolSearchDemo {

    public static void main(String[] args) {
        // --- Setup ---
        SymbolIndex index = new TrigramInvertedIndex();
        List<Symbol> symbols = buildSymbols();
        new SymbolIndexer(index).indexAll(symbols);

        SymbolSearchEngine engine = new SymbolSearchEngine(
            index,
            Arrays.asList(
                new ExactSubstringStrategy(),    // tried first — highest precision
                new CamelCaseStrategy(),          // tried second — camelCase token spanning
                new SubsequenceStrategy()         // tried last — broadest match
            ),
            new DefaultRankingStrategy()
        );

        SearchResultCache cache = new SearchResultCache(engine, 100);

        // --- Scenario 1: The original IntelliJ example ---
        // "amthe" must find "letsSayIAmTheWord" via CamelCase span [Am][The]
        runSearch(cache, "Scenario 1 — CamelCase span: 'amthe'",
            SearchQuery.builder("amthe").build());

        // --- Scenario 2: Exact substring match ---
        // "cache" appears literally inside "LRUEvictionPolicy" → no, but in "CacheManager" → yes
        runSearch(cache, "Scenario 2 — Exact substring: 'cache'",
            SearchQuery.builder("cache").build());

        // --- Scenario 3: Type-filtered search (CLASS only) ---
        runSearch(cache, "Scenario 3 — Type filter CLASS only: 'manager'",
            SearchQuery.builder("manager").filterByType(SymbolType.CLASS).build());

        // --- Scenario 4: Short query (< 3 chars, no trigrams — falls back to full scan) ---
        runSearch(cache, "Scenario 4 — Short query: 'lv'",
            SearchQuery.builder("lv").build());

        // --- Scenario 5: Subsequence-only match ---
        // "obeng" is not a substring, not camelCase-spanning, but is a subsequence of "OrderBookEngine"
        runSearch(cache, "Scenario 5 — Subsequence only: 'obeng'",
            SearchQuery.builder("obeng").build());

        // --- Scenario 6: No match ---
        runSearch(cache, "Scenario 6 — No match: 'xyzqwerty'",
            SearchQuery.builder("xyzqwerty").build());

        // --- Scenario 7: Cache hit (repeat of Scenario 1) ---
        System.out.println("\n--- Scenario 7 — Cache hit (repeating Scenario 1 query) ---");
        long start = System.nanoTime();
        cache.search(SearchQuery.builder("amthe").build());
        long elapsed = System.nanoTime() - start;
        System.out.printf("  Cache hit served in %d ns (no engine call)%n", elapsed);

        // --- Curveball: remove a symbol, verify it no longer appears in results ---
        System.out.println("\n--- Curveball — Remove 'letsSayIAmTheWord', re-search 'amthe' ---");
        Symbol iAmTheWord = symbols.stream()
            .filter(s -> s.getName().equals("letsSayIAmTheWord"))
            .findFirst()
            .orElseThrow();
        index.remove(iAmTheWord);
        // Use maxResults(5) to produce a different cache key — forces engine to run (not cache hit)
        runSearch(cache, "After removal — 'amthe' (maxResults=5)",
            SearchQuery.builder("amthe").maxResults(5).build());

        // --- Curveball: add a new symbol dynamically, verify it appears in results ---
        System.out.println("\n--- Curveball — Add new symbol 'AmTheNewClass', search 'amthe' ---");
        Symbol newSymbol = new Symbol(
            "AmTheNewClass",
            "com.example.AmTheNewClass",
            SymbolType.CLASS,
            "src/AmTheNewClass.java"
        );
        index.add(newSymbol);
        runSearch(cache, "After add — 'amthe' (maxResults=3)",
            SearchQuery.builder("amthe").maxResults(3).build());
    }

    private static void runSearch(SearchResultCache cache, String label, SearchQuery query) {
        System.out.println("\n--- " + label + " ---");
        List<SearchResult> results = cache.search(query);
        if (results.isEmpty()) {
            System.out.println("  (no results)");
        } else {
            results.forEach(r -> System.out.println("  " + r));
        }
    }

    private static List<Symbol> buildSymbols() {
        return Arrays.asList(
            new Symbol("letsSayIAmTheWord",       "com.example.LetsSayIAmTheWord",                   SymbolType.CLASS,    "src/LetsSayIAmTheWord.java"),
            new Symbol("OrderBookEngine",          "com.lldprep.systems.orderbook.OrderBookEngine",   SymbolType.CLASS,    "src/orderbook/OrderBookEngine.java"),
            new Symbol("LRUEvictionPolicy",        "com.lldprep.systems.cache.LRUEvictionPolicy",     SymbolType.CLASS,    "src/cache/LRUEvictionPolicy.java"),
            new Symbol("LFUEvictionPolicy",        "com.lldprep.systems.cache.LFUEvictionPolicy",     SymbolType.CLASS,    "src/cache/LFUEvictionPolicy.java"),
            new Symbol("CacheManager",             "com.lldprep.systems.cache.CacheManager",          SymbolType.CLASS,    "src/cache/CacheManager.java"),
            new Symbol("RateLimiterFactory",       "com.lldprep.systems.ratelimiter.RateLimiterFactory", SymbolType.CLASS, "src/ratelimiter/RateLimiterFactory.java"),
            new Symbol("TokenBucketLimiter",       "com.lldprep.systems.ratelimiter.TokenBucketLimiter", SymbolType.CLASS, "src/ratelimiter/TokenBucketLimiter.java"),
            new Symbol("AccountManager",           "com.example.AccountManager",                      SymbolType.CLASS,    "src/AccountManager.java"),
            new Symbol("UserSessionManager",       "com.example.UserSessionManager",                  SymbolType.CLASS,    "src/UserSessionManager.java"),
            new Symbol("AmplitudeThreshold",       "com.example.AmplitudeThreshold",                  SymbolType.CLASS,    "src/AmplitudeThreshold.java"),
            new Symbol("calculateMatchScore",      "com.example.Scorer.calculateMatchScore",           SymbolType.METHOD,   "src/Scorer.java"),
            new Symbol("amortizedComplexity",      "com.example.Theory.amortizedComplexity",           SymbolType.METHOD,   "src/Theory.java"),
            new Symbol("getLastAccessedAt",        "com.lldprep.symbolsearch.Symbol.getLastAccessedAt", SymbolType.METHOD, "src/symbolsearch/Symbol.java"),
            new Symbol("findAmountTheUser",        "com.example.PaymentService.findAmountTheUser",     SymbolType.METHOD,   "src/PaymentService.java"),
            new Symbol("amountTheory",             "com.example.Finance.amountTheory",                 SymbolType.METHOD,   "src/Finance.java"),
            new Symbol("matchThreshold",           "com.example.Config.matchThreshold",                SymbolType.FIELD,    "src/Config.java"),
            new Symbol("lruCacheValue",            "com.example.Holder.lruCacheValue",                 SymbolType.FIELD,    "src/Holder.java"),
            new Symbol("SymbolSearchEngine.java",  "src/symbolsearch/SymbolSearchEngine.java",         SymbolType.FILE,     "src/symbolsearch/SymbolSearchEngine.java"),
            new Symbol("TaskSchedulerDemo.java",   "src/taskscheduler/TaskSchedulerDemo.java",          SymbolType.FILE,     "src/taskscheduler/TaskSchedulerDemo.java"),
            new Symbol("maxRetries",               "com.example.Config.maxRetries",                    SymbolType.VARIABLE, "src/Config.java")
        );
    }
}
