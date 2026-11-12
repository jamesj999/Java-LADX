package linksawakening.equipment;

import static linksawakening.equipment.Sword.*;

import java.util.function.IntSupplier;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;

/** Sword attack, charge and presentation state used by ROM {@code CheckItemsToUse}. */
abstract class SwordChargeMotion extends SwordAttackMotion {
    protected SwordChargeMotion(RomTables romTables, SwordSpriteSheet spriteSheet) {
        super(romTables, spriteSheet);
    }

    protected SwordChargeMotion(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier);
    }

    protected SwordChargeMotion(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette);
    }

    protected SwordChargeMotion(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette,
            BeamTarget beamTarget) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette, beamTarget);
    }

    public void submitOam(linksawakening.render.OamSink sink, int x, int y, int direction, int offsetX, int offsetY) {
        if (state == STATE_NONE && !spinAttackActive()) return;
        renderer.submitOam(
                sink,
                x,
                y,
                effectiveDirection(direction),
                effectiveBladeRenderState(),
                chargedFlashActive() ? swordPalette.charged() : swordPalette.normal(),
                offsetX,
                offsetY);
    }

    public int attackDirection(int linkDirection) {
        lastKnownDirection = linkDirection;
        return effectiveDirection(linkDirection);
    }

    public int staticCollisionMapIndex(int linkDirection) {
        lastKnownDirection = linkDirection;
        if (!spinAttackActive()) {
            return Direction.fromJavaOrDown(linkDirection).romIndex();
        }
        int effectiveDirection = effectiveDirection(linkDirection);
        int romDirection = Direction.fromJavaOrDown(effectiveDirection).romIndex();
        int swordDirection = swordTables.swordDirectionFor(romDirection, spinAnimationState());
        return 4 + (swordDirection & 0x07);
    }

    public boolean spinAttackActive() {
        return spinFramesRemaining > 0;
    }

    /** Starts the ROM's forced sword-spin state (wIsUsingSpinAttack=$20). */
    public void startSwordAcquisitionSpin(int baseDirection) {
        if (baseDirection < Direction.DOWN.javaIndex() || baseDirection > Direction.RIGHT.javaIndex()) {
            throw new IllegalArgumentException("Invalid Link direction: " + baseDirection);
        }
        spinBaseDirection = baseDirection;
        spinFramesRemaining = SPIN_ATTACK_FRAMES;
        spinFrameCounter = 1;
        spinSector = 7;
        spinAttackQueued = false;
        queuedSpinDelayFrames = 0;
        state = STATE_NONE;
        charge = 0;
    }

    /** The room-load WRAM clear also discards any pre-death sword animation. */
    public void resetAfterGameOver() {
        resetSpinAttack();
        state = STATE_NONE;
        timer = 0;
        animationMotionBlocked = false;
        motionBlockedBeforeAnimation = false;
        facingLockCountdown = 0;
        facingLockedBeforeAnimation = false;
        suppressesJumpAnimation = false;
    }

    public void resetSpinAttack() {
        boolean wasActive = spinAttackActive();
        spinAttackQueued = false;
        queuedSpinDelayFrames = 0;
        charge = 0;
        spinFramesRemaining = 0;
        spinFrameCounter = 0;
        spinSector = 0;
        if (wasActive) {
            state = STATE_NONE;
        }
    }

    /**
     * Computes the enemy collision rectangle populated by UpdateSwordAnimation
     * in bank2.asm. The rectangle is independent from the visual blade OAM
     * bounds and is therefore kept as a separate ROM-table lookup.
     */
    public Sword.CollisionBox enemyCollisionBox(int linkEntityX, int linkEntityY, int linkDirection) {
        return enemyCollisionBox(linkEntityX, linkEntityY, linkDirection, false);
    }

    /** Bank 02:42F4 keeps the held blade collision active while running with Pegasus Boots. */
    public Sword.CollisionBox enemyCollisionBox(
            int linkEntityX, int linkEntityY, int linkDirection, boolean runningWithPegasusBoots) {
        lastKnownDirection = linkDirection;
        if ((!staticCollisionActive() && !(runningWithPegasusBoots && state == STATE_HOLDING)) || swordTables == null) {
            return Sword.CollisionBox.inactive();
        }
        if (spinAttackActive()) {
            return new Sword.CollisionBox(true, linkEntityX + 0x08, 0x18, linkEntityY + 0x08, 0x18);
        }

        int romDirection = Direction.fromJavaOrDown(linkDirection).romIndex();
        if (!swordTables.swordEnemyCollisionEnabled(romDirection, state)) {
            return Sword.CollisionBox.inactive();
        }
        return new Sword.CollisionBox(
                true,
                linkEntityX + swordTables.swordCollisionXOriginOffset(romDirection, state),
                swordTables.swordCollisionWidth(romDirection, state),
                linkEntityY + swordTables.swordCollisionYOriginOffset(romDirection, state),
                swordTables.swordCollisionHeight(romDirection, state));
    }

    // Flash is driven by bit 2 of the global frame counter during HOLDING at
    // max charge (bank0.asm:17BF computes `(hFrameCounter << 2) & $10`).
    protected boolean chargedFlashActive() {
        return state == STATE_HOLDING && charge >= MAX_CHARGE && (frameCounter & 0x04) != 0;
    }

    protected int effectiveLinkAnimationState() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        // HOLDING maps to HIDDEN (0xFF) in the ROM for hLinkAnimationState,
        // meaning "keep the previous frame". Since the previous frame was
        // SWING_END's sword-extended pose, substitute that lookup directly.
        return state == STATE_HOLDING ? STATE_SWING_END : state;
    }

    protected int effectiveBladeRenderState() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        return state;
    }

    protected int effectiveDirection(int direction) {
        if (!spinAttackActive()) {
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

    protected int spinAnimationState() {
        int baseDirection = Direction.fromJavaOrDown(spinBaseDirection).romIndex();
        return swordTables == null
                ? SPIN_ANIMATION_STATE_BY_BASE_DIRECTION[baseDirection][spinSector()]
                : swordTables.swordSpinAnimationState(
                        Direction.fromJavaOrDown(spinBaseDirection).romIndex(), spinSector());
    }

    protected int spinSector() {
        return Math.min(7, Math.max(0, spinSector));
    }

    protected void advanceSpinAttackAnimation() {
        spinFramesRemaining--;
        if (spinFramesRemaining <= 0) {
            endSpinAttack();
            return;
        }

        spinSector = spinFramesRemaining >> 2;
        state = spinAnimationState();

        if ((spinFrameCounter & 0x03) == 0) {
            spinFramesRemaining--;
            if (spinFramesRemaining <= 0) {
                endSpinAttack();
                return;
            }
        }
        spinFrameCounter++;
    }

    protected void endSpinAttack() {
        spinFramesRemaining = 0;
        spinFrameCounter = 0;
        spinSector = 0;
        state = STATE_NONE;
    }
}
