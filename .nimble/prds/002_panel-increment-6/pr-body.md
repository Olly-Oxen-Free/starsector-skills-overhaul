## Panel increment 6 + raid zeal wiring

### Tasks
1. **Per-tree load isolation** (cc836d9). One malformed tree JSON, or a duplicate node id, no longer stops its whole tab from loading. The failed tree is logged and listed in `TreeRegistry.getFailed`, and the panel shows a "Failed to load <tree>" column header for it, as spec §5 requires.
2. **T0 replace mode and real respec cost** (8648bec, 3912834).
   - Activating a third T0 used to displace `active.get(0)` automatically. It now enters a replace mode: the active T0 nodes pulse, a status line says "Choose an active basic to replace (Esc to cancel)", and the player clicks the one to replace.
   - The respec button shows "Respec (free)" or "Respec (1 SP)" from the new `RespecService.costFor`. It is disabled when a paid respec is unaffordable, and its tooltip explains why.
3. **Raid zeal** (ea6ec0d). This is a bug fix. `RaidZealListener` now implements `ColonyPlayerHostileActListener`, so Heavy Detachments grants +1 Zeal per raid (valuables or disrupt). Bombardments grant nothing. Before this change, `onRaid` was never called.

### Deliverables
| Deliverable | Check |
|---|---|
| Tree load isolation + label | `TreeLoaderFailureTest` passes |
| T0 replace mode | code review; in-game check pending |
| Respec cost | `RespecServiceTest` (`costFor` cases) passes |
| Raid zeal | `RaidZealTest` passes; it cannot compile against the old code |

### Validation
- build: `./build.sh` passed
- test: `./build.sh test` passed

### Advisory findings
- Failed-tree columns are appended after the loaded trees instead of sitting at their original index.
- The one-argument `TreeLoader.loadAllForTab(tab)` has no remaining callers.
- A disrupt raid grants one stack for each disrupted industry. Vanilla disrupts one industry per raid.
- Not yet checked in-game:
  - replace-mode pulse and Esc
  - respec tooltip
  - a raid granting Zeal
  - the failed-tree label

🤖 Generated with [Claude Code](https://claude.com/claude-code)
