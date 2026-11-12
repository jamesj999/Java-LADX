package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;

import java.util.List;
import java.util.Objects;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.state.InventoryItem;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;

final class EntityPhase {
    private final EntityRewardApplier rewards = new EntityRewardApplier();
    private final GameplayCombatEvents combatEvents;
    private final LinkWriteApplier linkWrites = new LinkWriteApplier();
    private final EntityDialogRouting routing;

    EntityPhase(GameplayCombatEvents combatEvents, EntityDialogRouting routing) {
        this.combatEvents = Objects.requireNonNull(combatEvents);
        this.routing = Objects.requireNonNull(routing);
    }

    private final EntityFrameStages stages = new EntityFrameStages() {
        public List<EntityProjectileEvent> tickEntities(GameplaySimulation c, Sword.CollisionBox b) {
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

        public void combat(GameplaySimulation c, List<EntityProjectileEvent> events) {
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

        public void projectiles(GameplaySimulation c, List<EntityProjectileEvent> events) {
            LinkWriteApplier.applyProjectileResponses(c, events);
        }

        public void finalEffects(GameplaySimulation c) {
            LinkWriteApplier.applyFinalEffects(c);
        }
    };

    void tick(GameplaySimulation c, Sword.CollisionBox b) {
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

    private void applyCombat(GameplaySimulation c, List<EntityProjectileEvent> projectileEvents) {
        var results = c.roomSession().entityFrameResults();
        var screenShakeRequests = results.presentation().consumeScreenShakeRequests();
        for (int requestIndex = 0; requestIndex < screenShakeRequests.size(); requestIndex++) {
            ScreenShakeRequest request = screenShakeRequests.get(requestIndex);
            c.scrollController().startScreenShake(request.countdown(), request.phase());
        }
        EnemyProjectileEventConsumer.consume(
                projectileEvents, c.playerState(), c.link(), c.gameplaySoundSink(), combatEvents);
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
        c.roomSession().story().setBirdKeyOwned(c.playerState().progression().birdKeyCount() != 0);
        c.roomSession().story().setTailKeyOwned(c.playerState().progression().tailKeyCount() != 0);
        int chestMusicTrack = results.presentation().consumePendingMusicTrack();
        if (chestMusicTrack >= 0) {
            routing.music(c, chestMusicTrack);
        }
        var likeLikeEvents = results.presentation().consumeLikeLikeEvents();
        for (int eventIndex = 0; eventIndex < likeLikeEvents.size(); eventIndex++) {
            LikeLikeEvent event = likeLikeEvents.get(eventIndex);

            if (event.kind() == LikeLikeEvent.Kind.CAPTURE) {
                if (event.stolenInventorySlot() == 0) {
                    c.playerState().inventory().setItemB(InventoryItem.EMPTY.romValue());
                } else if (event.stolenInventorySlot() == 1) {
                    c.playerState().inventory().setItemA(InventoryItem.EMPTY.romValue());
                }
                c.link().apply(LinkCommandAdapter.from(event));
            } else c.link().apply(LinkCommandAdapter.from(event));
        }
        routing.dialogs(c);
        routing.manbo(c);
        routing.instrument(c);
    }
}
