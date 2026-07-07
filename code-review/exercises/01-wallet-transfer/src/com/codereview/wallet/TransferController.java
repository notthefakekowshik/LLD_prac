package com.codereview.wallet;

import java.util.Map;

public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // Invoked by the HTTP layer with raw query/body params.
    public String handle(Map<String, String> params) {
        TransferRequest request = new TransferRequest();
        request.setFromId(params.get("from"));
        request.setToId(params.get("to"));
        request.setAmount(Double.parseDouble(params.get("amount")));
        request.setNote(params.get("note"));

        boolean success = transferService.transfer(request);
        if (success) {
            return "OK";
        } else {
            return "FAILED";
        }
    }
}
