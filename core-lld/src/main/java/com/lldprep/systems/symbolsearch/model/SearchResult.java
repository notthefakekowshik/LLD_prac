package com.lldprep.systems.symbolsearch.model;

/**
 * Value object wrapping a matched Symbol with its relevance score and the strategy that matched it.
 * Implements Comparable for descending score ordering.
 */
public class SearchResult implements Comparable<SearchResult> {

    private final Symbol symbol;
    private final double score;
    private final MatchType matchType;

    public SearchResult(Symbol symbol, double score, MatchType matchType) {
        this.symbol = symbol;
        this.score = score;
        this.matchType = matchType;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public double getScore() {
        return score;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    // Descending order — highest score sorts first
    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("[score=%-5.1f | %-12s] %s", score, matchType, symbol);
    }
}
