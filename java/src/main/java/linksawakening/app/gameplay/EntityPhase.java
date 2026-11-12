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
            var projectileEvents = c.roomSession().tickEntities(EntityFrameInputsFactory.create(c, b));
            LinkWriteApplier.applyPositionWrites(c);
            LinkWriteApplier.applyMotionWrites(c);
            LinkWriteApplier.applyAttackWrites(c);
            LinkWriteApplier.applySwordWrites(c);
            applyCombat(c, projectileEvents);
            applyChestRewards(c);
            applyProgressRewards(c);
            applyWitchRewards(c);
            routeDialogs(c);
            LinkWriteApplier.applyProjectileResponses(c, projectileEvents);
            LinkWriteApplier.applyFinalEffects(c);
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
                c.roomSession().entityState()::resetGuardianAcornCounterOnHurt,
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
        c.roomSession().story().setBirdKeyOwned(c.playerState().birdKeyCount() != 0);
        c.roomSession().story().setTailKeyOwned(c.playerState().tailKeyCount() != 0);
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
