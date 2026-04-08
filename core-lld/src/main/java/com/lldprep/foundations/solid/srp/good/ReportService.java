// SRP GOOD: Orchestration only — ReportService coordinates the three separate responsibilities
// but owns none of their logic. It only changes if the orchestration flow itself changes.
// Each collaborator (fetcher, formatter, sender) has its own single reason to change.
package com.lldprep.foundations.solid.srp.good;

import java.util.List;

public class ReportService {

    private final DataFetcher fetcher;
    private final ReportFormatter formatter;
    private final NotificationSender sender;

    public ReportService(DataFetcher fetcher, ReportFormatter formatter, NotificationSender sender) {
        this.fetcher = fetcher;
        this.formatter = formatter;
        this.sender = sender;
    }

    public void generateAndSend(String source, String recipient) {
        List<String> data = fetcher.fetchData(source);
        String report = formatter.format(data);
        sender.send(recipient, report);
    }
}
