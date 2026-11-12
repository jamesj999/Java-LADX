package linksawakening.equipment;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;
import linksawakening.rom.SwordTables;
import linksawakening.state.Direction;
import linksawakening.world.entity.RomRandomByteSource;

/** Swing, charge and movement gates from CheckItemsToUse and UpdateLinkAnimation. */
public final class Sword implements EquipmentItem {
    private final SwordSpin spin;

    private static final int DRAW_FRAMES = 4;
    private static final int SWING_START_FRAMES = 3;
    private static final int SWING_MIDDLE_FRAMES = 8;

    private final SwordTables swordTables;
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
    private int lastKnownDirection = Direction.DOWN.javaIndex();

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
        this.swordTables = romTables == null ? null : romTables.swordTables();
        this.renderer = new SwordRenderer(romTables, spriteSheet);
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.randomByteSupplier = Objects.requireNonNull(randomByteSupplier, "randomByteSupplier");
        this.swordPalette = Objects.requireNonNull(swordPalette, "swordPalette");
        this.beamTarget = Objects.requireNonNull(beamTarget, "beamTarget");

        spin = new SwordSpin(swordTables);
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

    public int state() {
        if (spinAttackActive()) {
            return spin.animationState();
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
                spin.queue(lastKnownDirection);
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
        if (spin.queued() || spin.active()) {
            state = spin.advance(state);
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
        return spin.queued() || spinAttackActive() || facingLockedBeforeAnimation;
    }

    /** Movement phase preceding this tick's UpdateLinkAnimation. */
    public boolean blocksTopDownMotion() {
        return spinAttackActive() || motionBlockedBeforeAnimation;
    }

    @Override
    public boolean locksFacing() {
        return spin.queued() || spinAttackActive() || state != STATE_NONE;
    }

    @Override
    public int overrideAnimationState(int direction, int walkFrame) {
        lastKnownDirection = direction;
        if (state == STATE_NONE && !spinAttackActive()) {
            return -1;
        }
        int effectiveDirection = spin.effectiveDirection(direction);
        int lookupState = effectiveLinkAnimationState();
        int value = swordTables.swordDirectionAnimState(
                Direction.fromJavaOrDown(effectiveDirection).romIndex(), lookupState);
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
                spin.effectiveDirection(direction),
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
                spin.effectiveDirection(direction),
                effectiveBladeRenderState(),
                chargedFlashActive() ? swordPalette.charged() : swordPalette.normal(),
                offsetX,
                offsetY);
    }

    public int attackDirection(int linkDirection) {
        lastKnownDirection = linkDirection;
        return spin.effectiveDirection(linkDirection);
    }

    public int staticCollisionMapIndex(int linkDirection) {
        lastKnownDirection = linkDirection;
        if (!spinAttackActive()) {
            return Direction.fromJavaOrDown(linkDirection).romIndex();
        }
        int effectiveDirection = spin.effectiveDirection(linkDirection);
        int romDirection = Direction.fromJavaOrDown(effectiveDirection).romIndex();
        int swordDirection = swordTables.swordDirectionFor(romDirection, spin.animationState());
        return 4 + (swordDirection & 0x07);
    }

    public boolean spinAttackActive() {
        return spin.active();
    }
    /** Starts the ROM forced sword-spin state (wIsUsingSpinAttack=$20). */
    public void startSwordAcquisitionSpin(int baseDirection) {
        spin.startAcquisition(baseDirection);
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
        boolean wasActive = spin.active();
        spin.clearQueue();
        charge = 0;
        spin.clearAnimation();
        if (wasActive) state = STATE_NONE;
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
    private boolean chargedFlashActive() {
        return state == STATE_HOLDING && charge >= MAX_CHARGE && (frameCounter & 0x04) != 0;
    }

    private int effectiveLinkAnimationState() {
        if (spinAttackActive()) {
            return spin.animationState();
        }
        // HOLDING maps to HIDDEN (0xFF) in the ROM for hLinkAnimationState,
        // meaning "keep the previous frame". Since the previous frame was
        // SWING_END's sword-extended pose, substitute that lookup directly.
        return state == STATE_HOLDING ? STATE_SWING_END : state;
    }

    private int effectiveBladeRenderState() {
        if (spinAttackActive()) {
            return spin.animationState();
        }
        return state;
    }

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
    public static final int MAX_CHARGE = 40;
}
