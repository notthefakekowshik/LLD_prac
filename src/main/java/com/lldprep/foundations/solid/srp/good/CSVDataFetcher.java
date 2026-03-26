// SRP GOOD: Single responsibility — fetches data from a CSV source only.
// Only changes if CSV parsing logic changes. Formatting and notifications are not its concern.
package com.lldprep.foundations.solid.srp.good;

import java.util.List;

public class CSVDataFetcher implements DataFetcher {

    @Override
    public List<String> fetchData(String source) {
        System.out.println("[SRP-Good] CSVDataFetcher: reading from source '" + source + "'");
        // Hardcoded data simulating parsed CSV rows
        return List.of("Alice,30,Engineering", "Bob,25,Marketing", "Charlie,35,Finance");
    }
}
