package com.lldprep.systems.symbolsearch.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Value object representing a user's search request.
 * Uses Builder pattern — maxResults and typeFilter are optional.
 */
public class SearchQuery {

    private final String rawQuery;
    private final int maxResults;
    private final Set<SymbolType> typeFilter;

    private SearchQuery(Builder builder) {
        this.rawQuery = builder.rawQuery;
        this.maxResults = builder.maxResults;
        this.typeFilter = Collections.unmodifiableSet(builder.typeFilter);
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public Set<SymbolType> getTypeFilter() {
        return typeFilter;
    }

    public boolean hasTypeFilter() {
        return !typeFilter.isEmpty();
    }

    public static Builder builder(String rawQuery) {
        return new Builder(rawQuery);
    }

    public static class Builder {

        private final String rawQuery;
        private int maxResults = 10;
        private Set<SymbolType> typeFilter = EnumSet.noneOf(SymbolType.class);

        private Builder(String rawQuery) {
            if (rawQuery == null || rawQuery.isBlank()) {
                throw new IllegalArgumentException("Query must not be blank");
            }
            this.rawQuery = rawQuery.trim();
        }

        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be positive");
            }
            this.maxResults = maxResults;
            return this;
        }

        public Builder filterByType(SymbolType first, SymbolType... rest) {
            this.typeFilter = EnumSet.of(first, rest);
            return this;
        }

        public SearchQuery build() {
            return new SearchQuery(this);
        }
    }
}
