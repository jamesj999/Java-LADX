package linksawakening.equipment;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.world.entity.RomRandomByteSource;

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
public final class Sword implements EquipmentItem {

    /** Optional bridge to the room-owned ROM sword-beam entity. */
    public interface BeamTarget {
        boolean fireSwordBeam();
    }

    /** The ROM's wC140..wC143 rectangle for enemy collision. */
    public record CollisionBox(boolean active, int x, int width, int y, int height)
            implements EquipmentItem.CollisionBox {
        public static CollisionBox inactive() {
            return new CollisionBox(false, 0, 0, 0, 0);
        }
    }

    public static final int STATE_NONE = 0;
    public static final int STATE_DRAW = 1;
    public static final int STATE_SWING_START = 2;
    public static final int STATE_SWING_MIDDLE = 3;
    public static final int STATE_SWING_END = 4;
    public static final int STATE_HOLDING = 5;

    private static final int DRAW_FRAMES = 4;
    private static final int SWING_START_FRAMES = 3;
    private static final int SWING_MIDDLE_FRAMES = 8;
    private static final int SPIN_ATTACK_FRAMES = 0x20;
    public static final int MAX_CHARGE = 40; // MAX_SWORD_CHARGE ($28) in the ROM
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

    private final RomTables romTables;
    private final SwordRenderer renderer;
    private final GameplaySoundSink soundSink;
    private final IntSupplier randomByteSupplier;
    private final SwordPalette swordPalette;
    private final BeamTarget beamTarget;

    private int state = STATE_NONE;
    private int timer;
    private boolean animationMotionBlocked;
    private boolean motionBlockedBeforeAnimation;
    private int facingLockCountdown;
    private boolean facingLockedBeforeAnimation;
    private boolean suppressesJumpAnimation;
    private int charge;
    private int frameCounter;
    private int spinFramesRemaining;
    private int spinFrameCounter;
    private int spinSector;
    private int spinBaseDirection = Direction.DOWN.javaIndex();
    private int lastKnownDirection = Direction.DOWN.javaIndex();
    private boolean spinAttackQueued;
    private int queuedSpinDelayFrames;

    public Sword(RomTables romTables, SwordSpriteSheet spriteSheet) {
        this(
                romTables,
                spriteSheet,
                GameplaySoundSink.none(),
                new RomRandomByteSource(),
                SwordPalette.compatibility(),
                () -> false);
    }

    public Sword(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier) {
        this(romTables, spriteSheet, soundSink, randomByteSupplier, SwordPalette.compatibility());
    }

    public Sword(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette) {
        this(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette, () -> false);
    }

    public Sword(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette,
            BeamTarget beamTarget) {
        this.romTables = romTables;
        this.renderer = new SwordRenderer(romTables, spriteSheet);
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.randomByteSupplier = Objects.requireNonNull(randomByteSupplier, "randomByteSupplier");
        this.swordPalette = Objects.requireNonNull(swordPalette, "swordPalette");
        this.beamTarget = Objects.requireNonNull(beamTarget, "beamTarget");
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
            return toRomDirection(linkDirection);
        }
        int effectiveDirection = effectiveDirection(linkDirection);
        int romDirection = toRomDirection(effectiveDirection);
        int swordDirection = romTables.swordDirectionFor(romDirection, spinAnimationState());
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

    /**
     * Mirrors the ROM's ResetSpinAttack helper (bank0.asm:$0CAF).  Entity
     * collisions use it to cancel a charged or active spin before Link's next
     * motion update.
     */
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
    public CollisionBox enemyCollisionBox(int linkEntityX, int linkEntityY, int linkDirection) {
        return enemyCollisionBox(linkEntityX, linkEntityY, linkDirection, false);
    }

    /** Bank 02:42F4 keeps the held blade collision active while running with Pegasus Boots. */
    public CollisionBox enemyCollisionBox(
            int linkEntityX, int linkEntityY, int linkDirection, boolean runningWithPegasusBoots) {
        lastKnownDirection = linkDirection;
        if ((!staticCollisionActive() && !(runningWithPegasusBoots && state == STATE_HOLDING)) || romTables == null) {
            return CollisionBox.inactive();
        }
        if (spinAttackActive()) {
            return new CollisionBox(true, linkEntityX + 0x08, 0x18, linkEntityY + 0x08, 0x18);
        }

        int romDirection = toRomDirection(linkDirection);
        if (!romTables.swordEnemyCollisionEnabled(romDirection, state)) {
            return CollisionBox.inactive();
        }
        return new CollisionBox(
                true,
                linkEntityX + romTables.swordCollisionXOriginOffset(romDirection, state),
                romTables.swordCollisionWidth(romDirection, state),
                linkEntityY + romTables.swordCollisionYOriginOffset(romDirection, state),
                romTables.swordCollisionHeight(romDirection, state));
    }

    // Flash is driven by bit 2 of the global frame counter during HOLDING at
    // max charge (bank0.asm:17BF computes `(hFrameCounter << 2) & $10`).
    private boolean chargedFlashActive() {
        return state == STATE_HOLDING && charge >= MAX_CHARGE && (frameCounter & 0x04) != 0;
    }

    private int effectiveLinkAnimationState() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        // HOLDING maps to HIDDEN (0xFF) in the ROM for hLinkAnimationState,
        // meaning "keep the previous frame". Since the previous frame was
        // SWING_END's sword-extended pose, substitute that lookup directly.
        return state == STATE_HOLDING ? STATE_SWING_END : state;
    }

    private int effectiveBladeRenderState() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        return state;
    }

    private int effectiveDirection(int direction) {
        if (!spinAttackActive()) {
            return direction;
        }
        int baseDirection = toSpinDirectionIndex(spinBaseDirection);
        return romTables == null
                ? SPIN_ABSOLUTE_DIRECTION_BY_BASE_DIRECTION[baseDirection][spinSector()]
                : fromRomDirection(
                        romTables.swordSpinAbsoluteDirection(toRomDirection(spinBaseDirection), spinSector()));
    }

    private int spinAnimationState() {
        int baseDirection = toSpinDirectionIndex(spinBaseDirection);
        return romTables == null
                ? SPIN_ANIMATION_STATE_BY_BASE_DIRECTION[baseDirection][spinSector()]
                : romTables.swordSpinAnimationState(toRomDirection(spinBaseDirection), spinSector());
    }

    private int spinSector() {
        return Math.min(7, Math.max(0, spinSector));
    }

    private void advanceSpinAttackAnimation() {
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

    private void endSpinAttack() {
        spinFramesRemaining = 0;
        spinFrameCounter = 0;
        spinSector = 0;
        state = STATE_NONE;
    }

    private static int toSpinDirectionIndex(int javaDirection) {
        return Direction.fromJavaOrDown(javaDirection).romIndex();
    }

    private static int toRomDirection(int javaDirection) {
        return Direction.fromJavaOrDown(javaDirection).romIndex();
    }

    private static int fromRomDirection(int romDirection) {
        return Direction.fromRom(romDirection & 0x03).javaIndex();
    }

    private static GameplaySoundEvent swordSwingSound(int randomByte) {
        switch (randomByte & 0x03) {
            case 0:
                return GameplaySoundEvent.SWORD_SWING_A;
            case 1:
                return GameplaySoundEvent.SWORD_SWING_B;
            case 2:
                return GameplaySoundEvent.SWORD_SWING_C;
            default:
                return GameplaySoundEvent.SWORD_SWING_D;
        }
    }
}
