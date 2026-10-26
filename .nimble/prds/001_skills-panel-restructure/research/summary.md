# Research summary — skills-panel-restructure

## Key takeaways
1. **The y-axis bug is real.** `getY()` is the bottom edge and y grows up; frame, XP bar, scrollbar, portrait border and scissor use top-down math, and the backup hit-test is wrong whenever it fires. Clicks work only because the primary path uses `containsEvent`, and tooltip/wheel math works only because the panel is centred. (internal Q2; external Q1)
2. **A model can be built from what already exists.** `SpendService.spend` does not mutate on failure, so a dry run is `spend` on a cloned snapshot; batch = sequential spend in topo order. The tier gate is UI-only and must move into `PrereqValidator`, which needs a reason-returning API. `GameSpend.snapshot` must widen from one TreeDef to the whole tree. (internal Q3, Q4)
3. **Host pattern is proven by three mods.** `CoreUITabListener` fires before the tab exists (defer, cap ~180 frames, nex4x); there is no close hook, so poll `getCurrentCoreTab()` while mounted. SiC mounts by `parent.removeComponent(vanilla)` + `parent.addComponent(custom)` with no sibling parking. (internal Q6; external Q4)
4. **SiC and this mod both remove the same vanilla panel**, so under SiC the only workable host is a dialog: `showInteractionDialog(dummy, playerFleet)` → `dialog.showCustomVisualDialog(w,h,delegate)`, opened by a transient `CampaignInputListener` hotkey that consumes its key in `processCampaignInputPreCore`. (external Q2, Q3; internal Q6)
5. **Mounting in the parent means the layout must fit the parent's size**, so TreeLayout with zoom/scroll offsets (increment 3) is a precondition for the host change (increment 5). Zoom has no in-workspace precedent: manual `glTranslatef/glScalef` with inverted hit tests. (internal Q6, Q8; external Q7)

## Recommended approach
Land in order 1 → 2 → 3 → 4 → 5, each as one wave:
- **1:** a pure coordinate helper (bottom-up panel-local) + dead code/debug-text purge. Merge/sublane code is unreachable (`Node.getLane()` is always `"default"`).
- **2:** `SkillTreeModel` (Map snapshot + `List<TreeDef>` → OWNED/PENDING/UNLOCKABLE/BLOCKED(reason)), a reason-returning `PrereqValidator` with the tier gate, an all-or-nothing confirm via dry run, and a red-flash state. Headless tests.
- **3:** `TreeLayout` instance (geometry, zoom, scroll clamps). Headless tests.
- **4:** `SkillTreeView` built once per tree switch; cached `SpriteAPI`; `addTooltipTo` tooltips per spec §4.1.
- **5:** `VanillaCharTabProbe` (explicit result type, wraps the seven reflective calls + `CampaignState`/`AppDriver`), `CharTabHost` (listener + deferred mount in parent), `DialogHost` + hotkey; use DialogHost under SiC or when the probe fails.

## Open gaps
- In-game only: whether the listener fires for CHARACTER; vanilla parent lifetime on tab flip; whether consuming a key in PreCore blocks vanilla bindings; `reportDismissed(int)` meaning; `getSprite` shared-instance alpha state.
- Atomicity of `NodeEffectApplier.apply` mid-batch is unknown (not covered by the spend dry run).
- Decisions for the plan: hotkey default; whether clicking an owned T0 stays an active-slot toggle (spec §4.3 says no-op; increment 6 covers displacement choice); whether `GameSpend` keeps calling `ui.SkillUISounds`; whether to keep spec §4.1 colours or the current palette; `CrusadeNPCStub` reflection stays out of scope.
