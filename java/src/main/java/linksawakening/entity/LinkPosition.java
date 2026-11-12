package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkMotion.*;

import linksawakening.physics.PhysicsFlags;
import linksawakening.state.Direction;

/** Link position state and operations used by ROM {@code ApplyLinkMotionState}. */
abstract class LinkPosition {

    protected final LinkComponentRef<LinkAirborne> linkAirborne;
    protected final LinkComponentRef<LinkAnimationState> linkAnimationState;
    protected final LinkComponentRef<LinkAnimator> linkAnimator;
    protected final LinkComponentRef<LinkControls> linkControls;
    protected final LinkComponentRef<LinkDamage> linkDamage;
    protected final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    protected final LinkComponentRef<LinkEquipment> linkEquipment;
    protected final LinkComponentRef<LinkGroundMotion> linkGroundMotion;
    protected final LinkComponentRef<LinkItemState> linkItemState;
    protected final LinkComponentRef<LinkServices> linkServices;
    protected final LinkComponentRef<LinkSideView> linkSideView;
    protected final LinkComponentRef<LinkStateMachine> linkStateMachine;

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkEquipment> linkEquipment;
        LinkComponentRef<LinkGroundMotion> linkGroundMotion;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
    }

    int subX;
    int subY;
    int lastRomSpeedX;
    int lastRomSpeedY;
    boolean forcedSpeedPending;
    boolean forcedSpeedXOnly;
    int forcedSpeedX;
    int forcedSpeedY;
    int romCollisionType;
    boolean doorWarpPending;
    final int[] collisionProbePixelX = new int[4];
    final int[] collisionProbePixelY = new int[4];

    int lastSafeSubX;
    int lastSafeSubY;
    boolean hasLastSafePosition;
    int romFinalSubX;
    int romFinalSubY;
    boolean hasRomFinalPosition;
    int roomEntrySubX;
    int roomEntrySubY;
    boolean hasRoomEntryPosition;

    LinkPosition(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkEquipment = dependencies.linkEquipment;
        linkGroundMotion = dependencies.linkGroundMotion;
        linkItemState = dependencies.linkItemState;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkStateMachine = dependencies.linkStateMachine;
    }

    static boolean isDirectionalLedgePhysics(int physicsFlag) {
        return physicsFlag >= PhysicsFlags.CAT_LEDGE && physicsFlag <= PhysicsFlags.CAT_LEDGE + 3;
    }

    static int joypadBitForDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> LinkRomConstants.JOY_DOWN;
            case UP -> LinkRomConstants.JOY_UP;
            case LEFT -> LinkRomConstants.JOY_LEFT;
            case RIGHT -> LinkRomConstants.JOY_RIGHT;
            default -> 0;
        };
    }

    protected abstract void tryMoveAxis(int signedSpeed, boolean isXAxis);

    protected abstract boolean leadingEdgeBlocked(int spriteX, int spriteY, int dir);

    protected abstract int leadingEdgeCollisionMask(int spriteX, int spriteY, int dir);

    protected abstract boolean applyDirectionalLedgeCollision(int physicsFlag, int moveDirection);

    protected abstract boolean directionalLedgeHalfIsPassable(int physicsFlag, int pointX);

    protected abstract boolean applyOverworldLedgeCollision(int pointY);

    protected abstract void beginOverworldLedgeUnstuckingMotion();

    protected abstract boolean ledgeUnstuckingPositionBlocked();

    protected abstract void setPixelPosition(int pixelX, int pixelY);

    protected abstract void setRoomEntryPixelPosition(int pixelX, int pixelY);

    protected abstract void setRoomScrollEntryPixelPosition(int pixelX, int pixelY);

    protected abstract void markRoomEntryPosition();

    protected abstract int roomEntryPixelX();

    protected abstract int roomEntryPixelY();

    protected abstract int roomEntryRomPositionX();

    protected abstract int roomEntryRomPositionY();

    protected abstract void setRoomEntryRomPosition(int romPositionX, int romPositionY);

    protected abstract void applyRomSpeed(int speedX, int speedY);

    protected abstract void applyRomSpeedX(int speedX);

    protected abstract void applyRomFinalPosition(int speedX, int speedY);

    protected abstract void captureRomFinalPosition();

    protected abstract void restoreRomFinalPosition();

    protected abstract void restoreRomFinalPositionX();

    protected abstract int pixelX();

    protected abstract int pixelY();

    protected abstract int romEntityX();

    protected abstract int romEntityY();

    protected abstract int romFinalPositionX();

    protected abstract void clearRomPositionIncrement();

    protected abstract boolean accessConsumeDoorWarp();

    protected abstract int accessRomCollisionType();

    protected abstract int accessRomCollisionProbePixelX();

    protected abstract int accessRomCollisionProbePixelY();

    protected abstract int accessRomSpeedX();

    protected abstract int accessRomSpeedY();

    protected abstract int accessRomSwordCollisionY();

    protected abstract void accessSetPixelPosition(int pixelX, int pixelY);

    protected abstract void accessSetRoomScrollEntryPixelPosition(int pixelX, int pixelY);

    protected abstract void accessSetRoomEntryPixelPosition(int pixelX, int pixelY);

    protected abstract void accessMarkRoomEntryPosition();

    protected abstract int accessRoomEntryPixelX();

    protected abstract int accessRoomEntryPixelY();

    protected abstract int accessRoomEntryRomPositionX();

    protected abstract int accessRoomEntryRomPositionY();

    protected abstract void accessSetRoomEntryRomPosition(int romPositionX, int romPositionY);

    protected abstract void accessApplyRomSpeed(int speedX, int speedY);

    protected abstract void accessApplyRomSpeedX(int speedX);

    protected abstract void accessApplyRomFinalPosition(int speedX, int speedY);

    protected abstract void accessCaptureRomFinalPosition();

    protected abstract void accessRestoreRomFinalPosition();

    protected abstract void accessRestoreRomFinalPositionX();

    protected abstract void accessClearRomPositionIncrement();

    protected abstract int accessPixelX();

    protected abstract int accessPixelY();

    protected abstract int accessRomEntityX();

    protected abstract int accessRomFinalPositionX();

    protected abstract int accessRomEntityY();
}
