package linksawakening.entity;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
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

    final LinkAnimator linkAnimator = new LinkAnimator(this);

    final LinkSideView linkSideView = new LinkSideView(this);

    final LinkStateMachine linkStateMachine = new LinkStateMachine(this);

    final LinkEquipment linkEquipment = new LinkEquipment(this);

    final LinkEntityEffects linkEntityEffects = new LinkEntityEffects(this);

    final LinkMotion linkMotion = new LinkMotion(this);

    final LinkAirborne linkAirborne = new LinkAirborne(this);

    final LinkSwimming linkSwimming = new LinkSwimming(this);

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
    public static final int LINK_MOTION_PASS_OUT = 0x07;
    public static final int LINK_MOTION_SWIMMING = 0x01;
    public static final int LINK_MOTION_RECOVER = 0x08;

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
    private int worldMotionFrame = -1;
    int romConsecutiveStepsCount = 7;
    private boolean grassGroundEffectActive;
    boolean topDownFeatherStarted;

    int romLinkPushing;
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

    public boolean debugNoClip() {
        return linkStateMachine.debugNoClip;
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

    /** Returns whether Link stood on a side-view entity during the current frame. */
    public boolean standingOnSideViewEntity() {
        return linkSideView.standingOnSideViewEntity;
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
    /** wC167: suppress menus and CheckItemsToUse during recovery and falling. */
    public boolean recoveryBlocksInput() {
        return linkStateMachine.recoveringInWater || linkAirborne.fallingIntoPit || pitWarpPending;
    }

    boolean pitWarpPending;
    public boolean consumePitWarp() {
        boolean result = pitWarpPending;
        pitWarpPending = false;
        return result;
    }
    public boolean pitWarpPending() { return pitWarpPending; }
    /** ApplyCollisionWithDoor requested a fade during a movement probe. */
    public boolean consumeDoorWarp() {
        boolean pending = linkMotion.doorWarpPending;
        linkMotion.doorWarpPending = false;
        return pending;
    }
    public void setPitWarpArrival(int z) {
        linkStateMachine.motionState = LINK_MOTION_DEFAULT;
        linkAirborne.zSubPixels = z << SUB_PIXEL_SHIFT;
        linkAirborne.zVelocity = 0;
        linkAirborne.airborne = z != 0;
        linkAirborne.spinningPitWarpArrival = z != 0;
    }

    public record WaterSplash(int romX, int romY) {}
    WaterSplash pendingWaterSplash;

    public WaterSplash consumeWaterSplash() {
        WaterSplash result = pendingWaterSplash;
        pendingWaterSplash = null;
        return result;
    }
    private int passOutCountdown = -1;
    private int passOutPaletteEffect;
    public boolean isPassingOut() { return romMotionState() == LINK_MOTION_PASS_OUT; }
    public boolean gameOverScreenPending() { return isPassingOut() && passOutCountdown == 0; }
    private int gameOverCountdown;
    public int gameOverStage() { return isPassingOut() ? romPhysicsModifier() : 0; }
    public int gameOverCountdown() { return gameOverCountdown; }
    public void presentGameOverStage(int stage, int countdown) {
        linkStateMachine.physicsModifier = stage;
        gameOverCountdown = countdown;
    }
    public int passOutCountdown() { return passOutCountdown; }
    /** wBGPaletteEffectAddress is held at $10 during the pass-out animation. */
    public int passOutBgPaletteEffectAddress() { return isPassingOut() && !gameOverScreenPending() ? 0x10 : 0; }
    public int passOutPaletteEffect() { return passOutPaletteEffect; }
    public int passOutPaletteFrames() { return isPassingOut() ? 192 - passOutCountdown : 0; }
    public void finishGameOver() {
        linkStateMachine.resetTransientStateForDebug();
        passOutCountdown = -1;
        passOutPaletteEffect = 0;
        gameOverCountdown = 0;
    }
    public void beginPassOut() {
        linkStateMachine.motionState = LINK_MOTION_PASS_OUT;
        linkStateMachine.physicsModifier = 0;
        if (playerState != null) playerState.setInvincibilityCounter(0);
        linkDamage.collisionIgnoreFramesRemaining = 0;
        linkMotion.forcedSpeedPending = false;
        linkMotion.lastRomSpeedX = 0;
        linkMotion.lastRomSpeedY = 0;
        movingThisFrame = false;
    }
    public void presentPassOut(int countdown, int animationState, int paletteEffect) {
        passOutCountdown = countdown;
        passOutPaletteEffect = paletteEffect;
        linkAnimator.romAnimationStateOverride = animationState;
    }

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
        return linkStateMachine.pegasusBootsChargeMeter & 0xFF;
    }

    /** Mirrors wPegasusBootsCollisionCountdown. */
    public int pegasusBootsCollisionCountdown() {
        return linkStateMachine.pegasusBootsCollisionCountdown & 0xFF;
    }

    /** Mirrors wPegasusBootsCollisionPosX. */
    public int pegasusBootsCollisionPosX() {
        return linkStateMachine.pegasusBootsCollisionPosX & 0xFF;
    }

    /** Mirrors wPegasusBootsCollisionPosY. */
    public int pegasusBootsCollisionPosY() {
        return linkStateMachine.pegasusBootsCollisionPosY & 0xFF;
    }

    /** A screen-shake write emitted by the ROM's Pegasus collision handler. */
    public record ScreenShakeRequest(int countdown, int phase) {
    }

    /** Previous entity pass's Gel state and current world frame for motion gates. */
    public void setGelClinging(boolean clinging, int worldFrame) {
        linkStateMachine.gelClinging = clinging;
        linkStateMachine.gelMotionFrame = worldFrame & 0xFF;
        worldMotionFrame = worldFrame & 0xFF;
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

    /** Apply one complete external Link effect. */
    public void apply(LinkCommand command) {
        Objects.requireNonNull(command, "command").applyTo(this);
    }

    public void update() {
        linkStateMachine.update();
        linkDamage.applySpikeGroundPhysics();
        // ApplyLinkGroundPhysics uses the completed motion position. Dialogs
        // and scrolling reuse this result while ordinary Link updates pause.
        grassGroundEffectActive = collision != null && !linkSideView.sideScrolling
            && !linkAirborne.airborne && !linkAirborne.fallingIntoPit
            && linkStateMachine.motionState != 0x02 && romEntityY() < 0x88
            && collision.objectPhysicsFlagAtPoint(pixelX() + 8, pixelY() + 12)
                == linksawakening.physics.PhysicsFlags.GRASS;
    }

    public boolean grassGroundEffectActive() { return grassGroundEffectActive; }
    public boolean grassGroundEffectFlipX() { return (romConsecutiveStepsCount & 8) != 0; }
    public int romConsecutiveStepsCount() { return romConsecutiveStepsCount; }
    void clearGroundEffects() { grassGroundEffectActive = false; }

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
        // original: $02:44A9 skips the final position update when the global
        // hFrameCounter & 3 == 0, including after dialogs and room scrolling.
        // Isolated Link callers without a session clock retain their local tick.
        int frame = worldMotionFrame >= 0 ? worldMotionFrame : groundMotionCounter;
        return (frame & 0x03) != 0;
    }

    boolean buttonWasPressed(int key) {
        return inputState != null && key >= 0 && inputState.wasPressed(key);
    }

    boolean playerHasFlippers() {
        return playerState != null && playerState.hasFlippers();
    }

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
    /** Records a scroll landing while retaining the source fractional accumulators. */
    public void setRoomScrollEntryPixelPosition(int pixelX, int pixelY) { linkMotion.setRoomScrollEntryPixelPosition(pixelX, pixelY); }

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
    public void setDebugNoClip(boolean enabled) { linkStateMachine.setDebugNoClip(enabled); }
    public void resetTransientStateForDebug() { linkStateMachine.resetTransientStateForDebug(); }
    public void finishRoomScroll(boolean downward) { linkStateMachine.finishRoomScroll(downward); }
    /** Raw source write, distinct from internal sword/presentation movement locks. */
    public void writeRomInteractiveMotionBlock(int value) { linkStateMachine.writeRomInteractiveMotionBlock(value); }
    public int romInteractiveMotionBlockValue() { return linkStateMachine.romInteractiveMotionBlockValue; }
    public int romSlowWalkingSpeed() { return linkStateMachine.slowWalkingSpeed; }
    public int romItemUsageContext() { return linkStateMachine.itemUsageContext; }
    /** Bank2:42B2 skips top-down item dispatch and animation only for the exact value 2. */
    public boolean romItemAnimationFrameBlocked() {
        return !linkSideView.sideScrolling && romInteractiveMotionBlockValue() == 2;
    }
    public void setGrabbingObject(boolean grabbing, boolean pulling) {
        linkEntityEffects.grabbingObject = grabbing;
        linkEntityEffects.pullingObject = grabbing && pulling;
    }
    public void blockNextRomMotionFrame() { linkStateMachine.blockNextRomMotionFrame(); }
    public void initializeRoomPhysicsForRoom(long loadToken, int mapId, int roomId,
                                             int mapCategory) { linkStateMachine.initializeRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory); }
    public void setRoomPhysicsForRoom(long loadToken, int mapId, int roomId,
                                      int mapCategory) { linkStateMachine.setRoomPhysicsForRoom(loadToken, mapId, roomId, mapCategory); }
    public void setRomPhysicsModifier(int modifier) { linkStateMachine.setRomPhysicsModifier(modifier); }
    public ScreenShakeRequest consumePegasusScreenShakeRequest() { return linkStateMachine.consumePegasusScreenShakeRequest(); }
    public void resetPegasusBoots() { linkStateMachine.resetPegasusBoots(); }
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY,
                                              int romSpeedY, boolean standing) { linkSideView.applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY, standing); }
    public void applySideViewPlatformContact(int horizontalDelta, int romPositionY,
                                              int romSpeedY) { linkSideView.applySideViewPlatformContact(horizontalDelta, romPositionY, romSpeedY); }
    public void clearSideViewPlatformState() { linkSideView.clearSideViewPlatformState(); }
    public void clearStandingOnSideViewEntity() { linkSideView.clearStandingOnSideViewEntity(); }
    public void applySideViewPotContact(int romPositionY, int romSpeedY) { linkSideView.applySideViewPotContact(romPositionY, romSpeedY); }
    public void submitOam(linksawakening.render.OamSink sink, int offsetX, int offsetY) { linkAnimator.submitOam(sink, offsetX, offsetY); }
}
