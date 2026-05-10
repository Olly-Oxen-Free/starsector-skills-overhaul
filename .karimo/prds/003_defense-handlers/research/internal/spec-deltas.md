# Spec Deltas — items researcher missed

Read of full spec doc `2026-04-30-pilot-defense-spec.md` revealed values
the initial research either omitted or stated vaguely. Captured here so
implementing tasks can pull exact values without re-reading spec.

## §5.4 AdamantineState (line 39, 102-112)
- Discharge AoE: 300 energy + **1500 EMP** (research said 1200) — spec marks [TUNE]
- Discharge AoE radius: **600u**
- **5s minimum cooldown between discharges** (research missed)
- Buffer recharges via tracking **`lastKnownFlux` each frame** (research had R4 flagging stale maxFlux but didn't capture the recharge mechanism)
- Buffer cap formula: `min(currFlux × 1.0, maxFlux × 0.20)`

## §5.5 IronBastionState (line 118-120)
- PD bonus: **+20% fire rate + 25% turn rate** (specific values)
- Active-damage gate: hull or armor delta in last **0.5s**
- Fighter-kill heal: **5% of killed fighter's max HP**

## §4 Module Layout — Lead From the Front (line 41)
- IntervalUtil **0.5s** scan
- Range: **1500u**
- Allies: +5% DR (hull+armor) / +3% wpn dmg
- Fighters: +3% DR / +2% wpn dmg
- Self: +1% DR / +1% wpn dmg per ally in range, max +10% each

## §4 Module Layout — Unbreakable (line 57)
- Floor 20%→50% per armor cell
- **8s block + 15s cooldown PER LOCATION** (research said only "per-cell floor")
- Implies per-cell timer state, not single ship-wide flag

## §4 Module Layout — Overwhelming Force (line 61)
- Gated on `ship.getEngineController().isAccelerating()` (advancing condition)
- +25% flux dissipation
- +20% hard-flux dissipation while shields raised
- (Research said "flux dissipation mults per spec" without the advancing gate)

## §4 Module Layout — Fortress Shield (line 50)
- +10% absorption per hit (already partial)
- +20° arc bonus (already done)
- −50% hard-flux spike on breach (research captured)

## §5.1 FortifyState (line 71-82)
- Trigger: hits >100 combined hull+armor damage (spec exact threshold)
- Stack 0-5, +5%/stack DR
- Decay: −1 stack per second after 0.5s no qualifying hit
- Apply: hull + armor + **shield** DR (research correctly noted shield missing)

## §5.2 TargeState (line 86-92)
- +1% wpn dmg per 1% hull lost, max +50%
- Hull regen 0.1%/s (= `maxHull × 0.001 × dt`)
- Spec line 92 explicitly notes: "If no direct regen API: apply via RepairTracker
  or synthetic repair event — inspect API and document fallback" — research's R2
  captured this concern correctly

## §5.3 DeathsDoorState (line 94-100)
- Trigger: `ship.getHullLevel() < 0.10f`
- Window: 4s
- One-shot per combat (`used = true` flag)
- EMP/flux NOT suppressed (research correctly noted)
