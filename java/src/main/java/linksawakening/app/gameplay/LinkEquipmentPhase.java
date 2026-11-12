package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.entity.Link;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.transition.Warp;

final class LinkEquipmentPhase {
    void tick(GameplaySimulation context, boolean dialogBlocksGameplay, boolean inventoryBlockedAtStart) {
        var roomSession = context.roomSession();
        var transitionController = context.transitionController();
        var roomTransitionCoordinator = context.roomTransitionCoordinator();
        var debugNoClip = context.debugNoClip();
        var frameCounter = context.frameCounter();
        var ROOM_PIXEL_HEIGHT = context.ROOM_PIXEL_HEIGHT();
        var scrollController = context.scrollController();
        var playerState = context.playerState();
        var inventoryMenu = context.inventoryMenu();
        var inventoryController = context.inventoryController();
        var musicDirector = context.musicDirector();
        var transientVfxSystem = context.transientVfxSystem();
        var droppableRupeeSystem = context.droppableRupeeSystem();
        var equipmentController = context.equipmentController();
        var link = context.link();
        var lift = context.lift();
        var bush = context.bush();
        var debugScroll = context.debugScroll();
        var direction = context.direction();
        if (!dialogBlocksGameplay) {
            if (playerState != null) {
                playerState.tickResourceBuffers(frameCounter);
                var resourceSound = playerState.takeResourceSound();
                if (resourceSound != null) context.gameplaySoundSink().play(resourceSound);
            }
            if (transientVfxSystem != null) {
                transientVfxSystem.tick();
            }
            if (droppableRupeeSystem != null && link != null) {
                droppableRupeeSystem.tick(link.pixelX(), link.pixelY(), playerState);
            }
        }

        // ApplyGotItem $00:0FC7 runs before normal Link/item work. A
        // toadstool requested by that work starts decrementing next frame.
        if (link != null) {
            int itemDialog = link.tickDialogGotItem(dialogBlocksGameplay);
            if (itemDialog == 2) {
                context.dialogController()
                        .openPreformattedForLinkY(
                                context.dialogTextLoader().load(new linksawakening.gameplay.SignpostDialogRef(0, 0x0E)),
                                linksawakening.gameplay.LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
            }
            if (link.presentedDialogGotItem() == 2) {
                context.gpu().updatePowderInventoryTiles(context.romData(), true);
            }
        }

        if (context.passOutController().tick(context, dialogBlocksGameplay)) return;

        // LinkMotionMapFadeInHandler ($00:19DE-$00:1A21) restores normal
        // motion and returns; held movement starts on the following frame.
        boolean linkActive = !inventoryBlockedAtStart
                && !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay
                && (link == null || !link.dialogGotItemActive())
                && (roomSession == null || !roomSession.gotItemPresentationActive());
        boolean swordPickupSequenceActive = roomSession != null && roomSession.swordPickupSequenceActive();
        linkActive = linkActive && !swordPickupSequenceActive;
        if (linkActive && link != null) {
            // AnimateEntities runs after Link motion and its normal NPC
            // handlers restore hLinkFinalPosition when they push Link.
            // The ROM seeds that shadow from hLinkPosition before the
            // frame's motion work (bank0.asm:$0F97), so capture it before
            // any Link movement can change the position.
            if (roomSession != null) {
                int room = roomSession.currentRoomId();
                link.setSideScrolling(roomSession.mapCategory() == Warp.CATEGORY_SIDESCROLL);
                // $02:6CB6's manual exits precede the normal recoil gate.
                boolean sideViewManualExit =
                        roomSession.mapCategory() == Warp.CATEGORY_SIDESCROLL && room != 0xE8 && room != 0xF8;
                boolean caveManualExit = roomSession.mapId() == 0x1F && (room == 0xF2 || room == 0xF5);
                link.setNormalRecoilRoomBoundaries(!sideViewManualExit && !caveManualExit);
            }
            link.setGelClinging(roomSession != null && roomSession.gelClingingToLink(), frameCounter);
            link.captureRomFinalPosition();
            boolean keyholeSequenceActive = roomSession != null && roomSession.tailCaveKeyholeSequenceActive();
            if (keyholeSequenceActive) {
                link.blockNextRomMotionFrame();
            }
            boolean powerBraceletButtonHeld = !link.recoveryBlocksInput() && lift.isPowerBraceletButtonHeld();
            boolean bombButtonHeld = !link.recoveryBlocksInput() && lift.isBombButtonHeld();
            if (roomSession != null && !keyholeSequenceActive) {
                roomSession.setEntityPowerBraceletButtonHeld(powerBraceletButtonHeld);
                roomSession.setEntityBombButtonHeld(bombButtonHeld);
                var liftedState = roomSession.liftedEntityState();
                boolean bombBeingThrown = liftedState.type() == 0x02;
                boolean throwPressed = !link.recoveryBlocksInput() && lift.isThrowButtonPressed(bombBeingThrown);
                if (throwPressed && liftedState.carryState() == 0x01) {
                    roomSession.throwLiftedEntity(link.direction());
                }
            }
            boolean canGrab = !keyholeSequenceActive
                    && !link.isAirborne()
                    && link.romMotionState() == 0
                    && playerState != null
                    && !playerState.runningWithPegasusBoots()
                    && !link.isCarryingLiftedObject();
            boolean grabbing = roomSession != null
                    && playerState != null
                    && roomSession.tryLiftObject(
                            link.pixelX(),
                            link.pixelY(),
                            link.direction(),
                            powerBraceletButtonHeld && canGrab,
                            link.romPressedButtonsMask(),
                            playerState.activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER,
                            playerState.powerBraceletLevel(),
                            playerState.activePowerUp() != PlayerState.ACTIVE_POWER_UP_NONE);
            link.setGrabbingObject(
                    grabbing,
                    (link.romPressedButtonsMask()
                                    & switch (link.direction()) {
                                        case Link.DIRECTION_RIGHT -> 2;
                                        case Link.DIRECTION_LEFT -> 1;
                                        case Link.DIRECTION_UP -> 8;
                                        default -> 4;
                                    })
                            != 0);
            if (!keyholeSequenceActive
                    && !link.recoveryBlocksInput()
                    && !link.isCarryingLiftedObject()
                    && !link.romItemAnimationFrameBlocked()) {
                if (link.romMotionState() == 0x05 || (roomSession != null && roomSession.gelClingingToLink())) {
                    equipmentController.tickEquippedItemsWithoutInput(frameCounter);
                } else {
                    if (!swordPickupSequenceActive
                            && (roomSession == null
                                    || (!roomSession.powerUpPickupSequenceActive()
                                            && !roomSession.heartPiecePickupActive())))
                        equipmentController.dispatchButtonEdges();
                    equipmentController.tickEquippedItems(frameCounter);
                }
            }
            if (debugNoClip && debugScroll.getAsBoolean()) {
                link.tickAnimation();
            } else {
                link.tickRomLinkPushing();
                boolean groundInteractionBlocked = link.romInteractiveMotionBlockValue() != 0;
                boolean wasAirborne = link.isAirborne();
                int beforeMotionX = link.pixelX(), beforeMotionY = link.pixelY();
                link.update();
                if (wasAirborne
                        && !link.isAirborne()
                        && !link.isFallingIntoPit()
                        && roomSession != null
                        && roomSession.mapCategory() != Warp.CATEGORY_SIDESCROLL) {
                    // ApplyLinkGroundMotion $02:4588 probes the landing tile
                    // before func_002_434A applies XY. $00:1794 probes again
                    // after XY and copies that first sample at $02:7589.
                    roomSession.tickCrackedFloorOnLanding(
                            groundFrame(context, beforeMotionX, beforeMotionY, groundInteractionBlocked),
                            dialogGotItemActive(context));
                }
                tickCrackedFloor(context, groundInteractionBlocked);
                var splash = link.consumeWaterSplash();
                if (splash != null && transientVfxSystem != null) {
                    // VFX stores ROM/OAM coordinates, as entity VFX requests do.
                    transientVfxSystem.spawn(
                            linksawakening.vfx.TransientVfxType.WATER_SPLASH, splash.romX(), splash.romY());
                }
            }
            if (roomSession != null && playerState != null) {
                int collisionProbeX = link.romCollisionProbePixelX();
                int collisionProbeY = link.romCollisionProbePixelY();
                roomSession.tryUnlockTailCaveKeyhole(
                        collisionProbeX,
                        collisionProbeY,
                        link.direction(),
                        link.romCollisionType(),
                        playerState.tailKeyCount() != 0);
                roomSession.tryUnlockKeyCavernKeyhole(
                        collisionProbeX,
                        collisionProbeY,
                        link.direction(),
                        link.romCollisionType(),
                        playerState.goldenLeavesCount());
                roomSession.tryUnlockIndoorKeyDoor(
                        collisionProbeX, collisionProbeY, link.direction(), link.romCollisionType());
                roomSession.tryInteractWithIndoorBlock(
                        collisionProbeX, collisionProbeY, link.direction(), link.romCollisionType());
            }
            Link.ScreenShakeRequest pegasusShake = link.consumePegasusScreenShakeRequest();
            if (pegasusShake != null) {
                scrollController.startScreenShake(pegasusShake.countdown(), pegasusShake.phase());
            }
            if (roomSession != null) {
                roomSession.setEntityScreenShakeCountdown(scrollController.screenShakeCountdown());
                roomSession.setSecretSeashellPegasusCollisionState(
                        scrollController.screenShakeCountdown() != 0,
                        link.pegasusBootsCollisionCountdown() != 0,
                        link.pegasusBootsCollisionPosX(),
                        link.pegasusBootsCollisionPosY());
            }
            if (roomSession != null && playerState != null) {
                roomSession.setChestPlayerLevels(
                        playerState.shieldLevel(), playerState.swordLevel(), playerState.powerBraceletLevel());
                roomSession.setToadstoolPlayerState(playerState.hasToadstool(), playerState.magicPowderCount());
                var sword = equipmentController.activeSword();
                var box = sword == null
                        ? linksawakening.equipment.Sword.CollisionBox.inactive()
                        : sword.enemyCollisionBox(
                                link.romEntityX(),
                                link.romSwordCollisionY(),
                                link.direction(),
                                playerState.runningWithPegasusBoots());
                EntityPickupEvent pickup = roomSession.collectEntityIfNeeded(
                        frameCounter,
                        link.romEntityX(),
                        link.romEntityY(),
                        link.isAirborne(),
                        true,
                        link.direction(),
                        link.romEntityZ(),
                        box.active(),
                        box.x(),
                        box.width(),
                        box.y(),
                        box.height());
                if (pickup != null) {
                    if (pickup.type() == 0x36) {
                        playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_NONE);
                        musicDirector.playDirectMusic(0x25);
                    } else if (pickup.sourceVariant() >= 0) {
                        playerState.applyFloatingItemPickup(pickup.type(), pickup.sourceVariant());
                    } else {
                        playerState.applyEntityPickup(pickup.type());
                    }
                }
            }
            bush.maybeCutBushWithSword();
            // Warp-transition check runs BEFORE edge-scroll/clamp:
            // indoor rooms clamp Link at y=112 (== ROOM_PIXEL_HEIGHT −
            // SPRITE_SIZE) in the edge-scroll fallback, but the
            // indoor-exit trigger needs to see Link at y > 112 to fire.
            // If we clamp first, Link never crosses the threshold and
            // the south-edge warp never fires.
            // The source performs this ordinary Link check before
            // AnimateEntities; a standing platform handler performs its
            // own explicit check again after moving Link below.
            roomTransitionCoordinator.handleWarpAndIndoorBoundaries(link);
            roomTransitionCoordinator.handleOverworldBoundary(link);
        } else if (link != null) {
            if (scrollController.isActive()) {
                // Keep Link's walk animation cycling during a room transition.
                link.tickAnimation();
            }
            // $02:75C4 does not imply an unconditional pause: $76C0 can
            // return to Default via $7757 and reset wC1C9 during dialogs.
            if (!inventoryBlockedAtStart
                    && !inventoryController.shouldBlockOverworldInput()
                    && (dialogBlocksGameplay
                            || scrollController.isActive()
                            || transitionController.isInputBlocked()
                            || (roomSession != null && roomSession.gotItemPresentationActive()))) {
                tickCrackedFloor(context, link.romInteractiveMotionBlockValue() != 0);
            }
        }

        if (!dialogBlocksGameplay && roomSession != null) {
            int rumbleHorizontal = roomSession.tickTailCaveKeyholeSequence();
            scrollController.setScriptedScreenShakeHorizontal(
                    roomSession.tailCaveKeyholeSequenceActive(), rumbleHorizontal);
        }
    }

    private static void tickCrackedFloor(GameplaySimulation context, boolean interactionBlocked) {
        var room = context.roomSession();
        var link = context.link();
        if (room == null || link == null) return;
        room.tickCrackedFloor(
                groundFrame(context, link.pixelX(), link.pixelY(), interactionBlocked), dialogGotItemActive(context));
    }

    /** wDialogGotItem != 0 ($02:7854). Ported writers: Link's ApplyGotItem
     * sequence (UseMagicPowder $00:149E) and the witch presentation
     * ($05:4834, $05:48DB). */
    private static boolean dialogGotItemActive(GameplaySimulation context) {
        var link = context.link();
        var room = context.roomSession();
        return (link != null && link.dialogGotItemActive()) || (room != null && room.gotItemPresentationActive());
    }

    private static linksawakening.world.room.RoomSession.LinkGroundFrame groundFrame(
            GameplaySimulation context, int pixelX, int pixelY, boolean interactionBlocked) {
        var link = context.link();
        return new linksawakening.world.room.RoomSession.LinkGroundFrame(
                pixelX,
                pixelY,
                link.isAirborne(),
                interactionBlocked,
                context.dialogController() != null && context.dialogController().isActive(),
                context.scrollController().isActive()
                        || context.transitionController().isInputBlocked(),
                link.romMotionState(),
                link.romSlowWalkingSpeed(),
                link.romItemUsageContext());
    }
}
