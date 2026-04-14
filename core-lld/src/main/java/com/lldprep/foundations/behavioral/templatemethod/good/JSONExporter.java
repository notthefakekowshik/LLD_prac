package com.lldprep.foundations.behavioral.templatemethod.good;

import java.util.List;

/** Exports data as JSON. Only overrides the format step — inherits all other steps. */
public class JSONExporter extends DataExporter {

    @Override
    protected String formatData(List<String> data) {
        StringBuilder sb = new StringBuilder("[\n");
        for (String row : data) {
            String[] parts = row.split(",");
            sb.append("  {\"name\":\"").append(parts[0])
              .append("\",\"age\":").append(parts[1]).append("},\n");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    protected String formatName() { return "JSON"; }

    /** Overrides the post-export hook to send a notification after export. */
    @Override
    protected void postExport(String destination) {
        System.out.println("  [JSON] Notifying downstream system: export complete to " + destination);
    }
}
