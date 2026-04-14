package com.lldprep.foundations.behavioral.templatemethod.good;

import java.util.List;

/**
 * Exports data as XML.
 * Also demonstrates overriding the validate() hook to enforce a max-record limit.
 */
public class XMLExporter extends DataExporter {

    private static final int MAX_RECORDS = 2;

    @Override
    protected boolean validate(List<String> data) {
        if (data.size() > MAX_RECORDS) {
            System.out.println("  [XML] Too many records (" + data.size() + " > " + MAX_RECORDS + "). Rejecting.");
            return false;
        }
        return true;
    }

    @Override
    protected String formatData(List<String> data) {
        StringBuilder sb = new StringBuilder("<records>\n");
        for (String row : data) {
            String[] parts = row.split(",");
            sb.append("  <record><name>").append(parts[0])
              .append("</name><age>").append(parts[1]).append("</age></record>\n");
        }
        sb.append("</records>");
        return sb.toString();
    }

    @Override
    protected String formatName() { return "XML"; }
}
