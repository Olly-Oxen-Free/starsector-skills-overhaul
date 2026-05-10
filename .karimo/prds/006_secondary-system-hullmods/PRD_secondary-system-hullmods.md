# PRD: Secondary System Hullmods — Verification + Polish

- **Slug:** secondary-system-hullmods
- **Created:** 2026-05-10
- **Status:** draft
- **Spec source:** `docs/superpowers/specs/skills-overhaul/pilot/systems/2026-04-30-pilot-systems-spec.md:150-157`

## 1. Summary

Audit miscount **third major correction**: the 8 secondary-system hullmods CSV `BaseHullMod` entries are **intentional**. Hullmods are detection tags; mechanics live in 8 state machine classes (all exist, no TODOs). Real PRD scope: verify each state machine matches spec, fix any deviations, add tests, confirm activation + save-reload behavior.

## 2. Why

Original audit flagged this as a P1 critical gap. Reality: state machines exist, plugin wires them correctly, CSV pattern is correct vanilla Starsector idiom for tag-only hullmods. Risk shifted from "implement 8 systems" to "verify 8 systems and fix any deviations".

## 3. Scope

### Verification (per state machine)
- SODomainOverclock (confirmed match)
- SOFluxDissolverBattery, SOReflectorProtocol, SODissonanceProjector, SODimensionalSkimmer, SOStasisEmitter, SOCombatSnapshot, SOFluxGhostProjector

### Likely fixes
- Reflector projectile reflection mechanism (most complex, high risk)
- Combat Snapshot revert edge cases
- Flux Ghost clone lifecycle
- Activation keypress wiring confirmation
- Save-reload state persistence (cross-cutting w/ defense PRD T6)

### Tests
- POJO unit tests for cooldown timers, flux scaling math, state transitions
- In-game smoke for engine-bound mechanics

### Out of scope
- New secondary systems
- CSV restructure
- Experimental Overrides node mechanic itself (covered in systems PRD)

## 4. Success Criteria

- All 7 unverified state machines match spec or deviations documented + fixed.
- 5+ POJO unit tests pass under `java -ea`.
- All 8 secondaries activate in-game via R-key (or confirmed key).
- Save-reload preserves cooldown state OR resets cleanly (no exploit).
- `./build.sh` + test pass.

## 5. Risks

- **R1** Reflector reflection requires `DamageDealtModifier` or projectile-spawn-on-hit. May not be fully impl.
- **R2** Combat Snapshot 8s revert race conditions (ship destroyed/captured during window).
- **R3** Flux Ghost clone via `engine.getFleetManager` — clone AI, fleet ownership, wreck cleanup.
- **R4** ACTIVE_NODES save-reload (defense PRD T6 cross-cutting) — Experimental Overrides node must be active.
- **R5** State machine instance state persistence across save-reload — exploit risk if cooldown resets.

## 6. Tasks

4 tasks. Smaller PRD than other lanes.

## 7. Boundaries

- **require_review:** `data/hullmods/hull_mods.csv` (gated path; uncommitted modifications already present).
