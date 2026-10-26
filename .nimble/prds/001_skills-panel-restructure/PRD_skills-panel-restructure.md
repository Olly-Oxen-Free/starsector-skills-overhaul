---
feature_name: "Skills panel restructure (increments 1-5)"
feature_slug: "skills-panel-restructure"
owner: "jayden-eppcohen"
status: "draft"
created_date: "2026-10-26"
target_date: ""
phase: "Skills Overhaul UI"
scope_type: "refactor"
github_project: ""
links:
  - "research/summary.md"
  - "../../../../docs/superpowers/specs/skills-overhaul/2026-04-19-skills-overhaul-ui-design.md"
checkpoint_refs: []
cross_feature_blockers: []
---

# PRD: Skills panel restructure (increments 1-5)

## 1. Executive Summary

**One-liner:** Rebuild the Skills Overhaul character-tab panel as separate model, layout, view and
host layers, fixing the y-axis bug first and adding a dialog fallback that works under
Second-in-Command.

**What's changing:** Today one 1300-line plugin (`CharacterTabPanelPlugin`) plus `PathTreeBuilder`
owns state, layout, drawing and input, mixes top-down and bottom-up y math, rebuilds per frame, and
commits purchases node by node. `CharacterTabReplacer` mounts it over the core UI by parking
siblings, and installs nothing under SiC. After this PRD: a pure coordinate helper, a headless
`SkillTreeModel` with all-or-nothing commit, a `TreeLayout` with zoom and pan, a `SkillTreeView`
built once per tab with native tooltips, and two hosts: `CharTabHost` (replaces the vanilla skill
panel in its parent) and `DialogHost` (hotkey-opened dialog for SiC or probe failure).

**Who it's for:** The player opening the CHARACTER tab, including players running SiC, who today get
no Skills Overhaul panel at all.

**Why now:** The y-axis bug misplaces the frame by 14 px and breaks the backup hit test; the partial
commit can spend points on half a batch; SiC users have no UI.

**Done looks like:** Open CHARACTER: the skill tree fills the vanilla panel area, frame and content
align, hover shows native tooltips, wheel zooms, drag pans, an over-budget confirm buys nothing and
flashes red. With SiC enabled, press `K` on the map: the same view opens in a dialog.

**Primary risk:** Host behaviour that only the game can show: whether `CoreUITabListener` fires for
CHARACTER, how long the vanilla parent lives across tab flips, and whether consuming the hotkey in
PreCore blocks vanilla bindings.

## 2. Problem & Context

**Problem statement:** The panel's coordinate math is wrong wherever it is not masked by the panel
being centred; state and drawing are fused, so behaviour cannot be unit tested; purchase is not
atomic; the host conflicts with SiC.

**Supporting data / evidence:** `research/internal/findings.md` Q1-Q8 and
`research/external/findings.md` Q1-Q7. Key points: `getY()` is the bottom edge (Q2, external Q1);
`SpendService.spend` does not mutate on failure, so a dry run is cheap (Q3); three mods prove the
host pattern (Q6, external Q4).

**What happens if we don't build this:** Misaligned UI, partial purchases, no UI under SiC, and
increment 6 (failed-tree label, displacement choice, respec cost) has nothing to build on.

**Strategic fit:** Increments 1-5 of the panel restructure. Increment 6 follows in a later PRD.

## 3. Goals, Non-Goals & Success Metrics

### Goals

1. One bottom-up coordinate convention everywhere in the panel.
2. A headless model and layout whose behaviour `./build.sh test` checks.
3. A view built once per tab switch, with native tooltips, zoom and pan.
4. A host that coexists with SiC and degrades to a dialog instead of to nothing.

### Non-Goals

- Increment 6: failed-tree label, choosing which active T0 to displace, the real respec cost.
- Spec §4.1 colour palette, fixed 48x48 nodes, lanes, hybrids, merge nodes (`TreeDef.getMergeNodes`, `MergeNodeTest`).
- Changing `Node`/`TreeDef` APIs or tree JSON data.
- `zealot/CrusadeNPCStub` reflection.
- Changing `SpendService` pool order or point economics.
- Editing `mod_info.json`, `build.sh`, `build/`, `target/`, `*.jar`.

### Success Metrics

| Metric | Baseline | Target | How Measured |
| ------ | -------- | ------ | ------------ |
| Frame/content offset | 14 px (arithmetic) | 0 px | In-game check M1 |
| Partial purchase on failed confirm | Possible | Impossible | `PurchasePlanTest`; in-game M4 |
| Per-frame rebuild work | Edge rebuild, texture load, memory scan each frame | None | Grep of render/advance in 4a review |
| UI under SiC | None | Dialog via hotkey | In-game M5 |
| Headless UI tests | 0 | 7 new classes | `./build.sh test` |

## 4. Requirements

### Must Have (blocks launch)

| ID | Requirement | Acceptance Criteria |
| -- | ----------- | ------------------- |
| R1 | Bottom-up panel-local coordinates via one pure helper | `PanelCoordsTest` covers a non-centred panel; all panel y-sites use it (1a) |
| R2 | Dead code and debug text removed | Listed symbols and `NodeIconPanelPlugin` gone; build and tests green (1a) |
| R3 | `SkillTreeModel` with OWNED/PENDING/UNLOCKABLE/BLOCKED(reason) and tier gate in `PrereqValidator` | `SkillTreeModelTest`, `PrereqValidatorTest` pass (2a) |
| R4 | All-or-nothing confirm with red flash on rejection | `PurchasePlanTest` asserts no mutation on failure; view flashes red (2a, 4a) |
| R5 | `TreeLayout` with fit, zoom about cursor and clamped pan | `TreeLayoutTest` (3a); hit tests correct when zoomed (4a) |
| R6 | Native tooltips via `addTooltipTo` with name, cost, effects, prereq met/unmet | `NodeTooltipContentTest` (3b); tooltips in-game (4a) |
| R7 | View built once per tab switch, cached sprites | No per-frame rebuild/texture load (4a) |
| R8 | `CharTabHost`: deferred mount in the vanilla panel's parent, armed by `CoreUITabListener` or a transient poll of `getCurrentCoreTab() == CHARACTER`, explicit probe result | `MountRetryTest`; in-game M5 (5a, 5c) |
| R9 | `DialogHost` opened by a configurable hotkey, documented in README and CHANGELOG | `SkillsHotkeyGuardTest`, `ConfigTest` (5b); README/CHANGELOG lines (5c); in-game M5 |
| R10 | Routing: SiC or probe failure uses DialogHost; `CharacterTabReplacer` removed | 5c criteria |

### Should Have (important, not blocking)

| ID | Requirement | Acceptance Criteria |
| -- | ----------- | ------------------- |
| - | None beyond the must-haves | - |

### Could Have (nice to have, cut first)

| ID | Requirement | Acceptance Criteria |
| -- | ----------- | ------------------- |
| - | None | - |

## 5. UX & Interaction Notes

**Visual Assets:** none supplied.

**User Experience:**
- Click an unlockable node: it becomes pending. Click a pending node: it and its pending dependents
  leave pending. Click an unaffordable or tier-blocked node: it flashes red (~0.4 s), nothing changes.
- Confirm: buys the whole pending batch or nothing; failure flashes the node that broke the dry run.
  One sound per successful confirm.
- Esc clears pending (when pending exists); with nothing pending in DialogHost, Esc closes the dialog.
- Keys 1-3 switch Pilot/Commander/Admin; pending clears on switch.
- Wheel zooms about the cursor between fit and 2.0x; left-drag over 6 px pans.
- Owned T0 click keeps today's active-slot toggle (see §9).

**Visual Design:** current palette, centralised in `UiColors`. Existing header (portrait, XP bar,
points, respec/confirm buttons) stays. "Free Uni"/"Mig Pool" become "Free universal points" /
"Migrated points".

**Key screens & states:**
- Error state: under SiC or on probe failure, one campaign message per game session names the hotkey;
  SiC also logs one INFO line, probe failure one WARN line.
- Success state: confirmed nodes turn OWNED; pending clears.

**Accessibility / Responsive:** layout fits the parent panel (tab host) or ~0.9 of the screen
(dialog) at any resolution through fit-zoom.

## 6. Dependencies & Risks

### Cross-Feature Blockers

_None._

### External Blockers

| Blocker | Status | Fallback |
| ------- | ------ | -------- |
| `CoreUITabListener` firing for CHARACTER | Unknown (in-game) | Transient `getCurrentCoreTab() == CHARACTER` poll also arms the mount; probe failure routes to DialogHost via hotkey |
| SiC 2.0.0 removing the same vanilla panel | Known | DialogHost under SiC |

### Internal Dependencies

- `SpendService`, `PrereqValidator`, `PendingPurchaseTopoSort`, `ActiveLaneService`, `TreeRegistry`, `GameSpend`.

### Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
| ---- | ---------- | ------ | ---------- |
| Listener does not fire for CHARACTER or fires too early | Med | High | Every-frame CHARACTER-tab poll as a second trigger (SiC and current replacer pattern), deferred mount, 180-frame cap, DialogHost fallback |
| Vanilla parent discarded or reused on tab flip | Med | Med | Poll current core tab; remount on next open |
| Hotkey consumption blocks a vanilla binding | Low | Med | Configurable `ui_hotkey`; guard on map-only state |
| `NodeEffectApplier.apply` throws mid-batch | Low | Med | Catch per node, log ERROR, continue |
| UI children drift from GL transform at zoom | Med | Med | Reposition children from `TreeLayout.contentToScreen` on change |
| Shared `SpriteAPI` alpha state | Low | Low | Set alpha every render |

## 7. Rollout Plan

**Phase/level:** Six waves, one per increment; increment 5 spans waves 5-6.

**Deployment strategy:** direct merge per task PR; deploy to the game for manual checks after waves 1, 4 and 6.

**Rollback plan:** revert the wave's merge commit. No save data changes: memory keys stay the same.

**Monitoring:** `starsector.log` for the probe WARN, SiC INFO, and NodeEffectApplier ERROR lines.

## 8. Milestones & Release Criteria

| Milestone | What's True When Done | Target Date |
| --------- | --------------------- | ----------- |
| M1 (wave 1) | Coordinates fixed, dead code gone. **Manual (user):** frame, XP bar, scrollbar and portrait border align with content; node clicks and tooltips work | - |
| M2 (wave 2) | Model, reason API, dry-run commit; headless tests green | - |
| M3 (wave 3) | TreeLayout and tooltip content; headless tests green | - |
| M4 (wave 4) | SkillTreeView live. **Manual (user):** tooltips on hover; zoom/pan keep clicks on target; over-budget confirm buys nothing and flashes red; no frame-time drop with the panel open; keys 1-3, Esc, owned-T0 toggle behave as before | - |
| M5 (waves 5-6) | Hosts live. **Manual (user):** CHARACTER tab shows the view filling the vanilla panel area; leaving and reopening the tab restores it; with SiC enabled the `K` hotkey opens the dialog, Esc/close dismisses it, and vanilla bindings still work; `starsector.log` shows no reflection errors | - |

**Release criteria:**
- `./build.sh` and `./build.sh test` pass after every wave.
- M1, M4 and M5 manual checks confirmed by the user in-game.

## 9. Open Questions

| # | Question | Status | Resolution |
| - | -------- | ------ | ---------- |
| Q1 | Does `CoreUITabListener` fire for CHARACTER? | Open | In-game M5; no longer gates the mount, the tab poll covers it |
| Q2 | Vanilla parent lifetime on tab flip | Open | In-game M5 |
| Q3 | Does consuming a key in PreCore block vanilla bindings? | Open | In-game M5 |
| Q4 | Meaning of `reportDismissed(int)` | Open | Treated as closed in any case |
| Q5 | `getSprite` shared-instance alpha state | Open | Alpha set every render |

- Decision: View mounts in the vanilla panel's parent, replacing it; SiC or probe failure uses DialogHost via hotkey. Reason: SiC removes the same panel, so only a dialog coexists.
- Decision: Confirm is all-or-nothing through a dry run on one cloned whole-tab snapshot. Reason: spec §4.5; `spend` is non-mutating on failure.
- Decision: Bottom-up panel-local coordinates everywhere. Reason: engine convention confirmed by vanilla, LunaLib, SiC and nex4x.
- Assumption: Owned-T0 click keeps the active-slot toggle and its auto-displacement; increment 6 replaces it (spec §4.3 no-op deferred).
- Assumption: Current palette stays, in `UiColors`; spec §4.1 colours not adopted.
- Assumption: `GameSpend` stops playing sounds; the view plays one per successful confirm, so `level/` no longer imports `ui/`.
- Assumption: Hotkey default `K`, configurable as `ui_hotkey` in `data/config/skills_overhaul.json`; acts only on the map with no dialog, menu or core tab open.
- Assumption: The hotkey listener is registered in every game, not only under SiC or after probe failure.
- Assumption: A throw from `NodeEffectApplier.apply` mid-batch is caught per node and logged; points already written stay spent.
- Assumption: Tier gate lives in a new reason-returning `PrereqValidator` method; boolean `canPurchase*` keep prereq-only semantics.
- Assumption: "Free Uni"/"Mig Pool" get player-facing labels; "Keys 1-3 switch tabs" hint stays.
- Assumption: `Node.getLane/isMerge/getParentLanes` and `NodeSlotTest` stay.
- Assumption: `CrusadeNPCStub` reflection is out of scope.
- Assumption: Zoom range fit..2.0, wheel zooms about cursor, left-drag > 6 px pans.
- Assumption: Increment 4 stays one task at complexity 7 (user: one wave per increment); the tooltip builder moved to wave 3 to keep it there.
- Assumption: Increment 5 spans waves 5 and 6 because routing needs both hosts.
- Assumption: Old `GameSpend.purchase` and old UI wiring stay in waves 2-3; increment 4 switches to the model.

## 10. Checkpoint Learnings

**Patterns to reinforce:** transient listeners via `addListener(l, true)` (persistent listeners stacked on
load before: brain `skills-overhaul-core-systems`).

**Anti-patterns to avoid:** no prior corrections recorded for this mod.

**Estimate calibration notes:** none yet.

## 11. Agent Boundaries (Phase-Specific)

**Files the agent should reference for patterns:**
- `game/starfarer.api.zip` → `com/fs/starfarer/api/impl/campaign/ExampleCustomUIPanel.java` (bottom-up quads, hit tests)
- `workspace/nexerelin-4x-mod/src/nex4x/ui/CoreUITabInjectorListener.java`, `IntelDiplomacyTabInjector.java` (deferred mount, retry cap)
- `game/mods/Second-in-Command*/…/ui/SCSkillMenuPanel.kt:97-104` (remove/add in parent)
- `game/mods/Interastral Peace Casino-1.6.3/src/data/scripts/casino/interaction/GachaHandler.java:414,500` (custom visual dialog)
- `game/mods/Console Commands-4.0.9/src/org/lazywizard/console/ConsolePlugins.kt` (CampaignInputListener)

**Files the agent should NOT touch (beyond `never_touch`):**
- `mod_info.json`, `build.sh` (require review)
- `data/config/skills_overhaul/trees/**`
- `src/skills_overhaul/tree/Node.java`, `TreeDef.java`, `src/skills_overhaul/level/SpendService.java`
- `src/skills_overhaul/zealot/CrusadeNPCStub.java`

**Architecture decisions already made (don't re-decide):** the three decisions in §9.

**Known gotchas:**
- `PositionAPI.getY()` is the bottom edge; `inTL` pads are top-relative but positions are not.
- `CoreUITabListener` fires before the tab exists; never mount inside the callback.
- UI children do not follow `glTranslatef/glScalef`; reposition them.
- `ReflectionUtils.getChildrenCopy` swallows throwables into an empty list.
- `GameSpendSnapshotTest` uses package-private `GameSpend.purchasedKeys(TreeDef)`.
- New `*Test` classes with `main()` self-register in `./build.sh test`; there is no JUnit.

## Agent Tasks

Tasks are in `./tasks.yaml`.

| ID | Title | Wave | Complexity | Model | Priority | Dependencies |
|----|-------|------|------------|-------|----------|--------------|
| 1a | Bottom-up panel coordinates and dead UI code purge | 1 | 6 | opus | must | - |
| 2a | SkillTreeModel, reason-returning PrereqValidator, all-or-nothing commit | 2 | 6 | opus | must | 1a |
| 3a | TreeLayout with zoom and pan | 3 | 5 | opus | must | 2a |
| 3b | Node tooltip content and TooltipCreator adapter | 3 | 3 | sonnet | must | 2a |
| 4a | SkillTreeView built once per tab, with addTooltipTo tooltips | 4 | 7 | opus | must | 3a, 3b |
| 5a | VanillaCharTabProbe and CharTabHost | 5 | 6 | opus | must | 4a |
| 5b | DialogHost and skills hotkey listener | 5 | 5 | opus | must | 4a |
| 5c | Host routing, registration and replacer removal | 6 | 4 | sonnet | must | 5a, 5b |
