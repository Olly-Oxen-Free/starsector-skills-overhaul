---
created: 2026-10-26
source_files:
- src/skills_overhaul/effect/handlers/RaidZealListener.java
---

RaidZealListener extended BaseCampaignEventListener with an onRaid() that no engine path calls, so Heavy Detachments zeal never fired. ColonyPlayerHostileActListener (no Base adapter; 4 methods) fires from MarketCMD.raidConfirmContinue after the player confirms. Disrupt fires once per disrupted industry. Fixed in PR #10 (ea6ec0d).
