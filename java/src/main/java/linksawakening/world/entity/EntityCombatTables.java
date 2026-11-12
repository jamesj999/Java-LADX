package linksawakening.world.entity;

/** Owned CombatTables storage; preserves ROM slot indexing. */
final class EntityCombatTables {
    private final int[] enemyStunnedCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dyingCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] powerRecoilDeath = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyHealthGroup = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyHealth = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyFlashCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyIgnoreHitsCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] enemyHitboxFlags = new int[EntityRoomLoader.MAX_ENTITIES];

    int readEnemyStunnedCountdown(int slot) {
        return enemyStunnedCountdown[slot];
    }

    int writeEnemyStunnedCountdown(int slot, int value) {
        return enemyStunnedCountdown[slot] = value;
    }

    int postEnemyStunnedCountdown(int slot, int delta) {
        int previous = enemyStunnedCountdown[slot];
        enemyStunnedCountdown[slot] += delta;
        return previous;
    }

    int readDyingCountdown(int slot) {
        return dyingCountdown[slot];
    }

    int writeDyingCountdown(int slot, int value) {
        return dyingCountdown[slot] = value;
    }

    int postDyingCountdown(int slot, int delta) {
        int previous = dyingCountdown[slot];
        dyingCountdown[slot] += delta;
        return previous;
    }

    boolean readPowerRecoilDeath(int slot) {
        return powerRecoilDeath[slot];
    }

    boolean writePowerRecoilDeath(int slot, boolean value) {
        return powerRecoilDeath[slot] = value;
    }

    int readEnemyHealthGroup(int slot) {
        return enemyHealthGroup[slot];
    }

    int writeEnemyHealthGroup(int slot, int value) {
        return enemyHealthGroup[slot] = value;
    }

    int readEnemyHealth(int slot) {
        return enemyHealth[slot];
    }

    int writeEnemyHealth(int slot, int value) {
        return enemyHealth[slot] = value;
    }

    int readEnemyFlashCountdown(int slot) {
        return enemyFlashCountdown[slot];
    }

    int writeEnemyFlashCountdown(int slot, int value) {
        return enemyFlashCountdown[slot] = value;
    }

    int postEnemyFlashCountdown(int slot, int delta) {
        int previous = enemyFlashCountdown[slot];
        enemyFlashCountdown[slot] += delta;
        return previous;
    }

    int readEnemyIgnoreHitsCountdown(int slot) {
        return enemyIgnoreHitsCountdown[slot];
    }

    int writeEnemyIgnoreHitsCountdown(int slot, int value) {
        return enemyIgnoreHitsCountdown[slot] = value;
    }

    int postEnemyIgnoreHitsCountdown(int slot, int delta) {
        int previous = enemyIgnoreHitsCountdown[slot];
        enemyIgnoreHitsCountdown[slot] += delta;
        return previous;
    }

    int readEnemyHitboxFlags(int slot) {
        return enemyHitboxFlags[slot];
    }

    int writeEnemyHitboxFlags(int slot, int value) {
        return enemyHitboxFlags[slot] = value;
    }
}
