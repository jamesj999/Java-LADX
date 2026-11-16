package linksawakening.entity;

import linksawakening.state.LinkMotionState;

/** Explicit motion-mode, recovery and per-frame gates from ApplyLinkMotionState. */
final class LinkMotionStatus {

    private LinkMotionState motionState = LinkMotionState.DEFAULT;

    private int recoverCountdown;

    private boolean recoveringInWater;

    private int slowWalkingSpeed;

    private int itemUsageContext;

    private boolean romInteractiveMotionBlocked;

    private int romInteractiveMotionBlockValue;

    private boolean debugNoClip;

    private int frameCounter;

    private boolean gelClinging;

    private int gelMotionFrame;

    LinkMotionStatus() {}

    LinkMotionState motionState() {
        return motionState;
    }

    LinkMotionState motionState(LinkMotionState value) {
        motionState = value;
        return motionState;
    }

    int recoverCountdown() {
        return recoverCountdown;
    }

    int recoverCountdown(int value) {
        recoverCountdown = value;
        return recoverCountdown;
    }

    boolean recoveringInWater() {
        return recoveringInWater;
    }

    boolean recoveringInWater(boolean value) {
        recoveringInWater = value;
        return recoveringInWater;
    }

    int slowWalkingSpeed() {
        return slowWalkingSpeed;
    }

    int slowWalkingSpeed(int value) {
        slowWalkingSpeed = value;
        return slowWalkingSpeed;
    }

    int itemUsageContext() {
        return itemUsageContext;
    }

    int itemUsageContext(int value) {
        itemUsageContext = value;
        return itemUsageContext;
    }

    boolean romInteractiveMotionBlocked() {
        return romInteractiveMotionBlocked;
    }

    boolean romInteractiveMotionBlocked(boolean value) {
        romInteractiveMotionBlocked = value;
        return romInteractiveMotionBlocked;
    }

    int romInteractiveMotionBlockValue() {
        return romInteractiveMotionBlockValue;
    }

    int romInteractiveMotionBlockValue(int value) {
        romInteractiveMotionBlockValue = value;
        return romInteractiveMotionBlockValue;
    }

    boolean debugNoClip() {
        return debugNoClip;
    }

    boolean debugNoClip(boolean value) {
        debugNoClip = value;
        return debugNoClip;
    }

    int frameCounter() {
        return frameCounter;
    }

    int frameCounter(int value) {
        frameCounter = value;
        return frameCounter;
    }

    boolean gelClinging() {
        return gelClinging;
    }

    boolean gelClinging(boolean value) {
        gelClinging = value;
        return gelClinging;
    }

    int gelMotionFrame() {
        return gelMotionFrame;
    }

    int gelMotionFrame(int value) {
        gelMotionFrame = value;
        return gelMotionFrame;
    }

    /** Applies the ROM's next-frame hLinkInteractiveMotionBlocked=$02 write. */
    void blockNextRomMotionFrame() {
        writeRomInteractiveMotionBlock(2);
    }

    void writeRomInteractiveMotionBlock(int value) {
        if ((value & ~255) != 0) throw new IllegalArgumentException("Interaction block must be an unsigned byte");
        // Keep the existing movement lock separate from the exact ROM equipment gate.
        romInteractiveMotionBlockValue = value;
        romInteractiveMotionBlocked = value != 0;
    }

    boolean accessDebugNoClip() {
        return debugNoClip;
    }

    boolean accessIsSwimming() {
        return motionState == LinkMotionState.SWIMMING;
    }
}
