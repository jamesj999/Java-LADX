package linksawakening.equipment;

import static linksawakening.equipment.Sword.*;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;
import linksawakening.rom.SwordTables;
import linksawakening.state.Direction;
import linksawakening.world.entity.RomRandomByteSource;

/** Sword attack, charge and presentation state used by ROM {@code CheckItemsToUse}. */
abstract class SwordState implements EquipmentItem {

    protected static final int DRAW_FRAMES = 4;
    protected static final int SWING_START_FRAMES = 3;
    protected static final int SWING_MIDDLE_FRAMES = 8;
    protected static final int SPIN_ATTACK_FRAMES = 0x20; // MAX_SWORD_CHARGE ($28) in the ROM
    // bank2.asm:46C9-46E8. Indexed as (base hLinkDirection << 3) + spin sector.
    protected static final int[][] SPIN_ANIMATION_STATE_BY_BASE_DIRECTION = {
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
    protected static final int[][] SPIN_ABSOLUTE_DIRECTION_BY_BASE_DIRECTION = {
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

    protected final SwordTables swordTables;
    protected final SwordRenderer renderer;
    protected final GameplaySoundSink soundSink;
    protected final IntSupplier randomByteSupplier;
    protected final SwordPalette swordPalette;
    protected final BeamTarget beamTarget;

    protected int state = STATE_NONE;
    protected int timer;
    protected boolean animationMotionBlocked;
    protected boolean motionBlockedBeforeAnimation;
    protected int facingLockCountdown;
    protected boolean facingLockedBeforeAnimation;
    protected boolean suppressesJumpAnimation;
    protected int charge;
    protected int frameCounter;
    protected int spinFramesRemaining;
    protected int spinFrameCounter;
    protected int spinSector;
    protected int spinBaseDirection = Direction.DOWN.javaIndex();
    protected int lastKnownDirection = Direction.DOWN.javaIndex();
    protected boolean spinAttackQueued;
    protected int queuedSpinDelayFrames;

    public SwordState(RomTables romTables, SwordSpriteSheet spriteSheet) {
        this(
                romTables,
                spriteSheet,
                GameplaySoundSink.none(),
                new RomRandomByteSource(),
                SwordPalette.compatibility(),
                () -> false);
    }

    public SwordState(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier) {
        this(romTables, spriteSheet, soundSink, randomByteSupplier, SwordPalette.compatibility());
    }

    public SwordState(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette) {
        this(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette, () -> false);
    }

    public SwordState(
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
    }

    protected static int toSpinDirectionIndex(int javaDirection) {
        return Direction.fromJavaOrDown(javaDirection).romIndex();
    }

    protected static int toRomDirection(int javaDirection) {
        return Direction.fromJavaOrDown(javaDirection).romIndex();
    }

    protected static int fromRomDirection(int romDirection) {
        return Direction.fromRom(romDirection & 0x03).javaIndex();
    }

    protected static GameplaySoundEvent swordSwingSound(int randomByte) {
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

    public abstract int state();

    public abstract int charge();

    public abstract boolean staticCollisionActive();

    public abstract void onPress();

    public abstract void onRelease();

    public abstract void releaseHoldWithoutSpin();

    public abstract void tick(boolean buttonHeld);

    public abstract void tick(boolean buttonHeld, int frameCounter);

    public abstract boolean blocksMotion();

    public abstract boolean suppressesJumpAnimation();

    public abstract boolean locksTopDownFacing();

    public abstract boolean blocksTopDownMotion();

    public abstract boolean locksFacing();

    public abstract int overrideAnimationState(int direction, int walkFrame);

    public abstract void render(byte[] buffer, int x, int y, int direction, int offsetX, int offsetY);

    public abstract void submitOam(
            linksawakening.render.OamSink sink, int x, int y, int direction, int offsetX, int offsetY);

    public abstract int attackDirection(int linkDirection);

    public abstract int staticCollisionMapIndex(int linkDirection);

    public abstract boolean spinAttackActive();

    public abstract void startSwordAcquisitionSpin(int baseDirection);

    public abstract void resetAfterGameOver();

    public abstract void resetSpinAttack();

    public abstract Sword.CollisionBox enemyCollisionBox(int linkEntityX, int linkEntityY, int linkDirection);

    public abstract Sword.CollisionBox enemyCollisionBox(
            int linkEntityX, int linkEntityY, int linkDirection, boolean runningWithPegasusBoots);

    protected abstract boolean chargedFlashActive();

    protected abstract int effectiveLinkAnimationState();

    protected abstract int effectiveBladeRenderState();

    protected abstract int effectiveDirection(int direction);

    protected abstract int spinAnimationState();

    protected abstract int spinSector();

    protected abstract void advanceSpinAttackAnimation();

    protected abstract void endSpinAttack();
}
