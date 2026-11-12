package linksawakening.startup;

/** Source-timed state for TarinEntityHandler's indoor shield sequence. */
public final class TarinShieldMotion {
    /** Named state values. */
    public enum State {
        IDLE,
        OFFER,
        ITEM,
        COMPLETE
    }

    private static final int DOOR_Y = 0x7B;
    private static final int DOOR_DIALOG = 0x00;
    private static final int SHIELD_OFFER_DIALOG = 0x54;
    private static final int SHIELD_DIALOG = 0x91;

    private static final int UNBLOCKED = 0;
    private static final int BLOCK_DOOR = 1;
    private static final int OFFER_SHIELD = 2;
    private static final int OBTAIN_MUSIC = 3;
    private static final int WAIT_FOR_DIALOG = 4;
    private static final int GRANT_SHIELD = 5;
    private static final int PRESENT_SHIELD = 6;

    private State state = State.IDLE;
    private int transitionCountdown;

    public Update tick(boolean dialogActive, int linkY, boolean canTalk, int shieldLevel) {
        if ((linkY & ~0xFF) != 0 || shieldLevel < 0 || shieldLevel > 2) {
            throw new IllegalArgumentException("Tarin state inputs must be unsigned ROM values");
        }

        if (state == State.IDLE) {
            if (shieldLevel != 0) {
                state = State.COMPLETE;
                return Update.unblocked(linkY);
            }
            if (linkY >= DOOR_Y) {
                return Update.VALUES[BLOCK_DOOR][linkY - 2];
            }
            if (canTalk) {
                state = State.OFFER;
                return Update.VALUES[OFFER_SHIELD][linkY];
            }
            return Update.unblocked(linkY);
        }

        if (state == State.OFFER) {
            if (!dialogActive) {
                transitionCountdown = 0x80;
                state = State.ITEM;
                // TarinShield1Handler writes MUSIC_OBTAIN_ITEM only on the
                // frame that observes Dialog054 has ended, immediately
                // before advancing to state 2.
                return Update.VALUES[OBTAIN_MUSIC][linkY];
            }
            return Update.VALUES[WAIT_FOR_DIALOG][linkY];
        }

        if (state == State.ITEM) {
            if (transitionCountdown > 0) {
                transitionCountdown--;
            }
            if (transitionCountdown == 0) {
                state = State.COMPLETE;
                return Update.VALUES[GRANT_SHIELD][linkY];
            }
            return Update.VALUES[PRESENT_SHIELD][linkY];
        }

        return Update.unblocked(linkY);
    }

    public State state() {
        return state;
    }

    /** Mirrors ShouldLinkTalkToEntity's facing/proximity gate for Tarin. */
    public static boolean canTalkToEntity(
            int entityX,
            int entityY,
            int linkX,
            int linkY,
            int linkDirection,
            boolean linkAirborne,
            boolean actionButtonHeld,
            boolean dialogActive) {
        if (linkAirborne || !actionButtonHeld || dialogActive) {
            return false;
        }
        if (((linkY - entityY + 0x14) & 0xFF) >= 0x28 || ((linkX - entityX + 0x10) & 0xFF) >= 0x20) {
            return false;
        }
        int dx = (byte) (linkX - entityX);
        int dy = (byte) (linkY - entityY);
        int directionToLink;
        if (Math.abs(dy) >= Math.abs(dx)) {
            directionToLink = dy < 0 ? 2 : 3;
        } else {
            directionToLink = dx < 0 ? 1 : 0;
        }
        return directionToLink == (linkDirection ^ 0x01);
    }
    /** Immutable update values. */
    public record Update(
            int linkY,
            int dialogLowId,
            boolean linkMotionBlocked,
            boolean grantShield,
            boolean playObtainItemMusic,
            boolean presentShield) {
        // All inputs are validated ROM bytes. These immutable values may be retained by callers.
        private static final Update[][] VALUES = values();

        private static Update[][] values() {
            Update[][] values = new Update[7][256];
            for (int y = 0; y < 256; y++) {
                values[UNBLOCKED][y] = new Update(y, -1, false, false, false, false);
                values[BLOCK_DOOR][y] = new Update(y, DOOR_DIALOG, true, false, false, false);
                values[OFFER_SHIELD][y] = new Update(y, SHIELD_OFFER_DIALOG, true, false, false, false);
                values[OBTAIN_MUSIC][y] = new Update(y, -1, true, false, true, false);
                values[WAIT_FOR_DIALOG][y] = new Update(y, -1, true, false, false, false);
                values[GRANT_SHIELD][y] = new Update(y, SHIELD_DIALOG, true, true, false, false);
                values[PRESENT_SHIELD][y] = new Update(y, -1, true, false, false, true);
            }
            return values;
        }

        private static Update unblocked(int linkY) {
            return VALUES[UNBLOCKED][linkY];
        }
    }
}
