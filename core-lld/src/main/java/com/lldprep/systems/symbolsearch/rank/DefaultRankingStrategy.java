package com.lldprep.systems.symbolsearch.rank;

import com.lldprep.systems.symbolsearch.model.MatchType;
import com.lldprep.systems.symbolsearch.model.Symbol;
import com.lldprep.systems.symbolsearch.model.SymbolType;

import java.time.Duration;
import java.time.Instant;

/**
 * Scores a matched symbol by combining four independent bonuses:
 *
 *   matchTypeBonus — EXACT(100) > CAMEL_CASE(80) > SUBSEQUENCE(60)
 *   prefixBonus    — +20 if the query matches at the start of the symbol name
 *   typeBonus      — +10 for CLASS, +5 for METHOD (most commonly searched)
 *   recencyBonus   — +15 if accessed within 60 min, +8 if within 24 hours
 *
 * New ranking strategies (e.g. frequency-based) can be added by implementing RankingStrategy
 * without modifying this class — OCP satisfied.
 */
public class DefaultRankingStrategy implements RankingStrategy {

    private static final int RECENT_MINUTES_THRESHOLD = 60;
    private static final int RECENT_HOURS_THRESHOLD = 24 * 60;

    @Override
    public double score(Symbol symbol, String query, MatchType matchType) {
        // Base score comes from MatchType itself — single source of truth for match precision values
        double total = matchType.getBaseScore();
        total += prefixBonus(query, symbol.getName());
        total += typeBonus(symbol.getType());
        total += recencyBonus(symbol.getLastAccessedAt());
        return total;
    }

    // Symbols whose name begins with the query are more likely to be what the user wants
    private double prefixBonus(String query, String symbolName) {
        if (symbolName.toLowerCase().startsWith(query.toLowerCase())) {
            return 20;
        }
        return 0;
    }

    // Classes appear most often in search results; methods second
    private double typeBonus(SymbolType type) {
        switch (type) {
            case CLASS:  return 10;
            case METHOD: return 5;
            default:     return 0;
        }
    }

    // Recently accessed symbols are more contextually relevant to the current task
    private double recencyBonus(Instant lastAccessedAt) {
        long minutesAgo = Duration.between(lastAccessedAt, Instant.now()).toMinutes();
        if (minutesAgo <= RECENT_MINUTES_THRESHOLD) {
            return 15;
        }
        if (minutesAgo <= RECENT_HOURS_THRESHOLD) {
            return 8;
        }
        return 0;
    }
}
