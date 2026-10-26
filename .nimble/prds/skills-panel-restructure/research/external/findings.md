# External findings: skills-panel-restructure
Date: 2026-10-26. Mode: external (source-only; web search returned nothing on Starsector).

Paths are relative to `/home/jayden-eppcohen/Documents/Projects/GameDev/Modding/Starsector-Mods/`.
`API/` = `game/starfarer.api.zip` (API sources incl. javadoc and `impl/` examples, extracted read-only), same content as `game/starfarer.api.jar` (dated 2025-04-16, 0.98a-RC5 era; LunaLib 2.0.5 declares gameVersion 0.98a-RC5 in `game/mods/LunaLib-2.0.5/mod_info.json`).
`M/` = `game/mods/`. LunaLib 2.0.5, Second-in-Command 2.0.0 (SiC), Ship Mastery System 2.0.8 (SMS).
Web: one WebSearch returned only unrelated results. No forum/wiki claim below. All findings come from the API zip and installed mod sources.

## 1. Y-axis convention and GL quad drawing

### Takeaway
Origin is bottom-left, y grows up. `PositionAPI.getY()` is the bottom edge, `getY()+getHeight()` the top edge. `InputEventAPI.getX/getY` are in the same space, so `event.getY()` can be compared directly with `position.getY()`.

### Cited Findings
- Javadoc states no axis convention: `PositionAPI` and `InputEventAPI` have no comments on it (`API/com/fs/starfarer/api/ui/PositionAPI.java`, `API/com/fs/starfarer/api/input/InputEventAPI.java`). The convention is only evidenced by code below.
- Vanilla example `API/com/fs/starfarer/api/impl/campaign/ExampleCustomUIPanel.java:55-81`: `render()` reads `p.getX(), p.getY(), getWidth(), getHeight()` and draws `GL_QUADS` with vertices `(x,y) (x,y+h) (x+w,y+h) (x+w,y)`. Lines 42-46 store `event.getX()/getY()` after `p.containsEvent(event)`; lines 27-28 and 86 use the result as a sprite centre in the same space.
- LunaLib hit test uses bottom edge = `position.y`: `M/LunaLib-2.0.5/src/lunalib/lunaUI/elements/LunaElement.kt:81-82` (`x = position.x; y = position.y`) and `:285` and `:321` (`event.y in y..(y+height)`). Same in `M/LunaLib-2.0.5/src/lunalib/backend/ui/components/base/LunaUIBaseElement.kt:136-137,205`.
- LunaLib places its close button at top-right with `screenHeight - sprite.height` as y: `M/LunaLib-2.0.5/src/lunalib/lunaUI/panel/LunaBaseCustomPanelPlugin.kt:31`, hit-tested against `event.y` at `:162-170`. That only reads as top-right if y is up.
- LunaLib draws a panel border with `glVertex2f(panel.position.x, panel.position.y)` ... `+height` in `render` (`LunaBaseCustomPanelPlugin.kt:104-133`) and filled rects with `glRectf(posX, posY, posX+width, posY+height)` in `renderBelow` (`M/LunaLib-2.0.5/src/lunalib/backend/ui/components/base/LunaUIPlaceholder.kt:28-47`).
- SiC draws bars with `GL11.glRectf(x, y, x+width, y+height)` in `renderBelow` (`M/Second-in-Command-2.0.0/src/second_in_command/ui/elements/OfficerXPBar.kt:55-100`), and uses `GL_QUAD_STRIP`/`GL_LINE_STRIP` in `M/Second-in-Command-2.0.0/src/second_in_command/ui/elements/PlayerXPBarElement.kt:89-92,141-156`.
- GL state both mods set before drawing untextured quads: `glDisable(GL_TEXTURE_2D)`, `glEnable(GL_BLEND)`, `glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)`, `glColor4f(..., a*alphaMult)`, wrapped in `glPushMatrix/glPopMatrix` (OfficerXPBar.kt:55-100; vanilla example uses `glColor4ub` and no push/pop).
- `positionChanged(PositionAPI)` is called when location or size changes; cache the `PositionAPI` there (`API/com/fs/starfarer/api/campaign/CustomUIPanelPlugin.java`, vanilla example lines 25-29). `renderBelow` is "below any UI elements in the panel", `render` above them (same file).
- `SettingsAPI.getScreenWidth/Height` are virtual pixels (divided by `getScreenScaleMult()`); `getScreenWidthPixels/HeightPixels` are physical (`API/com/fs/starfarer/api/SettingsAPI.java:196-208,443`).

### Inferences
- Rendering bounds are `[x, x+w] x [y, y+h]` with y as the bottom edge; flip your tree-layout y when converting from a top-left model. Rests on ExampleCustomUIPanel and LunaElement hit tests.
- `inTL(xPad, yPad)` positions are top-left-relative in layout, but the resulting `PositionAPI.getY()` is still the bottom edge (SiC calls `inTL` then draws with `position.y`).

### Gaps
- No official statement of the convention; it is inferred from consistent vanilla and mod code. Whether `InputEventAPI` coordinates are affected by `screenScaleMult` was not tested (no in-game run).

## 2. showCustomVisualDialog and CustomVisualDialogDelegate

### Takeaway
Signature: `void InteractionDialogAPI.showCustomVisualDialog(float customPanelWidth, float customPanelHeight, CustomVisualDialogDelegate delegate)`. From the map with no dialog open, show a tiny dummy `InteractionDialogPlugin` via `CampaignUIAPI.showInteractionDialog` and call `showCustomVisualDialog` in its `init` (LunaLib pattern). From an existing dialog, call it directly on that dialog.

### Cited Findings
- `API/com/fs/starfarer/api/campaign/InteractionDialogAPI.java:106`: `void showCustomVisualDialog(float customPanelWidth, float customPanelHeight, CustomVisualDialogDelegate delegate);`
- `API/com/fs/starfarer/api/campaign/CustomVisualDialogDelegate.java`:
  ```java
  public interface CustomVisualDialogDelegate {
      public interface DialogCallbacks { void dismissDialog(); FaderUtil getPanelFader(); }
      void init(CustomPanelAPI panel, DialogCallbacks callbacks);
      CustomUIPanelPlugin getCustomPanelPlugin();
      float getNoiseAlpha();
      void advance(float amount);
      void reportDismissed(int option);
  }
  ```
- `CustomUIPanelPlugin` methods: `positionChanged(PositionAPI)`, `renderBelow(float)`, `render(float)`, `advance(float)`, `processInput(List<InputEventAPI>)`, `buttonPressed(Object)`. `BaseCustomUIPanelPlugin` is a no-op base (`API/com/fs/starfarer/api/campaign/CustomUIPanelPlugin.java`, `BaseCustomUIPanelPlugin.java`). Events must be `consume()`d when acted on; accessing a consumed event's data throws.
- Lifecycle as used by vanilla `DuelDialogDelegate` (`API/com/fs/starfarer/api/impl/campaign/eventide/DuelDialogDelegate.java:33-80`): `init(panel, callbacks)` stores callbacks and builds UI into `panel`; `getCustomPanelPlugin()` returns the plugin that receives render/input; `advance(amount)` runs per frame and may call `callbacks.dismissDialog()`; `reportDismissed(option)` runs after dismissal. `DuelPanel.java:535-537` dismisses on `KEY_ESCAPE` key-down.
- Opening from inside a dialog: `dialog.showCustomVisualDialog(1024, 700, new DuelDialogDelegate(...))` (`API/com/fs/starfarer/api/impl/campaign/missions/SwordOfEventide.java:194`; `procgen/EventTestPluginImpl.java:164`).
- Opening from the map: `Global.getSector().getCampaignUI().showInteractionDialog(new OpenCustomPanelFromDialog(plugin), Global.getSector().getPlayerFleet())` (`M/LunaLib-2.0.5/src/lunalib/lunaExtensions/SectorExtensions.kt:12-14`). Signature: `boolean CampaignUIAPI.showInteractionDialog(InteractionDialogPlugin plugin, SectorEntityToken interactionTarget)` (`API/com/fs/starfarer/api/campaign/CampaignUIAPI.java:42`; target may be null per its javadoc).
- LunaLib's dummy plugin `M/LunaLib-2.0.5/src/lunalib/backend/ui/OpenCustomPanelFromDialog.kt:15-60`: in `init(dialog)` it calls `dialog.hideVisualPanel(); dialog.hideTextPanel(); dialog.promptText = ""` then `dialog.showCustomVisualDialog(screenWidth*0.8f, screenHeight*0.8f, VisualDelegate(plugin, dialog, false))`. Other `InteractionDialogPlugin` methods are empty; `getContext()` and `getMemoryMap()` return null. Delegate `getNoiseAlpha()` returns 0, `reportDismissed` is empty (`:62-100`).
- Closing: LunaLib `close()` calls `dialog.showTextPanel(); dialog.showVisualPanel(); callbacks.dismissDialog()` and, when it was not opened from an existing dialog, also `dialog.dismiss()` (`M/LunaLib-2.0.5/src/lunalib/lunaUI/panel/LunaBaseCustomPanelPlugin.kt:190-203`). Without the extra `dialog.dismiss()` the dummy dialog would stay.
- Extension for in-dialog use: `InteractionDialogAPI.openLunaCustomPanel` (`M/LunaLib-2.0.5/src/lunalib/lunaExtensions/DialogExtensions.kt:9-14`).
- LunaLib's settings panel handles ESC and the settings keybind itself in `processInput` (key-down, `consume()`, then `close()`), with a cooldown to stop the open key re-closing it (`M/LunaLib-2.0.5/src/lunalib/backend/ui/settings/LunaSettingsUIMainPanel.kt:120-165`).

### Inferences
- Panel size passed to `showCustomVisualDialog` becomes the `CustomPanelAPI` size handed to `init`; LunaLib uses 0.8 of the virtual screen. A full-screen panel is possible by passing `getScreenWidth()/getScreenHeight()` (not verified in-game).
- The plugin must provide its own close path (ESC and/or button); nothing in the API closes it for you.

### Gaps
- Meaning of the `int option` in `reportDismissed` is undocumented; vanilla ignores it. Not checked in the obfuscated jar.
- Whether the dummy dialog flashes visibly at open was not tested.

## 3. CampaignInputListener and hotkeys

### Takeaway
Implement `CampaignInputListener` and register with `Global.getSector().getListenerManager().addListener(listener, true)` (transient). Match the key with `isKeyDownEvent` + `getEventValue`, skip consumed events, and `consume()` the event you act on. LunaLib itself does not use this; it polls `Keyboard.isKeyDown` in a transient EveryFrameScript.

### Cited Findings
- `API/com/fs/starfarer/api/campaign/listeners/CampaignInputListener.java`:
  ```java
  int getListenerInputPriority(); // higher = processes input first
  void processCampaignInputPreCore(List<InputEventAPI> events);
  void processCampaignInputPreFleetControl(List<InputEventAPI> events);
  void processCampaignInputPostCore(List<InputEventAPI> events);
  ```
- Dispatch is sorted by descending priority, per phase (`API/com/fs/starfarer/api/campaign/listeners/ListenerUtil.java:52-75`).
- Registration: `ListenerManagerAPI.addListener(Object listener, boolean isTransient)` (`API/com/fs/starfarer/api/campaign/listeners/ListenerManagerAPI.java:8-9`). SpeedUp registers transient in `onGameLoad`: `M/SpeedUp-1.2.2/org/dark/speedup/SUModPlugin.java:24`. RAT registers non-transient (`M/Random Assortment of Things-3.3.1/src/assortment_of_things/relics/RelicStations.kt:224-225`), which gets saved with the game; transient is the safer default.
- Consuming a hotkey: Console Commands `Keystroke.isPressed` loops events, skips `isConsumed`, requires `isKeyDownEvent` and `eventValue == keyCode`, checks `isCtrlDown/isAltDown/isShiftDown`, then `event.consume()` (`M/Console Commands-4.0.9/src/org/lazywizard/console/ConsoleSettings.kt:172-188`). Its listener runs in `processCampaignInputPreCore` at priority `Int.MAX_VALUE-10` and guards on `campaignUI.isShowingMenu` (`ConsolePlugins.kt:20-42`; it also calls `events.clear()` after opening).
- Simple example without consume: WWLB `MPC_missileStrikeKeypressListener` checks `ui.currentInteractionDialog == null` and `input.eventType == InputEventType.KEY_DOWN` (`M/What we left behind-4.5.1/src/data/scripts/campaign/abilities/MPC_missileStrikeKeypressListener.kt:14-30`).
- LunaLib hotkeys: `KeybindsScript : EveryFrameScript` with `runWhilePaused() = true`, registered by `Global.getSector().addTransientScript(KeybindsScript())` (`M/LunaLib-2.0.5/src/lunalib/LunaLibPlugin.kt:46`). It polls `Keyboard.isKeyDown(LoadedSettings.settingsKeybind)` each frame (`M/LunaLib-2.0.5/src/lunalib/backend/scripts/KeybindsScript.kt:45`), only when current core tab is FLEET/none and no dialog/menu is showing (`:30-33`), and debounces with a `keyPressed` flag plus 0.8 s `IntervalUtil` (`:18,35,42`). It does not consume the event, so the keystroke also reaches the game.

### Inferences
- For "no side effects", the listener approach with `consume()` is stronger than LunaLib's polling, which cannot consume. Rests on the two code paths above.
- Guard on `getCurrentInteractionDialog() == null`, `!isShowingMenu()` and `getCurrentCoreTab()` so the key does not fire in dialogs, menus or text input.

### Gaps
- Whether consuming in `PreCore` fully blocks vanilla hotkey handlers (e.g. a vanilla binding on the same key) was not tested; the javadoc says only that events should be consumed when acted on.

## 4. CoreUITabListener, close signal, and mounting into the vanilla skill panel

### Takeaway
`CoreUITabListener.reportAboutToOpenCoreTab(CoreUITabId tab, Object param)` fires only on open. There is no close method on it. SMS gets a close signal by deferring one frame, then reflecting on the core UI object to wrap its `CoreInteractionListener` via `getListener`/`setListener`. SiC does not use the listener at all: it polls every frame and swaps the vanilla panel in place.

### Cited Findings
- `API/com/fs/starfarer/api/campaign/listeners/CoreUITabListener.java`: `void reportAboutToOpenCoreTab(CoreUITabId tab, Object param);`. Dispatched by `ListenerUtil.reportAboutToOpenCoreTab` over `getListeners(CoreUITabListener.class)` (`ListenerUtil.java:338-342`). `CoreUITabId` = CHARACTER, FLEET, REFIT, CARGO, MAP, INTEL, OUTPOSTS (`API/com/fs/starfarer/api/campaign/CoreUITabId.java`). Register with `addListener(x, true)` (SMS: `M/Ship Mastery System-2.0.8/src/shipmastery/plugin/ModPlugin.java:337`).
- Close signal API: `CoreInteractionListener { void coreUIDismissed(); }` (`API/com/fs/starfarer/api/campaign/CoreInteractionListener.java`). `CoreUIAPI` itself exposes only `getTradeMode()` and `getHintPanel()` (`API/com/fs/starfarer/api/campaign/CoreUIAPI.java`); `getListener/setListener` are not in the API and are reflected.
- SMS one-frame defer pattern (`M/Ship Mastery System-2.0.8/src/shipmastery/campaign/listeners/CoreTabListenerHandler.java:17-41`): inside `reportAboutToOpenCoreTab` it calls `DeferredActionPlugin.performLater(action, 0f)`. The action gets `core = ReflectionUtils.getCoreUI()`, reads `origListener = (CoreInteractionListener) invokeMethod(core, "getListener")`, notifies its own `onCoreTabOpened(id)` listeners, and, if `origListener` is not already a `CoreInteractionListenerExt` marker, calls `setListener(CoreInteractionListener)` with a lambda that calls `origListener.coreUIDismissed()` then its own `onCoreUIDismissed()`. Reflected names: `getListener`, `setListener` (param class `CoreInteractionListener`, via `invokeMethodExtWithClasses`).
- Why defer: the UI is not built yet when the event fires. `DeferredActionPlugin` is an `EveryFrameScript` with `runWhilePaused() = true`, queue ordered by system time, so delay 0 runs on the next script tick (`M/Ship Mastery System-2.0.8/src/shipmastery/deferred/DeferredActionPlugin.java:12-60`; its comment notes game time runs faster in market dialogs).
- Finding the core UI (`M/Ship Mastery System-2.0.8/src/shipmastery/util/ReflectionUtils.java:147-159`): if `campaignUI.getCurrentInteractionDialog() == null` reflect field `"core"` on `campaignUI`, else call `"getCoreUI"` on the dialog.
- SiC (`M/Second-in-Command-2.0.0/src/second_in_command/scripts/SkillPanelReplacerScript.kt:21-100`, registered at `SCModPlugin.kt:117` as a transient script, `runWhilePaused = true`):
  - Gate: `Global.getSector().isPaused` and `campaignUI.currentCoreTab == CoreUITabId.CHARACTER` (`:33-34`).
  - Core: `state = AppDriver.getInstance().currentState` must be `com.fs.starfarer.campaign.CampaignState` (obfuscated-package class, `:37-38`); `invoke("getEncounterDialog", state)` then `invoke("getCoreUI", dialog)` when docked, else `invoke("getCore", state)` (`:44-53`).
  - Find vanilla skill panel (`:58-61`): `core.getChildrenCopy().filter { it is UIPanelAPI }`, then for each, the child that `ReflectionUtils.hasMethodOfName("canReassign", child)`; `panel.getParent()`; `parent.removeComponent(panel)`.
  - Mount (`:70-72`, `M/.../ui/SCSkillMenuPanel.kt:46-95`): `SCSkillMenuPanel(parent, ...)` reads `parent.getWidth()/getHeight()` (reflected), then `Global.getSettings().createCustom(width, height, null)`, `parent.addComponent(panel)`, `panel.createUIElement(width, height, true)`, `panel.addUIElement(element)`, positions children with `position.inTL(...)`.
  - The loop is idempotent because after `removeComponent` the `canReassign` child is gone, so later frames find nothing and return.
  - Reflected method names: `getChildrenCopy`, `getChildrenNonCopy`, `getParent`, `getWidth`, `getHeight`, `clearChildren` (`M/Second-in-Command-2.0.0/src/second_in_command/misc/UIExtensions.kt:8-38`), `getEncounterDialog`, `getCoreUI`, `getCore`, `canReassign` (existence check only). SiC reflection helper uses `MethodHandles` on `java.lang.reflect.Method/Field` loaded via `Class.forName` to avoid script-sandbox blocks (`M/Second-in-Command-2.0.0/src/second_in_command/misc/ReflectionUtils.kt:9-62,147-158,201-240`), with caches per class.
- SiC SCSkillMenuPanel rebuilds with `parent.removeComponent(panel)` then re-create/add on state change (`recreatePanel`, `SCSkillMenuPanel.kt:89-95`).

### Inferences
- A close signal without reflection does not exist in 0.98a API. Choices: (a) SMS wrapper on the core UI `CoreInteractionListener`, or (b) poll `campaignUI.getCurrentCoreTab() != CHARACTER` from an `EveryFrameScript(runWhilePaused=true)`, as LunaLib/SiC do. (b) needs no obfuscated names.
- If both a SMS-style wrapper and another mod wrap the listener, chaining via `origListener.coreUIDismissed()` is required (SMS does this); the `CoreInteractionListenerExt` marker (`M/Ship Mastery System-2.0.8/src/shipmastery/campaign/CoreInteractionListenerExt.java`) prevents double-wrapping only against SMS itself.
- Replacing vanilla's skill panel like SiC conflicts with SiC (both remove the `canReassign` child); a mod-compat decision for the PRD.

### Gaps
- Obfuscated class and method names (`CampaignState`, `getCore`, `getListener`) were taken from mod source, not re-verified in `starfarer_obf.jar`; they can change between game versions.
- `param` meaning of `reportAboutToOpenCoreTab` is undocumented.

## 5. TooltipMakerAPI.addTooltipTo on a CustomPanelAPI child

### Takeaway
Yes. `addTooltipTo(TooltipCreator, UIComponentAPI, TooltipLocation)` accepts any `UIComponentAPI`, and SiC attaches tooltips to LunaLib elements whose `elementPanel` is a `CustomPanelAPI`. Width comes from `TooltipCreator.getTooltipWidth(Object)`; expand behaviour is driven by `isTooltipExpandable` and the `expanded` argument.

### Cited Findings
- Signatures (`API/com/fs/starfarer/api/ui/TooltipMakerAPI.java:333-334`): `void addTooltipTo(TooltipCreator tc, UIComponentAPI to, TooltipLocation loc);` and `void addTooltipTo(TooltipCreator tc, UIComponentAPI to, TooltipLocation loc, boolean recreateEveryFrame);`. Also `addTooltipToPrevious(tc, loc[, recreateEveryFrame])` (`:242,316`) and `addTooltipToAddedRow(tc, loc[, recreateEveryFrame])` (`:189-190`).
- `TooltipLocation` = LEFT, RIGHT, ABOVE, BELOW (`:37-42`). `TooltipCreator`: `boolean isTooltipExpandable(Object tooltipParam); float getTooltipWidth(Object tooltipParam); void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam);` (`:59-63`). `BaseTooltipCreator` defaults: not expandable, width 500, empty body (`API/com/fs/starfarer/api/ui/BaseTooltipCreator.java`).
- `LunaElement.elementPanel` is a `CustomPanelAPI` (`M/LunaLib-2.0.5/src/lunalib/lunaUI/elements/LunaElement.kt:23`). SiC: `parentElement.addTooltipTo(SCSkillTooltipCreator(...), originSkillElement.elementPanel, TooltipLocation.BELOW)` (`M/Second-in-Command-2.0.0/src/second_in_command/ui/elements/AptitudeSkillBarElement.kt:68-71,120`) and a lambda helper `TooltipMakerAPI.addTooltip(to, location, width, lambda)` returning width, `isTooltipExpandable=false` (`M/Second-in-Command-2.0.0/src/second_in_command/misc/SCExtensions.kt:49-62`).
- MagicLib wraps the same call for a selector panel: `scrollerTooltip.addTooltip(selectorPanel, TooltipLocation.BELOW, 250f) { ... }` (`M/MagicLib-1.5.6/src/org/magiclib/paintjobs/MagicPaintjobRefitPanel.kt:141`).
- The `TooltipMakerAPI` that owns the call is the one the target component lives in (SiC calls it on the element the child was added to); `addTooltipTo` is called on the maker, not the panel.

### Inferences
- Width is fixed by `getTooltipWidth`; set it explicitly, since the 500 default is wide. Expand behaviour: when `isTooltipExpandable` is true the engine passes `expanded` to `createTooltip`; the trigger key/UI is not documented in these sources.
- `recreateEveryFrame=true` is the overload to use when tooltip text depends on live state (cost: tooltip rebuilt per frame while shown).

### Gaps
- Exact expand trigger and whether a tooltip on an off-screen/clipped scrolled child still shows were not found in the sources; test in-game.
- Whether a tooltip survives the target component being removed and re-added is untested.

## 6. Performance norms

### Takeaway
Cache `SpriteAPI` instances in fields and reuse them; call `loadTexture` only once per path and only when `getTextureId() <= 0`. All sampled mods use GL immediate mode in `render`/`renderBelow` without issue. No source quantifies `removeComponent`/re-add cost; mods avoid doing it per frame and rebuild only on state change.

### Cited Findings
- `SettingsAPI.getSprite(String filename)`: "File must already have been loaded" (`API/com/fs/starfarer/api/SettingsAPI.java:114-119`); `void loadTexture(String filename) throws IOException;` (`:227`); `void unloadTexture(String filename);` (`:489`); `getSprite(String category, String key)` and `getSprite(category, id, boolean emptySpriteOnNotFound)` (`:127,389`). Textures listed in `settings.json` graphics are preloaded; others need `loadTexture`.
- SMS load-if-missing: `SpriteAPI sprite = getSprite(path); if (sprite.getTextureId() <= 0) loadTexture(path);` (`M/Ship Mastery System-2.0.8/src/shipmastery/campaign/items/BasePseudocorePlugin.java:104-108`).
- Sprite cached as static field: `public static final SpriteAPI masteryBarSprite = Global.getSettings().getSprite("ui", "sms_mastery_bar")` (`M/Ship Mastery System-2.0.8/src/shipmastery/campaign/FleetPanelHandler.java:83`). Vanilla example loads in the constructor and reuses it each frame, mutating `setAlphaMult(alphaMult)` before `renderAtCenter` (`ExampleCustomUIPanel.java:21-23,85-86`). LunaLib fetches three sprites as plugin fields (`LunaBaseCustomPanelPlugin.kt:24-26`) and sets `alphaMult` per render (`:137-143`). SiC holds `border = getSprite("graphics/secondInCommand/skillBorderInactive.png")` as a field (`OfficerXPBar.kt:9`).
- Mutable per-sprite state (`alphaMult`, color, size, angle) is shared by every caller of the same path or category/key if `getSprite` returns a shared instance; this was not verified in code. Set state before every render to be safe.
- Immediate mode in render callbacks: vanilla (`ExampleCustomUIPanel.java:74-81`), LunaLib (`LunaBaseCustomPanelPlugin.kt:104-133`), SiC (`PlayerXPBarElement.kt:141-156`). SiC even toggles global state in UI code (`GL11.glDisable(GL_CULL_FACE)`, `glDisable(GL_BLEND)` in `SCSkillMenuPanel.recreatePanel`).
- Rebuild pattern: SiC rebuilds the whole mounted panel only on events (`recreatePanel`, `SCSkillMenuPanel.kt:89-95`) and the replacer idempotently self-disarms (`SkillPanelReplacerScript.kt:58-63`). LunaLib remembers scroll offset across rebuilds: `LunaSettingsUIModsPanel.kt:551` sets `externalScroller.yOffset = lastScroller` after rebuild; `:635-637` saves it.

### Inferences
- Per-frame `removeComponent`/re-add is unsupported by any evidence here and loses scroll/tooltip/hover state; rebuild on change only, and keep node sprites in a map keyed by path.
- To keep alpha correct under fade, always set `sprite.setAlphaMult(alphaMult * x)` inside `render`.

### Gaps
- No benchmark or forum source on the real cost of `removeComponent` or immediate mode vs VBOs in 0.98a; none found. `getSprite` sharing semantics and `loadTexture` memory cost are unverified.

## 7. Scroll and zoom in a CustomPanelAPI

### Takeaway
Two patterns exist. Vertical scrolling: use `CustomPanelAPI.createUIElement(w, h, true)` (scroller) and `addUIElement`, then read/write `getExternalScroller().getYOffset/setYOffset`. Pan/zoom of a 2D graph has no vanilla helper: no installed mod was found doing it, so it must be done by manual offset/scale in your own `render` and `processInput` with `isMouseScrollEvent`.

### Cited Findings
- `CustomPanelAPI.createUIElement(float width, float height, boolean withScroller)` and `addUIElement(TooltipMakerAPI)` (`API/com/fs/starfarer/api/ui/CustomPanelAPI.java`). `TooltipMakerAPI.getExternalScroller()` is "only non-null if this tooltip was added to a CustomPanelAPI using addUIElement()" and `setExternalScroller(ScrollPanelAPI)` exists (`TooltipMakerAPI.java:345-351`). `ScrollPanelAPI`: `getXOffset/getYOffset/setXOffset/setYOffset` (`API/com/fs/starfarer/api/ui/ScrollPanelAPI.java`).
- LunaLib: `subpanel.createUIElement(width - 9, height - space, true)` then restores `externalScroller.yOffset` after rebuild (`M/LunaLib-2.0.5/src/lunalib/backend/ui/settings/LunaSettingsUIModsPanel.kt:418,551,635-637`). With custom children, set `heightSoFar` before adding the element so the scroller knows content height: MagicLib `scrollerTooltip.heightSoFar = ...; paintjobPanel.addUIElement(scrollerTooltip)` with `addCustomDoNotSetPosition(...)` (`M/MagicLib-1.5.6/src/org/magiclib/paintjobs/MagicPaintjobRefitPanel.kt:100,126,176-179`); API: `TooltipMakerAPI.setHeightSoFar(float)` and `addCustomDoNotSetPosition(UIComponentAPI)` (`TooltipMakerAPI.java:287-291,302`).
- Raw wheel handling: SMS reads `event.isMouseScrollEvent()` and the sign of `getEventValue()` (negative = scroll down in its code) to step a slider (`M/Ship Mastery System-2.0.8/src/shipmastery/ui/MasteryDisplay.java:425-435`). `InputEventAPI` has `getDX/getDY` and `isMouseScrollEvent()` (`InputEventAPI.java`).
- Blocking input to what is underneath a panel: SiC's panel plugins consume mouse move, mouse down and scroll events (`M/Second-in-Command-2.0.0/src/second_in_command/ui/panels/ManagePanelPlugin.kt:125-129`, also `BackgroundPanelPlugin.kt:122-126`, `AssosciatesManagePanelPlugin.kt:125-129`); Console and MagicLib do the same (`M/Console Commands-4.0.9/src/org/lazywizard/console/overlay/v2/panels/ConsoleOverlayPanel.kt:1302-1305`; `MagicPaintjobRefitPanel.kt:79`; `LunaLib RefitPanelBackgroundPlugin.kt:164`). Note the Javadoc says mouse-move events should generally not be consumed (`CustomUIPanelPlugin.java`); these mods do so deliberately for modal overlays.
- No mod in `game/mods` with `zoom` handling in a CustomPanelAPI: grep of `zoom` over `*.kt/*.java` hit only combat/HUD code and MagicLib's refit panel, which calls a reflected vanilla `updateButtonPositionsToZoomLevel` on the refit ship display (`MagicPaintjobRefitPanel.kt:171`). RAT's minimap sets `params.zoomLevel` for the vanilla sector map only (`M/Random Assortment of Things-3.3.1/src/assortment_of_things/campaign/ui/MinimapUI.kt:116-118`).

### Inferences
- For a node-graph tree needing zoom, manual transform is required: keep `offsetX/offsetY/scale` in the plugin, draw with `GL11.glPushMatrix(); glTranslatef; glScalef` in `render`, and invert the same transform for hit tests on `event.getX()/getY()` (same coordinate space as `position`, Q1). `TooltipMakerAPI` scrollers give vertical or horizontal list scrolling only, with native scrollbar and clipping.
- Native `TooltipMakerAPI` children added with `addCustom` do not follow a manual `glTranslatef/glScalef`; mixing scaled GL drawing with positioned UI children (tooltips, buttons) means converting to screen space yourself.
- GL scissor to clip content to the panel bounds is not demonstrated in any sampled mod; the scroller approach gets clipping for free.

### Gaps
- No example of zoom or GL scissor clipping in a `CustomPanelAPI` found in installed mods; web search returned nothing for Starsector. Consider checking forum threads on `CustomUIPanelPlugin` (Starsector forum, modding section) if manual zoom is chosen.
- Whether `glTranslatef/glScalef` inside `render` is affected by panel clipping in a scroller-wrapped panel is untested.
