package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.GroundStatus;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** Owns charge, rebound and shake for ROM UsePegasusBoots / bank2:74AD. */
final class LinkDash {

    private int pegasusBootsChargeMeter;

    int pegasusBootsChargeMeter() {
        return pegasusBootsChargeMeter;
    }

    private int pegasusBootsCollisionCountdown;

    int pegasusBootsCollisionCountdown() {
        return pegasusBootsCollisionCountdown;
    }

    int pegasusBootsCollisionCountdown(int value) {
        return pegasusBootsCollisionCountdown = value;
    }

    private int pegasusBootsCollisionPosX;

    private int pegasusBootsCollisionPosY;

    private int pendingPegasusScreenShakeCountdown;

    private int pendingPegasusScreenShakePhase;

    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkMotionStatus status;

    LinkDash(
            LinkComponentRef<LinkAirborne> linkAirborne,
            LinkComponentRef<LinkAnimationState> linkAnimationState,
            LinkComponentRef<LinkEntityEffects> linkEntityEffects,
            LinkComponentRef<LinkMotion> linkMotion,
            LinkComponentRef<LinkServices> linkServices,
            LinkMotionStatus status) {
        this.linkAirborne = linkAirborne;
        this.linkAnimationState = linkAnimationState;
        this.linkEntityEffects = linkEntityEffects;
        this.linkMotion = linkMotion;
        this.linkServices = linkServices;
        this.status = status;
    }

    /** Mirrors CheckItemsToUse's two Pegasus-Boots slot branches and UsePegasusBoots. */
    void updatePegasusBootsUse() {
        if (linkServices.get().playerState == null
                || linkServices.get().inputState == null
                || linkServices.get().inputConfig == null) {
            return;
        }
        boolean aHeld = linkServices.get().playerState.inventory().itemA() == InventoryItem.PEGASUS_BOOTS.romValue()
                && linkServices
                        .get()
                        .inputState
                        .isDown(linkServices.get().inputConfig.aKey());
        boolean bHeld = linkServices.get().playerState.inventory().itemB() == InventoryItem.PEGASUS_BOOTS.romValue()
                && linkServices
                        .get()
                        .inputState
                        .isDown(linkServices.get().inputConfig.bKey());
        if (!aHeld && !bHeld) {
            pegasusBootsChargeMeter = 0;
            return;
        }
        if (linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots()
                || status.romInteractiveMotionBlocked()
                || linkAirborne.get().elevation().airborne()
                || linkAirborne.get().accessZPixels() != 0
                || linkAirborne.get().pit().groundStatus() == GroundStatus.PIT
                || linkEntityEffects.get().accessIsCarryingLiftedObject()
                || status.motionState() == LinkMotionState.SWIMMING) {
            return;
        }

        int heldBootsSlots = (aHeld ? 1 : 0) + (bHeld ? 1 : 0);
        for (int slot = 0; slot < heldBootsSlots; slot++) {
            if (pegasusBootsChargeMeter >= LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                break;
            }
            pegasusBootsChargeMeter = pegasusBootsChargeMeter + 1;
            if (pegasusBootsChargeMeter == LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                linkServices.get().playerState.equipmentLevels().setRunningWithPegasusBoots(true);
                linkMotion.get().position().forcedSpeedPending(false);
                linkMotion.get().position().forcedSpeedXOnly(false);
                break;
            }
        }
    }

    int pegasusRunningSpeedX() {
        return switch (linkAnimationState.get().direction) {
            case LEFT -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case RIGHT -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    int pegasusRunningSpeedY() {
        return switch (linkAnimationState.get().direction) {
            case UP -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case DOWN -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    /** Mirrors bank2.asm:74AD's dash collision response and func_020_49BA. */
    void handlePegasusBootsCollision() {
        if (linkServices.get().playerState == null
                || !linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots()
                || (linkMotion.get().collisionState().romCollisionType() & 0x0F) == 0) {
            return;
        }

        int reflectedSpeedX = (-Link.signedByte(linkMotion.get().position().lastRomSpeedX())) >> 2;
        int reflectedSpeedY = (-Link.signedByte(linkMotion.get().position().lastRomSpeedY())) >> 2;
        resetPegasusBoots();
        linkMotion.get().position().forcedSpeedX(reflectedSpeedX);
        linkMotion.get().position().forcedSpeedY(reflectedSpeedY);
        linkMotion.get().position().forcedSpeedPending(true);
        linkMotion.get().position().lastRomSpeedX(reflectedSpeedX & 0xFF);
        linkMotion.get().position().lastRomSpeedY(reflectedSpeedY & 0xFF);
        linkAirborne.get().elevation().airborne(true);
        linkAirborne.get().elevation().zSubPixels(0);
        linkAirborne.get().elevation().zVelocity(LinkRomConstants.PEGASUS_BOOTS_COLLISION_VELOCITY_Z);

        int romDirection = linkAnimationState.get().direction.romIndex();
        int[] xOffsets = {0x10, 0xF0, 0x08, 0x08};
        int[] yOffsets = {0x0C, 0x0C, 0xF0, 0x10};
        pegasusBootsCollisionPosX = (linkMotion.get().position().romEntityX() + xOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionPosY = (linkMotion.get().position().romEntityY() + yOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionCountdown = LinkRomConstants.PEGASUS_BOOTS_COLLISION_COUNTDOWN;
        pendingPegasusScreenShakeCountdown = LinkRomConstants.PEGASUS_BOOTS_COLLISION_SHAKE;
        pendingPegasusScreenShakePhase = (romDirection & 0x02) << 1;
    }

    /** Mirrors ResetPegasusBoots for handlers that interrupt a this. */
    void resetPegasusBoots() {
        pegasusBootsChargeMeter = 0;
        if (linkServices.get().playerState != null) {
            linkServices.get().playerState.equipmentLevels().setRunningWithPegasusBoots(false);
        }
    }

    /** Returns and clears the one-shot shake request written this frame. */
    ScreenShakeRequest consumePegasusScreenShakeRequest() {
        int countdown = pendingPegasusScreenShakeCountdown;
        int phase = pendingPegasusScreenShakePhase;
        pendingPegasusScreenShakeCountdown = 0;
        pendingPegasusScreenShakePhase = 0;
        return countdown == 0 ? null : new ScreenShakeRequest(countdown, phase);
    }

    int accessPegasusBootsChargeMeter() {
        return pegasusBootsChargeMeter & 0xFF;
    }

    int accessPegasusBootsCollisionCountdown() {
        return pegasusBootsCollisionCountdown & 0xFF;
    }

    int accessPegasusBootsCollisionPosX() {
        return pegasusBootsCollisionPosX & 0xFF;
    }

    int accessPegasusBootsCollisionPosY() {
        return pegasusBootsCollisionPosY & 0xFF;
    }
}
