// SRP GOOD: Single responsibility — formats data as plain text only.
// Only changes if the text layout/style changes. Fetching and sending are not its concern.
package com.lldprep.foundations.solid.srp.good;

import java.util.List;

public class TextReportFormatter implements ReportFormatter {

    @Override
    public String format(List<String> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORT ===\n");
        for (String row : data) {
            sb.append(row).append("\n");
        }
        sb.append("==============");
        return sb.toString();
    }
}
