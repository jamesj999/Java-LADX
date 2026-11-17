package linksawakening.app.gameplay.entity;

import static linksawakening.app.gameplay.GameplaySimulation.shouldTickRoomEntities;

import java.util.List;
import java.util.Objects;
import linksawakening.app.state.GameplaySounds;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Ticks room entities and applies their results in source order. Retained for one simulation. */
public final class EntityPhase {
    private final GameplaySounds sounds;
    private final InventoryController inventoryController;
    private final InventoryMenu inventoryMenu;
    private final Link playerLink;
    private final PlayerState playerState;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final TransitionController transitionController;
    private final EntityFrameInputsFactory inputs;
    private final EntityRewardApplier rewards;
    private final LinkWriteApplier linkWrites;
    private final GameplayCombatEvents combatEvents;
    private final EntityDialogRouting routing;

    /** Creates the retained entity phase and its combat-event drain for one session. */
    private final EntityFrameStages stages = new EntityFrameStages() {
        @Override
        public List<EntityProjectileEvent> tickEntities(Sword.CollisionBox b) {
            return EntityPhase.this.rooms.tickEntities(EntityPhase.this.inputs.borrow(b));
        }

        @Override
        public void positions() {
            EntityPhase.this.linkWrites.applyPositionWrites();
        }

        @Override
        public void motion() {
            EntityPhase.this.linkWrites.applyMotionWrites();
        }

        @Override
        public void attacks() {
            EntityPhase.this.linkWrites.applyAttackWrites();
        }

        @Override
        public void sword() {
            EntityPhase.this.linkWrites.applySwordWrites();
        }

        @Override
        public void combat(List<EntityProjectileEvent> events) {
            applyCombat(events);
        }

        @Override
        public void chests() {
            applyChestRewards();
        }

        @Override
        public void progress() {
            applyProgressRewards();
        }

        @Override
        public void witch() {
            applyWitchRewards();
        }

        @Override
        public void dialogs() {
            routeDialogs();
        }

        @Override
        public void projectiles(List<EntityProjectileEvent> events) {
            EntityPhase.this.linkWrites.applyProjectileResponses(events);
        }

        @Override
        public void finalEffects() {
            EntityPhase.this.linkWrites.applyFinalEffects();
        }
    };

    public EntityPhase(Inputs inputs) {
        this.sounds = Objects.requireNonNull(inputs.gameplaySoundSink);
        this.inventoryController = Objects.requireNonNull(inputs.inventoryController);
        this.inventoryMenu = Objects.requireNonNull(inputs.inventoryMenu);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        this.scrollController = Objects.requireNonNull(inputs.scrollController);
        this.transitionController = Objects.requireNonNull(inputs.transitionController);
        this.inputs = Objects.requireNonNull(inputs.inputs);
        this.rewards = Objects.requireNonNull(inputs.rewards);
        this.linkWrites = Objects.requireNonNull(inputs.linkWrites);
        this.combatEvents = Objects.requireNonNull(inputs.combatEvents);
        this.routing = Objects.requireNonNull(inputs.routing);
    }

    public void tick(Sword.CollisionBox b) {
        // The original calls AnimateEntities after Link movement and
        // room-transition application. Give handlers the same current
        // position and active-room snapshot.
        if (shouldTickRoomEntities(
                EntityPhase.this.scrollController.isActive(),
                EntityPhase.this.transitionController.isInputBlocked(),
                EntityPhase.this.inventoryController.shouldBlockOverworldInput()
                        && !EntityPhase.this.inventoryMenu.transition().worldHandlerActive(),
                EntityPhase.this.inventoryMenu.isTransitioning())) {
            stages.run(b);
        }
    }

    public void applyResults(List<EntityProjectileEvent> events) {
        stages.applyResults(events);
    }

    public void applyChestRewards() {
        EntityPhase.this.rewards.applyChestRewards();
    }

    public void routeDialogs() {
        var results = EntityPhase.this.rooms.entityFrameResults();
        EntityPhase.this
                .rooms
                .story()
                .setBirdKeyOwned(EntityPhase.this.playerState.progression().birdKeyCount() != 0);
        EntityPhase.this
                .rooms
                .story()
                .setTailKeyOwned(EntityPhase.this.playerState.progression().tailKeyCount() != 0);
        int chestMusicTrack = results.presentation().consumePendingMusicTrack();
        if (chestMusicTrack >= 0) {
            EntityPhase.this.routing.music(chestMusicTrack);
        }
        var likeLikeEvents = results.presentation().consumeLikeLikeEvents();
        for (int eventIndex = 0; eventIndex < likeLikeEvents.size(); eventIndex++) {
            LikeLikeEvent event = likeLikeEvents.get(eventIndex);

            if (event.kind() == LikeLikeEvent.Kind.CAPTURE) {
                if (event.stolenInventorySlot() == 0) {
                    EntityPhase.this.playerState.inventory().setItemB(InventoryItem.EMPTY.romValue());
                } else if (event.stolenInventorySlot() == 1) {
                    EntityPhase.this.playerState.inventory().setItemA(InventoryItem.EMPTY.romValue());
                }
                EntityPhase.this.playerLink.apply(LinkCommandAdapter.from(event));
            } else EntityPhase.this.playerLink.apply(LinkCommandAdapter.from(event));
        }
        EntityPhase.this.routing.dialogs();
        EntityPhase.this.routing.manbo();
        EntityPhase.this.routing.instrument();
    }

    private void applyCombat(List<EntityProjectileEvent> projectileEvents) {
        var results = EntityPhase.this.rooms.entityFrameResults();
        var screenShakeRequests = results.presentation().consumeScreenShakeRequests();
        for (int requestIndex = 0; requestIndex < screenShakeRequests.size(); requestIndex++) {
            ScreenShakeRequest request = screenShakeRequests.get(requestIndex);
            EntityPhase.this.scrollController.startScreenShake(request.countdown(), request.phase());
        }
        EnemyProjectileEventConsumer.consume(
                projectileEvents,
                EntityPhase.this.playerState,
                EntityPhase.this.playerLink,
                EntityPhase.this.sounds.sink(),
                combatEvents);
        results.presentation().drainEntityEvents(combatEvents);
    }

    private void applyProgressRewards() {
        EntityPhase.this.rewards.applyProgressRewards();
    }

    private void applyWitchRewards() {
        EntityPhase.this.rewards.applyWitchRewards();
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private GameplaySounds gameplaySoundSink;
        private InventoryController inventoryController;
        private InventoryMenu inventoryMenu;
        private Link link;
        private PlayerState playerState;
        private RoomSession roomSession;
        private ScrollController scrollController;
        private TransitionController transitionController;
        private EntityFrameInputsFactory inputs;
        private EntityRewardApplier rewards;
        private LinkWriteApplier linkWrites;
        private GameplayCombatEvents combatEvents;
        private EntityDialogRouting routing;

        public Inputs gameplaySoundSink(GameplaySounds value) {
            this.gameplaySoundSink = value;
            return this;
        }

        public Inputs inventoryController(InventoryController value) {
            this.inventoryController = value;
            return this;
        }

        public Inputs inventoryMenu(InventoryMenu value) {
            this.inventoryMenu = value;
            return this;
        }

        public Inputs link(Link value) {
            this.link = value;
            return this;
        }

        public Inputs playerState(PlayerState value) {
            this.playerState = value;
            return this;
        }

        public Inputs roomSession(RoomSession value) {
            this.roomSession = value;
            return this;
        }

        public Inputs scrollController(ScrollController value) {
            this.scrollController = value;
            return this;
        }

        public Inputs transitionController(TransitionController value) {
            this.transitionController = value;
            return this;
        }

        public Inputs inputs(EntityFrameInputsFactory value) {
            this.inputs = value;
            return this;
        }

        public Inputs rewards(EntityRewardApplier value) {
            this.rewards = value;
            return this;
        }

        public Inputs linkWrites(LinkWriteApplier value) {
            this.linkWrites = value;
            return this;
        }

        public Inputs combatEvents(GameplayCombatEvents value) {
            this.combatEvents = value;
            return this;
        }

        public Inputs routing(EntityDialogRouting value) {
            this.routing = value;
            return this;
        }
    }
}
