package com.lldprep.systems.symbolsearch.exception;

public class SymbolNotFoundException extends RuntimeException {

    public SymbolNotFoundException(String symbolName) {
        super("Symbol not found in index: " + symbolName);
    }
}
