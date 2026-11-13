package linksawakening.equipment;

import static linksawakening.equipment.Sword.STATE_NONE;

import java.util.Objects;
import linksawakening.rom.SwordTables;
import linksawakening.state.Direction;

/** Owns queued release and sector timing for ROM UpdateSpinAttackAnimation (bank2:46C9-4708). */
final class SwordSpin {
    private final SwordTables swordTables;

    SwordSpin(SwordTables swordTables) {
        this.swordTables = Objects.requireNonNull(swordTables, "swordTables");
    }

    boolean active() {
        return spinFramesRemaining > 0;
    }

    boolean queued() {
        return spinAttackQueued;
    }

    void queue(int direction) {
        // Preserve arbitrary non-spin facing input; fallback lookup maps unknown values down.
        spinBaseDirection = direction;
        spinAttackQueued = true;
        queuedSpinDelayFrames = 1;
    }

    void startAcquisition(int baseDirection) {
        if (baseDirection < Direction.DOWN.javaIndex() || baseDirection > Direction.RIGHT.javaIndex()) {
            throw new IllegalArgumentException("Invalid Link direction: " + baseDirection);
        }
        spinBaseDirection = baseDirection;
        spinFramesRemaining = SPIN_ATTACK_FRAMES;
        spinFrameCounter = 1;
        spinSector = 7;
        clearQueue();
    }

    void clearQueue() {
        spinAttackQueued = false;
        queuedSpinDelayFrames = 0;
    }

    void clearAnimation() {
        spinFramesRemaining = 0;
        spinFrameCounter = 0;
        spinSector = 0;
    }

    int advance(int previousState) {
        if (spinAttackQueued) {
            if (queuedSpinDelayFrames > 0) {
                queuedSpinDelayFrames--;
                return previousState;
            }
            spinAttackQueued = false;
            spinFramesRemaining = SPIN_ATTACK_FRAMES;
            // The ROM also decrements on hFrameCounter % 4 == 0; retain the visible cadence.
            spinFrameCounter = 1;
        }
        spinFramesRemaining--;
        if (spinFramesRemaining <= 0) {
            clearAnimation();
            return STATE_NONE;
        }
        spinSector = spinFramesRemaining >> 2;
        int state = animationState();
        if ((spinFrameCounter & 0x03) == 0) {
            spinFramesRemaining--;
            if (spinFramesRemaining <= 0) {
                clearAnimation();
                return STATE_NONE;
            }
        }
        spinFrameCounter++;
        return state;
    }

    private static final int SPIN_ATTACK_FRAMES = 0x20; // wIsUsingSpinAttack initial countdown ($20)
    private int spinFramesRemaining;
    private int spinFrameCounter;
    private int spinSector;
    private int spinBaseDirection = Direction.DOWN.javaIndex();
    private boolean spinAttackQueued;
    private int queuedSpinDelayFrames;

    int effectiveDirection(int direction) {
        if (!active()) {
            return direction;
        }
        int baseDirection = Direction.fromJavaOrDown(spinBaseDirection).romIndex();
        return Direction.fromRom((swordTables.swordSpinAbsoluteDirection(baseDirection, spinSector())) & 0x03)
                .javaIndex();
    }

    int animationState() {
        int baseDirection = Direction.fromJavaOrDown(spinBaseDirection).romIndex();
        return swordTables.swordSpinAnimationState(baseDirection, spinSector());
    }

    int spinSector() {
        return Math.min(7, Math.max(0, spinSector));
    }
}
