# Admin Trees (Warden / Merchant / Prospector / Steward) — Implementation Plan

> **For agentic workers:** fresh subagent per task; unit tests + one commit per task. Data files
> have no compiler — every JSON change is verified by `./build.sh test` (tree-load tests) **and** an
> in-game load with `starsector.log` checked. No completion claim without the in-game check.

**Spec:** `docs/superpowers/specs/skills-overhaul/admin/2026-04-25-admin-trees-design-v3.md` (workspace
root repo). Supersedes the obsolete Task 10.3 / 10.4 of
`docs/superpowers/plans/skills-overhaul/2026-04-18-skills-overhaul-plan.md` (Industrialist /
Statesman / Autocrat lanes, `market_income_mult` etc. — never built; do not resurrect).

**Status:** plan only. Nothing here is implemented. §11 lists spec items that need a user decision
before their phase starts — **do not start a node flagged `DECIDE` until the user has answered**.

**Goal:** replace the four admin placeholder trees (`data/config/skills_overhaul/trees/admin_*.json`,
T0 + T5 stubs only) with the 88 v3 nodes, built from a small set of shared market-effect handlers,
with every mod integration soft and reflection-free.

---

## 0. Ground truth checked for this plan (2026-10-02)

Facts verified against the live code / jars, not the spec:

| Fact | Source | Consequence |
|---|---|---|
| Admin tab already loads 4 trees from `admin_index.json`; each tree has only `admin_<tree>_t0` / `admin_<tree>_t5` placeholders with **no `effects`** | `data/config/skills_overhaul/trees/admin_*.json` | Keep those two ids per tree (saves may own them). Add 20 nodes per tree. |
| `VanillaSuppressor` zeroes `industrial_planning`, `hypercognition`, `space_operations`, `planetary_operations`, `colony_management` | `src/skills_overhaul/suppress/VanillaSuppressor.java:44-45` | The admin trees are the **only** colony skills the player has. Balance target = "at least vanilla admin power at equal points". See §11 D1. |
| Node-effect pipeline: `NodeEffectApplier` → `EffectRegistry.get(type).apply(nodeId, fx)`; T0 has `base` (always) and `core` (only while the T0 is in the 2-slot active set, `ActiveLaneService.MAX_ACTIVE = 2`) channels | `effect/NodeEffectApplier.java`, `basics/ActiveLaneService.java` | Spec "Ribbon (when inactive)" = `base` channel; designation effect = `core` channel. The active set is **shared across all tabs** — see §11 D2. |
| All effects are re-applied on load by `NodeApplyService.reapplyAllOwned()` | `SkillsOverhaulModPlugin.java:119` | Handlers must be idempotent and keyed by `nodeId` source strings. |
| Listeners: `getListenerManager().addListener(l, true)` (transient) / `addTransientListener` / `addTransientScript` on every `onGameLoad` | `SkillsOverhaulModPlugin.java:64-98` | All new admin listeners follow this. Nothing non-serializable stored. |
| Soft deps declared: only `lunalib`, `nexerelin` | `mod_info.json` | Add `IndEvo`, `aotd_vok`, `aotd_sop`, `Cryo_but_better`, `Terraforming & Station Construction`, `nex4x_expansion`, `physical_trade` to `softDependencies` (load-order only, not a hard dep). |
| Build classpath = game jars only | `build.sh` `CLASSPATH` | Any compile-time reference to another mod's classes needs a classpath change (§5). |
| **Influence** is a nex4x resource (`nex4x.influence.InfluenceManager.get().addLump/spend`) — not vanilla | `workspace/nexerelin-4x-mod/src/nex4x/influence/InfluenceManager.java` | Every Influence cost/reward in the spec is **nex4x-gated**. Without nex4x it needs a fallback currency — §11 D3. |
| nex4x `Pressure` = `nex4x.pressure.PressureManager.get().applyEvent(from,to,source,amt)` | `nexerelin-4x-mod/src/nex4x/pressure/PressureManager.java` | Usable for Orbital Suppression / Cascade Failure. |
| `HireMercenariesAction`, `DiplomatOfficialSupportAction` live in **nex4x** (user repo); player sabotage is Nex's `exerelin.campaign.intel.agents.SabotageIndustry` (`SabotageIndustryAction` is NPC-AI only) | `nexerelin-4x-mod/src/nex4x/agents/actions/...`, `Nexerelin-0.12.2c/jars/ExerelinCore.jar` | Cost hooks for these need a nex4x-side change (cross-repo). |
| Physical Trade has **no public API facade**; `ShortageDebitLedger` is an internal shortage ledger (`Entry{routeId, marketId, commodityId, expectedQty}`), not a commission/fee ledger; PT needs LTV Economy to run convoys | `workspace/Physical Trade/src/physical_trade/...`, PT `mod_info.json` | Every **PT overlay** needs a PT-side API first — §11 D4. |
| **Covert-market framework does not exist** anywhere in the workspace | grep over `workspace/` | All 9 `[C]` nodes and the 4 capstone covert overlays are blocked. Ship them as "Coming Soon" stubs (spec §Covert). |
| Enabled in the current game profile: `IndEvo`, `nexerelin`, `nex4x_expansion`, `physical_trade`, `wfg_ltv_econ`, `lunalib`. **Not enabled:** AoTD (any), TASC, Dreams of Past, Second-in-Command | `game/mods/enabled_mods.json` | AoTD/TASC paths cannot be in-game verified without a second profile. |
| Real mod industry ids (from each mod's `data/campaign/industries.csv`) | see §4.4 | Spec ids `IndEvo_Specialized Mining/Refining` = `IndEvo_SwitchableMining` / `IndEvo_SwitchableRefining`. `cryorevival`/`reawakening_facility` = mod `Cryo_but_better`. TASC mod id is literally `Terraforming & Station Construction`. |

Vanilla API facts used below (verified with `javap` on `starfarer.api.jar`):

- `MarketAPI`: `getStability()` (`MutableStatWithTempMods`), `getAccessibilityMod()` (`StatBonus`),
  `getHazard()`, `getIncomeMult()`, `getUpkeepMult()`, `getTariff()`, `isFreePort()`,
  `getStats().getDynamic()`, `addTransientImmigrationModifier(MarketImmigrationModifier)`,
  `getDaysInExistence()`, `isPlayerOwned()`, `getAdmin()`, `reapplyConditions()`, `reapplyIndustries()`.
- `Stats` market keys: `PRODUCTION_QUALITY_MOD`, `FLEET_QUALITY_MOD`, `COMBAT_FLEET_SIZE_MULT`,
  `PATROL_NUM_LIGHT/MEDIUM/HEAVY_MOD`, `GROUND_DEFENSES_MOD`, `MAX_INDUSTRIES`, `MAX_MARKET_SIZE`,
  `SUPPLY_BONUS_MOD`, `DEMAND_REDUCTION_MOD`, `OFFICER_PROB_MOD`; fleet: `PLANETARY_OPERATIONS_MOD`.
- `Industry`/`BaseIndustry`: `getSupplyBonus()` (persistent `MutableStat`, flat units to every
  supplied commodity — the AI-core/Industrial-Planning hook), `getSupply(id).getQuantity()`,
  `getIncome()`, `getUpkeep()`, `isBuilding()`, `getBuildOrUpgradeProgress()`,
  `setBuildProgress(float)`, `setBuildCostOverride(float)`, `getBuildCost()`, `getBuildTime()`.
- Listeners available: `EconomyTickListener`, `PlayerColonizationListener`
  (`reportPlayerColonizedPlanet`, `reportPlayerAbandonedColony`), `ColonyDecivListener`,
  `ColonySizeChangeListener`, `ColonyOtherFactorsListener`, `IndustryOptionProvider` /
  `BaseIndustryOptionProvider` (adds buttons to an industry's panel — the designation UI),
  `GroundRaidObjectivesListener`, `FleetEventListener`.
- `HostileActivityEventIntel.addFactor(EventFactor)` — a negative `BaseEventFactor` slows HA progress.
- Hegemony AI inspection: `intel.inspection.HegemonyInspectionIntel` (+ `AntiInspectionOrders`,
  `HegemonyInspectionOutcome`, `InspectionEndedListener`), `HegemonyInspectionManager`.
- `CampaignClockAPI` has `getCycle()`, `getMonth()`, `getDay()` (day-of-month), `getTimestamp()`,
  `getElapsedDaysSince(long)` — **no day-of-year accessor**. See §6 for the monotonic-day rule.

---

## 1. Architecture

### 1.1 Why not vanilla skills

Vanilla colony skills are `MarketSkillEffect`s (`apply(MarketAPI, id, level)`) on real
`data/characters/skills/*.skill` specs; the game applies them per governed market. Skills Overhaul
deliberately removes those, and a mod-defined hidden skill would put an unknown skill id into every
save (uninstall breaks). So admin effects are applied **directly to market stats**, keyed by source
string, by our own service.

### 1.2 `AdminMarketEffectService` (new, `src/skills_overhaul/admin/`)

One service owns every per-market modification. Handlers never touch markets directly; they
register a **spec** and the service applies it.

```
AdminEffectSpec { nodeId, scope (ALL_PLAYER | DESIGNATED:<role> | INDUSTRY_FILTER | SYSTEM_FILTER),
                  condition (ALWAYS | AT_WAR | FIRST_CYCLE | ...), List<StatOp> }
StatOp          { target (STABILITY|ACCESSIBILITY|HAZARD|INCOME_MULT|UPKEEP_MULT|TARIFF|DYNAMIC:<key>
                          |INDUSTRY_SUPPLY_BONUS|INDUSTRY_SUPPLY:<commodity>|INDUSTRY_UPKEEP|INDUSTRY_INCOME),
                  op (FLAT|PERCENT|MULT), value, industryIds? }
```

- **Registry is static and transient** (like `CommandantCombatPlugin.ACTIVE_NODES`): rebuilt on
  every load by `reapplyAllOwned()`. Nothing about specs is saved.
- **`refresh(MarketAPI)`**: unmodify every `so_admin:<nodeId>` source on the market, then apply
  every spec whose scope + condition matches. Idempotent. Source string = `"so_admin:" + nodeId`
  (+ `":" + industryId` for industry-level ops).
- **When it runs:** `reportEconomyTick(iterIndex == 0)` for all player markets (same cadence as
  `AdminXPTracker`), on `PlayerColonizationListener.reportPlayerColonizedPlanet`, on
  `ColonySizeChangeListener`, on handler `apply/remove` (refresh all), and on designation change.
- **Uninstall safety:** stat modifications are plain keyed `StatMod`s; if the mod is removed they are
  inert strings in the save. No custom `MarketCondition`, no custom industry, no saved plugin
  instances. `UninstallHelper.prepareUninstall()` additionally calls
  `AdminMarketEffectService.stripAll()` so the save is clean.
- **Market scope = player-faction markets** (`market.getFaction().isPlayerFaction()`), not "markets
  the player personally governs". See §11 D5 — vanilla Industrial Planning was governed-only.

> **Phase 0 spike must confirm** that an externally keyed `industry.getSupplyBonus().modifyFlat(...)`
> and `industry.getSupply(c).getQuantity().modifyFlat(...)` survive `BaseIndustry.apply()` /
> `reapplyIndustries()` between economy ticks. If `apply()` rebuilds supply, the service re-applies
> after each tick instead (same code path, later hook) — the fallback is the per-tick refresh above.

### 1.3 Integer output units

Vanilla commodity output is **integer units** (≈ market size ± modifiers), not a continuous
quantity. Every spec "+X% output" is mapped to flat units:

`units = X ≥ 50% ? +2 : X ≥ 15% ? +1 : 0` (tunable in `skills_overhaul.json`), applied with
`INDUSTRY_SUPPLY:<commodity>` (one commodity) or `INDUSTRY_SUPPLY_BONUS` (all outputs, the
AI-core path). This is a reinterpretation — flagged in §11 D6.

### 1.4 Designations (T0 / T5 "designate one colony as …")

New `AdminDesignationService` (memory: `$so.admin.designation.<role>` = marketId; roles
`garrison_capital`, `forge_capital`, `trade_house`, `sector_exchange`, `living_charter`,
`genesis_world`). UI = `AdminDesignationOptionProvider extends BaseIndustryOptionProvider`
registered transiently; it adds "Designate as Garrison Capital" to the **population** industry
panel of an eligible player market (eligibility per role, e.g. Garrison Capital requires an industry
with tag `military`/`patrol` — verify tag in Phase 0). One market per role; re-designation has a
30-day lockout (memory key with expiry) to stop swap-farming. Lost/decivved market → designation
cleared on next tick.

### 1.5 Campaign tickers and actives

- `AdminDailyTicker` (transient `EveryFrameScript`, fires once per monotonic day): build-time
  acceleration, first-cycle timers, war-state cache, temp-bonus expiry.
- `AdminMonthlyListener` (`EconomyTickListener.reportEconomyMonthEnd`): interest, reserves,
  Influence income, relation drift, HA counter-factor refresh.
- Active abilities (once/cycle, once/60 days …) = campaign abilities in
  `data/campaign/abilities.csv`, granted via the existing `effect/AbilityGrants` path, cooldowns as
  memory keys with expiry. Reuse the Corsair `LetterOfMarqueAbility` / Commandant
  `CraftSuppliesAbility` shape.

### 1.6 Build-cost and build-time hooks

- **Build time −X%**: `AdminDailyTicker` finds `industry.isBuilding()` matching the filter and adds
  `X/(1−X) × (1/buildTime)` progress per day via `setBuildProgress`. Easy, verified setter exists.
- **Build cost −X%**: vanilla charges credits in the build dialog **before** the `Industry` instance
  exists, so `setBuildCostOverride` cannot discount a new build. Implementation = **refund on
  start**: the daily ticker sees a newly building industry (not yet in
  `$so.admin.refunded.<marketId>.<industryId>`) and credits `X × getBuildCost()` to the player with a
  message. Medium. UI still shows full price — §11 D7.

---

## 2. Shared handlers to add

All registered in `EffectRegistry` with the `SO_admin_` prefix, using vanilla parameter vocabulary
(`stats[]`, `key`, `value`), per the Skills Overhaul naming convention. One handler class per
mechanism, shared across all four trees.

| Effect type | Class | Params | Used by (count) |
|---|---|---|---|
| `SO_admin_market_stat` | `SOAdminMarketStatHandler` | `scope`, `condition`, `industries[]?`, `stats[{key, op, value}]` | ~35 nodes — stability, accessibility, hazard, income/upkeep, dynamic market stats |
| `SO_admin_output` | `SOAdminOutputHandler` | `industries[]`, `commodities[]?`, `units`, `condition` | ~20 nodes — the +output family |
| `SO_admin_build` | `SOAdminBuildHandler` | `industries[]`, `timeMult?`, `costRefund?`, `scope` | ~10 nodes |
| `SO_admin_designation` | `SOAdminDesignationHandler` | `role`, then `stats[]` like market_stat | 8 (T0 + T5 core channels) |
| `SO_admin_growth` | `SOAdminGrowthHandler` | `scope`, `growthPercent`, `condition` (transient `MarketImmigrationModifier`) | ~8 nodes |
| `SO_admin_flag` | `SOAdminFlagHandler` | none — adds `nodeId` to `AdminActiveNodes` set for listener-driven nodes | ~25 nodes (mirrors `SOCombatScriptHandler`) |
| `SO_admin_ability` | `SOAdminAbilityHandler` | `abilityId` | ~8 nodes (actives) |
| `SO_admin_gate` | wrapper semantics, not a class: any effect may carry `"requiresMod": "<modId>"` and `"fallback": {...}` — `NodeEffectApplier.shouldApply` reads it (one small change) | — | all M/PT nodes |
| `SO_fleet_stat` (existing) | add keys `cargoCapacityMod`, `fuelCapacityMod` (per-member `getCargoMod`/`getFuelMod`) | | Wholesale Logistics, Hardened Logistics (sensorProfileMod exists) |

Supporting services (no effect type): `AdminMarketEffectService`, `AdminDesignationService`,
`AdminDesignationOptionProvider`, `AdminDailyTicker`, `AdminMonthlyListener`, `WarState`
(vanilla `FactionAPI.isHostileTo` over `Factions` majors; Nex-aware when loaded), `ModGate`,
`InfluenceBridge` (nex4x), `AdminActiveNodes`.

---

## 3. Phases and effort

Units: **task** = one subagent brief → one commit; **h** = focused implementation + unit-test hours,
excluding in-game verification, which is listed per phase.

| Phase | Content | Tasks | Est. | In-game check |
|---|---|---|---|---|
| **0 — Spike** | Verify the 6 open API questions in §9 R1–R6 with a throwaway console `runcode` script; record findings in this doc | 1 | 4 h | 1 session |
| **1 — Framework** | `AdminMarketEffectService`, `AdminDesignationService` + option provider, `AdminDailyTicker`, `AdminMonthlyListener`, `WarState`, `ModGate`, monotonic day util, `requiresMod`/`fallback` gate in `NodeEffectApplier`, the 7 shared handlers, `SO_fleet_stat` cargo/fuel keys, `UninstallHelper.stripAll`, 4 tree JSON skeletons (all 22 ids per tree, grid + prereqs, unimplemented nodes `"effects": []` + "Coming Soon" desc) | 9 | 30 h | 1 session: load old save, designate, respec, reload |
| **2 — Vanilla T0–T3** | All `V` effects of T0–T3 in 4 trees that are easy/medium (§4) | 4 (one per tree) | 32 h | 1 session per tree |
| **3 — Vanilla T4–T5** | T4 `V` nodes + T5 cores/passives + T5 actives | 4 | 30 h | 1 session per tree |
| **4a — Nex / nex4x** | `WarState` Nex path, `InfluenceBridge`, ColonyDeciv-driven nodes, nex4x action cost hooks (needs nex4x commits, §5.3) | 3 (+2 in nex4x repo) | 20 h | 1 session |
| **4b — IndEvo** | IndEvo id-gated nodes (build refunds, output on IndEvo industries) | 1 | 6 h | 1 session |
| **4c — AoTD / TASC / DoP** | id-gated output, refunds, stability; internals-dependent parts stay stubs | 2 | 10 h | **needs a second profile with AoTD + TASC** |
| **4d — Physical Trade overlays** | Blocked on PT API (§11 D4) | 0 now | 12–20 h once API exists | — |
| **5 — Covert `[C]` nodes** | Blocked on covert-market framework (not designed) | 0 now | not estimable | — |

**Total for Phases 0–4c: 24 tasks, ≈ 132 h, ≈ 9 in-game sessions.** Phases 4d and 5 are out of scope
until their prerequisites exist.

Order rationale: framework first because ~70% of nodes are parameterizations of 4 handlers; vanilla
before mods so every M node has a working fallback to fall back *to*; T4–T5 after T0–T3 so balance
of the cheaper tiers is observed before the capstones stack on them.

---

## 4. Per-node tables

Legend — **Feas.**: **E** easy (parameterize a shared handler), **M** medium (new listener logic /
ability / UI), **H** hard (depends on another mod's internals, private game state, or cross-repo
change), **X** impossible as written (no hook exists, or the mechanic does not exist in the game).
**Ph** = phase. `DECIDE` = blocked on §11. Node ids follow `admin_<tree>_t<tier>_<slug>`; T0/T5 keep
the existing placeholder ids. Grid: T1–T4 nodes at `x = 0..4` in spec order; prereq = same column
one tier up (commander-tree convention); T5 = `atLeast 2 of` the T4 row (as `commandant_t5`).

### 4.1 Warden

| Node id | Effect (as implemented) | Hook API | Handler | Feas. | Ph |
|---|---|---|---|---|---|
| `admin_warden_t0` core | Garrison Capital designation (needs military industry): fleet size ×1.5, ground def ×(see D8), +1 unit to supplies/fuel/marines/weapons/heavy_machinery producers, −25% upkeep on military industries, quality +D9 | `IndustryOptionProvider`; `Stats.COMBAT_FLEET_SIZE_MULT`, `GROUND_DEFENSES_MOD`, `PRODUCTION_QUALITY_MOD`; `Industry.getSupply().getQuantity()`, `getUpkeep()` | `SO_admin_designation` + `SO_admin_output` | M | 2 |
| `admin_warden_t0` base | Ribbon: ground def +25%, accessibility +10% (D10) on military markets | `GROUND_DEFENSES_MOD`, `getAccessibilityMod()` | `SO_admin_market_stat` | E | 2 |
| `admin_warden_t1_shipyard_throughput` | heavyindustry/orbitalworks build time −25%; quality +D9 | `setBuildProgress`; `PRODUCTION_QUALITY_MOD` | `SO_admin_build` + `SO_admin_market_stat` | E | 2 |
| `admin_warden_t1_bastion_engineering` | Station family + `IndEvo_Artillery_*` + `aotd_citadel*` build cost −30% (refund). "Tier requirement −1" has no meaning for stations | refund-on-start | `SO_admin_build` | M; tier part X, `DECIDE` D11 | 2 / 4b-c |
| `admin_warden_t1_drilled_quartermasters` | Spec: stockpile cap +25%, docking CR refit speed. **No stockpile cap and no docking-refit stat exist** | — | — | X, `DECIDE` D11 | — |
| `admin_warden_t1_hardened_logistics` | Player fleet sensor profile −25% while in non-hostile-owned systems. "Inspection chance" has no stat | `SO_fleet_stat sensorProfileMod` toggled by daily location check | `SO_admin_flag` + ticker | M; inspection part X | 2 |
| `admin_warden_t1_marine_corps` | marines +1 unit at colonies with military industry; player ground ops +25% | `getSupply("marines")`; `Stats.PLANETARY_OPERATIONS_MOD` | `SO_admin_output` + `SO_fleet_stat planetaryOperationsMod` | E | 2 |
| `admin_warden_t2_layered_defense` | Ground defenses ×2 on markets with an orbital station. IndEvo artillery "immunity zone" undefined | `GROUND_DEFENSES_MOD` with station-present filter | `SO_admin_market_stat` | E; artillery part `DECIDE` | 2 |
| `admin_warden_t2_customs_mandate` | Spec: seize illegal cargo of visiting NPC fleets. **NPC fleets are never cargo-inspected at player markets; no event to hook** | — | — | X, `DECIDE` D11 | — |
| `admin_warden_t2_patrol_network` | V: player fleet sensor range +X% in owned systems that contain a player-owned comm relay. M IndEvo: same, extended to systems linked by `IndEvo_IntArray`. "Faction-wide visibility" not modelable | `SO_fleet_stat sensorRangeMod`, daily location check; system objective faction check | `SO_admin_flag` + ticker | M (after D11 reword) | 2 / 4b |
| `admin_warden_t2_wartime_production` | While `WarState.atWar()`: military producers +1 unit, upkeep −25%; 14-day decay after peace | `WarState` (vanilla hostility; Nex when present) | `SO_admin_output` + `SO_admin_market_stat` (`condition: AT_WAR_DECAY14`) | E–M | 2 |
| `admin_warden_t2_hostile_activity_suppression` | Colony HA progress −50% via a negative `EventFactor` sized to the monthly positive progress. "Suppression action cost" refers to nothing in vanilla | `HostileActivityEventIntel.get()` + `addFactor` | `SO_admin_flag` + monthly listener | M; cost part X | 2 |
| `admin_warden_t3_armory_compact` | While any major-faction war: heavyindustry/orbitalworks markets income +X (flat or %; per-commodity export income has no hook) | `Industry.getIncome()` on those industries; `WarState.anyMajorWar()` | `SO_admin_market_stat` (`INDUSTRY_INCOME`) | M, `DECIDE` D12 (war definition) | 2 |
| `admin_warden_t3_ai_inspection_brokerage` | Spec: pre-resolve Hegemony AI inspections by player choice. Vanilla already offers orders (comply/bribe/hide) in `HIOrdersInteractionDialogPluginImpl`; overriding outcome = intel internals | `HegemonyInspectionIntel.InspectionEndedListener` to apply a post-hoc credit/rep/Influence adjustment | `SO_admin_flag` | H, `DECIDE` | 3 or cut |
| `admin_warden_t3_convoy_hegemony` | V: +1 light patrol (`PATROL_NUM_LIGHT_MOD` +1) on military markets; +X% accessibility on markets in systems with ≥2 player markets. PT: real escorted company | `Stats.PATROL_NUM_LIGHT_MOD`, accessibility | `SO_admin_market_stat` | E (V); PT H | 2 / 4d |
| `admin_warden_t3_standing_reserve` | Monthly reserve accrual (memory float, 30 at war / 15 at peace) ; ability spends reserve to spawn a defense fleet at a player market | `FleetFactoryV3`, fleet assignment `DEFEND_LOCATION`; ability | `SO_admin_ability` + monthly listener | M; "no cooldown" `DECIDE` D13 | 3 |
| `admin_warden_t3_dead_man_doctrine` | When a player military market is lost (owner change detected on economy tick): nearest player military market +1 patrol (light+medium) for 30 days. "Surviving garrison routes" not modelable (patrols despawn with market) | owner-change poll; temp `PATROL_NUM_*` with expiry | `SO_admin_flag` + monthly/tick | M | 2 |
| `admin_warden_t4_invasion_coordinator` | V fallback (ships in Ph 3): player ground ops +50%. Nex: invasion prep/fleet/cost apply to Nex's **NPC-faction** invasion manager; the player faction does not launch Nex invasion fleets in the same way | `PLANETARY_OPERATIONS_MOD`; Nex `InvasionFleetManager` | `SO_fleet_stat` | E (V); Nex H, `DECIDE` | 3 / 4a |
| `admin_warden_t4_mercenary_compact` | nex4x `HireMercenariesAction` −40% credits / −25% Influence | needs nex4x to read a cost multiplier (`$so.admin.mercCostMult`) | `SO_admin_flag` (sets memory) | M (cross-repo) | 4a |
| `admin_warden_t4_doctrine_of_total_war` | Hostile to ≥2 majors: player fleet max CR +X. "Export tier cap", per-commodity price +50% have no hook | `SO_fleet_stat maxCombatReadinessFlat` toggled by `WarState` | `SO_admin_flag` + ticker | M; price/tier parts X | 3 |
| `admin_warden_t4_orbital_suppression_authority` | nex4x agent raid: target stability −2d3 (temp `getStability().addTemporaryModFlat`), 100 Influence + 1 SP, Pressure event | ability + `PressureManager.applyEvent` + `InfluenceBridge` | `SO_admin_ability` | H (nex4x action design) | 4a |
| `admin_warden_t4_shadow_fleet_doctrine` [C] | Covert | — | stub | blocked | 5 |
| `admin_warden_t5` core | Forge Capital: `MAX_INDUSTRIES` +1, quality +D9 (stacks) | `Stats.MAX_INDUSTRIES`, `PRODUCTION_QUALITY_MOD` | `SO_admin_designation` | E | 3 |
| `admin_warden_t5` core (cont.) | "Ships built there −25% credits, ignore one D-mod roll": player custom production is faction-wide, not per market; no per-build D-mod hook | — | — | X / H, `DECIDE` D11 | — |
| `admin_warden_t5` passive | At war: Forge Capital heavy industry income +50%. "Buyers +0.5 rel/cycle" — buyers undefined | `Industry.getIncome()` | `SO_admin_market_stat` | E; relations `DECIDE` | 3 |
| `admin_warden_t5` active | Veteran Squadron once/cycle: reuse Corsair `PrivateerFleetService` spawn/despawn lifecycle (30 days) | `PrivateerFleetService` pattern | `SO_admin_ability` | M | 3 |
| `admin_warden_t5` PT / covert overlays | `forge_stamped_hulls` commodity; covert sales | PT API | — | H / blocked | 4d / 5 |

### 4.2 Merchant

| Node id | Effect (as implemented) | Hook API | Handler | Feas. | Ph |
|---|---|---|---|---|---|
| `admin_merchant_t0` core | Trade House designation (commerce/spaceport/megaport): accessibility +25%, market income +X% (spec's "tariff income from visiting fleets" does not exist; player market income is export income) | `getAccessibilityMod`, `getIncomeMult` | `SO_admin_designation` | M, `DECIDE` D11 | 2 |
| `admin_merchant_t0` base | All player markets income +5%. "−25% inspection chance on your fleets" has no stat | `getIncomeMult` | `SO_admin_market_stat` | E; inspection X | 2 |
| `admin_merchant_t1_logistical_genius` | Megaport markets +10% accessibility. "Spaceport/megaport tier requirement −1": the size gate is hard-coded in the industry plugin class (`isAvailableToBuild`) — only replaceable by overriding vanilla `industries.csv` plugin rows (conflict-prone) | `getAccessibilityMod` | `SO_admin_market_stat` | E; tier part X/H | 2 |
| `admin_merchant_t1_tariff_specialist` | Spec: tariff scaling +25%, docking fees +50%. `MarketAPI.getTariff()` only changes **the player's own** buy/sell prices at that market — raising it hurts the player; there are no docking fees | `getTariff()` | — | X as written, `DECIDE` D11 | — |
| `admin_merchant_t1_banking_license` | Player credits ≥ 500k earn 1.5%/month at month end, if a Trade House is designated (storage does not hold credits) | `reportEconomyMonthEnd`, `getPlayerFleet().getCargo().getCredits()` | `SO_admin_flag` + monthly | E; balance `DECIDE` D14 | 2 |
| `admin_merchant_t1_free_port_mastery` | Free-port markets: stability +2 (offsets half the free-port penalty — verify current penalty in Phase 0). "Black-market tier +1" — black markets have no tiers | `isFreePort()`, `getStability()` | `SO_admin_market_stat` (`condition: FREE_PORT`) | E; tier part X | 2 |
| `admin_merchant_t1_convoy_coordination` | V: none meaningful ("raid risk on your trade fleets" — player has no trade fleets without PT). IndEvo: `IndEvo_PrivatePort` throughput is IndEvo-internal | — | — | H/X, `DECIDE` | 4b or cut |
| `admin_merchant_t2_free_trade_charter` | Free-port markets: accessibility +20%; tariff ×0.5 (benefits the player's own trades there) | `getAccessibilityMod`, `getTariff` | `SO_admin_market_stat` | E | 2 |
| `admin_merchant_t2_domain_era_banking` | +1 Influence per 100k monthly empire income; M: `aotd_capital_complex`/`IndEvo_senate` presence doubles it | `InfluenceBridge`; industry id check | `SO_admin_flag` + monthly | E with nex4x; `DECIDE` D3 without | 4a |
| `admin_merchant_t2_crisis_speculator` | During defined "sector events" all player markets income +50%. Events need definitions; "auto-adjust prices to peak demand" has no hook | `WarState`, deciv listener, condition presence | `SO_admin_market_stat` (`condition: SECTOR_EVENT`) | M, `DECIDE` D12 | 2 |
| `admin_merchant_t2_customs_cooperation` | NPC inspection at player markets does not exist (as Customs Mandate) | — | — | X, `DECIDE` D11 | — |
| `admin_merchant_t2_black_routing` [C] | Covert | — | stub | blocked | 5 |
| `admin_merchant_t3_liquidity_crisis` | V: ability once/cycle — copy N units of a chosen commodity from a non-allied market's open market cargo into the Trade House storage; target market gets a 30-day temporary availability/stability malus. PT version needs PT API | `SubmarketAPI.getCargo()`, `getStability().addTemporaryModFlat` | `SO_admin_ability` | M; PT H | 3 / 4d |
| `admin_merchant_t3_diplomatic_couriers` | nex4x `DiplomatOfficialSupportAction` −60% credits; +1 diplomat slot | nex4x reads `$so.admin.*` multipliers (cross-repo) | `SO_admin_flag` | M (cross-repo) | 4a |
| `admin_merchant_t3_trade_league_compact` | V "Commerce Treaty": pick 2 NPC factions yearly; +Influence/cycle while both non-hostile to player. "+1 stability each" target undefined | ability + monthly; `FactionAPI.isHostileTo` | `SO_admin_ability` | M, `DECIDE` | 3 |
| `admin_merchant_t3_wholesale_logistics` | Player fleet cargo + fuel capacity +5%/player colony, cap +30% | per-member `getCargoMod`/`getFuelMod` | `SO_fleet_stat` (new keys) recomputed monthly | E | 2 |
| `admin_merchant_t3_contraband_cartel` [C] | Covert (V fallback also needs `aotd_underworld`/`IndEvo_pirateHaven` — partial V possible: monthly credits from hub industry presence) | industry id check | stub | blocked (V partial `DECIDE`) | 5 |
| `admin_merchant_t4_megaport_charter` | "Megaport tier max +1": no tier above megaport. "+1 security rating stack" undefined | — | — | X, `DECIDE` D11 | — |
| `admin_merchant_t4_banking_conglomerate` | Interest +1.5% (total 3%); storage cargo at Trade House accrues value × rate as credits | monthly; `Misc.getStorageCargo(market)` | `SO_admin_flag` + monthly | E; balance `DECIDE` D14 | 3 |
| `admin_merchant_t4_offshore_vault` | Wealth taxes / reparations / confiscation do not exist in vanilla or Nex (verify Starlogue) | — | — | X, `DECIDE` D11 | — |
| `admin_merchant_t4_free_port_empire` | "Pool black-market depth": submarkets are per market — X. "+25% income from player black-market sales": needs a price hook on black-market submarkets | `SubmarketPlugin` price mods (verify) | — | X / H | — |
| `admin_merchant_t4_underworld_information_network` [C] | Covert | — | stub | blocked | 5 |
| `admin_merchant_t5` core | Sector Exchange: accessibility +50%, +100k/month flat income (population industry `getIncome().modifyFlat`), monthly relation +0.25 with all non-hostile factions (D15 scale) | accessibility, `Industry.getIncome`, `FactionAPI.adjustRelationship` | `SO_admin_designation` + monthly | E | 3 |
| `admin_merchant_t5` active 1 | Embargo: 30-day temporary stability/accessibility malus on all markets of the target faction; 3 SP + 50 Influence. Spec's `event_trade_disruption` condition id must be verified (Phase 0) | `addTemporaryModFlat` or condition | `SO_admin_ability` | M | 3 |
| `admin_merchant_t5` active 2 | Commodity bias ×0.85–1.15 sector-wide for 30 days: needs a per-commodity price hook on every market | `CommodityOnMarketAPI` price mods (verify R5) | `SO_admin_ability` | H | 3 or cut |
| `admin_merchant_t5` PT / covert overlays | Commission from routes; Shadow Exchange | PT API | — | H / blocked | 4d / 5 |

### 4.3 Prospector

| Node id | Effect (as implemented) | Hook API | Handler | Feas. | Ph |
|---|---|---|---|---|---|
| `admin_prospector_t0` core | Boom Town: each new colony — spaceport + first industry build time −50%, cost −25% (refund); hazard −25% for its first 365 days; then permanent +10% accessibility | `PlayerColonizationListener`, `getDaysInExistence()`, `getHazard()`, refund/progress | `SO_admin_flag` + `SO_admin_build` + `SO_admin_market_stat` (`condition: FIRST_CYCLE`) | M | 2 |
| `admin_prospector_t0` base | mining/techmining/refining/fuelprod +1 unit; their upkeep −25% (the hazard penalty is market-wide in vanilla, not per industry) | `getSupplyBonus`, `Industry.getUpkeep` | `SO_admin_output` + `SO_admin_market_stat` | E | 2 |
| `admin_prospector_t1_vein_reading` | Resource conditions are already visible once surveyed — tooltip part is a no-op. IndEvo free Switchable Mining = IndEvo-internal | — | — | X / H, `DECIDE` D11 | — |
| `admin_prospector_t1_stable_location_surveyor` | Extraction industries +1 unit in owned systems with ≥1 player-faction comm relay / nav buoy / sensor array. Makeshift build cost −50% = rules.csv dialog cost (verify R6) | `StarSystemAPI` objective entities faction check | `SO_admin_output` (`condition: SYSTEM_HAS_PLAYER_OBJECTIVE`) | E; cost part H | 2 |
| `admin_prospector_t1_catalytic_mastery` | V fallback: refining +1 unit. AoTD: `aotd_crystalizator`, `aotd_enrichment_facility` cost −30% (refund). Catalytic item effect ×2 = item-plugin internals | id gate | `SO_admin_output` + `SO_admin_build` | E; item part H | 2 / 4c |
| `admin_prospector_t1_hard_vac_crews` | Extraction colonies: growth modifier offsetting up to +25% hazard (immigration modifier adds back the hazard growth penalty, capped) | `addTransientImmigrationModifier` | `SO_admin_growth` | M | 2 |
| `admin_prospector_t1_off_grid_refining` | Markets have no sensor profile; rival "sensor reports" do not exist | — | — | X, `DECIDE` D11 | — |
| `admin_prospector_t2_salvage_doctrine` | V: post-battle salvage +50% in systems with a player market — reuse `supply/SalvageBonusListener` / Corsair `SalvageMultiplierHandler`. IndEvo ScrapYard ×2 = IndEvo-internal | salvage listener with location filter | existing salvage handler + `SO_admin_flag` | E–M; IndEvo H | 2 / 4b |
| `admin_prospector_t2_domain_archaeology_lead` | TASC/`techmining` rare-yield +50%, ruins never deplete below Scattered: techmining depletion lives in vanilla `TechMining` market memory (verify) | techmining internals | — | H | 4c or cut |
| `admin_prospector_t2_volatiles_cartel` | While any major war: fuelprod markets income +X. "Export to factions at war" / "supplying both sides" not modelable | `Industry.getIncome`, `WarState` | `SO_admin_market_stat` | E; `DECIDE` D12 | 2 |
| `admin_prospector_t2_refinery_cascade` | Market hosting ≥2 of refining / `aotd_smelting` / `aotd_enrichment_facility` / `aotd_crystalizator`: each +1 unit | industry id set count | `SO_admin_output` (`condition: COUNT_GE_2`) | E | 2 / 4c |
| `admin_prospector_t2_unsanctioned_claim` [C] | Covert | — | stub | blocked | 5 |
| `admin_prospector_t3_megastructure_leverage` | TASC `BOGGLED_STELLAR_REFLECTOR_ARRAY`/`_ATMOSPHERE_PROCESSOR`/`_PLANET_CRACKER` cost −40% (refund), time −30% | id gate | `SO_admin_build` | E (TASC profile) | 4c |
| `admin_prospector_t3_industrial_throughput` | Reinterpreted: extraction industries +1 unit (a "tier above market size" gate does not exist for mining — output derives from size + resource condition) | `getSupplyBonus` | `SO_admin_output` | E (reinterpretation D6) | 2 |
| `admin_prospector_t3_ai_mining_drone_charter` | V fallback: mining +1 unit. TASC drones +50% and inspection exemption = internals | — | `SO_admin_output` | E (V); TASC H | 2 / 4c |
| `admin_prospector_t3_reclamation_rights` | V: refund 25% of colonization cost for colonies founded in systems with an existing player market (colonization cost formula verify R6). Nex first-claim window with pre-built industries = H | `PlayerColonizationListener`, `ColonyDecivListener` | `SO_admin_flag` | M (V); first-claim H | 2 / 4a |
| `admin_prospector_t3_strip_mine_license` [C] | Covert | — | stub | blocked | 5 |
| `admin_prospector_t4_coronal_tap_mastery` | Extraction industries +1 unit in systems containing a player-controlled coronal hypershunt; restoration cost (rules dialog) H | entity faction check | `SO_admin_output` | E; restore part H | 3 |
| `admin_prospector_t4_ouyang_optimization` | TASC `BOGGLED_OUYANG_OPTIMIZER` cost refund 100%; output ×2 = TASC-internal | id gate | `SO_admin_build` | E / H | 4c |
| `admin_prospector_t4_industrial_hegemony` | Monthly: if player supply of any commodity > 50% of sector supply → +1 Influence. "Kingmaker petitions" undefined | `EconomyAPI` market iteration, `getCommodityData(c).getMaxSupply()` (verify) | `SO_admin_flag` + monthly | M; Influence D3 | 3 / 4a |
| `admin_prospector_t4_catalytic_cascade_failure` | Wrap Nex player sabotage (`SabotageIndustry` agent action) cost; nex4x Pressure on trace | Nex agent intel; nex4x | `SO_admin_ability` | H | 4a |
| `admin_prospector_t4_hoard_cellar` [C] | Covert | — | stub | blocked | 5 |
| `admin_prospector_t5` core | Extraction industries +1 unit (stacks) | `getSupplyBonus` | `SO_admin_output` | E | 3 |
| `admin_prospector_t5` passive | Annual: set survey level FULL on all planets in systems with a player market. "Auto-recover ruins" undefined | `MarketAPI.setSurveyLevel` | `SO_admin_flag` + monthly | E; recover part `DECIDE` | 3 |
| `admin_prospector_t5` actives | Megastructure restoration −50% (rules-dialog costs, verify R6); techmining blueprint drop +50% (techmining loot internals) | — | — | H / H | 3 or cut |
| `admin_prospector_t5` covert overlay | Hoard | — | — | blocked | 5 |

### 4.4 Steward

| Node id | Effect (as implemented) | Hook API | Handler | Feas. | Ph |
|---|---|---|---|---|---|
| `admin_steward_t0` core | Living Charter designation: stability +1, growth +50%, farming/aquaculture/lightindustry +1 unit. TASC terraform +50% = TASC-internal | stability, immigration modifier, supply | `SO_admin_designation` + `SO_admin_growth` + `SO_admin_output` | M; TASC H | 2 |
| `admin_steward_t0` base | farming/aquaculture/lightindustry +1 unit empire-wide, growth +25% | | `SO_admin_output` + `SO_admin_growth` | E | 2 |
| `admin_steward_t1_bio_adaptive_crews` | Growth +10%; lightindustry +1 luxury_goods unit (D6 — "per cycle" read as supply units) | | `SO_admin_growth` + `SO_admin_output` | E | 2 |
| `admin_steward_t1_hydroponic_yields` | V: farming +1 unit. TASC: `BOGGLED_HYDROPONICS`/`_CLONING` +1 unit. "Water worlds as Class III" undefined | id gate | `SO_admin_output` | E | 2 / 4c |
| `admin_steward_t1_sanctuary_doctrine` | Growth +50%. Nex refugee influx: needs Nex war-proximity check (H) | | `SO_admin_growth` | E; Nex part H | 2 |
| `admin_steward_t1_genelab_accreditation` | TASC `BOGGLED_GENELAB` cost −50% (refund); +1 chosen rare commodity per month to storage (picker dialog) | id gate; cargo add | `SO_admin_build` + `SO_admin_flag` | E / M | 4c |
| `admin_steward_t1_quiet_soil` | "No signature" undefined; AI-inspection frequency on bio-industries = `HegemonyInspectionManager` internals | — | — | X / H, `DECIDE` | — |
| `admin_steward_t2_mesozoic_compact` | Markets with `BOGGLED_MESOZOIC_PARK`: stability +2 | id gate | `SO_admin_market_stat` | E | 4c |
| `admin_steward_t2_stellar_reflector_mastery` | Markets with `BOGGLED_STELLAR_REFLECTOR_ARRAY`: hazard −25%. Terraform speed = TASC-internal | id gate | `SO_admin_market_stat` | E / H | 4c |
| `admin_steward_t2_refugee_underground` | Non-player market in a system with a player market decivilizes → nearest eco-colony growth +50% for 180 days, +Influence while active. **Vanilla `ColonyDecivListener` covers it — no Nex needed** | `ColonyDecivListener` | `SO_admin_flag` + `SO_admin_growth` (`condition: TEMP`) | M | 2 |
| `admin_steward_t2_cryosanctum_diplomatic_lever` | Market with `BOGGLED_CRYOSANCTUM`: +Influence/month. Diplomat retrain speed = nex4x-internal | id gate; `InfluenceBridge` | `SO_admin_flag` + monthly | E (TASC+nex4x); retrain H | 4a/4c |
| `admin_steward_t2_hidden_bio_lab` [C] | Covert | — | stub | blocked | 5 |
| `admin_steward_t3_domed_cities_mandate` | TASC `BOGGLED_DOMED_CITIES` cost −50% (refund). "Lift hazard floor", "garden-tier" undefined | id gate | `SO_admin_build` | E / `DECIDE` | 4c |
| `admin_steward_t3_cloning_pits` | "Crew recovery rate", "population recovery from collapse +100%" undefined | — | — | X, `DECIDE` | — |
| `admin_steward_t3_eden_growth` | Any market decivilizes sector-wide → eco-colonies growth +50% for 180 days (stacking cap needed). "Sector instability events" undefined | `ColonyDecivListener` | `SO_admin_flag` + `SO_admin_growth` | M | 2 |
| `admin_steward_t3_biosphere_guarantee` | Markets running a bio-industry (TASC ids): stability +2. Inspection invisibility = H | id gate | `SO_admin_market_stat` | E / H | 4c |
| `admin_steward_t3_forbidden_bio_production` [C] | Covert | — | stub | blocked | 5 |
| `admin_steward_t4_terraform_acceleration` | TASC terraform project speed = TASC-internal | — | — | H | 4c or cut |
| `admin_steward_t4_garden_stability` | If player total market size is the highest of all factions: all player markets stability +2; non-player markets in systems with a player market growth −25% | `EconomyAPI` aggregation; immigration modifier on **non-player** markets | `SO_admin_market_stat` + `SO_admin_growth` | M | 3 |
| `admin_steward_t4_recovery_cache` | On total fleet defeat, restore at the Genelab world. Vanilla fleet-loss handling must be traced first (R6); 5 SP per use | campaign defeat hook (unknown) | — | H, `DECIDE` | 3 or cut |
| `admin_steward_t4_atmospheric_diplomat` | Fund a TASC processor on a rival world = TASC construction internals | — | — | H | cut or 4c |
| `admin_steward_t4_eco_industry_synergy` | Market with ≥2 of `BOGGLED_GENELAB`/`_HYDROPONICS`/`_CLONING`/`_MESOZOIC_PARK`: each +1 unit | id set count | `SO_admin_output` | E | 4c |
| `admin_steward_t5` core | Genesis World: stability +5. Terraform ×2 (TASC H) | | `SO_admin_designation` | E | 3 |
| `admin_steward_t5` passives | All player markets growth +15%; "sector growth +5%" (all markets — touches every NPC market, `DECIDE`); monthly relations +D15 with all factions; collapse → reset to size 3 (deciv is irreversible in vanilla: re-colonization script H) | immigration, `adjustRelationship` | `SO_admin_growth` + monthly | E / M / H | 3 |
| `admin_steward_t5` active 1 | Environmental Gift: needs `BOGGLED_ATMOSPHERE_PROCESSOR`; target rival market hazard −25% for 365 days (temp mod) + relations | `getHazard` temp mod | `SO_admin_ability` | M (TASC-gated) | 4c |
| `admin_steward_t5` active 2 | Population Surge: 25 Influence → all player markets growth +50% for 180 days | `SO_admin_growth` temp | `SO_admin_ability` | M; Influence D3 | 3 |
| `admin_steward_t5` covert overlay | Bio-Forge | — | — | blocked | 5 |

### 4.5 Feasibility totals (88 nodes; T0/T5 counted once each, judged on their primary V effect)

| Tree | E | M | H | X | Blocked [C] |
|---|---|---|---|---|---|
| Warden | 7 | 10 | 2 | 2 | 1 |
| Merchant | 8 | 5 | 0 | 6 | 3 |
| Prospector | 11 | 4 | 2 | 2 | 3 |
| Steward | 11 | 4 | 3 | 2 | 2 |
| **Total** | **37** | **23** | **7** | **12** | **9** |

60 of 88 nodes (68%) are E/M as written or with a named reinterpretation. Of the 37 E nodes, 12 are
E only with their gating mod present (mostly TASC/AoTD, Phase 4c). 12 need a redesign (X), 7 depend
on another mod's internals or a cross-repo change (H), 9 wait for the covert framework. Partial
effects inside E/M nodes that are X/H are listed in their rows and in §11 D11/D18.

---

## 5. Soft-dependency gating

### 5.1 Rules

1. **No hard deps.** `mod_info.json` gets `softDependencies` entries only (load order).
2. **No reflection.** The sandbox blocks `java.lang.reflect`. Integration is, in order of preference:
   - **(a) Id-only gating** — `Global.getSettings().getModManager().isModEnabled(id)` + string
     industry / condition ids (`market.hasIndustry("IndEvo_ScrapYard")`). Needs no other mod's
     classes. Covers all IndEvo / AoTD / TASC / DoP **E** nodes. Default.
   - **(b) Memory-key contract** with the user's own mods (nex4x, PT): SO writes
     `$so.admin.<knob>` to sector memory; the other mod reads it. No classpath coupling in either
     direction. Requires a commit in that repo (§5.3).
   - **(c) Guarded bridge class** compiled against the other mod's jar: all references to foreign
     classes live in one `integration/<Mod>Bridge.java`, called only after `isModEnabled`. The JVM
     resolves the foreign class lazily, so the bridge never loads when the mod is absent. Needs the
     jar on `build.sh` `CLASSPATH` (compile-only). Use for `InfluenceManager` / `PressureManager`
     reads, where (b) would be clumsy.
   - **(d) `java.lang.invoke.MethodHandles`** only if (c) is impossible (e.g. a jar we cannot put on
     the classpath). Confirm in Phase 0 that the sandbox allows `java.lang.invoke` before relying on
     it.
3. **Every M effect declares its fallback in JSON**:
   `{"type": "SO_admin_output", "requiresMod": "IndEvo", "fallback": {"type": "SO_admin_output", ...}}`.
   `NodeEffectApplier.shouldApply` resolves `requiresMod` once per apply; the fallback applies when
   the mod is absent. Gate decisions are **not** saved — they are re-evaluated every load, so adding
   or removing a mod mid-save switches cleanly.
4. **Tooltips** show "Requires IndEvo — fallback active" so a gated node is never silently dead.
5. **`ModGate`** caches `isModEnabled` results per session (static, reset in `onGameLoad`).

### 5.2 Mod id table

| Mod | `mod_info` id | Used for |
|---|---|---|
| Nexerelin | `nexerelin` | war state, invasions, agents |
| Nex 4X Expansion | `nex4x_expansion` | Influence, Pressure, merc/diplomat actions |
| Industrial Evolution | `IndEvo` | `IndEvo_*` ids |
| AoTD Vaults of Knowledge | `aotd_vok` | `aotd_*` industries |
| AoTD Seats of Power | `aotd_sop` | `aotd_capital_complex` |
| AoTD Dreams of Past | `Cryo_but_better` | `cryorevival`, `reawakening_facility` |
| TASC | `Terraforming & Station Construction` | `BOGGLED_*` |
| Physical Trade | `physical_trade` | overlays (blocked) |

### 5.3 Cross-repo work (not in this repo; separate commits there)

- nex4x: read `$so.admin.mercCostMult`, `$so.admin.diplomatCostMult`, `$so.admin.diplomatSlotBonus`
  in `HireMercenariesAction` / `DiplomatOfficialSupportAction` / agent-slot cap. ~2 tasks, 4 h.
- PT: public `PhysicalTradeAPI` (route-arrival events with value, company spawn) — scope it in a PT
  plan, not here.

---

## 6. Save-compat and listener lifecycle rules

1. **Listeners are transient.** `getListenerManager().addListener(l, true)` or
   `addTransientListener` / `addTransientScript`, re-registered in `onGameLoad`. Never
   `addListener(l)` / `addListener(l, false)` (that saves the instance and duplicates it every load).
   `IndustryOptionProvider` and `MarketImmigrationModifier` instances are added with the transient
   variants (`addTransientImmigrationModifier`).
2. **No stored non-serializable state.** Persistent state is primitives / Strings in
   `MemoryAPI` under `$so.admin.*` (sector memory, or market memory for per-market flags).
   No `MarketAPI`, `Industry`, `FleetAPI` references in fields of anything saved — store ids.
3. **Monotonic day** for every timer and once-per-period check:
   `day = clock.getCycle() * 365 + dayOfYear`, where `dayOfYear` is derived from
   `clock.getCal().get(Calendar.DAY_OF_YEAR)` (the API has no direct accessor; Phase 0 verifies the
   calendar matches 365-day cycles) — never `getDay()` alone (day-of-month, wraps). Put it in one
   util (`AdminTime.today()`), unit-tested for month and cycle rollover. For "N days since" use
   `getTimestamp()` + `getElapsedDaysSince` where a timestamp is stored.
4. **Stat source keys** = `"so_admin:" + nodeId` (+ `":" + industryId`). Removal unmodifies exactly
   those keys; respec must leave no residue — covered by a test that applies, removes, and checks
   each touched stat is back to baseline.
5. **Idempotence:** `apply` twice = `apply` once (refresh-from-scratch design guarantees it).
6. **Existing ids preserved:** `admin_<tree>_t0` and `admin_<tree>_t5` keep their ids. A save that
   owns a placeholder gets real effects on load — acceptable (it is an upgrade); note in CHANGELOG.
7. **Uninstall:** extend `UninstallHelper.prepareUninstall()` to `stripAll()` admin stat mods and
   clear `$so.admin.*`. Abilities granted by admin nodes are removed there too (an unknown ability
   id in a save after uninstall is the one real crash risk).
8. **Vassal markets** (Nex): `AdminXPTracker.isVassal` already exists — `DECIDE` D5 whether admin
   effects apply to vassals.

---

## 7. Test strategy

**Unit tests** (`test/skills_overhaul/admin/`, plain `main` + `assert`, run by `./build.sh test`):

| Test | Covers |
|---|---|
| `AdminTreeLoadTest` | All 4 JSON trees load; 22 nodes each; grid rules; every `type` resolves to a registered handler (no `NoOpHandler`); every `requiresMod` has a `fallback` |
| `AdminTimeTest` | Monotonic day across month/cycle rollover; 365-day expiry math |
| `OutputUnitsTest` | % → units mapping (D6) |
| `AdminEffectSpecTest` | Scope/condition matching as pure functions (market facts passed in as a small value object, no game API) |
| `RefundMathTest` | Refund amount, double-refund guard key |
| `BuildProgressTest` | Daily progress increment yields the intended total time reduction |
| `InterestMathTest` | Banking thresholds, caps |
| `GrowthModifierTest` | Growth percent composition, temp expiry |
| `DesignationServiceTest` | One market per role, 30-day lockout, clear on loss |
| `WarStateTest` | Hostility matrix → at-war / ≥2 wars / decay window |
| `HandlerRoundTripTest` | apply → remove → baseline for each shared handler against a fake stat holder |

Handlers keep pure math in static methods (as `SOInGoodHandsHandler.reductionForLevel`) so they test
without the game.

**No compiler for data.** JSON is only checked by `AdminTreeLoadTest` + loading the game. After any
data change: launch, open the Admin tab, check `starsector.log` for `[SkillsOverhaul]` warnings
(unknown effect type is logged by `NoOpHandler`).

**In-game checklist** (per phase, in the current profile; AoTD/TASC rows need the second profile):

1. Load an existing save from before this work — no errors; placeholder T0/T5 ownership preserved.
2. Buy each T0; designate via the population industry panel; colony screen shows the
   `so_admin:` modifiers in stability / accessibility / income tooltips.
3. Swap active T0 out → core effects vanish, ribbon stays.
4. Respec the admin tab → every modifier gone (inspect stability, accessibility, fleet size, quality
   tooltips on two colonies).
5. Save, reload → modifiers identical (no duplicates, no loss).
6. Advance 1 month → interest / Influence / reserve / relation ticks fire once.
7. Start a building under a refund node → one refund message, not two after reload.
8. Found a colony with Boom Town → build-time reduction visible in days remaining; hazard tooltip
   shows the first-cycle modifier.
9. Lose a market (console `setmarketowner` or abandon) → designation clears; Dead-Man Doctrine fires.
10. Disable IndEvo in a test profile → IndEvo nodes show "fallback active", no crash.
11. `UninstallHelper.prepareUninstall()` → save → disable mod → load: no crash.

---

## 8. Files

**Create** (`src/skills_overhaul/`):
`admin/AdminMarketEffectService.java`, `admin/AdminEffectSpec.java`, `admin/StatOp.java`,
`admin/AdminDesignationService.java`, `admin/AdminDesignationOptionProvider.java`,
`admin/AdminDailyTicker.java`, `admin/AdminMonthlyListener.java`, `admin/AdminActiveNodes.java`,
`admin/AdminTime.java`, `admin/WarState.java`, `admin/ModGate.java`,
`integration/Nex4xBridge.java` (Influence/Pressure), abilities under `admin/ability/`,
handlers `effect/handlers/admin/SOAdmin{MarketStat,Output,Build,Designation,Growth,Flag,Ability}Handler.java`.

**Modify:** `effect/EffectRegistry.java` (register 7 types), `effect/NodeEffectApplier.java`
(`requiresMod`/`fallback`), `effect/handlers/SOFleetStatHandler.java` (cargo/fuel keys),
`SkillsOverhaulModPlugin.java` (register listeners/scripts/provider),
`console/UninstallHelper.java` (`stripAll`), `mod_info.json` (soft deps),
`data/config/skills_overhaul/trees/admin_{warden,merchant,prospector,steward}.json`,
`data/campaign/abilities.csv` (admin actives), `data/config/skills_overhaul.json` (tunables),
`build.sh` (compile-only classpath for the nex4x jar, if §5.1(c) is used), `CHANGELOG.md`.

---

## 9. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | Externally keyed industry supply mods may be rebuilt by `BaseIndustry.apply()` each tick | Phase 0 spike; fallback = re-apply after tick (designed in) |
| R2 | Refund-on-start may double-refund across save/load or on upgrade vs new build | refunded-key per market+industry+build start timestamp; unit test |
| R3 | Active-basics slot (2) is shared across Pilot/Commander/Admin — admin designation competes with combat T0s | D2 |
| R4 | Monthly credit/Influence generators (Banking, Exchange +100k, Hegemony) are exploit-prone | Config caps; D14 |
| R5 | Per-commodity price hooks (Total War, Commodity Bias, black-market income) may not exist | Phase 0; cut if absent |
| R6 | Unverified costs: colonization cost formula, makeshift-objective build cost, megastructure restoration cost, fleet-total-loss flow | Phase 0 traces with `starsector-decompile`; nodes stay `H` until traced |
| R7 | AoTD/TASC paths untestable in the current profile | Build a second test profile before Phase 4c. Until then the id-gated code ships gated but unverified, and the CHANGELOG says so |
| R8 | Hostile Activity counter-factor could push progress negative / interact with crisis resolution XP (`AdminXPTracker.CRISIS_VALUES`) | Clamp to −50% of positive monthly progress, never below 0 |
| R9 | Another agent commits on the same branch | Plan touches only new files until Phase 1; JSON trees owned by this plan only |
| R10 | Growth modifiers on **non-player** markets (Garden Stability, Genesis) change NPC economies | D16 |

---

## 10. Out of scope

Covert-market framework; PT public API; TASC/AoTD internals (terraform speed, item effects,
techmining yields); any UI beyond the industry-panel designation button and ability tooltips.

---

## 11. Decisions needed from the user (not decided here)

- **D1 — Power budget.** Vanilla admin skills are suppressed; admin trees must replace Industrial
  Planning / Hypercognition. Confirm target: "≥ vanilla at equal points" or "stronger, by how much".
- **D2 — Active T0 slots.** `ActiveLaneService.MAX_ACTIVE = 2` is global across tabs. Should admin
  T0 designations count against it, have their own 2 slots, or always be active?
- **D3 — Influence without nex4x.** Influence exists only in nex4x. Fallback: credits (at what
  rate), story points, or "node inert without nex4x"?
- **D4 — Physical Trade overlays.** Require a PT public API (none exists; `ShortageDebitLedger` is
  not a fee ledger). Build that API in PT first, or drop PT overlays from v1?
- **D5 — Scope.** Effects on all player-faction markets, only markets the player governs
  (vanilla Industrial Planning scope), and/or Nex vassals?
- **D6 — Output percentages.** Vanilla output is integer units. Approve the % → units mapping in
  §1.3 (15%→+1, 50%→+2), or specify units per node.
- **D7 — Build-cost reductions.** Only implementable as a refund after the build starts (UI shows
  full price). Accept, or convert all cost reductions to time reductions?
- **D8/D9/D10 — Undefined units.** "+1 ground defense rating" (flat or %?), "+1 ship quality tier"
  (quality is a 0–1 float; proposal +0.1 per tier), "+1 accessibility" (proposal +10%).
- **D11 — Redesign required (X items).** These spec effects have no game mechanic to hook; each
  needs a replacement effect or a cut:
  Warden: Drilled Quartermasters, Customs Mandate, Bastion "tier −1", Hardened Logistics inspection
  part, HA "suppression action cost", Forge Capital per-build cost/D-mod;
  Merchant: Tariff Specialist, Customs Cooperation, Logistical Genius "tier −1", Megaport Charter,
  Offshore Vault, Free Port Empire pooling, Trade House "visiting tariff" + ribbon inspection part,
  Convoy Coordination V fallback;
  Prospector: Vein Reading, Off-Grid Refining;
  Steward: Quiet Soil, Cloning Pits, Domed Cities "hazard floor/garden-tier".
- **D12 — "War" definition.** Armory Compact / Volatiles Cartel / Crisis Speculator / Wartime
  Production need one definition: any two major factions hostile (always true in vanilla — Hegemony
  vs. pirates/Path), player hostile to a major, or Nex formal war only?
- **D13 — Standing Reserve "no cooldown".** Free defense fleets with no cooldown are farmable;
  add a cooldown or a reserve cost floor?
- **D14 — Interest economy.** 1.5–3%/month on 500k+ is 90–180k/cycle passive at the threshold and
  unbounded above it. Cap?
- **D15 — Relation units.** "+0.25 / +1 relations per cycle" — Starsector reputation is −1..1
  (100 points). Is "+1" one point (0.01)?
- **D16 — NPC-market effects.** Garden Stability (rival growth −25%), Genesis "sector growth +5%",
  Embargo modify other factions' markets. Allowed?
- **D17 — Prereq edges and capstone cap.** Spec gives no prereqs; plan uses column chains + T5
  `atLeast 2` of T4. Spec "max 2 capstones per character via SP gating" has no mechanism in the
  current point system — implement as a hard check in `SpendService`, or drop?
- **D18 — Hard nodes to cut or keep as stubs:** AI Inspection Brokerage, Commodity Bias, Recovery
  Cache, Atmospheric Diplomat, Terraform Acceleration, Domain Archaeology Lead, megastructure
  restoration actives, Invasion Coordinator Nex part.
