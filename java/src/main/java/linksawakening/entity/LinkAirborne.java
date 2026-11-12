package linksawakening.entity;

import static linksawakening.entity.Link.SUB_PIXEL_SHIFT;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.state.GroundStatus;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** ROM-faithful airborne collaborator for Link. */
final class LinkAirborne {
    private final LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
    private final LinkComponentRef<LinkAnimationState> linkAnimationState;
    private final LinkComponentRef<LinkAnimator> linkAnimator;
    private final LinkComponentRef<LinkControls> linkControls;
    private final LinkComponentRef<LinkDamage> linkDamage;
    private final LinkComponentRef<LinkEntityEffects> linkEntityEffects;
    private final LinkComponentRef<LinkItemState> linkItemState;
    private final LinkComponentRef<LinkMotion> linkMotion;
    private final LinkComponentRef<LinkServices> linkServices;
    private final LinkComponentRef<LinkSideView> linkSideView;
    private final LinkComponentRef<LinkSignals> linkSignals;
    private final LinkComponentRef<LinkStateMachine> linkStateMachine;
    private final LinkComponentRef<LinkSwimming> linkSwimming;

    LinkAirborne(Dependencies dependencies) {
        dialogGotItemSequence = dependencies.dialogGotItemSequence;
        linkAnimationState = dependencies.linkAnimationState;
        linkAnimator = dependencies.linkAnimator;
        linkControls = dependencies.linkControls;
        linkDamage = dependencies.linkDamage;
        linkEntityEffects = dependencies.linkEntityEffects;
        linkItemState = dependencies.linkItemState;
        linkMotion = dependencies.linkMotion;
        linkServices = dependencies.linkServices;
        linkSideView = dependencies.linkSideView;
        linkSignals = dependencies.linkSignals;
        linkStateMachine = dependencies.linkStateMachine;
        linkSwimming = dependencies.linkSwimming;
    }

    static final class Dependencies {
        LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence;
        LinkComponentRef<LinkAnimationState> linkAnimationState;
        LinkComponentRef<LinkAnimator> linkAnimator;
        LinkComponentRef<LinkControls> linkControls;
        LinkComponentRef<LinkDamage> linkDamage;
        LinkComponentRef<LinkEntityEffects> linkEntityEffects;
        LinkComponentRef<LinkItemState> linkItemState;
        LinkComponentRef<LinkMotion> linkMotion;
        LinkComponentRef<LinkServices> linkServices;
        LinkComponentRef<LinkSideView> linkSideView;
        LinkComponentRef<LinkSignals> linkSignals;
        LinkComponentRef<LinkStateMachine> linkStateMachine;
        LinkComponentRef<LinkSwimming> linkSwimming;
    }

    boolean airborne;
    /** wD475, set by the top-view pit warp and cleared at ground level. */
    boolean spinningPitWarpArrival;

    int zSubPixels;
    int zVelocity;
    boolean fallingIntoPit;
    GroundStatus groundStatus = GroundStatus.NORMAL;
    int pitSlippingCounter;
    int fallingFrameCounter;
    int pitSlipTargetTopLeftX;
    int pitSlipTargetTopLeftY;
    int pitSlipPhysicsFlag = PhysicsFlags.NORMAL_PIT;
    boolean hasPitSlipTarget;
    /** Mirrors LINK_MOTION_UNSTUCKING after an overworld $10 ledge collision. */
    boolean overworldLedgeFallActive;
    /** Mirrors wC10A while a directional ledge jump owns Link's XY speed. */
    boolean directionalLedgeJumpMotionLocked;

    int directionalLedgeJumpSpeedX;
    int directionalLedgeJumpSpeedY;
    /** Mirrors wC191, the collision-probe countdown for directional ledges. */
    int directionalLedgeCollisionCounter;

    void tickJump() {
        tickJump(true);
    }

    private void tickJump(boolean animate) {
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

    private void playLandingSound() {
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

    void tickOverworldLedgeFall() {
        // $02:49AA applies gravity without moving XY, then $49B6 writes
        // wIsLinkInTheAir=1 even on landing, before UpdateLinkAnimation.
        // The next default-motion frame clears it again. hLinkPhysicsModifier
        // remains 1; only wLinkMotionState changes back to default.
        tickJump(false);
        linkStateMachine.get().physicsModifier = 1;
        airborne = true;
        if (!swordSuppressesJumpAnimation()) tickJumpAnimation();
    }

    static int steerAirSpeed(int speedByte, int target) {
        int speed = (byte) speedByte;
        return (speed + Integer.compare(target, speed)) & 0xFF;
    }

    boolean swordSuppressesJumpAnimation() {
        if (linkServices.get().itemRegistry == null) return false;
        EquipmentItem item = linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue());
        return item instanceof linksawakening.equipment.Sword sword && sword.suppressesJumpAnimation();
    }

    void tickJumpAnimation() {
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

    boolean linkOverPit() {
        return linkServices.get().collision != null
                && linkServices
                        .get()
                        .collision
                        .linkOnPit(linkMotion.get().pixelX(), linkMotion.get().pixelY());
    }

    void refreshGroundStatus() {
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

    void enterPitGroundState() {
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

    void slipTowardPitCenter() {
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

    void startPitFall() {
        startPitFall(true);
    }

    private void startPitFall(boolean slipping) {
        // $02:7644 routes $50 and $51 through the same slip/fall path.
        groundStatus = GroundStatus.PIT;
        fallingIntoPit = true;
        airborne = false;
        zSubPixels = 0;
        zVelocity = 0;
        fallingFrameCounter = 0;
        linkAnimationState.get().walkTickCounter = 0;
        linkAnimationState.get().walkFrame = 0;
        linkAnimationState.get().movingThisFrame = false;
        if (slipping) linkMotion.get().subY += 3 << Link.SUB_PIXEL_SHIFT;
        linkServices.get().soundSink.play(GameplaySoundEvent.PIT_FALL);
    }

    void tickPitFall() {
        fallingFrameCounter++;
        if (fallingFrameCounter < LinkRomConstants.FALL_DURATION_FRAMES) {
            return;
        }

        fallingIntoPit = false;
        groundStatus = GroundStatus.NORMAL;
        hasPitSlipTarget = false;
        pitSlippingCounter = 0;
        fallingFrameCounter = 0;
        // LinkMotionFallingDownHandler ($02:512B-5136): only $50
        // takes pit damage; the well's solid byte follows the warp path.
        if (pitSlipPhysicsFlag != PhysicsFlags.NORMAL_PIT) {
            linkSignals.get().pitWarpPending = true;
            return;
        }
        if (linkServices.get().playerState != null) {
            linkDamage.get().applyPitDamage();
        }
        if (linkMotion.get().hasRoomEntryPosition) {
            linkMotion.get().subX = linkMotion.get().roomEntrySubX;
            linkMotion.get().subY = linkMotion.get().roomEntrySubY;
        } else if (linkMotion.get().hasLastSafePosition) {
            linkMotion.get().subX = linkMotion.get().lastSafeSubX;
            linkMotion.get().subY = linkMotion.get().lastSafeSubY;
        }
    }

    void updateLastSafePositionIfPossible() {
        if (airborne || fallingIntoPit || groundStatus == GroundStatus.PIT || linkOverPit()) {
            return;
        }
        linkMotion.get().lastSafeSubX = linkMotion.get().subX;
        linkMotion.get().lastSafeSubY = linkMotion.get().subY;
        linkMotion.get().hasLastSafePosition = true;
    }

    void accessSetPitWarpArrival(int z) {
        linkStateMachine.get().motionState = LinkMotionState.DEFAULT;
        this.zSubPixels = z << SUB_PIXEL_SHIFT;
        this.zVelocity = 0;
        this.airborne = z != 0;
        this.spinningPitWarpArrival = z != 0;
    }

    int accessRomMotionState() {
        if (this.groundStatus == GroundStatus.PIT || this.fallingIntoPit) {
            return 0x06;
        }
        return linkStateMachine.get().motionState.romValue();
    }

    boolean accessIsAirborne() {
        return this.airborne;
    }

    int accessZVelocity() {
        return this.zVelocity & 0xFF;
    }

    boolean accessIsFallingIntoPit() {
        return this.fallingIntoPit;
    }

    void accessUseRocsFeather() {
        if (this.airborne
                || linkSideView.get().sideViewFeatherPending
                || this.groundStatus == GroundStatus.PIT
                || this.fallingIntoPit
                || linkStateMachine.get().accessIsSwimming()) {
            return;
        }
        if (linkSideView.get().sideScrolling) {
            // The ROM runs $02:68B7 before UseRocsFeather ($00:14E6).
            // Defer its extra XY step until normal side-view motion finishes.
            linkSideView.get().sideViewFeatherPending = true;
            return;
        }
        linkAnimationState.get().topDownFeatherStarted = true;
        this.airborne = true;
        this.zSubPixels = 0;
        this.zVelocity = 0x20;
        linkAnimator.get().jumpAnimationCounter = 0;
        linkAnimator.get().jumpAnimationFrame = 0;
        linkServices.get().soundSink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);
    }

    void accessBounceFromGoomba() {
        this.airborne = true;
        this.zVelocity = 0x10;
        this.fallingIntoPit = false;
        this.groundStatus = GroundStatus.NORMAL;
        linkSwimming.get().leaveSwimming();
    }

    int accessZPixels() {
        return Math.max(0, this.zSubPixels >> SUB_PIXEL_SHIFT);
    }

    int accessTickDialogGotItem(boolean dialogActive) {
        int openForItem = dialogGotItemSequence.get().advance(dialogActive);
        if (dialogGotItemSequence.get().presentedItem() == 0) return openForItem;
        // ApplyGotItem caches body Y before func_21E1 updates Z; the held OAM
        // entry uses the updated Z. Preserve the fractional position at ground.
        linkAnimator.get().bedJumpRenderZ = accessZPixels();
        if (linkServices.get().itemRegistry != null
                && linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue())
                        instanceof linksawakening.equipment.Sword sword) {
            sword.resetSpinAttack();
        }
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        int z = (this.zSubPixels + (byte) this.zVelocity) & 0xFFF;
        this.zVelocity = (byte) (this.zVelocity - 2);
        if (((z >>> SUB_PIXEL_SHIFT) & 0x80) != 0) {
            z = 0;
            this.zVelocity = 0;
        }
        this.zSubPixels = z;
        linkAnimator.get().romAnimationStateOverride = 0x6B;
        linkAnimationState.get().movingThisFrame = false;
        linkMotion.get().lastRomSpeedX = linkMotion.get().lastRomSpeedY = 0;
        return openForItem;
    }
}
