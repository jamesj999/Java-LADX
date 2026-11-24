package linksawakening.entity;

import java.util.Objects;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;
import linksawakening.state.PlayerState;

/** ROM-faithful damage collaborator for Link. */
final class LinkDamage {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSideView> linkSideView;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;

    int collisionIgnoreFramesRemaining;
    int recoilSpeedX;
    int recoilSpeedY;
    boolean normalRecoilRoomBoundaries = true;
    boolean collisionDamageImmune;

    LinkDamage(Dependencies dependencies) {
        linkAirborne = Objects.requireNonNull(dependencies.linkAirborne, "linkAirborne");
        linkAnimationState = Objects.requireNonNull(dependencies.linkAnimationState, "linkAnimationState");
        linkAnimator = Objects.requireNonNull(dependencies.linkAnimator, "linkAnimator");
        linkMotion = Objects.requireNonNull(dependencies.linkMotion, "linkMotion");
        linkServices = Objects.requireNonNull(dependencies.linkServices, "linkServices");
        linkSideView = Objects.requireNonNull(dependencies.linkSideView, "linkSideView");
        linkStateMachine = Objects.requireNonNull(dependencies.linkStateMachine, "linkStateMachine");
    }

    void applySpikeGroundPhysics() {
        if (linkStateMachine.get().status().accessDebugNoClip()
                || linkAirborne.get().elevation().zSubPixels() != 0
                || linkStateMachine.get().status().motionState() == LinkMotionState.UNSTUCKING) return;
        int x = linkMotion.get().position().pixelX() + 8;
        int y = linkMotion.get().position().pixelY() + 12;
        int object = linkServices.get().collision.objectIdAtPoint(x, y);
        boolean indoor = linkServices.get().collision.usesIndoorPhysicsTable();
        if (!indoor && object == 0x61) {
            linkAirborne.get().pit().enterPitGroundState();
            return;
        }
        if ((indoor && object == 0x4C && ((linkMotion.get().position().romEntityY() - 1) & 15) >= 12)
                || linkServices.get().collision.objectPhysicsFlagAtPoint(x, y) == 0xE0) hurtBySpikes();
    }

    void hurtBySpikes() {
        if (linkServices.get().playerState.vitals().invincibilityCounter() != 0) return;
        if (linkServices.get().itemRegistry.item(InventoryItem.SWORD.romValue())
                instanceof linksawakening.equipment.Sword sword) sword.resetSpinAttack();
        linkMotion.get().position().lastRomSpeedX(-linkMotion.get().position().lastRomSpeedX() & 0xFF);
        linkMotion.get().position().lastRomSpeedY(-linkMotion.get().position().lastRomSpeedY() & 0xFF);
        linkAirborne.get().elevation().airborne(true);
        if (!linkSideView.get().sideScrolling) {
            linkAirborne.get().elevation().zVelocity(0x10);
            linkAirborne
                    .get()
                    .elevation()
                    .zSubPixels(linkAirborne.get().elevation().zSubPixels() + (2 << Link.SUB_PIXEL_SHIFT));
        }
        linkMotion.get().position().forcedSpeedPending(false);
        linkMotion.get().position().forcedSpeedXOnly(false);
        setCollisionIgnoreFrames(0x10);
        linkServices.get().playerState.vitals().setInvincibilityCounter(0x30);
        linkServices.get().playerState.vitals().addSubtractHealthBuffer(4);
        linkServices.get().soundSink.play(linksawakening.gameplay.GameplaySoundEvent.LINK_HURT);
    }

    void tickRecoil(int directionMask) {
        collisionIgnoreFramesRemaining--;
        int speedX = linkMotion.get().position().forcedSpeedPending()
                ? linkMotion.get().position().forcedSpeedX()
                : recoilSpeedX;
        int speedY = linkMotion.get().position().forcedSpeedPending()
                        && !linkMotion.get().position().forcedSpeedXOnly()
                ? linkMotion.get().position().forcedSpeedY()
                : recoilSpeedY;
        linkMotion.get().position().forcedSpeedPending(false);
        linkMotion.get().position().forcedSpeedXOnly(false);
        int beforeRecoilX = linkMotion.get().position().subX();
        int beforeRecoilY = linkMotion.get().position().subY();
        if (speedX != 0) linkMotion.get().tryMoveAxis(speedX, true);
        if (speedY != 0) linkMotion.get().tryMoveAxis(speedY, false);
        if (normalRecoilRoomBoundaries && blocksRecoilAtEdge(directionMask, beforeRecoilY)) {
            // CopyLinkFinalPositionToPosition restores the two integer bytes. The movement fractions survive.
            int fractionMask = (1 << Link.SUB_PIXEL_SHIFT) - 1;
            var position = linkMotion.get().position();
            position.subX((beforeRecoilX & ~fractionMask) | (position.subX() & fractionMask));
            position.subY((beforeRecoilY & ~fractionMask) | (position.subY() & fractionMask));
            collisionIgnoreFramesRemaining = 0;
            speedX = 0;
            speedY = 0;
        }
        if ((linkMotion.get().collisionState().romCollisionType()
                        & (LinkRomConstants.COLLISION_TYPE_LEFT | LinkRomConstants.COLLISION_TYPE_RIGHT))
                != 0) speedX = 0;
        if ((linkMotion.get().collisionState().romCollisionType()
                        & (LinkRomConstants.COLLISION_TYPE_UP | LinkRomConstants.COLLISION_TYPE_DOWN))
                != 0) speedY = 0;
        recoilSpeedX = speedX;
        recoilSpeedY = speedY;
        linkMotion.get().position().lastRomSpeedX(speedX & 0xFF);
        linkMotion.get().position().lastRomSpeedY(speedY & 0xFF);
        linkAnimationState.get().movingThisFrame = speedX != 0 || speedY != 0;
    }

    private boolean blocksRecoilAtEdge(int directionMask, int beforeRecoilY) {
        var position = linkMotion.get().position();
        int x = position.romEntityX() & 0xFF;
        int y = position.romEntityY() & 0xFF;
        Direction edge;
        if (y < 0x0C) edge = Direction.UP;
        else if (y >= 0x84) edge = Direction.DOWN;
        else if (x < 0x04) edge = Direction.LEFT;
        else if (x >= 0x9C) edge = Direction.RIGHT;
        else return false;

        // $02:6D94 rejects active recoil. On its last step, test the held direction
        // just as for normal travel, including the south-edge and motion exceptions.
        if (collisionIgnoreFramesRemaining != 0) return true;
        int restoredY = ((beforeRecoilY >> Link.SUB_PIXEL_SHIFT) + 16) & 0xFF;
        if (restoredY >= 0x88
                || linkSideView.get().sideScrolling
                || linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots()
                || linkStateMachine.get().status().slowWalkingSpeed() != 0) return false;
        int horizontal = directionMask & 3;
        int vertical = (directionMask >>> 2) & 3;
        return (horizontal == 0 || horizontal - 1 != edge.romIndex())
                && (vertical == 0 || vertical + 1 != edge.romIndex());
    }

    void setCollisionIgnoreFrames(int frames) {
        collisionIgnoreFramesRemaining = Math.max(0, frames);
        recoilSpeedX = linkMotion.get().position().forcedSpeedPending()
                ? linkMotion.get().position().forcedSpeedX()
                : (byte) linkMotion.get().position().lastRomSpeedX();
        recoilSpeedY = linkMotion.get().position().forcedSpeedPending()
                        && !linkMotion.get().position().forcedSpeedXOnly()
                ? linkMotion.get().position().forcedSpeedY()
                : (byte) linkMotion.get().position().lastRomSpeedY();
    }

    boolean hasRecoil() {
        return collisionIgnoreFramesRemaining > 0;
    }

    void clearRecoilSpeed() {
        recoilSpeedX = 0;
        recoilSpeedY = 0;
    }

    void tickInvincibility() {
        linkServices.get().playerState.vitals().tickInvincibility();
    }

    void applyPitDamage() {
        linkServices.get().playerState.vitals().damage(PlayerState.HP_PER_HEART / 2);
        linkServices.get().playerState.vitals().setInvincibilityCounter(0x40);
    }

    void accessSetCollisionIgnoreFrames(int frames) {
        this.setCollisionIgnoreFrames(frames);
    }

    void accessSetNormalRecoilRoomBoundaries(boolean normalBoundaries) {
        this.normalRecoilRoomBoundaries = normalBoundaries;
    }

    boolean accessIsCollisionDamageImmune() {
        return this.collisionDamageImmune || linkAnimator.get().accessDialogGotItemActive();
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }
}
