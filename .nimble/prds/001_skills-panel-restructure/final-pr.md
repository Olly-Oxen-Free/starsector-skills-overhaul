## Skills panel restructure (increments 1-5)
This rebuilds the Character-tab skill panel. The single 1300-line plugin is split into model, layout, view and host layers, panel coordinates are now bottom-up, and the vanilla-tab reflection is confined to one probe class. A reflection-free dialog opened by hotkey (default `K`) is added and is used under Second-in-Command or when the probe fails. Before this change, players running Second-in-Command had no skill UI at all.

### Tasks
- [1a] Bottom-up panel coordinates and dead UI code purge (#1)
- [2a] SkillTreeModel, a PrereqValidator that returns block reasons, and an all-or-nothing commit (#2)
- [3a] TreeLayout with zoom and pan (#4)
- [3b] Node tooltip content and TooltipCreator adapter (#3)
- [4a] SkillTreeView built once per tab, with addTooltipTo tooltips (#5)
- [5a] VanillaCharTabProbe and CharTabHost (#7)
- [5b] DialogHost and skills hotkey listener (#6)
- [5c] Host routing, registration and replacer removal (#8)

Each task PR had one review round. Models: 6 tasks on opus, 2 on sonnet.

### Changes
- 40 files changed, +3955 / -2615 lines.
- Code: 37 files under `src/` and `test/`.
- Docs and config: `README.md`, `CHANGELOG.md`, and `data/config/skills_overhaul.json` (adds the `ui_hotkey` key).
- Removed: `CharacterTabPanelPlugin`, `PathTreeBuilder`, `NodeIconPanelPlugin`, `CharacterTabReplacer`.
- Added: `ui/model`, `ui/layout`, `ui/view`, `ui/host`, `ui/PanelCoords`.
- 10 headless test classes are new or extended.

### Validation
- build: `./build.sh` passed
- test: `./build.sh test` passed (every `*Test` class)

### Must pass in-game before merge (owner)
- **M1 (coordinates):** the window frame, XP bar, scrollbar and portrait border line up with the content. Hover and click hit the node under the cursor.
- **M4 (view):**
  - Tooltips anchor on the hovered node and list effects and prereqs with ✓/✗.
  - Zoom and pan keep clicks on the right node.
  - Confirming a batch that is over budget flashes red and buys nothing; a valid confirm buys the whole batch and plays one sound.
  - Frame time shows no regression.
- **M5 (hosts):**
  - The CHARACTER tab shows the new panel in the vanilla area.
  - Switching tabs and coming back restores it.
  - With Second-in-Command enabled:
    - `K` opens the dialog, and vanilla bindings still work.
    - `starsector.log` has one SiC INFO line.
    - A message naming the hotkey appears once per session.
  - A probe failure logs one WARN with the reason.

### Open advisory findings
These are in `findings.md`. None of them block.
- A screen-size change needs the tab reopened before the panel remounts.
- `TreeLayout` spacing ratios are duplicated, and the `topPad` field is unused.
- `NodeTooltip` uses literal colours instead of `UiColors`.
- `CharTabHost.rollback` does not discard the view.
- In-game questions still open: the dialog may flash on open, `K` may be consumed before vanilla bindings see it, and the order in which `reportDismissed` fires is unconfirmed.
- `./build.sh` rewrites the tracked `jars/SkillsOverhaul.jar`; it is not committed here.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
