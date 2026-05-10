# Research Findings — secondary-system-hullmods

## Major audit correction

Audit flagged 8 hullmods as broken because CSV `script` column references `com.fs.starfarer.api.combat.BaseHullMod`. **This is intentional architecture, not a bug.**

Hullmods serve as **detection tags only**. Mechanics live in `SystemsCombatPlugin` (lines 274-281) which scans `ship.getVariant().hasHullMod("so_secondary_*")` and instantiates the appropriate state machine class. `BaseHullMod` is the correct vanilla no-op script class for tag-only hullmods.

## Architecture

```
data/hullmods/hull_mods.csv (rows 3-10)  ← detection tags + display text
        ↓ tag detected by
src/skills_overhaul/pilot/systems/SystemsCombatPlugin.java:274-281
        ↓ instantiates
src/skills_overhaul/pilot/systems/secondary/{SODomainOverclock, SOFluxDissolverBattery, SOReflectorProtocol, SODissonanceProjector, SODimensionalSkimmer, SOStasisEmitter, SOCombatSnapshot, SOFluxGhostProjector}.java
        ↓ implements
SecondarySystemHullmod interface (activate/advance/cooldown/getMaxCooldown)
        ↓ extends
SOSecondarySystem (template: tickCooldown, activeTimer, baseCooldown, activeDuration, onActivate)
```

Activation: `ExperimentalOverridesService.hasSecondaryHullmod(ship)` checks tag presence; player input (key-R or similar) routed by `SystemsCombatPlugin` advances state machines.

## State machine inventory (vs spec lines 150-157)

| Hullmod | LOC | Spec mech | State machine status |
|---------|-----|-----------|----------------------|
| SODomainOverclock | 40 | 4s ×1.6 timescale, 20s CD | ✓ matches spec — `engine.getTimeMult().modifyMult` for DURATION |
| SOFluxDissolverBattery | 66 | 300% vent + micromissile/150 flux, 25s | needs verification |
| SOReflectorProtocol | 80 | 3s reflect 240° arc + bonus on >800 reflected, 30s | needs verification (most complex) |
| SODissonanceProjector | 38 | 300 energy + 1200 EMP at 700u, scaled by flux ×0.5→×1.5, 30s | needs verification |
| SODimensionalSkimmer | 61 | 800u teleport + intangible 0.4s + 400 EMP on arrival, 25s | needs verification |
| SOStasisEmitter | 51 | 6s 700u −40% spd −20% wpn turn + self +25% DR, 32s | needs verification |
| SOCombatSnapshot | 63 | record, 8s revert (flux+pos, hull permanent), 55s | most complex revert path |
| SOFluxGhostProjector | 99 | autonomous clone 30% dmg / 1.5× taken / 20s, 45s | needs verification |

All 8 have proper `@Override`s for `baseCooldown`, `activeDuration`, `getDisplayName`, `advance`. No TODO/FIXME/stub markers in any.

## CSV row sanity

Rows 3-10 of `data/hullmods/hull_mods.csv`:
- All 8 use `script: com.fs.starfarer.api.combat.BaseHullMod` (correct — tag-only)
- Tags: `"player_only, so_secondary"`
- Cost: 5 OP, hidden 0 (display normally)
- All 4 size-cost slots = 0 (implies no per-size cost — verify intent)
- 4 tagged "Domain", 4 "Unknown" (faction style)

## Spec gaps to verify per state machine

Without deep code review per file, the following spec mechanics need verification — **task per state machine**:

1. **Domain Overclock** — confirmed matches spec.
2. **Flux Dissolver** — verify (a) emergency vent rate triple, (b) micromissile spawn per 150 flux dissipated, (c) micromissile damage 80 energy + 200 EMP.
3. **Reflector Protocol** — verify (a) projectile reflection (DamageDealtModifier?), (b) 240° arc check, (c) >800 reflected threshold, (d) +30% dmg / +20% ROF post-bonus.
4. **Dissonance Projector** — verify (a) AoE 700u radius, (b) 300 energy + 1200 EMP, (c) flux scaling formula ×0.5 at 0% → ×1.5 at 100%.
5. **Dimensional Skimmer** — verify (a) 800u teleport in thrust direction, (b) 0.4s intangibility (collision off), (c) 400 EMP at 500u arrival radius.
6. **Stasis Emitter** — verify (a) 700u radius, (b) all-ship slow application, (c) self DR mult.
7. **Combat Snapshot** — verify (a) flux + position record, (b) 8s timer or manual re-activate revert, (c) hull damage permanent (don't restore hull), (d) edge cases (target lost, ship destroyed during snapshot).
8. **Flux Ghost Projector** — verify (a) autonomous clone spawn (likely `engine.spawnShip` or fleet duplication), (b) clone weapon damage 30% (`getDamageDealtMult.modifyMult`), (c) clone hull damage taken 1.5× (`getHullDamageTakenMult`), (d) 20s lifetime, (e) inheritance of current ship state (variant, flux level?).

## Activation key wiring

`ExperimentalOverridesService.hasSecondaryHullmod()` checks for tag. Activation route from `SystemsCombatPlugin`: confirmed via `EXPERIMENTAL_OVERRIDES` ACTIVE_NODES gate (referenced in systems PRD). Need to verify keypress → `state.activate(ship, engine)` call site.

## Risks

- **R1** State machines untested — no `test/skills_overhaul/pilot/systems/secondary/` directory.
- **R2** Reflector projectile reflection — needs a damage-dealt-modifier or projectile-spawn-on-hit mechanism. Most complex; high risk of incomplete impl.
- **R3** Combat Snapshot revert path — race conditions if ship destroyed/captured during 8s window. Hull-damage-permanent flag must persist correctly.
- **R4** Flux Ghost clone — Starsector clone-spawn API limitations (`engine.getFleetManager` add ship). Clone AI behavior, fleet member ownership, wreck cleanup all need verification.
- **R5** ACTIVE_NODES save-reload bug (cross-cutting from defense PRD T6) — Experimental Overrides node must be active for any secondary to fire.
- **R6** State machine save persistence — if active timer / cooldown stored in state object instance, save-reload may reset to 0 cooldown (advantage glitch) or stuck-active (combat-lock). Verify state lifecycle.

## Recommended task slicing — 4 tasks (much smaller than expected)

1. **T0** — Read each of the 7 unverified state machine source files. Compare actual code to spec mechanic. Document deviations + missing sub-mechanics. Output: per-secondary verification table.
2. **T1** — Fix any spec deviations found in T0. Likely targets per research: Reflector reflection mechanism, Combat Snapshot edge cases, Flux Ghost clone lifecycle.
3. **T2** — Add unit tests where possible. State machines are mostly POJO-side (cooldown timer, active timer, flux scaling math) — testable via `java -ea`. Combat-engine-bound mechanics deferred to in-game smoke.
4. **T3** — Verify activation keypress → state.activate path. Confirm save-reload behavior (R5/R6). Smoke test all 8 in-game. CHANGELOG entry. CSV review (size-cost slots = 0 confirm intentional).

## Cited sources

- `data/hullmods/hull_mods.csv:3-10`
- `src/skills_overhaul/pilot/systems/secondary/{SecondarySystemHullmod, SOSecondarySystem}.java`
- `src/skills_overhaul/pilot/systems/secondary/SO{DomainOverclock, FluxDissolverBattery, ReflectorProtocol, DissonanceProjector, DimensionalSkimmer, StasisEmitter, CombatSnapshot, FluxGhostProjector}.java`
- `src/skills_overhaul/pilot/systems/SystemsCombatPlugin.java:274-281`
- `src/skills_overhaul/pilot/systems/ExperimentalOverridesService.java`
- Spec: `docs/superpowers/specs/skills-overhaul/pilot/systems/2026-04-30-pilot-systems-spec.md:150-157`
