package com.lldprep.systems.symbolsearch.model;

public enum MatchType {
    EXACT(100),
    CAMEL_CASE(80),
    SUBSEQUENCE(60);

    private final int baseScore;

    MatchType(int baseScore) {
        this.baseScore = baseScore;
    }

    public int getBaseScore() {
        return baseScore;
    }
}
