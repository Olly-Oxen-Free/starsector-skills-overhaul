# PRD: Pilot/Defense Lane — Spec Gap Closure

- **Slug:** defense-handlers
- **Created:** 2026-05-10
- **Status:** draft
- **Author:** jayden-eppcohen
- **Spec source:** `docs/superpowers/specs/skills-overhaul/pilot/defense/2026-04-30-pilot-defense-spec.md`

## 1. Summary

Largest pilot-lane gap closure to date. **2 fully missing nodes** (Impact Mitigation, Carrier Supremacy), **5 nodes effectively no-op** (Iron Bastion, Flux Fortress, Overwhelming Force, Unbreakable, Adamantine — the last being only-visual), **11 partial** spec-mismatch nodes. Plus a **session-reload bug** in `ACTIVE_NODES` (DefenseCombatPlugin:16) that silently drops all defensive node effects after save/reload.

## 2. Why

Players spending capstone (Adamantine) and T4 (Flux Fortress / Overwhelming Force / Unbreakable) points get visual flair only — no mechanical effect. Iron Bastion buys a state-machine that the plugin never reads. Save-reload nukes all defensive bonuses until respec.

## 3. Scope

### Truly missing nodes
- Impact Mitigation (T1)
- Carrier Supremacy (T4) — orphan handler

### Effectively no-op (need applyMods blocks)
- Iron Bastion, Flux Fortress, Overwhelming Force, Unbreakable, Adamantine post-discharge

### Partial nodes (sub-mechanics)
- Hardened Modules HP params, Damage Control module repair, Combat Stamina CR timing, Battle Hardened malfunction threshold, Immovable Object (all params), Fortify shield mult, Fortress Shield hard-flux-spike, Defensive Configuration hull-spike cap, Redundant Bays disabled-bay capacity, Shield Mastery flux conversion, Lead From the Front self-armor mult, Derelict Fortifications d-mod negatives, Targe regen refactor

### Critical bug
- ACTIVE_NODES static-field reset on session reload (silently kills all defense bonuses)

### Out of scope
- Tree topology; balance retuning of working values; Defense Configuration ad-hoc value reconciliation (separate balance pass)

## 4. Success Criteria

- `./build.sh` + `./build.sh test` clean.
- All 22 spec'd nodes (incl. Impact Mitigation + Carrier Supremacy) present in JSON, with handler/plugin behavior matching spec.
- No `NoOpHandler` warnings; no log-and-skip warnings for any defense-related `SO_piloted_stat` key.
- ACTIVE_NODES survives save/reload (validation: load combat after reload, verify Fortify state-machine engages on damage).
- IronBastion ShipDestroyedListener fires; fighter-kill heal applies.
- Targe regen no longer uses raw `setHitpoints()` per-frame.

## 5. Risks

- **R1** Damage interception API — Unbreakable per-cell floor + Fortress Shield breach + Carrier Supremacy flagship-damage gate need `TakeDamageResultListener`/`DamageDealtModifier`/flux listener. None currently used. **T0 must verify.**
- **R2** ACTIVE_NODES persistence — design choice between `Global.getSector().getPersistentData()` vs re-invoking `apply()` on each load. **T0 decides.**
- **R3** DR multiplicative stacking — Fortify × Death's Door × Immovable Object → invulnerability windows. Document & cap.
- **R4** `setMass`/mass override path — no `MutableShipStatsAPI` method. Likely needs `ship.getMass()` override or hullmod-equivalent. **T0 verifies.**
- **R5** `Stats.CR_MALFUNCION_RANGE` access — SiC pattern at `SCCRPluginImpl.java:395`. Confirm dynamic-stat key id matches in 0.98a-RC8.
- **R6** Targe `setHitpoints()` race — refactor to tick-based. May still conflict with vanilla repair if both write same frame.

## 6. Tasks

See `tasks.yaml`. 9 tasks. T0 (investigation) gates R1-R5 dependent tasks. ACTIVE_NODES fix (T6) prioritized — tracking for parallel work.

## 7. Boundaries

- **require_review:** `data/config/skills_overhaul/trees/pilot_defense.json`.
- **never_touch:** none.

## 8. Open Questions

- Damage interception API (R1)
- ACTIVE_NODES persistence design (R2)
- Mass override path (R4)
- 0.98a-RC8 Stats key for CR malfunction range (R5)

All resolved in T0.
