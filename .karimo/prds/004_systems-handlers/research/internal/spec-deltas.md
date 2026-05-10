# Spec Deltas — items researcher missed

Re-read of `2026-04-30-pilot-systems-spec.md` revealed details research
either missed or got wrong.

## §5.4 VoidStrikeWindow (lines 106-116)
- All ordnance: +35% (phase) / +30% (non-phase) damage during window
- All weapons: +20% (phase) / +15% (non-phase) damage
- **Mines: +40% damage PERMANENT after first Void Strike** (additive, stored in MemoryAPI)
- Research note "getOrdnanceSpeedBonus computed but not applied" stands;
  spec confirms ordnance speed should also be applied during window

## §5.5 TargetPainterState (line 124)
- **Stat is `getShieldEfficiencyMult`, NOT `getShieldDamageTakenMult`** —
  research used wrong stat name. Per spec:
  ```
  target.getMutableStats().getShieldEfficiencyMult()
        .modifyMult("target_painter", 1f + stacks × 0.03f)
  ```
- This means more flux per shield-hit on target (efficiency mult > 1)

## §5.7 ExperimentalOverrides framework (line 144)
- **Kill Sync integration:** secondary system activations count as standard
  system uses for KillSync cooldown reduction. Plugin must call KillSync
  on every secondary activation event (track `lastSecondaryActivationTime`).
- Affects T6 wire-up — verify hookup.

## §5.2 ReactiveSystemsTracker (line 45) — exact values
- Phase ships: every second phased → −0.75s system cooldown (after min 3s phased)
- Non-phase ships hull-class scaling:
  - Frigate: 1s per 50 dmg
  - Destroyer: 1s per 100 dmg
  - Cruiser: 1s per 200 dmg
  - Capital: 1s per 400 dmg

## §3 line 14-16 — node tier shuffle
- EMP Specialist moved T1 → T2 (swapped with Kill Sync)
- Kill Sync moved T1 → T3
- Phase Resonance T3 renamed "Reactive Systems"
- Phase Resonance MERGE remains stub (different node from Reactive Systems!)

## §4 line 50 — Experimental Overrides activation
- Spec says "registers keybind listener via CombatPlugin"
- T0 must verify keybind name + lifecycle
