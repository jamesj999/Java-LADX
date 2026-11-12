package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.Direction;
import linksawakening.state.LinkMotionState;

/** Owns the timed pose and transition for ROM LinkMotionRevolvingDoor. */
final class LinkDoorTraversal {
    private int revolvingDoorFrame;

    int revolvingDoorFrame() {
        return revolvingDoorFrame;
    }

    int revolvingDoorFrame(int value) {
        return revolvingDoorFrame = value;
    }

    private boolean revolvingDoorTransitionPending;

    boolean revolvingDoorTransitionPending() {
        return revolvingDoorTransitionPending;
    }

    boolean revolvingDoorTransitionPending(boolean value) {
        return revolvingDoorTransitionPending = value;
    }

    private int revolvingDoorPaletteVariant = -1;

    int revolvingDoorPaletteVariant() {
        return revolvingDoorPaletteVariant;
    }

    int revolvingDoorPaletteVariant(int value) {
        return revolvingDoorPaletteVariant = value;
    }

    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkMotionStatus status;

    LinkDoorTraversal(
            LinkComponentRef<LinkAnimationState> linkAnimationState,
            LinkComponentRef<LinkAnimator> linkAnimator,
            LinkComponentRef<LinkMotion> linkMotion,
            LinkComponentRef<LinkServices> linkServices,
            LinkMotionStatus status) {
        this.linkAnimationState = linkAnimationState;
        this.linkAnimator = linkAnimator;
        this.linkMotion = linkMotion;
        this.linkServices = linkServices;
        this.status = status;
    }

    void tickRevolvingDoor() {
        linkMotion.get().position().subX((0x50 - 8) << Link.SUB_PIXEL_SHIFT);
        linkMotion.get().position().subY(0);
        linkAnimationState.get().movingThisFrame = false;
        if (this.revolvingDoorFrame() == 0 || this.revolvingDoorFrame() == 0x28) this.revolvingDoorPaletteVariant(0);
        if (this.revolvingDoorFrame() == 0x18) this.revolvingDoorPaletteVariant(1);
        if (this.revolvingDoorFrame() < 0x38) {
            linkAnimator
                    .get()
                    .romAnimationStateOverride(
                            linkServices.get().linkMotionTables.revolvingDoorAnimation(this.revolvingDoorFrame()));
        }
        if (this.revolvingDoorFrame(this.revolvingDoorFrame() + 1) >= 0x38) {
            linkMotion.get().position().subY((-5 - 16) << Link.SUB_PIXEL_SHIFT);
            linkAnimationState.get().direction = Direction.UP;
            linkAnimator.get().romAnimationStateOverride(0x04);
        }
        if (this.revolvingDoorFrame() == 0x48) {
            status.motionState(LinkMotionState.DEFAULT);
            this.revolvingDoorPaletteVariant(-1);
            this.revolvingDoorTransitionPending(true);
        }
    }

    boolean accessConsumeRevolvingDoorTransition() {
        boolean pending = this.revolvingDoorTransitionPending();
        this.revolvingDoorTransitionPending(false);
        return pending;
    }
}
