package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EnemyProjectileCollision;

final class RoomEntityFrameState {
    final RecentRoomEntityClears recentRoomEntityClears = new RecentRoomEntityClears();
    EnemyDropResolver.CounterState enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
    int boomerangSoundCounter;
    boolean boomerangSoundClockProvided;
    int enemyDropMaxHearts = 3;
    int enemyDropHealth = 6;
    Boolean enemyDropLowHealth;
    boolean enemyDropActivePowerUp;
    int currentLinkMotionState = EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE;
    boolean worldLinkMotionBlockPending;
    GameplaySoundSink colorShellSoundSink = GameplaySoundSink.none();
}
