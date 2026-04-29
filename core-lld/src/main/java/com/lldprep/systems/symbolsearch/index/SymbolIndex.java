package com.lldprep.systems.symbolsearch.index;

import com.lldprep.systems.symbolsearch.model.Symbol;

import java.util.Set;

public interface SymbolIndex {

    void add(Symbol symbol);

    void remove(Symbol symbol);

    /**
     * Returns a pre-filtered candidate set using the trigram index.
     * Callers must still run MatchStrategy on each candidate — this is a fast pre-filter, not a match.
     */
    Set<Symbol> getCandidates(String query);
}
