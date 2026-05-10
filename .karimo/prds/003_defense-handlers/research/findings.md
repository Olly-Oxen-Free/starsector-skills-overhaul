# Research Findings — defense-handlers

## Spec inventory

22 nodes across 6 tiers per `2026-04-30-pilot-defense-spec.md`. Audit pattern repeats — but defense has the **largest gap** of any pilot lane: 2 fully missing nodes, 5 effectively no-op, 11 partial.

## Status table

### TRULY MISSING (no JSON or no plugin behavior)
- **Impact Mitigation** (T1) — node absent from `pilot_defense.json`. Spec: `hitChance:0.50, damageReduction:0.25` (projectile/frag/explosive).
- **Carrier Supremacy** (T4) — handler registered (EffectRegistry:93) but no JSON node. Spec: assault/bomber +25% dmg/+20%HP, interceptor +30% speed, +15% replacement while flagship taking hits.
- **Flux Fortress** — JSON exists, registry routes to shared handler, **applyMods has no block**. Zero behavior.
- **Overwhelming Force** — same: no applyMods block. Zero behavior.
- **Unbreakable** — same: no applyMods block. Spec: per-armor-cell floor 20%→50%; needs damage interception.

### EFFECTIVELY MISSING
- **Iron Bastion** — IronBastionState exists; **applyMods has NO iron_bastion block** (DefenseCombatPlugin.java:89-111). PD rate/turn bonus not applied. ShipDestroyedListener not registered → fighter-kill heal dead.
- **Adamantine** — visual explosion only; spec EMP resist +25%, hull regen +8%, EMP AoE on discharge all missing.

### PARTIAL
- **Hardened Modules** — `weaponHPBonus`, `engineHPBonus`, `fighterBayHPBonus` log-and-skip
- **Damage Control** — `moduleRepairMult` skipped
- **Combat Stamina** — `crDeclineStart`, `crRegenInCombat` skipped
- **Battle Hardened** — `crMalfunctionThreshold` skipped (fix path: `Stats.CR_MALFUNCION_RANGE` dynamic stat per SiC `SCCRPluginImpl.java:395`)
- **Immovable Object** — ALL params skipped (`massMult`, `weaponHPBonus`, `engineHPBonus`, `collisionDamageDealt`) — effectively no-op
- **Fortify** — applyMods missing `shieldDamageTakenMult` (DefenseCombatPlugin.java:95-96 only hull+armor)
- **Fortress Shield** — hard-flux-spike-on-breach suppression missing
- **Defensive Configuration** — universal hull-spike cap missing; current stat values appear ad-hoc vs spec
- **Redundant Bays** — disabled-bay 75% capacity missing (only refit time done)
- **Shield Mastery** — hard→soft flux conversion 8%/s missing (only upkeep mult done)
- **Lead From the Front** — self `armorDamageTakenMult` missing (LeadFromTheFrontListener.java:42)
- **Derelict Fortifications** — −20% d-mod negatives missing
- **Targe of the Blooded** — regen via `setHitpoints()` is fragile, conflicts with vanilla repair tracker

### FULLY WORKING
Hull Conditioning, Reinforced Plating, Capacitor Banks, Death's Door, Targe (with caveat above)

## SiC reference patterns

Path: `/home/jayden-eppcohen/Games/Starsector/mods/Second-in-Command-2.0.0/src/second_in_command/scripts/SCCRPluginImpl.java`

- **Lines 334-336** — DR triple pattern: `getArmorDamageTakenMult / getHullDamageTakenMult / getShieldDamageTakenMult` via `modifyPercent`. Adopt for Fortify shield mult fix.
- **Line 395** — CR malfunction threshold: `stats.getDynamic().getStat(Stats.CR_MALFUNCION_RANGE)`. Adopt for Battle Hardened `crMalfunctionThreshold`.
- **Line 353** — `getAutofireAimAccuracy` — confirms no CR-level PD rate override; Iron Bastion needs direct `getBeamPDWeaponDamageMult`/`getBallisticPDDamageBonus`.

SiC Safety Overrides has no Java sources accessible — config-only mod.

## MutableShipStatsAPI defense surface

Coverage already in research output. Notable: `getMassBonus` doesn't exist — `massMult` requires non-stat path; `Unbreakable` per-cell armor floor requires `TakeDamageResultListener` or `DamageDealtModifier` (no stat path).

## Risks

- **R1 — DR stacking**: Fortify (5×5%) + Death's Door (×0) + Immovable Object compounds multiplicatively. Hull invulnerability windows possible. No guard.
- **R2 — `setHitpoints()` race** (TargeState regen, DefenseCombatPlugin:63): every-frame writes conflict with vanilla repair tracker. Refactor to 0.5s tick + repair-subsystem additive.
- **R3 — ACTIVE_NODES static-field session reload bug** (DefenseCombatPlugin:16): handlers add to static set at campaign-layer purchase. After save/reload, set is empty until next purchase event. Fix: persist via `Global.getSector().getPersistentData()` and re-invoke handler `apply()` on game load.
- **R4 — AdamantineState maxFlux capture** (DefenseCombatPlugin:44): captured at init; stale if Capacitor Banks reapplies mid-combat (rare but modded ships).
- **R5 — Unbreakable damage interception**: needs DamageListener-style API not currently used in mod. Investigation required.
- **R6 — Fortress Shield breach suppression**: needs flux-spike listener pattern, not present in codebase.
- **R7 — Compound DR + EMP resist** at Adamantine discharge: stacking with Combat Stamina + Hardened Modules EMP resist could near-cap to immunity.

## Recommended task slicing — 8 tasks

1. **T0** — Investigate damage-interception API (Unbreakable, Fortress Shield, Carrier Supremacy gates), `Stats.CR_MALFUNCION_RANGE` access path, mass override path, ACTIVE_NODES persistence design.
2. **T1** — Add applyMods() blocks for Iron Bastion, Flux Fortress, Overwhelming Force; complete Adamantine post-discharge buffs (EMP resist + hull regen). State machines exist; only plugin wiring needed.
3. **T2** — Add JSON nodes: `Impact Mitigation` (T1), `Carrier Supremacy` (T4). Wire Carrier Supremacy to existing handler; add `hitChance` + `damageReduction` to handler skipped-keys list.
4. **T3** — Implement Unbreakable per-cell armor floor + Fortress Shield hard-flux-breach suppression (both need damage/flux listeners from T0).
5. **T4** — Extend SOPilotedStatHandler for skipped defense keys: `weaponHPBonus`, `engineHPBonus`, `fighterBayHPBonus`, `massMult`, `collisionDamageDealt`, `crDeclineStart`, `crRegenInCombat`, `crMalfunctionThreshold`, `moduleRepairMult`, `hitChance`, `damageReduction`. Some need plugin-side iteration (weapon HP per slot).
6. **T5** — Shield Mastery hard→soft flux conversion + Fortify shieldDamageTakenMult fix + Lead From the Front self armorDamageTakenMult fix.
7. **T6** — Fix ACTIVE_NODES persistence (R3): persistent data store or apply-on-load hook.
8. **T7** — IronBastion ShipDestroyedListener registration + Targe regen refactor (R2: tick-based, repair-subsystem path) + Derelict Fortifications d-mod negative reduction + Redundant Bays disabled-bay capacity. Wire-up + smoke test.

## Cited sources

- `data/config/skills_overhaul/trees/pilot_defense.json`
- `src/skills_overhaul/effect/EffectRegistry.java:60-95` (defense block)
- `src/skills_overhaul/effect/handlers/defense/*.java`
- `src/skills_overhaul/effect/handlers/SOPilotedStatHandler.java:73-93` (skip log)
- `src/skills_overhaul/pilot/defense/DefenseCombatPlugin.java:16, :44, :63, :75, :89-111`
- `src/skills_overhaul/pilot/defense/{FortifyState,TargeState,DeathsDoorState,AdamantineState,IronBastionState,LeadFromTheFrontListener}.java`
- SiC: `/home/jayden-eppcohen/Games/Starsector/mods/Second-in-Command-2.0.0/src/second_in_command/scripts/SCCRPluginImpl.java:334-336, :353, :395`
- Spec: `docs/superpowers/specs/skills-overhaul/pilot/defense/2026-04-30-pilot-defense-spec.md`
