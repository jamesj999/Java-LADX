package linksawakening.entity;

import static linksawakening.entity.Link.SUB_PIXEL_SHIFT;

import linksawakening.equipment.EquipmentItem;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.GroundStatus;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;

/** Jump integration and landing for ROM ApplyLinkGroundMotion ($02:4523). */
final class LinkAirborne {
    private final LinkElevation elevation = new LinkElevation();
    private final LinkPitFall pit;

    LinkElevation elevation() {
        return elevation;
    }

    LinkPitFall pit() {
        return pit;
    }
    /** wD475, set by the top-view pit warp and cleared at ground level. */
    private boolean spinningPitWarpArrival;

    boolean spinningPitWarpArrival() {
        return spinningPitWarpArrival;
    }

    boolean spinningPitWarpArrival(boolean value) {
        return spinningPitWarpArrival = value;
    }
    /** Mirrors LINK_MOTION_UNSTUCKING after an overworld $10 ledge collision. */
    private boolean overworldLedgeFallActive;

    boolean overworldLedgeFallActive() {
        return overworldLedgeFallActive;
    }

    boolean overworldLedgeFallActive(boolean value) {
        return overworldLedgeFallActive = value;
    }
    /** Mirrors wC10A while a directional ledge jump owns Link's XY speed. */
    private boolean directionalLedgeJumpMotionLocked;

    boolean directionalLedgeJumpMotionLocked() {
        return directionalLedgeJumpMotionLocked;
    }

    boolean directionalLedgeJumpMotionLocked(boolean value) {
        return directionalLedgeJumpMotionLocked = value;
    }

    private int directionalLedgeJumpSpeedX;

    int directionalLedgeJumpSpeedX() {
        return directionalLedgeJumpSpeedX;
    }

    int directionalLedgeJumpSpeedX(int value) {
        return directionalLedgeJumpSpeedX = value;
    }

    private int directionalLedgeJumpSpeedY;

    int directionalLedgeJumpSpeedY() {
        return directionalLedgeJumpSpeedY;
    }

    int directionalLedgeJumpSpeedY(int value) {
        return directionalLedgeJumpSpeedY = value;
    }
    /** Mirrors wC191, the collision-probe countdown for directional ledges. */
    private int directionalLedgeCollisionCounter;

    int directionalLedgeCollisionCounter() {
        return directionalLedgeCollisionCounter;
    }

    int directionalLedgeCollisionCounter(int value) {
        return directionalLedgeCollisionCounter = value;
    }

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
        pit = new LinkPitFall(linkAnimationState, linkDamage, linkMotion, linkServices, linkSignals, elevation);
    }

    static int steerAirSpeed(int speedByte, int target) {
        int speed = (byte) speedByte;
        return (speed + Integer.compare(target, speed)) & 0xFF;
    }

    void tickJump() {
        tickJump(true);
    }

    void tickJump(boolean animate) {
        if (!elevation.airborne() || linkEntityEffects.get().roosterCarryActive) {
            return;
        }

        if (animate
                && !spinningPitWarpArrival
                && !linkEntityEffects.get().marinBedJumpMotionLocked
                && !swordSuppressesJumpAnimation()) tickJumpAnimation();
        elevation.zSubPixels(elevation.zSubPixels() + (elevation.zVelocity()));
        elevation.zVelocity(elevation.zVelocity() - (2));
        // $02:4523–4561 changes only axes with held input. With no
        // input, momentum is retained; reversing takes 32 speed steps.
        if (!spinningPitWarpArrival
                && !linkSideView.get().sideScrolling
                && !linkEntityEffects.get().marinBedJumpMotionLocked
                && !directionalLedgeJumpMotionLocked
                && (linkServices.get().playerState == null
                        || !linkServices.get().playerState.equipmentLevels().runningWithPegasusBoots())) {
            int mask = linkControls.get().accessBuildJoypadMask();
            if ((mask & (LinkRomConstants.JOY_RIGHT | LinkRomConstants.JOY_LEFT)) != 0) {
                int target = linkServices.get().linkMotionTables.airSpeedX(mask & 3);
                linkMotion.get().lastRomSpeedX = steerAirSpeed(linkMotion.get().lastRomSpeedX, target);
            }
            if ((mask & (LinkRomConstants.JOY_UP | LinkRomConstants.JOY_DOWN)) != 0) {
                int target = linkServices.get().linkMotionTables.airSpeedY((mask >>> 2) & 3);
                linkMotion.get().lastRomSpeedY = steerAirSpeed(linkMotion.get().lastRomSpeedY, target);
            }
        }
        // $02:4563 tests hLinkPositionZ, not its fractional byte wC149.
        if ((elevation.zSubPixels() >> Link.SUB_PIXEL_SHIFT) > 0 || elevation.zVelocity() > 0) {
            return;
        }

        elevation.airborne(false);
        spinningPitWarpArrival = false;
        linkEntityEffects.get().marinBedJumpMotionLocked = false;
        directionalLedgeJumpMotionLocked = false;
        directionalLedgeJumpSpeedX = 0;
        directionalLedgeJumpSpeedY = 0;
        overworldLedgeFallActive = false;
        if (linkStateMachine.get().status().motionState() == LinkMotionState.UNSTUCKING) {
            linkStateMachine.get().status().motionState(LinkMotionState.DEFAULT);
            linkStateMachine.get().roomPhysics().physicsModifier(0);
        }
        elevation.zSubPixels(0);
        elevation.zVelocity(0);
        linkAnimator.get().jumpAnimationCounter(0);
        linkAnimator.get().jumpAnimationFrame(0);
        pit.refreshGroundStatus();
        playLandingSound();
    }

    void playLandingSound() {
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
        linkStateMachine.get().roomPhysics().physicsModifier(1);
        elevation.airborne(true);
        if (!swordSuppressesJumpAnimation()) tickJumpAnimation();
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
        if (linkAnimator.get().jumpAnimationCounter() < 3 * LinkRomConstants.JUMP_FRAME_TICKS) {
            linkAnimator
                    .get()
                    .jumpAnimationFrame(linkAnimator.get().jumpAnimationCounter() / LinkRomConstants.JUMP_FRAME_TICKS);
            linkAnimator.get().jumpAnimationCounter(linkAnimator.get().jumpAnimationCounter() + 1);
        } else if (!overworldLedgeFallActive) {
            linkAnimator.get().jumpAnimationFrame(3);
        }
    }

    void accessSetPitWarpArrival(int z) {
        linkStateMachine.get().status().motionState(LinkMotionState.DEFAULT);
        elevation.zSubPixels(z << SUB_PIXEL_SHIFT);
        elevation.zVelocity(0);
        elevation.airborne(z != 0);
        this.spinningPitWarpArrival = z != 0;
    }

    int accessRomMotionState() {
        if (pit.groundStatus() == GroundStatus.PIT || pit.fallingIntoPit()) {
            return 0x06;
        }
        return linkStateMachine.get().status().motionState().romValue();
    }

    boolean accessIsAirborne() {
        return elevation.airborne();
    }

    int accessZVelocity() {
        return elevation.zVelocity() & 0xFF;
    }

    boolean accessIsFallingIntoPit() {
        return pit.fallingIntoPit();
    }

    void accessUseRocsFeather() {
        if (elevation.airborne()
                || linkSideView.get().sideViewFeatherPending
                || pit.groundStatus() == GroundStatus.PIT
                || pit.fallingIntoPit()
                || linkStateMachine.get().status().accessIsSwimming()) {
            return;
        }
        if (linkSideView.get().sideScrolling) {
            // The ROM runs $02:68B7 before UseRocsFeather ($00:14E6).
            // Defer its extra XY step until normal side-view motion finishes.
            linkSideView.get().sideViewFeatherPending = true;
            return;
        }
        linkAnimationState.get().topDownFeatherStarted = true;
        elevation.airborne(true);
        elevation.zSubPixels(0);
        elevation.zVelocity(0x20);
        linkAnimator.get().jumpAnimationCounter(0);
        linkAnimator.get().jumpAnimationFrame(0);
        linkServices.get().soundSink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);
    }

    void accessBounceFromGoomba() {
        elevation.airborne(true);
        elevation.zVelocity(0x10);
        pit.fallingIntoPit(false);
        pit.groundStatus(GroundStatus.NORMAL);
        linkSwimming.get().leaveSwimming();
    }

    int accessZPixels() {
        return Math.max(0, elevation.zSubPixels() >> SUB_PIXEL_SHIFT);
    }

    int accessTickDialogGotItem(boolean dialogActive) {
        int openForItem = dialogGotItemSequence.get().advance(dialogActive);
        if (dialogGotItemSequence.get().presentedItem() == 0) return openForItem;
        // ApplyGotItem caches body Y before func_21E1 updates Z; the held OAM
        // entry uses the updated Z. Preserve the fractional position at ground.
        linkAnimator.get().bedJumpRenderZ(accessZPixels());
        if (linkServices.get().itemRegistry != null
                && linkServices.get().itemRegistry.lookup(InventoryItem.SWORD.romValue())
                        instanceof linksawakening.equipment.Sword sword) {
            sword.resetSpinAttack();
        }
        linkItemState.get().romAttackStepAnimationCountdown = 0;
        linkDamage.get().collisionIgnoreFramesRemaining = 0;
        int z = (elevation.zSubPixels() + (byte) elevation.zVelocity()) & 0xFFF;
        elevation.zVelocity((byte) (elevation.zVelocity() - 2));
        if (((z >>> SUB_PIXEL_SHIFT) & 0x80) != 0) {
            z = 0;
            elevation.zVelocity(0);
        }
        elevation.zSubPixels(z);
        linkAnimator.get().romAnimationStateOverride(0x6B);
        linkAnimationState.get().movingThisFrame = false;
        linkMotion.get().lastRomSpeedX = linkMotion.get().lastRomSpeedY = 0;
        return openForItem;
    }
}
