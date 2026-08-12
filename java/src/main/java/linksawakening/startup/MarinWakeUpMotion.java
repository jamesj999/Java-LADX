package linksawakening.startup;

/** Source-timed state for MarinEntityHandler_Indoor's new-game bed sequence. */
public final class MarinWakeUpMotion {
    private static final int LINK_BED_X = 0x38;
    private static final int LINK_BED_Y = 0x34;
    private static final int WAKE_DIALOG = 0x01;
    private static final int FOLLOW_UP_DIALOG = 0x02;
    private static final int[] BED_ANIMATION = {
        3, 3, 3, 3, 3, 4, 3, 4, 3, 3, 3, 2, 2, 2, 2, 2,
        0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1
    };
    private static final int[] MARIN_VARIANT_FOR_DIRECTION = { 6, 4, 2, 0 };

    private enum State { TOSSING, AWAKE, COMPLETE }

    private State state = State.TOSSING;
    private int slowTossingCountdown = 127;
    private boolean marinInitialized;
    private int marinX;
    private int marinY;
    private int marinDirection = 1;
    private int marinInertia;

    public static MarinWakeUpMotion postWake() {
        MarinWakeUpMotion motion = new MarinWakeUpMotion();
        motion.state = State.COMPLETE;
        motion.slowTossingCountdown = 0;
        return motion;
    }

    public Update tick(int frameCounter, boolean dialogActive, boolean directionPressed) {
        boolean openWakeDialog = false;
        boolean leaveBed = false;
        if (state == State.TOSSING) {
            if (!dialogActive && (frameCounter & 0x03) == 0 && slowTossingCountdown > 0) {
                slowTossingCountdown--;
            }
            if (slowTossingCountdown == 0) {
                state = State.AWAKE;
                openWakeDialog = true;
            }
        } else if (state == State.AWAKE && !dialogActive) {
            if (directionPressed) {
                state = State.COMPLETE;
                leaveBed = true;
            }
        }
        int bedVariant = state == State.TOSSING
            ? BED_ANIMATION[(slowTossingCountdown >>> 2) & 0x1F] : 3;
        return new Update(openWakeDialog, WAKE_DIALOG, state != State.COMPLETE,
            leaveBed, LINK_BED_X, LINK_BED_Y, bedVariant);
    }

    public MarinPresentation tickMarinPresentation(int frameCounter, int sourceX, int sourceY,
                                                    int linkX, int linkY) {
        boolean initializedNow = false;
        if (!marinInitialized) {
            marinInitialized = true;
            initializedNow = true;
            marinX = (sourceX - 8) & 0xFF;
            marinY = (sourceY - 8) & 0xFF;
        }
        if (initializedNow) {
            return new MarinPresentation(marinX, marinY, marinDirection,
                MARIN_VARIANT_FOR_DIRECTION[marinDirection]);
        }
        if ((frameCounter & 0x1F) == 0) {
            marinDirection = directionToLink(marinX, marinY, linkX, linkY);
        }
        marinInertia = (marinInertia + 1) & 0xFF;
        int variant = MARIN_VARIANT_FOR_DIRECTION[marinDirection]
            | ((marinInertia >>> 4) & 0x01);
        return new MarinPresentation(marinX, marinY, marinDirection, variant);
    }

    public boolean complete() {
        return state == State.COMPLETE;
    }

    public int followUpDialogLowId() {
        return FOLLOW_UP_DIALOG;
    }

    public boolean canOpenFollowUpDialog(int marinX, int marinY, int linkX, int linkY,
                                         int linkDirection, boolean linkAirborne,
                                         boolean actionButtonHeld, boolean dialogActive) {
        if (!complete() || linkAirborne || dialogActive || !actionButtonHeld
            || ((linkY - marinY + 0x14) & 0xFF) >= 0x28
            || ((linkX - marinX + 0x10) & 0xFF) >= 0x20) {
            return false;
        }
        return directionToLink(marinX, marinY, linkX, linkY) == (linkDirection ^ 0x01);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = (byte) (linkX - entityX);
        int distanceY = (byte) (linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    public record Update(boolean openWakeDialog, int dialogLowId,
                         boolean linkMotionBlocked, boolean leaveBed,
                         int linkRomX, int linkRomY, int bedSpriteVariant) {
    }

    public record MarinPresentation(int romX, int romY, int romDirection, int spriteVariant) {
    }
}
