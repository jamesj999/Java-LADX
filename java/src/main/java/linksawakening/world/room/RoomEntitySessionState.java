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

    private final RoomEntityOutbox outbox = new RoomEntityOutbox();

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

    private final RecentRoomEntityClears recentRoomEntityClears = new RecentRoomEntityClears();
    private EnemyDropResolver.CounterState enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
    private int boomerangSoundCounter;
    private boolean boomerangSoundClockProvided;
    private int enemyDropMaxHearts = 3;
    private int enemyDropHealth = 6;
    /** Absent means derive low-health status from hearts; tests may override the ROM gate. */
    private Boolean enemyDropLowHealth;

    private boolean enemyDropActivePowerUp;
    private int currentLinkMotionState = EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE;
    private boolean worldLinkMotionBlockPending;
    private GameplaySoundSink colorShellSoundSink = GameplaySoundSink.none();

    void configureEnemyDropRuntime(
            RoomRuntime loaded, linksawakening.world.entity.EnemyDropResolver resolver, RoomEntityInputs inputs) {
        loaded.entities().configuration().setDialogActive(inputs.entityDialogActive());
        loaded.entities().configuration().setEnemyDropResolver(resolver);
        loaded.entities().configuration().setEnemyDropCounters(enemyDropCounters);
        loaded.entities()
                .configuration()
                .setEnemyDropPlayerState(enemyDropMaxHearts, enemyDropHealth, enemyDropActivePowerUp);
        if (enemyDropLowHealth != null) {
            loaded.entities()
                    .configuration()
                    .setEnemyDropPlayerState(
                            enemyDropMaxHearts, enemyDropHealth,
                            enemyDropActivePowerUp, enemyDropLowHealth);
        }
    }

    RecentRoomEntityClears recentRoomEntityClears() {
        return this.recentRoomEntityClears;
    }

    RoomEntityOutbox outbox() {
        return this.outbox;
    }

    int currentLinkMotionState() {
        return this.currentLinkMotionState;
    }

    boolean worldLinkMotionBlockPending() {
        return this.worldLinkMotionBlockPending;
    }

    boolean worldLinkMotionBlockPending(boolean value) {
        this.worldLinkMotionBlockPending = value;
        return this.worldLinkMotionBlockPending;
    }

    EnemyDropResolver.CounterState enemyDropCounters() {
        return this.enemyDropCounters;
    }

    EnemyDropResolver.CounterState enemyDropCounters(EnemyDropResolver.CounterState value) {
        this.enemyDropCounters = value;
        return this.enemyDropCounters;
    }

    int enemyDropMaxHearts() {
        return this.enemyDropMaxHearts;
    }

    int enemyDropMaxHearts(int value) {
        this.enemyDropMaxHearts = value;
        return this.enemyDropMaxHearts;
    }

    int enemyDropHealth() {
        return this.enemyDropHealth;
    }

    int enemyDropHealth(int value) {
        this.enemyDropHealth = value;
        return this.enemyDropHealth;
    }

    Boolean enemyDropLowHealth() {
        return this.enemyDropLowHealth;
    }

    Boolean enemyDropLowHealth(Boolean value) {
        this.enemyDropLowHealth = value;
        return this.enemyDropLowHealth;
    }

    boolean enemyDropActivePowerUp() {
        return this.enemyDropActivePowerUp;
    }

    boolean enemyDropActivePowerUp(boolean value) {
        this.enemyDropActivePowerUp = value;
        return this.enemyDropActivePowerUp;
    }

    int boomerangSoundCounter() {
        return this.boomerangSoundCounter;
    }

    int boomerangSoundCounter(int value) {
        this.boomerangSoundCounter = value;
        return this.boomerangSoundCounter;
    }

    boolean boomerangSoundClockProvided() {
        return this.boomerangSoundClockProvided;
    }

    boolean boomerangSoundClockProvided(boolean value) {
        this.boomerangSoundClockProvided = value;
        return this.boomerangSoundClockProvided;
    }

    GameplaySoundSink colorShellSoundSink() {
        return this.colorShellSoundSink;
    }
}
