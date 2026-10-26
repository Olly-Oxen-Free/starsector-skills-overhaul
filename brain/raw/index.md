# Raw Layer Index

Durable findings that survive context compaction. The wiki is regenerable;
this layer is the evidence it cites.

- `sessions/YYYY-MM-DD-<slug>.md` - decisions, root causes, handoffs
- `sources/<slug>.md` - excerpts from external references
- `corrections/YYYY-MM-DD-<slug>.md` - mistakes and their patches

## Findings
- [2026-10-26 starsector ui y axis](sessions/2026-10-26-starsector-ui-y-axis.md) — Starsector UI coordinates are bottom-up: PositionAPI.getY() is the bottom edge, InputEventAPI.getY() shares that space
- [2026-10-26 raid listener api](sessions/2026-10-26-raid-listener-api.md) — Player raids reach mods only through ColonyPlayerHostileActListener registered via ListenerManager, not BaseCampaignEventListener
- [2026-10-26 sic panel conflict](sessions/2026-10-26-sic-panel-conflict.md) — Second-in-Command removes the vanilla skill panel (canReassign) from its parent, so any mod replacing it in the Character tab conflicts; use a dialog host under SiC
- [2026-10-26 skills panel restructure](sessions/2026-10-26-skills-panel-restructure.md) — Panel restructure + increment 6 on feature branch; PR #9 awaits owner merge and in-game checks
