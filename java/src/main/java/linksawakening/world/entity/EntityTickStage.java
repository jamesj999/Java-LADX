package linksawakening.world.entity;

/** Dispatch sites in slot execution order. Each visit retains its original early-return boundary. */
public enum EntityTickStage {
    PHASE_1_ENTRY(1),
    PHASE_1_AFTER_SPAWN_GUARD(1),
    PHASE_1_LEAF_PRODUCER(1),
    PHASE_1_BEFORE_STATUS_TIMERS(2),
    PHASE_1_AFTER_STATUS_TIMERS(2),
    PHASE_1_BEFORE_BOSS_INTRO(2),
    PHASE_1_BOSS_DOORWAY(1),
    PHASE_1_BOSS_INTRO(1),
    PHASE_1_BEFORE_PICKUP_SEQUENCE(25),
    PHASE_1_AFTER_INSTRUMENT_PICKUP(2),
    PHASE_2_BEFORE_STATUS_BRANCHES(1),
    PHASE_2_LIFTED(1),
    PHASE_2_LIFTED_FOLLOWER(1),
    PHASE_2_THROWN(1),
    PHASE_2_FALLING(1),
    PHASE_2_DYING(4),
    PHASE_2_STUNNED(1),
    PHASE_2_BEFORE_INITIALIZE(1),
    PHASE_2_AFTER_INITIALIZE(12),
    PHASE_2_BEFORE_RECOIL(3),
    PHASE_2_BEFORE_RECOIL_APPLICATION(10),
    PHASE_2_AFTER_RECOIL_APPLICATION(7),
    PHASE_2_AFTER_LEEVER_CONTACT(2),
    PHASE_3_BEFORE_LASER(16),
    PHASE_3_AFTER_LASER(17),
    PHASE_4_BEFORE_FOLLOWER(23),
    PHASE_5_BEFORE_COMMIT(2);

    private final int visits;

    EntityTickStage(int visits) {
        this.visits = visits;
    }

    int visits() {
        return visits;
    }
}
