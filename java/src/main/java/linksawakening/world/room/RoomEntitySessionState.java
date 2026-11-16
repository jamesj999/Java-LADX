package linksawakening.world.room;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EnemyProjectileCollision;

/** Session-level entity output queues, drop registers and per-frame gates; no loaded-room alias. */
final class RoomEntitySessionState {
    void setColorShellSoundSink(GameplaySoundSink sink) {
        colorShellSoundSink = sink == null ? GameplaySoundSink.none() : sink;
    }

    void observeMotionState(int state) {
        currentLinkMotionState = state & 0xFF;
    }

    final RoomEntityOutbox outbox = new RoomEntityOutbox();

    void overrideEnemyDropLowHealth(boolean low) {
        enemyDropLowHealth = low;
    }

    void clearGameOverPowerCounter() {
        enemyDropCounters = new EnemyDropResolver.CounterState(enemyDropCounters.guardianAcornCounter(), 0);
    }

    void clearRecentRoomEntityClears() {
        recentRoomEntityClears.reset();
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

    void configureEnemyDropRuntime(
            RoomRuntime loaded, linksawakening.world.entity.EnemyDropResolver resolver, RoomEntityInputs inputs) {
        loaded.entities.configuration().setDialogActive(inputs.entityDialogActive);
        loaded.entities.configuration().setEnemyDropResolver(resolver);
        loaded.entities.configuration().setEnemyDropCounters(enemyDropCounters);
        loaded.entities
                .configuration()
                .setEnemyDropPlayerState(enemyDropMaxHearts, enemyDropHealth, enemyDropActivePowerUp);
        if (enemyDropLowHealth != null) {
            loaded.entities
                    .configuration()
                    .setEnemyDropPlayerState(
                            enemyDropMaxHearts, enemyDropHealth,
                            enemyDropActivePowerUp, enemyDropLowHealth);
        }
    }
}
