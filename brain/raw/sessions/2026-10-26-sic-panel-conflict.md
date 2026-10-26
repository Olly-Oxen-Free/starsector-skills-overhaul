---
created: 2026-10-26
source_files:
- src/skills_overhaul/ui/host/HostRouter.java
---

SiC SkillPanelReplacerScript.kt finds the child with method canReassign, removeComponent()s it and mounts its own panel. Skills Overhaul routes to DialogHost (showCustomVisualDialog, hotkey K / ui_hotkey) when SiC is enabled or VanillaCharTabProbe fails. CoreUITabListener fires before the tab exists and has no close hook: defer a frame and poll getCurrentCoreTab().
