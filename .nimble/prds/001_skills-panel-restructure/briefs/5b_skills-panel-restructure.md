# Task brief: 5b DialogHost and skills hotkey listener

PRD: skills-panel-restructure | Wave: 5 | Complexity: 5/10 | Model: opus | Priority: must
Agent: implementer

## Objective
Add `DialogHost`, which opens `SkillTreeView` in a custom visual dialog from the campaign map, and `SkillsHotkeyListener`, a `CampaignInputListener` that opens it on a configurable hotkey and consumes the key. Add the `ui_hotkey` config key. This is the fallback path for players under Second-in-Command (SiC), or when the character-tab mount fails, who otherwise get no Skills Overhaul panel.

## Context
Mod: Skills Overhaul (Starsector, Java 8, no compiler beyond `./build.sh`). PRD "Skills panel restructure" rebuilds the character-tab panel as model, layout, view and host layers. Fulfils R9: "`DialogHost` opened by a configurable hotkey", verified by `SkillsHotkeyGuardTest` and `ConfigTest`.
- Builds on 4a (done before this wave): `src/skills_overhaul/ui/view/SkillTreeView.java`, a `CustomUIPanelPlugin` that builds its components once per tab switch and handles Esc (clears pending), keys 1-3 and zoom/pan. Read its constructor and public API before coding; do not change its behaviour except what you must to host it.
- Consumed by 5c (wave 6): it registers `SkillsHotkeyListener` with `addListener(l, true)` guarded by `hasListenerOfClass`, adds README/CHANGELOG lines, and routes SiC/probe failure to the hotkey path. Do NOT register anything in `SkillsOverhaulModPlugin` and do NOT touch README/CHANGELOG here.

## Requirements
1. `DialogHost` (new, `ui/host`): a static entry such as `DialogHost.open()` that, from the map with no dialog open, calls `Global.getSector().getCampaignUI().showInteractionDialog(dummyPlugin, null-or-player-fleet)`. The dummy `InteractionDialogPlugin` has empty methods except `init(dialog)`, which calls `dialog.hideVisualPanel(); dialog.hideTextPanel(); dialog.setPromptText("")` and then `dialog.showCustomVisualDialog(w, h, delegate)` with w,h about 0.9 of `Global.getSettings().getScreenWidth()/getScreenHeight()`. `getContext()` and `getMemoryMap()` return null.
2. The `CustomVisualDialogDelegate`: `init(panel, callbacks)` stores callbacks and builds `SkillTreeView` into `panel`; `getCustomPanelPlugin()` returns the view's plugin; `getNoiseAlpha()` returns 0; `advance` is a no-op; `reportDismissed(int)` treats any call as closed and drops pending state (its argument meaning is undocumented).
3. Closing: Esc with no pending purchases dismisses; Esc with pending is handled by `SkillTreeView` (clears pending), so do not consume it twice. Also a visible close button. Closing runs `dialog.showTextPanel(); dialog.showVisualPanel(); callbacks.dismissDialog();` and then `dialog.dismiss()` so the dummy dialog does not stay (LunaLib pattern). Wrap or extend the view's plugin to intercept Esc-with-no-pending; add a small hook to the view only if unavoidable.
4. `SkillsHotkeyListener` (new, `ui/host`): implements `CampaignInputListener`. `getListenerInputPriority()` returns a high value (Console Commands uses `Integer.MAX_VALUE - 10`; pick similar and comment why). `processCampaignInputPreCore` loops events, skips consumed ones, and for the first event that passes the guard calls `event.consume()` then `DialogHost.open()`. `PreFleetControl` and `PostCore` are empty.
5. Pure guard, a static method in `SkillsHotkeyListener` taking only primitives (for example `shouldOpen(boolean consumed, boolean keyDown, int eventKey, int configuredKey, boolean hasInteractionDialog, boolean showingMenu, boolean coreTabOpen)`), true only for an unconsumed key-down of the configured key with no interaction dialog, no menu and no core tab open. Query game state in the listener (`getCampaignUI().getCurrentInteractionDialog() != null`, `isShowingMenu()`, `getCurrentCoreTab() != null`) and pass booleans in. Read the key from `ConfigLoader.get().uiHotkey`. Never access event data (`getEventValue`, `isKeyDownEvent`) on a consumed event: it throws, so check `isConsumed()` first.
6. `Config`: add `public final int uiHotkey` = `j.optInt("ui_hotkey", Keyboard.KEY_K)` (import `org.lwjgl.input.Keyboard`; `lwjgl.jar` is on the build classpath; `KEY_K` is 37). Add `"ui_hotkey": 37` to `data/config/skills_overhaul.json` (keep it valid JSON, matching the file's alignment style, mind the trailing comma on the previous last line).

## Success criteria
- [ ] DialogHost opens a dummy InteractionDialogPlugin via showInteractionDialog and calls showCustomVisualDialog from its init with SkillTreeView sized to about 0.9 of the screen; Esc with no pending, or a close button, dismisses it
- [ ] SkillsHotkeyGuardTest passes: the pure guard opens only on a key-down of the configured key that is not consumed, with no interaction dialog, no menu and no core tab open
- [ ] Config gains ui_hotkey (default Keyboard.KEY_K) read from data/config/skills_overhaul.json; ConfigTest asserts the default
- [ ] The listener consumes the event it acts on in processCampaignInputPreCore and ignores all others; ./build.sh and ./build.sh test pass

## Files to modify
| File | Action | Purpose |
|---|---|---|
| `src/skills_overhaul/ui/host/DialogHost.java` | create | dummy dialog plus custom visual dialog hosting SkillTreeView (`ui/host/` dir does not exist yet; package `skills_overhaul.ui.host`) |
| `src/skills_overhaul/ui/host/SkillsHotkeyListener.java` | create | CampaignInputListener plus pure static guard |
| `src/skills_overhaul/config/Config.java` | modify | add `uiHotkey` field (`:6-40`, constructor reads via `j.optInt`) |
| `data/config/skills_overhaul.json` | modify | add `ui_hotkey` |
| `test/skills_overhaul/ui/host/SkillsHotkeyGuardTest.java` | create | guard truth table |
| `test/skills_overhaul/config/ConfigTest.java` | modify | assert default `uiHotkey == Keyboard.KEY_K` when key absent (existing test builds a JSON without it) and optionally an override value |

Verify with Glob before editing; `SkillTreeView.java` is expected from 4a.

## Deliverables
| Artifact | Path | Done when |
|---|---|---|
| Config key | `data/config/skills_overhaul.json` | file stays valid JSON with ui_hotkey set to the KEY_K code and Config reads it with the same default when absent |

## Implementation guidance
- Pattern for config field: `src/skills_overhaul/config/Config.java` constructor lines, one `j.optInt(...)` per field. Pattern for tests: `test/skills_overhaul/config/ConfigTest.java`, a class with `public static void main`, `assert cond : "msg";`, ending with `System.out.println("ALL PASSED");`. Second style example: `test/skills_overhaul/level/GameSpendSnapshotTest.java`.
- Precedent for the custom dialog: `showCustomVisualDialog(1000f, 700f, delegate)` in `game/mods/Interastral Peace Casino-1.6.3/src/data/scripts/casino/interaction/GachaHandler.java:414,500`. Precedent for CampaignInputListener: `game/mods/Console Commands-4.0.9/src/org/lazywizard/console/ConsolePlugins.kt:20-42` (priority `Int.MAX_VALUE-10`, guards on `isShowingMenu`).
- API facts (research Q2/Q3, with sources):
  - `void InteractionDialogAPI.showCustomVisualDialog(float w, float h, CustomVisualDialogDelegate d)`; the w,h become the `CustomPanelAPI` size passed to `init`.
  - `CustomVisualDialogDelegate`: `init(CustomPanelAPI, DialogCallbacks)`, `getCustomPanelPlugin()`, `getNoiseAlpha()`, `advance(float)`, `reportDismissed(int)`; `DialogCallbacks` has `dismissDialog()` and `getPanelFader()`.
  - Map open: `boolean CampaignUIAPI.showInteractionDialog(InteractionDialogPlugin, SectorEntityToken target)`; target may be null per javadoc. LunaLib pattern: `M/LunaLib-2.0.5/src/lunalib/backend/ui/OpenCustomPanelFromDialog.kt:15-60`; close pattern `LunaBaseCustomPanelPlugin.kt:190-203`.
  - Nothing closes the dialog for you: the plugin must handle Esc (key-down on `Keyboard.KEY_ESCAPE`, `consume()`, then close). Vanilla example `DuelPanel.java:535-537`. LunaLib uses a cooldown so the open key does not instantly re-close; here the hotkey is consumed in PreCore, but do not bind close to the hotkey.
  - `CampaignInputListener` methods: `getListenerInputPriority()` (higher first), `processCampaignInputPreCore`, `processCampaignInputPreFleetControl`, `processCampaignInputPostCore` (all `List<InputEventAPI>`). Match with `isKeyDownEvent()` plus `getEventValue()`; skip `isConsumed()`; `consume()` what you act on.
- Whether consuming in PreCore blocks a vanilla binding on the same key is untested. Do not work around it; list it in the PR description as an in-game gap.
- Whether the dummy dialog flashes visibly on open is untested; note it for in-game verification (M5).
- Keep the guard free of `Global` and lwjgl runtime calls so the test runs headless. `Keyboard.KEY_*` constants are compile-time and safe.
- Edge cases: key opens nothing if a dialog, menu or core tab is open (prevents firing while typing in text fields); a second press while `DialogHost` is open cannot occur because an interaction dialog is then showing, so the guard already blocks it; consumed events are ignored and their data is never read; absent `ui_hotkey` falls back to K.
- Java 8 source level (`javac -source 8`): no `var`, no `List.of`.
- Verification is by tests and build only; in-game checks belong to the user (M5).

## Prior knowledge
From `brain/wiki/skills-overhaul-core-systems.md` (no `verified:` date in the entry):
- Campaign listeners: `addListener(l, true)` means transient (not saved); `false` saves a new copy every load. `AdminXPTracker` and `SupplyBufferRenderer` were persistent and stacked until fixed.
- `./build.sh` builds the jar; `./build.sh test` runs all `*Test` classes.

## Testing requirements
Run `validate.sh --changed` (targeted tests), which runs `./build.sh test`. Each `*Test` class runs its `main()` with `assert` (Java 8, `-ea`), discovered by class name ending in `Test`.
Add:
- `SkillsHotkeyGuardTest` (new, `test/skills_overhaul/ui/host/`): the guard returns true for unconsumed key-down of the configured key with no dialog/menu/core tab; false for each of: consumed, key-up, other key, interaction dialog open, menu showing, core tab open.
- `ConfigTest`: assert `cfg.uiHotkey == Keyboard.KEY_K` when `ui_hotkey` is absent; optionally an override.
Do not edit or weaken existing tests to make them pass. End your report with the `Tests:` evidence line.

## Boundaries
- Do not modify: `build/`, `target/`, `*.jar`. Never edit any jar. `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`: do not commit that file (`git restore jars/SkillsOverhaul.jar` or leave it unstaged). Do not commit anything unless your caller says so.
- Flag in the commit body if modified: `mod_info.json`, `build.sh` (this task should touch neither).
- Out of scope: registering the listener or any host in `SkillsOverhaulModPlugin` (5c); README/CHANGELOG (5c); `CharTabHost` (5a); removing `CharacterTabReplacer` (5c); SiC detection and campaign messages (5c); increment 6 (failed-tree label, active-T0 displacement, real respec cost); changing `Node`/`TreeDef` APIs or tree JSON.

## Execution
- Branch: `worktree/skills-panel-restructure-5b`
- Validate: `"/home/jayden-eppcohen/Documents/Projects/Software/nimble/.claude/plugins/nimble/scripts/validate.sh" --changed`
- Commands: build `./build.sh`, test `./build.sh test`
