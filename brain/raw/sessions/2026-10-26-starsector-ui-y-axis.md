---
created: 2026-10-26
source_files:
- src/skills_overhaul/ui/PanelCoords.java
---

Verified against vanilla ExampleCustomUIPanel (game/starfarer.api.zip), LunaLib, SiC and nex4x DiplomacyOverlayPlugin. The old panel assumed y-down and only worked because it was centred with inMid(). Convert inTL(x, yTop) as localY = panelH - yTop - h; cursor local y = event.getY() - py. Use PanelCoords. Fixed in PR #1 (PRD 001).
