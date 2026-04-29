package com.lldprep.systems.symbolsearch.service;

import com.lldprep.systems.symbolsearch.index.SymbolIndex;
import com.lldprep.systems.symbolsearch.match.MatchStrategy;
import com.lldprep.systems.symbolsearch.model.SearchQuery;
import com.lldprep.systems.symbolsearch.model.SearchResult;
import com.lldprep.systems.symbolsearch.model.Symbol;
import com.lldprep.systems.symbolsearch.rank.RankingStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrates the full search pipeline:
 *
 *   1. Retrieve candidates from the trigram index (fast pre-filter)
 *   2. Apply type filter if specified in the query
 *   3. Run each MatchStrategy in priority order (EXACT → CAMEL_CASE → SUBSEQUENCE)
 *   4. Score the best match via RankingStrategy
 *   5. Sort descending by score and return top-K
 *
 * Template Method pattern: the algorithm skeleton is fixed here;
 * the matching and ranking steps are delegated to injected strategies (DIP).
 */
public class SymbolSearchEngine implements Searchable {

    private final SymbolIndex index;
    private final List<MatchStrategy> strategies;
    private final RankingStrategy ranker;

    public SymbolSearchEngine(SymbolIndex index, List<MatchStrategy> strategies, RankingStrategy ranker) {
        this.index = index;
        this.strategies = Collections.unmodifiableList(strategies);
        this.ranker = ranker;
    }

    public List<SearchResult> search(SearchQuery query) {
        Set<Symbol> candidates = index.getCandidates(query.getRawQuery());
        List<SearchResult> results = new ArrayList<>();

        for (Symbol candidate : candidates) {
            if (query.hasTypeFilter() && !query.getTypeFilter().contains(candidate.getType())) {
                continue;
            }
            bestMatch(candidate, query.getRawQuery()).ifPresent(results::add);
        }

        Collections.sort(results);
        int limit = Math.min(results.size(), query.getMaxResults());
        // Copy into a new ArrayList — subList returns a live-backed view which must not be cached
        return new ArrayList<>(results.subList(0, limit));
    }

    /**
     * Tries each strategy in priority order.
     * Returns a SearchResult for the first strategy that matches,
     * scored by the ranker using that strategy's MatchType.
     */
    private Optional<SearchResult> bestMatch(Symbol symbol, String query) {
        for (MatchStrategy strategy : strategies) {
            if (strategy.matches(query, symbol.getName())) {
                double score = ranker.score(symbol, query, strategy.type());
                symbol.touchAccessTime();
                return Optional.of(new SearchResult(symbol, score, strategy.type()));
            }
        }
        return Optional.empty();
    }
}
