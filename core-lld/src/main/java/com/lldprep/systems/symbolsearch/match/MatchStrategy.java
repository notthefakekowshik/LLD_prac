package com.lldprep.systems.symbolsearch.match;

import com.lldprep.systems.symbolsearch.model.MatchType;

/**
 * Strategy: decides whether a query string matches a symbol name, and provides a base score.
 * Implementations are stateless — safe for concurrent use without synchronization.
 */
public interface MatchStrategy {

    boolean matches(String query, String symbolName);

    /** Base score before the ranker applies position, type, and recency bonuses. */
    int baseScore();

    MatchType type();
}
