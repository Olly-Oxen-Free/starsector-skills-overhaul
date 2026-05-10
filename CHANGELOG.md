# Changelog

## Unreleased — Defense lane spec gap closure (2026-05-10)

**Major fixes:**
- Iron Bastion (T3): +20% RoF + 25% weapon turn rate while taking active damage. Fighter-kill heal: +5% of dead fighter's HP via polling (no ShipDestroyedListener in 0.98a-RC8).
- Flux Fortress (T4): shield damage taken scales with flux level (0 → −30% at 100%).
- Overwhelming Force (T4): +25% flux dissipation + 20% hard-flux dissipation flat while advancing/shielded.
- Adamantine (capstone): post-discharge grants −25% EMP taken + +8% hull regen over 5s window; real EMP arc (300 energy + 1500 EMP); buffer recharges as flux climbs (was stale).
- Unbreakable (T4): per armor cell floor raised to 50% via DamageTakenModifier; 8s block + 15s cooldown per location.
- Fortress Shield (T2): on shield breach, hard-flux spike halved.
- Shield Mastery (T3): per-frame hard→soft flux conversion at 8%/s while shielded.
- Fortify (T2): now also reduces shield damage taken (was hull+armor only).
- Lead From the Front (T3): self bonus now applies armor DR (was hull-only).
- Targe of the Blooded (T3): hull regen refactored to 0.5s tick + accumulator (was per-frame setHitpoints, conflicted with vanilla repair).
- Derelict Fortifications (T4): −20% d-mod negatives via counter-buffs (maxSpeed, ROF, CR loss rate). Approximation — no per-d-mod stat key in API.

**Cross-lane fixes:**
- ACTIVE_NODES persistence: save/reload bug (static-set cleared on combat init) fixed via NodeApplyService.reapplyAllOwned() on game load. Affects all 4 pilot CombatPlugins (Defense, Gunnery, Maneuvering, Systems).
- SOPilotedStatHandler: 13 previously-skipped param keys now mapped — engineHPBonus, crMalfunctionThreshold, accelDecelBonus, maneuverBonus, overloadRecoveryReduction, phaseEntryFluxReduction, maneuverabilityBonus, ventRateBonus, energyFluxReduction, energyDamageBonus, energyRangeBonus, allWeaponTurnRateBonus, sensorProfileReduction, shieldRotationSpeedBonus, shieldRaiseSpeedBonus.

**DR cap guard:** Combined hull-damage-taken multipliers from Fortify × Death's Door × other sources are now clamped to ≥5% (95% max DR) to prevent invulnerability.

**Orphan skills (off-tree):** Impact Mitigation + Carrier Supremacy implemented but not in tree (5-slot grid full). Grant via Console Commands:
```
runcode skills_overhaul.console.OrphanSkillGranter.grant("impact_mitigation");
runcode skills_overhaul.console.OrphanSkillGranter.grant("carrier_supremacy");
runcode skills_overhaul.console.OrphanSkillGranter.revoke("<name>");
```
Persists in MemoryAPI; re-applied on game load.

**Deferred:** Redundant Bays disabled-bay 75% capacity (needs deeper FighterLaunchBayAPI work); massMult (no MutableShipStatsAPI surface).

## v0.2 — Character tab UI takeover (2026-04-20)

- Replaced v0.1 dialog stopgap with full character-tab takeover.
- New 3-tab panel: PILOT / COMMANDER / ADMIN.
- 48×48 node buttons on a 3-lane × (L/M/R) slot grid per tier.
- Restored merge nodes with cross-lane prereqs.
- Pending-purchase + confirm flow (vanilla parity). Esc clears.
- GL_LINES prereq edge drawing with state colors (teal/amber/gray/stippled-green).
- Schema: added `slot` field + `parentLanes` for merge nodes.
- Tests: NodeSlotTest, MergeNodeTest, PendingPurchaseTopoSortTest added.
PrereqValidatorTest + TreeLoaderRealJSONTest extended.

## 0.1.0 (unreleased)

- v1: three-tree skill overhaul (Pilot/Commander/Admin), activity-gated XP, custom UI, save-compat migration.