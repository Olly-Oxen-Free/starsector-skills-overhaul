
## 1a (advisory, PR #1)
- PathTreeBuilder inTL comment still says "same top-down Y convention"; reword (inTL uses a top pad, stored geometry is bottom-up). Hand to 3a.
- portraitY in renderBelow assumes the portrait is the rail's first item at its top edge; verify in M1.
- Pre-existing dead code left for 3a: `pyy` local in processInput, unused `metrics` param of emitLaneColumn, 6-arg refreshEdgesAfterLayout overload, always-false Edge.stippled branch.
- M1 in-game check still pending (user).
