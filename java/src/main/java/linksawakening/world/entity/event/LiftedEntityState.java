package linksawakening.world.entity.event;

public record LiftedEntityState(
        int slot,
        int type,
        int phase,
        int transitionCountdown,
        int carryState,
        int sourceRomDirection,
        int effectiveRomDirection) {
    private static final LiftedEntityState NONE = new LiftedEntityState(-1, 0xFF, 0, 0, 0, 0, 0);

    public static LiftedEntityState none() {
        return NONE;
    }

    public boolean active() {
        return slot >= 0;
    }
}
