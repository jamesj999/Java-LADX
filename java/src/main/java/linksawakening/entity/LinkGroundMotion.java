package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.LinkMotionState;

/** Link ground motion state and operations used by ROM {@code ApplyLinkMotionState}. */
final class LinkGroundMotion {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSideView> linkSideView;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    LinkGroundMotion(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkDamage = dependencies.linkDamage;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    int groundMotionCounter;
    int worldMotionFrame = -1;
    boolean grassGroundEffectActive;

    void accessUpdate() {
        linkStateMachine.get().update();
        linkDamage.get().applySpikeGroundPhysics();
        // ApplyLinkGroundPhysics uses the completed motion position. Dialogs
        // and scrolling reuse this result while ordinary Link updates pause.
        grassGroundEffectActive = linkServices.get().collision != null
                && !linkSideView.get().sideScrolling
                && !linkAirborne.get().airborne
                && !linkAirborne.get().fallingIntoPit
                && linkStateMachine.get().status().motionState() != LinkMotionState.UNSTUCKING
                && linkMotion.get().accessRomEntityY() < 0x88
                && linkServices
                                .get()
                                .collision
                                .objectPhysicsFlagAtPoint(
                                        linkMotion.get().accessPixelX() + 8,
                                        linkMotion.get().accessPixelY() + 12)
                        == linksawakening.physics.PhysicsFlags.GRASS;
    }

    boolean accessGrassGroundEffectActive() {
        return grassGroundEffectActive;
    }

    void accessClearGroundEffects() {
        grassGroundEffectActive = false;
    }

    boolean accessShouldApplyGroundMotion() {
        groundMotionCounter++;
        if (linkServices.get().collision == null
                || !linkServices
                        .get()
                        .collision
                        .linkOnSlowGround(
                                linkMotion.get().pixelX(), linkMotion.get().pixelY())) {
            return true;
        }
        // GROUND_STATUS_SLOW ($03) uses the same frame-counter mask in the
        // original: $02:44A9 skips the final position update when the global
        // hFrameCounter & 3 == 0, including after dialogs and room scrolling.
        // Isolated Link callers without a session clock retain their local tick.
        int frame = worldMotionFrame >= 0 ? worldMotionFrame : groundMotionCounter;
        return (frame & 0x03) != 0;
    }
}
