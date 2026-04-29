package com.lldprep.systems.symbolsearch.match;

import com.lldprep.systems.symbolsearch.model.MatchType;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches when the query characters span one or more camelCase tokens in order.
 *
 * Example: "amthe" matches "IAmTheWord"
 *   Tokens: [I] [Am] [The] [Word]
 *   'a' matches 'A' in [Am], 'm' matches 'm' in [Am], 't' matches 'T' in [The],
 *   'h' matches 'h' in [The], 'e' matches 'e' in [The] — all 5 chars consumed ✓
 *
 * The query chars must appear in token order but need not start a token.
 * This is the pattern IntelliJ uses for its "Search Everywhere" matching.
 */
public class CamelCaseStrategy implements MatchStrategy {

    @Override
    public boolean matches(String query, String symbolName) {
        String lowerQuery = query.toLowerCase();
        List<String> tokens = tokenize(symbolName);
        int queryIdx = 0;

        for (String token : tokens) {
            String lowerToken = token.toLowerCase();
            for (int i = 0; i < lowerToken.length() && queryIdx < lowerQuery.length(); i++) {
                if (lowerToken.charAt(i) == lowerQuery.charAt(queryIdx)) {
                    queryIdx++;
                }
            }
        }

        return queryIdx == lowerQuery.length();
    }

    /**
     * Splits a camelCase symbol name into tokens at each uppercase boundary.
     * "IAmTheWord" → ["I", "Am", "The", "Word"]
     * "lruCachePolicy" → ["lru", "Cache", "Policy"]
     */
    private List<String> tokenize(String symbolName) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char c : symbolName.toCharArray()) {
            if (Character.isUpperCase(c) && current.length() > 0) {
                tokens.add(current.toString());
                current = new StringBuilder();
            }
            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    @Override
    public int baseScore() {
        return 80;
    }

    @Override
    public MatchType type() {
        return MatchType.CAMEL_CASE;
    }
}
