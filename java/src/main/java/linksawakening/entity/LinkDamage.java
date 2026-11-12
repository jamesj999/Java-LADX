package linksawakening.entity;

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

    LinkDamage(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkStateMachine = dependencies.linkStateMachine;
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

    int collisionIgnoreFramesRemaining;
    int recoilSpeedX;
    int recoilSpeedY;
    boolean normalRecoilRoomBoundaries = true;
    boolean collisionDamageImmune;

    void applySpikeGroundPhysics() {
        if (linkServices.get().collision == null
                || linkStateMachine.get().accessDebugNoClip()
                || linkAirborne.get().zSubPixels != 0
                || linkStateMachine.get().motionState == LinkMotionState.UNSTUCKING) return;
        int x = linkMotion.get().accessPixelX() + 8;
        int y = linkMotion.get().accessPixelY() + 12;
        int object = linkServices.get().collision.objectIdAtPoint(x, y);
        boolean indoor = linkServices.get().collision.usesIndoorPhysicsTable();
        if (!indoor && object == 0x61) {
            linkAirborne.get().enterPitGroundState();
            return;
        }
        if ((indoor && object == 0x4C && ((linkMotion.get().accessRomEntityY() - 1) & 15) >= 12)
                || linkServices.get().collision.objectPhysicsFlagAtPoint(x, y) == 0xE0) hurtBySpikes();
    }

    void hurtBySpikes() {
        if (linkServices.get().playerState == null
                || linkServices.get().playerState.vitals().invincibilityCounter() != 0) return;
        if (linkServices.get().itemRegistry != null
                && linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue())
                        instanceof linksawakening.equipment.Sword sword) sword.resetSpinAttack();
        linkMotion.get().lastRomSpeedX = (-linkMotion.get().lastRomSpeedX) & 0xFF;
        linkMotion.get().lastRomSpeedY = (-linkMotion.get().lastRomSpeedY) & 0xFF;
        linkAirborne.get().airborne = true;
        if (!linkSideView.get().sideScrolling) {
            linkAirborne.get().zVelocity = 0x10;
            linkAirborne.get().zSubPixels += 2 << Link.SUB_PIXEL_SHIFT;
        }
        linkMotion.get().forcedSpeedPending = false;
        linkMotion.get().forcedSpeedXOnly = false;
        setCollisionIgnoreFrames(0x10);
        linkServices.get().playerState.vitals().setInvincibilityCounter(0x30);
        linkServices.get().playerState.vitals().addSubtractHealthBuffer(4);
        linkServices.get().soundSink.play(linksawakening.gameplay.GameplaySoundEvent.LINK_HURT);
    }

    void tickRecoil() {
        collisionIgnoreFramesRemaining--;
        int speedX = linkMotion.get().forcedSpeedPending ? linkMotion.get().forcedSpeedX : recoilSpeedX;
        int speedY = linkMotion.get().forcedSpeedPending && !linkMotion.get().forcedSpeedXOnly
                ? linkMotion.get().forcedSpeedY
                : recoilSpeedY;
        linkMotion.get().forcedSpeedPending = false;
        linkMotion.get().forcedSpeedXOnly = false;
        int beforeRecoilX = linkMotion.get().subX;
        int beforeRecoilY = linkMotion.get().subY;
        if (speedX != 0) linkMotion.get().tryMoveAxis(speedX, true);
        if (speedY != 0) linkMotion.get().tryMoveAxis(speedY, false);
        // CheckPositionForMapTransition restores the previous position
        // at a normal room edge, then $02:6D94 rejects active recoil.
        if (normalRecoilRoomBoundaries
                && collisionIgnoreFramesRemaining > 0
                && (linkMotion.get().romEntityX() < 0x04
                        || linkMotion.get().romEntityX() >= 0x9C
                        || linkMotion.get().romEntityY() < 0x0C
                        || linkMotion.get().romEntityY() >= 0x84)) {
            linkMotion.get().subX = beforeRecoilX;
            linkMotion.get().subY = beforeRecoilY;
            collisionIgnoreFramesRemaining = 0;
            speedX = 0;
            speedY = 0;
        }
        if ((linkMotion.get().romCollisionType
                        & (LinkRomConstants.COLLISION_TYPE_LEFT | LinkRomConstants.COLLISION_TYPE_RIGHT))
                != 0) speedX = 0;
        if ((linkMotion.get().romCollisionType
                        & (LinkRomConstants.COLLISION_TYPE_UP | LinkRomConstants.COLLISION_TYPE_DOWN))
                != 0) speedY = 0;
        recoilSpeedX = speedX;
        recoilSpeedY = speedY;
        linkMotion.get().lastRomSpeedX = speedX & 0xFF;
        linkMotion.get().lastRomSpeedY = speedY & 0xFF;
        linkAnimationState.get().movingThisFrame = speedX != 0 || speedY != 0;
    }

    void setCollisionIgnoreFrames(int frames) {
        collisionIgnoreFramesRemaining = Math.max(0, frames);
        recoilSpeedX = linkMotion.get().forcedSpeedPending
                ? linkMotion.get().forcedSpeedX
                : (byte) linkMotion.get().lastRomSpeedX;
        recoilSpeedY = linkMotion.get().forcedSpeedPending && !linkMotion.get().forcedSpeedXOnly
                ? linkMotion.get().forcedSpeedY
                : (byte) linkMotion.get().lastRomSpeedY;
    }

    boolean hasRecoil() {
        return collisionIgnoreFramesRemaining > 0;
    }

    void tickInvincibility() {
        if (linkServices.get().playerState != null)
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
}
