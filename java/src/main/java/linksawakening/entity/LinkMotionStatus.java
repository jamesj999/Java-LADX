package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.LinkMotionState;

/** Explicit motion-mode, recovery and per-frame gates from ApplyLinkMotionState. */
final class LinkMotionStatus {

    private LinkMotionState motionState = LinkMotionState.DEFAULT;

    LinkMotionState motionState() {
        return motionState;
    }

    LinkMotionState motionState(LinkMotionState value) {
        return motionState = value;
    }

    private int recoverCountdown;

    int recoverCountdown() {
        return recoverCountdown;
    }

    int recoverCountdown(int value) {
        return recoverCountdown = value;
    }

    private boolean recoveringInWater;

    boolean recoveringInWater() {
        return recoveringInWater;
    }

    boolean recoveringInWater(boolean value) {
        return recoveringInWater = value;
    }

    private int slowWalkingSpeed;

    int slowWalkingSpeed() {
        return slowWalkingSpeed;
    }

    int slowWalkingSpeed(int value) {
        return slowWalkingSpeed = value;
    }

    private int itemUsageContext;

    int itemUsageContext() {
        return itemUsageContext;
    }

    int itemUsageContext(int value) {
        return itemUsageContext = value;
    }

    private boolean romInteractiveMotionBlocked;

    boolean romInteractiveMotionBlocked() {
        return romInteractiveMotionBlocked;
    }

    boolean romInteractiveMotionBlocked(boolean value) {
        return romInteractiveMotionBlocked = value;
    }

    private int romInteractiveMotionBlockValue;

    int romInteractiveMotionBlockValue() {
        return romInteractiveMotionBlockValue;
    }

    int romInteractiveMotionBlockValue(int value) {
        return romInteractiveMotionBlockValue = value;
    }

    private boolean debugNoClip;

    boolean debugNoClip() {
        return debugNoClip;
    }

    boolean debugNoClip(boolean value) {
        return debugNoClip = value;
    }

    private int frameCounter;

    int frameCounter() {
        return frameCounter;
    }

    int frameCounter(int value) {
        return frameCounter = value;
    }

    private boolean gelClinging;

    boolean gelClinging() {
        return gelClinging;
    }

    boolean gelClinging(boolean value) {
        return gelClinging = value;
    }

    private int gelMotionFrame;

    int gelMotionFrame() {
        return gelMotionFrame;
    }

    int gelMotionFrame(int value) {
        return gelMotionFrame = value;
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
