package com.lldprep.systems.symbolsearch.rank;

import com.lldprep.systems.symbolsearch.model.MatchType;
import com.lldprep.systems.symbolsearch.model.Symbol;

/**
 * Strategy: computes the final relevance score for a matched symbol.
 * Combines match type, position, symbol type, and recency into one score.
 */
public interface RankingStrategy {

    double score(Symbol symbol, String query, MatchType matchType);
}
