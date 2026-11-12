package linksawakening.entity;

import static linksawakening.entity.LinkAirborne.*;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.GroundStatus;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** ROM-faithful airborne collaborator for Link. */

/** LinkJumpMotion behavior on the shared LinkAirborne state. */
abstract class LinkJumpMotion extends LinkAirborneState {
    protected LinkJumpMotion(Dependencies dependencies) {
        super(dependencies);
    }

    protected void tickJump() {
        tickJump(true);
    }

    protected void tickJump(boolean animate) {
        if (!airborne || linkEntityEffects.get().roosterCarryActive) {
            return;
        }

        if (animate
                && !spinningPitWarpArrival
                && !linkEntityEffects.get().marinBedJumpMotionLocked
                && !swordSuppressesJumpAnimation()) tickJumpAnimation();
        zSubPixels += zVelocity;
        zVelocity -= 2;
        // $02:4523–4561 changes only axes with held input. With no
        // input, momentum is retained; reversing takes 32 speed steps.
        if (!spinningPitWarpArrival
                && !linkSideView.get().sideScrolling
                && !linkEntityEffects.get().marinBedJumpMotionLocked
                && !directionalLedgeJumpMotionLocked
                && (linkServices.get().playerState == null
                        || !linkServices.get().playerState.runningWithPegasusBoots())) {
            int mask = linkControls.get().accessBuildJoypadMask();
            if ((mask & (LinkRomConstants.JOY_RIGHT | LinkRomConstants.JOY_LEFT)) != 0) {
                int target = linkServices.get().romTables.airSpeedX(mask & 3);
                linkMotion.get().lastRomSpeedX = steerAirSpeed(linkMotion.get().lastRomSpeedX, target);
            }
            if ((mask & (LinkRomConstants.JOY_UP | LinkRomConstants.JOY_DOWN)) != 0) {
                int target = linkServices.get().romTables.airSpeedY((mask >>> 2) & 3);
                linkMotion.get().lastRomSpeedY = steerAirSpeed(linkMotion.get().lastRomSpeedY, target);
            }
        }
        // $02:4563 tests hLinkPositionZ, not its fractional byte wC149.
        if ((zSubPixels >> Link.SUB_PIXEL_SHIFT) > 0 || zVelocity > 0) {
            return;
        }

        airborne = false;
        spinningPitWarpArrival = false;
        linkEntityEffects.get().marinBedJumpMotionLocked = false;
        directionalLedgeJumpMotionLocked = false;
        directionalLedgeJumpSpeedX = 0;
        directionalLedgeJumpSpeedY = 0;
        overworldLedgeFallActive = false;
        if (linkStateMachine.get().motionState == LinkMotionState.UNSTUCKING) {
            linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
            linkStateMachine.get().physicsModifier = 0;
        }
        zSubPixels = 0;
        zVelocity = 0;
        linkAnimator.get().jumpAnimationCounter = 0;
        linkAnimator.get().jumpAnimationFrame = 0;
        refreshGroundStatus();
        playLandingSound();
    }

    protected void playLandingSound() {
        // ApplyLinkGroundMotion $02:4582-$45AA: room-bottom landings skip
        // ground effects; wells, deep water, lava and pits do not request
        // the footstep noise. Deep-water entry owns its splash request.
        if (linkMotion.get().accessRomEntityY() >= 0x88) return;
        int x = linkMotion.get().accessPixelX() + 8;
        int y = linkMotion.get().accessPixelY() + 12;
        if (linkServices.get().collision != null) {
            if (linkServices.get().collision.objectIdAtPoint(x, y) == 0x61) return;
            int physics = linkServices.get().collision.objectPhysicsFlagAtPoint(x, y);
            if (physics == 5) {
                linkServices.get().soundSink.play(GameplaySoundEvent.WATER_SPLASH);
                return;
            }
            if (physics == 7 || physics == 0x0B || physics == 0x50 || physics == 0x51) return;
        }
        linkServices.get().soundSink.play(GameplaySoundEvent.LINK_FOOTSTEP);
    }

    protected void tickOverworldLedgeFall() {
        // $02:49AA applies gravity without moving XY, then $49B6 writes
        // wIsLinkInTheAir=1 even on landing, before UpdateLinkAnimation.
        // The next default-motion frame clears it again. hLinkPhysicsModifier
        // remains 1; only wLinkMotionState changes back to default.
        tickJump(false);
        linkStateMachine.get().physicsModifier = 1;
        airborne = true;
        if (!swordSuppressesJumpAnimation()) tickJumpAnimation();
    }

    protected boolean swordSuppressesJumpAnimation() {
        if (linkServices.get().itemRegistry == null) return false;
        EquipmentItem item = linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue());
        return item instanceof linksawakening.equipment.Sword sword && sword.suppressesJumpAnimation();
    }

    protected void tickJumpAnimation() {
        // UpdateLinkAnimation ($02:47BC) writes the current pose BEFORE
        // incrementing wC153/wC152. Each pose is displayed for eight full
        // updates; phase 3 stops overriding the normal walking animation.
        // The unstucking handler does not refresh walking animation, so it
        // retains the final flip pose until landing ($02:49AA-$49C7).
        if (linkAnimator.get().jumpAnimationCounter < 3 * LinkRomConstants.JUMP_FRAME_TICKS) {
            linkAnimator.get().jumpAnimationFrame =
                    linkAnimator.get().jumpAnimationCounter / LinkRomConstants.JUMP_FRAME_TICKS;
            linkAnimator.get().jumpAnimationCounter++;
        } else if (!overworldLedgeFallActive) {
            linkAnimator.get().jumpAnimationFrame = 3;
        }
    }

    protected boolean linkOverPit() {
        return linkServices.get().collision != null
                && linkServices
                        .get()
                        .collision
                        .linkOnPit(linkMotion.get().pixelX(), linkMotion.get().pixelY());
    }

    protected void refreshGroundStatus() {
        if (linkServices.get().collision != null) {
            linkServices
                    .get()
                    .collision
                    .refreshLinkGroundInteraction(
                            linkMotion.get().pixelX(), linkMotion.get().pixelY());
        }
        // ApplyLinkGroundPhysics ($02:75CA-75D4) bypasses the solid
        // physics byte for OBJECT_WELL and enters the fall without slipping.
        if (linkServices.get().collision != null
                && !linkServices.get().collision.usesIndoorPhysicsTable()
                && linkServices
                                .get()
                                .collision
                                .objectUnderLinkFeet(
                                        linkMotion.get().accessPixelX(),
                                        linkMotion.get().accessPixelY())
                        == 0x61) {
            if (!fallingIntoPit) {
                pitSlipPhysicsFlag = PhysicsFlags.SOLID;
                startPitFall(false);
            }
        } else if (linkOverPit()) {
            enterPitGroundState();
        } else if (!fallingIntoPit) {
            groundStatus = GroundStatus.NORMAL;
            hasPitSlipTarget = false;
        }
    }

    protected void enterPitGroundState() {
        if (groundStatus != GroundStatus.PIT) {
            pitSlippingCounter = 0;
            OverworldCollision.PitCell pit = linkServices.get().collision == null
                    ? null
                    : linkServices
                            .get()
                            .collision
                            .normalPitUnderLink(
                                    linkMotion.get().pixelX(), linkMotion.get().pixelY());
            if (pit != null) {
                pitSlipTargetTopLeftX = pit.targetTopLeftX();
                pitSlipTargetTopLeftY = pit.targetTopLeftY();
                pitSlipPhysicsFlag = pit.physicsFlag();
                hasPitSlipTarget = true;
            }
        }
        groundStatus = GroundStatus.PIT;
    }

    protected void slipTowardPitCenter() {
        if (!hasPitSlipTarget) {
            groundStatus = GroundStatus.NORMAL;
            return;
        }

        pitSlippingCounter++;
        if ((pitSlippingCounter & 0x03) != 0) {
            return;
        }

        int x = linkMotion.get().pixelX();
        int y = linkMotion.get().pixelY();
        if (x < pitSlipTargetTopLeftX) {
            linkMotion.get().subX += 1 << Link.SUB_PIXEL_SHIFT;
        } else if (x > pitSlipTargetTopLeftX) {
            linkMotion.get().subX -= 1 << Link.SUB_PIXEL_SHIFT;
        }
        if (y < pitSlipTargetTopLeftY) {
            linkMotion.get().subY += 1 << Link.SUB_PIXEL_SHIFT;
        } else if (y > pitSlipTargetTopLeftY) {
            linkMotion.get().subY -= 1 << Link.SUB_PIXEL_SHIFT;
        }

        if (Math.abs(linkMotion.get().pixelX() - pitSlipTargetTopLeftX) <= 1
                && Math.abs(linkMotion.get().pixelY() - pitSlipTargetTopLeftY) <= 1) {
            startPitFall();
        }
    }
}
