# PRD: Pilot/Systems Lane — Spec Gap Closure

- **Slug:** systems-handlers
- **Created:** 2026-05-10
- **Status:** draft
- **Spec source:** `docs/superpowers/specs/skills-overhaul/pilot/systems/2026-04-30-pilot-systems-spec.md`

## 1. Summary

Close gaps between Pilot/Systems lane and v1 spec. **4 truly missing nodes** (EMP Specialist, Strike Coordination, Phase Mastery, Carrier Command). **11+ partial nodes** with skipped sub-mechanics. **2 clear bugs** (Target Painter dead damage path, SynapticRelay hullmod leak on respec). Phase Resonance Merge intentionally a v1 stub per spec — excluded.

## 2. Why

T2/T4 unlock paths invest points in dead nodes. Synaptic Relay leaves a permanent fleet-wide hullmod even after respec. `SO_piloted_stat` for capacitor_banks/energy_mastery/precision_optics/ecm/quickened_response/air_superiority silently drops most params at handler default-warn branch.

## 3. Scope

### Truly missing → implement
- EMP Specialist (T2), Strike Coordination (T2), Phase Mastery (T4), Carrier Command (T4)

### Bug fixes
- Target Painter `onShieldDamage(0f)` always-zero (`SystemsCombatPlugin:160-161`)
- SynapticRelay `remove()` leak — must call `SynapticRelayService.remove()`
- Red Line `getCRBonus()` computed but never applied
- Void Strike `getOrdnanceSpeedBonus()` computed but never applied

### SO_piloted_stat key extensions
- ventRateBonus, energyFluxReduction, energyDamageBonus, energyRangeBonus, allWeaponTurnRateBonus, sensorProfileReduction, ecmRatingBonus (campaign-layer note), enemyAccuracyReduction (feasibility check), shieldRotationSpeedBonus, shieldRaiseSpeedBonus, maneuverBonus
- Air Superiority: bomberDamageVsCruiserCapital, interceptorSpeedBonus, interceptorDamageVsFighters (custom DamageTakenModifier + wing API)

### Other
- Automata Integra consumer: hook `PlayerShipChangedEvent` to gate automated-ship point cost on memory flag

### Out of scope
- **Phase Resonance Merge** — intentional v1 stub per spec line 202
- ACTIVE_NODES save-reload fix — covered by defense PRD T6 cross-cutting (this PRD verifies coverage in smoke task)

## 4. Success Criteria

- `./build.sh` + `./build.sh test` clean.
- 4 missing nodes produce in-combat behavior per spec.
- All `SO_piloted_stat` skipped keys for systems lane produce stat changes (no default-warn log lines for these keys).
- Target Painter accumulates real damage stacks; SynapticRelay hullmod removed cleanly on node refund.
- Red Line CR + Void Strike ordnance speed actually applied.

## 5. Risks

- **R1** Bomber-hit detection — no generic on-weapon-hit hook. Need DamageDealtModifier or per-frame target damage delta. T0 decides.
- **R2** EMP hit detection — `target.getEMPDamageStaggered()` delta polling vs DamageTakenModifier.
- **R3** Phase Mastery: invert vanilla phase-speed penalty formula (`getPhaseSpeedMult` modifyMult). Verify factor.
- **R4** Wing API for Air Superiority — `getWingAPI()` / `getFighterRefitData()` call sites.
- **R5** `enemyAccuracyReduction`: likely no per-combat API. May need to be tooltip-only or removed from spec.
- **R6** ACTIVE_NODES save-reload bug present in SystemsCombatPlugin too — defense PRD T6 must cover it; this PRD's T6 verifies.

## 6. Tasks

7 tasks. T0 investigation gates T2/T3/T5. T1 (bug fixes) parallel. T4 (Automata Integra) independent. T6 final wire-up.

## 7. Boundaries

- **require_review:** `data/config/skills_overhaul/trees/pilot_systems.json` (already uncommitted/modified — caution).

## 8. Open Questions (resolved in T0)

- Bomber-hit detection API path
- EMP-hit detection API path
- Vanilla phase-speed penalty factor
- Wing API surface for fighter-type-specific stats
- `enemyAccuracyReduction` feasibility
- `PlayerShipChangedEvent` exact hook name
