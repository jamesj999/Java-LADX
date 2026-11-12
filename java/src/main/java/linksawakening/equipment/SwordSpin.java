package linksawakening.equipment;

import static linksawakening.equipment.Sword.*;

import linksawakening.rom.SwordTables;
import linksawakening.state.Direction;

/** Owns queued release and sector timing for ROM UpdateSpinAttackAnimation (bank2:46C9-4708). */
final class SwordSpin {
    private final SwordTables swordTables;

    SwordSpin(SwordTables swordTables) {
        this.swordTables = swordTables;
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

    private static final int SPIN_ATTACK_FRAMES = 0x20; // MAX_SWORD_CHARGE ($28) in the ROM
    // bank2.asm:46C9-46E8. Indexed as (base hLinkDirection << 3) + spin sector.
    private static final int[][] SPIN_ANIMATION_STATE_BY_BASE_DIRECTION = {
        {
            STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_START,
            STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_END
        },
        {
            STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_START,
            STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_END
        },
        {
            STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_END,
            STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_END
        },
        {
            STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_END,
            STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_END
        },
    };
    // bank2.asm:46E9-4708. Indexed as (base hLinkDirection << 3) + spin sector.
    private static final int[][] SPIN_ABSOLUTE_DIRECTION_BY_BASE_DIRECTION = {
        {
            Direction.RIGHT.javaIndex(), Direction.UP.javaIndex(), Direction.UP.javaIndex(), Direction.LEFT.javaIndex(),
            Direction.LEFT.javaIndex(), Direction.DOWN.javaIndex(), Direction.DOWN.javaIndex(),
                    Direction.RIGHT.javaIndex()
        },
        {
            Direction.LEFT.javaIndex(), Direction.UP.javaIndex(), Direction.UP.javaIndex(), Direction.RIGHT.javaIndex(),
            Direction.RIGHT.javaIndex(), Direction.DOWN.javaIndex(), Direction.DOWN.javaIndex(),
                    Direction.LEFT.javaIndex()
        },
        {
            Direction.UP.javaIndex(), Direction.RIGHT.javaIndex(), Direction.RIGHT.javaIndex(),
                    Direction.DOWN.javaIndex(),
            Direction.DOWN.javaIndex(), Direction.LEFT.javaIndex(), Direction.LEFT.javaIndex(), Direction.UP.javaIndex()
        },
        {
            Direction.DOWN.javaIndex(), Direction.LEFT.javaIndex(), Direction.LEFT.javaIndex(),
                    Direction.UP.javaIndex(),
            Direction.UP.javaIndex(), Direction.RIGHT.javaIndex(), Direction.RIGHT.javaIndex(),
                    Direction.DOWN.javaIndex()
        },
    };
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
        return swordTables == null
                ? SPIN_ABSOLUTE_DIRECTION_BY_BASE_DIRECTION[baseDirection][spinSector()]
                : Direction.fromRom((swordTables.swordSpinAbsoluteDirection(
                                        Direction.fromJavaOrDown(spinBaseDirection)
                                                .romIndex(),
                                        spinSector()))
                                & 0x03)
                        .javaIndex();
    }

    int animationState() {
        int baseDirection = Direction.fromJavaOrDown(spinBaseDirection).romIndex();
        return swordTables == null
                ? SPIN_ANIMATION_STATE_BY_BASE_DIRECTION[baseDirection][spinSector()]
                : swordTables.swordSpinAnimationState(
                        Direction.fromJavaOrDown(spinBaseDirection).romIndex(), spinSector());
    }

    int spinSector() {
        return Math.min(7, Math.max(0, spinSector));
    }
}
