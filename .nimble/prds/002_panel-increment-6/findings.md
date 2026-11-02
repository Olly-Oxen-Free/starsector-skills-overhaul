## Task 1 (cc836d9, advisory)
- Failed-tree placeholder columns are appended after loaded trees, not at the failed tree's original index.
- One-arg `TreeLoader.loadAllForTab(tab)` has no callers and now drops failures with a WARN only; remove or document.
## Task 2 (8648bec + 3912834)
- Three advisories (status wording, n/a tooltip reason, clear replace mode on confirm) fixed in 3912834.
## Task 3 (ea6ec0d, advisory)
- reportRaidToDisruptFinished fires once per disrupted industry; vanilla disrupt targets one industry, so one stack per raid in practice. Accepted.
- Listener registration and callbacks are verified only by compile (@Override); needs in-game raid check.
