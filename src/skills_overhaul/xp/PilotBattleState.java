package skills_overhaul.xp;

/** Engine-free battle bookkeeping for PilotXPTracker, so it can be tested. */
final class PilotBattleState {
    private String firstShipId = null;
    private boolean firstShipEverDied = false;

    /** Called every frame with the current player ship. The flagship latches on the
     *  first frame a live, non-shuttle player ship exists (it is usually null in
     *  init(), before deployment). Returns true on the frame it latches. */
    boolean observePlayerShip(String id, boolean alive, boolean shuttle) {
        if (firstShipId == null) {
            if (id == null || !alive || shuttle) return false;
            firstShipId = id;
            return true;
        }
        if (!firstShipEverDied && (id == null || !alive || !firstShipId.equals(id))) {
            firstShipEverDied = true;
        }
        return false;
    }

    boolean isLatched() { return firstShipId != null; }
    boolean firstShipEverDied() { return firstShipEverDied; }

    /** Victory needs an enemy to have been seen, no enemy reserves left to
     *  deploy, and no enemy ship alive on the field; otherwise only the engine's
     *  own combat-over flag ends the battle. */
    static boolean battleEnded(boolean combatOver, boolean anyEnemySeen,
                               int enemyReserves, boolean anyEnemyAlive) {
        if (combatOver) return true;
        return anyEnemySeen && enemyReserves == 0 && !anyEnemyAlive;
    }
}
