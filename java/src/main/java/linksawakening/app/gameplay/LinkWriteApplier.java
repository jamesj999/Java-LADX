package linksawakening.app.gameplay;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.entity.LinkCommand;
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

final class LinkWriteApplier implements LinkWriteSink.Positions, LinkWriteSink.Attacks {
    private GameplaySimulation current;

    void applyPositionWrites(GameplaySimulation c) {
        c.link().clearStandingOnSideViewEntity();
        current = c;
        try {
            c.roomSession().entityFrameResults().link().drainPositionWrites(this);
        } finally {
            current = null;
        }
    }

    public void platform(SideViewPlatformLinkRequest request) {
        var c = current;
        if (request.standing()) {
            c.link().apply(LinkCommandAdapter.from(request));
            c.roomTransitionCoordinator().handleWarpAndIndoorBoundaries(c.link());
            c.roomTransitionCoordinator().handleOverworldBoundary(c.link());
            if (c.scrollController().isActive() || c.transitionController().isInputBlocked())
                c.link().clearSideViewPlatformState();
        }
    }

    public void position(LinkFinalPositionRequest request) {
        current.link().apply(LinkCommandAdapter.from(request));
    }

    public void pot(SideViewPotLinkRequest request) {
        current.link().apply(LinkCommandAdapter.from(request));
    }

    public void clear(LinkAttackClearRequest request) {
        current.link().apply(LinkCommandAdapter.from(request));
    }

    public void step(LinkAttackStepRequest request) {
        current.link().apply(LinkCommandAdapter.from(request));
    }

    static void applyMotionWrites(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        var linkMotionBlockRequests = results.link().consumeLinkMotionBlockRequests();
        for (int requestIndex = 0; requestIndex < linkMotionBlockRequests.size(); requestIndex++) {
            LinkMotionBlockRequest request = linkMotionBlockRequests.get(requestIndex);
            c.link().apply(LinkCommandAdapter.from(request));
        }
        var warpLinkStateRequests = results.link().consumeWarpLinkStateRequests();
        for (int requestIndex = 0; requestIndex < warpLinkStateRequests.size(); requestIndex++) {
            WarpLinkStateRequest request = warpLinkStateRequests.get(requestIndex);

            c.link().apply(LinkCommandAdapter.from(request));
            resetSword(c);
        }
        if (results.link().consumeWorldLinkMotionBlockRequest()) {
            c.link().apply(LinkCommand.Action.BLOCK_MOTION);
        }
        var linkFallenPoseRequests = results.link().consumeLinkFallenPoseRequests();
        for (int requestIndex = 0; requestIndex < linkFallenPoseRequests.size(); requestIndex++) {
            LinkFallenPoseRequest request = linkFallenPoseRequests.get(requestIndex);
            c.link().apply(LinkCommandAdapter.from(request));
        }
        var linkFacingRequests = results.link().consumeLinkFacingRequests();
        for (int requestIndex = 0; requestIndex < linkFacingRequests.size(); requestIndex++) {
            LinkFacingRequest request = linkFacingRequests.get(requestIndex);
            c.link().apply(LinkCommandAdapter.from(request));
        }
    }

    void applyAttackWrites(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        current = c;
        try {
            results.link().drainAttackWrites(this);
        } finally {
            current = null;
        }
        var linkHeldItemPoseRequests = results.link().consumeLinkHeldItemPoseRequests();
        for (int requestIndex = 0; requestIndex < linkHeldItemPoseRequests.size(); requestIndex++) {
            LinkHeldItemPoseRequest request = linkHeldItemPoseRequests.get(requestIndex);

            c.link().apply(LinkCommandAdapter.from(request));
            resetSword(c);
        }
    }

    static void applySwordWrites(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        var linkSwordSpinPoseRequests = results.link().consumeLinkSwordSpinPoseRequests();
        for (int requestIndex = 0; requestIndex < linkSwordSpinPoseRequests.size(); requestIndex++) {
            LinkSwordSpinPoseRequest request = linkSwordSpinPoseRequests.get(requestIndex);

            int acquisitionBaseDirection = c.link().direction();
            c.link().apply(LinkCommandAdapter.from(request));
            if (c.itemRegistry().lookup(InventoryItem.SWORD.romValue()) instanceof Sword acquisitionSword) {
                if (request.countdown() == 0x20) {
                    acquisitionSword.startSwordAcquisitionSpin(acquisitionBaseDirection);
                } else if (acquisitionSword.spinAttackActive()) {
                    acquisitionSword.tick(false, c.frameCounter());
                }
            }
        }
        var linkSwordFinalPoseRequests = results.link().consumeLinkSwordFinalPoseRequests();
        for (int requestIndex = 0; requestIndex < linkSwordFinalPoseRequests.size(); requestIndex++) {
            LinkSwordFinalPoseRequest request = linkSwordFinalPoseRequests.get(requestIndex);

            c.link().apply(LinkCommandAdapter.from(request));
            if (c.itemRegistry().lookup(InventoryItem.SWORD.romValue()) instanceof Sword sword) {
                sword.resetSpinAttack();
            }
        }
    }

    static void applyProjectileResponses(GameplaySimulation c, List<EntityProjectileEvent> projectileEvents) {
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

    static void applyFinalEffects(GameplaySimulation c) {
        var results = c.roomSession().entityFrameResults();
        var roosterLinkStateRequests = results.link().consumeRoosterLinkStateRequests();
        for (int requestIndex = 0; requestIndex < roosterLinkStateRequests.size(); requestIndex++) {
            RoosterLinkStateRequest request = roosterLinkStateRequests.get(requestIndex);
            c.link().apply(LinkCommandAdapter.from(request));
        }
        var hinoxLinkEffectRequests = results.link().consumeHinoxLinkEffectRequests();
        for (int requestIndex = 0; requestIndex < hinoxLinkEffectRequests.size(); requestIndex++) {
            HinoxLinkEffectRequest request = hinoxLinkEffectRequests.get(requestIndex);

            applyHinoxEffect(c.link(), c.playerState(), request);
        }
        c.lift().synchronizeLinkLiftedPresentation();
    }

    static void applyHinoxEffect(Link link, PlayerState player, HinoxLinkEffectRequest request) {
        link.apply(LinkCommandAdapter.from(request));
        if (request.damage() != 0) player.applyRomEnemyDamage(request.damage());
    }

    private static void resetSword(GameplaySimulation c) {
        Sword sword = c.equipmentController().activeSword();
        if (sword != null) sword.resetSpinAttack();
    }
}
