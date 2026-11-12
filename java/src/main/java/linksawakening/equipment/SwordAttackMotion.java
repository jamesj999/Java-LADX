package linksawakening.equipment;

import static linksawakening.equipment.Sword.*;

import java.util.function.IntSupplier;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;

/** Sword attack, charge and presentation state used by ROM {@code CheckItemsToUse}. */
abstract class SwordAttackMotion extends SwordState {
    protected SwordAttackMotion(RomTables romTables, SwordSpriteSheet spriteSheet) {
        super(romTables, spriteSheet);
    }

    protected SwordAttackMotion(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier);
    }

    protected SwordAttackMotion(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette);
    }

    protected SwordAttackMotion(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette,
            BeamTarget beamTarget) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette, beamTarget);
    }

    public int state() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        return state;
    }

    public int charge() {
        return charge;
    }

    /**
     * Mirrors CheckStaticSwordCollision: HOLDING does not cut static objects,
     * while normal swings and active spin frames do.
     */
    public boolean staticCollisionActive() {
        return spinAttackActive() || (state != STATE_NONE && state != STATE_HOLDING);
    }

    @Override
    public void onPress() {
        if (state != STATE_NONE) {
            return;
        }
        state = STATE_DRAW;
        timer = DRAW_FRAMES;
        charge = 0;
        soundSink.play(swordSwingSound(randomByteSupplier.getAsInt()));
        beamTarget.fireSwordBeam();
    }

    @Override
    public void onRelease() {
        if (state == STATE_HOLDING) {
            if (charge >= MAX_CHARGE) {
                spinBaseDirection = lastKnownDirection;
                spinAttackQueued = true;
                queuedSpinDelayFrames = 1;
                soundSink.play(GameplaySoundEvent.SPIN_ATTACK);
            } else {
                state = STATE_NONE;
            }
            charge = 0;
        }
    }

    /** CheckItemsToUse cannot renew a held sword while a Gel is attached. */
    public void releaseHoldWithoutSpin() {
        if (state == STATE_HOLDING) {
            state = STATE_NONE;
            charge = 0;
        }
    }

    @Override
    public void tick(boolean buttonHeld) {
        tick(buttonHeld, (frameCounter + 1) & 0xFF);
    }

    @Override
    public void tick(boolean buttonHeld, int frameCounter) {
        this.frameCounter = frameCounter & 0xFF;
        // XY movement ($02:43C8) consumes the previous animation's flag.
        // UpdateLinkAnimation sets it before advancing even the final swing state.
        suppressesJumpAnimation = state != STATE_NONE;
        facingLockedBeforeAnimation = facingLockCountdown != 0;
        if (facingLockCountdown > 0) facingLockCountdown--;
        if (state != STATE_NONE) facingLockCountdown = 4; // wC16E, $02:47F3
        motionBlockedBeforeAnimation = animationMotionBlocked;
        animationMotionBlocked = blocksMotion();
        if (spinAttackQueued) {
            if (queuedSpinDelayFrames > 0) {
                queuedSpinDelayFrames--;
                return;
            }
            spinAttackQueued = false;
            spinFramesRemaining = SPIN_ATTACK_FRAMES;
            // The ROM also decrements on hFrameCounter % 4 == 0. Without a
            // shared frame counter here, start on the 25-frame visible cadence.
            spinFrameCounter = 1;
            advanceSpinAttackAnimation();
            return;
        }
        if (spinAttackActive()) {
            advanceSpinAttackAnimation();
            return;
        }
        switch (state) {
            case STATE_NONE:
                return;
            case STATE_DRAW:
                if (--timer <= 0) {
                    state = STATE_SWING_START;
                    timer = SWING_START_FRAMES;
                }
                return;
            case STATE_SWING_START:
                if (--timer <= 0) {
                    state = STATE_SWING_MIDDLE;
                    timer = SWING_MIDDLE_FRAMES;
                }
                return;
            case STATE_SWING_MIDDLE:
                if (--timer <= 0) {
                    state = buttonHeld ? STATE_HOLDING : STATE_NONE;
                    if (state == STATE_NONE) {
                        charge = 0;
                    }
                }
                return;
            case STATE_SWING_END:
                state = buttonHeld ? STATE_HOLDING : STATE_NONE;
                if (state == STATE_NONE) {
                    charge = 0;
                }
                return;
            case STATE_HOLDING:
                if (charge < MAX_CHARGE) {
                    charge++;
                    if (charge == MAX_CHARGE) {
                        soundSink.play(GameplaySoundEvent.SWORD_FULLY_CHARGED);
                    }
                }
                return;
            default:
                state = STATE_NONE;
        }
    }

    @Override
    public boolean blocksMotion() {
        return spinAttackActive() || (state != STATE_NONE && state != STATE_HOLDING);
    }

    /** $02:47AD checks the sword state before advancing its animation. */
    public boolean suppressesJumpAnimation() {
        return suppressesJumpAnimation;
    }

    public boolean locksTopDownFacing() {
        return spinAttackQueued || spinAttackActive() || facingLockedBeforeAnimation;
    }

    /** Movement phase preceding this tick's UpdateLinkAnimation. */
    public boolean blocksTopDownMotion() {
        return spinAttackActive() || motionBlockedBeforeAnimation;
    }

    @Override
    public boolean locksFacing() {
        return spinAttackQueued || spinAttackActive() || state != STATE_NONE;
    }

    @Override
    public int overrideAnimationState(int direction, int walkFrame) {
        lastKnownDirection = direction;
        if (state == STATE_NONE && !spinAttackActive()) {
            return -1;
        }
        int effectiveDirection = effectiveDirection(direction);
        int lookupState = effectiveLinkAnimationState();
        int value = swordTables.swordDirectionAnimState(toRomDirection(effectiveDirection), lookupState);
        if (value == 0xFF) {
            return -1;
        }
        return value;
    }

    @Override
    public void render(byte[] buffer, int x, int y, int direction, int offsetX, int offsetY) {
        lastKnownDirection = direction;
        if (state == STATE_NONE && !spinAttackActive()) return;
        renderer.render(
                buffer,
                x,
                y,
                effectiveDirection(direction),
                effectiveBladeRenderState(),
                chargedFlashActive() ? swordPalette.charged() : swordPalette.normal(),
                offsetX,
                offsetY);
    }
}
