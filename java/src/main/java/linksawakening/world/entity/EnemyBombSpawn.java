package linksawakening.world.entity;

/** Inputs for EntityEnemySpawns.spawnEnemyBomb. */
record EnemyBombSpawn(
        int entityX,
        int entityY,
        int entityZ,
        int transitionCountdown,
        int speedX,
        int speedY,
        int speedZ,
        boolean initializeMotion,
        int privateState4) {}
