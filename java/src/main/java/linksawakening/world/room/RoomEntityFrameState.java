package linksawakening.world.room;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EnemyProjectileCollision;

final class RoomEntityFrameState {
    /** Absent until LoadRoomEntities installs the current room runtime. */
    linksawakening.world.entity.RoomEntityRuntime runtime;

    final RoomEntityOutbox outbox = new RoomEntityOutbox(() -> runtime);

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
}
