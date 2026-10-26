# Changelog

## Unreleased — Defense lane spec gap closure (2026-05-10)

**Skills panel:**
- Skills hotkey: `K` by default (`ui_hotkey` in `data/config/skills_overhaul.json`) opens the skills dialog. Used under Second-in-Command or when the character tab cannot be replaced; a one-time campaign message names the key. `CharacterTabReplacer` is removed; `CharTabHost` mounts the new view in the CHARACTER tab.

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

## Unreleased — Gunnery, Maneuvering, Systems, Suppressor (2026-05-10)

**Gunnery (PRD 001):**
- Surgical Strikes (T2): target weapon + engine damage taken mults +30%.
- Twist the Knife (T3): target <60% hull or retreating → +25% attacker dmg, target speed ×0.85, target ZFB removed.
- Dead Eye (T3): +10% non-missile range, perfect autofire (modifyFlat 1.0); −10% flux beyond 750u.
- Weapons Discipline (T4): malfunction frequencies zeroed; flux costs ×0.80.
- Ordnance Reserve (T1, was broken): nonMissileAmmoCapBonus + ammoRegenBonus now wired via getBallisticAmmoBonus / getEnergyAmmoBonus / getMissileAmmoRegenMult.

**Maneuvering (PRD 002):**
- Stealth Adaptation (T1): timescale +0.15 at >80% speed (normal); +0.25 while phased.
- Phase Entry (T1, phase): phaseEntryFluxReduction:0.25 wired via SOPilotedStatHandler.
- Engine Flux Tuning (T1): expanded to accelDecelBonus + maneuverBonus + overloadRecoveryReduction (was accelBonus only).
- Ghost Protocol (T3, phase): phase upkeep cost ×0.10 while phased.
- The Untouchable (capstone): permanent ZFB while firing or accelerating; phase ships → zero phase activation/upkeep costs.

**Systems (PRD 004):**
- Bug fixes: TargetPainter damage delta tracking (was always 0); Red Line CR bonus wiring; Void Strike ordnance speed wiring; SynapticRelay.remove() leak (so_neural_uplink hullmod was permanent on respec).
- EMP Specialist (T2): +20% energy weapon damage.
- Strike Coordination (T2): allied fighters in fleet get +25% dmg / +20% hull.
- Phase Mastery (T4): max speed ×1.5 while phased (counters vanilla penalty); phase-exit burst spawns 300 energy + 600 EMP scaled by exit flux.
- Carrier Command (T4): +20% fighter replacement rate.

**Vanilla Suppressor (PRD 007):**
- Hardened: explicit `CharacterStatsSkillEffect.unapply()` invocation on each non-so_ skill before setSkillLevel(0). Spec §7.2 mandates explicit unregister; defensive — no harm if vanilla already unregisters via setSkillLevel.

**Multi-tree UI (PRD 005):**
- Lane header label bug fixed (was showing tree name on every lane).
- Metrics.laneCount semantic split (added explicit treeCount + isMultiTree()).
- WIP multi-tree refactor + 8 secondary-system hullmod CSV entries committed (BaseHullMod is intentional detection-tag pattern; mechanics live in 8 SOSecondarySystem state machines + SystemsCombatPlugin wiring).

**Secondary System hullmods (PRD 006):**
- Verification: all 8 state machines (DomainOverclock, FluxDissolverBattery, ReflectorProtocol, DissonanceProjector, DimensionalSkimmer, StasisEmitter, CombatSnapshot, FluxGhostProjector) functional; mechanics live in plugin via hullmod-tag detection. CSV BaseHullMod confirmed intentional.

**Deferred to follow-up:**
- Phase Entry first-phase-free flag (vanilla phase-cost intercept needed)
- Untouchable 30% projectile negation (DamageTakenModifier + probability gate)
- Evasive Action missile-proximity bonus
- EMP Specialist per-EMP-hit weapon turn debuff (per-shot detection)
- Strike Coordination bomber-hit target debuff
- Carrier Command bomber-target-kill teleport-home
- Active-basics displacement modal (PRD 005 T3)
- Multi-tree unit tests (PRD 005 T4-T5)
- VanillaSuppressor empirical in-game test (PRD 007 T0)
- Redundant Bays disabled-bay 75% capacity (PRD 003 follow-up)

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