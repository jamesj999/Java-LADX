package linksawakening.equipment;

import static linksawakening.equipment.Sword.*;

import java.util.function.IntSupplier;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;

/**
 * Port of the sword swing + charge logic from the LADX disassembly (see
 * {@code check_items_to_use.asm}, {@code bank2.asm:UpdateLinkAnimation},
 * and {@code bank0.asm:ApplyLinkMotionState}).
 *
 * <p>The swing runs NONE -> DRAW -> SWING_START -> SWING_MIDDLE, and if the
 * button is still held at the end, settles into HOLDING where a
 * charge meter builds up (matches {@code wSwordCharge} /
 * {@code MAX_SWORD_CHARGE = $28}). At max charge the blade flashes between
 * its normal palette and a brighter "charged" palette every 4 frames,
 * mirroring the attribute-bit-4 toggle gated by bit 2 of the frame counter
 * in {@code bank0.asm:17BF-17C5} (and the GBC palette-4 branch in
 * {@code func_020_4AB3}).
 *
 * <p>The blade position, per-frame orientation, and sprite-tile selection
 * all come from ROM tables loaded into {@link RomTables}: the same
 * {@code LinkDirectionToSwordDirection}, {@code LinkDirectionTo_wC13A/139/13B},
 * and {@code Data_020_4A93/4AA3} the original uses.
 */

/** SwordAttackMotion behavior on the shared Sword state. */
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
        int value = romTables.swordDirectionAnimState(toRomDirection(effectiveDirection), lookupState);
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
