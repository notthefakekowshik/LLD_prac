# Code Review Exercises

Interview-style code review practice. Each exercise is a **real multi-file
codebase** (a simulated pull request) with intentionally seeded issues. You read
it, narrate your opinions, leave the comments you'd leave in a PR — then get
graded against a rubric, like a real interview round.

Modeled on the "code review" interview format (e.g. hellointerview.com/practice/code-review).

## How to practice (as the candidate)

1. Pick an exercise under `exercises/`.
2. Open its `PR.md` — it's the pull request: context, what changed, your task.
3. Read **all** the source files under its `src/` before commenting.
4. Narrate as you go, then leave PR comments:
   `<file>:<method-or-line> — [BLOCKING|MAJOR|MINOR|NIT|QUESTION] <comment>`
5. Say **"submit review"** to be graded against [`RUBRIC.md`](RUBRIC.md).
6. **Don't** open `ANSWER_KEY.md` — it's the grader's copy (seeded issues + band gates).

### With Claude

- **Run one as an interview:** invoke the `code-review-interview` skill (or just
  say *"run code review exercise 01"*). Claude presents the PR, plays the author,
  and grades you at the end — without leaking the answer key.
- **Have Claude take the review** (to see a worked example): tell it to review
  the PR and leave comments before grading itself.

## How to add a new exercise

Invoke the `code-review-author` skill (or say *"create a new code review
exercise about X"*). It generates a realistic codebase, seeds a graded spread of
issues + decoys, and writes the `PR.md` / `src/` / `ANSWER_KEY.md`. See that
skill for the authoring contract and quality bar.

## Exercise index (built)

| # | Name | Difficulty | Time | Files | Focus |
|---|------|-----------|------|-------|-------|
| 01 | [Wallet Transfer](exercises/01-wallet-transfer/PR.md) | Medium | ~25 min | 6 | Concurrency, money safety, SQL injection, transactions |

## Author backlog

Realistic codebases to build next (invoke `code-review-author` and name one).
Modeled on the hellointerview.com/practice/code-review catalog — each is a
distinct domain that exercises a different bug family.

| Name | Difficulty | ~Time | Files | Bug families to seed |
|------|-----------|-------|-------|----------------------|
| Order checkout service | Easy | 15 min | 3 | validation, pricing/rounding, order-creation edge cases |
| Redis-backed job queue worker | Easy | 15 min | 3 | distributed lock misuse, lease expiry, at-least-once dupes |
| User spend leaderboard | Easy | 15 min | 3 | N+1 queries, tie-breaking, aggregation correctness |
| Document sharing API | Medium | 20 min | 3 | multi-tenant authz (IDOR), input validation, over-fetch |
| Feature flag client SDK | Medium | 15 min | 2 | cache staleness/TTL, thread-safety, fail-open vs fail-closed |
| Payment webhook handler | Medium | 25 min | 7 | idempotency, signature verification, retry/replay, ordering |
| LLM coding agent loop | Hard | 20 min | 8 | tool-exec injection, unbounded context, no sandboxing, loop guards |
| (Hard, TBD) | Hard | — | — | pick a concurrency- or distributed-systems-heavy domain |

## Layout

```
code-review/
├── README.md            ← this file
├── RUBRIC.md            ← shared grading rubric (6 dimensions, bands)
└── exercises/
    └── NN-slug/
        ├── PR.md         ← candidate-facing: the "pull request"
        ├── src/          ← the multi-file codebase to review
        └── ANSWER_KEY.md ← 🔒 grader only: seeded issues + band gates
```

> The `src/` code is deliberately flawed and is **not** part of any Maven
> module — it never gets compiled by `mvn`, so the seeded issues don't break the
> build.
