package com.lldprep.systems.symbolsearch.service;

import com.lldprep.systems.symbolsearch.model.SearchQuery;
import com.lldprep.systems.symbolsearch.model.SearchResult;

import java.util.List;

/**
 * Abstraction for any component that can execute a symbol search.
 * SearchResultCache depends on this interface, not on SymbolSearchEngine directly — DIP satisfied.
 */
public interface Searchable {

    List<SearchResult> search(SearchQuery query);
}
