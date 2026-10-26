# Interview log: skills-panel-restructure

## Round 1 (autonomous; no questions asked) - 2026-10-26

The user asked for autonomous execution and fixed the scope to increments 1-5. No open gap blocks
the plan: each candidate decision is reversible or already settled by the spec or the increment
plan, so all were resolved as assumptions. No questions were sent.

Hypothesis: the user wants the character-tab skills panel rebuilt as model, layout, view and host
layers, with the y-axis bug fixed first and a working dialog fallback under Second-in-Command.
Confidence: ~85%. Unresolved: in-game behaviour only (listener firing for CHARACTER, vanilla parent
lifetime, hotkey consumption), which only manual verification can settle.

### Decisions
- load-bearing: The view mounts in the vanilla skill panel's parent, replacing the vanilla panel
  (SiC pattern). Under SiC, or when the probe fails, the panel opens only in a DialogHost through a
  hotkey. Reason: SiC removes the same vanilla panel; research Q4/Q6.
- load-bearing: Purchases commit all-or-nothing. A dry run of the whole pending batch runs on one
  cloned whole-tab snapshot in topological order before any memory write. Reason: spec §4.5; the
  current loop leaves earlier nodes bought on failure.
- load-bearing: Panel-local coordinates are bottom-up everywhere (`py + localY`, cursor
  `event.getY() - py`). TreeLayout stores content in bottom-up space. Reason: research Q2 and
  external Q1 confirm the engine convention.
- The tier gate moves into `PrereqValidator` through a new reason-returning method. The boolean
  `canPurchase`/`canPurchaseWithPending` keep their prereq-only semantics so existing tests and
  callers do not change.
- Increment 5 spans waves 5 and 6. Host routing needs both hosts, so it cannot run in parallel
  with them.
- Increment 4 stays one task at complexity 7. The user set one wave per increment, and splitting
  the view would cross waves. The tooltip content builder moves to wave 3 instead (parallel with
  TreeLayout, disjoint files).

### Assumptions
- A click on an owned T0 node keeps today's active-slot toggle, including its auto-displacement
  of `active.get(0)`. Increment 6 replaces the displacement choice. Spec §4.3 "no-op" is deferred.
- The current colour palette stays, centralised in one class. Spec §4.1 colours are not adopted.
- `GameSpend` stops playing sounds. The view plays one sound per successful confirm (increment 4).
  `level/` no longer imports `ui/`.
- Hotkey default: `K` (LWJGL `KEY_K`), configurable as `ui_hotkey` in
  `data/config/skills_overhaul.json`. The listener acts only on the campaign map with no dialog,
  menu or core tab open, and consumes the event it acts on.
- The hotkey listener is registered in every game, not only under SiC or after probe failure.
- `NodeEffectApplier.apply` throwing mid-batch is caught per node and logged at ERROR; points
  already written stay spent. The dry run covers affordability and prereqs only.
- "Free Uni" / "Mig Pool" are real pool readouts; they get player-facing labels instead of being
  removed. The "Keys 1-3 switch tabs" hint stays because the keys stay.
- `Node.getLane/isMerge/getParentLanes` and `NodeSlotTest` stay; only unreachable UI code goes.
- `zealot/CrusadeNPCStub` reflection is out of scope.
- Zoom: mouse wheel zooms about the cursor between fit-to-panel and 2.0x. Left-drag beyond 6 px
  pans; shorter is a click.
- New classes go under `skills_overhaul.ui.{model,layout,view,host}`; tests mirror the packages.

### Confidence after round
~85%. The remaining risk is in-game behaviour, recorded as manual verification steps.
