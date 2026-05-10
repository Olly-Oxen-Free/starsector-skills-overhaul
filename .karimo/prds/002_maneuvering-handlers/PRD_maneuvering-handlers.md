# PRD: Pilot/Maneuvering Lane — Spec Gap Closure

- **Slug:** maneuvering-handlers
- **Created:** 2026-05-10
- **Status:** draft
- **Author:** jayden-eppcohen
- **Spec source:** `docs/superpowers/specs/skills-overhaul/pilot/maneuvering/2026-04-30-pilot-maneuvering-spec.md`

## 1. Summary

Close gaps between the Pilot/Maneuvering tree's current implementation and the v0.30 spec. Three nodes truly unimplemented (`stealth_adapt`, `phase_entry`, `ghost_protocol`). Multiple "working" nodes missing spec'd sub-mechanics. One ZFB-threshold capstone (`untouchable`) missing 3 of 4 spec'd mechanics. SiC reference patterns inform the maneuvering-stat application approach.

## 2. Why

Audit miscount (claimed 15/15 no-op) corrected — most logic exists in `ManeuveringCombatPlugin.applyMods()`. But spec cross-reference shows 3 unimplemented nodes and 6 partially-implemented nodes. Players spending capstone points on `untouchable` get only 1 of 4 spec'd benefits.

## 3. Scope (spec-driven)

### Truly missing
- **`stealth_adapt`** (T1) — timescale +0.15 at >80% speed; phase-ship variant +0.25 while phased.
- **`phase_entry`** (T1, phase) — `phaseEntryFluxReduction:0.25` + first-phase-per-combat free flag.
- **`ghost_protocol`** (T3, phase) — suppress flux damage while phased + enable phase-while-venting.

### Partial — missing spec sub-mechanics
- **`drift_instinct`** — add `momentumRetention:0.30` + `engineHPBonus:0.20` (currently skipped at `SOPilotedStatHandler:82-93`).
- **`evasive_action`** — add `maneuverabilityBonus:0.20` + per-frame +4%/missile-within-1000u plugin block.
- **`running_guns`** — currently always-on; spec gates on >50% top speed.
- **`engine_tuning`** (Engine Flux Tuning) — replace `accelBonus:0.25` with `accelDecelBonus:0.25`, add `maneuverBonus:0.10`, `overloadRecoveryReduction:0.30`, post-overload surge (+20%/4s).
- **`untouchable`** — add (a) permanent ZFB while firing/accelerating, (b) 30% projectile negation via DamageListener, (c) phase ships: phase flux costs removed.

### `SOPilotedStatHandler` extensions
- Implement currently-skipped keys: `momentumRetention`, `engineHPBonus`, `phaseEntryFluxReduction`, `accelDecelBonus`, `maneuverBonus`, `overloadRecoveryReduction`, `maneuverabilityBonus`.
- Use SiC pattern (`SCCRPluginImpl.java:319-329`) for accel+decel+turnAccel triple application.

### Out of scope
- Defense/Gunnery/Systems lanes (separate PRDs)
- Tree topology changes / new nodes
- Save migration of in-flight skill choices (additive changes only — re-applied on respec)

## 4. Success Criteria

- `./build.sh` and `./build.sh test` pass.
- All spec'd params from `2026-04-30-pilot-maneuvering-spec.md §6` applied; no log-and-skip warnings for any of the 7 keys above.
- `ManeuveringCombatPlugin.applyMods()` blocks added/extended for: `stealth_adapt`, `ghost_protocol`, `untouchable` (3 new mechanics), `evasive_action` missile-proximity, `running_guns` speed gate, `engine_tuning` post-overload surge.
- `untouchable` projectile negation has functional `DamageListener` OR per-frame fallback (spec line 82).
- Mod load logs: zero `NoOpHandler` warnings for `SO_*_maneuver_*`.

## 5. Risks

- **R1 — ZFB threshold API.** Spec line 40: "vanilla ZFB activates via internal ship stats or hardcoded threshold". `ZFBThresholdManager` already simulates via `getZeroFluxSpeedBoost` modifier. Confirm permanent-ZFB-while-firing path doesn't fight this manager.
- **R2 — Phase-while-venting.** Spec line 105: "suppress vent-cancel event or keep `isVenting` modifiable while phased — inspect API". May not be possible without engine intervention. T0 must investigate.
- **R3 — Flux-from-phase suppression.** Spec line 107: `getFluxGenFromPhase()` may not exist. Synthetic per-frame flux reduction is fallback. T0 verifies API.
- **R4 — Compound speed mults.** vent_sprint × zfm × phase_dancer × kinetic_cooling × stealth_adapt(timescale interacts with all). No cap. Recommend documentation only — actual capping deferred.
- **R5 — DamageListener attach point.** `untouchable` projectile negation needs listener on player ship. Verify lifecycle (combat-start hook?).
- **R6 — `momentumRetention`** has no obvious MutableShipStatsAPI mapping. Likely needs custom plugin damping logic. T0 investigates.

## 6. Tasks

See `tasks.yaml`. 8 tasks across 4 waves. T0 (investigation) gates phase + untouchable work.

## 7. Boundaries

- **require_review:** `data/config/skills_overhaul/trees/pilot_maneuvering.json` (gated path; spec param additions).
- **never_touch:** none.

## 8. Open Questions

- `momentumRetention` — pure plugin damping vs a MutableStat path?
- Phase-while-venting feasibility (R2/R3).
- `untouchable` projectile negation: DamageListener vs per-frame probability gate.

All resolved in T0.
