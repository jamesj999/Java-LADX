package linksawakening.entity;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.state.PlayerState;

/** ROM-faithful airborne collaborator for Link. */
final class LinkAirborne {
    private final Link link;
    boolean airborne;
    /** wD475, set by the top-view pit warp and cleared at ground level. */
    boolean spinningPitWarpArrival;
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
    /** Mirrors LINK_MOTION_UNSTUCKING after an overworld $10 ledge collision. */
    boolean overworldLedgeFallActive;
    /** Mirrors wC10A while a directional ledge jump owns Link's XY speed. */
    boolean directionalLedgeJumpMotionLocked;
    int directionalLedgeJumpSpeedX;
    int directionalLedgeJumpSpeedY;
    /** Mirrors wC191, the collision-probe countdown for directional ledges. */
    int directionalLedgeCollisionCounter;

    LinkAirborne(Link link) { this.link = link; }

    void tickJump() {
        tickJump(true);
    }

    private void tickJump(boolean animate) {
        if (!airborne || link.linkEntityEffects.roosterCarryActive) {
            return;
        }

        if (animate && !spinningPitWarpArrival && !link.linkEntityEffects.marinBedJumpMotionLocked && !swordSuppressesJumpAnimation()) tickJumpAnimation();
        zSubPixels += zVelocity;
        zVelocity -= 2;
        // $02:4523–4561 changes only axes with held input. With no
        // input, momentum is retained; reversing takes 32 speed steps.
        if (!spinningPitWarpArrival && !link.linkSideView.sideScrolling && !link.linkEntityEffects.marinBedJumpMotionLocked
            && !directionalLedgeJumpMotionLocked
            && (link.playerState == null || !link.playerState.runningWithPegasusBoots())) {
            int mask = link.buildJoypadMask();
            if ((mask & (LinkRomConstants.JOY_RIGHT | LinkRomConstants.JOY_LEFT)) != 0) {
                int target = link.romTables.airSpeedX(mask & 3);
                link.linkMotion.lastRomSpeedX = steerAirSpeed(link.linkMotion.lastRomSpeedX, target);
            }
            if ((mask & (LinkRomConstants.JOY_UP | LinkRomConstants.JOY_DOWN)) != 0) {
                int target = link.romTables.airSpeedY((mask >>> 2) & 3);
                link.linkMotion.lastRomSpeedY = steerAirSpeed(link.linkMotion.lastRomSpeedY, target);
            }
        }
        // $02:4563 tests hLinkPositionZ, not its fractional byte wC149.
        if ((zSubPixels >> Link.SUB_PIXEL_SHIFT) > 0 || zVelocity > 0) {
            return;
        }

        airborne = false;
        spinningPitWarpArrival = false;
        link.linkEntityEffects.marinBedJumpMotionLocked = false;
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
        link.linkAnimator.jumpAnimationCounter = 0;
        link.linkAnimator.jumpAnimationFrame = 0;
        if (linkOverPit()) {
            enterPitGroundState();
        } else {
            groundStatus = Link.GROUND_STATUS_NORMAL;
            fallingIntoPit = false;
        }
    }

    void tickOverworldLedgeFall() {
        // $02:49AA applies gravity without moving XY, then $49B6 writes
        // wIsLinkInTheAir=1 even on landing, before UpdateLinkAnimation.
        // The next default-motion frame clears it again. hLinkPhysicsModifier
        // remains 1; only wLinkMotionState changes back to default.
        tickJump(false);
        link.linkStateMachine.physicsModifier = 1;
        airborne = true;
        if (!swordSuppressesJumpAnimation()) tickJumpAnimation();
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
        if (link.linkAnimator.jumpAnimationCounter < 3 * LinkRomConstants.JUMP_FRAME_TICKS) {
            link.linkAnimator.jumpAnimationFrame = link.linkAnimator.jumpAnimationCounter / LinkRomConstants.JUMP_FRAME_TICKS;
            link.linkAnimator.jumpAnimationCounter++;
        } else if (!overworldLedgeFallActive) {
            link.linkAnimator.jumpAnimationFrame = 3;
        }
    }

    boolean linkOverPit() {
        return link.collision != null && link.collision.linkOnPit(link.linkMotion.pixelX(), link.linkMotion.pixelY());
    }

    void refreshGroundStatus() {
        if (link.collision != null) {
            link.collision.refreshLinkGroundInteraction(link.linkMotion.pixelX(), link.linkMotion.pixelY());
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
            OverworldCollision.PitCell pit = link.collision == null ? null : link.collision.normalPitUnderLink(link.linkMotion.pixelX(), link.linkMotion.pixelY());
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

        int x = link.linkMotion.pixelX();
        int y = link.linkMotion.pixelY();
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

        if (Math.abs(link.linkMotion.pixelX() - pitSlipTargetTopLeftX) <= 1
            && Math.abs(link.linkMotion.pixelY() - pitSlipTargetTopLeftY) <= 1) {
            startPitFall();
        }
    }

    void startPitFall() {
        // $02:7644 routes $50 and $51 through the same slip/fall path.
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
        if (fallingFrameCounter < LinkRomConstants.FALL_DURATION_FRAMES) {
            return;
        }

        fallingIntoPit = false;
        groundStatus = Link.GROUND_STATUS_NORMAL;
        hasPitSlipTarget = false;
        pitSlippingCounter = 0;
        fallingFrameCounter = 0;
        if (pitSlipPhysicsFlag == 0x51) {
            link.pitWarpPending = true;
            return;
        }
        if (link.playerState != null) {
            link.linkDamage.applyPitDamage();
        }
        if (link.linkMotion.hasRoomEntryPosition) {
            link.linkMotion.subX = link.linkMotion.roomEntrySubX;
            link.linkMotion.subY = link.linkMotion.roomEntrySubY;
        } else if (link.linkMotion.hasLastSafePosition) {
            link.linkMotion.subX = link.linkMotion.lastSafeSubX;
            link.linkMotion.subY = link.linkMotion.lastSafeSubY;
        }
    }

    void updateLastSafePositionIfPossible() {
        if (airborne || fallingIntoPit || groundStatus == Link.GROUND_STATUS_PIT || linkOverPit()) {
            return;
        }
        link.linkMotion.lastSafeSubX = link.linkMotion.subX;
        link.linkMotion.lastSafeSubY = link.linkMotion.subY;
        link.linkMotion.hasLastSafePosition = true;
    }
}
