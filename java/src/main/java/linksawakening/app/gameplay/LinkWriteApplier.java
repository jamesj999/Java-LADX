package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.Sword;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.room.*;

final class LinkWriteApplier {
    static void applyPositionWrites(GameplaySimulation c) {
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

    static void applyMotionWrites(GameplaySimulation c) {
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

    static void applyAttackWrites(GameplaySimulation c) {
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

    static void applySwordWrites(GameplaySimulation c) {
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

    static void applyProjectileResponses(GameplaySimulation c, java.util.List<EntityProjectileEvent> projectileEvents) {
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

    static void applyFinalEffects(GameplaySimulation c) {
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

    private static void resetSword(GameplaySimulation c) {
        Sword sword = c.equipmentController().activeSword();
        if (sword != null) sword.resetSpinAttack();
    }
}
