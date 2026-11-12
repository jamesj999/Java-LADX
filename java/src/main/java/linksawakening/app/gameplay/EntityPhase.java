package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.room.RoomSession;

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
                        inventoryController.shouldBlockOverworldInput()
                                && !inventoryMenu.transition().worldHandlerActive(),
                        inventoryMenu != null && inventoryMenu.isTransitioning())) {
            var projectileEvents = roomSession.tickEntities(new RoomSession.EntityFrameInputs(
                    frameCounter,
                    new RoomSession.LinkInputs(
                            new RoomSession.LinkPosition(
                                    link == null ? 0x08 : link.romEntityX(),
                                    link == null ? 0x10 : link.romEntityY(),
                                    link == null ? 0x00 : link.romEntityZ(),
                                    link == null ? 0x00 : link.direction(),
                                    link == null ? 0x00 : link.romCollisionType()),
                            new RoomSession.LinkMotion(
                                    link != null && link.isAirborne(),
                                    link == null ? 0x02 : link.romMotionState(),
                                    link != null && link.isUsingShield(),
                                    playerState == null
                                            ? 0
                                            : link != null && link.isCollisionDamageImmune()
                                                    ? 1
                                                    : playerState.invincibilityCounter(),
                                    link == null ? 0 : link.romSpeedX(),
                                    link == null ? 0 : link.romSpeedY()),
                            link == null ? 0 : link.romAttackStepAnimationCountdown(),
                            link == null ? 0 : link.romFinalPositionX(),
                            link != null && link.debugNoClip(),
                            link == null ? 0 : link.zVelocity()),
                    new RoomSession.PlayerInputs(
                            new RoomSession.ProgressInputs(
                                    playerState.heartPieces(),
                                    playerState != null && playerState.birdKeyCount() != 0,
                                    playerState != null && playerState.tailKeyCount() != 0,
                                    playerState == null ? 0 : playerState.tradeSequenceItem(),
                                    playerState != null && playerState.hasToadstool(),
                                    playerState == null ? 0 : playerState.magicPowderCount(),
                                    playerState == null ? 0 : playerState.goldenLeavesCount()),
                            new RoomSession.InventoryInputs(
                                    playerState == null ? PlayerState.INVENTORY_EMPTY : playerState.itemA(),
                                    playerState == null ? PlayerState.INVENTORY_EMPTY : playerState.itemB(),
                                    playerState != null && playerState.tunicType() == PlayerState.TUNIC_RED,
                                    playerState != null
                                            && playerState.activePowerUp()
                                                    == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER),
                            new RoomSession.PlayerLevels(
                                    playerState == null ? 1 : playerState.shieldLevel(),
                                    playerState == null ? 1 : playerState.swordLevel(),
                                    playerState == null ? 1 : playerState.powerBraceletLevel()),
                            new RoomSession.PlayerHealth(
                                    playerState.maxHearts(),
                                    playerState.health(),
                                    playerState.activePowerUp() != PlayerState.ACTIVE_POWER_UP_NONE,
                                    playerState.isOnLowHealth())),
                    new RoomSession.UiInputs(
                            dialogController != null && dialogController.isActive(),
                            inventoryMenu != null && inventoryMenu.inventoryAppearing(),
                            dialogController == null ? 0 : dialogController.dialogCooldown(),
                            inventoryMenu == null ? 0x80 : inventoryMenu.windowY(),
                            dialogController == null ? 0 : dialogController.choiceSelectionIndex(),
                            musicPlayer != null && musicPlayer.isMusicPlaying()),
                    new RoomSession.ButtonInputs(
                            inputState != null && inputConfig != null && inputState.isDown(inputConfig.aKey()),
                            inputState != null && inputConfig != null && inputState.isDown(inputConfig.bKey()),
                            inputState != null
                                    && inputConfig != null
                                    && (inputState.isDown(inputConfig.upKey())
                                            || inputState.isDown(inputConfig.downKey())
                                            || inputState.isDown(inputConfig.leftKey())
                                            || inputState.isDown(inputConfig.rightKey())
                                            || inputState.isDown(inputConfig.aKey())
                                            || inputState.isDown(inputConfig.bKey())
                                            || inputState.isDown(inputConfig.selectKey())
                                            || inputState.isDown(inputConfig.menuOpenKey())),
                            link == null ? 0 : link.romPressedButtonsMask(),
                            lift.isPowerBraceletButtonHeld(),
                            lift.isBombButtonHeld()),
                    new RoomSession.SwordBox(
                            swordBoxForEntityTick.active(),
                            swordBoxForEntityTick.x(),
                            swordBoxForEntityTick.width(),
                            swordBoxForEntityTick.y(),
                            swordBoxForEntityTick.height())));
            var results = roomSession.entityFrameResults();
            if (link != null) {
                link.clearStandingOnSideViewEntity();
            }
            for (var write : results.link().consumePositionWrites()) {
                switch (write) {
                    case RoomSession.LinkWrite.PlatformWrite value -> {
                        var request = value.request();
                        if (link != null && request.standing()) {
                            link.apply(LinkCommandAdapter.from(request));
                            roomTransitionCoordinator.handleWarpAndIndoorBoundaries(link);
                            roomTransitionCoordinator.handleOverworldBoundary(link);
                            if (scrollController.isActive() || transitionController.isInputBlocked()) {
                                link.clearSideViewPlatformState();
                            }
                        }
                    }
                    case RoomSession.LinkWrite.FinalPositionWrite value -> {
                        var request = value.request();
                        if (link != null) link.apply(LinkCommandAdapter.from(request));
                        if (request.resetPegasusBoots() && link == null && playerState != null) {
                            playerState.setRunningWithPegasusBoots(false);
                        }
                    }
                    case RoomSession.LinkWrite.PotWrite value -> {
                        var request = value.request();
                        if (link != null) link.apply(LinkCommandAdapter.from(request));
                    }
                    default -> throw new IllegalStateException("Unexpected position write: " + write);
                }
            }
            for (var request : results.link().consumeLinkMotionBlockRequests()) {
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
            }
            for (var request : results.link().consumeWarpLinkStateRequests()) {
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
                Sword warpSword = equipmentController.activeSword();
                if (warpSword != null) {
                    warpSword.resetSpinAttack();
                }
            }
            if (results.link().consumeWorldLinkMotionBlockRequest() && link != null) {
                link.apply(linksawakening.entity.LinkCommand.Action.BLOCK_MOTION);
            }
            for (var request : results.link().consumeLinkFallenPoseRequests()) {
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
            }
            for (var request : results.link().consumeLinkFacingRequests()) {
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
            }
            for (var write : results.link().consumeAttackWrites()) {
                switch (write) {
                    case RoomSession.LinkWrite.AttackClearWrite value -> {
                        var request = value.request();
                        if (link != null) link.apply(LinkCommandAdapter.from(request));
                    }
                    case RoomSession.LinkWrite.AttackStepWrite value -> {
                        var request = value.request();
                        if (link != null) link.apply(LinkCommandAdapter.from(request));
                    }
                    default -> throw new IllegalStateException("Unexpected attack write: " + write);
                }
            }
            for (var request : results.link().consumeLinkHeldItemPoseRequests()) {
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
                Sword heldPoseSword = equipmentController.activeSword();
                if (heldPoseSword != null) {
                    heldPoseSword.resetSpinAttack();
                }
            }
            for (var request : results.link().consumeLinkSwordSpinPoseRequests()) {
                int acquisitionBaseDirection = link == null ? Link.DIRECTION_DOWN : link.direction();
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
                Sword acquisitionSword = itemRegistry == null
                        ? null
                        : itemRegistry.lookup(PlayerState.INVENTORY_SWORD) instanceof Sword sword ? sword : null;
                if (acquisitionSword != null) {
                    if (request.countdown() == 0x20) {
                        acquisitionSword.startSwordAcquisitionSpin(acquisitionBaseDirection);
                    } else if (acquisitionSword.spinAttackActive()) {
                        acquisitionSword.tick(false, frameCounter);
                    }
                }
            }
            for (var request : results.link().consumeLinkSwordFinalPoseRequests()) {
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
                if (itemRegistry != null && itemRegistry.lookup(PlayerState.INVENTORY_SWORD) instanceof Sword sword) {
                    sword.resetSpinAttack();
                }
            }
            for (var request : results.presentation().consumeScreenShakeRequests()) {
                scrollController.startScreenShake(request.countdown(), request.phase());
            }
            EnemyProjectileEventConsumer.consume(
                    projectileEvents,
                    playerState,
                    link,
                    gameplaySoundSink,
                    roomSession::resetGuardianAcornCounterOnHurt,
                    () -> musicDirector.restoreMusicAfterPowerUpEnds(roomSession));
            var entityCombatEvents = results.presentation().consumeEntityEvents();
            EnemyCombatEventConsumer.consume(entityCombatEvents, gameplaySoundSink, transientVfxSystem);
            for (var event : entityCombatEvents) {
                if (link != null
                        && event.linkDamage() == 0
                        && event.linkCollisionResponse().active()) {
                    link.apply(LinkCommandAdapter.from(event));
                }
            }
            int musicFadeOutCountdown = results.presentation().consumePendingMusicFadeOutCountdown();
            if (musicFadeOutCountdown != 0 && gameplayMusicController != null) {
                gameplayMusicController.requestFadeOut(musicFadeOutCountdown);
            }
            for (var reward : results.rewards().consumeChestRewardEvents()) {
                if (playerState != null) {
                    playerState.applyChestReward(reward.itemType());
                }
            }
            for (var reward : results.rewards().consumeKeyRewardEvents()) {
                if (playerState != null) {
                    playerState.applyChestReward(reward.itemType());
                }
            }
            for (var reward : results.rewards().consumeSlimeKeyRewardEvents()) {
                if (playerState != null) {
                    playerState.setGoldenLeavesCount(reward.goldenLeavesCount());
                }
            }
            for (var update : results.rewards().consumeHeartPieceUpdates()) {
                dialogController.setInteractionLocked(update.interactionLocked());
                if (update.incrementPiece()) playerState.setHeartPieces(playerState.heartPieces() + 1);
                if (update.newContainer()) playerState.completeHeartPieceContainer();
                if (update.tilesCommand() != 0) gpu.updateHeartPieceMeterTiles(romData, update.tilesCommand());
                if (update.complete()) gpu.updateTradingItemTiles(romData, playerState.tradeSequenceItem());
            }
            for (var reward : results.rewards().consumeHeartContainerRewards()) {
                if (playerState != null) {
                    playerState.applyHeartContainerReward();
                }
                musicDirector.playDirectMusic(0x18);
            }
            for (var request : results.rewards().consumeMadamMeowMeowFullHealRequests()) {
                if (playerState != null) {
                    playerState.requestFullHeal();
                }
            }
            for (var event : results.rewards().consumeRichardProgressEvents()) {
                if (playerState != null && event.kind() == RichardProgressEvent.Kind.RETURN_ALL_LEAVES) {
                    playerState.setGoldenLeavesCount(event.goldenLeavesCount());
                    if (gpu != null && romData != null) {
                        gpu.replaceSlimeKeyTilesByGoldenLeaf(romData);
                    }
                }
            }
            BeachSwordRewardConsumer.consume(roomSession, playerState);
            for (var reward : results.rewards().consumeToadstoolRewards()) {
                if (playerState != null) {
                    playerState.applyToadstoolReward();
                }
            }
            for (var event : results.rewards().consumeWitchExchangeEvents()) {
                if (playerState != null) {
                    playerState.beginWitchToadstoolExchange(event.inventorySlot());
                    // Only the B-slot branch clears wDialogGotItem/C1A8.
                    // The witch tests ownership, equipped item and held input,
                    // independently of the got-item presentation.
                    if (event.inventorySlot() == 0 && link != null) link.clearDialogGotItem();
                }
            }
            for (var reward : results.rewards().consumeWitchRewardEvents()) {
                if (playerState != null) {
                    playerState.applyWitchMagicPowderReward();
                }
            }
            roomSession.setBirdKeyOwned(playerState != null && playerState.birdKeyCount() != 0);
            roomSession.setTailKeyOwned(playerState != null && playerState.tailKeyCount() != 0);
            int chestMusicTrack = results.presentation().consumePendingMusicTrack();
            if (chestMusicTrack >= 0) {
                musicDirector.playDirectMusic(chestMusicTrack);
            }
            for (var event : results.presentation().consumeLikeLikeEvents()) {
                if (event.kind() == linksawakening.world.entity.event.LikeLikeEvent.Kind.CAPTURE) {
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
                            || event.linkSpeedX() != 0
                            || event.linkSpeedY() != 0;
                    if (hasLinkResponse) {
                        Sword interruptedSword = equipmentController.activeSword();
                        if (interruptedSword != null) {
                            interruptedSword.resetSpinAttack();
                        }
                    }
                    continue;
                }
                boolean hookshotPull = event.kind() == EntityProjectileEvent.Kind.HOOKSHOT_PULL;
                boolean hasLinkResponse =
                        event.linkIgnoreCollisionCountdown() != 0 || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
                if ((!hookshotPull && !hasLinkResponse) || link == null) {
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
            for (var request : results.link().consumeRoosterLinkStateRequests()) {
                if (link != null) {
                    link.apply(LinkCommandAdapter.from(request));
                }
            }
            for (var request : results.link().consumeHinoxLinkEffectRequests()) {
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
