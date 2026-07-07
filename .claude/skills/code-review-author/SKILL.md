---
name: code-review-author
description: Author a new code-review interview exercise in code-review/exercises/. Use when the user says "create a code review exercise", "new code review exercise", "add a code review practice", "make a PR to review about X", or wants a seeded multi-file codebase for review practice. Generates the codebase, PR.md, and hidden ANSWER_KEY.md.
---

# Code Review Exercise Author

Build a new interview-style code review exercise under
`code-review/exercises/`. The output is a realistic multi-file "pull request"
with a graded spread of intentionally seeded issues, plus a hidden answer key.

Read `code-review/README.md` and `code-review/RUBRIC.md` first — the exercise
must be gradeable against that rubric. Study `exercises/01-wallet-transfer/` as
the reference implementation.

## Inputs to settle before writing

Ask the user only if unspecified; otherwise pick sensible defaults:
- **Domain** (payments, inventory, booking, cache, auth, messaging, ...). Prefer
  something adjacent to this repo's LLD systems so it doubles as design practice.
- **Difficulty:** Junior / Mid / Senior. Drives issue count and subtlety.
- **Language:** default Java (matches the repo). Any language is fine.

## The quality bar (this is the whole point — don't cut it)

1. **The code must look real and idiomatic**, not obviously broken. A skilled
   reviewer should have to *think*. No comments hinting at the bugs. Happy path
   should plausibly "work when tested locally once."
2. **Multi-file** — real collaboration (controller → service → repository →
   entity, listeners, DTOs, tool executors). One-file exercises don't test the
   skill of holding a codebase in your head. Scale with difficulty:
   Easy ≈ 2–3 files / ~15 min, Medium ≈ 3–7 files / ~20–25 min,
   Hard ≈ 7–8 files / ~20–30 min.
3. **Issues must be discoverable by reading**, not by running or needing external
   docs. No "you'd only know this from our internal style guide."
4. **Seed a deliberate spread** across categories and severities — mirror the
   rubric's six dimensions. Target counts:

   | Difficulty | Critical | Major | Minor | Decoys |
   |-----------|----------|-------|-------|--------|
   | Junior | 2 | 2 | 3 | 1 |
   | Mid | 3–4 | 3 | 4–5 | 2 |
   | Senior | 4–5 | 4 | 4 | 2–3 |

   Spread categories: correctness/edge, **concurrency & data integrity**,
   security (injection/validation/secrets/sensitive logging), design/SOLID,
   error handling, readability/API. Every exercise should have at least one
   money-safety-grade *Critical* (something that loses data, overdraws, corrupts
   state, or opens an injection).
5. **Include decoys** — code that looks wrong but isn't, or where the obvious
   complaint is the symptom not the cause. Decoys test judgment; the answer key
   records why each is a false positive and how to score someone who chases it.
6. **Anchor findings by file + method/symbol + a code snippet**, NOT raw line
   numbers (lines drift). 

## Deliverables (exactly this structure)

```
code-review/exercises/NN-slug/
├── PR.md            ← candidate-facing pull request
├── src/<pkgpath>/   ← the flawed multi-file codebase
└── ANSWER_KEY.md    ← 🔒 grader only
```

- `NN` = next zero-padded number in the index. `slug` = kebab-case.
- **Put nothing runnable in any Maven module** — keep `src/` here so `mvn` never
  compiles the seeded flaws.

### PR.md must contain
- Title `# PR #<n> — <feature>`, difficulty, est. review time.
- **Context**: what the feature does, where the entry point is, and the runtime
  reality that makes bugs matter (e.g. "runs concurrently on a shared pool",
  "input comes straight from the HTTP layer"). This context is how you make a
  concurrency/security bug *fair* to find without hinting at it.
- **What changed**: table of files + purpose.
- An author's note (casual, over-confident — sets the review tone).
- The candidate task block (read all files → narrate → leave `file:method —
  [SEVERITY] comment` → say "submit review"). Tell them not to open ANSWER_KEY.

### ANSWER_KEY.md must contain
- `🔒 GRADER ONLY` banner + "credit valid off-key findings as bonus" note.
- Total seeded count.
- Tables by severity (Critical / Major / Minor): each row =
  **# · file·location · category · issue · expected fix**.
- A **Decoys** table: location + why it's not a real defect + how to score it.
- **Grading notes**: per-exercise **band gates** (which criticals are
  non-negotiable and what missing each caps the band at) — see the reference.

## After writing
- Add a row to the exercise index table in `code-review/README.md`.
- Do a self-check: re-read the finished `src/` as if blind and confirm every
  seeded issue is actually present and actually a defect (no accidental fixes),
  and that decoys are genuinely benign. State how many issues you seeded.
