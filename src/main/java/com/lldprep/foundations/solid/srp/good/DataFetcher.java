// SRP GOOD: Single responsibility — defines the contract for data retrieval only.
// Any change to HOW data is fetched (CSV, DB, API) creates a new implementation,
// not a modification to this interface or any unrelated class.
package com.lldprep.foundations.solid.srp.good;

import java.util.List;

public interface DataFetcher {
    List<String> fetchData(String source);
}
