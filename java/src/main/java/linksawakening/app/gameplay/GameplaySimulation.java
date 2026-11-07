package linksawakening.app.gameplay;

import linksawakening.app.gameplay.*;
import linksawakening.app.MusicDirector;
import linksawakening.app.story.OpeningStory;

import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.EnemyAttackContext;
import linksawakening.world.EntityCombatEvent;
import linksawakening.world.EntityPickupEvent;
import linksawakening.world.EntityProjectileEvent;
import linksawakening.world.RoomEntityRuntime;
import linksawakening.world.RoomSession;
import linksawakening.world.RoomTransitionCoordinator;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;
import linksawakening.world.Warp;


import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;


import java.util.function.*;

/** ROM-ordered simulation between transition advancement and input completion. */
public record GameplaySimulation(GPU gpu, byte[] romData, RoomSession roomSession, TransitionController transitionController, RoomTransitionCoordinator roomTransitionCoordinator, boolean debugNoClip, int frameCounter, int ROOM_PIXEL_HEIGHT, ScrollController scrollController, InputConfig inputConfig, InputState inputState, PlayerState playerState, InventoryMenu inventoryMenu, InventoryController inventoryController, DialogController dialogController, DialogTextLoader dialogTextLoader, GameplaySoundSink gameplaySoundSink, FrameMusicPlayer musicPlayer, GameplayMusicController gameplayMusicController, MusicDirector musicDirector, TransientVfxSystem transientVfxSystem, DroppableRupeeSystem droppableRupeeSystem, ItemRegistry itemRegistry, EquipmentController equipmentController, Link link, OpeningStory story, LiftInteraction lift, BushInteraction bush, DialogInteractions dialogInteractions, Supplier<OverworldDialogBlockers> dialogBlockers, BooleanSupplier debugScroll, IntUnaryOperator direction, Runnable dialogSounds) {
    public void tick() {
        {
            if (roomSession != null) {
                // $00:0F22 runs before world transitions and dialog gates.
                // The separate inventory gameplay handler does not run it.
                roomSession.advanceBoomerangSoundClock(
                    inventoryMenu == null || inventoryMenu.isFullyClosed());
            }
            scrollController.tickScreenShake();
            if (roomSession != null && link != null && dialogController != null
                && !transitionController.isInputBlocked()) {
                int dungeonTitle = roomSession.tickDungeonTitleMessage(
                    dialogController.isActive(), inventoryMenu.isFullyClosed(),
                    link.romMotionState(), debugNoClip);
                if (dungeonTitle >= 0) {
                    dialogController.openPreformattedForLinkY(
                        dialogTextLoader.load(new SignpostDialogRef(0, dungeonTitle)),
                        LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
                }
            }
            if (dialogController != null) {
                dialogController.tick();
                dialogSounds.run();
            }
            story.updateNewGameMarinPresentation();
            if (story.tickNewGameWakeUp()) {
                completeGameplayFrame(dialogController);
                return;
            }
            if (story.tickNewGameTarinShield()) {
                completeGameplayFrame(dialogController);
                return;
            }
            OverworldDialogBlockers blockers = dialogBlockers.get();
            boolean dialogBlocksGameplay = blockers.pausesGameplay();

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

            Sword.CollisionBox swordBoxForEntityTick = Sword.CollisionBox.inactive();
            if (link != null && roomSession != null
                && !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
                Sword sword = equipmentController.activeSword();
                swordBoxForEntityTick = sword == null
                    ? Sword.CollisionBox.inactive()
                    : sword.enemyCollisionBox(link.romEntityX(), link.romSwordCollisionY(),
                        link.direction());
                EnemyAttackContext attackContext = new EnemyAttackContext(
                    playerState.swordLevel(),
                    playerState.tunicType() == PlayerState.TUNIC_RED,
                    playerState.activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER,
                    sword != null && sword.spinAttackActive(),
                    playerState.runningWithPegasusBoots(), link.isUsingShield(),
                    direction.applyAsInt(link.direction()), link.romEntityZ());
                var combatEvents = roomSession.resolveEntityCombat(
                    frameCounter, link.romEntityX(), link.romEntityY(),
                    link.isAirborne(),
                    roomSession.mapCategory() == Warp.CATEGORY_SIDESCROLL
                        ? link.romSpeedY() : link.zVelocity(), !debugNoClip,
                    swordBoxForEntityTick.active(), swordBoxForEntityTick.x(),
                    swordBoxForEntityTick.width(), swordBoxForEntityTick.y(),
                    swordBoxForEntityTick.height(), attackContext);
                EnemyCombatEventConsumer.consume(combatEvents, gameplaySoundSink, transientVfxSystem);
                for (EntityCombatEvent event : combatEvents) {
                    if (event.linkDamage() > 0 && playerState.invincibilityCounter() == 0
                        && (link == null || !link.isCollisionDamageImmune())) {
                        playerState.applyRomEnemyDamage(event.linkDamage());
                        if (event.linkCollisionResponse().active()) {
                            EntityCombatEvent.LinkCollisionResponse response =
                                event.linkCollisionResponse();
                            link.applyRomSpeed(response.speedX(), response.speedY());
                            link.setCollisionIgnoreFrames(
                                response.ignoreCollisionCountdown());
                            if (response.invincibilityCountdown() != 0) {
                                playerState.setInvincibilityCounter(
                                    response.invincibilityCountdown());
                            }
                            playerState.setRunningWithPegasusBoots(false);
                            // ApplyLinkCollisionWithEnemy writes WAVE_SFX_LINK_HURT
                            // before the shared Link response helper.
                            gameplaySoundSink.play(GameplaySoundEvent.LINK_HURT);
                        }
                    }
                    if (event.linkDamage() == 0 && event.linkCollisionResponse().active()) {
                        var response = event.linkCollisionResponse();
                        link.applyRomSpeed(response.speedX(), response.speedY());
                        link.setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
                        playerState.setRunningWithPegasusBoots(false);
                    }
                    if (event.linkAction()
                        == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN) {
                        link.bounceFromGoomba();
                    } else if (event.linkAction()
                        == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING) {
                        link.applyRomSpeed(0, 0xF0);
                    }
                }
            }

            // The original calls AnimateEntities after Link movement and
            // room-transition application. Give handlers the same current
            // position and active-room snapshot.
            if (roomSession != null
                && shouldTickRoomEntities(
                    scrollController.isActive(),
                    transitionController.isInputBlocked(),
                    inventoryController.shouldBlockOverworldInput(),
                    inventoryMenu != null && inventoryMenu.isTransitioning())) {
                roomSession.setEntityDialogActive(
                    dialogController != null && dialogController.isActive());
                roomSession.setEntityTalkState(
                    inventoryMenu != null && inventoryMenu.isTransitioning(),
                    dialogController == null ? 0 : dialogController.dialogCooldown(),
                    inventoryMenu == null ? 0x80 : inventoryMenu.windowY());
                roomSession.setEntityDialogAskSelectionIndex(
                    dialogController == null ? 0 : dialogController.choiceSelectionIndex());
                roomSession.setHeartPiecesCount(playerState.heartPieces());
                roomSession.setEntityMusicActive(
                    musicPlayer != null && musicPlayer.isMusicPlaying());
                roomSession.setEntityActionButtonsHeld(
                    inputState != null && inputConfig != null
                        && inputState.isDown(inputConfig.aKey()),
                    inputState != null && inputConfig != null
                        && inputState.isDown(inputConfig.bKey()));
                roomSession.setEntityJoypadHeld(
                    inputState != null && inputConfig != null
                        && (inputState.isDown(inputConfig.upKey())
                            || inputState.isDown(inputConfig.downKey())
                            || inputState.isDown(inputConfig.leftKey())
                            || inputState.isDown(inputConfig.rightKey())
                            || inputState.isDown(inputConfig.aKey())
                            || inputState.isDown(inputConfig.bKey())
                            || inputState.isDown(inputConfig.selectKey())
                            || inputState.isDown(inputConfig.menuOpenKey())));
                roomSession.setEntityPressedButtonsMask(
                    link == null ? 0 : link.romPressedButtonsMask());
                roomSession.setEntityPowerBraceletButtonHeld(lift.isPowerBraceletButtonHeld());
                roomSession.setEntityBombButtonHeld(lift.isBombButtonHeld());
                roomSession.setEntityAttackStepAnimationCountdown(
                    link == null ? 0 : link.romAttackStepAnimationCountdown());
                roomSession.setEntityFinalPositionX(
                    link == null ? 0 : link.romFinalPositionX());
                roomSession.setBirdKeyOwned(playerState != null && playerState.birdKeyCount() != 0);
                roomSession.setTailKeyOwned(playerState != null && playerState.tailKeyCount() != 0);
                roomSession.setEntityInventorySlots(
                    playerState == null ? PlayerState.INVENTORY_EMPTY : playerState.itemA(),
                    playerState == null ? PlayerState.INVENTORY_EMPTY : playerState.itemB());
                roomSession.setChestPlayerLevels(
                    playerState == null ? 1 : playerState.shieldLevel(),
                    playerState == null ? 1 : playerState.swordLevel(),
                    playerState == null ? 1 : playerState.powerBraceletLevel());
                roomSession.setToadstoolPlayerState(
                    playerState != null && playerState.hasToadstool(),
                    playerState == null ? 0 : playerState.magicPowderCount());
                roomSession.setEntityGoldenLeavesCount(
                    playerState == null ? 0 : playerState.goldenLeavesCount());
                roomSession.setEnemyDropPlayerState(
                    playerState.maxHearts(), playerState.health(),
                    playerState.activePowerUp() != PlayerState.ACTIVE_POWER_UP_NONE);
                roomSession.setFreeMovementMode(link != null && link.debugNoClip());
                roomSession.setEntityLinkVelocityZ(link == null ? 0 : link.zVelocity());
                var projectileEvents = roomSession.tickEntitiesWithProjectileEvents(
                    frameCounter,
                    link == null ? 0x08 : link.romEntityX(),
                    link == null ? 0x10 : link.romEntityY(),
                    link == null ? 0x00 : link.romEntityZ(),
                    link != null && link.isAirborne(),
                    link == null ? 0x02 : link.romMotionState(),
                    link == null ? 0x00 : link.direction(),
                    link == null ? 0x00 : link.romCollisionType(),
                    link != null && link.isUsingShield(),
                    playerState == null ? 1 : playerState.shieldLevel(),
                    playerState == null ? 0
                        : link != null && link.isCollisionDamageImmune()
                            ? 1 : playerState.invincibilityCounter(),
                    swordBoxForEntityTick.active(), swordBoxForEntityTick.x(),
                    swordBoxForEntityTick.width(), swordBoxForEntityTick.y(),
                    swordBoxForEntityTick.height(),
                    link == null ? 0 : link.romSpeedX(),
                    link == null ? 0 : link.romSpeedY());
                if (link != null) {
                    link.clearStandingOnSideViewEntity();
                }
                var platformRequests = roomSession.consumeSideViewPlatformLinkRequests();
                var potContactRequests = roomSession.consumeSideViewPotLinkRequests();
                var finalPositionRequests = roomSession.consumeLinkFinalPositionRequests();
                int platformRequestIndex = 0;
                int potContactRequestIndex = 0;
                int finalPositionRequestIndex = 0;
                // AnimateEntities processes slots in descending order. Merge
                // all Link-position writers to preserve source order.
                while (platformRequestIndex < platformRequests.size()
                    || potContactRequestIndex < potContactRequests.size()
                    || finalPositionRequestIndex < finalPositionRequests.size()) {
                    int nextStream = -1;
                    int nextSlot = -1;
                    if (platformRequestIndex < platformRequests.size()
                        && platformRequests.get(platformRequestIndex).sourceSlot() > nextSlot) {
                        nextStream = 0;
                        nextSlot = platformRequests.get(platformRequestIndex).sourceSlot();
                    }
                    if (finalPositionRequestIndex < finalPositionRequests.size()
                        && finalPositionRequests.get(finalPositionRequestIndex).sourceSlot() > nextSlot) {
                        nextStream = 1;
                        nextSlot = finalPositionRequests.get(finalPositionRequestIndex).sourceSlot();
                    }
                    if (potContactRequestIndex < potContactRequests.size()
                        && potContactRequests.get(potContactRequestIndex).sourceSlot() >= nextSlot) {
                        // Equal-slot emission is not expected; keep D6's
                        // explicit writes as the most specific one.
                        nextStream = 2;
                    }
                    if (nextStream == 0) {
                        var request = platformRequests.get(platformRequestIndex++);
                        if (link != null && request.standing()) {
                            link.applySideViewPlatformContact(request.horizontalDelta(),
                                request.positionY(), request.speedY(), request.standing());
                            roomTransitionCoordinator.handleWarpAndIndoorBoundaries(link);
                            roomTransitionCoordinator.handleOverworldBoundary(link);
                            if (scrollController.isActive()
                                || transitionController.isInputBlocked()) {
                                link.clearSideViewPlatformState();
                            }
                        }
                    } else if (nextStream == 1) {
                        var request = finalPositionRequests.get(finalPositionRequestIndex++);
                        if (link != null) {
                            link.restoreRomFinalPosition();
                            if (request.clearLinkPositionIncrement()) {
                                link.clearRomPositionIncrement();
                            }
                            if (request.markLinkPushing()) {
                                link.markRomLinkPushing(0x03);
                            }
                        }
                        if (request.resetPegasusBoots()) {
                            if (link != null) {
                                link.resetPegasusBoots();
                            } else if (playerState != null) {
                                playerState.setRunningWithPegasusBoots(false);
                            }
                        }
                    } else {
                        var request = potContactRequests.get(potContactRequestIndex++);
                        if (link != null) {
                            if (request.restoreFinalPositionX()) {
                                link.restoreRomFinalPositionX();
                            }
                            if (request.resetPegasusBoots()) {
                                link.resetPegasusBoots();
                            }
                            if (request.speedX() != 0) {
                                link.applyRomSpeedX(request.speedX());
                            }
                            if (request.ignoreCollisionCountdown() != 0) {
                                link.setCollisionIgnoreFrames(
                                    request.ignoreCollisionCountdown());
                            }
                            if (request.snapTop()) {
                                link.applySideViewPotContact(request.positionY(), request.speedY());
                            }
                        }
                    }
                }
                for (var request : roomSession.consumeLinkMotionBlockRequests()) {
                    if (link != null) {
                        link.applyRomFinalPosition((request.positionDeltaX() * 16) & 0xFF,
                            (request.positionDeltaY() * 16) & 0xFF);
                        link.blockNextRomMotionFrame();
                    }
                }
                for (var request : roomSession.consumeWarpLinkStateRequests()) {
                    if (link != null) {
                        link.applyWarpState3(request.positionX(), request.visualPositionY(),
                            request.immunityCountdown());
                    }
                    Sword warpSword = equipmentController.activeSword();
                    if (warpSword != null) {
                        warpSword.resetSpinAttack();
                    }
                }
                if (roomSession.consumeWorldLinkMotionBlockRequest() && link != null) {
                    link.blockNextRomMotionFrame();
                }
                for (var request : roomSession.consumeLinkFallenPoseRequests()) {
                    if (link != null) {
                        link.showRomFallenPose();
                    }
                }
                for (var request : roomSession.consumeLinkFacingRequests()) {
                    if (link != null) {
                        if (request.preserveWalkingPhase()) {
                            link.faceRomDirectionPreservingWalkPhase(request.romDirection());
                        } else {
                            link.setRomDirection(request.romDirection());
                        }
                    }
                }
                var linkAttackClearRequests = roomSession.consumeLinkAttackClearRequests();
                var linkAttackStepRequests = roomSession.consumeLinkAttackStepRequests();
                int linkAttackClearIndex = 0;
                int linkAttackStepIndex = 0;
                // A slot cannot emit both writes in one handler pass. If a
                // future bridge does produce a tie, the throw write wins.
                while (linkAttackClearIndex < linkAttackClearRequests.size()
                    || linkAttackStepIndex < linkAttackStepRequests.size()) {
                    boolean applyClear = linkAttackStepIndex >= linkAttackStepRequests.size()
                        || (linkAttackClearIndex < linkAttackClearRequests.size()
                            && linkAttackClearRequests.get(linkAttackClearIndex).sourceSlot()
                                > linkAttackStepRequests.get(linkAttackStepIndex).sourceSlot());
                    if (applyClear) {
                        linkAttackClearIndex++;
                        if (link != null) {
                            link.clearRomAttackStepAnimationCountdown();
                        }
                    } else {
                        linkAttackStepIndex++;
                        if (link != null) {
                            link.startRomItemAttackStep();
                        }
                    }
                }
                for (var request : roomSession.consumeLinkHeldItemPoseRequests()) {
                    if (link != null) {
                        if (request.powerUp()) link.showPowerUpPickupPose();
                        else link.showHeldItemPose();
                    }
                    Sword heldPoseSword = equipmentController.activeSword();
                    if (heldPoseSword != null) {
                        heldPoseSword.resetSpinAttack();
                    }
                }
                for (var request : roomSession.consumeLinkSwordSpinPoseRequests()) {
                    int acquisitionBaseDirection = link == null ? Link.DIRECTION_DOWN
                        : link.direction();
                    if (link != null) {
                        link.showSwordAcquisitionSpinPose(request.countdown());
                    }
                    Sword acquisitionSword = itemRegistry == null ? null
                        : itemRegistry.lookup(PlayerState.INVENTORY_SWORD) instanceof Sword sword
                            ? sword : null;
                    if (acquisitionSword != null) {
                        if (request.countdown() == 0x20) {
                            acquisitionSword.startSwordAcquisitionSpin(acquisitionBaseDirection);
                        } else if (acquisitionSword.spinAttackActive()) {
                            acquisitionSword.tick(false, frameCounter);
                        }
                    }
                }
                for (var request : roomSession.consumeLinkSwordFinalPoseRequests()) {
                    if (link != null) {
                        link.showSwordAcquisitionFinalPose();
                    }
                    if (itemRegistry != null
                        && itemRegistry.lookup(PlayerState.INVENTORY_SWORD) instanceof Sword sword) {
                        sword.resetSpinAttack();
                    }
                }
                for (var request : roomSession.consumeScreenShakeRequests()) {
                    scrollController.startScreenShake(request.countdown(), request.phase());
                }
                EnemyProjectileEventConsumer.consume(projectileEvents, playerState, link,
                    gameplaySoundSink);
                EnemyCombatEventConsumer.consume(roomSession.consumeEntityEvents(),
                    gameplaySoundSink, transientVfxSystem);
                int musicFadeOutCountdown =
                    roomSession.consumePendingMusicFadeOutCountdown();
                if (musicFadeOutCountdown != 0 && gameplayMusicController != null) {
                    gameplayMusicController.requestFadeOut(musicFadeOutCountdown);
                }
                for (var reward : roomSession.consumeChestRewardEvents()) {
                    if (playerState != null) {
                        playerState.applyChestReward(reward.itemType());
                    }
                }
                for (var reward : roomSession.consumeKeyRewardEvents()) {
                    if (playerState != null) {
                        playerState.applyChestReward(reward.itemType());
                    }
                }
                for (var reward : roomSession.consumeSlimeKeyRewardEvents()) {
                    if (playerState != null) {
                        playerState.setGoldenLeavesCount(reward.goldenLeavesCount());
                    }
                }
                for (var update : roomSession.consumeHeartPieceUpdates()) {
                    dialogController.setInteractionLocked(update.interactionLocked());
                    if (update.incrementPiece()) playerState.setHeartPieces(playerState.heartPieces() + 1);
                    if (update.newContainer()) playerState.completeHeartPieceContainer();
                    if (update.tilesCommand() != 0) gpu.updateHeartPieceMeterTiles(romData, update.tilesCommand());
                    if (update.complete()) gpu.updateTradingItemTiles(romData, playerState.tradeSequenceItem());
                }
                for (var reward : roomSession.consumeHeartContainerRewards()) {
                    if (playerState != null) {
                        playerState.applyHeartContainerReward();
                    }
                    musicDirector.playDirectMusic(0x18);
                }
                for (var request : roomSession.consumeMadamMeowMeowFullHealRequests()) {
                    if (playerState != null) {
                        playerState.requestFullHeal();
                    }
                }
                for (var event : roomSession.consumeRichardProgressEvents()) {
                    if (playerState != null
                        && event.kind()
                            == RoomEntityRuntime.RichardProgressEvent.Kind.RETURN_ALL_LEAVES) {
                        playerState.setGoldenLeavesCount(event.goldenLeavesCount());
                        if (gpu != null && romData != null) {
                            gpu.replaceSlimeKeyTilesByGoldenLeaf(romData);
                        }
                    }
                }
                BeachSwordRewardConsumer.consume(roomSession, playerState);
                for (var reward : roomSession.consumeToadstoolRewards()) {
                    if (playerState != null) {
                        playerState.applyToadstoolReward();
                    }
                }
                for (var event : roomSession.consumeWitchExchangeEvents()) {
                    if (playerState != null) {
                        playerState.beginWitchToadstoolExchange(event.inventorySlot());
                    }
                }
                for (var reward : roomSession.consumeWitchRewardEvents()) {
                    if (playerState != null) {
                        playerState.applyWitchMagicPowderReward();
                    }
                }
                roomSession.setBirdKeyOwned(playerState != null && playerState.birdKeyCount() != 0);
                roomSession.setTailKeyOwned(playerState != null && playerState.tailKeyCount() != 0);
                int chestMusicTrack = roomSession.consumePendingMusicTrack();
                if (chestMusicTrack >= 0) {
                    musicDirector.playDirectMusic(chestMusicTrack);
                }
                for (var event : roomSession.consumeLikeLikeEvents()) {
                    if (event.kind()
                        == linksawakening.world.RoomEntityRuntime.LikeLikeEvent.Kind.CAPTURE) {
                        if (event.stolenInventorySlot() == 0) {
                            playerState.setItemB(PlayerState.INVENTORY_EMPTY);
                        } else if (event.stolenInventorySlot() == 1) {
                            playerState.setItemA(PlayerState.INVENTORY_EMPTY);
                        }
                        if (link != null) {
                            link.applyLikeLikeCapture(event.entityX(), event.entityY(),
                                event.pinLinkPosition());
                        }
                    } else if (link != null) {
                        link.releaseLikeLikeCapture();
                    }
                }
                dialogInteractions.openEntityDialogRequests();
                if (roomTransitionCoordinator.handlePendingManboTransition(link)) {
                    gameplaySoundSink.play(linksawakening.gameplay.GameplaySoundEvent.MANBO_WARP);
                }
                roomTransitionCoordinator.handlePendingInstrumentTransition(link);
                for (var event : projectileEvents) {
                    // Accepted LINK_DAMAGE responses are applied by the
                    // projectile consumer before PlayerState becomes
                    // invincible. Keep this pass for shield reflection,
                    // sword-poke, and hookshot responses only.
                    if (event.kind() == EntityProjectileEvent.Kind.LINK_DAMAGE) {
                        boolean hasLinkResponse = event.linkIgnoreCollisionCountdown() != 0
                            || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
                        if (hasLinkResponse) {
                            Sword interruptedSword = equipmentController.activeSword();
                            if (interruptedSword != null) {
                                interruptedSword.resetSpinAttack();
                            }
                        }
                        continue;
                    }
                    boolean hookshotPull = event.kind() == EntityProjectileEvent.Kind.HOOKSHOT_PULL;
                    boolean hasLinkResponse = event.linkIgnoreCollisionCountdown() != 0
                        || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
                    if ((!hookshotPull && !hasLinkResponse)
                        || link == null) {
                        continue;
                    }
                    if (hookshotPull) {
                        link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY());
                        continue;
                    }
                    link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY());
                    link.setCollisionIgnoreFrames(event.linkIgnoreCollisionCountdown());
                    Sword reflectedSword = equipmentController.activeSword();
                    if (reflectedSword != null) {
                        reflectedSword.resetSpinAttack();
                    }
                }
                for (var request : roomSession.consumeRoosterLinkStateRequests()) {
                    if (link != null) {
                        link.applyRoosterFlightState(request.positionZ(), request.velocityZ(),
                            request.speedX(), request.speedY(), request.romDirection());
                    }
                }
                for (var request : roomSession.consumeHinoxLinkEffectRequests()) {
                    if (link != null) {
                        link.applyHinoxGrabState(request.heldX(), request.heldY(), request.heldZ(),
                            request.speedX(), request.speedY(), request.velocityZ(),
                            request.airborneState(), request.motionBlocked(),
                            request.applyHeldLinkPose());
                    }
                    if (request.damage() != 0 && playerState != null) {
                        playerState.applyRomEnemyDamage(request.damage());
                    }
                }
                lift.synchronizeLinkLiftedPresentation();
            }
            completeGameplayFrame(dialogController);

            // Advance the animated BG tiles (waterfalls, weather vanes, etc.).
            // Original gates this behind wRoomTransitionState == 0 and the
            // inventory window not overlapping; mirror both here.
            if (!scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
                if (roomSession == null) {
                    gpu.tickAnimatedTiles(romData);
                } else {
                    roomSession.tickGameplayVBlank();
                }
            }
        }

    }
    public static boolean shouldTickRoomEntities(boolean scrollActive,
                                          boolean transitionInputBlocked,
                                          boolean inventoryBlocksInput,
                                          boolean inventoryTransitioning) {
        return !scrollActive
            && !transitionInputBlocked
            && (!inventoryBlocksInput || inventoryTransitioning);
    }

    public static void completeGameplayFrame(DialogController dialog) {
        if (dialog != null) {
            dialog.tickGameplayFrameCooldown();
        }
    }

}
