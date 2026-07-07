# Code Review Rubric

Shared grading rubric for every exercise in `code-review/`. The grader (a human
or an AI running the `code-review-interview` skill) scores against the six
dimensions below, then maps to a hiring band.

The exercise's `ANSWER_KEY.md` supplies the ground-truth findings; this rubric
turns coverage + communication into a score.

---

## Six dimensions (100 pts)

| # | Dimension | Wt | What earns points |
|---|-----------|----|-------------------|
| 1 | **Correctness & bugs** | 25 | Finds functional defects: logic errors, null/edge cases, broken contracts, off-by-one, resource leaks. |
| 2 | **Concurrency & data integrity** | 20 | Spots races, non-atomic read-modify-write, missing transactions, lost updates, unsafe sharing, money/rounding correctness. |
| 3 | **Security** | 15 | Injection, missing input validation at trust boundaries, secrets, sensitive-data logging, authz gaps. |
| 4 | **Design & maintainability** | 15 | SOLID violations, coupling, poor error modeling, magic numbers, testability, API clarity. |
| 5 | **Communication** | 15 | Comments are specific, actionable (what+why+fix), respectful PR tone, anchored to file:line. |
| 6 | **Judgment & prioritization** | 10 | Correctly ranks blocking vs. nit; avoids false positives and decoy/nitpick chasing; asks good questions where the code is ambiguous. |

**Coverage** within dims 1–4 is measured against the answer key:
`found / (criticals + majors + minors)`, weighting criticals heaviest. Valid
findings not in the key count as bonus (cap +5 total).

---

## Severity labels the candidate should use

| Label | Meaning |
|-------|---------|
| `BLOCKING` | Do not merge. Correctness/security/data-loss. |
| `MAJOR` | Should fix before or right after merge. |
| `MINOR` | Worth fixing; not urgent. |
| `NIT` | Style/preference; take it or leave it. |
| `QUESTION` | Not sure it's wrong — asking the author. |

Using the right label *is* part of the Judgment score.

---

## Bands

| Band | Bar |
|------|-----|
| **Strong Hire** | All criticals found + most majors + clear prioritized communication. |
| **Hire** | All-but-one critical + several majors; solid comments. |
| **Lean Hire** | Found some criticals but missed a money-safety / data-loss one, or communication weak. |
| **No Hire** | Missed multiple criticals, or spent the review on nits/decoys. |

Per-exercise **band gates** in `ANSWER_KEY.md` override this table (some
criticals are non-negotiable).

---

## Scorecard template (grader fills this in)

```
Exercise: __
Dim 1 Correctness & bugs      __/25   — 
Dim 2 Concurrency & integrity __/20   — 
Dim 3 Security                __/15   — 
Dim 4 Design                  __/15   — 
Dim 5 Communication           __/15   — 
Dim 6 Judgment                __/10   — 
Bonus (valid, off-key)        +__/5
------------------------------------
Total                         __/100
Band: __

Criticals found:   [ ... ]
Criticals MISSED:  [ ... ]   ← the coaching priority
Decoys chased:     [ ... ]
Top 3 things to do better next time:
  1.
  2.
  3.
```
