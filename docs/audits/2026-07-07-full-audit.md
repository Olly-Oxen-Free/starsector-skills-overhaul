# Skills Overhaul — Full Audit (2026-07-07)

Five parallel subsystem audits: core progression, effect handlers + pilot trees,
hidden trees (commandant/corsair/zealot), hidden trees (artificer/collective/barevents/support),
and the UI/suppression hack layer.

**TLDR:** Campaign math is solid and well-tested, but the combat effect layer is
largely broken in-game — most pilot + zealot combat skills do nothing in battle.
One purchase-flow bug blocks buying any node above tier 0 in the live UI. Both
big hacks (char-tab hijack, vanilla suppression) are the right approach — keep,
harden. Statics-leaking-across-saves is systemic.

## Critical — fix first

1. **Purchase flow broken above tier 0.** `level/GameSpend.java:46-58` —
   `snapshot(mem)` copies point pools but not `$skills.purchased.*` flags.
   `PrereqValidator.canPurchase` then sees an empty owned-set → every node with a
   prereq fails ("purchase failed") in the live UI. Untested because `GameSpend`
   needs `Global.getSector()`. Fix: copy purchased flags into the snapshot or
   validate against real MemoryAPI.

2. **`ACTIVE_NODES.clear()` in combat `init()` kills all script-gated skills.**
   Same bug in `GunneryCombatPlugin:33`, `ManeuveringCombatPlugin:34`,
   `SystemsCombatPlugin:42`, `DefenseCombatPlugin:37`, `ZealotCombatPlugin:30`.
   Set is populated only at purchase/game-load; every battle start wipes it and
   nothing refills it. Entire gunnery/maneuvering/systems/defense/zealot combat
   lanes are inert. Fix: remove the `clear()` (set is persistent state) or
   re-seed from `SkillMemory` in `init()`.

3. **DefenseCombatPlugin never registered.** Missing from
   `data/config/settings.json` plugins block. Whole defense lane has no plugin
   even after #2 is fixed.

4. **`SO_piloted_stat` passives (23 nodes) never apply.**
   `SOPilotedStatHandler.apply()` reads `Global.getCombatEngine().getPlayerShip()`
   — null at purchase and load time; no combat plugin re-applies. Reinforced
   Plating, Capacitor Banks, etc. are all no-ops. Fix: apply per-combat from a
   plugin — repurpose the dead `SkillsOverhaulCombatPlugin` (currently logs
   "applied 0 effects" every battle) as the applier.

5. **`onKill()` entry points never invoked.** `GunneryCombatPlugin:266`,
   `ManeuveringCombatPlugin:291` — no caller. Killing Machine, Bounty Hunter,
   The Ace stacks dead even after #2. Systems lane polls kills itself
   (`detectKills()`) — copy that pattern.

6. **Systemic static leak across save loads.** `NodeApplyService.reapplyAllOwned()`
   is additive-only — never clears statics first. JVM survives save loads, so
   load save A → load save B carries over: `ForgeRecipeRegistry.UNLOCKED`,
   `FreeDeployQuotaTracker.BONUSES`, all `ACTIVE_NODES` sets,
   `CommandFieldService.PARAMS`, `SupplyBufferService.CAP_MULTIPLIERS`,
   `CollectiveActiveNodes`. Fix once centrally: `resetAll()` pass at the top of
   `reapplyAllOwned()`.

7. **No uninstall path — vanilla skills permanently destroyed.**
   `VanillaSuppressor.restoreAll()` has zero callers. Player removes mod → save
   loads with all vanilla skills at 0, forever. Fix: console command + README
   removal procedure; consider LunaLib toggle.

## High — exploits and wrong behavior

- **Supply buffer siphons purchased supplies.** `SupplyBufferListener:37-45`
  captures any positive supply delta (incl. market buys) into the invisible
  buffer, no skill gate, and upkeep never draws from it. Player pays for 100
  supplies, gets ~80 usable.
- **Privateer income faucet.** `PrivateerLifecycleScript:47-60` pays 30% of
  `getNonPlayerSide()` base cargo value on every off-screen win — wrong side,
  wrong basis, unbounded. Also duplicate privateer fleets when player changes
  system (`PrivateerFleetService.isCommissioned` only scans current location).
- **Officer Imprint = uncapped permanent officer stat points** per 12 supplies /
  7 days; recipe description says "copies officer skills to fighter wing" — a
  different effect entirely (`forge/devices/OfficerImprintDevice.java:12-18`).
- **Distributed Consciousness reads `getCaptain().getId()` not `getAICoreId()`**
  (`SODistributedConsciousnessListener:69-73`) — alpha/beta core bonuses never
  trigger (always generic +2%).
- **Iron Doctrine / Field Commands params leak into static
  `CommandFieldService.PARAMS`** mid-battle, never flushed at battle end
  (`CommandantCombatPlugin:46-54`, `FieldCommandsService.resetForBattle:160`) —
  buffs persist across battles and saves.
- **Alpha Storm shelter hardcoded at (0,0)** — `AlphaStormBattleScript:34`,
  exempts anything near map origin, both sides, regardless of player assets.
- **Commander XP loses first post-load engagement** — `CommanderXPListener:22-23`
  baseline resets to −1 each load; first engagement establishes baseline and its
  XP is dropped.

## Half-wired / dead features (silent no-ops behind live tooltips)

| Feature | Where | State |
|---|---|---|
| Crusade "Zeal cap +50" | `CrusadeAbility:39` | flag set, never read; `ZealService.registerCapBonus` never called |
| `CrusadeService.rivalDestroyed()` | zealot | never called — crusade never resolves if rival faction dies |
| Automated-ship DP cap raise | `AutomatedShipPointService` | `getRaisedCap()`/`computeOverCap()` zero callers — both nodes do nothing |
| Beam Specialization debuff | `ArtificerCombatPlugin:140-161` | self-targeting (`onBeamHit(s, s, …)`), results never read, classifies any non-missile as beam |
| Comm Jammer / Sensor Beacon | forge devices | spawn inert permanent map entities; expiry keys never read |
| `pulsar_array`, `magnetic_field_emitter` recipes | `ForgeRecipeRegistry:89-94` | no unlock path — unreachable |
| Marching Grace | `IronDoctrineState` | computed, never read; mixes days and combat-seconds |
| Commander XP multiplier API | `SkillsOverhaulAPI` | `getCommanderNumericMultiplier()` never consumed — mirror is 1:1 |
| Precision Strike, Phase Resonance merge | `EffectRegistry:187,189` | marker handler only, no plugin logic |
| `forge/devices/ForgeCampaignBuffsTicker` | whole class | dead duplicate of `supply/` version — delete |
| `capstoneCost/mergeCost/nodeCost` config | `Config.java:8-10` | parsed, never read — misleads tuners |

## Hack verdicts

| Hack | Verdict |
|---|---|
| **CharacterTabReplacer** (reflection tab hijack) | **Keep + harden.** No API exists for char-tab replacement; same technique SiC uses. Harden: handle resolution change while installed (panel size computed once at install); detect SiC by mod-id, not `getSimpleName().contains("SCSkillMenu")`; cache reflection `Method` handles; log game version on reflection failure; downgrade per-open INFO spam to debug. Escape hatch if ever needed: custom intel-screen skills UI (zero reflection) as fallback when the crawl fails — UX tradeoff (not on `C` key). |
| **VanillaSuppressor** (runtime zeroing) | **Keep + add reverse gear.** Most save-compatible option; `skill_data.csv` blanking rejected (breaks save compat). Must-fix: expose `restoreAll()` (critical #7); re-stash when suppressed level rises (stash-once guard keeps stale values, `VanillaSuppressor:35-37`); verify fleet-effect detach in-game — currently trusted on a comment. |
| **ReflectionUtils** classloader dodge | **Keep** — required by Starsector's script classloader. Add method-handle caching (`ReflectionUtils:159-176` re-crawls per call). |
| **LunaLib** | Adopt for config toggles only (suppression on/off, UI replace on/off). Cannot replace the core hack. |

## Cleanup / hygiene

- Mod repo has **no `.gitignore`**; `build/`, `jars/classes/**.class`,
  `build_output.*`, 3 identical `javap` dump txts committed. Delete + ignore.
- Workspace root: `com/` = extracted obfuscated game bytecode (do not commit —
  copyrighted; move to `reference/`); `.attach_pid*` = JVM attach residue;
  stray `skills_overhaul/ui/CharacterTabReplacer.class` deletion unstaged.
- Pervasive `catch (Throwable ignored)` hides every silent no-op above — log
  warns in catches; validate hardcoded hull IDs (`"tesseract_small"` has a TODO)
  at `onApplicationLoad`.
- Perf: `XPCurve.levelFromXP` O(cap²) 3×/sec (`LevelReconciler`);
  `CommandFieldService.computeTotalsForShip` O(ships²) per 0.5s;
  `VoltaicArcEmitter`/`ECMTracker` per-frame full-fleet scans + allocation churn;
  `InfamyDailyTicker` unthrottled per-frame memory reads.
- Duplication risk: services keep parallel `Map` + `MemoryAPI` overloads — this
  drift pattern caused critical #1. Consider adapting MemoryAPI to a Map view so
  logic lives once.
- Uncommitted diffs reviewed: `NexIntegration` change is correct (fixes
  sector-vs-market memory mismatch); `PilotXPTracker` change good except
  pointless try/catch around `isCombatOver()`; `OrphanSkillGranter.fireCrisis`
  fine. Nex conquest detection still hinges on unversioned `$nex_marketCapturedBy`
  key; re-conquered player colonies get no XP (one-shot `colony_seen` latch).

## Balance / misc quick hits

- `CraftFuelAbility`: 20 supplies → 1 fuel — strict loss, ratio surely wrong.
- Retreated ships counted as kills/losses (Zealot Avenge/Tribute,
  `ZealotCombatPlugin:119-126, 230-238`).
- Twist the Knife debuff sticks to old target on target switch
  (`GunneryCombatPlugin:199-217`); Lead From The Front buff never removed when
  ally leaves range (`LeadFromTheFrontListener:24-38`);
  `SystemsCombatPlugin.prevShieldFlux` shared by Reactive Systems and Target
  Painter → garbage when both active.
- Secondary-system key hardcoded `R` (`SystemsCombatPlugin:37`), no rebind.
- Alpha Storm time-skip billed at final-day intensity for all skipped days
  (`AlphaStormCampaignTicker:32-62`).
- Duplicate battle-device deploy silently destroys second item
  (`ForgeBattleHook:17-34` boolean pending, not counter); free-quota assignment
  HashSet-order nondeterministic (`ForgeBattlePlugin:26-39`).
- Fighter respawns never re-receive Wide-Range Propagation buffs
  (`FighterInheritanceTracker` permanent seen-set); post-trigger reinforcements
  miss Distributed Consciousness fleet bonus.
- `SupplyBufferRenderer` registered persistent + re-added each load → stacked
  render listeners (`SupplyBufferRenderer:22`).
- `LevelReconciler` reimplements `LevelService.reconcileFromXP` inline — tested
  path is not the shipped path.
- `RespecService.respecTree` refunds all record keys into the single passed tree
  — correct only via caller pre-filtering.
- Dead code: `PilotXPMath.effectiveByMode`, `DamageLedger.getEntry/entries`,
  `CrusadeNPCStub` no-op listener registration + throwaway instantiation,
  `SkillsOverhaulCombatPlugin` legacy stub + `piloted_*` orphan handlers,
  4 orphan gunnery registry keys (`EffectRegistry:103-106`), `Node.java` legacy
  accessors (`getPrereqs()` flattens OR-clause semantics).
- DR cap guard reads its own prior-frame output (`DefenseCombatPlugin:151-161`);
  inconsistent phase-ship detection helpers; Strike Coordination ignores its
  range spec.

## Recommended fix order

1. GameSpend snapshot (nothing purchasable = mod unusable)
2. Combat wiring cluster #2–#5 (one PR — makes skills actually work)
3. Static reset pass #6 (one central fix)
4. `restoreAll()` trigger + README uninstall procedure
5. Exploits (supply siphon, privateer income, officer imprint)
6. Half-wired features — decide each: wire or cut
7. Hygiene sweep (.gitignore, cruft, logging, perf)
