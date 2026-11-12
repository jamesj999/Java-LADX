package linksawakening.entity;

import linksawakening.state.PlayerState;

/** ROM-faithful damage collaborator for Link. */
final class LinkDamage {
    private final Link link;
    int collisionIgnoreFramesRemaining;
    int recoilSpeedX;
    int recoilSpeedY;
    boolean normalRecoilRoomBoundaries = true;
    boolean collisionDamageImmune;

    LinkDamage(Link link) {
        this.link = link;
    }

    void applySpikeGroundPhysics() {
        if (link.collision == null
                || link.debugNoClip()
                || link.linkAirborne.zSubPixels != 0
                || link.linkStateMachine.motionState == 2) return;
        int x = link.pixelX() + 8;
        int y = link.pixelY() + 12;
        int object = link.collision.objectIdAtPoint(x, y);
        boolean indoor = link.collision.usesIndoorPhysicsTable();
        if (!indoor && object == 0x61) {
            link.linkAirborne.enterPitGroundState();
            return;
        }
        if ((indoor && object == 0x4C && ((link.romEntityY() - 1) & 15) >= 12)
                || link.collision.objectPhysicsFlagAtPoint(x, y) == 0xE0) hurtBySpikes();
    }

    void hurtBySpikes() {
        if (link.playerState == null || link.playerState.invincibilityCounter() != 0) return;
        if (link.itemRegistry != null
                && link.itemRegistry.lookup(PlayerState.INVENTORY_SWORD)
                        instanceof linksawakening.equipment.Sword sword) sword.resetSpinAttack();
        link.linkMotion.lastRomSpeedX = (-link.linkMotion.lastRomSpeedX) & 0xFF;
        link.linkMotion.lastRomSpeedY = (-link.linkMotion.lastRomSpeedY) & 0xFF;
        link.linkAirborne.airborne = true;
        if (!link.linkSideView.sideScrolling) {
            link.linkAirborne.zVelocity = 0x10;
            link.linkAirborne.zSubPixels += 2 << Link.SUB_PIXEL_SHIFT;
        }
        link.linkMotion.forcedSpeedPending = false;
        link.linkMotion.forcedSpeedXOnly = false;
        setCollisionIgnoreFrames(0x10);
        link.playerState.setInvincibilityCounter(0x30);
        link.playerState.addSubtractHealthBuffer(4);
        link.soundSink.play(linksawakening.gameplay.GameplaySoundEvent.LINK_HURT);
    }

    void tickRecoil() {
        collisionIgnoreFramesRemaining--;
        int speedX = link.linkMotion.forcedSpeedPending ? link.linkMotion.forcedSpeedX : recoilSpeedX;
        int speedY = link.linkMotion.forcedSpeedPending && !link.linkMotion.forcedSpeedXOnly
                ? link.linkMotion.forcedSpeedY
                : recoilSpeedY;
        link.linkMotion.forcedSpeedPending = false;
        link.linkMotion.forcedSpeedXOnly = false;
        int beforeRecoilX = link.linkMotion.subX;
        int beforeRecoilY = link.linkMotion.subY;
        if (speedX != 0) link.linkMotion.tryMoveAxis(speedX, true);
        if (speedY != 0) link.linkMotion.tryMoveAxis(speedY, false);
        // CheckPositionForMapTransition restores the previous position
        // at a normal room edge, then $02:6D94 rejects active recoil.
        if (normalRecoilRoomBoundaries
                && collisionIgnoreFramesRemaining > 0
                && (link.linkMotion.romEntityX() < 0x04
                        || link.linkMotion.romEntityX() >= 0x9C
                        || link.linkMotion.romEntityY() < 0x0C
                        || link.linkMotion.romEntityY() >= 0x84)) {
            link.linkMotion.subX = beforeRecoilX;
            link.linkMotion.subY = beforeRecoilY;
            collisionIgnoreFramesRemaining = 0;
            speedX = 0;
            speedY = 0;
        }
        if ((link.linkMotion.romCollisionType
                        & (LinkRomConstants.COLLISION_TYPE_LEFT | LinkRomConstants.COLLISION_TYPE_RIGHT))
                != 0) speedX = 0;
        if ((link.linkMotion.romCollisionType
                        & (LinkRomConstants.COLLISION_TYPE_UP | LinkRomConstants.COLLISION_TYPE_DOWN))
                != 0) speedY = 0;
        recoilSpeedX = speedX;
        recoilSpeedY = speedY;
        link.linkMotion.lastRomSpeedX = speedX & 0xFF;
        link.linkMotion.lastRomSpeedY = speedY & 0xFF;
        link.movingThisFrame = speedX != 0 || speedY != 0;
    }

    void setCollisionIgnoreFrames(int frames) {
        collisionIgnoreFramesRemaining = Math.max(0, frames);
        recoilSpeedX = link.linkMotion.forcedSpeedPending
                ? link.linkMotion.forcedSpeedX
                : (byte) link.linkMotion.lastRomSpeedX;
        recoilSpeedY = link.linkMotion.forcedSpeedPending && !link.linkMotion.forcedSpeedXOnly
                ? link.linkMotion.forcedSpeedY
                : (byte) link.linkMotion.lastRomSpeedY;
    }

    boolean hasRecoil() {
        return collisionIgnoreFramesRemaining > 0;
    }

    void tickInvincibility() {
        if (link.playerState != null) link.playerState.tickInvincibility();
    }

    void applyPitDamage() {
        link.playerState.damage(PlayerState.HP_PER_HEART / 2);
        link.playerState.setInvincibilityCounter(0x40);
    }
}
