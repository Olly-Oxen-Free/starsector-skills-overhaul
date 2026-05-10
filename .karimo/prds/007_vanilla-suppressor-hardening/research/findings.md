# Research Findings — vanilla-suppressor-hardening

## Spec source

`docs/superpowers/specs/skills-overhaul/2026-04-18-skills-overhaul-design.md` §7.2 (lines 339-345).

**Spec mandates:**
1. On every `onGameLoad()` (not just first): iterate player-owned vanilla skills.
2. **Unregister registered `SkillEffect` modifiers from `player.stats.*`** — explicit unregister, not just level-zero.
3. Replace vanilla character-screen rules.csv trigger with custom-panel trigger.
4. Don't intercept vanilla XP ledger — let it accrue silently.

**Uninstall safety (spec §7.3):**
- Our suppression stops firing on uninstall → vanilla SkillEffects re-register automatically.
- Vanilla level + unspent points intact.
- Memory keys orphaned but harmless.

## Current implementation

`src/skills_overhaul/suppress/VanillaSuppressor.java` (60 LOC).

```java
applyOnEveryLoad():
  for each non-so_ skill with level > 0:
    stash level under SUPPRESS_TAG memory key (idempotent)
    s.setSkillLevel(id, 0f)
```

**FIXME at line 14** (author-acknowledged): "verify setSkillLevel(id, 0f) actually unregisters the vanilla skill's stat mods on an existing save — if vanilla caches bonuses elsewhere, may need additional teardown path."

**Caller:** `SkillsOverhaulModPlugin.java:104` — `applyOnEveryLoad()` invoked on game load.

## Gap vs spec

| Spec requirement | Current impl | Gap |
|------------------|--------------|-----|
| Iterate player-owned vanilla skills | ✓ `getSkillsCopy()` filtered by `!startsWith("so_")` | none |
| Unregister registered `SkillEffect` modifiers | ✗ only calls `setSkillLevel(id, 0f)` | **likely incomplete — see R1** |
| Idempotent across reloads | ✓ memory check before stash | none |
| Replace character-screen rules.csv trigger | ✓ via `CharacterTabReplacer` (separate class) | none |
| Don't intercept XP ledger | ✓ no XP code touched | none |
| Uninstall safety | ✓ `restoreAll()` re-applies stashed levels | none for restore — but if `setSkillLevel(0)` didn't fully unregister, vanilla effects may have lingered all along (no harm on uninstall) |

## Risks (spec-driven)

- **R1** `setSkillLevel(id, 0f)` may not invoke `SkillEffect.unapply()` for skills that cached stat mods at registration time. Vanilla `SkillSpecAPI` and `SkillEffect` lifecycle: when `setSkillLevel` writes a lower value, vanilla recomputes effects — but some `SkillEffect` impls may have applied permanent stat mods (modifyFlat/modifyMult with character-stat source ID) that persist regardless of level. **Test required: confirm vanilla skill stat bonuses actually disappear after suppression.**
- **R2** Officers — spec §6 says officers should still get vanilla skills. `applyOnEveryLoad` only touches `getPlayerPerson()` (line 22) — correct. **Verify:** officer character-stat suppression does not occur. Spec §6 line 362: "All 72 vanilla .skill files are preserved unmodified" — officers untouched is correct.
- **R3** Mod-added vanilla-style skills (UAF, Tahlan, VIC custom skills) — spec §13 line 513 lists as untested. `!startsWith("so_")` filter would also suppress these. May be intended (treat them like vanilla) or a bug (mod-author intent). **Decision needed.**
- **R4** New vanilla skills added in Starsector updates — `!startsWith("so_")` future-proofs against new vanilla skills (will be auto-suppressed). 
- **R5** `getSkillsCopy()` returns snapshot — modifying `s.setSkillLevel` during iteration is safe (operates on underlying stats not the copy), but verify no `ConcurrentModificationException` on edge cases.
- **R6** Memory key naming `$so_suppressed_<id>` — spec §7.3 says memory keys orphan harmlessly on uninstall. Verify uninstall + reinstall sequence: stashed key from previous install would re-apply old level via `restoreAll()` if invoked. Currently `restoreAll()` is not auto-invoked anywhere — only on explicit uninstall hook (which doesn't exist as a trigger). **Investigate uninstall path.**
- **R7** No integration test exists. `test/skills_overhaul/suppress/` directory not present.

## Reference mods

- **SiC** — does not suppress vanilla skills (additive, not replacement). No applicable pattern.
- **LunaLib** — config-only, no skill manipulation.
- No mod in workspace ecosystem replaces vanilla skills like Skills Overhaul does. Pattern is novel — must be tested empirically.

## API surface (Starsector 0.98a-RC8)

`MutableCharacterStatsAPI`:
- `setSkillLevel(String skillId, float level)` — writes level. **Behavior on level=0 not documented.**
- `getSkillsCopy()` — returns `List<SkillLevelAPI>` snapshot.
- `getSkillLevel(String)` — read.
- `addSkillPoints(int)`, `removeSkillPoints(int)` — point manipulation.

`SkillSpecAPI` / `SkillEffect`:
- Each skill has 1+ `SkillEffect` instances (see `Skills.java` per-skill files).
- `SkillEffect.apply(stats, level, hadPrev)` and `unapply(stats)` — vanilla calls these on level changes.
- Theoretically `setSkillLevel(0)` should call `unapply` — needs runtime verification.

## Recommended task slicing — 4 tasks

1. **T0** — In-game test: load save with vanilla skills → confirm stat bonuses disappear post-suppression. Test cases: each aptitude (Combat, Leadership, Industry, Tech), at level 1, 5, 10. Capture ship-stat snapshots before vs after. Output: empirical result table.
2. **T1** — If R1 confirmed (stat mods persist): add explicit `SkillEffect.unapply()` invocation. Read each skill's effects via `Global.getSettings().getSkillSpec(id).getEffects()`, iterate, call `unapply(stats)`. Stash level + flag suppressed.
3. **T2** — Mod-added vanilla skills decision (R3): document intent (suppress all non-so_ vs whitelist `vanilla:` prefix). If suppress-all kept, confirm in CHANGELOG/README. If whitelist needed, add config knob.
4. **T3** — Add integration test + uninstall hook + smoke. Test class invokes mock CharacterStats via reflection; verify pre/post stat values; verify `restoreAll()` round-trip. Add explicit uninstall path (CharacterTabPanelPlugin already replaces tab — needs symmetric uninstall? Or rely on mod removal). CHANGELOG entry.

## Cited sources

- `src/skills_overhaul/suppress/VanillaSuppressor.java:14, :22, :30, :40-50`
- `src/skills_overhaul/SkillsOverhaulModPlugin.java:104`
- Spec: `docs/superpowers/specs/skills-overhaul/2026-04-18-skills-overhaul-design.md:339-356, :513, :59, :88, :99`
- Starsector API (no online doc readily citable for `setSkillLevel(0)` behavior — empirical test required)
