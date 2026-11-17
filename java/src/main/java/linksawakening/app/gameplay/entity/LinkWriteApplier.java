package linksawakening.app.gameplay.entity;

import java.util.List;
import java.util.Objects;
import linksawakening.app.gameplay.interaction.LiftInteraction;
import linksawakening.app.state.FrameClock;
import linksawakening.entity.Link;
import linksawakening.entity.LinkCommand;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.room.LinkWriteSink;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Applies entity-produced Link writes in source order. One instance retains the position-write adapter per
 * session.
 */
public final class LinkWriteApplier implements LinkWriteSink.Positions, LinkWriteSink.Attacks {
    private final EquipmentController equipmentController;
    private final FrameClock clock;
    private final ItemRegistry itemRegistry;
    private final LiftInteraction lift;
    private final Link playerLink;
    private final PlayerState playerState;
    private final RoomSession rooms;
    private final RoomTransitionCoordinator roomTransitionCoordinator;
    private final ScrollController scrollController;
    private final TransitionController transitionController;

    public LinkWriteApplier(Inputs inputs) {
        this.equipmentController = Objects.requireNonNull(inputs.equipmentController);
        this.clock = Objects.requireNonNull(inputs.frameCounter);
        this.itemRegistry = Objects.requireNonNull(inputs.itemRegistry);
        this.lift = Objects.requireNonNull(inputs.lift);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        this.roomTransitionCoordinator = Objects.requireNonNull(inputs.roomTransitionCoordinator);
        this.scrollController = Objects.requireNonNull(inputs.scrollController);
        this.transitionController = Objects.requireNonNull(inputs.transitionController);
    }

    public void applyPositionWrites() {
        this.playerLink.sideView().clearStandingOnSideViewEntity();
        this.rooms.entityFrameResults().link().drainPositionWrites(this);
    }

    @Override
    public void platform(SideViewPlatformLinkRequest request) {
        if (request.standing()) {
            this.playerLink.apply(LinkCommandAdapter.from(request));
            this.roomTransitionCoordinator.handleWarpAndIndoorBoundaries(this.playerLink);
            this.roomTransitionCoordinator.handleOverworldBoundary(this.playerLink);
            if (this.scrollController.isActive() || this.transitionController.isInputBlocked())
                this.playerLink.sideView().clearSideViewPlatformState();
        }
    }

    @Override
    public void position(LinkFinalPositionRequest request) {
        this.playerLink.apply(LinkCommandAdapter.from(request));
    }

    @Override
    public void pot(SideViewPotLinkRequest request) {
        this.playerLink.apply(LinkCommandAdapter.from(request));
    }

    @Override
    public void clear(LinkAttackClearRequest request) {
        this.playerLink.apply(LinkCommandAdapter.from(request));
    }

    @Override
    public void step(LinkAttackStepRequest request) {
        this.playerLink.apply(LinkCommandAdapter.from(request));
    }

    public void applyMotionWrites() {
        var results = this.rooms.entityFrameResults();
        var linkMotionBlockRequests = results.link().consumeLinkMotionBlockRequests();
        for (int requestIndex = 0; requestIndex < linkMotionBlockRequests.size(); requestIndex++) {
            LinkMotionBlockRequest request = linkMotionBlockRequests.get(requestIndex);
            this.playerLink.apply(LinkCommandAdapter.from(request));
        }
        var warpLinkStateRequests = results.link().consumeWarpLinkStateRequests();
        for (int requestIndex = 0; requestIndex < warpLinkStateRequests.size(); requestIndex++) {
            WarpLinkStateRequest request = warpLinkStateRequests.get(requestIndex);

            this.playerLink.apply(LinkCommandAdapter.from(request));
            resetSword();
        }
        if (results.link().consumeWorldLinkMotionBlockRequest()) {
            this.playerLink.apply(LinkCommand.Action.BLOCK_MOTION);
        }
        var linkFallenPoseRequests = results.link().consumeLinkFallenPoseRequests();
        for (int requestIndex = 0; requestIndex < linkFallenPoseRequests.size(); requestIndex++) {
            LinkFallenPoseRequest request = linkFallenPoseRequests.get(requestIndex);
            this.playerLink.apply(LinkCommandAdapter.from(request));
        }
        var linkFacingRequests = results.link().consumeLinkFacingRequests();
        for (int requestIndex = 0; requestIndex < linkFacingRequests.size(); requestIndex++) {
            LinkFacingRequest request = linkFacingRequests.get(requestIndex);
            this.playerLink.apply(LinkCommandAdapter.from(request));
        }
    }

    public void applyAttackWrites() {
        var results = this.rooms.entityFrameResults();
        results.link().drainAttackWrites(this);
        var linkHeldItemPoseRequests = results.link().consumeLinkHeldItemPoseRequests();
        for (int requestIndex = 0; requestIndex < linkHeldItemPoseRequests.size(); requestIndex++) {
            LinkHeldItemPoseRequest request = linkHeldItemPoseRequests.get(requestIndex);

            this.playerLink.apply(LinkCommandAdapter.from(request));
            resetSword();
        }
    }

    public void applySwordWrites() {
        var results = this.rooms.entityFrameResults();
        var linkSwordSpinPoseRequests = results.link().consumeLinkSwordSpinPoseRequests();
        for (int requestIndex = 0; requestIndex < linkSwordSpinPoseRequests.size(); requestIndex++) {
            LinkSwordSpinPoseRequest request = linkSwordSpinPoseRequests.get(requestIndex);

            int acquisitionBaseDirection = this.playerLink.animation().direction();
            this.playerLink.apply(LinkCommandAdapter.from(request));
            if (this.itemRegistry.item(InventoryItem.SWORD.romValue()) instanceof Sword acquisitionSword) {
                if (request.countdown() == 0x20) {
                    acquisitionSword.startSwordAcquisitionSpin(acquisitionBaseDirection);
                } else if (acquisitionSword.spinAttackActive()) {
                    acquisitionSword.tick(false, this.clock.value());
                }
            }
        }
        var linkSwordFinalPoseRequests = results.link().consumeLinkSwordFinalPoseRequests();
        for (int requestIndex = 0; requestIndex < linkSwordFinalPoseRequests.size(); requestIndex++) {
            LinkSwordFinalPoseRequest request = linkSwordFinalPoseRequests.get(requestIndex);

            this.playerLink.apply(LinkCommandAdapter.from(request));
            if (this.itemRegistry.item(InventoryItem.SWORD.romValue()) instanceof Sword sword) {
                sword.resetSpinAttack();
            }
        }
    }

    public void applyProjectileResponses(List<EntityProjectileEvent> projectileEvents) {
        for (int eventIndex = 0; eventIndex < projectileEvents.size(); eventIndex++) {
            EntityProjectileEvent event = projectileEvents.get(eventIndex);

            // Accepted LINK_DAMAGE responses are applied by the
            // projectile consumer before PlayerState becomes
            // invincible. Keep this pass for shield reflection,
            // sword-poke, and hookshot responses only.
            if (event.kind() == EntityProjectileEvent.Kind.LINK_DAMAGE) {
                boolean hasLinkResponse =
                        event.linkIgnoreCollisionCountdown() != 0 || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
                if (hasLinkResponse) {
                    resetSword();
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
                this.playerLink.apply(LinkCommandAdapter.from(event));
                continue;
            }
            this.playerLink.apply(LinkCommandAdapter.from(event));
            resetSword();
        }
    }

    public void applyFinalEffects() {
        var results = this.rooms.entityFrameResults();
        var roosterLinkStateRequests = results.link().consumeRoosterLinkStateRequests();
        for (int requestIndex = 0; requestIndex < roosterLinkStateRequests.size(); requestIndex++) {
            RoosterLinkStateRequest request = roosterLinkStateRequests.get(requestIndex);
            this.playerLink.apply(LinkCommandAdapter.from(request));
        }
        var hinoxLinkEffectRequests = results.link().consumeHinoxLinkEffectRequests();
        for (int requestIndex = 0; requestIndex < hinoxLinkEffectRequests.size(); requestIndex++) {
            HinoxLinkEffectRequest request = hinoxLinkEffectRequests.get(requestIndex);

            applyHinoxEffect(this.playerLink, this.playerState, request);
        }
        this.lift.synchronizeLinkLiftedPresentation();
    }

    static void applyHinoxEffect(Link link, PlayerState player, HinoxLinkEffectRequest request) {
        link.apply(LinkCommandAdapter.from(request));
        if (request.damage() != 0) player.vitals().applyRomEnemyDamage(request.damage());
    }

    private void resetSword() {
        Sword sword = this.equipmentController.activeSword();
        if (sword != null) sword.resetSpinAttack();
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private EquipmentController equipmentController;
        private FrameClock frameCounter;
        private ItemRegistry itemRegistry;
        private LiftInteraction lift;
        private Link link;
        private PlayerState playerState;
        private RoomSession roomSession;
        private RoomTransitionCoordinator roomTransitionCoordinator;
        private ScrollController scrollController;
        private TransitionController transitionController;

        public Inputs equipmentController(EquipmentController value) {
            this.equipmentController = value;
            return this;
        }

        public Inputs frameCounter(FrameClock value) {
            this.frameCounter = value;
            return this;
        }

        public Inputs itemRegistry(ItemRegistry value) {
            this.itemRegistry = value;
            return this;
        }

        public Inputs lift(LiftInteraction value) {
            this.lift = value;
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

        public Inputs roomTransitionCoordinator(RoomTransitionCoordinator value) {
            this.roomTransitionCoordinator = value;
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
    }
}
