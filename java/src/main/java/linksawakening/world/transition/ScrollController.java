package linksawakening.world.transition;

import linksawakening.world.entity.ScreenShake;
import linksawakening.world.room.RoomRenderSnapshot;

public final class ScrollController {
    public static final int NONE = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;

    // Retain one callable instance: its identity or allocation-free reuse is part of this retained path.
    @SuppressWarnings("UnnecessaryLambda")
    private static final Runnable NO_COMPLETION = () -> {};

    private Runnable completion = NO_COMPLETION;
    private int direction = NONE;
    private int offset;
    private int target;
    private int linkScreenX;
    private int linkScreenY;
    private RoomRenderSnapshot previousRoom;
    private Preparation preparation;
    private PreparationPhase preparationPhase;
    private int transferFramesRemaining;
    private int switchBlockFramesRemaining;
    private boolean tileTransferOnlyFrame;
    private final ScreenShake screenShakeMotion = ScreenShake.create();
    private boolean scriptedScreenShakeActive;
    private int scriptedScreenShakeHorizontal;

    public void start(int direction, int linkScreenX, int linkScreenY, RoomRenderSnapshot previousRoom, int target) {
        this.completion = NO_COMPLETION;
        this.direction = direction;
        this.linkScreenX = linkScreenX;
        this.linkScreenY = linkScreenY;
        this.previousRoom = previousRoom;
        this.target = target;
        this.offset = 0;
        this.preparation = null;
        this.tileTransferOnlyFrame = false;
    }

    public void startRoomTransition(
            int direction,
            int linkScreenX,
            int linkScreenY,
            RoomRenderSnapshot previousRoom,
            int target,
            Preparation preparation) {
        start(direction, linkScreenX, linkScreenY, previousRoom, target);
        this.preparation = preparation;
        this.preparationPhase = PreparationPhase.PREPARE_ROOM;
        this.transferFramesRemaining = 0;
        this.switchBlockFramesRemaining = preparation.switchBlockTransfers();
    }

    /** Runs once after the destination has finished scrolling into view. */
    public void onCompletion(Runnable completion) {
        this.completion = completion == null ? NO_COMPLETION : completion;
    }

    public boolean tick(int speed) {
        tileTransferOnlyFrame = false;
        if (!isActive()) {
            return false;
        }
        if (preparation != null) {
            tickPreparation();
            return false;
        }
        offset += speed;
        if (offset < target) {
            return false;
        }
        offset = 0;
        direction = NONE;
        target = 0;
        previousRoom = null;
        Runnable finished = completion;
        completion = NO_COMPLETION;
        finished.run();
        return true;
    }

    private void tickPreparation() {
        if (transferFramesRemaining > 0) {
            transferFramesRemaining--;
            tileTransferOnlyFrame = true;
            return;
        }
        // The requesting frame can already perform the first transfer in VBlank.
        switch (preparationPhase) {
            case PREPARE_ROOM -> {
                transferFramesRemaining = Math.max(0, preparation.roomTransfers() - 1);
                preparationPhase = PreparationPhase.SELECT_TILESETS;
            }
            case SELECT_TILESETS -> {
                transferFramesRemaining = Math.max(0, preparation.tilesetTransfers() - 1);
                if (preparation.tilesetTransfers() == 0 && switchBlockFramesRemaining > 0) {
                    switchBlockFramesRemaining--;
                }
                preparationPhase = PreparationPhase.CONFIGURE_TARGETS;
            }
            case CONFIGURE_TARGETS -> {
                if (switchBlockFramesRemaining > 0) {
                    switchBlockFramesRemaining--;
                } else {
                    preparation = null;
                }
            }
        }
    }

    /** The main loop's $00:02EF tile gate bypasses gameplay after reading input. */
    public boolean tileTransferOnlyFrame() {
        return tileTransferOnlyFrame;
    }

    /** The destination becomes dispatchable on the Prepare frame, not on boundary detection. */
    public boolean incomingRoomPrepared() {
        return isActive() && (preparation == null || preparationPhase != PreparationPhase.PREPARE_ROOM);
    }

    /** Amount of VBlank work scheduled by the first two ROM scroll handlers. */
    public record Preparation(int roomTransfers, int tilesetTransfers, int switchBlockTransfers) {}

    private enum PreparationPhase {
        PREPARE_ROOM,
        SELECT_TILESETS,
        CONFIGURE_TARGETS
    }

    public boolean isActive() {
        return direction != NONE;
    }

    public int direction() {
        return direction;
    }

    public int offset() {
        return offset;
    }

    public int target() {
        return target;
    }

    public int linkScreenX() {
        return linkScreenX;
    }

    public int linkScreenY() {
        return linkScreenY;
    }

    public RoomRenderSnapshot previousRoom() {
        return previousRoom;
    }

    /** Starts the ROM's shared screen-shake timer. */
    public void startScreenShake(int countdown, int phase) {
        screenShakeMotion.start(countdown, phase);
    }

    /** Advances the ROM screen-shake timer once before the gameplay frame renders. */
    public void tickScreenShake() {
        screenShakeMotion.tick();
    }

    public int screenShakeHorizontal() {
        return scriptedScreenShakeActive ? scriptedScreenShakeHorizontal : screenShakeMotion.horizontal();
    }

    public int screenShakeVertical() {
        return screenShakeMotion.vertical();
    }

    public int screenShakeCountdown() {
        return screenShakeMotion.countdown();
    }

    /** Applies a source-specific shake that does not use the shared phase table. */
    public void setScriptedScreenShakeHorizontal(boolean active, int horizontal) {
        scriptedScreenShakeActive = active;
        scriptedScreenShakeHorizontal = active ? horizontal : 0;
    }
}
