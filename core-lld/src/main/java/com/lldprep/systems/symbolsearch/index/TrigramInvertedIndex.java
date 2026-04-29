package com.lldprep.systems.symbolsearch.index;

import com.lldprep.systems.symbolsearch.exception.SymbolNotFoundException;
import com.lldprep.systems.symbolsearch.model.Symbol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inverted index keyed by trigrams (3-character substrings).
 *
 * HOW IT WORKS:
 *   On add:   lowercase the symbol name → extract all trigrams → store trigram → Set<Symbol>.
 *             Also store the reverse map (Symbol → trigrams) for O(1) removal.
 *
 *   On query: extract query trigrams → intersect the matching symbol sets.
 *             Only symbols containing ALL query trigrams survive — a very small candidate set
 *             that is then passed to MatchStrategy for exact/camelCase/subsequence verification.
 *
 * WHY TRIGRAMS:
 *   Bigrams produce too many false positives (intersection too large).
 *   4-grams miss short queries (a 2-char query has no 4-gram).
 *   3 characters is the practical sweet spot used by most search engines.
 *
 * EDGE CASE — short queries (< 3 chars):
 *   No trigrams can be extracted. All indexed symbols are returned as candidates.
 *   Acceptable because short queries are rare and MatchStrategy filters them down quickly.
 *
 * THREAD SAFETY:
 *   ConcurrentHashMap for both maps. ConcurrentHashMap.newKeySet() for the symbol sets.
 *   Safe for concurrent reads and incremental updates (curveball: file watcher scenario).
 */
public class TrigramInvertedIndex implements SymbolIndex {

    // CRITICAL SECTION — shared mutable state accessed by indexer and query threads
    private final Map<String, Set<Symbol>> trigramToSymbols = new ConcurrentHashMap<>();
    private final Map<Symbol, Set<String>> symbolToTrigrams = new ConcurrentHashMap<>();

    @Override
    public void add(Symbol symbol) {
        Set<String> trigrams = extractTrigrams(symbol.getName().toLowerCase());
        symbolToTrigrams.put(symbol, trigrams);
        for (String trigram : trigrams) {
            trigramToSymbols
                .computeIfAbsent(trigram, k -> ConcurrentHashMap.newKeySet())
                .add(symbol);
        }
    }

    @Override
    public void remove(Symbol symbol) {
        Set<String> trigrams = symbolToTrigrams.remove(symbol);
        if (trigrams == null) {
            throw new SymbolNotFoundException(symbol.getName());
        }
        for (String trigram : trigrams) {
            Set<Symbol> symbols = trigramToSymbols.get(trigram);
            if (symbols != null) {
                symbols.remove(symbol);
                if (symbols.isEmpty()) {
                    trigramToSymbols.remove(trigram);
                }
            }
        }
    }

    @Override
    public Set<Symbol> getCandidates(String query) {
        Set<String> queryTrigrams = extractTrigrams(query.toLowerCase());

        if (queryTrigrams.isEmpty()) {
            // Query too short — fall back to all indexed symbols
            return new HashSet<>(symbolToTrigrams.keySet());
        }

        Set<Symbol> candidates = null;
        for (String trigram : queryTrigrams) {
            Set<Symbol> matches = trigramToSymbols.get(trigram);
            if (matches == null) {
                // No symbol in the index has this trigram — no match possible
                return Collections.emptySet();
            }
            if (candidates == null) {
                candidates = new HashSet<>(matches);
            } else {
                candidates.retainAll(matches); // intersection — must contain ALL trigrams
            }
        }

        return candidates != null ? candidates : Collections.emptySet();
    }

    // Extracts all 3-character substrings from a lowercased symbol name
    private Set<String> extractTrigrams(String text) {
        Set<String> trigrams = new HashSet<>();
        for (int i = 0; i <= text.length() - 3; i++) {
            trigrams.add(text.substring(i, i + 3));
        }
        return trigrams;
    }
}
