package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;
import static linksawakening.entity.LinkStateMachine.*;

import linksawakening.state.Direction;
import linksawakening.state.GroundStatus;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** Link room motion state and operations used by ROM {@code ApplyLinkMotionState}. */
abstract class LinkRoomMotion extends LinkFrameMotion {
    protected LinkRoomMotion(Dependencies dependencies) {
        super(dependencies);
    }

    protected void tickRevolvingDoor() {
        linkMotion.get().subX = (0x50 - 8) << Link.SUB_PIXEL_SHIFT;
        linkMotion.get().subY = 0;
        linkAnimationState.get().movingThisFrame = false;
        if (revolvingDoorFrame == 0 || revolvingDoorFrame == 0x28) revolvingDoorPaletteVariant = 0;
        if (revolvingDoorFrame == 0x18) revolvingDoorPaletteVariant = 1;
        if (revolvingDoorFrame < 0x38) {
            linkAnimator.get().romAnimationStateOverride =
                    linkServices.get().linkMotionTables.revolvingDoorAnimation(revolvingDoorFrame);
        }
        if (++revolvingDoorFrame >= 0x38) {
            linkMotion.get().subY = (-5 - 16) << Link.SUB_PIXEL_SHIFT;
            linkAnimationState.get().direction = Direction.UP;
            linkAnimator.get().romAnimationStateOverride = 0x04;
        }
        if (revolvingDoorFrame == 0x48) {
            motionState = LinkMotionState.DEFAULT;
            revolvingDoorPaletteVariant = -1;
            revolvingDoorTransitionPending = true;
        }
    }

    // $02:4409-4427 selects the second row only for Piece of Power.
    protected int groundSpeedIndex(int mask) {
        return (mask & 0x0F)
                | (linkServices.get().playerState != null
                                && linkServices
                                                .get()
                                                .playerState
                                                .equipmentLevels()
                                                .activePowerUp()
                                        == 1
                        ? 0x10
                        : 0);
    }

    protected void updateDebugNoClip() {
        int mask = linkControls.get().accessBuildJoypadMask();
        int newDirection = LinkRomConstants.JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1) {
            linkAnimationState.get().direction = Direction.fromJava(newDirection);
        }
        int speedX = (byte) linkServices.get().linkMotionTables.linkSpeedX(groundSpeedIndex(mask));
        int speedY = (byte) linkServices.get().linkMotionTables.linkSpeedY(groundSpeedIndex(mask));
        linkMotion.get().lastRomSpeedX = speedX & 0xFF;
        linkMotion.get().lastRomSpeedY = speedY & 0xFF;
        linkAnimationState.get().movingThisFrame = speedX != 0 || speedY != 0;
        linkMotion.get().subX += speedX;
        linkMotion.get().subY += speedY;
        if (linkAnimationState.get().movingThisFrame) {
            linkAnimationState.get().walkTickCounter++;
            if (linkAnimationState.get().walkTickCounter >= LinkRomConstants.WALK_FRAME_TICKS) {
                linkAnimationState.get().walkTickCounter = 0;
                linkAnimationState.get().walkFrame ^= 1;
            }
        } else {
            linkAnimationState.get().walkTickCounter = 0;
            linkAnimationState.get().walkFrame = 0;
        }
    }

    /** Mirrors CheckItemsToUse's two Pegasus-Boots slot branches and UsePegasusBoots. */
    protected void updatePegasusBootsUse() {
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
                || romInteractiveMotionBlocked
                || linkAirborne.get().airborne
                || linkAirborne.get().accessZPixels() != 0
                || linkAirborne.get().groundStatus == GroundStatus.PIT
                || linkEntityEffects.get().accessIsCarryingLiftedObject()
                || motionState == LinkMotionState.SWIMMING) {
            return;
        }

        int heldBootsSlots = (aHeld ? 1 : 0) + (bHeld ? 1 : 0);
        for (int slot = 0; slot < heldBootsSlots; slot++) {
            if (pegasusBootsChargeMeter >= LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                break;
            }
            pegasusBootsChargeMeter++;
            if (pegasusBootsChargeMeter == LinkRomConstants.PEGASUS_BOOTS_MAX_CHARGE) {
                linkServices.get().playerState.equipmentLevels().setRunningWithPegasusBoots(true);
                linkMotion.get().forcedSpeedPending = false;
                linkMotion.get().forcedSpeedXOnly = false;
                break;
            }
        }
    }

    protected int pegasusRunningSpeedX() {
        return switch (linkAnimationState.get().direction) {
            case LEFT -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case RIGHT -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    protected int pegasusRunningSpeedY() {
        return switch (linkAnimationState.get().direction) {
            case UP -> -LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            case DOWN -> LinkRomConstants.PEGASUS_BOOTS_RUNNING_SPEED;
            default -> 0;
        };
    }

    /** Mirrors bank2.asm:74AD's dash collision response and func_020_49BA. */
    protected void handlePegasusBootsCollision() {
        if (linkServices.get().playerState == null
                || !linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots()
                || (linkMotion.get().romCollisionType & 0x0F) == 0) {
            return;
        }

        int reflectedSpeedX = (-Link.signedByte(linkMotion.get().lastRomSpeedX)) >> 2;
        int reflectedSpeedY = (-Link.signedByte(linkMotion.get().lastRomSpeedY)) >> 2;
        accessResetPegasusBoots();
        linkMotion.get().forcedSpeedX = reflectedSpeedX;
        linkMotion.get().forcedSpeedY = reflectedSpeedY;
        linkMotion.get().forcedSpeedPending = true;
        linkMotion.get().lastRomSpeedX = reflectedSpeedX & 0xFF;
        linkMotion.get().lastRomSpeedY = reflectedSpeedY & 0xFF;
        linkAirborne.get().airborne = true;
        linkAirborne.get().zSubPixels = 0;
        linkAirborne.get().zVelocity = LinkRomConstants.PEGASUS_BOOTS_COLLISION_VELOCITY_Z;

        int romDirection = linkAnimationState.get().direction.romIndex();
        int[] xOffsets = {0x10, 0xF0, 0x08, 0x08};
        int[] yOffsets = {0x0C, 0x0C, 0xF0, 0x10};
        pegasusBootsCollisionPosX = (linkMotion.get().romEntityX() + xOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionPosY = (linkMotion.get().romEntityY() + yOffsets[romDirection]) & 0xFF;
        pegasusBootsCollisionCountdown = LinkRomConstants.PEGASUS_BOOTS_COLLISION_COUNTDOWN;
        pendingPegasusScreenShakeCountdown = LinkRomConstants.PEGASUS_BOOTS_COLLISION_SHAKE;
        pendingPegasusScreenShakePhase = (romDirection & 0x02) << 1;
    }

    /** Enables the development movement mode; normal gameplay leaves this off. */
    protected void setDebugNoClip(boolean enabled) {
        debugNoClip = enabled;
        if (enabled) {
            linkAirborne.get().groundStatus = GroundStatus.NORMAL;
            linkAirborne.get().fallingIntoPit = false;
            linkAirborne.get().airborne = false;
            linkAirborne.get().zSubPixels = 0;
            linkAirborne.get().zVelocity = 0;
        }
    }
}
