package com.lldprep.systems.symbolsearch.cache;

import com.lldprep.systems.symbolsearch.model.SearchQuery;
import com.lldprep.systems.symbolsearch.model.SearchResult;
import com.lldprep.systems.symbolsearch.service.Searchable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Decorator: wraps SymbolSearchEngine with an LRU query result cache.
 *
 * The engine is unaware of caching — it only knows how to search.
 * Adding or removing caching here requires zero changes to SymbolSearchEngine (OCP).
 *
 * Cache key encodes query string + maxResults + typeFilter so that two queries
 * with different type filters never collide on the same cached result.
 *
 * CACHE INVALIDATION NOTE:
 *   This cache has no invalidation on index updates (symbol add/remove).
 *   Stale results are acceptable here — the cache is shallow (100 entries, short-lived queries).
 *   A production system would subscribe to index change events and evict affected keys.
 *   That extension is covered in DESIGN.md Step 6 (Curveballs).
 */
public class SearchResultCache {

    private final Searchable engine;
    private final Map<String, List<SearchResult>> cache;

    public SearchResultCache(Searchable engine, int cacheCapacity) {
        this.engine = engine;
        // LinkedHashMap in access-order mode; removeEldestEntry enforces LRU eviction
        this.cache = Collections.synchronizedMap(
            new LinkedHashMap<String, List<SearchResult>>(cacheCapacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<SearchResult>> eldest) {
                    return size() > cacheCapacity;
                }
            }
        );
    }

    public List<SearchResult> search(SearchQuery query) {
        String key = cacheKey(query);
        List<SearchResult> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        List<SearchResult> results = engine.search(query);
        cache.put(key, results);
        return results;
    }

    // Sort type names explicitly — do not rely on EnumSet.toString() which is fragile
    // if the field type ever changes to a plain HashSet
    private String cacheKey(SearchQuery query) {
        String typeFilterKey = query.getTypeFilter().stream()
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(","));
        return query.getRawQuery() + "|" + query.getMaxResults() + "|" + typeFilterKey;
    }
}
