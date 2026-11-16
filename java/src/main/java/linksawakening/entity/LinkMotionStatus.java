package linksawakening.entity;

import linksawakening.state.LinkMotionState;

/** Explicit motion-mode, recovery and per-frame gates from ApplyLinkMotionState. */
final class LinkMotionStatus {

    private LinkMotionState motionState = LinkMotionState.DEFAULT;

    LinkMotionState motionState() {
        return motionState;
    }

    LinkMotionState motionState(LinkMotionState value) {
        motionState = value;
        return motionState;
    }

    private int recoverCountdown;

    int recoverCountdown() {
        return recoverCountdown;
    }

    int recoverCountdown(int value) {
        recoverCountdown = value;
        return recoverCountdown;
    }

    private boolean recoveringInWater;

    boolean recoveringInWater() {
        return recoveringInWater;
    }

    boolean recoveringInWater(boolean value) {
        recoveringInWater = value;
        return recoveringInWater;
    }

    private int slowWalkingSpeed;

    int slowWalkingSpeed() {
        return slowWalkingSpeed;
    }

    int slowWalkingSpeed(int value) {
        slowWalkingSpeed = value;
        return slowWalkingSpeed;
    }

    private int itemUsageContext;

    int itemUsageContext() {
        return itemUsageContext;
    }

    int itemUsageContext(int value) {
        itemUsageContext = value;
        return itemUsageContext;
    }

    private boolean romInteractiveMotionBlocked;

    boolean romInteractiveMotionBlocked() {
        return romInteractiveMotionBlocked;
    }

    boolean romInteractiveMotionBlocked(boolean value) {
        romInteractiveMotionBlocked = value;
        return romInteractiveMotionBlocked;
    }

    private int romInteractiveMotionBlockValue;

    int romInteractiveMotionBlockValue() {
        return romInteractiveMotionBlockValue;
    }

    int romInteractiveMotionBlockValue(int value) {
        romInteractiveMotionBlockValue = value;
        return romInteractiveMotionBlockValue;
    }

    private boolean debugNoClip;

    boolean debugNoClip() {
        return debugNoClip;
    }

    boolean debugNoClip(boolean value) {
        debugNoClip = value;
        return debugNoClip;
    }

    private int frameCounter;

    int frameCounter() {
        return frameCounter;
    }

    int frameCounter(int value) {
        frameCounter = value;
        return frameCounter;
    }

    private boolean gelClinging;

    boolean gelClinging() {
        return gelClinging;
    }

    boolean gelClinging(boolean value) {
        gelClinging = value;
        return gelClinging;
    }

    private int gelMotionFrame;

    int gelMotionFrame() {
        return gelMotionFrame;
    }

    int gelMotionFrame(int value) {
        gelMotionFrame = value;
        return gelMotionFrame;
    }

    LinkMotionStatus() {}

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
