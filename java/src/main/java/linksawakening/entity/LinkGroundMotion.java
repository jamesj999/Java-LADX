package linksawakening.entity;

import java.util.Objects;
import linksawakening.state.LinkMotionState;

/** Link ground motion state and operations used by ROM {@code ApplyLinkMotionState}. */
final class LinkGroundMotion {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSideView> linkSideView;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    int groundMotionCounter;
    int worldMotionFrame = -1;
    boolean grassGroundEffectActive;

    LinkGroundMotion(Dependencies dependencies) {
        linkAirborne = Objects.requireNonNull(dependencies.linkAirborne, "linkAirborne");
        linkDamage = Objects.requireNonNull(dependencies.linkDamage, "linkDamage");
        linkMotion = Objects.requireNonNull(dependencies.linkMotion, "linkMotion");
        linkServices = Objects.requireNonNull(dependencies.linkServices, "linkServices");
        linkSideView = Objects.requireNonNull(dependencies.linkSideView, "linkSideView");
        linkStateMachine = Objects.requireNonNull(dependencies.linkStateMachine, "linkStateMachine");
    }

    void accessUpdate() {
        linkStateMachine.get().update();
        linkDamage.get().applySpikeGroundPhysics();
        // ApplyLinkGroundPhysics uses the completed motion position. Dialogs
        // and scrolling reuse this result while ordinary Link updates pause.
        grassGroundEffectActive = !linkSideView.get().sideScrolling
                && !linkAirborne.get().elevation().airborne()
                && !linkAirborne.get().pit().fallingIntoPit()
                && linkStateMachine.get().status().motionState() != LinkMotionState.UNSTUCKING
                && linkMotion.get().position().romEntityY() < 0x88
                && linkServices
                                .get()
                                .collision
                                .objectPhysicsFlagAtPoint(
                                        linkMotion.get().position().pixelX() + 8,
                                        linkMotion.get().position().pixelY() + 12)
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
        if (!linkServices
                .get()
                .collision
                .linkOnSlowGround(
                        linkMotion.get().position().pixelX(),
                        linkMotion.get().position().pixelY())) {
            return true;
        }
        // GROUND_STATUS_SLOW ($03) uses the same frame-counter mask in the
        // original: $02:44A9 skips the final position update when the global
        // hFrameCounter & 3 == 0, including after dialogs and room scrolling.
        // Isolated Link callers without a session clock retain their local tick.
        int frame = worldMotionFrame >= 0 ? worldMotionFrame : groundMotionCounter;
        return (frame & 0x03) != 0;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }
}
