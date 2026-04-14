package com.lldprep.foundations.behavioral.templatemethod.bad;

import java.util.List;

/**
 * BAD: Each exporter duplicates the entire algorithm, including shared steps.
 *
 * Problems:
 * 1. The skeleton (fetchData → format → write) is copy-pasted in every exporter.
 * 2. Changing a shared step (e.g., adding validation) requires editing every class.
 * 3. No enforcement that all exporters follow the same sequence — easy to skip a step.
 * 4. DRY violation — shared logic repeated across CSVExporterBad and JSONExporterBad.
 */
public class DataExporterBad {

    public static class CSVExporterBad {
        public void export(String destination) {
            // Step 1: fetch — duplicated across all exporters
            List<String> data = List.of("Alice,30", "Bob,25");
            System.out.println("[CSV] Fetched " + data.size() + " records");

            // Step 2: format — only this step differs
            StringBuilder sb = new StringBuilder("name,age\n");
            for (String row : data) sb.append(row).append("\n");

            // Step 3: write — duplicated across all exporters
            System.out.println("[CSV] Writing to " + destination + ": \n" + sb);
        }
    }

    public static class JSONExporterBad {
        public void export(String destination) {
            // Step 1: fetch — identical to CSVExporterBad
            List<String> data = List.of("Alice,30", "Bob,25");
            System.out.println("[JSON] Fetched " + data.size() + " records");

            // Step 2: format — only this step differs
            StringBuilder sb = new StringBuilder("[\n");
            for (String row : data) {
                String[] parts = row.split(",");
                sb.append("  {\"name\":\"").append(parts[0])
                  .append("\",\"age\":").append(parts[1]).append("},\n");
            }
            sb.append("]");

            // Step 3: write — identical to CSVExporterBad
            System.out.println("[JSON] Writing to " + destination + ": \n" + sb);
        }
    }
}
