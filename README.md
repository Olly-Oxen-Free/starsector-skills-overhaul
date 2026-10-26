# Skills Overhaul

Replaces vanilla's skill system with three independent, activity-gated trees:

| Tree | XP source | Lanes |
|------|-----------|-------|
| **Pilot** | Personal ship damage dealt | Gunner / Ace / Defender |
| **Commander** | Fleet battles won (fleet XP mirror) | Tactics / Logistics / Doctrine |
| **Admin** | Colony production / trade income | Industry / Trade / Stability |

Each tree caps at level 25. Skill points bought with XP from its own stream — no cross-pollination by default. CK3-lifestyle topology: root nodes gate into 3 lanes ending in capstones, with optional lane-merge bonuses.

See `docs/superpowers/specs/2026-04-18-skills-overhaul-design.md` for full design.

---

## v0.1.0 Scope

This is a **v0.1 alpha**. Infrastructure is in; content is first-pass drafts flagged `_review` per node.

**Wired & working:**
- Three XP streams with memory-backed per-tree XP/level/points
- Tree JSON loader + registry + ModPlugin bootstrap
- Purchase / refund math + respec (first free, 1 SP thereafter)
- Vanilla skill suppressor (zeros `non-so_` skills on load, stashes for uninstall)
- SkillsOverhaulAPI (external query surface for other mods)
- Migration from vanilla skills on first load of existing save
- Combat plugin that applies `piloted_maneuverability` to MaxTurnRate / TurnAcceleration / Acceleration

**Stubbed or unwired (see FIXME markers):**
- **Entry point**: the skill panel is mounted over the vanilla CHARACTER tab (`CharTabHost`). When the tab cannot be replaced (Second-in-Command, or the vanilla panel cannot be probed), the `K` hotkey dialog below is the way in. The old `CharacterInfoMenu` rules trigger does not fire in vanilla and is not relied on.
- **Skills hotkey**: press `K` (default; set `ui_hotkey` in `data/config/skills_overhaul.json`) to open the skills dialog. It is the way in under Second-in-Command or when the character tab cannot be replaced; the player is told once per session.
- **Ship-stat handlers**: only `piloted_maneuverability` is wired to real combat-time stat modifiers. All other pilot-tree effect types (damage, range, hull, armor, flux, shield) log STUB and no-op. Add branches to `SkillsOverhaulCombatPlugin.applyShipStatEffect()` post-playtest.
- **Campaign-wide handlers**: commander-tree and admin-tree effects (fleet CR, officer slots, colony production, accessibility, etc.) have no handler — they route to `NoOpHandler` which warns at purchase and does nothing. Content is drafted with effect types; handlers land post-playtest.
- **Bar event**: `SeasonedCrewBarEvent` is a skeleton; `createBarEvent()` returns null so the event is skipped. Real dialog + XP award is post-playtest.
- **Capstone extras**: capstones render with a ★ marker but no additional gating beyond tier chain.
- **Merge nodes**: `merges[]` is scaffolded per tree but the loader doesn't consume the array yet.

---

## Known Issues

1. **Skill panel entry point does not fire.** See above. Not a regression — just not wired to any player-reachable hook.
2. **Re-entrant dialog risk.** `SkillPanel.show()` re-opens itself after each button press to redraw; advisor flagged potential recursion / leak. Monitor in playtest; may need rewrite to mutate the existing panel in place.
3. **Vanilla skill suppression** calls `setSkillLevel(id, 0f)` for every vanilla skill (ids from vanilla `skill_data.csv`). Vanilla may cache stat bonuses elsewhere; teardown path has a `FIXME(playtest)` in `VanillaSuppressor.java`.
4. **Migration pool lock** prevents mixing migration points across trees by locking the first-spent tree. Intended, documented in `SpendService`, but un-mocked in tests against real save data.

## Deliberate deviations from spec

- **Craft Fuel** converts 10 supplies → 30 fuel, not the spec's 20 → 1 (a strict loss); changed in the 2026-07-07 audit.
- **Drilled and Ready** drops the spec's "crew/marine XP +30%": the API has no crew or marine XP stat.

---

## Installation

1. Drop the `Skills Overhaul` folder in `<Starsector>/mods`.
2. Enable in launcher. No required dependencies; optional: LunaLib, Nexerelin.
3. **Existing save compatibility**: yes, via `MigrationPlugin` running once on first load. Vanilla skill points convert to the universal migration pool.

## Uninstall

**Do not just disable the mod** — vanilla skills are suppressed to level 0 while
the mod is active, and a save loaded without the mod keeps them at 0 permanently.

With [Console Commands](https://fractalsoftworks.com/forum/index.php?topic=4106.0) installed:

1. Load your save and run:
   `runcode skills_overhaul.console.UninstallHelper.prepareUninstall();`
   This restores your stashed vanilla skill levels and disables suppression.
2. Save the game.
3. Exit and disable Skills Overhaul in the launcher.

Changed your mind? `runcode skills_overhaul.console.UninstallHelper.reenableSuppression();`

---

## Building

```
./build.sh       # compile + package jars/SkillsOverhaul.jar
./build.sh test  # compile src + test, run all *Test main methods
```

All 13 unit tests should pass on a clean checkout.

---

## For Contributors

Effect types in tree JSON can be anything — unknown types route to `NoOpHandler` with a warning. To add a real handler:

- **Combat-time piloted ship stats** → add a branch to `SkillsOverhaulCombatPlugin.applyShipStatEffect()`. Use `modifyMult(nodeId, ...)` so respec cleanly unregisters.
- **Campaign-wide stats** (fleet, colony, officer) → subclass `EffectHandler`, register in `EffectRegistry`. `apply(nodeId, params)` fetches player stats or markets directly.
- **Behavioral changes** (conditional damage reduction, zero-flux-while-firing, etc.) → combat plugin plus per-frame polling; see `piloted_damage_reduction_low_hull` as example of non-trivial spec.

Search for `_review` in the tree JSONs to find all nodes flagged as draft balance.
