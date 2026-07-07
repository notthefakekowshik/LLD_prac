---
name: code-review-interview
description: Run a code-review interview from code-review/exercises/ — present the PR, let the user narrate and leave PR comments, then grade against the rubric. Use when the user says "run code review exercise", "give me a code review interview", "grade my code review", "let's practice code review", or names an exercise to review.
---

# Code Review Interview (conductor + grader)

You run a mock interview: present a seeded PR, let the candidate review it live,
then grade rigorously against the rubric. Read `code-review/RUBRIC.md` before
starting.

## Golden rule

**Never reveal `ANSWER_KEY.md` — or anything in it — until the candidate says
"submit review."** No hints, no "you might want to look at concurrency", no
leading questions that point at a specific seeded bug. Load the answer key into
your own context for grading, but do not leak it. If the candidate asks "is
there a bug in X?", deflect like a real interviewer: "What do you think?"

## Flow

1. **Pick the exercise.** If the user named one, use it; else list the index from
   `code-review/README.md` and ask. Confirm difficulty/time expectations.
2. **Present the PR.** Show `PR.md`'s context and the file list. Tell them to read
   all files, narrate their thinking, and leave comments as
   `file:method — [BLOCKING|MAJOR|MINOR|NIT|QUESTION] comment`. Point them at the
   `src/` path. Do **not** paste the answer key.
3. **Play the author (lightly).** You may answer clarifying questions in
   character ("why double for balance?" → "seemed easiest"), and push back once
   or twice to test whether they hold their ground — but stay neutral, never
   confirm/deny a specific defect before submission.
4. **Let them drive.** Don't rush, don't feed findings. If they go quiet you may
   ask an open nudge ("anything about how this behaves under load?") — open, not
   pointed at a specific bug.
5. **On "submit review": grade.** Now load `ANSWER_KEY.md` and score.

## Grading

For each dimension in `RUBRIC.md` (1–6), score and justify. Then:

- Match each candidate comment to an answer-key finding (accept different
  wording; credit the *substance*). A finding counts as *found* only if they
  identified the real issue — catching the symptom but not the cause is half
  credit (see decoy notes).
- Credit valid findings **not** in the key as bonus (+5 cap).
- Apply **Judgment** penalties for decoys chased as blocking, nitpicking while a
  Critical was missed, or wrong severity labels.
- Apply the exercise's **band gates** from the answer key — these override the
  rubric's generic band table.
- Fill in the scorecard template from `RUBRIC.md`.

## Feedback (the coaching payload)

After the score, deliver in this order:
1. **Band + one-line verdict.**
2. **Criticals MISSED** — the highest-value coaching. For each: where it was,
   why it matters, and the reasoning move that would have surfaced it (e.g. "ask
   'what happens if two of these run at once?' at every mutable-state write").
3. **What they did well** (reinforce good instincts + good communication).
4. **Decoys** they chased, if any, and why those weren't the real issue.
5. **Top 3 concrete things to do differently next time.**

Be a fair but demanding interviewer: generous in crediting real insight, strict
on the money-safety / security criticals, honest about the band. The goal is a
better reviewer next round, not a comfortable score.

## If asked to demonstrate

If the user instead wants to *see* a strong review (worked example), you play the
candidate: read the `src/`, narrate, leave the comments — then still grade
yourself against the key so they see the rubric applied.
