# PRD: Vanilla Suppressor Hardening

- **Slug:** vanilla-suppressor-hardening
- **Created:** 2026-05-10
- **Spec source:** `docs/superpowers/specs/skills-overhaul/2026-04-18-skills-overhaul-design.md` §7.2-7.3

## 1. Summary

Verify and harden the vanilla-skill suppression path. Current `setSkillLevel(id, 0f)` may not actually unregister vanilla `SkillEffect` stat mods (author-acknowledged FIXME at `VanillaSuppressor.java:14`). Spec mandates **explicit unregister of registered SkillEffect modifiers** — current impl only zeros the level. If vanilla caches stat bonuses on apply, players may keep vanilla bonuses despite mod suppression.

## 2. Why

Whole mod premise depends on vanilla skills being silent. If suppression is incomplete, players get vanilla skill bonuses + Skills Overhaul tree bonuses — double-dip. Existing FIXME has been outstanding since v0.1.

## 3. Scope

### Empirical verification
- Load save with vanilla skills → snapshot ship stats before vs after `applyOnEveryLoad()` → confirm stat mods actually removed for each aptitude (Combat/Leadership/Industry/Tech) at multiple levels.

### Hardening (if R1 confirmed)
- Add explicit `SkillEffect.unapply()` invocation: `Global.getSettings().getSkillSpec(id).getEffects()` → iterate → call `unapply(stats)`.

### Mod-skill decision
- Decide policy for mod-added vanilla-style skills (UAF/Tahlan/VIC). Suppress-all vs whitelist.

### Tests + uninstall
- Integration test (reflection-based stub stats).
- Uninstall round-trip test (`restoreAll`).
- Confirm uninstall path actually invokable (no current trigger).

### Out of scope
- Officer skill behavior (spec §6 — officers untouched, correct as-is)
- XP ledger interception (spec §7.2.4 explicit no)
- Vanilla data file modification

## 4. Success Criteria

- Empirical test: vanilla skill stat bonuses zero post-suppression for all 4 aptitudes.
- If hardening needed: explicit `unapply()` invoked; second test round confirms zero.
- `restoreAll()` round-trip preserves original level + stats.
- Mod-skill policy documented (CHANGELOG/README).
- Integration test in test/ passing.

## 5. Risks

- **R1** `setSkillLevel(0)` may leave SkillEffect stat mods registered — primary investigation target.
- **R2** Officers safety — verify suppressor only touches `getPlayerPerson()`.
- **R3** Mod-added skills with vanilla-style effects — policy decision needed.
- **R4** Future Starsector updates may add new vanilla skills — current `!startsWith("so_")` filter auto-handles, but stat-bonus persistence question (R1) re-applies.
- **R5** `getSkillsCopy()` iteration safety vs concurrent setSkillLevel — appears safe but confirm.
- **R6** Memory key persistence across uninstall + reinstall — old stash may re-apply old levels. `restoreAll()` not auto-invoked currently.

## 6. Tasks

4 tasks. T0 empirical test gates T1 (only fix if broken).

## 7. Boundaries

- **never_touch:** vanilla `data/characters/skills/*.skill`, `data/characters/skill_data.csv`.
- **require_review:** `SkillsOverhaulModPlugin.java` (load hook).
