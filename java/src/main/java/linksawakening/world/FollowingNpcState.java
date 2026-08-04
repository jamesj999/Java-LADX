package linksawakening.world;

/**
 * The persistent follower flags consumed by CreateFollowingNpcEntity in
 * bank $01. Values are kept in the same byte-shaped form as the WRAM flags so
 * event code can eventually bind this value object directly to save state.
 */
public record FollowingNpcState(
    boolean roosterFollowing,
    int ghostFollowingState,
    boolean marinFollowing,
    boolean bowWowFollowing,
    int instrument4Flags,
    int powerBraceletLevel,
    boolean db10) {

    public FollowingNpcState {
        if (ghostFollowingState < 0 || ghostFollowingState > 2) {
            throw new IllegalArgumentException("Ghost following state must be 0, 1, or 2");
        }
        if ((instrument4Flags & ~0xFF) != 0) {
            throw new IllegalArgumentException("Instrument flags must be an unsigned byte");
        }
        if (powerBraceletLevel < 0 || powerBraceletLevel > 0xFF) {
            throw new IllegalArgumentException("Power Bracelet level must be an unsigned byte");
        }
    }

    public static FollowingNpcState none() {
        return new FollowingNpcState(false, 0, false, false, 0, 0, false);
    }

    public FollowingNpcState withGhostFollowingState(int state) {
        return new FollowingNpcState(roosterFollowing, state, marinFollowing, bowWowFollowing,
            instrument4Flags, powerBraceletLevel, db10);
    }
}
