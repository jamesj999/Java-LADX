package linksawakening.world.room;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EnemyProjectileCollision;

final class RoomEntityFrameState {
    /** Absent until LoadRoomEntities installs the current room runtime. */
    linksawakening.world.entity.RoomEntityRuntime runtime;

    final RoomEntityOutbox outbox = new RoomEntityOutbox(() -> runtime);

    boolean hookshotActive() {
        return runtime != null && runtime.inspection().hookshotActive();
    }

    linksawakening.world.entity.RoomEntityRuntime runtime() {
        return runtime;
    }

    void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay)
            boomerangSoundCounter = (boomerangSoundCounter + 1)
                    % linksawakening.world.entity.EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD;
        boomerangSoundClockProvided = true;
    }

    final RecentRoomEntityClears recentRoomEntityClears = new RecentRoomEntityClears();
    EnemyDropResolver.CounterState enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
    int boomerangSoundCounter;
    boolean boomerangSoundClockProvided;
    int enemyDropMaxHearts = 3;
    int enemyDropHealth = 6;
    /** Absent means derive low-health status from hearts; tests may override the ROM gate. */
    Boolean enemyDropLowHealth;

    boolean enemyDropActivePowerUp;
    int currentLinkMotionState = EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE;
    boolean worldLinkMotionBlockPending;
    GameplaySoundSink colorShellSoundSink = GameplaySoundSink.none();

    void configureEnemyDropRuntime(linksawakening.world.entity.EnemyDropResolver resolver, RoomEntityInputs inputs) {
        if (runtime == null) {
            return;
        }
        runtime.configuration().setDialogActive(inputs.entityDialogActive);
        runtime.configuration().setEnemyDropResolver(resolver);
        runtime.configuration().setEnemyDropCounters(enemyDropCounters);
        runtime.configuration().setEnemyDropPlayerState(enemyDropMaxHearts, enemyDropHealth, enemyDropActivePowerUp);
        if (enemyDropLowHealth != null) {
            runtime.configuration()
                    .setEnemyDropPlayerState(
                            enemyDropMaxHearts, enemyDropHealth,
                            enemyDropActivePowerUp, enemyDropLowHealth);
        }
    }
}
