package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.Sword;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.state.InventoryItem;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.room.*;

final class EntityPhase {
    private final EntityRewardApplier rewards = new EntityRewardApplier();
    private boolean collaboratorsVerified;
    private GameplaySimulation combatContext;
    private RoomEntityState hurtState;
    private final RoomFrameDrain<linksawakening.world.entity.event.EntityCombatEvent> combatEvents = events -> {
        var c = combatContext;
        var entityCombatEvents = events;
        EnemyCombatEventConsumer.consume(entityCombatEvents, c.gameplaySoundSink(), c.transientVfxSystem());
        for (int eventIndex = 0; eventIndex < entityCombatEvents.size(); eventIndex++) {
            linksawakening.world.entity.event.EntityCombatEvent event = entityCombatEvents.get(eventIndex);

            if (event.linkDamage() == 0 && event.linkCollisionResponse().active()) {
                c.link().apply(LinkCommandAdapter.from(event));
            }
        }
    };
    private final Runnable onContactHurt = () -> hurtState.resetGuardianAcornCounterOnHurt();
    private final Runnable onPowerUpEnded =
            () -> combatContext.musicDirector().restoreMusicAfterPowerUpEnds(combatContext.roomSession());
    private final LinkWriteApplier linkWrites = new LinkWriteApplier();
    private final EntityDialogRouting routing;

    EntityPhase() {
        this(EntityDialogRouting.DIRECT);
    }

    EntityPhase(EntityDialogRouting routing) {
        this.routing = routing;
    }

    private final EntityFrameStages stages = new EntityFrameStages() {
        public java.util.List<EntityProjectileEvent> tickEntities(GameplaySimulation c, Sword.CollisionBox b) {
            return c.roomSession().tickEntities(EntityFrameInputsFactory.create(c, b));
        }

        public void positions(GameplaySimulation c) {
            linkWrites.applyPositionWrites(c);
        }

        public void motion(GameplaySimulation c) {
            LinkWriteApplier.applyMotionWrites(c);
        }

        public void attacks(GameplaySimulation c) {
            linkWrites.applyAttackWrites(c);
        }

        public void sword(GameplaySimulation c) {
            LinkWriteApplier.applySwordWrites(c);
        }

        public void combat(GameplaySimulation c, java.util.List<EntityProjectileEvent> events) {
            applyCombat(c, events);
        }

        public void chests(GameplaySimulation c) {
            applyChestRewards(c);
        }

        public void progress(GameplaySimulation c) {
            applyProgressRewards(c);
        }

        public void witch(GameplaySimulation c) {
            applyWitchRewards(c);
        }

        public void dialogs(GameplaySimulation c) {
            routeDialogs(c);
        }

        public void projectiles(GameplaySimulation c, java.util.List<EntityProjectileEvent> events) {
            LinkWriteApplier.applyProjectileResponses(c, events);
        }

        public void finalEffects(GameplaySimulation c) {
            LinkWriteApplier.applyFinalEffects(c);
        }
    };

    void tick(GameplaySimulation c, Sword.CollisionBox b) {
        if (!collaboratorsVerified) {
            assert requiredCollaboratorsPresent(c);
            collaboratorsVerified = true;
        }
        // The original calls AnimateEntities after Link movement and
        // room-transition application. Give handlers the same current
        // position and active-room snapshot.
        if (shouldTickRoomEntities(
                c.scrollController().isActive(),
                c.transitionController().isInputBlocked(),
                c.inventoryController().shouldBlockOverworldInput()
                        && !c.inventoryMenu().transition().worldHandlerActive(),
                c.inventoryMenu().isTransitioning())) {
            stages.run(c, b);
        }
    }

    private void applyCombat(GameplaySimulation c, java.util.List<EntityProjectileEvent> projectileEvents) {
        var results = c.roomSession().entityFrameResults();
        var screenShakeRequests = results.presentation().consumeScreenShakeRequests();
        for (int requestIndex = 0; requestIndex < screenShakeRequests.size(); requestIndex++) {
            linksawakening.world.entity.event.ScreenShakeRequest request = screenShakeRequests.get(requestIndex);
            c.scrollController().startScreenShake(request.countdown(), request.phase());
        }
        hurtState = c.roomSession().entityState();
        combatContext = c;
        EnemyProjectileEventConsumer.consume(
                projectileEvents, c.playerState(), c.link(), c.gameplaySoundSink(), onContactHurt, onPowerUpEnded);
        results.presentation().drainEntityEvents(combatEvents);
    }

    void applyChestRewards(GameplaySimulation c) {
        rewards.applyChestRewards(c);
    }

    private void applyProgressRewards(GameplaySimulation c) {
        rewards.applyProgressRewards(c);
    }

    private void applyWitchRewards(GameplaySimulation c) {
        rewards.applyWitchRewards(c);
    }

    void routeDialogs(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        c.roomSession().story().setBirdKeyOwned(c.playerState().birdKeyCount() != 0);
        c.roomSession().story().setTailKeyOwned(c.playerState().tailKeyCount() != 0);
        int chestMusicTrack = results.presentation().consumePendingMusicTrack();
        if (chestMusicTrack >= 0) {
            routing.music(c, chestMusicTrack);
        }
        var likeLikeEvents = results.presentation().consumeLikeLikeEvents();
        for (int eventIndex = 0; eventIndex < likeLikeEvents.size(); eventIndex++) {
            linksawakening.world.entity.event.LikeLikeEvent event = likeLikeEvents.get(eventIndex);

            if (event.kind() == linksawakening.world.entity.event.LikeLikeEvent.Kind.CAPTURE) {
                if (event.stolenInventorySlot() == 0) {
                    c.playerState().setItemB(InventoryItem.EMPTY.romValue());
                } else if (event.stolenInventorySlot() == 1) {
                    c.playerState().setItemA(InventoryItem.EMPTY.romValue());
                }
                c.link().apply(LinkCommandAdapter.from(event));
            } else c.link().apply(LinkCommandAdapter.from(event));
        }
        routing.dialogs(c);
        routing.manbo(c);
        routing.instrument(c);
    }

    private boolean requiredCollaboratorsPresent(GameplaySimulation c) {
        return c.roomSession() != null
                && c.link() != null
                && c.playerState() != null
                && c.gpu() != null
                && c.romData() != null
                && c.inputState() != null
                && c.inputConfig() != null
                && c.inventoryMenu() != null
                && c.inventoryController() != null
                && c.dialogController() != null
                && c.musicPlayer() != null
                && c.gameplayMusicController() != null
                && c.musicDirector() != null
                && c.itemRegistry() != null
                && c.equipmentController() != null
                && c.lift() != null
                && c.dialogInteractions() != null
                && c.scrollController() != null
                && c.transitionController() != null
                && c.roomTransitionCoordinator() != null;
    }
}
