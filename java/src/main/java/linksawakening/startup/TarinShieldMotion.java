package linksawakening.startup;

/** Source-timed state for TarinEntityHandler's indoor shield sequence. */
public final class TarinShieldMotion {
    public enum State { IDLE, OFFER, ITEM, COMPLETE }

    private static final int DOOR_Y = 0x7B;
    private static final int DOOR_DIALOG = 0x00;
    private static final int SHIELD_OFFER_DIALOG = 0x54;
    private static final int SHIELD_DIALOG = 0x91;

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
                return new Update(linkY - 2, DOOR_DIALOG, true, false, false, false);
            }
            if (canTalk) {
                state = State.OFFER;
                return new Update(linkY, SHIELD_OFFER_DIALOG, true, false, false, false);
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
                return new Update(linkY, -1, true, false, true, false);
            }
            return new Update(linkY, -1, true, false, false, false);
        }

        if (state == State.ITEM) {
            if (transitionCountdown > 0) {
                transitionCountdown--;
            }
            if (transitionCountdown == 0) {
                state = State.COMPLETE;
                return new Update(linkY, SHIELD_DIALOG, true, true, false, false);
            }
            return new Update(linkY, -1, true, false, false, true);
        }

        return Update.unblocked(linkY);
    }

    public State state() {
        return state;
    }

    /** Mirrors ShouldLinkTalkToEntity's facing/proximity gate for Tarin. */
    public static boolean canTalkToEntity(int entityX, int entityY, int linkX, int linkY,
                                          int linkDirection, boolean linkAirborne,
                                          boolean actionButtonHeld, boolean dialogActive) {
        if (linkAirborne || !actionButtonHeld || dialogActive) {
            return false;
        }
        if (((linkY - entityY + 0x14) & 0xFF) >= 0x28
            || ((linkX - entityX + 0x10) & 0xFF) >= 0x20) {
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

    public record Update(int linkY, int dialogLowId, boolean linkMotionBlocked,
                         boolean grantShield, boolean playObtainItemMusic,
                         boolean presentShield) {
        private static Update unblocked(int linkY) {
            return new Update(linkY, -1, false, false, false, false);
        }
    }
}
