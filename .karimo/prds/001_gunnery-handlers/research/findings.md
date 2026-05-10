# Research Findings — gunnery-handlers

## Correction to audit premise

Audit claimed 15/18 gunnery effects had no behavior. Actual state per `GunneryCombatPlugin.applyMods()` (lines 74-190):

**Working (13/18):** lock_on, hair_trigger, efficiency, target_sol, overload_hunter, threat_acq, berserker, glass_cannon, dragon_slayer, point_blank, bounty_hunter, wolfpack, killing_machine

**Truly unimplemented (4):** `SO_gunnery_surgical`, `SO_gunnery_twist_knife`, `SO_gunnery_dead_eye`, `SO_gunnery_weapons_discipline`

**Silently broken (1):** `SO_gunnery_ordnance_res` — `nonMissileAmmoCapBonus` param logged-and-skipped at `SOPilotedStatHandler.java:78-80`. No ammo cap change occurs.

## Architecture (two-layer)

1. **`EffectHandler.apply/remove(nodeId, params)`** — called at node purchase/refund. Static handlers mutate `getPlayerShip().getMutableStats()` directly. Conditional handlers only add/remove `nodeId` from `GunneryCombatPlugin.ACTIVE_NODES`.
2. **`GunneryCombatPlugin extends BaseEveryFrameCombatPlugin`** — `applyMods()` reads `ACTIVE_NODES` each frame, applies/removes conditional `MutableStat` mods. State machines (`LockOnState`, `ThreatAcquisitionState`, `KillingMachineState`, `BountyHunterState`) are pure POJOs, testable with `java -ea`.

Registry: `EffectRegistry.java:99-120`. Static `HashMap.put(typeId, handler)`.

## Stat API (MutableShipStatsAPI)

- Damage: `getBallisticWeaponDamageMult/getEnergyWeaponDamageMult/getMissileWeaponDamageMult/getBeamWeaponDamageMult` → `modifyMult(nodeId, x)` (compounds multiplicatively across sources).
- Range: `getEnergyWeaponRangeBonus` etc. → `modifyPercent`.
- Accuracy: `getAutofireAimAccuracy` → `modifyMult` (clamped ≤1).
- Armor: `getArmorDamageMult`. Hull-taken: `getHullDamageTakenMult`.
- Convention in this codebase: `modifyMult` for damage, `modifyPercent` for range/turn-rate.
- Per-flagship only: `Global.getCombatEngine().getPlayerShip()`. Always guard `if (ship == null) return`.

## The 5 nodes — suggested mechanics

| id | Tier | Prereq | Mechanic (suggested) | Surface |
|----|------|--------|----------------------|---------|
| surgical | T2 | lock_on | Bonus armor damage OR reduced recoil | `getArmorDamageMult` / `getRecoilPerShotMult` |
| dead_eye | T3 | target_sol | Autofire accuracy + range | `getAutofireAimAccuracy` + range stats |
| twist_knife | T3 | threat_acq | Bonus dmg vs damaged hulls (hull% threshold) | applyMods conditional on `target.getHullLevel()` |
| weapons_discipline | T4 | glass_cannon | Reduce GlassCannon hull-dmg-taken penalty OR ROF at low flux | applyMods flux-conditional or `getHullDamageTakenMult` |
| ordnance_res | T1 | lock_on | +40% non-missile ammo cap — NO DIRECT API | EveryFrameCombatPlugin iterating `ship.getAllWeapons()` at combat start, calling `weapon.setAmmo()` |

## Recommended task slicing — 4 tasks

1. **Static stat handlers**: `surgical` + `dead_eye` (Group A — new handler classes, register, unit-testable design choices)
2. **Plugin conditional blocks**: `twist_knife` + `weapons_discipline` (Group B — add `if (ACTIVE_NODES.contains(...))` blocks in `GunneryCombatPlugin.applyMods()`)
3. **Ammo cap**: `ordnance_res` (Group C alone — design decision required: setAmmo on combat start vs frame plugin; risk of fighting engine init)
4. **Wire-up + smoke test**: EffectRegistry edits, in-game verification pass for all 5 nodes

## Risks

- `GunneryCombatPlugin.onKill()` call-site not visible in read files. Verify wiring before expanding kill-reactive patterns.
- Ammo cap has no `MutableShipStatsAPI` method — implementation will be plugin-based. Highest-risk task.
- Compound `modifyMult`: GlassCannon (+30%) × surgical (+X%) compounds, doesn't add. Pick multiplier values with this in mind.
- Static handlers run at purchase time when `getPlayerShip()` is null — must guard. Stats applied at purchase appear to persist via ship's `MutableStats`; confirm.
- No existing handler test patterns (handlers depend on game runtime). State-machine POJOs ARE testable.

## Cited sources

- `src/skills_overhaul/effect/EffectHandler.java:11`
- `src/skills_overhaul/effect/EffectRegistry.java:99-120`, `:228`
- `src/skills_overhaul/effect/handlers/gunnery/SOGunneryGlassCannonHandler.java:10-39`
- `src/skills_overhaul/effect/handlers/gunnery/SOGunneryCombatScriptHandler.java:8-18`
- `src/skills_overhaul/effect/handlers/SOPilotedStatHandler.java:46-98`
- `src/skills_overhaul/pilot/gunnery/GunneryCombatPlugin.java:74-190`, `:192`
- `data/config/skills_overhaul/trees/pilot_gunnery.json`
- MutableShipStatsAPI: https://fractalsoftworks.com/starfarer.api/com/fs/starfarer/api/combat/MutableShipStatsAPI.html
