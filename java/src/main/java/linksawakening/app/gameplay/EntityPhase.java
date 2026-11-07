package linksawakening.app.gameplay;

import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.RichardProgressEvent;

import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.RoomEntityRuntime;

import static linksawakening.world.RoomConstants.*;

import static linksawakening.app.gameplay.GameplaySimulation.*;

final class EntityPhase {
    void tick(GameplaySimulation context, Sword.CollisionBox swordBoxForEntityTick) {
        var gpu = context.gpu();
        var romData = context.romData();
        var roomSession = context.roomSession();
        var transitionController = context.transitionController();
        var roomTransitionCoordinator = context.roomTransitionCoordinator();
        var debugNoClip = context.debugNoClip();
        var frameCounter = context.frameCounter();
        var scrollController = context.scrollController();
        var inputConfig = context.inputConfig();
        var inputState = context.inputState();
        var playerState = context.playerState();
        var inventoryMenu = context.inventoryMenu();
        var inventoryController = context.inventoryController();
        var dialogController = context.dialogController();
        var gameplaySoundSink = context.gameplaySoundSink();
        var musicPlayer = context.musicPlayer();
        var gameplayMusicController = context.gameplayMusicController();
        var musicDirector = context.musicDirector();
        var transientVfxSystem = context.transientVfxSystem();
        var itemRegistry = context.itemRegistry();
        var equipmentController = context.equipmentController();
        var link = context.link();
        var lift = context.lift();
        var dialogInteractions = context.dialogInteractions();
        var direction = context.direction();
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
                            link.apply(LinkCommandAdapter.from(request));
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
                            link.apply(LinkCommandAdapter.from(request));
                        }
                        if (request.resetPegasusBoots() && link == null && playerState != null) {
                            playerState.setRunningWithPegasusBoots(false);
                        }
                    } else {
                        var request = potContactRequests.get(potContactRequestIndex++);
                        if (link != null) {
                            link.apply(LinkCommandAdapter.from(request));
                        }
                    }
                }
                for (var request : roomSession.consumeLinkMotionBlockRequests()) {
                    if (link != null) {
                        link.apply(LinkCommandAdapter.from(request));
                    }
                }
                for (var request : roomSession.consumeWarpLinkStateRequests()) {
                    if (link != null) {
                        link.apply(LinkCommandAdapter.from(request));
                    }
                    Sword warpSword = equipmentController.activeSword();
                    if (warpSword != null) {
                        warpSword.resetSpinAttack();
                    }
                }
                if (roomSession.consumeWorldLinkMotionBlockRequest() && link != null) {
                    link.apply(linksawakening.entity.LinkCommand.Action.BLOCK_MOTION);
                }
                for (var request : roomSession.consumeLinkFallenPoseRequests()) {
                    if (link != null) {
                        link.apply(LinkCommandAdapter.from(request));
                    }
                }
                for (var request : roomSession.consumeLinkFacingRequests()) {
                    if (link != null) {
                        link.apply(LinkCommandAdapter.from(request));
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
                        var request = linkAttackClearRequests.get(linkAttackClearIndex++);
                        if (link != null) {
                            link.apply(LinkCommandAdapter.from(request));
                        }
                    } else {
                        var request = linkAttackStepRequests.get(linkAttackStepIndex++);
                        if (link != null) {
                            link.apply(LinkCommandAdapter.from(request));
                        }
                    }
                }
                for (var request : roomSession.consumeLinkHeldItemPoseRequests()) {
                    if (link != null) {
                        link.apply(LinkCommandAdapter.from(request));
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
                        link.apply(LinkCommandAdapter.from(request));
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
                        link.apply(LinkCommandAdapter.from(request));
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
                            == RichardProgressEvent.Kind.RETURN_ALL_LEAVES) {
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
                        == linksawakening.world.entity.event.LikeLikeEvent.Kind.CAPTURE) {
                        if (event.stolenInventorySlot() == 0) {
                            playerState.setItemB(PlayerState.INVENTORY_EMPTY);
                        } else if (event.stolenInventorySlot() == 1) {
                            playerState.setItemA(PlayerState.INVENTORY_EMPTY);
                        }
                        if (link != null) {
                            link.apply(LinkCommandAdapter.from(event));
                        }
                    } else if (link != null) {
                        link.apply(LinkCommandAdapter.from(event));
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
                        link.apply(LinkCommandAdapter.from(event));
                        continue;
                    }
                    link.apply(LinkCommandAdapter.from(event));
                    Sword reflectedSword = equipmentController.activeSword();
                    if (reflectedSword != null) {
                        reflectedSword.resetSpinAttack();
                    }
                }
                for (var request : roomSession.consumeRoosterLinkStateRequests()) {
                    if (link != null) {
                        link.apply(LinkCommandAdapter.from(request));
                    }
                }
                for (var request : roomSession.consumeHinoxLinkEffectRequests()) {
                    if (link != null) {
                        link.apply(LinkCommandAdapter.from(request));
                    }
                    if (request.damage() != 0 && playerState != null) {
                        playerState.applyRomEnemyDamage(request.damage());
                    }
                }
                lift.synchronizeLinkLiftedPresentation();
            }

    }
}
