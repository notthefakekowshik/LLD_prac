package com.lldprep.foundations.behavioral.templatemethod.good;

import java.util.List;

/** Exports data as CSV. Only overrides the format step — inherits all other steps. */
public class CSVExporter extends DataExporter {

    @Override
    protected String formatData(List<String> data) {
        StringBuilder sb = new StringBuilder("name,age\n");
        for (String row : data) sb.append(row).append("\n");
        return sb.toString();
    }

    @Override
    protected String formatName() { return "CSV"; }
}
