# Research Findings — systems-handlers

## Spec inventory

22 nodes per `2026-04-30-pilot-systems-spec.md`. **Note:** spec line 17/73/202 declares `Phase Resonance Merge` an intentional v1 stub — exclude from gap fixes.

## Per-node status (vs spec)

### TRULY MISSING (handler/plugin block absent)
- **EMP Specialist** (T2) — handler register no-op; spec line 178: +20% EMP damage + 3s weapon-turn debuff on EMP hit
- **Strike Coordination** (T2) — dead; spec line 182: bomber +25%dmg/+20%HP + 15% target damage-taken on bomber hit (5s)
- **Phase Mastery** (T4) — dead; spec line 194: phase speed-penalty removal + exit burst 300 energy + 600 EMP scaled by flux
- **Carrier Command** (T4) — dead; spec line 198: bomber wing teleport to carrier on target-kill, 30s CD
- **Phase Resonance Merge** — **INTENTIONAL STUB** (spec line 202) — no-op v1 placeholder, "Coming Soon" tooltip. Skip.

### PARTIAL / SUB-MECHANIC GAPS
- **Capacitor Banks** — `ventRateBonus` log-and-skip; `fluxCapBonus` works
- **Energy Mastery** — `energyFluxReduction` skipped (default warn branch)
- **Precision Optics** — `energyDamageBonus`, `energyRangeBonus`, `allWeaponTurnRateBonus` all skipped
- **ECM** — `sensorProfileReduction`, `ecmRatingBonus`, `enemyAccuracyReduction` all skipped
- **Quickened Response** — `shieldRotationSpeedBonus`, `shieldRaiseSpeedBonus`, `maneuverBonus` (line 88 explicit skip) all skipped
- **Air Superiority** — `bomberDamageVsCruiserCapital`, `interceptorSpeedBonus`, `interceptorDamageVsFighters` skipped — needs custom DamageTakenModifier + wing API
- **Target Painter** — bug: `onShieldDamage(0f, gameTime)` always passes 0 damage. Stacks only via target-switch; damage-threshold path dead (SystemsCombatPlugin:160-161)
- **Red Line** — `getCRBonus()` computed but never applied to mutableStats
- **Void Strike** — `getOrdnanceSpeedBonus()` computed but never applied (line 17)
- **Automata Integra** — sets memory key `$so_automata_integra_enabled`; spec line 192: needs `PlayerShipChangedEvent` hook to skip automated-ship-point deduction. Currently no consumer.
- **Synaptic Relay** — `remove()` doesn't call `SynapticRelayService.remove()` → `so_neural_uplink` hullmod LEAKS on respec, persists on all fleet members permanently

### WORKING
Kill Sync, Phase Coil, Flux Exploitation, Reactive Systems, Experimental Overrides

## ACTIVE_NODES audit (cross-cutting)

`SystemsCombatPlugin:15` — `public static final Set<String> ACTIVE_NODES`. `init()` line 42 calls `clear()`. **Same save-reload bug as defense/gunnery/maneuvering plugins.** Confirms cross-lane systemic issue. Fix in defense PRD T6 must cover this plugin too.

Memory-key handlers (Automata Integra, Experimental Overrides) are SAFE — `Global.getSector().getMemoryWithoutUpdate()` persists through save-reload natively.

## ShipSystemAPI surface (mostly UNUSED in mod)

| API | Currently used? |
|-----|-----------------|
| `getCooldownRemaining/setCooldownRemaining` | YES (KillSync, ReactiveSystems) |
| `getAmmo/setAmmo`, `setFluxPerUse`, `setCooldown` | NO |
| `getSystemCooldownBonus`, `getSystemFluxCostBonus`, `getSystemRegenBonus`, `getSystemUsesBonus`, `getSystemRangeBonus`, `getPhaseCloakCooldownBonus` | **NONE used.** Campaign-layer stat-bonus path entirely unused. |

Phase-speed penalty for Phase Mastery: `getPhaseSpeedMult().modifyMult("phase_mastery", 1f/vanillaPenaltyFactor)` per spec line 194.

## SiC reference

SiC and SiC-SO have no `ShipSystemAPI` Java patterns to copy. SiC modifies systems through MutableShipStats DSL only. No drop-in pattern available. Current `setCooldownRemaining()` approach in KillSync/ReactiveSystems is the correct pattern — no refactor needed.

## Risks

- **R1** ACTIVE_NODES save-reload (already in defense PRD T6 — extends here)
- **R2** Target Painter dead damage path (`onShieldDamage(0f)`) — clear bug
- **R3** SynapticRelay hullmod leak — clear bug; fleet-wide persistent buff after respec
- **R4** Phase Mastery vanilla phase-speed formula — needs inversion math; risk of overshoot
- **R5** Strike Coord + Carrier Command both need bomber-hit detection. No generic on-weapon-hit hook in API. DamageDealtModifier or per-frame target damage-delta polling. T0 verifies.
- **R6** EMP hit detection — `target.getEMPDamageStaggered()` delta polling vs DamageTakenModifier
- **R7** `enemyAccuracyReduction` (ECM) — no per-combat API; campaign-layer only

## Recommended task slicing — 7 tasks

1. **T0** — Investigate: bomber-hit detection (Strike Coord, Carrier Command), EMP-hit detection (EMP Specialist), `getPhaseSpeedMult` API + vanilla penalty factor (Phase Mastery), Wing API for fighter stats (Air Superiority), `PlayerShipChangedEvent` hook (Automata Integra). Output decisions.
2. **T1** — Fix bugs: Target Painter damage delta tracking, SynapticRelay remove() leak, Red Line CR application, Void Strike ordnance speed application. (4 isolated fixes.)
3. **T2** — Implement EMP Specialist + Strike Coordination plugin blocks (both need T0 hit-detection path).
4. **T3** — Implement Phase Mastery + Carrier Command (both phase/carrier-flavored, T4 nodes).
5. **T4** — Implement Automata Integra consumer: hook `PlayerShipChangedEvent`, gate automated-ship point cost on memory flag.
6. **T5** — Extend SOPilotedStatHandler for the 11+ skipped systems-tagged params (capacitor_banks ventRate, energy_mastery, precision_optics x3, ecm x3, quickened_response x3). Plus air_superiority carrier/wing stats (split into sub-task; may need plugin assist).
7. **T6** — Wire-up + smoke. Include cross-plugin ACTIVE_NODES verification (relies on defense T6 fix landing first).

## Cited sources

- `data/config/skills_overhaul/trees/pilot_systems.json` (uncommitted)
- `src/skills_overhaul/effect/EffectRegistry.java:172-198`
- `src/skills_overhaul/effect/handlers/systems/*.java`
- `src/skills_overhaul/pilot/systems/SystemsCombatPlugin.java:15, :42, :160-165, :17`
- `src/skills_overhaul/effect/handlers/SOPilotedStatHandler.java:88`
- `src/skills_overhaul/effect/handlers/systems/SOSystemsSynapticRelayHandler.java:14`
- Spec: `docs/superpowers/specs/skills-overhaul/pilot/systems/2026-04-30-pilot-systems-spec.md:14-202`
