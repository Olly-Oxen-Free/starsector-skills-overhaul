# Task brief: 5c Host routing, registration and replacer removal

PRD: skills-panel-restructure | Wave: 6 | Complexity: 4/10 | Model: sonnet | Priority: must
Agent: implementer

## Objective
Wire the two new hosts into the mod plugin and delete the old `CharacterTabReplacer`. `CharTabHost` (5a) mounts the new skill view inside the vanilla CHARACTER tab. `SkillsHotkeyListener` (5b) opens `DialogHost` on a hotkey. Under Second-in-Command (SiC) or when the vanilla-panel probe fails, the tab mount is skipped, the player is told once about the hotkey, and the hotkey path stays. Document the hotkey in README and CHANGELOG.

## Context
Mod: Skills Overhaul (Starsector, Java 8). PRD "Skills panel restructure (increments 1-5)", increment 5, requirements R8, R9, R10.
- Today `CharacterTabReplacer` (a transient EveryFrameScript) mounts the old `CharacterTabPanelPlugin` over the core UI and installs nothing under SiC, so SiC players have no Skills Overhaul UI. SiC removes the same vanilla skill panel, so only a dialog can coexist with it.
- Upstream (already merged when you start; read them before editing):
  - 5a: `src/skills_overhaul/ui/host/CharTabHost.java` (arms a deferred mount from a `CoreUITabListener` callback or a transient every-frame poll of `getCurrentCoreTab() == CHARACTER`; unmounts when the tab leaves), `VanillaCharTabProbe.java` (explicit result: `{core, vanillaPanel, parent}` or a named failure reason; SiC detection and probe failure reach the caller through the `HostEvents` callback, `onSicDetected()` and `onProbeExhausted(reason)`, passed to the `CharTabHost` constructor), `MountRetry.java`, `HostEvents.java`.
  - 5b: `src/skills_overhaul/ui/host/DialogHost.java` and `SkillsHotkeyListener.java` (a `CampaignInputListener`), plus `Config.ui_hotkey` (default `Keyboard.KEY_K`) read from `data/config/skills_overhaul.json`.
  - Neither is registered yet. Use their real signatures; the names above come from the plan and may differ in detail.
- Nothing downstream consumes this task. Increment 6 comes in a later PRD.

## Requirements
1. In `SkillsOverhaulModPlugin.onGameLoad`, construct ONE `CharTabHost` (passing `HostRouter` as its `HostEvents`, see 5a) and register that same instance both as a transient listener (`getListenerManager().addListener(host, true)`) and as the CHARACTER-tab poll script (`Global.getSector().addTransientScript(host)`), the way `privLifecycle` is registered as both script and listener in the same method. Check both guards (`hasListenerOfClass(CharTabHost.class)`, `hasTransientScript(CharTabHost.class)`) before constructing it, so the listener and the script are never two different instances. Register `SkillsHotkeyListener` with `addListener(l, true)` guarded by `hasListenerOfClass`. Remove the `addTransientScript(new skills_overhaul.ui.CharacterTabReplacer())` line in `onGameLoad`.
2. Routing (put the decision in `src/skills_overhaul/ui/host/HostRouter.java`, new; it implements 5a's `HostEvents` and is passed to the `CharTabHost` constructor; keep it free of game state where you can so it can be reasoned about). `HostRouter` is the only place that logs the SiC INFO and probe-failure WARN (5a logs attempts at DEBUG only). It has two cases, both leaving the hotkey `DialogHost` path in place.
   - SiC enabled (`Global.getSettings().getModManager().isModEnabled("second_in_command")`): `CharTabHost` must not mount. Log exactly one INFO line and show one campaign message that names the hotkey.
   - Probe failure: log exactly one WARN with the probe's reason and show one campaign message naming the hotkey.
3. The campaign message is shown at most once per game session. `Global.getSector().getCampaignUI().addMessage(text)`. Use a static "shown this session" flag that is reset on game load (nex4x static-reset pattern: `Nex4xModPlugin.java:132`, `IntelDiplomacyTabInjector.java:36-40`). SiC and probe failure share this flag, so SiC plus a probe failure shows one message, and later tab opens never repeat it. The hotkey name in the text comes from `Config.ui_hotkey` (`Keyboard.getKeyName`), not a hard-coded "K".
4. Delete `src/skills_overhaul/ui/CharacterTabReplacer.java`. Delete `src/skills_overhaul/ui/ReflectionUtils.java` only if `grep -rn ReflectionUtils src test` shows no remaining reference (5a's probe may still use it; if so, keep it).
5. README.md: add a line naming the default hotkey `K`, the `ui_hotkey` key in `data/config/skills_overhaul.json`, and that the hotkey opens the skills dialog under Second-in-Command or when the character tab cannot be replaced. The README currently says the entry point does not fire (sections "Stubbed or unwired" and "Known Issues" item 1); add your line where it fits, and do not rewrite those sections beyond what your line contradicts. CHANGELOG.md: add a matching entry under the first `## Unreleased` section in the file (today `## Unreleased — Defense lane spec gap closure (2026-05-10)`, the first heading after the title); do not add a new section.

## Success criteria
- [ ] SkillsOverhaulModPlugin registers CharTabHost and SkillsHotkeyListener with addListener(l, true), each guarded by hasListenerOfClass, adds the CharTabHost CHARACTER-tab poll with addTransientScript guarded by hasTransientScript, and no longer adds CharacterTabReplacer
- [ ] With second_in_command enabled, CharTabHost does not mount, one INFO line is logged, one campaign message naming the hotkey is shown once per game session, and the hotkey still opens DialogHost
- [ ] A probe failure logs one WARN with the probe's reason and shows one campaign message naming the hotkey once per game session (not repeated on later tab opens, and not shown twice if SiC is also enabled)
- [ ] README.md gains a line naming the default hotkey K, the ui_hotkey key in data/config/skills_overhaul.json, and that it opens the skills dialog under Second-in-Command or when the character tab cannot be replaced; CHANGELOG.md's Unreleased section gains a matching entry
- [ ] CharacterTabReplacer.java is deleted; ReflectionUtils is deleted if no reference remains; ./build.sh and ./build.sh test pass

## Files to modify
| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/SkillsOverhaulModPlugin.java` | modify | register hosts, drop the replacer registration (`onGameLoad`, `:69-74` area) |
| `src/skills_overhaul/ui/host/HostRouter.java` | create | SiC / probe-failure routing, once-per-session message and log lines |
| `src/skills_overhaul/ui/CharacterTabReplacer.java` | delete | superseded by CharTabHost |
| `src/skills_overhaul/ui/ReflectionUtils.java` | delete if unreferenced | only the replacer (and possibly 5a's probe) uses it |
| `README.md` | modify | hotkey line |
| `CHANGELOG.md` | modify | Unreleased entry |

## Deliverables
| Artifact | Path | Done when |
|---|---|---|
| Hotkey documentation | `README.md`, `CHANGELOG.md` | README.md and CHANGELOG.md each name the K default and the ui_hotkey config key |
| Manual in-game verification (user) | `.nimble/prds/001_skills-panel-restructure/PRD_skills-panel-restructure.md` §8 M5 | user confirms the CHARACTER tab shows SkillTreeView filling the vanilla panel area, switching tabs and back restores it, and with SiC enabled the hotkey opens the dialog and vanilla bindings still work; with SiC enabled `starsector.log` has exactly one SiC INFO line and one campaign message names the hotkey, shown once per game session (not again on later tab opens; once more after a reload); a probe failure, if it occurs, logs one WARN with the reason (otherwise verified by code review) |

Note for the reviewer: the M5 row above is the owner's manual check at the final gate, before the feature branch merges to master. It is not a merge criterion for this task PR. Judge this PR on its code criteria, build and tests.

The second row is the user's check after merge, not yours. Mention it in your report as pending.

## Implementation guidance
- Pattern to follow: `SkillsOverhaulModPlugin.onGameLoad`. Transient listeners use `Global.getSector().getListenerManager().addListener(new X(), true)` (see the `AdminXPTracker` registration; `true` = transient, `false` would save a copy per load). nex4x guard pattern: `Nex4xModPlugin.java:359-368` (`hasListenerOfClass` before `addListener(…, true)`). For scripts use `Global.getSector().hasTransientScript(Class)` before `addTransientScript`.
- SiC detection today: `CharacterTabReplacer.java:127-145` (`isModEnabled("second_in_command")` in try/catch, plus a sibling-class scan for `SCSkillMenu`). 5a's probe exposes SiC detection as a result; call that and do not copy the scan. Read the old code before deleting it.
- Both triggers reach one message path. Suggested shape: `HostRouter.onSicDetected()` / `HostRouter.onProbeFailed(String reason)` each log their own line (INFO / WARN, prefix `SkillsOverhaulModPlugin.LOG_PREFIX`) and call a shared `notifyHotkeyOnce()` guarded by the static flag. The log lines should also be once-only, not per frame or per tab open. Reset the flags in `onGameLoad` before registering. This is inferred; adapt to the 5a result types.
- The probe-failure path must not re-run forever: 5a's `MountRetry` caps at 180 frames and reports exhaustion once; the WARN and message hang off `HostEvents.onProbeExhausted(reason)`, which 5a calls once per exhaustion.
- SiC mount precedent, for context only: `second_in_command/ui/SCSkillMenuPanel.kt:97-104`. The SiC poll is the same trigger SiC uses (`SkillPanelReplacerScript.kt:31-34`).
- Unresolved in-game questions (do not try to answer them in code): whether `CoreUITabListener` fires for CHARACTER (the poll covers it), vanilla parent lifetime across tab flips, whether consuming the hotkey in PreCore blocks vanilla bindings. Note them in the report as pending M5.
- Edge cases: SiC and probe failure both true shows one message; reload in the same JVM re-arms the message (flag reset on game load); listeners are not persisted, so every load registers them again, and the guards stop duplicates if `onGameLoad` runs twice.
- `./build.sh` rewrites tracked `jars/SkillsOverhaul.jar`. Never edit a `*.jar`. Do not stage or commit `jars/SkillsOverhaul.jar` (`git checkout -- jars/SkillsOverhaul.jar` or leave it unstaged). `.serena/project.yml` is also modified in the tree and is not yours.

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (verified: 2026-10-08):
- Campaign listeners: `addListener(l, true)` means transient (not saved); `false` saves a new copy every load. `AdminXPTracker` and `SupplyBufferRenderer` were persistent and stacked until fixed.
- Order on load: migration first, then suppression, then reapply, then orphan grants. Keep the new registrations before that block, next to the old replacer line, and do not reorder it.
- `./build.sh` builds the jar; `./build.sh test` runs 77 test classes (more after this PRD).

## Testing requirements
Implementation task: run `validate.sh --changed` (targeted tests). Add tests only where named here: none are named. If `HostRouter` ends up with pure logic (once-only flag, SiC plus probe failure shows one message), add a small `test/skills_overhaul/ui/host/HostRouterTest.java` with a `main()` using `assert` and printing `ALL PASSED`, matching `test/skills_overhaul/config/ConfigTest.java` (Java 8, no framework; each `*Test` class runs its `main()`). Asserts need `-ea`, which `./build.sh test` applies. `./build.sh test` must pass. Real behaviour (log lines, campaign message, mount) can only be seen in-game; the build does not prove it, so say that in the report.
Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit a jar by hand.
- Commit: commit your work on the task branch per the implementer contract (conventional subject, files staged by explicit path). `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`: never stage or commit it; run `git checkout -- jars/SkillsOverhaul.jar` before each commit.
- Flag in the commit body if modified: `mod_info.json`, `build.sh` (neither should be touched).
- Out of scope: changing `CharTabHost`, `DialogHost`, `SkillsHotkeyListener`, `VanillaCharTabProbe`, `Config` beyond what compilation forces; increment 6 (failed-tree label, displacement choice, respec cost); `zealot/CrusadeNPCStub` reflection; tree JSON and `Node`/`TreeDef` APIs; `SpendService`.

## Execution
- Branch: `worktree/skills-panel-restructure-5c`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
- Gaming guard: run `~/.claude/hooks/gaming-check.sh` before the first build or test and before each later build; exit 1 means the owner is gaming: stop, commit WIP, and report `paused: gaming` instead of retrying.
