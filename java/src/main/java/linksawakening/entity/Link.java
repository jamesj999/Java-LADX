package linksawakening.entity;

import linksawakening.equipment.EquippedItem;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;

import java.util.Objects;

/**
 * Link's state and per-frame update, ported from the LADX disassembly's
 * {@code LinkMotionDefault} path (bank2.asm:161).
 *
 * <p>Position is stored in sub-pixels (1 pixel = 16 sub-pixels), matching the
 * scale of the original {@code HorizontalIncrementForLinkPosition} /
 * {@code VerticalIncrementForLinkPosition} tables.
 *
 * <p>The 16x16 AABB used for both rendering and collision has its origin at
 * Link's top-left.
 */
public final class Link implements RocsFeather.JumpTarget {

    public static final int DIRECTION_DOWN = 0;
    public static final int DIRECTION_UP = 1;
    public static final int DIRECTION_LEFT = 2;
    public static final int DIRECTION_RIGHT = 3;

    public static final int SPRITE_SIZE = 16;
    public static final int SUB_PIXEL_SHIFT = 4;
    public static final int GROUND_STATUS_NORMAL = 0x00;
    public static final int GROUND_STATUS_PIT = 0x07;
    public static final int LINK_MOTION_DEFAULT = 0x00;
    public static final int LINK_MOTION_SWIMMING = 0x01;

    // Bit order matches JoypadToLinkDirection (bank2.asm:1183): entry 1 = right,
    // 2 = left, 4 = up, 8 = down.
    private static final int JOY_RIGHT = 1 << 0;
    private static final int JOY_LEFT = 1 << 1;
    private static final int JOY_UP = 1 << 2;
    private static final int JOY_DOWN = 1 << 3;

    // JoypadToLinkDirection table from bank2.asm:1183 — any two-axis combination
    // keeps the previous facing. Encoded as one of DIRECTION_* or -1 (= KEEP).
    private static final int[] JOYPAD_TO_DIRECTION = {
        -1,                 //  0000 none       -> keep
        DIRECTION_RIGHT,    //  0001 right
        DIRECTION_LEFT,     //  0010 left
        -1,                 //  0011 right+left -> keep
        DIRECTION_UP,       //  0100 up
        -1,                 //  0101 up+right   -> keep
        -1,                 //  0110 up+left    -> keep
        -1,                 //  0111 keep
        DIRECTION_DOWN,     //  1000 down
        -1,                 //  1001 down+right -> keep
        -1,                 //  1010 down+left  -> keep
        -1,                 //  1011 keep
        -1,                 //  1100 down+up    -> keep
        -1,                 //  1101 keep
        -1,                 //  1110 keep
        -1,                 //  1111 keep
    };

    // hLinkAnimationState values from gameplay.asm. Index = direction, inner
    // index = walking-frame bit (0 = standing, 1 = walking alt). These match
    // LinkAnimationsList_WalkingNoShield (bank2.asm:1208).
    private static final int[][] ANIMATION_STATE = {
        { 0x00, 0x01 },  // DOWN:  standing, walking
        { 0x04, 0x05 },  // UP
        { 0x06, 0x07 },  // LEFT
        { 0x0A, 0x0B },  // RIGHT
    };

    // LinkAnimationsList_WalkUsingDefaultShield and the corresponding mirror
    // list in bank2.asm:1214-1240. Java direction order is DOWN, UP, LEFT,
    // RIGHT, matching the ordinary walking table above.
    private static final int[][] SHIELD_USE_ANIMATION_STATE = {
        { 0x24, 0x25 },  // DOWN
        { 0x30, 0x31 },  // UP
        { 0x28, 0x29 },  // LEFT
        { 0x2A, 0x2B },  // RIGHT
    };
    private static final int[][] MIRROR_SHIELD_USE_ANIMATION_STATE = {
        { 0x26, 0x27 },  // DOWN
        { 0x32, 0x33 },  // UP
        { 0x28, 0x29 },  // LEFT
        { 0x2E, 0x2F },  // RIGHT
    };

    // LinkAnimationsList_LiftingObject (bank2.asm:1249), indexed in the
    // same Java direction order as the walking table.
    private static final int[][] LIFTING_ANIMATION_STATE = {
        { 0x44, 0x45 },  // DOWN
        { 0x42, 0x43 },  // UP
        { 0x40, 0x41 },  // LEFT
        { 0x3E, 0x3F },  // RIGHT
    };

    // Data_002_4948 (bank2.asm:1257), in Java direction order. The second
    // row is selected while hLinkPhysicsModifier is non-zero (diving).
    private static final int[][] SWIMMING_ANIMATION_STATE = {
        { 0x4C, 0x4D },  // DOWN
        { 0x4A, 0x4B },  // UP
        { 0x48, 0x49 },  // LEFT
        { 0x46, 0x47 },  // RIGHT
    };
    private static final int[] DIVING_ANIMATION_STATE = { 0x4E, 0x4F };

    private static final int WALK_FRAME_TICKS = 8;
    private static final int JUMP_FRAME_TICKS = 8;
    private static final int FALL_FRAME_TICKS = 16;
    private static final int FALL_DURATION_FRAMES = 96;
    private static final int PIT_RECOVERY_INVINCIBILITY_FRAMES = 0x40;
    private static final int PIT_DAMAGE = PlayerState.HP_PER_HEART / 2;
    private static final int ATTACK_STEP_ITEM_ANY = 0x00;
    private static final int ATTACK_STEP_DURATION_MASK = 0x7F;
    private static final int ROM_ITEM_ATTACK_STEP_COUNTDOWN = 0x0C | ATTACK_STEP_ITEM_ANY;
    private static final int COLLISION_TYPE_UP = 0x01;
    private static final int COLLISION_TYPE_DOWN = 0x02;
    private static final int COLLISION_TYPE_LEFT = 0x04;
    private static final int COLLISION_TYPE_RIGHT = 0x08;
    private static final int PEGASUS_BOOTS_MAX_CHARGE = 0x20;
    private static final int PEGASUS_BOOTS_RUNNING_SPEED = 0x20;
    private static final int PEGASUS_BOOTS_COLLISION_COUNTDOWN = 0x02;
    private static final int PEGASUS_BOOTS_COLLISION_SHAKE = 0x20;
    private static final int PEGASUS_BOOTS_COLLISION_VELOCITY_Z = 0x18;

    private static final int[][] JUMP_ANIMATION_STATE = {
        { 0x64, 0x65, 0x66 }, // DOWN
        { 0x67, 0x68, 0x69 }, // UP
        { 0x5E, 0x5F, 0x60 }, // LEFT
        { 0x61, 0x62, 0x63 }, // RIGHT
    };
    private static final int[] FALL_ANIMATION_STATE = { 0x55, 0x56, 0x57, 0x57 };

    private final InputState inputState;
    private final InputConfig inputConfig;
    private final RomTables romTables;
    private final OverworldCollision collision;
    private final LinkSpriteSheet spriteSheet;
    private final PlayerState playerState;
    private final ItemRegistry itemRegistry;
    private final LinkTunicPalette tunicPalette;

    // Two collision check points per direction, as offsets from the
    // sprite's top-left. Ported from LinkCollisionPointsX/Y
    // (bank2.asm:6302-6319). Only the leading edge of movement is
    // checked, which is why Link can walk out of a "stuck in tree row"
    // position by pressing UP (top-edge points are in the clear row)
    // even though his feet are still overlapping a solid cell below.
    //
    // Indexed by DIRECTION_* constants below.
    private static final int[][] COLLISION_POINTS_X = {
        { 6, 9 },   // DOWN
        { 6, 9 },   // UP
        { 4, 4 },   // LEFT
        { 11, 11 }, // RIGHT
    };
    private static final int[][] COLLISION_POINTS_Y = {
        { 15, 15 }, // DOWN — bottom edge
        { 6,  6 },  // UP — top edge (not 0; matches LA's offset)
        { 9,  12 }, // LEFT — middle-left
        { 9,  12 }, // RIGHT — middle-right
    };

    private int subX;
    private int subY;
    private int direction = DIRECTION_DOWN;
    private int walkTickCounter;
    private int walkFrame;
    private boolean movingThisFrame;
    private int groundMotionCounter;
    private int collisionIgnoreFramesRemaining;
    private int romCollisionType;
    private boolean forcedSpeedPending;
    private int forcedSpeedX;
    private int forcedSpeedY;
    private int pegasusBootsChargeMeter;
    private int pegasusBootsCollisionCountdown;
    private int pegasusBootsCollisionPosX;
    private int pegasusBootsCollisionPosY;
    private int pendingPegasusScreenShakeCountdown;
    private int pendingPegasusScreenShakePhase;
    private int lastRomSpeedX;
    private int lastRomSpeedY;
    private int groundStatus = GROUND_STATUS_NORMAL;
    private int motionState = LINK_MOTION_DEFAULT;
    private boolean romInteractiveMotionBlocked;
    private int romAnimationStateOverride = -1;
    private int physicsModifier;
    private int swimmingSpeedX;
    private int swimmingSpeedY;
    private int swimmingFastCountdown;
    private int divingCountdown;
    private int frameCounter;
    private int romAttackStepAnimationCountdown;
    private boolean airborne;
    private int zSubPixels;
    private int zVelocity;
    private boolean likeLikeCaptured;
    private int likeLikeCaptureSubX;
    private int likeLikeCaptureSubY;
    private boolean fallingIntoPit;
    private int jumpAnimationCounter;
    private int jumpAnimationFrame;
    private int pitSlippingCounter;
    private int fallingFrameCounter;
    private int pitSlipTargetTopLeftX;
    private int pitSlipTargetTopLeftY;
    private int pitSlipPhysicsFlag;
    private boolean hasPitSlipTarget;
    private int carryingLiftedObjectState;
    private int carryingLiftedObjectRomDirection = 3;
    private boolean roosterCarryActive;
    private int lastSafeSubX;
    private int lastSafeSubY;
    private boolean hasLastSafePosition;
    private int romFinalSubX;
    private int romFinalSubY;
    private boolean hasRomFinalPosition;
    private int roomEntrySubX;
    private int roomEntrySubY;
    private boolean hasRoomEntryPosition;
    private final GameplaySoundSink soundSink;
    private final Tile[] composedTiles = new Tile[4];

    public Link(InputState inputState,
                InputConfig inputConfig,
                RomTables romTables,
                OverworldCollision collision,
                LinkSpriteSheet spriteSheet,
                PlayerState playerState,
                ItemRegistry itemRegistry) {
        this(inputState, inputConfig, romTables, collision, spriteSheet, playerState,
                itemRegistry, GameplaySoundSink.none(), LinkTunicPalette.greenCompatibility());
    }

    public Link(InputState inputState,
                InputConfig inputConfig,
                RomTables romTables,
                OverworldCollision collision,
                LinkSpriteSheet spriteSheet,
                PlayerState playerState,
                ItemRegistry itemRegistry,
                GameplaySoundSink soundSink) {
        this(inputState, inputConfig, romTables, collision, spriteSheet, playerState,
            itemRegistry, soundSink, LinkTunicPalette.greenCompatibility());
    }

    public Link(InputState inputState,
                InputConfig inputConfig,
                RomTables romTables,
                OverworldCollision collision,
                LinkSpriteSheet spriteSheet,
                PlayerState playerState,
                ItemRegistry itemRegistry,
                GameplaySoundSink soundSink,
                LinkTunicPalette tunicPalette) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.romTables = romTables;
        this.collision = collision;
        this.spriteSheet = spriteSheet;
        this.playerState = playerState;
        this.itemRegistry = itemRegistry;
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.tunicPalette = Objects.requireNonNull(tunicPalette, "tunicPalette");
    }

    public void setPixelPosition(int pixelX, int pixelY) {
        subX = pixelX << SUB_PIXEL_SHIFT;
        subY = pixelY << SUB_PIXEL_SHIFT;
    }

    public void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        setPixelPosition(pixelX, pixelY);
        markRoomEntryPosition();
    }

    public void markRoomEntryPosition() {
        roomEntrySubX = subX;
        roomEntrySubY = subY;
        hasRoomEntryPosition = true;
    }

    /** Returns the Java top-left X coordinate recorded for the current room entry. */
    public int roomEntryPixelX() {
        return hasRoomEntryPosition ? roomEntrySubX >> SUB_PIXEL_SHIFT : pixelX();
    }

    /** Returns the Java top-left Y coordinate recorded for the current room entry. */
    public int roomEntryPixelY() {
        return hasRoomEntryPosition ? roomEntrySubY >> SUB_PIXEL_SHIFT : pixelY();
    }

    /**
     * Returns the source {@code hLinkPositionX} value for the recorded entry.
     * The Game Boy stores Link's OAM origin, which is eight pixels to the right
     * of this host model's top-left sprite coordinate.
     */
    public int roomEntryRomPositionX() {
        return roomEntryPixelX() + 0x08;
    }

    /**
     * Returns the source {@code hLinkPositionY} value for the recorded entry.
     * The Game Boy stores the lower OAM origin, sixteen pixels below this host
     * model's top-left sprite coordinate.
     */
    public int roomEntryRomPositionY() {
        return roomEntryPixelY() + 0x10;
    }

    /** Applies a source warp/save position (OAM origin) to the host model. */
    public void setRoomEntryRomPosition(int romPositionX, int romPositionY) {
        setRoomEntryPixelPosition(romPositionX - 0x08, romPositionY - 0x10);
    }

    /**
     * Ignore collision on Link's next {@code frames} movement updates. Used
     * after a warp so that Link can walk off his entrance tile even though
     * the door/stairs object beneath him is flagged SOLID — the LADX
     * disassembly uses {@code wIgnoreLinkCollisionsCountdown} for this
     * (bank2.asm:589).
     */
    public void setCollisionIgnoreFrames(int frames) {
        collisionIgnoreFramesRemaining = Math.max(0, frames);
    }

    /**
     * Applies the next-frame hLinkSpeedX/hLinkSpeedY values written by an
     * entity handler such as the mirror-shield laser reflection path.
     *
     * <p>The ROM stores these as signed bytes in HRAM.  They are consumed by
     * the following Link motion update, after AnimateEntities has finished
     * writing them for the current frame.</p>
     */
    public void applyRomSpeed(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link response speeds must be unsigned bytes");
        }
        forcedSpeedX = (byte) speedX;
        forcedSpeedY = (byte) speedY;
        forcedSpeedPending = true;
    }

    /**
     * Applies the speed bytes written by a handler that immediately calls
     * {@code UpdateFinalLinkPosition}, bypassing input and collision probes.
     */
    public void applyRomFinalPosition(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link final-position speeds must be unsigned bytes");
        }
        subY += (byte) speedY;
        subX += (byte) speedX;
    }

    /** Captures hLinkPositionX/Y into the ROM's per-frame final-position shadow. */
    public void captureRomFinalPosition() {
        romFinalSubX = subX;
        romFinalSubY = subY;
        hasRomFinalPosition = true;
    }

    /** Copies the captured ROM final position back over an entity push. */
    public void restoreRomFinalPosition() {
        if (!hasRomFinalPosition) {
            return;
        }
        subX = romFinalSubX;
        subY = romFinalSubY;
    }

    /** Applies the ROM's next-frame hLinkInteractiveMotionBlocked=$02 write. */
    public void blockNextRomMotionFrame() {
        romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6A;
    }

    public int pixelX() {
        return subX >> SUB_PIXEL_SHIFT;
    }

    public int pixelY() {
        return subY >> SUB_PIXEL_SHIFT;
    }

    /** ROM hLinkPositionX: the first 8x16 OAM column's X origin. */
    public int romEntityX() {
        return pixelX() + 0x08;
    }

    /** ROM hLinkPositionY before the separate vertical Z offset is applied. */
    public int romEntityY() {
        return pixelY() + 0x10;
    }

    /** ROM hLinkPositionZ used by follower position history and OAM Z state. */
    public int romEntityZ() {
        return zPixels();
    }

    /** Applies Like Like's hLinkPositionX/Y and hidden animation state. */
    public void applyLikeLikeCapture(int romEntityX, int romEntityY) {
        if ((romEntityX & ~0xFF) != 0 || (romEntityY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Like Like capture coordinates must be bytes");
        }
        likeLikeCaptureSubX = ((romEntityX - 0x08) & 0xFF) << SUB_PIXEL_SHIFT;
        likeLikeCaptureSubY = ((romEntityY - 0x10) & 0xFF) << SUB_PIXEL_SHIFT;
        likeLikeCaptured = true;
        subX = likeLikeCaptureSubX;
        subY = likeLikeCaptureSubY;
        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        fallingIntoPit = false;
        groundStatus = GROUND_STATUS_NORMAL;
        motionState = LINK_MOTION_DEFAULT;
        physicsModifier = 0;
        swimmingSpeedX = 0;
        swimmingSpeedY = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        movingThisFrame = false;
        romCollisionType = 0;
        lastRomSpeedX = 0;
        lastRomSpeedY = 0;
        forcedSpeedPending = false;
    }

    /** Releases Link when the swallowed Like Like handler accepts A or B. */
    public void releaseLikeLikeCapture() {
        likeLikeCaptured = false;
    }

    public boolean isLikeLikeCaptured() {
        return likeLikeCaptured;
    }

    /**
     * ROM wLinkMotionState values needed by entity collision handlers. Normal
     * ground and airborne motion both remain interactive; falling into or
     * slipping across a pit uses the non-interactive falling state.
     */
    public int romMotionState() {
        if (groundStatus == GROUND_STATUS_PIT || fallingIntoPit) {
            return 0x06;
        }
        return motionState;
    }

    public boolean isSwimming() {
        return motionState == LINK_MOTION_SWIMMING;
    }

    public boolean isDiving() {
        return isSwimming() && physicsModifier != 0;
    }

    /** Mirrors wLinkAttackStepAnimationCountdown after a player projectile spawn. */
    public int romAttackStepAnimationCountdown() {
        return romAttackStepAnimationCountdown & 0xFF;
    }

    /** Starts the ROM's generic item attack-step animation window. */
    public void startRomItemAttackStep() {
        romAttackStepAnimationCountdown = ROM_ITEM_ATTACK_STEP_COUNTDOWN;
    }

    /** Starts SprinkleMagicPowder's two-frame-longer generic item window ($0E). */
    public void startRomMagicPowderAttackStep() {
        romAttackStepAnimationCountdown = 0x0E;
    }

    /** Starts UseMagicRod's high-bit attack-step animation window. */
    public void startRomMagicRodAttackStep() {
        romAttackStepAnimationCountdown = 0x8E;
    }

    /**
     * Mirrors the leading CheckItemsToUse gate used before an equipped item
     * spends inventory. Airborne motion remains eligible; PlaceBomb does not
     * reject it, while pit, carry, and interactive item motion do.
     */
    public boolean canUseItems() {
        if (romMotionState() != 0 || isCarryingLiftedObject()) {
            return false;
        }
        return !itemsBlockMotion();
    }

    /** Mirrors the unsigned {@code wCollisionType} byte written by Link motion. */
    public int romCollisionType() {
        return romCollisionType & 0xFF;
    }

    /** ROM hLinkSpeedX written by the current Link motion update. */
    public int romSpeedX() {
        return lastRomSpeedX & 0xFF;
    }

    /** ROM hLinkSpeedY written by the current Link motion update. */
    public int romSpeedY() {
        return lastRomSpeedY & 0xFF;
    }

    /** Mirrors wPegasusBootsChargeMeter for the live item-use path. */
    public int pegasusBootsChargeMeter() {
        return pegasusBootsChargeMeter & 0xFF;
    }

    /** Mirrors wPegasusBootsCollisionCountdown. */
    public int pegasusBootsCollisionCountdown() {
        return pegasusBootsCollisionCountdown & 0xFF;
    }

    /** Mirrors wPegasusBootsCollisionPosX. */
    public int pegasusBootsCollisionPosX() {
        return pegasusBootsCollisionPosX & 0xFF;
    }

    /** Mirrors wPegasusBootsCollisionPosY. */
    public int pegasusBootsCollisionPosY() {
        return pegasusBootsCollisionPosY & 0xFF;
    }

    /** A screen-shake write emitted by the ROM's Pegasus collision handler. */
    public record ScreenShakeRequest(int countdown, int phase) {
    }

    /** Returns and clears the one-shot shake request written this frame. */
    public ScreenShakeRequest consumePegasusScreenShakeRequest() {
        ScreenShakeRequest request = new ScreenShakeRequest(
            pendingPegasusScreenShakeCountdown, pendingPegasusScreenShakePhase);
        pendingPegasusScreenShakeCountdown = 0;
        pendingPegasusScreenShakePhase = 0;
        return request.countdown() == 0 ? null : request;
    }

    /** Mirrors ResetPegasusBoots for handlers that interrupt a dash. */
    public void resetPegasusBoots() {
        pegasusBootsChargeMeter = 0;
        if (playerState != null) {
            playerState.setRunningWithPegasusBoots(false);
        }
    }

    /**
     * Mirrors wIsUsingShield: merely owning the shield is insufficient; the
     * button bound to the slot containing it must be held this frame.
     */
    public boolean isUsingShield() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        boolean usingA = playerState.itemA() == PlayerState.INVENTORY_SHIELD
            && inputState.isDown(inputConfig.aKey());
        boolean usingB = playerState.itemB() == PlayerState.INVENTORY_SHIELD
            && inputState.isDown(inputConfig.bKey());
        return usingA || usingB;
    }

    /** ROM wC145: the Y origin used by sword collision while airborne. */
    public int romSwordCollisionY() {
        return romEntityY() - zPixels();
    }

    public int direction() {
        return direction;
    }

    /**
     * Mirrors {@code func_157C}: a single currently-held D-pad direction
     * immediately becomes Link's facing before an arrow is spawned; neutral
     * and diagonal masks preserve the existing facing.
     */
    public int applyRomItemDirectionFromInput() {
        int newDirection = JOYPAD_TO_DIRECTION[buildJoypadMask()];
        if (newDirection != -1) {
            direction = newDirection;
        }
        return romDirectionForJavaDirection(direction);
    }

    /**
     * Applies wIsCarryingLiftedObject and the direction written by
     * EntityLiftedHandler. The direction argument uses the ROM order:
     * right, left, up, down.
     */
    public void setCarryingLiftedObjectState(int carryState, int romDirection) {
        if ((carryState & ~0xFF) != 0) {
            throw new IllegalArgumentException("Carry state must be an unsigned byte: "
                + carryState);
        }
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        }
        carryingLiftedObjectState = carryState;
        carryingLiftedObjectRomDirection = romDirection;
        if (carryState != 0) {
            direction = javaDirectionForRomDirection(romDirection);
        }
    }

    /** Applies the Link HRAM writes emitted by RoosterEntityHandler. */
    public void applyRoosterFlightState(int positionZ, int velocityZ,
                                        int speedX, int speedY, int romDirection) {
        if ((positionZ & ~0xFF) != 0 || (velocityZ & ~0xFF) != 0) {
            throw new IllegalArgumentException("Rooster Link Z values must be unsigned bytes");
        }
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("Rooster ROM direction must be between 0 and 3");
        }
        motionState = LINK_MOTION_DEFAULT;
        airborne = true;
        zSubPixels = (positionZ & 0xFF) << SUB_PIXEL_SHIFT;
        zVelocity = velocityZ & 0xFF;
        groundStatus = GROUND_STATUS_NORMAL;
        fallingIntoPit = false;
        physicsModifier = 0;
        roosterCarryActive = true;
        direction = javaDirectionForRomDirection(romDirection);
        applyRomSpeed(speedX, speedY);
    }

    /** Ends Rooster's custom airborne carry state when it is thrown or cleared. */
    public void clearRoosterCarryState() {
        roosterCarryActive = false;
        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        groundStatus = GROUND_STATUS_NORMAL;
        fallingIntoPit = false;
        motionState = LINK_MOTION_DEFAULT;
        physicsModifier = 0;
        forcedSpeedPending = false;
    }

    public boolean isRoosterCarryActive() {
        return roosterCarryActive;
    }

    public int carryingLiftedObjectState() {
        return carryingLiftedObjectState;
    }

    public boolean isCarryingLiftedObject() {
        return carryingLiftedObjectState != 0 || roosterCarryActive;
    }

    public void setDirection(int newDirection) {
        if (newDirection < DIRECTION_DOWN || newDirection > DIRECTION_RIGHT) {
            throw new IllegalArgumentException("Invalid Link direction: " + newDirection);
        }
        direction = newDirection;
        walkTickCounter = 0;
        walkFrame = 0;
    }

    public boolean isAirborne() {
        return airborne;
    }

    public int zVelocity() {
        return zVelocity & 0xFF;
    }

    public boolean isFallingIntoPit() {
        return fallingIntoPit;
    }

    @Override
    public void useRocsFeather() {
        if (airborne || groundStatus == GROUND_STATUS_PIT || fallingIntoPit || isSwimming()) {
            return;
        }
        airborne = true;
        zSubPixels = 0;
        zVelocity = 0x20;
        jumpAnimationCounter = 0;
        jumpAnimationFrame = 0;
        soundSink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);
    }

    /** Applies the top-down Link bounce written by ApplyLinkCollisionWithEnemy. */
    public void bounceFromGoomba() {
        airborne = true;
        zVelocity = 0x10;
        fallingIntoPit = false;
        groundStatus = GROUND_STATUS_NORMAL;
        leaveSwimming();
    }

    /** Advance the walking-cycle timer without processing input or collision. */
    public void tickAnimation() {
        walkTickCounter++;
        if (walkTickCounter >= WALK_FRAME_TICKS) {
            walkTickCounter = 0;
            walkFrame ^= 1;
        }
    }

    public void update() {
        frameCounter = (frameCounter + 1) & 0xFF;
        if (pegasusBootsCollisionCountdown > 0) {
            pegasusBootsCollisionCountdown--;
        }
        tickRomAttackStepAnimationCountdown();
        romCollisionType = 0;
        lastRomSpeedX = 0;
        lastRomSpeedY = 0;
        if (playerState != null) {
            playerState.tickInvincibility();
        }

        boolean interactiveMotionBlocked = romInteractiveMotionBlocked;
        romInteractiveMotionBlocked = false;
        if (!interactiveMotionBlocked) {
            romAnimationStateOverride = -1;
        }
        if (interactiveMotionBlocked) {
            movingThisFrame = false;
            walkTickCounter = 0;
            walkFrame = 0;
            return;
        }

        if (likeLikeCaptured) {
            subX = likeLikeCaptureSubX;
            subY = likeLikeCaptureSubY;
            airborne = false;
            zSubPixels = 0;
            zVelocity = 0;
            movingThisFrame = false;
            return;
        }

        if (fallingIntoPit) {
            tickPitFall();
            return;
        }

        if (motionState == LINK_MOTION_SWIMMING) {
            updateSwimming();
            return;
        }

        int mask = buildJoypadMask();
        int newDirection = JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1 && !itemsLockFacing() && !isLiftTransitionBlockingMotion()) {
            direction = newDirection;
        }
        updatePegasusBootsUse();
        if (!airborne && groundStatus != GROUND_STATUS_PIT) {
            refreshGroundStatus();
            updateLastSafePositionIfPossible();
        }

        if (groundStatus == GROUND_STATUS_PIT) {
            movingThisFrame = false;
            walkTickCounter = 0;
            walkFrame = 0;
            slipTowardPitCenter();
            return;
        }

        // Equipped items (sword swing, etc.) can block motion for a window of
        // frames. Mirrors the disassembly's hLinkInteractiveMotionBlocked flag
        // set by UpdateLinkAnimation while wSwordAnimationState != NONE. The
        // ROM still refreshes hLinkDirection from held single-axis input before
        // this early-out path, so direction update must happen above.
        if (itemsBlockMotion()) {
            movingThisFrame = false;
            walkTickCounter = 0;
            walkFrame = 0;
            if (collisionIgnoreFramesRemaining > 0) {
                collisionIgnoreFramesRemaining--;
            }
            tickJump();
            return;
        }

        int speedX;
        int speedY;
        if (playerState != null && playerState.runningWithPegasusBoots()) {
            speedX = pegasusRunningSpeedX();
            speedY = pegasusRunningSpeedY();
        } else if (forcedSpeedPending) {
            speedX = forcedSpeedX;
            speedY = forcedSpeedY;
            forcedSpeedPending = false;
        } else {
            speedX = (byte) romTables.linkSpeedX(mask);
            speedY = (byte) romTables.linkSpeedY(mask);
        }
        lastRomSpeedX = speedX & 0xFF;
        lastRomSpeedY = speedY & 0xFF;
        movingThisFrame = (speedX != 0 || speedY != 0);
        boolean applyGroundMotion = airborne || shouldApplyGroundMotion();

        if (applyGroundMotion && speedX != 0) {
            tryMoveAxis(speedX, true);
        }
        if (applyGroundMotion && speedY != 0) {
            tryMoveAxis(speedY, false);
        }

        if (collisionIgnoreFramesRemaining > 0) {
            collisionIgnoreFramesRemaining--;
        }

        tickJump();
        handlePegasusBootsCollision();

        if (!airborne) {
            if (!enterSwimmingIfNeeded()) {
                updateLastSafePositionIfPossible();
            }
        }

        if (movingThisFrame) {
            walkTickCounter++;
            if (walkTickCounter >= WALK_FRAME_TICKS) {
                walkTickCounter = 0;
                walkFrame ^= 1;
            }
        } else {
            walkTickCounter = 0;
            walkFrame = 0;
        }
    }

    /** Mirrors CheckItemsToUse's two Pegasus-Boots slot branches and UsePegasusBoots. */
    private void updatePegasusBootsUse() {
        if (playerState == null || inputState == null || inputConfig == null) {
            return;
        }
        boolean aHeld = playerState.itemA() == PlayerState.INVENTORY_PEGASUS_BOOTS
            && inputState.isDown(inputConfig.aKey());
        boolean bHeld = playerState.itemB() == PlayerState.INVENTORY_PEGASUS_BOOTS
            && inputState.isDown(inputConfig.bKey());
        if (!aHeld && !bHeld) {
            pegasusBootsChargeMeter = 0;
            return;
        }
        if (playerState.runningWithPegasusBoots()
            || romInteractiveMotionBlocked || airborne
            || zPixels() != 0 || groundStatus == GROUND_STATUS_PIT
            || isCarryingLiftedObject() || motionState == LINK_MOTION_SWIMMING) {
            return;
        }

        int heldBootsSlots = (aHeld ? 1 : 0) + (bHeld ? 1 : 0);
        for (int slot = 0; slot < heldBootsSlots; slot++) {
            if (pegasusBootsChargeMeter >= PEGASUS_BOOTS_MAX_CHARGE) {
                break;
            }
            pegasusBootsChargeMeter++;
            if (pegasusBootsChargeMeter == PEGASUS_BOOTS_MAX_CHARGE) {
                playerState.setRunningWithPegasusBoots(true);
                forcedSpeedPending = false;
                break;
            }
        }
    }

    private int pegasusRunningSpeedX() {
        return switch (direction) {
            case DIRECTION_LEFT -> -PEGASUS_BOOTS_RUNNING_SPEED;
            case DIRECTION_RIGHT -> PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    private int pegasusRunningSpeedY() {
        return switch (direction) {
            case DIRECTION_UP -> -PEGASUS_BOOTS_RUNNING_SPEED;
            case DIRECTION_DOWN -> PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    /** Mirrors bank2.asm:74AD's dash collision response and func_020_49BA. */
    private void handlePegasusBootsCollision() {
        if (playerState == null || !playerState.runningWithPegasusBoots()
            || (romCollisionType & 0x0F) == 0) {
            return;
        }

        int reflectedSpeedX = (-signedByte(lastRomSpeedX)) >> 2;
        int reflectedSpeedY = (-signedByte(lastRomSpeedY)) >> 2;
        resetPegasusBoots();
        forcedSpeedX = reflectedSpeedX;
        forcedSpeedY = reflectedSpeedY;
        forcedSpeedPending = true;
        lastRomSpeedX = reflectedSpeedX & 0xFF;
        lastRomSpeedY = reflectedSpeedY & 0xFF;
        airborne = true;
        zSubPixels = 0;
        zVelocity = PEGASUS_BOOTS_COLLISION_VELOCITY_Z;

        int romDirection = romDirectionForJavaDirection(direction);
        int[] xOffsets = {0x10, 0xF0, 0x08, 0x08};
        int[] yOffsets = {0x0C, 0x0C, 0xF0, 0x10};
        pegasusBootsCollisionPosX = (romEntityX() + xOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionPosY = (romEntityY() + yOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionCountdown = PEGASUS_BOOTS_COLLISION_COUNTDOWN;
        pendingPegasusScreenShakeCountdown = PEGASUS_BOOTS_COLLISION_SHAKE;
        pendingPegasusScreenShakePhase = (romDirection & 0x02) << 1;
    }

    private static int signedByte(int value) {
        int normalized = value & 0xFF;
        return normalized < 0x80 ? normalized : normalized - 0x100;
    }

    private void tickRomAttackStepAnimationCountdown() {
        if ((romAttackStepAnimationCountdown & ATTACK_STEP_DURATION_MASK) == 0) {
            romAttackStepAnimationCountdown = 0;
        } else {
            romAttackStepAnimationCountdown = (romAttackStepAnimationCountdown - 1) & 0xFF;
        }
    }

    private boolean itemsBlockMotion() {
        if (isLiftTransitionBlockingMotion()) {
            return true;
        }
        if (itemRegistry == null || playerState == null) {
            return false;
        }
        EquippedItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null && a.blocksMotion()) {
            return true;
        }
        EquippedItem b = itemRegistry.lookup(playerState.itemB());
        return b != null && b.blocksMotion();
    }

    private boolean itemsLockFacing() {
        if (itemRegistry == null || playerState == null) {
            return false;
        }
        EquippedItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null && a.locksFacing()) {
            return true;
        }
        EquippedItem b = itemRegistry.lookup(playerState.itemB());
        return b != null && b.locksFacing();
    }

    private boolean shouldApplyGroundMotion() {
        groundMotionCounter++;
        if (collision == null || !collision.linkOnSlowGround(pixelX(), pixelY())) {
            return true;
        }
        // GROUND_STATUS_SLOW ($03) uses the same frame-counter mask in the
        // original: skip the final position update when frameCounter & 3 == 0.
        return (groundMotionCounter & 0x03) != 0;
    }

    private boolean enterSwimmingIfNeeded() {
        if (!playerHasFlippers() || romTables == null || collision == null
            || !collision.linkOnDeepWater(pixelX(), pixelY())) {
            return false;
        }

        motionState = LINK_MOTION_SWIMMING;
        physicsModifier = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        swimmingSpeedX = romTables.swimmingEntrySpeedX(romDirectionForJavaDirection(direction));
        swimmingSpeedY = romTables.swimmingEntrySpeedY(romDirectionForJavaDirection(direction));
        lastRomSpeedX = swimmingSpeedX & 0xFF;
        lastRomSpeedY = swimmingSpeedY & 0xFF;
        return true;
    }

    private void updateSwimming() {
        if (collision == null || !collision.linkOnDeepWater(pixelX(), pixelY())) {
            leaveSwimming();
            return;
        }

        int mask = buildJoypadMask();
        int newDirection = JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            direction = newDirection;
        }

        if (swimmingFastCountdown > 0) {
            swimmingFastCountdown--;
        }
        if (divingCountdown > 0) {
            divingCountdown--;
            if (divingCountdown == 0) {
                physicsModifier = 0;
            }
        }

        if (buttonWasPressed(inputConfig == null ? -1 : inputConfig.bKey())) {
            physicsModifier ^= 1;
            divingCountdown = physicsModifier == 0 ? 0 : 0xA0;
        }
        if (buttonWasPressed(inputConfig == null ? -1 : inputConfig.aKey())) {
            swimmingFastCountdown = 0x20;
        }

        if (itemsBlockMotion()) {
            movingThisFrame = false;
            walkTickCounter = 0;
            walkFrame = 0;
            lastRomSpeedX = 0;
            lastRomSpeedY = 0;
            return;
        }

        if (forcedSpeedPending) {
            swimmingSpeedX = forcedSpeedX;
            swimmingSpeedY = forcedSpeedY;
            forcedSpeedPending = false;
        } else if ((frameCounter & 0x01) == 0) {
            boolean fast = swimmingFastCountdown >= 0x10;
            int targetX = romTables.swimmingSpeedX(mask, fast);
            int targetY = romTables.swimmingSpeedY(mask, fast);
            swimmingSpeedX = approachSignedSpeed(swimmingSpeedX, targetX);
            swimmingSpeedY = approachSignedSpeed(swimmingSpeedY, targetY);
        }

        lastRomSpeedX = swimmingSpeedX & 0xFF;
        lastRomSpeedY = swimmingSpeedY & 0xFF;
        movingThisFrame = swimmingSpeedX != 0 || swimmingSpeedY != 0;
        if (swimmingSpeedX != 0) {
            tryMoveAxis(swimmingSpeedX, true);
        }
        if (swimmingSpeedY != 0) {
            tryMoveAxis(swimmingSpeedY, false);
        }

        if (!collision.linkOnDeepWater(pixelX(), pixelY())) {
            leaveSwimming();
            return;
        }

        if (movingThisFrame) {
            walkTickCounter++;
            if (walkTickCounter >= WALK_FRAME_TICKS) {
                walkTickCounter = 0;
                walkFrame ^= 1;
            }
        } else {
            walkTickCounter = 0;
            walkFrame = 0;
        }
    }

    private boolean buttonWasPressed(int key) {
        return inputState != null && key >= 0 && inputState.wasPressed(key);
    }

    private boolean playerHasFlippers() {
        return playerState != null && playerState.hasFlippers();
    }

    private static int approachSignedSpeed(int current, int target) {
        current = (byte) current;
        target = (byte) target;
        int difference = target - current;
        if (difference == 0) {
            return current;
        }
        return (byte) (current + (difference < 0 ? -1 : 1));
    }

    private void leaveSwimming() {
        motionState = LINK_MOTION_DEFAULT;
        physicsModifier = 0;
        swimmingSpeedX = 0;
        swimmingSpeedY = 0;
        swimmingFastCountdown = 0;
        divingCountdown = 0;
        walkTickCounter = 0;
        walkFrame = 0;
    }

    private void tryMoveAxis(int signedSpeed, boolean isXAxis) {
        int candidateSubX = isXAxis ? subX + signedSpeed : subX;
        int candidateSubY = isXAxis ? subY : subY + signedSpeed;

        int candidatePixelX = candidateSubX >> SUB_PIXEL_SHIFT;
        int candidatePixelY = candidateSubY >> SUB_PIXEL_SHIFT;

        int moveDirection;
        if (isXAxis) {
            moveDirection = signedSpeed > 0 ? DIRECTION_RIGHT : DIRECTION_LEFT;
        } else {
            moveDirection = signedSpeed > 0 ? DIRECTION_DOWN : DIRECTION_UP;
        }

        if (collisionIgnoreFramesRemaining == 0
            && leadingEdgeBlocked(candidatePixelX, candidatePixelY, moveDirection)) {
            romCollisionType |= switch (moveDirection) {
                case DIRECTION_UP -> COLLISION_TYPE_UP;
                case DIRECTION_DOWN -> COLLISION_TYPE_DOWN;
                case DIRECTION_LEFT -> COLLISION_TYPE_LEFT;
                case DIRECTION_RIGHT -> COLLISION_TYPE_RIGHT;
                default -> 0;
            };
            return;
        }

        subX = candidateSubX;
        subY = candidateSubY;
    }

    /**
     * Point-based collision check on the leading edge of movement,
     * mirroring {@code LinkCollisionPointsX/Y} (bank2.asm:6302). Tests
     * two specific points on Link's sprite for the given direction; if
     * either is in a blocked cell, the move is rejected. This is what
     * lets Link step up and out of a scrolled-into-a-tree-row
     * situation — his upper-edge collision points are in the clear row
     * above even while his feet still overlap the tree cells below.
     */
    private boolean leadingEdgeBlocked(int spriteX, int spriteY, int dir) {
        int[] xs = COLLISION_POINTS_X[dir];
        int[] ys = COLLISION_POINTS_Y[dir];
        for (int i = 0; i < xs.length; i++) {
            int pointX = spriteX + xs[i];
            int pointY = spriteY + ys[i];
            if (collision.pointBlockedForLink(pointX, pointY, playerHasFlippers())
                && !collision.pointNormalPit(pointX, pointY)) {
                return true;
            }
        }
        return false;
    }

    private void tickJump() {
        if (!airborne || roosterCarryActive) {
            return;
        }

        tickJumpAnimation();
        zSubPixels += zVelocity;
        zVelocity -= 2;
        if (zSubPixels > 0 || zVelocity > 0) {
            return;
        }

        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        if (linkOverPit()) {
            enterPitGroundState();
        } else {
            groundStatus = GROUND_STATUS_NORMAL;
            fallingIntoPit = false;
        }
    }

    private void tickJumpAnimation() {
        jumpAnimationCounter++;
        if (jumpAnimationCounter >= JUMP_FRAME_TICKS) {
            jumpAnimationCounter = 0;
            if (jumpAnimationFrame < 2) {
                jumpAnimationFrame++;
            }
        }
    }

    private boolean linkOverPit() {
        return collision != null && collision.linkOnNormalPit(pixelX(), pixelY());
    }

    private void refreshGroundStatus() {
        if (collision != null) {
            collision.refreshLinkGroundInteraction(pixelX(), pixelY());
        }
        if (linkOverPit()) {
            enterPitGroundState();
        } else if (!fallingIntoPit) {
            groundStatus = GROUND_STATUS_NORMAL;
            hasPitSlipTarget = false;
        }
    }

    private void enterPitGroundState() {
        if (groundStatus != GROUND_STATUS_PIT) {
            pitSlippingCounter = 0;
            OverworldCollision.PitCell pit = collision == null ? null : collision.normalPitUnderLink(pixelX(), pixelY());
            if (pit != null) {
                pitSlipTargetTopLeftX = pit.targetTopLeftX();
                pitSlipTargetTopLeftY = pit.targetTopLeftY();
                pitSlipPhysicsFlag = pit.physicsFlag();
                hasPitSlipTarget = true;
            }
        }
        groundStatus = GROUND_STATUS_PIT;
    }

    private void slipTowardPitCenter() {
        if (!hasPitSlipTarget) {
            groundStatus = GROUND_STATUS_NORMAL;
            return;
        }

        pitSlippingCounter++;
        if ((pitSlippingCounter & 0x03) != 0) {
            return;
        }

        int x = pixelX();
        int y = pixelY();
        if (x < pitSlipTargetTopLeftX) {
            subX += 1 << SUB_PIXEL_SHIFT;
        } else if (x > pitSlipTargetTopLeftX) {
            subX -= 1 << SUB_PIXEL_SHIFT;
        }
        if (y < pitSlipTargetTopLeftY) {
            subY += 1 << SUB_PIXEL_SHIFT;
        } else if (y > pitSlipTargetTopLeftY) {
            subY -= 1 << SUB_PIXEL_SHIFT;
        }

        if (Math.abs(pixelX() - pitSlipTargetTopLeftX) <= 1
            && Math.abs(pixelY() - pitSlipTargetTopLeftY) <= 1) {
            startPitFall();
        }
    }

    private void startPitFall() {
        // Current top-view fall/recovery support is only for ordinary $50 pits.
        groundStatus = GROUND_STATUS_PIT;
        fallingIntoPit = true;
        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        fallingFrameCounter = 0;
        walkTickCounter = 0;
        walkFrame = 0;
        movingThisFrame = false;
        subY += 3 << SUB_PIXEL_SHIFT;
        soundSink.play(GameplaySoundEvent.PIT_FALL);
    }

    private void tickPitFall() {
        fallingFrameCounter++;
        if (fallingFrameCounter < FALL_DURATION_FRAMES) {
            return;
        }

        fallingIntoPit = false;
        groundStatus = GROUND_STATUS_NORMAL;
        hasPitSlipTarget = false;
        pitSlippingCounter = 0;
        fallingFrameCounter = 0;
        if (playerState != null) {
            playerState.damage(PIT_DAMAGE);
            playerState.setInvincibilityCounter(PIT_RECOVERY_INVINCIBILITY_FRAMES);
        }
        if (hasRoomEntryPosition) {
            subX = roomEntrySubX;
            subY = roomEntrySubY;
        } else if (hasLastSafePosition) {
            subX = lastSafeSubX;
            subY = lastSafeSubY;
        }
    }

    private void updateLastSafePositionIfPossible() {
        if (airborne || fallingIntoPit || groundStatus == GROUND_STATUS_PIT || linkOverPit()) {
            return;
        }
        lastSafeSubX = subX;
        lastSafeSubY = subY;
        hasLastSafePosition = true;
    }

    private int buildJoypadMask() {
        int mask = 0;
        if (inputState.isDown(inputConfig.downKey())) mask |= JOY_DOWN;
        if (inputState.isDown(inputConfig.upKey())) mask |= JOY_UP;
        if (inputState.isDown(inputConfig.leftKey())) mask |= JOY_LEFT;
        if (inputState.isDown(inputConfig.rightKey())) mask |= JOY_RIGHT;
        return mask;
    }

    /** Returns the currently held ROM D-pad mask used by projectile launch code. */
    public int romPressedButtonsMask() {
        if (inputState == null || inputConfig == null) {
            return 0;
        }
        return buildJoypadMask();
    }

    private static int romDirectionForJavaDirection(int javaDirection) {
        return switch (javaDirection) {
            case DIRECTION_RIGHT -> 0;
            case DIRECTION_LEFT -> 1;
            case DIRECTION_UP -> 2;
            case DIRECTION_DOWN -> 3;
            default -> throw new IllegalArgumentException("Link direction out of range: "
                + javaDirection);
        };
    }

    public void render(byte[] displayBuffer, int offsetX, int offsetY) {
        if (likeLikeCaptured) {
            return;
        }
        int animationState = resolveAnimationState();
        spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = spriteSheet.rightHalfFlipY(animationState);

        int originX = pixelX() + offsetX;
        int renderY = pixelY() - zPixels();
        int originY = renderY + offsetY;
        int[] palette = playerState != null
            && (playerState.invincibilityCounter() & 0x04) != 0
            // DrawLinkSprite's GBC path selects object palette 4 from bit 2
            // of wInvincibilityCounter while Link is flashing.
            ? tunicPalette.forObjectPalette(4)
            : tunicPalette.forTunic(
                playerState == null ? PlayerState.TUNIC_GREEN : playerState.tunicType());

        // Column-major tile layout: [0]=UL, [1]=LL, [2]=UR, [3]=LR.
        // GB 8x16 flipY swaps the two stacked tiles inside a column and flips
        // each vertically.
        Tile leftTop = composedTiles[leftFlipY ? 1 : 0];
        Tile leftBottom = composedTiles[leftFlipY ? 0 : 1];
        Tile rightTop = composedTiles[rightFlipY ? 3 : 2];
        Tile rightBottom = composedTiles[rightFlipY ? 2 : 3];

        drawTile(displayBuffer, leftTop,     originX,     originY,     leftFlipX,  leftFlipY,
            palette);
        drawTile(displayBuffer, leftBottom,  originX,     originY + 8, leftFlipX,  leftFlipY,
            palette);
        drawTile(displayBuffer, rightTop,    originX + 8, originY,     rightFlipX, rightFlipY,
            palette);
        drawTile(displayBuffer, rightBottom, originX + 8, originY + 8, rightFlipX, rightFlipY,
            palette);

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        renderEquippedItems(displayBuffer, offsetX, offsetY);
    }

    private void renderEquippedItems(byte[] displayBuffer, int offsetX, int offsetY) {
        if (itemRegistry == null || playerState == null) {
            return;
        }
        EquippedItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null) {
            a.render(displayBuffer, pixelX(), pixelY() - zPixels(), direction, offsetX, offsetY);
        }
        EquippedItem b = itemRegistry.lookup(playerState.itemB());
        if (b != null && b != a) {
            b.render(displayBuffer, pixelX(), pixelY() - zPixels(), direction, offsetX, offsetY);
        }
    }

    private int zPixels() {
        return Math.max(0, zSubPixels >> SUB_PIXEL_SHIFT);
    }

    private int resolveAnimationState() {
        if (romAnimationStateOverride >= 0) {
            return romAnimationStateOverride;
        }
        if (fallingIntoPit) {
            int frame = Math.min(FALL_ANIMATION_STATE.length - 1, fallingFrameCounter / FALL_FRAME_TICKS);
            return FALL_ANIMATION_STATE[frame];
        }

        if (motionState == LINK_MOTION_SWIMMING) {
            return physicsModifier != 0
                ? DIVING_ANIMATION_STATE[walkFrame]
                : SWIMMING_ANIMATION_STATE[direction][walkFrame];
        }

        // func_002_4338 copies the intermediate ROM carry animation state
        // directly (for example $37/$39/$3B/$3D). The final carry state is
        // the regular two-frame lifting list below.
        if (carryingLiftedObjectState >= 2) {
            return carryingLiftedObjectState;
        }
        int baseState = ANIMATION_STATE[direction][walkFrame];
        if (carryingLiftedObjectState == 1) {
            return LIFTING_ANIMATION_STATE[direction][walkFrame];
        }
        int override = queryOverride(itemInSlotA());
        if (override >= 0) {
            return override;
        }
        override = queryOverride(itemInSlotB());
        if (override >= 0) {
            return override;
        }
        if (airborne) {
            return JUMP_ANIMATION_STATE[direction][jumpAnimationFrame];
        }
        if (isUsingShield() && playerState != null && playerState.shieldLevel() > 0) {
            int[][] shieldTable = playerState.shieldLevel() >= 2
                ? MIRROR_SHIELD_USE_ANIMATION_STATE : SHIELD_USE_ANIMATION_STATE;
            return shieldTable[direction][walkFrame];
        }
        return baseState;
    }

    private boolean isLiftTransitionBlockingMotion() {
        return carryingLiftedObjectState >= 2 && !roosterCarryActive;
    }

    private static int javaDirectionForRomDirection(int romDirection) {
        return switch (romDirection) {
            case 0 -> DIRECTION_RIGHT;
            case 1 -> DIRECTION_LEFT;
            case 2 -> DIRECTION_UP;
            case 3 -> DIRECTION_DOWN;
            default -> throw new AssertionError(romDirection);
        };
    }

    private EquippedItem itemInSlotA() {
        if (itemRegistry == null || playerState == null) {
            return null;
        }
        return itemRegistry.lookup(playerState.itemA());
    }

    private EquippedItem itemInSlotB() {
        if (itemRegistry == null || playerState == null) {
            return null;
        }
        return itemRegistry.lookup(playerState.itemB());
    }

    private int queryOverride(EquippedItem item) {
        if (item == null) {
            return -1;
        }
        return item.overrideAnimationState(direction, walkFrame);
    }

    private static void drawTile(byte[] buffer, Tile tile, int screenX, int screenY,
                                 boolean flipX, boolean flipY, int[] palette) {
        if (tile == null) {
            return;
        }
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int sx = flipX ? 7 - tx : tx;
                int sy = flipY ? 7 - ty : ty;
                int colorIndex = tile.getPixel(sx, sy);
                if (colorIndex == 0) {
                    continue; // transparent
                }
                int color = palette[colorIndex];
                int offset = (py * Framebuffer.WIDTH + px) * 4;
                buffer[offset] = (byte) ((color >> 16) & 0xFF);
                buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
                buffer[offset + 2] = (byte) (color & 0xFF);
                buffer[offset + 3] = (byte) 0xFF;
            }
        }
    }
}
