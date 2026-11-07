package linksawakening.entity;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.state.PlayerState;

/** ROM-faithful airborne collaborator for Link. */
final class LinkAirborne {
    private final Link link;
    boolean airborne;
    int zSubPixels;
    int zVelocity;
    boolean fallingIntoPit;
    int groundStatus = Link.GROUND_STATUS_NORMAL;
    int pitSlippingCounter;
    int fallingFrameCounter;
    int pitSlipTargetTopLeftX;
    int pitSlipTargetTopLeftY;
    int pitSlipPhysicsFlag;
    boolean hasPitSlipTarget;
    boolean overworldLedgeFallActive;
    boolean directionalLedgeJumpMotionLocked;
    int directionalLedgeJumpSpeedX;
    int directionalLedgeJumpSpeedY;
    int directionalLedgeCollisionCounter;

    LinkAirborne(Link link) { this.link = link; }

    void tickJump() {
        if (!airborne || link.roosterCarryActive) {
            return;
        }

        if (!link.marinBedJumpMotionLocked && !swordSuppressesJumpAnimation()) tickJumpAnimation();
        zSubPixels += zVelocity;
        zVelocity -= 2;
        // $02:4523–4561 changes only axes with held input. With no
        // input, momentum is retained; reversing takes 32 speed steps.
        if (!link.linkSideView.sideScrolling && !link.marinBedJumpMotionLocked
            && !directionalLedgeJumpMotionLocked && !overworldLedgeFallActive
            && (link.playerState == null || !link.playerState.runningWithPegasusBoots())) {
            int mask = link.buildJoypadMask();
            if ((mask & (Link.JOY_RIGHT | Link.JOY_LEFT)) != 0) {
                int target = link.romTables.airSpeedX(mask & 3);
                link.linkMotion.lastRomSpeedX = steerAirSpeed(link.linkMotion.lastRomSpeedX, target);
            }
            if ((mask & (Link.JOY_UP | Link.JOY_DOWN)) != 0) {
                int target = link.romTables.airSpeedY((mask >>> 2) & 3);
                link.linkMotion.lastRomSpeedY = steerAirSpeed(link.linkMotion.lastRomSpeedY, target);
            }
        }
        if (zSubPixels > 0 || zVelocity > 0) {
            return;
        }

        airborne = false;
        link.marinBedJumpMotionLocked = false;
        directionalLedgeJumpMotionLocked = false;
        directionalLedgeJumpSpeedX = 0;
        directionalLedgeJumpSpeedY = 0;
        overworldLedgeFallActive = false;
        if (link.linkStateMachine.motionState == 0x02) {
            link.linkStateMachine.motionState = Link.LINK_MOTION_DEFAULT;
            link.linkStateMachine.physicsModifier = 0;
        }
        zSubPixels = 0;
        zVelocity = 0;
        if (linkOverPit()) {
            enterPitGroundState();
        } else {
            groundStatus = Link.GROUND_STATUS_NORMAL;
            fallingIntoPit = false;
        }
    }

    static int steerAirSpeed(int speedByte, int target) {
        int speed = (byte) speedByte;
        return (speed + Integer.compare(target, speed)) & 0xFF;
    }

    boolean swordSuppressesJumpAnimation() {
        if (link.itemRegistry == null) return false;
        EquipmentItem item = link.itemRegistry.lookup(PlayerState.INVENTORY_SWORD);
        return item instanceof linksawakening.equipment.Sword sword
            && sword.suppressesJumpAnimation();
    }

    void tickJumpAnimation() {
        // UpdateLinkAnimation ($02:47BC) writes the current pose BEFORE
        // incrementing wC153/wC152. Each pose is displayed for eight full
        // updates; phase 3 stops overriding the normal walking animation.
        // The unstucking handler does not refresh walking animation, so it
        // retains the final flip pose until landing ($02:49AA-$49C7).
        if (link.jumpAnimationCounter < 3 * Link.JUMP_FRAME_TICKS) {
            link.jumpAnimationFrame = link.jumpAnimationCounter / Link.JUMP_FRAME_TICKS;
            link.jumpAnimationCounter++;
        } else if (!overworldLedgeFallActive) {
            link.jumpAnimationFrame = 3;
        }
    }

    boolean linkOverPit() {
        return link.collision != null && link.collision.linkOnNormalPit(link.pixelX(), link.pixelY());
    }

    void refreshGroundStatus() {
        if (link.collision != null) {
            link.collision.refreshLinkGroundInteraction(link.pixelX(), link.pixelY());
        }
        if (linkOverPit()) {
            enterPitGroundState();
        } else if (!fallingIntoPit) {
            groundStatus = Link.GROUND_STATUS_NORMAL;
            hasPitSlipTarget = false;
        }
    }

    void enterPitGroundState() {
        if (groundStatus != Link.GROUND_STATUS_PIT) {
            pitSlippingCounter = 0;
            OverworldCollision.PitCell pit = link.collision == null ? null : link.collision.normalPitUnderLink(link.pixelX(), link.pixelY());
            if (pit != null) {
                pitSlipTargetTopLeftX = pit.targetTopLeftX();
                pitSlipTargetTopLeftY = pit.targetTopLeftY();
                pitSlipPhysicsFlag = pit.physicsFlag();
                hasPitSlipTarget = true;
            }
        }
        groundStatus = Link.GROUND_STATUS_PIT;
    }

    void slipTowardPitCenter() {
        if (!hasPitSlipTarget) {
            groundStatus = Link.GROUND_STATUS_NORMAL;
            return;
        }

        pitSlippingCounter++;
        if ((pitSlippingCounter & 0x03) != 0) {
            return;
        }

        int x = link.pixelX();
        int y = link.pixelY();
        if (x < pitSlipTargetTopLeftX) {
            link.linkMotion.subX += 1 << Link.SUB_PIXEL_SHIFT;
        } else if (x > pitSlipTargetTopLeftX) {
            link.linkMotion.subX -= 1 << Link.SUB_PIXEL_SHIFT;
        }
        if (y < pitSlipTargetTopLeftY) {
            link.linkMotion.subY += 1 << Link.SUB_PIXEL_SHIFT;
        } else if (y > pitSlipTargetTopLeftY) {
            link.linkMotion.subY -= 1 << Link.SUB_PIXEL_SHIFT;
        }

        if (Math.abs(link.pixelX() - pitSlipTargetTopLeftX) <= 1
            && Math.abs(link.pixelY() - pitSlipTargetTopLeftY) <= 1) {
            startPitFall();
        }
    }

    void startPitFall() {
        // Current top-view fall/recovery support is only for ordinary $50 pits.
        groundStatus = Link.GROUND_STATUS_PIT;
        fallingIntoPit = true;
        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        fallingFrameCounter = 0;
        link.walkTickCounter = 0;
        link.walkFrame = 0;
        link.movingThisFrame = false;
        link.linkMotion.subY += 3 << Link.SUB_PIXEL_SHIFT;
        link.soundSink.play(GameplaySoundEvent.PIT_FALL);
    }

    void tickPitFall() {
        fallingFrameCounter++;
        if (fallingFrameCounter < Link.FALL_DURATION_FRAMES) {
            return;
        }

        fallingIntoPit = false;
        groundStatus = Link.GROUND_STATUS_NORMAL;
        hasPitSlipTarget = false;
        pitSlippingCounter = 0;
        fallingFrameCounter = 0;
        if (link.playerState != null) {
            link.linkDamage.applyPitDamage();
        }
        if (link.hasRoomEntryPosition) {
            link.linkMotion.subX = link.roomEntrySubX;
            link.linkMotion.subY = link.roomEntrySubY;
        } else if (link.hasLastSafePosition) {
            link.linkMotion.subX = link.lastSafeSubX;
            link.linkMotion.subY = link.lastSafeSubY;
        }
    }

    void updateLastSafePositionIfPossible() {
        if (airborne || fallingIntoPit || groundStatus == Link.GROUND_STATUS_PIT || linkOverPit()) {
            return;
        }
        link.lastSafeSubX = link.linkMotion.subX;
        link.lastSafeSubY = link.linkMotion.subY;
        link.hasLastSafePosition = true;
    }
}
