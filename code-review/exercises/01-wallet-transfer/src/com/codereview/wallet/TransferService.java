package com.codereview.wallet;

public class TransferService {

    private final WalletRepository repository;
    private final AuditLogger auditLogger;

    public TransferService(WalletRepository repository, AuditLogger auditLogger) {
        this.repository = repository;
        this.auditLogger = auditLogger;
    }

    public boolean transfer(TransferRequest request) {
        try {
            Wallet from = repository.findById(request.getFromId());
            Wallet to = repository.findById(request.getToId());

            if (from.getBalance() >= request.getAmount()) {
                from.setBalance(from.getBalance() - request.getAmount());
                to.setBalance(to.getBalance() + request.getAmount());

                repository.save(from);
                repository.save(to);

                if (request.getAmount() > 10000) {
                    auditLogger.log("Large transfer from " + from.getOwnerName()
                            + " (" + from.getId() + ") amount=" + request.getAmount()
                            + " note=" + request.getNote());
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
