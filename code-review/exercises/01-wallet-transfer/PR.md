# PR #482 — Add peer-to-peer wallet transfers

**Author:** a teammate
**Difficulty:** Mid
**Est. review time:** 20–30 min

## Context

We're adding peer-to-peer money transfers to our payments backend. A user can
move funds from their wallet to another user's wallet. Large transfers are
written to an audit log for compliance.

This is a backend service behind an HTTP layer. `TransferController.handle()` is
the entry point — the HTTP framework hands it a map of request params. Wallets
live in a SQL database accessed through `WalletRepository`. Multiple requests
run concurrently on a shared thread pool; the same wallet can be touched by more
than one request at a time.

## What changed

Six new files under `src/com/codereview/wallet/`:

| File | Purpose |
|------|---------|
| `Wallet.java` | Wallet entity (id, owner, balance) |
| `TransferRequest.java` | Request DTO |
| `WalletRepository.java` | SQL persistence |
| `TransferService.java` | Core transfer logic |
| `AuditLogger.java` | Compliance audit log |
| `TransferController.java` | HTTP entry point |

## Author's note

> "Happy path works and I tested a transfer locally. Let me know what you think
> before I add tests."

---

## Your task

Review this PR the way you would at work / in an interview:

1. **Read all six files** before commenting.
2. **Narrate your thinking out loud** as you go — first impressions, what you'd
   check, trade-offs you're weighing.
3. **Leave PR comments** in this format, one per finding:

   ```
   <file>:<method-or-line> — [BLOCKING|MAJOR|MINOR|NIT|QUESTION] <comment>
   ```

   A good comment says *what's wrong*, *why it matters*, and *what to do
   instead*. Prioritize: what would you block the merge on vs. what's a nit.
4. When you're done, say **"submit review"** and you'll be graded against the
   rubric in [`../../RUBRIC.md`](../../RUBRIC.md).

Do **not** open `ANSWER_KEY.md` — that's the grader's copy.
