package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.Sword;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.room.*;

final class EntityPhase {
    void tick(GameplaySimulation c, Sword.CollisionBox b) {
        assert requiredCollaboratorsPresent(c);
        // The original calls AnimateEntities after Link movement and
        // room-transition application. Give handlers the same current
        // position and active-room snapshot.
        if (shouldTickRoomEntities(
                c.scrollController().isActive(),
                c.transitionController().isInputBlocked(),
                c.inventoryController().shouldBlockOverworldInput()
                        && !c.inventoryMenu().transition().worldHandlerActive(),
                c.inventoryMenu().isTransitioning())) {
            var projectileEvents = c.roomSession().tickEntities(buildInputs(c, b));
            applyPositionWrites(c);
            applyMotionWrites(c);
            applyAttackWrites(c);
            applySwordWrites(c);
            applyCombat(c, projectileEvents);
            applyChestRewards(c);
            applyProgressRewards(c);
            applyWitchRewards(c);
            routeDialogs(c);
            applyProjectileResponses(c, projectileEvents);
            applyFinalEffects(c);
        }
    }

    private EntityFrameInputs buildInputs(GameplaySimulation c, Sword.CollisionBox b) {
        return new EntityFrameInputs(
                c.frameCounter(),
                buildLinkInputs(c),
                buildPlayerInputs(c),
                buildUiInputs(c),
                buildButtonInputs(c),
                buildSwordBox(c, b));
    }

    private void applyPositionWrites(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        c.link().clearStandingOnSideViewEntity();
        for (var write : results.link().consumePositionWrites()) {
            switch (write) {
                case LinkWrite.PlatformWrite value -> {
                    var request = value.request();
                    if (request.standing()) {
                        c.link().apply(LinkCommandAdapter.from(request));
                        c.roomTransitionCoordinator().handleWarpAndIndoorBoundaries(c.link());
                        c.roomTransitionCoordinator().handleOverworldBoundary(c.link());
                        if (c.scrollController().isActive()
                                || c.transitionController().isInputBlocked()) {
                            c.link().clearSideViewPlatformState();
                        }
                    }
                }
                case LinkWrite.FinalPositionWrite value -> {
                    var request = value.request();
                    c.link().apply(LinkCommandAdapter.from(request));
                }
                case LinkWrite.PotWrite value -> {
                    var request = value.request();
                    c.link().apply(LinkCommandAdapter.from(request));
                }
                default -> throw new IllegalStateException("Unexpected position write: " + write);
            }
        }
    }

    private void applyMotionWrites(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        for (var request : results.link().consumeLinkMotionBlockRequests())
            c.link().apply(LinkCommandAdapter.from(request));
        for (var request : results.link().consumeWarpLinkStateRequests()) {
            c.link().apply(LinkCommandAdapter.from(request));
            resetSword(c);
        }
        if (results.link().consumeWorldLinkMotionBlockRequest()) {
            c.link().apply(linksawakening.entity.LinkCommand.Action.BLOCK_MOTION);
        }
        for (var request : results.link().consumeLinkFallenPoseRequests())
            c.link().apply(LinkCommandAdapter.from(request));
        for (var request : results.link().consumeLinkFacingRequests()) c.link().apply(LinkCommandAdapter.from(request));
    }

    private void applyAttackWrites(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        for (var write : results.link().consumeAttackWrites()) {
            switch (write) {
                case LinkWrite.AttackClearWrite value -> {
                    var request = value.request();
                    c.link().apply(LinkCommandAdapter.from(request));
                }
                case LinkWrite.AttackStepWrite value -> {
                    var request = value.request();
                    c.link().apply(LinkCommandAdapter.from(request));
                }
                default -> throw new IllegalStateException("Unexpected attack write: " + write);
            }
        }
        for (var request : results.link().consumeLinkHeldItemPoseRequests()) {
            c.link().apply(LinkCommandAdapter.from(request));
            resetSword(c);
        }
    }

    private void applySwordWrites(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        for (var request : results.link().consumeLinkSwordSpinPoseRequests()) {
            int acquisitionBaseDirection = c.link().direction();
            c.link().apply(LinkCommandAdapter.from(request));
            if (c.itemRegistry().lookup(PlayerState.INVENTORY_SWORD) instanceof Sword acquisitionSword) {
                if (request.countdown() == 0x20) {
                    acquisitionSword.startSwordAcquisitionSpin(acquisitionBaseDirection);
                } else if (acquisitionSword.spinAttackActive()) {
                    acquisitionSword.tick(false, c.frameCounter());
                }
            }
        }
        for (var request : results.link().consumeLinkSwordFinalPoseRequests()) {
            c.link().apply(LinkCommandAdapter.from(request));
            if (c.itemRegistry().lookup(PlayerState.INVENTORY_SWORD) instanceof Sword sword) {
                sword.resetSpinAttack();
            }
        }
    }

    private void applyCombat(GameplaySimulation c, java.util.List<EntityProjectileEvent> projectileEvents) {
        var results = c.roomSession().entityFrameResults();
        for (var request : results.presentation().consumeScreenShakeRequests())
            c.scrollController().startScreenShake(request.countdown(), request.phase());
        EnemyProjectileEventConsumer.consume(
                projectileEvents,
                c.playerState(),
                c.link(),
                c.gameplaySoundSink(),
                c.roomSession()::resetGuardianAcornCounterOnHurt,
                () -> c.musicDirector().restoreMusicAfterPowerUpEnds(c.roomSession()));
        var entityCombatEvents = results.presentation().consumeEntityEvents();
        EnemyCombatEventConsumer.consume(entityCombatEvents, c.gameplaySoundSink(), c.transientVfxSystem());
        for (var event : entityCombatEvents) {
            if (event.linkDamage() == 0 && event.linkCollisionResponse().active()) {
                c.link().apply(LinkCommandAdapter.from(event));
            }
        }
    }

    private void applyChestRewards(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        int musicFadeOutCountdown = results.presentation().consumePendingMusicFadeOutCountdown();
        if (musicFadeOutCountdown != 0) {
            c.gameplayMusicController().requestFadeOut(musicFadeOutCountdown);
        }
        for (var reward : results.rewards().consumeChestRewardEvents())
            c.playerState().applyChestReward(reward.itemType());
        for (var reward : results.rewards().consumeKeyRewardEvents())
            c.playerState().applyChestReward(reward.itemType());
        for (var reward : results.rewards().consumeSlimeKeyRewardEvents())
            c.playerState().setGoldenLeavesCount(reward.goldenLeavesCount());
        for (var update : results.rewards().consumeHeartPieceUpdates()) {
            c.dialogController().setInteractionLocked(update.interactionLocked());
            if (update.incrementPiece())
                c.playerState().setHeartPieces(c.playerState().heartPieces() + 1);
            if (update.newContainer()) c.playerState().completeHeartPieceContainer();
            if (update.tilesCommand() != 0) c.gpu().updateHeartPieceMeterTiles(c.romData(), update.tilesCommand());
            if (update.complete())
                c.gpu().updateTradingItemTiles(c.romData(), c.playerState().tradeSequenceItem());
        }
        for (var reward : results.rewards().consumeHeartContainerRewards()) {
            c.playerState().applyHeartContainerReward();
            c.musicDirector().playDirectMusic(0x18);
        }
    }

    private void applyProgressRewards(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        for (var request : results.rewards().consumeMadamMeowMeowFullHealRequests())
            c.playerState().requestFullHeal();
        for (var event : results.rewards().consumeRichardProgressEvents()) {
            if (event.kind() == RichardProgressEvent.Kind.RETURN_ALL_LEAVES) {
                c.playerState().setGoldenLeavesCount(event.goldenLeavesCount());
                c.gpu().replaceSlimeKeyTilesByGoldenLeaf(c.romData());
            }
        }
        BeachSwordRewardConsumer.consume(c.roomSession(), c.playerState());
    }

    private void applyWitchRewards(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        for (var reward : results.rewards().consumeToadstoolRewards())
            c.playerState().applyToadstoolReward();
        for (var event : results.rewards().consumeWitchExchangeEvents()) {
            c.playerState().beginWitchToadstoolExchange(event.inventorySlot());
            // Only the B-slot branch clears wDialogGotItem/C1A8.
            // The witch tests ownership, equipped item and held input,
            // independently of the got-item presentation.
            if (event.inventorySlot() == 0) c.link().clearDialogGotItem();
        }
        for (var reward : results.rewards().consumeWitchRewardEvents())
            c.playerState().applyWitchMagicPowderReward();
    }

    private void routeDialogs(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        c.roomSession().setBirdKeyOwned(c.playerState().birdKeyCount() != 0);
        c.roomSession().setTailKeyOwned(c.playerState().tailKeyCount() != 0);
        int chestMusicTrack = results.presentation().consumePendingMusicTrack();
        if (chestMusicTrack >= 0) {
            c.musicDirector().playDirectMusic(chestMusicTrack);
        }
        for (var event : results.presentation().consumeLikeLikeEvents()) {
            if (event.kind() == linksawakening.world.entity.event.LikeLikeEvent.Kind.CAPTURE) {
                if (event.stolenInventorySlot() == 0) {
                    c.playerState().setItemB(PlayerState.INVENTORY_EMPTY);
                } else if (event.stolenInventorySlot() == 1) {
                    c.playerState().setItemA(PlayerState.INVENTORY_EMPTY);
                }
                c.link().apply(LinkCommandAdapter.from(event));
            } else c.link().apply(LinkCommandAdapter.from(event));
        }
        c.dialogInteractions().openEntityDialogRequests();
        if (c.roomTransitionCoordinator().handlePendingManboTransition(c.link())) {
            c.gameplaySoundSink().play(linksawakening.gameplay.GameplaySoundEvent.MANBO_WARP);
        }
        c.roomTransitionCoordinator().handlePendingInstrumentTransition(c.link());
    }

    private void applyProjectileResponses(
            GameplaySimulation c, java.util.List<EntityProjectileEvent> projectileEvents) {
        for (var event : projectileEvents) {
            // Accepted LINK_DAMAGE responses are applied by the
            // projectile consumer before PlayerState becomes
            // invincible. Keep this pass for shield reflection,
            // sword-poke, and hookshot responses only.
            if (event.kind() == EntityProjectileEvent.Kind.LINK_DAMAGE) {
                boolean hasLinkResponse =
                        event.linkIgnoreCollisionCountdown() != 0 || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
                if (hasLinkResponse) {
                    resetSword(c);
                }
                continue;
            }
            boolean hookshotPull = event.kind() == EntityProjectileEvent.Kind.HOOKSHOT_PULL;
            boolean hasLinkResponse =
                    event.linkIgnoreCollisionCountdown() != 0 || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
            if (!hookshotPull && !hasLinkResponse) {
                continue;
            }
            if (hookshotPull) {
                c.link().apply(LinkCommandAdapter.from(event));
                continue;
            }
            c.link().apply(LinkCommandAdapter.from(event));
            resetSword(c);
        }
    }

    private void applyFinalEffects(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        for (var request : results.link().consumeRoosterLinkStateRequests())
            c.link().apply(LinkCommandAdapter.from(request));
        for (var request : results.link().consumeHinoxLinkEffectRequests()) {
            c.link().apply(LinkCommandAdapter.from(request));
            if (request.damage() != 0) {
                c.playerState().applyRomEnemyDamage(request.damage());
            }
        }
        c.lift().synchronizeLinkLiftedPresentation();
    }

    private LinkInputs buildLinkInputs(GameplaySimulation c) {
        var l = c.link();
        var p = c.playerState();
        return new LinkInputs(
                new LinkPosition(l.romEntityX(), l.romEntityY(), l.romEntityZ(), l.direction(), l.romCollisionType()),
                new LinkMotion(
                        l.isAirborne(),
                        l.romMotionState(),
                        l.isUsingShield(),
                        l.isCollisionDamageImmune() ? 1 : p.invincibilityCounter(),
                        l.romSpeedX(),
                        l.romSpeedY()),
                l.romAttackStepAnimationCountdown(),
                l.romFinalPositionX(),
                l.debugNoClip(),
                l.zVelocity());
    }

    private PlayerInputs buildPlayerInputs(GameplaySimulation c) {
        var p = c.playerState();
        return new PlayerInputs(
                new ProgressInputs(
                        p.heartPieces(),
                        p.birdKeyCount() != 0,
                        p.tailKeyCount() != 0,
                        p.tradeSequenceItem(),
                        p.hasToadstool(),
                        p.magicPowderCount(),
                        p.goldenLeavesCount()),
                new InventoryInputs(
                        p.itemA(),
                        p.itemB(),
                        p.tunicType() == PlayerState.TUNIC_RED,
                        p.activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER),
                new PlayerLevels(p.shieldLevel(), p.swordLevel(), p.powerBraceletLevel()),
                new PlayerHealth(
                        p.maxHearts(),
                        p.health(),
                        p.activePowerUp() != PlayerState.ACTIVE_POWER_UP_NONE,
                        p.isOnLowHealth()));
    }

    private UiInputs buildUiInputs(GameplaySimulation c) {
        var d = c.dialogController();
        var m = c.inventoryMenu();
        var p = c.musicPlayer();
        return new UiInputs(
                d.isActive(),
                m.inventoryAppearing(),
                d.dialogCooldown(),
                m.windowY(),
                d.choiceSelectionIndex(),
                p.isMusicPlaying());
    }

    private ButtonInputs buildButtonInputs(GameplaySimulation c) {
        var i = c.inputState();
        var k = c.inputConfig();
        var l = c.link();
        var lift = c.lift();
        return new ButtonInputs(
                i.isDown(k.aKey()),
                i.isDown(k.bKey()),
                (i.isDown(k.upKey())
                        || i.isDown(k.downKey())
                        || i.isDown(k.leftKey())
                        || i.isDown(k.rightKey())
                        || i.isDown(k.aKey())
                        || i.isDown(k.bKey())
                        || i.isDown(k.selectKey())
                        || i.isDown(k.menuOpenKey())),
                l.romPressedButtonsMask(),
                lift.isPowerBraceletButtonHeld(),
                lift.isBombButtonHeld());
    }

    private SwordBox buildSwordBox(GameplaySimulation c, Sword.CollisionBox b) {
        return new SwordBox(b.active(), b.x(), b.width(), b.y(), b.height());
    }

    private void resetSword(GameplaySimulation c) {
        Sword sword = c.equipmentController().activeSword();
        if (sword != null) sword.resetSpinAttack();
    }

    private boolean requiredCollaboratorsPresent(GameplaySimulation c) {
        return java.util.stream.Stream.of(
                        c.roomSession(),
                        c.link(),
                        c.playerState(),
                        c.gpu(),
                        c.romData(),
                        c.inputState(),
                        c.inputConfig(),
                        c.inventoryMenu(),
                        c.inventoryController(),
                        c.dialogController(),
                        c.musicPlayer(),
                        c.gameplayMusicController(),
                        c.musicDirector(),
                        c.itemRegistry(),
                        c.equipmentController(),
                        c.lift(),
                        c.dialogInteractions(),
                        c.scrollController(),
                        c.transitionController(),
                        c.roomTransitionCoordinator())
                .allMatch(java.util.Objects::nonNull);
    }
}
