package com.lldprep.systems.symbolsearch.match;

import com.lldprep.systems.symbolsearch.model.MatchType;

/**
 * Matches when the query appears as a literal substring inside the symbol name (case-insensitive).
 * Example: "amthe" matches "IAmTheWord" because "iamtheword".contains("amthe") is true.
 * Highest base score — an exact substring hit is the most precise match.
 */
public class ExactSubstringStrategy implements MatchStrategy {

    @Override
    public boolean matches(String query, String symbolName) {
        return symbolName.toLowerCase().contains(query.toLowerCase());
    }

    @Override
    public int baseScore() {
        return 100;
    }

    @Override
    public MatchType type() {
        return MatchType.EXACT;
    }
}
