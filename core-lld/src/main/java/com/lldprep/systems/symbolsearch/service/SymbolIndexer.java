package com.lldprep.systems.symbolsearch.service;

import com.lldprep.systems.symbolsearch.index.SymbolIndex;
import com.lldprep.systems.symbolsearch.model.Symbol;

import java.util.List;

/**
 * Bulk-loads symbols into the index at startup.
 * In a real IDE this would be driven by a file system scanner or compiler PSI tree.
 * Here it acts as the seeding layer between the symbol source and the index.
 */
public class SymbolIndexer {

    private final SymbolIndex index;

    public SymbolIndexer(SymbolIndex index) {
        this.index = index;
    }

    public void indexAll(List<Symbol> symbols) {
        for (Symbol symbol : symbols) {
            index.add(symbol);
        }
    }

    public void index(Symbol symbol) {
        index.add(symbol);
    }
}
