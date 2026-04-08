// SRP GOOD: Single responsibility — defines the contract for formatting report data only.
// Only changes if the concept of "formatting" changes. Fetching and sending are not its concern.
package com.lldprep.foundations.solid.srp.good;

import java.util.List;

public interface ReportFormatter {
    String format(List<String> data);
}
