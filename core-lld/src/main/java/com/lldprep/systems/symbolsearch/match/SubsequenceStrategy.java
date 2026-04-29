package com.lldprep.systems.symbolsearch.match;

import com.lldprep.systems.symbolsearch.model.MatchType;

/**
 * Matches when all query characters appear in the symbol name in order, but not necessarily adjacent.
 * Uses a two-pointer scan: advance the symbol pointer always; advance the query pointer on a match.
 *
 * Example: "amthe" matches "IAmTheWord"
 *   i=0 'i' != 'a', i=1 'a'=='a' queryIdx=1, i=2 'm'=='m' queryIdx=2,
 *   i=3 't' != 't'? wait — 't' is lowercase, symbol[3]='T' → toLowerCase → 't'=='t' queryIdx=3 ...
 *   All 5 chars consumed before reaching end of symbol ✓
 *
 * Lowest base score — a subsequence match is the broadest and least precise.
 */
public class SubsequenceStrategy implements MatchStrategy {

    @Override
    public boolean matches(String query, String symbolName) {
        String lowerQuery = query.toLowerCase();
        String lowerSymbol = symbolName.toLowerCase();
        int queryIdx = 0;

        for (int i = 0; i < lowerSymbol.length() && queryIdx < lowerQuery.length(); i++) {
            if (lowerSymbol.charAt(i) == lowerQuery.charAt(queryIdx)) {
                queryIdx++;
            }
        }

        return queryIdx == lowerQuery.length();
    }

    @Override
    public int baseScore() {
        return 60;
    }

    @Override
    public MatchType type() {
        return MatchType.SUBSEQUENCE;
    }
}
