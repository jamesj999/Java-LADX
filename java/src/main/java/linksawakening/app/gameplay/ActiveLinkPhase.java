package linksawakening.app.gameplay;

import static linksawakening.state.Direction.LEFT;
import static linksawakening.state.Direction.RIGHT;
import static linksawakening.state.Direction.UP;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.gameplay.interaction.BushInteraction;
import linksawakening.app.gameplay.interaction.LiftInteraction;
import linksawakening.app.state.DebugState;
import linksawakening.app.state.FrameClock;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Sword;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

/**
 * Link's active-frame work in ROM order, between gating and transition checks. One instance is retained for
 * the owning session.
 */
public final class ActiveLinkPhase {
    private final BushInteraction bush;
    private final DebugState debugState;
    private final DebugScroll debugScroll;
    private final EquipmentController equipmentController;
    private final FrameClock clock;
    private final FrameInputCache frameInputs;
    private final LiftInteraction lift;
    private final Link playerLink;
    private final MusicDirector musicDirector;
    private final PlayerState playerState;
    private final RoomSession rooms;
    private final RoomTransitionCoordinator roomTransitionCoordinator;
    private final ScrollController scrollController;
    private final TransientVfxSystem transientVfxSystem;
    private final LinkGroundPhase ground;
    private final LinkMotionDriver motion;

    private final Sword.CollisionBox NO_SWORD = Sword.CollisionBox.inactive();

    public ActiveLinkPhase(Inputs inputs) {
        this.bush = Objects.requireNonNull(inputs.bush);
        this.debugState = Objects.requireNonNull(inputs.debugNoClip);
        this.debugScroll = Objects.requireNonNull(inputs.debugScroll);
        this.equipmentController = Objects.requireNonNull(inputs.equipmentController);
        this.clock = Objects.requireNonNull(inputs.frameCounter);
        this.frameInputs = Objects.requireNonNull(inputs.frameInputs);
        this.lift = Objects.requireNonNull(inputs.lift);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.musicDirector = Objects.requireNonNull(inputs.musicDirector);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        this.roomTransitionCoordinator = Objects.requireNonNull(inputs.roomTransitionCoordinator);
        this.scrollController = Objects.requireNonNull(inputs.scrollController);
        this.transientVfxSystem = Objects.requireNonNull(inputs.transientVfxSystem);
        this.ground = Objects.requireNonNull(inputs.ground);
        this.motion = Objects.requireNonNull(inputs.motion);
    }

    public void tick(boolean swordPickupSequenceActive) {
        // AnimateEntities runs after Link motion and its normal NPC
        // handlers restore hLinkFinalPosition when they push Link.
        // The ROM seeds that shadow from hLinkPosition before the
        // frame's motion work (bank0.asm:$0F97), so capture it before
        // any Link movement can change the position.
        configureRoomMotion();
        playerLink.motion().setGelClinging(this.rooms.entityState().gelClingingToLink(), this.clock.value());
        this.motion.capture(this.playerLink);
        if (playerLink.airborne().warpHoleControlsLink()) {
            motion.update(playerLink);
            roomTransitionCoordinator.handleWarpAndIndoorBoundaries(playerLink);
            return;
        }
        boolean keyholeSequenceActive = this.rooms.doors().tailCaveKeyholeSequenceActive();
        if (keyholeSequenceActive) {
            this.playerLink.motion().blockNextRomMotionFrame();
        }
        updateLifting(keyholeSequenceActive);
        Sword sword = this.equipmentController.activeSword();
        boolean swordIdle = sword != null && sword.state() == Sword.STATE_NONE;
        Sword updatedSword = tickEquipment(sword, keyholeSequenceActive, swordPickupSequenceActive);
        moveLink(updatedSword);
        if (swordIdle
                && updatedSword != null
                && updatedSword.state() == Sword.STATE_DRAW
                && !playerLink.airborne().isAirborne()) {
            // UseSword ($00:1556) clears XY speed after the current motion step.
            playerLink.position().clearRomPositionIncrement();
        }
        interactWithDoors();
        updateScreenShake();
        collectPickups();
        this.bush.maybeCutBushWithSword();
        // Warp-transition check runs BEFORE edge-scroll/clamp:
        // indoor rooms clamp Link at y=112 (== ROOM_PIXEL_HEIGHT −
        // SPRITE_SIZE) in the edge-scroll fallback, but the
        // indoor-exit trigger needs to see Link at y > 112 to fire.
        // If we clamp first, Link never crosses the threshold and
        // the south-edge warp never fires.
        // The source performs this ordinary Link check before
        // AnimateEntities; a standing platform handler performs its
        // own explicit check again after moving Link below.
        this.roomTransitionCoordinator.handleWarpAndIndoorBoundaries(this.playerLink);
        this.roomTransitionCoordinator.handleOverworldBoundary(this.playerLink);
    }

    private void configureRoomMotion() {
        {
            int roomId = this.rooms.rooms().currentRoomId();
            playerLink
                    .airborne()
                    .setOverworldWarpHoleRoom(rooms.rooms().mapCategory() == Warp.CATEGORY_OVERWORLD
                            && Warp.isOverworldWarpHoleRoom(roomId));
            playerLink.sideView().setSideScrolling(this.rooms.rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL);
            // $02:6CB6's manual exits precede the normal recoil gate.
            boolean sideViewManualExit =
                    this.rooms.rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL && roomId != 0xE8 && roomId != 0xF8;
            boolean caveManualExit = this.rooms.rooms().mapId() == 0x1F && (roomId == 0xF2 || roomId == 0xF5);
            this.playerLink.status().setNormalRecoilRoomBoundaries(!sideViewManualExit && !caveManualExit);
        }
    }

    private void updateLifting(boolean keyholeSequenceActive) {
        boolean powerBraceletButtonHeld =
                !this.playerLink.status().recoveryBlocksInput() && this.lift.isPowerBraceletButtonHeld();
        boolean bombButtonHeld = !this.playerLink.status().recoveryBlocksInput() && this.lift.isBombButtonHeld();
        if (!keyholeSequenceActive) {
            this.rooms.entityState().setEntityPowerBraceletButtonHeld(powerBraceletButtonHeld);
            this.rooms.entityState().setEntityBombButtonHeld(bombButtonHeld);
            var liftedState = this.rooms.rooms().liftedEntityState();
            boolean bombBeingThrown = liftedState.type() == 0x02;
            boolean throwPressed =
                    !this.playerLink.status().recoveryBlocksInput() && this.lift.isThrowButtonPressed(bombBeingThrown);
            if (throwPressed && liftedState.carryState() == 0x01) {
                rooms.playerItems()
                        .throwLiftedEntity(
                                Direction.fromJava(this.playerLink.animation().direction()));
            }
        }
        boolean canGrab = !keyholeSequenceActive
                && !this.playerLink.airborne().isAirborne()
                && this.playerLink.airborne().romMotionState() == 0
                && !this.playerState.equipmentLevels().runningWithPegasusBoots()
                && !this.playerLink.actions().isCarryingLiftedObject();
        boolean grabbing = rooms.doors()
                .tryLiftObject(
                        this.playerLink.position().pixelX(),
                        this.playerLink.position().pixelY(),
                        Direction.fromJavaOrUnknown(this.playerLink.animation().direction()),
                        powerBraceletButtonHeld && canGrab,
                        this.playerLink.controls().romPressedButtonsMask(),
                        this.playerState.equipmentLevels().activePowerUp() == PowerUp.PIECE_OF_POWER.romValue(),
                        this.playerState.equipmentLevels().powerBraceletLevel(),
                        this.playerState.equipmentLevels().activePowerUp() != PowerUp.NONE.romValue());
        playerLink
                .actions()
                .setGrabbingObject(
                        grabbing,
                        (this.playerLink.controls().romPressedButtonsMask()
                                        & switch (Direction.fromJavaOrUnknown(
                                                playerLink.animation().direction())) {
                                            case RIGHT -> 2;
                                            case LEFT -> 1;
                                            case UP -> 8;
                                            default -> 4;
                                        })
                                != 0);
    }

    private Sword tickEquipment(Sword sword, boolean keyholeSequenceActive, boolean swordPickupSequenceActive) {
        if (keyholeSequenceActive
                || this.playerLink.status().recoveryBlocksInput()
                || this.playerLink.actions().isCarryingLiftedObject()
                || this.playerLink.sideView().romItemAnimationFrameBlocked()) {
            return null;
        }
        if (this.playerLink.airborne().romMotionState() == 0x05
                || this.rooms.entityState().gelClingingToLink()) {
            this.equipmentController.tickEquippedItemsWithoutInput(this.clock.value());
        } else {
            if (!swordPickupSequenceActive
                    && (!this.rooms.entityState().powerUpPickupSequenceActive()
                            && !this.rooms.rooms().heartPiecePickupActive()))
                this.equipmentController.dispatchButtonEdges();
            this.equipmentController.tickEquippedItems(this.clock.value());
        }
        return sword;
    }

    private void moveLink(Sword updatedSword) {
        if (this.debugState.debugNoClip() && this.debugScroll.tryDebugScreenScroll()) {
            this.playerLink.animation().tickAnimation();
        } else {
            this.playerLink.equipment().tickRomLinkPushing();
            boolean groundInteractionBlocked = this.playerLink.motion().romInteractiveMotionBlockValue() != 0;
            boolean wasAirborne = this.playerLink.airborne().isAirborne();
            int beforeMotionX = this.playerLink.position().pixelX(),
                    beforeMotionY = this.playerLink.position().pixelY();
            this.motion.update(this.playerLink);
            if (wasAirborne
                    && !this.playerLink.airborne().isAirborne()
                    && !this.playerLink.airborne().isFallingIntoPit()
                    && this.rooms.rooms().mapCategory() != Warp.CATEGORY_SIDESCROLL) {
                // ApplyLinkGroundMotion $02:4588 probes the landing tile
                // before func_002_434A applies XY. $00:1794 probes again
                // after XY and copies that first sample at $02:7589.
                rooms.terrain()
                        .tickCrackedFloorOnLanding(
                                this.ground.groundFrame(beforeMotionX, beforeMotionY, groundInteractionBlocked),
                                this.ground.dialogGotItemActive());
            }
            // UpdateLinkAnimation ($02:4807) sets the block before the post-motion
            // ground check ($02:7851), even if this tick advances the sword out of its swing.
            boolean swordBlocksGroundCheck = updatedSword != null
                    && updatedSword.blocksMotionAfterAnimation(
                            this.playerState.equipmentLevels().runningWithPegasusBoots());
            this.ground.tickCrackedFloor(groundInteractionBlocked || swordBlocksGroundCheck);
            var splash = this.playerLink.status().consumeWaterSplash();
            if (splash != null) {
                // VFX stores ROM/OAM coordinates, as entity VFX requests do.
                this.transientVfxSystem.spawn(TransientVfxType.WATER_SPLASH, splash.romX(), splash.romY());
            }
        }
    }

    private void interactWithDoors() {
        {
            int collisionProbeX = this.playerLink.position().romCollisionProbePixelX();
            int collisionProbeY = this.playerLink.position().romCollisionProbePixelY();
            rooms.doors()
                    .tryUnlockTailCaveKeyhole(
                            collisionProbeX,
                            collisionProbeY,
                            Direction.fromJavaOrUnknown(
                                    this.playerLink.animation().direction()),
                            this.playerLink.position().romCollisionType(),
                            this.playerState.progression().tailKeyCount() != 0);
            rooms.doors()
                    .tryUnlockKeyCavernKeyhole(
                            collisionProbeX,
                            collisionProbeY,
                            Direction.fromJavaOrUnknown(
                                    this.playerLink.animation().direction()),
                            this.playerLink.position().romCollisionType(),
                            this.playerState.progression().goldenLeavesCount());
            rooms.doors()
                    .tryUnlockIndoorKeyDoor(
                            collisionProbeX,
                            collisionProbeY,
                            Direction.fromJavaOrUnknown(
                                    this.playerLink.animation().direction()),
                            this.playerLink.position().romCollisionType());
            rooms.doors()
                    .tryInteractWithIndoorBlock(
                            collisionProbeX,
                            collisionProbeY,
                            Direction.fromJavaOrUnknown(
                                    this.playerLink.animation().direction()),
                            this.playerLink.position().romCollisionType());
        }
    }

    private void updateScreenShake() {
        Link.ScreenShakeRequest pegasusShake = this.playerLink.motion().consumePegasusScreenShakeRequest();
        if (pegasusShake != null) {
            this.scrollController.startScreenShake(pegasusShake.countdown(), pegasusShake.phase());
        }
        {
            rooms.entityState().setEntityScreenShakeCountdown(this.scrollController.screenShakeCountdown());
            rooms.entityState()
                    .setSecretSeashellPegasusCollisionState(
                            this.scrollController.screenShakeCountdown() != 0,
                            this.playerLink.motion().pegasusBootsCollisionCountdown() != 0,
                            this.playerLink.motion().pegasusBootsCollisionPosX(),
                            this.playerLink.motion().pegasusBootsCollisionPosY());
        }
    }

    private void collectPickups() {
        {
            rooms.story()
                    .setChestPlayerLevels(
                            playerState.equipmentLevels().shieldLevel(),
                            playerState.equipmentLevels().swordLevel(),
                            playerState.equipmentLevels().powerBraceletLevel());
            rooms.entityState()
                    .setToadstoolPlayerState(
                            this.playerState.ammunition().hasToadstool(),
                            this.playerState.ammunition().magicPowderCount());
            var sword = this.equipmentController.activeSword();
            var box = sword == null
                    ? NO_SWORD
                    : sword.enemyCollisionBox(
                            this.playerLink.position().romEntityX(),
                            this.playerLink.position().romSwordCollisionY(),
                            this.playerLink.animation().direction(),
                            playerState.equipmentLevels().runningWithPegasusBoots());
            EntityPickupEvent pickup = rooms.entities().collectEntityIfNeeded(this.frameInputs.pickup(box));
            if (pickup != null) {
                if (pickup.type() == 0x36) {
                    this.playerState.equipmentLevels().setActivePowerUp(PowerUp.NONE.romValue());
                    this.musicDirector.playDirectMusic(0x25);
                } else if (pickup.sourceVariant() >= 0) {
                    playerState.playerRewards().applyFloatingItemPickup(pickup.type(), pickup.sourceVariant());
                } else {
                    this.playerState.playerRewards().applyEntityPickup(pickup.type());
                }
            }
        }
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private BushInteraction bush;
        private DebugState debugNoClip;
        private DebugScroll debugScroll;
        private EquipmentController equipmentController;
        private FrameClock frameCounter;
        private FrameInputCache frameInputs;
        private LiftInteraction lift;
        private Link link;
        private MusicDirector musicDirector;
        private PlayerState playerState;
        private RoomSession roomSession;
        private RoomTransitionCoordinator roomTransitionCoordinator;
        private ScrollController scrollController;
        private TransientVfxSystem transientVfxSystem;
        private LinkGroundPhase ground;
        private LinkMotionDriver motion;

        public Inputs bush(BushInteraction value) {
            this.bush = value;
            return this;
        }

        public Inputs debugNoClip(DebugState value) {
            this.debugNoClip = value;
            return this;
        }

        public Inputs debugScroll(DebugScroll value) {
            this.debugScroll = value;
            return this;
        }

        public Inputs equipmentController(EquipmentController value) {
            this.equipmentController = value;
            return this;
        }

        public Inputs frameCounter(FrameClock value) {
            this.frameCounter = value;
            return this;
        }

        public Inputs frameInputs(FrameInputCache value) {
            this.frameInputs = value;
            return this;
        }

        public Inputs lift(LiftInteraction value) {
            this.lift = value;
            return this;
        }

        public Inputs link(Link value) {
            this.link = value;
            return this;
        }

        public Inputs musicDirector(MusicDirector value) {
            this.musicDirector = value;
            return this;
        }

        public Inputs playerState(PlayerState value) {
            this.playerState = value;
            return this;
        }

        public Inputs roomSession(RoomSession value) {
            this.roomSession = value;
            return this;
        }

        public Inputs roomTransitionCoordinator(RoomTransitionCoordinator value) {
            this.roomTransitionCoordinator = value;
            return this;
        }

        public Inputs scrollController(ScrollController value) {
            this.scrollController = value;
            return this;
        }

        public Inputs transientVfxSystem(TransientVfxSystem value) {
            this.transientVfxSystem = value;
            return this;
        }

        public Inputs ground(LinkGroundPhase value) {
            this.ground = value;
            return this;
        }

        public Inputs motion(LinkMotionDriver value) {
            this.motion = value;
            return this;
        }
    }
}
