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
import linksawakening.state.LinkPlayerState;

import static linksawakening.entity.LinkRomConstants.*;

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

    final LinkEquipment linkEquipment = new LinkEquipment(this);

    final LinkAnimator linkAnimator = new LinkAnimator(this);

    final LinkEntityEffects linkEntityEffects = new LinkEntityEffects(this);

    final LinkMotion linkMotion = new LinkMotion(this);

    final LinkAirborne linkAirborne = new LinkAirborne(this);

    final LinkSwimming linkSwimming = new LinkSwimming(this);

    final LinkStateMachine linkStateMachine = new LinkStateMachine(this);

    final LinkSideView linkSideView = new LinkSideView(this);

    final LinkDamage linkDamage = new LinkDamage(this);

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

    final InputState inputState;
    final InputConfig inputConfig;
    final RomTables romTables;
    final OverworldCollision collision;
    final LinkSpriteSheet spriteSheet;
    final LinkPlayerState playerState;
    final ItemRegistry itemRegistry;
    final LinkTunicPalette tunicPalette;

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
    private int romLinkPushing;
    /** Mirrors wC10A while Marin's scripted bed-exit jump owns Link motion. */
    /** Mirrors wC191, the collision-probe countdown for directional ledges. */
    /** Mirrors wC10A while a directional ledge jump owns Link's XY speed. */
    /** Mirrors LINK_MOTION_UNSTUCKING after an overworld $10 ledge collision. */
    private long physicsRoomLoadToken = -1;
    private int physicsRoomMapId = -1;
    private int physicsRoomId = -1;
    private int physicsRoomCategory = -1;
    int frameCounter;
    int romAttackStepAnimationCountdown;
    final GameplaySoundSink soundSink;

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
        linkAnimator.romAnimationStateOverride = -1;
        linkAnimator.swordAcquisitionBaseRomDirection = -1;
        linkAnimator.swordAcquisitionAnimationState = -1;
        linkAnimator.tarinShieldPresentation = false;
        linkAnimator.tarinShieldPalette = null;
        linkAnimator.marinWakeUpBedVariant = -1;
        linkEntityEffects.marinBedJumpMotionLocked = false;
        linkAnimator.bedJumpRenderZ = -1;
        linkAirborne.directionalLedgeJumpMotionLocked = false;
        linkAirborne.overworldLedgeFallActive = false;
        linkEntityEffects.likeLikeCaptured = false;
        linkEntityEffects.likeLikeCapturePinsPosition = false;
        linkAirborne.fallingIntoPit = false;
        linkEntityEffects.carryingLiftedObjectState = 0;
        topDownFeatherStarted = false;
        linkEntityEffects.roosterCarryActive = false;
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
        int probeX = linkMotion.pixelX();
        int probeY = linkMotion.pixelY();
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
            linkMotion.subX = (linkMotion.romEntityX() & 0xF0) << SUB_PIXEL_SHIFT;
            linkMotion.subY -= 8 << SUB_PIXEL_SHIFT;
            linkStateMachine.motionState = 0x02;
            linkStateMachine.physicsModifier = 0;
            linkAirborne.overworldLedgeFallActive = true;
            linkMotion.beginOverworldLedgeUnstuckingMotion();
        }
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

    /** Applies the side-view platform handler's Link position and speed writes. */
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY,
                                              int romSpeedY, boolean standing) {
        if (horizontalDelta < -0x80 || horizontalDelta > 0x7F) {
            throw new IllegalArgumentException("Side-view platform X delta must be a signed byte");
        }
        validateRomByte(romPositionY, "Side-view platform Link Y");
        validateRomByte(romSpeedY, "Side-view platform Link Y speed");

        int currentRomX = linkMotion.romEntityX() & 0xFF;
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

    public boolean isTarinShieldPresentationVisible() {
        return linkAnimator.tarinShieldPresentation;
    }

    public int[] tarinShieldPalette() {
        return linkAnimator.tarinShieldPalette == null ? null : linkAnimator.tarinShieldPalette.clone();
    }

    public boolean isMarinWakeUpBedVisible() {
        return linkAnimator.marinWakeUpBedVariant >= 0;
    }

    public int marinWakeUpBedVariant() {
        return linkAnimator.marinWakeUpBedVariant;
    }

    /** ROM hLinkPositionZ used by follower position history and OAM Z state. */
    public int romEntityZ() {
        return zPixels();
    }

    public boolean isLikeLikeCaptured() {
        return linkEntityEffects.likeLikeCaptured;
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
        return hasFacingCollision() ? linkMotion.collisionProbePixelX[direction] : linkMotion.pixelX();
    }

    public int romCollisionProbePixelY() {
        return hasFacingCollision() ? linkMotion.collisionProbePixelY[direction] : linkMotion.pixelY();
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






    /** ROM wC145: the Y origin used by sword collision while linkAirborne.airborne. */
    public int romSwordCollisionY() {
        return linkMotion.romEntityY() - zPixels();
    }

    public int direction() {
        return direction;
    }



    /** Mirrors wIsLinkImmuneToCollisionDamage independently of hit flashing. */
    public boolean isCollisionDamageImmune() {
        return linkDamage.collisionDamageImmune;
    }

    public boolean isRoosterCarryActive() {
        return linkEntityEffects.roosterCarryActive;
    }

    public int carryingLiftedObjectState() {
        return linkEntityEffects.carryingLiftedObjectState;
    }

    public boolean isCarryingLiftedObject() {
        return linkEntityEffects.carryingLiftedObjectState != 0 || linkEntityEffects.roosterCarryActive;
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
        linkAnimator.jumpAnimationCounter = 0;
        linkAnimator.jumpAnimationFrame = 0;
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
    public void apply(LinkCommand command) {
        Objects.requireNonNull(command, "command").applyTo(this);
    }

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

    static void validateRomByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }















    boolean shouldApplyGroundMotion() {
        groundMotionCounter++;
        if (collision == null || !collision.linkOnSlowGround(linkMotion.pixelX(), linkMotion.pixelY())) {
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

    int zPixels() {
        return Math.max(0, linkAirborne.zSubPixels >> SUB_PIXEL_SHIFT);
    }

    private int resolveAnimationState() { return linkAnimator.resolveAnimationState(); }

    public int romAnimationState() {
        return linkAnimator.resolveAnimationState();
    }

    boolean isLiftTransitionBlockingMotion() {
        return linkEntityEffects.carryingLiftedObjectState >= 2 && !linkEntityEffects.roosterCarryActive;
    }

    static int javaDirectionForRomDirection(int romDirection) {
        return switch (romDirection) {
            case 0 -> DIRECTION_RIGHT;
            case 1 -> DIRECTION_LEFT;
            case 2 -> DIRECTION_UP;
            case 3 -> DIRECTION_DOWN;
            default -> throw new AssertionError(romDirection);
        };
    }





    public void setPixelPosition(int pixelX, int pixelY) { linkMotion.setPixelPosition(pixelX, pixelY); }
    public void setRoomEntryPixelPosition(int pixelX, int pixelY) { linkMotion.setRoomEntryPixelPosition(pixelX, pixelY); }
    public void markRoomEntryPosition() { linkMotion.markRoomEntryPosition(); }
    public int roomEntryPixelX() { return linkMotion.roomEntryPixelX(); }
    public int roomEntryPixelY() { return linkMotion.roomEntryPixelY(); }
    public int roomEntryRomPositionX() { return linkMotion.roomEntryRomPositionX(); }
    public int roomEntryRomPositionY() { return linkMotion.roomEntryRomPositionY(); }
    public void setRoomEntryRomPosition(int romPositionX, int romPositionY) { linkMotion.setRoomEntryRomPosition(romPositionX, romPositionY); }
    public void applyRomSpeed(int speedX, int speedY) { linkMotion.applyRomSpeed(speedX, speedY); }
    public void applyRomSpeedX(int speedX) { linkMotion.applyRomSpeedX(speedX); }
    public void applyRomFinalPosition(int speedX, int speedY) { linkMotion.applyRomFinalPosition(speedX, speedY); }
    public void captureRomFinalPosition() { linkMotion.captureRomFinalPosition(); }
    public void restoreRomFinalPosition() { linkMotion.restoreRomFinalPosition(); }
    public void restoreRomFinalPositionX() { linkMotion.restoreRomFinalPositionX(); }
    public void clearRomPositionIncrement() { linkMotion.clearRomPositionIncrement(); }
    public int pixelX() { return linkMotion.pixelX(); }
    public int pixelY() { return linkMotion.pixelY(); }
    public int romEntityX() { return linkMotion.romEntityX(); }
    public int romFinalPositionX() { return linkMotion.romFinalPositionX(); }
    public int romEntityY() { return linkMotion.romEntityY(); }
    public void applyLikeLikeCapture(int romEntityX, int romEntityY) { linkEntityEffects.applyLikeLikeCapture(romEntityX, romEntityY); }
    public void applyLikeLikeCapture(int romEntityX, int romEntityY, boolean pinPosition) { linkEntityEffects.applyLikeLikeCapture(romEntityX, romEntityY, pinPosition); }
    public void releaseLikeLikeCapture() { linkEntityEffects.releaseLikeLikeCapture(); }
    public void setCarryingLiftedObjectState(int carryState, int romDirection) { linkEntityEffects.setCarryingLiftedObjectState(carryState, romDirection); }
    public void applyRoosterFlightState(int positionZ, int velocityZ,
                                        int speedX, int speedY, int romDirection) { linkEntityEffects.applyRoosterFlightState(positionZ, velocityZ, speedX, speedY, romDirection); }
    public void applyHinoxGrabState(int romPositionX, int romPositionY, int positionZ,
                                    int speedX, int speedY, int velocityZ,
                                    int airborneState) { linkEntityEffects.applyHinoxGrabState(romPositionX, romPositionY, positionZ, speedX, speedY, velocityZ, airborneState); }
    public void applyHinoxGrabState(int romPositionX, int romPositionY, int positionZ,
                                    int speedX, int speedY, int velocityZ,
                                    int airborneState, boolean motionBlocked,
                                    boolean applyHeldLinkPose) { linkEntityEffects.applyHinoxGrabState(romPositionX, romPositionY, positionZ, speedX, speedY, velocityZ, airborneState, motionBlocked, applyHeldLinkPose); }
    public void applyWarpState3(int romPositionX, int visualRomPositionY,
                                int immunityCountdown) { linkEntityEffects.applyWarpState3(romPositionX, visualRomPositionY, immunityCountdown); }
    public void leaveMarinWakeUpBed() { linkEntityEffects.leaveMarinWakeUpBed(); }
    public void clearRoosterCarryState() { linkEntityEffects.clearRoosterCarryState(); }
    public void showRomFallenPose() { linkAnimator.showRomFallenPose(); }
    public void showHeldItemPose() { linkAnimator.showHeldItemPose(); }
    public void showPowerUpPickupPose() { linkAnimator.showPowerUpPickupPose(); }
    public void showTarinShieldPresentation(int[] palette) { linkAnimator.showTarinShieldPresentation(palette); }
    public void clearTarinShieldPresentation() { linkAnimator.clearTarinShieldPresentation(); }
    public void showStandingShieldDownPose() { linkAnimator.showStandingShieldDownPose(); }
    public void showSwordAcquisitionSpinPose(int countdown) { linkAnimator.showSwordAcquisitionSpinPose(countdown); }
    public void showSwordAcquisitionFinalPose() { linkAnimator.showSwordAcquisitionFinalPose(); }
    public void showMarinWakeUpBed(int spriteVariant) { linkAnimator.showMarinWakeUpBed(spriteVariant); }
    public void render(byte[] displayBuffer, int offsetX, int offsetY) { linkAnimator.render(displayBuffer, offsetX, offsetY); }
    public void startRomItemAttackStep() { linkEquipment.startRomItemAttackStep(); }
    public void startRomMagicPowderAttackStep() { linkEquipment.startRomMagicPowderAttackStep(); }
    public void startRomMagicRodAttackStep() { linkEquipment.startRomMagicRodAttackStep(); }
    public void clearRomAttackStepAnimationCountdown() { linkEquipment.clearRomAttackStepAnimationCountdown(); }
    public boolean canUseItems() { return linkEquipment.canUseItems(); }
    public void setShieldInputAllowed(BooleanSupplier allowed) { linkEquipment.setShieldInputAllowed(allowed); }
    public boolean isUsingShield() { return linkEquipment.isUsingShield(); }
    public int applyRomItemDirectionFromInput() { return linkEquipment.applyRomItemDirectionFromInput(); }
}
