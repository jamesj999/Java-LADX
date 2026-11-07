package linksawakening.entity;

import linksawakening.equipment.EquipmentItem;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;

import java.util.Objects;
import java.util.function.BooleanSupplier;

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

    final LinkAirborne linkAirborne = new LinkAirborne(this);

    final LinkSwimming linkSwimming = new LinkSwimming(this);

    final LinkStateMachine linkStateMachine = new LinkStateMachine(this);

    final LinkSideView linkSideView = new LinkSideView(this);

    final LinkDamage linkDamage = new LinkDamage(this);

    final LinkMotion linkMotion = new LinkMotion(this);

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
    static final int JOY_RIGHT = 1 << 0;
    static final int JOY_LEFT = 1 << 1;
    static final int JOY_UP = 1 << 2;
    static final int JOY_DOWN = 1 << 3;

    // JoypadToLinkDirection table from bank2.asm:1183 — any two-axis combination
    // keeps the previous facing. Encoded as one of DIRECTION_* or -1 (= KEEP).
    static final int[] JOYPAD_TO_DIRECTION = {
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

    // LinkAnimationsList_WalkUsingDefaultShield and the corresponding mirror
    // list in bank2.asm:1214-1240. Java direction order is DOWN, UP, LEFT,
    // RIGHT, matching the ordinary walking table above.

    // LinkAnimationsList_LiftingObject (bank2.asm:1249), indexed in the
    // same Java direction order as the walking table.

    // Data_002_4948 (bank2.asm:1257), in Java direction order. The second
    // row is selected while hLinkPhysicsModifier is non-zero (diving).

    static final int WALK_FRAME_TICKS = 8;
    static final int JUMP_FRAME_TICKS = 8;
    private static final int FALL_FRAME_TICKS = 16;
    static final int FALL_DURATION_FRAMES = 96;
    private static final int PIT_RECOVERY_INVINCIBILITY_FRAMES = 0x40;
    private static final int PIT_DAMAGE = PlayerState.HP_PER_HEART / 2;
    private static final int ATTACK_STEP_ITEM_ANY = 0x00;
    private static final int ATTACK_STEP_DURATION_MASK = 0x7F;
    private static final int ROM_ITEM_ATTACK_STEP_COUNTDOWN = 0x0C | ATTACK_STEP_ITEM_ANY;
    static final int COLLISION_TYPE_UP = 0x01;
    static final int COLLISION_TYPE_DOWN = 0x02;
    static final int COLLISION_TYPE_LEFT = 0x04;
    static final int COLLISION_TYPE_RIGHT = 0x08;
    static final int PEGASUS_BOOTS_MAX_CHARGE = 0x20;
    static final int PEGASUS_BOOTS_RUNNING_SPEED = 0x20;
    static final int PEGASUS_BOOTS_COLLISION_COUNTDOWN = 0x02;
    static final int PEGASUS_BOOTS_COLLISION_SHAKE = 0x20;
    static final int PEGASUS_BOOTS_COLLISION_VELOCITY_Z = 0x18;


    final InputState inputState;
    final InputConfig inputConfig;
    final RomTables romTables;
    final OverworldCollision collision;
    private final LinkSpriteSheet spriteSheet;
    final PlayerState playerState;
    final ItemRegistry itemRegistry;
    private final LinkTunicPalette tunicPalette;

    // Two collision check points per direction, as offsets from the
    // sprite's top-left. Ported from LinkCollisionPointsX/Y
    // (bank2.asm:6302-6319). Only the leading edge of movement is
    // checked, which is why Link can walk out of a "stuck in tree row"
    // position by pressing UP (top-edge points are in the clear row)
    // even though his feet are still overlapping a solid cell below.
    //
    // Indexed by DIRECTION_* constants below.
    static final int[][] COLLISION_POINTS_X = {
        { 6, 9 },   // DOWN
        { 6, 9 },   // UP
        { 4, 4 },   // LEFT
        { 11, 11 }, // RIGHT
    };
    static final int[][] COLLISION_POINTS_Y = {
        { 15, 15 }, // DOWN — bottom edge
        { 6,  6 },  // UP — top edge (not 0; matches LA's offset)
        { 9,  12 }, // LEFT — middle-left
        { 9,  12 }, // RIGHT — middle-right
    };

    int direction = DIRECTION_DOWN;
    int walkTickCounter;
    int walkFrame;
    boolean movingThisFrame;
    private int groundMotionCounter;
    int pegasusBootsChargeMeter;
    int pegasusBootsCollisionCountdown;
    int pegasusBootsCollisionPosX;
    int pegasusBootsCollisionPosY;
    int pendingPegasusScreenShakeCountdown;
    int pendingPegasusScreenShakePhase;
    /** Mirrors wC147: Link was standing on a side-view moving entity this frame. */
    /** One-frame hLinkSpeedY write supplied by the side-view platform handler. */
    boolean topDownFeatherStarted;

    /** Warp entry sets hLinkPhysicsModifier=1 at $00:18F0. */


    boolean debugNoClip;
    int romAnimationStateOverride = -1;
    int swordAcquisitionBaseRomDirection = -1;
    int swordAcquisitionAnimationState = -1;
    private boolean tarinShieldPresentation;
    private int[] tarinShieldPalette;
    private int romLinkPushing;
    int marinWakeUpBedVariant = -1;
    /** Mirrors wC10A while Marin's scripted bed-exit jump owns Link motion. */
    boolean marinBedJumpMotionLocked;
    int bedJumpRenderZ = -1;
    /** Mirrors wC191, the collision-probe countdown for directional ledges. */
    /** Mirrors wC10A while a directional ledge jump owns Link's XY speed. */
    /** Mirrors LINK_MOTION_UNSTUCKING after an overworld $10 ledge collision. */
    private long physicsRoomLoadToken = -1;
    private int physicsRoomMapId = -1;
    private int physicsRoomId = -1;
    private int physicsRoomCategory = -1;
    int frameCounter;
    private int romAttackStepAnimationCountdown;
    boolean likeLikeCaptured;
    boolean likeLikeCapturePinsPosition;
    int likeLikeCaptureSubX;
    int likeLikeCaptureSubY;
    int jumpAnimationCounter;
    int jumpAnimationFrame;
    private int carryingLiftedObjectState;
    private int carryingLiftedObjectRomDirection = 3;
    boolean roosterCarryActive;
    int lastSafeSubX;
    int lastSafeSubY;
    boolean hasLastSafePosition;
    private int romFinalSubX;
    private int romFinalSubY;
    private boolean hasRomFinalPosition;
    int roomEntrySubX;
    int roomEntrySubY;
    boolean hasRoomEntryPosition;
    final GameplaySoundSink soundSink;
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

    public void setSideScrolling(boolean enabled) {
        linkSideView.setSideScrolling(enabled);
    }

    public void setPixelPosition(int pixelX, int pixelY) {
        linkMotion.subX = pixelX << SUB_PIXEL_SHIFT;
        linkMotion.subY = pixelY << SUB_PIXEL_SHIFT;
    }

    /** Enables the development movement mode; normal gameplay leaves this off. */
    public void setDebugNoClip(boolean enabled) {
        debugNoClip = enabled;
        if (enabled) {
            linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
            linkAirborne.fallingIntoPit = false;
            linkAirborne.airborne = false;
            linkAirborne.zSubPixels = 0;
            linkAirborne.zVelocity = 0;
        }
    }

    public boolean debugNoClip() {
        return debugNoClip;
    }

    /** Clears scripted presentation/capture state before a debug game reset. */
    public void resetTransientStateForDebug() {
        debugNoClip = false;
        linkStateMachine.romInteractiveMotionBlocked = false;
        linkSideView.standingOnSideViewEntity = false;
        linkSideView.sideViewPlatformVerticalSpeedPending = -1;
        linkDamage.collisionDamageImmune = false;
        romAnimationStateOverride = -1;
        swordAcquisitionBaseRomDirection = -1;
        swordAcquisitionAnimationState = -1;
        tarinShieldPresentation = false;
        tarinShieldPalette = null;
        marinWakeUpBedVariant = -1;
        marinBedJumpMotionLocked = false;
        bedJumpRenderZ = -1;
        linkAirborne.directionalLedgeJumpMotionLocked = false;
        linkAirborne.overworldLedgeFallActive = false;
        likeLikeCaptured = false;
        likeLikeCapturePinsPosition = false;
        linkAirborne.fallingIntoPit = false;
        carryingLiftedObjectState = 0;
        topDownFeatherStarted = false;
        roosterCarryActive = false;
        linkAirborne.airborne = false;
        linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
        linkStateMachine.motionState = LINK_MOTION_DEFAULT;
        linkStateMachine.revolvingDoorFrame = 0;
        linkStateMachine.revolvingDoorTransitionPending = false;
        linkStateMachine.revolvingDoorPaletteVariant = -1;
        linkStateMachine.physicsModifier = 0;
        physicsRoomLoadToken = -1;
        physicsRoomMapId = -1;
        physicsRoomId = -1;
        physicsRoomCategory = -1;
        linkAirborne.zSubPixels = 0;
        linkAirborne.zVelocity = 0;
        linkMotion.forcedSpeedPending = false;
        linkMotion.forcedSpeedXOnly = false;
        linkDamage.collisionIgnoreFramesRemaining = 0;
        romLinkPushing = 0;
        romAttackStepAnimationCountdown = 0;
        movingThisFrame = false;
        walkTickCounter = 0;
        walkFrame = 0;
    }

    public void setRoomEntryPixelPosition(int pixelX, int pixelY) {
        clearSideViewPlatformState();
        setPixelPosition(pixelX, pixelY);
        markRoomEntryPosition();
    }

    /** ApplyRoomTransition's completion path ($02:793D-$7977). */
    public void finishRoomScroll(boolean downward) {
        linkMotion.lastRomSpeedX = 0;
        linkMotion.lastRomSpeedY = 0;
        linkAirborne.zVelocity = 0;
        if (!downward || debugNoClip || collision == null) {
            return;
        }
        // Run the ordinary downward collision handler, including its ledge
        // half-tile filters, before applying the source's fallback.
        int probeX = pixelX();
        int probeY = pixelY();
        linkMotion.lastRomSpeedY = 0x01;
        boolean blocked = linkMotion.leadingEdgeBlocked(probeX, probeY, DIRECTION_DOWN);
        int object = collision.objectIdAtPoint(
            probeX + COLLISION_POINTS_X[DIRECTION_DOWN][1],
            probeY + COLLISION_POINTS_Y[DIRECTION_DOWN][1]);
        // Switch blocks $DB/$DC are exempt from fallback unstucking.
        if (object != 0xDB && object != 0xDC && !linkAirborne.overworldLedgeFallActive
            && (object == 0xE1 || blocked)) {
            // func_002_6EAD aligns X and enters LINK_MOTION_UNSTUCKING.
            // This transition fallback does not play the ledge jingle.
            linkMotion.subX = (romEntityX() & 0xF0) << SUB_PIXEL_SHIFT;
            linkMotion.subY -= 8 << SUB_PIXEL_SHIFT;
            linkStateMachine.motionState = 0x02;
            linkStateMachine.physicsModifier = 0;
            linkAirborne.overworldLedgeFallActive = true;
            linkMotion.beginOverworldLedgeUnstuckingMotion();
        }
    }

    public void markRoomEntryPosition() {
        roomEntrySubX = linkMotion.subX;
        roomEntrySubY = linkMotion.subY;
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

    /** Manual warp exits bypass the ordinary room-edge recoil gate. */
    public void setCollisionIgnoreFrames(int frames) {
        linkDamage.setCollisionIgnoreFrames(frames);
    }

    public void setNormalRecoilRoomBoundaries(boolean normalBoundaries) {
        linkDamage.normalRecoilRoomBoundaries = normalBoundaries;
    }

    /**
     * Sets wIgnoreLinkCollisionsCountdown: preserve recoil speed instead of
     * reading movement input. Background collisions still run ($02:44C2).
     */


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
        linkMotion.forcedSpeedX = (byte) speedX;
        linkMotion.forcedSpeedY = (byte) speedY;
        linkMotion.forcedSpeedXOnly = false;
        linkMotion.forcedSpeedPending = true;
    }

    /** Applies only the hLinkSpeedX write emitted by a side-view pot push. */
    public void applyRomSpeedX(int speedX) {
        validateRomByte(speedX, "Link X speed");
        linkMotion.forcedSpeedX = (byte) speedX;
        // The pot handler leaves hLinkSpeedY untouched; Link computes its
        // ordinary Y input on the next motion update.
        linkMotion.forcedSpeedXOnly = true;
        linkMotion.forcedSpeedPending = true;
    }

    /**
     * Applies the speed bytes written by a handler that immediately calls
     * {@code UpdateFinalLinkPosition}, bypassing input and collision probes.
     */
    public void applyRomFinalPosition(int speedX, int speedY) {
        if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Link final-position speeds must be unsigned bytes");
        }
        linkMotion.subY += (byte) speedY;
        linkMotion.subX += (byte) speedX;
    }

    /** Applies the side-view platform handler's Link position and speed writes. */
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY,
                                              int romSpeedY, boolean standing) {
        if (horizontalDelta < -0x80 || horizontalDelta > 0x7F) {
            throw new IllegalArgumentException("Side-view platform X delta must be a signed byte");
        }
        validateRomByte(romPositionY, "Side-view platform Link Y");
        validateRomByte(romSpeedY, "Side-view platform Link Y speed");

        int currentRomX = romEntityX() & 0xFF;
        int nextRomX = (currentRomX + horizontalDelta) & 0xFF;
        int nextTopLeftX = (nextRomX - 0x08) & 0xFF;
        int nextTopLeftY = (romPositionY - 0x10) & 0xFF;
        linkMotion.subX = (nextTopLeftX << SUB_PIXEL_SHIFT) | (linkMotion.subX & 0x0F);
        linkMotion.subY = (nextTopLeftY << SUB_PIXEL_SHIFT) | (linkMotion.subY & 0x0F);
        linkSideView.standingOnSideViewEntity = standing;
        // The platform handler writes hLinkSpeedY as an HRAM observation for
        // this frame and for one subsequent vertical movement attempt. It
        // never queues horizontal movement.
        linkMotion.lastRomSpeedY = romSpeedY & 0xFF;
        linkSideView.sideViewPlatformVerticalSpeedPending = standing ? romSpeedY & 0xFF : -1;
    }

    /** Applies a standing platform contact (the common side-view case). */
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY,
                                              int romSpeedY) {
        applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY, true);
    }

    /** Returns whether Link stood on a side-view entity during the current frame. */
    public boolean standingOnSideViewEntity() {
        return linkSideView.standingOnSideViewEntity;
    }

    /** Clears all transient side-view platform state during a room transition. */
    public void clearSideViewPlatformState() {
        linkSideView.standingOnSideViewEntity = false;
        linkSideView.sideViewPlatformVerticalSpeedPending = -1;
    }

    /** Clears only the per-frame side-view standing flag before entity processing. */
    public void clearStandingOnSideViewEntity() {
        linkSideView.standingOnSideViewEntity = false;
    }

    /** Captures hLinkPositionX/Y into the ROM's per-frame final-position shadow. */
    public void captureRomFinalPosition() {
        romFinalSubX = linkMotion.subX;
        romFinalSubY = linkMotion.subY;
        hasRomFinalPosition = true;
    }

    /** Copies the captured ROM final position back over an entity push. */
    public void restoreRomFinalPosition() {
        if (!hasRomFinalPosition) {
            return;
        }
        linkMotion.subX = romFinalSubX;
        linkMotion.subY = romFinalSubY;
    }

    /** Copies only hLinkFinalPositionX back over a side-view pot push. */
    public void restoreRomFinalPositionX() {
        if (hasRomFinalPosition) {
            linkMotion.subX = romFinalSubX;
        }
    }

    /** Applies state-0 pot's top snap and hLinkSpeedY/wC147 writes. */
    public void applySideViewPotContact(int romPositionY, int romSpeedY) {
        validateRomByte(romPositionY, "Side-view pot Link Y");
        validateRomByte(romSpeedY, "Side-view pot Link Y speed");
        int nextTopLeftY = (romPositionY - 0x10) & 0xFF;
        linkMotion.subY = (nextTopLeftY << SUB_PIXEL_SHIFT) | (linkMotion.subY & 0x0F);
        linkSideView.standingOnSideViewEntity = true;
        linkMotion.lastRomSpeedY = romSpeedY;
        linkSideView.sideViewPlatformVerticalSpeedPending = romSpeedY;
    }

    /** Mirrors ClearLinkPositionIncrement for handlers that clear HRAM speed bytes. */
    public void clearRomPositionIncrement() {
        linkMotion.forcedSpeedX = 0;
        linkMotion.forcedSpeedY = 0;
        linkMotion.forcedSpeedPending = false;
        linkMotion.forcedSpeedXOnly = false;
        linkSideView.sideViewPlatformVerticalSpeedPending = -1;
        linkMotion.lastRomSpeedX = 0;
        linkMotion.lastRomSpeedY = 0;
    }

    /** Mirrors the transient wIsLinkPushing value consumed by item handlers. */
    public void markRomLinkPushing(int countdown) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Link pushing countdown must be an unsigned byte");
        }
        romLinkPushing = countdown;
    }

    /** Mirrors func_20_4B1F's one-frame decrement of wIsLinkPushing. */
    public void tickRomLinkPushing() {
        if (romLinkPushing > 0) {
            romLinkPushing--;
        }
    }

    public boolean isRomLinkPushing() {
        return romLinkPushing != 0;
    }

    /** Applies the ROM's next-frame hLinkInteractiveMotionBlocked=$02 write. */
    public void blockNextRomMotionFrame() {
        linkStateMachine.romInteractiveMotionBlocked = true;
    }

    /** Applies the ROM's explicit hLinkAnimationState=$6A fallen-pose write. */
    public void showRomFallenPose() {
        linkStateMachine.romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6A;
        movingThisFrame = false;
    }

    /** Applies HoldEntityAboveLink's got-item pose and interaction lock. */
    public void showHeldItemPose() {
        linkStateMachine.romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6C;
        direction = DIRECTION_DOWN;
        romAttackStepAnimationCountdown = 0;
        movingThisFrame = false;
    }

    /** HandleGotItemB's power-up pose, distinct from lifting a physical object. */
    public void showPowerUpPickupPose() {
        linkStateMachine.romInteractiveMotionBlocked = true;
        romAnimationStateOverride = 0x6B;
        linkDamage.collisionIgnoreFramesRemaining = 0;
        linkMotion.forcedSpeedPending = false;
        romAttackStepAnimationCountdown = 0;
        movingThisFrame = false;
    }

    /** Applies TarinShield2Handler's shield-specific got-item presentation. */
    public void showTarinShieldPresentation(int[] palette) {
        if (palette == null || palette.length < 4) {
            throw new IllegalArgumentException("Tarin shield palette must contain four colors");
        }
        showHeldItemPose();
        tarinShieldPresentation = true;
        tarinShieldPalette = palette.clone();
    }

    public void clearTarinShieldPresentation() {
        tarinShieldPresentation = false;
    }

    public boolean isTarinShieldPresentationVisible() {
        return tarinShieldPresentation;
    }

    public int[] tarinShieldPalette() {
        return tarinShieldPalette == null ? null : tarinShieldPalette.clone();
    }

    /** Mirrors TarinShield2Handler's final LINK_ANIMATION_STATE_STANDING_SHIELD_DOWN write. */
    public void showStandingShieldDownPose() {
        tarinShieldPresentation = false;
        romAnimationStateOverride = 0x22;
        linkStateMachine.romInteractiveMotionBlocked = false;
        direction = DIRECTION_DOWN;
        movingThisFrame = false;
    }

    /** Applies one ROM spin sector while obtaining the beach sword. */
    public void showSwordAcquisitionSpinPose(int countdown) {
        if (countdown < 0 || countdown > 0x20) {
            throw new IllegalArgumentException("Sword-spin countdown must be between 0 and 32");
        }
        if (romTables == null) {
            throw new IllegalStateException("Sword acquisition spin requires ROM tables");
        }
        if (swordAcquisitionBaseRomDirection < 0) {
            swordAcquisitionBaseRomDirection = romDirectionForJavaDirection(direction);
            swordAcquisitionAnimationState = resolveAnimationState();
        }
        // The spin handler writes a fresh hLinkAnimationState. Do not let a
        // preceding generic motion-block request leave its $6A butt pose in
        // front of this ROM-driven sword pose.
        romAnimationStateOverride = -1;
        int sector = Math.min(7, countdown >> 2);
        int swordState = romTables.swordSpinAnimationState(
            swordAcquisitionBaseRomDirection, sector);
        int absoluteRomDirection = romTables.swordSpinAbsoluteDirection(
            swordAcquisitionBaseRomDirection, sector);
        int animationState = romTables.swordDirectionAnimState(
            absoluteRomDirection, swordState);
        // UpdateSpinAttackAnimation leaves hLinkAnimationState unchanged for
        // LINK_ANIMATION_STATE_HIDDEN, so retain the previous ROM pose.
        if (animationState != 0xFF) {
            swordAcquisitionAnimationState = animationState;
        }
        linkStateMachine.romInteractiveMotionBlocked = true;
        direction = javaDirectionForRomDirection(absoluteRomDirection);
        movingThisFrame = false;
    }

    /** Mirrors the sword handler's final hLinkAnimationState=$6B write. */
    public void showSwordAcquisitionFinalPose() {
        linkStateMachine.romInteractiveMotionBlocked = true;
        swordAcquisitionBaseRomDirection = -1;
        swordAcquisitionAnimationState = -1;
        romAnimationStateOverride = 0x6B;
        direction = DIRECTION_DOWN;
        movingThisFrame = false;
    }

    /** Hides ordinary Link OAM and selects Marin's dedicated bed display list. */
    public void showMarinWakeUpBed(int spriteVariant) {
        if (spriteVariant < 0 || spriteVariant > 4) {
            throw new IllegalArgumentException("Bed sprite variant must be between 0 and 4");
        }
        marinWakeUpBedVariant = spriteVariant;
    }

    public boolean isMarinWakeUpBedVisible() {
        return marinWakeUpBedVariant >= 0;
    }

    public int marinWakeUpBedVariant() {
        return marinWakeUpBedVariant;
    }

    public int pixelX() {
        return linkMotion.subX >> SUB_PIXEL_SHIFT;
    }

    public int pixelY() {
        return linkMotion.subY >> SUB_PIXEL_SHIFT;
    }

    /** ROM hLinkPositionX: the first 8x16 OAM column's X origin. */
    public int romEntityX() {
        return pixelX() + 0x08;
    }

    /** ROM hLinkFinalPositionX captured before the current Link update. */
    public int romFinalPositionX() {
        return hasRomFinalPosition ? (romFinalSubX >> SUB_PIXEL_SHIFT) + 0x08
            : romEntityX();
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
        applyLikeLikeCapture(romEntityX, romEntityY, true);
    }

    /** Hiding and shield theft precede the ROM's default-motion position gate. */
    public void applyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition) {
        if ((romEntityX & ~0xFF) != 0 || (romEntityY & ~0xFF) != 0) {
            throw new IllegalArgumentException("Like Like capture coordinates must be bytes");
        }
        likeLikeCaptured = true;
        likeLikeCapturePinsPosition = pinPosition;
        if (!pinPosition) {
            return;
        }
        likeLikeCaptureSubX = ((romEntityX - 0x08) & 0xFF) << SUB_PIXEL_SHIFT;
        likeLikeCaptureSubY = ((romEntityY - 0x10) & 0xFF) << SUB_PIXEL_SHIFT;
        linkMotion.subX = likeLikeCaptureSubX;
        linkMotion.subY = likeLikeCaptureSubY;
        linkAirborne.airborne = false;
        linkAirborne.zSubPixels = 0;
        linkAirborne.zVelocity = 0;
        linkAirborne.fallingIntoPit = false;
        linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
        linkStateMachine.motionState = LINK_MOTION_DEFAULT;
        linkStateMachine.physicsModifier = 0;
        linkSwimming.swimmingSpeedX = 0;
        linkSwimming.swimmingSpeedY = 0;
        linkSwimming.swimmingFastCountdown = 0;
        linkSwimming.divingCountdown = 0;
        movingThisFrame = false;
        linkMotion.romCollisionType = 0;
        linkMotion.lastRomSpeedX = 0;
        linkMotion.lastRomSpeedY = 0;
        linkMotion.forcedSpeedPending = false;
        linkMotion.forcedSpeedXOnly = false;
    }

    /** Releases Link when the swallowed Like Like handler accepts A or B. */
    public void releaseLikeLikeCapture() {
        likeLikeCaptured = false;
        likeLikeCapturePinsPosition = false;
    }

    public boolean isLikeLikeCaptured() {
        return likeLikeCaptured;
    }

    /**
     * ROM wLinkMotionState values needed by entity collision handlers. Normal
     * ground and linkAirborne.airborne motion both remain interactive; falling into or
     * slipping across a pit uses the non-interactive falling state.
     */
    public int romMotionState() {
        if (linkAirborne.groundStatus == GROUND_STATUS_PIT || linkAirborne.fallingIntoPit) {
            return 0x06;
        }
        return linkStateMachine.motionState;
    }

    public boolean isSwimming() {
        return linkStateMachine.motionState == LINK_MOTION_SWIMMING;
    }

    public boolean isDiving() {
        return isSwimming() && linkStateMachine.physicsModifier != 0;
    }

    /** Mirrors hLinkPhysicsModifier for source-shaped map-transition checks. */
    public int romPhysicsModifier() {
        return linkStateMachine.physicsModifier & 0xFF;
    }

    /**
     * Applies the source room-entry default once per loaded room. The load
     * serial also distinguishes a save/new-game reload of the same room, so a
     * prior ladder or diving modifier cannot leak across the reload.
     */
    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId,
                                             int mapCategory) {
        if (physicsRoomLoadToken == loadToken
            && physicsRoomMapId == mapId
            && physicsRoomId == roomId
            && physicsRoomCategory == mapCategory) {
            return;
        }
        linkStateMachine.physicsModifier = mapCategory == 2 ? 0x01 : 0x00;
        physicsRoomLoadToken = loadToken;
        physicsRoomMapId = mapId;
        physicsRoomId = roomId;
        physicsRoomCategory = mapCategory;
        linkDamage.collisionDamageImmune = false;
    }

    /** Applies the room-entry physics default after a completed warp. */
    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId,
                                      int mapCategory) {
        linkStateMachine.physicsModifier = mapCategory == 2 ? 0x01 : 0x00;
        physicsRoomLoadToken = loadToken;
        physicsRoomMapId = mapId;
        physicsRoomId = roomId;
        physicsRoomCategory = mapCategory;
        linkDamage.collisionDamageImmune = false;
    }

    /** Mirrors source ladder/diving updates to hLinkPhysicsModifier. */
    public void setRomPhysicsModifier(int modifier) {
        linkStateMachine.physicsModifier = modifier & 0xFF;
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

    /** Mirrors TarinEntityHandler's direct clear of the item attack-step byte. */
    public void clearRomAttackStepAnimationCountdown() {
        romAttackStepAnimationCountdown = 0;
    }

    /**
     * Mirrors the leading CheckItemsToUse gate used before an equipped item
     * spends inventory. Airborne motion remains eligible; PlaceBomb does not
     * reject it, while pit, carry, and interactive item motion do.
     */
    public boolean canUseItems() {
        if (romMotionState() != 0 || isCarryingLiftedObject() || isRomLinkPushing()) {
            return false;
        }
        return !itemsBlockMotion();
    }

    /** Mirrors the unsigned {@code wCollisionType} byte written by Link motion. */
    public int romCollisionType() {
        return linkMotion.romCollisionType & 0xFF;
    }

    /**
     * Position used by the facing-direction collision probe before movement was
     * rejected. Bank 2 dispatches interactive objects during that probe; callers
     * running after update must not re-probe Link's restored, passable position.
     */
    public int romCollisionProbePixelX() {
        return hasFacingCollision() ? linkMotion.collisionProbePixelX[direction] : pixelX();
    }

    public int romCollisionProbePixelY() {
        return hasFacingCollision() ? linkMotion.collisionProbePixelY[direction] : pixelY();
    }

    private boolean hasFacingCollision() {
        int flag = switch (direction) {
            case DIRECTION_UP -> COLLISION_TYPE_UP;
            case DIRECTION_DOWN -> COLLISION_TYPE_DOWN;
            case DIRECTION_LEFT -> COLLISION_TYPE_LEFT;
            case DIRECTION_RIGHT -> COLLISION_TYPE_RIGHT;
            default -> 0;
        };
        return (linkMotion.romCollisionType & flag) != 0;
    }

    /** ROM hLinkSpeedX written by the current Link motion update. */
    public int romSpeedX() {
        return linkMotion.lastRomSpeedX & 0xFF;
    }

    /** ROM hLinkSpeedY written by the current Link motion update. */
    public int romSpeedY() {
        return linkMotion.lastRomSpeedY & 0xFF;
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

    boolean gelClinging;
    int gelMotionFrame;

    /** Previous entity pass's wIsGelClingingToLink and current hFrameCounter. */
    public void setGelClinging(boolean clinging, int worldFrame) {
        gelClinging = clinging;
        gelMotionFrame = worldFrame & 0xFF;
    }

    private BooleanSupplier shieldInputAllowed = () -> true;

    /** Live input gate, also consulted when rendering a paused dialogue frame. */
    public void setShieldInputAllowed(BooleanSupplier allowed) {
        shieldInputAllowed = Objects.requireNonNull(allowed);
    }

    /** Mirrors wIsUsingShield: equipped, held, and permitted by gameplay input. */
    public boolean isUsingShield() {
        if (gelClinging || !shieldInputAllowed.getAsBoolean()) {
            return false;
        }
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        boolean usingA = playerState.itemA() == PlayerState.INVENTORY_SHIELD
            && inputState.isDown(inputConfig.aKey());
        boolean usingB = playerState.itemB() == PlayerState.INVENTORY_SHIELD
            && inputState.isDown(inputConfig.bKey());
        return usingA || usingB;
    }

    /** ROM wC145: the Y origin used by sword collision while linkAirborne.airborne. */
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
        linkStateMachine.motionState = LINK_MOTION_DEFAULT;
        linkAirborne.airborne = true;
        linkAirborne.zSubPixels = (positionZ & 0xFF) << SUB_PIXEL_SHIFT;
        linkAirborne.zVelocity = velocityZ & 0xFF;
        linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
        linkAirborne.fallingIntoPit = false;
        linkStateMachine.physicsModifier = 0;
        roosterCarryActive = true;
        direction = javaDirectionForRomDirection(romDirection);
        applyRomSpeed(speedX, speedY);
    }

    /** Applies Hinox's direct hLinkPosition/throw writes for the next motion frame. */
    public void applyHinoxGrabState(int romPositionX, int romPositionY, int positionZ,
                                    int speedX, int speedY, int velocityZ,
                                    int airborneState) {
        applyHinoxGrabState(romPositionX, romPositionY, positionZ, speedX, speedY,
            velocityZ, airborneState, true, true);
    }

    /** Applies Hinox's held position; only the source's countdown sets blocking. */
    public void applyHinoxGrabState(int romPositionX, int romPositionY, int positionZ,
                                    int speedX, int speedY, int velocityZ,
                                    int airborneState, boolean motionBlocked,
                                    boolean applyHeldLinkPose) {
        validateRomByte(romPositionX, "Hinox Link X");
        validateRomByte(romPositionY, "Hinox Link Y");
        validateRomByte(positionZ, "Hinox Link Z");
        validateRomByte(speedX, "Hinox Link X speed");
        validateRomByte(speedY, "Hinox Link Y speed");
        validateRomByte(velocityZ, "Hinox Link Z velocity");
        validateRomByte(airborneState, "Hinox Link linkAirborne.airborne state");
        setPixelPosition((romPositionX - 0x08) & 0xFF,
            (romPositionY - 0x10) & 0xFF);
        linkStateMachine.romInteractiveMotionBlocked = motionBlocked;
        if (applyHeldLinkPose) {
            romAnimationStateOverride = 0x6A;
        }
        movingThisFrame = false;
        if (applyHeldLinkPose) {
            linkAirborne.zSubPixels = (positionZ & 0xFF) << SUB_PIXEL_SHIFT;
        }
        linkAirborne.zVelocity = velocityZ & 0xFF;
        linkAirborne.airborne = airborneState != 0;
        if (speedX != 0 || speedY != 0) {
            applyRomSpeed(speedX, speedY);
        }
    }

    /** Applies entity-$61 WarpState3Handler's direct Link writes each frame. */
    public void applyWarpState3(int romPositionX, int visualRomPositionY,
                                int immunityCountdown) {
        validateRomByte(romPositionX, "Warp Link X");
        validateRomByte(visualRomPositionY, "Warp Link visual Y");
        validateRomByte(immunityCountdown, "Warp Link immunity");
        setPixelPosition((romPositionX - 0x08) & 0xFF,
            (visualRomPositionY - 0x10) & 0xFF);
        linkStateMachine.romInteractiveMotionBlocked = true;
        romAnimationStateOverride = -1;
        romAttackStepAnimationCountdown = 0;
        linkDamage.collisionIgnoreFramesRemaining = 0;
        movingThisFrame = false;
        linkAirborne.airborne = false;
        linkAirborne.zSubPixels = 0;
        linkAirborne.zVelocity = 0;
        linkAirborne.fallingIntoPit = false;
        linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
        if (immunityCountdown != 0) {
            linkDamage.collisionDamageImmune = true;
        }
        if (playerState != null) {
            playerState.setInvincibilityCounter(0);
            playerState.clearSubtractHealthBuffer();
        }
    }

    /** Mirrors wIsLinkImmuneToCollisionDamage independently of hit flashing. */
    public boolean isCollisionDamageImmune() {
        return linkDamage.collisionDamageImmune;
    }

    /** Applies Marin's final writes when Link jumps out of the bed. */
    public void leaveMarinWakeUpBed() {
        linkStateMachine.motionState = LINK_MOTION_DEFAULT;
        linkStateMachine.romInteractiveMotionBlocked = false;
        romAnimationStateOverride = -1;
        // The entity handler has already drawn the bed on the launch frame.
        // Ordinary Link rendering takes over at the next physics update.
        marinWakeUpBedVariant = 3;
        marinBedJumpMotionLocked = true;
        movingThisFrame = false;
        linkAirborne.airborne = true;
        linkAirborne.zSubPixels = 1 << SUB_PIXEL_SHIFT;
        linkAirborne.zVelocity = 0x12;
        linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
        linkAirborne.fallingIntoPit = false;
        direction = DIRECTION_RIGHT;
        linkMotion.lastRomSpeedX = 0x0C;
        linkMotion.lastRomSpeedY = 0;
        linkMotion.forcedSpeedPending = false;
        linkMotion.forcedSpeedXOnly = false;
    }

    /** Ends Rooster's custom linkAirborne.airborne carry state when it is thrown or cleared. */
    public void clearRoosterCarryState() {
        roosterCarryActive = false;
        linkAirborne.airborne = false;
        linkAirborne.zSubPixels = 0;
        linkAirborne.zVelocity = 0;
        linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
        linkAirborne.fallingIntoPit = false;
        linkStateMachine.motionState = LINK_MOTION_DEFAULT;
        linkStateMachine.physicsModifier = 0;
        linkMotion.forcedSpeedPending = false;
        linkMotion.forcedSpeedXOnly = false;
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

    public void setRomDirection(int romDirection) {
        setDirection(javaDirectionForRomDirection(romDirection));
    }

    /**
     * Applies Tarin's hLinkDirection write and keeps the existing walking phase used by
     * UpdateLinkWalkingAnimation.
     */
    public void faceRomDirectionPreservingWalkPhase(int romDirection) {
        direction = javaDirectionForRomDirection(romDirection);
    }

    public boolean isAirborne() {
        return linkAirborne.airborne;
    }

    public int zVelocity() {
        return linkAirborne.zVelocity & 0xFF;
    }

    public boolean isFallingIntoPit() {
        return linkAirborne.fallingIntoPit;
    }

    @Override
    public void useRocsFeather() {
        if (linkAirborne.airborne || linkSideView.sideViewFeatherPending || linkAirborne.groundStatus == GROUND_STATUS_PIT
            || linkAirborne.fallingIntoPit || isSwimming()) {
            return;
        }
        if (linkSideView.sideScrolling) {
            // The ROM runs $02:68B7 before UseRocsFeather ($00:14E6).
            // Defer its extra XY step until normal side-view motion finishes.
            linkSideView.sideViewFeatherPending = true;
            return;
        }
        topDownFeatherStarted = true;
        linkAirborne.airborne = true;
        linkAirborne.zSubPixels = 0;
        linkAirborne.zVelocity = 0x20;
        jumpAnimationCounter = 0;
        jumpAnimationFrame = 0;
        soundSink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);
    }

    /** Applies the top-down Link bounce written by ApplyLinkCollisionWithEnemy. */
    public void bounceFromGoomba() {
        linkAirborne.airborne = true;
        linkAirborne.zVelocity = 0x10;
        linkAirborne.fallingIntoPit = false;
        linkAirborne.groundStatus = GROUND_STATUS_NORMAL;
        linkSwimming.leaveSwimming();
    }

    /** Advance the walking-cycle timer without processing input or collision. */
    public void update() {
        linkStateMachine.update();
    }

    public void tickAnimation() {
        walkTickCounter++;
        if (walkTickCounter >= WALK_FRAME_TICKS) {
            walkTickCounter = 0;
            walkFrame ^= 1;
        }
    }







    public boolean consumeRevolvingDoorTransition() {
        boolean pending = linkStateMachine.revolvingDoorTransitionPending;
        linkStateMachine.revolvingDoorTransitionPending = false;
        return pending;
    }



    /** Mirrors CheckItemsToUse's two Pegasus-Boots slot branches and UsePegasusBoots. */






    /** Mirrors bank2.asm:74AD's dash collision response and func_020_49BA. */


    static int signedByte(int value) {
        int normalized = value & 0xFF;
        return normalized < 0x80 ? normalized : normalized - 0x100;
    }

    private static void validateRomByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    void tickRomAttackStepAnimationCountdown() {
        if ((romAttackStepAnimationCountdown & ATTACK_STEP_DURATION_MASK) == 0) {
            romAttackStepAnimationCountdown = 0;
        } else {
            romAttackStepAnimationCountdown = (romAttackStepAnimationCountdown - 1) & 0xFF;
        }
    }

    boolean itemsBlockMotion() {
        return itemsBlockMotion(false);
    }

    boolean itemsBlockMotion(boolean beforeTopDownAnimation) {
        if (isLiftTransitionBlockingMotion()) {
            return true;
        }
        if (itemRegistry == null || playerState == null) {
            return false;
        }
        EquipmentItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null && itemBlocksMotion(a, beforeTopDownAnimation)) {
            return true;
        }
        EquipmentItem b = itemRegistry.lookup(playerState.itemB());
        return b != null && itemBlocksMotion(b, beforeTopDownAnimation);
    }

    private static boolean itemBlocksMotion(EquipmentItem item, boolean beforeTopDownAnimation) {
        return beforeTopDownAnimation && item instanceof linksawakening.equipment.Sword sword
            ? sword.blocksTopDownMotion() : item.blocksMotion();
    }

    boolean itemsLockFacing() {
        return itemsLockFacing(false);
    }

    boolean itemsLockFacing(boolean beforeTopDownAnimation) {
        if (itemRegistry == null || playerState == null) {
            return false;
        }
        EquipmentItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null && itemLocksFacing(a, beforeTopDownAnimation)) {
            return true;
        }
        EquipmentItem b = itemRegistry.lookup(playerState.itemB());
        return b != null && itemLocksFacing(b, beforeTopDownAnimation);
    }

    private static boolean itemLocksFacing(EquipmentItem item, boolean beforeTopDownAnimation) {
        return beforeTopDownAnimation && item instanceof linksawakening.equipment.Sword sword
            ? sword.locksTopDownFacing() : item.locksFacing();
    }

    boolean shouldApplyGroundMotion() {
        groundMotionCounter++;
        if (collision == null || !collision.linkOnSlowGround(pixelX(), pixelY())) {
            return true;
        }
        // GROUND_STATUS_SLOW ($03) uses the same frame-counter mask in the
        // original: skip the final position update when frameCounter & 3 == 0.
        return (groundMotionCounter & 0x03) != 0;
    }





    boolean buttonWasPressed(int key) {
        return inputState != null && key >= 0 && inputState.wasPressed(key);
    }

    boolean playerHasFlippers() {
        return playerState != null && playerState.hasFlippers();
    }





    /** $02:68B7–6C74: side-view speeds and single-point collision probes. */








    /**
     * Point-based collision check on the leading edge of movement,
     * mirroring {@code LinkCollisionPointsX/Y} (bank2.asm:6302). Tests
     * two specific points on Link's sprite for the given direction; if
     * either is in a blocked cell, the move is rejected. This is what
     * lets Link step up and out of a scrolled-into-a-tree-row
     * situation — his upper-edge collision points are in the clear row
     * above even while his feet still overlap the tree cells below.
     */










    /** Ports LinkMotionUnstuckingHandler's initial downward search. */






























    int buildJoypadMask() {
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

    static int romDirectionForJavaDirection(int javaDirection) {
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
        int renderY = pixelY() - (bedJumpRenderZ >= 0 ? bedJumpRenderZ : zPixels());
        int originY = renderY + offsetY;
        int[] palette = playerState != null
            && (playerState.invincibilityCounter() & 0x04) != 0
            // DrawLinkSprite's GBC path selects object palette 4 from bit 2
            // of wInvincibilityCounter while Link is flashing.
            ? tunicPalette.forObjectPalette(4)
            : tunicPalette.forTunic(
                playerState == null ? PlayerState.TUNIC_GREEN : playerState.tunicType());

        if (linkStateMachine.revolvingDoorPaletteVariant >= 0 && linkStateMachine.revolvingDoorFrame < 0x38) {
            palette = romTables.revolvingDoorPalette(linkStateMachine.revolvingDoorPaletteVariant);
        }

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
        EquipmentItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null) {
            a.render(displayBuffer, pixelX(), pixelY() - zPixels(), direction, offsetX, offsetY);
        }
        EquipmentItem b = itemRegistry.lookup(playerState.itemB());
        if (b != null && b != a) {
            b.render(displayBuffer, pixelX(), pixelY() - zPixels(), direction, offsetX, offsetY);
        }
        if (swordAcquisitionAnimationState >= 0) {
            // ApplyLinkMotionState draws the sword during the pickup spin
            // before GiveInventoryItem awards it to slot A/B. Reuse the ROM
            // sword handler from the registry, but do not draw it twice if a
            // save already has the sword equipped.
            EquipmentItem acquisitionSword = itemRegistry.lookup(PlayerState.INVENTORY_SWORD);
            if (acquisitionSword != null && acquisitionSword != a && acquisitionSword != b) {
                acquisitionSword.render(displayBuffer, pixelX(), pixelY() - zPixels(),
                    direction, offsetX, offsetY);
            }
        }
    }

    int zPixels() {
        return Math.max(0, linkAirborne.zSubPixels >> SUB_PIXEL_SHIFT);
    }

    public int romAnimationState() {
        return resolveAnimationState();
    }

    private int resolveAnimationState() {
        if (romAnimationStateOverride >= 0) {
            return romAnimationStateOverride;
        }
        if (swordAcquisitionAnimationState >= 0) {
            return swordAcquisitionAnimationState;
        }
        if (linkAirborne.fallingIntoPit) {
            int frame = Math.min(LinkAnimator.FALL_ANIMATION_STATE.length - 1, linkAirborne.fallingFrameCounter / FALL_FRAME_TICKS);
            return LinkAnimator.FALL_ANIMATION_STATE[frame];
        }

        if (linkStateMachine.motionState == LINK_MOTION_SWIMMING) {
            return linkStateMachine.physicsModifier != 0
                ? LinkAnimator.DIVING_ANIMATION_STATE[walkFrame]
                : LinkAnimator.SWIMMING_ANIMATION_STATE[direction][walkFrame];
        }

        // func_002_4338 copies the intermediate ROM carry animation state
        // directly (for example $37/$39/$3B/$3D). The final carry state is
        // the regular two-frame lifting list below.
        if (carryingLiftedObjectState >= 2) {
            return carryingLiftedObjectState;
        }
        // ApplyLinkGroundMotion ($02:4503) sets the step counter to $FF
        // while linkAirborne.airborne, selecting the alternate normal pose after the flip.
        int normalFrame = linkAirborne.airborne ? 1 : walkFrame;
        int baseState = LinkAnimator.ANIMATION_STATE[direction][normalFrame];
        if (carryingLiftedObjectState == 1) {
            return LinkAnimator.LIFTING_ANIMATION_STATE[direction][walkFrame];
        }
        int override = queryOverride(itemInSlotA());
        if (override >= 0) {
            return override;
        }
        override = queryOverride(itemInSlotB());
        if (override >= 0) {
            return override;
        }
        if (linkAirborne.airborne && jumpAnimationFrame < 3 && !linkAirborne.swordSuppressesJumpAnimation()) {
            return LinkAnimator.JUMP_ANIMATION_STATE[direction][jumpAnimationFrame];
        }
        if (spriteSheet != null) {
            int carriedShield = !gelClinging && playerState != null
                && (playerState.itemA() == PlayerState.INVENTORY_SHIELD
                    || playerState.itemB() == PlayerState.INVENTORY_SHIELD)
                        ? playerState.shieldLevel() : 0;
            return spriteSheet.walkingAnimationState(romDirectionForJavaDirection(direction),
                normalFrame, carriedShield, isUsingShield());
        }
        if (isUsingShield() && playerState != null && playerState.shieldLevel() > 0) {
            int[][] shieldTable = playerState.shieldLevel() >= 2
                ? LinkAnimator.MIRROR_SHIELD_USE_ANIMATION_STATE : LinkAnimator.SHIELD_USE_ANIMATION_STATE;
            return shieldTable[direction][normalFrame];
        }
        return baseState;
    }

    boolean isLiftTransitionBlockingMotion() {
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

    private EquipmentItem itemInSlotA() {
        if (itemRegistry == null || playerState == null) {
            return null;
        }
        return itemRegistry.lookup(playerState.itemA());
    }

    private EquipmentItem itemInSlotB() {
        if (itemRegistry == null || playerState == null) {
            return null;
        }
        return itemRegistry.lookup(playerState.itemB());
    }

    private int queryOverride(EquipmentItem item) {
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
