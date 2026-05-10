# Research Findings — maneuvering-handlers

## Audit correction

Audit claimed 15/15 maneuvering effects no-op. **Reality:** 16/21 nodes WORKING via `ManeuveringCombatPlugin.applyMods()`. Pattern same as gunnery — registry "shared no-op" handler is actually a plugin-activation marker (`SOManeuverCombatScriptHandler.java:9-11`).

## Status table (21 nodes)

| Node | Status | Notes |
|------|--------|-------|
| drift_instinct | PARTIAL | maxSpeedBonus works; `engineHPBonus:0.20` SILENTLY SKIPPED (`SOPilotedStatHandler.java:82`) |
| evasive_action | WORKING | turnRateBonus |
| running_guns | WORKING | weaponTurnRate + spreadReduction |
| engine_tuning | WORKING* | accelBonus only — asymmetric (no decel/turnAccel) |
| **stealth_adapt** | **MISSING** | empty `{type:SO_piloted_stat}` — no params, no behavior |
| **phase_entry** | **MISSING** | empty effect — `phaseEntryFluxReduction` listed in skip table |
| vent_sprint | WORKING | Plugin:82-88 |
| deep_vent | WORKING | Plugin:91-95 (VentTracker) |
| knife_fighter | WORKING | Plugin:98-109 (300u proximity) |
| reactive_drive | WORKING | Plugin:112-124 |
| phase_dancer | WORKING | Plugin:127-133 + PhaseStateTracker |
| crossfire | WORKING | Plugin:136-148 |
| zfm | WORKING | Plugin:73-79 + ZFBThresholdManager |
| kinetic_cooling | WORKING | Plugin:151-163 |
| ace | WORKING | Plugin:166-173 + AceKillStacker |
| **ghost_protocol** | **MISSING** | Plugin:236 comment "future — placeholder" |
| perfect_timing | WORKING | Plugin:176-190 |
| battle_focus | WORKING | piggybacks perfect_timing buff |
| flanking | WORKING | Plugin:193-204 |
| tactical_break | WORKING | Plugin:207-233 |
| untouchable | WORKING* | only raises ZFB threshold to 50%; capstone arguably needs second mechanic |

## Missing node mechanics (suggested)

- **stealth_adapt** (T1) — sensor profile reduction. Add `sensorProfileReduction:0.15` param; `getSensorProfile().modifyMult(id, 1f-v)` case in `SOPilotedStatHandler`. Or conditional-while-strafing in plugin.
- **phase_entry** (T1) — `phaseEntryFluxReduction:0.25` param; `getPhaseActivationFluxLevel().modifyMult(id, 1f-v)` in handler. Already in skipped-param list (line 89).
- **ghost_protocol** (capstone phase branch) — implement inside existing `isPhased` block in plugin. Suggest: `getHullDamageTakenMult` reduction while phased, OR bonus damage on phase exit.

## Partial fixes

- **drift_instinct.engineHPBonus** — no direct engine-HP API. Map to `getEngineDamageTakenMult().modifyMult(id, 1f-v)`. Add case to `SOPilotedStatHandler`.
- **engine_tuning.accelBonus** — currently only `getAcceleration()`. SiC pattern (`SCCRPluginImpl.java:319-329`) bundles accel + decel + turnAccel. Either expand existing param or add `decelBonus`/`turnAccelBonus`.

## SiC reference patterns

Path: `/home/jayden-eppcohen/Games/Starsector/mods/Second-in-Command-2.0.0/`

- `SCCRPluginImpl.java:319-329` — full maneuvering package application: `getAcceleration() / getDeceleration() / getTurnAcceleration()` together via `modifyPercent`. **Adopt for engine_tuning expansion.**
- All movement stats use `modifyPercent(id, pct)` then `unmodify(id)`.

## API surface gaps in current SO

Unused but available: `getDeceleration`, `getTurnAcceleration`, `getZeroFluxSpeedBoost` (distinct from MaxSpeed mult).

## Risks

- **Compounding** — `vent_sprint + zfm + phase_dancer + kinetic_cooling` all `modifyMult(getMaxSpeed)`. No cap. Consider floor/ceil guard.
- **Plugin vs handler placement** — static stats → `SOPilotedStatHandler` (equip-time). Conditional/per-frame → `applyMods()`. Do not mix.
- **Save compat** — adding params to existing live nodes only re-applies on respec/reload. Acceptable.
- **ghost_protocol** — currently sold as capstone but does nothing. Any impl must be additive (won't surprise existing buyers negatively).
- **untouchable** as capstone with one effect — design decision needed (defer to PRD or add second mechanic).

## Recommended task slicing (5-6 tasks)

1. **T0** — Fix `drift_instinct.engineHPBonus` in `SOPilotedStatHandler` (engineDamageTakenMult). Low complexity, no design.
2. **T1** — Implement `stealth_adapt` (decide: static sensor profile vs conditional). Add JSON params + handler case.
3. **T2** — Implement `phase_entry` via existing `phaseEntryFluxReduction` skip-target. Add params + handler case.
4. **T3** — Implement `ghost_protocol` in plugin's `isPhased` block. Design decision required.
5. **T4** — Expand `engine_tuning` to cover `getDeceleration` + `getTurnAcceleration` (SiC pattern).
6. **T5** — Decide + implement second mechanic for `untouchable` capstone (or document as intentional). Optional / design-only.
7. **T6** — Wire-up + smoke test (registry + build + load logs).

## Cited sources

- `src/skills_overhaul/effect/EffectRegistry.java` (maneuvering block)
- `src/skills_overhaul/effect/handlers/maneuvering/SOManeuverCombatScriptHandler.java:9-11`
- `src/skills_overhaul/effect/handlers/SOPilotedStatHandler.java:73-93`
- `src/skills_overhaul/pilot/maneuvering/ManeuveringCombatPlugin.java:73-262`
- `src/skills_overhaul/pilot/maneuvering/ZFBThresholdManager.java:8-9`
- `src/skills_overhaul/pilot/maneuvering/PhaseStateTracker.java`, `VentTracker.java`, `AceKillStacker.java`
- `data/config/skills_overhaul/trees/pilot_maneuvering.json`
- SiC: `/home/jayden-eppcohen/Games/Starsector/mods/Second-in-Command-2.0.0/jars/src/.../SCCRPluginImpl.java:319-329`
