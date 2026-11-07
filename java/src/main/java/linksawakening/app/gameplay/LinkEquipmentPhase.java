package linksawakening.app.gameplay;

import linksawakening.entity.Link;
import linksawakening.state.PlayerState;
import linksawakening.world.EntityPickupEvent;
import linksawakening.world.Warp;

import static linksawakening.world.RoomConstants.*;

import static linksawakening.app.gameplay.GameplaySimulation.*;

final class LinkEquipmentPhase {
    void tick(GameplaySimulation context, boolean dialogBlocksGameplay) {
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
                inventoryController.tick();
                if (playerState != null) {
                    playerState.tickResourceBuffers(frameCounter);
                }
                if (transientVfxSystem != null) {
                    transientVfxSystem.tick();
                }
                if (droppableRupeeSystem != null && link != null) {
                    droppableRupeeSystem.tick(link.pixelX(), link.pixelY(), playerState);
                }
            }

            if (!dialogBlocksGameplay && inventoryMenu.isFullyOpen()) {
                inventoryController.dispatchMenuInput();
            }

            boolean linkActive = !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay
                && (roomSession == null || !roomSession.gotItemPresentationActive());
            boolean swordPickupSequenceActive = roomSession != null
                && roomSession.swordPickupSequenceActive();
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
                    boolean sideViewManualExit = roomSession.mapCategory() == Warp.CATEGORY_SIDESCROLL
                        && room != 0xE8 && room != 0xF8;
                    boolean caveManualExit = roomSession.mapId() == 0x1F
                        && (room == 0xF2 || room == 0xF5);
                    link.setNormalRecoilRoomBoundaries(!sideViewManualExit && !caveManualExit);
                }
                link.setGelClinging(roomSession != null && roomSession.gelClingingToLink(), frameCounter);
                link.captureRomFinalPosition();
                boolean keyholeSequenceActive = roomSession != null
                    && roomSession.tailCaveKeyholeSequenceActive();
                if (keyholeSequenceActive) {
                    link.blockNextRomMotionFrame();
                }
                boolean powerBraceletButtonHeld = lift.isPowerBraceletButtonHeld();
                boolean bombButtonHeld = lift.isBombButtonHeld();
                if (roomSession != null && !keyholeSequenceActive) {
                    roomSession.setEntityPowerBraceletButtonHeld(powerBraceletButtonHeld);
                    roomSession.setEntityBombButtonHeld(bombButtonHeld);
                    var liftedState = roomSession.liftedEntityState();
                    boolean bombBeingThrown = liftedState.type() == 0x02;
                    boolean liftedThrowButtonHeld = bombBeingThrown
                        ? bombButtonHeld : powerBraceletButtonHeld;
                    if (liftedThrowButtonHeld && liftedState.carryState() == 0x01) {
                        roomSession.throwLiftedEntity(link.direction());
                    }
                }
                if (!keyholeSequenceActive && !link.isCarryingLiftedObject()) {
                    if (link.romMotionState() == 0x05
                        || (roomSession != null && roomSession.gelClingingToLink())) {
                        equipmentController.tickEquippedItemsWithoutInput(frameCounter);
                    } else {
                        if (!swordPickupSequenceActive
                            && (roomSession == null || (!roomSession.powerUpPickupSequenceActive() && !roomSession.heartPiecePickupActive())))
                            equipmentController.dispatchButtonEdges();
                        equipmentController.tickEquippedItems(frameCounter);
                    }
                }
                if (debugNoClip && debugScroll.getAsBoolean()) {
                    link.tickAnimation();
                } else {
                    link.tickRomLinkPushing();
                    link.update();
                }
                if (roomSession != null && playerState != null) {
                    int collisionProbeX = link.romCollisionProbePixelX();
                    int collisionProbeY = link.romCollisionProbePixelY();
                    roomSession.tryUnlockTailCaveKeyhole(
                        collisionProbeX, collisionProbeY, link.direction(),
                        link.romCollisionType(), playerState.tailKeyCount() != 0);
                    roomSession.tryUnlockKeyCavernKeyhole(
                        collisionProbeX, collisionProbeY, link.direction(),
                        link.romCollisionType(), playerState.goldenLeavesCount());
                    roomSession.tryUnlockIndoorKeyDoor(
                        collisionProbeX, collisionProbeY, link.direction(),
                        link.romCollisionType());
                    roomSession.tryInteractWithIndoorBlock(
                        collisionProbeX, collisionProbeY, link.direction(),
                        link.romCollisionType());
                    roomSession.tryLiftIndoorObject(
                        link.pixelX(), link.pixelY(), link.direction(),
                        powerBraceletButtonHeld,
                        link.romPressedButtonsMask(),
                        playerState.activePowerUp()
                            == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
                }
                Link.ScreenShakeRequest pegasusShake =
                    link.consumePegasusScreenShakeRequest();
                if (pegasusShake != null) {
                    scrollController.startScreenShake(
                        pegasusShake.countdown(), pegasusShake.phase());
                }
                if (roomSession != null) {
                    roomSession.setSecretSeashellPegasusCollisionState(
                        scrollController.screenShakeCountdown() != 0,
                        link.pegasusBootsCollisionCountdown() != 0,
                        link.pegasusBootsCollisionPosX(),
                        link.pegasusBootsCollisionPosY());
                }
                if (roomSession != null && playerState != null) {
                    roomSession.setChestPlayerLevels(
                        playerState.shieldLevel(), playerState.swordLevel(),
                        playerState.powerBraceletLevel());
                    roomSession.setToadstoolPlayerState(
                        playerState.hasToadstool(), playerState.magicPowderCount());
                    EntityPickupEvent pickup = roomSession.collectEntityIfNeeded(
                        frameCounter, link.romEntityX(), link.romEntityY(), link.isAirborne(), true,
                        link.direction(), link.romEntityZ());
                    if (pickup != null) {
                        if (pickup.type() == 0x36) {
                            playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_NONE);
                            musicDirector.playDirectMusic(0x25);
                        } else if (pickup.sourceVariant() >= 0) {
                            playerState.applyFloatingItemPickup(
                                pickup.type(), pickup.sourceVariant());
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
            } else if (scrollController.isActive() && link != null) {
                // Keep Link's walk animation cycling during a room transition.
                link.tickAnimation();
            }

            if (!dialogBlocksGameplay && roomSession != null) {
                int rumbleHorizontal = roomSession.tickTailCaveKeyholeSequence();
                scrollController.setScriptedScreenShakeHorizontal(
                    roomSession.tailCaveKeyholeSequenceActive(), rumbleHorizontal);
            }

    }
}
