package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.LinkGroundPhase.*;
import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.state.Direction;
import linksawakening.state.PowerUp;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.room.EntityPickupInputs;
import linksawakening.world.room.SwordBox;
import linksawakening.world.transition.Warp;

/** Link's active-frame work in ROM order, between gating and transition checks. */
final class ActiveLinkPhase {
    private final LinkMotionDriver motion;

    ActiveLinkPhase(LinkMotionDriver motion) {
        this.motion = Objects.requireNonNull(motion);
    }

    void tick(GameplaySimulation context, boolean swordPickupSequenceActive) {
        // AnimateEntities runs after Link motion and its normal NPC
        // handlers restore hLinkFinalPosition when they push Link.
        // The ROM seeds that shadow from hLinkPosition before the
        // frame's motion work (bank0.asm:$0F97), so capture it before
        // any Link movement can change the position.
        configureRoomMotion(context);
        context.link().setGelClinging(context.roomSession().entityState().gelClingingToLink(), context.frameCounter());
        motion.capture(context.link());
        boolean keyholeSequenceActive = context.roomSession().doors().tailCaveKeyholeSequenceActive();
        if (keyholeSequenceActive) {
            context.link().blockNextRomMotionFrame();
        }
        updateLifting(context, keyholeSequenceActive);
        tickEquipment(context, keyholeSequenceActive, swordPickupSequenceActive);
        moveLink(context);
        interactWithDoors(context);
        updateScreenShake(context);
        collectPickups(context);
        context.bush().maybeCutBushWithSword();
        // Warp-transition check runs BEFORE edge-scroll/clamp:
        // indoor rooms clamp Link at y=112 (== ROOM_PIXEL_HEIGHT −
        // SPRITE_SIZE) in the edge-scroll fallback, but the
        // indoor-exit trigger needs to see Link at y > 112 to fire.
        // If we clamp first, Link never crosses the threshold and
        // the south-edge warp never fires.
        // The source performs this ordinary Link check before
        // AnimateEntities; a standing platform handler performs its
        // own explicit check again after moving Link below.
        context.roomTransitionCoordinator().handleWarpAndIndoorBoundaries(context.link());
        context.roomTransitionCoordinator().handleOverworldBoundary(context.link());
    }

    private static void configureRoomMotion(GameplaySimulation context) {
        {
            int roomId = context.roomSession().rooms().currentRoomId();
            context.link().setSideScrolling(context.roomSession().rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL);
            // $02:6CB6's manual exits precede the normal recoil gate.
            boolean sideViewManualExit = context.roomSession().rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL
                    && roomId != 0xE8
                    && roomId != 0xF8;
            boolean caveManualExit =
                    context.roomSession().rooms().mapId() == 0x1F && (roomId == 0xF2 || roomId == 0xF5);
            context.link().setNormalRecoilRoomBoundaries(!sideViewManualExit && !caveManualExit);
        }
    }

    private static void updateLifting(GameplaySimulation context, boolean keyholeSequenceActive) {
        boolean powerBraceletButtonHeld =
                !context.link().recoveryBlocksInput() && context.lift().isPowerBraceletButtonHeld();
        boolean bombButtonHeld =
                !context.link().recoveryBlocksInput() && context.lift().isBombButtonHeld();
        if (!keyholeSequenceActive) {
            context.roomSession().entityState().setEntityPowerBraceletButtonHeld(powerBraceletButtonHeld);
            context.roomSession().entityState().setEntityBombButtonHeld(bombButtonHeld);
            var liftedState = context.roomSession().rooms().liftedEntityState();
            boolean bombBeingThrown = liftedState.type() == 0x02;
            boolean throwPressed =
                    !context.link().recoveryBlocksInput() && context.lift().isThrowButtonPressed(bombBeingThrown);
            if (throwPressed && liftedState.carryState() == 0x01) {
                context.roomSession()
                        .playerItems()
                        .throwLiftedEntity(context.link().direction());
            }
        }
        boolean canGrab = !keyholeSequenceActive
                && !context.link().isAirborne()
                && context.link().romMotionState() == 0
                && !context.playerState().runningWithPegasusBoots()
                && !context.link().isCarryingLiftedObject();
        boolean grabbing = context.roomSession()
                .doors()
                .tryLiftObject(
                        context.link().pixelX(),
                        context.link().pixelY(),
                        context.link().direction(),
                        powerBraceletButtonHeld && canGrab,
                        context.link().romPressedButtonsMask(),
                        context.playerState().activePowerUp() == PowerUp.PIECE_OF_POWER.romValue(),
                        context.playerState().powerBraceletLevel(),
                        context.playerState().activePowerUp() != PowerUp.NONE.romValue());
        context.link()
                .setGrabbingObject(
                        grabbing,
                        (context.link().romPressedButtonsMask()
                                        & switch (Direction.fromJavaOrUnknown(
                                                context.link().direction())) {
                                            case RIGHT -> 2;
                                            case LEFT -> 1;
                                            case UP -> 8;
                                            default -> 4;
                                        })
                                != 0);
    }

    private static void tickEquipment(
            GameplaySimulation context, boolean keyholeSequenceActive, boolean swordPickupSequenceActive) {
        if (!keyholeSequenceActive
                && !context.link().recoveryBlocksInput()
                && !context.link().isCarryingLiftedObject()
                && !context.link().romItemAnimationFrameBlocked()) {
            if (context.link().romMotionState() == 0x05
                    || (context.roomSession().entityState().gelClingingToLink())) {
                context.equipmentController().tickEquippedItemsWithoutInput(context.frameCounter());
            } else {
                if (!swordPickupSequenceActive
                        && ((!context.roomSession().entityState().powerUpPickupSequenceActive()
                                && !context.roomSession().rooms().heartPiecePickupActive())))
                    context.equipmentController().dispatchButtonEdges();
                context.equipmentController().tickEquippedItems(context.frameCounter());
            }
        }
    }

    private void moveLink(GameplaySimulation context) {
        if (context.debugNoClip() && context.debugScroll().tryDebugScreenScroll()) {
            context.link().tickAnimation();
        } else {
            context.link().tickRomLinkPushing();
            boolean groundInteractionBlocked = context.link().romInteractiveMotionBlockValue() != 0;
            boolean wasAirborne = context.link().isAirborne();
            int beforeMotionX = context.link().pixelX(),
                    beforeMotionY = context.link().pixelY();
            motion.update(context.link());
            if (wasAirborne
                    && !context.link().isAirborne()
                    && !context.link().isFallingIntoPit()
                    && context.roomSession().rooms().mapCategory() != Warp.CATEGORY_SIDESCROLL) {
                // ApplyLinkGroundMotion $02:4588 probes the landing tile
                // before func_002_434A applies XY. $00:1794 probes again
                // after XY and copies that first sample at $02:7589.
                context.roomSession()
                        .terrain()
                        .tickCrackedFloorOnLanding(
                                groundFrame(context, beforeMotionX, beforeMotionY, groundInteractionBlocked),
                                dialogGotItemActive(context));
            }
            tickCrackedFloor(context, groundInteractionBlocked);
            var splash = context.link().consumeWaterSplash();
            if (splash != null) {
                // VFX stores ROM/OAM coordinates, as entity VFX requests do.
                context.transientVfxSystem().spawn(TransientVfxType.WATER_SPLASH, splash.romX(), splash.romY());
            }
        }
    }

    private static void interactWithDoors(GameplaySimulation context) {
        {
            int collisionProbeX = context.link().romCollisionProbePixelX();
            int collisionProbeY = context.link().romCollisionProbePixelY();
            context.roomSession()
                    .doors()
                    .tryUnlockTailCaveKeyhole(
                            collisionProbeX,
                            collisionProbeY,
                            context.link().direction(),
                            context.link().romCollisionType(),
                            context.playerState().tailKeyCount() != 0);
            context.roomSession()
                    .doors()
                    .tryUnlockKeyCavernKeyhole(
                            collisionProbeX,
                            collisionProbeY,
                            context.link().direction(),
                            context.link().romCollisionType(),
                            context.playerState().goldenLeavesCount());
            context.roomSession()
                    .doors()
                    .tryUnlockIndoorKeyDoor(
                            collisionProbeX,
                            collisionProbeY,
                            context.link().direction(),
                            context.link().romCollisionType());
            context.roomSession()
                    .doors()
                    .tryInteractWithIndoorBlock(
                            collisionProbeX,
                            collisionProbeY,
                            context.link().direction(),
                            context.link().romCollisionType());
        }
    }

    private static void updateScreenShake(GameplaySimulation context) {
        Link.ScreenShakeRequest pegasusShake = context.link().consumePegasusScreenShakeRequest();
        if (pegasusShake != null) {
            context.scrollController().startScreenShake(pegasusShake.countdown(), pegasusShake.phase());
        }
        {
            context.roomSession()
                    .entityState()
                    .setEntityScreenShakeCountdown(context.scrollController().screenShakeCountdown());
            context.roomSession()
                    .entityState()
                    .setSecretSeashellPegasusCollisionState(
                            context.scrollController().screenShakeCountdown() != 0,
                            context.link().pegasusBootsCollisionCountdown() != 0,
                            context.link().pegasusBootsCollisionPosX(),
                            context.link().pegasusBootsCollisionPosY());
        }
    }

    private static void collectPickups(GameplaySimulation context) {
        {
            context.roomSession()
                    .story()
                    .setChestPlayerLevels(
                            context.playerState().shieldLevel(),
                            context.playerState().swordLevel(),
                            context.playerState().powerBraceletLevel());
            context.roomSession()
                    .entityState()
                    .setToadstoolPlayerState(
                            context.playerState().hasToadstool(),
                            context.playerState().magicPowderCount());
            var sword = context.equipmentController().activeSword();
            var box = sword == null
                    ? Sword.CollisionBox.inactive()
                    : sword.enemyCollisionBox(
                            context.link().romEntityX(),
                            context.link().romSwordCollisionY(),
                            context.link().direction(),
                            context.playerState().runningWithPegasusBoots());
            EntityPickupEvent pickup = context.roomSession()
                    .entities()
                    .collectEntityIfNeeded(new EntityPickupInputs(
                            context.frameCounter(),
                            new EntityPickupInputs.Link(
                                    context.link().romEntityX(),
                                    context.link().romEntityY(),
                                    context.link().isAirborne(),
                                    true,
                                    context.link().direction(),
                                    context.link().romEntityZ()),
                            new SwordBox(box.active(), box.x(), box.width(), box.y(), box.height())));
            if (pickup != null) {
                if (pickup.type() == 0x36) {
                    context.playerState().setActivePowerUp(PowerUp.NONE.romValue());
                    context.musicDirector().playDirectMusic(0x25);
                } else if (pickup.sourceVariant() >= 0) {
                    context.playerState().applyFloatingItemPickup(pickup.type(), pickup.sourceVariant());
                } else {
                    context.playerState().applyEntityPickup(pickup.type());
                }
            }
        }
    }
}
