package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.state.PlayerState;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.Warp;

final class LinkEquipmentPhase {
    void tick(GameplaySimulation context, boolean dialogBlocksGameplay, boolean inventoryBlockedAtStart) {
        if (!dialogBlocksGameplay) {
            {
                context.playerState().tickResourceBuffers(context.frameCounter());
                var resourceSound = context.playerState().takeResourceSound();
                if (resourceSound != null) context.gameplaySoundSink().play(resourceSound);
            }
            {
                context.transientVfxSystem().tick();
            }
            {
                context.droppableRupeeSystem()
                        .tick(context.link().pixelX(), context.link().pixelY(), context.playerState());
            }
        }

        // ApplyGotItem $00:0FC7 runs before normal Link/item work. A
        // toadstool requested by that work starts decrementing next frame.
        {
            int itemDialog = context.link().tickDialogGotItem(dialogBlocksGameplay);
            if (itemDialog == 2) {
                context.dialogController()
                        .openPreformattedForLinkY(
                                context.dialogTextLoader().load(new SignpostDialogRef(0, 0x0E)),
                                LinkDialogPosition.dialogYFromTopLeft(
                                        context.link().pixelY()));
            }
            if (context.link().presentedDialogGotItem() == 2) {
                context.gpu().updatePowderInventoryTiles(context.rom().unsafeBytes(), true);
            }
        }

        if (context.passOutController().tick(context, dialogBlocksGameplay)) return;

        // LinkMotionMapFadeInHandler ($00:19DE-$00:1A21) restores normal
        // motion and returns; held movement starts on the following frame.
        boolean linkActive = !inventoryBlockedAtStart
                && !context.scrollController().isActive()
                && !context.transitionController().isInputBlocked()
                && !context.inventoryController().shouldBlockOverworldInput()
                && !dialogBlocksGameplay
                && (!context.link().dialogGotItemActive())
                && (!context.roomSession().gotItemPresentationActive());
        boolean swordPickupSequenceActive = context.roomSession().swordPickupSequenceActive();
        linkActive = linkActive && !swordPickupSequenceActive;
        if (linkActive) {
            // AnimateEntities runs after Link motion and its normal NPC
            // handlers restore hLinkFinalPosition when they push Link.
            // The ROM seeds that shadow from hLinkPosition before the
            // frame's motion work (bank0.asm:$0F97), so capture it before
            // any Link movement can change the position.
            {
                int roomId = context.roomSession().currentRoomId();
                context.link().setSideScrolling(context.roomSession().mapCategory() == Warp.CATEGORY_SIDESCROLL);
                // $02:6CB6's manual exits precede the normal recoil gate.
                boolean sideViewManualExit = context.roomSession().mapCategory() == Warp.CATEGORY_SIDESCROLL
                        && roomId != 0xE8
                        && roomId != 0xF8;
                boolean caveManualExit = context.roomSession().mapId() == 0x1F && (roomId == 0xF2 || roomId == 0xF5);
                context.link().setNormalRecoilRoomBoundaries(!sideViewManualExit && !caveManualExit);
            }
            context.link().setGelClinging(context.roomSession().gelClingingToLink(), context.frameCounter());
            context.link().captureRomFinalPosition();
            boolean keyholeSequenceActive = context.roomSession().tailCaveKeyholeSequenceActive();
            if (keyholeSequenceActive) {
                context.link().blockNextRomMotionFrame();
            }
            boolean powerBraceletButtonHeld =
                    !context.link().recoveryBlocksInput() && context.lift().isPowerBraceletButtonHeld();
            boolean bombButtonHeld =
                    !context.link().recoveryBlocksInput() && context.lift().isBombButtonHeld();
            if (!keyholeSequenceActive) {
                context.roomSession().setEntityPowerBraceletButtonHeld(powerBraceletButtonHeld);
                context.roomSession().setEntityBombButtonHeld(bombButtonHeld);
                var liftedState = context.roomSession().liftedEntityState();
                boolean bombBeingThrown = liftedState.type() == 0x02;
                boolean throwPressed =
                        !context.link().recoveryBlocksInput() && context.lift().isThrowButtonPressed(bombBeingThrown);
                if (throwPressed && liftedState.carryState() == 0x01) {
                    context.roomSession().throwLiftedEntity(context.link().direction());
                }
            }
            boolean canGrab = !keyholeSequenceActive
                    && !context.link().isAirborne()
                    && context.link().romMotionState() == 0
                    && !context.playerState().runningWithPegasusBoots()
                    && !context.link().isCarryingLiftedObject();
            boolean grabbing = context.roomSession()
                    .tryLiftObject(
                            context.link().pixelX(),
                            context.link().pixelY(),
                            context.link().direction(),
                            powerBraceletButtonHeld && canGrab,
                            context.link().romPressedButtonsMask(),
                            context.playerState().activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER,
                            context.playerState().powerBraceletLevel(),
                            context.playerState().activePowerUp() != PlayerState.ACTIVE_POWER_UP_NONE);
            context.link()
                    .setGrabbingObject(
                            grabbing,
                            (context.link().romPressedButtonsMask()
                                            & switch (context.link().direction()) {
                                                case Link.DIRECTION_RIGHT -> 2;
                                                case Link.DIRECTION_LEFT -> 1;
                                                case Link.DIRECTION_UP -> 8;
                                                default -> 4;
                                            })
                                    != 0);
            if (!keyholeSequenceActive
                    && !context.link().recoveryBlocksInput()
                    && !context.link().isCarryingLiftedObject()
                    && !context.link().romItemAnimationFrameBlocked()) {
                if (context.link().romMotionState() == 0x05
                        || (context.roomSession().gelClingingToLink())) {
                    context.equipmentController().tickEquippedItemsWithoutInput(context.frameCounter());
                } else {
                    if (!swordPickupSequenceActive
                            && ((!context.roomSession().powerUpPickupSequenceActive()
                                    && !context.roomSession().heartPiecePickupActive())))
                        context.equipmentController().dispatchButtonEdges();
                    context.equipmentController().tickEquippedItems(context.frameCounter());
                }
            }
            if (context.debugNoClip() && context.debugScroll().getAsBoolean()) {
                context.link().tickAnimation();
            } else {
                context.link().tickRomLinkPushing();
                boolean groundInteractionBlocked = context.link().romInteractiveMotionBlockValue() != 0;
                boolean wasAirborne = context.link().isAirborne();
                int beforeMotionX = context.link().pixelX(),
                        beforeMotionY = context.link().pixelY();
                context.link().update();
                if (wasAirborne
                        && !context.link().isAirborne()
                        && !context.link().isFallingIntoPit()
                        && context.roomSession().mapCategory() != Warp.CATEGORY_SIDESCROLL) {
                    // ApplyLinkGroundMotion $02:4588 probes the landing tile
                    // before func_002_434A applies XY. $00:1794 probes again
                    // after XY and copies that first sample at $02:7589.
                    context.roomSession()
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
            {
                int collisionProbeX = context.link().romCollisionProbePixelX();
                int collisionProbeY = context.link().romCollisionProbePixelY();
                context.roomSession()
                        .tryUnlockTailCaveKeyhole(
                                collisionProbeX,
                                collisionProbeY,
                                context.link().direction(),
                                context.link().romCollisionType(),
                                context.playerState().tailKeyCount() != 0);
                context.roomSession()
                        .tryUnlockKeyCavernKeyhole(
                                collisionProbeX,
                                collisionProbeY,
                                context.link().direction(),
                                context.link().romCollisionType(),
                                context.playerState().goldenLeavesCount());
                context.roomSession()
                        .tryUnlockIndoorKeyDoor(
                                collisionProbeX,
                                collisionProbeY,
                                context.link().direction(),
                                context.link().romCollisionType());
                context.roomSession()
                        .tryInteractWithIndoorBlock(
                                collisionProbeX,
                                collisionProbeY,
                                context.link().direction(),
                                context.link().romCollisionType());
            }
            Link.ScreenShakeRequest pegasusShake = context.link().consumePegasusScreenShakeRequest();
            if (pegasusShake != null) {
                context.scrollController().startScreenShake(pegasusShake.countdown(), pegasusShake.phase());
            }
            {
                context.roomSession()
                        .setEntityScreenShakeCountdown(
                                context.scrollController().screenShakeCountdown());
                context.roomSession()
                        .setSecretSeashellPegasusCollisionState(
                                context.scrollController().screenShakeCountdown() != 0,
                                context.link().pegasusBootsCollisionCountdown() != 0,
                                context.link().pegasusBootsCollisionPosX(),
                                context.link().pegasusBootsCollisionPosY());
            }
            {
                context.roomSession()
                        .setChestPlayerLevels(
                                context.playerState().shieldLevel(),
                                context.playerState().swordLevel(),
                                context.playerState().powerBraceletLevel());
                context.roomSession()
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
                        .collectEntityIfNeeded(
                                context.frameCounter(),
                                context.link().romEntityX(),
                                context.link().romEntityY(),
                                context.link().isAirborne(),
                                true,
                                context.link().direction(),
                                context.link().romEntityZ(),
                                box.active(),
                                box.x(),
                                box.width(),
                                box.y(),
                                box.height());
                if (pickup != null) {
                    if (pickup.type() == 0x36) {
                        context.playerState().setActivePowerUp(PlayerState.ACTIVE_POWER_UP_NONE);
                        context.musicDirector().playDirectMusic(0x25);
                    } else if (pickup.sourceVariant() >= 0) {
                        context.playerState().applyFloatingItemPickup(pickup.type(), pickup.sourceVariant());
                    } else {
                        context.playerState().applyEntityPickup(pickup.type());
                    }
                }
            }
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
        } else {
            if (context.scrollController().isActive()) {
                // Keep Link's walk animation cycling during a room transition.
                context.link().tickAnimation();
            }
            // $02:75C4 does not imply an unconditional pause: $76C0 can
            // return to Default via $7757 and reset wC1C9 during dialogs.
            if (!inventoryBlockedAtStart
                    && !context.inventoryController().shouldBlockOverworldInput()
                    && (dialogBlocksGameplay
                            || context.scrollController().isActive()
                            || context.transitionController().isInputBlocked()
                            || (context.roomSession().gotItemPresentationActive()))) {
                tickCrackedFloor(context, context.link().romInteractiveMotionBlockValue() != 0);
            }
        }

        if (!dialogBlocksGameplay) {
            int rumbleHorizontal = context.roomSession().tickTailCaveKeyholeSequence();
            context.scrollController()
                    .setScriptedScreenShakeHorizontal(
                            context.roomSession().tailCaveKeyholeSequenceActive(), rumbleHorizontal);
        }
    }

    private static void tickCrackedFloor(GameplaySimulation context, boolean interactionBlocked) {
        context.roomSession()
                .tickCrackedFloor(
                        groundFrame(
                                context, context.link().pixelX(), context.link().pixelY(), interactionBlocked),
                        dialogGotItemActive(context));
    }

    /** wDialogGotItem != 0 ($02:7854). Ported writers: Link's ApplyGotItem
     * sequence (UseMagicPowder $00:149E) and the witch presentation
     * ($05:4834, $05:48DB). */
    private static boolean dialogGotItemActive(GameplaySimulation context) {
        return (context.link().dialogGotItemActive()) || (context.roomSession().gotItemPresentationActive());
    }

    private static RoomSession.LinkGroundFrame groundFrame(
            GameplaySimulation context, int pixelX, int pixelY, boolean interactionBlocked) {
        return new RoomSession.LinkGroundFrame(
                pixelX,
                pixelY,
                context.link().isAirborne(),
                interactionBlocked,
                context.dialogController().isActive(),
                context.scrollController().isActive()
                        || context.transitionController().isInputBlocked(),
                context.link().romMotionState(),
                context.link().romSlowWalkingSpeed(),
                context.link().romItemUsageContext());
    }
}
