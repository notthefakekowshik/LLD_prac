package com.lldprep.systems.symbolsearch.model;

import java.time.Instant;

public class Symbol {

    private final String name;
    private final String fullyQualifiedName;
    private final SymbolType type;
    private final String filePath;
    private Instant lastAccessedAt;

    public Symbol(String name, String fullyQualifiedName, SymbolType type, String filePath) {
        this.name = name;
        this.fullyQualifiedName = fullyQualifiedName;
        this.type = type;
        this.filePath = filePath;
        this.lastAccessedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public SymbolType getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    // Called by the engine when this symbol appears in a search result — boosts recency score
    public void touchAccessTime() {
        this.lastAccessedAt = Instant.now();
    }

    // Identity is defined by fully qualified name — two Symbol objects representing the same
    // code element must be treated as equal so Map/Set lookups in TrigramInvertedIndex work correctly.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Symbol)) {
            return false;
        }
        Symbol other = (Symbol) o;
        return fullyQualifiedName.equals(other.fullyQualifiedName);
    }

    @Override
    public int hashCode() {
        return fullyQualifiedName.hashCode();
    }

    @Override
    public String toString() {
        return type + " " + fullyQualifiedName + " [" + filePath + "]";
    }
}
