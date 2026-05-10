# T0 — Defense PRD Investigation Findings

Resolves R1-R5 open questions before downstream tasks begin.

## R1 — Damage interception API (0.98a-RC8)

**Available in `starfarer.api.jar` `com.fs.starfarer.api.combat.listeners`:**
- `DamageDealtModifier` ✓ already used in `ZealotArmorCritListener`, `SOFurorMachinaeLogic`, `SOVoltaicChainLogic`
- `DamageTakenModifier` ✓ NOT yet used in mod
- `DamageListener` ✓ used in `PilotXPTracker:128`

**NOT available** (confirmed by codebase comments + jar listing):
- `ShipDestroyedListener` — `SOMindCascadeListener:14` notes "no ShipDestroyedListener in this API version". Use polling fallback (`SODistributedConsciousnessListener:18` `wasAlive` Map<ShipAPI,Boolean> pattern).
- `TakeDamageResultListener` — not in jar.

**Decisions:**
- **Unbreakable per-cell floor** → register `DamageTakenModifier` on flagship. On hit, inspect armor cell coordinates via `ship.getArmorGrid()`, raise effective floor to 50%, set per-cell `unbreakable_lastTriggered` timestamp; gate re-trigger on 8s block + 15s cooldown per cell (spec line 57).
- **Fortress Shield breach** → register `DamageTakenModifier` on flagship; check shield-collapse event by polling `ship.getShield().isOff()` transition; on breach frame, cap hard-flux delta to 50% via `getFluxTracker().setHardFlux(prevHard + delta*0.5)` or similar.
- **IronBastion fighter-kill heal** → polling pattern (`wasAlive` Map). Each frame, scan `engine.getShips()` for fighters whose `getOwner()==flagship.getOwner()` (or whose `getWing().getSourceShip()==flagship`) that transitioned alive→dead. Heal 5% of dead fighter's `getMaxHitpoints()`.
- **AdamantineState hull-damage intercept** (spec line 108) → register `DamageTakenModifier`; on hull damage, reduce by `min(damage, bufferRemaining)`, decrement buffer.

## R2 — ACTIVE_NODES persistence design

**Decision: Option B (re-invoke handler.apply() on game load).**

Rationale:
- Avoids serialization complexity of LinkedHashSet contents.
- Source of truth = purchased-node ledger in `MemoryAPI` (already persistent via vanilla save).
- Single hook in `SkillsOverhaulModPlugin.onGameLoad()` iterates owned nodes, calls `EffectRegistry.handlerFor(typeId).apply(nodeId, params)` for each.

Implementation outline:
```java
// SkillsOverhaulModPlugin.onGameLoad()
TreeRegistry.forEachOwnedNode(mem, new Visitor() {
  public void visit(String nodeId, Node node) {
    EffectHandler h = EffectRegistry.handlerFor(node.effectType());
    if (h != null) h.apply(nodeId, node.effectParams());
  }
});
```

Apply same fix to gunnery (`GunneryCombatPlugin.ACTIVE_NODES`), maneuvering, systems plugins. Hook fires in T6.

## R3 — DR multiplicative stacking guard

Apply clamp in `DefenseCombatPlugin.applyMods()` after Fortify/DeathsDoor/Immovable Object writes:
```java
float min_mult = 0.05f;  // permit 95% max DR, never invuln
if (combinedHullMult < min_mult) {
  // re-normalize all sources proportionally
}
```
Defer detailed impl to T8 (DR cap guard task).

## R4 — Mass override path

`MutableShipStatsAPI` does NOT expose `getMass()` modifier. `ship.getMass()` is read-only on `ShipAPI` (returns hull-spec mass).

**Decision:** `massMult` (Immovable Object) implementation requires either:
- (a) Spawning ship with modified `getHullSpec().getMass()` (impractical mid-combat), OR
- (b) Skip; document as won't-fix in T4. Pivot Immovable Object to provide alternative defensive value (e.g., higher armor or `getCollisionDamageDealtMult` only).

Recommend (b) — flag for design discussion. T4 implements other Immovable Object params (weaponHPBonus, engineHPBonus, collisionDamageDealt) and documents `massMult` as deferred.

## R5 — `Stats.CR_MALFUNCION_RANGE` access path

**Confirmed exists in 0.98a-RC8:** `com.fs.starfarer.api.impl.campaign.ids.Stats.CR_MALFUNCION_RANGE` (string constant `"cr_malfunction_range_mult"`).

Usage pattern (per SiC `SCCRPluginImpl.java:395`):
```java
stats.getDynamic().getStat(Stats.CR_MALFUNCION_RANGE).modifyMult(id, mult);
```

For Battle Hardened `crMalfunctionThreshold:0.30`: `modifyMult(id, 1f - 0.30f)` shifts malfunction band down by 30%.

## Reference mod pattern citations

- **Polling fighter-kill** (no ShipDestroyedListener): `SODistributedConsciousnessListener:18-50` (`wasAlive` Map pattern).
- **DamageDealtModifier impl**: `ZealotArmorCritListener.java:19` (Shape A signature) — copy for damage-taken-mod variant.
- **CR_MALFUNCION_RANGE**: SiC `SCCRPluginImpl.java:395`.
- **DamageListener (XP)**: `PilotXPTracker.java:128` — read-only damage observation; not what we need (we need modification, hence `DamageTakenModifier`).

## Summary table

| R | Decision |
|---|----------|
| R1 | DamageTakenModifier + polling for ship destroyed |
| R2 | Re-invoke handler.apply() on game load via owned-node ledger |
| R3 | Clamp combined DR mult ≥0.05 in applyMods (T8) |
| R4 | massMult won't-fix; document; pivot Immovable Object value (T4) |
| R5 | `Stats.CR_MALFUNCION_RANGE` — confirmed; SiC pattern |

T0 complete. Downstream tasks (T2, T3, T4, T6) unblocked.
